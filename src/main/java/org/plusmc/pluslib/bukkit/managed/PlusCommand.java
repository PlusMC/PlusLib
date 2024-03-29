package org.plusmc.pluslib.bukkit.managed;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.plusmc.pluslib.bukkit.managing.PlusCommandManager;

import java.util.ArrayList;
import java.util.List;

/**
 * A command that can be registered to a {@link JavaPlugin} using {@link PlusCommandManager#register(PlusCommand)}.
 * Useful for making complicated commands easier to write.
 */
@SuppressWarnings("unused")
public interface PlusCommand extends CommandExecutor, TabCompleter, Loadable {
    /**
     * Gets the permission required to use this command.
     *
     * @return Permission required to use this command
     */
    default String getPermission() {
        return null;
    }

    /**
     * Gets the usage of this command.
     *
     * @return Usage of this command
     */
    default String getUsage() {
        return "/" + getName();
    }

    /**
     * Gets the command name.
     *
     * @return Command name
     */
    String getName();

    /**
     * Gets the description of this command.
     *
     * @return Description of this command
     */
    default String getDescription() {
        return "";
    }

    @Override
    default List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 0)
            return new ArrayList<>();
        return filterCompletions(args[args.length - 1], args.length);
    }

    /**
     * Filters the completions for the given argument.
     *
     * @param arg   Current argument
     * @param index Current index
     * @return List of filtered completions
     */
    @NotNull
    default List<String> filterCompletions(String arg, int index) {
        String arg2 = arg.toLowerCase();
        List<String> completions = getCompletions(index) == null ? new ArrayList<>() : getCompletions(index);
        List<String> filtered = new ArrayList<>();
        completions.forEach(s -> {
            if (s.toLowerCase().startsWith(arg2))
                filtered.add(s);
        });
        return filtered;
    }

    /**
     * Gets the completions of the command.
     *
     * @param index Index of the current argument
     * @return Completions of the command
     */
    default List<String> getCompletions(int index) {
        return new ArrayList<>();
    }
}
