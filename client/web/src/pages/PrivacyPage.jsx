function PrivacySection({ title, children }) {
  return (
    <section className="rounded-2xl border border-stone-200 bg-white p-8 shadow-sm">
      <h3 className="text-xl font-black text-stone-900">{title}</h3>
      <div className="mt-4 space-y-3 text-sm font-semibold leading-7 text-stone-500">{children}</div>
    </section>
  );
}

export function PrivacyPage() {
  return (
    <div className="mx-auto max-w-[1100px] space-y-8">
      <div>
        <h2 className="text-3xl font-black tracking-tight text-stone-800">개인정보처리방침</h2>
        <p className="mt-3 text-sm font-bold text-stone-500">
          서비스 운영을 위한 개인정보 처리 기준 예시입니다.
        </p>
      </div>

      <PrivacySection title="1. 수집하는 정보">
        <p>회사는 회원가입, 로그인, 계좌 기능, 고객 문의 처리 과정에서 필요한 최소한의 개인정보를 수집할 수 있습니다.</p>
        <p>서비스 이용 과정에서 접속 로그, 기기 정보, 이용 기록 등 서비스 운영에 필요한 정보가 자동으로 생성될 수 있습니다.</p>
      </PrivacySection>

      <PrivacySection title="2. 이용 목적">
        <p>수집한 정보는 회원 식별, 서비스 제공, 공지 전달, 고객 지원, 보안 관리 및 서비스 품질 개선을 위해 사용됩니다.</p>
      </PrivacySection>

      <PrivacySection title="3. 보관 및 파기">
        <p>회사는 관련 법령 또는 이용 목적 달성에 필요한 기간 동안 개인정보를 보관하며, 보관 목적이 종료되면 지체 없이 파기합니다.</p>
      </PrivacySection>

      <PrivacySection title="4. 제3자 제공 및 위탁">
        <p>회사는 법령상 근거가 있거나 이용자의 동의가 있는 경우를 제외하고 개인정보를 외부에 제공하지 않습니다.</p>
        <p>서비스 운영을 위해 일부 업무를 외부에 위탁하는 경우 관련 내용을 별도 고지할 수 있습니다.</p>
      </PrivacySection>

      <PrivacySection title="5. 이용자의 권리">
        <p>이용자는 자신의 개인정보에 대한 열람, 정정, 삭제, 처리정지 등을 요청할 수 있습니다.</p>
        <p>개인정보 관련 문의는 서비스 내 안내된 연락 채널을 통해 접수할 수 있습니다.</p>
      </PrivacySection>
    </div>
  );
}
