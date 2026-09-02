package fileidea.rack.intake;

import fileidea.rack.common.BatchStatus;
import fileidea.rack.common.NotFoundException;
import fileidea.rack.common.ImageKind;
import fileidea.rack.common.ItemStatus;
import fileidea.rack.common.TaskStatus;
import fileidea.rack.common.Vendor;
import fileidea.rack.imaging.ImageAsset;
import fileidea.rack.imaging.ImageAssetRepository;
import fileidea.rack.integration.storage.ImageStorage;
import fileidea.rack.store.Store;
import fileidea.rack.store.StoreRepository;
import fileidea.rack.task.TaskEndpoints;
import fileidea.rack.task.TaskOrchestrator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Writes a finished upload to the database in one transaction.
 *
 * <p>Split out of {@link IntakeService} so that the slow part of an upload happens outside the
 * transaction. Garment detection is a vendor round trip of several seconds, and running it inside
 * the transaction meant every upload held a database connection for its whole duration - which
 * caps concurrent uploads at the connection pool size for no reason, since none of that waiting
 * needs a database at all.
 *
 * <p>It is a separate bean rather than another method on {@code IntakeService} because Spring's
 * {@code @Transactional} is proxy-based: a method calling an annotated method on {@code this}
 * bypasses the proxy entirely and runs with no transaction at all. That failure is silent, and it
 * would be a far worse bug than the one being fixed here.
 *
 * <p>The write stays a single transaction on purpose. Tasks are enqueued in it, so the poller
 * cannot see work for an item until the item, its image record and its batch are all committed.
 */
@Service
public class IntakeWriter {

    private final StoreRepository stores;
    private final BatchRepository batches;
    private final ItemRepository items;
    private final ImageAssetRepository images;
    private final ImageStorage storage;
    private final TaskOrchestrator tasks;

    public IntakeWriter(
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

    /** One garment ready to be stored: its pixels, and how to name the file they go in. */
    public record Piece(byte[] bytes, String extension, String contentType) {
    }

    /**
     * @param storeId the store to file this batch under, resolved inside the transaction rather
     *                than handed in as an entity. The caller loads the store first to fail fast on
     *                a bad id before spending seconds on detection, but that instance is detached
     *                by the time this runs, and attaching a detached entity to a new row is a way
     *                to get behaviour that depends on flush order rather than on the code.
     */
    @Transactional
    public Batch persist(Long storeId, List<Piece> pieces) {
        if (pieces == null || pieces.isEmpty()) {
            throw new IllegalArgumentException("upload at least one photo");
        }
        Store store = stores.findById(storeId)
                .orElseThrow(() -> new NotFoundException("store " + storeId));

        Batch batch = new Batch();
        batch.setStore(store);
        batch.setStatus(BatchStatus.RECEIVED);
        batch = batches.save(batch);

        List<Item> created = new ArrayList<>();
        for (Piece piece : pieces) {
            Item item = new Item();
            item.setBatch(batch);
            item.setStatus(ItemStatus.UPLOADED);
            item.setCondition("Good");
            item.setSourceImageUrl("pending");
            item = items.save(item);

            String key = "batches/" + batch.getId() + "/" + item.getId() + "-"
                    + UUID.randomUUID() + piece.extension();
            String url = storage.store(key, piece.bytes(), piece.contentType());
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

        batch.setItemCount(created.size());
        batch.setStatus(BatchStatus.IDENTIFYING);
        return batches.save(batch);
    }
}
