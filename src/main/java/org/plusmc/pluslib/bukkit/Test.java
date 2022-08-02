package org.plusmc.pluslib.bukkit;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.plusmc.pluslib.bukkit.managed.PlusCommand;
import org.plusmc.pluslib.bukkit.particle.ParticleParser;
import org.plusmc.pluslib.bukkit.particle.PlusParticleEffect;

import java.io.IOException;
import java.io.InputStream;

public class Test implements PlusCommand {

    BukkitTask task = null;

    @Override
    public String getName() {
        return "test";
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is only available for players");
            return true;
        }

        /*
        if (task != null) {
            task.cancel();
            task = null;
            player.sendMessage("Stopped");
            return true;
        }
        task = Bukkit.getScheduler().runTaskTimer(PlusLibBukkit.getInstance(), () -> {
            for (int i = -5; i < 5; i++) {
                for (int j = -5; j < 5; j++) {
                    Location location = player.getLocation();
                    location = MCPosParser.parse(location, i, j, 3, "^^^");
                    player.getWorld().spawnParticle(Particle.REDSTONE, location, 1, new Particle.DustOptions(Color.fromRGB(255, 0, 255), 2));
                }
            }
        }, 0, 1);
        player.sendMessage("Started");

         */


        try {
            InputStream in = PlusLibBukkit.getInstance().getResource("particles/alchemymod.mcparticle");
            InputStream in2 = PlusLibBukkit.getInstance().getResource("particles/alchemymod2.mcparticle");
            PlusParticleEffect circle = ParticleParser.parseFile(in);
            PlusParticleEffect circle2 = ParticleParser.parseFile(in2);
            in.close();
            in2.close();
            player.sendMessage("parsed! particles: " + circle2.getParticles().size());
            circle.playExplodeAndImplode(player.getEyeLocation(), false, 0.75f, false);
            circle2.playExplodeAndImplode(player.getEyeLocation(), true, 0.75f, true);
            player.sendMessage("played!");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        Location location = player.getEyeLocation();
        location.setX(player.getBoundingBox().getCenterX());
        location.setZ(player.getBoundingBox().getCenterZ());
        //MCPosParser.explodeAndImplode(location, 10, 10, 2f, 1.5f);
        return true;
    }
}
