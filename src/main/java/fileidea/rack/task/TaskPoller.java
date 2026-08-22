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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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

    @Scheduled(fixedDelayString = "${rack.tasks.poll-interval-ms:15000}")
    public void poll() {
        List<VendorTask> due = new ArrayList<>();
        due.addAll(tasks.findByStatus(TaskStatus.PENDING));
        due.addAll(tasks.findByStatus(TaskStatus.IN_FLIGHT));
        for (VendorTask task : due) {
            dispatch(task);
        }
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
            log.warn("task {} failed for item {}: {}", task.getId(), itemId, e.getMessage());
            task.setStatus(TaskStatus.ERROR);
            tasks.save(task);
        }
    }
}
