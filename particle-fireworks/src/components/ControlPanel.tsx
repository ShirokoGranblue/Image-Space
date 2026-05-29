import { useState } from 'react';
import { Slider } from '@/components/ui/slider';
import type { ExplosionConfig, TrailConfig, PresetName } from '@/types/particle';
import { presets } from '@/lib/presets';

interface Props {
  presetName: PresetName;
  explosion: ExplosionConfig;
  trail: TrailConfig;
  onPresetChange: (name: PresetName) => void;
  onExplosionChange: (config: ExplosionConfig) => void;
  onTrailChange: (config: TrailConfig) => void;
}

export default function ControlPanel({
  presetName,
  explosion,
  trail,
  onPresetChange,
  onExplosionChange,
  onTrailChange,
}: Props) {
  const [expanded, setExpanded] = useState(true);

  return (
    <div className="fixed left-3 top-3 z-10 max-w-[260px]">
      <div className="bg-black/70 backdrop-blur-md rounded-xl border border-white/10 p-4 text-white shadow-2xl">
        {/* Header */}
        <button
          onClick={() => setExpanded(!expanded)}
          className="w-full flex items-center justify-between gap-2 mb-3 text-xs font-medium text-white/70 hover:text-white transition-colors"
        >
          <span>控制面板</span>
          <span className="text-[10px]">{expanded ? '收起 ▲' : '展开 ▼'}</span>
        </button>

        {/* Presets */}
        <div className="flex gap-1.5 mb-3 flex-wrap">
          {(Object.keys(presets) as PresetName[]).map((name) => (
            <button
              key={name}
              onClick={() => onPresetChange(name)}
              className={`px-2 py-1 rounded-lg text-xs font-medium transition-all ${
                presetName === name
                  ? 'bg-white/20 text-white ring-1 ring-white/30'
                  : 'bg-white/5 text-white/50 hover:bg-white/10 hover:text-white/80'
              }`}
            >
              {presets[name].icon} {presets[name].label}
            </button>
          ))}
        </div>

        {expanded && (
          <>
            {/* Explosion Settings */}
            <div className="mb-3">
              <h4 className="text-[10px] font-semibold uppercase tracking-wider text-white/40 mb-2">
                爆炸参数
              </h4>
              <div className="space-y-2">
                <SliderRow
                  label="粒子数量"
                  value={explosion.particleCount}
                  min={10}
                  max={200}
                  step={5}
                  onChange={(v) =>
                    onExplosionChange({ ...explosion, particleCount: v })
                  }
                />
                <SliderRow
                  label="爆发速度"
                  value={explosion.speed}
                  min={50}
                  max={600}
                  step={10}
                  onChange={(v) => onExplosionChange({ ...explosion, speed: v })}
                />
                <SliderRow
                  label="重力"
                  value={explosion.gravity}
                  min={-100}
                  max={400}
                  step={10}
                  onChange={(v) =>
                    onExplosionChange({ ...explosion, gravity: v })
                  }
                />
                <SliderRow
                  label="粒子大小"
                  value={explosion.size}
                  min={1}
                  max={12}
                  step={0.5}
                  onChange={(v) => onExplosionChange({ ...explosion, size: v })}
                />
                <SliderRow
                  label="环数"
                  value={explosion.rings}
                  min={1}
                  max={4}
                  step={1}
                  onChange={(v) =>
                    onExplosionChange({ ...explosion, rings: v })
                  }
                />
              </div>
            </div>

            {/* Trail Settings */}
            <div>
              <h4 className="text-[10px] font-semibold uppercase tracking-wider text-white/40 mb-2">
                拖尾参数
              </h4>
              <div className="space-y-2">
                <SliderRow
                  label="拖尾密度"
                  value={trail.density}
                  min={0}
                  max={2}
                  step={0.1}
                  onChange={(v) => onTrailChange({ ...trail, density: v })}
                />
                <SliderRow
                  label="拖尾大小"
                  value={trail.size}
                  min={0.5}
                  max={8}
                  step={0.5}
                  onChange={(v) => onTrailChange({ ...trail, size: v })}
                />
                <SliderRow
                  label="衰减速度"
                  value={trail.decay}
                  min={0.1}
                  max={1.5}
                  step={0.05}
                  onChange={(v) => onTrailChange({ ...trail, decay: v })}
                />
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}

function SliderRow({
  label,
  value,
  min,
  max,
  step,
  onChange,
}: {
  label: string;
  value: number;
  min: number;
  max: number;
  step: number;
  onChange: (v: number) => void;
}) {
  return (
    <div className="flex items-center gap-2">
      <span className="text-[10px] text-white/50 w-14 shrink-0 text-right">
        {label}
      </span>
      <Slider
        value={[value]}
        onValueChange={([v]) => onChange(v)}
        min={min}
        max={max}
        step={step}
        className="flex-1"
      />
      <span className="text-[10px] text-white/40 w-8 text-right tabular-nums">
        {value}
      </span>
    </div>
  );
}
