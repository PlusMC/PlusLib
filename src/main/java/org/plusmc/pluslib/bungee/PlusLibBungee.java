package org.plusmc.pluslib.bungee;


import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.api.scheduler.TaskScheduler;
import org.plusmc.pluslibcore.mongo.DatabaseHandler;
import org.plusmc.pluslibcore.reflection.bungeebukkit.config.InjectableConfig;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class PlusLibBungee extends Plugin {
    private static PlusLibBungee instance;
    private ScheduledTask task;

    public static Logger logger() {
        return instance.getLogger();
    }

    public static PlusLibBungee getInstance() {
        return instance;
    }

    private static void setInstance(PlusLibBungee instance) {
        PlusLibBungee.instance = instance;
    }

    @Override
    public void onEnable() {
        setInstance(this);
        TaskScheduler scheduler = ProxyServer.getInstance().getScheduler();
        File file = new File(getDataFolder(), "config.yml");
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().severe("Could not create data folder!");
            return;
        }

        if (!file.exists()) {
            try (InputStream in = getResourceAsStream("config.yml")) {
                Files.copy(in, file.toPath());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        InjectableConfig config;
        try {
            config = InjectableConfig.create(file);
        } catch (IOException e) {
            throw new IllegalStateException("Could not load config", e);
        }

        DatabaseHandler.createInstance(config.section("Mongodb"));

        this.task = scheduler.schedule(this, () -> {
            if (DatabaseHandler.getInstance() != null && DatabaseHandler.getInstance().isLoaded())
                DatabaseHandler.getInstance().updateCache();
        }, 5, 30, TimeUnit.SECONDS);
    }

    @Override
    public void onDisable() {
        task.cancel();
        setInstance(null);
    }
}
