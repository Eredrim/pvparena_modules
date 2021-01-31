package net.slipcor.pvparena.modules.respawnrelay;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaPlayer.Status;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.SpawnManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class RespawnRelay extends ArenaModule {
    private class RelayListener implements Listener {
        @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
        public void onAsyncChat(final AsyncPlayerChatEvent event) {
            final ArenaPlayer player = ArenaPlayer.parsePlayer(event.getPlayer().getName());

            if (player.getArena() == null) {
                return;
            }

            RespawnRelay module = null;

            for (final ArenaModule mod : player.getArena().getMods()) {
                if ("RespawnRelay".equals(mod.getName())) {
                    module = (RespawnRelay) mod;
                    break;
                }
            }

            if (module == null || !player.getArena().getArenaConfig().getBoolean(CFG.MODULES_RESPAWNRELAY_CHOOSESPAWN)) {
                return;
            }

            if (!module.runnerMap.containsKey(player.getName())) {
                return;
            }

            event.setCancelled(true);

            final Set<PASpawn> map = SpawnManager.getPASpawnsStartingWith(player.getArena(), event.getMessage());

            if (map.size() < 1) {
                return;
            }

            int pos = new Random().nextInt(map.size());

            for (final PASpawn s : map) {
                if (--pos < 0) {
                    overrideMap.put(player.getName(), s.getName());
                    return;
                }
            }

            overrideMap.put(player.getName(), event.getMessage());
        }
    }

    private Map<String, BukkitRunnable> runnerMap;
    final Map<String, String> overrideMap = new HashMap<>();
    private static Listener listener;

    public RespawnRelay() {
        super("RespawnRelay");
    }

    @Override
    public String version() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public String checkForMissingSpawns(final Set<String> list) {
        if (listener == null) {
            listener = new RelayListener();
            Bukkit.getPluginManager().registerEvents(listener, PVPArena.getInstance());
        }

        boolean allTeams = true;

        for (String team : arena.getTeamNames()) {
            if (!list.contains(team.toLowerCase()+"relay")) {
                allTeams = false;
                break;
            }
        }

        return (allTeams || list.contains("relay")) ? null : "relay(s) not set";
    }

    @Override
    public void displayInfo(final CommandSender sender) {
        sender.sendMessage("seconds: " + arena.getArenaConfig().getInt(CFG.MODULES_RESPAWNRELAY_INTERVAL));
    }

    Map<String, BukkitRunnable> getRunnerMap() {
        if (runnerMap == null) {
            runnerMap = new HashMap<>();
        }
        return runnerMap;
    }

    @Override
    public boolean hasSpawn(final String s) {
        for (String team : arena.getTeamNames()) {
            if ((team.toLowerCase()+"relay").equals(s)) {
                return true;
            }
        }
        return "relay".equals(s);
    }

    @Override
    public void reset(final boolean force) {
        for (final BukkitRunnable br : getRunnerMap().values()) {
            br.cancel();
        }
        getRunnerMap().clear();
    }

    @Override
    public boolean tryDeathOverride(final ArenaPlayer ap, List<ItemStack> drops) {
        ap.setStatus(Status.DEAD);

        if (drops == null) {
            drops = new ArrayList<>();
        }
        if (SpawnManager.getSpawnByExactName(arena, ap.getArenaTeam().getName()+"relay") == null) {
            SpawnManager.respawn(arena, ap, "relay");
        } else {
            SpawnManager.respawn(arena, ap, ap.getArenaTeam().getName()+"relay");
        }
        arena.unKillPlayer(ap.get(), ap.get().getLastDamageCause() == null ? null : ap.get().getLastDamageCause().getCause(), ap.get().getKiller());

        if (getRunnerMap().containsKey(ap.getName())) {
            return true;
        }

        getRunnerMap().put(ap.getName(), new RelayRunnable(this, arena, ap, drops));

        return true;
    }
}
