package org.plusmc.pluslib.bukkit.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.plusmc.pluslib.bukkit.PlusLibBukkit;
import org.plusmc.pluslibcore.reflection.bukkitpaper.timings.WrappedTimings;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

/**
 * A utility class for Bukkit.
 */
@SuppressWarnings("unused")
public class BukkitUtil {
    private static final HashMap<Player, Long> JOIN_TIMES = new HashMap<>();


    private BukkitUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Get a list of online player's names
     *
     * @return List of online player's names
     */
    public static List<String> allPlayers() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
    }

    /**
     * Get the time since the player joined the server
     *
     * @param player Player to get
     * @return Time since the player joined the server
     */
    public static long getTimeSinceJoin(Player player) {
        return System.currentTimeMillis() - JOIN_TIMES.getOrDefault(player, System.currentTimeMillis());
    }

    //stolen from spigot (kinda) and modified to fit our needs
    public static void registerWithPreChecks(org.bukkit.event.Listener listener, JavaPlugin plugin, BiConsumer<AtomicBoolean, Event> preChecks) {
        Set<Method> methods;
        try {
            Method[] publicMethods = listener.getClass().getMethods();
            Method[] privateMethods = listener.getClass().getDeclaredMethods();
            methods = new HashSet<>(publicMethods.length + privateMethods.length, 1.0f);
            methods.addAll(Arrays.asList(publicMethods));
            methods.addAll(Arrays.asList(privateMethods));
        } catch (NoClassDefFoundError e) {
            throw new IllegalStateException("Listener class does not exist");
        }

        for (Method method : methods) {
            EventHandler eventHandler = method.getAnnotation(EventHandler.class);
            if (eventHandler == null) continue;
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != 1 || !Event.class.isAssignableFrom(parameterTypes[0]))
                continue;

            Class<?> checkClass = parameterTypes[0];

            Class<? extends Event> eventClass = checkClass.asSubclass(Event.class);
            method.setAccessible(true);
            final WrappedTimings timings = WrappedTimings.create(plugin, "Plugin: " + plugin.getDescription().getFullName() + " Event: " + listener.getClass().getName() + "::" + method.getName() + "(" + eventClass.getSimpleName() + ")");
            Bukkit.getPluginManager().registerEvent(eventClass, listener, eventHandler.priority(), (listener1, event) -> {
                if (!eventClass.isAssignableFrom(event.getClass()))
                    return;

                try {
                    AtomicBoolean cancelled = new AtomicBoolean(false);
                    preChecks.accept(cancelled, event);
                    if (cancelled.get()) return;

                    boolean isAsync = event.isAsynchronous();
                    if (!isAsync) timings.startTiming();
                    method.invoke(listener1, event);
                    if (!isAsync) timings.stopTiming();
                } catch (Exception e) {
                    throw new EventException(e);
                }
            }, plugin);
        }
    }


    /**
     * Listener for {@link BukkitUtil} (DO NOT REGISTER THIS LISTENER FOR INTERNAL USE ONLY)
     */
    public static class Listener implements org.bukkit.event.Listener {
        /**
         * Event handler for {@link PlayerJoinEvent}
         *
         * @param event Event to handle
         */

        boolean isFirst = true;

        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event) {
            Player player = event.getPlayer();
            JOIN_TIMES.put(player, System.currentTimeMillis());
            if (isFirst) {
                Bukkit.getScheduler().runTask(PlusLibBukkit.getInstance(), BungeeUtil::checkPlusMC);
                isFirst = false;
            }
        }
    }
}
