package fileidea.rack.task;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fileidea.rack.common.TaskStatus;
import fileidea.rack.common.Vendor;
import fileidea.rack.intake.Item;

@Service
public class TaskOrchestrator {

    private final VendorTaskRepository tasks;

    public TaskOrchestrator(VendorTaskRepository tasks) {
        this.tasks = tasks;
    }

    @Transactional
    public VendorTask enqueue(Item item, Vendor vendor, String endpoint, String idempotencyKey) {
        return tasks.findByIdempotencyKey(idempotencyKey).orElseGet(() -> {
            VendorTask task = new VendorTask();
            task.setItem(item);
            task.setVendor(vendor);
            task.setEndpoint(endpoint);
            task.setStatus(TaskStatus.PENDING);
            task.setAttempts(0);
            task.setIdempotencyKey(idempotencyKey);
            return tasks.save(task);
        });
    }

    /**
     * Puts a finished step back in the queue. Used when the seller corrects something the
     * pipeline inferred, so the downstream numbers are recomputed from the corrected input
     * rather than left stale. Unlike {@link #enqueue}, this deliberately overrides the
     * idempotency guard \u2014 the input changed, so the previous result is no longer valid.
     */
    @Transactional
    public void requeue(String idempotencyKey) {
        tasks.findByIdempotencyKey(idempotencyKey).ifPresent(task -> {
            task.setStatus(TaskStatus.PENDING);
            task.setAttempts(0);
            tasks.save(task);
        });
    }
}
