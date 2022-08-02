package org.plusmc.pluslib.bukkit.particle;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.plusmc.pluslib.bukkit.util.MCPosParser;

import java.util.ArrayList;
import java.util.List;

public class PlusParticleEffect {
    private final List<PlusParticle> particles = new ArrayList<>();

    public List<PlusParticle> getParticles() {
        return particles;
    }

    public void play(Location location, World world) {
        for (PlusParticle particle : particles) {
            particle.play(location, world);
        }
    }

    public void playExplodeAndImplode(Location location, boolean reverse, float speed, boolean special) {
        MCPosParser.explodeAndImplode(location, particles, speed, reverse, special);
    }

    public record PlusParticle(Vector vector, String modifier, int size, Color color, int count, Vector delta) {

        public void play(Location location, World world) {
            location.setPitch(0);
            //location = MCPosParser.parse(location, vector, modifier);
            //world.spawnParticle(Particle.REDSTONE, location, count, delta.getX(), delta.getY(), delta.getZ(), 0, new Particle.DustOptions(color, size), true);
        }

    }
}
