import { useState, useEffect, useRef, useCallback } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { Search, Bell, CheckCheck } from 'lucide-react';
import { TOKENS } from '../data/mock.js';
import { useApp } from '../context/AppContext.jsx';
import { useAlarmSocket } from '../hooks/useAlarmSocket.js';
import { cn } from '../lib/utils.js';
import { StoneLogo } from './ui/StoneLogo.jsx';
import api from '../lib/api.js';

const NAV_ITEMS = [
  { label: '홈',    path: '/',           end: true },
  { label: '내 계좌', path: '/portfolio' },
  { label: '관심',   path: '/watchlist' },
  { label: '공시',   path: '/disclosure' },
  { label: '공지',   path: '/notice' },
];

// alarmType → 내계좌 탭 이동 대상
function resolveAlarmTab(alarmType) {
  if (alarmType === 'ORDER_FILLED' || alarmType === 'ORDER_PARTIAL') return 'orders';
  if (alarmType === 'DIVIDEND') return 'dividends';
  return null;
}

// 알람 생성 시각 → "방금 전 / N분 전 / N시간 전 / 날짜" 포맷
function formatAlarmTime(createdAt) {
  const diff = Date.now() - new Date(createdAt).getTime();
  const min  = Math.floor(diff / 60000);
  if (min < 1)   return '방금 전';
  if (min < 60)  return `${min}분 전`;
  const hr = Math.floor(min / 60);
  if (hr  < 24)  return `${hr}시간 전`;
  return new Date(createdAt).toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' });
}

