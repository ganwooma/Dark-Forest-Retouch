package world.world;

import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

public class MasterCompassDeathListener implements Listener {

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

            }
        }
    }
}