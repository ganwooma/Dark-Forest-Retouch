package world.world;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlayerReviveManager {
    private final Map<UUID, Long> bannedPlayers = new HashMap<>();
    private final JavaPlugin plugin;

    public PlayerReviveManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void banPlayer(UUID playerUUID, long banDuration) {
        bannedPlayers.put(playerUUID, System.currentTimeMillis() + banDuration);
    }

    public void checkBanOnLogin(Player player) {
        UUID playerUUID = player.getUniqueId();
        Long banEndTime = bannedPlayers.get(playerUUID);

        if (banEndTime != null && banEndTime > System.currentTimeMillis()) {
            long remainingMillis = banEndTime - System.currentTimeMillis();
            long seconds = (remainingMillis / 1000) % 60;
            long minutes = (remainingMillis / (1000 * 60)) % 60;
            long hours = (remainingMillis / (1000 * 60 * 60)) % 24;

            player.kickPlayer(String.format("밴된 상태입니다. 남은 시간: %02d분 %02d초", hours, minutes, seconds));
        } else {
            bannedPlayers.remove(playerUUID);
        }
    }

    public long getBanTimeRemaining(UUID playerUUID) {
        Long banEndTime = bannedPlayers.get(playerUUID);
        if (banEndTime != null && banEndTime > System.currentTimeMillis()) {
            return banEndTime - System.currentTimeMillis();
        }
        return 0;
    }

    public void unbanPlayer(UUID playerUUID) {
        bannedPlayers.remove(playerUUID);
    }

    public Map<UUID, Long> getBannedPlayers() {
        return bannedPlayers;
    }

    // 모든 밴된 플레이어들의 UUID를 가져오는 메서드
    public List<UUID> getAllBannedPlayers() {
        return bannedPlayers.keySet().stream().collect(Collectors.toList());
    }
}