package net.slipcor.pvparena.modules.startfreeze;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.commands.AbstractArenaCommand;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.PermissionManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StartFreeze extends ArenaModule implements Listener {
    StartFreezer runnable;
    private boolean setup;
    private Map<String, Float> speeds = new HashMap<>();


    public StartFreeze() {
        super("StartFreeze");
    }

    @Override
    public String version() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public boolean checkCommand(final String s) {
        return "startfreeze".equals(s) || "!sf".equals(s);
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("startfreeze");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!sf");
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        // !sf 5

        if (!PermissionManager.hasAdminPerm(sender) && !PermissionManager.hasBuilderPerm(sender, arena)) {
            arena.msg(sender, MSG.ERROR_NOPERM, Language.parse(MSG.ERROR_NOPERM_X_ADMIN));
            return;
        }

        if (!AbstractArenaCommand.argCountValid(sender, arena, args, new Integer[]{2})) {
            return;
        }

        if ("!sf".equals(args[0]) || "startfreeze".equals(args[0])) {
            final int i;
            try {
                i = Integer.parseInt(args[1]);
            } catch (final Exception e) {
                arena.msg(sender, MSG.ERROR_NOT_NUMERIC, args[1]);
                return;
            }

            arena.getConfig().set(CFG.MODULES_STARTFREEZE_TIMER, i);
            arena.getConfig().save();
            arena.msg(sender, MSG.SET_DONE, CFG.MODULES_STARTFREEZE_TIMER.getNode(), String.valueOf(i));
        }
    }

    @Override
    public void displayInfo(final CommandSender sender) {
        sender.sendMessage("seconds: " + arena.getConfig().getInt(CFG.MODULES_STARTFREEZE_TIMER));
    }

    @Override
    public void reset(final boolean force) {
        if (runnable != null) {
            runnable.cancel();
        }
        runnable = null;
        speeds.clear();
    }

    @Override
    public void resetPlayer(final Player p, final boolean soft, final boolean force) {
        if (speeds.containsKey(p.getName())) {
            p.setWalkSpeed(speeds.get(p.getName()));
        }
        int ticks = arena.getConfig().getInt(Config.CFG.MODULES_STARTFREEZE_TIMER) * 20;

        for (ArenaPlayer arenaPlayer : arena.getFighters()) {
            try {
                if (arenaPlayer.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.JUMP, ticks, -7, true, true), true));
            } catch (Exception e) {

            }
        }
    }

    @Override
    public void parseStart() {
        runnable = new StartFreezer(this);
    }


    @Override
    public void configParse(final YamlConfiguration config) {
        if (!setup) {
            Bukkit.getPluginManager().registerEvents(this, PVPArena.getInstance());
            setup = true;
        }
    }

    @EventHandler
    public void onPlayerMove(final PlayerMoveEvent event) {
        final Player p = event.getPlayer();
        final ArenaPlayer ap = ArenaPlayer.fromPlayer(p);
        if (ap.getArena() == null || !arena.equals(ap.getArena())) {
            return;
        }
        if (runnable != null) {
            final Location from = event.getFrom();
            final Location to = event.getTo();
            if (from.getBlockX() != to.getBlockX() ||
                    from.getBlockY() != to.getBlockY() ||
                    from.getBlockZ() != to.getBlockZ()) {
                event.setCancelled(true);
            }
        }
    }

    public void speed(Map<String, Float> speeds) {
        this.speeds = speeds;
    }
}
