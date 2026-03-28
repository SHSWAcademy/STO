import { cn } from '../../lib/utils.js';

/**
 * 다크 stone 테마:
 *   "buy"     — 매수 상승 (붉은 계열)
 *   "sell"    — 매도 하락 (파란 계열)
 *   "muted"   — 비활성 중립
 *   "gold"    — 골드 포인트
 *
 * 라이트 어드민 테마:
 *   "success" — 정상 / 완료
 *   "danger"  — 오류 / 정지
 *   "warning" — 경고 / 대기
 *   "neutral" — 중립 / 기타
 *   "blue"    — 정보 / 관리자
 */
const STYLES = {
  buy:     'bg-brand-red-light text-brand-red',
  sell:    'bg-brand-blue-light text-brand-blue',
  muted:   'bg-stone-100 text-stone-400',
  gold:    'bg-[#fef6dc] text-[#a07828]',
  success: 'bg-[#e8f4ee] text-[#3d7a58]',
  danger:  'bg-brand-red-light text-brand-red-dk',
  warning: 'bg-[#fef6dc] text-[#a07828]',
  neutral: 'bg-stone-100 text-stone-500',
  blue:    'bg-[#e8f0fa] text-brand-blue-dk',
};

export function Badge({ children, variant = 'muted', className }) {
  return (
    <span className={cn(
      'inline-flex items-center text-[10px] font-semibold px-2 py-0.5 rounded tracking-wide',
      STYLES[variant] ?? STYLES.muted,
      className
    )}>
      {children}
    </span>
  );
}
