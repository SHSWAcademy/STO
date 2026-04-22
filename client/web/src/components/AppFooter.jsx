import { Link } from "react-router-dom";
import { StoneLogo } from "./ui/StoneLogo.jsx";

const FOOTER_LINKS = {
  platform: [
    { label: "서비스 소개", to: "/about" },
    { label: "공지사항", to: "/notice" },
    { label: "공시", to: "/disclosure" },
  ],
  policy: [
    { label: "이용약관", to: "/terms" },
    { label: "개인정보처리방침", to: "/privacy" },
  ],
};

export function AppFooter() {
  return (
    <footer className="mt-0 border-t border-stone-200 bg-white">
      <div className="mx-auto flex max-w-[1760px] flex-col gap-4 px-4 py-4 lg:px-5">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
          <div className="max-w-[720px]">
            <p className="mt-2 overflow-hidden text-ellipsis whitespace-nowrap text-[12px] font-semibold leading-5 text-stone-500">
              STONE은 실물 기반 토큰증권 자산 정보를 탐색하고, 공시 및 거래
              흐름을 함께 확인할 수 있도록 구성한 STO 플랫폼입니다.
            </p>
            <div className="mt-2 space-y-1 text-[11px] font-semibold leading-4 text-stone-400">
              <p>서울특별시 마포구 월드컵북로4길 77, 1층</p>
              <p>대표 문의 doyeon@stone.com</p>
              <p>운영시간 24시간</p>
            </div>
          </div>

          <div className="grid gap-8 lg:min-w-[620px] lg:grid-cols-[120px_140px_minmax(0,1fr)]">
            <div>
              <div className="mb-2 text-[12px] font-black text-stone-800">
                플랫폼
              </div>
              <div className="space-y-1.5">
                {FOOTER_LINKS.platform.map((link) => (
                  <Link
                    key={link.to}
                    to={link.to}
                    className="block text-[12px] font-semibold text-stone-500 transition-colors hover:text-stone-900"
                  >
                    {link.label}
                  </Link>
                ))}
              </div>
            </div>

            <div>
              <div className="mb-2 text-[12px] font-black text-stone-800">
                정책
              </div>
              <div className="space-y-1.5">
                {FOOTER_LINKS.policy.map((link) => (
                  <Link
                    key={link.to}
                    to={link.to}
                    className="block text-[12px] font-semibold text-stone-500 transition-colors hover:text-stone-900"
                  >
                    {link.label}
                  </Link>
                ))}
              </div>
            </div>

            <div className="text-[12px] font-semibold leading-5 text-stone-500">
              <p>
                AI 요약과 뉴스 정보는 참고용이며 투자 판단의 책임은 이용자에게
                있습니다.
              </p>
              <p>
                서비스 정책과 이용 조건은 이용약관 및 개인정보처리방침에서
                확인할 수 있습니다.
              </p>
            </div>
          </div>
        </div>

        <div className="flex flex-col gap-1 border-t border-stone-200 pt-2 text-[11px] font-semibold text-stone-400 lg:flex-row lg:items-center lg:justify-between">
          <p>© 2026 STONE. All rights reserved.</p>
          <p>본 화면의 정보는 투자 권유를 위한 자료가 아닙니다.</p>
        </div>
      </div>
    </footer>
  );
}
