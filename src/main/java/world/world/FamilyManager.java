package world.world;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

public class FamilyManager implements Listener {
    private final BeaconMain plugin;
    private static final List<ChatColor> usedColors = new ArrayList<>();
    private static final ChatColor[] availableColors = {
            ChatColor.RED, ChatColor.BLUE, ChatColor.GREEN, ChatColor.YELLOW,
            ChatColor.LIGHT_PURPLE, ChatColor.AQUA,
            ChatColor.GOLD, ChatColor.DARK_GREEN, ChatColor.DARK_AQUA, ChatColor.DARK_PURPLE
    };

    private static final Map<String, Family> families = new HashMap<>();
    private static final int RECOVERY_COOLDOWN_HOURS = 12;

    // 플러그인 디렉토리 위치와 파일 이름 처리
    private static final String PLUGIN_FOLDER = "plugins/BeaconManager";
    private static String FILE_PATH; // 값 변경 가능하도록 선언

    public FamilyManager(BeaconMain plugin) {
        this.plugin = plugin;

        // FILE_PATH 설정 (플러그인 데이터를 저장하는 디렉토리 기반으로 설정)
        FILE_PATH = plugin.getDataFolder().getAbsolutePath() + "/families.json";

        // 플러그인 디렉토리가 없을 경우 생성
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        // 데이터 로드
        loadFamilies();
    }

    public class Family {
        private final String leader;
        private final Set<String> members;
        private Location beaconLocation;
        private long lastRecoveryTime;
        private ChatColor teamColor;
        private String teamName; // 팀 이름 추가

        public Family(String leader, Location beaconLocation, ChatColor teamColor) {
            this.leader = leader;
            this.members = new HashSet<>();
            this.members.add(leader); // 리더는 자동으로 멤버에 추가됩니다
            this.beaconLocation = beaconLocation;
            this.lastRecoveryTime = 0;
            this.teamColor = teamColor; // 초기 팀 색상 설정
            this.teamName = leader; // 기본적으로 팀 이름은 리더 이름으로 설정됩니다
        }
        public void setTeamColor(ChatColor teamColor) {
            this.teamColor = teamColor;
        }

        // Getter와 Setter 메소드
        public String getLeader() {
            return leader;
        }

        public Set<String> getMembers() {
            return members;
        }

        public Location getBeaconLocation() {
            return beaconLocation;
        }

        public void setBeaconLocation(Location newLocation) {
            this.beaconLocation = newLocation;
        }

        public long getLastRecoveryTime() {
            return lastRecoveryTime;
        }

        public void setLastRecoveryTime(long time) {
            this.lastRecoveryTime = time;
        }

        public ChatColor getTeamColor() {
            return teamColor;
        }


        public void addMember(String memberName) {
            this.members.add(memberName);
        }

        public boolean removeMemberFromFamily(String member) {
            if (members.contains(member)) {
                members.remove(member);
                return true;
            }
            return false;
        }

        // getLeaderColor 메서드 추가
        public ChatColor getLeaderColor() {
            return this.teamColor;
        }

        public String getTeamName() {
            return leader; // 팀 이름 = 리더 이름
        }

        public void setTeamName(String teamName) {
            this.teamName = teamName;
        }
    }

    public void createFamily(String leader, Location beaconLocation, ChatColor teamColor) {
        families.put(leader, new Family(leader, beaconLocation, teamColor)); // 가문 이름은 리더 이름으로 자동 설정
        Family family = families.get(leader);
        usedColors.add(teamColor);
        updateTeamColor(family);
        saveFamilies();
    }

    public static boolean hasFamily(String playerName) {
        return families.values().stream()
                .anyMatch(family -> family.getMembers().contains(playerName));
    }

    public static Family getFamily(String playerName) {
        return families.values().stream()
                .filter(family -> family.getMembers().contains(playerName))
                .findFirst().orElse(null);
    }

    public static Family getFamilyByLeader(String leaderName) {
        return families.get(leaderName);
    }


    public static Location getBeaconLocation(String playerName) {
        Family family = getFamily(playerName);
        if (family == null) {
            //System.err.println("가족 정보가 없습니다: " + playerName);
            return null;
        }
        return family.getBeaconLocation();
    }

    public static void setBeaconLocation(String playerName, Location location) {
        Family family = getFamily(playerName);
        if (family != null) {
            //System.out.println("Setting beacon location for player: " + playerName + " at " + location);
            family.setBeaconLocation(location);
            saveFamilies();
        } else {
            //System.err.println("Error: Family not found for player: " + playerName);
        }
    }

