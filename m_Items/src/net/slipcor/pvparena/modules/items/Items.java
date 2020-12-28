package net.slipcor.pvparena.modules.items;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.loadables.ArenaModule;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;

public class Items extends ArenaModule {
    private int id = -1;

    public Items() {
        super("Items");
    }

    @Override
    public String version() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public void displayInfo(final CommandSender sender) {
        sender.sendMessage("interval: " + arena.getArenaConfig().getInt(CFG.MODULES_ITEMS_INTERVAL));
        ItemStack[] items = arena.getArenaConfig().getItems(CFG.MODULES_ITEMS_ITEMS);
        StringBuilder itemsString = new StringBuilder("items : ");
        for(ItemStack item : items) {
            itemsString.append(item.getType().name());
        }
        sender.sendMessage(itemsString.toString());
    }

    @Override
    public boolean hasSpawn(final String s) {
        return s.toLowerCase().startsWith("item");
    }

    @Override
    public void reset(final boolean force) {
        Bukkit.getScheduler().cancelTask(id);
        id = -1;
    }

    @Override
    public void parseStart() {
        if (arena.getArenaConfig().getInt(CFG.MODULES_ITEMS_INTERVAL) > 0) {
            Bukkit.getScheduler().cancelTask(id);
            id = Bukkit.getScheduler()
                    .scheduleSyncRepeatingTask(
                            PVPArena.getInstance(),
                            new ItemSpawnRunnable(arena),
                            arena.getArenaConfig().getInt(
                                    CFG.MODULES_ITEMS_INTERVAL) * 20L,
                            arena.getArenaConfig().getInt(
                                    CFG.MODULES_ITEMS_INTERVAL) * 20L);
        }
    }
}
