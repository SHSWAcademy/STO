# CI/CD 개념 완전 정복

> 내가 만든 파이프라인을 직접 예시로 삼아 설명합니다.
> 명령어를 따라치기만 했어도, 이 문서를 읽고 나면 내가 무엇을 만들었는지 이해할 수 있습니다.

---

## 1. CI/CD가 뭔가요?

### 배포를 수동으로 하면 어떻게 될까?

코드를 고쳤다고 가정합니다. 서버에 반영하려면:

```
1. 코드 수정
2. 빌드 (gradle build)
3. JAR 파일을 서버에 복사
4. 기존 서버 프로세스 종료
5. 새 JAR 실행
6. 잘 됐는지 확인
```

팀원이 여러 명이면? 서버가 3개면? 매번 이걸 손으로?
실수도 나고, 시간도 걸리고, 배포가 두렵고 귀찮아집니다.

### CI/CD는 이걸 자동화한 것

- **CI (Continuous Integration, 지속적 통합)**
  코드를 push할 때마다 자동으로 빌드 + 테스트를 실행해서
  "이 코드가 문제없는지" 즉시 확인하는 것

- **CD (Continuous Delivery/Deployment, 지속적 배포)**
  빌드가 성공하면 자동으로 서버에 배포까지 해주는 것

한 줄 요약: **코드 push → 자동으로 빌드 → 자동으로 배포**

---

## 2. 코드 저장소 - GitHub vs GitLab vs Gitea

세 가지 모두 **코드를 저장하고 관리하는 서비스**입니다.
git 자체는 버전 관리 도구이고, 이 세 가지는 git을 기반으로 만든 플랫폼입니다.

### GitHub

- 전 세계에서 가장 많이 쓰는 코드 저장소
- Microsoft가 운영하는 외부 서비스 (클라우드)
- 무료 공개 저장소 무제한, 비공개도 일정량 무료
- 코드가 GitHub 서버에 올라감 → 외부 서버에 내 코드가 저장됨
- **GitHub Actions**: 코드 push 시 자동으로 CI/CD를 실행해주는 GitHub 내장 기능

### GitLab

- GitHub과 거의 똑같은 기능을 제공하는 코드 저장소
- **핵심 차이**: **내 서버에 직접 설치할 수 있음** (Self-hosted)
- 우리 프로젝트에서는 맥미니 VM 안에 GitLab을 직접 설치함
  → 코드가 외부 서버(GitHub)가 아닌 우리 맥미니에 저장됨
- CI/CD 기능도 내장되어 있음 (GitLab CI/CD)
- Jenkins 없이도 GitLab 자체에서 빌드/배포 파이프라인 구성 가능

### Gitea

- GitLab보다 훨씬 가벼운 자체 설치형 코드 저장소
- 라즈베리파이 같은 저사양 기기에서도 돌아갈 정도로 가벼움
- CI/CD 기능이 없거나 제한적 → Jenkins 같은 외부 도구와 조합 필요
- 개인 서버에 간단히 GitHub 비슷한 환경을 만들고 싶을 때 사용

### 비교 요약

| 항목 | GitHub | GitLab | Gitea |
|------|--------|--------|-------|
| 운영 방식 | 외부 클라우드 | 외부 클라우드 또는 **자체 설치** | **자체 설치** |
| CI/CD 내장 | GitHub Actions | GitLab CI/CD | 없음 (외부 연동 필요) |
| 용량/비용 | 무료 플랜 있음 | 무료 플랜 있음 | 완전 무료 (내 서버 비용만) |
| 무거움 | - | 무거움 | 매우 가벼움 |
| 우리 프로젝트 | 백업용 origin | **CI/CD 트리거용** | 미사용 |

---

## 3. 빌드(Build)가 뭔가요?

**빌드**: 사람이 읽는 소스코드를 컴퓨터가 실행할 수 있는 파일로 변환하는 작업

