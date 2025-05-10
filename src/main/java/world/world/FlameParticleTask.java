package world.world;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class FlameParticleTask extends BukkitRunnable {

    private final Location start;
    private final Location end;
    private double t = 0; // 진행 상태
    private final double speed = 1.5; // 속도 (한 틱당 이동량)

    public FlameParticleTask(Location start, Location end) {
        this.start = start.clone();
        this.end = end.clone();
    }

    @Override
    public void run() {
        Vector direction = end.clone().subtract(start).toVector().normalize(); // 이동 방향
        start.add(direction.multiply(speed)); // 현재 위치 갱신

        // 해당 위치에 불꽃 파티클 생성
        start.getWorld().spawnParticle(
                Particle.FLAME,
                start,
                30, // 파티클 개수
                0.5, 0.5, 0.5, // 반지름
                0.1 // 속도
        );

        t += speed;

        // 목적지에 도달했거나 충분한 시간이 지나면 종료
        if (t >= 40 || start.distance(end) < speed) {
            this.cancel();
        }
    }
}