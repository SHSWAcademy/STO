import { useState } from 'react';
import { Settings, Activity, Save, RefreshCw, CheckCircle2, DollarSign, Percent } from 'lucide-react';
import { cn } from '../../lib/utils.js';

export function SystemSettings() {
  const [isSaving, setIsSaving]       = useState(false);
  const [saveSuccess, setSaveSuccess] = useState(false);

  function handleSave() {
    setIsSaving(true);
    setTimeout(() => {
      setIsSaving(false);
      setSaveSuccess(true);
      setTimeout(() => setSaveSuccess(false), 3000);
    }, 1000);
  }

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-stone-800">시스템 설정</h1>
          <p className="text-sm text-stone-400">플랫폼 운영 및 거래 시스템의 핵심 설정을 관리합니다.</p>
        </div>
        <button onClick={handleSave} disabled={isSaving}
          className="flex items-center gap-2 px-8 py-3 bg-stone-800 text-white text-sm font-semibold rounded-md hover:bg-black transition-colors disabled:opacity-60">
          {isSaving ? <RefreshCw className="w-5 h-5 animate-spin" /> : <Save className="w-5 h-5" />}
          설정 저장하기
        </button>
      </div>

      {saveSuccess && (
        <div className="bg-brand-green-light border border-brand-green-light rounded-md p-4 flex items-center gap-3 text-brand-green text-sm font-medium">
          <CheckCircle2 className="w-5 h-5" />
          모든 설정이 성공적으로 저장되었습니다.
        </div>
      )}

      <div className="grid lg:grid-cols-4 gap-8">
        {/* Sidebar */}
        <div className="lg:col-span-1 space-y-2">
          <button className="w-full flex items-center gap-3 px-6 py-4 rounded-lg text-sm font-semibold transition-colors border bg-white border-stone-200 text-stone-800">
            <Activity size={18} className="text-brand-blue" />
            거래 및 정산 설정
          </button>
        </div>

        {/* Content */}
        <div className="lg:col-span-3 space-y-8">
          <div className="bg-white border border-stone-200 rounded-lg p-8 space-y-8">
            <h3 className="text-sm font-semibold text-stone-800 uppercase tracking-widest border-b border-stone-200 pb-4 flex items-center gap-2">
              <DollarSign size={16} className="text-brand-red" />
              거래 수수료 및 배당 정산 설정
            </h3>

            <div className="grid grid-cols-2 gap-8">
              {[
                { label: '기본 거래 수수료 (%)',    defaultValue: '0.05', step: '0.01', suffix: '%' },
                { label: '배당금 원천징수 세율 (%)', defaultValue: '15.4', step: '0.1',  suffix: '%' },
                { label: '배당금 정산 일자 (매월)',  defaultValue: '10',   step: '1',    suffix: '일', min: '1', max: '31' },
                { label: '배당금 지급일 (매월)',     defaultValue: '20',   step: '1',    suffix: '일', min: '1', max: '31' },
              ].map(({ label, defaultValue, step, suffix, min, max }) => (
                <div key={label} className="space-y-1.5">
                  <label className="text-[10px] font-semibold text-stone-400 uppercase tracking-widest ml-1">{label}</label>
                  <div className="relative">
                    <input
                      type="number"
                      step={step}
                      defaultValue={defaultValue}
                      min={min}
                      max={max}
                      className="w-full bg-stone-100 border border-stone-200 rounded-xl px-4 py-3 text-sm text-stone-800 font-bold outline-none focus:border-brand-blue transition-all"
                    />
                    <span className="absolute right-4 top-1/2 -translate-y-1/2 text-[10px] font-black text-stone-400">
                      {suffix === '%' ? <Percent className="w-4 h-4" /> : suffix}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
