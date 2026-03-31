# CI/CD 구축 기록 및 학습 가이드

> 맥미니 + UTM Rocky Linux 환경에서 GitLab + Jenkins + Docker로 CI/CD를 구축한 과정입니다.
> 다른 프로젝트, 다른 환경에서도 이 문서를 보고 그대로 따라할 수 있도록 작성했습니다.

---

## 전체 구성도 (내가 만든 것)

```
[개발자 맥북]
     |
     | git push
     ↓
[맥미니]
└── UTM (가상머신 앱)
    └── Rocky Linux VM (IP: 192.168.64.7)
        │
        ├── GitLab  :8929   ← 코드 저장소 (우리 서버에 설치한 GitHub)
        ├── Jenkins :8080   ← 자동 빌드/배포 도구
        │     │
        │     │ push 감지하면 자동 빌드
        │     ↓
        └── Docker Compose
              ├── sto-general  :8082  (메인 API 서버, Spring Boot)
              ├── sto-match    :8081  (체결 서버, Spring Boot)
              ├── sto-batch           (배치 서버, Spring Boot)
              ├── PostgreSQL   :5432  (데이터베이스)
              └── Redis        :6379  (캐시)
```

---

## CI/CD 전체 흐름도

```
① 개발자가 코드를 수정하고 push
   git push gitlab main

② GitLab이 push를 감지
   → Webhook으로 Jenkins에 알림 (HTTP 요청)

③ Jenkins가 알림을 받고 자동으로 빌드 시작
   → GitLab에서 최신 코드를 받아옴 (git checkout)

④ Jenkins가 Docker 이미지 빌드
   → docker compose build --no-cache
   → 각 서버의 Dockerfile을 읽어서 JAR 빌드 후 이미지 생성

⑤ Jenkins가 컨테이너 재시작
   → docker compose up -d
   → 새 이미지로 컨테이너 교체 (서비스 중단 최소화)

⑥ 배포 완료
   → 팀원들이 새 버전을 바로 사용 가능
```

**핵심**: `git push` 한 번으로 ①~⑥이 자동으로 일어납니다.

---

## 각 구성요소 설명

| 이름 | 역할 | 비유 |
|------|------|------|
| UTM | 맥에서 리눅스를 실행해주는 프로그램 | 맥 안의 컴퓨터 |
| Rocky Linux | 서버 운영체제 | 실제 서버 역할 |
| Docker | 앱을 격리된 환경(컨테이너)에서 실행 | 도시락통 |
| Docker Compose | 여러 컨테이너를 한 번에 관리 | 도시락통 여러 개를 한 번에 |
| GitLab | 코드 저장소 (자체 설치한 GitHub) | 코드 창고 |
| Jenkins | 코드 push 시 자동 빌드/배포 | 자동화 로봇 |
| Cloudflare Tunnel | 외부에서 내 서버에 접속 가능하게 해주는 통로 | 인터넷 ↔ 내 서버 다리 |

---

## Cloudflare Tunnel 설명

### 팀원 URL 공유하면 어떻게 되나?

```
팀원 브라우저
    ↓ URL 입력
Cloudflare 서버 (인터넷 어딘가)
    ↓ 터널을 통해
맥미니 Rocky Linux VM
    ↓
앱 (General API, GitLab 등)
```

- 팀원이 `https://xxx.trycloudflare.com` URL을 브라우저에 입력
- Cloudflare가 맥미니 VM에 뚫어놓은 터널로 연결해줌
- 팀원은 맥미니의 IP를 몰라도, 같은 와이파이가 아니어도 접속 가능

### 접속 가능 여부

