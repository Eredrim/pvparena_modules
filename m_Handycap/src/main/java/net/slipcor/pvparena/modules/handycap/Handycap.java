package net.slipcor.pvparena.modules.handycap;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.loadables.ArenaModule;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.slipcor.pvparena.config.Debugger.debug;

public class Handycap extends ArenaModule {
    enum HandycapType {
        HEALTH,
        POWERUP
    }
    private HandycapType type = HandycapType.HEALTH;

    private Map<PotionEffectType, Integer> effects = new HashMap<>();
    private Map<ArenaTeam, Integer> handycapped = new HashMap<>();
    private int max = 0;

    public Handycap() {
        super("Handycap");
    }

    private void applyEffects() {

        if (type == HandycapType.POWERUP) {
            for (ArenaTeam team : handycapped.keySet()) {
                if (handycapped.get(team) < max) {
                    continue;
                }
                for (ArenaPlayer arenaPlayer : team.getTeamMembers()) {
                    for (PotionEffectType pet : effects.keySet()) {
                        arenaPlayer.getPlayer().removePotionEffect(pet);
                        arenaPlayer.getPlayer().addPotionEffect(new PotionEffect(pet, Integer.MAX_VALUE, effects.get(pet), true));
                    }
                }
            }
            return;
        }

        double maxHealth = 0;

        for (ArenaTeam team : handycapped.keySet()) {
            if (handycapped.get(team) == max) {
                for (ArenaPlayer arenaPlayer : team.getTeamMembers()) {
                    maxHealth = arenaPlayer.getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
                    break;
                }
            }
        }

        for (ArenaTeam team : handycapped.keySet()) {
            if (handycapped.get(team) < max) {
                double ratio = 1.0 - (double) handycapped.get(team) / (double) max;
                double value = maxHealth * ratio;

                for (ArenaPlayer arenaPlayer : team.getTeamMembers()) {
                    arenaPlayer.getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(value);
                }
            }
        }
    }

    private void applyEffects(Player player) {
        if (type == HandycapType.POWERUP) {
            for (PotionEffectType pet : effects.keySet()) {
                player.removePotionEffect(pet);
                player.addPotionEffect(new PotionEffect(pet, Integer.MAX_VALUE, effects.get(pet), true));
            }
            return;
        }

        double maxHealth = 0;
        ArenaTeam team = ArenaPlayer.fromPlayer(player).getArenaTeam();

        for (ArenaTeam at : handycapped.keySet()) {
            if (handycapped.get(at) == max) {
                for (ArenaPlayer arenaPlayer : at.getTeamMembers()) {
                    maxHealth = arenaPlayer.getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
                    break;
                }
            }
        }

        double ratio = 1.0 - (double) handycapped.get(team) / (double) max;
        double value = maxHealth * ratio;

        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(value);
    }

    private void calculateHandycap() {
        debug(getArena(), "applying handycap type:" + String.valueOf(type));

        max = 0;
        handycapped.clear();

        boolean foundIssue = false;

        removeEffects();

        for (ArenaTeam team : getArena().getTeams()) {
            if (team.getTeamMembers().size() > 0) {
                int members = team.getTeamMembers().size();
                if (max > 0 && members > 0) {
                    foundIssue = true;
                }
                max = Math.max(0, members);
                handycapped.put(team, members);
            }
        }

        if (foundIssue) {
            applyEffects();
        }
    }

