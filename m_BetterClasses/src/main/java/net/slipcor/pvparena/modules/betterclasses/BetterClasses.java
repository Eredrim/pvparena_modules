package net.slipcor.pvparena.modules.betterclasses;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.*;
import net.slipcor.pvparena.commands.AbstractArenaCommand;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.loadables.ArenaModule;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Field;
import java.util.*;

import static net.slipcor.pvparena.config.Debugger.debug;

public class BetterClasses extends ArenaModule {

    private final Map<Arena, HashMap<ArenaClass, HashSet<PotionEffect>>> superMap = new HashMap<>();

    public BetterClasses() {
        super("BetterClasses");
    }

    @Override
    public String version() {
        return getClass().getPackage().getImplementationVersion();
    }

    private Map<ArenaTeam, Integer> teamSwitches = new HashMap<>();
    private Map<ArenaPlayer, Integer> playerSwitches = new HashMap<>();

    @Override
    public boolean cannotSelectClass(final Player player,
                                     final String className) {
        debug(arena, player, "checking if cannotSelectClass");
        if (notEnoughEXP(player, className)) {
            arena.msg(player, Language.parse(MSG.ERROR_CLASS_NOTENOUGHEXP, className));
            return true;
        }

        int max;
        int globalmax;

        try {
            max = (Integer) arena.getArenaConfig().getUnsafe("modules.betterclasses.maxPlayers." + className);
            globalmax = (Integer) arena.getArenaConfig().getUnsafe("modules.betterclasses.maxGlobalPlayers." + className);
        } catch (final Exception e) {
            debug(arena, "Exception at BetterClasses.class getting " + className +" config:" + e.getMessage() + " at line "+e.getStackTrace()[1].getLineNumber());
            max = 0;
            globalmax = 0;
        }
        debug(arena, player, "max: " +max);
        debug(arena, player, "gmax: "+globalmax);

        if (!(max < 1 && globalmax < 1)) {
            final ArenaTeam team = ArenaPlayer.parsePlayer(player.getName()).getArenaTeam();

            if (team == null) {
                debug(arena, player, "arenaTeam NULL: "+player.getName());
                return true;
            }
            int globalsum = 0;
            int sum = 0;
            for (final ArenaTeam ateam : arena.getTeams()) {
                for (final ArenaPlayer ap : ateam.getTeamMembers()) {
                    if (ap.getArenaClass() == null) {
                        continue;
                    }
                    if (ap.getArenaClass().getName().equals(className)) {
                        globalsum++;
                        if (team.equals(ateam)) {
                            sum++;
                        }
                    }
                }
            }
            debug(arena, player, "sum: " +sum);

            if ((max > 0 && sum >= max) || (globalmax > 0 && globalsum > globalmax)) {
                if (sum >= max) {
                    debug(arena, player, sum + ">="+max);
                }   else {
                    debug(arena, player, globalsum + ">"+globalmax);
                }
                arena.msg(player, Language.parse(MSG.ERROR_CLASS_FULL, className));
                return true;
            }
        }

        for (ArenaTeam at : teamSwitches.keySet()) {
            if (!at.hasPlayer(player)) {
                continue;
            }
            if (teamSwitches.get(at) == 0) {
                arena.msg(player, Language.parse(MSG.MODULE_BETTERCLASSES_CLASSCHANGE_MAXTEAM));
                return true;
            } else {
                debug(arena, player, "teamswitches of "+at.getName()+": " +teamSwitches.get(at));
            }
        }

        for (ArenaPlayer ap : playerSwitches.keySet()) {
            if (ap.getName().equals(player.getName())) {
                if (playerSwitches.get(ap) == 0) {
                    arena.msg(player, Language.parse(MSG.MODULE_BETTERCLASSES_CLASSCHANGE_MAXPLAYER));
                    return true;
                } else {
                    debug(arena, player, "playerswitches: " +playerSwitches.get(ap));
                }
            }
        }

        return false;
    }