| 상황 | 접속 가능? | 이유 |
|------|-----------|------|
| 팀원이 집에서 접속 | ✅ 가능 | Cloudflare URL은 인터넷만 되면 됨 |
| 팀원이 카페에서 접속 | ✅ 가능 | 동일 |
| 내가 다른 와이파이로 바꿔도 | ✅ 가능 | VM은 맥미니 안에서 독립 실행, 와이파이 무관 |
| 맥미니가 꺼졌을 때 | ❌ 불가 | VM 자체가 꺼지기 때문 |
| VM은 켜졌지만 터널 미실행 | ❌ 불가 | 터널이 다리 역할을 하기 때문 |

### 현재 외부 접속 URL (재시작마다 바뀜)

| 서비스 | URL |
|--------|-----|
| General API | https://messaging-session-ooo-gained.trycloudflare.com |
| GitLab | https://approximately-insulin-fraser-drug.trycloudflare.com |
| Jenkins | https://together-bone-protein-willing.trycloudflare.com |

> Quick Tunnel은 VM 재시작 시 URL이 바뀝니다. 고정 URL을 원하면 도메인 구매 후 Named Tunnel로 업그레이드 필요.

---

## VM 재시작 시 해야 할 작업

```bash
# 1. 터널 실행 (URL이 바뀜, 팀원에게 새 URL 공유 필요)
nohup /usr/local/bin/cloudflared tunnel --url http://localhost:8082 > /tmp/cf-general.log 2>&1 &
nohup /usr/local/bin/cloudflared tunnel --url http://localhost:8929 > /tmp/cf-gitlab.log 2>&1 &
nohup /usr/local/bin/cloudflared tunnel --url http://localhost:8080 > /tmp/cf-jenkins.log 2>&1 &

# 2. 새 URL 확인
grep -o 'https://.*trycloudflare.com' /tmp/cf-general.log
grep -o 'https://.*trycloudflare.com' /tmp/cf-gitlab.log
grep -o 'https://.*trycloudflare.com' /tmp/cf-jenkins.log
```

> 컨테이너(GitLab, Jenkins, 앱)들은 `--restart always` 옵션으로 자동 시작됩니다.

---

## STEP별 구축 과정

### STEP 1. UTM 디스크 용량 확장

**왜 했나?**
Rocky Linux VM 기본 디스크가 8GB → Docker 이미지 설치 중 공간 부족 오류

**맥에서 (VM 종료 후)**
```bash
# VM 디스크 파일 경로로 이동
cd ~/Library/Containers/com.utmapp.UTM/Data/Documents/Rocky\ Linux.utm/Data/

# 50GB로 확장
qemu-img resize 디스크파일.img 50G
```

**Rocky Linux에서 (VM 재시작 후)**
```bash
sudo dnf install -y cloud-utils-growpart  # growpart 도구 설치
sudo growpart /dev/vda 3                  # 파티션 확장
sudo pvresize /dev/vda3                   # LVM 물리 볼륨 확장
sudo lvextend -l +100%FREE /dev/mapper/rl_lee-root  # 논리 볼륨 확장
sudo xfs_growfs /                         # 파일시스템 확장
df -h                                     # 확인 (48G 이상이면 성공)
```

**개념 정리**
- LVM: 물리 디스크를 유연하게 나눠쓰는 기술 (물리디스크 → PV → VG → LV 순서)
- xfs_growfs: 파일시스템 자체를 새 크기에 맞게 늘림

---

### STEP 2. 방화벽 설정

**왜 했나?**
Rocky Linux는 기본적으로 모든 외부 접근 차단. 필요한 포트를 열어야 함.

```bash
sudo firewall-cmd --permanent --add-port=80/tcp    # HTTP
sudo firewall-cmd --permanent --add-port=443/tcp   # HTTPS
sudo firewall-cmd --permanent --add-port=8929/tcp  # GitLab
sudo firewall-cmd --permanent --add-port=8080/tcp  # Jenkins
sudo firewall-cmd --permanent --add-port=8081/tcp  # match 서버
sudo firewall-cmd --permanent --add-port=8082/tcp  # general 서버
sudo firewall-cmd --permanent --add-port=50000/tcp # Jenkins agent
sudo firewall-cmd --reload
sudo firewall-cmd --list-ports  # 확인
```

