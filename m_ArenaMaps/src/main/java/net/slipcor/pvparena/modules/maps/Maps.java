package net.slipcor.pvparena.modules.maps;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.commands.AbstractArenaCommand;
import net.slipcor.pvparena.commands.CommandTree;
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
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class Maps extends ArenaModule {
    private HashSet<String> mappings = new HashSet<>();
    private HashSet<MapItem> items = new HashSet<>();
    private boolean setup;

    public Maps() {
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

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        result.define(new String[]{"align", "true"});
        result.define(new String[]{"align", "false"});
        result.define(new String[]{"lives", "true"});
        result.define(new String[]{"lives", "false"});
        result.define(new String[]{"players", "true"});
        result.define(new String[]{"players", "false"});
        result.define(new String[]{"spawns", "true"});
        result.define(new String[]{"spawns", "false"});
        return result;
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        // !map align
        // !map lives
        // !map players
        // !map spawns


        if (!PermissionManager.hasAdminPerm(sender) && !PermissionManager.hasBuilderPerm(sender, this.arena)) {
            this.arena.msg(sender, MSG.ERROR_NOPERM, Language.parse(MSG.ERROR_NOPERM_X_ADMIN));
            return;
        }

        if (!AbstractArenaCommand.argCountValid(sender, this.arena, args, new Integer[]{2})) {
            return;
        }

        if ("!map".equals(args[0]) || "arenamaps".equals(args[0])) {
            CFG c = null;
            if ("align".equals(args[1])) {
                c = CFG.MODULES_ARENAMAPS_ALIGNTOPLAYER;
            }
            if ("lives".equals(args[1])) {
                c = CFG.MODULES_ARENAMAPS_SHOWLIVES;
            }
            if ("players".equals(args[1])) {
                c = CFG.MODULES_ARENAMAPS_SHOWPLAYERS;
            }
            if ("spawns".equals(args[1])) {
                c = CFG.MODULES_ARENAMAPS_SHOWSPAWNS;

            }
            if (c == null) {

                this.arena.msg(sender, MSG.ERROR_ARGUMENT, args[1], "align | lives | players | spawns");
                return;
            }
            final boolean b = this.arena.getConfig().getBoolean(c);
            this.arena.getConfig().set(c, !b);
            this.arena.getConfig().save();
            this.arena.msg(sender, MSG.SET_DONE, c.getNode(), String.valueOf(!b));
        }
    }

    @Override
    public void displayInfo(final CommandSender sender) {
        sender.sendMessage(StringParser.colorVar("playerAlign",
                this.arena.getConfig().getBoolean(
                        CFG.MODULES_ARENAMAPS_ALIGNTOPLAYER)) + "||" +
                StringParser.colorVar("showLives",
                        this.arena.getConfig().getBoolean(
                                CFG.MODULES_ARENAMAPS_SHOWLIVES)) + "||" +
                StringParser.colorVar("showPlayers",
                        this.arena.getConfig().getBoolean(
                                CFG.MODULES_ARENAMAPS_SHOWPLAYERS)) + "||" +
                StringParser.colorVar("showSpawns",
                        this.arena.getConfig().getBoolean(
                                CFG.MODULES_ARENAMAPS_SHOWSPAWNS)));
    }

    public HashSet<MapItem> getItems() {
        return this.items;
    }

    void trySetup() {
        if (this.setup) {
            return;
        }
        Bukkit.getPluginManager().registerEvents(new MapListener(this), PVPArena.getInstance());
        this.setup = true;
    }

    @Override
    public void parseJoin(final Player player, final ArenaTeam team) {
        this.trySetup();
        final HashSet<String> maps;
        if (this.mappings.isEmpty()) {
            maps = new HashSet<>();
            this.prepareSpawnLocations();
        } else {
            maps = this.mappings;
        }

        maps.add(player.getName());

        this.items.add(new MapItem(player, team.getColor()));
        this.mappings = maps;
    }

    private void prepareSpawnLocations() {
        if (!this.items.isEmpty()) {
            this.items.clear();
            // recalculate, in case admin added stuff
        }

        final HashSet<MapItem> locations = new HashSet<>();

        for (final ArenaTeam team : this.arena.getTeams()) {
            for (final PASpawn spawn : this.arena.getSpawns()) {
                if (spawn.getTeamName().equals(team.getName())) {
                    locations.add(new MapItem(new PABlockLocation(spawn.getPALocation().toLocation()), team.getColor()));
                }
            }

            if (this.arena.getGoal() instanceof AbstractFlagGoal) {
                for (final PASpawn spawn : this.arena.getSpawns()) {
                    if (spawn.getTeamName().equals(team.getName()) && spawn.getName().startsWith("flag")) {
                        locations.add(new MapItem(new PABlockLocation(spawn.getPALocation().toLocation()), team.getColor()));
                    }
                }
            }
        }
        this.items = locations;
    }

    @Override
    public void reset(final boolean force) {
        this.mappings.remove(this.arena.getName());
    }

    @Override
    public void parseRespawn(final Player player, final ArenaTeam team, final DamageCause cause, final Entity damager) {
        if (player == null) {
            return;
        }
        if (!this.arena.hasPlayer(player)) {
            return;
        }

        class RunLater implements Runnable {

            @Override
            public void run() {
                final Short value = MyRenderer.getId(player.getName());
                player.getInventory().addItem(new ItemStack(Material.MAP, 1, value));
                Maps.this.mappings.add(player.getName());
                if (value != Short.MIN_VALUE) {
                    final MapView map = Bukkit.getMap(value);

                    final MapRenderer mr = new MyRenderer(Maps.this);
                    map.addRenderer(mr);
                }
            }
        }
        Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), new RunLater(), 5L);
    }

    @Override
    public void parseStart() {

        if (this.mappings.isEmpty()) {
            return;
        }
        for (final String playerName : this.mappings) {
            final Player player = Bukkit.getPlayerExact(playerName);
            if (player == null) {
                continue;
            }
            if (!this.arena.hasPlayer(player)) {
                continue;
            }
            final Short value = MyRenderer.getId(playerName);
            player.getInventory().addItem(new ItemStack(Material.MAP, 1, value));
            this.mappings.add(player.getName());
            if (value != Short.MIN_VALUE) {
                final MapView map = Bukkit.getServer().getMap(value);
                if (map == null) {
                    PVPArena.getInstance().getLogger().severe("Map #"+value+" seems to be corrupted, please check the PVP Arena config for this value!");
                    PVPArena.getInstance().getLogger().severe("Affected player: "+player.getName());
                    continue;
                }
                final MapRenderer mr = new MyRenderer(this);
                map.addRenderer(mr);
            }
        }
    }

    public boolean hasCustomMap(final String sPlayer) {
        return this.mappings.contains(sPlayer);
    }
}
