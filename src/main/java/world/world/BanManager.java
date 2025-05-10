package world.world;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BanManager {
    private final Map<UUID, Long> bannedPlayers = new HashMap<>();

    // 플레이어를 밴하는 메서드
    public void banPlayer(UUID playerId, long durationMillis) {
        long banEndTime = System.currentTimeMillis() + durationMillis;
        // banEndTime을 데이터베이스나 어떤 형태로든 저장하는 로직 추가
        bannedPlayers.put(playerId, banEndTime);
    }
}