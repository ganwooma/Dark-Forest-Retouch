package world.world;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinListener implements Listener {
    private final BeaconMain plugin;
    private final FamilyManager familyManager;
    private final PlayerReviveManager reviveManager;
    private final DiscordManager discordManager;

    public PlayerJoinListener(BeaconMain plugin, FamilyManager familyManager, PlayerReviveManager reviveManager, DiscordManager discordManager) {
        this.plugin = plugin;
        this.familyManager = familyManager;
        this.reviveManager = reviveManager;
        this.discordManager = discordManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();

        // 기존 코드 유지
        FamilyManager.Family family = FamilyManager.getFamily(playerName);
        if (family != null) {
            player.sendMessage(ChatColor.GREEN + "당신은 " + family.getLeader() + " 가문의 멤버입니다.");
            plugin.setTeamColor(player, family.getTeamColor());

        } else {
            player.sendMessage(ChatColor.YELLOW + "당신은 아직 가문에 소속되어 있지 않습니다.");
        }
    }
}