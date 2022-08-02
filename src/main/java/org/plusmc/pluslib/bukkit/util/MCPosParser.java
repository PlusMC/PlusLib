package org.plusmc.pluslib.bukkit.util;

import com.google.common.base.Preconditions;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.plusmc.pluslib.bukkit.PlusLibBukkit;
import org.plusmc.pluslib.bukkit.managed.Tickable;
import org.plusmc.pluslib.bukkit.managing.BaseManager;
import org.plusmc.pluslib.bukkit.particle.PlusParticleEffect;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;


public class MCPosParser {


    static List<FallingBlock> fallingBlocks = new ArrayList<>();

    public static Location parse(Location location, Vector vector, String modifiers) {
        Preconditions.checkArgument(modifiers.length() == 3, "Modifier must be 3 characters long");

        char[] chars = modifiers.toCharArray();

        Vector looking = new Vector();
        Vector offset = new Vector();
        switch (chars[0]) {
            case '^' -> looking.setX(vector.getX());
            case '~' -> offset.setX(vector.getX());
            default -> location.setX(vector.getX());
        }
        switch (chars[1]) {
            case '^' -> looking.setY(vector.getY());
            case '~' -> offset.setY(vector.getY());
            default -> location.setY(vector.getY());
        }
        switch (chars[2]) {
            case '^' -> looking.setZ(vector.getZ());
            case '~' -> offset.setZ(vector.getZ());
            default -> location.setZ(vector.getZ());
        }
        location.add(offset);
        location = lookingPosParse(location, looking);
        return location;
    }

    public static Location lookingPosParse(Location location, Vector vector) {


        double yaw = location.getYaw();
        vector.rotateAroundY(-Math.toRadians(yaw));
        double upDownRot = Math.toRadians(location.getPitch());
        vector.rotateAroundNonUnitAxis(new Vector(1, 0, 0).rotateAroundY(-Math.toRadians(yaw)), upDownRot);
        return location.clone().add(vector);
    }

    public static Location collect(Location location, Vector vector) {
        double yaw = location.getYaw();
        vector.rotateAroundY(-Math.toRadians(yaw)); //left right
        double upDownRot = Math.toRadians(location.getPitch()); //split this with rotateAroundX and rotateAroundZ depending on the yaw
        vector.rotateAroundNonUnitAxis(location.getDirection().setY(0).crossProduct(vector), upDownRot); //up down

        return location.clone().add(vector);
    }

