import useScrollReveal from '../hooks/useScrollReveal';

const INK   = 'var(--color-ink)';

const STEPS = [
  { num: '01', label: 'IDENTIFY',   title: 'Snap the pile' },
  { num: '02', label: 'PRICE',      title: 'Real sold comps' },
  { num: '03', label: 'PHOTOGRAPH', title: 'Studio + on-model' },
  { num: '04', label: 'PUBLISH',    title: 'Live on your domain' },
];

export default function HowItWorksSection() {
  const reveal = useScrollReveal({ stagger: 120, distance: 24 });

  return (
    <section
      style={{
        backgroundColor: 'transparent',
        padding: '64px 24px',
        width: '100%',
        display: 'flex',
        justifyContent: 'center',
        position: 'relative',
        zIndex: 10,
      }}
    >
      <div
        style={{
          display: 'flex',
          flexDirection: 'row',
          alignItems: 'center',
          gap: '32px',
          flexWrap: 'wrap',
          justifyContent: 'center',
        }}
      >
        {STEPS.map((step, i) => (
          <div
            key={step.num}
            ref={reveal}
            style={{ display: 'flex', alignItems: 'center', gap: '32px' }}
          >
            <div style={{ textAlign: 'center' }}>
              <p
                style={{
                  fontSize: '10px',
                  fontFamily: 'Manrope, sans-serif',
                  fontWeight: 700,
                  letterSpacing: '0.12em',
                  color: '#bfa7ad',
                  marginBottom: '8px',
                  textTransform: 'uppercase',
                }}
              >
                {step.num} {step.label}
              </p>
              <p
                style={{
                  fontSize: '22px',
                  fontFamily: 'Cormorant Garamond, serif',
                  fontStyle: 'italic',
                  fontWeight: 400,
                  color: INK,
                  letterSpacing: '-0.01em',
                }}
              >
                {step.title}
              </p>
            </div>
            {/* Arrow separator */}
            {i < STEPS.length - 1 && (
              <svg width="14" height="10" viewBox="0 0 14 10" fill="none" style={{ opacity: 0.3, color: INK }}>
                <path d="M1 5H13M13 5L9 1M13 5L9 9" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            )}
          </div>
        ))}
      </div>
    </section>
  );
}
