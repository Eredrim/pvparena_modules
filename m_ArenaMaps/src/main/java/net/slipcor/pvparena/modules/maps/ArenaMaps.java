package net.slipcor.pvparena.modules.maps;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.classes.PABlock;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.commands.AbstractArenaCommand;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.goals.AbstractFlagGoal;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.PermissionManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.Arrays.stream;

public class ArenaMaps extends ArenaModule {
    private final HashMap<String, MapView> playerMaps = new HashMap<>();
    private final List<MapElement> mapElements = new ArrayList<>();
    private boolean setup;

    public ArenaMaps() {
        super("ArenaMaps");
    }

    @Override
    public String version() {
        return this.getClass().getPackage().getImplementationVersion();
    }

    @Override
    public boolean checkCommand(final String s) {
        return "!map".equals(s) || "arenamaps".equals(s);
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("arenamaps");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!map");
    }

    public HashMap<String, MapView> getPlayerMaps() {
        return this.playerMaps;
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        Stream.of("true", "false").forEach(val -> {
            result.define(new String[]{"align", val});
            result.define(new String[]{"score", val});
        });

        for (String val : MapElementVisibility.stringValues()) {
            result.define(new String[]{"players", val});
            result.define(new String[]{"blocks", val});
            result.define(new String[]{"spawns", val});
        }
        return result;
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        // !map align
        // !map score
        // !map players
        // !map blocks
        // !map spawns


        if (!PermissionManager.hasAdminPerm(sender) && !PermissionManager.hasBuilderPerm(sender, this.arena)) {
            this.arena.msg(sender, MSG.ERROR_NOPERM, Language.parse(MSG.ERROR_NOPERM_X_ADMIN));
            return;
        }

        if (!AbstractArenaCommand.argCountValid(sender, this.arena, args, new Integer[]{3})) {
            return;
        }

        if ("!map".equals(args[0]) || "arenamaps".equals(args[0])) {
            CFG cfgNode;
            Object newValue;
            if ("align".equals(args[1])) {
                cfgNode = CFG.MODULES_ARENAMAPS_ALIGNTOPLAYER;
                newValue = !this.arena.getConfig().getBoolean(cfgNode);
            } else if ("score".equals(args[1])) {
                cfgNode = CFG.MODULES_ARENAMAPS_SHOWSCORE;
                newValue = !this.arena.getConfig().getBoolean(cfgNode);
            } else {
                if ("players".equals(args[1])) {
                    cfgNode = CFG.MODULES_ARENAMAPS_SHOWPLAYERS;
                } else if ("blocks".equals(args[1])) {
                    cfgNode = CFG.MODULES_ARENAMAPS_SHOWBLOCKS;
                } else if ("spawns".equals(args[1])) {
                    cfgNode = CFG.MODULES_ARENAMAPS_SHOWSPAWNS;
                } else {
                    this.arena.msg(sender, MSG.ERROR_ARGUMENT, args[1], "align | score | players | blocks | spawns");
                    return;
                }

                if(args.length == 3) {
                    String strValue = args[2];
                    try {
                        newValue = MapElementVisibility.valueOf(strValue.toUpperCase()).name();
                    } catch (IllegalArgumentException e) {
                        this.arena.msg(sender, MSG.ERROR_ARGUMENT, strValue, String.join(" | ", MapElementVisibility.stringValues()));
                        return;
                    }
                } else {
                    this.arena.msg(sender, MSG.ERROR_INVALID_ARGUMENT_COUNT, String.valueOf(args.length - 1), "2");
                    return;
                }
            }

            this.arena.getConfig().set(cfgNode, newValue);
            this.arena.getConfig().save();
            this.arena.msg(sender, MSG.CFG_SET_DONE, cfgNode.getNode(), String.valueOf(newValue));
        }
    }