**알아두기**
- Docker는 iptables를 직접 수정하므로 Docker 포트는 firewall-cmd에 안 보여도 자동으로 열림
- `--permanent`: 재부팅 후에도 유지

---

### STEP 3. Docker 설치

**왜 했나?**
GitLab, Jenkins, 앱 서버 모두 Docker 컨테이너로 실행하기 위해

```bash
sudo dnf config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo dnf install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
sudo systemctl enable --now docker  # 부팅 시 자동 시작 + 즉시 시작
sudo usermod -aG docker $USER       # sudo 없이 docker 사용 가능하게
newgrp docker                       # 그룹 변경 즉시 적용
docker --version                    # 확인
```

---

### STEP 4. GitLab 설치

**왜 했나?**
GitHub는 외부 서비스라 Jenkins와 내부 연동 복잡. 우리 서버에 직접 GitLab을 설치하면 완전한 내부 CI/CD 가능.

```bash
sudo mkdir -p /srv/gitlab/config /srv/gitlab/logs /srv/gitlab/data

docker run -d \
  --name gitlab \
  --restart always \
  -p 8929:8929 \           # 호스트포트:컨테이너포트
  -p 2424:22 \             # SSH용
  -v /srv/gitlab/config:/etc/gitlab \    # 설정 파일 저장
  -v /srv/gitlab/logs:/var/log/gitlab \  # 로그 저장
  -v /srv/gitlab/data:/var/opt/gitlab \  # 데이터 저장
  -e GITLAB_OMNIBUS_CONFIG="external_url 'http://192.168.64.7:8929'" \
  gitlab/gitlab-ce:latest

# 초기 비밀번호 확인 (기동 5분 후)
docker exec gitlab cat /etc/gitlab/initial_root_password
```

**주의사항**
- `external_url`의 포트와 Docker 포트 매핑 반드시 일치 (`8929:8929`)
- 기동까지 5~10분 소요
- 초기 비밀번호 파일은 24시간 후 자동 삭제

---

### STEP 5. Jenkins 설치

**왜 했나?**
코드 push 시 자동 빌드 및 배포를 위해

```bash
sudo mkdir -p /srv/jenkins
sudo chown 1000:1000 /srv/jenkins  # Jenkins 컨테이너 내부 사용자 ID

DOCKER_GID=$(stat -c '%g' /var/run/docker.sock)  # Docker 그룹 ID 확인

docker run -d \
  --name jenkins \
  --restart always \
  -p 8080:8080 \
  -p 50000:50000 \
  -v /srv/jenkins:/var/jenkins_home \              # Jenkins 데이터 저장
  -v /var/run/docker.sock:/var/run/docker.sock \   # 호스트 Docker 제어 가능하게
  --group-add $DOCKER_GID \                        # Docker 소켓 권한 부여
  jenkins/jenkins:lts

# 초기 비밀번호
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

**핵심 포인트**
- `/var/run/docker.sock` 마운트: Jenkins가 호스트의 Docker를 직접 제어할 수 있게 해줌
- `--group-add $DOCKER_GID`: Jenkins 컨테이너가 Docker 소켓에 접근 가능
- Jenkins 내부에 Docker CLI 설치 필요:
  ```bash
  docker exec -u root jenkins bash -c "curl -fsSL https://get.docker.com | sh"
  ```

---

### STEP 6. GitHub → GitLab으로 코드 이전

**GitLab Admin에서 import 허용**
Admin Area → Settings → General → Import and export settings → Repository by URL 체크

**GitLab에서 프로젝트 import**
New Project → Import project → Repository by URL → `https://github.com/SHSWAcademy/STO`

**맥에서 GitLab을 remote로 추가**
```bash
git remote add gitlab http://root@192.168.64.7:8929/root/STO.git
git push gitlab main
```

