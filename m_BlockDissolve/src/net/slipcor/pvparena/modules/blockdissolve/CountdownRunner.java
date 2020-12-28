package net.slipcor.pvparena.modules.blockdissolve;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.runnables.ArenaRunnable;

public class CountdownRunner extends ArenaRunnable {
    private final MoveChecker checker;

    public CountdownRunner(final Arena arena, final MoveChecker mc, final int seconds) {
        super(MSG.ARENA_STARTING_IN.getNode(), seconds, null, arena, false);
        this.checker = mc;
    }

    @Override
    protected void commit() {
        this.checker.active = true;
        this.checker.startTask();
    }

    @Override
    protected void warn() {
        PVPArena.getInstance().getLogger().warning("CountdownRunner not scheduled yet!");
    }

}
