package net.slipcor.pvparena.modules.squads;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.commands.AbstractArenaCommand;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.RandomUtils;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.PermissionManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

public class Squads extends ArenaModule {
    private final Set<ArenaSquad> squadList = new HashSet<>();
    private final Map<ArenaSquad, Sign> signMap = new HashMap<>();
    private final static String SQUADS_LIMITS = "modules.squads.limits";

    public Squads() {
        super("Squads");
    }

    @Override
    public String version() {
        return this.getClass().getPackage().getImplementationVersion();
    }

    @Override
    public void configParse(final YamlConfiguration cfg) {
        this.initSquadList(cfg);
    }

    @Override
    public boolean checkCommand(final String s) {
        return "!sq".equalsIgnoreCase(s) || "squads".equalsIgnoreCase(s);
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("squads");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!sq");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        result.define(new String[]{"add"});
        for (final ArenaSquad squad : this.squadList) {
            result.define(new String[]{"remove", squad.getName()});
            result.define(new String[]{"set", squad.getName()});
        }
        return result;
    }

    /**
     *  !sq | show the arena squads
     *  !sq add [name] [limit] | add squad with player limit
     *  !sq remove [name] | remove squad [name]
     *  !sq set [name] [limit] | set player limit for squad
     * @param sender the player committing the command
     * @param args   the command arguments
     */
    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {

        if (!PermissionManager.hasAdminPerm(sender) && !PermissionManager.hasBuilderPerm(sender, this.arena)) {
            this.arena.msg(sender, MSG.ERROR_NOPERM, Language.parse(MSG.ERROR_NOPERM_X_ADMIN));
            return;
        }

        if (!AbstractArenaCommand.argCountValid(sender, this.arena, args, new Integer[]{1, 3, 4})) {
            return;
        }

        if (!this.arena.getConfig().getBoolean(Config.CFG.MODULES_SQUADS_INGAMESWITCH) && this.arena.isFightInProgress()) {
            return;
        }

        if (args == null || args.length > 4 || asList(0, 2).contains(args.length)) {
            this.arena.msg(sender, MSG.MODULE_SQUADS_HELP);
            return;
        }

        if (args.length == 1) {
            // !sq | show the arena squads
            if (this.squadList.isEmpty()) {
                this.arena.msg(sender, MSG.MODULE_SQUADS_NOSQUAD);
            } else {
                this.arena.msg(sender, MSG.MODULE_SQUADS_LISTHEAD, this.arena.getName());
                for (final ArenaSquad squad : this.squadList) {
                    final String max = squad.getMax() > 0 ? String.valueOf(squad.getMax()) : "none";
                    this.arena.msg(sender, MSG.MODULE_SQUADS_LISTITEM, squad.getName(), max);
                }
            }
        }

        if (args.length == 3 || args.length == 4) {
            try {
                if ("add".equalsIgnoreCase(args[1])) {
                    // !sq add [name] [limit] | add squad with player limit
                    ArenaSquad newSquad = new ArenaSquad(args[2], Integer.parseInt(args[3]));
                    this.squadList.add(newSquad);
                    this.arena.msg(sender, MSG.MODULE_SQUADS_ADDED, args[2]);
                } else if ("remove".equalsIgnoreCase(args[1]) || "set".equalsIgnoreCase(args[1])) {
                    // !sq remove [name] | remove squad [name]
                    // /pa !sq set [name] [limit] | set player limit for squad

                    ArenaSquad searchedSquad = this.squadList.stream()
                            .filter(sq -> sq.getName().equalsIgnoreCase(args[2]))
                            .findFirst()
                            .orElseThrow(IllegalArgumentException::new);

                    this.squadList.remove(searchedSquad);
                    if("set".equalsIgnoreCase(args[1])) {
                        ArenaSquad newSquad = new ArenaSquad(args[2], Integer.parseInt(args[3]));
                        this.squadList.add(newSquad);
                        this.arena.msg(sender, MSG.MODULE_SQUADS_SET, args[2]);
                    } else {
                        this.arena.msg(sender, MSG.MODULE_SQUADS_REMOVED, args[2]);
                    }
                } else {
                    throw new IllegalArgumentException();
                }

                // Saving to configuration
                Map<String, Integer> squadsMap = this.squadList.stream().collect(Collectors.toMap(ArenaSquad::getName, ArenaSquad::getMax));
                this.arena.getConfig().setManually(SQUADS_LIMITS, squadsMap);
                this.arena.getConfig().save();
            } catch(IllegalArgumentException e) {
                this.arena.msg(sender, MSG.MODULE_SQUADS_NOTEXIST, args[2]);
            } catch (Exception e) {
                this.arena.msg(sender, MSG.MODULE_SQUADS_ERROR);
                this.arena.msg(sender, MSG.MODULE_SQUADS_HELP);
            }
        }
    }

    @Override
    public void reset(final boolean force) {
        for (final ArenaSquad squad : this.squadList) {
            squad.reset();
        }
        this.squadList.clear();
        this.initSquadList(this.arena.getConfig().getYamlConfiguration());
        this.signMap.values().forEach(this::cleanSign);
        this.signMap.clear();
    }

    @Override
    public void resetPlayer(Player player, boolean soft, boolean force) {
        if (!force) {
            ArenaPlayer ap = ArenaPlayer.fromPlayer(player);
            this.squadList.stream().filter(sq -> sq.contains(ap))
                    .findAny()
                    .ifPresent(squad -> {
                        squad.remove(ap);
                        Sign squadSign = this.signMap.get(squad);
                        if(squadSign != null) {
                            this.setPlayersOnSign(squadSign, squad);
                        }
                    });
        }
    }

