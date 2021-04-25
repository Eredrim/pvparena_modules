package net.slipcor.pvparena.modules.arenaboards;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.ArenaManager;
import net.slipcor.pvparena.managers.SpawnManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;

import static net.slipcor.pvparena.config.Debugger.debug;

public class ArenaBoardManager extends ArenaModule implements Listener {
    final Map<PABlockLocation, ArenaBoard> boards = new HashMap<>();
    private int BOARD_ID = -1;
    private int GLOBAL_ID = -1;
    static ArenaBoard globalBoard;

    public ArenaBoardManager() {
        super("ArenaBoards");
    }

    @Override
    public String version() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public void configParse(final YamlConfiguration config) {
        if (config.get("spawns") != null) {
            debug("checking for leaderboard");
            if (config.get("spawns.leaderboard") != null) {
                debug("leaderboard exists");
                final PABlockLocation loc = Config.parseBlockLocation(config.getString("spawns.leaderboard"));


                boards.put(loc, new ArenaBoard(this, loc, arena));
            }
        }
        final String leaderboard = PVPArena.getInstance().getConfig().getString(
                "leaderboard");
        if (leaderboard != null && GLOBAL_ID < 0 && globalBoard == null) {
            final PABlockLocation lbLoc = Config.parseBlockLocation(leaderboard);
            globalBoard = new ArenaBoard(this, lbLoc, null);

            GLOBAL_ID = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                    PVPArena.getInstance(), new BoardRunnable(null), 100L, 100L);
        }

        BOARD_ID = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                PVPArena.getInstance(), new BoardRunnable(this), 100L, 100L);

        Bukkit.getServer().getPluginManager().registerEvents(this, PVPArena.getInstance());
    }

    @Override
    public void onBlockBreak(final Block block) {
        if (globalBoard != null && globalBoard.getLocation().toLocation().equals(block.getLocation())) {
            globalBoard.destroy();
            globalBoard = null;
            Bukkit.getScheduler().cancelTask(GLOBAL_ID);
            GLOBAL_ID = -1;
        } else if (boards.containsKey(new PABlockLocation(block.getLocation()))) {
            boards.get(new PABlockLocation(block.getLocation())).destroy();
            boards.remove(new PABlockLocation(block.getLocation()));
        } else {
            return;
        }

        final String msg = Language.parse(MSG.MODULE_ARENABOARDS_DESTROYED);
        for (final Entity e : arena.getWorld().getEntities()) {
            if (e instanceof Player) {
                final Player player = (Player) e;
                if (player.getLocation().distance(block.getLocation()) > 5) {
                    continue;
                }
                arena.msg(player, msg);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSignChange(final SignChangeEvent event) {

        String headline = event.getLine(0);
        if (headline == null || headline.isEmpty()) {
            return;
        }

        if (!headline.startsWith("[PAA]")) {
            return;
        }

        headline = headline.replace("[PAA]", "");

        final Arena a = ArenaManager.getArenaByName(headline);

        // trying to create an arena leaderboard

        if (arena == a && arena != null && boards.containsKey(new PABlockLocation(event.getBlock().getLocation()))) {
            Arena.pmsg(event.getPlayer(), MSG.MODULE_ARENABOARDS_EXISTS);
            return;
        }

        if (!PVPArena.hasAdminPerms(event.getPlayer())
                && a != null && !PVPArena.hasCreatePerms(event.getPlayer(),
                a)) {
            a.msg(event.getPlayer(), MSG.ERROR_NOPERM, Language.parse(MSG.MODULE_ARENABOARDS_CREATE));
            return;
        }

        event.setLine(0, headline);
        if (a == null) {
            debug(event.getPlayer(), "creating global leaderboard");
            globalBoard = new ArenaBoard(this, new PABlockLocation(event.getBlock().getLocation()), null);
            final Location loc = event.getBlock().getLocation();
            final int x = loc.getBlockX();
            final int y = loc.getBlockY();
            final int z = loc.getBlockZ();
            final Float yaw = loc.getYaw();
            final Float pitch = loc.getPitch();

            final String s = loc.getWorld().getName() + ',' + x + ','
                    + y + ',' + z;
            PVPArena.getInstance().getConfig().set("leaderboard", s);
            PVPArena.getInstance().saveConfig();

            GLOBAL_ID = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                    PVPArena.getInstance(), new BoardRunnable(null), 100L, 100L);
        } else {
            final PABlockLocation loc = new PABlockLocation(event.getBlock().getLocation());
            boards.put(loc, new ArenaBoard(this, loc, a));
            SpawnManager.setBlock(a, new PABlockLocation(event.getBlock().getLocation()), "leaderboard", null);
        }
    }

    @Override
    public boolean onPlayerInteract(final PlayerInteractEvent event) {
        return ArenaBoard.checkInteract(this, event);
    }

    @Override
    public void reset(final boolean force) {
        if (BOARD_ID > -1) {
            Bukkit.getScheduler().cancelTask(BOARD_ID);
        }
        BOARD_ID = -1;
    }

    @Override
    public void parseStart() {
        if (BOARD_ID == -1) {
            this.BOARD_ID = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                    PVPArena.getInstance(), new BoardRunnable(this), 100L, 100L);
        }
    }
}
