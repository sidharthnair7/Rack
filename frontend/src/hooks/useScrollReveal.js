import { useEffect, useRef, useCallback } from 'react';

/**
 * useScrollReveal — IntersectionObserver-based reveal animation.
 *
 * Returns a ref callback. Attach it to elements (or use data-reveal on children).
 * When each element enters the viewport it fades up.
 *
 * Options:
 *   threshold  — IO threshold (default 0.15)
 *   stagger    — delay between children in ms (default 80)
 *   distance   — translateY start in px (default 32)
 *   once       — only animate once (default true)
 */
export default function useScrollReveal({
  threshold = 0.15,
  stagger = 80,
  distance = 32,
  once = true,
} = {}) {
  const elementsRef = useRef(new Set());
  const observerRef = useRef(null);

  // Check reduced motion
  const prefersReducedMotion =
    typeof window !== 'undefined' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  const refCallback = useCallback((el) => {
    if (!el) return;
    elementsRef.current.add(el);

    // Set initial hidden state (unless reduced motion)
    if (!prefersReducedMotion) {
      el.style.opacity = '0';
      el.style.transform = `translateY(${distance}px)`;
      el.style.transition = `opacity 0.7s var(--ease-out-expo), transform 0.7s var(--ease-out-expo)`;
      el.style.willChange = 'opacity, transform';
    }
  }, [prefersReducedMotion, distance]);

  useEffect(() => {
    if (prefersReducedMotion) return;

    const elements = elementsRef.current;

    observerRef.current = new IntersectionObserver(
      (entries) => {
        // Group entries that fired at the same time for stagger
        const revealing = entries.filter((e) => e.isIntersecting);
        revealing.forEach((entry, i) => {
          const el = entry.target;
          const delay = i * stagger;

          setTimeout(() => {
            el.style.opacity = '1';
            el.style.transform = 'translateY(0)';
          }, delay);

          if (once) {
            observerRef.current?.unobserve(el);
          }
        });
      },
      { threshold }
    );

    elements.forEach((el) => observerRef.current.observe(el));

    return () => {
      observerRef.current?.disconnect();
    };
  }, [threshold, stagger, once, prefersReducedMotion]);

  return refCallback;
}
