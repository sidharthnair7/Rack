// ShowcaseSection — "One photo becomes the whole listing."
// Layout: eyebrow + heading centred, 3-column × 2-row photo grid with badges,
//         then a "Flat-lay → Editorial model shot → Priced listing" breadcrumb.
//
// The cards were fixed gradient placeholders badged "AI TRY-ON", which meant the landing page
// promised a transformation and then showed empty pink boxes. They now pull real pairs from
// whatever the store has actually published, so the first genuine run fills this section in.
// The gradient remains as the fallback for a store with nothing listed yet — a placeholder is
// honest, a fabricated example would not be.

import { useEffect, useState } from 'react';

const INK        = 'var(--color-ink)';
const MUTED      = 'var(--color-muted)';
const CREAM      = 'var(--color-cream)';
const ACCENT     = 'var(--color-accent)';
const ACCENT_TINT = 'var(--color-accent-tint)';

const GRADIENT = 'linear-gradient(135deg, #d4b4bc 0%, #c9a4b0 40%, #b89099 100%)';

/** Real before/after pairs from published listings, newest first. Empty until a run completes. */
function useShowcasePairs(limit = 3) {
  const [pairs, setPairs] = useState([]);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const demo = await (await fetch('/api/demo')).json();
        const listings = await (await fetch(`/api/stores/${demo.storeId}/listings`)).json();
        const recent = listings.slice(-limit).reverse();

        const found = [];
        for (const listing of recent) {
          const images = await (await fetch(`/api/items/${listing.itemId}/images`)).json();
          const pick = kind => images.find(i => i.kind === kind && i.url)?.url ?? null;
          const before = pick('ORIGINAL');
          const after = pick('ON_MODEL') ?? pick('STUDIO') ?? pick('ENHANCED') ?? pick('CUTOUT');
          // Only a genuine transformation counts — if imaging fell back, both URLs are the same
          // file and showing it as a before/after would be a claim the pipeline did not earn.
          if (before && after && before !== after) found.push({ before, after, title: listing.title });
        }
        if (!cancelled) setPairs(found);
      } catch {
        if (!cancelled) setPairs([]);
      }
    })();
    return () => { cancelled = true; };
  }, [limit]);

  return pairs;
}

export default function ShowcaseSection() {
  const pairs = useShowcasePairs(3);
  const before = i => pairs[i]?.before ?? null;
  const after  = i => pairs[i]?.after ?? null;

  return (
    <section
      style={{
        backgroundColor: CREAM,
        padding: '120px 24px 80px',
        textAlign: 'center',
      }}
    >
      {/* Eyebrow */}
      <p
        style={{
          fontSize: '11px',
          fontFamily: 'Manrope, sans-serif',
          fontWeight: 600,
          letterSpacing: '0.14em',
          textTransform: 'uppercase',
          color: MUTED,
          marginBottom: '16px',
        }}
      >
        Showcase
      </p>

      {/* Heading */}
      <h2
        style={{
          fontSize: 'clamp(28px, 3.8vw, 48px)',
          fontFamily: 'Cormorant Garamond, serif',
          fontWeight: 400,
          fontStyle: 'italic',
          lineHeight: 1.1,
          letterSpacing: '-0.03em',
          color: INK,
          marginBottom: '56px',
        }}
      >
        One photo becomes<br />the whole listing.
      </h2>

      {/* Grid */}
      <div
        style={{
          maxWidth: '860px',
          margin: '0 auto',
          display: 'grid',
          gridTemplateColumns: '1fr 1fr 1fr',
          gridTemplateRows: 'auto auto auto',
          gap: '10px',
        }}
      >
        {/* Col 0, rows 0+1 — tall "before" card */}
        <PhotoCard badge="BEFORE" gridArea="1 / 1 / 3 / 2" minHeight="340px" src={before(0)} />

        {/* Col 1, row 0 */}
        <PhotoCard badge="AI TRY-ON" gridArea="1 / 2 / 2 / 3" aspectRatio="16/9" src={after(0)} />

        {/* Col 2, row 0 */}
        <PhotoCard badge="BEFORE" gridArea="1 / 3 / 2 / 4" aspectRatio="16/9" src={before(1)} />

        {/* Col 1, row 1 */}
        <PhotoCard badge="AI TRY-ON" gridArea="2 / 2 / 3 / 3" aspectRatio="16/9" src={after(1)} />

        {/* Col 2, row 1 */}
        <PhotoCard badge="AI TRY-ON" gridArea="2 / 3 / 3 / 4" aspectRatio="16/9" src={after(2)} />

        {/* Col 0+1, row 2 — wide "before" card */}
        <PhotoCard badge="BEFORE" gridArea="3 / 1 / 4 / 3" minHeight="200px" src={before(2)} />
      </div>

      {/* Breadcrumb */}
      <div
        style={{
          marginTop: '40px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          gap: '12px',
          fontFamily: 'Manrope, sans-serif',
          fontSize: '13px',
          color: INK,
          opacity: 0.6,
        }}
      >
        <span>Flat-lay</span>
        <Arrow />
        <span>Editorial model shot</span>
        <Arrow />
        <span>Priced listing</span>
      </div>
    </section>
  );
}

function PhotoCard({ badge, gridArea, aspectRatio, minHeight, src }) {
  const isBefore = badge === 'BEFORE';
  return (
    <div
      style={{
        gridArea,
        borderRadius: '12px',
        overflow: 'hidden',
        position: 'relative',
        background: GRADIENT,
        aspectRatio: aspectRatio || undefined,
        minHeight: minHeight || undefined,
      }}
    >
      {src && (
        <img
          src={src}
          alt={badge === 'BEFORE' ? 'Photo as taken' : 'Rendered on a model'}
          loading="lazy"
          style={{ position: 'absolute', inset: 0, width: '100%', height: '100%', objectFit: 'cover' }}
        />
      )}
      {/* Subtle inner gradient overlay for depth */}
      <div
        style={{
          position: 'absolute',
          inset: 0,
          background: 'linear-gradient(160deg, rgba(255,255,255,0.12) 0%, transparent 60%)',
          pointerEvents: 'none',
        }}
      />
      {/* Badge */}
      <span
        style={{
          position: 'absolute',
          top: '10px',
          left: '10px',
          padding: '4px 10px',
          borderRadius: '9999px',
          fontSize: '10px',
          fontFamily: 'Manrope, sans-serif',
          fontWeight: 700,
          letterSpacing: '0.08em',
          textTransform: 'uppercase',
          background: isBefore ? 'rgba(20,16,16,0.82)' : 'rgba(255,255,255,0.88)',
          color: isBefore ? '#fff' : MUTED,
          backdropFilter: 'blur(6px)',
          WebkitBackdropFilter: 'blur(6px)',
          boxShadow: '0 1px 4px rgba(0,0,0,0.08)',
        }}
      >
        {badge}
      </span>
    </div>
  );
}

function Arrow() {
  return (
    <svg width="18" height="8" viewBox="0 0 18 8" fill="none" xmlns="http://www.w3.org/2000/svg">
      <path d="M0 4H16M16 4L13 1M16 4L13 7" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}
