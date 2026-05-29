import { useState, useCallback } from 'react';
import ParticleCanvas from '@/components/ParticleCanvas';
import ControlPanel from '@/components/ControlPanel';
import { presets } from '@/lib/presets';
import type { PresetName, ExplosionConfig, TrailConfig } from '@/types/particle';

function App() {
  const [presetName, setPresetName] = useState<PresetName>('fireworks');
  const [explosion, setExplosion] = useState<ExplosionConfig>(presets.fireworks.explosion);
  const [trail, setTrail] = useState<TrailConfig>(presets.fireworks.trail);

  const handlePresetChange = useCallback((name: PresetName) => {
    setPresetName(name);
    setExplosion({ ...presets[name].explosion });
    setTrail({ ...presets[name].trail });
  }, []);

  return (
    <div className="relative w-screen h-screen overflow-hidden bg-[#0a0a14]">
      <ParticleCanvas explosionConfig={explosion} trailConfig={trail} />
      <ControlPanel
        presetName={presetName}
        explosion={explosion}
        trail={trail}
        onPresetChange={handlePresetChange}
        onExplosionChange={setExplosion}
        onTrailChange={setTrail}
      />

      {/* Hint */}
      <div className="fixed bottom-4 left-1/2 -translate-x-1/2 z-10 pointer-events-none">
        <p className="text-white/25 text-xs font-medium">
          点击画面产生爆炸 · 移动鼠标产生拖尾
        </p>
      </div>
    </div>
  );
}

export default App;
