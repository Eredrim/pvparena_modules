package net.slipcor.pvparena.modules.arenaboards;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.managers.StatisticsManager;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.Map;

import static net.slipcor.pvparena.config.Debugger.debug;
import static net.slipcor.pvparena.config.Debugger.trace;

public class ArenaBoard {
    private final PABlockLocation location;
    private final ArenaBoardManager abm;
    private final boolean global;

    private StatisticsManager.Type sortBy = StatisticsManager.Type.KILLS;

    private final Map<StatisticsManager.Type, ArenaBoardColumn> columns = new HashMap<>();

    /**
     * create an arena board instance
     *
     * @param loc the location to hook to
     * @param a   the arena to save the board to
     */
    public ArenaBoard(final ArenaBoardManager m, final PABlockLocation loc, final Arena a) {
        abm = m;
        location = loc;
        global = a == null;

        debug("constructing arena board");
        construct();
    }

    /**
     * actually construct the arena board, read colums, save signs etc
     */
    private void construct() {
        PABlockLocation l = location;
        try {
            Sign s = (Sign) l.toLocation().getBlock().getState();
            final BlockFace bf = getRightDirection(s);
            debug("parsing signs: ");
            int border = 10;
            do {
                StatisticsManager.Type t = null;
                try {
                    t = StatisticsManager.getTypeBySignLine(s.getLine(0));
                    s.setLine(0, ChatColor.AQUA + t.getNiceName());
                } catch (final Exception e) {
                    // nothing
                }

                columns.put(t, new ArenaBoardColumn(l));
                debug("putting column type {}", this);
                l = new PABlockLocation(l.toLocation().getBlock().getRelative(bf).getLocation());
                s = (Sign) l.toLocation().getBlock().getState();
            } while (border-- > 0);
        } catch (final Exception e) {
            // no more signs, out!
        }
    }

    public PABlockLocation getLocation() {
        return location;
    }

    /**
     * get the right next board direction from the attachment data
     *
     * @param s the sign to check
     * @return the blockface of the direction of the next column
     */
    private BlockFace getRightDirection(final Sign s) {

        final Block block = s.getBlock();
        final org.bukkit.material.Sign sign = (org.bukkit.material.Sign) block.getState().getData();

        if (sign.getFacing() == BlockFace.EAST) {
            return BlockFace.NORTH;
        }
        if (sign.getFacing() == BlockFace.WEST) {
            return BlockFace.SOUTH;
        }
        if (sign.getFacing() == BlockFace.NORTH) {
            return BlockFace.WEST;
        }
        if (sign.getFacing() == BlockFace.SOUTH) {
            return BlockFace.EAST;
        }

        return null;
    }

    /**
     * save arena board statistics to each column
     */
    public void update() {
        trace("ArenaBoard update()");
        for (final StatisticsManager.Type t : StatisticsManager.Type.values()) {
            trace("checking stat: {}", t.name());
            if (!this.columns.containsKey(t)) {
                continue;
            }
            trace("found! reading!");
            final String[] s =
                    StatisticsManager.getStatsValuesForBoard(this.global ? null : this.abm.getArena(), this.sortBy);
            this.columns.get(t).write(s);
        }
    }

    /**
     * check if a player clicked a leaderboard sign
     *
     * @param event the InteractEvent
     * @return true if the player clicked a leaderboard sign, false otherwise
     */
    public static boolean checkInteract(final ArenaBoardManager abm, final PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        debug("checking ArenaBoard interact", player);

        if (event.getClickedBlock() == null) {
            return false;
        }

        if (event.getHand().equals(EquipmentSlot.OFF_HAND)) {
            debug("exiting: offhand", player);
            return false;
        }

        debug("block is not null", player);

        if (!abm.boards.containsKey(new PABlockLocation(event.getClickedBlock().getLocation()))
                && ArenaBoardManager.globalBoard == null
                || !ArenaBoardManager.globalBoard.location.equals(
                new PABlockLocation(event.getClickedBlock().getLocation()))) {
            return false;
        }

        debug("arenaboard exists", player);

        ArenaBoard ab = abm.boards.get(new PABlockLocation(event.getClickedBlock().getLocation()));

        if (ab == null) {
            ab = ArenaBoardManager.globalBoard;
        }

        if (ab.global) {
            debug("global!", player);
            if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                ab.sortBy = StatisticsManager.Type.next(ab.sortBy);
                Arena.pmsg(player,
                        Language.parse(MSG.MODULE_ARENABOARDS_SORTINGBY, ab.sortBy.toString()));
                return true;
            } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                ab.sortBy = StatisticsManager.Type.last(ab.sortBy);
                Arena.pmsg(player,
                        Language.parse(MSG.MODULE_ARENABOARDS_SORTINGBY, ab.sortBy.toString()));
                return true;
            }
        } else {
            debug("not global!", player);
            if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                ab.sortBy = StatisticsManager.Type.next(ab.sortBy);
                ab.abm.getArena().msg(player,
                        Language.parse(MSG.MODULE_ARENABOARDS_SORTINGBY, ab.sortBy.toString()));
                return true;
            } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                ab.sortBy = StatisticsManager.Type.last(ab.sortBy);
                ab.abm.getArena().msg(player,
                        Language.parse(MSG.MODULE_ARENABOARDS_SORTINGBY, ab.sortBy.toString()));
                return true;
            }
        }

        return false;
    }

    public void destroy() {
        // TODO clear signs
        if (global) {
            PVPArena.getInstance().getConfig().set("leaderboard", null);
            PVPArena.getInstance().saveConfig();
        } else {
            abm.getArena().getArenaConfig().setManually("spawns.leaderboard", null);
            abm.getArena().getArenaConfig().save();
        }
    }
}
