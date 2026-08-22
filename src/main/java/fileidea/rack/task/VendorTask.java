package fileidea.rack.task;

import fileidea.rack.common.TaskStatus;
import fileidea.rack.common.Vendor;
import fileidea.rack.intake.Item;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
public class VendorTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id")
    private Item item;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Vendor vendor;

    @Column(nullable = false)
    private String endpoint;

    private String externalTaskId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.PENDING;

    private int attempts;

    private Instant lastPolledAt;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;
}
