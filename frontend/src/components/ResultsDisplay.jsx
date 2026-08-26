import { useEffect, useRef } from 'react';
import gsap from 'gsap';
import { ExternalLink, RotateCcw } from 'lucide-react';

export default function ResultsDisplay({ data, onReset }) {
  const wrapRef  = useRef(null);
  const cardsRef = useRef([]);

  useEffect(() => {
    gsap.fromTo(wrapRef.current, { opacity: 0, y: 16 }, { opacity: 1, y: 0, duration: 0.5, ease: 'power2.out' });
    gsap.fromTo(
      cardsRef.current,
      { opacity: 0, y: 24 },
      { opacity: 1, y: 0, duration: 0.5, stagger: 0.1, ease: 'power2.out', delay: 0.2 }
    );
  }, []);

  return (
    <div
      ref={wrapRef}
      style={{
        backgroundColor: '#f7f5f3',
        minHeight: '80vh',
        padding: '120px 24px 80px',
        maxWidth: '1280px',
        margin: '0 auto',
        width: '100%',
        boxSizing: 'border-box',
      }}
    >
      {/* Page header */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'flex-end',
          marginBottom: '48px',
          borderBottom: '1px solid rgba(13,13,13,0.10)',
          paddingBottom: '24px',
        }}
      >
        <div>
          <p
            style={{
              fontSize: '11px',
              fontWeight: 600,
              letterSpacing: '0.12em',
              textTransform: 'uppercase',
              color: '#9a9796',
              marginBottom: '8px',
              fontFamily: 'Manrope, sans-serif',
            }}
          >
            Analysis complete
          </p>
          <h1
            style={{
              fontSize: '38px',
              fontFamily: 'Cormorant Garamond, serif',
              fontWeight: 350,
              lineHeight: 1.08,
              letterSpacing: '-0.04em',
              color: '#0d0d0d',
              fontStyle: 'italic',
              margin: 0,
            }}
          >
            Your garment, priced.
          </h1>
        </div>
        <button
          onClick={onReset}
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            padding: '10px 20px',
            background: '#f7f5f3',
            border: '1px solid rgba(13,13,13,0.15)',
            borderRadius: '16px',
            fontSize: '14px',
            fontFamily: 'Manrope, sans-serif',
            fontWeight: 500,
            color: '#0d0d0d',
            cursor: 'pointer',
            transition: 'background 150ms ease',
          }}
          onMouseEnter={e => e.currentTarget.style.background = 'rgba(13,13,13,0.05)'}
          onMouseLeave={e => e.currentTarget.style.background = '#f7f5f3'}
        >
          <RotateCcw size={14} strokeWidth={1.5} />
          Scan another
        </button>
      </div>

      {/* Two-column layout */}
      <div className="grid grid-cols-1 lg:grid-cols-3" style={{ gap: '24px' }}>

        {/* AI Try-On — left column */}
        <div className="lg:col-span-1">
          <div style={{ position: 'sticky', top: '96px' }}>
            <p
              style={{
                fontSize: '11px',
                fontWeight: 600,
                letterSpacing: '0.12em',
                textTransform: 'uppercase',
                color: '#9a9796',
                marginBottom: '12px',
                fontFamily: 'Manrope, sans-serif',
              }}
            >
              AI try-on
            </p>
            <div
              style={{
                borderRadius: '16px',
                overflow: 'hidden',
                background: '#ede9e4',
                aspectRatio: '3/4',
                position: 'relative',
              }}
            >
              <img
                src={data.aiTryOn.modelImage}
                alt="AI Model"
                style={{ width: '100%', height: '100%', objectFit: 'cover', display: 'block' }}
              />
              <div
                style={{
                  position: 'absolute',
                  bottom: 0, left: 0, right: 0,
                  padding: '16px',
                  background: 'linear-gradient(to top, rgba(13,13,13,0.5), transparent)',
                }}
              >
                <span style={{ fontSize: '13px', color: '#f7f5f3', fontWeight: 400, opacity: 0.9 }}>
                  {data.aiTryOn.description}
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* Pricing cards — right columns */}
        <div className="lg:col-span-2">
          <p
            style={{
              fontSize: '11px',
              fontWeight: 600,
              letterSpacing: '0.12em',
              textTransform: 'uppercase',
              color: '#9a9796',
              marginBottom: '12px',
              fontFamily: 'Manrope, sans-serif',
            }}
          >
            Market pricing
          </p>
          <div className="grid grid-cols-1 sm:grid-cols-2" style={{ gap: '16px' }}>
            {data.pricing.map((item, i) => (
              <a
                key={i}
                href={item.link}
                ref={el => { cardsRef.current[i] = el; }}
                style={{
                  display: 'flex',
                  flexDirection: 'column',
                  background: '#fff',
                  borderRadius: '16px',
                  overflow: 'hidden',
                  textDecoration: 'none',
                  color: 'inherit',
                  border: '1px solid rgba(13,13,13,0.06)',
                  transition: 'transform 0.2s',
                }}
                onMouseEnter={e => e.currentTarget.style.transform = 'translateY(-2px)'}
                onMouseLeave={e => e.currentTarget.style.transform = 'translateY(0)'}
              >
                {/* Image */}
                <div style={{ aspectRatio: '4/3', overflow: 'hidden', position: 'relative' }}>
                  <img
                    src={item.image}
                    alt={item.title}
                    style={{
                      width: '100%', height: '100%',
                      objectFit: 'cover', display: 'block',
                      transition: 'transform 0.5s',
                    }}
                    onMouseEnter={e => e.currentTarget.style.transform = 'scale(1.04)'}
                    onMouseLeave={e => e.currentTarget.style.transform = 'scale(1)'}
                  />
                  {/* Source badge */}
                  <div
                    style={{
                      position: 'absolute',
                      top: '12px', left: '12px',
                      padding: '4px 10px',
                      background: 'rgba(247,245,243,0.92)',
                      borderRadius: '9999px',
                      fontSize: '11px',
                      fontWeight: 600,
                      color: '#0d0d0d',
                      letterSpacing: '0.06em',
                      textTransform: 'uppercase',
                      fontFamily: 'Manrope, sans-serif',
                    }}
                  >
                    {item.source}
                  </div>
                </div>

                {/* Content */}
                <div style={{ padding: '16px', flexGrow: 1, display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
                  <p
                    style={{
                      fontSize: '15px',
                      fontWeight: 400,
                      color: '#0d0d0d',
                      lineHeight: 1.4,
                      marginBottom: '12px',
                      fontFamily: 'Manrope, sans-serif',
                    }}
                  >
                    {item.title}
                  </p>
                  <div
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      borderTop: '1px solid rgba(13,13,13,0.08)',
                      paddingTop: '12px',
                    }}
                  >
                    <span
                      style={{
                        fontSize: '24px',
                        fontFamily: 'Cormorant Garamond, serif',
                        fontWeight: 350,
                        letterSpacing: '-0.03em',
                        color: '#0d0d0d',
                        fontStyle: 'italic',
                      }}
                    >
                      {item.price}
                    </span>
                    <ExternalLink size={14} color="#9a9796" strokeWidth={1.5} />
                  </div>
                </div>
              </a>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
