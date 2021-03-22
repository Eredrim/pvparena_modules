package net.slipcor.pvparena.modules.blockdissolve;

import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.loadables.ArenaModule;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

public class BlockDissolve extends ArenaModule {

    private boolean setup;
    private MoveChecker checker;
    private ItemStack[] dissolveItems;
    private int dissolveTicks;

    public BlockDissolve() {
        super("BlockDissolve");
    }

    @Override
    public String version() {
        return this.getClass().getPackage().getImplementationVersion();
    }

    @Override
    public void configParse(final YamlConfiguration config) {
        if (this.setup) {
            return;
        }
        if (this.checker == null) {
            this.dissolveItems = this.arena.getConfig().getItems(CFG.MODULES_BLOCKDISSOLVE_MATERIALS);
            this.dissolveTicks = this.arena.getConfig().getInt(CFG.MODULES_BLOCKDISSOLVE_TICKS);
            this.checker = new MoveChecker(this.arena, this.dissolveItems, this.dissolveTicks);
        }
        this.setup = true;
    }

    @Override
    public void displayInfo(final CommandSender sender) {
        sender.sendMessage("ticks: " + this.dissolveTicks);
        StringBuilder materials = new StringBuilder("materials: ");
        ItemStack[] items = this.dissolveItems;
        for(ItemStack item : items) {
            materials.append(item.getType().name());
        }
        sender.sendMessage(materials.toString());
    }

    @Override
    public void parseStart() {
        if (this.checker == null) {
            this.checker = new MoveChecker(this.arena, this.dissolveItems, this.dissolveTicks);
        }
        this.checker.start();
    }

    @Override
    public boolean commitEnd(final ArenaTeam aTeam) {
        if (this.checker != null) {
            this.checker.clear();
        }
        return false;
    }

    @Override
    public void reset(final boolean force) {
        if (this.checker != null) {
            this.checker.clear();
        }
    }
}
