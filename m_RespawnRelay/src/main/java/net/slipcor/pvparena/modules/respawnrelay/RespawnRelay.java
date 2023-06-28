package net.slipcor.pvparena.modules.respawnrelay;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.classes.PADeathInfo;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.SpawnManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

public class RespawnRelay extends ArenaModule {

    public static final String RELAY = "relay";

    private Map<String, BukkitRunnable> runnerMap;
    private final Map<String, String> spawnPointOverrideMap = new HashMap<>();
    private Listener listener;

    public RespawnRelay() {
        super("RespawnRelay");
    }

    @Override
    public String version() {
        return this.getClass().getPackage().getImplementationVersion();
    }

    @Override
    public Set<PASpawn> checkForMissingSpawns(final Set<PASpawn> spawns) {
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
        sender.sendMessage(String.format("seconds: %d", this.arena.getConfig().getInt(CFG.MODULES_RESPAWNRELAY_INTERVAL)));
    }

    public Map<String, BukkitRunnable> getRunnerMap() {
        if (this.runnerMap == null) {
            this.runnerMap = new HashMap<>();
        }
        return this.runnerMap;
    }

    public Map<String, String> getSpawnPointOverrideMap() {
        return this.spawnPointOverrideMap;
    }

    @Override
    public boolean hasSpawn(final String s, final String teamName) {
        return RELAY.equals(s) || this.arena.getTeamNames().stream().anyMatch(t -> t.equalsIgnoreCase(teamName));
    }

    @Override
    public void parseStart() {
        if(this.listener == null && this.arena.isFreeForAll() && this.arena.getConfig().getBoolean(Config.CFG.MODULES_RESPAWNRELAY_CHOOSESPAWN)) {
            this.listener = new RelayListener(this);
            Bukkit.getPluginManager().registerEvents(this.listener, PVPArena.getInstance());
        }
    }

    @Override
    public void reset(final boolean force) {
        this.getRunnerMap().values().forEach(BukkitRunnable::cancel);
        this.getRunnerMap().clear();

        if(this.listener != null) {
            HandlerList.unregisterAll(this.listener);
        }
    }

    @Override
    public boolean tryDeathOverride(ArenaPlayer arenaPlayer, PADeathInfo deathInfo, List<ItemStack> keptItems) {
        if(arenaPlayer.getArena().getGoal().getLives(arenaPlayer) <= 0) {
            return false;
        }

        String teamRelay = String.format("%s%s", arenaPlayer.getArenaTeam().getName(), RELAY);

        if (SpawnManager.getSpawnByExactName(this.arena, teamRelay) == null) {
            SpawnManager.respawn(arenaPlayer, RELAY);
        } else {
            SpawnManager.respawn(arenaPlayer, teamRelay);
        }

        arenaPlayer.revive(deathInfo);

        // Suggest respawn choice if listener has been enabled (FFA arena + respawnrelay.choosespawn enabled)
        if(this.listener != null) {
            String spawnNumberList = SpawnManager.getPASpawnsStartingWith(this.arena, PASpawn.FIGHT).stream()
                    .map(spawn -> spawn.getName().substring(5))
                    .sorted()
                    .collect(Collectors.joining(", "));

            this.arena.msg(arenaPlayer.getPlayer(), Language.MSG.MODULE_RESPAWNRELAY_CHOICE, spawnNumberList);
        }

        RelayRunnable relayRunnable = new RelayRunnable(this, this.arena, arenaPlayer, deathInfo, ofNullable(keptItems).orElse(new ArrayList<>()));
        this.getRunnerMap().putIfAbsent(arenaPlayer.getName(), relayRunnable);

        return true;
    }
}
