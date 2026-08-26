import { useRef } from 'react';

const MASONRY_ITEMS = [
  {
    src: 'https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=500&h=650&fit=crop',
    caption: 'Vintage Denim Jacket — est. $45',
    tag: 'before',
    height: 340,
  },
  {
    src: 'https://images.unsplash.com/photo-1509631179647-0177331693ae?w=500&h=700&fit=crop',
    caption: 'Silk Slip Dress — est. $120',
    tag: 'ai try-on',
    height: 420,
  },
  {
    src: 'https://images.unsplash.com/photo-1434389677669-e08b4cac3105?w=500&h=500&fit=crop',
    caption: 'Wool Blazer — est. $85',
    tag: 'before',
    height: 280,
  },
  {
    src: 'https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?w=500&h=700&fit=crop',
    caption: 'Floral Midi Dress — est. $68',
    tag: 'ai try-on',
    height: 400,
  },
  {
    src: 'https://images.unsplash.com/photo-1469334031218-e382a71b716b?w=500&h=600&fit=crop',
    caption: 'Cashmere Turtleneck — est. $95',
    tag: 'before',
    height: 320,
  },
  {
    src: 'https://images.unsplash.com/photo-1525507119028-ed4c629a60a3?w=500&h=650&fit=crop',
    caption: 'Leather Trench — est. $210',
    tag: 'ai try-on',
    height: 380,
  },
  {
    src: 'https://images.unsplash.com/photo-1512436991641-6745cdb1723f?w=500&h=550&fit=crop',
    caption: 'Striped Linen Shirt — est. $38',
    tag: 'before',
    height: 300,
  },
  {
    src: 'https://images.unsplash.com/photo-1490481651871-ab68de25d43d?w=500&h=700&fit=crop',
    caption: 'Vintage Cardi Set — est. $72',
    tag: 'ai try-on',
    height: 420,
  },
  {
    src: 'https://images.unsplash.com/photo-1581044777550-4cfa60707c03?w=500&h=560&fit=crop',
    caption: 'Wide-Leg Trousers — est. $55',
    tag: 'before',
    height: 310,
  },
];

function MasonryCard({ item }) {
  const overlayRef = useRef(null);
  const imgRef = useRef(null);

  const handleEnter = () => {
    if (overlayRef.current) overlayRef.current.style.opacity = '1';
    if (imgRef.current)    imgRef.current.style.transform = 'scale(1.04)';
  };
  const handleLeave = () => {
    if (overlayRef.current) overlayRef.current.style.opacity = '0';
    if (imgRef.current)    imgRef.current.style.transform = 'scale(1)';
  };

  return (
    <div
      className="masonry-item"
      onMouseEnter={handleEnter}
      onMouseLeave={handleLeave}
      style={{
        position: 'relative',
        borderRadius: '16px',
        overflow: 'hidden',
        cursor: 'pointer',
        height: item.height,
        // Warm off-white base so unloaded images don't flash black
        background: '#ede9e4',
      }}
    >
      <img
        ref={imgRef}
        src={item.src}
        alt={item.caption}
        loading="lazy"
        style={{
          width: '100%',
          height: '100%',
          objectFit: 'cover',
          display: 'block',
          transition: 'transform 400ms cubic-bezier(0.25, 0.46, 0.45, 0.94)',
          willChange: 'transform',
        }}
      />

      {/* Tag badge — consistent semi-opaque dark backing */}
      <div
        style={{
          position: 'absolute',
          top: '16px',
          left: '16px',
          padding: '6px 12px',
          borderRadius: '9999px',
          fontSize: '11px',
          fontWeight: 600,
          fontFamily: 'Manrope, sans-serif',
          letterSpacing: '0.08em',
          textTransform: 'uppercase',
          background: 'rgba(0,0,0,0.65)',
          color: '#ffffff',
          backdropFilter: 'blur(4px)',
          WebkitBackdropFilter: 'blur(4px)',
        }}
      >
        {item.tag}
      </div>

      {/* Hover caption overlay */}
      <div
        ref={overlayRef}
        style={{
          position: 'absolute',
          inset: 0,
          background: 'linear-gradient(to top, rgba(13,13,13,0.7) 0%, rgba(13,13,13,0.15) 50%, transparent 100%)',
          opacity: 0,
          transition: 'opacity 350ms ease',
          display: 'flex',
          alignItems: 'flex-end',
          padding: '20px',
        }}
      >
        <span
          style={{
            fontSize: '14px',
            fontFamily: 'Manrope, sans-serif',
            fontWeight: 500,
            color: '#f7f5f3',
            lineHeight: 1.4,
            letterSpacing: '0.01em',
          }}
        >
          {item.caption}
        </span>
      </div>
    </div>
  );
}

export default function MasonryGrid() {
  return (
    <section
      style={{
        width: '100%',
        maxWidth: '1280px',
        margin: '0 auto',
        padding: '0 24px 120px',
      }}
    >
      <div style={{ marginBottom: '48px' }}>
        <p
          style={{
            fontSize: '11px',
            fontFamily: 'Manrope, sans-serif',
            fontWeight: 600,
            letterSpacing: '0.12em',
            textTransform: 'uppercase',
            color: '#9a9796',
            marginBottom: '12px',
          }}
        >
          Showcase
        </p>
        <h2
          style={{
            fontSize: 'clamp(28px, 4vw, 42px)',
            fontFamily: 'Cormorant Garamond, serif',
            fontWeight: 350,
            fontStyle: 'italic',
            lineHeight: 1.1,
            letterSpacing: '-0.04em',
            color: '#0d0d0d',
            maxWidth: '400px',
          }}
        >
          Every piece,<br />priced and styled.
        </h2>
      </div>

      <div className="masonry-grid">
        {MASONRY_ITEMS.map((item, i) => (
          <MasonryCard key={i} item={item} />
        ))}
      </div>
    </section>
  );
}
