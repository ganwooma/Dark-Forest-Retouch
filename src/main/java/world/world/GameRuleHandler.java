package world.world;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class GameRuleHandler implements Listener {

    private final JavaPlugin plugin;

    public GameRuleHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        // 이벤트 등록
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        initializeWorldSettings();
    }

    /**
     * 월드 초기화 시 설정 (난이도, 기본 게임룰 등).
     */
    private void initializeWorldSettings() {
        for (World world : Bukkit.getWorlds()) {
            setWorldGameRules(world);
        }
    }

    /**
     * 해당 월드에 기본 게임룰 적용
     */
    private void setWorldGameRules(World world) {
        // 모든 월드의 난이도를 Hard로 설정
        world.setDifficulty(Difficulty.HARD);

        // 플레이어 아이템 소지 유지
        world.setGameRule(GameRule.KEEP_INVENTORY, true);

        // 좌표 숨김 설정
        world.setGameRule(GameRule.REDUCED_DEBUG_INFO, true);

        // 팬텀 스폰을 막기 위해 doInsomnia 게임룰 설정
        world.setGameRule(GameRule.DO_INSOMNIA, false);
    }

    /**
     * 플레이어가 차원을 변경했을 때 이벤트 처리.
     */
    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        World world = event.getPlayer().getWorld();
        setWorldGameRules(world); // 이동한 차원에서도 동일한 게임룰 설정
    }
}