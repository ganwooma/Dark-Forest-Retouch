package world.world;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class FamilyCommand implements CommandExecutor {
    private final FamilyManager familyManager;

    private static final List<ChatColor> usedColors = new ArrayList<>();

    public static List<ChatColor> getUsedColors() {
        return usedColors;
    }

    public FamilyCommand(FamilyManager familyManager) {
        this.familyManager = familyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("플레이어만 사용할 수 있습니다.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "올바르지 않은 명령어 사용입니다. 사용 가능한 명령어: /family <create|join|leave|info|color>");
            return true;
        }

        String subCommand = args[0];

        switch (subCommand.toLowerCase()) {
            case "create":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "가문 이름을 입력해주세요: /family create <가문이름>");
                    return true;
                }
                String familyName = args[1];
                Location playerLocation = player.getLocation();
                ChatColor familyColor = FamilyManager.getRandomTeamColor();
                if (familyColor == null) {
                    player.sendMessage(ChatColor.RED + "사용 가능한 팀 색상이 없습니다.");
                    return true;
                }

                familyManager.createFamily(player.getName(), playerLocation, familyColor);
                player.sendMessage(ChatColor.GREEN + "가문 '" + familyName + "'이(가) 성공적으로 생성되었습니다.");
                return true;

            case "join":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "가입할 가문 이름을 입력해주세요: /family join <리더이름>");
                    return true;
                }
                String leaderName = args[1];
                familyManager.addToFamily(player.getName(), leaderName);
                return true;

            case "leave":
                FamilyManager.Family family = FamilyManager.getFamily(player.getName());
                if (family != null) {
                    familyManager.removeMember(player.getName());
                    player.sendMessage(ChatColor.GREEN + "성공적으로 가문을 탈퇴하였습니다.");
                } else {
                    player.sendMessage(ChatColor.RED + "가문에 속해 있지 않습니다.");
                }
                return true;

            case "info":
                family = FamilyManager.getFamily(player.getName());
                if (family != null) {
                    player.sendMessage(ChatColor.GOLD + "가문 이름: " + family.getLeader());
                    player.sendMessage(ChatColor.GOLD + "가문 리더: " + family.getLeader());
                    player.sendMessage(ChatColor.GOLD + "가문 멤버: " + String.join(", ", family.getMembers()));
                } else {
                    player.sendMessage(ChatColor.RED + "가문에 속해 있지 않습니다.");
                }
                return true;

            case "color":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "변경할 색상을 입력해주세요: /family color <색상>");
                    return true;
                }
                String colorName = args[1].toUpperCase();
                ChatColor newColor;

                try {
                    newColor = ChatColor.valueOf(colorName);
                } catch (IllegalArgumentException e) {
                    player.sendMessage(ChatColor.RED + "잘못된 색상입니다. 지원되는 색상을 입력해주세요.");
                    return true;
                }

                FamilyManager.Family playerFamily = FamilyManager.getFamily(player.getName());

                if (playerFamily == null) {
                    player.sendMessage(ChatColor.RED + "가문에 속해 있지 않습니다.");
                    return true;
                }

                // 리더 여부 확인
                if (!playerFamily.getLeader().equals(player.getName())) {
                    player.sendMessage(ChatColor.RED + "가문 색상을 변경할 권한이 없습니다. 리더만 색상을 변경할 수 있습니다.");
                    return true;
                }

                // 색상 변경
                ChatColor oldColor = playerFamily.getTeamColor(); // 기존 색상
                playerFamily.setTeamColor(newColor);             // 새 색상으로 변경

                // 변경 사항 저장
                FamilyManager.saveFamilies();
                player.sendMessage(ChatColor.GREEN + "가문의 색상이 " + oldColor + "에서 " + newColor + "로 변경되었습니다!");
                return true;

            default:
                player.sendMessage(ChatColor.RED + "올바르지 않은 명령어 사용입니다. 사용 가능한 명령어: /family <create|join|leave|info|color>");
                return true;
        }
    }
}
