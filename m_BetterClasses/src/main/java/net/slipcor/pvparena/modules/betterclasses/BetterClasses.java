package net.slipcor.pvparena.modules.betterclasses;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaClass;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.arena.PlayerState;
import net.slipcor.pvparena.commands.AbstractArenaCommand;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.core.StringUtils;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.PermissionManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.joining;
import static net.slipcor.pvparena.config.Debugger.debug;
import static net.slipcor.pvparena.compatibility.Constants.INFINITE_EFFECT_DURATION;
import static net.slipcor.pvparena.modules.betterclasses.BetterClassDef.*;

public class BetterClasses extends ArenaModule {

    public BetterClasses() {
        super("BetterClasses");
    }

    @Override
    public String version() {
        return this.getClass().getPackage().getImplementationVersion();
    }

    private final Map<ArenaClass, BetterClassDef> betterClassMap = new HashMap<>();
    private final Map<ArenaTeam, Integer> teamSwitches = new HashMap<>();
    private final Map<ArenaPlayer, Integer> playerSwitches = new HashMap<>();

    @Override
    public boolean cannotSelectClass(Player player, String className) {
        ArenaClass arenaClass = this.arena.getArenaClass(className);
        BetterClassDef betterClassDef = this.betterClassMap.get(arenaClass);
        ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);
        ArenaTeam playerTeam = arenaPlayer.getArenaTeam();

        debug(arenaPlayer, "[BetterClasses] Checking if cannotSelectClass");

        if (!this.hasEnoughExp(arenaPlayer, arenaClass)) {
            this.arena.msg(player, MSG.ERROR_CLASS_NOTENOUGHEXP, className);
            return true;
        }

        int globalMax = betterClassDef.getMaxGlobalPlayers();
        int teamMax = betterClassDef.getMaxTeamPlayers();

        if (globalMax > 0 || teamMax > 0) {

            int globalSum = 0;
            int teamSum = 0;

            for (ArenaTeam team : this.arena.getTeams()) {
                for (ArenaPlayer ap : team.getTeamMembers()) {
                    if (ap.getArenaClass() != null && ap.getArenaClass().getName().equals(className)) {
                        globalSum++;
                        if (team.equals(playerTeam)) {
                            teamSum++;
                        }
                    }
                }
            }

            debug(arenaPlayer, "[BetterClasses] Players check for class '{}': team {}/{} - global {}/{}", className, teamSum, teamMax, globalSum, globalMax);

            if ((teamMax != -1 && teamSum >= teamMax) || (globalMax != -1 && globalSum >= globalMax)) {
                this.arena.msg(player, MSG.ERROR_CLASS_FULL, className);
                return true;
            }
        }

        if(playerTeam != null) {
            Integer remainingTeamSwitches = this.teamSwitches.get(playerTeam);
            debug(arenaPlayer, "[BetterClasses] TeamSwitches of team {}: {}", playerTeam, remainingTeamSwitches);
            if (remainingTeamSwitches != null && remainingTeamSwitches == 0) {
                this.arena.msg(player, MSG.MODULE_BETTERCLASSES_CLASSCHANGE_MAXTEAM);
                return true;
            }
        }

        Integer remainingSwitches = this.playerSwitches.get(arenaPlayer);
        debug(arenaPlayer, "[BetterClasses] Reamining playersSwitches: {}", remainingSwitches);
        if (remainingSwitches != null && remainingSwitches == 0) {
            this.arena.msg(player, MSG.MODULE_BETTERCLASSES_CLASSCHANGE_MAXPLAYER);
            return true;
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
            result.define(new String[]{aClass.getName(), "add", "{PotionEffectType}"});
            result.define(new String[]{aClass.getName(), "clear"});
            result.define(new String[]{aClass.getName(), "set", "maxTeamPlayers"});
            result.define(new String[]{aClass.getName(), "set", "maxGlobalPlayers"});
            result.define(new String[]{aClass.getName(), "set", "neededEXPLevel"});
            result.define(new String[]{aClass.getName(), "respawncommand"});
            result.define(new String[]{aClass.getName(), "remove", "{PotionEffectType}"});
        }
        return result;
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        if (!PermissionManager.hasAdminPerm(sender) && !PermissionManager.hasBuilderPerm(sender, this.arena)) {
            this.arena.msg(sender, MSG.ERROR_NOPERM, Language.parse(MSG.ERROR_NOPERM_X_ADMIN));
            return;
        }

