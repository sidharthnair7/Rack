import { useEffect, useRef } from 'react';
import gsap from 'gsap';
import { ExternalLink, RotateCcw, ArrowUpRight, TrendingUp, TrendingDown, Minus } from 'lucide-react';

const INK = '#0d0d0d';
const MUTED = '#9a9796';
const CREAM = '#f7f5f3';
const HAIR = 'rgba(13,13,13,0.10)';
const SERIF = 'Cormorant Garamond, serif';
const SANS = 'Manrope, sans-serif';

const money = n =>
  n == null ? '—' : `$${Number(n).toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 2 })}`;

const eyebrow = {
  fontSize: '11px', fontWeight: 600, letterSpacing: '0.12em', textTransform: 'uppercase',
  color: MUTED, fontFamily: SANS, margin: 0,
};

function DemandBadge({ demand }) {
  if (!demand) return null;
  const map = {
    RISING: { Icon: TrendingUp, label: 'Demand rising', color: '#3f6b46' },
    FALLING: { Icon: TrendingDown, label: 'Demand falling', color: '#8a5a2b' },
    FLAT: { Icon: Minus, label: 'Demand flat', color: MUTED },
  };
  const { Icon, label, color } = map[demand] ?? map.FLAT;
  return (
    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', fontSize: '13px', color, fontFamily: SANS }}>
      <Icon size={14} strokeWidth={1.6} />
      {label}
    </span>
  );
}

/**
 * One garment: what the phone saw, what Rack produced, what it is worth, and the actual
 * listings that number came from. Every figure and link here comes from the API — a missing
 * value renders as "—" rather than being filled in with something plausible.
 */
