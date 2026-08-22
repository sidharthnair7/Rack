package fileidea.rack.intake;

import fileidea.rack.common.BatchStatus;
import fileidea.rack.store.Store;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "batches")
@Getter
@Setter
@NoArgsConstructor
public class Batch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "store_id")
    private Store store;

    private Instant uploadedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BatchStatus status = BatchStatus.RECEIVED;

    private int itemCount;

    @Column(precision = 12, scale = 2)
    private BigDecimal totalEstimatedValue = BigDecimal.ZERO;

    @PrePersist
    void onCreate() {
        if (uploadedAt == null) {
            uploadedAt = Instant.now();
        }
    }
}
