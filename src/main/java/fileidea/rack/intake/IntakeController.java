package fileidea.rack.intake;

import fileidea.rack.web.dto.BatchResponse;
import fileidea.rack.web.dto.Dtos;
import fileidea.rack.web.dto.ItemResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/batches")
public class IntakeController {

    private final IntakeService intakeService;

    public IntakeController(IntakeService intakeService) {
        this.intakeService = intakeService;
    }

    @PostMapping
    public BatchResponse create(
            @RequestParam Long storeId,
            @RequestParam("photos") List<MultipartFile> photos
    ) {
        var batch = intakeService.createBatch(storeId, photos);
        return Dtos.batch(batch, intakeService.itemsFor(batch.getId()));
    }

    @GetMapping("/{id}")
    public BatchResponse get(@PathVariable Long id) {
        return Dtos.batch(intakeService.getBatch(id), intakeService.itemsFor(id));
    }

    @GetMapping("/{id}/items")
    public List<ItemResponse> items(@PathVariable Long id) {
        return intakeService.itemsFor(id).stream().map(Dtos::item).toList();
    }
}
