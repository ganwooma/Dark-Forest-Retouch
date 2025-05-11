package world.world;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BeaconProximityListener implements Listener {
    private final BeaconMain plugin;
    private final FamilyManager familyManager;
    private final DiscordManager discordManager;
    private static final int PROXIMITY_CHECK_DISTANCE = 50; // 비콘 감지 범위
    private static final int COOLDOWN_SECONDS = 60; // 알림 쿨다운

    // 플레이어별 알림 쿨다운 맵 (가문별 쿨다운)
    private final Map<UUID, Map<String, Long>> playerAlertCooldowns = new HashMap<>();

    public BeaconProximityListener(BeaconMain plugin, FamilyManager familyManager, DiscordManager discordManager) {
        this.plugin = plugin;
        this.familyManager = familyManager;
        this.discordManager = discordManager;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // 성능을 위해 위치가 실제로 변경됐을 때만 체크
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        String playerName = player.getName();

        // 플레이어가 가문에 속해 있는지 확인
        if (!FamilyManager.hasFamily(playerName)) {
            return;
        }

        // 플레이어의 가문 정보
        FamilyManager.Family playerFamily = FamilyManager.getFamily(playerName);
        if (playerFamily == null) {
            return;
        }

        // 모든 가문의 비콘 위치 확인
        for (FamilyManager.Family family : FamilyManager.getAllFamilies()) {
            // 자신의 가문 비콘은 제외
            if (family.getLeader().equals(playerFamily.getLeader())) {
                continue;
            }

            Location beaconLocation = family.getBeaconLocation();
            if (beaconLocation == null || !beaconLocation.getWorld().equals(player.getWorld())) {
                continue;
            }

            // 거리 계산
            double distance = beaconLocation.distance(player.getLocation());
            if (distance <= PROXIMITY_CHECK_DISTANCE) {
                // 이미 경고가 최근에 발송되었는지 확인 (쿨다운 체크)
                if (isOnCooldown(player.getUniqueId(), family.getLeader())) {
                    continue;
                }

                // 쿨다운 설정
                setCooldown(player.getUniqueId(), family.getLeader());

                // 디스코드로 알림 전송
                String alertMessage = ":warning: **경고!** " + playerFamily.getLeader() + " 가문의 " + playerName +
                        "님이 " + family.getLeader() + " 가문의 비콘 근처(" +
                        Math.round(distance) + "m)에 접근했습니다!";

                discordManager.sendFamilyAlert(family.getLeader(), alertMessage);

                // 해당 가문의 온라인 멤버들에게 인게임 알림
                for (String memberName : family.getMembers()) {
                    Player member = Bukkit.getPlayer(memberName);
                    if (member != null && member.isOnline()) {
                        member.sendMessage(ChatColor.RED + "경고! " + playerFamily.getLeader() +
                                " 가문의 " + playerName + "님이 비콘 근처에 접근했습니다! (" +
                                Math.round(distance) + "m)");
                    }
                }
            }
        }
    }

    private boolean isOnCooldown(UUID playerId, String familyLeader) {
        if (!playerAlertCooldowns.containsKey(playerId)) {
            return false;
        }

        Map<String, Long> familyCooldowns = playerAlertCooldowns.get(playerId);
        if (!familyCooldowns.containsKey(familyLeader)) {
            return false;
        }

        long lastAlertTime = familyCooldowns.get(familyLeader);
        return (System.currentTimeMillis() - lastAlertTime) < (COOLDOWN_SECONDS * 1000);
    }

    private void setCooldown(UUID playerId, String familyLeader) {
        playerAlertCooldowns.computeIfAbsent(playerId, k -> new HashMap<>())
                .put(familyLeader, System.currentTimeMillis());
    }
}