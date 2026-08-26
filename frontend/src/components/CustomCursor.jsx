import { useEffect, useRef, useState } from 'react';

/**
 * CustomCursor — dot + ring cursor that changes state on hover.
 *
 * States: default, link (buttons/anchors), view (showcase tiles), drag (scrollable).
 * Completely hidden on touch devices and prefers-reduced-motion.
 */
export default function CustomCursor() {
  const dotRef = useRef(null);
  const ringRef = useRef(null);
  const labelRef = useRef(null);
  const mouse = useRef({ x: -100, y: -100 });
  const pos = useRef({ x: -100, y: -100 });
  const [hidden, setHidden] = useState(false);

  // Bail on touch / reduced motion
  const isDisabled =
    typeof window !== 'undefined' &&
    (window.matchMedia('(pointer: coarse)').matches ||
     window.matchMedia('(prefers-reduced-motion: reduce)').matches);

  useEffect(() => {
    if (isDisabled) return;

    // Hide default cursor
    document.body.style.cursor = 'none';

    const onMove = (e) => {
      mouse.current = { x: e.clientX, y: e.clientY };
    };

    const onLeave = () => setHidden(true);
    const onEnter = () => setHidden(false);

    window.addEventListener('mousemove', onMove, { passive: true });
    document.addEventListener('mouseleave', onLeave);
    document.addEventListener('mouseenter', onEnter);

    // Lerp loop
    let raf;
    const lerp = (a, b, t) => a + (b - a) * t;

    const tick = () => {
      pos.current.x = lerp(pos.current.x, mouse.current.x, 0.15);
      pos.current.y = lerp(pos.current.y, mouse.current.y, 0.15);

      if (dotRef.current) {
        dotRef.current.style.transform = `translate(${mouse.current.x}px, ${mouse.current.y}px) translate(-50%, -50%)`;
      }
      if (ringRef.current) {
        ringRef.current.style.transform = `translate(${pos.current.x}px, ${pos.current.y}px) translate(-50%, -50%) scale(${ringRef.current.dataset.scale || 1})`;
      }
      raf = requestAnimationFrame(tick);
    };
    raf = requestAnimationFrame(tick);

    // Hover detection
    const onOver = (e) => {
      const el = e.target.closest('a, button, [data-cursor]');
      if (!ringRef.current || !labelRef.current) return;

      if (!el) {
        ringRef.current.dataset.scale = '1';
        ringRef.current.style.width = '36px';
        ringRef.current.style.height = '36px';
        ringRef.current.style.borderColor = 'var(--color-ink)';
        ringRef.current.style.opacity = '0.4';
        labelRef.current.textContent = '';
        labelRef.current.style.opacity = '0';
        return;
      }

      const cursorType = el.dataset?.cursor;

      if (cursorType === 'view') {
        ringRef.current.dataset.scale = '1';
        ringRef.current.style.width = '80px';
        ringRef.current.style.height = '80px';
        ringRef.current.style.borderColor = 'var(--color-ink)';
        ringRef.current.style.opacity = '0.9';
        ringRef.current.style.background = 'rgba(247,245,243,0.08)';
        labelRef.current.textContent = 'View';
        labelRef.current.style.opacity = '1';
      } else if (cursorType === 'drag') {
        ringRef.current.dataset.scale = '1';
        ringRef.current.style.width = '64px';
        ringRef.current.style.height = '64px';
        ringRef.current.style.borderColor = 'var(--color-ink)';
        ringRef.current.style.opacity = '0.7';
        ringRef.current.style.background = 'transparent';
        labelRef.current.textContent = '⟷';
        labelRef.current.style.opacity = '1';
      } else {
        // link/button
        ringRef.current.dataset.scale = '1.4';
        ringRef.current.style.width = '52px';
        ringRef.current.style.height = '52px';
        ringRef.current.style.borderColor = 'var(--color-ink)';
        ringRef.current.style.opacity = '0.2';
        ringRef.current.style.background = 'transparent';
        labelRef.current.textContent = '';
        labelRef.current.style.opacity = '0';
      }
    };

    document.addEventListener('mouseover', onOver, { passive: true });

    return () => {
      cancelAnimationFrame(raf);
      window.removeEventListener('mousemove', onMove);
      document.removeEventListener('mouseleave', onLeave);
      document.removeEventListener('mouseenter', onEnter);
      document.removeEventListener('mouseover', onOver);
      document.body.style.cursor = '';
    };
  }, [isDisabled]);

  if (isDisabled) return null;

  const base = {
    position: 'fixed',
    top: 0,
    left: 0,
    pointerEvents: 'none',
    zIndex: 99999,
    willChange: 'transform',
  };

  return (
    <>
      {/* Dot */}
      <div
        ref={dotRef}
        style={{
          ...base,
          width: '6px',
          height: '6px',
          borderRadius: '50%',
          backgroundColor: 'var(--color-ink)',
          opacity: hidden ? 0 : 0.9,
          transition: 'opacity 0.3s var(--ease-out-expo), width 0.3s var(--ease-out-expo), height 0.3s var(--ease-out-expo)',
        }}
      />
      {/* Ring */}
      <div
        ref={ringRef}
        data-scale="1"
        style={{
          ...base,
          width: '36px',
          height: '36px',
          borderRadius: '50%',
          border: '1.5px solid var(--color-ink)',
          opacity: hidden ? 0 : 0.4,
          background: 'transparent',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          transition: 'opacity 0.3s var(--ease-out-expo), width 0.35s var(--ease-out-expo), height 0.35s var(--ease-out-expo), border-color 0.3s var(--ease-out-expo), background 0.3s var(--ease-out-expo)',
        }}
      >
        <span
          ref={labelRef}
          style={{
            fontSize: '11px',
            fontFamily: 'Manrope, sans-serif',
            fontWeight: 600,
            letterSpacing: '0.06em',
            color: 'var(--color-ink)',
            opacity: 0,
            transition: 'opacity 0.2s var(--ease-out-expo)',
            textTransform: 'uppercase',
            userSelect: 'none',
          }}
        />
      </div>
    </>
  );
}
