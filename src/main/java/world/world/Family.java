package world.world;

import org.bukkit.ChatColor;
import org.bukkit.Location;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Family {
    private final String leader; // 리더 필드는 변경 불가능하도록 final 추가
    private final ChatColor teamColor; // 팀 컬러 필드도 final 추가
    private final String teamName; // 팀 이름 필드도 final 추가
    private final Set<String> members; // Set은 내부적으로 수정되므로 final 유지
    private Location beaconLocation;

    // 생성자
    public Family(String leader, Location beaconLocation, ChatColor teamColor, String teamName) {
        if (leader == null || leader.isEmpty()) {
            throw new IllegalArgumentException("리더 이름은 null이거나 비어있을 수 없습니다.");
        }
        if (teamName == null || teamName.isEmpty()) {
            throw new IllegalArgumentException("팀 이름은 null이거나 비어있을 수 없습니다.");
        }

        // 초기화
        this.leader = leader;
        this.teamColor = teamColor;
        this.beaconLocation = beaconLocation;
        this.teamName = teamName;
        this.members = new HashSet<>();
        this.members.add(leader); // 리더를 멤버로 자동 추가
    }

    // 팀 이름 반환
    public String getTeamName() {
        return teamName;
    }

    // equals 및 hashCode 구현
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Family family = (Family) obj;
        return Objects.equals(leader, family.leader) &&
                Objects.equals(teamName, family.teamName); // 리더 및 팀 이름을 기준으로 비교
    }

    @Override
    public int hashCode() {
        return Objects.hash(leader, teamName); // equals와 일치하는 필드로 hashCode 구현
    }

    // toString 메서드 추가 (로깅 및 디버깅에 유용)
    @Override
    public String toString() {
        return "Family{" +
                "leader='" + leader + '\'' +
                ", teamColor=" + teamColor +
                ", teamName='" + teamName + '\'' +
                ", members=" + members +
                ", beaconLocation=" + beaconLocation +
                '}';
    }
}