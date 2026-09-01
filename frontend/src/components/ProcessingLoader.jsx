import { useEffect, useRef } from 'react';
import gsap from 'gsap';

const INK = '#1c1a17';
const MUTED = '#6e6862';
const HAIR = 'rgba(28,26,23,0.12)';
const ACCENT = 'var(--color-accent)';
const SERIF = 'Cormorant Garamond, serif';
const SANS = 'Manrope, sans-serif';

// The four stages an item actually passes through, in order, keyed to ItemStatus on the backend.
// This used to be five invented messages on a 2.2s carousel, which meant the text kept changing
// while nothing about the state had changed - the one thing a loading screen must not do, since
// it trains the viewer to stop believing it. Naming the vendor under each stage is also the
// honest version of the pitch: these are the four companies doing the work.
const STAGES = [
  { key: 'IDENTIFIED', active: 'Identifying the garment', done: 'Identified',   vendor: 'Google Lens' },
  { key: 'PRICED',     active: 'Pricing from live listings', done: 'Priced',    vendor: 'eBay · Shopping · Trends' },
  { key: 'IMAGED',     active: 'Photographing it on a model', done: 'Photographed', vendor: 'Perfect Corp' },
  { key: 'LISTED',     active: 'Publishing the listing', done: 'Published',     vendor: 'Stripe · name.com' },
];

const ORDER = ['UPLOADED', 'IDENTIFIED', 'PRICED', 'IMAGED', 'LISTED'];

/**
 * How many stages are finished. For a batch this is the least advanced item still running, so the
 * screen never claims to be further along than the slowest piece actually is.
 */
function completedStages(items) {
  if (!items || items.length === 0) return 0;
  const live = items.filter(i => i.status !== 'FAILED');
  if (live.length === 0) return STAGES.length;
  return Math.min(...live.map(i => Math.max(0, ORDER.indexOf(i.status))));
}

function StageRow({ stage, state }) {
  const dotRef = useRef(null);

  useEffect(() => {
    if (state !== 'active' || !dotRef.current) return;
    const tween = gsap.to(dotRef.current, {
      opacity: 0.25, scale: 0.6, duration: 0.7,
      repeat: -1, yoyo: true, ease: 'sine.inOut',
    });
    return () => tween.kill();
  }, [state]);

  const isDone = state === 'done';
  const isActive = state === 'active';

  return (
    <div
      style={{
        display: 'grid',
        gridTemplateColumns: '18px 1fr auto',
        alignItems: 'center',
        gap: '14px',
        padding: '11px 0',
        borderBottom: `1px solid ${HAIR}`,
        opacity: isDone || isActive ? 1 : 0.4,
        transition: 'opacity 0.5s var(--ease-out-expo)',
      }}
    >
      <span style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', width: 18, height: 18 }}>
        {isDone ? (
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke={ACCENT}
               strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round">
            <polyline points="20 6 9 17 4 12" />
          </svg>
        ) : (
          <span
            ref={dotRef}
            style={{
              width: isActive ? 9 : 7,
              height: isActive ? 9 : 7,
              borderRadius: '50%',
              background: isActive ? INK : 'transparent',
              border: isActive ? 'none' : `1.5px solid ${MUTED}`,
              display: 'block',
            }}
          />
        )}
      </span>

      <span style={{
        fontFamily: SANS,
        fontSize: '14px',
        color: isDone || isActive ? INK : MUTED,
        fontWeight: isActive ? 600 : 400,
      }}>
        {isDone ? stage.done : stage.active}
      </span>

      <span style={{
        fontFamily: SANS, fontSize: '11px', letterSpacing: '0.07em',
        textTransform: 'uppercase', color: MUTED, whiteSpace: 'nowrap',
      }}>
        {stage.vendor}
      </span>
    </div>
  );
}

export default function ProcessingLoader({ progress }) {
  const wrapRef = useRef(null);
  const headingRef = useRef(null);

  const items = progress?.items ?? [];
  const total = progress?.total ?? 0;
  const complete = completedStages(items);
  const activeIndex = Math.min(complete, STAGES.length - 1);
  const heading = STAGES[activeIndex].active;

  useEffect(() => {
    gsap.fromTo(wrapRef.current, { opacity: 0 }, { opacity: 1, duration: 0.4 });
  }, []);

  // Re-animate the heading only when the stage genuinely changes, so movement on screen always
  // corresponds to movement in the pipeline.
  useEffect(() => {
    if (!headingRef.current) return;
    gsap.fromTo(headingRef.current,
      { opacity: 0, y: 8 },
      { opacity: 1, y: 0, duration: 0.35, ease: 'power2.out' });
  }, [activeIndex]);

  return (
    <div
      ref={wrapRef}
      className="flex-1 flex flex-col items-center justify-center"
      style={{ minHeight: '80vh', backgroundColor: '#f7f5f3', padding: '120px 24px 80px' }}
    >
      <div style={{ width: '100%', maxWidth: '440px' }}>
        <p style={{
          fontSize: '11px', fontFamily: SANS, fontWeight: 600, letterSpacing: '0.14em',
          textTransform: 'uppercase', color: MUTED, textAlign: 'center', margin: '0 0 10px',
        }}>
          {total > 1 ? `${total} pieces` : 'One piece'}
        </p>

        <p
          ref={headingRef}
          style={{
            fontSize: '30px', fontFamily: SERIF, fontWeight: 350, fontStyle: 'italic',
            lineHeight: 1.15, letterSpacing: '-0.03em', color: INK,
            textAlign: 'center', margin: '0 0 36px',
          }}
        >
          {heading}
        </p>

        <div>
          {STAGES.map((stage, i) => (
            <StageRow
              key={stage.key}
              stage={stage}
              state={i < complete ? 'done' : i === complete ? 'active' : 'pending'}
            />
          ))}
        </div>

        {total > 1 && (
          <p style={{
            marginTop: '20px', fontSize: '12px', fontFamily: SANS, letterSpacing: '0.08em',
            textTransform: 'uppercase', color: MUTED, textAlign: 'center',
          }}>
            {progress?.done ?? 0} of {total} pieces finished
          </p>
        )}

        <p style={{
          marginTop: '26px', fontSize: '12px', fontFamily: SANS, color: MUTED,
          textAlign: 'center', lineHeight: 1.6,
        }}>
          Rendering the garment on a model takes about ten seconds of that.
        </p>
      </div>
    </div>
  );
}
