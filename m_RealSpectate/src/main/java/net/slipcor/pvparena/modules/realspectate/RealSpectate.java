package net.slipcor.pvparena.modules.realspectate;


import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.classes.PALocation;
import net.slipcor.pvparena.commands.PAG_Leave;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.exceptions.GameplayException;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.loadables.ModuleType;
import net.slipcor.pvparena.modules.realspectate.runnables.InitSpectateRunnable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;
import static net.slipcor.pvparena.config.Debugger.debug;

public class RealSpectate extends ArenaModule {
    public RealSpectate() {
        super("RealSpectate");
    }

    private RealSpectateListener listener;
    private final Map<Player, Set<Player>> fighterWithSpectators = new HashMap<>();
    private final Set<Player> spectators = new HashSet<>();

    private static final int PRIORITY = 2;

    @Override
    public String version() {
        return this.getClass().getPackage().getImplementationVersion();
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public ModuleType getType() {
        return ModuleType.SPECTATE;
    }

    @Override
    public boolean handleSpectate(Player p) throws GameplayException {
        if (this.arena.getFighters().isEmpty()) {
            throw new GameplayException(MSG.ERROR_NOPLAYERFOUND);
        }
        return true;
    }

    @Override
    public void commitSpectate(final Player player) {
        debug(player, this,"Commit spectate");

        this.initListenerIfNeeded();

        final ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);
        arenaPlayer.setLocation(new PALocation(arenaPlayer.getPlayer().getLocation()));

        arenaPlayer.setArena(this.arena);
        arenaPlayer.setStatus(PlayerStatus.WATCH);
        arenaPlayer.setSpectating(true);
        arenaPlayer.setTelePass(true);

        if (arenaPlayer.getState() == null) {

            // Important: clear inventory before setting player state to deal with armor modifiers (like health)
            ArenaPlayer.backupAndClearInventory(this.arena, player);
            arenaPlayer.createState(player);
            arenaPlayer.dump();
        }

        player.setCollidable(false);
        this.arena.msg(player, MSG.MODULE_REALSPECTATE_INFO);
        this.switchPlayer(player, null, true);
    }

    @Override
    public void switchToSpectate(Player player) {
        debug(player, this,"switch to spectate");

        this.initListenerIfNeeded();

        ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);
        arenaPlayer.setSpectating(true);
        arenaPlayer.setTelePass(true);
        player.setCollidable(false);
        this.arena.msg(player, MSG.MODULE_REALSPECTATE_INFO);
        this.switchPlayer(player, null, true);
    }

    @Override
    public void reset(final boolean force) {
        if(this.listener != null) {
            this.spectators.clear();
            this.fighterWithSpectators.clear();
            HandlerList.unregisterAll(this.listener);
            this.listener = null;
        }
    }

    @Override
    public void resetPlayer(Player player, boolean soft, boolean force) {
        if (this.spectators.contains(player)) {
            player.setInvisible(false);
            player.setCollidable(true);
            this.spectators.remove(player);
            this.fighterWithSpectators.forEach((fighter, spectators) -> {
                if (spectators.contains(player)) {
                    player.showPlayer(PVPArena.getInstance(), fighter);
                    spectators.remove(player);
                }
            });
            Bukkit.getScheduler().runTask(PVPArena.getInstance(), () -> {
                Bukkit.getOnlinePlayers().forEach(p -> p.showPlayer(PVPArena.getInstance(), player));
            });
        } else if (this.fighterWithSpectators.containsKey(player)) {
            this.fighterWithSpectators.get(player).forEach(spec -> this.switchPlayer(spec, player, true));
        }
    }

    void switchPlayer(Player spectator, Player currentSubject, boolean forward) {

        List<Player> fighterList = this.getArena().getFighters().stream()
                .filter(ap -> !ap.isSpectating())
                .map(ArenaPlayer::getPlayer)
                .sorted(Comparator.comparing(Player::getName))
                .collect(Collectors.toList());

        if (currentSubject != null && fighterList.size() <= 1) {
            debug(spectator, this, "No fighters to spectate, leaving arena");
            new PAG_Leave().commit(this.getArena(), spectator, new String[0]);
            return;
        }

        Player nextPlayer;

        final int currentSubjectIndex = ofNullable(currentSubject).map(fighterList::indexOf).orElse(-1);
        if (currentSubjectIndex == -1 || (currentSubjectIndex == fighterList.size() - 1 && forward)) {
            nextPlayer = fighterList.get(0);
        } else if (currentSubjectIndex == 0 && !forward) {
            nextPlayer = fighterList.get(fighterList.size() - 1);
        } else {
            nextPlayer = fighterList.get(currentSubjectIndex + (forward ? 1 : -1));
        }

        if (currentSubject != null) {
            spectator.showPlayer(PVPArena.getInstance(), currentSubject);
            this.fighterWithSpectators.get(currentSubject).remove(spectator);
        } else {
            Bukkit.getOnlinePlayers().forEach(p -> p.hidePlayer(PVPArena.getInstance(), spectator));
        }

        this.addNewSpectatorToFighter(spectator, nextPlayer);
    }

    private void addNewSpectatorToFighter(Player spectator, Player fighter) {
        debug(spectator, this, "Player is now spectating {}", fighter.getName());

        Set<Player> specListForFighter = this.fighterWithSpectators.get(fighter);
        if (specListForFighter == null) {
            this.fighterWithSpectators.put(fighter, Stream.of(spectator).collect(Collectors.toSet()));
        } else {
            specListForFighter.add(spectator);
        }
        this.spectators.add(spectator);

        new InitSpectateRunnable(spectator, fighter).runTaskLater(PVPArena.getInstance(), 5L);
    }

    private void initListenerIfNeeded() {
        if (this.listener == null) {
            this.listener = new RealSpectateListener(this, this.spectators, this.fighterWithSpectators);
            Bukkit.getPluginManager().registerEvents(this.listener, PVPArena.getInstance());
        }
    }

}
