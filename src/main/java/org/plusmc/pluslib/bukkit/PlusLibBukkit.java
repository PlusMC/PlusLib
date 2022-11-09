package org.plusmc.pluslib.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.plusmc.pluslib.bukkit.util.BukkitUtil;
import org.plusmc.pluslib.bukkit.util.BungeeUtil;
import org.plusmc.pluslib.bukkit.voicechat.PlusLibVoicechat;
import org.plusmc.pluslibcore.mongo.DatabaseHandler;
import org.plusmc.pluslibcore.reflection.velocitybukkit.config.InjectableConfig;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;


/**
 * The plugin class for the PlusLib
 * Don't use this class if you are
 */
@SuppressWarnings("unused")
public final class PlusLibBukkit extends JavaPlugin {

    public static final String BUNGEE_CORD = "BungeeCord";
    public static final String PLUSMC_BUNGEE = "plusmc:bungee";

    /**
     * Gets the {@link Logger} of the plugin.
     * Do not use this method, it's only for internal use.
     *
     * @return The {@link Logger} of the plugin
     */
    public static Logger logger() {
        return PlusLibBukkit.getInstance().getLogger();
    }

    /**
     * Gets the instance of the plugin.
     * Do not use this method, it's only for internal use.
     *
     * @return The instance of the plugin
     */
    public static PlusLibBukkit getInstance() {
        return JavaPlugin.getPlugin(PlusLibBukkit.class);
    }

    @Override
    public void onDisable() {
        Bukkit.getMessenger().unregisterIncomingPluginChannel(this, BUNGEE_CORD);
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(this, BUNGEE_CORD);

        Bukkit.getMessenger().unregisterIncomingPluginChannel(this, PLUSMC_BUNGEE);
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(this, PLUSMC_BUNGEE);
    }

    @Override
    public void onEnable() {
        BungeeUtil util = new BungeeUtil();
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, BUNGEE_CORD);
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, PLUSMC_BUNGEE);

        Bukkit.getMessenger().registerIncomingPluginChannel(this, BUNGEE_CORD, util);
        Bukkit.getMessenger().registerIncomingPluginChannel(this, PLUSMC_BUNGEE, util);

        saveDefaultConfig();
        InjectableConfig config;
        try {
            config = InjectableConfig.create(new File(getDataFolder(), "config.yml"));
        } catch (IOException e) {
            throw new IllegalStateException("Could not load config", e);
        }

        DatabaseHandler.createInstance(config.section("Mongodb"));

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (DatabaseHandler.getInstance() != null && DatabaseHandler.getInstance().isLoaded())
                DatabaseHandler.getInstance().updateCache();
        }, 100, 600);

        if (Bukkit.getPluginManager().isPluginEnabled("voicechat")) {
            getLogger().info("VoiceChat detected, enabling VoiceChat support");
            PlusLibVoicechat.createInstance(config.section("VoiceChat"));
        }

        Bukkit.getPluginManager().registerEvents(new BukkitUtil.Listener(), this);
    }


}
