import { useEffect, useRef } from 'react';
import gsap from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';
import HeroShader from './HeroShader';

gsap.registerPlugin(ScrollTrigger);

/**
 * CinemaWheel — scroll-driven rotating photo field for the RACK hero.
 *
 * Now includes:
 * - WebGL shader canvas behind the tile scatter
 * - Differential parallax speeds between layers
 * - prefers-reduced-motion compliance
 */

const PHOTOS_POOL = [
  'https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=400&h=400&fit=crop',
  'https://images.unsplash.com/photo-1509631179647-0177331693ae?w=400&h=400&fit=crop',
  'https://images.unsplash.com/photo-1434389677669-e08b4cac3105?w=400&h=400&fit=crop',
  'https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?w=400&h=400&fit=crop',
  'https://images.unsplash.com/photo-1469334031218-e382a71b716b?w=400&h=400&fit=crop',
  'https://images.unsplash.com/photo-1525507119028-ed4c629a60a3?w=400&h=400&fit=crop',
  'https://images.unsplash.com/photo-1512436991641-6745cdb1723f?w=400&h=400&fit=crop',
  'https://images.unsplash.com/photo-1490481651871-ab68de25d43d?w=400&h=400&fit=crop',
  'https://images.unsplash.com/photo-1581044777550-4cfa60707c03?w=400&h=400&fit=crop',
  'https://images.unsplash.com/photo-1540221652346-e5dd6b50f3e7?w=400&h=400&fit=crop',
];

const ASPECT_RATIOS = [
  { w: 120, h: 120 },
  { w: 100, h: 140 },
  { w: 140, h: 100 },
];

function pseudoRandom(seed) {
  let value = seed;
  return function() {
    value = (value * 9301 + 49297) % 233280;
    return value / 233280;
  }
}
const prng = pseudoRandom(12345);

// Tuned parallax: background slower, foreground faster
const LAYERS_CONFIG = [
  { id: 1, count: 32, radiusMin: 400, radiusMax: 1200, scaleBase: 0.45, speed: 28,  dir: 1 },  // slower bg
  { id: 2, count: 24, radiusMin: 300, radiusMax: 1000, scaleBase: 0.75, speed: 70,  dir: -1 }, // mid
  { id: 3, count: 18, radiusMin: 250, radiusMax: 800,  scaleBase: 1.15, speed: 130, dir: 1 },  // faster fg
];

const GENERATED_LAYERS = LAYERS_CONFIG.map(layer => {
  const items = [];
  for (let i = 0; i < layer.count; i++) {
    const angle = (i / layer.count) * 360 + (prng() * 60 - 30);
    const radius = layer.radiusMin + prng() * (layer.radiusMax - layer.radiusMin);
    const cx = Math.cos((angle * Math.PI) / 180) * radius;
    const cy = Math.sin((angle * Math.PI) / 180) * radius;
    const scale = layer.scaleBase * (0.85 + prng() * 0.3);
    const rotation = prng() * 360;
    const src = PHOTOS_POOL[Math.floor(prng() * PHOTOS_POOL.length)];
    const aspect = ASPECT_RATIOS[Math.floor(prng() * ASPECT_RATIOS.length)];
    items.push({ initialAngle: angle, cx, cy, baseScale: scale, rotation, src, w: aspect.w, h: aspect.h, id: `${layer.id}-${i}` });
  }
  return { ...layer, items };
});

const SCROLL_HEIGHT = 1000;

