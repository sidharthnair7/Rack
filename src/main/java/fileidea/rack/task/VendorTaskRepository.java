package fileidea.rack.task;

import fileidea.rack.common.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VendorTaskRepository extends JpaRepository<VendorTask, Long> {

    List<VendorTask> findByStatus(TaskStatus status);

    Optional<VendorTask> findByIdempotencyKey(String idempotencyKey);
}
