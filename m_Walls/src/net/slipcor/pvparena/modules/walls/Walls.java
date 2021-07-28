package net.slipcor.pvparena.modules.walls;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.commands.AbstractArenaCommand;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.loadables.ArenaRegion;
import net.slipcor.pvparena.managers.PermissionManager;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;

public class Walls extends ArenaModule {
    WallsRunner runnable;
    private boolean needsReset = false;


    public Walls() {
        super("Walls");
    }

    @Override
    public String version() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public boolean checkCommand(final String s) {
        return "wallseconds".equals(s) || "wallmaterial".equals(s) || "!ww".equals(s) || "!wm".equals(s);
    }

    @Override
    public List<String> getMain() {
        return Arrays.asList("wallseconds", "wallmaterial");
    }

    @Override
    public List<String> getShort() {
        return Arrays.asList("!ww", "!wm");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        result.define(new String[]{"{Material}"});
        return result;
    }

    private void createWalls() {
        Material mat;
        try {
            mat = Material.getMaterial(arena.getArenaConfig().getString(CFG.MODULES_WALLS_MATERIAL));
        } catch (final Exception e) {
            mat = Material.SAND;
        }
        debug.i("material: "+mat.name());
        debug.i("replacing the wall for the following regions:");

        for (final ArenaRegion region : arena.getRegions()) {
            if (region.getRegionName().toLowerCase().contains("wall")) {
                debug.i(region.getRegionName());
                final World world = region.getWorld();
                final int x1 = region.getShape().getMinimumLocation().getX();
                final int y1 = region.getShape().getMinimumLocation().getY();
                final int z1 = region.getShape().getMinimumLocation().getZ();

                final int x2 = region.getShape().getMaximumLocation().getX();
                final int y2 = region.getShape().getMaximumLocation().getY();
                final int z2 = region.getShape().getMaximumLocation().getZ();

                for (int a = x1; a <= x2; a++) {
                    for (int b = y1; b <= y2; b++) {
                        for (int c = z1; c <= z2; c++) {
                            Block block = world.getBlockAt(a, b, c);
                            if (block.getType() == Material.AIR) {
                                block.setType(mat);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        // !sf 5

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

        if ("!ww".equals(args[0]) || "wallseconds".equals(args[0])) {
            // setting walls seconds
            final int i;
            try {
                i = Integer.parseInt(args[1]);
            } catch (final Exception e) {
                arena.msg(sender,
                        Language.parse(MSG.ERROR_NOT_NUMERIC, args[1]));
                return;
            }

            arena.getArenaConfig().set(CFG.MODULES_WALLS_SECONDS, i);
            arena.getArenaConfig().save();
            arena.msg(sender, Language.parse(MSG.SET_DONE, CFG.MODULES_WALLS_SECONDS.getNode(), String.valueOf(i)));
        } else {
            // setting walls material
            final Material mat;
            try {
                mat = Material.getMaterial(args[1].toUpperCase());
                debug.i("wall material: " + mat.name());
            } catch (final Exception e) {
                arena.msg(sender, Language.parse(MSG.ERROR_MAT_NOT_FOUND, args[1]));
                return;
            }

            arena.getArenaConfig().set(CFG.MODULES_WALLS_MATERIAL, mat.name());
            arena.getArenaConfig().save();
            arena.msg(sender, Language.parse(MSG.SET_DONE, CFG.MODULES_WALLS_MATERIAL.getNode(), mat.name()));
        }
    }

    @Override
    public void displayInfo(final CommandSender sender) {
        sender.sendMessage("seconds: " + arena.getArenaConfig().getInt(CFG.MODULES_WALLS_SECONDS) +
                "material: " + arena.getArenaConfig().getString(CFG.MODULES_WALLS_MATERIAL));
    }

    @Override
    public void parseStart() {
        runnable = new WallsRunner(this, arena, arena.getArenaConfig().getInt(CFG.MODULES_WALLS_SECONDS));
        createWalls();
    }

    @Override
    public void reset(final boolean force) {
        debug.i("resetting WALLS");
        if (runnable != null) {
            runnable.cancel();
            if (arena.getArenaConfig().getBoolean(Config.CFG.MODULES_WALLS_SCOREBOARDCOUNTDOWN)) {
                arena.removeCustomScoreBoardEntry(this, 99);
                arena.removeCustomScoreBoardEntry(this, 98);
            }
        }
        if (!needsReset) {
            debug.i("[WorldEdit] we did not start yet, no reset needed!");
            return;
        }
        needsReset = false;
        runnable = null;
        createWalls();
    }

    public void removeWalls() {
        Material mat;
        try {
            mat = Material.getMaterial(arena.getArenaConfig().getString(CFG.MODULES_WALLS_MATERIAL));
        } catch (final Exception e) {
            mat = Material.SAND;
        }
        for (final ArenaRegion region : arena.getRegions()) {

            if (region.getRegionName().toLowerCase().contains("wall")) {
                final World world = region.getWorld();
                final int x1 = region.getShape().getMinimumLocation().getX();
                final int y1 = region.getShape().getMinimumLocation().getY();
                final int z1 = region.getShape().getMinimumLocation().getZ();

                final int x2 = region.getShape().getMaximumLocation().getX();
                final int y2 = region.getShape().getMaximumLocation().getY();
                final int z2 = region.getShape().getMaximumLocation().getZ();

                for (int a = x1; a <= x2; a++) {
                    for (int b = y1; b <= y2; b++) {
                        for (int c = z1; c <= z2; c++) {
                            Block block = world.getBlockAt(a, b, c);
                            if (block.getType() == mat) {
                                block.setType(Material.AIR);
                            }
                        }
                    }
                }
                needsReset = true;
            }
        }
        if (arena.getArenaConfig().getBoolean(Config.CFG.MODULES_WALLS_SCOREBOARDCOUNTDOWN)) {
            arena.removeCustomScoreBoardEntry(this, 99);
            arena.removeCustomScoreBoardEntry(this, 98);
        }
    }
}
