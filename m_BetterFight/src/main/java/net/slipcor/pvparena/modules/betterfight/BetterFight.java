package net.slipcor.pvparena.modules.betterfight;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.commands.AbstractArenaCommand;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.events.goal.PAGoalEvent;
import net.slipcor.pvparena.events.goal.PAGoalScoreEvent;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.PermissionManager;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
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

public class BetterFight extends ArenaModule {

    private Map<String, Integer> killMap;

    public BetterFight() {
        super("BetterFight");
    }

    @Override
    public String version() {
        return this.getClass().getPackage().getImplementationVersion();
    }

    @Override
    public boolean checkCommand(final String s) {
        return "!bf".equals(s) || "betterfight".equals(s);
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
        result.define(new String[]{"messages"});
        result.define(new String[]{"items"});
        result.define(new String[]{"reset"});
        result.define(new String[]{"explode"});
        result.define(new String[]{"explodeonlyononehit"});
        return result;
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        // !bf messages #
        // !bf items [items]
        // !bf reset
        // !bf explode
        // !bf explodeonlyononehit

        if (!PermissionManager.hasAdminPerm(sender) && !PermissionManager.hasBuilderPerm(sender, this.arena)) {
            this.arena.msg(sender, MSG.ERROR_NOPERM, Language.parse(MSG.ERROR_NOPERM_X_ADMIN));
            return;
        }

        if ("!bf".equals(args[0]) || "betterfight".equals(args[0])) {
            if (args.length == 2) {
                if ("reset".equals(args[1])) {
                    final boolean b = this.arena.getConfig().getBoolean(CFG.MODULES_BETTERFIGHT_RESETKILLSTREAKONDEATH);

                    this.arena.getConfig().set(CFG.MODULES_BETTERFIGHT_RESETKILLSTREAKONDEATH, !b);
                    this.arena.getConfig().save();
                    this.arena.msg(sender, MSG.SET_DONE, CFG.MODULES_BETTERFIGHT_RESETKILLSTREAKONDEATH.getNode(), String.valueOf(!b));
                    return;
                }
                if (args[1].equals("explode")) {
                    boolean b = this.arena.getConfig().getBoolean(CFG.MODULES_BETTERFIGHT_EXPLODEONDEATH);

                    this.arena.getConfig().set(CFG.MODULES_BETTERFIGHT_EXPLODEONDEATH, !b);
                    this.arena.getConfig().save();
                    this.arena.msg(sender, MSG.SET_DONE, CFG.MODULES_BETTERFIGHT_EXPLODEONDEATH.getNode(), String.valueOf(!b));
                    return;
                }
                if (args[1].equals("explodeonlyononehit")) {
                    boolean b = this.arena.getConfig().getBoolean(CFG.MODULES_BETTERFIGHT_EXPLODEONDEATHONLYONONEHIT);

                    this.arena.getConfig().set(CFG.MODULES_BETTERFIGHT_EXPLODEONDEATHONLYONONEHIT, !b);
                    this.arena.getConfig().save();
                    this.arena.msg(sender, MSG.SET_DONE, CFG.MODULES_BETTERFIGHT_EXPLODEONDEATHONLYONONEHIT.getNode(), String.valueOf(!b));
                    return;
                }
                this.arena.msg(sender, MSG.ERROR_ARGUMENT, args[1], "reset | explode | explodeonlyononehit");
                return;
            }
            if ("items".equals(args[1])) {

                if (!AbstractArenaCommand.argCountValid(sender, this.arena, args, new Integer[]{3})) {
                    return;
                }

                this.arena.getConfig().set(CFG.MODULES_BETTERFIGHT_ONEHITITEMS, args[2]);
                this.arena.getConfig().save();
                this.arena.msg(sender, MSG.SET_DONE, CFG.MODULES_BETTERFIGHT_ONEHITITEMS.getNode(), args[2]);
                return;

            }
            if (args[1].equals("messages")) {
                int i;
                try {
                    i = Integer.parseInt(args[2]);
                } catch (Exception e) {
                    this.arena.msg(sender, MSG.ERROR_NOT_NUMERIC, args[2]);
                    return;
                }
                String value = StringParser.joinArray(StringParser.shiftArrayBy(args, 2), " ");
                this.arena.getConfig().setManually("modules.betterfight.messages.m" + i,
                        value);
                this.arena.getConfig().save();
                this.arena.msg(sender, MSG.SET_DONE, "modules.betterfight.messages.m" + i, value);
                return;
            }

            this.arena.msg(sender, MSG.ERROR_ARGUMENT, args[1], "reset | items | messages | explode | explodeonlyononehit");
        }
    }

    @Override
    public void configParse(final YamlConfiguration config) {

        if (config.get("betterfight") != null) {
            final ConfigurationSection cs = config.getConfigurationSection("betterfight");
            final ConfigurationSection newCS = config.getConfigurationSection("modules.betterfight");

            for (final String node : cs.getKeys(true)) {
                newCS.set(node, cs.get(node));
            }

            config.set("betterfight", null);
        }

        if (this.arena.getConfig().getBoolean(CFG.MODULES_BETTERFIGHT_MESSAGES)) {
            config.addDefault("modules.betterfight.messages.m1", "First Kill!");
            config.addDefault("modules.betterfight.messages.m2", "Double Kill!");
            config.addDefault("modules.betterfight.messages.m3", "Triple Kill!");
            config.addDefault("modules.betterfight.messages.m4", "Quadra Kill!");
            config.addDefault("modules.betterfight.messages.m5", "Super Kill!");
            config.addDefault("modules.betterfight.messages.m6", "Ultra Kill!");
            config.addDefault("modules.betterfight.messages.m7", "Godlike!");
            config.addDefault("modules.betterfight.messages.m8", "Monster!");
        }

        config.addDefault("modules.betterfight.sounds.arrow", "none");
        config.addDefault("modules.betterfight.sounds.egg", "none");
        config.addDefault("modules.betterfight.sounds.snow", "none");
        config.addDefault("modules.betterfight.sounds.fireball", "none");

        config.options().copyDefaults(true);
    }

