package fileidea.rack.imaging;

import fileidea.rack.web.dto.Dtos;
import fileidea.rack.web.dto.ImageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/items")
public class ImagingController {

    private final ImagingService imagingService;
    private final ImageAssetRepository imageAssetRepository;

    public ImagingController(ImagingService imagingService, ImageAssetRepository imageAssetRepository) {
        this.imagingService = imagingService;
        this.imageAssetRepository = imageAssetRepository;
    }

    @PostMapping("/{id}/photograph")
    public List<ImageResponse> photograph(@PathVariable Long id) {
        return imagingService.photograph(id).stream().map(Dtos::image).toList();
    }

    @GetMapping("/{id}/images")
    public List<ImageResponse> images(@PathVariable Long id) {
        return imageAssetRepository.findByItemId(id).stream().map(Dtos::image).toList();
    }
}
