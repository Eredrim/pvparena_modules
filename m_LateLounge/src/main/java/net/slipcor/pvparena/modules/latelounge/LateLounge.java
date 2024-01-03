package net.slipcor.pvparena.modules.latelounge;

import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.exceptions.GameplayException;
import net.slipcor.pvparena.exceptions.GameplayExceptionNotice;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.WorkflowManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static java.util.Arrays.asList;

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

    private final List<Player> playerList = new ArrayList<>();

    @Override
    public boolean handleJoin(Player player) throws GameplayExceptionNotice {
        int requiredPlayers = this.arena.getConfig().getInt(CFG.READY_MINPLAYERS);

        boolean isLoungeOpen = this.arena.getFighters().stream()
                .anyMatch(ap -> asList(PlayerStatus.LOUNGE, PlayerStatus.READY).contains(ap.getStatus()));

        if(isLoungeOpen) {
            return false;
        }

        // Player already joined
        if (this.playerList.contains(player)) {
            if (this.playerList.size() < requiredPlayers) {
                int pos = this.playerList.indexOf(player) + 1;
                this.arena.msg(player, MSG.MODULE_LATELOUNGE_POSITION, String.valueOf(pos));
                throw new GameplayExceptionNotice(MSG.MODULE_LATELOUNGE_WAIT);
            }
        }

        // not enough players
        if (requiredPlayers > this.playerList.size() + 1) {
            this.playerList.add(player);
            ArenaPlayer.fromPlayer(player).setQueuedArena(this.arena);

            Bukkit.getOnlinePlayers().stream()
                    .filter(p -> !p.equals(player))
                    .forEach(p -> {
                        try {
                            this.arena.msg(p, MSG.MODULE_LATELOUNGE_ANNOUNCE, this.arena.getName(), player.getName());
                        } catch (Exception ignored) {}
                    });
            this.arena.msg(player, MSG.MODULE_LATELOUNGE_POSITION, String.valueOf(this.playerList.size()));
            throw new GameplayExceptionNotice(MSG.MODULE_LATELOUNGE_WAIT);
        }

        if (requiredPlayers == this.playerList.size() + 1) {
            // if this player is ok => enough players
            this.playerList.add(player);

            Set<Player> readyList = new HashSet<>();

            this.playerList.stream().filter(Objects::nonNull).forEach(p -> {
                this.arena.getMods().stream().filter(mod -> !mod.getName().equals(this.getName())).forEach(mod -> {
                    try {
                        mod.checkJoin(p);
                        readyList.add(p);
                    } catch (GameplayException e) {
                        this.arena.msg(p, MSG.MODULE_LATELOUNGE_REJOIN);
                    }
                });
            });

            if (readyList.size() < this.playerList.size()) {
                this.playerList.removeIf(p -> !readyList.contains(p));
                throw new GameplayExceptionNotice(MSG.MODULE_LATELOUNGE_WAIT);
            } else {
                // SUCCESS!
                this.playerList.stream()
                        .filter(p -> !p.equals(player))
                        .forEach(p -> {
                            ArenaPlayer.fromPlayer(p).setQueuedArena(null);
                            WorkflowManager.handleJoin(this.arena, p, new String[0]);
                        });
                this.playerList.clear();
            }
        }
        // enough, ignore and let something else handle the start!
        return false;
    }

    @Override
    public boolean handleQueuedLeave(final ArenaPlayer arenaPlayer) {
        if(this.playerList.contains(arenaPlayer.getPlayer())) {
            this.playerList.remove(arenaPlayer.getPlayer());
            this.arena.msg(arenaPlayer.getPlayer(), MSG.MODULE_LATELOUNGE_LEAVE, this.arena.getName());
            arenaPlayer.setQueuedArena(null);
            return true;
        }
        return false;
    }

    @Override
    public void reset(final boolean force) {
        this.playerList.clear();
    }
}
