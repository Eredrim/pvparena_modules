package net.slipcor.pvparena.modules.autovote;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.core.Debug;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.managers.ArenaManager;
import net.slipcor.pvparena.runnables.ArenaRunnable;
import org.bukkit.Bukkit;

import java.util.HashSet;
import java.util.Set;

public class AutoVoteRunnable extends ArenaRunnable {
    private final Debug debug = new Debug(68);
    private final String definition;
    private final AutoVote module;

    public AutoVoteRunnable(final Arena a, final int i, final AutoVote mod, final String definition) {
        super(MSG.MODULE_AUTOVOTE_VOTENOW.getNode(), i, null, a, false);
        this.definition = definition;
        debug.i("AutoVoteRunnable constructor");
        module = mod;
    }

    @Override
    protected void commit() {
        debug.i("ArenaVoteRunnable commiting");
        AutoVote.commit(definition, module.players);
        Bukkit.getScheduler().runTaskLater(PVPArena.instance, new Runnable() {
            @Override
            public void run() {
                module.vote = null;
                arena.getDebugger().i("clearing 'AutoVote.players'");
                for (final String player : AutoVote.votes.keySet()) {
                    arena.getDebugger().i("removing vote of: " + player);
                }
                AutoVote.votes.clear();
                module.players.clear();
            }
        }, 20L);
    }

    @Override
    protected void warn() {
        PVPArena.instance.getLogger().warning("ArenaVoteRunnable not scheduled yet!");
    }

    @Override
    public void spam() {
        if ((super.message == null) || (MESSAGES.get(seconds) == null)) {
            return;
        }
        final MSG msg = MSG.getByNode(this.message);
        if (msg == null) {
            PVPArena.instance.getLogger().warning("MSG not found: " + this.message);
            return;
        }

        final String arenastring;

        if (definition == null) {
            arenastring = ArenaManager.getNames();
        } else {
            final Set<String> arenas = new HashSet<>();
            for (final String string : ArenaManager.getShortcutDefinitions().get(definition)) {
                arenas.add(string);
            }
            arenastring = StringParser.joinSet(arenas, ", ");
        }

        final String message = seconds > 5 ? Language.parse(msg, MESSAGES.get(seconds), arenastring) : MESSAGES.get(seconds);

        for (final ArenaPlayer ap : module.players) {
            if (!module.hasVoted(ap.getName())) {
                module.getArena().msg(ap.get(), message);
            }
        }
    }
}
