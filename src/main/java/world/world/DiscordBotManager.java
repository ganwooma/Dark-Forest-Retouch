package world.world;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class DiscordBotManager {
    private static JDA jda;
    private static TextChannel channel;

    public static void start(String token, String channelId) throws Exception {
        jda = JDABuilder.createDefault(token).build();
        jda.awaitReady(); // 봇이 준비될 때까지 대기
        channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            throw new IllegalStateException("채널 ID가 잘못되었습니다: " + channelId);
        }
    }

    public static void sendMessage(String message) {
        if (channel != null) {
            channel.sendMessage(message).queue();
        }
    }

    public static void shutdown() {
        if (jda != null) jda.shutdown();
    }
}
