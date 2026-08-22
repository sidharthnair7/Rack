package fileidea.rack.intake;

import fileidea.rack.common.ItemStatus;
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

@Entity
@Table(name = "items")
@Getter
@Setter
@NoArgsConstructor
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "batch_id")
    private Batch batch;

    @Column(nullable = false)
    private String sourceImageUrl;

    private String identifiedBrand;
    private String identifiedType;
    private String category;

    @Column(name = "item_condition")
    private String condition;

    private String userCorrectedBrand;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemStatus status = ItemStatus.UPLOADED;

    public String displayBrand() {
        if (userCorrectedBrand != null && !userCorrectedBrand.isBlank()) {
            return userCorrectedBrand;
        }
        return identifiedBrand;
    }
}
