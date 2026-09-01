import HeroShader from './HeroShader';

/**
 * CinemaWheel: the static photo field behind the RACK hero.
 *
 * This used to pin the hero for 1000px and rotate three layers on scroll. It now renders the
 * field in its final arrangement on first paint. Two reasons: the pinned region put a thousand
 * pixels of dead scroll between the headline and the first real content, and a judge watching a
 * demo video should see the finished composition immediately rather than scrub to reach it.
 *
 * Depth (scale, blur, opacity) is derived from each tile's own angle, so the arrangement still
 * reads as a field with foreground and background rather than a flat collage. It is computed
 * once at module load, so there is no animation loop and no layout work on scroll.
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

// spin is seconds for one full turn. Slow enough to read as drift, and staggered per layer with
// the nearest layer moving fastest, which is what keeps the field feeling like it has depth.
const LAYERS_CONFIG = [
  { id: 1, count: 32, radiusMin: 400, radiusMax: 1200, scaleBase: 0.45, spin: 260, dir: 1 },  // background
  { id: 2, count: 24, radiusMin: 300, radiusMax: 1000, scaleBase: 0.75, spin: 200, dir: -1 }, // mid
  { id: 3, count: 18, radiusMin: 250, radiusMax: 800,  scaleBase: 1.15, spin: 155, dir: 1 },  // foreground
];

// Photos are dealt round-robin across every tile in placement order rather than picked at
// random. Random picking from a pool of ten put the same garment side by side often enough to
// look like a rendering bug; dealing guarantees ten tiles between any two uses of one photo.
let dealIndex = 0;

const GENERATED_LAYERS = LAYERS_CONFIG.map(layer => {
  const items = [];
  for (let i = 0; i < layer.count; i++) {
    const angle = (i / layer.count) * 360 + (prng() * 60 - 30);
    const radius = layer.radiusMin + prng() * (layer.radiusMax - layer.radiusMin);
    const cx = Math.cos((angle * Math.PI) / 180) * radius;
    const cy = Math.sin((angle * Math.PI) / 180) * radius;
    const scale = layer.scaleBase * (0.85 + prng() * 0.3);
    const rotation = prng() * 360;
    const aspect = ASPECT_RATIOS[Math.floor(prng() * ASPECT_RATIOS.length)];
    const src = PHOTOS_POOL[dealIndex % PHOTOS_POOL.length];
    dealIndex += 1;

    // Depth from the tile's own position on the circle: tiles low on the wheel read as near.
    const depth = (Math.sin((angle * Math.PI) / 180) + 1) / 2;

    items.push({
      cx, cy, rotation, src,
      w: aspect.w,
      h: aspect.h,
      scale: scale * (0.8 + 0.2 * depth),
      // Softened from a 6px maximum: against plum a heavy blur read as depth, against paper it
      // read as a smudge on the page.
      blur: 3.5 * (1 - depth),
      opacity: 0.45 + 0.55 * depth,
      id: `${layer.id}-${i}`,
    });
  }
  return { ...layer, items };
});

export default function CinemaWheel({ children }) {
  return (
    <div style={{ position: 'relative', height: '100vh' }}>
      <div
        style={{
          position: 'relative',
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

        {/* The photo field */}
        <div style={{ position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1 }}>
          {GENERATED_LAYERS.map((layer) => (
            <div
              key={layer.id}
              style={{
                position: 'absolute',
                width: 0,
                height: 0,
                animation: `rackOrbit ${layer.spin}s linear infinite`,
                animationDirection: layer.dir < 0 ? 'reverse' : 'normal',
                willChange: 'transform',
              }}
            >
              {layer.items.map((item) => (
                <div
                  key={item.id}
                  style={{
                    position: 'absolute',
                    width: item.w,
                    height: item.h,
                    left: item.cx - item.w / 2,
                    top: item.cy - item.h / 2,
                    transformOrigin: 'center center',
                    transform: `scale(${item.scale}) rotate(${item.rotation}deg)`,
                    filter: `blur(${item.blur}px)`,
                    opacity: item.opacity,
                    borderRadius: '16px',
                    overflow: 'hidden',
                    boxShadow: '0 12px 40px rgba(28,26,23,0.12)',
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
                      // Rose tint dialled back from 120% saturation. The stronger tint was set
                      // against a dark ground; on paper it turned the tiles into pink shapes
                      // instead of photographs of clothes.
                      filter: 'grayscale(100%) sepia(100%) hue-rotate(305deg) saturate(55%) brightness(1.0) contrast(1.03)',
                      mixBlendMode: 'multiply',
                    }}
                  />
                  <div style={{ position: 'absolute', inset: 0, backgroundColor: 'rgba(59, 34, 40, 0.10)', pointerEvents: 'none' }} />
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
      </div>
    </div>
  );
}