export default function CinemaWheel({ children }) {
  const sectionRef = useRef(null);
  const stickyRef = useRef(null);
  const layerRefs = useRef([]);
  const itemRefs = useRef({});

  const prefersReducedMotion =
    typeof window !== 'undefined' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  useEffect(() => {
    const section = sectionRef.current;
    if (!section || prefersReducedMotion) return;

    const tl = gsap.timeline({
      scrollTrigger: {
        trigger: section,
        start: 'top top',
        end: `+=${SCROLL_HEIGHT}`,
        scrub: 1.2,
        pin: stickyRef.current,
        pinSpacing: false,
        anticipatePin: 1,
      },
    });

    GENERATED_LAYERS.forEach((layer, idx) => {
      if (layerRefs.current[idx]) {
        const state = {
          scrollRot: 0,
          introRot: layer.speed * layer.dir * 0.8 // start offset for the intro spin
        };

        const applyRot = () => {
          const currentRot = state.scrollRot + state.introRot;
          gsap.set(layerRefs.current[idx], { rotation: currentRot });

          layer.items.forEach((item) => {
            const el = itemRefs.current[item.id];
            if (!el) return;
            let globalAngle = (item.initialAngle + currentRot) % 360;
            if (globalAngle < 0) globalAngle += 360;
            const rad = (globalAngle * Math.PI) / 180;
            const yPos = Math.sin(rad);
            const depth = (yPos + 1) / 2;
            const targetScale = item.baseScale * (0.8 + 0.2 * depth);
            const targetBlur = 6 * (1 - depth);
            const targetOpacity = 0.4 + 0.6 * depth;
            el.style.transform = `scale(${targetScale}) rotate(${item.rotation}deg)`;
            el.style.filter = `blur(${targetBlur}px)`;
            el.style.opacity = targetOpacity;
          });
        };

        // Intro spin animation
        gsap.to(state, {
          introRot: 0,
          duration: 3,
          ease: 'power3.out',
          onUpdate: applyRot
        });

        // Scroll animation
        tl.to(state, {
          scrollRot: layer.speed * layer.dir,
          ease: 'none',
          onUpdate: applyRot
        }, 0);
      }
    });

    return () => {
      ScrollTrigger.getAll().forEach(st => st.kill());
    };
  }, [prefersReducedMotion]);

  return (
    <div ref={sectionRef} style={{ position: 'relative', height: `calc(100vh + ${SCROLL_HEIGHT}px)` }}>
      <div
        ref={stickyRef}
        style={{
          position: 'sticky',
          top: 0,
          width: '100%',
          height: '100vh',
          overflow: 'hidden',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          backgroundColor: 'var(--color-cream)',
        }}
      >
        {/* WebGL fluid shader background */}
        <HeroShader />

        {/* The deep immersive photo field */}
        <div style={{ position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1 }}>
          {GENERATED_LAYERS.map((layer, idx) => (
            <div
              key={layer.id}
              ref={el => { layerRefs.current[idx] = el; }}
              style={{
                position: 'absolute',
                width: 0, height: 0,
                willChange: 'transform',
              }}
            >
              {layer.items.map((item) => (
                <div
                  key={item.id}
                  ref={el => { itemRefs.current[item.id] = el; }}
                  style={{
                    position: 'absolute',
                    width: item.w,
                    height: item.h,
                    left: item.cx - item.w / 2,
                    top: item.cy - item.h / 2,
                    transformOrigin: 'center center',
                    borderRadius: '16px',
                    overflow: 'hidden',
                    boxShadow: '0 12px 40px rgba(0,0,0,0.15)',
                    willChange: 'transform, filter, opacity',
                  }}
                >
                  <img
                    src={item.src}
                    alt=""
                    loading="lazy"
                    style={{
                      width: '100%',
                      height: '100%',
                      objectFit: 'cover',
                      display: 'block',
                      pointerEvents: 'none',
                      filter: 'grayscale(100%) sepia(100%) hue-rotate(305deg) saturate(120%) brightness(0.85) contrast(1.1)',
                      mixBlendMode: 'multiply',
                    }}
                  />
                  <div style={{ position: 'absolute', inset: 0, backgroundColor: 'rgba(59, 34, 40, 0.15)', pointerEvents: 'none' }} />
                </div>
              ))}
            </div>
          ))}
        </div>

        {/* Masking Vignette */}
        <div
          style={{
            position: 'absolute',
            inset: 0,
            pointerEvents: 'none',
            zIndex: 5,
            background: `
              radial-gradient(circle at 50% 50%, var(--color-cream) 18%, transparent 45%),
              linear-gradient(to bottom, var(--color-cream) 1%, transparent 15%, transparent 70%, var(--color-cream) 98%)
            `,
          }}
        />

        {/* Center content */}
        <div
          style={{
            position: 'relative',
            zIndex: 10,
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            textAlign: 'center',
            padding: '0 24px',
            maxWidth: '640px',
          }}
        >
          {children}
        </div>

        {/* Scroll cue */}
        <div
          style={{
            position: 'absolute',
            bottom: '40px',
            left: '50%',
            transform: 'translateX(-50%)',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            gap: '8px',
            opacity: 0.38,
            zIndex: 10,
          }}
        >
          <span style={{ fontSize: '11px', fontFamily: 'Manrope, sans-serif', fontWeight: 700, letterSpacing: '0.12em', textTransform: 'uppercase', color: 'var(--color-accent)' }}>
            Scroll
          </span>
          <svg width="16" height="24" viewBox="0 0 16 24" fill="none">
            <rect x="6.5" y="0" width="3" height="10" rx="1.5" fill="var(--color-accent)" style={{ animation: 'scrollDot 1.8s ease-in-out infinite' }} />
            <rect x="0" y="0" width="16" height="24" rx="8" stroke="var(--color-accent)" strokeWidth="1.5" fill="none" />
          </svg>
        </div>
      </div>
    </div>
  );
}
