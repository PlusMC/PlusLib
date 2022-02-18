package org.plusmc.pluslib.managing;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.SimplePluginManager;
import org.plusmc.pluslib.PlusLib;
import org.plusmc.pluslib.managed.Loadable;
import org.plusmc.pluslib.managed.PlusCommand;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides a way to register {@link PlusCommand}.
 * Using reflection.
 */
@SuppressWarnings("unused")
public class PlusCommandManager extends GeneralManager {
    private final CommandMap COMMAND_MAP = getCommandMap();
    private final Plugin plugin;
    private List<PlusCommand> COMMANDS = new ArrayList<>();

    protected PlusCommandManager(Plugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    private static PluginCommand createCommand(String name, Plugin plugin) {
        PluginCommand command = null;
        try {
            Constructor<PluginCommand> c = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            c.setAccessible(true);

            command = c.newInstance(name, plugin);
        } catch (Exception e) {
            PlusLib.logger().severe("Failed to create PluginCommand");
        }

        return command;
    }

    private static CommandMap getCommandMap() {
        CommandMap commandMap = null;
        try {
            if (Bukkit.getPluginManager() instanceof SimplePluginManager) {
                Field f = SimplePluginManager.class.getDeclaredField("commandMap");
                f.setAccessible(true);

                commandMap = (CommandMap) f.get(Bukkit.getPluginManager());
            }
        } catch (Exception e) {
            PlusLib.logger().severe("Failed to get CommandMap");
        }

        return commandMap;
    }

    /**
     * Registers the command.
     *
     * @param cmd Command to register
     */
    @Override
    public void register(Loadable cmd) {
        if (!(cmd instanceof PlusCommand pCmd)) return;
        PluginCommand command = createCommand(pCmd.getName(), pCmd.getPlugin());
        command.setExecutor(pCmd);
        command.setTabCompleter(pCmd);
        command.setPermission(pCmd.getPermission());
        command.setUsage(pCmd.getUsage());
        command.setDescription(pCmd.getDescription());
        COMMAND_MAP.register(pCmd.getPlugin().getName(), command);
        COMMANDS.add(pCmd);
        pCmd.load();
        PlusLib.logger().info("Registered command: " + pCmd.getName());
    }

    /**
     * Registers the command. Will be called automatically when the library plugin is unloaded.
     *
     * @param cmd Command to unload
     */
    @Override
    public void unregister(Loadable cmd) {
        if (!(cmd instanceof PlusCommand pCmd)) return;
        CommandMap COMMAND_MAP = getCommandMap();
        PluginCommand command = pCmd.getPlugin().getCommand(pCmd.getName());
        if (command == null) {
            PlusLib.logger().warning("Failed to unregister command: " + pCmd.getName());
            return;
        }
        command.unregister(COMMAND_MAP);
        pCmd.unload();
        PlusLib.logger().info("Unregistered command: " + pCmd.getName());
    }

    @Override
    Plugin getPlugin() {
        return plugin;
    }

    @Override
    protected void init() {
        COMMANDS = new ArrayList<>();
    }

    @Override
    public Class<? extends Loadable> getLoadableClass() {
        return PlusCommand.class;
    }


    @Override
    protected void shutdown() {
        COMMANDS.forEach(this::unregister);
    }
}