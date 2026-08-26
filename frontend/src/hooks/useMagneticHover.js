import { useRef, useCallback, useEffect } from 'react';

/**
 * useMagneticHover — subtle pull-toward-cursor on hover.
 *
 * Returns { ref, handlers } to spread onto the element.
 * On mousemove inside the element, the element translates toward the
 * cursor by up to `strength` pixels. On leave it springs back.
 *
 * No-ops on:
 *   - prefers-reduced-motion
 *   - touch/coarse-pointer devices
 */
export default function useMagneticHover({ strength = 15 } = {}) {
  const ref = useRef(null);
  const rafRef = useRef(null);

  const isDisabled =
    typeof window !== 'undefined' &&
    (window.matchMedia('(prefers-reduced-motion: reduce)').matches ||
     window.matchMedia('(pointer: coarse)').matches);

  const onMouseMove = useCallback(
    (e) => {
      if (isDisabled || !ref.current) return;
      cancelAnimationFrame(rafRef.current);
      rafRef.current = requestAnimationFrame(() => {
        const el = ref.current;
        if (!el) return;
        const rect = el.getBoundingClientRect();
        const cx = rect.left + rect.width / 2;
        const cy = rect.top + rect.height / 2;
        const dx = ((e.clientX - cx) / (rect.width / 2)) * strength;
        const dy = ((e.clientY - cy) / (rect.height / 2)) * strength;
        el.style.transform = `translate(${dx}px, ${dy}px)`;
        el.style.transition = 'transform 0.2s var(--ease-out-expo)';
      });
    },
    [isDisabled, strength]
  );

  const onMouseLeave = useCallback(() => {
    if (isDisabled || !ref.current) return;
    cancelAnimationFrame(rafRef.current);
    ref.current.style.transform = 'translate(0, 0)';
    ref.current.style.transition = 'transform 0.5s var(--ease-spring)';
  }, [isDisabled]);

  useEffect(() => {
    return () => cancelAnimationFrame(rafRef.current);
  }, []);

  return {
    ref,
    handlers: isDisabled
      ? {}
      : { onMouseMove, onMouseLeave },
  };
}
