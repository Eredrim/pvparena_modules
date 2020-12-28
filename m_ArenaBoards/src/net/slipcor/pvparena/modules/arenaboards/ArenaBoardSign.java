package net.slipcor.pvparena.modules.arenaboards;

import net.slipcor.pvparena.config.Debugger;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;

import static net.slipcor.pvparena.config.Debugger.debug;

class ArenaBoardSign {
    private final BlockState state;

    /**
     * create an arena board sign instance
     *
     * @param loc the location where the sign resides
     */
    public ArenaBoardSign(final Location loc) {
        state = loc.getBlock().getState();
        debug("adding sign at location {}",  loc);
    }

    /**
     * set a line
     *
     * @param i      the line to set
     * @param string the string to set
     */
    public void set(final int i, final String string) {
        ((Sign) state).setLine(i, string);
    }

    /**
     * update the sign
     */
    public void update() {
        state.update();
    }
}
