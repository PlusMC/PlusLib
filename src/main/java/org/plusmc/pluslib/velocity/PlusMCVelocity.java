package org.plusmc.pluslib.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyReloadEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import org.plusmc.pluslibcore.mongo.DatabaseHandler;
import org.plusmc.pluslibcore.reflection.velocitybukkit.VelocityBukkitReflection;
import org.plusmc.pluslibcore.reflection.velocitybukkit.config.InjectableConfig;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Plugin(id = "pluslib-velocity", name = "PlusLib", version = "0.14-INDEV", description = "PlusLib for Velocity", authors = {"OakleyCord"})
public class PlusMCVelocity {
    private final ProxyServer server;
    private final Logger logger;

    private final Path dataDirectory;

    @Inject
    public PlusMCVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;

        VelocityBukkitReflection.initProxy(server, logger, this);
    }

    public void loadDatabase() {
        File dataFolder = dataDirectory.toFile();
        File file = new File(dataDirectory.toFile(), "config.yml");
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            logger.severe("Could not create data folder!");
            return;
        }

        if (!file.exists()) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.yml")) {
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
    }

    public void startCaching(){
        server.getScheduler().buildTask(this, () -> {
            if (DatabaseHandler.getInstance() != null && DatabaseHandler.getInstance().isLoaded())
                DatabaseHandler.getInstance().updateCache();
        }).repeat(30L, TimeUnit.SECONDS).schedule();
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        loadDatabase();
        startCaching();
    }


    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        Collection<ScheduledTask> tasks = server.getScheduler().tasksByPlugin(this);

        for (ScheduledTask task : tasks) {
            task.cancel();
        }
    }

    @Subscribe
    public void onProxyReload(ProxyReloadEvent event) {
        Collection<ScheduledTask> tasks = server.getScheduler().tasksByPlugin(this);

        for (ScheduledTask task : tasks) {
            task.cancel();
        }

        //TODO: Make sure that plugins that use this are unloaded correctly with the new DatabaseHandler.stop() method
        DatabaseHandler.stopInstance();

        loadDatabase();

        startCaching();
    }

}