    @Override
    public boolean checkCommand(final String s) {
        return "!bc".equals(s) || s.startsWith("betterclass");
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("betterclasses");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!bc");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        if (arena == null) {
            return result;
        }
        for (final ArenaClass aClass : arena.getClasses()) {
            result.define(new String[]{aClass.getName(), "add"});
            result.define(new String[]{aClass.getName(), "clear"});
            result.define(new String[]{aClass.getName(), "set", "exp"});
            result.define(new String[]{aClass.getName(), "set", "max"});
            result.define(new String[]{aClass.getName(), "respawncommand"});
            result.define(new String[]{aClass.getName(), "remove", "{PotionEffectType}"});
        }
        return result;
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        // !bc [classname] | show
        // !bc [classname] add [def]| add
        // !bc [classname] remove [type] | remove
        // !bc [classname] clear | clear

        // !bc [classname] set exp [level]
        // !bc [classname] set max [count]

        if (!PVPArena.hasAdminPerms(sender)
                && !PVPArena.hasCreatePerms(sender, arena)) {
            arena.msg(sender,
                    Language.parse(MSG.ERROR_NOPERM, Language.parse(MSG.ERROR_NOPERM_X_ADMIN)));
            return;
        }

        if ((!(args.length > 2 && "respawncommand".equals(args[2])) || args.length < 3)  &&
                !AbstractArenaCommand.argCountValid(sender, arena, args, new Integer[]{2, 3, 4, 5})) {
            return;
        }

        final ArenaClass c = arena.getClass(args[1]);

        if (c == null) {
            arena.msg(sender, Language.parse(MSG.ERROR_CLASS_NOT_FOUND, args[1]));
            return;
        }

        if (args.length == 5 && "set".equals(args[2])) {
            final int value;
            try {
                value = Integer.parseInt(args[4]);
            } catch (final Exception e) {
                arena.msg(sender, Language.parse(MSG.ERROR_NOT_NUMERIC, args[4]));
                return;
            }

            if ("exp".equalsIgnoreCase(args[3])) {
                final String node = "modules.betterclasses.neededEXPLevel." + c.getName();
                arena.getArenaConfig().setManually(node, value);
                arena.msg(sender, Language.parse(MSG.SET_DONE, node, String.valueOf(value)));
            } else if ("max".equalsIgnoreCase(args[3])) {
                final String node = "modules.betterclasses.maxPlayers." + c.getName();
                arena.getArenaConfig().setManually(node, value);
                arena.msg(sender, Language.parse(MSG.SET_DONE, node, String.valueOf(value)));
            } else if ("globalmax".equalsIgnoreCase(args[3])) {
                final String node = "modules.betterclasses.maxGlobalPlayers." + c.getName();
                arena.getArenaConfig().setManually(node, value);
                arena.msg(sender, Language.parse(MSG.SET_DONE, node, String.valueOf(value)));
            } else {
                return;
            }
            arena.getArenaConfig().save();
            return;
        } else if (args.length > 2 && "respawncommand".equals(args[2])) {
            final String node = "modules.betterclasses.respawnCommand." + c.getName();
            if (args.length == 3) {
                arena.getArenaConfig().setManually(node, null);
                arena.msg(sender, Language.parse(MSG.MODULE_BETTERCLASSES_RESPAWNCOMMAND_REMOVED, node, c.getName()));
            } else {
                String command = StringParser.joinArray(StringParser.shiftArrayBy(args, 3), " ");
                arena.getArenaConfig().setManually(node, command);
                arena.msg(sender, Language.parse(MSG.SET_DONE, node, command));
            }
            arena.getArenaConfig().save();
            return;
        }


        HashSet<PotionEffect> ape = new HashSet<>();

        final String s = (String) arena.getArenaConfig().getUnsafe("modules.betterclasses.permEffects." + c.getName());
        if (s != null) {
            ape = parseStringToPotionEffects(s);
        }

