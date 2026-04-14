import { useState, useEffect, useRef, useCallback } from 'react';
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom';
import { Search, Bell, CheckCheck, X } from 'lucide-react';
import { TOKENS } from '../data/mock.js';
import { useApp } from '../context/AppContext.jsx';
import { useAlarmSocket } from '../hooks/useAlarmSocket.js';
import { cn } from '../lib/utils.js';
import { StoneLogo } from './ui/StoneLogo.jsx';
import { Modal } from './ui/Modal.jsx';
import api from '../lib/api.js';

const PROTECTED_PATHS = new Set(['/portfolio', '/watchlist']);
const NOTICE_BANNER_DISMISS_KEY = 'dismissedNoticeBannerId';

const NAV_ITEMS = [
  { label: '홈', path: '/', end: true },
  { label: '내 계좌', path: '/portfolio' },
  { label: '관심', path: '/watchlist' },
  { label: '공시', path: '/disclosure' },
  { label: '공지', path: '/notice' },
];

function resolveAlarmTab(alarmType) {
  if (alarmType === 'ORDER_FILLED' || alarmType === 'ORDER_PARTIAL') return 'orders';
  if (alarmType === 'DIVIDEND') return 'dividends';
  return null;
}

function formatAlarmTime(createdAt) {
  const diff = Date.now() - new Date(createdAt).getTime();
  const min = Math.floor(diff / 60000);
  if (min < 1) return '방금 전';
  if (min < 60) return `${min}분 전`;
  const hr = Math.floor(min / 60);
  if (hr < 24) return `${hr}시간 전`;
  return new Date(createdAt).toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' });
}

function getDismissedNoticeId() {
  try {
    return sessionStorage.getItem(NOTICE_BANNER_DISMISS_KEY);
  } catch {
    return null;
  }
}

function setDismissedNoticeId(noticeId) {
  try {
    sessionStorage.setItem(NOTICE_BANNER_DISMISS_KEY, String(noticeId));
  } catch {
    console.warn('[NoticeBanner] 세션 저장 실패');
  }
}

