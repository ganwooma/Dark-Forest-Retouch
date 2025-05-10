package world.world;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Random;

public class StartGameCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            // 서바이벌 플레이어들을 이동시키는 로직 처리
            start(player);
            return true;
        }

        sender.sendMessage("이 명령어는 플레이어만 사용할 수 있습니다.");
        return false;
    }

    private void start(Player player) {
        Random random = new Random();
        World overworld = Bukkit.getWorld("world");
        World nether = Bukkit.getWorld("world_nether");

        if (overworld == null) {
            player.sendMessage(ChatColor.RED + "오버월드가 로드되지 않았습니다!");
            Bukkit.getLogger().severe("오버월드가 null입니다. 서버의 월드 이름을 확인하세요.");
            return;
        }

        // 월드 경계 설정
        configureWorldBorder(overworld, 20000);

        if (nether == null) {
            player.sendMessage(ChatColor.RED + "네더 월드가 로드되지 않았습니다!");
            Bukkit.getLogger().severe("네더 월드가 null입니다.");
        } else {
            configureWorldBorder(nether, 160000);
        }

        boolean hasTarget = false; // 서바이벌 모드 플레이어 여부 확인

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() == GameMode.SURVIVAL) {
                hasTarget = true;

                // 랜덤한 위치 생성
                Location randomLocation = getRandomLocationWithinBorder(overworld, random);
                if (randomLocation == null) {
                    continue;
                }

                // 플레이어를 랜덤 위치로 이동
                p.teleport(randomLocation);

                // 해당 위치를 플레이어의 스폰포인트로 설정
                p.setBedSpawnLocation(randomLocation, true);
            }
        }

        if (!hasTarget) {
            player.sendMessage(ChatColor.RED + "서바이벌 모드 상태의 플레이어가 없습니다!");
        }
    }

    private Location getRandomLocationWithinBorder(World world, Random random) {
        WorldBorder border = world.getWorldBorder();
        double halfSize = border.getSize() / 2;
        double centerX = border.getCenter().getX();
        double centerZ = border.getCenter().getZ();

        for (int attempts = 0; attempts < 10; attempts++) { // 최대 10번 시도
            double x = centerX - halfSize + random.nextDouble() * border.getSize();
            double z = centerZ - halfSize + random.nextDouble() * border.getSize();
            int y = world.getHighestBlockYAt((int) x, (int) z);

            if (y <= 0) { // 유효하지 않은 위치는 반복
                continue;
            }

            Location location = new Location(world, x, y, z);

            // **추가: 특정 위치가 스폰에 적합한지 검사**
            if (isLocationSafe(world, location)) {
                return location;
            }
        }

        return null; // 유효한 위치 찾기 실패
    }

    private boolean isLocationSafe(World world, Location location) {
        // 플레이어가 실제로 서 있을 수 있는지 + 공간이 비어 있는지 검사
        Material blockType = world.getBlockAt(location).getType();
        Material aboveBlockType = world.getBlockAt(location.clone().add(0, 1, 0)).getType();
        Material belowBlockType = world.getBlockAt(location.clone().add(0, -1, 0)).getType();

        // 안전 조건: 플레이어 머리 위와 발밑이 비어 있어야 하고, 발밑 블록이 단단해야 함
        return blockType == Material.AIR && aboveBlockType == Material.AIR &&
                belowBlockType.isSolid();
    }


    private void configureWorldBorder(World world, int size) {
        WorldBorder border = world.getWorldBorder();
        border.setCenter(0, 0);
        border.setSize(size);
    }
}