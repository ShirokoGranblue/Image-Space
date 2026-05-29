import { useEffect, useRef, useState, useCallback } from 'react';
import type { Particle, ExplosionConfig, TrailConfig } from '@/types/particle';
import { createExplosion, createTrail, updateParticles } from '@/lib/engine';

interface Props {
  explosionConfig: ExplosionConfig;
  trailConfig: TrailConfig;
}

export default function ParticleCanvas({ explosionConfig, trailConfig }: Props) {
  const [, setFrame] = useState(0);
  const particlesRef = useRef<Particle[]>([]);
  const mouseRef = useRef({ x: -1000, y: -1000, onScreen: false });
  const explosionRef = useRef(explosionConfig);
  const trailRef = useRef(trailConfig);
  const svgRef = useRef<SVGSVGElement>(null);

  explosionRef.current = explosionConfig;
  trailRef.current = trailConfig;

  useEffect(() => {
    let running = true;
    let lastTime = performance.now();

    const loop = (time: number) => {
      if (!running) return;
      const dt = Math.min((time - lastTime) / 1000, 0.05);
      lastTime = time;

      if (mouseRef.current.onScreen) {
        const trails = createTrail(mouseRef.current.x, mouseRef.current.y, trailRef.current);
        const updated = updateParticles([...particlesRef.current, ...trails], dt);
        particlesRef.current = updated;
      } else {
        particlesRef.current = updateParticles(particlesRef.current, dt);
      }

      setFrame((f) => f + 1);
      requestAnimationFrame(loop);
    };

    requestAnimationFrame(loop);
    return () => {
      running = false;
    };
  }, []);

  const handleClick = useCallback((e: React.MouseEvent<SVGSVGElement>) => {
    const rect = svgRef.current?.getBoundingClientRect();
    if (!rect) return;
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;
    const explosion = createExplosion(x, y, explosionRef.current);
    particlesRef.current = [...particlesRef.current, ...explosion];
  }, []);

  const handleMouseMove = useCallback((e: React.MouseEvent<SVGSVGElement>) => {
    const rect = svgRef.current?.getBoundingClientRect();
    if (!rect) return;
    mouseRef.current.x = e.clientX - rect.left;
    mouseRef.current.y = e.clientY - rect.top;
    mouseRef.current.onScreen = true;
  }, []);

  const handleMouseLeave = useCallback(() => {
    mouseRef.current.onScreen = false;
  }, []);

  return (
    <svg
      ref={svgRef}
      className="fixed inset-0 w-full h-full cursor-crosshair select-none"
      style={{ background: '#0a0a14' }}
      onClick={handleClick}
      onMouseMove={handleMouseMove}
      onMouseLeave={handleMouseLeave}
    >
      <defs>
        <filter id="glow">
          <feGaussianBlur stdDeviation="1.5" result="blur" />
          <feMerge>
            <feMergeNode in="blur" />
            <feMergeNode in="SourceGraphic" />
          </feMerge>
        </filter>
        <filter id="glow-strong">
          <feGaussianBlur stdDeviation="3" result="blur" />
          <feMerge>
            <feMergeNode in="blur" />
            <feMergeNode in="SourceGraphic" />
          </feMerge>
        </filter>
      </defs>

      {particlesRef.current.map((p) => (
        <circle
          key={p.id}
          cx={p.x}
          cy={p.y}
          r={Math.max(0.3, p.size * p.life)}
          fill={p.color}
          opacity={Math.min(1, p.life * 1.2)}
          filter={p.type === 'explosion' ? 'url(#glow)' : undefined}
        />
      ))}
    </svg>
  );
}
