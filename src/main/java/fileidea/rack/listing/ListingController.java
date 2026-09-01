package fileidea.rack.listing;

import fileidea.rack.common.ImageKind;
import fileidea.rack.imaging.ImageAsset;
import fileidea.rack.imaging.ImageAssetRepository;
import fileidea.rack.imaging.ImagingService;
import fileidea.rack.intake.IntakeService;
import fileidea.rack.intake.Item;
import fileidea.rack.pricing.Comp;
import fileidea.rack.pricing.PriceEstimate;
import fileidea.rack.pricing.PricingService;
import fileidea.rack.store.Store;
import fileidea.rack.store.StoreService;
import fileidea.rack.web.dto.Dtos;
import fileidea.rack.web.dto.ListingResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The public storefront. This is the page that lives at the domain the seller registers, so it is
 * what a visitor actually judges the product by. It renders the sourced price *and* the live
 * comparable listings the price came from, because a number nobody can check is worth nothing here.
 */
@RestController
public class ListingController {

    /** Comps rendered on a listing page. The price still derives from every result found. */
    private static final int MAX_VISIBLE_COMPS = 6;

    private static final DateTimeFormatter SOLD_ON =
            DateTimeFormatter.ofPattern("MMM d", Locale.US).withZone(ZoneOffset.UTC);

    private final ListingService listingService;
    private final IntakeService intakeService;
    private final ImagingService imagingService;
    private final ImageAssetRepository images;
    private final PricingService pricingService;
    private final StoreService storeService;

