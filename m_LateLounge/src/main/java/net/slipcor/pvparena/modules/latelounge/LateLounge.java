package net.slipcor.pvparena.modules.latelounge;

import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.commands.AbstractArenaCommand;
import net.slipcor.pvparena.commands.PAG_Join;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.exceptions.GameplayException;
import net.slipcor.pvparena.exceptions.GameplayExceptionNotice;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.ArenaManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LateLounge extends ArenaModule {
    public LateLounge() {
        super("LateLounge");
    }

    private static final int PRIORITY = 3;

    @Override
    public String version() {
        return this.getClass().getPackage().getImplementationVersion();
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    private final List<String> playerList = new ArrayList<>();

    @Override
    public boolean handleJoin(Player player) throws GameplayExceptionNotice {
        int requiredPlayers = this.arena.getArenaConfig().getInt(CFG.READY_MINPLAYERS);

        if (this.playerList.contains(player.getName())) {
            if (this.playerList.size() < requiredPlayers) {
                int pos = this.playerList.indexOf(player.getName());
                this.arena.msg(player, Language.parse(MSG.MODULE_LATELOUNGE_POSITION, String.valueOf(pos)));
                
                throw new GameplayExceptionNotice(MSG.MODULE_LATELOUNGE_WAIT);
            }
        }

        if (requiredPlayers > this.playerList.size() + 1) {
            // not enough players
            this.playerList.add(player.getName());
            final int pos = this.playerList.size();
            for (final Player p : Bukkit.getOnlinePlayers()) {
                if (!p.equals(player)) {
                    try {
                        this.arena.msg(p, Language.parse(MSG.MODULE_LATELOUNGE_ANNOUNCE, ArenaManager.getIndirectArenaName(this.arena), player.getName()));
                    } catch (Exception ignored) {

                    }
                }
            }
            this.arena.msg(player, Language.parse(MSG.MODULE_LATELOUNGE_POSITION, String.valueOf(pos)));
            throw new GameplayExceptionNotice(MSG.MODULE_LATELOUNGE_WAIT);
        }

        if (requiredPlayers == this.playerList.size() + 1) {
            // if this player is ok => enough players
            this.playerList.add(player.getName());

            Set<String> removals = new HashSet<>();

            for (String s : this.playerList) {
                Player p = Bukkit.getPlayerExact(s);

                if (p != null) {
                    for (ArenaModule mod : this.arena.getMods()) {
                        if (!mod.getName().equals(this.getName())) {
                            try {
                                mod.checkJoin(p);
                            } catch (GameplayException e) {
                                this.arena.msg(p, Language.parse(MSG.MODULE_LATELOUNGE_REJOIN));
                                removals.add(s);
                                break;
                            }
                        }
                    }
                } else {
                    removals.add(s);
                }
            }

            if (!removals.isEmpty()) {
                this.playerList.removeAll(removals);
                throw new GameplayExceptionNotice(MSG.MODULE_LATELOUNGE_WAIT);
            } else {
                // SUCCESS!
                for (String s : this.playerList) {
                    if (!s.equals(player.getName())) {
                        Player p = Bukkit.getPlayerExact(s);
                        AbstractArenaCommand command = new PAG_Join();
                        command.commit(this.arena, p, new String[0]);
                    }
                }
            }
        }
        // enough, ignore and let something else handle the start!
        return false;
    }

    @Override
    public boolean hasSpawn(final String name) {
        return this.playerList.contains(name);
    }

    @Override
    public boolean handleSpecialLeave(final ArenaPlayer player) {
        if(this.playerList.contains(player.getName())) {
            this.playerList.remove(player.getName());
            this.arena.msg(player.get(), Language.parse(MSG.MODULE_LATELOUNGE_LEAVE, this.arena.getName()));
            return true;
        }
        return false;
    }

    @Override
    public void reset(final boolean force) {
        this.playerList.clear();
    }
}