Spring Boot 프로젝트를 예로 들면:

```
소스코드(.java 파일들)
        ↓  gradle bootJar 실행
실행 가능한 JAR 파일 (app.jar)
        ↓  java -jar app.jar
서버 실행
```

`.java` 파일은 텍스트 파일이에요. 서버가 직접 실행할 수 없어요.
`gradle bootJar`를 실행하면 모든 `.java`파일을 컴파일하고,
라이브러리(의존성)까지 하나로 묶어서 `app.jar`라는 실행 파일을 만들어 줍니다.

### 우리 프로젝트의 빌드 과정

Jenkins가 이걸 Docker 안에서 실행합니다:

```dockerfile
# 1단계: JDK로 빌드
FROM eclipse-temurin:21-jdk AS builder
COPY . .
RUN ./gradlew bootJar -x test   ← 여기서 빌드 (JAR 파일 생성)

# 2단계: JRE로 실행 (더 가벼운 이미지)
FROM eclipse-temurin:21-jre
COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]   ← 서버 실행
```

`-x test`는 테스트를 건너뜀. CI 환경에는 DB가 없어서 테스트가 실패할 수 있기 때문.

---

## 4. Jenkins가 뭔가요?

**Jenkins**: 코드 push를 감지하면 미리 정해둔 작업(빌드, 테스트, 배포)을 자동으로 실행해주는 자동화 도구

### 비유

> 공장의 자동화 로봇입니다.
> "부품(코드)이 들어오면 → 조립(빌드)하고 → 검사(테스트)하고 → 출고(배포)해라"
> 라는 지시서(Jenkinsfile)를 읽고 자동으로 실행합니다.

### Jenkins가 하는 일 (우리 프로젝트)

```
Jenkinsfile 내용:

1. Checkout   → GitLab에서 최신 코드 받아오기
2. Build      → docker compose build --no-cache (이미지 빌드)
3. Deploy     → docker compose up -d (컨테이너 재시작)
```

### Jenkins vs GitHub Actions vs GitLab CI/CD vs AWS CodeDeploy

모두 CI/CD 도구지만 **어디서 실행되느냐**가 다릅니다.

| 도구 | 실행 위치 | 특징 | 언제 씀? |
|------|-----------|------|----------|
| **Jenkins** | 내 서버 (맥미니 VM) | 자유도 높음, 직접 서버 관리 필요 | 자체 인프라가 있을 때 |
| **GitHub Actions** | GitHub 서버 (무료) | 설정 간단, GitHub push에 반응 | GitHub 쓸 때 가장 편함 |
| **GitLab CI/CD** | GitLab 서버 또는 내 서버 | GitLab과 완전 통합 | GitLab 쓸 때 Jenkins 없이 가능 |
| **AWS CodeDeploy** | AWS 클라우드 | AWS EC2/ECS 배포 특화 | AWS 인프라로 서비스할 때 |

#### Jenkins의 장점과 단점

장점:
- 플러그인이 수천 개 → 거의 모든 것과 연동 가능
- 내 서버에서 실행 → 보안 코드를 외부에 안 보내도 됨
- 빌드 서버 비용 없음 (내 서버 사용)

단점:
- 직접 설치하고 관리해야 함 (우리가 한 것들)
- 초기 설정이 복잡
- 서버가 꺼지면 CI/CD도 멈춤

#### GitHub Actions가 더 편한 이유

```yaml
# .github/workflows/deploy.yml 파일 하나만 만들면 끝
on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest   ← GitHub이 서버 제공 (내가 관리 안 해도 됨)
    steps:
      - uses: actions/checkout@v3
      - run: ./gradlew bootJar
```

설치할 것도 없고, 서버 관리도 없고, GitHub에 push하면 자동으로 실행됩니다.
단, 실제 서버(맥미니 VM)에 배포하려면 추가 설정이 필요합니다.

---

