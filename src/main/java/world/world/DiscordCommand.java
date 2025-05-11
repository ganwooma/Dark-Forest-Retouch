package world.world;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DiscordCommand implements CommandExecutor {
    private final BeaconMain plugin;
    private final DiscordManager discordManager;

    public DiscordCommand(BeaconMain plugin, DiscordManager discordManager) {
        this.plugin = plugin;
        this.discordManager = discordManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("beaconplugin.discord")) {
            sender.sendMessage(ChatColor.RED + "이 명령어를 사용할 권한이 없습니다!");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "사용법: /discord <set|reload> [가문이름] [채널ID]");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                discordManager.reload();
                sender.sendMessage(ChatColor.GREEN + "디스코드 설정이 리로드되었습니다.");
                return true;

            case "set":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "사용법: /discord set <가문이름> <채널ID>");
                    return true;
                }

                String familyName = args[1];
                String channelId = args[2];

                discordManager.setFamilyChannel(familyName, channelId);
                sender.sendMessage(ChatColor.GREEN + familyName + " 가문의 알림 채널이 설정되었습니다.");
                return true;

            default:
                sender.sendMessage(ChatColor.RED + "알 수 없는 서브 명령어: " + args[0]);
                sender.sendMessage(ChatColor.YELLOW + "사용법: /discord <set|reload> [가문이름] [채널ID]");
                return true;
        }
    }
}