앞으로 push는 두 곳 모두:
```bash
git push origin main   # GitHub (백업)
git push gitlab main   # GitLab (Jenkins 트리거)
```

**Rocky Linux에서 코드 클론**
```bash
git clone http://root@192.168.64.7:8929/root/STO.git /srv/sto
```

---

### STEP 7. Dockerfile 작성

**왜 했나?**
Docker가 Spring Boot 앱을 빌드하고 실행하려면 방법을 적은 Dockerfile 필요

각 서버(main, match, batch)에 동일한 패턴:

```dockerfile
# 1단계: 빌드 (JDK 필요)
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY . .
RUN ./gradlew bootJar -x test  # 테스트 제외하고 실행 가능한 JAR 생성

# 2단계: 실행 (가벼운 JRE만 사용)
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**개념 정리**
- 멀티스테이지 빌드: 빌드용(JDK)과 실행용(JRE) 이미지를 분리 → 최종 이미지 크기 감소
- `bootJar`: Spring Boot 전용 Gradle 태스크, 실행 가능한 fat JAR 생성
- `-x test`: 테스트 건너뜀 (CI 환경에서 DB 없이 빌드 가능하게)

---

### STEP 8. docker-compose.yml 수정

**수정한 내용들**

1. Dockerfile 경로 수정 (context를 각 서버 디렉토리로)
```yaml
general:
  build:
    context: ./server/main   # 전체 server/ 가 아닌 각 서버 디렉토리
    dockerfile: Dockerfile
```

2. 없는 서비스(ganache) 의존성 제거
```yaml
# 제거
ganache:
  condition: service_started
```

3. nginx 임시 비활성화 (클라이언트 코드 미완성)
```yaml
# nginx:  ← 주석 처리
```

4. general 포트를 8082로 변경 (Jenkins가 8080 사용 중)
```yaml
ports:
  - "8082:8080"  # 호스트8082 → 컨테이너8080
```

---

### STEP 9. Spring Boot Docker 프로파일 설정

**왜 했나?**
`application.properties`가 H2(인메모리 테스트 DB)로 설정되어 있어서 Docker에서 PostgreSQL 연결 실패

**각 서버에 `application-docker.properties` 추가**
```properties
spring.datasource.url=jdbc:postgresql://postgres:5432/sto
spring.datasource.username=sto
spring.datasource.password=sto1234
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

spring.h2.console.enabled=false

spring.data.redis.host=redis
spring.data.redis.port=6379
```

**docker-compose.yml에서 프로파일 활성화**
```yaml
environment:
  SPRING_PROFILES_ACTIVE: docker
```

**개념 정리**
- Spring Profile: 환경별(개발/운영/Docker) 다른 설정 파일을 적용하는 기능
- `application-docker.properties`는 `SPRING_PROFILES_ACTIVE=docker`일 때만 로드
- 로컬 개발 시에는 기존 `application.properties`(H2)가 그대로 사용됨

---

### STEP 10. Jenkinsfile 작성

프로젝트 루트에 `Jenkinsfile` 생성:

```groovy
pipeline {
    agent any
    stages {
        stage('Checkout') {
            steps {
                checkout scm   // GitLab에서 최신 코드 받아옴
            }
        }
        stage('Build') {
            steps {
                sh 'docker compose build --no-cache'  // 도커 이미지 빌드
            }
        }
        stage('Deploy') {
            steps {
                sh 'docker compose up -d'  // 컨테이너 실행/교체
            }
        }
    }
    post {
        success { echo '배포 성공' }
        failure { echo '배포 실패' }
    }
}
```

**Jenkins에서 파이프라인 생성**
1. New Item → Pipeline → 이름: STO
2. Pipeline script from SCM 선택
3. SCM: Git
4. Repository URL: `http://192.168.64.7:8929/root/STO.git`
5. Credentials: GitLab Access Token
6. Branch: `*/main`
7. Script Path: `Jenkinsfile`
8. 저장

