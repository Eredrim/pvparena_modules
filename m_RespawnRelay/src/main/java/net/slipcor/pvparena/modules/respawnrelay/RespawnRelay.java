package net.slipcor.pvparena.modules.respawnrelay;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.PlayerStatus;
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
import java.util.stream.Collectors;

public class RespawnRelay extends ArenaModule {

    public static final String RELAY = "relay";

    private class RelayListener implements Listener {
        @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
        public void onAsyncChat(final AsyncPlayerChatEvent event) {
            final ArenaPlayer player = ArenaPlayer.fromPlayer(event.getPlayer());

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

            if (module == null || !player.getArena().getConfig().getBoolean(CFG.MODULES_RESPAWNRELAY_CHOOSESPAWN)) {
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
    public Set<String> checkForMissingSpawns(final Set<String> list) {
        if (listener == null) {
            listener = new RelayListener();
            Bukkit.getPluginManager().registerEvents(listener, PVPArena.getInstance());
        }

        Set<String> missingSpawns = new HashSet<>();
        if(this.arena.isFreeForAll()) {
            if(!list.contains(RELAY)) {
                missingSpawns.add(RELAY);
            }
        } else {
            missingSpawns = this.arena.getTeamNames().stream()
                    .filter(team -> !list.contains(team.toLowerCase() + RELAY))
                    .map(team -> team.toLowerCase() + RELAY)
                    .collect(Collectors.toSet());
        }

        return missingSpawns;
    }

    @Override
    public void displayInfo(final CommandSender sender) {
        sender.sendMessage("seconds: " + arena.getConfig().getInt(CFG.MODULES_RESPAWNRELAY_INTERVAL));
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
            if ((team.toLowerCase()+ RELAY).equals(s)) {
                return true;
            }
        }
        return RELAY.equals(s);
    }

    @Override
    public void reset(final boolean force) {
        for (final BukkitRunnable br : getRunnerMap().values()) {
            br.cancel();
        }
        getRunnerMap().clear();
    }

    @Override
    public boolean tryDeathOverride(final ArenaPlayer arenaPlayer, List<ItemStack> drops) {
        arenaPlayer.setStatus(PlayerStatus.DEAD);

        if (drops == null) {
            drops = new ArrayList<>();
        }
        if (SpawnManager.getSpawnByExactName(arena, arenaPlayer.getArenaTeam().getName()+ RELAY) == null) {
            SpawnManager.respawn(arena, arenaPlayer, RELAY);
        } else {
            SpawnManager.respawn(arena, arenaPlayer, arenaPlayer.getArenaTeam().getName()+ RELAY);
        }
        arena.unKillPlayer(arenaPlayer.getPlayer(), arenaPlayer.getPlayer().getLastDamageCause() == null ? null : arenaPlayer.getPlayer().getLastDamageCause().getCause(), arenaPlayer.getPlayer().getKiller());

        if (getRunnerMap().containsKey(arenaPlayer.getName())) {
            return true;
        }

        getRunnerMap().put(arenaPlayer.getName(), new RelayRunnable(this, arena, arenaPlayer, drops));

        return true;
    }
}
