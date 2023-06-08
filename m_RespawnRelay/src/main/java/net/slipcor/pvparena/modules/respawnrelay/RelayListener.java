package net.slipcor.pvparena.modules.respawnrelay;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.managers.SpawnManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Set;

class RelayListener implements Listener {
    private final RespawnRelay module;

    public RelayListener(RespawnRelay module) {
        this.module = module;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onAsyncChat(final AsyncPlayerChatEvent event) {
        final ArenaPlayer player = ArenaPlayer.fromPlayer(event.getPlayer());

        Arena arena = player.getArena();
        if (!this.module.getArena().equals(arena) || !this.module.getRunnerMap().containsKey(player.getName())) {
            return;
        }

        event.setCancelled(true);

        String maybeNextSpawn = PASpawn.FIGHT + event.getMessage();
        Set<PASpawn> potentialSpawns = SpawnManager.getPASpawnsStartingWith(arena, maybeNextSpawn);

        if (!potentialSpawns.isEmpty()) {
            this.module.getSpawnPointOverrideMap().put(player.getName(), maybeNextSpawn);
            arena.msg(event.getPlayer(), Language.MSG.MODULE_RESPAWNRELAY_CHOSEN);
        }
    }
}
