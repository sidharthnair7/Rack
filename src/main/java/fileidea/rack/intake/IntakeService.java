package fileidea.rack.intake;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import fileidea.rack.common.NotFoundException;
import fileidea.rack.config.ImagingProperties;
import fileidea.rack.store.StoreRepository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class IntakeService {

    private final StoreRepository stores;
    private final BatchRepository batches;
    private final ItemRepository items;
    private final GarmentDetector detector;
    private final ImagingProperties flags;
    private final IntakeWriter writer;

    public IntakeService(
            StoreRepository stores,
            BatchRepository batches,
            ItemRepository items,
            GarmentDetector detector,
            ImagingProperties flags,
            IntakeWriter writer
    ) {
        this.stores = stores;
        this.batches = batches;
        this.items = items;
        this.detector = detector;
        this.flags = flags;
        this.writer = writer;
    }

    /**
     * Deliberately not {@code @Transactional}.
     *
     * <p>Splitting a photo into its garments is a vendor round trip of several seconds. Inside a
     * transaction that would hold a database connection for the whole wait, capping concurrent
     * uploads at the pool size for work that touches no database at all. So the slow part happens
     * here, and {@link IntakeWriter} commits the finished result in one short transaction.
     */
    public Batch createBatch(Long storeId, List<MultipartFile> photos) {
        if (photos == null || photos.isEmpty()) {
            throw new IllegalArgumentException("upload at least one photo");
        }
        // Checked here so a bad store id fails immediately rather than after seconds of detection.
        stores.findById(storeId).orElseThrow(() -> new NotFoundException("store " + storeId));

        List<IntakeWriter.Piece> pieces = new ArrayList<>();
        for (MultipartFile photo : photos) {
            if (photo == null || photo.isEmpty()) {
                continue;
            }
            byte[] bytes = read(photo);

            // One photo can hold several garments. Splitting here rather than downstream means
            // each piece becomes an ordinary Item and travels the pipeline that already works,
            // including running in parallel with its siblings - so photographing a pile costs
            // roughly what photographing one garment costs.
            List<byte[]> garments = flags.splitGarments()
                    ? detector.split(bytes)
                    : List.of(bytes);
            boolean wasSplit = garments.size() > 1;

            for (byte[] garment : garments) {
                // A crop is re-encoded as JPEG regardless of what was uploaded, so it must not
                // inherit the original file's extension or content type.
                pieces.add(new IntakeWriter.Piece(
                        garment,
                        wasSplit ? ".jpg" : extension(photo),
                        wasSplit ? "image/jpeg" : photo.getContentType()));
            }
        }

        if (pieces.isEmpty()) {
            throw new IllegalArgumentException("upload at least one photo");
        }
        return writer.persist(storeId, pieces);
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