    @Override
    public boolean onPlayerInteract(final PlayerInteractEvent event) {
        final ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(event.getPlayer());

        if (!this.arena.equals(arenaPlayer.getArena())) {
            return false;
        }

        if (this.arena.isFightInProgress() && !this.arena.getConfig().getBoolean(Config.CFG.MODULES_SQUADS_INGAMESWITCH)) {
            return false;
        }

        if (EquipmentSlot.OFF_HAND.equals(event.getHand())) {
            return false;
        }

        List<PlayerStatus> disabledStatusList = asList(PlayerStatus.DEAD, PlayerStatus.LOST, PlayerStatus.NULL, PlayerStatus.WARM, PlayerStatus.WATCH);
        if (disabledStatusList.contains(arenaPlayer.getStatus())) {
            return false;
        }

        if (!event.hasBlock() || !(event.getClickedBlock().getState() instanceof Sign)) {
            return false;
        }

        Sign sign = (Sign) event.getClickedBlock().getState();

        return this.useSignToTogglePlayerInSquad(sign, arenaPlayer);
    }

    @Override
    public void parseRespawn(final Player player, final ArenaTeam team,
                             final DamageCause cause, final Entity damager) {

        final ArenaPlayer ap = ArenaPlayer.fromPlayer(player);
        List<ArenaPlayer> squadMates = this.squadList.stream()
                .filter(sq -> sq.contains(ap) && sq.getCount() > 1)
                .flatMap(sq -> sq.getPlayers().stream().filter(squadPlayer -> squadPlayer != ap))
                .collect(Collectors.toList());

        if(!squadMates.isEmpty()) {
            ArenaPlayer randomSquadMate = RandomUtils.getRandom(squadMates, new Random());
            try {
                Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), () -> {
                    ap.setTelePass(true);
                    player.teleport(randomSquadMate.getPlayer());
                    ap.setTelePass(false);
                }, 10);
            } catch (final Exception ignored) {

            }
        }
    }

    private void initSquadList(YamlConfiguration cfg) {
        final ConfigurationSection squadsCfg = cfg.getConfigurationSection(SQUADS_LIMITS);

        Set<ArenaSquad> arenaSquads = new HashSet<>();

        if(squadsCfg != null) {
            arenaSquads = squadsCfg.getKeys(false).stream()
                    .map(name -> new ArenaSquad(name, squadsCfg.getInt(name)))
                    .collect(Collectors.toSet());
        }
        this.squadList.addAll(arenaSquads);
    }

    private boolean useSignToTogglePlayerInSquad(Sign sign, ArenaPlayer arenaPlayer) {
        Optional<ArenaSquad> foundSquad = this.squadList.stream()
                .filter(sq -> sq.getName().equalsIgnoreCase(ChatColor.stripColor(sign.getLine(0))))
                .findAny();

        if(foundSquad.isPresent()) {
            ArenaSquad squad = foundSquad.get();
            if (squad.getMax() != 0 && squad.getCount() >= squad.getMax()) {
                this.arena.msg(arenaPlayer.getPlayer(), MSG.MODULE_SQUADS_FULL);
                return false;
            }
            if(squad.contains(arenaPlayer)) {
                squad.remove(arenaPlayer);
                this.arena.msg(arenaPlayer.getPlayer(), MSG.MODULE_SQUADS_LEAVE, squad.getName());
            } else {
                this.squadList.stream()
                        .filter(sq -> sq.contains(arenaPlayer))
                        .findAny()
                        .ifPresent(previousSquad -> {
                            previousSquad.remove(arenaPlayer);
                            Sign prevSquadSign = this.signMap.get(previousSquad);
                            if(prevSquadSign != null) {
                                this.setPlayersOnSign(prevSquadSign, previousSquad);
                            }
                        });
                squad.add(arenaPlayer);
                this.arena.msg(arenaPlayer.getPlayer(), MSG.MODULE_SQUADS_JOIN, squad.getName());
            }
            this.setPlayersOnSign(sign, squad);
            return true;
        }

        return false;
    }

    private void setPlayersOnSign(Sign sign, ArenaSquad squad) {
        this.signMap.putIfAbsent(squad, sign);

        List<String> playersName = squad.getPlayers().stream()
                .map(ArenaPlayer::getName)
                .collect(Collectors.toList());

        Sign currentSign = sign;
        int lineIndex = 2;
        int playerNameIndex = 0;

        while (currentSign != null) {
            if(playerNameIndex < playersName.size()) {
                currentSign.setLine(lineIndex, playersName.get(playerNameIndex));
                playerNameIndex++;
            } else {
                currentSign.setLine(lineIndex, "");
            }

            if (lineIndex == 3) {
                currentSign.setEditable(false);
                currentSign.update();
                Block block = currentSign.getBlock().getRelative(BlockFace.DOWN);
                if (block.getState() instanceof Sign) {
                    currentSign = (Sign) block.getState();
                    lineIndex = 0;
                } else {
                    currentSign = null;
                }
            } else {
                lineIndex++;
            }
        }
    }

    private void cleanSign(Sign sign) {
        sign.setEditable(true);
        Sign currentSign = sign;
        int currentIndex = 2;

        while (currentSign != null) {
            currentSign.setLine(currentIndex, "");

            if (currentIndex == 3) {
                currentSign.setEditable(true);
                currentSign.update();
                Block block = currentSign.getBlock().getRelative(BlockFace.DOWN);
                if (block.getState() instanceof Sign) {
                    currentSign = (Sign) block.getState();
                    currentIndex = 0;
                } else {
                    currentSign = null;
                }
            } else {
                currentIndex++;
            }
        }
    }
}