        if (args.length < 3) {
            // !bc [classname] | show
            arena.msg(sender, Language.parse(MSG.MODULE_BETTERCLASSES_LISTHEAD, c.getName()));
            if (ape.size() >= 1) {
                for (final PotionEffect pe : ape) {
                    arena.msg(sender, pe.getType().getName() + 'x' + pe.getAmplifier());
                }
            } else {
                arena.msg(sender, "---");
            }
            return;
        }

        if (args.length < 4) {
            // !bc [classname] clear | clear
            if (!"clear".equals(args[2])) {
                printHelp(arena, sender);
                return;
            }

            arena.getArenaConfig().setManually("modules.betterclasses.permEffects." + c.getName(), "none");
            arena.getArenaConfig().save();
            arena.msg(sender, Language.parse(MSG.MODULE_BETTERCLASSES_CLEAR, c.getName()));
            return;
        }

        if ("add".equals(args[2])) {
            // 0   1           2      3     4
            // !bc [classname] add    [def] [amp]| add
            PotionEffectType pet = null;

            for (final PotionEffectType x : PotionEffectType.values()) {
                if (x == null) {
                    continue;
                }
                if (x.getName().equalsIgnoreCase(args[3])) {
                    pet = x;
                    break;
                }
            }

            if (pet == null) {
                arena.msg(sender, Language.parse(MSG.ERROR_POTIONEFFECTTYPE_NOTFOUND, args[3]));
                return;
            }

            int amp = 1;

            if (args.length == 5) {
                try {
                    amp = Integer.parseInt(args[4]);
                } catch (final Exception e) {
                    arena.msg(sender, Language.parse(MSG.ERROR_NOT_NUMERIC, args[4]));
                    return;
                }
            }

            ape.add(new PotionEffect(pet, Integer.MAX_VALUE, amp));
            arena.getArenaConfig().setManually("modules.betterclasses.permEffects." + c.getName(), parsePotionEffectsToString(ape));
            arena.getArenaConfig().save();
            arena.msg(sender, Language.parse(MSG.MODULE_BETTERCLASSES_ADD, c.getName(), pet.getName()));
            return;
        }
        if (args[2].equals("remove")) {
            // 0   1           2      3
            // !bc [classname] remove [type] | remove
            PotionEffectType pet = null;

            for (PotionEffectType x : PotionEffectType.values()) {
                if (x == null) {
                    continue;
                }
                if (x.getName().equalsIgnoreCase(args[3])) {
                    pet = x;
                    break;
                }
            }

            if (pet == null) {
                arena.msg(sender, Language.parse(MSG.ERROR_POTIONEFFECTTYPE_NOTFOUND, args[3]));
                return;
            }

            PotionEffect remove = null;

            for (PotionEffect pe : ape) {
                if (pe.getType().equals(pet)) {
                    remove = pe;
                    break;
                }
            }

            ape.remove(remove);
            arena.getArenaConfig().setManually("modules.betterclasses.permEffects." + c.getName(), parsePotionEffectsToString(ape));
            arena.getArenaConfig().save();
            arena.msg(sender, Language.parse(MSG.MODULE_BETTERCLASSES_REMOVE, c.getName(), remove.getType().getName()));
            return;
        }
        printHelp(arena, sender);
    }

    @Override
    public void configParse(final YamlConfiguration cfg) {
        for (final ArenaClass c : arena.getClasses()) {
            cfg.addDefault("modules.betterclasses.permEffects." + c.getName(), "none");
            cfg.addDefault("modules.betterclasses.maxPlayers." + c.getName(), 0);
            cfg.addDefault("modules.betterclasses.maxGlobalPlayers." + c.getName(), 0);
            cfg.addDefault("modules.betterclasses.neededEXPLevel." + c.getName(), 0);
            cfg.addDefault("modules.betterclasses.respawnCommand." + c.getName(), "");
        }
        for (final String team : arena.getTeamNames()) {
            cfg.addDefault("modules.betterclasses.maxTeamSwitches." + team, -1);
        }
        cfg.addDefault("modules.betterclasses.maxPlayerSwitches", -1);
    }

    @Override
    public void displayInfo(final CommandSender sender) {
        if (superMap == null || !superMap.containsKey(arena)) {
            return;
        }

        final Map<ArenaClass, HashSet<PotionEffect>> map = superMap.get(arena);

        for (final Map.Entry<ArenaClass, HashSet<PotionEffect>> arenaClassHashSetEntry : map.entrySet()) {
            final Set<String> set = new HashSet<>();
            for (final PotionEffect pef : arenaClassHashSetEntry.getValue()) {
                set.add(pef.getType().getName() + 'x' + pef.getAmplifier() + 1);
            }
            sender.sendMessage(arenaClassHashSetEntry.getKey().getName() + ": " + StringParser.joinSet(
                    set, "; "));
        }
    }

    @Override
    public void lateJoin(final Player player) {
        if (!superMap.containsKey(arena)) {
            init_map();
        }
        final ArenaPlayer ap = ArenaPlayer.parsePlayer(player.getName());
        debug(player, "respawning player " + ap);
        final Map<ArenaClass, HashSet<PotionEffect>> map = superMap.get(arena);
        if (map == null) {
            PVPArena.getInstance().getLogger().warning("No superMap entry for arena " + arena);
            return;
        }

        final ArenaClass c = ap.getArenaClass();

        final Iterable<PotionEffect> ape = map.get(c);
        if (ape == null) {
            debug(player, "no effects for team " + c);
            return;
        }
        for (final PotionEffect pe : ape) {
            debug(player, "adding " + pe.getType());
            player.addPotionEffect(pe);
        }
    }

    private void init_map() {
        final HashMap<ArenaClass, HashSet<PotionEffect>> map = new HashMap<>();

        superMap.put(arena, map);

        for (final ArenaClass c : arena.getClasses()) {
            final String s = (String) arena.getArenaConfig().getUnsafe("modules.betterclasses.permEffects." + c.getName());
            if (s == null) {
                continue;
            }
            final HashSet<PotionEffect> ape = parseStringToPotionEffects(s);
            if (ape == null || ape.size() < 1) {
                continue;
            }
            map.put(c, ape);
        }

        for (final ArenaPlayer ap : arena.getFighters()) {
            final Iterable<PotionEffect> ape = map.get(ap.getArenaClass());
            if (ape == null) {
                continue;
            }
            for (final PotionEffect pe : ape) {
                ap.get().addPotionEffect(pe);
            }
        }
    }

    private boolean notEnoughEXP(final Player player, final String className) {
        final int needed;
        final int available;

        try {
            needed = (Integer) arena.getArenaConfig().getUnsafe("modules.betterclasses.neededEXPLevel." + className);
            final PlayerState state = ArenaPlayer.parsePlayer(player.getName()).getState();

            final Field value = state.getClass().getDeclaredField("explevel");
            value.setAccessible(true);
            available = value.getInt(state);
        } catch (final Exception e) {
            return false;
        }

        return available < needed;
    }

    private String parsePotionEffectsToString(final Iterable<PotionEffect> ape) {
        final Set<String> result = new HashSet<>();
        for (final PotionEffect pe : ape) {
            result.add(pe.getType().getName() + ':' + pe.getAmplifier());
        }
        return StringParser.joinSet(result, ",");
    }

    @Override
    public void reset(final boolean force) {
        playerSwitches.clear();
        teamSwitches.clear();
    }

    @Override
    public void parseRespawn(final Player player, final ArenaTeam team, final DamageCause cause, final Entity damager) {
        if (!superMap.containsKey(arena)) {
            init_map();
        }
        final ArenaPlayer ap = ArenaPlayer.parsePlayer(player.getName());
        debug(player, "respawning player " + ap);

        final ArenaClass c = ap.getArenaClass();
        if (c != null) {
            final String node = "modules.betterclasses.respawnCommand." + c.getName();
            String cmd = arena.getArenaConfig().getYamlConfiguration().getString(node, "");
            if (cmd.length() > 0) {
                cmd = cmd.replace("%player%", player.getName());
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }
        }


        final Map<ArenaClass, HashSet<PotionEffect>> map = superMap.get(arena);
        if (map == null) {
            PVPArena.getInstance().getLogger().warning("No superMap entry for arena " + arena);
            return;
        }

        if ((team != null || cause != null || damager != null) &&
                player.getActivePotionEffects() != null && !player.getActivePotionEffects().isEmpty()) {
            for (final PotionEffect eff : player.getActivePotionEffects()) {
                player.removePotionEffect(eff.getType());
            }
        }

        final Iterable<PotionEffect> ape = map.get(c);
        if (ape == null) {
            debug(player, "no effects for class " + c);
            return;
        }

        class RunLater implements Runnable {
            @Override
            public void run() {
                for (final PotionEffect pe : ape) {
                    debug(player, "adding " + pe.getType());
                    player.addPotionEffect(pe);
                }
            }
        }

        try {
            Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), new RunLater(), 20L);
        } catch (Exception e) {
            debug(arena, player, "[betterclass] exception when adding potion effects: " + e.getMessage());
        }
    }

    @Override
    public void parseClassChange(Player player, ArenaClass aClass) {
        debug(arena, "BetterClass handling class change!");
        ArenaPlayer ap = ArenaPlayer.parsePlayer(player.getName());
        if (playerSwitches.containsKey(ap)) {
            int value = playerSwitches.get(ap);
            if (value-- > 0) {
                playerSwitches.put(ap, value);
                debug(arena, "player " + ap.getName() + ": " + value);
            }
        }
        ArenaTeam at = ap.getArenaTeam();
        if (teamSwitches.containsKey(at) && teamSwitches.get(at) != null) {
            int value = teamSwitches.get(at);
            if (value-- > 0) {
                teamSwitches.put(at, value);
                debug(arena, "team " + at.getName() + ": " + value);
            }
        }
        if (arena.isFightInProgress()) {
            parseRespawn(ap.get(), at, null, null);
        }
    }

    @Override
    public void parseStart() {
        if (!superMap.containsKey(arena)) {
            init_map();
        }
        for (final ArenaPlayer ap : arena.getFighters()) {
            parseRespawn(ap.get(), null, null, null);
            playerSwitches.put(ap,
                    (Integer) arena.getArenaConfig()
                            .getUnsafe("modules.betterclasses.maxPlayerSwitches"));
        }

        for (ArenaTeam at : arena.getTeams()) {
            teamSwitches.put(at,
                    (Integer) arena.getArenaConfig()
                            .getUnsafe("modules.betterclasses.maxTeamSwitches."+at.getName()));
        }
    }

    private HashSet<PotionEffect> parseStringToPotionEffects(final String s) {
        final HashSet<PotionEffect> spe = new HashSet<>();

        if (s == null || "none".equals(s) || s != null && s.isEmpty()) {
            return spe;
        }

        String current = null;

        try {
            final String[] ss = s.split(",");
            for (final String sss : ss) {
                current = sss;

                final String[] values = sss.split(":");

                final PotionEffectType type = PotionEffectType.getByName(values[0].toUpperCase());

                final int amp = values.length < 2 ? 1 : Integer.parseInt(values[1]);

                final PotionEffect pe = new PotionEffect(type, Integer.MAX_VALUE, amp - 1);
                spe.add(pe);
            }
        } catch (final Exception e) {
            PVPArena.getInstance().getLogger().warning("error while parsing POTION EFFECT DEFINITION \"" + s + "\" : " + current);
        }

        return spe;
    }

    private void printHelp(final Arena arena, final CommandSender sender) {
        arena.msg(sender, "/pa [arenaname] !bc [classname] | list potion effects");
        arena.msg(sender, "/pa [arenaname] !bc [classname] clear | clear potion effects");
        arena.msg(sender, "/pa [arenaname] !bc [classname] add [type] [amp] | add potion effect");
        arena.msg(sender, "/pa [arenaname] !bc [classname] remove [type] | remove potion effect");
        arena.msg(sender, "/pa [arenaname] !bc [classname] respawncommand [command] | command on respawn");
    }
}
