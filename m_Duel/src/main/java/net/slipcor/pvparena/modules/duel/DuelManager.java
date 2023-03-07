package net.slipcor.pvparena.modules.duel;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.commands.PAG_Leave;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.exceptions.GameplayException;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.modules.vaultsupport.VaultSupport;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DuelManager extends ArenaModule {
    public DuelManager() {
        super("Duel");
    }

    @Override
    public String version() {
        return this.getClass().getPackage().getImplementationVersion();
    }

    private String duelSender = null;
    private String duelReceiver = null;
    private int amount = 0;

    @Override
    public boolean checkCommand(final String s) {
        return "duel".equalsIgnoreCase(s) || "accept".equalsIgnoreCase(s) || "decline".equalsIgnoreCase(s);
    }

    @Override
    public List<String> getMain() {
        return Arrays.asList("duel", "accept", "decline");
    }

    @Override
    public List<String> getShort() {
        return new ArrayList<>();
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        result.define(new String[]{"{Player}"});
        return result;
    }

    @Override
    public void checkJoin(Player player) throws GameplayException {
        if (!player.getName().equals(this.duelReceiver) && !player.getName().equals(this.duelSender)) {
            throw new GameplayException(Language.parse(MSG.MODULE_DUEL_NODIRECTJOIN, this.arena.getName()));
        }
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        if (this.arena.isFightInProgress()) {
            this.arena.msg(sender, MSG.ERROR_FIGHT_IN_PROGRESS);
            return;
        }

        String arenaName =  this.arena.getName();

        if ("duel".equalsIgnoreCase(args[0]) && args.length > 1) {
            if (sender.getName().equals(this.duelSender)) {
                this.arena.msg(sender, MSG.MODULE_DUEL_REQUESTED_ALREADY);
            } else {
                final Player p = Bukkit.getPlayer(args[1]);
                if (p == null) {
                    this.arena.msg(sender, MSG.ERROR_PLAYER_NOTFOUND, args[1]);
                    return;
                }

                ArenaPlayer ap = ArenaPlayer.fromPlayer(p);
                if (ap.getArena() != null) {
                    this.arena.msg(sender, MSG.MODULE_DUEL_BUSY, args[1]);
                    return;
                }

                String message = "";

                if (args.length > 2) {
                    try {
                        int iAmount = Integer.parseInt(args[2]);
                        for (ArenaModule mod : this.arena.getMods()) {
                            if (mod instanceof VaultSupport) {
                                VaultSupport sup = (VaultSupport) mod;
                                if (!sup.checkForBalance(this, sender, iAmount, true)) {
                                    return;
                                }
                                if (!sup.checkForBalance(this, p, iAmount, false)) {
                                    this.arena.msg(sender, MSG.MODULE_VAULT_THEYNOTENOUGH, p.getName());
                                    return;
                                }
                                message = VaultSupport.tryFormat(this, iAmount);
                                this.amount = iAmount;
                            }
                        }

                    } catch( Exception e) {
                        this.arena.msg(sender, MSG.ERROR_NOT_NUMERIC, args[2]);
                        return;
                    }
                }

                this.arena.msg(p, MSG.MODULE_DUEL_ANNOUNCE, sender.getName(), arenaName);
                if (!message.equals("")) {
                    this.arena.msg(p, MSG.MODULE_DUEL_ANNOUNCEMONEY, message);
                }
                this.arena.msg(p, MSG.MODULE_DUEL_ANNOUNCE2, sender.getName(), arenaName);
                this.arena.msg(sender, MSG.MODULE_DUEL_REQUESTED, p.getName());
                this.duelSender = sender.getName();
                this.duelReceiver = p.getName();
                class LaterRunner implements Runnable {
                    @Override
                    public void run() {
                        if (DuelManager.this.duelSender != null) {
                            if (p != null) {
                                DuelManager.this.arena.msg(p, MSG.MODULE_DUEL_REQUEST_EXPIRED_RECEIVER);
                            }
                            if (sender != null) {
                                DuelManager.this.arena.msg(sender, MSG.MODULE_DUEL_REQUEST_EXPIRED_SENDER);
                            }
                            DuelManager.this.duelSender = null;
                            DuelManager.this.duelReceiver = null;
                        }
                    }
                }
                Bukkit.getScheduler().scheduleSyncDelayedTask(PVPArena.getInstance(), new LaterRunner(), 1200L);
            }
        } else if ("accept".equalsIgnoreCase(args[0])) {
            if (this.duelSender != null && !this.arena.isFightInProgress()) {

                final Player p = Bukkit.getPlayer(this.duelSender);
                if (p != null) {
                    for (ArenaModule mod : this.arena.getMods()) {
                        if (mod instanceof VaultSupport) {
                            VaultSupport sup = (VaultSupport) mod;
                            if (!sup.checkForBalance(this, sender, this.amount, true)) {
                                break;
                            }
                            if (!sup.checkForBalance(this, p, this.amount, true)) {
                                break;
                            }
                            sup.tryWithdraw(this, sender, this.amount, true);
                            sup.tryWithdraw(this, p, this.amount, true);
                        }
                    }
                    Bukkit.getScheduler().scheduleSyncDelayedTask(PVPArena.getInstance(), new DuelRunnable(this, this.duelSender, sender.getName()), 500L);
                    this.arena.msg(p, MSG.MODULE_DUEL_ACCEPTED, sender.getName());
                }
                this.duelSender = null;
                this.duelReceiver = null;
            } else if (this.arena.isFightInProgress()) {
                this.arena.msg(sender, MSG.ERROR_FIGHT_IN_PROGRESS);
            }
        } else if ("decline".equalsIgnoreCase(args[0]) && this.duelSender != null) {
            ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(this.duelSender);
            if (arenaPlayer != null && arenaPlayer.getPlayer() != null){
                this.arena.msg(arenaPlayer.getPlayer(), MSG.MODULE_DUEL_DECLINED_SENDER);
            }
            this.arena.msg(sender, MSG.MODULE_DUEL_DECLINED_RECEIVER);
            this.duelSender = null;
            this.duelReceiver = null;
        }
    }
    @Override
    public void resetPlayer(Player player, boolean force, boolean soft) {
        if (player == null ||
                !player.getName().equals(this.duelReceiver) ||
                !player.getName().equals(this.duelSender)) {
            // player invalid or not a duelling player, ignore!
            return;
        }
        for (final ArenaPlayer ap : this.arena.getFighters()) {
            if (ap == null || ap.getName().equals(player.getName())) {
                continue;
            }
            if (ArenaPlayer.fromPlayer(player).getStatus() == PlayerStatus.LOUNGE) {
                this.arena.msg(ap.getPlayer(), MSG.MODULE_DUEL_CANCELLED);
            }
            if (this.amount > 0) {
                for (ArenaModule mod : this.arena.getMods()) {
                    if (mod instanceof VaultSupport) {
                        VaultSupport sup = (VaultSupport) mod;

                        if (ArenaPlayer.fromPlayer(player).getStatus() == PlayerStatus.LOUNGE) {
                            sup.tryRefund(this, ap.getPlayer(), this.amount, true);
                            sup.tryRefund(this, player, this.amount, true);
                            this.amount = 0;
                            class RunLater implements Runnable {

                                @Override
                                public void run() {
                                    new PAG_Leave().commit(DuelManager.this.getArena(), ap.getPlayer(), new String[]{});
                                }
                            }
                            if (force) {
                                new RunLater().run();
                            } else {
                                Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), new RunLater(), 3L);
                            }
                        } else {
                            sup.tryDeposit(this, ap.getPlayer(), this.amount *2, true);
                            this.amount = 0;
                        }
                    }
                }
            } else {
                class RunLater implements Runnable {

                    @Override
                    public void run() {
                        new PAG_Leave().commit(DuelManager.this.getArena(), ap.getPlayer(), new String[]{});
                    }
                }
                if (force) {
                    new RunLater().run();
                } else {
                    Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), new RunLater(), 3L);
                }
            }
        }
    }
}
