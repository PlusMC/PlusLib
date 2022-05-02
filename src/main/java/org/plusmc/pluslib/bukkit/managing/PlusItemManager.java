package org.plusmc.pluslib.bukkit.managing;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;
import org.plusmc.pluslib.bukkit.PlusLib;
import org.plusmc.pluslib.bukkit.managed.Loadable;
import org.plusmc.pluslib.bukkit.managed.PlusItem;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Registers and manages all PlusItems.
 */
@SuppressWarnings("unused")
public class PlusItemManager extends BaseManager {
    /**
     * The NamespacedKey of the PlusItem.
     */
    public static NamespacedKey PLUS_ITEM_KEY = new NamespacedKey(PlusLib.getInstance(), "custom_item");
    private static List<PlusItem> PLUS_ITEMS;


    protected PlusItemManager(JavaPlugin plugin) {
        super(plugin);
    }

    /**
     * Gets the list of all PlusItems.
     *
     * @return The list of all PlusItems.
     */
    public static List<PlusItem> getPlusItems() {
        return new ArrayList<>(PLUS_ITEMS);
    }

    /**
     * Checks if the item is a plus item.
     *
     * @param stack The item to check.
     * @param id    The ID of the plus item.
     * @return True if the item is a plus item.
     */
    public static boolean hasCustomItem(ItemStack stack, String id) {
        if (stack == null) return false;
        if (stack.getItemMeta() == null) return false;
        if (!stack.getItemMeta().getPersistentDataContainer().has(PLUS_ITEM_KEY, PersistentDataType.STRING))
            return false;
        return Objects.equals(stack.getItemMeta().getPersistentDataContainer().get(PLUS_ITEM_KEY, PersistentDataType.STRING), id);
    }

    /**
     * Gets a {@link PlusItem} from an ItemStack.
     *
     * @param stack The item to get;
     * @return The plus item if the item is a plus item.
     */
    @Nullable
    public static PlusItem getPlusItem(ItemStack stack) {
        if (stack == null) return null;
        if (stack.getItemMeta() == null) return null;
        if (!stack.getItemMeta().getPersistentDataContainer().has(PLUS_ITEM_KEY, PersistentDataType.STRING))
            return null;
        return getPlusItem(stack.getItemMeta().getPersistentDataContainer().get(PLUS_ITEM_KEY, PersistentDataType.STRING));
    }

    /**
     * Gets a {@link PlusItem} from an ID.
     *
     * @param id the id of the plus item.
     * @return The plus item if the ID is valid.
     */
    @Nullable
    public static PlusItem getPlusItem(String id) {
        for (PlusItem item : PLUS_ITEMS) {
            if (item.getID().equals(id))
                return item;
        }
        return null;
    }


    /**
     * Registers a {@link PlusItem}.
     *
     * @param item The item to register.
     */
    @Override
    protected void register(Loadable item) {
        if (!(item instanceof PlusItem pItem)) return;
        PLUS_ITEMS.add(pItem);
        PlusLib.logger().info("Registered PlusItem: " + pItem.getID());
    }

    /**
     * Unregisters a {@link PlusItem}.
     * Called automatically when the library plugin is disabled.
     *
     * @param item The item to unregister.
     */
    @Override
    protected void unregister(Loadable item) {
        if (!(item instanceof PlusItem pItem)) return;
        PLUS_ITEMS.remove(pItem);
        item.unload();
        PlusLib.logger().info("Unregistered PlusItem: " + pItem.getID());
    }


    @Override
    protected void init() {
        Bukkit.getPluginManager().registerEvents(new Listener(), this.getPlugin());
        PLUS_ITEMS = new ArrayList<>();
    }

    @Override
    protected void shutdown() {
        for (Iterator<PlusItem> iterator = PLUS_ITEMS.iterator(); iterator.hasNext(); ) {
            PlusItem item = iterator.next();
            iterator.remove();
            unregister(item);
        }
        PLUS_ITEMS.clear();
    }

    @Override
    public Class<? extends Loadable> getManaged() {
        return PlusItem.class;
    }

    private static class Listener implements org.bukkit.event.Listener {

        @EventHandler
        public void onBlockInteract(PlayerInteractEvent e) {
            ItemStack stack = e.getItem();
            if (stack == null) return;
            ItemMeta meta = stack.getItemMeta();
            if (meta == null) return;
            try {
                String id = meta.getPersistentDataContainer().get(PlusItemManager.PLUS_ITEM_KEY, PersistentDataType.STRING);
                PlusItem item = PlusItemManager.getPlusItem(id);
                if (item == null) return;
                item.onInteractBlock(e);
            } catch (Exception ex) {
                //ignore
            }

            Block block = e.getClickedBlock();
            if (block == null) return;
            if (!(block.getState() instanceof TileState state)) return;
            try {
                String id = state.getPersistentDataContainer().get(PlusItemManager.PLUS_ITEM_KEY, PersistentDataType.STRING);
                PlusItem item = PlusItemManager.getPlusItem(id);
                if (item == null) return;
                item.onInteractAsBlock(e);
            } catch (Exception ex) {
                //ignore
            }
        }

        @EventHandler
        public void onEntityInteract(PlayerInteractEntityEvent e) {
            ItemStack stack = e.getPlayer().getInventory().getItemInMainHand();
            ItemMeta meta = stack.getItemMeta();
            if (meta == null) return;
            try {
                String id = meta.getPersistentDataContainer().get(PlusItemManager.PLUS_ITEM_KEY, PersistentDataType.STRING);
                PlusItem item = PlusItemManager.getPlusItem(id);
                if (item == null) return;
                item.onInteractEntity(e);
            } catch (Exception ex) {
                //ignore
            }

        }

        @EventHandler
        public void onPlayerHit(EntityDamageByEntityEvent e) {
            if (!(e.getDamager() instanceof Player p)) return;
            ItemStack stack = p.getInventory().getItemInMainHand();
            ItemMeta meta = stack.getItemMeta();
            if (meta == null) return;
            try {
                String id = meta.getPersistentDataContainer().get(PlusItemManager.PLUS_ITEM_KEY, PersistentDataType.STRING);
                PlusItem item = PlusItemManager.getPlusItem(id);
                if (item == null) return;
                item.onDamageEntity(e);
            } catch (Exception ex) {
                //ignore
            }
        }

        @EventHandler
        public void onBlockPlace(BlockPlaceEvent e) {
            ItemStack stack = e.getItemInHand();
            ItemMeta meta = stack.getItemMeta();
            if (meta == null) return;
            try {
                String id = meta.getPersistentDataContainer().get(PlusItemManager.PLUS_ITEM_KEY, PersistentDataType.STRING);
                PlusItem item = PlusItemManager.getPlusItem(id);
                if (item == null) return;
                item.onBlockPlace(e);
            } catch (Exception ex) {
                //ignore
            }
        }
    }

}
