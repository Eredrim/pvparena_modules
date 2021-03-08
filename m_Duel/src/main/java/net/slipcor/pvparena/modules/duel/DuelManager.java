package net.slipcor.pvparena.modules.duel;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.commands.PAG_Leave;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.exceptions.GameplayException;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.ArenaManager;
import net.slipcor.pvparena.modules.vaultsupport.VaultSupport;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DuelManager extends ArenaModule {
    public DuelManager() {
        super("Duel");
    }

    @Override
    public String version() {
        return getClass().getPackage().getImplementationVersion();
    }

    private String duelSender = null;
    private String duelReceiver = null;
    private int amount = 0;

    @Override
    public boolean checkCommand(final String s) {
        return "duel".equals(s.toLowerCase()) || "accept".equals(s.toLowerCase()) || "decline".equals(s.toLowerCase());
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
    public boolean hasPerms(final CommandSender sender, final Arena arena) {
        return super.hasPerms(sender, arena) || PVPArena.hasPerms(sender, arena);
    }

    @Override
    public void checkJoin(Player player) throws GameplayException {
        if (!player.getName().equals(this.duelReceiver) && !player.getName().equals(this.duelSender)) {
            throw new GameplayException(Language.parse(arena, MSG.MODULE_DUEL_NODIRECTJOIN, this.arena.getName()));
        }
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        if (arena.isFightInProgress()) {
            arena.msg(sender,
                    Language.parse(MSG.ERROR_FIGHT_IN_PROGRESS));
            return;
        }
        String arenaName = null;

        if (!PVPArena.hasOverridePerms(sender) && ArenaManager.isUsingShortcuts()) {
            Map<String, List<String>> vals = ArenaManager.getShortcutDefinitions();
            for (String key : vals.keySet()) {
                if (vals.get(key).contains(arena.getName())) {
                    arenaName = key;
                }
            }
            if (arenaName == null) {
                arenaName = arena.getName();
            }
        } else {
            arenaName = arena.getName();
        }
        if ("duel".equals(args[0].toLowerCase()) && args.length > 1) {
            if (sender.getName().equals(duelSender)) {
                arena.msg(sender, Language.parse(MSG.MODULE_DUEL_REQUESTED_ALREADY));
            } else {
                final Player p = Bukkit.getPlayer(args[1]);
                if (p == null) {
                    arena.msg(sender, Language.parse(MSG.ERROR_PLAYER_NOTFOUND, args[1]));
                    return;
                }

                ArenaPlayer ap = ArenaPlayer.fromPlayer(p);
                if (ap.getArena() != null) {
                    arena.msg(sender, Language.parse(MSG.MODULE_DUEL_BUSY, args[1]));
                    return;
                }

                String message = "";

                if (args.length > 2) {
                    try {
                        int iAmount = Integer.parseInt(args[2]);
                        for (ArenaModule mod : arena.getMods()) {
                            if (mod instanceof VaultSupport) {
                                VaultSupport sup = (VaultSupport) mod;
                                if (!sup.checkForBalance(this, sender, iAmount, true)) {
                                    return;
                                }
                                if (!sup.checkForBalance(this, p, iAmount, false)) {
                                    arena.msg(sender, Language.parse(MSG.MODULE_VAULT_THEYNOTENOUGH, p.getName()));
                                    return;
                                }
                                message = sup.tryFormat(this, iAmount);
                                amount = iAmount;
                            }
                        }

                    } catch( Exception e) {
                        arena.msg(sender, Language.parse(MSG.ERROR_NOT_NUMERIC, args[2]));
                        return;
                    }
                }

                arena.msg(p, Language.parse(MSG.MODULE_DUEL_ANNOUNCE, sender.getName(), arenaName));
                if (!message.equals("")) {
                    arena.msg(p, Language.parse(MSG.MODULE_DUEL_ANNOUNCEMONEY, message));
                }
                arena.msg(p, Language.parse(MSG.MODULE_DUEL_ANNOUNCE2, sender.getName(), arenaName));
                arena.msg(sender, Language.parse(MSG.MODULE_DUEL_REQUESTED, p.getName()));
                duelSender = sender.getName();
                duelReceiver = p.getName();
                class LaterRunner implements Runnable {
                    @Override
                    public void run() {
                        if (duelSender != null) {
                            if (p != null) {
                                arena.msg(p, Language.parse(MSG.MODULE_DUEL_REQUEST_EXPIRED_RECEIVER));
                            }
                            if (sender != null) {
                                arena.msg(sender, Language.parse(MSG.MODULE_DUEL_REQUEST_EXPIRED_SENDER));
                            }
                            duelSender = null;
                            duelReceiver = null;
                        }
                    }
                }
                Bukkit.getScheduler().scheduleSyncDelayedTask(PVPArena.getInstance(), new LaterRunner(), 1200L);
            }
        } else if ("accept".equals(args[0].toLowerCase())) {
            if (duelSender != null && !arena.isFightInProgress()) {

                final Player p = Bukkit.getPlayer(duelSender);
                if (p != null) {
                    for (ArenaModule mod : arena.getMods()) {
                        if (mod instanceof VaultSupport) {
                            VaultSupport sup = (VaultSupport) mod;
                            if (!sup.checkForBalance(this, sender, amount, true)) {
                                break;
                            }
                            if (!sup.checkForBalance(this, p, amount, true)) {
                                break;
                            }
                            sup.tryWithdraw(this, sender, amount, true);
                            sup.tryWithdraw(this, p, amount, true);
                        }
                    }
                    Bukkit.getScheduler().scheduleSyncDelayedTask(PVPArena.getInstance(), new DuelRunnable(this, duelSender, sender.getName()), 500L);
                    arena.msg(p, Language.parse(MSG.MODULE_DUEL_ACCEPTED, sender.getName()));
                }
                duelSender = null;
                duelReceiver = null;
            } else if (arena.isFightInProgress()) {
                arena.msg(sender,
                        Language.parse(MSG.ERROR_FIGHT_IN_PROGRESS));
            }
        } else if ("decline".equals(args[0].toLowerCase()) && duelSender != null) {
            ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(duelSender);
            if (arenaPlayer != null && arenaPlayer.getPlayer() != null){
                arena.msg(arenaPlayer.getPlayer(), Language.parse(MSG.MODULE_DUEL_DECLINED_SENDER));
            }
            arena.msg(sender, Language.parse(MSG.MODULE_DUEL_DECLINED_RECEIVER));
            duelSender = null;
            duelReceiver = null;
        }
    }
    @Override
    public void resetPlayer(Player player, boolean force, boolean soft) {
        if (player == null ||
                !player.getName().equals(duelReceiver) ||
                !player.getName().equals(duelSender)) {
            // player invalid or not a duelling player, ignore!
            return;
        }
        for (final ArenaPlayer ap : arena.getFighters()) {
            if (ap == null || ap.getName().equals(player.getName())) {
                continue;
            }
            if (ArenaPlayer.fromPlayer(player).getStatus() == ArenaPlayer.Status.LOUNGE) {
                arena.msg(ap.getPlayer(), Language.parse(MSG.MODULE_DUEL_CANCELLED));
            }
            if (amount > 0) {
                for (ArenaModule mod : arena.getMods()) {
                    if (mod instanceof VaultSupport) {
                        VaultSupport sup = (VaultSupport) mod;

                        if (ArenaPlayer.fromPlayer(player).getStatus() == ArenaPlayer.Status.LOUNGE) {
                            sup.tryRefund(this, ap.getPlayer(), amount, true);
                            sup.tryRefund(this, player, amount, true);
                            amount = 0;
                            class RunLater implements Runnable {

                                @Override
                                public void run() {
                                    new PAG_Leave().commit(getArena(), ap.getPlayer(), new String[]{});
                                }
                            }
                            if (force) {
                                new RunLater().run();
                            } else {
                                Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), new RunLater(), 3L);
                            }
                        } else {
                            sup.tryDeposit(this, ap.getPlayer(), amount*2, true);
                            amount = 0;
                        }
                    }
                }
            } else {
                class RunLater implements Runnable {

                    @Override
                    public void run() {
                        new PAG_Leave().commit(getArena(), ap.getPlayer(), new String[]{});
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
