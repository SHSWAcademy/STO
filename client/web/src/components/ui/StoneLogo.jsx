// StoneLogo — STONE 브랜드 로고 아이콘
// 미니멀 사각 분할: 다이아몬드를 수평선으로 분할, STO 조각화 의미
//
// props:
//   size  — px 크기 (default: 32)
//   className — 추가 클래스

export function StoneLogo({ size = 32, className = '' }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 32 32"
      fill="none"
      className={className}
      aria-label="STONE 로고"
    >
      {/* 다이아몬드 외곽 */}
      <path d="M16 2 L30 16 L16 30 L2 16 Z" stroke="#c9a84c" strokeWidth="2" fill="none" />
      {/* 위 삼각형 (골드 fill) */}
      <path d="M16 2 L30 16 L2 16 Z" fill="#c9a84c" fillOpacity="0.25" />
      {/* 수평 분할선 */}
      <line x1="2" y1="16" x2="30" y2="16" stroke="#c9a84c" strokeWidth="1.5" />
    </svg>
  );
}

/* ─── 대안 B안 (키스톤) — 필요 시 이 컴포넌트로 교체 ───────────────
export function StoneLogo({ size = 32, className = '' }) {
  return (
    <svg width={size} height={size} viewBox="0 0 32 32" fill="none" className={className} aria-label="STONE 로고">
      <path d="M6 26 L11 8 L21 8 L26 26 Z" stroke="#c9a84c" strokeWidth="2" fill="none" strokeLinejoin="round" />
      <path d="M6 26 L11 8 L21 8 L26 26 Z" fill="#c9a84c" fillOpacity="0.2" strokeLinejoin="round" />
      <line x1="9" y1="17" x2="23" y2="17" stroke="#c9a84c" strokeWidth="1.5" />
    </svg>
  );
}
─────────────────────────────────────────────────────────────────── */
