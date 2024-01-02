package net.slipcor.pvparena.modules.startfreeze;

import net.slipcor.pvparena.commands.AbstractArenaCommand;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.PermissionManager;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class StartFreeze extends ArenaModule {
    private StartFreezer runnable;

    public StartFreeze() {
        super("StartFreeze");
    }

    @Override
    public String version() {
        return this.getClass().getPackage().getImplementationVersion();
    }

    @Override
    public boolean checkCommand(final String s) {
        return "startfreeze".equalsIgnoreCase(s) || "!sf".equalsIgnoreCase(s);
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("startfreeze");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!sf");
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        // !sf 5

        if (!PermissionManager.hasAdminPerm(sender) && !PermissionManager.hasBuilderPerm(sender, this.arena)) {
            this.arena.msg(sender, MSG.ERROR_NOPERM, Language.parse(MSG.ERROR_NOPERM_X_ADMIN));
            return;
        }

        if (!AbstractArenaCommand.argCountValid(sender, this.arena, args, new Integer[]{2})) {
            return;
        }

        if ("!sf".equalsIgnoreCase(args[0]) || "startfreeze".equalsIgnoreCase(args[0])) {
            int i;
            try {
                i = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                this.arena.msg(sender, MSG.ERROR_NOT_NUMERIC, args[1]);
                return;
            }

            this.arena.getConfig().set(CFG.MODULES_STARTFREEZE_TIMER, i);
            this.arena.getConfig().save();
            this.arena.msg(sender, MSG.SET_DONE, CFG.MODULES_STARTFREEZE_TIMER.getNode(), String.valueOf(i));
        }
    }

    @Override
    public void displayInfo(final CommandSender sender) {
        sender.sendMessage(String.format("seconds: %d", this.arena.getConfig().getInt(CFG.MODULES_STARTFREEZE_TIMER)));
    }

    @Override
    public void reset(final boolean force) {
        if (this.runnable != null) {
            this.runnable.cancel();
        }
        this.runnable = null;
    }

    @Override
    public void parseStart() {
        this.runnable = new StartFreezer(this.arena);
    }
}