export function AppHeader() {
  const [searchQuery, setSearchQuery] = useState('');
  const [showDropdown, setShowDropdown] = useState(false);
  const [showAlarms, setShowAlarms] = useState(false);
  const [alarms, setAlarms] = useState([]);
  const [noticeBanner, setNoticeBanner] = useState(null);
  const alarmDropdownRef = useRef(null);
  const navigate = useNavigate();
  const location = useLocation();
  const {
    user,
    logout,
    guestBanner,
    dismissGuestBanner,
    loginOverlay,
    showLoginOverlay,
    hideLoginOverlay,
  } = useApp();

  const unreadCount = alarms.filter((alarm) => !alarm.isRead).length;

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

  useEffect(() => {
    let mounted = true;
    async function loadNoticeBanner() {
      try {
        const { data } = await api.get('/api/notice', {
          params: { page: 0, size: 1 },
        });
        if (!mounted) return;
        const latestNotice = Array.isArray(data?.content) ? data.content[0] : null;
        if (!latestNotice) {
          setNoticeBanner(null);
          return;
        }
        if (getDismissedNoticeId() === String(latestNotice.noticeId)) {
          setNoticeBanner(null);
          return;
        }
        setNoticeBanner(latestNotice);
      } catch (error) {
        if (!mounted) return;
        console.warn('[NoticeBanner] 최신 공지 로드 실패', error);
        setNoticeBanner(null);
      }
    }
    loadNoticeBanner();
    return () => { mounted = false; };
  }, []);

  useAlarmSocket({
    memberId: user?.memberId,
    token: user?.accessToken,
    onSnapshot: (snapshot) => {
      setAlarms((prev) => {
        const existingIds = new Set(prev.map((alarm) => alarm.alarmId));
        const newItems = snapshot.filter((alarm) => !existingIds.has(alarm.alarmId));
        return [...newItems, ...prev].sort(
            (a, b) => new Date(b.createdAt) - new Date(a.createdAt),
        );
      });
    },
    onNewAlarm: (alarm) => {
      setAlarms((prev) => [alarm, ...prev].slice(0, 50));
    },
  });

  useEffect(() => {
    function handleClickOutside(event) {
      if (alarmDropdownRef.current && !alarmDropdownRef.current.contains(event.target)) {
        setShowAlarms(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  async function handleAlarmClick(alarm) {
    if (!alarm.isRead) {
      try {
        await api.patch(`/api/alarm/${alarm.alarmId}/read`);
        setAlarms((prev) =>
            prev.map((item) => (item.alarmId === alarm.alarmId ? { ...item, isRead: true } : item)),
        );
      } catch (e) {
        console.warn('[Alarm] 읽음 처리 실패', e);
      }
    }
    setShowAlarms(false);
    const tab = resolveAlarmTab(alarm.alarmType);
    if (tab) {
      navigate('/portfolio', { state: { tab } });
    }
  }

  async function handleMarkAllAsRead() {
    try {
      await api.patch('/api/alarm/read/all');
      setAlarms((prev) => prev.map((alarm) => ({ ...alarm, isRead: true })));
    } catch (e) {
      console.warn('[Alarm] 전체 읽음 실패', e);
    }
  }

  const searchResults = searchQuery.trim()
      ? TOKENS.filter((token) =>
          token.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
          token.id.toLowerCase().includes(searchQuery.toLowerCase()),
      )
      : [];

  function handleSelectToken() {
    setSearchQuery('');
    setShowDropdown(false);
    navigate('/trading');
  }

  function handleProtectedNavigation(path) {
    if (!user && PROTECTED_PATHS.has(path)) {
      showLoginOverlay({
        from: path,
        title: '로그인이 필요한 메뉴예요',
        message: '내 계좌와 관심 종목은 로그인 후 확인할 수 있어요.',
      });
      return;
    }
    navigate(path);
  }

  function handleLoginNavigation() {
    navigate('/login', { state: { from: location.pathname } });
  }

  function handleNoticeBannerClose() {
    if (noticeBanner?.noticeId != null) {
      setDismissedNoticeId(noticeBanner.noticeId);
    }
    setNoticeBanner(null);
  }

  function handleNoticeBannerView() {
    if (noticeBanner?.noticeId != null) {
      setDismissedNoticeId(noticeBanner.noticeId);
    }
    setNoticeBanner(null);
    navigate('/notice');
  }

  return (
      <>
        <header className="sticky top-0 z-50 border-b border-stone-200 bg-white">
          <div className="flex h-16 items-center justify-between px-8">
            <div className="flex items-center gap-8">
              <Link to="/" className="group flex items-center gap-2.5">
                <StoneLogo size={32} className="shrink-0 transition-transform group-hover:scale-105" />
                <h1 className="text-lg font-black tracking-tighter text-stone-800">STONE</h1>
              </Link>

              <nav className="flex items-center gap-6">
                {NAV_ITEMS.map((item) => (
                    !user && PROTECTED_PATHS.has(item.path) ? (
                        <button
                            key={item.path}
                            type="button"
                            onClick={() => handleProtectedNavigation(item.path)}
                            className="text-sm font-bold text-stone-500 transition-colors hover:text-stone-800"
                        >
                          {item.label}
                        </button>
                    ) : (
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
                    )
                ))}
              </nav>
            </div>

            <div className="mx-8 flex max-w-md flex-1 items-center gap-4">
              <div className="group relative w-full">
                <Search className="absolute left-4 top-1/2 z-10 -translate-y-1/2 text-stone-400 group-focus-within:text-stone-800" size={16} />
                <input
                    type="text"
                    value={searchQuery}
                    onChange={(e) => { setSearchQuery(e.target.value); setShowDropdown(true); }}
                    onFocus={() => setShowDropdown(true)}
                    onBlur={() => setTimeout(() => setShowDropdown(false), 150)}
                    placeholder="종목명 검색.."
                    className="w-full rounded-xl border border-stone-200 bg-stone-100 py-2 pl-10 pr-4 text-xs text-stone-800 outline-none focus:border-stone-800"
                />
                {showDropdown && searchResults.length > 0 && (
                    <div className="absolute left-0 right-0 top-full z-50 mt-1 overflow-hidden rounded-xl border border-stone-200 bg-white shadow-xl">
                      {searchResults.map((token) => (
                          <button key={token.id} onMouseDown={handleSelectToken} className="flex w-full items-center gap-3 px-4 py-3 text-left hover:bg-stone-100">
                            <div className="flex h-8 w-8 items-center justify-center rounded-lg border border-stone-200 bg-stone-100 text-xs font-black text-stone-400">
                              {token.symbol.slice(0, 2)}
                            </div>
                            <div>
                              <p className="text-xs font-black text-stone-800">{token.name}</p>
                              <p className="text-[10px] font-bold text-stone-500">{token.id} · {token.price?.toLocaleString()}원</p>
                            </div>
                          </button>
                      ))}
                    </div>
                )}
              </div>
            </div>

            <div className="flex items-center gap-3">
              {user && (
                  <div className="relative" ref={alarmDropdownRef}>
                    <button
                        onClick={() => setShowAlarms(prev => { if (!prev) loadAlarms(); return !prev; })}
                        className="relative p-2 text-stone-400 hover:text-stone-800 transition-colors"
                    >
                      <Bell size={20} />
                      {unreadCount > 0 && (
                          <span className="absolute top-1 right-1 min-w-[16px] h-4 px-0.5 bg-red-500 text-white text-[10px] font-black rounded-full flex items-center justify-center border-2 border-white">
                      {unreadCount > 99 ? '99+' : unreadCount}
                    </span>
                      )}
                    </button>

                    {showAlarms && (
                        <div className="absolute right-0 top-full z-50 mt-2 w-80 overflow-hidden rounded-2xl border border-stone-200 bg-white shadow-2xl">
                          <div className="flex items-center justify-between border-b border-stone-100 px-4 py-3">
                            <span className="text-sm font-black text-stone-800">알림 {unreadCount > 0 && <span className="ml-1.5 text-xs font-bold text-red-500">{unreadCount}</span>}</span>
                            {unreadCount > 0 && (
                                <button onClick={handleMarkAllAsRead} className="flex items-center gap-1 text-[11px] font-bold text-stone-400 hover:text-stone-700">
                                  <CheckCheck size={13} /> 전체 읽음
                                </button>
                            )}
                          </div>
                          <ul className="max-h-80 divide-y divide-stone-100 overflow-y-auto">
                            {alarms.length === 0 ? (
                                <li className="px-4 py-8 text-center text-xs font-bold text-stone-400">새로운 알림이 없습니다.</li>
                            ) : (
                                alarms.map((alarm) => (
                                    <li key={alarm.alarmId}>
                                      <button
                                          onClick={() => handleAlarmClick(alarm)}
                                          className={cn("w-full px-4 py-3 text-left hover:bg-stone-50 transition-colors", !alarm.isRead ? "bg-blue-50/60" : "bg-white")}
                                      >
                                        <div className="flex items-start gap-2">
                                          <span className={cn("mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full", !alarm.isRead ? "bg-red-500" : "bg-transparent")} />
                                          <div className="min-w-0 flex-1">
                                            <p className={cn("break-words text-xs leading-relaxed", alarm.isRead ? "font-medium text-stone-400" : "font-bold text-stone-800")}>{alarm.message}</p>
                                            <p className="mt-0.5 text-[10px] font-bold text-stone-400">{formatAlarmTime(alarm.createdAt)}</p>
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
                    <Link to="/portfolio" className="flex h-8 w-8 items-center justify-center rounded-lg bg-stone-800 text-xs font-black text-white hover:scale-105 transition-transform">
                      {user.name?.[0] ?? '?'}
                    </Link>
                    <button onClick={() => { logout(); navigate('/'); }} className="text-xs font-bold text-stone-500 hover:text-stone-800">로그아웃</button>
                  </>
              ) : (
                  <button onClick={handleLoginNavigation} className="text-xs font-bold text-stone-500 hover:text-stone-800">로그인</button>
              )}
            </div>
          </div>

          {noticeBanner && (
              <div className="flex items-center justify-center gap-3 border-t border-stone-200 bg-stone-900 px-12 py-3 text-white">
                <p className="text-sm font-medium leading-relaxed">
                  <span className="mr-2 rounded-full border border-white/15 bg-white/10 px-2 py-0.5 text-[11px] font-black uppercase tracking-[0.18em] text-white/75">Notice</span>
                  {noticeBanner.noticeTitle}
                </p>
                <div className="flex items-center justify-center gap-2">
                  <button onClick={handleNoticeBannerView} className="rounded-full bg-white px-3 py-1 text-[11px] font-black text-stone-900 hover:bg-stone-100">공지 보기</button>
                  <button onClick={handleNoticeBannerClose} className="rounded-full p-1 text-white/70 hover:bg-white/10 hover:text-white"><X size={15} /></button>
                </div>
              </div>
          )}

          {!user && guestBanner && (
              <div className="flex items-center justify-center gap-3 border-t border-stone-200 bg-stone-900 px-12 py-3 text-white">
                <p className="text-sm font-medium leading-relaxed">{guestBanner}</p>
                <div className="flex items-center justify-center gap-2">
                  <button onClick={handleLoginNavigation} className="rounded-full bg-white px-3 py-1 text-[11px] font-black text-stone-900 hover:bg-stone-100">로그인</button>
                  <button onClick={dismissGuestBanner} className="rounded-full p-1 text-white/70 hover:bg-white/10 hover:text-white"><X size={15} /></button>
                </div>
              </div>
          )}
        </header>

        <Modal isOpen={!user && !!loginOverlay} onClose={hideLoginOverlay} title={loginOverlay?.title}>
          <div className="space-y-5 p-6">
            <p className="text-sm font-medium leading-relaxed text-stone-600">{loginOverlay?.message}</p>
            <div className="flex gap-3">
              <button
                  onClick={() => { const from = loginOverlay?.from ?? location.pathname; hideLoginOverlay(); navigate('/login', { state: { from } }); }}
                  className="flex-1 rounded-xl bg-stone-800 px-4 py-3 text-sm font-black text-white hover:bg-black"
              >
                로그인하기
              </button>
              <button onClick={hideLoginOverlay} className="flex-1 rounded-xl border border-stone-200 bg-white px-4 py-3 text-sm font-bold text-stone-500 hover:bg-stone-100">닫기</button>
            </div>
          </div>
        </Modal>
      </>
  );
}