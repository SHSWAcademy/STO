import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Eye, EyeOff, LockKeyhole, ShieldCheck } from 'lucide-react';
import { useApp } from '../../context/AppContext.jsx';
import { StoneLogo } from '../../components/ui/StoneLogo.jsx';

export function AdminLoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { loginAdmin } = useApp();

  const [adminLoginId, setAdminLoginId] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');

    if (!adminLoginId.trim() || !password.trim()) {
      setError('관리자 아이디와 비밀번호를 입력해 주세요.');
      return;
    }

    try {
      setIsSubmitting(true);
      await loginAdmin(adminLoginId.trim(), password);
      navigate(location.pathname || '/admin-console', { replace: true });
    } catch (err) {
      console.error('[AdminLogin] login failed:', err);
      setError('관리자 로그인에 실패했습니다. 계정 정보를 확인해 주세요.');
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main className="min-h-screen bg-stone-100 text-stone-800">
      <div className="mx-auto flex min-h-screen w-full max-w-6xl items-center justify-center px-6 py-10">
        <div className="grid w-full overflow-hidden rounded-lg border border-stone-200 bg-white shadow-xl lg:grid-cols-[1fr_440px]">
          <section className="flex min-h-[560px] flex-col justify-between bg-stone-800 p-10 text-white">
            <div className="flex items-center gap-3">
              <StoneLogo size={34} />
              <div>
                <p className="text-lg font-black tracking-tight">STONE ADMIN</p>
                <p className="text-xs font-semibold text-white/55">운영자 콘솔</p>
              </div>
            </div>

            <div className="max-w-xl space-y-6">
              <div className="inline-flex items-center gap-2 rounded-md border border-white/15 bg-white/10 px-3 py-2 text-xs font-bold text-white/80">
                <ShieldCheck className="h-4 w-4 text-brand-gold" />
                관리자 인증 필요
              </div>
              <div className="space-y-3">
                <h1 className="text-4xl font-black tracking-tight">관리자 전용 로그인</h1>
                <p className="max-w-md text-sm font-medium leading-6 text-white/60">
                  회원 계정이 아닌 관리자 계정으로 로그인해야 운영 기능에 접근할 수 있습니다.
                </p>
              </div>
            </div>

            <p className="text-xs font-semibold text-white/35">/admin-console</p>
          </section>

          <section className="flex items-center p-8 sm:p-10">
            <form onSubmit={handleSubmit} className="w-full space-y-6">
              <div className="space-y-2">
                <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-stone-100 text-stone-700">
                  <LockKeyhole className="h-6 w-6" />
                </div>
                <h2 className="text-2xl font-black tracking-tight text-stone-900">로그인</h2>
                <p className="text-sm font-medium text-stone-500">관리자 아이디와 비밀번호를 입력해 주세요.</p>
              </div>

              {error && (
                <div className="rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm font-semibold text-red-600">
                  {error}
                </div>
              )}

              <div className="space-y-2">
                <label htmlFor="admin-login-id" className="text-xs font-black uppercase tracking-widest text-stone-500">
                  관리자 아이디
                </label>
                <input
                  id="admin-login-id"
                  type="text"
                  value={adminLoginId}
                  onChange={(event) => setAdminLoginId(event.target.value)}
                  autoComplete="username"
                  placeholder="관리자 아이디"
                  className="w-full rounded-lg border border-stone-200 bg-stone-100 px-4 py-3 text-sm font-bold text-stone-900 outline-none transition-colors focus:border-brand-blue focus:bg-white"
                />
              </div>

              <div className="space-y-2">
                <label htmlFor="admin-password" className="text-xs font-black uppercase tracking-widest text-stone-500">
                  비밀번호
                </label>
                <div className="relative">
                  <input
                    id="admin-password"
                    type={showPassword ? 'text' : 'password'}
                    value={password}
                    onChange={(event) => setPassword(event.target.value)}
                    autoComplete="current-password"
                    placeholder="비밀번호"
                    className="w-full rounded-lg border border-stone-200 bg-stone-100 px-4 py-3 pr-12 text-sm font-bold text-stone-900 outline-none transition-colors focus:border-brand-blue focus:bg-white"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword((value) => !value)}
                    className="absolute right-4 top-1/2 -translate-y-1/2 text-stone-400 transition-colors hover:text-stone-700"
                    aria-label={showPassword ? '비밀번호 숨기기' : '비밀번호 보기'}
                  >
                    {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                  </button>
                </div>
              </div>

              <button
                type="submit"
                disabled={isSubmitting}
                className="w-full rounded-lg bg-stone-900 px-4 py-3.5 text-sm font-black text-white transition-colors hover:bg-black disabled:cursor-not-allowed disabled:opacity-60"
              >
                {isSubmitting ? '로그인 중...' : '관리자 로그인'}
              </button>
            </form>
          </section>
        </div>
      </div>
    </main>
  );
}
