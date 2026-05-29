import type { Particle, ExplosionConfig, TrailConfig } from '@/types/particle';

let nextId = 0;
const MAX_PARTICLES = 500;

export function createExplosion(x: number, y: number, config: ExplosionConfig): Particle[] {
  const particles: Particle[] = [];
  const perRing = Math.floor(config.particleCount / config.rings);

  for (let ring = 0; ring < config.rings; ring++) {
    const ringSpeed = config.speed * (1 - ring * 0.15);
    const ringSize = config.size * (1 - ring * 0.1);

    for (let i = 0; i < perRing; i++) {
      const angle = (Math.PI * 2 * i) / perRing + (Math.random() - 0.5) * 0.5;
      const speed = ringSpeed * (0.7 + Math.random() * 0.6);
      const angleOffset = (Math.random() - 0.5) * 0.3;

      particles.push({
        id: nextId++,
        x,
        y,
        vx: Math.cos(angle + angleOffset) * speed,
        vy: Math.sin(angle + angleOffset) * speed,
        life: 1,
        maxLife: 0.6 + Math.random() * 0.8,
        size: ringSize + (Math.random() - 0.5) * config.sizeVariance,
        color: config.colors[Math.floor(Math.random() * config.colors.length)],
        type: 'explosion',
        gravity: config.gravity,
      });
    }
  }

  return particles;
}

export function createTrail(x: number, y: number, config: TrailConfig): Particle[] {
  const particles: Particle[] = [];
  const count = Math.floor(config.density * (0.5 + Math.random()));

  for (let i = 0; i < count; i++) {
    particles.push({
      id: nextId++,
      x: x + (Math.random() - 0.5) * 8,
      y: y + (Math.random() - 0.5) * 8,
      vx: (Math.random() - 0.5) * 30,
      vy: (Math.random() - 0.5) * 30,
      life: 1,
      maxLife: config.decay * (0.5 + Math.random() * 0.5),
      size: config.size * (0.5 + Math.random()),
      color: config.color,
      type: 'trail',
      gravity: 0,
    });
  }

  return particles;
}

export function updateParticles(particles: Particle[], dt: number): Particle[] {
  const updated = particles
    .map((p) => ({
      ...p,
      x: p.x + p.vx * dt,
      y: p.y + p.vy * dt,
      vy: p.vy + p.gravity * dt,
      life: p.life - dt / p.maxLife,
    }))
    .filter((p) => p.life > 0);

  if (updated.length > MAX_PARTICLES) {
    return updated.slice(updated.length - MAX_PARTICLES);
  }

  return updated;
}
