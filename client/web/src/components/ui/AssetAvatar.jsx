import { useState } from 'react';
import { cn } from '../../lib/utils.js';
import { FILE_URLS } from '../../lib/config.js';

const SIZE_MAP = {
  sm: 'w-7 h-7 rounded-md text-[10px]',
  md: 'w-10 h-10 rounded-lg text-xs',
  lg: 'w-12 h-12 rounded-lg text-sm',
};

function resolveImageSrc(src) {
  if (!src) return null;
  if (/^(https?:)?\/\//.test(src) || src.startsWith('data:') || src.startsWith('blob:')) {
    return src;
  }
  if (src.startsWith('/')) return src;
  return `${FILE_URLS.imageBase}/${src}`;
}

/**
 * variant="dark"  — stone 다크 테마 (기본)
 * variant="light" — 어드민 라이트 테마
 */
export function AssetAvatar({ symbol, src, alt, size = 'md', variant = 'dark', className }) {
  const abbr = (symbol || '').slice(0, 2).toUpperCase();
  const [imageFailed, setImageFailed] = useState(false);
  const imageSrc = resolveImageSrc(src);

  if (imageSrc && !imageFailed) {
    return (
      <div className={cn(
        SIZE_MAP[size],
        'overflow-hidden border bg-stone-100 border-stone-200',
        className
      )}>
        <img
          src={imageSrc}
          alt={alt || symbol || 'asset'}
          className="h-full w-full object-cover"
          onError={() => setImageFailed(true)}
        />
      </div>
    );
  }

  if (variant === 'light') {
    return (
      <div className={cn(
        SIZE_MAP[size],
        'flex items-center justify-center font-bold border bg-stone-100 border-stone-200 text-stone-400',
        className
      )}>
        {abbr}
      </div>
    );
  }

  return (
    <div className={cn(
      SIZE_MAP[size],
      'flex items-center justify-center font-bold border bg-stone-100 border-stone-200 text-stone-400',
      className
    )}>
      {abbr}
    </div>
  );
}
