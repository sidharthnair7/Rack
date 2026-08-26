export default function DarkTransition() {
  return (
    <section
      style={{
        position: 'relative',
        width: '100%',
        minHeight: '40vh',
        background: 'linear-gradient(160deg, #0A0A0F 0%, #3D1B33 60%, #5C2A4D 100%)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        overflow: 'hidden',
      }}
    >
      {/* Subtle grain texture via SVG noise — no extra assets */}
      <div
        style={{
          position: 'absolute',
          inset: 0,
          backgroundImage: `url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)' opacity='0.04'/%3E%3C/svg%3E")`,
          backgroundSize: '200px 200px',
          opacity: 0.6,
          pointerEvents: 'none',
        }}
      />

      {/* One breath of copy */}
      <div style={{ position: 'relative', zIndex: 10, textAlign: 'center', padding: '64px 24px' }}>
        <p
          style={{
            fontSize: 'clamp(28px, 4.5vw, 52px)',
            fontFamily: 'Instrument Serif, serif',
            fontWeight: 400,
            fontStyle: 'italic',
            lineHeight: 1.15,
            letterSpacing: '-0.03em',
            color: '#FAFAF8',
            opacity: 0.92,
            marginBottom: '14px',
          }}
        >
          Every piece has a story.
        </p>
        <p
          style={{
            fontSize: '14px',
            fontFamily: 'Manrope, sans-serif',
            fontWeight: 400,
            color: '#F4E1D2',
            opacity: 0.5,
            letterSpacing: '0.04em',
            textTransform: 'uppercase',
          }}
        >
          RACK reads it. Then prices it.
        </p>
      </div>
    </section>
  );
}
