package net.slipcor.pvparena.modules.betterfight;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.core.CollectionUtils;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.PermissionManager;
import net.slipcor.pvparena.regions.RegionProtection;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static net.slipcor.pvparena.commands.AbstractArenaCommand.argCountValid;
import static net.slipcor.pvparena.core.CollectionUtils.containsIgnoreCase;

public class BetterFight extends ArenaModule {

    private Map<String, Integer> playerKillMap;

    private static final List<String> ONE_SHOT_ITEMS = asList("SNOWBALL", "EGG", "ARROW", "FIREBALL");

    public BetterFight() {
        super("BetterFight");
    }

    @Override
    public String version() {
        return this.getClass().getPackage().getImplementationVersion();
    }

    @Override
    public boolean checkCommand(final String s) {
        return "!bf".equalsIgnoreCase(s) || "betterfight".equalsIgnoreCase(s);
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("betterfight");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!bf");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        result.define(new String[]{"explode"});
        result.define(new String[]{"explodeOnlyWithOneShot"});
        Stream.of("add", "remove", "sound").forEach(action -> {
            ONE_SHOT_ITEMS.forEach(item -> {
                if("sound".equals(action)) {
                    for (Sound sound : Sound.values()) {
                        result.define(new String[]{"items", action, item, sound.name()});
                    }
                } else {
                    result.define(new String[]{"items", action, item});
                }
            });
        });
        return result;
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        // !bf items add/remove/sound [item] (sound)
        // !bf explode
        // !bf explodeonlywithoneshot

        if (!PermissionManager.hasAdminPerm(sender) && !PermissionManager.hasBuilderPerm(sender, this.arena)) {
            this.arena.msg(sender, MSG.ERROR_NOPERM, Language.parse(MSG.ERROR_NOPERM_X_ADMIN));
            return;
        }

        Config config = this.arena.getConfig();
        if (args.length == 2) {
            Map<String, CFG> cmdMapping = new HashMap<>();
            cmdMapping.put("explode", CFG.MODULES_BETTERFIGHT_EXPLODEONDEATH);
            cmdMapping.put("explodeonlywithoneshot", CFG.MODULES_BETTERFIGHT_EXPLODEONLYWITHONESHOTITEM);

            if (cmdMapping.containsKey(args[1].toLowerCase())) {
                CFG cfgKey = cmdMapping.get(args[1].toLowerCase());
                boolean b = config.getBoolean(cfgKey);

                config.set(cfgKey, !b);

                config.save();
                this.arena.msg(sender, MSG.SET_DONE, cfgKey.getNode(), String.valueOf(!b));
                return;
            }

            this.arena.msg(sender, MSG.ERROR_ARGUMENT, args[1], "reset | explode | explodeonlywithoneshot");
            return;
        }
        if ("items".equalsIgnoreCase(args[1])) {

            if (!"sound".equalsIgnoreCase(args[2]) && !argCountValid(sender, this.arena, args, new Integer[]{4})) {
                this.arena.msg(sender, MSG.ERROR_INVALID_ARGUMENT_COUNT, String.valueOf(args.length), String.valueOf(4));
                return;
            }

            if(!containsIgnoreCase(ONE_SHOT_ITEMS, args[3])) {
                this.arena.msg(sender, MSG.ERROR_INVALID_VALUE, args[3]);
                return;
            }

            List<String> onHitItems = config.getStringList(CFG.MODULES_BETTERFIGHT_ONESHOTITEMS);
            if("add".equalsIgnoreCase(args[2])) {
                if(containsIgnoreCase(onHitItems, args[3])) {
                    this.arena.msg(sender, MSG.ERROR_EXISTING_VALUE, args[3].toUpperCase(), CFG.MODULES_BETTERFIGHT_ONESHOTITEMS.getNode());
                    return;
                }
                onHitItems.add(args[3].toUpperCase());
                config.set(CFG.MODULES_BETTERFIGHT_ONESHOTITEMS, onHitItems);
                this.arena.msg(sender, MSG.ADD_DONE, CFG.MODULES_BETTERFIGHT_ONESHOTITEMS.getNode(), args[3].toUpperCase());

            } else if("remove".equalsIgnoreCase(args[2])) {
                if(!containsIgnoreCase(onHitItems, args[3])) {
                    this.arena.msg(sender, MSG.ERROR_NON_EXISTING_VALUE, args[3].toUpperCase(), CFG.MODULES_BETTERFIGHT_ONESHOTITEMS.getNode());
                    return;
                }
                onHitItems.removeIf(str -> str.equalsIgnoreCase(args[3]));
                config.set(CFG.MODULES_BETTERFIGHT_ONESHOTITEMS, onHitItems);
                this.arena.msg(sender, MSG.ADD_DONE, CFG.MODULES_BETTERFIGHT_ONESHOTITEMS.getNode(), args[3].toUpperCase());

            } else if("sound".equalsIgnoreCase(args[2])) {
                if (!argCountValid(sender, this.arena, args, new Integer[]{5})) {
                    this.arena.msg(sender, MSG.ERROR_INVALID_ARGUMENT_COUNT, String.valueOf(args.length), String.valueOf(4));
                    return;
                }

                try {
                    String settingPath = String.format("modules.betterfight.sounds.%s", args[3].toLowerCase());
                    if("none".equalsIgnoreCase(args[4])) {
                        config.setManually(settingPath, "none");
                    } else {
                        Sound sound = Sound.valueOf(args[4].toUpperCase());
                        config.setManually(settingPath, sound.name());
                    }
                    this.arena.msg(sender, MSG.SET_DONE, settingPath, args[4].toUpperCase());
                } catch (IllegalArgumentException ignored) {
                    this.arena.msg(sender, MSG.ERROR_INVALID_VALUE, args[3]);
                }
            }

            config.save();
            return;

        }

        this.arena.msg(sender, MSG.ERROR_ARGUMENT, args[1], "reset | items | explode | explodeonlywithoneshot");
    }

