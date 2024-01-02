package net.slipcor.pvparena.modules.startfreeze;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.runnables.ArenaRunnable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static net.slipcor.pvparena.config.Debugger.debug;

class StartFreezer extends ArenaRunnable implements Listener {

    public static final float DEFAULT_WALK_SPEED = 0.2f;
    private final Map<String, Collection<PotionEffect>> effects = new HashMap<>();

    StartFreezer(final Arena arena) {
        super(Language.MSG.MODULE_STARTFREEZE_ANNOUNCE.getNode(),
                arena.getConfig().getInt(Config.CFG.MODULES_STARTFREEZE_TIMER),
                null, arena, false);
        this.arena.getFighters().forEach(arenaPlayer -> {
            Player player = arenaPlayer.getPlayer();
            int ticks = this.seconds * 20;
            this.effects.put(player.getName(), player.getActivePotionEffects());
            player.setNoDamageTicks(ticks);
            player.getActivePotionEffects().forEach(potionEffect -> player.removePotionEffect(potionEffect.getType()));
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, ticks, -7, false, false, false), true);
            player.setWalkSpeed(0);
        });
        Bukkit.getPluginManager().registerEvents(this, PVPArena.getInstance());
    }

    @Override
    protected void warn() {}

    /**
     * the run method, commit start
     */
    @Override
    protected void commit() {
        this.arena.getFighters().forEach(arenaPlayer -> {
            Player player = arenaPlayer.getPlayer();
            player.setWalkSpeed(DEFAULT_WALK_SPEED);
            player.addPotionEffects(this.effects.get(player.getName()));
        });
        this.effects.clear();
        HandlerList.unregisterAll(this);
    }

    @Override
    public void spam() {
        Language.MSG msg = Language.MSG.MODULE_STARTFREEZE_ANNOUNCE;
        final String message = this.seconds > 5 ? Language.parse(msg, String.valueOf(this.seconds)) : MESSAGES.get(this.seconds);

        if (this.arena != null) {
            this.arena.getFighters().forEach(ap -> this.arena.msg(ap.getPlayer(), message));
        }
    }

    @Override
    public synchronized void cancel() throws IllegalStateException {
        super.cancel();
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        ArenaPlayer ap = ArenaPlayer.fromPlayer(event.getPlayer());
        if (ap.getArena() != null && ap.getStatus() == PlayerStatus.FIGHT && this.arena.equals(ap.getArena()) &&
            asList(TeleportCause.ENDER_PEARL, TeleportCause.CHORUS_FRUIT).contains(event.getCause())) {
            debug(ap, "[StartFreeze] cancel teleport");
            event.setCancelled(true);
        }
    }
}
