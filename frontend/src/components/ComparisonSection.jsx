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
// buyers?"), because Rack does not replace where you sell — it replaces the two hours of work
// before you sell. Vendoo is the most widely used crosslister; Crosslist is the closest on
// features and is the only one of the three that already does background removal.
//
// The Crosslist tick on background removal is deliberate and load-bearing: a table where the
// competition scores zero on everything reads as a strawman, and a judge who spots one
// overstatement stops believing the rest of the page.
const ROWS = [
  { feature: 'Identifies the garment from a photo',   rack: true, vendoo: false, crosslist: false },
  { feature: 'Prices from live comparable listings',  rack: true, vendoo: false, crosslist: false },
  { feature: 'Every price links to its source',       rack: true, vendoo: false, crosslist: false },
  { feature: 'Removes the background',                rack: true, vendoo: false, crosslist: true  },
  { feature: 'Renders it worn on a model',            rack: true, vendoo: false, crosslist: false },
  { feature: 'Your own storefront and checkout',      rack: true, vendoo: false, crosslist: false },
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
                        —
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
          Compared against reseller listing tools, not marketplaces — Rack doesn&rsquo;t replace where
          you sell, it replaces the two hours of work before you sell. The photograph is the part
          none of them touch, and it is the part that decides what you get offered.
        </p>
      </div>
    </section>
  );
}
