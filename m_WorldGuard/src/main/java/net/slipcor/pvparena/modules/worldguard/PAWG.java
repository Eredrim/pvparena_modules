package net.slipcor.pvparena.modules.worldguard;

import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.commands.PAA_Region;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.loadables.ArenaRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PAWG extends ArenaModule {
    private static RegionContainer wgRegionContainer;

    public PAWG() {
        super("WorldGuard");
    }

    @Override
    public String version() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public boolean checkCommand(final String s) {
        return "wgload".equals(s)
                || "!wgl".equals(s) || "worldguard".equals(s);
    }

    @Override
    public List<String> getMain() {
        return Arrays.asList("wgload", "worldguard");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!wgl");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        if (arena == null) {
            return result;
        }
        for (final ArenaRegion region : arena.getRegions()) {
            result.define(new String[]{region.getRegionName()});
        }
        return result;
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("pvparena.admin")) {
            arena.msg(sender, Language.parse(MSG.ERROR_NOPERM, Language.parse(MSG.ERROR_NOPERM_X_ADMIN)));
            return;
        }

        // /pa wgsth [regionname] [wgregionname]

        if (args.length != 3) {
            helpCommands(arena, sender);
            return;
        }

        final ArenaRegion ars = arena.getRegion(args[1]);

        if (ars == null) {
            create((Player) sender, arena, args[1], args[2]);
        } else {
            update((Player) sender, arena, ars, args[2]);
        }
    }

    private void create(final Player p, final Arena arena, final String regionName, final String wgRegion) {

        final RegionManager rgManager = wgRegionContainer.get(new BukkitWorld(p.getWorld()));
        final ProtectedRegion region = (rgManager != null) ? rgManager.getRegion(wgRegion) : null;

        if (region == null) {
            arena.msg(p, Language.parse(MSG.MODULE_WORLDGUARD_NOTFOUND, wgRegion));
            return;
        }

        final Location loc1 = new Location(p.getWorld(),
                region.getMinimumPoint().getBlockX(),
                region.getMinimumPoint().getBlockY(),
                region.getMinimumPoint().getBlockZ());
        final Location loc2 = new Location(p.getWorld(),
                region.getMaximumPoint().getBlockX(),
                region.getMaximumPoint().getBlockY(),
                region.getMaximumPoint().getBlockZ());

        final ArenaPlayer ap = ArenaPlayer.parsePlayer(p.getName());
        ap.setSelection(loc1, false);
        ap.setSelection(loc2, true);

        final PAA_Region cmd = new PAA_Region();
        final String[] args = {regionName, "CUBOID"};

        cmd.commit(arena, p, args);
    }

    private void update(final Player p, final Arena arena, final ArenaRegion ars, final String wgRegion) {

        final RegionManager rgManager = wgRegionContainer.get(new BukkitWorld(p.getWorld()));
        final ProtectedRegion region = (rgManager != null) ? rgManager.getRegion(wgRegion) : null;

        if (region == null) {
            arena.msg(p, Language.parse(MSG.MODULE_WORLDGUARD_NOTFOUND, wgRegion));
            return;
        }

        final Location loc1 = new Location(p.getWorld(),
                region.getMinimumPoint().getBlockX(),
                region.getMinimumPoint().getBlockY(),
                region.getMinimumPoint().getBlockZ());
        final Location loc2 = new Location(p.getWorld(),
                region.getMaximumPoint().getBlockX(),
                region.getMaximumPoint().getBlockY(),
                region.getMaximumPoint().getBlockZ());

        ars.locs[0] = new PABlockLocation(loc1);
        ars.locs[1] = new PABlockLocation(loc2);
        ars.saveToConfig();
        arena.msg(p, Language.parse(MSG.MODULE_WORLDGUARD_SAVED, ars.getRegionName(), wgRegion));
    }

    private void helpCommands(final Arena arena, final CommandSender sender) {
        arena.msg(sender, Language.parse(MSG.ERROR_ERROR, "/pa wgload [regionname] [wgregionname]"));
    }

    @Override
    public void onThisLoad() {
        final Plugin pwep = Bukkit.getPluginManager().getPlugin("WorldGuard");
        if (pwep != null && pwep.isEnabled() && pwep instanceof WorldGuardPlugin) {
            wgRegionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
        }
    }
}
