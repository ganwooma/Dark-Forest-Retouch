package world.world;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.util.*;
import java.util.logging.Logger;

public class BeaconMain extends JavaPlugin implements Listener {
    private boolean preventEnder;
    private BanManager banManager;
    private static final int BEACON_EFFECT_RADIUS = 100;
    private static Set<String> allUsers = new HashSet<>();
    private PlayerReviveManager reviveManager;
    private FamilyGUIHandler familyGuiHandler;
    private ScoreboardManager scoreboardManager;
    private Scoreboard scoreboard;
    private FamilyManager familyManager;
    private DiscordManager discordManager; // 디스코드 매니저 추가
    private Map<String, Location> playerLastTpLocations = new HashMap<>();
    private Set<String> messageSentPlayers = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();
        preventEnder = config.getBoolean("preventEnder", true); // 설정 파일에서 preventEnder 값을 불러옴 (기본값 true)

        getServer().getPluginManager().registerEvents(this, this);

        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onJoin(PlayerJoinEvent event) {
                event.setJoinMessage(null); // 접속 메시지 숨기기
            }

            @EventHandler
            public void onQuit(PlayerQuitEvent event) {
                event.setQuitMessage(null); // 퇴장 메시지 숨기기
            }
        }, this);

        // 기존 코드 유지
        familyManager = new FamilyManager(this);
        banManager = new BanManager();
        reviveManager = new PlayerReviveManager(this);
        scoreboardManager = getServer().getScoreboardManager();
        scoreboard = scoreboardManager.getMainScoreboard();

        // 디스코드 매니저 초기화
        discordManager = new DiscordManager(this);

        PluginManager pm = getServer().getPluginManager();

        // 기존 코드 유지
        MasterCompassManager masterCompassManager = new MasterCompassManager(this, familyManager);
        pm.registerEvents(new MasterCompassListener(masterCompassManager), this);
        pm.registerEvents(new MasterCompassDeathListener(masterCompassManager), this);

        Logger logger = this.getLogger();
        familyGuiHandler = new FamilyGUIHandler(this, reviveManager, familyManager, logger);
        pm.registerEvents(familyGuiHandler, this);

        // 기존 이벤트 리스너 등록
        pm.registerEvents(new EventListener(), this);
        pm.registerEvents(new PlayerJoinListener(this, familyManager, reviveManager, discordManager), this);
        pm.registerEvents(new BeaconBlockEventHandler(this, familyGuiHandler, familyManager, reviveManager), this);
        pm.registerEvents(new ChatPermissionManager(), this);

        // 디스코드 관련 이벤트 리스너 등록
        pm.registerEvents(new BeaconProximityListener(this, familyManager, discordManager), this);

        // 기존 명령어 등록
        this.getCommand("getbeacon").setExecutor(this);
        this.getCommand("start").setExecutor(new StartGameCommand());
        this.getCommand("family").setExecutor(new FamilyCommand(familyManager));

        // 디스코드 명령어 등록
        this.getCommand("discord").setExecutor(new DiscordCommand(this, discordManager));

        // 게임 규칙 핸들러 초기화
        new GameRuleHandler(this);

        // 비콘 효과를 5초 주기로 적용 (기존 코드)
        new BukkitRunnable() {
            @Override
            public void run() {
                applyBeaconEffects();
            }
        }.runTaskTimer(this, 0, 20 * 5);

        // 기존 코드 유지
        familyGuiHandler.startGiftBoxUpdater();
        disableAchievements();
        disableCoordinates();
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (preventEnder && event.getTo().getWorld().getName().equals("world_the_end")) {
            event.setCancelled(true);  // 엔더 월드로 가는 텔레포트를 막음
            event.getPlayer().sendMessage("엔더 월드로 갈 수 없습니다!");
        }
    }


    @Override
    public void onDisable() {
        if (familyManager != null) {
            familyManager.saveFamilies();
            getLogger().info("Families saved!");
        }

        // 디스코드 연결 종료
        if (discordManager != null) {
            discordManager.shutdown();
            getLogger().info("Discord connection closed!");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (cmd.getName().equalsIgnoreCase("getbeacon")) {
                player.getInventory().addItem(new ItemStack(Material.BEACON, 1));
                return true;
            }
        }
        if (!(sender instanceof org.bukkit.entity.Player)) {
            sender.sendMessage("이 명령어는 플레이어만 사용할 수 있습니다.");
            return false;
        }

        org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;

        // OP 권한이 있는지 체크
        if (!player.isOp()) {
            player.sendMessage("이 명령어는 OP만 사용할 수 있습니다.");
            return false;
        }

        if (cmd.getName().equalsIgnoreCase("EndBlock")) {
            if (args.length == 1 && (args[0].equalsIgnoreCase("on") || args[0].equalsIgnoreCase("off"))) {
                FileConfiguration config = getConfig();
                if (args[0].equalsIgnoreCase("on")) {
                    preventEnder = true;  // 엔더 월드 차단 활성화
                    config.set("preventEnder", true);
                    player.sendMessage("엔더 월드 진입 차단이 활성화되었습니다.");
                    // End 월드에서 0, 100, 0 좌표 설정
                    Location location = new Location(Bukkit.getWorld("world_the_end"), 0, 100, 0);

                    // 드래곤 소환
                    EnderDragon dragon = (EnderDragon) location.getWorld().spawnEntity(location, EntityType.ENDER_DRAGON);

                    // 드래곤의 HP를 1024로 설정
                    dragon.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(1024);
                    dragon.setHealth(1024);  // HP를 설정
                } else if (args[0].equalsIgnoreCase("off")) {
                    preventEnder = false;  // 엔더 월드 차단 비활성화
                    config.set("preventEnder", false);
                    player.sendMessage("엔더 월드 진입 차단이 비활성화되었습니다.");
                }
                saveConfig();  // 설정 파일에 변경 사항 저장
                return true;
            }
            player.sendMessage("사용법: /EndBlock [on/off]");
        }
        return false;
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        Iterator<Block> blockIterator = event.blockList().iterator();
        while (blockIterator.hasNext()) {
            Block block = blockIterator.next();
            if (isBeaconBaseBlock(block) || isPartOfBeaconBase(block)) {
                blockIterator.remove();
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            Player killer = player.getKiller();

            banManager.banPlayer(player.getUniqueId(), 60000L);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        // 플레이어의 가문 정보 가져오기
        FamilyManager.Family family = FamilyManager.getFamily(player.getName());

        if (family != null) {
            Location beaconLocation = family.getBeaconLocation();

            if (beaconLocation != null) {
                Location respawnLocation = beaconLocation.clone();
                respawnLocation.setX(respawnLocation.getX() - 1); // 위치 조정

                // 배리어 블록 확인
                Location belowLocation = respawnLocation.clone().add(0, -1, 0); // 아래 블록
                if (belowLocation.getBlock().getType() != Material.BARRIER) {
                    event.setRespawnLocation(respawnLocation); // 배리어가 아닐 때 리스폰 위치 설정
                    playerLastTpLocations.put(player.getName(), beaconLocation);
                }
            }
        }
    }

    @EventHandler
    public void onBedPlace(BlockPlaceEvent event) {
        Material[] bedColors = {
                Material.RED_BED, Material.ORANGE_BED, Material.YELLOW_BED,
                Material.LIME_BED, Material.GREEN_BED, Material.CYAN_BED,
                Material.BLUE_BED, Material.PURPLE_BED, Material.MAGENTA_BED,
                Material.PINK_BED, Material.BROWN_BED, Material.GRAY_BED,
                Material.LIGHT_GRAY_BED, Material.BLACK_BED, Material.WHITE_BED
        };
        for (Material colorBed : bedColors) {
            if (event.getBlock().getType() == colorBed) {
                World world = event.getBlock().getWorld();
                if (world.getEnvironment() == World.Environment.NETHER || world.getEnvironment() == World.Environment.THE_END) {
                    event.setCancelled(true); // 네더, 엔드에서는 침대 설치를 막음
                }
            }
        }
    }

    // 리스폰 정박기 설치 제한: 오버월드, 엔드에서도 설치 가능하도록 설정
    @EventHandler
    public void onRespawnAnchorPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() == Material.RESPAWN_ANCHOR) {
            World world = event.getBlock().getWorld();
            if (world.getEnvironment() == World.Environment.NETHER || world.getEnvironment() == World.Environment.NORMAL || world.getEnvironment() == World.Environment.THE_END) {
                // 오버월드, 엔드에서 리스폰 정박기 설치 가능
            }
        }
    }

    // 엔더 크리스탈 소환 시 방지 (이벤트에서 소환된 크리스탈이 즉시 죽지 않도록 처리)
    @EventHandler
    public void onEnderCrystalSpawn(EntitySpawnEvent event) {
        if (event.getEntity() instanceof EnderCrystal) {
            event.setCancelled(true);
        }
    }

    private void applyBeaconEffects() {
        getServer().getWorlds().forEach(world -> {
            world.getPlayers().forEach(player -> {
                if (FamilyManager.hasFamily(player.getName())) {
                    Location playerLocation = player.getLocation();
                    FamilyManager.Family playerFamily = FamilyManager.getFamily(player.getName());
                    if (playerFamily != null) {
                        Location beaconLocation = playerFamily.getBeaconLocation();
                        if (beaconLocation != null) {
                            // 월드가 동일한지 확인
                            if (playerLocation.getWorld().equals(beaconLocation.getWorld())) {
                                double distance = playerLocation.distance(beaconLocation);
                                if (distance <= BEACON_EFFECT_RADIUS) {
                                    for (Player otherPlayer : world.getPlayers()) {
                                        if (otherPlayer.equals(player)) {
                                            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 120, 0, true, false));
                                            player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 120, 1, true, false));
                                            player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 120, 1, true, false));
                                        } else {
                                            FamilyManager.Family otherPlayerFamily = FamilyManager.getFamily(otherPlayer.getName());
                                            if (otherPlayerFamily != null && !otherPlayerFamily.equals(playerFamily)) {
                                                Location otherPlayerLocation = otherPlayer.getLocation();
                                                // 다른 플레이어도 같은 월드인지 확인
                                                if (beaconLocation.getWorld().equals(otherPlayerLocation.getWorld())
                                                        && otherPlayerLocation.distance(beaconLocation) <= BEACON_EFFECT_RADIUS) {
                                                    otherPlayer.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 120, 1, true, false));
                                                    otherPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 120, 0, true, false));
                                                    otherPlayer.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 120, 0, true, false));
                                                    otherPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 120, 1, true, false));
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            });
        });
    }

    public void setTeamColor(Player player, ChatColor teamColor) {
        if (teamColor != null && player != null) {
            Team team = scoreboard.getTeam(player.getName());
            if (team == null) {
                team = scoreboard.registerNewTeam(player.getName());
            }
            team.setColor(teamColor);
            team.addEntry(player.getName());
        }
    }

    private boolean isBeaconBaseBlock(Block block) {
        for (FamilyManager.Family family : FamilyManager.getAllFamilies()) {
            Location beaconLocation = family.getBeaconLocation();
            if (beaconLocation != null) {
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        Location baseBlockLocation = beaconLocation.clone().add(x, -1, z);
                        if (baseBlockLocation.getBlock().equals(block)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // 신호기 기반 블록인지 확인
        if (isBeaconBaseBlock(block)) {
            event.setCancelled(true); // 이벤트 취소
            player.sendMessage(ChatColor.RED + "신호기 기반 블록은 파괴할 수 없습니다!");
        }
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        List<Block> pushedBlocks = event.getBlocks();

        for (Block block : pushedBlocks) {
            // 신호기 혹은 신호기 기반 블록인지 확인
            if (isBeaconOrBase(block)) {
                event.setCancelled(true);
                break;
            }
        }
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        List<Block> retractedBlocks = event.getBlocks();

        for (Block block : retractedBlocks) {
            // 신호기 혹은 신호기 기반 블록인지 확인
            if (isBeaconOrBase(block)) {
                event.setCancelled(true);
                break;
            }
        }
    }

    private boolean isBeaconOrBase(Block block) {
        if (block.getType() == Material.BEACON) {
            return true; // 신호기인지 확인
        }

        // 신호기 기반 블록인지 확인
        for (FamilyManager.Family family : familyManager.getAllFamilies()) {
            if (family != null && family.getBeaconLocation() != null) {
                Location beaconLocation = family.getBeaconLocation();
                if (isPartOfBeaconBase(block, beaconLocation)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isPartOfBeaconBase(Block block, Location beaconLocation) {
        if (beaconLocation == null) {
            return false;
        }

        // 신호기 아래의 기반 블록 확인 (3x3 영역)
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Location baseBlockLocation = beaconLocation.clone().add(x, -1, z);
                if (baseBlockLocation.getBlock().equals(block)) {
                    return true;
                }
            }
        }
        return false;
    }


    private boolean isPartOfBeaconBase(Block block) {
        for (FamilyManager.Family family : FamilyManager.getAllFamilies()) {
            Location beaconLocation = family.getBeaconLocation();
            if (beaconLocation != null) {
                for (int y = -1; y >= -BEACON_EFFECT_RADIUS; y--) {
                    Location baseBlockLocation = beaconLocation.clone().add(0, y, 0);
                    if (baseBlockLocation.getBlock().equals(block)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void addUser(String username) {
        allUsers.add(username);
    }

    private void disableAchievements() {
        for (World world : getServer().getWorlds()) {
            world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        }
    }

    private void disableCoordinates() {
        for (Player player : getServer().getOnlinePlayers()) {
            player.setPlayerListName(player.getName());
            player.setPlayerListHeaderFooter("", "");
        }
    }
}