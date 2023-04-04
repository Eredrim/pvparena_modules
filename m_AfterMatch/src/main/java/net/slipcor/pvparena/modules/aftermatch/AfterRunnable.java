package net.slipcor.pvparena.modules.aftermatch;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.runnables.ArenaRunnable;

import static net.slipcor.pvparena.config.Debugger.debug;

public class AfterRunnable extends ArenaRunnable {
    private final AfterMatch module;

    public AfterRunnable(final AfterMatch afterMatch, final int i) {
        super(MSG.MODULE_AFTERMATCH_STARTINGIN.getNode(), i, null, afterMatch.getArena(), false);
        this.module = afterMatch;
        debug("AfterRunnable constructor");
    }

    @Override
    protected void commit() {
        debug("AfterRunnable commiting");
        if (!this.module.getArena().isLocked()) {
            this.module.afterMatch();
        }
    }

    @Override
    protected void warn() {
        PVPArena.getInstance().getLogger().warning("AfterRunnable not scheduled yet!");
    }
}
