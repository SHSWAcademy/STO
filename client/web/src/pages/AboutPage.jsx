function Section({ title, children }) {
  return (
    <section className="rounded-2xl border border-stone-200 bg-white p-8 shadow-sm">
      <h3 className="text-xl font-black text-stone-900">{title}</h3>
      <div className="mt-4 space-y-3 text-sm font-semibold leading-7 text-stone-500">{children}</div>
    </section>
  );
}

export function AboutPage() {
  return (
    <div className="mx-auto max-w-[1100px] space-y-8">
      <div>
        <h2 className="text-3xl font-black tracking-tight text-stone-800">서비스 소개</h2>
        <p className="mt-3 text-sm font-bold text-stone-500">
          STONE 플랫폼의 목적과 제공 범위를 안내합니다.
        </p>
      </div>

      <Section title="STONE이 제공하는 것">
        <p>실물 기반 토큰증권 자산의 가격, 거래 흐름, 공시, 공지사항을 한 화면에서 탐색할 수 있도록 구성했습니다.</p>
        <p>메인 페이지에서는 자산 리스트, 실시간 1분봉 차트, AI 요약, STO 관련 뉴스를 함께 제공합니다.</p>
      </Section>

      <Section title="이용 대상">
        <p>토큰증권 시장 흐름을 빠르게 파악하고 싶은 일반 이용자와 자산별 공시 및 거래 내역을 확인하려는 투자자를 대상으로 합니다.</p>
        <p>일부 기능은 로그인 이후 사용할 수 있으며, 계좌와 관심 자산 기능은 회원 상태에 따라 접근 권한이 달라질 수 있습니다.</p>
      </Section>

      <Section title="안내 사항">
        <p>표시되는 시세 및 뉴스, AI 요약 정보는 참고용입니다.</p>
        <p>투자 판단은 이용자 본인의 책임으로 이루어져야 하며, 개별 상품의 상세 조건은 별도 공시 문서를 통해 확인해야 합니다.</p>
      </Section>
    </div>
  );
}
