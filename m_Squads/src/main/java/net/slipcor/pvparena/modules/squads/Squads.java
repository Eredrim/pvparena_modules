package net.slipcor.pvparena.modules.squads;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaPlayer.Status;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.commands.AbstractArenaCommand;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.loadables.ArenaModule;
import org.bukkit.Bukkit;
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

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

public class Squads extends ArenaModule {
    private final Map<Arena, Set<ArenaSquad>> squadsMap = new HashMap<>();
    private final Map<Sign, ArenaSquad> signs = new HashMap<>();
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
        final ConfigurationSection squadsCfg = cfg.getConfigurationSection(SQUADS_LIMITS);

        Set<ArenaSquad> arenaSquads = new HashSet<>();

        if(squadsCfg != null) {
            for (final String name : squadsCfg.getKeys(false)) {
                final ArenaSquad squad = new ArenaSquad(name, squadsCfg.getInt(name));
                arenaSquads.add(squad);
            }
        }
        this.squadsMap.putIfAbsent(this.arena, arenaSquads);
    }

    @Override
    public boolean checkCommand(final String s) {
        return "!sq".equals(s) || "squad".equals(s);
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
        for (final ArenaSquad squad : this.getArenaSquads()) {
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

        if (!PVPArena.hasAdminPerms(sender) && !PVPArena.hasCreatePerms(sender, this.arena)) {
            this.arena.msg(sender,
                    Language.parse(MSG.ERROR_NOPERM, Language.parse(MSG.ERROR_NOPERM_X_ADMIN)));
            return;
        }

        if (!AbstractArenaCommand.argCountValid(sender, this.arena, args, new Integer[]{1, 3, 4})) {
            return;
        }

        if (!this.arena.getArenaConfig().getBoolean(Config.CFG.MODULES_SQUADS_INGAMESWITCH) && this.arena.isFightInProgress()) {
            return;
        }

        if (args == null || args.length > 4 || asList(0, 2).contains(args.length)) {
            this.arena.msg(sender, Language.parse(MSG.MODULE_SQUADS_HELP));
            return;
        }

        if (args.length == 1) {
            // !sq | show the arena squads
            if (this.getArenaSquads().size() < 1) {
                this.arena.msg(sender, Language.parse(MSG.MODULE_SQUADS_NOSQUAD));
            } else {
                this.arena.msg(sender, Language.parse(MSG.MODULE_SQUADS_LISTHEAD, this.arena.getName()));
                for (final ArenaSquad squad : this.getArenaSquads()) {
                    final String suffix = squad.getName().equalsIgnoreCase(this.arena.getArenaConfig().getString(Config.CFG.MODULES_SQUADS_AUTOSQUAD)) ? " (auto)" : "";
                    final String max = squad.getMax() > 0 ? String.valueOf(squad.getMax()) : "none";
                    this.arena.msg(sender, Language.parse(MSG.MODULE_SQUADS_LISTITEM, squad.getName(), max, suffix));
                }
            }
        }

        if (args.length == 3 || args.length == 4) {
            try {
                if ("add".equalsIgnoreCase(args[1])) {
                    // !sq add [name] [limit] | add squad with player limit
                    ArenaSquad newSquad = new ArenaSquad(args[2], Integer.parseInt(args[3]));
                    this.getArenaSquads().add(newSquad);
                    this.arena.msg(sender, Language.parse(MSG.MODULE_SQUADS_ADDED, args[2]));
                } else if ("remove".equalsIgnoreCase(args[1]) || "set".equalsIgnoreCase(args[1])) {
                    // !sq remove [name] | remove squad [name]
                    // /pa !sq set [name] [limit] | set player limit for squad

                    ArenaSquad searchedSquad = this.getArenaSquads().stream()
                            .filter(sq -> sq.getName().equalsIgnoreCase(args[2]))
                            .findFirst()
                            .orElseThrow(IllegalArgumentException::new);

                    this.getArenaSquads().remove(searchedSquad);
                    if("set".equalsIgnoreCase(args[1])) {
                        ArenaSquad newSquad = new ArenaSquad(args[2], Integer.parseInt(args[3]));
                        this.getArenaSquads().add(newSquad);
                        this.arena.msg(sender, Language.parse(MSG.MODULE_SQUADS_SET, args[2]));
                    } else {
                        this.arena.msg(sender, Language.parse(MSG.MODULE_SQUADS_REMOVED, args[2]));
                    }
                } else {
                    throw new IllegalArgumentException();
                }

                // Saving to configuration
                Map<String, Integer> squadsMap = this.getArenaSquads().stream().collect(Collectors.toMap(ArenaSquad::getName, ArenaSquad::getMax));
                this.arena.getArenaConfig().setManually(SQUADS_LIMITS, squadsMap);
                this.arena.getArenaConfig().save();
            } catch(IllegalArgumentException e) {
                this.arena.msg(sender, Language.parse(MSG.MODULE_SQUADS_NOTEXIST, args[2]));
            } catch (Exception e) {
                this.arena.msg(sender, Language.parse(MSG.MODULE_SQUADS_ERROR));
                this.arena.msg(sender, Language.parse(MSG.MODULE_SQUADS_HELP));
            }
        }
    }

    @Override
    public void reset(final boolean force) {
        for (final ArenaSquad squad : this.getArenaSquads()) {
            squad.reset();
        }
        for (final Sign sign : this.signs.keySet()) {
            sign.setLine(1, "----------");
            sign.setLine(2, "");
            sign.setLine(3, "");
        }
        this.signs.clear();
    }

    @Override
    public boolean onPlayerInteract(final PlayerInteractEvent event) {
        final ArenaPlayer arenaPlayer = ArenaPlayer.parsePlayer(event.getPlayer().getName());

        if (!this.arena.equals(arenaPlayer.getArena())) {
            return false;
        }

        if (this.arena.isFightInProgress() && !this.arena.getArenaConfig().getBoolean(
                Config.CFG.MODULES_SQUADS_INGAMESWITCH)) {
            return false;
        }

        if (EquipmentSlot.OFF_HAND.equals(event.getHand())) {
            return false;
        }

        List<Status> disabledStatusList = asList(Status.DEAD, Status.LOST, Status.NULL, Status.WARM, Status.WATCH);
        if (disabledStatusList.contains(arenaPlayer.getStatus())) {
            return false;
        }

        if (!event.hasBlock() || !(event.getClickedBlock().getState() instanceof Sign)) {
            return false;
        }

        final Sign sign = (Sign) event.getClickedBlock().getState();

        for (final ArenaSquad squad : this.getArenaSquads()) {
            if (squad.getName().equals(sign.getLine(0))) {
                if (squad.getMax() != 0 && squad.getCount() >= squad.getMax()) {
                    this.arena.msg(arenaPlayer.getPlayer(), Language.parse(MSG.MODULE_SQUADS_FULL));
                    return false;
                }
                for (final ArenaSquad s : this.getArenaSquads()) {
                    if (s.equals(squad)) {
                        continue;
                    }
                    if (s.contains(arenaPlayer)) {
                        s.remove(arenaPlayer);
                        this.removePlayerFromSigns(arenaPlayer);
                        break;
                    }
                }
                squad.add(arenaPlayer);
                this.addPlayerToSigns(sign, arenaPlayer);
                return true;
            }
        }

        return false;
    }

    private Set<ArenaSquad> getArenaSquads() {
        return this.squadsMap.getOrDefault(this.arena, Collections.emptySet());
    }

    private void addPlayerToSigns(final Sign sign, final ArenaPlayer ap) {
        for (int i = 2; i < 4; i++) {
            if (sign.getLine(i).isEmpty()) {
                sign.setLine(i, ap.getName());
                sign.update();
                return;
            }
        }

        final Block block = sign.getBlock().getRelative(BlockFace.DOWN);

        if (block.getState() instanceof Sign) {
            final Sign sign2 = (Sign) block.getState();
            for (int i = 2; i < 4; i++) {
                if (sign2.getLine(i).isEmpty()) {
                    sign2.setLine(i, ap.getName());
                    sign2.update();
                    return;
                }
            }
        }
    }

    private void removePlayerFromSigns(final ArenaPlayer ap) {
        for (final Sign sign : this.signs.keySet()) {
            for (int i = 0; i < 4; i++) {
                if (ap.getName().equals(sign.getLine(i))) {
                    sign.setLine(i, "");
                }
            }
            sign.update();

            final Block block = sign.getBlock().getRelative(BlockFace.DOWN);

            if (block.getState() instanceof Sign) {
                final Sign sign2 = (Sign) block.getState();
                for (int i = 2; i < 4; i++) {
                    if (ap.getName().equals(sign2.getLine(i))) {
                        sign2.setLine(i, "");
                    }
                }
                sign2.update();
            }
        }
    }

    @Override
    public void parseJoin(final CommandSender sender, final ArenaTeam team) {
        String autoSquadName = this.arena.getArenaConfig().getString(Config.CFG.MODULES_SQUADS_AUTOSQUAD);
        try {
            ArenaSquad autoSquad = this.getArenaSquads().stream()
                    .filter(sq -> sq.getName().equalsIgnoreCase(autoSquadName))
                    .findFirst()
                    .orElseThrow(IllegalArgumentException::new);
            autoSquad.add(ArenaPlayer.parsePlayer(sender.getName()));
        } catch (Exception ignored) {

        }
    }

    @Override
    public void parseRespawn(final Player player, final ArenaTeam team,
                             final DamageCause cause, final Entity damager) {

        final ArenaPlayer ap = ArenaPlayer.parsePlayer(player.getName());
        for (final ArenaSquad squad : this.getArenaSquads()) {
            if (squad.contains(ap) && squad.getCount() > 1) {
                int pos = new Random().nextInt(squad.getCount() - 1);
                for (final ArenaPlayer tap : squad.getPlayers()) {
                    if (--pos <= 0) {
                        try {
                            Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), () -> player.teleport(tap.getPlayer()), 10);
                        } catch (final Exception ignored) {

                        }
                    }
                }
            }
        }
    }
}