    @Override
    public void displayInfo(final CommandSender sender) {

        sender.sendMessage("one-hit items: " + this.arena.getConfig().getString(
                CFG.MODULES_BETTERFIGHT_ONEHITITEMS));
        sender.sendMessage(StringParser.colorVar("explode",
                this.arena.getConfig().getBoolean(
                        CFG.MODULES_BETTERFIGHT_EXPLODEONDEATH)) + " | " +
                StringParser.colorVar("explodeonlyononehit",
                        this.arena.getConfig().getBoolean(
                        CFG.MODULES_BETTERFIGHT_EXPLODEONDEATHONLYONONEHIT)) + " | " +
                StringParser.colorVar("messages",
                        this.arena.getConfig().getBoolean(
                                CFG.MODULES_BETTERFIGHT_MESSAGES)) + " | " +
                StringParser.colorVar("reset on death",
                        this.arena.getConfig().getBoolean(
                                CFG.MODULES_BETTERFIGHT_RESETKILLSTREAKONDEATH)));

    }

    private Map<String, Integer> getKills() {
        if (this.killMap == null) {
            this.killMap = new HashMap<>();
        }
        return this.killMap;
    }

    @Override
    public void onEntityDamageByEntity(final Player attacker,
                                       final Player defender, final EntityDamageByEntityEvent event) {
        final String s = this.arena.getConfig().getDefinedString(CFG.MODULES_BETTERFIGHT_ONEHITITEMS);
        if (s == null) {
            return;
        }

        if (event.getDamager() instanceof Projectile) {
            if (event.getDamager() instanceof Snowball) {
                this.handle(event, "snow");
                if (s.toLowerCase().contains("snow")) {
                    event.setDamage(1000);
                }
            }
            if (event.getDamager() instanceof Arrow) {
                this.handle(event, "arrow");
                if (s.toLowerCase().contains("arrow")) {
                    event.setDamage(1000);
                }
            }
            if (event.getDamager() instanceof Fireball) {
                this.handle(event, "fireball");
                if (s.toLowerCase().contains("fireball")) {
                    event.setDamage(1000);
                }
            }
            if (event.getDamager() instanceof Egg) {
                this.handle(event, "egg");
                if (s.toLowerCase().contains("egg")) {
                    event.setDamage(1000);
                }
            }
        }
    }

    private void handle(final EntityDamageByEntityEvent event, final String string) {
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
                if (event.getEntity() instanceof Player) {
                    final Player damagee = (Player) event.getEntity();

                    damagee.playSound(shooter.getLocation(), sound, volume, pitch);
                }
            } catch (final Exception e) {
                PVPArena.getInstance().getLogger().warning("Node " + node + " is not a valid sound in arena " + this.arena.getName());
            }
        }
    }

    @Override
    public void parsePlayerDeath(final Player player,
                                 final EntityDamageEvent cause) {
        final Player p = ArenaPlayer.getLastDamagingPlayer(cause);

        if (this.arena.getConfig().getBoolean(CFG.MODULES_BETTERFIGHT_RESETKILLSTREAKONDEATH)) {
            this.getKills().put(player.getName(), 0);
        }

        if (this.arena.getConfig().getBoolean(CFG.MODULES_BETTERFIGHT_EXPLODEONDEATH)) {
            if (cause.getDamage() == 1000 || !this.arena.getConfig().getBoolean(CFG.MODULES_BETTERFIGHT_EXPLODEONDEATHONLYONONEHIT)) {
                class RunLater implements Runnable {
                    final World world;
                    final double x;
                    final double y;
                    final double z;
                    public RunLater(final World world, final double x, final double y, final double z) {
                        this.world = world;
                        this.x = x;
                        this.y = y;
                        this.z = z;
                    }

                    @Override
                    public void run() {
                        this.world.createExplosion(this.x, this.y, this.z, 2.0f, false, false);
                    }

                }
                Bukkit.getScheduler().scheduleSyncDelayedTask(PVPArena.getInstance(), new RunLater(player.getLocation().getWorld(), player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ()), 2L);
            }
        }

        if (p == null || this.getKills().get(p.getName()) == null) {
            return;
        }
        int killcount = this.getKills().get(p.getName());

        this.getKills().put(p.getName(), ++killcount);

        if (!this.arena.getConfig().getBoolean(CFG.MODULES_BETTERFIGHT_MESSAGES)) {
            return;
        }

        final String msg = (String) this.arena.getConfig().getUnsafe("modules.betterfight.messages.m" + killcount);

        ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(p);
        final PAGoalEvent scoreEvent = new PAGoalScoreEvent(this.arena, null, arenaPlayer, arenaPlayer.getArenaTeam(), (long) killcount);
        Bukkit.getPluginManager().callEvent(scoreEvent);

        // content[X].contains(score) => "score:player:team:value"

        if (msg == null || msg != null && msg.isEmpty()) {
            return;
        }

        this.arena.broadcast(msg);
    }

    @Override
    public void parseStart() {
        for (final ArenaTeam team : this.arena.getTeams()) {
            for (final ArenaPlayer ap : team.getTeamMembers()) {
                this.getKills().put(ap.getName(), 0);
            }
        }
    }

    @Override
    public void reset(final boolean force) {
        this.getKills().clear();
    }
}
