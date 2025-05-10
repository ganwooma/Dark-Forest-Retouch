package world.world;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerJoinListener implements Listener {

    private final JavaPlugin plugin;
    private final FamilyManager familyManager;
    private final PlayerReviveManager reviveManager;

    public PlayerJoinListener(JavaPlugin plugin, FamilyManager familyManager, PlayerReviveManager reviveManager) {
        this.plugin = plugin;
        this.familyManager = familyManager;
        this.reviveManager = reviveManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // 플레이어가 서버에 접속했을 때 수행할 코드
        reviveManager.checkBanOnLogin(event.getPlayer());

    }
}