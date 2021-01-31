package net.slipcor.pvparena.modules.latelounge;

import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.classes.PACheck;
import net.slipcor.pvparena.commands.AbstractArenaCommand;
import net.slipcor.pvparena.commands.PAG_Join;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.ArenaManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LateLounge extends ArenaModule {
    public LateLounge() {
        super("LateLounge");
    }

    private static final int priority = 3;

    @Override
    public String version() {
        return this.getClass().getPackage().getImplementationVersion();
    }

    private List<String> playerList;

    @Override
    public PACheck checkJoin(final CommandSender sender,
                             final PACheck res, final boolean join) {
        if (!join || res.hasError() || res.getPriority() > priority) {
            return res;
        }

        final Player player = (Player) sender;

        if (this.getPlayerList().contains(player.getName())) {
            if (this.getPlayerList().size() < this.arena.getArenaConfig().getInt(CFG.READY_MINPLAYERS)) {
                res.setError(this, Language.parse(MSG.MODULE_LATELOUNGE_WAIT));
                int pos = 1;

                for (final String name : this.getPlayerList()) {
                    if (name.equals(player.getName())) {
                        break;
                    }
                    pos++;
                }

                this.arena.msg(player, Language.parse(MSG.MODULE_LATELOUNGE_POSITION, String.valueOf(pos)));
                return res;
            }
        }

        if (this.arena.getArenaConfig().getInt(CFG.READY_MINPLAYERS) > this.getPlayerList().size() + 1) {
            // not enough players
            this.getPlayerList().add(player.getName());
            final int pos = this.getPlayerList().size();
            for (final Player p : Bukkit.getOnlinePlayers()) {
                if (p.equals(player)) {
                    continue;
                }
                try {
                    this.arena.msg(p, Language.parse(MSG.MODULE_LATELOUNGE_ANNOUNCE, ArenaManager.getIndirectArenaName(this.arena), player.getName()));

                } catch (final Exception e) {
                    //
                }
            }
            this.arena.msg(player, Language.parse(MSG.MODULE_LATELOUNGE_POSITION, String.valueOf(pos)));
            res.setError(this, Language.parse(MSG.MODULE_LATELOUNGE_WAIT));
            return res;
        }
        if (this.arena.getArenaConfig().getInt(CFG.READY_MINPLAYERS) == this.getPlayerList().size() + 1) {
            // not enough players
            this.getPlayerList().add(player.getName());

            Set<String> removals = new HashSet<>();

            for (String s : this.getPlayerList()) {
                Player p = Bukkit.getPlayerExact(s);

                boolean removeMe = false;

                if (p != null) {
                    for (ArenaModule mod : this.arena.getMods()) {
                        if (!mod.getName().equals(this.getName())) {
                            if (mod.checkJoin(p, new PACheck(), true).hasError()) {
                                removeMe = true;
                                break;
                            }
                        }
                    }
                }

                if (p == null || removeMe) {
                    removals.add(s);
                    if (p != null) {
                        res.setError(this, Language.parse(MSG.MODULE_LATELOUNGE_REJOIN));
                    }
                }
            }

            if (removals.size() > 0) {
                for (String s : removals) {
                    this.getPlayerList().remove(s);
                }
            } else {
                // SUCCESS!
                for (String s : this.getPlayerList()) {
                    if (s.equals(sender.getName())) {
                        continue;
                    }
                    Player p = Bukkit.getPlayerExact(s);
                    AbstractArenaCommand command = new PAG_Join();
                    command.commit(this.arena, p, new String[0]);
                }
                return res;
            }
            res.setError(this, Language.parse(MSG.MODULE_LATELOUNGE_WAIT));
        }
        // enough, ignore and let something else handle the start!
        return res;
    }

    private List<String> getPlayerList() {
        if (this.playerList == null) {
            this.playerList = new ArrayList<>();
        }
        return this.playerList;
    }

    @Override
    public boolean hasSpawn(final String name) {
        return this.getPlayerList().contains(name);
    }

    @Override
    public boolean handleSpecialLeave(final ArenaPlayer player) {
        if(this.getPlayerList().contains(player.getName())) {
            this.getPlayerList().remove(player.getName());
            this.arena.msg(player.get(), Language.parse(MSG.MODULE_LATELOUNGE_LEAVE, this.arena.getName()));
            return true;
        }
        return false;
    }

    @Override
    public void reset(final boolean force) {
        this.getPlayerList().clear();
    }
}
