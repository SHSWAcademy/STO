import { Link, useLocation, useNavigate, Outlet } from 'react-router-dom';
import {
  LayoutDashboard, Users, FileText, Settings, LogOut,
  Search, TrendingUp, ShieldCheck, DollarSign, BarChart3, Database,
} from 'lucide-react';
import { cn } from '../../lib/utils.js';
import { useApp } from '../../context/AppContext.jsx';

const MENU_ITEMS = [
  { icon: LayoutDashboard, label: '대시보드',        path: '/admin' },
  { icon: Users,           label: '사용자 관리',      path: '/admin/users' },
  { icon: TrendingUp,      label: '자산 관리',        path: '/admin/assets' },
  { icon: BarChart3,       label: '플랫폼 수익/보유', path: '/admin/revenue' },
  { icon: DollarSign,      label: '배당 관리',        path: '/admin/dividends' },
  { icon: FileText,        label: '공시/공지 관리',   path: '/admin/content' },
  { icon: Database,        label: '로그 관리',        path: '/admin/logs' },
  { icon: Settings,        label: '시스템 설정',      path: '/admin/settings' },
];

export function AdminLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useApp();

  function handleLogout() {
    logout();
    navigate('/');
  }

  // exact match for /admin, prefix match for sub-pages
  function isActive(path) {
    if (path === '/admin') return location.pathname === '/admin';
    return location.pathname.startsWith(path);
  }

  return (
    <div className="flex h-screen bg-[#f7f5f0] font-sans">
      {/* Sidebar */}
      <aside className="w-64 bg-[#2a2820] text-white flex flex-col shrink-0">
        <div className="p-6 flex items-center gap-3 border-b border-white/10">
          <div className="w-8 h-8 bg-[#4a72a0] rounded-lg flex items-center justify-center">
            <ShieldCheck className="w-5 h-5 text-white" />
          </div>
          <span className="font-black text-lg tracking-tight">STO ADMIN</span>
        </div>

        <nav className="flex-1 p-4 space-y-1 overflow-y-auto">
          {MENU_ITEMS.map(item => (
            <Link
              key={item.path}
              to={item.path}
              className={cn(
                'flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-bold transition-all',
                isActive(item.path)
                  ? 'bg-[#4a72a0] text-white shadow-lg shadow-[#4a72a0]/20'
                  : 'text-white/60 hover:text-white hover:bg-white/5',
              )}
            >
              <item.icon className="w-5 h-5" />
              {item.label}
            </Link>
          ))}
        </nav>

        <div className="p-4 border-t border-white/10">
          <button
            onClick={handleLogout}
            className="flex items-center gap-3 px-4 py-3 w-full rounded-xl text-sm font-bold text-[#b85450] hover:bg-[#b85450]/10 transition-all"
          >
            <LogOut className="w-5 h-5" />
            로그아웃
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <main className="flex-1 flex flex-col overflow-hidden">
        {/* Header */}
        <header className="h-16 bg-white border-b border-[#e0dace] flex items-center justify-between px-8 shrink-0">
          <div className="flex items-center gap-4 bg-[#e0dace] px-4 py-2 rounded-xl w-96">
            <Search className="w-4 h-4 text-[#9a9080]" />
            <input
              type="text"
              placeholder="사용자, 거래번호 검색..."
              className="bg-transparent border-none outline-none text-sm w-full"
            />
          </div>
          <div className="flex items-center gap-3 pl-6 border-l border-[#e0dace]">
            <div className="text-right">
              <p className="text-sm font-black text-[#2a2820]">{user?.name || '관리자'}</p>
              <p className="text-[10px] font-bold text-[#9a9080]">Super Admin</p>
            </div>
            <div className="w-10 h-10 bg-[#e0dace] rounded-full flex items-center justify-center font-black text-[#9a9080]">
              {user?.name?.[0] ?? 'A'}
            </div>
          </div>
        </header>

        {/* Page Content */}
        <div className="flex-1 overflow-y-auto p-8">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
