package world.world;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.entity.Entity;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;


public class FamilyGUIHandler implements GUIHandler, Listener {
    private static final String REVIVE_GUI_TITLE = "부활 관리";
    private static final int FIREWORK_COOLDOWN = 20 * 60 * 60 * 24; // 24시간 쿨타임 (틱 단위)
    private final JavaPlugin plugin;
    private final PlayerReviveManager reviveManager;
    private final FamilyManager familyManager;
    private final Logger logger;
    private final Map<UUID, Long> cooldownMap = new HashMap<>();
    private final Set<UUID> activeRoulettePlayers = new HashSet<>();

    public FamilyGUIHandler(JavaPlugin plugin, PlayerReviveManager reviveManager, FamilyManager familyManager, Logger logger) {
        this.plugin = plugin;
        this.reviveManager = reviveManager;
        this.familyManager = familyManager;
        this.logger = logger;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        loadCooldowns();
    }



    @Override
    public void openGUI(Player player) {
        FamilyManager.Family playerFamily = FamilyManager.getFamily(player.getName());
        if (playerFamily == null) {
            player.sendMessage(ChatColor.RED + "가문에 속해 있지 않습니다.");
            return;
        }

        Inventory reviveGui = Bukkit.createInventory(null, 9, REVIVE_GUI_TITLE);
        List<UUID> bannedPlayers = reviveManager.getAllBannedPlayers();

        for (UUID uuid : bannedPlayers) {
            OfflinePlayer bannedPlayer = Bukkit.getOfflinePlayer(uuid);
            FamilyManager.Family bannedPlayerFamily = FamilyManager.getFamily(bannedPlayer.getName());

            if (playerFamily.equals(bannedPlayerFamily)) {
                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) head.getItemMeta();
                if (meta != null) {
                    meta.setOwningPlayer(bannedPlayer);
                    meta.setDisplayName(ChatColor.RED + bannedPlayer.getName());

                    long remainingTime = reviveManager.getBanTimeRemaining(uuid);
                    int requiredEmeralds = (int) (remainingTime / 1000 / 60);
                    List<String> lore = new ArrayList<>();
                    lore.add(ChatColor.GOLD + "남은 시간: " + ChatColor.WHITE + formatTime(remainingTime));
                    lore.add(ChatColor.GREEN + "필요한 에메랄드: " + ChatColor.WHITE + requiredEmeralds);

                    meta.setLore(lore);
                    head.setItemMeta(meta);
                    reviveGui.addItem(head);
                }
            }
        }

        ItemStack scoutFirework = new ItemStack(Material.FIREWORK_ROCKET);
        ItemMeta fireworkMeta = scoutFirework.getItemMeta();
        if (fireworkMeta != null) {
            fireworkMeta.setDisplayName(ChatColor.RED + "정찰용 폭죽");
            List<String> lore = new ArrayList<>();

            long cooldownRemaining = getCooldownRemaining(player.getUniqueId());
            if (cooldownRemaining > 0) {
                lore.add(ChatColor.RED + "쿨타임 남은 시간: " + ChatColor.WHITE + formatTime(cooldownRemaining));
            }
            fireworkMeta.setLore(lore);
            scoutFirework.setItemMeta(fireworkMeta);
        }

        reviveGui.setItem(8, scoutFirework);  // 9슬롯 중 마지막 슬롯에 폭죽을 추가

        player.openInventory(reviveGui);
        ItemStack pickTeamMember = new ItemStack(Material.DIAMOND);
        ItemMeta pickMeta = pickTeamMember.getItemMeta();
        if (pickMeta != null) {
            pickMeta.setDisplayName(ChatColor.AQUA + "팀원 뽑기");
            List<String> lore = new ArrayList<>();
            pickMeta.setLore(lore);
            pickTeamMember.setItemMeta(pickMeta);
        }
        reviveGui.setItem(7, pickTeamMember);

        player.openInventory(reviveGui);

