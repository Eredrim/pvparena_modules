package net.slipcor.pvparena.modules.blockdissolve;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.loadables.ArenaModule;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;

import java.util.Arrays;
import java.util.stream.Collectors;

public class BlockDissolve extends ArenaModule {
    private MoveChecker checker;

    public BlockDissolve() {
        super("BlockDissolve");
    }

    @Override
    public String version() {
        return this.getClass().getPackage().getImplementationVersion();
    }

    @Override
    public void displayInfo(final CommandSender sender) {
        sender.sendMessage(String.format("Ticks: %d", this.arena.getConfig().getInt(CFG.MODULES_BLOCKDISSOLVE_TICKS)));
        String matList = Arrays.stream(this.arena.getConfig().getItems(CFG.MODULES_BLOCKDISSOLVE_MATERIALS))
                .map(itemStack -> itemStack.getType().name())
                .collect(Collectors.joining(", "));

        sender.sendMessage(String.format("Materials: %s", matList));
    }

    @Override
    public void parseStart() {
        if (this.checker == null) {
            this.checker = new MoveChecker(this.arena);
            Bukkit.getPluginManager().registerEvents(this.checker, PVPArena.getInstance());
        }
        this.checker.startCountdown();
    }

    @Override
    public boolean commitEnd(final ArenaTeam aTeam) {
        if (this.checker != null) {
            this.checker.clear();
            HandlerList.unregisterAll(this.checker);
            this.checker = null;
        }
        return false;
    }

    @Override
    public void reset(final boolean force) {
        if (this.checker != null) {
            this.checker.clear();
            HandlerList.unregisterAll(this.checker);
            this.checker = null;
        }
    }
}
