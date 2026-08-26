import { useState, useRef, useEffect, useCallback } from 'react';
import { UploadCloud, CheckCircle2, Camera, X, Sparkles } from 'lucide-react';
import gsap from 'gsap';
import useMagneticHover from '../hooks/useMagneticHover';

const INK   = 'var(--color-ink)';
const MUTED = 'var(--color-muted)';
const CREAM = 'var(--color-cream)';
const HAIR  = 'var(--color-hairline)';
const ACCENT = 'var(--color-accent)';

export default function UploadSection({ onUpload }) {
  const [isDragging, setIsDragging]   = useState(false);
  const [file, setFile]               = useState(null);
  const [cameraOpen, setCameraOpen]   = useState(false);
  const [stream, setStream]           = useState(null);
  const [cameraError, setCameraError] = useState('');

  const containerRef = useRef(null);
  const iconRef      = useRef(null);
  const wrapRef      = useRef(null);
  const videoRef     = useRef(null);
  const canvasRef    = useRef(null);
  const overlayRef   = useRef(null);

  const uploadMagnetic = useMagneticHover({ strength: 8 });
  const cameraMagnetic = useMagneticHover({ strength: 8 });

  // ── entrance animation ──────────────────────────────────────────────────
  useEffect(() => {
    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (prefersReducedMotion) return;
    gsap.fromTo(wrapRef.current,
      { y: 32, opacity: 0 },
      { y: 0, opacity: 1, duration: 0.8, ease: 'power3.out' }
    );
  }, []);

  // ── attach stream to video element when camera opens ────────────────────
  useEffect(() => {
    if (stream && videoRef.current) {
      videoRef.current.srcObject = stream;
    }
  }, [stream]);

  // ── stop stream on unmount ──────────────────────────────────────────────
  useEffect(() => {
    return () => { stream?.getTracks().forEach(t => t.stop()); };
  }, [stream]);

  // ── drag/drop helpers ───────────────────────────────────────────────────
  const handleDragOver  = (e) => { e.preventDefault(); if (!isDragging) { setIsDragging(true);  gsap.to(containerRef.current, { scale: 1.02, duration: 0.3, ease: 'power2.out' }); } };
  const handleDragLeave = (e) => { e.preventDefault(); setIsDragging(false); gsap.to(containerRef.current, { scale: 1, duration: 0.3, ease: 'power2.out' }); };
  const handleDrop      = (e) => { e.preventDefault(); setIsDragging(false); gsap.to(containerRef.current, { scale: 1, duration: 0.3, ease: 'power2.out' }); if (e.dataTransfer.files?.[0]) handleFile(e.dataTransfer.files[0]); };
  const handleChange    = (e) => { if (e.target.files?.[0]) handleFile(e.target.files[0]); };

  const handleFile = (selectedFile) => { setFile(selectedFile); setTimeout(() => onUpload(selectedFile), 700); };

  // ── camera helpers ──────────────────────────────────────────────────────
  const openCamera = useCallback(async () => {
    setCameraError('');
    try {
      const s = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' }, audio: false });
      setStream(s);
      setCameraOpen(true);
      setTimeout(() => {
        if (overlayRef.current) gsap.fromTo(overlayRef.current, { opacity: 0 }, { opacity: 1, duration: 0.4, ease: 'power2.out' });
      }, 10);
    } catch {
      setCameraError('Camera access denied. Please allow camera permission and try again.');
    }
  }, []);

  const closeCamera = useCallback(() => {
    stream?.getTracks().forEach(t => t.stop());
    setStream(null);
    setCameraOpen(false);
  }, [stream]);

  const snapPhoto = useCallback(() => {
    const video  = videoRef.current;
    const canvas = canvasRef.current;
    if (!video || !canvas) return;
    canvas.width  = video.videoWidth;
    canvas.height = video.videoHeight;
    canvas.getContext('2d').drawImage(video, 0, 0);
    canvas.toBlob((blob) => {
      const captured = new File([blob], `rack-capture-${Date.now()}.jpg`, { type: 'image/jpeg' });
      closeCamera();
      handleFile(captured);
    }, 'image/jpeg', 0.92);
  }, [closeCamera]);

  return (
    <div
      className="flex-1 flex flex-col items-center justify-center"
      style={{ backgroundColor: 'transparent', minHeight: '80vh', padding: '120px 24px 80px' }}
    >
      <div ref={wrapRef} style={{ width: '100%', maxWidth: '640px' }}>
        {/* Sparkle icon */}
        <div style={{ textAlign: 'center', marginBottom: '20px' }}>
          <Sparkles size={24} color={MUTED} strokeWidth={1.2} style={{ opacity: 0.5 }} />
        </div>

        {/* Heading — oversized, matching site typography */}
        <h1
          style={{
            fontSize: 'clamp(38px, 6vw, 64px)',
            fontFamily: 'Cormorant Garamond, serif',
            fontWeight: 400,
            fontStyle: 'italic',
            lineHeight: 1.0,
            letterSpacing: '-0.04em',
            color: INK,
            marginBottom: '12px',
            textAlign: 'center',
          }}
        >
          Upload a garment photo
        </h1>

        <p
          style={{
            fontSize: '15px',
            fontFamily: 'Manrope, sans-serif',
            color: MUTED,
            textAlign: 'center',
            marginBottom: '48px',
            lineHeight: 1.6,
            maxWidth: '380px',
            margin: '0 auto 48px',
          }}
        >
          Works best with a clean, flat-lay photo of one item
        </p>

        {/* ── Two tiles ── */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>

          {/* Tile 1 — file upload / drag-drop (glassmorphism) */}
          <div
            ref={(el) => { containerRef.current = el; uploadMagnetic.ref.current = el; }}
            {...uploadMagnetic.handlers}
            data-cursor="upload"
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
            onClick={() => document.getElementById('file-upload-rack').click()}
            style={{
              background: isDragging
                ? 'rgba(247, 245, 243, 0.08)'
                : 'rgba(247, 245, 243, 0.03)',
              border: isDragging
                ? `1.5px solid rgba(247, 245, 243, 0.4)`
                : `1.5px solid ${HAIR}`,
              borderRadius: '20px',
              padding: '48px 24px',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              cursor: 'pointer',
              backdropFilter: 'blur(20px)',
              WebkitBackdropFilter: 'blur(20px)',
              transition: 'all 0.4s var(--ease-out-expo)',
            }}
            onMouseEnter={e => {
              if (!isDragging) {
                e.currentTarget.style.background = 'rgba(247, 245, 243, 0.06)';
                e.currentTarget.style.borderColor = 'rgba(247, 245, 243, 0.25)';
              }
            }}
            onMouseLeave={e => {
              if (!isDragging) {
                e.currentTarget.style.background = 'rgba(247, 245, 243, 0.03)';
                e.currentTarget.style.borderColor = HAIR;
              }
            }}
          >
            <input id="file-upload-rack" type="file" accept="image/*" className="hidden" onChange={handleChange} />
            <div ref={iconRef} style={{ marginBottom: '20px' }}>
              {file
                ? <CheckCircle2 size={40} color={INK} strokeWidth={1.2} />
                : <UploadCloud  size={40} color={MUTED} strokeWidth={1.2} />
              }
            </div>
            <p style={{
              fontSize: '18px',
              fontFamily: 'Cormorant Garamond, serif',
              fontStyle: 'italic',
              fontWeight: 400,
              color: INK,
              marginBottom: '6px',
              textAlign: 'center',
              letterSpacing: '-0.02em',
            }}>
              {file ? 'Photo ready' : 'Drop photo here'}
            </p>
            <p style={{
              fontSize: '12px',
              fontFamily: 'Manrope, sans-serif',
              color: MUTED,
              textAlign: 'center',
              lineHeight: 1.5,
            }}>
              {file ? file.name : 'or click to browse — JPG, PNG, WEBP'}
            </p>
          </div>

          {/* Tile 2 — camera */}
          <div
            ref={cameraMagnetic.ref}
            {...cameraMagnetic.handlers}
            onClick={openCamera}
            style={{
              background: 'rgba(247, 245, 243, 0.08)',
              border: `1.5px solid rgba(247, 245, 243, 0.15)`,
              borderRadius: '20px',
              padding: '48px 24px',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              cursor: 'pointer',
              transition: 'all 0.4s var(--ease-out-expo)',
            }}
            onMouseEnter={e => {
              e.currentTarget.style.background = 'rgba(247, 245, 243, 0.12)';
              e.currentTarget.style.borderColor = 'rgba(247, 245, 243, 0.3)';
            }}
            onMouseLeave={e => {
              e.currentTarget.style.background = 'rgba(247, 245, 243, 0.08)';
              e.currentTarget.style.borderColor = 'rgba(247, 245, 243, 0.15)';
            }}
          >
            <div style={{ marginBottom: '20px' }}>
              <Camera size={40} color={INK} strokeWidth={1.2} />
            </div>
            <p style={{
              fontSize: '18px',
              fontFamily: 'Cormorant Garamond, serif',
              fontStyle: 'italic',
              fontWeight: 400,
              color: INK,
              marginBottom: '6px',
              textAlign: 'center',
              letterSpacing: '-0.02em',
            }}>
              Take a photo
            </p>
            <p style={{
              fontSize: '12px',
              fontFamily: 'Manrope, sans-serif',
              color: MUTED,
              textAlign: 'center',
              lineHeight: 1.5,
            }}>
              Use your device camera
            </p>
          </div>
        </div>

        {/* Camera error */}
        {cameraError && (
          <p style={{ fontSize: '13px', fontFamily: 'Manrope, sans-serif', color: '#c0392b', textAlign: 'center', marginTop: '14px' }}>
            {cameraError}
          </p>
        )}
      </div>

      {/* ── Camera overlay (dark plum theme aligned) ────────────────────────── */}
      {cameraOpen && (
        <div
          ref={overlayRef}
          style={{
            position: 'fixed', inset: 0, zIndex: 1000,
            background: CREAM,
            display: 'flex', flexDirection: 'column',
            alignItems: 'center', justifyContent: 'center',
            padding: '32px 24px',
          }}
        >
          {/* RACK wordmark top-left */}
          <p style={{
            position: 'absolute', top: '28px', left: '32px',
            fontSize: '11px', fontFamily: 'Manrope, sans-serif',
            fontWeight: 600, letterSpacing: '0.14em',
            textTransform: 'uppercase', color: MUTED,
          }}>RACK</p>

          {/* Close */}
          <button
            onClick={closeCamera}
            style={{
              position: 'absolute', top: '20px', right: '24px',
              background: 'rgba(247, 245, 243, 0.1)', border: 'none',
              borderRadius: '50%', width: '44px', height: '44px',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              cursor: 'pointer', transition: 'background 0.3s var(--ease-out-expo)',
            }}
            onMouseEnter={e => e.currentTarget.style.background = 'rgba(247, 245, 243, 0.18)'}
            onMouseLeave={e => e.currentTarget.style.background = 'rgba(247, 245, 243, 0.1)'}
          >
            <X size={18} color={INK} strokeWidth={1.5} />
          </button>

          {/* Viewfinder label */}
          <p style={{
            fontSize: '11px', fontFamily: 'Manrope, sans-serif',
            fontWeight: 600, letterSpacing: '0.14em',
            textTransform: 'uppercase',
            color: MUTED,
            marginBottom: '20px',
          }}>
            Position your garment
          </p>

          {/* Video feed */}
          <div style={{
            position: 'relative', borderRadius: '20px', overflow: 'hidden',
            maxWidth: '540px', width: '100%',
            boxShadow: '0 0 0 1px rgba(247, 245, 243, 0.1), 0 12px 48px rgba(0,0,0,0.3)',
          }}>
            <video
              ref={videoRef}
              autoPlay
              playsInline
              muted
              style={{ display: 'block', width: '100%', height: 'auto' }}
            />
            {/* Corner guides */}
            {['tl','tr','bl','br'].map(c => (
              <div key={c} style={{
                position: 'absolute',
                top:    c.startsWith('t') ? 14 : 'auto',
                bottom: c.startsWith('b') ? 14 : 'auto',
                left:   c.endsWith('l')   ? 14 : 'auto',
                right:  c.endsWith('r')   ? 14 : 'auto',
                width: 24, height: 24,
                borderTop:    c.startsWith('t') ? `2px solid ${INK}` : 'none',
                borderBottom: c.startsWith('b') ? `2px solid ${INK}` : 'none',
                borderLeft:   c.endsWith('l')   ? `2px solid ${INK}` : 'none',
                borderRight:  c.endsWith('r')   ? `2px solid ${INK}` : 'none',
                opacity: 0.4,
              }} />
            ))}
          </div>

          {/* Shutter */}
          <button
            onClick={snapPhoto}
            style={{
              marginTop: '36px',
              width: '72px', height: '72px',
              borderRadius: '50%',
              background: INK,
              border: '4px solid rgba(247, 245, 243, 0.15)',
              cursor: 'pointer',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              transition: 'transform 0.3s var(--ease-spring)',
              boxShadow: '0 4px 24px rgba(0,0,0,0.3)',
            }}
            onMouseEnter={e => e.currentTarget.style.transform = 'scale(1.08)'}
            onMouseLeave={e => e.currentTarget.style.transform = 'scale(1)'}
            onMouseDown={e => e.currentTarget.style.transform = 'scale(0.94)'}
            onMouseUp={e => e.currentTarget.style.transform = 'scale(1.08)'}
          >
            <div style={{
              width: '54px', height: '54px', borderRadius: '50%',
              background: INK,
              border: '2px solid rgba(247, 245, 243, 0.2)',
            }} />
          </button>

          <p style={{
            fontSize: '12px', fontFamily: 'Manrope, sans-serif',
            color: MUTED, marginTop: '14px',
          }}>
            Click shutter to capture
          </p>
        </div>
      )}

      {/* Hidden canvas for snapshot */}
      <canvas ref={canvasRef} style={{ display: 'none' }} />
    </div>
  );
}
