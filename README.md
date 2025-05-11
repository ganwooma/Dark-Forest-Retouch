# 버전
Minecraft 1.20.4

Spigot Plugin (Paper도 사용 가능합니다.)

SDK 21 / JDK 21 사용


# 원본 프로젝트
https://github.com/didi78579/-

# 기본적인 사용법
https://www.koreaminecraft.net/plugins/3968610

# 추가된 기능
### 가문별 디스코드 채널 설정
각 가문마다 별도의 디스코드 채널을 설정할 수 있습니다
/discord set <가문이름> <채널ID> 명령어로 설정 가능


### 비콘 침입 알림
다른 가문의 플레이어가 비콘 근처(50m 이내)에 접근하면 해당 가문의 디스코드 채널에 알림이 전송됩니다
게임 내에서도 해당 가문 멤버들에게 알림이 전송됩니다


### 접속 / 퇴장 알림
플레이어가 접속하거나 퇴장할 때 소속 가문의 디스코드 채널에 알림이 전송됩니다


### 서버 시작 / 종료 알림
서버가 시작되거나 종료될 때 기본 채널에 알림이 전송됩니다


### 설정 방법
src/main/resource/config.yml 설정

    yamldiscord:
      enabled: true
      token: "여기에_디스코드_봇_토큰_입력"
      default-channel-id: "기본_디스코드_채널_ID"
      family-channels:
        # 가문별 채널 ID는 자동으로 저장됩니다


### 디스코드 봇 생성 및 토큰 획득

Discord Developer Portal에서 새 애플리케이션 생성

Bot 탭에서 봇 추가 및 토큰 확인

봇에 필요한 권한 (메시지 읽기 / 쓰기) 부여

서버에 봇 초대


### 채널 ID 확인 방법

디스코드 개발자 모드 활성화 (설정 → 고급 → 개발자 모드)

채널 우클릭 → ID 복사


### 명령어 설정

/discord set <가문이름> <채널ID> : 가문별 알림 채널 설정

/discord reload: 디스코드 설정 리로드


### 플레이어 밴 시간 조정
1시간 → 1분

### 플레이어 뽑기 가격
1회 → 16 다이아

2회 → 32 다이아

3회 → 64 다이아


# 기타 설명
플레이 가능한 인원은 4x4 16명입니다.

2팀, 3팀으로도 플레이 가능합니다.


# 빌드
    ./gradlew shadowJar
로 빌드하십시오

<h1>Made By didi78579 and ChatGPT and Claude</h1>