## 5. Docker가 뭔가요?

**Docker**: 앱과 그 앱이 실행되는 환경을 하나의 패키지(이미지)로 묶어서 어디서든 똑같이 실행할 수 있게 해주는 도구

### 비유

> 도시락통입니다.
> 음식(앱) + 반찬(라이브러리) + 그릇(실행 환경)을 하나로 포장해서
> 어디서 열어도 똑같은 음식이 나옵니다.

### 이미지 vs 컨테이너

```
이미지(Image) = 설계도, 틀, 스냅샷
     ↓  docker run
컨테이너(Container) = 실제로 실행 중인 인스턴스
```

마치 클래스(이미지)와 객체(컨테이너)의 관계입니다.

### Docker Hub

**Docker Hub**: Docker 이미지를 저장하고 공유하는 저장소 (이미지의 GitHub)

```
docker pull eclipse-temurin:21-jdk
```

이 명령어가 Docker Hub에서 JDK 이미지를 내려받는 것입니다.
우리 Dockerfile에서 `FROM eclipse-temurin:21-jdk`가 Docker Hub에서 이미지를 가져옵니다.

우리 프로젝트는 Docker Hub에 이미지를 올리지 않고, 맥미니 VM 로컬에서만 빌드/실행합니다.

### Docker Compose

**Docker Compose**: 여러 컨테이너를 한꺼번에 관리하는 도구

우리 프로젝트는 서버가 5개입니다:
- sto-general (메인 API)
- sto-match (체결 서버)
- sto-batch (배치 서버)
- PostgreSQL (DB)
- Redis (캐시)

이걸 매번 `docker run`으로 하나씩 실행하면 힘듭니다.
`docker-compose.yml`에 전부 정의해두고 `docker compose up -d` 한 번으로 전부 실행합니다.

---

## 6. Webhook이 뭔가요?

**Webhook**: 특정 이벤트가 발생하면 지정한 URL로 자동으로 HTTP 요청을 보내는 것

우리 파이프라인에서:

```
git push gitlab main
        ↓
GitLab이 push 이벤트 감지
        ↓
미리 등록해둔 URL로 HTTP POST 요청 전송
→ http://172.17.0.1:8080/project/STO  (Jenkins 주소)
        ↓
Jenkins가 요청을 받고 파이프라인 시작
```

카카오톡 알림과 비슷합니다. "주문이 들어오면 → 나한테 알림 보내줘" 처럼
"push가 오면 → Jenkins한테 알림 보내줘" 입니다.

---

## 7. Cloudflare Tunnel이 뭔가요?

**문제**: 맥미니는 집(또는 학교) 내부 네트워크에 있음 → 외부에서 IP로 직접 접근 불가

**해결**: Cloudflare Tunnel
- 맥미니에서 Cloudflare 서버로 터널(통로)을 미리 뚫어놓음
- 팀원이 `https://xxx.trycloudflare.com` 접속 → Cloudflare → 터널 → 맥미니 VM

```
팀원 (집, 카페 어디서든)
        ↓ https://xxx.trycloudflare.com
Cloudflare 서버 (인터넷)
        ↓ 터널
맥미니 Rocky Linux VM :8082
        ↓
sto-general 컨테이너
```

Quick Tunnel은 무료지만 **VM을 재시작할 때마다 URL이 바뀝니다**.
고정 URL을 원하면 도메인을 구매해서 Named Tunnel을 사용해야 합니다.

---

## 8. 전체 파이프라인 흐름 (지금 내가 만든 것)