    @Override
    public void displayInfo(final CommandSender sender) {
        Config config = this.arena.getConfig();
        sender.sendMessage(String.format("%s||%s||%s||%s||%s",
                StringParser.colorVar("playerAlign", config.getBoolean(CFG.MODULES_ARENAMAPS_ALIGNTOPLAYER)),
                StringParser.colorVar("showScore", config.getBoolean(CFG.MODULES_ARENAMAPS_SHOWSCORE)),
                String.format("showPlayers : %s", config.getString(CFG.MODULES_ARENAMAPS_SHOWPLAYERS)),
                String.format("showBlocks : %s", config.getString(CFG.MODULES_ARENAMAPS_SHOWBLOCKS)),
                String.format("showSpawns : %s", config.getString(CFG.MODULES_ARENAMAPS_SHOWSPAWNS))
        ));
    }

    public List<MapElement> getMapElements() {
        return this.mapElements;
    }

    void trySetup() {
        if (!this.setup) {
            Bukkit.getPluginManager().registerEvents(new MapListener(this), PVPArena.getInstance());
            this.setup = true;
        }
    }

    @Override
    public void parseJoin(final Player player, final ArenaTeam team) {
        this.trySetup();
        if(this.mapElements.isEmpty()) {
            this.prepareSpawnLocations();
        }
        this.playerMaps.putIfAbsent(player.getName(), null);
        this.mapElements.add(new MapElement(player, team));
    }

    private void prepareSpawnLocations() {
        Set<MapElement> locations = new HashSet<>();

        for (final ArenaTeam team : this.arena.getTeams()) {
            for (final PASpawn spawn : this.arena.getSpawns()) {
                if (team.getName().equals(spawn.getTeamName())) {
                    locations.add(new MapElement(spawn, team));
                }
            }

            if (this.arena.getGoal() instanceof AbstractFlagGoal) {
                for (final PABlock block : this.arena.getBlocks()) {
                    if (block.getTeamName().equals(team.getName()) && block.getName().startsWith("flag")) {
                        locations.add(new MapElement(block, team));
                    }
                }
            }
        }
        this.mapElements.addAll(locations);
    }

    @Override
    public void reset(final boolean force) {
        this.playerMaps.clear();
        this.mapElements.clear();
    }

    /**
     * On respawn, handling two cases:
     * - Player kept map in their inventory (due to respawn config) => do nothing
     * - Player reloaded their inventory => give a filled map and pass saved MapView as ItemMeta
     * @param player  the respawning player
     * @param team    the team he is part of
     * @param cause   the last damage cause
     * @param damager the last damaging entity
     */
    @Override
    public void parseRespawn(final Player player, final ArenaTeam team, final DamageCause cause, final Entity damager) {
        if (player != null && this.arena.hasPlayer(player)) {

            Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), () -> {
                MapView savedMapView = this.playerMaps.get(player.getName());

                if(savedMapView != null) {
                    ItemStack mapItem = new ItemStack(Material.FILLED_MAP, 1);

                    Predicate<ItemStack> hasMapPredicate = iStack -> iStack != null &&
                            iStack.getType() == Material.FILLED_MAP && iStack.getItemMeta() instanceof MapMeta &&
                            savedMapView.equals(((MapMeta) iStack.getItemMeta()).getMapView());
                    boolean shouldGiveNewMap = stream(player.getInventory().getContents()).noneMatch(hasMapPredicate);

                    if(shouldGiveNewMap) {
                        MapMeta mapMeta = (MapMeta) mapItem.getItemMeta();
                        mapMeta.setMapView(savedMapView);
                        mapItem.setItemMeta(mapMeta);
                        player.getInventory().addItem(mapItem);
                    }
                } else {
                    player.getInventory().addItem(new ItemStack(Material.MAP, 1));
                }
            }, 5L);
        }
    }

    /**
     * On game start, all players should get an empty map
     */
    @Override
    public void parseStart() {
        for (final String playerName : this.playerMaps.keySet()) {
            final Player player = Bukkit.getPlayerExact(playerName);
            if (player != null && this.arena.hasPlayer(player)) {
                player.getInventory().addItem(new ItemStack(Material.MAP, 1));
            }
        }
    }
}