    public ListingController(
            ListingService listingService,
            IntakeService intakeService,
            ImagingService imagingService,
            ImageAssetRepository images,
            PricingService pricingService,
            StoreService storeService
    ) {
        this.listingService = listingService;
        this.intakeService = intakeService;
        this.imagingService = imagingService;
        this.images = images;
        this.pricingService = pricingService;
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

    // ---------------------------------------------------------------- storefront

    @GetMapping(value = "/shop/{storeId}", produces = MediaType.TEXT_HTML_VALUE)
    public String storefront(@PathVariable Long storeId) {
        Store store = storeService.get(storeId);
        List<Listing> listings = listingService.forStore(storeId);

        BigDecimal total = BigDecimal.ZERO;
        StringBuilder cards = new StringBuilder();
        for (Listing listing : listings) {
            Long itemId = listing.getItem().getId();
            total = total.add(listing.getAskingPrice());
            String img = imagingService.catalogUrl(itemId);
            cards.append("<a class='card' href='/shop/").append(storeId).append('/').append(itemId).append("'>")
                    .append(img == null
                            ? "<div class='ph'></div>"
                            : "<img loading='lazy' src='" + esc(img) + "' alt='" + esc(listing.getTitle()) + "'>")
                    .append("<div class='meta'><span class='name'>").append(esc(listing.getTitle()))
                    .append("</span><span class='price'>").append(money(listing.getAskingPrice()))
                    .append("</span></div></a>");
        }

        String subtitle = listings.isEmpty()
                ? "Nothing listed yet."
                : listings.size() + (listings.size() == 1 ? " piece" : " pieces")
                        + " &middot; " + money(total) + " of inventory";

        String body = """
                <header class="masthead">
                  <div>
                    <h1>%s</h1>
                    <p class="sub">%s</p>
                  </div>
                  %s
                </header>
                <p class="proof">Every price here is the median of real comparable listings on eBay. Open any piece to see them.</p>
                <div class="grid">%s</div>
                <footer>Listed with <strong>Rack</strong> &middot; photographed, priced, and published from one photo.</footer>
                """.formatted(
                esc(store.getName()),
                subtitle,
                store.getDomain() == null ? "" : "<span class='domain'>" + esc(store.getDomain()) + "</span>",
                cards.length() == 0 ? "<p class='empty'>No pieces published yet.</p>" : cards.toString()
        );
        return page(store.getName(), body);
    }

    @GetMapping(value = "/shop/{storeId}/{itemId}", produces = MediaType.TEXT_HTML_VALUE)
    public String listingPage(@PathVariable Long storeId, @PathVariable Long itemId) {
        Listing listing = listingService.get(itemId);
        Item item = intakeService.getItem(itemId);
        Store store = storeService.get(storeId);
        String hero = imagingService.catalogUrl(itemId);

        String body = """
                <p class="back"><a href="/shop/%d">&larr; %s</a></p>
                <div class="detail">
                  <div class="shot">
                    %s
                    %s
                  </div>
                  <div class="info">
                    <h1>%s</h1>
                    <p class="ask">%s</p>
                    %s
                    <p class="desc">%s</p>
                    %s
                    %s
                  </div>
                </div>
                <footer>Listed with <strong>Rack</strong></footer>
                """.formatted(
                storeId,
                esc(store.getName()),
                hero == null
                        ? "<div class='ph tall'></div>"
                        : "<img src='" + esc(hero) + "' alt='" + esc(listing.getTitle()) + "'>",
                beforeAfter(itemId, hero),
                esc(listing.getTitle()),
                money(listing.getAskingPrice()),
                priceContext(itemId, item),
                esc(listing.getDescription()),
                buyBlock(listing),
                compsBlock(itemId)
        );
        return page(listing.getTitle(), body);
    }

    /**
     * The original phone photo beside what the pipeline produced. It is the clearest single
     * statement of what this product does, so it belongs on the public page, not only in a demo.
     */
    private String beforeAfter(Long itemId, String hero) {
        Optional<String> original = images.findByItemId(itemId).stream()
                .filter(asset -> asset.getKind() == ImageKind.ORIGINAL && asset.getUrl() != null)
                .map(ImageAsset::getUrl)
                .findFirst();
        if (original.isEmpty() || hero == null || original.get().equals(hero)) {
            return "";
        }
        return """
                <div class="ba">
                  <figure><img src="%s" alt="original photo"><figcaption>As photographed</figcaption></figure>
                  <figure><img src="%s" alt="processed photo"><figcaption>As listed</figcaption></figure>
                </div>
                """.formatted(esc(original.get()), esc(hero));
    }

    private String priceContext(Long itemId, Item item) {
        PriceEstimate estimate;
        try {
            estimate = pricingService.get(itemId);
        } catch (RuntimeException noPrice) {
            return "";
        }
        StringBuilder out = new StringBuilder("<ul class='ctx'>");
        if (item.getCondition() != null && !item.getCondition().isBlank()) {
            out.append("<li>Condition: ").append(esc(item.getCondition())).append("</li>");
        }
        if (estimate.getP25() != null && estimate.getP75() != null) {
            out.append("<li>Comparable listings run ").append(money(estimate.getP25()))
                    .append(" &ndash; ").append(money(estimate.getP75())).append("</li>");
        }
        if (estimate.getRetailAnchor() != null) {
            out.append("<li>Retails new at ").append(money(estimate.getRetailAnchor())).append("</li>");
        }
        if (estimate.getDemandDirection() != null) {
            out.append("<li>Search demand is ")
                    .append(estimate.getDemandDirection().name().toLowerCase(Locale.US))
                    .append(" over the last year</li>");
        }
        return out.append("</ul>").toString();
    }

    private String buyBlock(Listing listing) {
        if (listing.getCheckoutUrl() != null && !listing.getCheckoutUrl().isBlank()) {
            return "<a class='buy' href='" + esc(listing.getCheckoutUrl()) + "'>Buy &mdash; "
                    + money(listing.getAskingPrice()) + "</a>"
                    + "<p class='ship'>Secure checkout. Shipping address collected at payment.</p>";
        }
        return "<p class='ship'>Contact the seller to buy this piece.</p>";
    }

    /**
     * The listings the price came from, each one clickable. This is the whole credibility
     * argument: nothing on this page is a model's opinion of what the garment is worth. They are
     * live asking prices rather than completed sales - see SerpApiClient#ebayComps for why - so
     * the wording here says "listings", never "sold".
     */
    private String compsBlock(Long itemId) {
        List<Comp> comps;
        try {
            comps = pricingService.compsFor(pricingService.get(itemId).getId());
        } catch (RuntimeException noPrice) {
            return "";
        }
        if (comps.isEmpty()) {
            return "";
        }
        int total = 0;
        try {
            total = pricingService.get(itemId).getCompCount();
        } catch (RuntimeException ignored) {
            total = comps.size();
        }
        StringBuilder rows = new StringBuilder();
        int shown = 0;
        for (Comp comp : comps) {
            if (shown++ >= MAX_VISIBLE_COMPS) {
                break;
            }
            rows.append("<li><a href='").append(esc(comp.getSourceUrl()))
                    .append("' target='_blank' rel='noopener nofollow'>")
                    .append("<span class='cp'>").append(money(comp.getPrice())).append("</span>")
                    .append("<span class='ct'>").append(esc(comp.getTitle())).append("</span>")
                    .append(comp.getSoldDate() == null
                            ? "<span class='cd'></span>"
                            : "<span class='cd'>sold " + SOLD_ON.format(comp.getSoldDate()) + "</span>")
                    .append("</a></li>");
        }
        // shown is post-incremented on the iteration that breaks, so it overshoots by one;
        // derive the rendered count directly instead, or the maths on the page does not add up.
        int rendered = Math.min(MAX_VISIBLE_COMPS, comps.size());
        int remaining = Math.max(0, total - rendered);
        String more = remaining > 0
                ? "<p class='more'>&hellip; and " + remaining + " more comparable listings</p>"
                : "";
        return "<div class='comps'><h2>Priced from " + total
                + " comparable listings</h2><ul>" + rows + "</ul>" + more + "</div>";
    }

    // ---------------------------------------------------------------- shell

    private static String page(String title, String body) {
        return """
                <!DOCTYPE html><html lang="en"><head><meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>%s</title><style>
                :root{--ink:#1c1a17;--mute:#6e6862;--line:#e2dbd1;--bg:#f7f5f3;--card:#fff;--accent:#8a5a2b}
                *{box-sizing:border-box}
                body{margin:0;background:var(--bg);color:var(--ink);
                  font:16px/1.6 Georgia,"Times New Roman",serif;-webkit-font-smoothing:antialiased}
                a{color:inherit}
                .wrap{max-width:1040px;margin:0 auto;padding:2.5rem 1.25rem 4rem}
                .masthead{display:flex;justify-content:space-between;align-items:flex-end;gap:1rem;
                  border-bottom:1px solid var(--line);padding-bottom:1.25rem;flex-wrap:wrap}
                h1{margin:0;font-weight:500;letter-spacing:.01em;font-size:2rem}
                .sub{margin:.35rem 0 0;color:var(--mute)}
                .domain{color:var(--accent);font-size:.9rem;letter-spacing:.08em;text-transform:uppercase}
                .proof{color:var(--mute);font-size:.92rem;margin:1.25rem 0 1.75rem}
                .grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(230px,1fr));gap:1.5rem}
                .card{background:var(--card);text-decoration:none;display:block;border:1px solid var(--line);
                  transition:transform .15s ease,box-shadow .15s ease}
                .card:hover{transform:translateY(-2px);box-shadow:0 6px 20px rgba(0,0,0,.07)}
                .card img,.ph{width:100%%;aspect-ratio:3/4;object-fit:cover;display:block;background:#efe9e0}
                .meta{padding:.75rem .85rem;display:flex;justify-content:space-between;gap:.5rem;align-items:baseline}
                .name{font-size:.95rem}
                .price{color:var(--accent);white-space:nowrap}
                .empty{color:var(--mute)}
                .back{margin:0 0 1.5rem}
                .detail{display:grid;grid-template-columns:1.1fr .9fr;gap:2.5rem;align-items:start}
                .shot img{width:100%%;background:var(--card);border:1px solid var(--line);display:block}
                .ba{display:grid;grid-template-columns:1fr 1fr;gap:.75rem;margin-top:.75rem}
                .ba figure{margin:0}
                .ba img{width:100%%;aspect-ratio:3/4;object-fit:cover;border:1px solid var(--line)}
                .ba figcaption{font-size:.75rem;color:var(--mute);text-align:center;padding-top:.35rem;
                  letter-spacing:.06em;text-transform:uppercase}
                .ask{font-size:2rem;color:var(--accent);margin:.5rem 0 .75rem}
                .ctx{list-style:none;padding:0;margin:0 0 1.25rem;color:var(--mute);font-size:.92rem}
                .ctx li{padding:.15rem 0}
                .desc{margin:0 0 1.5rem}
                .buy{display:block;text-align:center;background:var(--ink);color:var(--bg);
                  text-decoration:none;padding:.85rem 1rem;letter-spacing:.03em}
                .buy:hover{background:var(--accent)}
                .ship{color:var(--mute);font-size:.85rem;margin:.6rem 0 0}
                .comps{margin-top:2rem;border-top:1px solid var(--line);padding-top:1.25rem}
                .comps h2{font-size:.78rem;letter-spacing:.1em;text-transform:uppercase;
                  color:var(--mute);font-weight:500;margin:0 0 .6rem}
                .comps ul{list-style:none;padding:0;margin:0}
                .comps a{display:grid;grid-template-columns:4.5rem 1fr auto;gap:.75rem;align-items:baseline;
                  padding:.45rem 0;border-bottom:1px solid var(--line);text-decoration:none;font-size:.88rem}
                .comps a:hover .ct{text-decoration:underline}
                .cp{color:var(--accent)}
                .ct{color:var(--ink);overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
                .cd{color:var(--mute);font-size:.8rem;white-space:nowrap}
                .more{color:var(--mute);font-size:.85rem;margin:.6rem 0 0}
                footer{margin-top:3.5rem;padding-top:1.25rem;border-top:1px solid var(--line);
                  color:var(--mute);font-size:.85rem}
                @media(max-width:720px){.detail{grid-template-columns:1fr;gap:1.5rem}h1{font-size:1.6rem}}
                </style></head><body><div class="wrap">%s</div></body></html>
                """.formatted(esc(title), body);
    }

    private static String money(BigDecimal amount) {
        if (amount == null) {
            return "&mdash;";
        }
        return "$" + amount.stripTrailingZeros().toPlainString();
    }

    private static String esc(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
