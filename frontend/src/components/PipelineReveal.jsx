/**
 * PipelineReveal — Scroll-driven garment processing reveal.
 *
 * As the user scrolls, a single garment image progresses through 4 stages:
 * PHONE PHOTO → BACKGROUND REMOVED → STUDIO LIT → ON MODEL
 *
 * Pinned via GSAP ScrollTrigger. This is the site's signature moment.
 */

import { useEffect, useRef, useState } from 'react';
import gsap from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';

gsap.registerPlugin(ScrollTrigger);

const STAGES = [
  {
    id: 'phone',
    label: 'PHONE PHOTO',
    sublabel: 'Your camera roll snap',
    icon: '📱',
  },
  {
    id: 'removed',
    label: 'BACKGROUND REMOVED',
    sublabel: 'Clean isolation in seconds',
    icon: '✂️',
  },
  {
    id: 'studio',
    label: 'STUDIO LIT',
    sublabel: 'Professional product shot',
    icon: '💡',
  },
  {
    id: 'model',
    label: 'ON MODEL',
    sublabel: 'AI-generated editorial image',
    icon: '👤',
  },
];

// Rose-plum gradient cards as placeholders for each stage
const STAGE_GRADIENTS = [
  'linear-gradient(145deg, #8a6570 0%, #6b4a53 50%, #4a3038 100%)', // raw, phone-quality feel
  'linear-gradient(145deg, #c9a4b0 0%, #b89099 50%, #a47d88 100%)', // cleaner, isolated
  'linear-gradient(145deg, #d4b4bc 0%, #c9a4b0 30%, #e8d5db 100%)', // studio-lit brightness
  'linear-gradient(145deg, #e8d5db 0%, #d4b4bc 40%, #f0e4e8 100%)', // editorial/model warmth
];

const INK = 'var(--color-ink)';
const CREAM = 'var(--color-cream)';
const MUTED = 'var(--color-muted)';

