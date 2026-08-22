package fileidea.rack.identify;

import tools.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fileidea.rack.common.ItemStatus;
import fileidea.rack.common.NotFoundException;
import fileidea.rack.common.NotImplemented;
import fileidea.rack.common.Vendor;
import fileidea.rack.intake.Item;
import fileidea.rack.intake.ItemRepository;
import fileidea.rack.integration.serpapi.SerpApiClient;
import fileidea.rack.integration.storage.ImageStorage;
import fileidea.rack.task.TaskEndpoints;
import fileidea.rack.task.TaskOrchestrator;

@Service
public class IdentifyService {

    private final ItemRepository items;
    private final ImageStorage storage;
    private final SerpApiClient serpApi;
    private final TaskOrchestrator tasks;

    public IdentifyService(
            ItemRepository items,
            ImageStorage storage,
            SerpApiClient serpApi,
            TaskOrchestrator tasks
    ) {
        this.items = items;
        this.storage = storage;
        this.serpApi = serpApi;
        this.tasks = tasks;
    }

    @Transactional
    public Item identify(Long itemId) {
        Item item = items.findById(itemId).orElseThrow(() -> new NotFoundException("item " + itemId));
        byte[] image = storage.read(storage.keyFromPublicUrl(item.getSourceImageUrl()));
        JsonNode lens = serpApi.lens(image);
        Identification guess = interpretLens(lens);
        item.setIdentifiedBrand(guess.brand());
        item.setIdentifiedType(guess.garmentType());
        item.setCategory(guess.category());
        item.setStatus(ItemStatus.IDENTIFIED);
        items.save(item);
        tasks.enqueue(item, Vendor.SERPAPI, TaskEndpoints.PRICE, TaskEndpoints.priceKey(item.getId()));
        return item;
    }

    @Transactional
    public Item correctBrand(Long itemId, String brand) {
        if (brand == null || brand.isBlank()) {
            throw new IllegalArgumentException("brand is required");
        }
        Item item = items.findById(itemId).orElseThrow(() -> new NotFoundException("item " + itemId));
        item.setUserCorrectedBrand(brand.trim());
        return items.save(item);
    }

    /**
     * YOUR LOGIC.
     *
     * Google Lens JSON is noisy: visual_matches[].title, sometimes knowledge_graph,
     * sometimes text_results. Pull out brand + garment type + category.
     *
     * Correctability is a feature — a decent guess the seller can tap to fix beats
     * a blank field. Spec lives in IdentifyServiceTest.
     */
    public Identification interpretLens(JsonNode lensJson) {
        return NotImplemented.yet("YOUR LOGIC: IdentifyService.interpretLens");
    }
}
