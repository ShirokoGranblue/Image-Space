export interface Particle {
  id: number;
  x: number;
  y: number;
  vx: number;
  vy: number;
  life: number;
  maxLife: number;
  size: number;
  color: string;
  type: 'explosion' | 'trail';
  gravity: number;
}

export interface ExplosionConfig {
  particleCount: number;
  speed: number;
  gravity: number;
  size: number;
  sizeVariance: number;
  colors: string[];
  rings: number;
}

export interface TrailConfig {
  density: number;
  size: number;
  decay: number;
  color: string;
}

export type PresetName = 'fireworks' | 'neon' | 'stars' | 'firefly' | 'confetti';

export interface Preset {
  name: PresetName;
  label: string;
  icon: string;
  explosion: ExplosionConfig;
  trail: TrailConfig;
}

export interface Params {
  explosion: ExplosionConfig;
  trail: TrailConfig;
}