    public static void removeBeacon(String playerName) {
        Family family = getFamily(playerName);
        if (family != null) {
            family.setBeaconLocation(null);
            family.setLastRecoveryTime(System.currentTimeMillis());
            saveFamilies();
        }
    }

    public ChatColor absorbFamily(String absorbingPlayer, String targetLeader) {
        // 흡수하려는 가문
        Family absorbingFamily = getFamily(absorbingPlayer);
        // 흡수 대상 가문
        Family targetFamily = families.get(targetLeader);

        if (absorbingFamily == null) {
            Bukkit.getLogger().warning("[Beacon_Plugin] Absorbing family not found for player: " + absorbingPlayer);
            return null;
        }

        if (targetFamily == null) {
            Bukkit.getLogger().warning("[Beacon_Plugin] Target family not found for leader: " + targetLeader);
            return null;
        }

        // 흡수 대상 가문을 가족 맵에서 제거
        families.remove(targetLeader);

        // 흡수
        absorbingFamily.getMembers().addAll(targetFamily.getMembers());
        usedColors.remove(targetFamily.getTeamColor()); // 사용된 색상에서 제거

        // 모든 멤버의 팀 색상 업데이트
        for (String member : targetFamily.getMembers()) {
            Player player = Bukkit.getPlayer(member);
            if (player != null && player.isOnline()) {
                plugin.setTeamColor(player, absorbingFamily.getTeamColor());
                player.sendMessage(ChatColor.GREEN + "You have been absorbed into " + absorbingFamily.getTeamName() + " family!");
            }
        }

        // 흡수된 대상 가문의 색상 및 데이터 저장
        absorbingFamily.setLastRecoveryTime(System.currentTimeMillis());
        updateTeamColor(absorbingFamily);
        saveFamilies();

        // 흡수된 가문의 색상 리턴
        return targetFamily.getTeamColor();
    }

    private void updateTeamColor(Family family) {
        ChatColor teamColor = family.getTeamColor();
        for (String member : family.getMembers()) {
            Player player = Bukkit.getPlayer(member);
            if (player != null && player.isOnline()) {
                plugin.setTeamColor(player, teamColor);
            }
        }
    }


    public static boolean canRecoverBeacon(String playerName) {
        Family family = getFamily(playerName);
        if (family != null) {
            long cooldownMillis = RECOVERY_COOLDOWN_HOURS * 60 * 60 * 1000;
            return System.currentTimeMillis() - family.getLastRecoveryTime() >= cooldownMillis;
        }
        return false;
    }

    public static String getRecoveryCooldownRemaining(String playerName) {
        Family family = getFamily(playerName);
        if (family != null) {
            long cooldownMillis = RECOVERY_COOLDOWN_HOURS * 60 * 60 * 1000;
            long timeElapsed = System.currentTimeMillis() - family.getLastRecoveryTime();
            long remainingMillis = Math.max(cooldownMillis - timeElapsed, 0);
            long seconds = (remainingMillis / 1000) % 60;
            long minutes = (remainingMillis / (1000 * 60)) % 60;
            long hours = (remainingMillis / (1000 * 60 * 60)) % 24;
            return String.format("%02d시간 %02d분 %02d초", hours, minutes, seconds);
        }
        return "00시간 00분 00초";
    }



    public static Set<Family> getAllFamilies() {
        return new HashSet<>(families.values());
    }


    public static ChatColor getFamilyColor(String playerName) {
        Family family = getFamily(playerName);
        return family != null ? family.getTeamColor() : null;
    }


    public static ChatColor getRandomTeamColor() {
        List<ChatColor> availableOptions = new ArrayList<>(Arrays.asList(availableColors));
        availableOptions.removeAll(usedColors);
        if (availableOptions.isEmpty()) return null;
        Random random = new Random();
        return availableOptions.get(random.nextInt(availableOptions.size()));
    }

    public void addToFamily(String playerName, String leaderName) {
        Family family = getFamilyByLeader(leaderName);
        if (family == null) {
            throw new RuntimeException("Target family does not exist.");
        }

        // 가문에 플레이어 추가
        family.addMember(playerName);

        // 플레이어가 온라인이라면 상태 변경
        Player player = Bukkit.getPlayer(playerName);
        if (player != null && player.isOnline()) {
            player.setGameMode(GameMode.SURVIVAL);
            player.sendMessage(ChatColor.GREEN + family.getTeamName() + " 가문에 가입되었습니다!");
        }

        // 리더에게 가입 메시지 전달
        Player leader = Bukkit.getPlayer(leaderName);
        if (leader != null && leader.isOnline()) {
            leader.sendMessage(ChatColor.YELLOW + playerName + " 님이 가문에 가입하였습니다!");
        }
    }

