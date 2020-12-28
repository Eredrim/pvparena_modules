package net.slipcor.pvparena.modules.aftermatch;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.runnables.ArenaRunnable;

import static net.slipcor.pvparena.config.Debugger.debug;

public class AfterRunnable extends ArenaRunnable {
    private final AfterMatch pum;

    public AfterRunnable(final AfterMatch pm, final int i) {
        super(MSG.MODULE_AFTERMATCH_STARTINGIN.getNode(), i, null, pm.getArena(), false);
        pum = pm;
        debug("AfterRunnable constructor");
    }

    @Override
    protected void commit() {
        debug("AfterRunnable commiting");
        if (!pum.getArena().isLocked()) {

            pum.afterMatch();
        }
    }

    @Override
    protected void warn() {
        PVPArena.getInstance().getLogger().warning("AfterRunnable not scheduled yet!");
    }
}
