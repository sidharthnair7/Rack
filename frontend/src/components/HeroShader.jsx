import { useEffect, useRef } from 'react';

/**
 * HeroShader — ambient fluid gradient rendered via raw WebGL.
 *
 * Color stops: paper (#f7f5f3), warm sand (#efe6e2), rose (#e3cdd2).
 * Slow, silk-like motion. Falls back silently if WebGL unavailable.
 * Respects prefers-reduced-motion (static frame).
 * Resolution capped at 0.5× DPR for performance.
 */

const VERTEX_SRC = `
  attribute vec2 a_position;
  void main() {
    gl_Position = vec4(a_position, 0.0, 1.0);
  }
`;

const FRAGMENT_SRC = `
  precision mediump float;
  uniform vec2 u_resolution;
  uniform float u_time;

  // Simplex-ish noise (compact 2D)
  vec3 mod289(vec3 x) { return x - floor(x * (1.0/289.0)) * 289.0; }
  vec2 mod289(vec2 x) { return x - floor(x * (1.0/289.0)) * 289.0; }
  vec3 permute(vec3 x) { return mod289(((x*34.0)+1.0)*x); }

  float snoise(vec2 v) {
    const vec4 C = vec4(0.211324865405187, 0.366025403784439,
                       -0.577350269189626, 0.024390243902439);
    vec2 i  = floor(v + dot(v, C.yy));
    vec2 x0 = v -   i + dot(i, C.xx);
    vec2 i1 = (x0.x > x0.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);
    vec4 x12 = x0.xyxy + C.xxzz;
    x12.xy -= i1;
    i = mod289(i);
    vec3 p = permute(permute(i.y + vec3(0.0, i1.y, 1.0)) + i.x + vec3(0.0, i1.x, 1.0));
    vec3 m = max(0.5 - vec3(dot(x0,x0), dot(x12.xy,x12.xy), dot(x12.zw,x12.zw)), 0.0);
    m = m*m; m = m*m;
    vec3 x_ = 2.0 * fract(p * C.www) - 1.0;
    vec3 h = abs(x_) - 0.5;
    vec3 ox = floor(x_ + 0.5);
    vec3 a0 = x_ - ox;
    m *= 1.79284291400159 - 0.85373472095314 * (a0*a0 + h*h);
    vec3 g;
    g.x = a0.x * x0.x + h.x * x0.y;
    g.yz = a0.yz * x12.xz + h.yz * x12.yw;
    return 130.0 * dot(m, g);
  }

  void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;

    // 3-octave flowing noise
    float t = u_time * 0.15;
    float n = 0.0;
    n += 0.5 * snoise(uv * 1.5 + vec2(t * 0.4, t * 0.3));
    n += 0.25 * snoise(uv * 3.0 + vec2(-t * 0.5, t * 0.7));
    n += 0.125 * snoise(uv * 6.0 + vec2(t * 0.6, -t * 0.4));
    n = n * 0.5 + 0.5; // normalize to 0–1

    // Palette: paper → warm sand → rose
    vec3 colA = vec3(0.969, 0.961, 0.953); // #f7f5f3
    vec3 colB = vec3(0.937, 0.902, 0.886); // #efe6e2
    vec3 colC = vec3(0.890, 0.804, 0.824); // #e3cdd2

    vec3 color = mix(colA, colB, smoothstep(0.0, 0.5, n));
    color = mix(color, colC, smoothstep(0.5, 1.0, n) * 0.4);

    // Subtle radial vignette
    float vignette = 1.0 - smoothstep(0.3, 0.9, length(uv - 0.5) * 1.2);
    color *= 0.97 + 0.03 * vignette;

    gl_FragColor = vec4(color, 1.0);
  }
`;

export default function HeroShader() {
  const canvasRef = useRef(null);
  const rafRef = useRef(null);
  const glRef = useRef(null);

  const prefersReducedMotion =
    typeof window !== 'undefined' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    // Try WebGL2, fall back to WebGL1
    let gl = canvas.getContext('webgl2');
    if (!gl) gl = canvas.getContext('webgl');
    if (!gl) {
      // No WebGL — hide canvas, show nothing
      canvas.style.display = 'none';
      return;
    }
    glRef.current = gl;

    // Compile shaders
    function compileShader(src, type) {
      const s = gl.createShader(type);
      gl.shaderSource(s, src);
      gl.compileShader(s);
      if (!gl.getShaderParameter(s, gl.COMPILE_STATUS)) {
        console.warn('Shader compile error:', gl.getShaderInfoLog(s));
        gl.deleteShader(s);
        return null;
      }
      return s;
    }

    const vs = compileShader(VERTEX_SRC, gl.VERTEX_SHADER);
    const fs = compileShader(FRAGMENT_SRC, gl.FRAGMENT_SHADER);
    if (!vs || !fs) {
      canvas.style.display = 'none';
      return;
    }

    const program = gl.createProgram();
    gl.attachShader(program, vs);
    gl.attachShader(program, fs);
    gl.linkProgram(program);
    if (!gl.getProgramParameter(program, gl.LINK_STATUS)) {
      canvas.style.display = 'none';
      return;
    }
    gl.useProgram(program);

    // Full-screen quad
    const positions = new Float32Array([-1,-1, 1,-1, -1,1, 1,1]);
    const buf = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, buf);
    gl.bufferData(gl.ARRAY_BUFFER, positions, gl.STATIC_DRAW);

    const aPos = gl.getAttribLocation(program, 'a_position');
    gl.enableVertexAttribArray(aPos);
    gl.vertexAttribPointer(aPos, 2, gl.FLOAT, false, 0, 0);

    const uRes = gl.getUniformLocation(program, 'u_resolution');
    const uTime = gl.getUniformLocation(program, 'u_time');

    // Resize handler — cap at 0.5× DPR
    const resize = () => {
      const dpr = Math.min(window.devicePixelRatio || 1, 2) * 0.5;
      canvas.width = canvas.clientWidth * dpr;
      canvas.height = canvas.clientHeight * dpr;
      gl.viewport(0, 0, canvas.width, canvas.height);
    };
    resize();
    window.addEventListener('resize', resize);

    // Render loop
    const start = performance.now();
    const render = (now) => {
      const t = (now - start) / 1000;
      gl.uniform2f(uRes, canvas.width, canvas.height);
      gl.uniform1f(uTime, prefersReducedMotion ? 0 : t);
      gl.drawArrays(gl.TRIANGLE_STRIP, 0, 4);

      if (!prefersReducedMotion) {
        rafRef.current = requestAnimationFrame(render);
      }
    };
    rafRef.current = requestAnimationFrame(render);

    return () => {
      cancelAnimationFrame(rafRef.current);
      window.removeEventListener('resize', resize);
    };
  }, [prefersReducedMotion]);

  return (
    <canvas
      ref={canvasRef}
      style={{
        position: 'absolute',
        inset: 0,
        width: '100%',
        height: '100%',
        opacity: 0.45,
        pointerEvents: 'none',
        zIndex: 0,
      }}
    />
  );
}
