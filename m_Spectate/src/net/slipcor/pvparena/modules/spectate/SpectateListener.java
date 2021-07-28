package net.slipcor.pvparena.modules.spectate;

import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.commands.PAA_Check;
import net.slipcor.pvparena.commands.PAG_Leave;
import net.slipcor.pvparena.loadables.ArenaRegion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static net.slipcor.pvparena.loadables.ArenaRegion.RegionType;
import static org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.SPECTATE;

class SpectateListener implements Listener {
    private Spectate spectate;
    private final Set<Player> spectators = new HashSet<>();

    public SpectateListener(Spectate spectateMod) {
        this.spectate = spectateMod;
    }

    @EventHandler
    public void onPlayerTeleport(final PlayerTeleportEvent event) {
        if(this.spectators.contains(event.getPlayer())) {
            Set<ArenaRegion> regionSet = spectate.getArena().getRegionsByType(RegionType.WATCH);

            if(event.getCause() == SPECTATE && regionSet.size() > 0) {
                boolean inWatchRegion = false;
                for (ArenaRegion region : regionSet) {
                    if (region.getShape().contains(new PABlockLocation(event.getTo()))) {
                        inWatchRegion = true;
                        break;
                    }
                }

                if (!inWatchRegion) {
                    event.setCancelled(true);
                }
            }
        }
    }


    public void addSpectator(final Player spectator) {
        spectators.add(spectator);
    }

    public void removeSpectator(final Player spectator) {
        spectators.remove(spectator);
    }

    public void stop() {
        final Collection<Player> removals = new HashSet<>();
        removals.addAll(spectators);
        for (final Player p : removals) {
            new PAG_Leave().commit(this.spectate.getArena(), p, new String[]{});
        }
        spectators.clear();
    }
}
