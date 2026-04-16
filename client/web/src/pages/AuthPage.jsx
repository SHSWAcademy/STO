import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { ArrowLeft, Eye, EyeOff, CheckCircle, Wallet, Landmark, Play } from 'lucide-react';
import { useApp } from '../context/AppContext.jsx';
import { cn } from '../lib/utils.js';
import { StoneLogo } from '../components/ui/StoneLogo.jsx';
import { Modal } from '../components/ui/Modal.jsx';
import { API_BASE_URL } from '../lib/config.js';

export function AuthPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useApp();

  const [tab, setTab] = useState('login');
  const [showComplete, setShowComplete] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  const [signupName, setSignupName] = useState('');
  const [signupEmail, setSignupEmail] = useState('');
  const [signupPassword, setSignupPassword] = useState('');
  const [signupPasswordConfirm, setSignupPasswordConfirm] = useState('');
  const [accountPassword, setAccountPassword] = useState('');
  const [signupError, setSignupError] = useState('');
  const [isSigningUp, setIsSigningUp] = useState(false);

  const returnPath = typeof location.state?.from === 'string' ? location.state.from : '/';

  function handleBack() {
    if (window.history.length > 1) {
      navigate(-1);
      return;
    }

    navigate(returnPath || '/');
  }

  async function handleLogin() {
    if (!email.trim() || !password.trim()) return;
    setError('');

    try {
      await login(email, password);
      navigate(returnPath, { replace: true });
    } catch (err) {
      console.error('[Login] failed:', err);
      setError('로그인에 실패했습니다. 계정 정보를 확인해 주세요.');
    }
  }

  async function handleSignup() {
    if (!signupName.trim()) {
      setSignupError('이름을 입력해 주세요.');
      return;
    }

    if (!signupEmail.trim()) {
      setSignupError('이메일을 입력해 주세요.');
      return;
    }

    if (!signupPassword.trim()) {
      setSignupError('비밀번호를 입력해 주세요.');
      return;
    }

    if (signupPassword.length < 8) {
      setSignupError('비밀번호는 8자 이상이어야 합니다.');
      return;
    }

    if (!signupPasswordConfirm.trim()) {
      setSignupError('비밀번호 확인을 입력해 주세요.');
      return;
    }

    if (signupPassword !== signupPasswordConfirm) {
      setSignupError('비밀번호와 비밀번호 확인이 일치하지 않습니다.');
      return;
    }

    if (!accountPassword.trim()) {
      setSignupError('계좌 비밀번호를 입력해 주세요.');
      return;
    }

    if (!/^\d{4}$/.test(accountPassword)) {
      setSignupError('계좌 비밀번호는 숫자 4자리여야 합니다.');
      return;
    }

    setSignupError('');
    setIsSigningUp(true);

    try {
      const res = await fetch(`${API_BASE_URL}/api/auth/member/signup`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          email: signupEmail,
          password: signupPassword,
          name: signupName,
          accountPassword,
        }),
      });

      if (!res.ok) {
        let errMsg = '회원가입에 실패했습니다. 다시 시도해 주세요.';
        try {
          const body = await res.json();
          errMsg = body.errorMessage || body.message || errMsg;
        } catch {}
        setSignupError(errMsg);
        return;
      }

      setShowComplete(true);
    } catch (err) {
      console.error('[Signup] failed:', err);
      setSignupError('회원가입 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.');
    } finally {
      setIsSigningUp(false);
    }
  }

  async function handleSignupComplete() {
    try {
      await login(signupEmail, signupPassword);
      navigate(returnPath, { replace: true });
    } catch (err) {
      console.error('[SignupComplete] auto login failed:', err);
      setSignupError('회원가입은 완료됐지만 자동 로그인에 실패했습니다. 로그인 화면에서 다시 로그인해 주세요.');
      setShowComplete(false);
      setTab('login');
      setEmail(signupEmail);
      setPassword(signupPassword);
    }
  }

  return (
    <div className="min-h-screen bg-stone-100 px-4 py-6">
      <div className="mx-auto w-full max-w-md">
        <div className="mb-8 flex items-center">
          <button
            type="button"
            onClick={handleBack}
            className="inline-flex items-center gap-2 px-0 py-1 text-xs font-bold text-stone-500 transition-colors hover:text-stone-800"
          >
            <ArrowLeft size={14} />
            <span className="tracking-tight">이전으로</span>
          </button>
        </div>

        <div className="flex flex-col items-center justify-center py-2">
          <div className="mb-8 text-center">
            <div className="mb-2 flex items-center justify-center gap-2">
              <StoneLogo size={40} />
              <span className="text-2xl font-black tracking-tight text-stone-800">STONE</span>
            </div>
            <p className="text-sm font-bold text-stone-500">증권형 토큰 거래 플랫폼</p>
          </div>

          <div className="w-full max-w-md">
            <div className="mb-6 flex rounded-2xl border border-stone-200 bg-stone-200 p-1 shadow-sm">
              {[
                { id: 'login', label: '로그인' },
                { id: 'signup', label: '회원가입' },
              ].map((item) => (
                <button
                  key={item.id}
                  onClick={() => setTab(item.id)}
                  className={cn(
                    'flex-1 rounded-xl py-2.5 text-sm font-black uppercase tracking-widest transition-all',
                    tab === item.id ? 'bg-white text-stone-800 shadow-lg' : 'text-stone-400 hover:text-stone-600',
                  )}
                >
                  {item.label}
                </button>
              ))}
            </div>

            {tab === 'login' && (
              <div className="space-y-5 rounded-2xl border border-stone-200 bg-white p-8 shadow-xl">
                <h2 className="text-xl font-black uppercase tracking-tight text-stone-800">로그인</h2>

                <div className="space-y-1.5">
                  <label className="block text-[10px] font-black uppercase tracking-widest text-stone-400">이메일 주소</label>
                  <input
                    type="text"
                    value={email}
                    onChange={(event) => setEmail(event.target.value)}
                    onKeyDown={(event) => event.key === 'Enter' && handleLogin()}
                    placeholder="이메일 주소"
                    className="w-full rounded-xl border border-stone-200 bg-stone-100 px-4 py-3 text-sm font-bold text-stone-800 outline-none transition-all focus:border-brand-blue"
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="block text-[10px] font-black uppercase tracking-widest text-stone-400">비밀번호</label>
                  <div className="relative">
                    <input
                      type={showPassword ? 'text' : 'password'}
                      value={password}
                      onChange={(event) => setPassword(event.target.value)}
                      onKeyDown={(event) => event.key === 'Enter' && handleLogin()}
                      placeholder="비밀번호"
                      className="w-full rounded-xl border border-stone-200 bg-stone-100 px-4 py-3 pr-12 text-sm font-bold text-stone-800 outline-none transition-all focus:border-brand-blue"
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword((prev) => !prev)}
                      className="absolute right-4 top-1/2 -translate-y-1/2 text-stone-400 transition-colors hover:text-stone-600"
                    >
                      {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                    </button>
                  </div>
                </div>

                <div className="flex items-center justify-between text-[11px] font-bold">
                  <label className="flex cursor-pointer items-center gap-2 text-stone-500 hover:text-stone-700">
                    <input type="checkbox" defaultChecked className="h-4 w-4 rounded border-stone-300" />
                    로그인 상태 유지
                  </label>
                  <button type="button" className="text-stone-600 hover:underline">비밀번호 찾기</button>
                </div>

                {error && (
                  <div className="rounded-md border border-red-200 bg-red-50 px-4 py-3 text-xs font-bold text-red-600">
                    {error}
                  </div>
                )}

                <button
                  onClick={handleLogin}
                  className="w-full rounded-xl bg-stone-800 py-3.5 text-xs font-black uppercase tracking-widest text-white shadow-lg transition-all hover:bg-black"
                >
                  로그인하기
                </button>
              </div>
            )}

            {tab === 'signup' && (
              <div className="space-y-5 rounded-2xl border border-stone-200 bg-white p-8 shadow-xl">
                <h2 className="text-xl font-black uppercase tracking-tight text-stone-800">회원가입</h2>

                <div className="space-y-1.5">
                  <label className="block text-[10px] font-black uppercase tracking-widest text-stone-400">이름</label>
                  <input
                    type="text"
                    value={signupName}
                    onChange={(event) => setSignupName(event.target.value)}
                    placeholder="홍길동"
                    className="w-full rounded-xl border border-stone-200 bg-stone-100 px-4 py-3 text-sm font-bold text-stone-800 outline-none transition-all focus:border-brand-blue"
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="block text-[10px] font-black uppercase tracking-widest text-stone-400">이메일 주소</label>
                  <input
                    type="email"
                    value={signupEmail}
                    onChange={(event) => setSignupEmail(event.target.value)}
                    placeholder="example@email.com"
                    className="w-full rounded-xl border border-stone-200 bg-stone-100 px-4 py-3 text-sm font-bold text-stone-800 outline-none transition-all focus:border-brand-blue"
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="block text-[10px] font-black uppercase tracking-widest text-stone-400">비밀번호</label>
                  <input
                    type="password"
                    value={signupPassword}
                    onChange={(event) => setSignupPassword(event.target.value)}
                    placeholder="8자 이상 입력"
                    className="w-full rounded-xl border border-stone-200 bg-stone-100 px-4 py-3 text-sm font-bold text-stone-800 outline-none transition-all focus:border-brand-blue"
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="block text-[10px] font-black uppercase tracking-widest text-stone-400">비밀번호 확인</label>
                  <input
                    type="password"
                    value={signupPasswordConfirm}
                    onChange={(event) => setSignupPasswordConfirm(event.target.value)}
                    placeholder="비밀번호를 다시 입력해 주세요"
                    className="w-full rounded-xl border border-stone-200 bg-stone-100 px-4 py-3 text-sm font-bold text-stone-800 outline-none transition-all focus:border-brand-blue"
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="block text-[10px] font-black uppercase tracking-widest text-stone-400">계좌 비밀번호</label>
                  <input
                    type="password"
                    value={accountPassword}
                    onChange={(event) => setAccountPassword(event.target.value)}
                    placeholder="4자리 숫자"
                    maxLength={4}
                    className="w-full rounded-xl border border-stone-200 bg-stone-100 px-4 py-3 text-sm font-bold text-stone-800 outline-none transition-all focus:border-brand-blue"
                  />
                </div>

                <div className="rounded-xl border border-stone-200 bg-stone-100 p-4">
                  <p className="mb-2 flex items-center gap-1.5 text-[10px] font-black uppercase tracking-widest text-stone-600">
                    <Wallet size={12} /> 가입 시 자동 연결 서비스
                  </p>
                  <div className="flex flex-wrap gap-2">
                    <span className="rounded-md border border-stone-200 bg-white px-2 py-0.5 text-[9px] font-black uppercase text-stone-600">블록체인 지갑</span>
                    <span className="rounded-md border border-stone-200 bg-white px-2 py-0.5 text-[9px] font-black uppercase text-brand-red">증권 계좌</span>
                  </div>
                </div>

                <div className="space-y-3">
                  <label className="group flex cursor-pointer items-center gap-3">
                    <input type="checkbox" defaultChecked className="h-4 w-4 rounded border-stone-300" />
                    <span className="text-[11px] font-bold uppercase tracking-widest text-stone-500 group-hover:text-stone-700">계좌 자동 생성에 동의합니다</span>
                  </label>
                  <label className="group flex cursor-pointer items-start gap-3">
                    <input type="checkbox" className="mt-0.5 h-4 w-4 rounded border-stone-300" />
                    <span className="text-[11px] font-bold leading-relaxed text-stone-500 group-hover:text-stone-700">
                      이용약관 및 개인정보 처리방침에 동의합니다. 만 19세 이상이며 국내 거주자임을 확인합니다.
                    </span>
                  </label>
                </div>

                {signupError && (
                  <div className="rounded-md border border-red-200 bg-red-50 px-4 py-3 text-xs font-bold text-red-600">
                    {signupError}
                  </div>
                )}

                <button
                  onClick={handleSignup}
                  disabled={isSigningUp}
                  className="w-full rounded-xl bg-stone-800 py-3.5 text-xs font-black uppercase tracking-widest text-white shadow-lg transition-all hover:bg-black disabled:bg-stone-400"
                >
                  {isSigningUp ? '회원가입 처리 중..' : '회원가입 완료'}
                </button>
              </div>
            )}
          </div>

          <Modal isOpen={showComplete} onClose={() => setShowComplete(false)}>
            <div className="space-y-6 p-8">
              <div className="text-center">
                <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full border border-stone-200 bg-brand-green-light">
                  <CheckCircle className="h-8 w-8 text-brand-green" />
                </div>
                <h3 className="text-xl font-black uppercase tracking-tight text-stone-800">가입 완료!</h3>
                <p className="mt-2 text-xs font-bold text-stone-500">STONE 회원이 되신 것을 환영합니다.</p>
              </div>

              <div className="space-y-3">
                {[
                  { icon: Wallet, bg: 'bg-stone-100', color: 'text-stone-600', label: '블록체인 지갑', value: '0x742d...1F3A' },
                  { icon: Landmark, bg: 'bg-stone-100', color: 'text-brand-red', label: '증권 계좌 연결', value: '국민증권**** 4521' },
                ].map((item, index) => {
                  const Icon = item.icon;
                  return (
                    <div key={index} className="flex items-center gap-4 rounded-2xl border border-stone-200 bg-stone-100 p-4">
                      <div className={`flex h-10 w-10 items-center justify-center rounded-xl ${item.bg}`}>
                        <Icon className={`h-5 w-5 ${item.color}`} />
                      </div>
                      <div className="flex-1">
                        <p className="text-[10px] font-black uppercase tracking-widest text-stone-400">{item.label}</p>
                        <p className="text-xs font-bold text-stone-800">{item.value}</p>
                      </div>
                      <CheckCircle className="h-4 w-4 text-brand-green" />
                    </div>
                  );
                })}
              </div>

              <button
                onClick={handleSignupComplete}
                className="flex w-full items-center justify-center gap-2 rounded-xl bg-stone-800 py-4 text-xs font-black uppercase tracking-widest text-white shadow-lg transition-all hover:bg-stone-700"
              >
                거래 시작하기 <Play size={14} />
              </button>
            </div>
          </Modal>
        </div>
      </div>
    </div>
  );
}
