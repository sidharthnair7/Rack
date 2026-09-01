# Setup

Rack boots and runs with **no keys at all**. Each integration degrades on its own:

| Missing | What happens |
|---|---|
| SerpApi key | Identify/price fail per item and are logged; nothing else breaks |
| Perfect Corp key | The original photo becomes the catalog image; the pipeline continues |
| name.com credentials | Domain search returns suggested names; the storefront serves at `/shop/{storeId}` |
| Stripe key | Listings show "Contact the seller" instead of a Buy button |

So you can develop the whole flow, then switch each vendor on independently.

---

## 1. Database

```bash
docker compose up -d
```

Postgres 16 on `localhost:5432`, database/user/password all `rack`.

---

## 2. Supplying keys

**Use environment variables.** Spring Boot's relaxed binding maps them automatically, and nothing lands in a file that could be committed.

```bash
export RACK_SERPAPI_API_KEY="..."
export RACK_PERFECTCORP_API_KEY="..."
export RACK_NAMECOM_USERNAME="..."
export RACK_NAMECOM_TOKEN="..."
export RACK_NAMECOM_STOREFRONT_IP="..."
export RACK_STRIPE_SECRET_KEY="sk_test_..."
```

PowerShell:

```bash
$env:RACK_SERPAPI_API_KEY="..."
```

The alternative is `src/main/resources/application-local.properties` (already in `.gitignore`), run with `--spring.profiles.active=local`. Copy `application-local.properties.example` to start.

**Never put a real key in `application.properties`.** That file is tracked.

---

## 3. Where to get each key

### SerpApi — https://serpapi.com/manage-api-key

⚠️ **The free tier is 250 searches/month.** Rack makes **four calls per item** (Lens, Shopping, eBay, Trends), so the free tier covers about 62 items total — for development *and* the demo combined.

**Email SerpApi and ask for hackathon credits before you start building.**

Until credits land, work against the cache (see §5).

### Perfect Corp — https://yce.perfectcorp.com/api-console

Authentication is a plain bearer token: `Authorization: Bearer YOUR_API_KEY`. No handshake, no token exchange.

These slugs have now been verified against a live account with a real key — all four enabled stages returned genuinely processed images:

| Config stage | Slug | Status |
|---|---|---|
| Background removal | `sod` | verified working |
| Relight | `lighting` | verified working |
| Enhance | `enhance` | verified working |
| Clothes try-on | `cloth-v4` | verified working |
| Studio background | `ai-studio` | **off by default** — themed portrait templates, not a product backdrop |

Also confirm the accepted values for `garment_category` on the try-on service. Rack sends `upper_body`, `lower_body`, or `full_body`; an unrecognised value causes the stage to fail soft and fall back to the catalog image.

**Set a synthetic model image** — never a photo of a real person:

```bash
export RACK_PERFECTCORP_MODEL_URL="https://.../your-generated-model.jpg"
```

Generate it with Perfect Corp's own **AI Image Generator** (text to image, Realistic style) so the reference is synthetic and the storefront look stays consistent across items. Not AI Avatar Generator: that one stylises a face you upload and returns a head-and-shoulders portrait, which try-on has no body to dress.

### name.com — https://www.name.com/account/settings/api

HTTP Basic auth with `username:token`.

- **Sandbox (default):** `https://api.dev.name.com` — free test registrations, use this for everything until the real demo domain.
- **Production:** set `RACK_NAMECOM_BASE_URL=https://api.name.com` **only** when registering the real domain. Registrations cost money.

`RACK_NAMECOM_STOREFRONT_IP` is the public IP of the deployed app. Without it, registration still happens but no A record is written.

### Stripe — https://dashboard.stripe.com/test/apikeys

Use the **test mode** secret key (`sk_test_...`). Test mode needs no business verification and no
bank details, and the checkout pages it produces are fully functional — use card `4242 4242 4242 4242`
with any future expiry.

Rack makes two calls per listing at publish time: create a Price (product declared inline), then a
Payment Link for it. Shipping address collection is enabled, so the seller gets a delivery address
with the payment. Each link is restricted to a single completed session, because every item is
one-of-a-kind.

⚠️ **Do not use a live key.** A demo that can take real money from a viewer is not a demo.

---

## 4. Feature flags

Each Perfect Corp stage can be turned off independently:

```properties
rack.imaging.background-removal=true
rack.imaging.lighting=true
rack.imaging.enhance=true
rack.imaging.studio=true
rack.imaging.try-on=true
```

If try-on quality disappoints, set `rack.imaging.try-on=false`. The rest of the pipeline is untouched and the before/after is still a strong demo.

---

## 5. Cache mode — for development and for the demo

Every SerpApi response is written to `cache/serpapi/` keyed by a hash of the image bytes or the query string.

```properties
rack.serpapi.cache-only=true
```

With this set, Rack serves **previously recorded real responses** and makes no network calls. A cache miss fails loudly rather than inventing data.

**Recommended workflow:**

1. Run once with a live key over the exact photos you will demo. Real calls, real responses, cached to disk.
2. Commit nothing from `cache/` (it is gitignored) but keep the directory.
3. Set `cache-only=true` for the recording.

Your demo is then deterministic and costs zero credits per take, while every number on screen still traces to a real sale. This is the same practice as pre-propagating the demo domain — a recorded real result, not a fabricated one.

---

## 6. Before recording

- [ ] Demo domain registered and propagated **at least a day** ahead (DNS takes up to 48h)
- [ ] SerpApi cache warmed over the exact demo photos, `cache-only=true`
- [ ] Perfect Corp service slugs and `garment_category` verified against a live account
- [ ] `RACK_PERFECTCORP_MODEL_URL` set to a synthetic model
- [ ] App reachable on the public domain, not just localhost
- [ ] No real person's photo anywhere in the system
- [ ] No price on screen without a visible source link