    public boolean removeMember(String playerName) {
        Family family = getFamily(playerName);
        if (family != null) {
            boolean result = family.removeMemberFromFamily(playerName);
            if (result && family.getLeader().equals(playerName)) {
                families.remove(playerName);
            }
            saveFamilies();
            return result;
        }
        return false;
    }



    public static synchronized void saveFamilies() {
        try {
            File folder = new File(PLUGIN_FOLDER);

            // 플러그인 폴더 확인 및 생성
            if (!folder.exists() && !folder.mkdirs()) {
                Bukkit.getLogger().severe("[Beacon_Plugin] Failed to create plugin directory!");
                return;
            }

            File file = new File(FILE_PATH);

            // families 맵이 비어 있는지 확인 후 저장
            if (families.isEmpty()) {
                Bukkit.getLogger().warning("[Beacon_Plugin] Warning: No families to save.");
                return;
            }

            Map<String, Map<String, Object>> jsonData = new HashMap<>();
            for (Map.Entry<String, Family> entry : families.entrySet()) {
                Family family = entry.getValue();

                Map<String, Object> familyData = new HashMap<>();
                familyData.put("leader", family.getLeader());
                familyData.put("members", new ArrayList<>(family.getMembers()));
                familyData.put("beaconLocation", serializeLocation(family.getBeaconLocation()));
                familyData.put("lastRecoveryTime", family.getLastRecoveryTime());
                familyData.put("teamColor", family.getTeamColor().name());
                familyData.put("teamName", family.getTeamName());

                jsonData.put(entry.getKey(), familyData);
            }

            Gson gson = new Gson();
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(gson.toJson(jsonData));
                Bukkit.getLogger().info("[Beacon_Plugin] Families successfully saved to file: " + file.getAbsolutePath());
            }
        } catch (IOException e) {
            Bukkit.getLogger().severe("[Beacon_Plugin] Failed to save families: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public synchronized void loadFamilies() {
        File file = new File(FILE_PATH);

        // 파일 존재 여부 확인
        if (!file.exists()) {
            Bukkit.getLogger().warning("[Beacon_Plugin] Families file not found. Skipping load.");
            return;
        }

        try (FileReader reader = new FileReader(file)) {
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, Map<String, Object>>>() {}.getType();

            Map<String, Map<String, Object>> jsonData = gson.fromJson(reader, type);

            if (jsonData == null || jsonData.isEmpty()) {
                Bukkit.getLogger().warning("[Beacon_Plugin] Families file is empty or invalid.");
                return;
            }

            families.clear();
            for (Map.Entry<String, Map<String, Object>> entry : jsonData.entrySet()) {
                Map<String, Object> data = entry.getValue();

                String leader = (String) data.get("leader");
                List<String> members = (List<String>) data.get("members");
                Location beaconLocation = parseLocation((Map<String, Object>) data.get("beaconLocation"));
                long lastRecoveryTime = ((Double) data.get("lastRecoveryTime")).longValue();
                ChatColor teamColor = ChatColor.valueOf((String) data.get("teamColor"));
                String teamName = (String) data.get("teamName");

                Family family = new Family(leader, beaconLocation, teamColor);
                family.getMembers().addAll(members);
                family.setLastRecoveryTime(lastRecoveryTime);
                family.setTeamName(teamName);

                families.put(leader, family);
            }

            Bukkit.getLogger().info("[Beacon_Plugin] Families successfully loaded from file: " + file.getAbsolutePath());
        } catch (Exception e) {
            Bukkit.getLogger().severe("[Beacon_Plugin] Failed to load families: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Location 객체를 JSON 데이터로 변환
    private static Map<String, Object> serializeLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }

        Map<String, Object> map = new HashMap<>();
        map.put("world", location.getWorld().getName());
        map.put("x", location.getX());
        map.put("y", location.getY());
        map.put("z", location.getZ());
        return map;
    }

    // JSON 데이터를 Location 객체로 변환
    private static Location parseLocation(Map<String, Object> map) {
        if (map == null || map.get("world") == null) {
            return null;
        }

        World world = Bukkit.getWorld((String) map.get("world"));
        double x = ((Number) map.get("x")).doubleValue();
        double y = ((Number) map.get("y")).doubleValue();
        double z = ((Number) map.get("z")).doubleValue();
        return new Location(world, x, y, z);
    }
}