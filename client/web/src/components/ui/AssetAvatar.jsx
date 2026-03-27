import { cn } from '../../lib/utils.js';

const SIZE_MAP = {
  sm: 'w-7 h-7 rounded-md text-[10px]',
  md: 'w-10 h-10 rounded-lg text-xs',
  lg: 'w-12 h-12 rounded-lg text-sm',
};

/**
 * variant="dark"  — stone 다크 테마 (기본)
 * variant="light" — 어드민 라이트 테마
 */
export function AssetAvatar({ symbol, size = 'md', variant = 'dark', className }) {
  const abbr = (symbol || '').slice(0, 2).toUpperCase();

  if (variant === 'light') {
    return (
      <div className={cn(
        SIZE_MAP[size],
        'flex items-center justify-center font-bold border bg-[#f0ede4] border-[#e0dace] text-[#9a9080]',
        className
      )}>
        {abbr}
      </div>
    );
  }

  return (
    <div className={cn(
      SIZE_MAP[size],
      'flex items-center justify-center font-bold border bg-stone-elevated border-stone-border text-stone-muted',
      className
    )}>
      {abbr}
    </div>
  );
}