        ItemStack giftBox = new ItemStack(Material.CHEST);
        ItemMeta giftBoxMeta = giftBox.getItemMeta();
        if (giftBoxMeta != null) {
            giftBoxMeta.setDisplayName(ChatColor.GOLD + "선물 상자");
            giftBox.setItemMeta(giftBoxMeta);
        }
        reviveGui.setItem(6, giftBox); // 다이아몬드 왼쪽 칸에 배치

        player.openInventory(reviveGui);
    }


    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.hasBlock()) {
            Player player = event.getPlayer();
            if (player == null) {
                return;
            }

            if (event.getClickedBlock().getType() == Material.BEACON) {
                Location beaconLocation = event.getClickedBlock().getLocation();
                FamilyManager.Family family = FamilyManager.getFamily(player.getName());

                if (family != null && family.getBeaconLocation() != null && family.getBeaconLocation().equals(beaconLocation)) {
                    openGUI(player);
                } else {
                    player.sendMessage(ChatColor.RED + "다른 가문의 신호기에서는 GUI를 열 수 없습니다.");
                }
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(REVIVE_GUI_TITLE)) {
            return;
        }

        if (event.isCancelled()) {
            return;
        }

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        FamilyManager.Family family = FamilyManager.getFamily(player.getName());

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null) {
            return;
        }

        if (clickedItem.getType() == Material.CHEST) {

            if (family == null || !family.getLeader().equals(player.getName())) {
                player.sendMessage(ChatColor.RED + "선물상자는 가문 리더만 사용할 수 있습니다.");
                event.setCancelled(true);
                return;
            }
            else {
                openGiftBoxGUI(player);
                event.setCancelled(true);
            }// 기존 클릭 이벤트 취소
        }

        // 부활 로직 처리
        if (clickedItem.getType() == Material.PLAYER_HEAD) {
            SkullMeta meta = (SkullMeta) clickedItem.getItemMeta();
            if (meta == null || meta.getOwningPlayer() == null || meta.getOwningPlayer().getName() == null) {
                event.setCancelled(true); // 이벤트 취소
                return;
            }

            String bannedPlayerName = ChatColor.stripColor(meta.getDisplayName());
            OfflinePlayer bannedPlayer = Bukkit.getOfflinePlayer(bannedPlayerName);
            UUID uuid = bannedPlayer.getUniqueId();
            FamilyManager.Family bannedPlayerFamily = FamilyManager.getFamily(bannedPlayer.getName());

            // 가문 확인
            if (!family.equals(bannedPlayerFamily)) {
                player.sendMessage(ChatColor.RED + "같은 가족의 멤버만 부활시킬 수 있습니다.");
                event.setCancelled(true); // 이벤트 취소
                return;
            }

            long remainingTime = reviveManager.getBanTimeRemaining(uuid);
            int requiredEmeralds = (int) (remainingTime / 1000 / 60);
            ItemStack emeralds = new ItemStack(Material.EMERALD, requiredEmeralds);

            // 인벤토리에 필요한 에메랄드가 충분한지 확인
            if (!player.getInventory().containsAtLeast(emeralds, requiredEmeralds)) {
                player.sendMessage(ChatColor.RED + "에메랄드가 부족합니다.");
                event.setCancelled(true); // 이벤트 취소
                return;
            }

            // 부활 처리
            player.getInventory().removeItem(emeralds);
            player.sendMessage(ChatColor.GREEN + bannedPlayerName + "님을 부활시켰습니다.");
            Player onlinePlayer = bannedPlayer.getPlayer();
            if (onlinePlayer != null) {
                onlinePlayer.sendMessage(ChatColor.GREEN + "부활되었습니다. 다시 서버에 접속할 수 있습니다.");
            }
            reviveManager.unbanPlayer(uuid);
            openGUI(player);

            event.setCancelled(true); // 이벤트 취소
            player.closeInventory();  // 인벤토리 닫기
        }

        // 정찰용 폭죽 로직 처리 - 리더만 사용 가능
        else if (clickedItem.getType() == Material.FIREWORK_ROCKET) {
            event.setCancelled(true); // 이벤트 취소를 맨 위에서 처리 (중복 방지)

            // 가문 정보 확인 및 리더 권한 확인
            if (family == null || !family.getLeader().equals(player.getName())) {
                player.sendMessage(ChatColor.RED + "정찰용 폭죽은 가문 리더만 사용할 수 있습니다.");
                return; // 리턴으로 종료하여 중복 실행 방지
            }

            // 슬롯 번호 확인
            if (event.getSlot() != 8) {
                player.sendMessage(ChatColor.RED + "정찰용 폭죽은 8번 슬롯에서만 사용할 수 있습니다.");
                return; // 리턴으로 종료하여 중복 실행 방지
            }

            // 쿨타임 확인
            long cooldownRemaining = getCooldownRemaining(player.getUniqueId());
            if (cooldownRemaining > 0) {
                player.sendMessage(ChatColor.RED +
                        "정찰용 폭죽은 쿨타임 중입니다. 남은 시간: " + formatTime(cooldownRemaining));
                return; // 리턴으로 종료하여 중복 실행 방지
            }

            // 갑옷 슬롯 확인 및 처리
            if (player.getInventory().getChestplate() != null) {
                player.sendMessage(ChatColor.RED + "갑옷 자리가 비어 있어야 합니다!");
                return; // 리턴으로 종료하여 중복 실행 방지
            }

            // 엘리트라 장착 및 정찰 시작
            ItemStack elytra = new ItemStack(Material.ELYTRA);
            ItemMeta elytraMeta = elytra.getItemMeta();
            if (elytraMeta != null) {
                elytraMeta.setUnbreakable(true); // 내구도 무한
                elytraMeta.addEnchant(Enchantment.BINDING_CURSE, 1, true); // 귀속 저주
                elytra.setItemMeta(elytraMeta);
            }
            player.getInventory().setChestplate(elytra);

            // 인벤토리에서 정찰용 폭죽 제거
            player.getInventory().remove(clickedItem);

            // 쿨타임 설정
            setCooldown(player.getUniqueId(), FIREWORK_COOLDOWN * 50); // 50ms 단위 저장 (24시간)
            player.sendMessage(ChatColor.AQUA + "정찰용 폭죽을 사용하였습니다.");

            // ScoutTask 시작
            new ScoutTask(player).runTaskTimer(plugin, 0, 1); // 1틱마다 실행
        }
        else if (clickedItem.getType() == Material.DIAMOND) {
            if (family == null || !family.getLeader().equals(player.getName())) {
                player.sendMessage(ChatColor.RED + "팀원 뽑기는 리더만 사용할 수 있습니다.");
                event.setCancelled(true);
                return;
            }

            openRouletteGUI(player); // 새로운 GUI 열기
            event.setCancelled(true);
        }
    }

    private final Map<UUID, Inventory> giftBoxInventories = new HashMap<>();// 각 플레이어별 GUI 저장

    public void openGiftBoxGUI(Player player) {
        Inventory giftBoxGui = giftBoxInventories.getOrDefault(player.getUniqueId(), null); // 기존 GUI 확인

        if (giftBoxGui == null) {
            // 새로운 GUI 생성
            giftBoxGui = Bukkit.createInventory(null, 27, ChatColor.GOLD + "선물 상자");

            // 초기 아이템 추가
            fillGiftBoxWithRewards(giftBoxGui);

            giftBoxInventories.put(player.getUniqueId(), giftBoxGui); // 저장
        }

        player.openInventory(giftBoxGui);
    }

    private void fillGiftBoxWithRewards(Inventory inventory) {
        Random random = new Random();

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            // 빈 슬롯 확인
            if (inventory.getItem(slot) == null || inventory.getItem(slot).getType() == Material.AIR) {
                // 50% 확률로 마석 또는 강화석 추가
                if (random.nextBoolean()) {
                    ItemStack upgradeStone = createGuiItem(Material.AMETHYST_SHARD, "마석");
                    upgradeStone.setAmount(random.nextInt(24) + 1); // 1 ~ 24개 랜덤 추가
                    inventory.setItem(slot, upgradeStone);
                } else {
                    ItemStack reinforcementStone = getUpgradeStone(random.nextInt(24) + 1); // 1 ~ 24개 랜덤 추가
                    inventory.setItem(slot, reinforcementStone);
                }
            }
        }
    }

    private ItemStack createGuiItem(Material material, String name) {
        ItemStack item = new ItemStack(Material.ECHO_SHARD, 4);

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("마석")
                    .color(NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false)); // 이탤릭체 비활성화
            item.setItemMeta(meta);
        }
        return item;
    }

        public static ItemStack getUpgradeStone(int quantity) {
        ItemStack upgradeStone = new ItemStack(Material.AMETHYST_SHARD, quantity);
        ItemMeta meta = upgradeStone.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "강화석");
            upgradeStone.setItemMeta(meta);
        }
        return upgradeStone;
    }


    public void startGiftBoxUpdater() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // 지급 시작 메시지
            Bukkit.broadcastMessage(ChatColor.GOLD + "[알림] " + ChatColor.GREEN + "무료나눔 도착");

            for (Map.Entry<UUID, Inventory> entry : giftBoxInventories.entrySet()) {
                fillGiftBoxWithRewards(entry.getValue()); // 모든 인벤토리를 업데이트
            }
        }, 0L, 288000L); // 0틱 후 처음 실행, 288000틱 (4시간) 간격으로 반복
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals(ChatColor.GOLD + "선물 상자")) {
            Player player = (Player) event.getPlayer();
            giftBoxInventories.put(player.getUniqueId(), event.getInventory());
        }

    }

    private void openRouletteGUI(Player player) {
        FamilyManager.Family family = FamilyManager.getFamily(player.getName());
        if (family == null) {
            player.sendMessage(ChatColor.RED + "가문 정보가 없습니다.");
            return;
        }

        int teamSize = family.getMembers().size(); // 현재 가문의 구성원 수
        if (teamSize >= 4) { // 나 포함 최대 팀원 수 4명 제한
            player.sendMessage(ChatColor.RED + "가문 최대 인원입니다. 더 이상 팀원을 뽑을 수 없습니다.");
            return;
        }

        Inventory rouletteGui = Bukkit.createInventory(null, 9, ChatColor.AQUA + "팀원 뽑기");

        // 다이아몬드 아이템 생성 (중앙 슬롯)
        ItemStack diamond = new ItemStack(Material.DIAMOND);
        ItemMeta diamondMeta = diamond.getItemMeta();
        if (diamondMeta != null) {
            diamondMeta.setDisplayName(ChatColor.AQUA + "팀원 뽑기");
            List<String> lore = new ArrayList<>();

            // 팀원 수에 따른 다이아몬드 소모량 하드코딩
            int requiredDiamonds = 0; // 초기값
            if (teamSize == 1) {
                requiredDiamonds = 64; // 1명일 때 64개
            } else if (teamSize == 2) {
                requiredDiamonds = 128; // 2명일 때 128개
            } else if (teamSize == 3) {
                requiredDiamonds = 256; // 3명일 때 256개
            }

            lore.add(ChatColor.GREEN + "클릭 시 다이아몬드 " + requiredDiamonds + "개 소모.");
            diamondMeta.setLore(lore);
            diamond.setItemMeta(diamondMeta);
        }
        rouletteGui.setItem(4, diamond); // 중앙 슬롯 (index 4)

        // GUI 열기
        player.openInventory(rouletteGui);
    }

    @EventHandler
    public void onInventoryClickRoulette(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(ChatColor.AQUA + "팀원 뽑기")) {
            return; // 다른 GUI에는 영향 없음
        }

        event.setCancelled(true); // GUI에서 아이템 이동 방지

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        // 클릭한 아이템이 다이아몬드인지 확인
        if (clickedItem == null || clickedItem.getType() != Material.DIAMOND) {
            return;
        }

        // 클릭한 슬롯이 4번 칸인지 확인 (0부터 시작하므로 4는 GUI 내에서 중앙 슬롯)
        if (event.getSlot() != 4) {
            return;
        }

        FamilyManager.Family family = FamilyManager.getFamily(player.getName());
        if (family == null) {
            player.sendMessage(ChatColor.RED + "가문 정보가 없습니다.");
            return;
        }

        int teamSize = family.getMembers().size(); // 현재 구성원 수
        if (teamSize >= 4) { // 나 포함 최대 팀원 수 4명 제한
            player.sendMessage(ChatColor.RED + "가문 최대 인원입니다. 더 이상 팀원을 뽑을 수 없습니다.");
            return;
        }

        // 다이아몬드 소모량을 하드코딩
        int requiredDiamonds = 0; // 초기값
        if (teamSize == 1) {
            requiredDiamonds = 64; // 1명일 때 64개
        } else if (teamSize == 2) {
            requiredDiamonds = 128; // 2명일 때 128개
        } else if (teamSize == 3) {
            requiredDiamonds = 256; // 3명일 때 256개
        }

        // 다이아몬드가 충분한지 확인
        if (!player.getInventory().contains(Material.DIAMOND, requiredDiamonds)) {
            player.sendMessage(ChatColor.RED + "다이아몬드가 부족합니다. 필요한 개수: " + requiredDiamonds);
            return;
        }

        // 다이아몬드 제거
        player.getInventory().removeItem(new ItemStack(Material.DIAMOND, requiredDiamonds));
        player.sendMessage(ChatColor.GREEN + "다이아몬드 " + requiredDiamonds + "개를 소비하여 팀원을 뽑습니다!");

        // 룰렛 애니메이션 시작 (GUI 닫기 방지)
        startRouletteAnimation(player, family, event.getView().getTopInventory());
    }

    private void startRouletteAnimation(Player player, FamilyManager.Family family, Inventory inventory) {
        OfflinePlayer[] allPlayers = Bukkit.getOfflinePlayers();
        List<OfflinePlayer> eligiblePlayers = new ArrayList<>();

        // 룰렛 참여 가능 플레이어 필터링: "가문에 속하지 않은 플레이어만 포함"
        for (OfflinePlayer offlinePlayer : allPlayers) {
            FamilyManager.Family playerFamily = FamilyManager.getFamily(offlinePlayer.getName());

            if (playerFamily == null) {  // 가문이 없는 플레이어만 룰렛에 포함
                eligiblePlayers.add(offlinePlayer);
            }
        }

        // 룰렛에 포함할 플레이어가 없는 경우
        if (eligiblePlayers.isEmpty()) {
            player.sendMessage(ChatColor.RED + "뽑을 수 있는 플레이어가 없습니다.");
            return;
        }

        // 뽑기 진행 중 상태 추가
        activeRoulettePlayers.add(player.getUniqueId());

        Random random = new Random();
        int[] counter = {0};
        OfflinePlayer[] selectedPlayer = {null};

        // 애니메이션 태스크
        new BukkitRunnable() {
            @Override
            public void run() {
                if (counter[0] < 50) {
                    OfflinePlayer randomPlayer = eligiblePlayers.get(random.nextInt(eligiblePlayers.size()));

                    // 플레이어 머리 아이템 생성
                    ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta meta = (SkullMeta) head.getItemMeta();
                    if (meta != null) {
                        meta.setOwningPlayer(randomPlayer);
                        meta.setDisplayName(ChatColor.YELLOW + randomPlayer.getName());
                        head.setItemMeta(meta);
                    }

                    inventory.setItem(4, head); // 중앙 슬롯(index 4)에 머리 배치
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

                    counter[0]++;
                } else { // 애니메이션 종료
                    cancel();

                    // 최종 선택된 플레이어
                    selectedPlayer[0] = eligiblePlayers.get(random.nextInt(eligiblePlayers.size()));

                    // 최종 선택한 플레이어 머리 고정
                    ItemStack finalHead = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta meta = (SkullMeta) finalHead.getItemMeta();
                    if (meta != null) {
                        meta.setOwningPlayer(selectedPlayer[0]);
                        meta.setDisplayName(ChatColor.GREEN + selectedPlayer[0].getName());
                        finalHead.setItemMeta(meta);
                    }
                    inventory.setItem(4, finalHead);

                    // 폭죽 효과 재생
                    spawnFireworks(player.getLocation(), 2);

                    // 가문에 추가 및 메시지 전달
                    addPlayerToFamily(player, family, selectedPlayer[0]);

                    // 뽑기 종료 상태 업데이트
                    activeRoulettePlayers.remove(player.getUniqueId());
                }
            }
        }.runTaskTimer(plugin, 0, 2); // 5틱(0.25초)마다 실행
    }

    @EventHandler
    public void onInventoryCloseRoulette(InventoryCloseEvent event) {
        // "팀원 뽑기" GUI와 관련된 경우
        if (event.getView().getTitle().equals(ChatColor.AQUA + "팀원 뽑기")) {
            Player player = (Player) event.getPlayer();

            // 뽑기가 진행 중인 경우에만 다시 창을 엽니다.
            if (activeRoulettePlayers.contains(player.getUniqueId())) {
                Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(event.getInventory()));
            }
        }
    }

    private void spawnFireworks(Location location, int count) {
        for (int i = 0; i < count; i++) {
            Firework firework = (Firework) location.getWorld().spawn(location, Firework.class);
            FireworkMeta fireworkMeta = firework.getFireworkMeta();

            // 폭죽 효과 설정
            fireworkMeta.addEffect(FireworkEffect.builder()
                    .withColor(Color.RED, Color.GREEN, Color.BLUE)
                    .withFade(Color.YELLOW, Color.ORANGE)
                    .with(FireworkEffect.Type.BALL_LARGE)
                    .trail(true)
                    .flicker(true)
                    .build());
            fireworkMeta.setPower(1);

            firework.setFireworkMeta(fireworkMeta);

            // 자동으로 터지도록 설정
            Bukkit.getScheduler().runTaskLater(plugin, firework::detonate, 20); // 1초 후 터짐
        }
    }

    private void addPlayerToFamily(Player executor, FamilyManager.Family family, OfflinePlayer selectedPlayer) {
        String selectedPlayerName = selectedPlayer.getName();
        if (selectedPlayerName == null) {
            executor.sendMessage(ChatColor.RED + "선택된 플레이어의 이름을 가져올 수 없습니다.");
            return;
        }

        // 가족에 추가
        family.addMember(selectedPlayerName);

        // 플레이어가 온라인 상태라면 서바이벌 모드로 설정
        if (selectedPlayer.isOnline()) {
            Player onlinePlayer = selectedPlayer.getPlayer();
            if (onlinePlayer != null) {
                onlinePlayer.setGameMode(GameMode.SURVIVAL);
                onlinePlayer.sendMessage(ChatColor.GREEN + "축하합니다! " + family.getTeamName() + " 가문에 합류되었습니다.");
            }
        }

        // 리더에게 메시지 전달
        executor.sendMessage(ChatColor.GREEN + selectedPlayerName + "님이 가문에 합류했습니다!");
    }

    private class ScoutTask extends BukkitRunnable {
        private final Player player;
        private boolean hasJumped = false;
        private boolean isWaitingToTeleport = false; // 귀환 대기 상태

        public ScoutTask(Player player) {
            this.player = player;
        }

        @Override
        public void run() {
            UUID playerId = player.getUniqueId();

            // 1. 플레이어가 접속 종료되었는지 확인
            if (!player.isOnline()) {
                if (isWaitingToTeleport) {
                    // 귀환 대기 상태 저장
                    ScoutManager.saveReturnWaitingState(playerId, true);
                } else if (hasJumped) {
                    // 정찰 중 상태 저장
                    ScoutManager.saveScoutState(playerId, true);
                }
                this.cancel();
                return;
            }

            // 2. 정찰 상승 시작
            if (!hasJumped && player.getVelocity().getY() > 0) {
                startScout();
            }

            // 3. 착지 시 귀환 대기 처리
            else if (player.isOnGround() && hasJumped && !isWaitingToTeleport) {
                finishScout();
            }
        }

        private void startScout() {
            hasJumped = true;
            player.setVelocity(player.getVelocity().setY(50)); // 초기 상승 속도 설정

            Firework firework = player.getWorld().spawn(player.getLocation(), Firework.class);
            FireworkMeta fireworkMeta = firework.getFireworkMeta();
            fireworkMeta.addEffect(FireworkEffect.builder().withColor(Color.AQUA).with(FireworkEffect.Type.BALL).build());
            firework.setFireworkMeta(fireworkMeta);

            // 주변 플레이어 탑 쌓기
            List<Player> nearbyPlayers = player.getWorld().getPlayers();
            List<Player> playersToStack = new ArrayList<>();
            for (Player p : nearbyPlayers) {
                if (p != player && p.getLocation().distance(player.getLocation()) <= 5) {
                    playersToStack.add(p);
                }
            }
            stackPlayers(player, playersToStack);
        }

        private void stackPlayers(Player bottomPlayer, List<Player> playersToStack) {
            Location currentStackLocation = bottomPlayer.getLocation().add(0, 2, 0);
            for (Player p : playersToStack) {
                p.teleport(currentStackLocation); // 각 플레이어를 위로 쌓음
                bottomPlayer.addPassenger(p);
                currentStackLocation = currentStackLocation.add(0, 2, 0);
                bottomPlayer = p;
            }
        }

        private void finishScout() {
            UUID playerId = player.getUniqueId();
            List<Entity> passengers = new ArrayList<>(player.getPassengers());
            for (Entity passenger : passengers) {
                player.removePassenger(passenger);
                if (passenger instanceof Player) {
                    ((Player) passenger).sendMessage(ChatColor.GREEN + "정찰이 종료되었습니다.");
                }
            }

            player.sendMessage(ChatColor.GREEN + "착지했습니다. 1분 후 신호기로 이동합니다.");
            player.getInventory().setChestplate(null);

            Location beaconLocation = FamilyManager.getBeaconLocation(player.getName());
            if (beaconLocation != null) {
                isWaitingToTeleport = true; // 귀환 대기 상태 ON
                ScoutManager.saveReturnWaitingState(playerId, true); // 대기 상태 저장

                // 1분 후 신호기로 이동
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!player.isOnline()) {
                            handleImmediateTeleport(); // 대기 중 접속 종료 시 즉시 귀환
                            return;
                        }

                        // 정상적으로 1분 후 귀환
                        player.teleport(beaconLocation);
                        player.sendMessage(ChatColor.GREEN + "신호기로 이동되었습니다.");

                        for (Entity passenger : passengers) {
                            if (passenger instanceof Player) {
                                ((Player) passenger).teleport(beaconLocation);
                                ((Player) passenger).sendMessage(ChatColor.GREEN + "신호기로 같이 이동되었습니다.");
                            }
                        }
                        isWaitingToTeleport = false; // 귀환 완료
                        ScoutManager.removeReturnWaitingState(playerId); // 대기 상태 제거
                    }
                }.runTaskLater(plugin, 20 * 60); // 1분 대기
            } else {
                player.sendMessage(ChatColor.RED + "신호기를 찾을 수 없습니다.");
            }
            this.cancel();
        }

        private void handleImmediateTeleport() {
            Location beaconLocation = FamilyManager.getBeaconLocation(player.getName());
            player.getInventory().setChestplate(null);

            if (beaconLocation != null) {
                player.teleport(beaconLocation);
                player.sendMessage(ChatColor.GREEN + "탈주로 인해 신호기로 즉시 이동되었습니다.");
            } else {
                player.sendMessage(ChatColor.RED + "가문의 신호기를 찾을 수 없습니다.");
            }
            ScoutManager.removeScoutState(player.getUniqueId()); // 상태 제거
            ScoutManager.removeReturnWaitingState(player.getUniqueId()); // 대기 상태 제거
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // 정찰 상태 확인 및 복구
        Boolean wasInScout = ScoutManager.getScoutState(playerId);
        if (wasInScout != null && wasInScout) {
            ScoutTask scoutTask = new ScoutTask(player);
            scoutTask.hasJumped = true; // 정찰 중 상태 복구
            scoutTask.runTaskTimer(plugin, 0, 20);
            ScoutManager.removeScoutState(playerId); // 복구 후 상태 제거
            return;
        }

        // 귀환 대기 중 확인 및 즉시 귀환 처리
        Boolean wasInReturnWaiting = ScoutManager.isReturnWaiting(playerId);
        if (wasInReturnWaiting) {
            Location beaconLocation = FamilyManager.getBeaconLocation(player.getName());
            if (beaconLocation != null) {
                player.teleport(beaconLocation);
                player.sendMessage(ChatColor.GREEN + "재접속하여 신호기로 즉시 이동되었습니다.");
            } else {
                player.sendMessage(ChatColor.RED + "신호기를 찾을 수 없습니다.");
            }

            ScoutManager.removeReturnWaitingState(playerId); // 대기 상태 제거
        }
    }

    public class ScoutManager {
        private static final Map<UUID, Boolean> scoutStates = new HashMap<>();
        private static final Map<UUID, Boolean> returnWaitingStates = new HashMap<>(); // 귀환 대기 상태 추가

        // 정찰 상태 저장
        public static void saveScoutState(UUID playerId, boolean isScouting) {
            scoutStates.put(playerId, isScouting);
        }

        // 정찰 상태 확인
        public static Boolean getScoutState(UUID playerId) {
            return scoutStates.get(playerId);
        }

        // 정찰 상태 제거
        public static void removeScoutState(UUID playerId) {
            scoutStates.remove(playerId);
        }

        // 귀환 대기 상태 저장
        public static void saveReturnWaitingState(UUID playerId, boolean isWaiting) {
            returnWaitingStates.put(playerId, isWaiting);
        }

        // 귀환 대기 상태 확인
        public static Boolean isReturnWaiting(UUID playerId) {
            return returnWaitingStates.getOrDefault(playerId, false);
        }

        // 귀환 대기 상태 제거
        public static void removeReturnWaitingState(UUID playerId) {
            returnWaitingStates.remove(playerId);
        }
    }

    private long getCooldownRemaining(UUID uuid) {
        long currentTime = System.currentTimeMillis();
        return cooldownMap.containsKey(uuid) ? cooldownMap.get(uuid) - currentTime : 0;
    }

    private void setCooldown(UUID uuid, long cooldown) {
        long currentTime = System.currentTimeMillis();
        cooldownMap.put(uuid, currentTime + cooldown);
        saveCooldowns();
    }

    private void loadCooldowns() {
        File file = new File(plugin.getDataFolder(), "cooldowns.yml");
        if (!file.exists()) return;
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            for (String key : config.getKeys(false)) {
                UUID uuid = UUID.fromString(key);
                long cooldownEnd = config.getLong(key);
                cooldownMap.put(uuid, cooldownEnd);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveCooldowns() {
        File file = new File(plugin.getDataFolder(), "cooldowns.yml");
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, Long> entry : cooldownMap.entrySet()) {
            config.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String formatTime(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(hours);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(minutes) - TimeUnit.HOURS.toSeconds(hours);
        return String.format("%02d시간 %02d분 %02d초", hours, minutes, seconds);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (cooldownMap.containsKey(uuid)) {
            cooldownMap.put(uuid, cooldownMap.get(uuid) - System.currentTimeMillis());
            saveCooldowns();
        }
    }
}