        if (!AbstractArenaCommand.argCountValid(sender, this.arena, args, new Integer[]{2, 3, 4, 5}) ||
                (args.length > 5 && !RESPAWN_COMMAND_KEY.equalsIgnoreCase(args[2]))) {
            this.arena.msg(sender, MSG.ERROR_INVALID_ARGUMENT_COUNT, String.valueOf(args.length - 1), "1 to 4");
            this.printHelp(this.arena, sender);
            return;
        }

        ArenaClass c = this.arena.getArenaClass(args[1]);

        if (c == null) {
            this.arena.msg(sender, MSG.ERROR_CLASS_NOT_FOUND, args[1]);
            return;
        }

        ConfigurationSection configSection = this.arena.getConfig().getConfigurationSection(String.format("modules.betterclasses.%s", c.getName()));
        BetterClassDef betterClassDef = BetterClassDef.convertFromConfig(configSection);

        if (args.length == 5 && "set".equalsIgnoreCase(args[2])) {
            final int value;
            try {
                value = Integer.parseInt(args[4]);
            } catch (final Exception e) {
                this.arena.msg(sender, MSG.ERROR_NOT_NUMERIC, args[4]);
                return;
            }

            if (NEEDED_EXP_LEVEL_KEY.equalsIgnoreCase(args[3])) {
                betterClassDef.setNeededEXPLevel(value);
                this.arena.msg(sender, MSG.SET_DONE, NEEDED_EXP_LEVEL_KEY, String.valueOf(value));
            } else if (MAX_TEAM_PLAYERS_KEY.equalsIgnoreCase(args[3])) {
                betterClassDef.setMaxTeamPlayers(value);
                this.arena.msg(sender, MSG.SET_DONE, MAX_TEAM_PLAYERS_KEY, String.valueOf(value));
            } else if (MAX_GLOBAL_PLAYERS_KEY.equalsIgnoreCase(args[3])) {
                betterClassDef.setMaxGlobalPlayers(value);
                this.arena.msg(sender, MSG.SET_DONE, MAX_GLOBAL_PLAYERS_KEY, String.valueOf(value));
            } else {
                return;
            }
            configSection.getParent().createSection(c.getName(), betterClassDef.convertToConfig());
            this.arena.getConfig().save();
            this.initMap();
            return;
        } else if (args.length > 2 && RESPAWN_COMMAND_KEY.equalsIgnoreCase(args[2])) {
            if (args.length == 3) {
                betterClassDef.setRespawnCommand(null);
                this.arena.msg(sender, MSG.MODULE_BETTERCLASSES_RESPAWNCOMMAND_REMOVED, RESPAWN_COMMAND_KEY, c.getName());
            } else {
                String command = StringParser.joinArray(StringParser.shiftArrayBy(args, 3), " ");
                betterClassDef.setRespawnCommand(command);
                this.arena.msg(sender, MSG.SET_DONE, RESPAWN_COMMAND_KEY, command);
            }
            configSection.getParent().createSection(c.getName(), betterClassDef.convertToConfig());
            this.arena.getConfig().save();
            this.initMap();
            return;
        }