    @Override
    public void initConfig() {
        this.configParse(this.arena.getConfig().getYamlConfiguration());
    }

    @Override
    public void configParse(final YamlConfiguration config) {
        config.addDefault("modules.betterfight.sounds.arrow", "none");
        config.addDefault("modules.betterfight.sounds.egg", "none");
        config.addDefault("modules.betterfight.sounds.snowball", "none");
        config.addDefault("modules.betterfight.sounds.fireball", "none");

        config.options().copyDefaults(true);
    }

    @Override
    public void displayInfo(final CommandSender sender) {

        Config config = this.arena.getConfig();
        sender.sendMessage("one-hit items: " + config.getString(CFG.MODULES_BETTERFIGHT_ONESHOTITEMS));
        sender.sendMessage(StringParser.colorVar("explode", config.getBoolean(CFG.MODULES_BETTERFIGHT_EXPLODEONDEATH)) + " | " +
                StringParser.colorVar("explodeonlywithoneshot", config.getBoolean(CFG.MODULES_BETTERFIGHT_EXPLODEONLYWITHONESHOTITEM))
        );
    }


    @Override
    public void onEntityDamageByEntity(Player attacker, Player defender, EntityDamageByEntityEvent event) {

        if (event.getDamager() instanceof Projectile && event.getDamage() != 1000) {

            List<String> oneShotProjectiles = this.arena.getConfig().getStringList(CFG.MODULES_BETTERFIGHT_ONESHOTITEMS);
            if (CollectionUtils.isEmpty(oneShotProjectiles)) {
                return;
            }

            EntityDamageEvent oneShotEvent = new EntityDamageByEntityEvent(event.getDamager(), event.getEntity(), event.getCause(), 1000);

            if (event.getDamager() instanceof Snowball && oneShotProjectiles.contains("SNOWBALL")) {
                this.playSound(event, "snowball");
                event.setDamage(0);
                Bukkit.getPluginManager().callEvent(oneShotEvent);
            }
            if (event.getDamager() instanceof Arrow && oneShotProjectiles.contains("ARROW")) {
                this.playSound(event, "arrow");
                event.setDamage(0);
                Bukkit.getPluginManager().callEvent(oneShotEvent);
            }
            if (event.getDamager() instanceof Fireball && oneShotProjectiles.contains("FIREBALL")) {
                this.playSound(event, "fireball");
                event.setDamage(0);
                Bukkit.getPluginManager().callEvent(oneShotEvent);
            }
            if (event.getDamager() instanceof Egg && oneShotProjectiles.contains("EGG")) {
                this.playSound(event, "egg");
                event.setDamage(0);
                Bukkit.getPluginManager().callEvent(oneShotEvent);
            }
        }
    }

    private void playSound(final EntityDamageByEntityEvent event, final String string) {
        if (((Projectile) event.getDamager()).getShooter() instanceof Player) {
            final Player shooter = (Player) ((Projectile) event.getDamager()).getShooter();
            final String node = "modules.betterfight.sounds." + string;
            final String value = (String) this.arena.getConfig().getUnsafe(node);

            if ("none".equals(value)) {
                return;
            }

            try {
                final Sound sound = Sound.valueOf(value.toUpperCase());
                final float pitch = 1.0f;
                final float volume = 1.0f;
                shooter.playSound(shooter.getLocation(), sound, volume, pitch);

            } catch (IllegalArgumentException e) {
                PVPArena.getInstance().getLogger().warning(String.format("Node %s is not a valid sound in arena %s", node, this.arena.getName()));
            }
        }
    }

    @Override
    public void parsePlayerDeath(final Player player,
                                 final EntityDamageEvent cause) {
        final Player p = ArenaPlayer.getLastDamagingPlayer(cause);

        if (this.arena.getConfig().getBoolean(CFG.MODULES_BETTERFIGHT_EXPLODEONDEATH)) {
            if (cause.getDamage() == 1000 || !this.arena.getConfig().getBoolean(CFG.MODULES_BETTERFIGHT_EXPLODEONLYWITHONESHOTITEM)) {
                this.planExplosion(player.getLocation());
            }
        }
    }

    private void planExplosion(Location location) {
        Location savedLocation = location.clone();
        final boolean shouldPreventBreak = this.arena.hasRegionsProtectingLocation(savedLocation, RegionProtection.TNT) ||
                this.arena.hasRegionsProtectingLocation(savedLocation, RegionProtection.TNTBREAK);
        Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), () -> {
            savedLocation.getWorld().createExplosion(savedLocation.getX(), savedLocation.getY(), savedLocation.getZ(), 2f, false, !shouldPreventBreak);
        }, 2);
    }
}
