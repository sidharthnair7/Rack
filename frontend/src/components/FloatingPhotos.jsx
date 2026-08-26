// Cosmos.so-inspired floating photo field for the RACK hero
// Photos orbit gently around the centered headline using CSS animations only — no JS library

const PHOTOS = [
  // ── Left edge ──────────────────────────────────────────
  {
    src: 'https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=300&h=380&fit=crop',
    style: { left: '2%', top: '10%', width: 108, height: 136 },
    rotate: '-7deg', anim: 'float-a', dur: '9s', delay: '0s',
  },
  {
    src: 'https://images.unsplash.com/photo-1581044777550-4cfa60707c03?w=260&h=300&fit=crop',
    style: { left: '5%', top: '52%', width: 88, height: 104 },
    rotate: '4deg', anim: 'float-b', dur: '11s', delay: '-3s',
  },
  {
    src: 'https://images.unsplash.com/photo-1434389677669-e08b4cac3105?w=280&h=360&fit=crop',
    style: { left: '1%', top: '76%', width: 98, height: 124 },
    rotate: '-3deg', anim: 'float-c', dur: '13s', delay: '-5s',
  },
  {
    src: 'https://images.unsplash.com/photo-1512436991641-6745cdb1723f?w=280&h=340&fit=crop',
    style: { left: '18%', top: '6%', width: 90, height: 112 },
    rotate: '5deg', anim: 'float-d', dur: '10s', delay: '-1.5s',
  },
  {
    src: 'https://images.unsplash.com/photo-1469334031218-e382a71b716b?w=260&h=320&fit=crop',
    style: { left: '14%', top: '72%', width: 84, height: 106 },
    rotate: '-5deg', anim: 'float-e', dur: '12s', delay: '-4s',
  },

  // ── Right edge ─────────────────────────────────────────
  {
    src: 'https://images.unsplash.com/photo-1509631179647-0177331693ae?w=300&h=380&fit=crop',
    style: { right: '3%', top: '8%', width: 114, height: 144 },
    rotate: '6deg', anim: 'float-b', dur: '10s', delay: '-2s',
  },
  {
    src: 'https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?w=280&h=340&fit=crop',
    style: { right: '1%', top: '44%', width: 96, height: 120 },
    rotate: '-4deg', anim: 'float-a', dur: '14s', delay: '-6s',
  },
  {
    src: 'https://images.unsplash.com/photo-1490481651871-ab68de25d43d?w=260&h=320&fit=crop',
    style: { right: '6%', top: '74%', width: 88, height: 110 },
    rotate: '3deg', anim: 'float-c', dur: '11s', delay: '-2.5s',
  },
  {
    src: 'https://images.unsplash.com/photo-1525507119028-ed4c629a60a3?w=280&h=350&fit=crop',
    style: { right: '19%', top: '4%', width: 82, height: 102 },
    rotate: '-6deg', anim: 'float-d', dur: '9.5s', delay: '-7s',
  },
  {
    src: 'https://images.unsplash.com/photo-1540221652346-e5dd6b50f3e7?w=260&h=310&fit=crop',
    style: { right: '15%', top: '76%', width: 86, height: 108 },
    rotate: '5deg', anim: 'float-e', dur: '12.5s', delay: '-3.5s',
  },

  // ── Top/Bottom center ──────────────────────────────────
  {
    src: 'https://images.unsplash.com/photo-1558171813-c95e3d428791?w=260&h=300&fit=crop',
    style: { left: '38%', top: '3%', width: 76, height: 94 },
    rotate: '2deg', anim: 'float-b', dur: '8.5s', delay: '-1s',
  },
  {
    src: 'https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=260&h=320&fit=crop&crop=bottom',
    style: { left: '42%', bottom: '4%', width: 80, height: 100 },
    rotate: '-3deg', anim: 'float-a', dur: '10.5s', delay: '-4.5s',
  },
];

export default function FloatingPhotos({ children }) {
  return (
    <div style={{ position: 'relative', width: '100%', minHeight: '100vh', overflow: 'hidden' }}>
      {/* Floating image tiles */}
      {PHOTOS.map((photo, i) => {
        const baseTransform = `rotate(${photo.rotate})`;
        return (
          <div
            key={i}
            className="float-photo"
            style={{
              ...photo.style,
              borderRadius: '16px',
              '--base-transform': baseTransform,
              animation: `${photo.anim} ${photo.dur} ease-in-out ${photo.delay} infinite`,
              // Slight 3D card tilt — alternates direction per photo
              transform: `${baseTransform} perspective(600px) rotateY(${i % 2 === 0 ? '6deg' : '-6deg'}) rotateX(${i % 3 === 0 ? '4deg' : '-3deg'})`,
              opacity: 0.92,
              boxShadow: '0 8px 32px rgba(0,0,0,0.28)',
            }}
          >
            <img src={photo.src} alt="" loading="lazy" />
          </div>
        );
      })}

      {/* Center content slot */}
      <div
        style={{
          position: 'relative',
          zIndex: 10,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          minHeight: '100vh',
        }}
      >
        {children}
      </div>
    </div>
  );
}
