package world.world;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class BeaconBlockEventHandler implements Listener {
    private final JavaPlugin plugin;
    private final GUIHandler guiHandler;
    private final FamilyManager familyManager;
    private final PlayerReviveManager playerReviveManager;
    private final Map<UUID, BukkitRunnable> activeWarningTasks = new HashMap<>();
    private static final long SPECTATOR_PERIOD = 7200000L; // 2시간 (밀리초)
    private static final long WARNING_INTERVAL = 1200L; // 1분 경고 인터벌 = 1200틱

    public BeaconBlockEventHandler(JavaPlugin plugin, GUIHandler guiHandler, FamilyManager familyManager, PlayerReviveManager playerReviveManager) {
        this.plugin = plugin;
        this.guiHandler = guiHandler;
        this.familyManager = familyManager;
        this.playerReviveManager = playerReviveManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin); // 이벤트 리스너 등록
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();

        if (block.getType() == Material.BEACON) {
            handleBeaconPlacement(event, player, block);
        }
    }

    private void handleBeaconPlacement(BlockPlaceEvent event, Player player, Block block) {
        String playerName = player.getName();
        Location newBeaconLocation = block.getLocation();

        if (familyManager.hasFamily(playerName)) {
            Location existingBeaconLocation = familyManager.getBeaconLocation(playerName);
            if (existingBeaconLocation != null) {
                if (existingBeaconLocation.equals(newBeaconLocation)) {
                    player.sendMessage(ChatColor.GREEN + "신호기가 성공적으로 설치되었습니다.");
                } else {
                    player.sendMessage(ChatColor.RED + "이미 설치된 신호기가 있습니다!");
                    event.setCancelled(true);
                    return;
                }
            }
        }

        if (!isValidBeaconPlacement(block)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "신호기의 설치 위치는 Y -32 ~ 63입니다.");
            return;
        }

        placeBeacon(newBeaconLocation);

        if (!familyManager.hasFamily(playerName)) {
            ChatColor teamColor = getRandomTeamColor();
            familyManager.createFamily(playerName, block.getLocation(), teamColor);
            player.sendMessage(ChatColor.GREEN + "가문이 성공적으로 생성되었습니다.");
            setFamilyColor(playerName, teamColor);
        } else {
            familyManager.setBeaconLocation(playerName, block.getLocation());
            setFamilyColor(playerName, familyManager.getFamilyColor(playerName));
        }

        cancelAndRemoveExistingTask(player.getUniqueId());
        startInstallationWarningTask(player);
    }


    private void placeBeacon(Location beaconLocation) {
        // 3x3 철블록 생성
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Location baseBlockLocation = beaconLocation.clone().add(x, -1, z);
                baseBlockLocation.getBlock().setType(Material.IRON_BLOCK);
            }
        }
        // 신호기 설치
        beaconLocation.getBlock().setType(Material.BEACON);

        // 베리어 설치 (건축 높이 제한까지)
        int buildHeightLimit = beaconLocation.getWorld().getMaxHeight(); // 해당 월드의 최대 높이 제한
        for (int y = beaconLocation.getBlockY() + 1; y < buildHeightLimit; y++) {
            Location barrierLocation = beaconLocation.clone().add(0, y - beaconLocation.getBlockY(), 0);
            barrierLocation.getBlock().setType(Material.BARRIER);
        }
    }

    private boolean isValidBeaconPlacement(Block beaconBlock) {
        // 신호기가 바다 레벨 아래에 있으면 설치 불가
        if (64 < beaconBlock.getY() || beaconBlock.getY() < -32) {
            return false;
        }

        // 신호기 위가 공기 또는 베리어로만 채워져 있어야 함
        return isAirOrBarrierAbove(beaconBlock);
    }

    private boolean isAirOrBarrierAbove(Block block) {
        World world = block.getWorld();
        int blockY = block.getY();
        int maxHeight = world.getMaxHeight();

        // 블록 위쪽 탐색 (최대 높이까지)
        for (int y = blockY + 1; y < maxHeight; y++) {
            Material blockMaterial = world.getBlockAt(block.getX(), y, block.getZ()).getType();

            // 블록이 공기나 베리어라면 통과
            if (blockMaterial == Material.AIR || blockMaterial == Material.BARRIER) {
                continue;
            }

            // 그 외의 블록 존재 시 설치 불가
            return false;
        }

        // 위쪽에 공기나 베리어만 있으면 설치 가능
        return true;
    }


    private ChatColor getRandomTeamColor() {
        ChatColor[] availableColors = {
                ChatColor.BLACK, ChatColor.DARK_BLUE, ChatColor.DARK_GREEN, ChatColor.DARK_AQUA,
                ChatColor.DARK_RED, ChatColor.DARK_PURPLE, ChatColor.GOLD, ChatColor.GRAY,
                ChatColor.DARK_GRAY, ChatColor.BLUE, ChatColor.GREEN, ChatColor.AQUA,
                ChatColor.RED, ChatColor.LIGHT_PURPLE, ChatColor.YELLOW, ChatColor.WHITE
        };
        Random random = new Random();
        return availableColors[random.nextInt(availableColors.length)];
    }

    private void setFamilyColor(String leaderName, ChatColor teamColor) {
        if (!familyManager.hasFamily(leaderName)) {
            return;
        }
        FamilyManager.Family family = familyManager.getFamilyByLeader(leaderName);
        if (family == null) {
            return;
        }
        for (String memberName : family.getMembers()) {
            Player member = Bukkit.getPlayer(memberName);
            if (member != null) {
                setTeamColor(member, teamColor);
            }
        }
    }

    private void setTeamColor(Player player, ChatColor teamColor) {
        if (player == null) {
            return;
        }
        player.setDisplayName(teamColor + player.getName());
        player.setPlayerListName(teamColor + player.getName());
    }

    // 신호기 및 기반 삭제 처리
    private void removeBeaconsBarrierAndBase(Location beaconLocation) {
        World world = beaconLocation.getWorld();
        if (world == null) {
            return; // 월드가 null인 경우 처리하지 않음
        }

        // 신호기 아래 3x3 철 블럭 제거
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Location baseBlockLocation = beaconLocation.clone().add(x, -1, z);
                Block baseBlock = baseBlockLocation.getBlock();
                if (baseBlock.getType() == Material.IRON_BLOCK) {
                    baseBlock.setType(Material.AIR); // 철 블럭 제거
                }
            }
        }

        // 신호기 위의 베리어 제거 (건축 높이 제한까지)
        int buildHeightLimit = world.getMaxHeight(); // 해당 월드의 건축 높이 제한
        for (int y = beaconLocation.getBlockY() + 1; y < buildHeightLimit; y++) {
            Location barrierLocation = beaconLocation.clone().add(0, y - beaconLocation.getBlockY(), 0);
            Block barrierBlock = barrierLocation.getBlock();
            if (barrierBlock.getType() == Material.BARRIER) {
                barrierBlock.setType(Material.AIR); // 베리어 제거
            } else if (barrierBlock.getType() != Material.AIR) {
                // 베리어나 공기가 아닌 다른 블럭이 발견될 경우 순회 중단
                break;
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location beaconLocation = block.getLocation();

        boolean isBase = isPartOfBeaconBase(block);

        if (block.getType() != Material.BEACON && !isBase) {
            return;
        }

        if (block.getType() == Material.BEACON) {
            handleBeaconBreak(event, player);
        }

        if (isBase && block.getType() == Material.IRON_BLOCK) {
            event.setCancelled(true);
            player.sendMessage("신호기 기반 블록은 파괴할 수 없습니다!");
        }

        if (block.getType() == Material.BEACON || isBase) {
            removeBeaconsBarrierAndBase(beaconLocation);
        }
    }


    private void handleBeaconBreak(BlockBreakEvent event, Player player) {
        Block block = event.getBlock();
        Location beaconLocation = block.getLocation();

        // 플레이어가 자기 신호기를 파괴할 경우
        Location playerBeaconLocation = familyManager.getBeaconLocation(player.getName());
        if (playerBeaconLocation != null && playerBeaconLocation.equals(beaconLocation)) {
            handlePlayerBeaconBreak(event, player, beaconLocation);
            return;
        }

        // 상대방 신호기를 파괴할 경우
        for (FamilyManager.Family family : familyManager.getAllFamilies()) {
            if (family.getBeaconLocation() != null && family.getBeaconLocation().equals(beaconLocation)) {
                handleEnemyBeaconBreak(event, player, family, beaconLocation);
                break;
            }
        }
    }

    private void handlePlayerBeaconBreak(BlockBreakEvent event, Player player, Location beaconLocation) {
        // 신호기 회수 쿨타임 확인
        if (!familyManager.canRecoverBeacon(player.getName())) {
            String remainingTime = familyManager.getRecoveryCooldownRemaining(player.getName());
            player.sendMessage(ChatColor.RED + "신호기를 회수할 수 없습니다. 남은 쿨타임: " + remainingTime);

            // 신호기와 기반 복구
            new BukkitRunnable() {
                @Override
                public void run() {
                    restoreBeaconAndBase(beaconLocation);
                }
            }.runTaskLater(plugin, 2L); // 2틱 후 복구 실행
            event.setCancelled(true);
            return;
        }

        // 쿨타임 충족 시 정상적으로 신호기 회수 처리
        familyManager.removeBeacon(player.getName());
        player.sendMessage(ChatColor.GREEN + "신호기가 회수되었습니다.");
        startSpectatorModeTask(player);
        startInstallationWarningTask(player);
    }

    private void handleEnemyBeaconBreak(BlockBreakEvent event, Player player, FamilyManager.Family enemyFamily, Location beaconLocation) {
        String enemyLeader = enemyFamily.getLeader();

        // 플레이어가 적 신호기를 파괴하는 경우:
        if (!enemyFamily.getBeaconLocation().equals(beaconLocation)) {
            event.setCancelled(true); // 신호기 위치가 맞지 않으면 취소
            return;
        }

        // 자기 신호기가 아닌 경우, 리더인지 검증 (적 신호기일 경우만 적용)
        FamilyManager.Family playerFamily = familyManager.getFamily(player.getName());
        if (!playerFamily.getLeader().equals(player.getName())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "가문의 리더만 적 신호기를 파괴할 수 있습니다!");
            restoreBeaconAndBase(beaconLocation);
            return;
        }

        // 상대 가문 흡수 로직 실행 (리더 확인 이후)
        ChatColor absorbedColor = familyManager.absorbFamily(player.getName(), enemyLeader);
        if (absorbedColor != null) {
            setFamilyColor(enemyLeader, absorbedColor);
            Bukkit.broadcastMessage(ChatColor.RED + enemyLeader + " 가문이 멸망하였습니다!");
        }
        player.sendMessage(ChatColor.GREEN + enemyLeader + " 가문을 흡수했습니다.");

        // 추가 작업 실행
        startSpectatorModeTask(player);
        startInstallationWarningTask(player);
    }

    // 신호기 및 기반 복원
    private void restoreBeaconAndBase(Location beaconLocation) {
        World world = beaconLocation.getWorld();
        if (world == null) {
            return; // 월드가 null이면 복원 중단
        }

        // **신호기 복원**
        Block beaconBlock = beaconLocation.getBlock();
        if (beaconBlock.getType() != Material.BEACON) {
            beaconBlock.setType(Material.BEACON); // 신호기 설치
        }

        // **신호기 아래 3x3 철 블럭 복원**
        for (int x = -1; x <= 1; x++) { // x축으로 -1에서 1까지 순회
            for (int z = -1; z <= 1; z++) { // z축으로 -1에서 1까지 순회
                Location baseBlockLocation = beaconLocation.clone().add(x, -1, z);
                Block baseBlock = baseBlockLocation.getBlock();
                if (baseBlock.getType() != Material.IRON_BLOCK) {
                    baseBlock.setType(Material.IRON_BLOCK); // 철 블럭 설치
                }
            }
        }

        // **건축 높이 제한까지 신호기 위의 베리어 복원**
        int maxHeight = world.getMaxHeight(); // 해당 월드의 최대 건축 높이 제한
        for (int y = beaconLocation.getBlockY() + 1; y < maxHeight; y++) {
            Location barrierLocation = beaconLocation.clone().add(0, y - beaconLocation.getBlockY(), 0);
            Block barrierBlock = barrierLocation.getBlock();
            if (barrierBlock.getType() != Material.BARRIER) {
                barrierBlock.setType(Material.BARRIER); // 베리어 설치
            }
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        List<Block> blocks = event.blockList();
        blocks.removeIf(block -> block.getType() == Material.BEACON);
    }

    private boolean isPartOfBeaconBase(Block block) {
        for (FamilyManager.Family family : familyManager.getAllFamilies()) {
            Location beaconLocation = family.getBeaconLocation();
            if (beaconLocation != null) {
                // 신호기 자신의 위치 포함 (위치 일치 여부를 체크)
                if (beaconLocation.getBlock().equals(block)) {
                    return true;
                }

                // 신호기 아래와 주변 8개의 철블럭 포함
                for (int y = -1; y >= -4; y--) { // 신호기 아래 최대 4개의 레이어 순회
                    for (int x = -1; x <= 1; x++) { // x축 범위 확인
                        for (int z = -1; z <= 1; z++) { // z축 범위 확인
                            Location baseBlockLocation = beaconLocation.clone().add(x, y, z);
                            if (baseBlockLocation.getBlock().equals(block)) { // 블록 위치가 신호기 기반에 포함 여부 확인
                                return true;
                            }
                        }
                    }
                }

                // 신호기 바로 위 블럭 검사 (베리어 확인)
                for (int y = 1; y <= 3; y++) { // 신호기 위쪽으로 최대 3블럭 확인
                    Location aboveBlockLocation = beaconLocation.clone().add(0, y, 0);
                    if (aboveBlockLocation.getBlock().equals(block)) {
                        return true;
                    }
                }
            }
        }
        return false; // 어떤 블록에도 포함되지 않을 경우
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        String playerName = player.getName();

        long banDuration = 180000L; // 3분 밴(밀리초)
        playerReviveManager.banPlayer(player.getUniqueId(), banDuration);

        new BukkitRunnable() {
            @Override
            public void run() {
                player.kickPlayer(ChatColor.RED + "사망하여 3분 동안 밴되었습니다.");
            }
        }.runTaskLater(plugin, 20L); // 1초 후 실행
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        playerReviveManager.checkBanOnLogin(player); // 플레이어 접속 시 밴 체크

        if (familyManager.hasFamily(player.getName()) && familyManager.getBeaconLocation(player.getName()) == null) {
            startInstallationWarningTask(player);
        }

        UUID playerId = player.getUniqueId();

        if (remainingTicksMap.containsKey(playerId)) {
            startInstallationWarningTask(player); // 남은 시간을 이용하여 타이머 재등록
        }
    }

    private final Map<UUID, Long> remainingTicksMap = new HashMap<>(); // 남은 시간 저장

    private void startInstallationWarningTask(final Player player) {
        cancelAndRemoveExistingTask(player.getUniqueId());

        // 저장된 남은 시간이 있다면 해당 시간으로 시작
        final UUID playerId = player.getUniqueId();
        long remainingTicks = remainingTicksMap.getOrDefault(playerId, SPECTATOR_PERIOD / 50); // 2시간 = 72000틱

        BukkitRunnable warningTask = new BukkitRunnable() {
            private long ticksRemaining = remainingTicks;

            @Override
            public void run() {
                if (familyManager.hasFamily(player.getName()) && familyManager.getBeaconLocation(player.getName()) == null) {
                    if (ticksRemaining <= 0) {
                        player.setGameMode(GameMode.SPECTATOR);
                        player.sendMessage(ChatColor.RED + "2시간 내에 신호기를 재설치하지 않아서 탈락되었습니다.");
                        remainingTicksMap.remove(playerId); // 남은 시간 제거
                        this.cancel();
                        return;
                    }

                    // 매 WARNING_INTERVAL마다 경고 메시지 전송
                    if (ticksRemaining % WARNING_INTERVAL == 0) {
                        sendTimeRemainingMessage(player, ticksRemaining);
                    }

                    ticksRemaining--;
                    remainingTicksMap.put(playerId, ticksRemaining); // 남은 시간 갱신
                } else {
                    remainingTicksMap.remove(playerId); // 작업 종료 시 시간 제거
                    this.cancel();
                }
            }
        };

        warningTask.runTaskTimer(plugin, 0L, 1L); // 매 틱마다 실행
        activeWarningTasks.put(playerId, warningTask);
    }

    private void sendTimeRemainingMessage(Player player, long remainingTicks) {
        long secondsRemaining = (remainingTicks / 20) % 60;
        long minutesRemaining = (remainingTicks / 1200) % 60;
        long hoursRemaining = (remainingTicks / 72000);
        player.sendMessage(ChatColor.YELLOW + String.format(
                "남은 시간: %02d시간 %02d분 %02d초.\n신호기를 재설치하지 않으면 탈락됩니다.",
                hoursRemaining, minutesRemaining, secondsRemaining
        ));
    }

    private void startSpectatorModeTask(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.setGameMode(GameMode.SPECTATOR);
                    player.sendMessage(ChatColor.RED + "2시간 내에 신호기를 설치하지 않아 탈락되었습니다.");
                    remainingTicksMap.remove(player.getUniqueId()); // 작업 종료 시 시간 제거
                }
            }
        }.runTaskLater(plugin, SPECTATOR_PERIOD / 50); // 2시간 후 실행
    }


    // 나갔을 때 타이머를 멈추고 남은 시간을 저장하지 않도록 처리
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (activeWarningTasks.containsKey(playerId)) {
            // 현재 타이머 취소
            BukkitRunnable task = activeWarningTasks.get(playerId);
            if (task != null) {
                task.cancel();
            }
            activeWarningTasks.remove(playerId);
        }
    }

    private void cancelAndRemoveExistingTask(UUID playerUUID) {
        if (activeWarningTasks.containsKey(playerUUID)) {
            BukkitRunnable existingTask = activeWarningTasks.get(playerUUID);
            if (existingTask != null) {
                existingTask.cancel();
            }
            activeWarningTasks.remove(playerUUID);
        }
    }

}