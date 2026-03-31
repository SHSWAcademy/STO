import { createContext, useContext, useState } from 'react';
import { TOKENS as INITIAL_TOKENS, ADMIN_DISCLOSURES, ADMIN_NOTICES } from '../data/mock.js';

// AppContext — 전역 공유 상태
// user: null = 비로그인, object = 로그인
// API 연결 시: login/logout을 실제 API 호출로 교체

const AppContext = createContext(null);

export function AppProvider({ children }) {
  const [user, setUser]               = useState(null);
  const [watchlist, setWatchlist]     = useState(['SEOULST', 'SONGDORE']);
  const [tokens, setTokens]           = useState(INITIAL_TOKENS);
  const [disclosures, setDisclosures] = useState(ADMIN_DISCLOSURES);
  const [notices, setNotices]         = useState(ADMIN_NOTICES);

  // mock 로그인: ADMIN/ADMIN → 관리자, 그 외 → 일반 유저
  function login(email, password) {
    const isAdmin =
      email.trim().toUpperCase() === 'ADMIN' &&
      password.trim().toUpperCase() === 'ADMIN';

    setUser({
      name:  isAdmin ? '관리자' : '홍길동',
      email: isAdmin ? 'admin@sto.exchange' : 'demo@sto.exchange',
      role:  isAdmin ? 'admin' : 'user',
    });

    return isAdmin; // 호출자가 admin 여부로 navigate 결정
  }

  function logout() {
    setUser(null);
  }

  function toggleWatchlist(id) {
    setWatchlist(prev =>
      prev.includes(id) ? prev.filter(i => i !== id) : [...prev, id]
    );
  }

  return (
    <AppContext.Provider value={{
      user, login, logout,
      watchlist, toggleWatchlist,
      tokens, setTokens,
      disclosures, setDisclosures,
      notices, setNotices,
    }}>
      {children}
    </AppContext.Provider>
  );
}

export function useApp() {
  return useContext(AppContext);
}
