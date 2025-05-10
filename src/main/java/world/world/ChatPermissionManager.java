package world.world;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ChatPermissionManager implements Listener {

    private static final Map<String, Integer> chatPermissions = new HashMap<>();
    private static final Map<String, Long> damageCooldowns = new HashMap<>();
    private static final Random random = new Random();

    // 사용자에게 채팅권 추가
    public static void addChatPermission(String username, int count) {
        chatPermissions.put(username, chatPermissions.getOrDefault(username, 0) + count);
    }

    // 사용자에게서 채팅권 사용
    private static boolean useChatPermission(String username) {
        int count = chatPermissions.getOrDefault(username, 0);
        if (count > 0) {
            chatPermissions.put(username, count - 1);
            return true;
        }
        return false;
    }

    // 사용자가 채팅할 수 있는 권한이 있는지 확인하는 메소드
    public static boolean canUserChat(String username) {
        return chatPermissions.getOrDefault(username, 0) > 0;
    }

    // 엔더맨 처치 시 채팅권 드랍 확률 계산 및 부여
    public static void onEndermanKilled(EntityDeathEvent event, Player player) {
        if (random.nextInt(100) < 50) { // 50% 확률
            ItemStack chatPermissionItem = new ItemStack(Material.BOOK);
            ItemMeta meta = chatPermissionItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("채팅권");
                chatPermissionItem.setItemMeta(meta);
            }
            Location location = event.getEntity().getLocation();
            event.getEntity().getWorld().dropItem(location, chatPermissionItem);
        }
    }

    // 플레이어의 상호작용 이벤트 처리 (채팅권 사용)
    public static void handlePlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) &&
                itemInHand != null &&
                itemInHand.getType() == Material.BOOK &&
                itemInHand.getItemMeta() != null &&
                "채팅권".equals(itemInHand.getItemMeta().getDisplayName())) {

            String username = player.getName();

            // 채팅권 사용 로직
            boolean used = tryUseChatPermission(player, itemInHand);
            if (used) {
                player.sendMessage(ChatColor.GREEN + "채팅권을 사용하였습니다.");
            } else {
                player.sendMessage(ChatColor.RED + "채팅권 사용 실패.");
            }
        }
    }

    private static boolean tryUseChatPermission(Player player, ItemStack itemInHand) {
        String username = player.getName();
        itemInHand.setAmount(itemInHand.getAmount() - 1);
        if (itemInHand.getAmount() == 0) {
            player.getInventory().setItemInMainHand(null);
        }
        addChatPermission(username, 1);
        return true;
    }

    // 사용자가 채팅을 시도할 때 호출하는 메서드
    public static void handlePlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String username = player.getName();

        // 채팅권을 사용할 수 있으면 사용
        if (!canUserChat(username)) {
            event.setCancelled(true);
        } else {
            useChatPermission(username);
        }
    }

    // 사용자가 명령어를 시도할 때 호출되는 메서드
    public static void handlePlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String username = player.getName();

        // 데미지를 받았는지 검사하고 1분 동안 제한
        if (damageCooldowns.containsKey(username) && (System.currentTimeMillis() - damageCooldowns.get(username) < 1 * 60 * 1000)) {
            long cooldownStartTime = damageCooldowns.get(username);
            long cooldownMillis = 1 * 60 * 1000; // 1분
            String remainingTime = getRemainingCooldown(cooldownStartTime, cooldownMillis);

            player.sendMessage(ChatColor.RED + "최근 데미지를 받아 1분간 명령어를 사용할 수 없습니다. 남은시간: " + remainingTime);
            event.setCancelled(true);
        }
    }

    // 플레이어가 데미지를 받을 때 호출되는 메서드 (데미지를 받으면 1분간 채팅 및 명령어 제한)
    public static void handlePlayerDamage(Player player) {
        String username = player.getName();
        damageCooldowns.put(username, System.currentTimeMillis());
    }

    // 엔더맨이 죽었을 때 호출되는 이벤트 리스너
    public static void handleEntityDeath(EntityDeathEvent event) {
        EntityType entityType = event.getEntity().getType();
        Player killer = event.getEntity().getKiller();

        if (killer == null) return;

        if (entityType == EntityType.ENDERMAN) {
            onEndermanKilled(event, killer);
        } else if (entityType == EntityType.EVOKER) {
            onEvokerKilled(event, killer);
        }
    }

    // 소환사를 죽였을 때 황금사과 드랍
    private static void onEvokerKilled(EntityDeathEvent event, Player player) {
        event.getDrops().removeIf(item -> item.getType() == Material.TOTEM_OF_UNDYING);
        event.getDrops().add(new ItemStack(Material.GOLDEN_APPLE));
    }

    private static String getRemainingCooldown(long startTime, long cooldownMillis) {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - startTime;
        long remainingTime = cooldownMillis - elapsedTime;

        long seconds = remainingTime / 1000;

        return String.format("%d초", seconds);
    }

    // 불사의 토템 사용을 막기 위한 이벤트 리스너 추가
    @EventHandler
    public void onEntityResurrect(EntityResurrectEvent event) {
        if (event.getEntityType() == EntityType.PLAYER) {
            Player player = (Player) event.getEntity();
            if (player.getInventory().contains(Material.TOTEM_OF_UNDYING)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        if (event.getItem().getType() == Material.TOTEM_OF_UNDYING) {
            event.setCancelled(true);
        }
    }
}