package net.slipcor.pvparena.modules.arenaboards;

import static net.slipcor.pvparena.config.Debugger.trace;

class BoardRunnable implements Runnable {
    private final ArenaBoardManager abm;

    /**
     * create a timed arena runnable
     *
     * @param m the module
     */
    public BoardRunnable(final ArenaBoardManager m) {
        abm = m;
        trace("BoardRunnable constructor");
    }

    /**
     * the run method, commit arena end
     */
    @Override
    public void run() {
        trace("BoardRunnable commiting");
        if (abm == null) {
            if (ArenaBoardManager.globalBoard != null) {
                ArenaBoardManager.globalBoard.update();
            }
        } else {
            for (final ArenaBoard ab : abm.boards.values()) {
                ab.update();
            }
        }
    }
}
