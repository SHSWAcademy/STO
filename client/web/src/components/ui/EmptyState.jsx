import { cn } from '../../lib/utils.js';

export function EmptyState({ message = '데이터가 없습니다.', light = false, className }) {
  return (
    <div className={cn(
      'flex flex-col items-center justify-center py-14 text-center',
      light
        ? 'rounded-lg border border-[#e8e4dc] bg-[#faf9f7]'
        : 'rounded-lg border border-stone-200 bg-stone-100',
      className
    )}>
      <div className={cn(
        'w-7 h-7 rounded-md flex items-center justify-center mb-3 text-base font-light select-none',
        light ? 'bg-stone-100 text-[#c0b8b0]' : 'bg-stone-200 text-stone-400'
      )}>
        —
      </div>
      <p className={cn(
        'text-sm font-medium',
        light ? 'text-stone-400' : 'text-stone-400'
      )}>
        {message}
      </p>
    </div>
  );
}
