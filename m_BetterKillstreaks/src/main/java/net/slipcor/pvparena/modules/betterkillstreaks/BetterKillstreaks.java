package net.slipcor.pvparena.modules.betterkillstreaks;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.commands.AbstractArenaCommand;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.events.PADeathEvent;
import net.slipcor.pvparena.events.PAKillEvent;
import net.slipcor.pvparena.loadables.ArenaModule;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class BetterKillstreaks extends ArenaModule implements Listener {
    public BetterKillstreaks() {
        super("BetterKillstreaks");
    }

    private final Map<String, Integer> streaks = new HashMap<>();

    private boolean setup;

    @Override
    public String version() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public boolean checkCommand(final String s) {
        return "!bk".equals(s) || s.startsWith("betterkillstreaks");
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("betterkillstreaks");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!bk");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        result.define(new String[]{"{int}", "potion"});
        result.define(new String[]{"{int}", "clear"});
        result.define(new String[]{"{int}", "items"});
        return result;
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {

        if (!PVPArena.hasAdminPerms(sender)
                && !PVPArena.hasCreatePerms(sender, arena)) {
            arena.msg(sender,
                    Language.parse(MSG.ERROR_NOPERM, Language.parse(MSG.ERROR_NOPERM_X_ADMIN)));
            return;
        }

        if (!AbstractArenaCommand.argCountValid(sender, arena, args, new Integer[]{2, 3, 4, 5, 6})) {
            return;
        }

        final Integer level;

        try {
            level = Integer.parseInt(args[1]);
        } catch (final Exception e) {
            arena.msg(sender, Language.parse(MSG.ERROR_NOT_NUMERIC, args[1]));
            return;
        }
        ConfigurationSection cs = arena.getArenaConfig().getYamlConfiguration().getConfigurationSection("modules.betterkillstreaks.definitions");
        if (args.length < 3) {
            // !bk [level] | show level content
            if (cs.get("d" + level) == null) {
                arena.msg(sender, "--------");
            } else {
                cs = cs.getConfigurationSection("d" + level);
                arena.msg(sender, "items: " + cs.getString("items", "NONE"));
                arena.msg(sender, "potion: " + cs.getString("potion", "NONE"));
            }
            return;
        }

        if ("clear".equals(args[2])) {
            // !bk [level] clear | clear the definition
            cs.set("d" + level, null);
            arena.msg(sender, "level " + level + " removed!");
            arena.getArenaConfig().save();
            return;
        }

        if ("items".equals(args[2])) {
            // !bk [level] items | set the items to your inventory
            cs.set("d" + level + ".items", ((Player) sender).getInventory().getContents());
            arena.getArenaConfig().save();
            arena.msg(sender, "Items of level " + level + " set to: " + ((Player) sender).getInventory().getContents().toString());
            return;
        }

        if (!"potion".equals(args[2])) {
            arena.msg(sender, "/pa [arenaname] !bk [level] | list level settings");
            arena.msg(sender, "/pa [arenaname] !bk [level] clear | clear level settings");
            arena.msg(sender, "/pa [arenaname] !bk [level] potion [type] {amp} {duration} | add potion effect");
            arena.msg(sender, "/pa [arenaname] !bk [level] items | set the level's items");

            return;
        }

        // !bk [level] potion [def]| add potioneffect definition

        final HashSet<PotionEffect> ape = new HashSet<>();


        // 0   1           2      3     4    5
        // !bc [level] potion    [def] [amp] [dur]| add
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

        if (args.length >= 5) {
            try {
                amp = Integer.parseInt(args[4]);
            } catch (final Exception e) {
                arena.msg(sender, Language.parse(MSG.ERROR_NOT_NUMERIC, args[4]));
                return;
            }
        }

        int duration = 2400;

        if (args.length >= 6) {
            try {
                duration = Integer.parseInt(args[5]);
            } catch (final Exception e) {
                arena.msg(sender, Language.parse(MSG.ERROR_NOT_NUMERIC, args[5]));
                return;
            }
        }

        ape.add(new PotionEffect(pet, duration, amp));

        final String val = parsePotionEffectsToString(ape);

        cs.set("d" + level + ".potion", val);

        arena.getArenaConfig().save();
        arena.msg(sender, "Level " + level + " now has potion effect: " + val);
    }

    @Override
    public void configParse(final YamlConfiguration config) {
        if (!setup) {
            Bukkit.getPluginManager().registerEvents(this, PVPArena.getInstance());
            setup = true;
        }
    }

    @Override
    public void parseStart() {
        streaks.clear();
    }

    @Override
    public void reset(final boolean force) {
        streaks.clear();
    }

    @EventHandler
    public void onPlayerDeath(final PADeathEvent event) {
        streaks.remove(event.getPlayer().getName());
    }

    @EventHandler
    public void onPlayerKill(final PAKillEvent event) {
        final int value;
        if (streaks.containsKey(event.getPlayer().getName())) {
            value = streaks.get(event.getPlayer().getName()) + 1;

        } else {
            value = 1;
        }
        streaks.put(event.getPlayer().getName(), value);
        reward(event.getPlayer(), value);
    }

    private String parsePotionEffectsToString(final Iterable<PotionEffect> ape) {
        final Set<String> result = new HashSet<>();
        for (final PotionEffect pe : ape) {
            result.add(pe.getType().getName() + ':' + pe.getAmplifier());
        }
        return StringParser.joinSet(result, ",");
    }

    private Iterable<PotionEffect> parseStringToPotionEffects(final String s) {
        final HashSet<PotionEffect> spe = new HashSet<>();

        if (s == null || "none".equals(s) || s.isEmpty()) {
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

                final int duration = values.length < 3 ? 2400 : Integer.parseInt(values[2]);

                final PotionEffect pe = new PotionEffect(type, duration, amp - 1);
                spe.add(pe);
            }
        } catch (final Exception e) {
            PVPArena.getInstance().getLogger().warning("error while parsing POTION EFFECT DEFINITION \"" + s + "\" : " + current);
        }

        return spe;
    }


    private void reward(final Player player, final int value) {
        final ConfigurationSection cs = arena.getArenaConfig().getYamlConfiguration().getConfigurationSection("modules.betterkillstreaks.definitions");
        for (final String key : cs.getKeys(false)) {
            if (key.equals("d" + value)) {
                final ItemStack[] items = cs.getList("items").toArray(new ItemStack[0]);
                for (final ItemStack item : items) {
                    player.getInventory().addItem(item);
                }

                final String pot = cs.getString("d" + value, "���");

                if (!"���".equals(pot)) {
                    for (final PotionEffect pe : parseStringToPotionEffects(pot)) {
                        player.addPotionEffect(pe);
                    }
                }
            }
        }
    }
}
