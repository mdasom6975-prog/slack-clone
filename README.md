# slack-clone

Java/Spring Boot로 만드는 Slack 클론 미니 프로젝트. 명세는 [`docs/PRD.md`](docs/PRD.md).

## 로컬 실행

JWT 서명 키는 비밀 값이라 코드와 설정 파일에 두지 않는다. 실행 전에 환경변수 `JWT_SECRET`(32바이트 이상)을 직접 지정해야 하며, 없으면 앱이 시작되지 않는다.

```bash
export JWT_SECRET="$(openssl rand -base64 48)"
./gradlew bootRun
```

- 테스트(`./gradlew test`, `./gradlew build`)는 테스트 전용 키를 `build.gradle`에서 주므로 별도 설정이 필요 없다.
- 이 값을 git에 올리거나 로그에 남기지 않는다.