    private void removeEffects() {
        if (type == HandycapType.POWERUP) {
            for (ArenaTeam team : handycapped.keySet()) {
                for (ArenaPlayer arenaPlayer : team.getTeamMembers()) {
                    for (PotionEffectType pet : effects.keySet()) {
                        arenaPlayer.getPlayer().removePotionEffect(pet);
                    }
                }
            }
            return;
        }

        double maxHealth = 0;

        for (ArenaTeam team : handycapped.keySet()) {
            if (handycapped.get(team) == max) {
                for (ArenaPlayer arenaPlayer : team.getTeamMembers()) {
                    maxHealth = arenaPlayer.getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
                    break;
                }
            }
        }

        for (ArenaTeam team : handycapped.keySet()) {
            if (handycapped.get(team) < max) {
                for (ArenaPlayer arenaPlayer : team.getTeamMembers()) {
                    arenaPlayer.getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);
                }
            }
        }
    }

    @Override
    public boolean checkCommand(final String cmd) {
        return cmd.equals("handycap") || "!hc".equals(cmd) || "handycaptype".equals(cmd) || "handycapeffect".equals(cmd);
    }

    @Override
    public void configParse(final YamlConfiguration cfg) {
        cfg.addDefault("modules.handycap.type", "MAXHEALTH");
    }
    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        if (args[0].equals("handycap") || args[0].equals("handycap")) {
            if (args.length < 3) {
                sender.sendMessage(Language.parse(Language.MSG.ERROR_INVALID_ARGUMENT_COUNT, String.valueOf(args.length), "2|3"));
                return;
            }
            if (args[1].equals("effect")) {
                commitSetEffect(sender, args[2]);
            } else if (args[1].equals("type")) {
                commitSetType(sender, args[2]);
            } else {
                sender.sendMessage(Language.parse(Language.MSG.ERROR_ARGUMENT, args[1], "effect, type"));
            }
        } else if (args.length > 1) {
            if (args[0].equals("handycapeffect")) {
                commitSetEffect(sender, args[1]);
            } else if (args[0].equals("handycaptype")) {
                commitSetType(sender, args[1]);
            } else {
                sender.sendMessage(Language.parse(Language.MSG.ERROR_ARGUMENT, args[0], "handycap, handycapeffect, handycaptype"));
            }
        } else {
            sender.sendMessage(Language.parse(Language.MSG.ERROR_INVALID_ARGUMENT_COUNT, String.valueOf(args.length), "2|3"));
        }
    }

    private void commitSetEffect(CommandSender sender, String arg) {
        if (arg.contains(":")) {
            String[] split = arg.split(":");
            boolean petWorked = false;
            try {
                PotionEffectType pet = PotionEffectType.getByName(split[0].toUpperCase());
                petWorked = true;
                Integer level = Integer.parseInt(split[1]);
                getArena().getConfig().setManually("modules.handycap.powerups."+pet.getName().toLowerCase(), level);
                getArena().getConfig().save();

            } catch (Exception e) {
                if (petWorked) {
                    sender.sendMessage(Language.parse(Language.MSG.ERROR_ARGUMENT, arg, "POTIONEFFECTTYPE:LEVEL"));
                    sender.sendMessage(Language.parse(Language.MSG.ERROR_NOT_NUMERIC, split[1]));
                } else {
                    sender.sendMessage(Language.parse(Language.MSG.ERROR_ARGUMENT, arg, "POTIONEFFECTTYPE:LEVEL"));
                    sender.sendMessage(Language.parse(Language.MSG.ERROR_ARGUMENT_TYPE, arg, "PotionEffectType"));
                }
            }
        } else {
            sender.sendMessage(Language.parse(Language.MSG.ERROR_ARGUMENT, arg, "POTIONEFFECTTYPE:LEVEL"));
        }
    }

    private void commitSetType(CommandSender sender, String arg) {
        if (arg.equalsIgnoreCase("health")) {
            getArena().getConfig().setManually("modules.handycap.type", "HEALTH");
        } else if (arg.toLowerCase().startsWith("powerup")) {
            getArena().getConfig().setManually("modules.handycap.type", "POWERUP");
        } else {
            sender.sendMessage(Language.parse(Language.MSG.ERROR_ARGUMENT, arg, "health, powerup"));
            return;
        }
        getArena().getConfig().save();
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        result.define(new String[]{"effect", "String"});
        result.define(new String[]{"type", "String"});
        result.define(new String[]{"health"});
        result.define(new String[]{"powerup"});
        return result;
    }

    @Override
    public void parsePlayerLeave(final Player player, final ArenaTeam team) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(PVPArena.getInstance(), new Runnable() {
            @Override
            public void run() {
                calculateHandycap();
            }
        }, 10L);
    }

    @Override
    public void parseRespawn(final Player player, final ArenaTeam team,
                             final EntityDamageEvent.DamageCause cause, final Entity damager) {
        if (handycapped.get(team) < max) {
            applyEffects(player);
        }
    }

    @Override
    public void parseStart() {
        effects.clear();

        ConfigurationSection cs = getArena().getConfig().getYamlConfiguration()
                .getConfigurationSection("modules.handycap");
        try {
            type = HandycapType.valueOf(cs.getString("type"));
        } catch (Exception e) {
            PVPArena.getInstance().getLogger().warning("Invalid content for \"modules.handycap.type\" for arena "+getArena().getName());
            final List<String> valids = new ArrayList<>();
            for (HandycapType type : HandycapType.values()) {
                valids.add(type.name());
            }
            PVPArena.getInstance().getLogger().warning("Valid values: " + StringParser.joinList(valids, ","));
        }

        if (type == HandycapType.POWERUP) {
            ConfigurationSection powerups = cs.getConfigurationSection("powerups");
            final List<String> invalids = new ArrayList<>();
            final List<String> valids = new ArrayList<>();

            for (PotionEffectType pet : PotionEffectType.values()) {
                if (pet != null) {
                    valids.add(pet.getName());
                }
            }

            for (String entry : powerups.getKeys(true)) {
                if (valids.contains(entry.toUpperCase())) {
                    effects.put(PotionEffectType.getByName(entry.toUpperCase()),
                            powerups.getInt(entry, 1));
                } else {
                    invalids.add(entry);
                }
            }

            if (!invalids.isEmpty()) {
                PVPArena.getInstance().getLogger().warning("Invalid content for these handycap definitions: '"+
                        StringParser.joinList(invalids, ",")+"'");
                PVPArena.getInstance().getLogger().warning("Valid values: " + StringParser.joinList(valids, ","));
            }
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask(PVPArena.getInstance(), new Runnable() {
            @Override
            public void run() {
                calculateHandycap();
            }
        }, 10L);
    }

    @Override
    public String version() {
        return getClass().getPackage().getImplementationVersion();
    }


}
