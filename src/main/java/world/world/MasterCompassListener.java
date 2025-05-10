package world.world;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class MasterCompassListener implements Listener {

    private final MasterCompassManager masterCompassManager;

    public MasterCompassListener(MasterCompassManager masterCompassManager) {
        this.masterCompassManager = masterCompassManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // 아이템이 마스터 컴퍼스인지 확인
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String itemName = item.getItemMeta().getDisplayName();
            if ("§6마스터 컴퍼스".equals(itemName)) {
                masterCompassManager.useMasterCompass(player);
                event.setCancelled(true); // 더 이상 기본 이벤트 처리 방지
            }
        }
    }
}