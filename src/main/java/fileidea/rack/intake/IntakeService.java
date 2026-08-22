package fileidea.rack.intake;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import fileidea.rack.common.BatchStatus;
import fileidea.rack.common.ImageKind;
import fileidea.rack.common.ItemStatus;
import fileidea.rack.common.NotFoundException;
import fileidea.rack.common.TaskStatus;
import fileidea.rack.common.Vendor;
import fileidea.rack.imaging.ImageAsset;
import fileidea.rack.imaging.ImageAssetRepository;
import fileidea.rack.integration.storage.ImageStorage;
import fileidea.rack.store.Store;
import fileidea.rack.store.StoreRepository;
import fileidea.rack.task.TaskEndpoints;
import fileidea.rack.task.TaskOrchestrator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class IntakeService {

    private final StoreRepository stores;
    private final BatchRepository batches;
    private final ItemRepository items;
    private final ImageAssetRepository images;
    private final ImageStorage storage;
    private final TaskOrchestrator tasks;

    public IntakeService(
            StoreRepository stores,
            BatchRepository batches,
            ItemRepository items,
            ImageAssetRepository images,
            ImageStorage storage,
            TaskOrchestrator tasks
    ) {
        this.stores = stores;
        this.batches = batches;
        this.items = items;
        this.images = images;
        this.storage = storage;
        this.tasks = tasks;
    }

    @Transactional
    public Batch createBatch(Long storeId, List<MultipartFile> photos) {
        if (photos == null || photos.isEmpty()) {
            throw new IllegalArgumentException("upload at least one photo");
        }
        Store store = stores.findById(storeId).orElseThrow(() -> new NotFoundException("store " + storeId));

        Batch batch = new Batch();
        batch.setStore(store);
        batch.setStatus(BatchStatus.RECEIVED);
        batch = batches.save(batch);

        List<Item> created = new ArrayList<>();
        for (MultipartFile photo : photos) {
            if (photo == null || photo.isEmpty()) {
                continue;
            }
            Item item = new Item();
            item.setBatch(batch);
            item.setStatus(ItemStatus.UPLOADED);
            item.setCondition("Good");
            item.setSourceImageUrl("pending");
            item = items.save(item);

            String key = "batches/" + batch.getId() + "/" + item.getId() + "-" + UUID.randomUUID() + extension(photo);
            String url = storage.store(key, read(photo), photo.getContentType());
            item.setSourceImageUrl(url);
            items.save(item);

            ImageAsset original = new ImageAsset();
            original.setItem(item);
            original.setKind(ImageKind.ORIGINAL);
            original.setUrl(url);
            original.setStatus(TaskStatus.SUCCESS);
            images.save(original);

            tasks.enqueue(item, Vendor.SERPAPI, TaskEndpoints.LENS, TaskEndpoints.lensKey(item.getId()));
            created.add(item);
        }

        if (created.isEmpty()) {
            throw new IllegalArgumentException("upload at least one photo");
        }

        batch.setItemCount(created.size());
        batch.setStatus(BatchStatus.IDENTIFYING);
        return batches.save(batch);
    }

    @Transactional(readOnly = true)
    public Batch getBatch(Long id) {
        return batches.findById(id).orElseThrow(() -> new NotFoundException("batch " + id));
    }

    @Transactional(readOnly = true)
    public List<Item> itemsFor(Long batchId) {
        getBatch(batchId);
        return items.findByBatchId(batchId);
    }

    @Transactional(readOnly = true)
    public Item getItem(Long id) {
        return items.findById(id).orElseThrow(() -> new NotFoundException("item " + id));
    }

    private static byte[] read(MultipartFile photo) {
        try {
            return photo.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String extension(MultipartFile photo) {
        String name = photo.getOriginalFilename();
        if (name != null && name.contains(".")) {
            return name.substring(name.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        }
        return ".jpg";
    }
}
