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
}
