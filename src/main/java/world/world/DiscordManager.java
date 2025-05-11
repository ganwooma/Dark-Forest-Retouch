package world.world;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class DiscordManager {
    private final BeaconMain plugin;
    private final Logger logger;
    private JDA jda;
    private String defaultChannelId;
    private final Map<String, String> familyChannels = new HashMap<>();
    private boolean enabled = false;

    public DiscordManager(BeaconMain plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        loadConfig();
    }

    private void loadConfig() {
        plugin.saveDefaultConfig();
        FileConfiguration config = plugin.getConfig();

        enabled = config.getBoolean("discord.enabled", false);
        if (!enabled) {
            logger.info("디스코드 기능이 비활성화되어 있습니다.");
            return;
        }

        String token = config.getString("discord.token");
        if (token == null || token.equals("여기에_디스코드_봇_토큰_입력")) {
            logger.warning("디스코드 봇 토큰이 설정되지 않았습니다!");
            enabled = false;
            return;
        }

        defaultChannelId = config.getString("discord.default-channel-id");
        if (defaultChannelId == null || defaultChannelId.equals("기본_디스코드_채널_ID")) {
            logger.warning("기본 디스코드 채널 ID가 설정되지 않았습니다!");
        }

        // 가문별 채널 ID 로드
        ConfigurationSection familyChannelsSection = config.getConfigurationSection("discord.family-channels");
        if (familyChannelsSection != null) {
            for (String familyName : familyChannelsSection.getKeys(false)) {
                String channelId = familyChannelsSection.getString(familyName);
                if (channelId != null && !channelId.isEmpty()) {
                    familyChannels.put(familyName, channelId);
                }
            }
        }

        connectToDiscord(token);
    }

    private void connectToDiscord(String token) {
        try {
            jda = JDABuilder.createDefault(token)
                    .enableIntents(EnumSet.of(
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.MESSAGE_CONTENT
                    ))
                    .build();

            jda.awaitReady();
            logger.info("디스코드 봇이 성공적으로 연결되었습니다!");

            // 테스트 메시지 전송
            sendMessageToDefaultChannel("마인크래프트 서버가 시작되었습니다.");
        } catch (Exception e) {
            logger.severe("디스코드 봇 연결 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            enabled = false;
        }
    }

    public void reload() {
        if (jda != null) {
            jda.shutdown();
        }

        familyChannels.clear();
        loadConfig();
    }

    public void sendMessageToDefaultChannel(String message) {
        if (!enabled || jda == null || defaultChannelId == null) return;

        TextChannel channel = jda.getTextChannelById(defaultChannelId);
        if (channel != null) {
            channel.sendMessage(message).queue();
        } else {
            logger.warning("기본 채널을 찾을 수 없습니다. ID: " + defaultChannelId);
        }
    }

    public void sendFamilyAlert(String familyName, String message) {
        if (!enabled || jda == null) return;

        String channelId = familyChannels.getOrDefault(familyName, defaultChannelId);
        if (channelId == null) return;

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel != null) {
            channel.sendMessage(message).queue();
        } else {
            logger.warning(familyName + " 가문의 채널을 찾을 수 없습니다. ID: " + channelId);
        }
    }

    public void setFamilyChannel(String familyName, String channelId) {
        familyChannels.put(familyName, channelId);

        // 설정 저장
        FileConfiguration config = plugin.getConfig();
        config.set("discord.family-channels." + familyName, channelId);
        plugin.saveConfig();

        // 테스트 메시지
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel != null) {
            channel.sendMessage("이제 " + familyName + " 가문의 알림이 이 채널로 전송됩니다.").queue();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void shutdown() {
        if (jda != null) {
            sendMessageToDefaultChannel("마인크래프트 서버가 종료되었습니다.");
            jda.shutdown();
        }
    }
}