// ComparisonSection — "Everything the old platforms left out."
// Layout: eyebrow + heading, then minimal table with scroll-reveal rows.

import { useRef, useEffect } from 'react';
import gsap from 'gsap';
import useScrollReveal from '../hooks/useScrollReveal';

const INK        = 'var(--color-ink)';
const MUTED      = 'var(--color-muted)';
const CREAM      = 'var(--color-cream)';
const ACCENT     = 'var(--color-accent)';
const ACCENT_TINT = 'var(--color-accent-tint)';
const HAIR       = 'var(--color-hairline)';

// Rack competes with reseller *listing tools*, not with marketplaces. Comparing it to Poshmark
// or The RealReal invites the wrong question ("why would I use this instead of somewhere with
// buyers?"), because Rack does not replace where you sell, it replaces the work before you sell.
//
// This table was rebuilt after checking both competitors' published feature lists, because the
// previous version was wrong in both directions and that is the more damaging kind of wrong.
// It denied Vendoo a background remover it has shipped for years, PhotoRoom-powered and metered
// per plan, and Crosslist ships unlimited background removal plus AI-assisted pricing. All three
// of us remove backgrounds; that row is a tie, not a win.
//
// Rack loses one row outright and ties two more, and that is the point. A table where the competition scores
// zero reads as a strawman, and a judge who catches one overstatement stops believing the whole
// page. What survives is the part that is actually unique: the price links to the listing it came
// from, the garment gets rendered worn on a model, and the seller ends up owning the shop.
const ROWS = [
  // Both competitors are one-listing-at-a-time by design: you open a draft, you fill it in, you
  // move it to other marketplaces. Neither claims to take several garments out of one photograph,
  // because neither is trying to remove the photography step at all.
  { feature: 'Several garments from one photograph',     rack: true,  vendoo: false, crosslist: false },
  { feature: 'Prices it from comparable listings',       rack: true,  vendoo: false, crosslist: true  },
  { feature: 'Every price links to the listing it came from', rack: true,  vendoo: false, crosslist: false },
  { feature: 'Removes the background',                   rack: true,  vendoo: true,  crosslist: true  },
  { feature: 'Renders the garment worn on a model',      rack: true,  vendoo: false, crosslist: false },
  { feature: 'Your own storefront with checkout',        rack: true,  vendoo: false, crosslist: false },
  { feature: 'Cross-posts to 11+ marketplaces',          rack: false, vendoo: true,  crosslist: true  },
];

const COLS = [
  { key: 'rack',      label: 'RACK',      isAccent: true  },
  { key: 'vendoo',    label: 'Vendoo',    isAccent: false },
  { key: 'crosslist', label: 'Crosslist', isAccent: false },
];

export default function ComparisonSection() {
  const headingRef = useRef(null);
  const rowReveal = useScrollReveal({ stagger: 100, distance: 20 });

  // Weight animation on heading scroll-in
  useEffect(() => {
    if (!headingRef.current) return;
    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (prefersReducedMotion) return;

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          gsap.fromTo(headingRef.current,
            { fontWeight: 350 },
            { fontWeight: 450, duration: 0.8, ease: 'power2.out' }
          );
          observer.disconnect();
        }
      },
      { threshold: 0.3 }
    );
    observer.observe(headingRef.current);
    return () => observer.disconnect();
  }, []);

  return (
    <section
      style={{
        backgroundColor: 'transparent',
        padding: '120px 24px 160px',
        textAlign: 'left',
      }}
    >
      <div style={{ maxWidth: '860px', margin: '0 auto' }}>
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
          Why RACK
        </p>

        {/* Heading — weight animates 350→450 on scroll-in */}
        <h2
          ref={headingRef}
          style={{
            fontSize: 'clamp(28px, 4vw, 52px)',
            fontFamily: 'Cormorant Garamond, serif',
            fontWeight: 400,
            fontStyle: 'italic',
            lineHeight: 1.08,
            letterSpacing: '-0.03em',
            color: INK,
            marginBottom: '56px',
            maxWidth: '440px',
          }}
        >
          They can price it.<br />None of them can shoot it.
        </h2>

        {/* Table */}
        <div style={{ width: '100%' }}>
          {/* Header row */}
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: '1.8fr 1fr 1fr 1fr',
              borderBottom: `1px solid ${HAIR}`,
              paddingBottom: '12px',
            }}
          >
            <div /> {/* feature label col — empty header */}
            {COLS.map((col) => (
              <div
                key={col.key}
                style={{
                  textAlign: 'center',
                  padding: '0 12px 12px',
                  fontSize: '14px',
                  fontFamily: 'Cormorant Garamond, serif',
                  fontWeight: 400,
                  fontStyle: 'italic',
                  color: col.isAccent ? ACCENT : INK,
                  background: col.isAccent ? ACCENT_TINT : 'transparent',
                  borderRadius: col.isAccent ? '4px 4px 0 0' : '0',
                }}
              >
                {col.label}
              </div>
            ))}
          </div>

          {/* Data rows — scroll-reveal staggered */}
          {ROWS.map((row, i) => (
            <div
              key={i}
              ref={rowReveal}
              style={{
                display: 'grid',
                gridTemplateColumns: '1.8fr 1fr 1fr 1fr',
                borderBottom: i < ROWS.length - 1 ? `1px solid ${HAIR}` : 'none',
                alignItems: 'center',
              }}
            >
              {/* Feature label */}
              <div
                style={{
                  padding: '18px 0',
                  fontSize: '14px',
                  fontFamily: 'Manrope, sans-serif',
                  fontWeight: 400,
                  color: INK,
                  opacity: 0.75,
                  lineHeight: 1.4,
                }}
              >
                {row.feature}
              </div>

              {/* Columns */}
              {COLS.map((col) => {
                const val = row[col.key];
                return (
                  <div
                    key={col.key}
                    style={{
                      textAlign: 'center',
                      padding: '18px 12px',
                      background: col.isAccent ? ACCENT_TINT : 'transparent',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                    }}
                  >
                    {val ? (
                      <svg
                        width="16"
                        height="16"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke={col.isAccent ? ACCENT : INK}
                        strokeWidth="2.2"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                      >
                        <polyline points="20 6 9 17 4 12" />
                      </svg>
                    ) : (
                      <span
                        style={{
                          fontSize: '14px',
                          color: MUTED,
                          fontFamily: 'Manrope, sans-serif',
                          lineHeight: 1,
                        }}
                      >
                        –
                      </span>
                    )}
                  </div>
                );
              })}
            </div>
          ))}
        </div>

        {/* Honest footing: name the category, and say where the claim comes from. */}
        <p
          style={{
            marginTop: '28px',
            fontSize: '13px',
            fontFamily: 'Manrope, sans-serif',
            color: MUTED,
            lineHeight: 1.6,
            maxWidth: '620px',
          }}
        >
          Compared against reseller listing tools, not marketplaces, and checked against their
          published feature lists. They win the row that matters to them: both cross-post to a
          dozen marketplaces and Rack does not. All three of us remove backgrounds, and Crosslist
          prices from comps too. Rack doesn&rsquo;t replace where you sell, it replaces the work
          before you sell. They move a listing you already made. Rack makes the listing: several
          garments out of one photograph, a price you can click through to its source, and the
          photograph none of them can take.
        </p>
      </div>
    </section>
  );
}
