package net.slipcor.pvparena.modules.eventactions;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.events.PADeathEvent;
import net.slipcor.pvparena.events.PAEndEvent;
import net.slipcor.pvparena.events.PAExitEvent;
import net.slipcor.pvparena.events.PAJoinEvent;
import net.slipcor.pvparena.events.PAKillEvent;
import net.slipcor.pvparena.events.PALeaveEvent;
import net.slipcor.pvparena.events.PALoseEvent;
import net.slipcor.pvparena.events.PAPlayerClassChangeEvent;
import net.slipcor.pvparena.events.PAStartEvent;
import net.slipcor.pvparena.events.PAWinEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

class PAListener implements Listener {
    private final EventActions ea;

    public PAListener(final EventActions ea) {
        this.ea = ea;
    }

    @EventHandler
    public void onDeath(final PADeathEvent event) {
        final Arena a = event.getArena();
        final Player p = event.getPlayer();
        this.ea.catchEvent(EventName.DEATH, p, a);
    }

    @EventHandler
    public void onEnd(final PAEndEvent event) {
        final Arena a = event.getArena();
        this.ea.catchEvent(EventName.END, null, a);
    }

    @EventHandler
    public void onExit(final PAExitEvent event) {
        final Arena a = event.getArena();
        final Player p = event.getPlayer();
        this.ea.catchEvent(EventName.EXIT, p, a);
    }

    @EventHandler
    public void onJoin(final PAJoinEvent event) {
        final Arena a = event.getArena();
        final Player p = event.getPlayer();

        if (event.isSpectator()) {
            this.ea.catchEvent(EventName.SPECTATE, p, a);
        } else {
            this.ea.catchEvent(EventName.JOIN, p, a);
        }
    }

    @EventHandler
    public void onKill(final PAKillEvent event) {
        final Arena a = event.getArena();
        final Player p = event.getPlayer();
        this.ea.catchEvent(EventName.KILL, p, a);
    }

    @EventHandler
    public void onLeave(final PALeaveEvent event) {
        final Arena a = event.getArena();
        final Player p = event.getPlayer();
        this.ea.catchEvent(EventName.LEAVE, p, a);
    }

    @EventHandler
    public void onLose(final PALoseEvent event) {
        final Arena a = event.getArena();
        final Player p = event.getPlayer();
        this.ea.catchEvent(EventName.LOSE, p, a);
    }

    @EventHandler
    public void onStart(final PAStartEvent event) {
        final Arena a = event.getArena();
        this.ea.catchEvent(EventName.START, null, a);
    }

    @EventHandler
    public void onWin(final PAWinEvent event) {
        final Arena a = event.getArena();
        final Player p = event.getPlayer();
        this.ea.catchEvent(EventName.WIN, p, a);
    }

    @EventHandler
    public void onClassChange(final PAPlayerClassChangeEvent event) {
        final Arena a = event.getArena();
        final Player p = event.getPlayer();
        this.ea.catchEvent(EventName.CLASSCHANGE, p, a, "%class%", event.getArenaClass().getName());
    }

}
