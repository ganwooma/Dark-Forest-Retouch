# HOW TO SERVER SETUP
README 파일을 먼저 모두 읽고 보시길 바랍니다.

https://ganwooma.github.io/build

# 버전
Minecraft 1.20.4

Spigot Plugin (Paper도 사용 가능합니다)

SDK 21 / JDK 21 사용


# 원본 프로젝트
https://github.com/didi78579/-


# 러스트 시스템 플러그인 (함께 사용하세요)
https://github.com/ganwooma/Rust-System-Plugin/tree/JAR


# 기본적인 사용법 & 업그레이드 플러그인
https://www.koreaminecraft.net/plugins/3968610


# 추가된 기능
### 가문별 디스코드 채널 설정
각 가문마다 별도의 디스코드 채널을 설정할 수 있습니다.
/discord set <가문이름> <채널ID> 명령어로 설정 가능.


### 비콘 침입 알림
다른 가문의 플레이어가 비콘 근처(50m 이내)에 접근하면 해당 가문의 디스코드 채널에 알림이 전송됩니다.

게임 내에서도 해당 가문 멤버들에게 알림이 전송됩니다.


### 서버 시작 / 종료 알림
서버가 시작되거나 종료될 때 기본 채널에 알림이 전송됩니다.


### 엔드 탐험 금지
플레이어가 드래곤을 잡으면 1분 후에 엔드에 있던 모든 플레이어가 밖으로 쫒겨납니다.
그리고 나서 엔드가 잠깁니다.


### 엔드 잠금
/EndBlock on|off로 엔드 잠금
off시 엔더드래곤이 스폰합니다.
기본적으로 off이므로 엔더드래곤이 잡힌 후에
알아서 off하십시오.


### 명령어 피드백
명령어 피드백을 비활성시킵니다.


### 발전과제
플레이어의 발전과제 알람을 비활성시킵니다.


### Debug Info
F3을 눌렀을 때 Debug Info가 나옵니다.


### 플레이어 참여 / 나감 알람
플레이어의 참여 알람과 나감 알람이 나오지 않습니다.


### 폭발 방지
지옥 / 엔드에서 침대가 설치되지 않습니다.

오버월드 / 엔드에서 리스폰 정박기가 설최되지 않습니다.

오버월드 / 지옥 / 엔드에서 엔드 크리스탈이 설최되지 않습니다.


### 겉날개 획득
겉날개를 획득하지 못합니다.


### 가문 최대 수
가문 최대 수를 6가문에서 10가문으로 늘렸습니다.

# 설정 방법
src/main/resource/config.yml 설정

```yml
discord:
  enabled: true
  token: "디스코드 토큰"
  default-channel-id: "서버 상태를 알려줄 디스코드 채널"
  family-channels:
  # 각 가문별 채널 ID는 이곳에 저장됩니다
  # 예: family_name: "채널ID"
  # 직접 저장하셔도 됩니다.
preventEnder: false  # 엔더 월드 진입 차단 여부 (true: 차단, false: 차단 안 함) false로 해놓으십시오
```


# 디스코드 봇 생성 및 토큰 획득

Discord Developer Portal에서 새 애플리케이션 생성

Bot 탭에서 봇 추가 및 토큰 확인

봇에 필요한 권한 (메시지 읽기 / 쓰기) 부여

서버에 봇 초대


# 채널 ID 확인 방법

디스코드 개발자 모드 활성화 (설정 → 고급 → 개발자 모드)

채널 우클릭 → ID 복사


# 명령어 설정

/discord set <가문이름> <채널ID> : 가문별 알림 채널 설정

/discord reload: 디스코드 설정 리로드


# 플레이어 밴 시간 조정
1시간 → 3분

# 플레이어 뽑기 가격
1회 → 16 다이아

2회 → 32 다이아

3회 → 64 다이아


# 기타 설명
플레이 가능한 인원은 4x4 16명입니다.

2팀, 3팀으로도 플레이 가능합니다.


# 빌드
윈도우에 경우
```powershell Get-Process | Where-Object {$_.CPU -gt 100}
gradlew shadowJar
```
리눅스에 경우 (bash)
```bash
./gradlew shadowJar
```
로 빌드하십시오

# 플레이
Simple Voice Chat Mod와 Simple Voice Chat Plugin 사용 권장

CurseForge(mod) URL: https://www.curseforge.com/minecraft/mc-mods/simple-voice-chat

Modrinth(mod and plugin) URL: https://modrinth.com/plugin/simple-voice-chat

<h1>Made By didi78579 and ChatGPT and Claude</h1>
and me
