import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useApp } from '../context/AppContext.jsx';
import { Eye, EyeOff, CheckCircle, Wallet, Landmark, Play } from 'lucide-react';
import { cn } from '../lib/utils.js';
import { StoneLogo } from '../components/ui/StoneLogo.jsx';
import { Modal } from '../components/ui/Modal.jsx';

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
    <div className="min-h-screen flex flex-col items-center justify-center bg-stone-100 p-4">

      {/* 로고 */}
      <div className="mb-8 text-center">
        <div className="flex items-center justify-center gap-2 mb-2">
          <StoneLogo size={40} />
          <span className="text-2xl font-black text-stone-800 tracking-tight">STONE</span>
        </div>
        <p className="text-stone-500 text-sm font-bold">증권형 토큰 거래 플랫폼</p>
      </div>

      <div className="w-full max-w-md">
        {/* 탭 */}
        <div className="flex bg-stone-200 border border-stone-200 rounded-2xl p-1 mb-6 shadow-sm">
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
                  ? 'bg-white text-stone-800 shadow-lg'
                  : 'text-stone-400 hover:text-stone-600'
              )}
            >
              {t.label}
            </button>
          ))}
        </div>

        {/* 로그인 */}
        {tab === 'login' && (
          <div className="bg-white rounded-2xl p-8 border border-stone-200 shadow-xl space-y-5">
            <h2 className="text-xl font-black text-stone-800 uppercase tracking-tight">로그인</h2>

            <div className="space-y-1.5">
              <label className="block text-[10px] font-black text-stone-400 uppercase tracking-widest">이메일 주소</label>
              <input
                type="text"
                value={email}
                onChange={e => setEmail(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && handleLogin()}
                placeholder="이메일 또는 ADMIN"
                className="w-full bg-stone-100 border border-stone-200 rounded-xl px-4 py-3 text-sm text-stone-800 focus:border-brand-blue outline-none transition-all font-bold"
              />
            </div>

            <div className="space-y-1.5">
              <label className="block text-[10px] font-black text-stone-400 uppercase tracking-widest">비밀번호</label>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                  onKeyDown={e => e.key === 'Enter' && handleLogin()}
                  placeholder="비밀번호 또는 ADMIN"
                  className="w-full bg-stone-100 border border-stone-200 rounded-xl px-4 py-3 pr-12 text-sm text-stone-800 focus:border-brand-blue outline-none transition-all font-bold"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(p => !p)}
                  className="absolute right-4 top-1/2 -translate-y-1/2 text-stone-400 hover:text-stone-600 transition-colors"
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </div>

            <div className="flex items-center justify-between text-[11px] font-bold">
              <label className="flex items-center gap-2 text-stone-500 cursor-pointer hover:text-stone-700">
                <input type="checkbox" defaultChecked className="w-4 h-4 rounded border-stone-300" />
                로그인 상태 유지
              </label>
              <button className="text-stone-600 hover:underline">비밀번호 찾기</button>
            </div>

            <button
              onClick={handleLogin}
              className="w-full bg-stone-800 hover:bg-black text-white font-black py-3.5 rounded-xl transition-all shadow-lg uppercase tracking-widest text-xs"
            >
              로그인하기
            </button>

            <p className="text-[10px] text-stone-400 text-center font-bold">
              테스트: 아무 값 입력 → 일반 유저 | ADMIN / ADMIN → 관리자
            </p>
          </div>
        )}

        {/* 회원가입 */}
        {tab === 'signup' && (
          <div className="bg-white rounded-2xl p-8 border border-stone-200 shadow-xl space-y-5">
            <h2 className="text-xl font-black text-stone-800 uppercase tracking-tight">회원가입</h2>

            {[
              { label: '이름',           type: 'text',     placeholder: '홍길동' },
              { label: '이메일 주소',     type: 'email',    placeholder: 'example@email.com' },
              { label: '비밀번호',        type: 'password', placeholder: '8자 이상, 영문+숫자+특수문자' },
              { label: '비밀번호 재입력', type: 'password', placeholder: '비밀번호를 다시 입력해주세요' },
              { label: '계좌 비밀번호',   type: 'password', placeholder: '4자리 숫자' },
            ].map((field, i) => (
              <div key={i} className="space-y-1.5">
                <label className="block text-[10px] font-black text-stone-400 uppercase tracking-widest">{field.label}</label>
                <input
                  type={field.type}
                  placeholder={field.placeholder}
                  className="w-full bg-stone-100 border border-stone-200 rounded-xl px-4 py-3 text-sm text-stone-800 outline-none font-bold focus:border-brand-blue transition-all"
                />
              </div>
            ))}

            <div className="p-4 bg-stone-100 rounded-xl border border-stone-200">
              <p className="text-[10px] font-black text-stone-600 mb-2 flex items-center gap-1.5 uppercase tracking-widest">
                <Wallet size={12} /> 가입 후 자동 연결 서비스
              </p>
              <div className="flex flex-wrap gap-2">
                <span className="px-2 py-0.5 rounded-md bg-white text-stone-600 text-[9px] font-black uppercase border border-stone-200">블록체인 지갑</span>
                <span className="px-2 py-0.5 rounded-md bg-white text-brand-red text-[9px] font-black uppercase border border-stone-200">은행 계좌</span>
              </div>
            </div>

            <div className="space-y-3">
              <label className="flex items-center gap-3 cursor-pointer group">
                <input type="checkbox" defaultChecked className="w-4 h-4 rounded border-stone-300" />
                <span className="text-[11px] font-bold text-stone-500 group-hover:text-stone-700 uppercase tracking-widest">계좌 자동 생성됩니다</span>
              </label>
              <label className="flex items-start gap-3 cursor-pointer group">
                <input type="checkbox" className="w-4 h-4 rounded border-stone-300 mt-0.5" />
                <span className="text-[11px] font-bold text-stone-500 leading-relaxed group-hover:text-stone-700">
                  이용약관 및 개인정보 처리방침에 동의합니다. 만 19세 이상이며 국내 거주자임을 확인합니다.
                </span>
              </label>
            </div>

            <button
              onClick={handleSignup}
              className="w-full bg-stone-800 hover:bg-black text-white font-black py-3.5 rounded-xl transition-all shadow-lg uppercase tracking-widest text-xs"
            >
              회원가입 완료
            </button>
          </div>
        )}
      </div>

      {/* 가입 완료 모달 */}
      <Modal isOpen={showComplete} onClose={() => setShowComplete(false)}>
        <div className="p-8 space-y-6">
          <div className="text-center">
            <div className="w-16 h-16 bg-brand-green-light rounded-full flex items-center justify-center mx-auto mb-4 border border-stone-200">
              <CheckCircle className="text-brand-green w-8 h-8" />
            </div>
            <h3 className="text-xl font-black text-stone-800 uppercase tracking-tight">가입 완료!</h3>
            <p className="text-xs text-stone-500 font-bold mt-2">STONE 회원이 되신 것을 환영합니다</p>
          </div>

          <div className="space-y-3">
            {[
              { icon: Wallet,   bg: 'bg-stone-100', color: 'text-stone-600', label: '블록체인 지갑',  value: '0x742d...1F3A' },
              { icon: Landmark, bg: 'bg-stone-100',  color: 'text-brand-red',  label: '은행 계좌 연결', value: '국민은행 **** 4521' },
            ].map((item, i) => {
              const Icon = item.icon;
              return (
                <div key={i} className="flex items-center gap-4 p-4 bg-stone-100 rounded-2xl border border-stone-200">
                  <div className={`w-10 h-10 ${item.bg} rounded-xl flex items-center justify-center`}>
                    <Icon className={`${item.color} w-5 h-5`} />
                  </div>
                  <div className="flex-1">
                    <p className="text-[10px] font-black text-stone-400 uppercase tracking-widest">{item.label}</p>
                    <p className="text-xs font-bold text-stone-800">{item.value}</p>
                  </div>
                  <CheckCircle className="text-brand-green w-4 h-4" />
                </div>
              );
            })}
          </div>

          <button
            onClick={handleSignupComplete}
            className="w-full bg-stone-800 hover:bg-stone-700 text-white py-4 rounded-xl text-xs font-black uppercase tracking-widest transition-all flex items-center justify-center gap-2 shadow-lg"
          >
            거래 시작하기 <Play size={14} />
          </button>
        </div>
      </Modal>
    </div>
  );
}
