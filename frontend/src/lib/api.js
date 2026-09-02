// Real client for the Rack backend.
//
// This file previously returned hardcoded prices, `link: "#"` comps, and a stock photo of a real
// person captioned "AI Generated Virtual Try-On". That is the one thing this product cannot do:
// the entire premise is that every number on screen came from a real listing you can click and
// check. Nothing here is invented — every value below is fetched from the API.

const TERMINAL = ['LISTED', 'FAILED'];

async function json(res) {
  if (!res.ok) {
    const body = await res.text().catch(() => '');
    // Prefer the server's own sentence over the status line. The API returns a JSON error body,
    // and pasting that raw put things like `400 Bad Request: {"message":"sidshops.com is already
    // registered..."}` in front of the user - the useful half wrapped in the half that is noise.
    let detail = body.slice(0, 300);
    try {
      const parsed = JSON.parse(body);
      detail = parsed.message || parsed.error || parsed.detail || detail;
    } catch {
      // Not JSON, so the raw text is the best we have.
    }
    throw new Error(detail ? detail : `${res.status} ${res.statusText}`);
  }
  return res.json();
}

/** The demo seller/store the backend seeds on startup. */
export async function getDemoStore() {
  const data = await json(await fetch('/api/demo'));
  return data.storeId;
}

export async function uploadBatch(files, storeId) {
  const form = new FormData();
  for (const file of files) form.append('photos', file);
  return json(await fetch(`/api/batches?storeId=${storeId}`, { method: 'POST', body: form }));
}

export async function fetchBatch(batchId) {
  return json(await fetch(`/api/batches/${batchId}`));
}

export async function fetchItemDetail(itemId) {
  return json(await fetch(`/api/items/${itemId}/detail`));
}

/** Re-runs pricing and the listing copy from a corrected brand. Imaging is not repeated. */
export async function correctBrand(itemId, brand) {
  return json(await fetch(`/api/items/${itemId}/brand?brand=${encodeURIComponent(brand)}`, {
    method: 'PATCH',
  }));
}

/** Runs the full name.com registration: availability, register, A record, subdomain, forwarding. */
export async function registerDomain(storeId, domain) {
  return json(await fetch(
    `/api/domains/register?storeId=${storeId}&domain=${encodeURIComponent(domain)}`,
    { method: 'POST' }
  ));
}

export async function searchDomains(query) {
  return json(await fetch(`/api/domains/search?query=${encodeURIComponent(query)}`, {
    method: 'POST',
  }));
}

/**
 * Waits for every item in a batch to reach a terminal state.
 *
 * The backend fans each item out across four vendors and polls them on a scheduler, so a batch
 * legitimately takes tens of seconds — this reports progress as it goes rather than sitting on a
 * spinner. It resolves (rather than throwing) on timeout so a slow vendor degrades to partial
 * results instead of losing the whole batch.
 *
 * Polled at 1.2s rather than 2.5s because the loading screen now renders the item's real stage:
 * four stages across ~20s at a 2.5s interval meant a finished stage could sit unacknowledged for
 * longer than some stages take, which reads as a frozen screen. The extra requests are a handful
 * of local reads against an already-open batch.
 */
export async function waitForBatch(batchId, onProgress, { timeoutMs = 120000, intervalMs = 1200 } = {}) {
  const deadline = Date.now() + timeoutMs;
  let batch = await fetchBatch(batchId);

  while (Date.now() < deadline) {
    const items = batch.items ?? [];
    const done = items.filter(i => TERMINAL.includes(i.status)).length;
    onProgress?.({ done, total: items.length || 1, items, batch });
    if (items.length > 0 && done === items.length) return batch;
    await new Promise(r => setTimeout(r, intervalMs));
    batch = await fetchBatch(batchId);
  }
  return batch;
}

/**
 * Upload → wait → collect full detail for every item.
 *
 * Accepts one File or several. Returns only items that actually produced a sourced price:
 * an item the backend failed (no comparable listings found) is deliberately not shown as a
 * result, because a listing with no evidence behind it is exactly what this product refuses
 * to render.
 */
export async function processImage(input, onProgress) {
  const files = Array.isArray(input) ? input : [input];
  const storeId = await getDemoStore();

  const created = await uploadBatch(files, storeId);
  const batch = await waitForBatch(created.id, onProgress);

  const details = await Promise.all(
    (batch.items ?? []).map(item => fetchItemDetail(item.id).catch(() => null))
  );

  const results = details.filter(d => d && d.price && d.price.suggested != null);
  const failed = (batch.items ?? []).length - results.length;

  return {
    storeId,
    batchId: batch.id,
    storefrontUrl: `/shop/${storeId}`,
    total: batch.totalEstimatedValue ?? 0,
    failed,
    // How many photographs produced these listings. When it is fewer than the number of pieces,
    // a single photo was split into its separate garments - which is the thing that makes this a
    // way to empty a closet rather than a way to make one listing, and it is invisible unless the
    // results screen says so.
    photos: files.length,
    items: results.map(detail => toCard(detail, storeId)),
  };
}

/** Flattens an ItemDetailResponse into what the results UI renders. */
function toCard(detail, storeId) {
  const imageOf = kind => detail.images?.find(i => i.kind === kind && i.url)?.url ?? null;

  // Best available processed image, in the order the pipeline improves them.
  const processed =
    imageOf('ON_MODEL') ?? imageOf('STUDIO') ?? imageOf('ENHANCED') ??
    imageOf('RELIT') ?? imageOf('CUTOUT');

  return {
    itemId: detail.item.id,
    title: detail.listing?.title ?? detail.price?.heading ?? 'Untitled piece',
    description: detail.listing?.description ?? '',
    brand: detail.item.displayBrand,
    type: detail.item.identifiedType,
    condition: detail.item.condition,
    status: detail.item.status,

    original: imageOf('ORIGINAL'),
    processed,
    // Only a genuine before/after — if imaging fell back, the two are the same file and
    // claiming a transformation happened would be a lie.
    hasTransform: Boolean(processed && imageOf('ORIGINAL') && processed !== imageOf('ORIGINAL')),

    price: detail.price?.suggested ?? null,
    rangeLow: detail.price?.rangeLow ?? null,
    rangeHigh: detail.price?.rangeHigh ?? null,
    retailNew: detail.price?.retailNew ?? null,
    demand: detail.price?.demand ?? null,
    compCount: detail.price?.compCount ?? 0,
    warning: detail.price?.warning ?? null,
    comps: (detail.price?.comps ?? []).slice(0, 6),

    checkoutUrl: detail.listing?.checkoutUrl ?? null,
    listingUrl: detail.listing ? `/shop/${storeId}/${detail.item.id}` : null,
  };
}
