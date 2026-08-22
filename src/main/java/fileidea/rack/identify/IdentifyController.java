package fileidea.rack.identify;

import fileidea.rack.intake.ItemDetailService;
import fileidea.rack.web.dto.Dtos;
import fileidea.rack.web.dto.ItemDetailResponse;
import fileidea.rack.web.dto.ItemResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fileidea.rack.intake.IntakeService;

@RestController
@RequestMapping("/api/items")
public class IdentifyController {

    private final IdentifyService identifyService;
    private final IntakeService intakeService;
    private final ItemDetailService details;

    public IdentifyController(IdentifyService identifyService, IntakeService intakeService, ItemDetailService details) {
        this.identifyService = identifyService;
        this.intakeService = intakeService;
        this.details = details;
    }

    @GetMapping("/{id}")
    public ItemResponse get(@PathVariable Long id) {
        return Dtos.item(intakeService.getItem(id));
    }

    @GetMapping("/{id}/detail")
    public ItemDetailResponse detail(@PathVariable Long id) {
        return details.detail(id);
    }

    @PostMapping("/{id}/identify")
    public ItemResponse identify(@PathVariable Long id) {
        return Dtos.item(identifyService.identify(id));
    }

    @PatchMapping("/{id}/brand")
    public ItemResponse correctBrand(@PathVariable Long id, @RequestParam String brand) {
        return Dtos.item(identifyService.correctBrand(id, brand));
    }
}
