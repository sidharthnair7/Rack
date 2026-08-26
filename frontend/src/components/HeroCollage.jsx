// Scattered polaroid image tiles around the hero headline
// Images are fashion/clothing from Unsplash (free to use, no auth needed)
const TILES = [
  {
    src: 'https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=200&h=260&fit=crop',
    alt: 'Fashion item',
    style: { top: '8%', left: '4%', width: 120, height: 155, rotate: '-4deg' },
  },
  {
    src: 'https://images.unsplash.com/photo-1512436991641-6745cdb1723f?w=180&h=220&fit=crop',
    alt: 'Clothing piece',
    style: { top: '40%', left: '2%', width: 100, height: 130, rotate: '3deg' },
  },
  {
    src: 'https://images.unsplash.com/photo-1434389677669-e08b4cac3105?w=200&h=250&fit=crop',
    alt: 'Garment',
    style: { bottom: '12%', left: '8%', width: 110, height: 140, rotate: '-2deg' },
  },
  {
    src: 'https://images.unsplash.com/photo-1490481651871-ab68de25d43d?w=200&h=250&fit=crop',
    alt: 'Style item',
    style: { top: '5%', right: '6%', width: 130, height: 165, rotate: '5deg' },
  },
  {
    src: 'https://images.unsplash.com/photo-1469334031218-e382a71b716b?w=180&h=220&fit=crop',
    alt: 'Outfit',
    style: { top: '45%', right: '2%', width: 105, height: 130, rotate: '-3deg' },
  },
  {
    src: 'https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?w=180&h=240&fit=crop',
    alt: 'Fashion piece',
    style: { bottom: '8%', right: '8%', width: 115, height: 145, rotate: '2deg' },
  },
  {
    src: 'https://images.unsplash.com/photo-1581044777550-4cfa60707c03?w=160&h=200&fit=crop',
    alt: 'Wardrobe item',
    style: { top: '20%', left: '18%', width: 88, height: 112, rotate: '-6deg' },
  },
  {
    src: 'https://images.unsplash.com/photo-1525507119028-ed4c629a60a3?w=160&h=200&fit=crop',
    alt: 'Closet piece',
    style: { bottom: '20%', right: '20%', width: 90, height: 115, rotate: '4deg' },
  },
];

export default function HeroCollage({ children }) {
  return (
    <div className="relative w-full min-h-screen flex items-center justify-center overflow-hidden">
      {/* Scattered tiles */}
      {TILES.map((tile, i) => (
        <div
          key={i}
          className="absolute pointer-events-none"
          style={{
            ...tile.style,
            transform: `rotate(${tile.style.rotate})`,
            zIndex: 0,
          }}
        >
          <img
            src={tile.src}
            alt={tile.alt}
            style={{
              width: tile.style.width,
              height: tile.style.height,
              borderRadius: '12px',
              objectFit: 'cover',
              display: 'block',
            }}
            loading="lazy"
          />
        </div>
      ))}

      {/* Center content */}
      <div className="relative z-10 flex flex-col items-center text-center px-4 max-w-2xl">
        {children}
      </div>
    </div>
  );
}
