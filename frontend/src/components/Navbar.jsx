
const INK   = 'var(--color-ink)';
const CREAM = 'var(--color-cream)';
const MUTED = 'var(--color-muted)';
const HAIR  = 'var(--color-hairline)';

// Each label has to describe what the reader actually lands on.
//
// "Pricing data" scrolled to the competitor comparison, whose own heading reads "Why Rack" - the
// link promised data about pricing and delivered a feature table. "Examples" scrolled to the
// single line stating the shop's inventory total, which is a number, not examples. A nav that
// misdescribes its own page is a small thing that quietly tells a reader not to trust the rest of
// it, so: the comparison is named for what it is, and Examples now opens the real storefront,
// where the actual published listings are.
const NAV_LINKS = [
  { label: 'How it works', id: 'how-it-works' },
  { label: 'Why Rack', id: 'pricing-data' },
  { label: 'Examples', href: '/shop/1' },
];

export default function Navbar({ onStart, onLogoClick, onNavClick }) {
  // No magnetic hover on this button. The effect pulls an element up to 10px toward the
  // cursor, which is fine in open space but this button sits inside a pill with 20px of
  // padding, so the pull pushed it visibly toward the edge and read as a broken layout.
  // The cursor crosses the navbar constantly, so it misfired constantly.

  return (
    <div
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        zIndex: 100,
        display: 'flex',
        justifyContent: 'center',
        padding: '12px 16px',
      }}
    >
      <nav
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '8px',
          padding: '8px 20px',
          borderRadius: '9999px',
          background: 'rgba(247, 245, 243, 0.82)',
          border: `1px solid ${HAIR}`,
          backdropFilter: 'blur(20px)',
          WebkitBackdropFilter: 'blur(20px)',
          width: '100%',
          maxWidth: '860px',
        }}
      >
        {/* Logo */}
        <a
          href="#"
          onClick={(e) => {
            e.preventDefault();
            if (onLogoClick) onLogoClick();
          }}
          style={{ flexShrink: 0, display: 'flex', alignItems: 'center', gap: '8px', textDecoration: 'none' }}
        >
          <DotLogo color="var(--color-ink)" />
          <span
            style={{
              fontFamily: 'Cormorant Garamond, serif',
              fontStyle: 'italic',
              fontWeight: 400,
              fontSize: '16px',
              letterSpacing: '-0.02em',
              color: INK,
            }}
          >
            RACK
          </span>
        </a>

        {/* Inline Links */}
        <div style={{ display: 'flex', gap: '24px', marginLeft: '32px' }}>
          {NAV_LINKS.map(link => (
            <a
              key={link.id ?? link.href}
              href={link.href ?? `#${link.id}`}
              onClick={(e) => {
                // A link with an href goes where it says; only the in-page ones are intercepted.
                if (link.href) return;
                e.preventDefault();
                if (onNavClick) onNavClick(link.id);
              }}
              style={{
                color: MUTED,
                fontSize: '13px',
                fontFamily: 'Manrope, sans-serif',
                fontWeight: 500,
                textDecoration: 'none',
                transition: 'color 0.3s var(--ease-out-expo)'
              }}
              onMouseEnter={e => e.currentTarget.style.color = INK}
              onMouseLeave={e => e.currentTarget.style.color = MUTED}
            >
              {link.label}
            </a>
          ))}
        </div>

        {/* Spacer */}
        <div style={{ flex: 1 }} />

        {/* Single CTA. There is deliberately no Log in / Sign up here: Rack has no accounts,
            and a button that opens a form which does nothing reads as unfinished to anyone who
            clicks it. One control that actually starts the flow is both honest and clearer. */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexShrink: 0 }}>
          <button
            onClick={onStart}
            style={{
              padding: '7px 16px',
              background: INK,
              border: 'none',
              borderRadius: '9999px',
              fontSize: '13px',
              fontFamily: 'Manrope, sans-serif',
              fontWeight: 600,
              color: CREAM,
              cursor: 'pointer',
              transition: 'opacity 0.3s var(--ease-out-expo)',
            }}
            onMouseEnter={e => { e.currentTarget.style.opacity = '0.85'; }}
            onMouseLeave={e => { e.currentTarget.style.opacity = '1'; }}
          >
            Start listing
          </button>
        </div>
      </nav>
    </div>
  );
}

// 8-dot grid mark
function DotLogo({ color = 'var(--color-ink)' }) {
  const positions = [
    [0,0],[1,0],[2,0],
    [0,1],      [2,1],
    [0,2],[1,2],[2,2],
  ];
  const DOT = 4, GAP = 4;
  const total = DOT * 3 + GAP * 2;
  return (
    <svg width={total} height={total} viewBox={`0 0 ${total} ${total}`}>
      {positions.map(([cx, cy], i) => (
        <rect
          key={i}
          x={cx * (DOT + GAP)}
          y={cy * (DOT + GAP)}
          width={DOT}
          height={DOT}
          rx={1}
          fill={color}
        />
      ))}
    </svg>
  );
}
