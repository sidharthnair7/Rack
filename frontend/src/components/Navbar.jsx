import useMagneticHover from '../hooks/useMagneticHover';

const INK   = 'var(--color-ink)';
const CREAM = 'var(--color-cream)';
const MUTED = 'var(--color-muted)';
const HAIR  = 'var(--color-hairline)';

const NAV_LINKS = [
  { label: 'How it works', id: 'how-it-works' },
  { label: 'Pricing data', id: 'pricing-data' },
  { label: 'Examples', id: 'examples' },
];

export default function Navbar({ onSignIn, onSignUp, onLogoClick, onNavClick }) {
  const ctaMagnetic = useMagneticHover({ strength: 10 });

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
          background: 'rgba(34, 22, 24, 0.85)',
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
              key={link.id}
              href={`#${link.id}`}
              onClick={(e) => {
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

        {/* Auth buttons */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexShrink: 0 }}>
          <button
            onClick={onSignIn}
            style={{
              padding: '7px 16px',
              background: 'transparent',
              border: `1px solid ${HAIR}`,
              borderRadius: '9999px',
              fontSize: '13px',
              fontFamily: 'Manrope, sans-serif',
              fontWeight: 500,
              color: MUTED,
              cursor: 'pointer',
              transition: 'all 0.3s var(--ease-out-expo)',
            }}
            onMouseEnter={e => {
              e.currentTarget.style.borderColor = INK;
              e.currentTarget.style.color = INK;
            }}
            onMouseLeave={e => {
              e.currentTarget.style.borderColor = HAIR;
              e.currentTarget.style.color = MUTED;
            }}
          >
            Log in
          </button>
          <button
            ref={ctaMagnetic.ref}
            {...ctaMagnetic.handlers}
            onClick={onSignUp}
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
            onMouseEnter={e => {
              e.currentTarget.style.opacity = '0.85';
            }}
            onMouseLeave={e => {
              e.currentTarget.style.opacity = '1';
            }}
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