function ItemCard({ item, cardRef }) {
  return (
    <article
      ref={cardRef}
      style={{
        background: '#fff', borderRadius: '16px', border: '1px solid rgba(13,13,13,0.06)',
        overflow: 'hidden', marginBottom: '24px',
      }}
    >
      <div className="grid grid-cols-1 lg:grid-cols-2">
        {/* Before / after */}
        <div style={{ padding: '20px', borderRight: '1px solid rgba(13,13,13,0.06)' }}>
          {item.hasTransform ? (
            <>
              <p style={{ ...eyebrow, marginBottom: '12px' }}>What changed</p>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                <figure style={{ margin: 0 }}>
                  <div style={{ borderRadius: '12px', overflow: 'hidden', background: '#ede9e4', aspectRatio: '3/4' }}>
                    <img src={item.original} alt="As photographed"
                         style={{ width: '100%', height: '100%', objectFit: 'cover', display: 'block' }} />
                  </div>
                  <figcaption style={{ ...eyebrow, fontSize: '10px', textAlign: 'center', paddingTop: '8px' }}>
                    As photographed
                  </figcaption>
                </figure>
                <figure style={{ margin: 0 }}>
                  <div style={{ borderRadius: '12px', overflow: 'hidden', background: '#ede9e4', aspectRatio: '3/4' }}>
                    <img src={item.processed} alt="As listed"
                         style={{ width: '100%', height: '100%', objectFit: 'cover', display: 'block' }} />
                  </div>
                  <figcaption style={{ ...eyebrow, fontSize: '10px', textAlign: 'center', paddingTop: '8px', color: INK }}>
                    As listed
                  </figcaption>
                </figure>
              </div>
            </>
          ) : (
            <>
              <p style={{ ...eyebrow, marginBottom: '12px' }}>Your photo</p>
              <div style={{ borderRadius: '12px', overflow: 'hidden', background: '#ede9e4', aspectRatio: '3/4' }}>
                <img src={item.processed ?? item.original} alt={item.title}
                     style={{ width: '100%', height: '100%', objectFit: 'cover', display: 'block' }} />
              </div>
            </>
          )}
        </div>

        {/* Price + evidence */}
        <div style={{ padding: '20px', display: 'flex', flexDirection: 'column' }}>
          <p style={{ ...eyebrow }}>{[item.brand, item.type].filter(Boolean).join(' · ') || 'Identified'}</p>
          <h3 style={{
            fontFamily: SERIF, fontSize: '26px', fontWeight: 350, fontStyle: 'italic',
            letterSpacing: '-0.03em', color: INK, margin: '6px 0 10px', lineHeight: 1.15,
          }}>
            {item.title}
          </h3>

          <div style={{ display: 'flex', alignItems: 'baseline', gap: '12px', flexWrap: 'wrap' }}>
            <span style={{ fontFamily: SERIF, fontSize: '40px', fontWeight: 350, fontStyle: 'italic', letterSpacing: '-0.04em', color: INK }}>
              {money(item.price)}
            </span>
            {item.retailNew != null && (
              <span style={{ fontSize: '13px', color: MUTED, fontFamily: SANS }}>
                retails new at {money(item.retailNew)}
              </span>
            )}
          </div>

          <div style={{ display: 'flex', gap: '16px', flexWrap: 'wrap', margin: '10px 0 4px' }}>
            {item.rangeLow != null && item.rangeHigh != null && (
              <span style={{ fontSize: '13px', color: MUTED, fontFamily: SANS }}>
                comparables run {money(item.rangeLow)}–{money(item.rangeHigh)}
              </span>
            )}
            <DemandBadge demand={item.demand} />
          </div>

          {item.warning && (
            <p style={{
              fontSize: '12px', color: '#8a5a2b', fontFamily: SANS, margin: '8px 0 0',
              background: 'rgba(138,90,43,0.07)', padding: '8px 10px', borderRadius: '8px',
            }}>
              {item.warning}
            </p>
          )}

          {/* The evidence. This is the whole argument: click any of these. */}
          {item.comps.length > 0 && (
            <div style={{ marginTop: '18px', borderTop: `1px solid ${HAIR}`, paddingTop: '14px' }}>
              <p style={{ ...eyebrow, marginBottom: '8px' }}>
                Priced from {item.compCount} comparable listing{item.compCount === 1 ? '' : 's'}
              </p>
              <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
                {item.comps.map((c, i) => (
                  <li key={i}>
                    <a href={c.sourceUrl} target="_blank" rel="noopener noreferrer"
                       style={{
                         display: 'grid', gridTemplateColumns: '4.5rem 1fr auto', gap: '10px',
                         alignItems: 'baseline', padding: '7px 0', textDecoration: 'none',
                         borderBottom: '1px solid rgba(13,13,13,0.05)', fontFamily: SANS, fontSize: '13px',
                       }}>
                      <span style={{ color: '#8a5a2b' }}>{money(c.price)}</span>
                      <span style={{ color: INK, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {c.title}
                      </span>
                      <ExternalLink size={12} color={MUTED} strokeWidth={1.5} />
                    </a>
                  </li>
                ))}
              </ul>
              {item.compCount > item.comps.length && (
                <p style={{ fontSize: '12px', color: MUTED, fontFamily: SANS, margin: '10px 0 0' }}>
                  … and {item.compCount - item.comps.length} more
                </p>
              )}
            </div>
          )}

          {/* Actions */}
          <div style={{ display: 'flex', gap: '10px', marginTop: 'auto', paddingTop: '18px', flexWrap: 'wrap' }}>
            {item.checkoutUrl && (
              <a href={item.checkoutUrl} target="_blank" rel="noopener noreferrer"
                 style={{
                   flex: '1 1 auto', textAlign: 'center', background: INK, color: CREAM,
                   padding: '11px 18px', borderRadius: '12px', textDecoration: 'none',
                   fontFamily: SANS, fontSize: '14px', fontWeight: 500,
                 }}>
                Buy — {money(item.price)}
              </a>
            )}
            {item.listingUrl && (
              <a href={item.listingUrl} target="_blank" rel="noopener noreferrer"
                 style={{
                   display: 'inline-flex', alignItems: 'center', gap: '6px',
                   padding: '11px 18px', borderRadius: '12px', textDecoration: 'none',
                   border: '1px solid rgba(13,13,13,0.15)', color: INK,
                   fontFamily: SANS, fontSize: '14px', fontWeight: 500,
                 }}>
                View listing <ArrowUpRight size={14} strokeWidth={1.6} />
              </a>
            )}
          </div>
        </div>
      </div>
    </article>
  );
}

export default function ResultsDisplay({ data, onReset }) {
  const wrapRef = useRef(null);
  const cardsRef = useRef([]);

  useEffect(() => {
    gsap.fromTo(wrapRef.current, { opacity: 0, y: 16 }, { opacity: 1, y: 0, duration: 0.5, ease: 'power2.out' });
    gsap.fromTo(
      cardsRef.current.filter(Boolean),
      { opacity: 0, y: 24 },
      { opacity: 1, y: 0, duration: 0.5, stagger: 0.1, ease: 'power2.out', delay: 0.2 }
    );
  }, [data]);

  const items = data?.items ?? [];

  return (
    <div ref={wrapRef} style={{
      backgroundColor: CREAM, minHeight: '80vh', padding: '120px 24px 80px',
      maxWidth: '1280px', margin: '0 auto', width: '100%', boxSizing: 'border-box',
    }}>
      <div style={{
        display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end',
        marginBottom: '40px', borderBottom: `1px solid ${HAIR}`, paddingBottom: '24px', gap: '16px', flexWrap: 'wrap',
      }}>
        <div>
          <p style={{ ...eyebrow, marginBottom: '8px' }}>
            {items.length} {items.length === 1 ? 'piece' : 'pieces'} listed
          </p>
          <h1 style={{
            fontSize: '38px', fontFamily: SERIF, fontWeight: 350, lineHeight: 1.08,
            letterSpacing: '-0.04em', color: INK, fontStyle: 'italic', margin: 0,
          }}>
            {money(data?.total)} of inventory.
          </h1>
          {data?.failed > 0 && (
            <p style={{ fontSize: '13px', color: MUTED, fontFamily: SANS, marginTop: '8px' }}>
              {data.failed} {data.failed === 1 ? 'piece' : 'pieces'} couldn’t be priced from real listings, so
              {data.failed === 1 ? ' it was' : ' they were'} left unlisted rather than guessed at.
            </p>
          )}
        </div>
        <div style={{ display: 'flex', gap: '10px' }}>
          {data?.storefrontUrl && (
            <a href={data.storefrontUrl} target="_blank" rel="noopener noreferrer"
               style={{
                 display: 'flex', alignItems: 'center', gap: '8px', padding: '10px 20px',
                 background: INK, color: CREAM, borderRadius: '16px', fontSize: '14px',
                 fontFamily: SANS, fontWeight: 500, textDecoration: 'none',
               }}>
              Open storefront <ArrowUpRight size={14} strokeWidth={1.6} />
            </a>
          )}
          <button onClick={onReset} style={{
            display: 'flex', alignItems: 'center', gap: '8px', padding: '10px 20px',
            background: CREAM, border: '1px solid rgba(13,13,13,0.15)', borderRadius: '16px',
            fontSize: '14px', fontFamily: SANS, fontWeight: 500, color: INK, cursor: 'pointer',
          }}>
            <RotateCcw size={14} strokeWidth={1.5} />
            Scan another
          </button>
        </div>
      </div>

      {items.length === 0 ? (
        <p style={{ fontFamily: SANS, color: MUTED, fontSize: '15px', maxWidth: '46ch' }}>
          Nothing could be priced from real comparable listings, so nothing was published.
          Rack won’t invent a number — try a clearer photo of a single garment.
        </p>
      ) : (
        items.map((item, i) => (
          <ItemCard key={item.itemId} item={item} cardRef={el => { cardsRef.current[i] = el; }} />
        ))
      )}
    </div>
  );
}
