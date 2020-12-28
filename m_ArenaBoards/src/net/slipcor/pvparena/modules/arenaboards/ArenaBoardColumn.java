package net.slipcor.pvparena.modules.arenaboards;

import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.config.Debugger;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;

import static net.slipcor.pvparena.config.Debugger.*;

public class ArenaBoardColumn {
    private final PABlockLocation location;

    private final ArenaBoardSign[] signs = new ArenaBoardSign[5];

    /**
     * create an arena board column instance
     *
     * @param l  the location of the column header
     */
    public ArenaBoardColumn(final PABlockLocation l) {
        location = l;

        debug("fetching sign column");
        fetchSigns();
    }

    /**
     * fetch sub signs and attach them to the sign array
     */
    private void fetchSigns() {
        Location l = location.toLocation().getBlock().getRelative(BlockFace.DOWN)
                .getLocation();
        try {
            int i = 0;
            do {
                final Sign s = (Sign) l.getBlock().getState();
                s.setLine(0, "");
                s.setLine(1, "");
                s.setLine(2, "");
                s.setLine(3, "");
                s.update();
                signs[i] = (new ArenaBoardSign(l));
                l = l.getBlock().getRelative(BlockFace.DOWN).getLocation();
            } while (++i < 5);
        } catch (final Exception e) {
            // no more signs, out!
        }
    }

    /**
     * write a string array to the signs
     *
     * @param s the string array to save
     */
    public void write(final String[] s) {
        debug("writing to column at location {}", location);
        int i = 0;
        for (final ArenaBoardSign abs : signs) {
            if (abs == null) {
                return;
            }
            int ii = 0;
            while (i < s.length && ii < 4) {
                abs.set(ii++, s[i++]);
            }
            abs.update();
        }
    }
}
