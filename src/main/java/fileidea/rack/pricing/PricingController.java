package fileidea.rack.pricing;

import fileidea.rack.intake.IntakeService;
import fileidea.rack.web.dto.Dtos;
import fileidea.rack.web.dto.PricePanelResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/items")
public class PricingController {

    private final PricingService pricingService;
    private final IntakeService intakeService;

    public PricingController(PricingService pricingService, IntakeService intakeService) {
        this.pricingService = pricingService;
        this.intakeService = intakeService;
    }

    @PostMapping("/{id}/price")
    public PricePanelResponse price(@PathVariable Long id) {
        var estimate = pricingService.price(id);
        return Dtos.pricePanel(
                intakeService.getItem(id),
                estimate,
                pricingService.compsFor(estimate.getId())
        );
    }

    @GetMapping("/{id}/price")
    public PricePanelResponse get(@PathVariable Long id) {
        var estimate = pricingService.get(id);
        return Dtos.pricePanel(
                intakeService.getItem(id),
                estimate,
                pricingService.compsFor(estimate.getId())
        );
    }
}
