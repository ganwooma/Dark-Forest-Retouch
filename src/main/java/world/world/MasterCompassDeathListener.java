package world.world;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class MasterCompassDeathListener extends JavaPlugin implements Listener {

    private final MasterCompassManager masterCompassManager;

    public MasterCompassDeathListener(MasterCompassManager masterCompassManager) {
        this.masterCompassManager = masterCompassManager;
    }

    @EventHandler
    public void onDragonDeath(EntityDeathEvent event) {
        // 죽은 엔티티가 엔드 드래곤인지 확인
        if (event.getEntity() instanceof EnderDragon) {
            // 드래곤을 처치한 플레이어 가져오기
            Player killer = event.getEntity().getKiller();
            if (killer != null) {
                // 마스터 컴퍼스를 생성하고 지급
                ItemStack masterCompass = masterCompassManager.createMasterCompass();
                killer.getInventory().addItem(masterCompass);
                killer.sendMessage("§6 축하합니다! 엔드 드래곤을 처치하여 마스터 컴퍼스를 획득했습니다!");
                for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                    player.sendMessage("§41분 뒤에 엔드에서 나가집니다!");
                }
                // 1분 뒤 작업 예약
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        // End 월드 가져오기
                        World endWorld = Bukkit.getWorld("world_the_end");
                        if (endWorld == null) return;

                        // End에 있는 모든 플레이어 반복
                        for (Player player : endWorld.getPlayers()) {
                            Plugin plugin;
                            FileConfiguration config = getConfig();
                            config.set("preventEnder", true);  // preventEnder 값을 true 또는 false로 설정
                            saveConfig();  // 변경된 설정을 config.yml에 저장

                            FamilyManager.Family family = FamilyManager.getFamily(player.getName());

                            if (family != null) {
                                Location beaconLocation = family.getBeaconLocation();

                                if (beaconLocation != null) {
                                    Location respawnLocation = beaconLocation.clone().subtract(1, 0, 0); // 위치 조정

                                    // 아래 블록이 배리어가 아니면 위치 적용
                                    Location belowLocation = respawnLocation.clone().add(0, -1, 0);
                                    if (belowLocation.getBlock().getType() != Material.BARRIER) {
                                        // 리스폰 위치 설정 → 즉시 효과가 있는 건 아님, death 이벤트에서 사용됨
                                        player.setBedSpawnLocation(respawnLocation, true);
                                    }
                                }
                            }
                        }
                    }
                }.runTaskLater(this, 20 * 60); // 60초(20틱 * 60)
            }
        }
    }
}