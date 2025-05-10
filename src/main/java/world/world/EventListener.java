package world.world;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class EventListener implements Listener {

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        ChatPermissionManager.handleEntityDeath(event);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        ChatPermissionManager.handlePlayerChat(event);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        ChatPermissionManager.handlePlayerInteract(event);
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        ChatPermissionManager.handlePlayerCommand(event);
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent) { // 데미지를 준 엔티티가 있는 경우에만 처리
            EntityDamageByEntityEvent damageByEntityEvent = (EntityDamageByEntityEvent) event;

            if (event.getEntity() instanceof Player && damageByEntityEvent.getDamager() instanceof Player) {
                // 데미지를 받은 엔티티와 데미지를 준 주체가 모두 플레이어인 경우
                Player player = (Player) event.getEntity();
                ChatPermissionManager.handlePlayerDamage(player); // 데미지를 받은 플레이어 처리
            }
        }
    }
}