    public static void explodeAndImplode(Location location, List<PlusParticleEffect.PlusParticle> particles, float speed, boolean reverse, boolean special) {

        Tickable tickable = new Tickable() {
            final UUID id = UUID.randomUUID();
            final Location center = location.clone().add(location.getDirection().multiply(3.5));
            final BoundingBox box = new BoundingBox(center.getX() - 2, center.getY() - 2, center.getZ() - 2, center.getX() + 2, center.getY() + 2, center.getZ() + 2);

            double frame = reverse ? -90 : 90;

            @Override
            public String getName() {
                return "ParticleEffect-" + id;
            }

            @Override
            public void tick(long tick) {
                particles.forEach(particle -> {
                    Location loc = explodeAndImplodeTick(location, particle.vector().clone().add(new Vector(0, 0, 5)), frame);
                    loc.getWorld().spawnParticle(Particle.REDSTONE, loc, particle.count(), 0, 0, 0, 0, new Particle.DustOptions(particle.color(), particle.size()), true);
                });

                if (tick % 20 == 0) {
                    System.out.println("Frame: " + frame);
                }
                if (tick % 2 == 0) {
                    center.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1, 0.1f);
                }

                if (special) {

                    for (Iterator<FallingBlock> iterator = fallingBlocks.iterator(); iterator.hasNext(); ) {
                        FallingBlock fallingBlock = iterator.next();
                        if (!fallingBlock.isValid() || fallingBlock.getTicksLived() > 200) {
                            fallingBlock.remove();
                            iterator.remove();
                            fallingBlocks.remove(fallingBlock);
                        }
                    }

                    for (Entity entity : center.getWorld().getNearbyEntities(location, 20, 20, 20)) {
                        if (entity instanceof Player)
                            continue;

                        if (box.contains(entity.getBoundingBox())) {
                            entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 1, 0.01f);
                            entity.remove();
                            //explode(entity.getLocation(), Color.FUCHSIA);
                        }

                        Vector vector = center.toVector().subtract(entity.getLocation().toVector());

                        entity.setVelocity(vector.normalize().multiply(speed));
                    }

                    suckBlocks(center);
                }

                frame += reverse ? speed : -speed;
                if (frame < -90 || frame > 90) {
                    explode(location, Color.ORANGE);
                    center.getWorld().playSound(center, Sound.BLOCK_END_GATEWAY_SPAWN, 1, 0.1f);
                    Bukkit.getOnlinePlayers().forEach(player -> {
                        player.stopSound(Sound.BLOCK_BEACON_ACTIVATE);
                    });

                    BaseManager.unregisterAny(this, PlusLibBukkit.getInstance());
                }
            }
        };

        BaseManager.registerAny(tickable, PlusLibBukkit.getInstance());


    }

    private static Location explodeAndImplodeTick(Location location, Vector vector, double frame) {
        vector = vector.clone();
        location = location.clone();
        location.setPitch((float) frame);
        double yaw = location.getYaw();
        vector.rotateAroundY(-Math.toRadians(yaw)); //left right\
        double upDownRot = Math.toRadians(location.getPitch()); //split this with rotateAroundX and rotateAroundZ depending on the yaw
        vector.rotateAroundNonUnitAxis(location.getDirection().setY(0), upDownRot);
        return location.clone().add(vector);
    }

    private static void suckBlocks(Location location) {
        for (Block block : getBlocks(location, 20)) {
            if (0.01 < Math.random()) continue;
            BlockData data = block.getBlockData().clone();
            block.setType(Material.AIR);
            if (isBlockExposed(block)) {
                FallingBlock fallingBlock = block.getWorld().spawnFallingBlock(block.getLocation(), data);
                fallingBlocks.add(fallingBlock);
                fallingBlock.setDropItem(false);
                fallingBlock.getWorld().playSound(fallingBlock.getLocation(), fallingBlock.getBlockData().getSoundGroup().getBreakSound(), 1, 1);
            }
        }
    }

    private static void explode(Location location, Color color) {
        for (int i = 0; i < 25; i++) {
            Vector vector = Vector.getRandom();
            //have chance to be negative
            if (Math.random() < 0.5)
                vector.multiply(-1);

            for (float j = 0.01f; j < 1; j += 0.01) {
                location.getWorld().spawnParticle(Particle.REDSTONE, location.clone().add(vector.multiply(j + 1)), 1, 0, 0, 0, 0, new Particle.DustOptions(color, 1), true);
            }
        }
    }

    private static List<Block> getBlocks(Location start, int radius) {
        ArrayList<Block> blocks = new ArrayList<>();
        for (double x = start.getX() - radius; x <= start.getX() + radius; x++) {
            for (double y = start.getY() - radius; y <= start.getY() + radius; y++) {
                for (double z = start.getZ() - radius; z <= start.getZ() + radius; z++) {
                    Location loc = new Location(start.getWorld(), x, y, z);
                    if (loc.distance(start) <= radius && loc.getBlock().getType() != Material.AIR) {
                        blocks.add(loc.getBlock());
                    }
                }
            }
        }
        return blocks;
    }

    private static boolean isBlockExposed(Block block) {
        return block.getRelative(BlockFace.UP).getType() == Material.AIR;
    }

    private static Block getBlock(Location location) {
        return location.getBlock();
    }

}
