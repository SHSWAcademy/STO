import { Search } from 'lucide-react';
import { cn } from '../../lib/utils.js';

/**
 * variant="dark"  — stone 다크 테마 (기본, 일반 유저 페이지)
 * variant="light" — 어드민 라이트 테마
 */
export function SearchInput({ value, onChange, placeholder = '검색...', className, variant = 'dark' }) {

  if (variant === 'light') {
    return (
      <div className={cn('flex items-center gap-2 bg-white border border-[#e0dace] rounded-lg px-3 py-2 flex-1', className)}>
        <Search className="w-3.5 h-3.5 text-[#b0a898] shrink-0" />
        <input
          type="text"
          placeholder={placeholder}
          value={value}
          onChange={e => onChange(e.target.value)}
          className="bg-transparent border-none outline-none text-sm w-full text-[#2a2820] placeholder:text-[#b0a898]"
        />
      </div>
    );
  }

  return (
    <div className={cn('relative', className)}>
      <Search
        className="absolute left-3 top-1/2 -translate-y-1/2 text-stone-muted pointer-events-none"
        size={15}
      />
      <input
        type="text"
        placeholder={placeholder}
        value={value}
        onChange={e => onChange(e.target.value)}
        className="bg-stone-bg border border-stone-border rounded-lg py-2 pl-9 pr-4 text-sm text-stone-text-primary placeholder:text-stone-muted outline-none focus:border-stone-gold transition-colors w-56"
      />
    </div>
  );
}