export default function PipelineReveal() {
  const sectionRef = useRef(null);
  const pinRef = useRef(null);
  const stageRefs = useRef([]);
  const labelRef = useRef(null);
  const sublabelRef = useRef(null);
  const progressDotsRef = useRef([]);
  const [activeStage, setActiveStage] = useState(0);
  const lastStageRef = useRef(0);

  const prefersReducedMotion =
    typeof window !== 'undefined' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  useEffect(() => {
    const section = sectionRef.current;
    if (!section) return;

    const stages = stageRefs.current.filter(Boolean);
    const totalScrollDistance = 2400; // px of scroll travel

    // Set all stages to initial state
    stages.forEach((el, i) => {
      if (i === 0) {
        gsap.set(el, { opacity: 1, scale: 1, zIndex: 4 });
      } else {
        gsap.set(el, { opacity: 0, scale: 1.08, zIndex: i });
      }
    });

    if (prefersReducedMotion) return; // static, no scroll animation

    const tl = gsap.timeline({
      scrollTrigger: {
        trigger: section,
        start: 'top top',
        end: `+=${totalScrollDistance}`,
        scrub: 0.8,
        pin: pinRef.current,
        pinSpacing: true,
        anticipatePin: 1,
      },
    });

    // Each stage gets ~25% of the timeline
    stages.forEach((_, i) => {
      if (i === 0) return; // first stage starts visible

      const enterStart = (i / STAGES.length);
      const enterEnd = enterStart + (0.5 / STAGES.length);

      tl.fromTo(
        stages[i],
        { opacity: 0, scale: 1.06 },
        {
          opacity: 1,
          scale: 1,
          zIndex: 10 + i,
          ease: 'power2.out',
          duration: enterEnd - enterStart,
          onStart: () => {
            setActiveStage(i);
          },
        },
        enterStart
      );

      // Slightly push the previous stage back
      tl.to(
        stages[i - 1],
        {
          scale: 0.96,
          opacity: 0.3,
          duration: enterEnd - enterStart,
          ease: 'power2.in',
        },
        enterStart
      );
    });

    return () => {
      ScrollTrigger.getAll().forEach(st => st.kill());
    };
  }, [prefersReducedMotion]);


  return (
    <section
      ref={sectionRef}
      style={{
        position: 'relative',
        backgroundColor: 'transparent',
      }}
    >
      <div
        ref={pinRef}
        style={{
          width: '100%',
          minHeight: '100vh',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '80px 24px',
          position: 'relative',
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
          PHOTOGRAPH
        </p>

        {/* Heading */}
        <h2
          style={{
            fontSize: 'clamp(32px, 5vw, 56px)',
            fontFamily: 'Cormorant Garamond, serif',
            fontWeight: 400,
            fontStyle: 'italic',
            lineHeight: 1.08,
            letterSpacing: '-0.03em',
            color: INK,
            marginBottom: '64px',
            textAlign: 'center',
          }}
        >
          No studio.<br />No photographer.
        </h2>

        {/* Card stack container */}
        <div
          style={{
            position: 'relative',
            width: '100%',
            maxWidth: '480px',
            aspectRatio: '3/4',
            borderRadius: '20px',
            overflow: 'hidden',
          }}
        >
          {STAGES.map((stage, i) => (
            <div
              key={stage.id}
              ref={(el) => { stageRefs.current[i] = el; }}
              style={{
                position: 'absolute',
                inset: 0,
                borderRadius: '20px',
                background: STAGE_GRADIENTS[i],
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                willChange: 'transform, opacity',
              }}
            >
              {/* Inner glow overlay */}
              <div
                style={{
                  position: 'absolute',
                  inset: 0,
                  background: 'linear-gradient(160deg, rgba(255,255,255,0.08) 0%, transparent 60%)',
                  borderRadius: '20px',
                  pointerEvents: 'none',
                }}
              />
              {/* Stage icon */}
              <span style={{ fontSize: '64px', opacity: 0.6, filter: 'grayscale(0.5)' }}>
                {stage.icon}
              </span>
            </div>
          ))}
        </div>

        {/* Stage label (below card) */}
        <div style={{ marginTop: '40px', textAlign: 'center', minHeight: '60px' }}>
          <p
            ref={labelRef}
            style={{
              fontSize: '11px',
              fontFamily: 'Manrope, sans-serif',
              fontWeight: 700,
              letterSpacing: '0.12em',
              textTransform: 'uppercase',
              color: INK,
              opacity: 0.5,
              marginBottom: '8px',
              transition: 'all 0.4s var(--ease-out-expo)',
            }}
          >
            {STAGES[activeStage].label}
          </p>
          <p
            ref={sublabelRef}
            style={{
              fontSize: '16px',
              fontFamily: 'Cormorant Garamond, serif',
              fontStyle: 'italic',
              fontWeight: 400,
              color: INK,
              opacity: 0.7,
              transition: 'all 0.4s var(--ease-out-expo)',
            }}
          >
            {STAGES[activeStage].sublabel}
          </p>
        </div>

        {/* Progress dots (right side) */}
        <div
          style={{
            position: 'absolute',
            right: '40px',
            top: '50%',
            transform: 'translateY(-50%)',
            display: 'flex',
            flexDirection: 'column',
            gap: '12px',
            alignItems: 'center',
          }}
        >
          {STAGES.map((stage, i) => (
            <div
              key={stage.id}
              ref={(el) => { progressDotsRef.current[i] = el; }}
              style={{
                width: activeStage === i ? '8px' : '4px',
                height: activeStage === i ? '8px' : '4px',
                borderRadius: '50%',
                backgroundColor: INK,
                opacity: activeStage === i ? 0.9 : 0.2,
                transition: 'all 0.4s var(--ease-out-expo)',
              }}
            />
          ))}
        </div>
      </div>
    </section>
  );
}
