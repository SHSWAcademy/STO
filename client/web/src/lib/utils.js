// 원본 cn() 유틸리티 — clsx + tailwind-merge 역할
export function cn(...classes) {
  return classes.filter(Boolean).join(' ');
}
