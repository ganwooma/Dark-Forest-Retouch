package world.world;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class MasterCompassManager {

    private final JavaPlugin plugin;
    private final FamilyManager familyManager;

    public MasterCompassManager(JavaPlugin plugin, FamilyManager familyManager) {
        this.plugin = plugin;
        this.familyManager = familyManager;
    }

    // 마스터 컴퍼스 아이템 생성
    public ItemStack createMasterCompass() {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6마스터 컴퍼스");
            meta.setLore(java.util.Arrays.asList(
                    "§c사용 후 사라집니다."
            ));
            meta.addEnchant(Enchantment.LUCK, 1, true); // 인첸트로 빛나게 설정
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);  // 인첸트 정보 숨기기
            compass.setItemMeta(meta);
        }
        return compass;
    }

    // 마스터 컴퍼스를 사용할 때 동작
    public void useMasterCompass(Player player) {
        Location beaconLocation = findNearestBeacon(player);

        if (beaconLocation == null) {
            player.sendMessage("§c가까운 신호기를 찾을 수 없습니다!");
            return;
        }

        // 신호기를 가리키는 빠른 불꽃 파티클 출력
        shootFlameParticles(player.getLocation(), beaconLocation);

        // 아이템 제거
        player.getInventory().setItemInMainHand(null);
    }

    // 가장 가까운 신호기를 찾는 메서드
    private Location findNearestBeacon(Player player) {
        Map<String, Location> beacons = new HashMap<>();

        for (FamilyManager.Family family : FamilyManager.getAllFamilies()) {
            Location location = family.getBeaconLocation();
            if (location != null && location.getWorld().equals(player.getWorld())) {
                beacons.put(family.getLeader(), location);
            }
        }

        double closestDistance = Double.MAX_VALUE;
        Location closestBeacon = null;

        for (Location location : beacons.values()) {
            double distance = location.distance(player.getLocation());
            if (distance < closestDistance) {
                closestDistance = distance;
                closestBeacon = location;
            }
        }

        return closestBeacon;
    }

    // 신호기 방향으로 빠르게 불꽃 파티클을 이동시키는 메서드
    private void shootFlameParticles(Location start, Location end) {
        new FlameParticleTask(start, end).runTaskTimer(plugin, 0, 1); // 0틱 지연 후 1틱 간격으로 실행
    }
}