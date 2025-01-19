package net.slipcor.pvparena.modules.walls;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.runnables.ArenaRunnable;

public class WallsRunner extends ArenaRunnable {

    private final Walls module;

    public WallsRunner(final Walls module, final Arena arena, final int seconds) {
        super(MSG.MODULE_WALLS_TIMER.getNode(), seconds, null, arena, false);
        this.module = module;
    }

    @Override
    protected void commit() {

        if (this.module != null) {
            this.module.removeWalls();
            this.module.runnable = null;
        }
    }

    @Override
    protected void spam() {
        super.spam();
        if (this.arena.getConfig().getBoolean(Config.CFG.MODULES_WALLS_SCOREBOARDCOUNTDOWN)) {
            int mins = this.seconds / 60;
            int seconds = this.seconds % 60;
            String value = mins + ":" + String.format("%02d", seconds);

            this.arena.getScoreboard().addCustomEntry(this.module, Language.parse(MSG.MODULE_WALLS_FALLINGIN, value), 99);
            this.arena.getScoreboard().addCustomEntry(this.module, Language.parse(MSG.MODULE_WALLS_SEPARATOR), 98);
        }
    }

    @Override
    protected void warn() {
        PVPArena.getInstance().getLogger().warning("WallsRunner not scheduled yet!");
    }
}
