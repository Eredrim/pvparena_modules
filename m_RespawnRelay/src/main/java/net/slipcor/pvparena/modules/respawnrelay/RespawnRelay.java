package net.slipcor.pvparena.modules.respawnrelay;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.classes.PADeathInfo;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.RandomUtils;
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

            if (map.isEmpty()) {
                return;
            }

            overrideMap.put(player.getName(), RandomUtils.getRandom(map, new Random()).getName());
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
    public Set<PASpawn> checkForMissingSpawns(final Set<PASpawn> spawns) {
        if (listener == null) {
            listener = new RelayListener();
            Bukkit.getPluginManager().registerEvents(listener, PVPArena.getInstance());
        }

        Set<PASpawn> missingSpawns = new HashSet<>();
        if(this.arena.isFreeForAll()) {
            missingSpawns.addAll(SpawnManager.getMissingFFACustom(spawns, RELAY));
        } else {
            missingSpawns.addAll(SpawnManager.getMissingTeamCustom(this.arena, spawns, RELAY));
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
    public boolean hasSpawn(final String s, final String teamName) {
        for (String team : arena.getTeamNames()) {
            if (team.equalsIgnoreCase(teamName) && RELAY.equals(s)) {
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
    public boolean tryDeathOverride(ArenaPlayer arenaPlayer, PADeathInfo deathInfo, List<ItemStack> keptItems) {
        arenaPlayer.setStatus(PlayerStatus.DEAD);

        if (keptItems == null) {
            keptItems = new ArrayList<>();
        }
        if (SpawnManager.getSpawnByExactName(arena, arenaPlayer.getArenaTeam().getName()+ RELAY) == null) {
            SpawnManager.respawn(arenaPlayer, RELAY);
        } else {
            SpawnManager.respawn(arenaPlayer, arenaPlayer.getArenaTeam().getName()+ RELAY);
        }
        arenaPlayer.revive(deathInfo);

        if (getRunnerMap().containsKey(arenaPlayer.getName())) {
            return true;
        }

        getRunnerMap().put(arenaPlayer.getName(), new RelayRunnable(this, arena, arenaPlayer, deathInfo, keptItems));

        return true;
    }
}