```
[개발자 맥북]
    │
    │ git push gitlab main
    ▼
[GitLab - 맥미니 VM :8929]
    │ 코드 저장
    │ Webhook 발송 → http://172.17.0.1:8080/project/STO
    ▼
[Jenkins - 맥미니 VM :8080]
    │ Jenkinsfile 실행
    │
    ├─ stage 1: Checkout
    │   GitLab에서 최신 코드 받아오기
    │
    ├─ stage 2: Build
    │   docker compose build --no-cache
    │   → server/main/Dockerfile 읽어서 JAR 빌드 후 이미지 생성
    │   → server/match/Dockerfile 읽어서 JAR 빌드 후 이미지 생성
    │   → server/batch/Dockerfile 읽어서 JAR 빌드 후 이미지 생성
    │
    └─ stage 3: Deploy
        docker compose up -d
        → 새 이미지로 컨테이너 교체
        → sto-general :8082 재시작
        → sto-match :8081 재시작
        → sto-batch 재시작
        → PostgreSQL, Redis는 이미지 변경 없으면 그대로 유지

[Cloudflare Tunnel]
    팀원이 https://xxx.trycloudflare.com/main/api/health 로 접속
    → 맥미니 VM :8082 → sto-general 컨테이너
```

---

## 9. 용어 한눈에 정리

| 용어 | 한 줄 설명 |
|------|-----------|
| git | 코드 버전 관리 도구 (변경 이력 추적) |
| GitHub | git 저장소 호스팅 서비스 (외부 클라우드) |
| GitLab | GitHub과 유사, 자체 서버 설치 가능 |
| Gitea | 초경량 자체 설치형 git 저장소 |
| Jenkins | 자동 빌드/배포 실행 도구 (자체 서버) |
| GitHub Actions | GitHub 내장 CI/CD (GitHub 서버에서 실행) |
| GitLab CI/CD | GitLab 내장 CI/CD |
| AWS CodeDeploy | AWS 환경 전용 배포 도구 |
| 빌드 | 소스코드 → 실행 가능한 파일로 변환 |
| Docker | 앱과 실행 환경을 하나로 묶는 컨테이너 도구 |
| 이미지 | Docker 컨테이너의 설계도/틀 |
| 컨테이너 | 실제 실행 중인 Docker 인스턴스 |
| Docker Hub | Docker 이미지 저장소 (이미지의 GitHub) |
| Docker Compose | 여러 컨테이너를 한 번에 관리 |
| Webhook | 이벤트 발생 시 특정 URL로 자동 HTTP 요청 |
| Cloudflare Tunnel | 외부에서 내부 서버에 접근할 수 있게 해주는 통로 |
| Jenkinsfile | Jenkins 파이프라인 실행 순서를 적은 파일 |
| CI | 코드 push마다 자동 빌드+테스트 |
| CD | 빌드 성공 시 자동 배포 |

---

## 10. 내가 만든 것 vs 다른 선택지

우리는 Jenkins + GitLab 조합을 선택했습니다.
실제 회사나 다른 프로젝트에서는 이런 조합도 많이 씁니다:

```
[가장 간단한 조합]
GitHub + GitHub Actions
→ 추가 서버 없이 GitHub 하나로 끝
→ 사이드 프로젝트, 오픈소스에 적합

[우리가 만든 조합]
GitLab(자체설치) + Jenkins + Docker
→ 코드를 외부에 안 올려도 됨
→ 자체 인프라를 최대한 활용
→ 초기 설정 복잡하지만 자유도 높음

[회사 AWS 환경]
GitHub + AWS CodeDeploy + ECR + ECS
→ 서버 관리 없이 AWS가 다 해줌
→ 비용 발생, AWS 서비스 종속

[GitLab 올인원]
GitLab(자체설치) + GitLab CI/CD + Docker
→ Jenkins 없이 GitLab 하나로 CI/CD까지 가능
→ 우리 구성에서 Jenkins만 빼면 됨
```

---

> 한 줄 요약:
> **git push 한 번 → GitLab이 받아서 Jenkins에 알림 → Jenkins가 Docker로 빌드 후 배포 → 팀원이 Cloudflare URL로 접속**
> 이게 우리가 만든 CI/CD 파이프라인의 전부입니다.
