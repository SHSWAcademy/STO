import { createContext, useContext, useEffect, useState } from 'react';
import { TOKENS as INITIAL_TOKENS, ADMIN_DISCLOSURES, ADMIN_NOTICES } from '../data/mock.js';
import { API_BASE_URL } from '../lib/config.js';

const API = API_BASE_URL;

// JWT payload에서 memberId(sub) 추출
function getMemberIdFromJwt(token) {
  try {
    return Number(JSON.parse(atob(token.split('.')[1])).sub);
  } catch {
    return null;
  }
}

const AppContext = createContext(null);

export function AppProvider({ children }) {
  const [user, setUser] = useState(null);
  const [likedTokenIds, setLikedTokenIds] = useState([]);
  const [tokens, setTokens] = useState(INITIAL_TOKENS);
  const [disclosures, setDisclosures] = useState(ADMIN_DISCLOSURES);
  const [notices, setNotices] = useState(ADMIN_NOTICES);
  const [guestBanner, setGuestBanner] = useState(null);
  const [loginOverlay, setLoginOverlay] = useState(null);

  async function fetchLikes(accessToken) {
    const res = await fetch(`${API}/api/likes`, {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    });

    if (!res.ok) throw new Error(`HTTP ${res.status}`);

    const data = await res.json();
    setLikedTokenIds(data.map((item) => item.tokenId));
  }

  async function login(email, password) {
    const res = await fetch(`${API}/api/auth/member/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password }),
    });

    if (!res.ok) throw new Error(`HTTP ${res.status}`);

    const data = await res.json();
    localStorage.setItem('token', data.accessToken);
    setUser({
      name: email,
      email,
      role: 'user',
      memberId: getMemberIdFromJwt(data.accessToken),
      accessToken: data.accessToken,
    });
    setGuestBanner(null);
    setLoginOverlay(null);
    try {
      await fetchLikes(data.accessToken);
    } catch (err) {
      console.error('[AppContext] likes load failed after login:', err);
      setLikedTokenIds([]);
    }
    return false;
  }

  async function loginAdmin(adminLoginId, password) {
    const res = await fetch(`${API}/api/auth/admin/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ adminLoginId, password }),
    });

    if (!res.ok) {
      throw new Error(`HTTP ${res.status}`);
    }

    const data = await res.json();
    if (data.userType !== 'ADMIN') {
      throw new Error(`Unexpected userType ${data.userType}`);
    }

    localStorage.setItem('token', data.accessToken);
    setUser({
      name: data.adminName ?? data.name ?? adminLoginId,
      email: data.email ?? adminLoginId,
      role: 'admin',
      accessToken: data.accessToken,
    });
    setGuestBanner(null);
    setLoginOverlay(null);
  }

  function logout() {
    localStorage.removeItem('token');
    setUser(null);
    setLikedTokenIds([]);
    setLoginOverlay(null);
  }

  function showGuestBanner(message) {
    setGuestBanner(message);
  }

  function dismissGuestBanner() {
    setGuestBanner(null);
  }

  function showLoginOverlay(options = {}) {
    setLoginOverlay({
      title: options.title ?? '로그인이 필요한 메뉴예요',
      message: options.message ?? '이 기능은 로그인 후 이용할 수 있어요.',
      from: options.from ?? '/',
    });
  }

  function hideLoginOverlay() {
    setLoginOverlay(null);
  }

  async function toggleLike(tokenId) {
    if (!user?.accessToken) return;

    const exists = likedTokenIds.includes(tokenId);

    if (exists) {
      const res = await fetch(`${API}/api/likes/${tokenId}`, {
        method: 'DELETE',
        headers: {
          Authorization: `Bearer ${user.accessToken}`,
        },
      });

      if (!res.ok) throw new Error(`HTTP ${res.status}`);

      setLikedTokenIds((prev) => prev.filter((id) => id !== tokenId));
      return;
    }

    const res = await fetch(`${API}/api/likes/${tokenId}`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${user.accessToken}`,
      },
    });

    if (!res.ok) throw new Error(`HTTP ${res.status}`);

    setLikedTokenIds((prev) => [...prev, tokenId]);
  }

  useEffect(() => {
    if (!user?.accessToken || user.role !== 'user') return;

    fetchLikes(user.accessToken).catch((err) => {
      console.error('[AppContext] likes load failed:', err);
      setLikedTokenIds([]);
    });
  }, [user]);

  return (
    <AppContext.Provider
      value={{
        user,
        login,
        loginAdmin,
        logout,
        likedTokenIds,
        toggleLike,
        tokens,
        setTokens,
        disclosures,
        setDisclosures,
        notices,
        setNotices,
        guestBanner,
        showGuestBanner,
        dismissGuestBanner,
        loginOverlay,
        showLoginOverlay,
        hideLoginOverlay,
      }}
    >
      {children}
    </AppContext.Provider>
  );
}

export function useApp() {
  return useContext(AppContext);
}