**GitLab Access Token 발급 방법**
GitLab → 우상단 프로필 → Edit profile → Access Tokens → `read_repository` 권한으로 생성

---

### STEP 11. GitLab Webhook 설정

**왜 했나?**
push 시 Jenkins가 자동으로 빌드를 시작하게 하기 위해

**Jenkins에서 GitLab Plugin 설치**
Jenkins 관리 → Plugins → Available plugins → GitLab 검색 → 설치

**Jenkins 파이프라인 설정**
STO 파이프라인 → 구성 → Build Triggers → "Build when a change is pushed to GitLab" 체크 → URL 복사

**GitLab에서 Webhook 등록**
1. GitLab Admin → Settings → Network → Outbound requests → "Allow requests to the local network" 체크
2. STO 프로젝트 → Settings → Webhooks
3. URL: `http://172.17.0.1:8080/project/STO` (172.17.0.1은 Docker 브리지 게이트웨이)
4. Push events 체크, SSL verification 해제
5. Add webhook

**Jenkins 익명 빌드 권한 허용**
Jenkins 관리 → Security → Authorization → Anyone can do anything

---

### STEP 12. Cloudflare Quick Tunnel 설치

**왜 했나?**
팀원이 외부(집, 카페 등)에서 서버에 접속할 수 있게 하기 위해

```bash
# 설치 (ARM64 - M1/M2 맥 기준)
sudo curl -L https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-arm64 \
  -o /usr/local/bin/cloudflared
sudo chmod +x /usr/local/bin/cloudflared

# 터널 실행 (백그라운드)
nohup /usr/local/bin/cloudflared tunnel --url http://localhost:8082 > /tmp/cf-general.log 2>&1 &
nohup /usr/local/bin/cloudflared tunnel --url http://localhost:8929 > /tmp/cf-gitlab.log 2>&1 &
nohup /usr/local/bin/cloudflared tunnel --url http://localhost:8080 > /tmp/cf-jenkins.log 2>&1 &

# URL 확인
grep -o 'https://.*trycloudflare.com' /tmp/cf-general.log
grep -o 'https://.*trycloudflare.com' /tmp/cf-gitlab.log
grep -o 'https://.*trycloudflare.com' /tmp/cf-jenkins.log
```

---

## 서비스 접속 정보

### 내부 접속 (같은 와이파이)
| 서비스 | URL | 계정 |
|--------|-----|------|
| GitLab | http://192.168.64.7:8929 | root / (초기 비밀번호) |
| Jenkins | http://192.168.64.7:8080 | nippyclouding / (설정한 비밀번호) |
| General API Swagger | http://192.168.64.7:8082/swagger | - |
| Match API | http://192.168.64.7:8081 | - |

### 외부 접속 (Cloudflare, 어디서든)
> VM 재시작마다 URL 바뀜. 재시작 후 위의 터널 명령어 실행 후 URL 확인 필요.

---

## 트러블슈팅 모음

| 문제 | 원인 | 해결 |
|------|------|------|
| `no space left on device` | VM 디스크 부족 | qemu-img로 디스크 확장 후 LVM 확장 |
| GitLab 포트 연결 거부 | external_url 포트와 Docker 포트 불일치 | `8929:8929`로 포트 매핑 일치 |
| `./gradlew: not found` | build context 경로 잘못됨 | context를 각 서버 디렉토리로 변경 |
| Hibernate Dialect 오류 | H2 설정이 PostgreSQL과 충돌 | `application-docker.properties`로 분리 |
| nginx mount 실패 | Jenkins 컨테이너 경로를 호스트에서 못 찾음 | `sudo ln -s /srv/jenkins /var/jenkins_home` |
| Webhook 403 오류 | Jenkins 익명 빌드 권한 없음 | Security → Anyone can do anything |
| Webhook invalid url | GitLab이 로컬 IP 차단 | `172.17.0.1` (Docker 브리지 IP) 사용 |
| docker.sock permission denied | Jenkins가 Docker API 접근 불가 | `--group-add $DOCKER_GID`로 컨테이너 재생성 |

