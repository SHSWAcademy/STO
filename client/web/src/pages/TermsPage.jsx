function TermsSection({ index, title, children }) {
  return (
    <section className="rounded-2xl border border-stone-200 bg-white p-8 shadow-sm">
      <h3 className="text-xl font-black text-stone-900">
        제{index}조 {title}
      </h3>
      <div className="mt-4 space-y-3 text-sm font-semibold leading-7 text-stone-500">{children}</div>
    </section>
  );
}

export function TermsPage() {
  return (
    <div className="mx-auto max-w-[1100px] space-y-8">
      <div>
        <h2 className="text-3xl font-black tracking-tight text-stone-800">이용약관</h2>
        <p className="mt-3 text-sm font-bold text-stone-500">
          아래 내용은 서비스 운영을 위한 기본 약관 예시입니다.
        </p>
      </div>

      <TermsSection index="1" title="목적">
        <p>본 약관은 STONE이 제공하는 토큰증권 정보 서비스의 이용과 관련하여 회사와 이용자의 권리, 의무 및 책임사항을 정하는 것을 목적으로 합니다.</p>
      </TermsSection>

      <TermsSection index="2" title="서비스 내용">
        <p>회사는 자산 정보, 공시, 공지사항, 뉴스, AI 요약, 관심 자산 및 계좌 관련 기능을 제공합니다.</p>
        <p>회사는 운영상 또는 기술상 필요에 따라 서비스의 일부를 변경하거나 중단할 수 있습니다.</p>
      </TermsSection>

      <TermsSection index="3" title="이용자의 의무">
        <p>이용자는 관련 법령과 본 약관을 준수하여야 하며, 타인의 권리를 침해하거나 서비스 운영을 방해하는 행위를 해서는 안 됩니다.</p>
        <p>이용자는 계정 및 인증 정보를 안전하게 관리해야 하며, 이에 대한 관리 책임은 이용자에게 있습니다.</p>
      </TermsSection>

      <TermsSection index="4" title="면책">
        <p>회사가 제공하는 시세, 뉴스, 공시 요약, AI 기반 정보는 참고용이며 투자 자문 또는 권유에 해당하지 않습니다.</p>
        <p>이용자의 투자 판단과 그 결과에 대한 책임은 이용자 본인에게 있습니다.</p>
      </TermsSection>

      <TermsSection index="5" title="기타">
        <p>본 약관은 서비스 정책 변경, 법령 개정, 운영상 필요에 따라 수정될 수 있습니다.</p>
        <p>중요 변경 사항은 공지사항 또는 별도 안내를 통해 고지합니다.</p>
      </TermsSection>
    </div>
  );
}
