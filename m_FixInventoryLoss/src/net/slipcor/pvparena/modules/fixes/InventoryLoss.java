package net.slipcor.pvparena.modules.fixes;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.classes.PACheck;
import net.slipcor.pvparena.commands.AbstractArenaCommand;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.PermissionManager;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

public class InventoryLoss extends ArenaModule {

    public InventoryLoss() {
        super("FixInventoryLoss");
    }

    @Override
    public String version() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public boolean checkCommand(final String s) {
        return "!fil".equals(s) || "fixinventoryloss".equals(s);
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("fixinventoryloss");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!fil");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        result.define(new String[]{"gamemode"});
        result.define(new String[]{"inventory"});
        return result;
    }

    @Override
    public PACheck checkJoin(final CommandSender sender,
                             final PACheck res, final boolean join) {
        final Player player = (Player) sender;
        final int priority = 5;

        if (res.hasError() || res.getPriority() > priority) {
            return res;
        }

        if (arena.getArenaConfig().getBoolean(CFG.MODULES_FIXINVENTORYLOSS_GAMEMODE)) {
            if (player.getGameMode() != GameMode.SURVIVAL) {
                res.setError(this, Language.parse(MSG.MODULE_FIXINVENTORYLOSS_GAMEMODE));
                return res;
            }
        }
        if (arena.getArenaConfig().getBoolean(CFG.MODULES_FIXINVENTORYLOSS_INVENTORY)) {
            for (final ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    res.setError(this, Language.parse(MSG.MODULE_FIXINVENTORYLOSS_INVENTORY));
                    return res;
                }
            }
        }
        return res;
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        // !fil [value]

        if (!PermissionManager.hasAdminPerm(sender)
                && !PermissionManager.hasBuilderPerm(sender, arena)) {
            arena.msg(
                    sender,
                    Language.parse(MSG.ERROR_NOPERM,
                            Language.parse(MSG.ERROR_NOPERM_X_ADMIN)));
            return;
        }

        if (!AbstractArenaCommand.argCountValid(sender, arena, args, new Integer[]{2})) {
            return;
        }

        CFG c = null;

        if ("gamemode".equals(args[1])) {
            c = CFG.MODULES_FIXINVENTORYLOSS_GAMEMODE;
        } else if ("inventory".equals(args[1])) {
            c = CFG.MODULES_FIXINVENTORYLOSS_INVENTORY;
        }

        if (c == null) {
            arena.msg(sender, Language.parse(MSG.ERROR_ARGUMENT, args[1], "gamemode | inventory"));
            return;
        }

        final boolean b = arena.getArenaConfig().getBoolean(c);
        arena.getArenaConfig().set(c, !b);
        arena.getArenaConfig().save();
        arena.msg(sender, Language.parse(MSG.SET_DONE, c.getNode(), String.valueOf(!b)));

    }

    @Override
    public void displayInfo(final CommandSender player) {
        player.sendMessage(StringParser.colorVar("gamemode", arena.getArenaConfig().getBoolean(CFG.MODULES_FIXINVENTORYLOSS_GAMEMODE))
                + " || "
                + StringParser.colorVar("inventory", arena.getArenaConfig().getBoolean(CFG.MODULES_FIXINVENTORYLOSS_INVENTORY)));
    }
}
