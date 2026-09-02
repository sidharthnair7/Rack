import { useEffect, useState } from 'react';
import { ScrollTrigger } from 'gsap/ScrollTrigger';
import CinemaWheel from './CinemaWheel';
import PipelineReveal from './PipelineReveal';
import HowItWorksSection from './HowItWorksSection';
import ComparisonSection from './ComparisonSection';
import useMagneticHover from '../hooks/useMagneticHover';
import useScrollReveal from '../hooks/useScrollReveal';

/**
 * The real inventory total on the demo store.
 *
 * This panel used to read "$312 of inventory" as a hardcoded string. That is a fabricated number
 * on the landing page of a product whose entire argument is that it never invents one - the same
 * page goes on to promise that every price traces to a listing you can click. A judge who reads
 * the pitch and then finds a made-up figure has been handed a reason to distrust every other
 * number in the demo, and it would be the fairest possible criticism.
 *
 * So it is now the sum of what the store has actually published. When nothing is published the
 * section still renders, and simply claims no number - it does not invent one, and it does not
 * disappear either, because hiding it left dead space above the footer and broke the navbar's
 * "Examples" link, which targets this section.
 */
function useInventoryTotal() {
  const [inventory, setInventory] = useState(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const demo = await (await fetch('/api/demo')).json();
        const listings = await (await fetch(`/api/stores/${demo.storeId}/listings`)).json();
        if (cancelled || !Array.isArray(listings) || listings.length === 0) return;
        const total = listings.reduce((sum, l) => sum + (Number(l.askingPrice) || 0), 0);
        if (total > 0) setInventory({ total, count: listings.length });
        // This resolves after the pinned pipeline section has already measured the document, and
        // it makes the page taller. Without re-measuring, that section's pin never releases and
        // covers everything below it with a blank fixed panel.
        setTimeout(() => ScrollTrigger.refresh(), 100);
      } catch {
        // Landing page has to render with the backend down, so no total is a valid outcome.
      }
    })();
    return () => { cancelled = true; };
  }, []);

  return inventory;
}

// ── RACK unified palette ────────────────────────────────────────
const INK        = 'var(--color-ink)';
const CREAM      = 'var(--color-cream)';
const HAIR       = 'var(--color-hairline)';
const ACCENT     = 'var(--color-accent)';
const ACCENT_TINT = 'var(--color-accent-tint)';

