package net.slipcor.pvparena.modules.startfreeze;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.runnables.ArenaRunnable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class StartFreezer extends ArenaRunnable {

    private final StartFreeze module;
    private final Map<String, Float> speeds = new HashMap<>();

    StartFreezer(final StartFreeze mod) {
        super(Language.MSG.MODULE_STARTFREEZE_ANNOUNCE.getNode(),
                mod.getArena().getArenaConfig().getInt(Config.CFG.MODULES_STARTFREEZE_TIMER),
                null, mod.getArena(), false);
        module = mod;
        for (ArenaPlayer ap : mod.getArena().getFighters()) {
            speeds.put(ap.getName(), ap.get().getWalkSpeed());
            ap.get().setWalkSpeed(0);
        }
    }


    @Override
    protected void warn() {

    }

    /**
     * the run method, commit start
     */
    @Override
    protected void commit() {
        if (module != null) {
            module.runnable = null;
            for (ArenaPlayer ap : module.getArena().getFighters()) {
                ap.get().setWalkSpeed(0.2f);
            }
            module.speed(speeds);
        }
    }



    @Override
    public void spam() {
        if ((message == null) || (MESSAGES.get(seconds) == null)) {
            return;
        }
        final Language.MSG msg = Language.MSG.getByNode(message);
        if (msg == null) {
            PVPArena.instance.getLogger().warning("MSG not found: " + message);
            return;
        }
        final String message = seconds > 5 ? Language.parse(arena, msg, String.valueOf(seconds)) : MESSAGES.get(seconds);
        if (global) {
            final Collection<? extends Player> players = Bukkit.getOnlinePlayers();

            for (final Player p : players) {
                try {
                    if (arena != null && arena.hasPlayer(p)) {
                        continue;
                    }
                    if (p.getName().equals(sPlayer)) {
                        continue;
                    }
                    Arena.pmsg(p, message);
                } catch (final Exception e) {
                }
            }

            return;
        }
        if (arena != null) {
            final Set<ArenaPlayer> players = arena.getFighters();
            for (final ArenaPlayer ap : players) {
                if (ap.getName().equals(sPlayer)) {
                    continue;
                }
                if (ap.get() != null) {
                    arena.msg(ap.get(), message);
                }
            }
            return;
        }

        if (Bukkit.getPlayer(sPlayer) != null) {
            final ArenaPlayer aPlayer = ArenaPlayer.parsePlayer(sPlayer);
            if (aPlayer.getArena() == null) {
                Arena.pmsg(Bukkit.getPlayer(sPlayer), message);
            } else {
                aPlayer.getArena().msg(aPlayer.get(), message);
            }
        }
    }
}
