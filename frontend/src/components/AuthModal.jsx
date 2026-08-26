import { useState } from 'react';
import { X } from 'lucide-react';

export default function AuthModal({ isOpen, onClose, initialMode = 'signin' }) {
  const [mode, setMode] = useState(initialMode);

  if (!isOpen) return null;

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        zIndex: 200,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '16px',
      }}
    >
      {/* Backdrop */}
      <div
        onClick={onClose}
        style={{
          position: 'absolute',
          inset: 0,
          background: 'rgba(13,13,13,0.18)',
          backdropFilter: 'blur(4px)',
          WebkitBackdropFilter: 'blur(4px)',
        }}
      />

      {/* Panel */}
      <div
        style={{
          position: 'relative',
          width: '100%',
          maxWidth: '400px',
          background: '#f7f5f3',
          borderRadius: '16px',
          padding: '32px',
          border: '1px solid rgba(13,13,13,0.10)',
        }}
      >
        {/* Close */}
        <button
          onClick={onClose}
          style={{
            position: 'absolute',
            top: '16px', right: '16px',
            background: 'none',
            border: 'none',
            cursor: 'pointer',
            color: '#9a9796',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '4px',
            borderRadius: '8px',
            transition: 'color 150ms ease',
          }}
          onMouseEnter={e => e.currentTarget.style.color = '#0d0d0d'}
          onMouseLeave={e => e.currentTarget.style.color = '#9a9796'}
        >
          <X size={18} strokeWidth={1.5} />
        </button>

        {/* Eyebrow */}
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
          RACK
        </p>

        {/* Heading */}
        <h2
          style={{
            fontSize: '33px',
            fontFamily: 'Cormorant Garamond, serif',
            fontWeight: 350,
            lineHeight: 1.1,
            letterSpacing: '-0.04em',
            color: '#0d0d0d',
            marginBottom: '28px',
            fontStyle: 'italic',
          }}
        >
          {mode === 'signin' ? 'Welcome back.' : 'Join RACK.'}
        </h2>

        {/* Form */}
        <form
          style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}
          onSubmit={e => { e.preventDefault(); onClose(); }}
        >
          {mode === 'signup' && (
            <FormField label="Full name" type="text" placeholder="Jane Doe" />
          )}
          <FormField label="Email"    type="email"    placeholder="you@example.com" />
          <FormField label="Password" type="password" placeholder="••••••••" />

          <button
            type="submit"
            style={{
              width: '100%',
              padding: '13px 24px',
              background: '#0d0d0d',
              color: '#f7f5f3',
              fontFamily: 'Manrope, sans-serif',
              fontSize: '15px',
              fontWeight: 500,
              borderRadius: '16px',
              border: 'none',
              cursor: 'pointer',
              marginTop: '8px',
              transition: 'opacity 0.15s',
            }}
            onMouseEnter={e => e.currentTarget.style.opacity = '0.8'}
            onMouseLeave={e => e.currentTarget.style.opacity = '1'}
          >
            {mode === 'signin' ? 'Log in' : 'Create account'}
          </button>
        </form>

        {/* Toggle */}
        <p
          style={{
            marginTop: '20px',
            textAlign: 'center',
            fontSize: '14px',
            color: '#9a9796',
            fontFamily: 'Manrope, sans-serif',
          }}
        >
          {mode === 'signin' ? "Don't have an account? " : 'Already have an account? '}
          <button
            type="button"
            onClick={() => setMode(mode === 'signin' ? 'signup' : 'signin')}
            style={{
              background: 'none',
              border: 'none',
              cursor: 'pointer',
              color: '#0d0d0d',
              fontFamily: 'Manrope, sans-serif',
              fontSize: '14px',
              fontWeight: 600,
              textDecoration: 'underline',
              textUnderlineOffset: '2px',
            }}
          >
            {mode === 'signin' ? 'Sign up' : 'Log in'}
          </button>
        </p>
      </div>
    </div>
  );
}

function FormField({ label, type, placeholder }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
      <label
        style={{
          fontSize: '13px',
          fontWeight: 500,
          color: '#9a9796',
          letterSpacing: '0.01em',
          fontFamily: 'Manrope, sans-serif',
        }}
      >
        {label}
      </label>
      <input
        type={type}
        placeholder={placeholder}
        required
        style={{
          padding: '11px 16px',
          background: '#fdfcfb',
          border: '1px solid rgba(13,13,13,0.12)',
          borderRadius: '16px',
          fontSize: '15px',
          fontFamily: 'Manrope, sans-serif',
          fontWeight: 400,
          color: '#0d0d0d',
          outline: 'none',
          transition: 'border-color 0.2s',
        }}
        onFocus={e  => e.currentTarget.style.borderColor = 'rgba(13,13,13,0.45)'}
        onBlur={e   => e.currentTarget.style.borderColor = 'rgba(13,13,13,0.12)'}
      />
    </div>
  );
}
