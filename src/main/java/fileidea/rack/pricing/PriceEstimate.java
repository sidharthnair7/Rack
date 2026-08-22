package fileidea.rack.pricing;

import fileidea.rack.common.DemandDirection;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "price_estimates")
@Getter
@Setter
@NoArgsConstructor
public class PriceEstimate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", unique = true)
    private Item item;

    @Column(precision = 12, scale = 2)
    private BigDecimal retailAnchor;

    @Column(precision = 12, scale = 2)
    private BigDecimal medianSoldPrice;

    @Column(precision = 12, scale = 2)
    private BigDecimal p25;

    @Column(precision = 12, scale = 2)
    private BigDecimal p75;

    @Column(nullable = false)
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    private DemandDirection demandDirection;

    private Instant computedAt;

    @PrePersist
    void onCreate() {
        if (computedAt == null) {
            computedAt = Instant.now();
        }
    }
}
