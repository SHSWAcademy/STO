import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useApp } from '../context/AppContext.jsx';
import { Coins, Eye, EyeOff, CheckCircle, Wallet, Landmark, Play } from 'lucide-react';
import { cn } from '../lib/utils.js';

// AuthPage — 로그인 / 회원가입 (탭 전환)
// mock 인증: 이메일·비밀번호 모두 ADMIN → 관리자, 그 외 아무 값 → 일반 유저
// API 연결 시: login() 내부만 교체

export function AuthPage() {
  const navigate = useNavigate();
  const { login } = useApp();

  const [tab, setTab]               = useState('login');
  const [showComplete, setShowComplete] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [email, setEmail]           = useState('');
  const [password, setPassword]     = useState('');

  function handleLogin() {
    if (!email.trim() || !password.trim()) return;
    const isAdmin = login(email, password);
    navigate(isAdmin ? '/admin' : '/');
  }

  function handleSignup() {
    setShowComplete(true);
  }

  function handleSignupComplete() {
    login('demo@sto.exchange', 'user');
    navigate('/');
  }

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-stone-bg p-4">

      {/* 로고 */}
      <div className="mb-8 text-center">
        <div className="flex items-center justify-center gap-2 mb-2">
          <div className="w-10 h-10 bg-stone-gold rounded-xl flex items-center justify-center shadow-lg shadow-stone-gold/20">
            <Coins className="text-white w-6 h-6" />
          </div>
          <span className="text-2xl font-black text-stone-gold tracking-tight">STONE</span>
        </div>
        <p className="text-stone-text-secondary text-sm font-bold">증권형 토큰 거래 플랫폼</p>
      </div>

      <div className="w-full max-w-md">
        {/* 탭 */}
        <div className="flex bg-stone-surface border border-stone-border rounded-2xl p-1 mb-6 shadow-sm">
          {[
            { id: 'login',  label: '로그인' },
            { id: 'signup', label: '회원가입' },
          ].map(t => (
            <button
              key={t.id}
              onClick={() => setTab(t.id)}
              className={cn(
                'flex-1 py-2.5 rounded-xl text-sm font-black transition-all uppercase tracking-widest',
                tab === t.id
                  ? 'bg-stone-elevated text-stone-text-primary shadow-lg'
                  : 'text-stone-muted hover:text-stone-text-secondary'
              )}
            >
              {t.label}
            </button>
          ))}
        </div>

        {/* 로그인 */}
        {tab === 'login' && (
          <div className="bg-stone-surface rounded-2xl p-8 border border-stone-border shadow-xl space-y-5">
            <h2 className="text-xl font-black text-stone-text-primary uppercase tracking-tight">로그인</h2>

            <div className="space-y-1.5">
              <label className="block text-[10px] font-black text-stone-muted uppercase tracking-widest">이메일 주소</label>
              <input
                type="text"
                value={email}
                onChange={e => setEmail(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && handleLogin()}
                placeholder="이메일 또는 ADMIN"
                className="w-full bg-stone-bg border border-stone-border rounded-xl px-4 py-3 text-sm text-stone-text-primary focus:border-stone-gold outline-none transition-all font-bold"
              />
            </div>

            <div className="space-y-1.5">
              <label className="block text-[10px] font-black text-stone-muted uppercase tracking-widest">비밀번호</label>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                  onKeyDown={e => e.key === 'Enter' && handleLogin()}
                  placeholder="비밀번호 또는 ADMIN"
                  className="w-full bg-stone-bg border border-stone-border rounded-xl px-4 py-3 pr-12 text-sm text-stone-text-primary focus:border-stone-gold outline-none transition-all font-bold"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(p => !p)}
                  className="absolute right-4 top-1/2 -translate-y-1/2 text-stone-muted hover:text-stone-text-secondary transition-colors"
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </div>

            <div className="flex items-center justify-between text-[11px] font-bold">
              <label className="flex items-center gap-2 text-stone-muted cursor-pointer hover:text-stone-text-secondary">
                <input type="checkbox" defaultChecked className="w-4 h-4 rounded border-stone-muted" />
                로그인 상태 유지
              </label>
              <button className="text-stone-gold hover:underline">비밀번호 찾기</button>
            </div>

            <button
              onClick={handleLogin}
              className="w-full bg-stone-elevated hover:bg-stone-border text-stone-text-primary font-black py-3.5 rounded-xl transition-all shadow-lg uppercase tracking-widest text-xs border border-stone-border"
            >
              로그인하기
            </button>

            <p className="text-[10px] text-stone-muted text-center font-bold">
              테스트: 아무 값 입력 → 일반 유저 | ADMIN / ADMIN → 관리자
            </p>
          </div>
        )}

        {/* 회원가입 */}
        {tab === 'signup' && (
          <div className="bg-stone-surface rounded-2xl p-8 border border-stone-border shadow-xl space-y-5">
            <h2 className="text-xl font-black text-stone-text-primary uppercase tracking-tight">회원가입</h2>

            {[
              { label: '이름',           type: 'text',     placeholder: '홍길동' },
              { label: '이메일 주소',     type: 'email',    placeholder: 'example@email.com' },
              { label: '비밀번호',        type: 'password', placeholder: '8자 이상, 영문+숫자+특수문자' },
              { label: '비밀번호 재입력', type: 'password', placeholder: '비밀번호를 다시 입력해주세요' },
              { label: '계좌 비밀번호',   type: 'password', placeholder: '4자리 숫자' },
            ].map((field, i) => (
              <div key={i} className="space-y-1.5">
                <label className="block text-[10px] font-black text-stone-muted uppercase tracking-widest">{field.label}</label>
                <input
                  type={field.type}
                  placeholder={field.placeholder}
                  className="w-full bg-stone-bg border border-stone-border rounded-xl px-4 py-3 text-sm text-stone-text-primary outline-none font-bold focus:border-stone-gold transition-all"
                />
              </div>
            ))}

            <div className="p-4 bg-stone-buy-bg rounded-xl border border-stone-buy-bg">
              <p className="text-[10px] font-black text-stone-gold mb-2 flex items-center gap-1.5 uppercase tracking-widest">
                <Wallet size={12} /> 가입 후 자동 연결 서비스
              </p>
              <div className="flex flex-wrap gap-2">
                <span className="px-2 py-0.5 rounded-md bg-stone-surface text-stone-gold text-[9px] font-black uppercase border border-stone-border">블록체인 지갑</span>
                <span className="px-2 py-0.5 rounded-md bg-stone-surface text-stone-buy text-[9px] font-black uppercase border border-stone-buy-bg">은행 계좌</span>
              </div>
            </div>

            <div className="space-y-3">
              <label className="flex items-center gap-3 cursor-pointer group">
                <input type="checkbox" defaultChecked className="w-4 h-4 rounded border-stone-muted" />
                <span className="text-[11px] font-bold text-stone-muted group-hover:text-stone-text-secondary uppercase tracking-widest">계좌 자동 생성됩니다</span>
              </label>
              <label className="flex items-start gap-3 cursor-pointer group">
                <input type="checkbox" className="w-4 h-4 rounded border-stone-muted mt-0.5" />
                <span className="text-[11px] font-bold text-stone-muted leading-relaxed group-hover:text-stone-text-secondary">
                  이용약관 및 개인정보 처리방침에 동의합니다. 만 19세 이상이며 국내 거주자임을 확인합니다.
                </span>
              </label>
            </div>

            <button
              onClick={handleSignup}
              className="w-full bg-stone-elevated hover:bg-stone-border text-stone-text-primary font-black py-3.5 rounded-xl transition-all shadow-lg uppercase tracking-widest text-xs border border-stone-border"
            >
              회원가입 완료
            </button>
          </div>
        )}
      </div>

      {/* 가입 완료 모달 */}
      {showComplete && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-stone-surface rounded-2xl p-8 w-full max-w-sm border border-stone-border shadow-2xl space-y-6">
            <div className="text-center">
              <div className="w-16 h-16 bg-stone-buy-bg rounded-full flex items-center justify-center mx-auto mb-4 border border-stone-buy-bg">
                <CheckCircle className="text-stone-buy w-8 h-8" />
              </div>
              <h3 className="text-xl font-black text-stone-text-primary uppercase tracking-tight">가입 완료!</h3>
              <p className="text-xs text-stone-text-secondary font-bold mt-2">STONE 회원이 되신 것을 환영합니다</p>
            </div>

            <div className="space-y-3">
              {[
                { icon: Wallet,   bg: 'bg-stone-buy-bg', color: 'text-stone-gold', label: '블록체인 지갑',  value: '0x742d...1F3A' },
                { icon: Landmark, bg: 'bg-stone-buy-bg',  color: 'text-stone-buy',  label: '은행 계좌 연결', value: '국민은행 **** 4521' },
              ].map((item, i) => {
                const Icon = item.icon;
                return (
                  <div key={i} className="flex items-center gap-4 p-4 bg-stone-elevated rounded-2xl border border-stone-border">
                    <div className={`w-10 h-10 ${item.bg} rounded-xl flex items-center justify-center`}>
                      <Icon className={`${item.color} w-5 h-5`} />
                    </div>
                    <div className="flex-1">
                      <p className="text-[10px] font-black text-stone-muted uppercase tracking-widest">{item.label}</p>
                      <p className="text-xs font-bold text-stone-text-primary">{item.value}</p>
                    </div>
                    <CheckCircle className="text-stone-buy w-4 h-4" />
                  </div>
                );
              })}
            </div>

            <button
              onClick={handleSignupComplete}
              className="w-full bg-stone-gold hover:bg-stone-gold-light text-[#1c1c1e] py-4 rounded-xl text-xs font-black uppercase tracking-widest transition-all flex items-center justify-center gap-2 shadow-lg shadow-stone-gold/20"
            >
              거래 시작하기 <Play size={14} />
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
