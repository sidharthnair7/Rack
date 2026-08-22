package fileidea.rack.listing;

import fileidea.rack.imaging.ImagingService;
import fileidea.rack.intake.IntakeService;
import fileidea.rack.store.Store;
import fileidea.rack.store.StoreService;
import fileidea.rack.web.dto.Dtos;
import fileidea.rack.web.dto.ListingResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
public class ListingController {

    private final ListingService listingService;
    private final IntakeService intakeService;
    private final ImagingService imagingService;
    private final StoreService storeService;

    public ListingController(
            ListingService listingService,
            IntakeService intakeService,
            ImagingService imagingService,
            StoreService storeService
    ) {
        this.listingService = listingService;
        this.intakeService = intakeService;
        this.imagingService = imagingService;
        this.storeService = storeService;
    }

    @PostMapping("/api/items/{id}/publish")
    public ListingResponse publish(@PathVariable Long id) {
        return Dtos.listing(listingService.publish(id));
    }

    @GetMapping("/api/items/{id}/listing")
    public ListingResponse get(@PathVariable Long id) {
        return Dtos.listing(listingService.get(id));
    }

    @GetMapping("/api/stores/{id}/listings")
    public List<ListingResponse> storeListings(@PathVariable Long id) {
        return listingService.forStore(id).stream().map(Dtos::listing).toList();
    }

    @GetMapping(value = "/shop/{storeId}", produces = MediaType.TEXT_HTML_VALUE)
    public String storefront(@PathVariable Long storeId) {
        Store store = storeService.get(storeId);
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='utf-8'><title>")
                .append(esc(store.getName()))
                .append("</title><style>")
                .append("body{font-family:Georgia,serif;margin:2rem auto;max-width:960px;background:#f6f1ea;color:#1a1a1a}")
                .append("h1{font-weight:500}.grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(220px,1fr));gap:1rem}")
                .append(".card{background:#fff;padding:1rem;text-decoration:none;color:inherit;display:block}")
                .append("img{width:100%;aspect-ratio:3/4;object-fit:cover}.price{font-size:1.2rem}")
                .append("</style></head><body><h1>")
                .append(esc(store.getName()));
        if (store.getDomain() != null) {
            html.append(" · ").append(esc(store.getDomain()));
        }
        html.append("</h1><p>Priced from real sold listings.</p><div class='grid'>");
        BigDecimal total = BigDecimal.ZERO;
        for (var listing : listingService.forStore(storeId)) {
            Long itemId = listing.getItem().getId();
            total = total.add(listing.getAskingPrice());
            String img = imagingService.catalogUrl(itemId);
            html.append("<a class='card' href='/shop/").append(storeId).append("/").append(itemId).append("'>");
            if (img != null) {
                html.append("<img src='").append(esc(img)).append("' alt=''>");
            }
            html.append("<p>").append(esc(listing.getTitle())).append("</p>")
                    .append("<p class='price'>$").append(listing.getAskingPrice()).append("</p></a>");
        }
        html.append("</div><p>$").append(total).append(" of inventory</p></body></html>");
        return html.toString();
    }

    @GetMapping(value = "/shop/{storeId}/{itemId}", produces = MediaType.TEXT_HTML_VALUE)
    public String listingPage(@PathVariable Long storeId, @PathVariable Long itemId) {
        var listing = listingService.get(itemId);
        var item = intakeService.getItem(itemId);
        String img = imagingService.catalogUrl(itemId);
        return "<!DOCTYPE html><html><head><meta charset='utf-8'><title>" + esc(listing.getTitle())
                + "</title><style>body{font-family:Georgia,serif;margin:2rem auto;max-width:720px;background:#f6f1ea}"
                + "img{width:100%;max-height:70vh;object-fit:contain;background:#fff}</style></head><body>"
                + "<p><a href='/shop/" + storeId + "'>← " + esc(item.getBatch().getStore().getName()) + "</a></p>"
                + (img == null ? "" : "<img src='" + esc(img) + "' alt=''>")
                + "<h1>" + esc(listing.getTitle()) + "</h1>"
                + "<p style='font-size:1.6rem'>$" + listing.getAskingPrice() + "</p>"
                + "<p>" + esc(listing.getDescription()) + "</p>"
                + "</body></html>";
    }

    private static String esc(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