        if (args.length == 2) {
            // !bc [classname] | show
            this.arena.msg(sender, MSG.MODULE_BETTERCLASSES_LISTHEAD, c.getName());
            if(!betterClassDef.getPermEffects().isEmpty()) {
                betterClassDef.getPermEffects().forEach(effect ->
                    this.arena.msg(sender, String.format("%s %d", effect.getType().getName(), effect.getAmplifier() + 1))
                );
                this.arena.msg(sender, "---");
            }
        } else if (args.length == 3) {
            if ("clear".equalsIgnoreCase(args[2])) {
                betterClassDef.getPermEffects().clear();
                this.arena.msg(sender, MSG.MODULE_BETTERCLASSES_CLEAR, c.getName());
                configSection.getParent().createSection(c.getName(), betterClassDef.convertToConfig());
                this.arena.getConfig().save();
                this.initMap();
            } else {
                this.arena.msg(sender, MSG.ERROR_COMMAND_UNKNOWN);
            }
        } else {
            PotionEffectType effectType = PotionEffectType.getByName(args[3]);
            if(effectType == null) {
                this.arena.msg(sender, MSG.ERROR_POTIONEFFECTTYPE_NOTFOUND, args[3]);
                return;
            }

            if("add".equalsIgnoreCase(args[2])) {
                int amplifier = 1;
                if(args.length == 5) {
                    try {
                        amplifier = Integer.parseInt(args[4]);
                    } catch (final NumberFormatException e) {
                        this.arena.msg(sender, MSG.ERROR_NOT_NUMERIC, args[4]);
                        return;
                    }
                }

                betterClassDef.getPermEffects().removeIf(effect -> effect.getType().equals(effectType));
                betterClassDef.getPermEffects().add(new PotionEffect(effectType, INFINITE_EFFECT_DURATION, amplifier - 1));
                this.arena.msg(sender, MSG.MODULE_BETTERCLASSES_ADD, c.getName(), effectType.getName());

            } else if ("remove".equalsIgnoreCase(args[2])) {
                betterClassDef.getPermEffects().removeIf(effect -> effect.getType().equals(effectType));
                this.arena.msg(sender, MSG.MODULE_BETTERCLASSES_REMOVE, c.getName(), effectType.getName());

            } else {
                this.arena.msg(sender, MSG.ERROR_COMMAND_INVALID, args[2]);
                this.printHelp(this.arena, sender);
                return;
            }

            configSection.getParent().createSection(c.getName(), betterClassDef.convertToConfig());
            this.arena.getConfig().save();
            this.initMap();
        }
    }

    @Override
    public void initConfig() {
        YamlConfiguration cfg = this.arena.getConfig().getYamlConfiguration();
        this.arena.getClasses().forEach(c -> {
            cfg.addDefault(String.format("modules.betterclasses.%s.maxTeamPlayers", c.getName()), -1);
            cfg.addDefault(String.format("modules.betterclasses.%s.maxGlobalPlayers", c.getName()), -1);
            cfg.addDefault(String.format("modules.betterclasses.%s.neededEXPLevel", c.getName()), 0);
        });

        if(!this.arena.isFreeForAll()) {
            this.arena.getTeamNames().forEach(team -> cfg.addDefault("modules.betterclasses.maxTeamSwitches." + team, -1));
        }

        cfg.addDefault("modules.betterclasses.maxPlayerSwitches", -1);
    }

    @Override
    public void displayInfo(final CommandSender sender) {
        this.betterClassMap.forEach((arenaClass, betterClassDef) -> {
            String effects = betterClassDef.getPermEffects().stream()
                    .map(pEffect -> String.format("%sx%d", pEffect.getType().getName(), pEffect.getAmplifier() + 1))
                    .collect(joining("; "));
            sender.sendMessage(String.format("%s: %s", arenaClass.getName(), effects));
        });
    }

    @Override
    public void lateJoin(final Player player) {
        ArenaPlayer ap = ArenaPlayer.fromPlayer(player);

        ArenaClass arenaClass = ap.getArenaClass();
        Set<PotionEffect> effectSet = this.betterClassMap.get(arenaClass).getPermEffects();

        Integer maxPlayerSwitches = (Integer) this.arena.getConfig().getUnsafe("modules.betterclasses.maxPlayerSwitches");
        this.playerSwitches.put(ap, maxPlayerSwitches);

        player.addPotionEffects(effectSet);
    }

    private void initMap() {
        this.betterClassMap.clear();
        this.arena.getClasses().forEach(c -> {
            ConfigurationSection cs = this.arena.getConfig().getConfigurationSection(String.format("modules.betterclasses.%s", c.getName()));
            if (cs != null) {
                BetterClassDef betterClassDef = BetterClassDef.convertFromConfig(cs);
                this.betterClassMap.put(c, betterClassDef);
            }
        });
    }

    private void applyEffectsToPlayers() {
        this.arena.getFighters().forEach(arenaPlayer -> {
            Set<PotionEffect> potionEffects = this.betterClassMap.get(arenaPlayer.getArenaClass()).getPermEffects();
            if (potionEffects != null) {
                arenaPlayer.getPlayer().addPotionEffects(potionEffects);
            }
        });
    }

    private boolean hasEnoughExp(ArenaPlayer arenaPlayer, ArenaClass arenaClass) {
        try {
            int needed = this.betterClassMap.get(arenaClass).getNeededEXPLevel();
            final PlayerState state = arenaPlayer.getState();

            final Field value = state.getClass().getDeclaredField("explevel");
            value.setAccessible(true);
            int available = value.getInt(state);
            return available >= needed;
        } catch (final Exception e) {
            return true;
        }
    }

    @Override
    public void reset(final boolean force) {
        this.betterClassMap.clear();
        this.playerSwitches.clear();
        this.teamSwitches.clear();
    }

    @Override
    public void parseRespawn(Player player, ArenaTeam team, DamageCause cause, Entity damager) {
        final ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);

        final ArenaClass arenaClass = arenaPlayer.getArenaClass();
        if (arenaClass == null) {
            return;
        }

        BetterClassDef betterClassDef = this.betterClassMap.get(arenaClass);
        applyRespawnCommand(player, betterClassDef);
        applyNewPotionEffects(arenaPlayer, betterClassDef.getPermEffects());
    }

    private static void applyNewPotionEffects(ArenaPlayer ap, Set<PotionEffect> potionEffects) {
        Player player = ap.getPlayer();
        player.getActivePotionEffects().forEach(potionEffect -> player.removePotionEffect(potionEffect.getType()));

        try {
            Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), () -> {
                player.addPotionEffects(potionEffects);
            }, 20L);
        } catch (IllegalArgumentException e) {
            debug(ap, "[BetterClasses] exception when adding potion effects: {}", e.getMessage());
        }
    }

    private static void applyRespawnCommand(Player player, BetterClassDef betterClassDef) {
        String respawnCommand = betterClassDef.getRespawnCommand();
        if(StringUtils.notBlank(respawnCommand)) {
            String parsedCommand = respawnCommand.replace("%player%", player.getName());
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), parsedCommand);
        }
    }

    @Override
    public void parseClassChange(Player player, ArenaClass aClass) {
        debug(this.arena, "[BetterClasses] handling class change!");

        if (this.arena.isFightInProgress()) {
            ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);

            if (this.playerSwitches.containsKey(arenaPlayer)) {
                int value = this.playerSwitches.get(arenaPlayer);
                if (value > 0) {
                    value--;
                    this.playerSwitches.put(arenaPlayer, value);
                    debug(arenaPlayer, "[BetterClasses] remaining player switches: {}", value);
                }
            }

            ArenaTeam at = arenaPlayer.getArenaTeam();
            if (!this.arena.isFreeForAll() && this.teamSwitches.containsKey(at)) {
                int value = this.teamSwitches.get(at);
                if (value > 0) {
                    value--;
                    this.teamSwitches.put(at, value);
                    debug(arenaPlayer, "[BetterClasses] remaining team switches - team {}: {}", at.getName(), value);
                }
            }

            applyNewPotionEffects(arenaPlayer, this.betterClassMap.get(aClass).getPermEffects());
        }
    }

    @Override
    public void parseStart() {
        this.applyEffectsToPlayers();

        Integer maxPlayerSwitches = (Integer) this.arena.getConfig().getUnsafe("modules.betterclasses.maxPlayerSwitches");
        for (ArenaPlayer arenaPlayer : this.arena.getFighters()) {
            applyRespawnCommand(arenaPlayer.getPlayer(), this.betterClassMap.get(arenaPlayer.getArenaClass()));
            this.playerSwitches.put(arenaPlayer, maxPlayerSwitches);
        }

        for (ArenaTeam at : this.arena.getTeams()) {
            Integer maxTeamSwitches = (Integer) this.arena.getConfig().getUnsafe("modules.betterclasses.maxTeamSwitches." + at.getName());
            this.teamSwitches.put(at, maxTeamSwitches);
        }
    }

    @Override
    public void checkJoin(Player player) {
        if (this.betterClassMap.isEmpty()) {
            this.initMap();
        }
    }

    private void printHelp(final Arena arena, final CommandSender sender) {
        arena.msg(sender, "/pa [arenaname] !bc <classname> | list potion effects");
        arena.msg(sender, "/pa [arenaname] !bc <classname> clear | clear potion effects");
        arena.msg(sender, "/pa [arenaname] !bc <classname> add <type> [amplifier] | add a potion effect");
        arena.msg(sender, "/pa [arenaname] !bc <classname> remove <type> | remove a potion effect");
        arena.msg(sender, "/pa [arenaname] !bc <classname> set <maxTeamPlayers/maxGlobalPlayers/neededEXPLevel> <value> | change setting value of a class");
        arena.msg(sender, "/pa [arenaname] !bc <classname> respawncommand [command] | change command of a class on respawn (keep empty to remove the former one)");
    }
}
