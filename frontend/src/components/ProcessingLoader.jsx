import { useEffect, useRef } from 'react';
import gsap from 'gsap';

const MESSAGES = [
  'Analyzing garment…',
  'Searching live listings…',
  'Pricing against market data…',
  'Generating try-on image…',
  'Almost there…',
];

// 8-dot pulsing grid — matches the Navbar DotLogo mark
function DotGrid() {
  const dotRefs = useRef([]);

  const positions = [
    [0,0],[1,0],[2,0],
    [0,1],      [2,1],
    [0,2],[1,2],[2,2],
  ];

  useEffect(() => {
    const dots = dotRefs.current.filter(Boolean);
    dots.forEach((dot, i) => {
      gsap.to(dot, {
        opacity: 0.12,
        scale: 0.5,
        duration: 0.65,
        repeat: -1,
        yoyo: true,
        ease: 'sine.inOut',
        delay: i * 0.1,
      });
    });
    return () => dots.forEach(dot => gsap.killTweensOf(dot));
  }, []);

  return (
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 10px)', gap: '10px', marginBottom: '40px' }}>
      {positions.map(([cx, cy], i) => (
        <div
          key={i}
          ref={el => { dotRefs.current[i] = el; }}
          style={{
            width: 10,
            height: 10,
            borderRadius: '50%',
            background: '#0d0d0d',   // ink — no plum
            gridColumnStart: cx + 1,
            gridRowStart:    cy + 1,
          }}
        />
      ))}
    </div>
  );
}

export default function ProcessingLoader() {
  const textRef     = useRef(null);
  const progressRef = useRef(null);
  const wrapRef     = useRef(null);

  useEffect(() => {
    gsap.fromTo(wrapRef.current, { opacity: 0 }, { opacity: 1, duration: 0.4 });

    let idx = 0;
    const interval = setInterval(() => {
      idx = (idx + 1) % MESSAGES.length;
      gsap.to(textRef.current, {
        opacity: 0, y: -8, duration: 0.2,
        onComplete: () => {
          if (textRef.current) {
            textRef.current.textContent = MESSAGES[idx];
            gsap.fromTo(textRef.current, { y: 8, opacity: 0 }, { y: 0, opacity: 1, duration: 0.2 });
          }
        },
      });
    }, 2200);

    // Progress bar — ink fill, no gradient
    gsap.to(progressRef.current, { width: '100%', duration: 11, ease: 'power1.inOut' });

    return () => clearInterval(interval);
  }, []);

  return (
    <div
      ref={wrapRef}
      className="flex-1 flex flex-col items-center justify-center"
      style={{ minHeight: '80vh', backgroundColor: '#f7f5f3', padding: '120px 24px 80px' }}
    >
      <DotGrid />

      <p
        ref={textRef}
        style={{
          fontSize: '28px',
          fontFamily: 'Cormorant Garamond, serif',
          fontWeight: 350,
          fontStyle: 'italic',
          lineHeight: 1.2,
          letterSpacing: '-0.03em',
          color: '#0d0d0d',
          marginBottom: '32px',
          textAlign: 'center',
          minHeight: '34px',
        }}
      >
        {MESSAGES[0]}
      </p>

      {/* Progress bar — solid ink, no gradient */}
      <div style={{ width: '200px', height: '1.5px', background: 'rgba(13,13,13,0.10)', borderRadius: '2px', overflow: 'hidden' }}>
        <div
          ref={progressRef}
          style={{ width: '0%', height: '100%', background: '#0d0d0d' }}
        />
      </div>
    </div>
  );
}
