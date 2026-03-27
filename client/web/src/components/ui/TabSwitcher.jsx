import { cn } from '../../lib/utils.js';

/**
 * variant="dark"  — DashboardPage (bg-stone-bg 컨테이너, bg-stone-surface 활성)
 * variant="pill"  — DisclosurePage / NoticePage (텍스트 필터 탭)
 * variant="light" — 어드민 라이트 테마 (bg-stone-100 컨테이너, bg-white 활성)
 */
export function TabSwitcher({ items, active, onChange, variant = 'dark', className }) {

  if (variant === 'pill') {
    return (
      <div className={cn('flex gap-1 overflow-x-auto scrollbar-hide', className)}>
        {items.map(item => (
          <button
            key={item}
            onClick={() => onChange(item)}
            className={cn(
              'px-4 py-2 rounded-md text-xs font-semibold transition-colors whitespace-nowrap',
              active === item
                ? 'bg-stone-text-primary text-stone-bg'
                : 'text-stone-muted hover:text-stone-text-primary hover:bg-stone-surface'
            )}
          >
            {item}
          </button>
        ))}
      </div>
    );
  }

  if (variant === 'light') {
    return (
      <div className={cn('flex items-center gap-0.5 p-1 bg-stone-100 border border-stone-200 rounded-lg w-fit', className)}>
        {items.map(item => {
          const id    = typeof item === 'string' ? item : item.id;
          const label = typeof item === 'string' ? item : item.label;
          const Icon  = typeof item === 'object' ? item.icon : null;
          const isActive = id === active;
          return (
            <button
              key={id}
              onClick={() => onChange(id)}
              className={cn(
                'flex items-center gap-1.5 px-4 py-2 rounded-md text-xs font-semibold transition-colors whitespace-nowrap',
                isActive
                  ? 'bg-white text-stone-800 border border-stone-200'
                  : 'text-stone-400 hover:text-[#5a5248]'
              )}
            >
              {Icon && (
                <Icon
                  size={13}
                  className={isActive ? 'text-brand-blue' : 'text-[#b0a898]'}
                />
              )}
              {label}
            </button>
          );
        })}
      </div>
    );
  }

  // default: dark
  return (
    <div className={cn('flex gap-0.5 bg-stone-bg p-0.5 rounded-lg border border-stone-surface', className)}>
      {items.map(item => (
        <button
          key={item}
          onClick={() => onChange(item)}
          className={cn(
            'px-3 py-1.5 rounded-md text-xs font-semibold transition-colors whitespace-nowrap',
            active === item
              ? 'bg-stone-surface text-stone-text-primary border border-stone-border'
              : 'text-stone-muted hover:text-stone-text-secondary'
          )}
        >
          {item}
        </button>
      ))}
    </div>
  );
}
