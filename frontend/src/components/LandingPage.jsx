import { useEffect, useRef } from 'react';
import gsap from 'gsap';
import CinemaWheel from './CinemaWheel';
import PipelineReveal from './PipelineReveal';
import HowItWorksSection from './HowItWorksSection';
import ComparisonSection from './ComparisonSection';
import useMagneticHover from '../hooks/useMagneticHover';
import useScrollReveal from '../hooks/useScrollReveal';

// ── RACK unified palette ────────────────────────────────────────
const INK        = 'var(--color-ink)';
const CREAM      = 'var(--color-cream)';
const HAIR       = 'var(--color-hairline)';
const ACCENT     = 'var(--color-accent)';
const ACCENT_TINT = 'var(--color-accent-tint)';

export default function LandingPage({ onStart, onSignUp }) {
  const heroHeadRef = useRef(null);
  const primaryMagnetic = useMagneticHover({ strength: 12 });
  const secondaryMagnetic = useMagneticHover({ strength: 10 });
  const impactReveal = useScrollReveal({ threshold: 0.2, distance: 40 });

  // Hero headline weight animation on mount
  useEffect(() => {
    if (!heroHeadRef.current) return;
    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (prefersReducedMotion) return;

    gsap.fromTo(heroHeadRef.current,
      { fontWeight: 300, opacity: 0, y: 20 },
      { fontWeight: 400, opacity: 1, y: 0, duration: 1.2, ease: 'power3.out', delay: 0.3 }
    );
  }, []);

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

        {/* Headline — oversized, variable weight */}
        <h1
          ref={heroHeadRef}
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
          There's $300 on<br />your bed. List it.
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
          Photograph the pile. RACK identifies each piece, prices it from real listings you can click and check, shoots it on a model, and publishes your storefront — in about four minutes.
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
            onClick={onSignUp}
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

      {/* ── 5. IMPACT FOOTER ──────────────────────────────────────────── */}
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
          $312 of inventory
        </h3>
        <p
          style={{
            fontSize: '14px',
            fontFamily: 'Manrope, sans-serif',
            color: INK,
            opacity: 0.6,
          }}
        >
          identified, priced, and listed from one photo of a bed.
        </p>
      </div>

    </div>
  );
}
