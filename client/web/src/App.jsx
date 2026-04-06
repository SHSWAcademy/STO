import { Routes, Route, Navigate, Outlet } from 'react-router-dom';
import { AppProvider, useApp }  from './context/AppContext.jsx';
import { AppHeader }            from './components/AppHeader.jsx';
import { AuthPage }             from './pages/AuthPage.jsx';
import { TradingPage }          from './pages/TradingPage.jsx';
import { DashboardPage }        from './pages/DashboardPage.jsx';
import { MyAccountPage }        from './pages/MyAccountPage.jsx';
import { WatchlistPage }        from './pages/WatchlistPage.jsx';
import { DisclosurePage }       from './pages/DisclosurePage.jsx';
import { NoticePage }           from './pages/NoticePage.jsx';
import { MockupPage }           from './pages/MockupPage.jsx';

// Admin
import { AdminLayout }          from './pages/admin/AdminLayout.jsx';
import { AdminDashboard }       from './pages/admin/AdminDashboard.jsx';
import { UserManagement }       from './pages/admin/UserManagement.jsx';
import { AssetManagement }      from './pages/admin/AssetManagement.jsx';
import { PlatformRevenue }      from './pages/admin/PlatformRevenue.jsx';
import { DividendManagement }   from './pages/admin/DividendManagement.jsx';
import { ContentManagement }    from './pages/admin/ContentManagement.jsx';
import { SystemLogs }           from './pages/admin/SystemLogs.jsx';
import { SystemSettings }       from './pages/admin/SystemSettings.jsx';

// 일반 페이지 공통 패딩 래퍼
function PageWrapper({ children }) {
  return (
    <div className="max-w-7xl mx-auto w-full p-8">
      {children}
    </div>
  );
}

// 일반 유저 레이아웃 (AppHeader + Outlet)
function MainLayout() {
  return (
    <div className="min-h-screen bg-stone-100 text-stone-800">
      <AppHeader />
      <main>
        <Outlet />
      </main>
    </div>
  );
}

// MockupPage 전용 독립 레이아웃 (비로그인 접근 가능)
function MockupLayout() {
  return (
    <div className="min-h-screen bg-stone-100 text-stone-800">
      <AppHeader />
      <main>
        <MockupPage />
      </main>
    </div>
  );
}

// 비로그인 시 AuthPage 반환하는 가드
function Auth({ children }) {
  const { user } = useApp();
  return user ? children : <AuthPage />;
}

function AppContent() {
  return (
    <Routes>
      {/* 관리자 라우트 */}
      <Route path="/admin" element={<Auth><AdminLayout /></Auth>}>
        <Route index             element={<AdminDashboard />} />
        <Route path="users"     element={<UserManagement />} />
        <Route path="assets"    element={<AssetManagement />} />
        <Route path="revenue"   element={<PlatformRevenue />} />
        <Route path="dividends" element={<DividendManagement />} />
        <Route path="content"   element={<ContentManagement />} />
        <Route path="logs"      element={<SystemLogs />} />
        <Route path="settings"  element={<SystemSettings />} />
      </Route>

      {/* 비로그인 접근 가능 — 독립 최상위 라우트 */}
      <Route path="/mockup/:tokenId" element={<MockupLayout />} />

      {/* 로그인 필요 라우트 */}
      <Route element={<MainLayout />}>
        <Route path="/"           element={<Auth><PageWrapper><DashboardPage /></PageWrapper></Auth>} />
        <Route path="/trading"    element={<Auth><TradingPage /></Auth>} />
        <Route path="/portfolio"  element={<Auth><PageWrapper><MyAccountPage /></PageWrapper></Auth>} />
        <Route path="/watchlist"  element={<Auth><PageWrapper><WatchlistPage /></PageWrapper></Auth>} />
        <Route path="/disclosure" element={<Auth><PageWrapper><DisclosurePage /></PageWrapper></Auth>} />
        <Route path="/notice"     element={<Auth><PageWrapper><NoticePage /></PageWrapper></Auth>} />
        <Route path="*"           element={<Auth><Navigate to="/" replace /></Auth>} />
      </Route>
    </Routes>
  );
}

export default function App() {
  return (
    <AppProvider>
      <AppContent />
    </AppProvider>
  );
}
