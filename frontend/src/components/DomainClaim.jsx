import { useState } from 'react';
import { searchDomains, registerDomain } from '../lib/api';
import { Globe, Check, Circle } from 'lucide-react';

const INK = '#1c1a17';
const MUTED = '#6e6862';
const HAIR = 'rgba(28,26,23,0.12)';
const ACCENT = 'var(--color-accent)';
const SERIF = 'Cormorant Garamond, serif';
const SANS = 'Manrope, sans-serif';

// The six name.com operations a registration actually performs, in order, named as the API names
// them. Shown ticking off as they happen rather than as a static list, because the whole point of
// this panel is that the API is doing the work rather than the copy claiming it did.
const STEPS = [
  'domains:search',
  'domains:checkAvailability',
  'domains (register)',
  'dns/records (A)',
  'subdomain www',
  'urlForwardings',
];

/**
 * The storefront claim step.
 *
 * This flow existed as a controller and an unused client function: name.com performed all six
 * operations and nothing in the product could reach them, so the final stage of the pipeline was
 * real in the code and invisible to anyone actually using it. This is the missing surface.
 */
export default function DomainClaim({ storeId, onClaimed }) {
  const [query, setQuery] = useState('');
  const [options, setOptions] = useState(null);
  const [busy, setBusy] = useState(false);
  const [claimed, setClaimed] = useState(null);
  const [error, setError] = useState(null);
  const [done, setDone] = useState(-1);
  // Step names the server reported as actually completed. The UI used to advance a counter on a
  // timer, which meant it would have shown six green ticks even when the API only managed three.
  const [completed, setCompleted] = useState([]);
  // Why each unfinished step did not finish, keyed by the name.com operation, as the server
  // reported it. A single shared caveat was wrong for at least one row every time.
  const [notes, setNotes] = useState({});

  // STEPS are display labels ("domains (register)"); the server keys on the operation ("domains").
  const reasonFor = (step) => {
    const key = Object.keys(notes).find((k) => step.startsWith(k));
    return key ? notes[key] : 'did not complete';
  };

  const runSearch = async (e) => {
    e.preventDefault();
    if (!query.trim() || busy) return;
    setBusy(true);
    setError(null);
    setOptions(null);
    setDone(0);
    try {
      const found = await searchDomains(query.trim());
      setOptions(found);
      setDone(1);
    } catch (err) {
      setError(err.message || 'name.com search failed.');
      setDone(-1);
    } finally {
      setBusy(false);
    }
  };

  const claim = async (domain) => {
    if (busy) return;
    setBusy(true);
    setError(null);
    try {
      setDone(2);
      const result = await registerDomain(storeId, domain);
      const name = result.domain || domain;
      // Only tick what the server says landed. In name.com's sandbox a registration can succeed
      // while the DNS write 404s, and showing six ticks for three completed calls would be the
      // kind of claim this whole product exists to avoid.
      setCompleted(['domains:search', ...(result.completed || [])]);
      setNotes(result.notes || {});
      setClaimed(name);
      setDone(STEPS.length);
      if (onClaimed) onClaimed(name);
    } catch (err) {
      setError(err.message || 'Registration failed.');
      setDone(1);
    } finally {
      setBusy(false);
    }
  };

  return (
    <section
      style={{
        border: '1px solid ' + HAIR,
        borderRadius: '16px',
        background: '#fff',
        padding: '24px',
        marginBottom: '32px',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '6px' }}>
        <Globe size={16} strokeWidth={1.6} color={ACCENT} />
        <p
          style={{
            fontSize: '11px', fontWeight: 600, letterSpacing: '0.12em',
            textTransform: 'uppercase', color: MUTED, fontFamily: SANS, margin: 0,
          }}
        >
          Claim your storefront
        </p>
      </div>

      <h2
        style={{
          fontSize: '26px', fontFamily: SERIF, fontStyle: 'italic', fontWeight: 350,
          letterSpacing: '-0.03em', color: INK, margin: '0 0 6px',
        }}
      >
        {claimed ? 'Your shop has an address.' : 'Put this shop on a domain you own.'}
      </h2>

      <p style={{ fontSize: '13px', color: MUTED, fontFamily: SANS, margin: '0 0 18px', lineHeight: 1.6 }}>
        {claimed
          ? 'Run against name.com’s sandbox, so nothing is charged. Search, availability and registration complete there and the domain appears in the account. The sandbox registers a domain without provisioning a DNS zone behind it, so the record, subdomain and forwarding calls return 404 there. Same code path, same requests, and they land in production.'
          : 'Search name.com for a name that is actually available. Registration, DNS and forwarding run in one step.'}
      </p>

      {!claimed && (
        <form onSubmit={runSearch} style={{ display: 'flex', gap: '8px' }}>
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="your shop name"
            style={{
              flex: 1, padding: '11px 14px', borderRadius: '10px', border: '1px solid ' + HAIR,
              fontFamily: SANS, fontSize: '14px', color: INK, background: '#fff', outline: 'none',
            }}
          />
          <button
            type="submit"
            disabled={busy || !query.trim()}
            style={{
              padding: '11px 22px', background: INK, color: '#f7f5f3', border: 'none',
              borderRadius: '10px', fontFamily: SANS, fontSize: '14px', fontWeight: 600,
              cursor: busy || !query.trim() ? 'default' : 'pointer',
              opacity: busy || !query.trim() ? 0.5 : 1,
            }}
          >
            {busy && !options ? 'Searching' : 'Search'}
          </button>
        </form>
      )}

      {options && !claimed && (
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px', marginTop: '16px' }}>
          {options.slice(0, 8).map((d) => (
            <button
              key={d}
              onClick={() => claim(d)}
              disabled={busy}
              style={{
                padding: '8px 14px', borderRadius: '9999px', border: '1px solid ' + HAIR,
                background: '#fff', fontFamily: SANS, fontSize: '13px', color: INK,
                cursor: busy ? 'default' : 'pointer',
              }}
            >
              {d}
            </button>
          ))}
        </div>
      )}

      {done >= 0 && (
        <ul style={{ listStyle: 'none', padding: 0, margin: '18px 0 0' }}>
          {STEPS.map((s, i) => (
            <li
              key={s}
              style={{
                display: 'flex', alignItems: 'center', gap: '10px', padding: '4px 0',
                fontFamily: SANS, fontSize: '12.5px',
                color: i < done ? INK : MUTED,
                opacity: i <= done ? 1 : 0.35,
              }}
            >
              {(completed.length ? completed.some((c) => s.startsWith(c)) : i < done)
                ? <Check size={13} strokeWidth={2.4} color={ACCENT} />
                : <Circle size={9} strokeWidth={1.8} color={MUTED} />}
              <code style={{ fontSize: '12px' }}>{s}</code>
              {/*
                The reason comes from the server per step, rather than one caveat pasted onto
                every incomplete row. "production only" is true of the DNS calls, which 404
                because the sandbox registers a domain without provisioning a zone behind it.
                It is not true of a registration that failed because the name was already taken,
                and labelling that "production only" would hide a real error behind a caveat.
              */}
              {completed.length > 0 && !completed.some((c) => s.startsWith(c)) && (
                <span style={{ fontSize: '11px', color: MUTED }}>
                  {reasonFor(s)}
                </span>
              )}
            </li>
          ))}
        </ul>
      )}

      {claimed && (
        <p style={{ fontFamily: SERIF, fontStyle: 'italic', fontSize: '22px', color: ACCENT, margin: '16px 0 0' }}>
          {claimed}
        </p>
      )}

      {error && (
        <p style={{ marginTop: '14px', fontSize: '13px', color: ACCENT, fontFamily: SANS }}>{error}</p>
      )}
    </section>
  );
}
