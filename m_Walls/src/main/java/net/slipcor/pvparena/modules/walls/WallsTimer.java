package net.slipcor.pvparena.modules.walls;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.runnables.ArenaRunnable;

import static net.slipcor.pvparena.config.Debugger.debug;

public class WallsTimer extends ArenaRunnable {

    private final Walls module;

    public WallsTimer(final Walls module, final Arena arena, final int seconds) {
        super(MSG.MODULE_WALLS_TIMER.getNode(), seconds, null, arena, false);
        this.module = module;
        debug(this.module.getArena(), this.module, "WallTimer START");
    }

    @Override
    protected void commit() {
        debug(this.module.getArena(), this.module, "WallTimer END");
        this.module.removeWallsAsync();
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