export function AppHeader() {
  const [searchQuery, setSearchQuery]       = useState('');
  const [showDropdown, setShowDropdown]     = useState(false);
  const [showAlarms, setShowAlarms]         = useState(false);
  const [alarms, setAlarms]                 = useState([]);
  const alarmDropdownRef                    = useRef(null);
  const navigate                            = useNavigate();
  const { user, logout }                    = useApp();

  const unreadCount = alarms.filter(a => !a.isRead).length;

  // ── REST: 초기 알람 목록 로드 ──────────────────────────────
  const loadAlarms = useCallback(async () => {
    if (!user) return;
    try {
      const res = await api.get('/api/alarm');
      setAlarms(res.data);
    } catch (e) {
      console.warn('[Alarm] 목록 로드 실패', e);
    }
  }, [user]);

  useEffect(() => {
    loadAlarms();
  }, [loadAlarms]);

  // ── WebSocket: 구독 직후 스냅샷 + 실시간 신규 알람 ─────────
  useAlarmSocket({
    memberId: user?.memberId,
    token:    user?.accessToken,
    // 구독 직후 백엔드가 미읽음 목록을 배열로 전송
    onSnapshot: (snapshot) => {
      setAlarms(prev => {
        // 기존 목록에 스냅샷을 머지 (alarmId 기준 중복 제거, 최신순)
        const existingIds = new Set(prev.map(a => a.alarmId));
        const newItems    = snapshot.filter(a => !existingIds.has(a.alarmId));
        return [...newItems, ...prev].sort(
          (a, b) => new Date(b.createdAt) - new Date(a.createdAt),
        );
      });
    },
    // 체결/배당 이벤트 발생 시 단건 수신
    onNewAlarm: (alarm) => {
      setAlarms(prev => [alarm, ...prev].slice(0, 50));
    },
  });

  // ── 드롭다운 외부 클릭 시 닫기 ────────────────────────────
  useEffect(() => {
    function handleClickOutside(e) {
      if (alarmDropdownRef.current && !alarmDropdownRef.current.contains(e.target)) {
        setShowAlarms(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // ── 알람 단건 읽음 + 페이지 이동 ──────────────────────────
  async function handleAlarmClick(alarm) {
    if (!alarm.isRead) {
      try {
        await api.patch(`/api/alarm/${alarm.alarmId}/read`);
        setAlarms(prev =>
          prev.map(a => a.alarmId === alarm.alarmId ? { ...a, isRead: true } : a),
        );
      } catch (e) {
        console.warn('[Alarm] 읽음 처리 실패', e);
      }
    }
    setShowAlarms(false);
    const tab = resolveAlarmTab(alarm.alarmType);
    if (tab) navigate('/portfolio', { state: { tab } });
  }

  // ── 전체 읽음 ─────────────────────────────────────────────
  async function handleMarkAllAsRead() {
    try {
      await api.patch('/api/alarm/read/all');
      setAlarms(prev => prev.map(a => ({ ...a, isRead: true })));
    } catch (e) {
      console.warn('[Alarm] 전체 읽음 실패', e);
    }
  }

  // ── 검색 ──────────────────────────────────────────────────
  const searchResults = searchQuery.trim()
    ? TOKENS.filter(t =>
        t.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        t.id.toLowerCase().includes(searchQuery.toLowerCase()),
      )
    : [];

  function handleSelectToken(token) {
    setSearchQuery('');
    setShowDropdown(false);
    navigate('/trading');
  }

  return (
    <header className="h-16 bg-white border-b border-stone-200 flex items-center justify-between px-8 sticky top-0 z-50">
      {/* 로고 + 네비 */}
      <div className="flex items-center gap-8">
        <Link to="/" className="flex items-center gap-2.5 group">
          <StoneLogo size={32} className="group-hover:scale-105 transition-transform shrink-0" />
          <h1 className="text-lg font-black text-stone-800 tracking-tighter">STONE</h1>
        </Link>

        <nav className="flex items-center gap-6">
          {NAV_ITEMS.map(item => (
            <NavLink
              key={item.path}
              to={item.path}
              end={item.end}
              className={({ isActive }) => cn(
                'text-sm font-bold transition-colors hover:text-stone-800',
                isActive ? 'text-stone-800' : 'text-stone-500',
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
            className="absolute left-4 top-1/2 -translate-y-1/2 text-stone-400 group-focus-within:text-stone-800 transition-colors z-10"
            size={16}
          />
          <input
            type="text"
            value={searchQuery}
            onChange={e => { setSearchQuery(e.target.value); setShowDropdown(true); }}
            onFocus={() => setShowDropdown(true)}
            onBlur={() => setTimeout(() => setShowDropdown(false), 150)}
            placeholder="종목명 검색..."
            className="w-full bg-stone-100 border border-stone-200 rounded-xl py-2 pl-10 pr-4 text-xs text-stone-800 outline-none focus:border-stone-800 transition-all"
          />
          {showDropdown && searchResults.length > 0 && (
            <div className="absolute top-full left-0 right-0 mt-1 bg-white border border-stone-200 rounded-xl shadow-xl overflow-hidden z-50">
              {searchResults.map(token => (
                <button
                  key={token.id}
                  onMouseDown={() => handleSelectToken(token)}
                  className="w-full flex items-center gap-3 px-4 py-3 hover:bg-stone-100 transition-colors text-left"
                >
                  <div className="w-8 h-8 rounded-lg overflow-hidden shrink-0 border border-stone-200 bg-stone-100 flex items-center justify-center text-xs font-black text-stone-400">
                    {token.symbol.slice(0, 2)}
                  </div>
                  <div>
                    <p className="text-xs font-black text-stone-800">{token.name}</p>
                    <p className="text-[10px] font-bold text-stone-500">
                      {token.id} · {(token.price ?? 0).toLocaleString()}원
                    </p>
                  </div>
                </button>
              ))}
            </div>
          )}
          {showDropdown && searchQuery.trim() && searchResults.length === 0 && (
            <div className="absolute top-full left-0 right-0 mt-1 bg-white border border-stone-200 rounded-xl shadow-xl z-50 px-4 py-3">
              <p className="text-xs text-stone-500 font-bold">검색 결과가 없습니다.</p>
            </div>
          )}
        </div>
      </div>

      {/* 우측: 알림(로그인 시만) + 유저 */}
      <div className="flex items-center gap-3">

        {/* 알람 벨 — 로그인한 사용자만 표시 */}
        {user && (
          <div className="relative" ref={alarmDropdownRef}>
            <button
              onClick={() => { setShowAlarms(prev => { if (!prev) loadAlarms(); return !prev; }); }}
              className="p-2 text-stone-400 hover:text-stone-800 transition-colors relative"
            >
              <Bell size={20} />
              {unreadCount > 0 && (
                <span className="absolute top-1 right-1 min-w-[16px] h-4 px-0.5 bg-red-500 text-white text-[10px] font-black rounded-full flex items-center justify-center border-2 border-white">
                  {unreadCount > 99 ? '99+' : unreadCount}
                </span>
              )}
            </button>

            {/* 알람 드롭다운 */}
            {showAlarms && (
              <div className="absolute right-0 top-full mt-2 w-80 bg-white border border-stone-200 rounded-2xl shadow-2xl overflow-hidden z-50">
                {/* 헤더 */}
                <div className="flex items-center justify-between px-4 py-3 border-b border-stone-100">
                  <span className="text-sm font-black text-stone-800">
                    알림
                    {unreadCount > 0 && (
                      <span className="ml-1.5 text-xs font-bold text-red-500">{unreadCount}</span>
                    )}
                  </span>
                  {unreadCount > 0 && (
                    <button
                      onClick={handleMarkAllAsRead}
                      className="flex items-center gap-1 text-[11px] font-bold text-stone-400 hover:text-stone-700 transition-colors"
                    >
                      <CheckCheck size={13} />
                      전체 읽음
                    </button>
                  )}
                </div>

                {/* 알람 목록 — 미읽음 먼저, 읽음 나중 (각 그룹 내 최신순) */}
                <ul className="max-h-80 overflow-y-auto divide-y divide-stone-100">
                  {alarms.length === 0 ? (
                    <li className="px-4 py-8 text-center text-xs font-bold text-stone-400">
                      새로운 알림이 없습니다.
                    </li>
                  ) : (
                    [...alarms]
                      .sort((a, b) => {
                        if (a.isRead !== b.isRead) return a.isRead ? 1 : -1;
                        return new Date(b.createdAt) - new Date(a.createdAt);
                      })
                      .map(alarm => (
                        <li key={alarm.alarmId}>
                          <button
                            onClick={() => handleAlarmClick(alarm)}
                            className={cn(
                              'w-full text-left px-4 py-3 transition-colors',
                              !alarm.isRead
                                ? 'bg-blue-50/60 hover:bg-blue-50'
                                : 'bg-stone-100 hover:bg-stone-200',
                            )}
                          >
                            <div className="flex items-start gap-2">
                              {/* 미읽음 도트 */}
                              <span className={cn(
                                'mt-1.5 w-1.5 h-1.5 rounded-full shrink-0',
                                !alarm.isRead ? 'bg-red-500' : 'bg-transparent',
                              )} />
                              <div className="flex-1 min-w-0">
                                <p className={cn(
                                  'text-xs leading-relaxed break-words',
                                  alarm.isRead ? 'text-stone-400 font-medium' : 'text-stone-800 font-bold',
                                )}>
                                  {alarm.message}
                                </p>
                                <p className={cn(
                                  'text-[10px] font-bold mt-0.5',
                                  alarm.isRead ? 'text-stone-400' : 'text-stone-400',
                                )}>
                                  {formatAlarmTime(alarm.createdAt)}
                                </p>
                              </div>
                            </div>
                          </button>
                        </li>
                      ))
                  )}
                </ul>
              </div>
            )}
          </div>
        )}

        {user ? (
          <>
            <Link
              to="/portfolio"
              className="w-8 h-8 rounded-lg flex items-center justify-center text-white text-xs font-black bg-stone-800 hover:scale-105 transition-transform"
            >
              {user.name?.[0] ?? '?'}
            </Link>
            <button
              onClick={() => { logout(); navigate('/'); }}
              className="text-xs font-bold text-stone-500 hover:text-stone-800 transition-colors"
            >
              로그아웃
            </button>
          </>
        ) : (
          <Link
            to="/login"
            className="text-xs font-bold text-stone-500 hover:text-stone-800 transition-colors"
          >
            로그인
          </Link>
        )}
      </div>
    </header>
  );
}
