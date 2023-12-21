package net.slipcor.pvparena.modules.playerfinder;

import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.loadables.ArenaModule;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Comparator.comparingDouble;
import static net.slipcor.pvparena.config.Debugger.debug;
import static net.slipcor.pvparena.config.Debugger.trace;
import static org.bukkit.event.block.Action.*;

public class PlayerFinder extends ArenaModule {
    public PlayerFinder() {
        super("PlayerFinder");
    }

    @Override
    public String version() {
        return this.getClass().getPackage().getImplementationVersion();
    }

    @Override
    public boolean onPlayerInteract(PlayerInteractEvent event) {
        final Player player = event.getPlayer();

        final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(player);

        if (!aPlayer.getArena().equals(this.arena)) {
            debug(player, "[PlayerFinder] Wrong arena!");
            return false;
        }

        if (player.getInventory().getItemInMainHand().getType() != Material.COMPASS) {
            debug(player, "[PlayerFinder] No compass!");
            return false;
        }

        if (!asList(LEFT_CLICK_AIR, LEFT_CLICK_BLOCK, RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK).contains(event.getAction())) {
            debug(player, "[PlayerFinder] No compass!");
            return false;
        }

        trace(player, "[PlayerFinder] ok!");
        final int maxRadius = this.arena.getConfig().getInt(CFG.MODULES_PLAYERFINDER_MAXRADIUS);

        Optional<Player> nearestPlayer = player.getNearbyEntities(maxRadius, maxRadius, maxRadius)
                .stream()
                .filter(e -> e instanceof Player && e != player)
                .map(e -> (Player) e)
                .filter(innerPlayer -> {
                    ArenaPlayer ap = ArenaPlayer.fromPlayer(innerPlayer);
                    return ap.getStatus() == PlayerStatus.FIGHT &&
                            (this.arena.isFreeForAll() || !ap.getArenaTeam().equals(aPlayer.getArenaTeam()));
                })
                .peek(innerPlayer -> trace(player, "[PlayerFinder] player found: {}", innerPlayer.getName()))
                .min(comparingDouble(innerPlayer -> player.getLocation().distance(innerPlayer.getLocation())));

        if (nearestPlayer.isPresent()) {
            if (event.getAction() == LEFT_CLICK_AIR || event.getAction() == LEFT_CLICK_BLOCK) {
                debug("[PlayerFinder] left click with compass");
                player.setCompassTarget(nearestPlayer.get().getLocation().clone());
                this.arena.msg(player, MSG.MODULE_PLAYERFINDER_POINT, nearestPlayer.get().getName());

            } else {
                debug("[PlayerFinder] right click with compass");
                double distance = player.getLocation().distance(nearestPlayer.get().getLocation());
                this.arena.msg(player, MSG.MODULE_PLAYERFINDER_NEAR, String.valueOf((int) distance));

            }
            event.setCancelled(true);
            return true;

        } else {
            debug(player, "[PlayerFinder] none there!");
            event.setCancelled(true);
            return false;
        }
    }
}
