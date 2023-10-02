package net.slipcor.pvparena.modules.betterkillstreaks;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.commands.AbstractArenaCommand;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.core.StringUtils;
import net.slipcor.pvparena.events.PADeathEvent;
import net.slipcor.pvparena.events.PAKillEvent;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.PermissionManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static net.slipcor.pvparena.core.ItemStackUtils.getItemStacksFromConfig;
import static net.slipcor.pvparena.core.Utils.getSerializableItemStacks;

public class BetterKillstreaks extends ArenaModule implements Listener {
    public BetterKillstreaks() {
        super("BetterKillstreaks");
    }

    private final Map<String, Integer> streaks = new HashMap<>();

    private boolean setup;

    @Override
    public String version() {
        return this.getClass().getPackage().getImplementationVersion();
    }

    @Override
    public boolean checkCommand(final String s) {
        return "!bk".equalsIgnoreCase(s) || "betterkillstreaks".equalsIgnoreCase(s);
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
        result.define(new String[]{"{int}", "addEffect", "{PotionEffectType}", "{int}", "{int}"});
        result.define(new String[]{"{int}", "removeEffect", "{PotionEffectType}"});
        result.define(new String[]{"{int}", "clear"});
        result.define(new String[]{"{int}", "items"});
        result.define(new String[]{"{int}", "message"});
        return result;
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {

        if (!PermissionManager.hasAdminPerm(sender) && !PermissionManager.hasBuilderPerm(sender, this.arena)) {
            this.arena.msg(sender, MSG.ERROR_NOPERM, Language.parse(MSG.ERROR_NOPERM_X_ADMIN));
            return;
        }

        if (!AbstractArenaCommand.argCountValid(sender, this.arena, args, new Integer[]{2, 3, 4, 5, 6})) {
            return;
        }

        final Integer level;

        try {
            level = Integer.parseInt(args[1]);
        } catch (final Exception e) {
            this.arena.msg(sender, MSG.ERROR_NOT_NUMERIC, args[1]);
            return;
        }
        Config config = this.arena.getConfig();
        YamlConfiguration yamlConfig = config.getYamlConfiguration();
        ConfigurationSection defSection = config.getOrCreateConfigurationSection("modules.betterkillstreaks.definitions");
        String levelPath = "l" + level;
        if(defSection.get(levelPath) == null) {
            defSection.createSection(levelPath);
        }
        ConfigurationSection levelSection = defSection.getConfigurationSection(levelPath);
        if (args.length < 3) {
            // !bk [level] | show level content
            if (levelSection.getKeys(false).isEmpty()) {
                this.arena.msg(sender, "--------");
            } else {
                this.arena.msg(sender, String.format("message: %s", levelSection.getString("msg", "NONE")));
                this.arena.msg(sender, String.format("items: %s", levelSection.getString("items", "NONE")));
                this.arena.msg(sender, String.format("effects: %s", levelSection.getStringList("effects")));
            }
            return;
        }

        if ("clear".equals(args[2])) {
            // !bk [level] clear | clear the definition
            defSection.set(levelPath, null);
            this.arena.msg(sender, "level " + level + " removed!");
            config.save();
            return;
        }

        if ("items".equals(args[2])) {
            // !bk [level] items | set the items to your inventory
            yamlConfig.set(levelSection.getCurrentPath() + ".items", getSerializableItemStacks(((Player) sender).getInventory().getContents()));
            config.save();
            this.arena.msg(sender, String.format("Items of level %d have been set to your current inventory content", level));
            return;
        }

        if ("message".equalsIgnoreCase(args[2])) {
            String value = String.join(" ", StringParser.shiftArrayBy(args, 3));
            levelSection.set("msg", value);
            config.save();
            this.arena.msg(sender, String.format("Message of level %d have been set to '%s'", level, value));
            return;
        }

        if ("addEffect".equals(args[2])) {
            // !bk [level] addEffect [effectType] (amplifier) (duration)
            PotionEffect newEffect;

            try {
                newEffect = this.generatePotionEffect(args[3], args[4], args[5]);
            } catch (NullPointerException ignored) {
                this.arena.msg(sender, MSG.ERROR_POTIONEFFECTTYPE_NOTFOUND, args[3]);
                return;
            } catch (NumberFormatException ignored) {
                this.arena.msg(sender, MSG.ERROR_NOT_NUMERIC, String.format("%s or %s", args[4], args[5]));
                return;
            }


            List<String> effectList = levelSection.getStringList("effects");
            String newEffectStr = this.parsePotionEffectToStringCfg(newEffect);
            effectList.add(newEffectStr);

            levelSection.set("effects", effectList);

            config.save();
            this.arena.msg(sender, String.format("Level %d has now potions effects: %s", level, effectList));
            return;
        }

        if ("removeEffect".equals(args[2])) {
            // !bk [level] removeEffect [effectType]
            PotionEffectType effectType = PotionEffectType.getByName(args[3].toUpperCase());
            if(effectType == null) {
                this.arena.msg(sender, MSG.ERROR_POTIONEFFECTTYPE_NOTFOUND, args[3]);
            } else {
                List<String> effectList = levelSection.getStringList("effects");
                boolean removed = effectList.removeIf(effectStr -> effectStr.startsWith(effectType.getName()));
                if(removed) {
                    this.arena.msg(sender, String.format("Effect %s has been removed", effectType.getName()));
                    levelSection.set("effects", effectList);
                    config.save();
                } else {
                    this.arena.msg(sender, String.format("Effect %s doesn't exist in level configuration", effectType.getName()));
                }
            }
            return;
        }

        this.arena.msg(sender, "/pa [arenaname] !bk [level] | list level settings");
        this.arena.msg(sender, "/pa [arenaname] !bk [level] clear | clear level settings");
        this.arena.msg(sender, "/pa [arenaname] !bk [level] addEffect [effectType] (amplifier) (duration) | add level potion effect");
        this.arena.msg(sender, "/pa [arenaname] !bk [level] removeEffect [effectType] | remove level potion effect");
        this.arena.msg(sender, "/pa [arenaname] !bk [level] items | set the level's items");
        this.arena.msg(sender, "/pa [arenaname] !bk [level] message | set the level's message, use 'none' to remove it");
    }

    /**
     * Generate a new potion effect from 3 strings, assuming duration is in seconds
     * @param type Effect type
     * @param amp Effect amp
     * @param duration Effect duration in seconds
     * @return PotionEffect object
     * @throws NumberFormatException if number values can't be parsed
     * @throws NullPointerException if effect type is null (can't be parsed)
     */
    private PotionEffect generatePotionEffect(String type, String amp, String duration) throws NumberFormatException, NullPointerException {
        int effectAmp = Integer.parseInt(amp);
        int effectDuration = Integer.parseInt(duration);
        PotionEffectType effectType = PotionEffectType.getByName(type.toUpperCase());
        if(effectType == null) {
            throw new NullPointerException();
        }

        return new PotionEffect(effectType, effectDuration * 20, effectAmp - 1);
    }

    @Override
    public void initConfig() {
        YamlConfiguration config = this.arena.getConfig().getYamlConfiguration();
        if(config.getConfigurationSection("modules.betterkillstreaks.definitions") == null) {
            config.set("modules.betterkillstreaks.definitions.l1.msg", "First Kill!");
            config.set("modules.betterkillstreaks.definitions.l2.msg", "Double Kill!");
            config.set("modules.betterkillstreaks.definitions.l3.msg", "Triple Kill!");
            config.set("modules.betterkillstreaks.definitions.l4.msg", "Quadra Kill!");
            config.set("modules.betterkillstreaks.definitions.l5.msg", "Super Kill!");
            config.set("modules.betterkillstreaks.definitions.l6.msg", "Ultra Kill!");
            config.set("modules.betterkillstreaks.definitions.l7.msg", "Godlike!");
            config.set("modules.betterkillstreaks.definitions.l8.msg", "Monster!");
        }
        this.arena.getConfig().save();
    }

    @Override
    public void parseStart() {
        if (!this.setup) {
            Bukkit.getPluginManager().registerEvents(this, PVPArena.getInstance());
            this.setup = true;
        }
        this.streaks.clear();
    }

    @Override
    public void reset(final boolean force) {
        this.streaks.clear();
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void onPlayerDeath(final PADeathEvent event) {
        this.streaks.remove(event.getPlayer().getName());
    }

    @EventHandler
    public void onPlayerKill(final PAKillEvent event) {
        final int value;
        String playerName = event.getPlayer().getName();
        if (this.streaks.containsKey(playerName)) {
            value = this.streaks.get(playerName) + 1;

        } else {
            value = 1;
        }
        this.streaks.put(playerName, value);
        this.reward(event.getPlayer(), value);
    }

    /**
     * Convert potion effect to string for config file
     * Duration in config is supposed to be in seconds, so duration of effect will be divided by 20
     * @param potionEffect The effect to serialize
     * @return Serialized string at format [type]:[amp]:[duration]
     */
    private String parsePotionEffectToStringCfg(PotionEffect potionEffect) {
        return String.join(":", potionEffect.getType().getName(), String.valueOf(potionEffect.getAmplifier() + 1), String.valueOf(potionEffect.getDuration() / 20));
    }

    private PotionEffect parseStringToPotionEffect(String s) {
        try {
            String[] effectEntry = s.split(":");
            return this.generatePotionEffect(effectEntry[0], effectEntry[1], effectEntry[2]);
        } catch (final Exception e) {
            PVPArena.getInstance().getLogger().warning(String.format("[BetterKillstreaks] Error while parsing effect definition: \"%s\"", s));
            return null;
        }
    }


    private void reward(final Player player, final int level) {
        ConfigurationSection levelSection = this.arena.getConfig().getYamlConfiguration().getConfigurationSection(String.format("modules.betterkillstreaks.definitions.l%d", level));
        if (levelSection != null) {
            Set<String> sectionKeys = levelSection.getKeys(false);
            if (!sectionKeys.isEmpty()) {

                if(sectionKeys.contains("items")) {
                    ItemStack[] items = getItemStacksFromConfig(levelSection.getMapList("items"));
                    player.getInventory().addItem(items);
                }

                if(sectionKeys.contains("effects")) {
                    levelSection.getStringList("effects").stream()
                            .map(this::parseStringToPotionEffect)
                            .filter(Objects::nonNull)
                            .forEach(player::addPotionEffect);
                }

                if(sectionKeys.contains("msg")) {
                    String msg = levelSection.getString("msg");

                    if (StringUtils.notBlank(msg) && !"none".equalsIgnoreCase(msg)) {
                        String playerNameDisplay = player.getName();
                        if (!this.arena.isFreeForAll()) {
                            playerNameDisplay = ArenaPlayer.fromPlayer(player).getArenaTeam().colorizePlayer(player);
                        }
                        this.arena.broadcast(String.format("%s - %s", playerNameDisplay, msg));
                    }
                }
            }
        }
    }
}
