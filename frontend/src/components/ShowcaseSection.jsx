// ShowcaseSection — "One photo becomes the whole listing."
// Layout: eyebrow + heading centred, 3-column × 2-row photo grid with badges,
//         then a "Flat-lay → Editorial model shot → Priced listing" breadcrumb.

const INK        = 'var(--color-ink)';
const MUTED      = 'var(--color-muted)';
const CREAM      = 'var(--color-cream)';
const ACCENT     = 'var(--color-accent)';
const ACCENT_TINT = 'var(--color-accent-tint)';

const GRADIENT = 'linear-gradient(135deg, #d4b4bc 0%, #c9a4b0 40%, #b89099 100%)';

export default function ShowcaseSection() {
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
        <PhotoCard badge="BEFORE" gridArea="1 / 1 / 3 / 2" minHeight="340px" />

        {/* Col 1, row 0 */}
        <PhotoCard badge="AI TRY-ON" gridArea="1 / 2 / 2 / 3" aspectRatio="16/9" />

        {/* Col 2, row 0 */}
        <PhotoCard badge="BEFORE" gridArea="1 / 3 / 2 / 4" aspectRatio="16/9" />

        {/* Col 1, row 1 */}
        <PhotoCard badge="AI TRY-ON" gridArea="2 / 2 / 3 / 3" aspectRatio="16/9" />

        {/* Col 2, row 1 */}
        <PhotoCard badge="AI TRY-ON" gridArea="2 / 3 / 3 / 4" aspectRatio="16/9" />

        {/* Col 0+1, row 2 — wide "before" card */}
        <PhotoCard badge="BEFORE" gridArea="3 / 1 / 4 / 3" minHeight="200px" />
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

function PhotoCard({ badge, gridArea, aspectRatio, minHeight }) {
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
