# 🌐 Planetgachi Mod

인게임에서 `/gachi` 명령어로 **Planetgachi 서버 아이템 시세**를 바로 조회할 수 있는 Fabric 클라이언트 모드입니다.

---

## 📋 요구 사항

| 항목 | 버전 |
|------|------|
| Minecraft | 1.20.1 |
| Fabric Loader | 0.15.0 이상 |
| Fabric API | 1.20.1 호환 버전 |

---

## ⚙️ 설치 방법

1. [Fabric 공식 사이트](https://fabricmc.net/use/)에서 **Fabric Loader** 설치 (기본런처 이용자만)
2. [Fabric API](https://modrinth.com/mod/fabric-api) 다운로드 후 `.minecraft/mods/` 폴더에 넣기 (기본런처 이용자만)
3. 상단 메뉴의 **[Releases]** 탭을 클릭합니다.
4. 최신 버전의 `.jar` 파일을 다운로드합니다.
5. 마인크래프트 실행기의 `mods` 폴더에 다운로드한 파일을 넣습니다.

---

## 💬 명령어

모든 명령어는 **클라이언트 전용**이며 서버에 패킷을 보내지 않습니다.

```
/gachi 가격조회 <아이템이름>
```

### 예시

```
/gachi 가격조회 다이아
/gachi 가격조회 네더라이트
/gachi 가격조회 인챈트
```

- 아이템 이름 **일부만 입력해도** 검색됩니다
- 상위 **5개** 결과까지 표시됩니다
- 연속 요청 방지를 위해 **1.5초 쿨다운**이 있습니다

---

## 🔨 직접 빌드하기

```bash
git clone https://github.com/본인아이디/planetgachi-mod.git
cd planetgachi-mod
./gradlew build
```

빌드된 JAR는 `build/libs/` 폴더에 생성됩니다.

> Windows는 `./gradlew` 대신 `gradlew.bat` 사용

---

##  GitHub Actions 자동 빌드

`main` 브랜치에 push하면 자동으로 빌드됩니다.  
`v*` 태그를 push하면 **GitHub Release에 JAR가 자동 첨부**됩니다.

```bash
git tag v1.0.0
git push origin v1.0.0
```

---

##  라이센스

이 프로젝트는 **Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)** 라이센스를 따릅니다.

[![CC BY-NC 4.0](https://licensebuttons.net/l/by-nc/4.0/88x31.png)](https://creativecommons.org/licenses/by-nc/4.0/)

### 허용
-  자유로운 **복사 및 재배포**
-  **수정 및 재작성** (출처 표기 시)
-  개인 및 비상업적 목적 사용

### 금지
- **상업적 이용** (판매, 유료 서비스 포함)
- 출처 표기 없는 배포

> 전문: [creativecommons.org/licenses/by-nc/4.0](https://creativecommons.org/licenses/by-nc/4.0/)
