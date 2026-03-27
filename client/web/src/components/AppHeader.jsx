import { useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { ShieldCheck, Search, Bell } from 'lucide-react';
import { TOKENS } from '../data/mock.js';
import { useApp } from '../context/AppContext.jsx';
import { cn } from '../lib/utils.js';

// 원본 MainLayout의 navItems와 동일한 경로
const NAV_ITEMS = [
  { label: '홈',    path: '/',           end: true },
  { label: '내 계좌', path: '/portfolio' },
  { label: '관심',   path: '/watchlist' },
  { label: '공시',   path: '/disclosure' },
  { label: '공지',   path: '/notice' },
];

export function AppHeader() {
  const [searchQuery, setSearchQuery]   = useState('');
  const [showDropdown, setShowDropdown] = useState(false);
  const navigate = useNavigate();
  const { user, logout } = useApp();

  const searchResults = searchQuery.trim()
    ? TOKENS.filter(t =>
        t.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        t.id.toLowerCase().includes(searchQuery.toLowerCase())
      )
    : [];

  function handleSelectToken(token) {
    setSearchQuery('');
    setShowDropdown(false);
    navigate('/trading');
  }

  return (
    <header className="h-16 bg-stone-bg border-b border-stone-border flex items-center justify-between px-8 sticky top-0 z-50">
      {/* 로고 + 네비 */}
      <div className="flex items-center gap-8">
        <Link to="/" className="flex items-center gap-2 group">
          <div className="w-8 h-8 bg-stone-gold rounded-lg flex items-center justify-center shadow-lg shadow-stone-gold/20 group-hover:scale-105 transition-transform">
            <ShieldCheck className="text-white" size={20} />
          </div>
          <h1 className="text-lg font-black text-stone-text-primary tracking-tighter">STONE</h1>
        </Link>

        <nav className="flex items-center gap-6">
          {NAV_ITEMS.map(item => (
            <NavLink
              key={item.path}
              to={item.path}
              end={item.end}
              className={({ isActive }) => cn(
                'text-sm font-bold transition-colors hover:text-stone-gold',
                isActive ? 'text-stone-text-primary' : 'text-stone-text-secondary'
              )}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </div>

      {/* 검색 */}
      <div className="flex items-center gap-4 flex-1 max-w-md mx-8">
        <div className="relative w-full group">
          <Search
            className="absolute left-4 top-1/2 -translate-y-1/2 text-stone-text-secondary group-focus-within:text-stone-gold transition-colors z-10"
            size={16}
          />
          <input
            type="text"
            value={searchQuery}
            onChange={e => { setSearchQuery(e.target.value); setShowDropdown(true); }}
            onFocus={() => setShowDropdown(true)}
            onBlur={() => setTimeout(() => setShowDropdown(false), 150)}
            placeholder="종목명 검색..."
            className="w-full bg-stone-surface border border-stone-border rounded-xl py-2 pl-10 pr-4 text-xs text-stone-text-primary outline-none focus:border-stone-gold transition-all"
          />
          {showDropdown && searchResults.length > 0 && (
            <div className="absolute top-full left-0 right-0 mt-1 bg-stone-surface border border-stone-border rounded-xl shadow-xl overflow-hidden z-50">
              {searchResults.map(token => (
                <button
                  key={token.id}
                  onMouseDown={() => handleSelectToken(token)}
                  className="w-full flex items-center gap-3 px-4 py-3 hover:bg-stone-bg transition-colors text-left"
                >
                  <div className="w-8 h-8 rounded-lg overflow-hidden shrink-0 border border-stone-border bg-stone-elevated flex items-center justify-center text-xs font-black text-stone-muted">
                    {token.symbol.slice(0, 2)}
                  </div>
                  <div>
                    <p className="text-xs font-black text-stone-text-primary">{token.name}</p>
                    <p className="text-[10px] font-bold text-stone-text-secondary">
                      {token.id} · {(token.price ?? 0).toLocaleString()}원
                    </p>
                  </div>
                </button>
              ))}
            </div>
          )}
          {showDropdown && searchQuery.trim() && searchResults.length === 0 && (
            <div className="absolute top-full left-0 right-0 mt-1 bg-stone-surface border border-stone-border rounded-xl shadow-xl z-50 px-4 py-3">
              <p className="text-xs text-stone-text-secondary font-bold">검색 결과가 없습니다.</p>
            </div>
          )}
        </div>
      </div>

      {/* 우측: 알림 + 유저 */}
      <div className="flex items-center gap-3">
        <button className="p-2 text-stone-text-secondary hover:text-stone-gold transition-colors relative">
          <Bell size={20} />
          <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-stone-buy rounded-full border-2 border-stone-bg" />
        </button>
        <Link
          to="/portfolio"
          className="w-8 h-8 rounded-lg flex items-center justify-center text-white text-xs font-black bg-stone-gold hover:scale-105 transition-transform"
        >
          {user?.name?.[0] ?? '?'}
        </Link>
        <button
          onClick={() => { logout(); navigate('/'); }}
          className="text-xs font-bold text-stone-text-secondary hover:text-stone-text-primary transition-colors"
        >
          로그아웃
        </button>
      </div>
    </header>
  );
}