---

## 남은 작업

### 1. React 클라이언트 배포
클라이언트 코드 완성 후:
```bash
cd client
npm install
npm run build  # dist/ 폴더 생성
```

docker-compose.yml에서 nginx 주석 해제:
```yaml
nginx:
  image: nginx:alpine
  ports:
    - "80:80"
  volumes:
    - ./client/dist:/usr/share/nginx/html:ro
    - ./nginx/nginx.conf:/etc/nginx/conf.d/default.conf:ro
```

### 2. /api/health 엔드포인트 추가 (권장)
General 서버에 헬스체크 엔드포인트 추가 시 더 안정적인 의존성 관리 가능:
```java
@RestController
public class HealthController {
    @GetMapping("/api/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("ok");
    }
}
```

### 3. Cloudflare URL 고정 (도메인 구매 시)
도메인이 있으면 Named Tunnel로 URL 영구 고정 가능.
연 $8~10 정도의 도메인 구매 후 Cloudflare에 연결하면 됨.

---

## 다른 프로젝트에 적용하는 법

1. **STEP 1~5** (디스크, 방화벽, Docker, GitLab, Jenkins)는 서버에 한 번만 하면 됨
2. 새 프로젝트의 각 서버에 **Dockerfile** 작성
3. 프로젝트 루트에 **Jenkinsfile** 작성
4. **docker-compose.yml** 작성
5. GitLab에 새 프로젝트 생성 후 코드 push
6. Jenkins에서 새 파이프라인 생성 후 Webhook 연결

---

## CI/CD 연결 해제 및 다른 플랫폼으로 이전하는 법

> 현재 파이프라인은 **GitLab → Jenkins → Docker** 구조.
> 다른 CI/CD 플랫폼(GitHub Actions, GitLab CI 등)으로 옮기거나 파이프라인을 해제하고 싶을 때 참고.

### 해제 순서

**1. GitLab Webhook 제거 (트리거 끊기)**

가장 먼저 해야 할 것. 이걸 지우면 push해도 Jenkins가 더 이상 반응하지 않음.

```
GitLab STO 프로젝트 → Settings → Webhooks
→ http://172.17.0.1:8080/project/STO 항목 삭제
```

**2. Jenkins 파이프라인 삭제**

```
Jenkins(:8080) → STO 파이프라인 → Delete Pipeline
```

Webhook만 지워도 트리거가 없어지지만, 깔끔하게 정리하려면 삭제.

**3. GitLab remote 제거 (선택)**

로컬에 `gitlab`이라는 remote가 등록되어 있음. 새 플랫폼에서는 origin(GitHub)만 쓰면 되므로 제거.

```bash
git remote -v              # 현재 remote 확인
git remote remove gitlab   # gitlab remote 제거
```

**4. Jenkinsfile 교체**

프로젝트 루트의 `Jenkinsfile`은 새 플랫폼으로 옮기면 필요 없어짐.
삭제하거나 새 CI 설정 파일로 교체.

```bash
rm Jenkinsfile
```

---

### 새 플랫폼으로 이전 시 참고

| 플랫폼 | 특징 | 설정 파일 위치 |
|--------|------|----------------|
| **GitHub Actions** | origin(GitHub) push 시 자동 트리거. 별도 서버 불필요. | `.github/workflows/deploy.yml` |
| **GitLab CI** | Jenkins 빼고 GitLab 서버만 그대로 유지. 가장 간단한 이전 방법. | `.gitlab-ci.yml` (프로젝트 루트) |
| **AWS CodePipeline** | 클라우드 인프라로 전환 시. ECR + ECS/EKS와 연동. | AWS 콘솔에서 구성 |
| **GCP Cloud Build** | GCP 환경 사용 시. `cloudbuild.yaml`로 파이프라인 정의. | `cloudbuild.yaml` (프로젝트 루트) |

