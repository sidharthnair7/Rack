package fileidea.rack.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import fileidea.rack.common.TaskStatus;
import fileidea.rack.identify.IdentifyService;
import fileidea.rack.imaging.ImagingService;
import fileidea.rack.listing.ListingService;
import fileidea.rack.pricing.PricingService;

import jakarta.annotation.PreDestroy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@ConditionalOnProperty(prefix = "rack.tasks", name = "poller-enabled", havingValue = "true")
public class TaskPoller {

    private static final Logger log = LoggerFactory.getLogger(TaskPoller.class);

    private final VendorTaskRepository tasks;
    private final IdentifyService identifyService;
    private final PricingService pricingService;
    private final ImagingService imagingService;
    private final ListingService listingService;

    public TaskPoller(
            VendorTaskRepository tasks,
            IdentifyService identifyService,
            PricingService pricingService,
            ImagingService imagingService,
            ListingService listingService
    ) {
        this.tasks = tasks;
        this.identifyService = identifyService;
        this.pricingService = pricingService;
        this.imagingService = imagingService;
        this.listingService = listingService;
    }

    /** A task that has failed this many times stops being retried, so one bad photo cannot loop. */
    private static final int MAX_ATTEMPTS = 4;

    /**
     * Items are independent chains, so they are worked in parallel. Serial dispatch meant the
     * whole batch moved at the speed of the slowest vendor call, one item at a time.
     */
    private final ExecutorService workers = Executors.newFixedThreadPool(4);

    /** Guards against the next tick re-dispatching a task that is still running here. */
    private final Set<Long> running = ConcurrentHashMap.newKeySet();

    @Scheduled(fixedDelayString = "${rack.tasks.poll-interval-ms:15000}")
    public void poll() {
        List<VendorTask> due = new ArrayList<>();
        due.addAll(tasks.findByStatus(TaskStatus.PENDING));
        due.addAll(tasks.findByStatus(TaskStatus.IN_FLIGHT));
        for (VendorTask task : due) {
            Long taskId = task.getId();
            if (taskId == null || !running.add(taskId)) {
                continue;
            }
            if (task.getAttempts() >= MAX_ATTEMPTS) {
                log.warn("task {} gave up after {} attempts", taskId, task.getAttempts());
                task.setStatus(TaskStatus.ERROR);
                tasks.save(task);
                running.remove(taskId);
                continue;
            }
            // execute(), not submit(): submit() parks any Throwable inside a Future that nobody
            // reads, so an Error (as opposed to an Exception) vanishes without a trace while
            // attempts still climb to the retry ceiling. Catching Throwable here means a failure
            // is always attributable to a task instead of disappearing.
            workers.execute(() -> {
                try {
                    dispatch(task);
                } catch (Throwable t) {
                    log.error("task {} threw {} outside normal handling", taskId, t.getClass().getName(), t);
                    task.setStatus(TaskStatus.ERROR);
                    tasks.save(task);
                } finally {
                    running.remove(taskId);
                }
            });
        }
    }

    @PreDestroy
    void shutdown() {
        workers.shutdownNow();
    }

    private void dispatch(VendorTask task) {
        Long itemId = task.getItem().getId();
        task.setStatus(TaskStatus.IN_FLIGHT);
        task.setLastPolledAt(Instant.now());
        task.setAttempts(task.getAttempts() + 1);
        tasks.save(task);
        try {
            switch (task.getEndpoint()) {
                case TaskEndpoints.LENS -> identifyService.identify(itemId);
                case TaskEndpoints.PRICE -> pricingService.price(itemId);
                case TaskEndpoints.PHOTOGRAPH -> imagingService.photograph(itemId);
                case TaskEndpoints.PUBLISH -> listingService.publish(itemId);
                default -> throw new IllegalStateException("unknown endpoint " + task.getEndpoint());
            }
            task.setStatus(TaskStatus.SUCCESS);
            tasks.save(task);
        } catch (UnsupportedOperationException waiting) {
            log.info("waiting on your logic for item {} ({}): {}", itemId, task.getEndpoint(), waiting.getMessage());
            task.setStatus(TaskStatus.PENDING);
            task.setAttempts(Math.max(0, task.getAttempts() - 1));
            tasks.save(task);
        } catch (Exception e) {
            // Back to PENDING, not ERROR, so the next tick tries again.
            //
            // This used to dead-end on the first failure, which meant MAX_ATTEMPTS was unreachable
            // for the one class of failure it exists for. A single blip from a vendor - a timeout,
            // a 502, a dropped connection - left the item stranded in a non-terminal state with
            // nothing to move it: the UI polls for two minutes and then reports the piece as
            // unlistable. Every stage here is idempotent (each writes by item id and the imaging
            // stages re-upload rather than resume), so retrying is safe, and the attempts ceiling
            // above still stops a genuinely bad photo from looping.
            int attempts = task.getAttempts();
            if (attempts >= MAX_ATTEMPTS) {
                log.warn("task {} failed for item {} on final attempt {}: {}",
                        task.getId(), itemId, attempts, e.getMessage());
                task.setStatus(TaskStatus.ERROR);
            } else {
                log.warn("task {} failed for item {} (attempt {} of {}), retrying: {}",
                        task.getId(), itemId, attempts, MAX_ATTEMPTS, e.getMessage());
                task.setStatus(TaskStatus.PENDING);
            }
            tasks.save(task);
        }
    }
}
