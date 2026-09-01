import { useState } from 'react';
import { processImage } from './lib/api';
import Navbar from './components/Navbar';
import LandingPage from './components/LandingPage';
import UploadSection from './components/UploadSection';
import ProcessingLoader from './components/ProcessingLoader';
import ResultsDisplay from './components/ResultsDisplay';
import Waves from './components/Waves';

export default function App() {
  const [stage, setStage] = useState('landing');
  const [data, setData] = useState(null);
  const [resetKey, setResetKey] = useState(0);
  const [progress, setProgress] = useState(null);
  const [error, setError] = useState(null);

  const handleLogoClick = () => {
    setStage('landing');
    setResetKey(k => k + 1);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleNavClick = (id) => {
    if (stage !== 'landing') {
      setStage('landing');
      setResetKey(k => k + 1);
      setTimeout(() => {
        document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' });
      }, 100);
    } else {
      document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' });
    }
  };

  const handleUpload = async (files) => {
    setStage('processing');
    setError(null);
    setProgress(null);
    try {
      // A batch really does take tens of seconds — four vendors per item, polled on a
      // scheduler — so report progress rather than leaving a spinner with nothing behind it.
      const result = await processImage(files, setProgress);
      setData(result);
      setStage('results');
    } catch (err) {
      // Surfaced, not swallowed: silently returning to the upload screen makes a backend that
      // is down look identical to a user who changed their mind.
      console.error('Error processing image:', err);
      setError(err.message || 'Something went wrong talking to the server.');
      setStage('upload');
    }
  };

  const handleReset = () => { setData(null); setError(null); setProgress(null); setStage('upload'); };

  return (
      <div
        className="min-h-screen flex flex-col font-sans"
        style={{ backgroundColor: 'var(--color-cream)', color: 'var(--color-ink)' }}
      >
        {/* Ambient Waves background */}
        <div style={{ position: 'fixed', inset: 0, zIndex: 0, pointerEvents: 'none' }}>
          <Waves
            lineColor="rgba(59, 34, 40, 0.055)"
            backgroundColor="transparent"
            waveSpeedX={0.015}
            waveSpeedY={0.005}
            waveAmpX={32}
            waveAmpY={16}
            xGap={14}
            yGap={32}
            friction={0.9}
            tension={0.01}
            maxCursorMove={120}
          />
        </div>

        {/* The custom dot-and-ring cursor was removed rather than restyled. It set
            `body { cursor: none }`, which only hides the pointer the browser draws: a screen
            recorder composites the operating system's cursor into the video separately, so a
            recording showed the real arrow and a lagging ring at the same time. Two cursors reads
            as a rendering bug, in the exact artifact being judged. */}

        {/* SVG grain overlay — barely perceptible texture */}
        <div
          style={{
            position: 'fixed',
            inset: 0,
            pointerEvents: 'none',
            zIndex: 9999,
            opacity: 0.022,
            mixBlendMode: 'multiply',
            animation: 'grainShift 8s steps(8) infinite',
          }}
        >
          <svg width="100%" height="100%" xmlns="http://www.w3.org/2000/svg">
            <filter id="rack-grain">
              <feTurbulence type="fractalNoise" baseFrequency="0.85" numOctaves="4" stitchTiles="stitch" />
              <feColorMatrix type="saturate" values="0" />
            </filter>
            <rect width="150%" height="150%" filter="url(#rack-grain)" />
          </svg>
        </div>

        {/* Navbar — always visible */}
        <Navbar onStart={() => setStage('upload')} onLogoClick={handleLogoClick} onNavClick={handleNavClick} />


        <main className="flex-1 flex flex-col w-full">
          {stage === 'landing'    && <LandingPage key={resetKey} onStart={() => setStage('upload')} />}
          {stage === 'upload'     && <UploadSection onUpload={handleUpload} error={error} />}
          {stage === 'processing' && <ProcessingLoader progress={progress} />}
          {stage === 'results' && data && <ResultsDisplay data={data} onReset={handleReset} />}
        </main>

        {/* Footer */}
        <footer
          style={{
            padding: '28px 32px',
            borderTop: '1px solid var(--color-hairline)',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            backgroundColor: 'var(--color-cream)',
          }}
        >
          <span
            style={{
              fontFamily: 'Cormorant Garamond, serif',
              fontStyle: 'italic',
              fontSize: '16px',
              fontWeight: 400,
              color: 'var(--color-ink)',
              letterSpacing: '-0.02em',
            }}
          >
            RACK
          </span>
          <span style={{ fontSize: '13px', color: 'var(--color-muted)', fontFamily: 'Manrope, sans-serif' }}>
            © {new Date().getFullYear()} · Photograph, price and publish your closet
          </span>
        </footer>
      </div>
  );
}