**중요**: `Dockerfile`과 `docker-compose.yml`은 플랫폼에 무관하게 그대로 재사용 가능.
CI 설정 파일(`Jenkinsfile`)만 새 플랫폼 형식으로 교체하면 됨.

---

### GitHub Actions로 이전하는 예시

Jenkins → GitHub Actions로 가장 많이 이전하는 케이스. 참고용 기본 구조:

```yaml
# .github/workflows/deploy.yml
name: Deploy

on:
  push:
    branches: [ main ]

jobs:
  deploy:
    runs-on: ubuntu-latest   # GitHub이 제공하는 빌드 서버 (무료)
    steps:
      - uses: actions/checkout@v3

      - name: Build Docker images
        run: docker compose build --no-cache

      - name: Deploy
        run: docker compose up -d
```

> 단, GitHub Actions에서 실제 서버(맥미니 VM)에 배포하려면 Self-hosted Runner 설정이 필요하거나,
> SSH로 원격 접속 후 배포하는 방식을 추가해야 함.

---

## 컴퓨터 켜고 Rocky Linux 접속 후 해야 할 작업

### 1. Cloudflare 터널 실행

컨테이너(GitLab, Jenkins, 앱 서버)는 `--restart always`로 자동 시작되므로 따로 실행할 필요 없음.
터널만 수동으로 실행하면 됨.

```bash
nohup /usr/local/bin/cloudflared tunnel --url http://localhost:8082 > /tmp/cf-general.log 2>&1 &
nohup /usr/local/bin/cloudflared tunnel --url http://localhost:8929 > /tmp/cf-gitlab.log 2>&1 &
nohup /usr/local/bin/cloudflared tunnel --url http://localhost:8080 > /tmp/cf-jenkins.log 2>&1 &
```

### 2. 새 URL 확인

```bash
grep -o 'https://.*trycloudflare.com' /tmp/cf-general.log
grep -o 'https://.*trycloudflare.com' /tmp/cf-gitlab.log
grep -o 'https://.*trycloudflare.com' /tmp/cf-jenkins.log
```

### 3. 팀원에게 URL 공유

터널을 실행할 때마다 URL이 바뀌므로, 실행 후 반드시 팀원에게 새 URL을 공유해야 함.

- **General API URL** (첫 번째로 출력된 URL) → 팀원이 API 서버에 접속할 때 사용
- GitLab, Jenkins URL은 본인만 사용하면 됨

> URL은 맥미니가 켜져 있고 VM이 실행 중일 때만 유효함. 맥미니가 꺼지면 접속 불가.

---

## git push gitlab main 했을 때 일어나는 일

```
① 맥북 터미널에서 push
   git push gitlab main

② Rocky Linux VM 안의 GitLab(192.168.64.7:8929)에 코드가 저장됨

③ GitLab이 push를 감지하고 Jenkins에 Webhook(HTTP 요청)을 자동으로 전송
   → Jenkins URL: http://172.17.0.1:8080/project/STO

④ Jenkins가 Webhook을 받고 자동으로 파이프라인 실행 시작
   → GitLab에서 최신 코드를 받아옴 (git checkout)

⑤ Jenkins가 Docker 이미지 빌드
   → docker compose build --no-cache
   → 각 서버(main, match, batch)의 Dockerfile을 읽어 JAR 빌드 후 이미지 생성

⑥ Jenkins가 컨테이너 재시작
   → docker compose up -d
   → 새 이미지로 컨테이너 교체

⑦ 배포 완료
   → 팀원들이 Cloudflare URL로 새 버전에 바로 접속 가능
```

**핵심**: `git push gitlab main` 한 번이면 ①~⑦이 자동으로 일어남.
GitHub에도 백업하려면 `git push origin main`을 추가로 실행.