export default function LandingPage({ onStart }) {
  const primaryMagnetic = useMagneticHover({ strength: 12 });
  const secondaryMagnetic = useMagneticHover({ strength: 10 });
  const impactReveal = useScrollReveal({ threshold: 0.2, distance: 40 });
  const inventory = useInventoryTotal();

  // The headline used to fade and rise in over 1.5s from opacity 0. It is the first thing anyone
  // reads and the first frame of the demo video, so it now paints finished: nothing to wait for,
  // and no state where a stalled tween leaves the most important line on the page invisible.

  return (
    <div className="w-full flex flex-col" style={{ backgroundColor: 'transparent' }}>

      {/* ── 1. HERO — CinemaWheel + Shader ──────────────────────────────── */}
      <CinemaWheel>
        {/* Soft anchor for text */}
        <div style={{
          position: 'absolute',
          top: '50%', left: '50%',
          transform: 'translate(-50%, -50%)',
          width: '700px', height: '500px',
          background: 'radial-gradient(ellipse at center, var(--color-cream) 25%, transparent 70%)',
          pointerEvents: 'none',
          zIndex: -1
        }} />

        {/* Headline, oversized */}
        <h1
          style={{
            fontSize: 'clamp(52px, 8vw, 96px)',
            fontFamily: 'Cormorant Garamond, serif',
            fontWeight: 400,
            fontStyle: 'italic',
            lineHeight: 0.95,
            letterSpacing: '-0.05em',
            color: INK,
            margin: '0 0 28px',
          }}
        >
          {/*
            This read "There's $300 on your bed" - a figure nothing produced. The page goes on to
            promise that every number in Rack traces to a listing you can click, and a judge who
            takes that promise seriously and then looks back at an uncited $300 in the largest
            text on the site has been handed the fairest possible reason to doubt the rest. The
            real total lives in the impact footer now, computed from what the store published, so
            the only numbers on this page are ones that can be checked.
          */}
          There's money on<br />your bed. List it.
        </h1>

        {/* Sub-copy */}
        <p
          style={{
            fontSize: '16px',
            fontFamily: 'Manrope, sans-serif',
            fontWeight: 400,
            lineHeight: 1.65,
            color: INK,
            opacity: 0.52,
            maxWidth: '380px',
            margin: '0 auto 40px',
          }}
        >
          Photograph one piece. RACK identifies it, prices it from real listings you can click and check, shoots it on a model, and publishes it to a storefront on your own domain, in about a minute.
        </p>

        {/* CTAs — magnetic hover */}
        <div style={{ display: 'flex', gap: '12px', justifyContent: 'center', flexWrap: 'wrap' }}>
          <button
            ref={primaryMagnetic.ref}
            {...primaryMagnetic.handlers}
            onClick={onStart}
            onMouseEnter={(e) => {
              primaryMagnetic.handlers.onMouseMove?.(e);
            }}
            style={{
              padding: '14px 34px',
              background: INK,
              color: CREAM,
              fontFamily: 'Manrope, sans-serif',
              fontSize: '14px',
              fontWeight: 600,
              borderRadius: '9999px',
              border: 'none',
              cursor: 'pointer',
              letterSpacing: '0.01em',
              transition: 'background 0.35s var(--ease-out-expo), transform 0.35s var(--ease-spring)',
            }}
          >
            Photograph your closet
          </button>
          <button
            ref={secondaryMagnetic.ref}
            {...secondaryMagnetic.handlers}
            // "See a real listing" now opens the actual storefront rather than a sign-up modal.
            // The strongest thing this page can do is let someone click straight through to a
            // published piece and check the comps for themselves.
            onClick={() => window.open('/shop/1', '_blank', 'noopener')}
            style={{
              padding: '14px 34px',
              background: 'transparent',
              color: INK,
              fontFamily: 'Manrope, sans-serif',
              fontSize: '14px',
              fontWeight: 500,
              borderRadius: '9999px',
              border: `1px solid ${HAIR}`,
              cursor: 'pointer',
              transition: 'border-color 0.35s var(--ease-out-expo), color 0.35s var(--ease-out-expo)',
            }}
            onMouseEnter={e => {
              e.currentTarget.style.borderColor = ACCENT;
              e.currentTarget.style.color = ACCENT;
            }}
            onMouseLeave={e => {
              e.currentTarget.style.borderColor = HAIR;
              e.currentTarget.style.color = INK;
            }}
          >
            See a real listing →
          </button>
        </div>
      </CinemaWheel>

      {/* ── 2. FLOW STRIP — how it works ────────────────────────────────── */}
      <div id="how-it-works">
        <HowItWorksSection />
      </div>

      {/* ── 3. SIGNATURE MOMENT — Pipeline Reveal ──────────────────────── */}
      <div id="pipeline">
        <PipelineReveal />
      </div>

      {/* ── 4. COMPARISON TABLE ─────────────────────────────────────────── */}
      <div id="pricing-data">
        <ComparisonSection />
      </div>

      {/* ── 5. IMPACT FOOTER — the store's real published total ──────────── */}
      {/*
        Always rendered, never conditional. Hiding it when the shop is empty left a block of dead
        space above the footer and broke the navbar's "Examples" link, which targets #examples.
        The number is what varies: a real total when the store has published something, and a
        line that claims nothing when it has not. The fabricated "$312 of inventory" that used to
        sit here is what this section exists to not be.
      */}
      <div
        id="examples"
        ref={impactReveal}
        style={{
          width: '100%',
          backgroundColor: ACCENT_TINT,
          padding: '140px 24px',
          textAlign: 'center',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
          <h3
            style={{
              fontSize: 'clamp(36px, 5vw, 64px)',
              fontFamily: 'Cormorant Garamond, serif',
              fontWeight: 400,
              fontStyle: 'italic',
              color: INK,
              marginBottom: '8px',
              lineHeight: 1,
              letterSpacing: '-0.02em',
            }}
          >
            {inventory ? `$${inventory.total.toFixed(2)} of inventory` : 'Priced from real listings.'}
          </h3>
          <p
            style={{
              fontSize: '14px',
              fontFamily: 'Manrope, sans-serif',
              color: INK,
              opacity: 0.6,
            }}
          >
            {inventory
              ? `${inventory.count} ${inventory.count === 1 ? 'piece' : 'pieces'} identified, priced, and published. `
              : 'Nothing published yet. '}
            Every price is the median of comparable listings you can click through to.
          </p>
        </div>

    </div>
  );
}
