package net.slipcor.pvparena.modules.points;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.ArenaClass;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.events.PADeathEvent;
import net.slipcor.pvparena.events.PAKillEvent;
import net.slipcor.pvparena.events.PAPlayerClassChangeEvent;
import net.slipcor.pvparena.events.PAWinEvent;
import net.slipcor.pvparena.events.goal.PAGoalScoreEvent;
import net.slipcor.pvparena.events.goal.PAGoalTriggerEvent;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static net.slipcor.pvparena.config.Debugger.debug;

public class Points extends ArenaModule implements Listener {
    private static final Map<String, Double> globalpoints = new HashMap<>();
    private final Map<String, Double> points = new HashMap<>();
    private static YamlConfiguration globalconfig;
    private static File gcf;

    public Points() {
        super("Points");
    }

    @Override
    public String version() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public void configParse(final YamlConfiguration config) {
        Bukkit.getPluginManager().registerEvents(this, PVPArena.getInstance());
        for (final ArenaClass aClass : arena.getClasses()) {
            config.addDefault("modules.points.classes." + aClass.getName(), 0.0d);
        }

        if (arena.getConfig().getBoolean(CFG.MODULES_POINTS_GLOBAL)) {
            if (globalconfig == null) {
                gcf = new File(PVPArena.getInstance().getDataFolder(), "points.yml");

                if (gcf.exists()) {
                    globalconfig = YamlConfiguration.loadConfiguration(gcf);
                } else {
                    try {
                        gcf.createNewFile();
                        globalconfig = YamlConfiguration.loadConfiguration(gcf);
                        globalconfig.addDefault("slipcor", 0.0d);
                        globalconfig.save(gcf);
                    } catch (IOException e) {
                        globalconfig = null;
                        e.printStackTrace();
                    }
                }

                if (config.get("modules.points.players") == null) {
                    return;
                }

                final ConfigurationSection cs = config.getConfigurationSection("modules.points.players");
                for (final String playerName : globalconfig.getKeys(false)) {
                    globalpoints.put(playerName, cs.getDouble(playerName));
                }
            }
        } else {
            if (config.get("modules.points.players") == null) {
                return;
            }

            final ConfigurationSection cs = config.getConfigurationSection("modules.points.players");
            for (final String playerName : cs.getKeys(false)) {
                points.put(playerName, cs.getDouble(playerName));
            }
        }

    }

    @Override
    public void displayInfo(final CommandSender player) {
        player.sendMessage(StringParser.colorVar(
                "global", arena.getConfig().getBoolean(
                        CFG.MODULES_POINTS_GLOBAL)));
    }

    @Override
    public void resetPlayer(final Player player, final boolean soft, final boolean force) {
        if (arena.getConfig().getBoolean(CFG.MODULES_POINTS_GLOBAL)) {
            globalconfig.set(player.getName(), globalpoints.get(player.getName()));
        } else {
            arena.getConfig().setManually("modules.points.players." + player.getName(), points.get(player.getName()));
        }
    }

    @Override
    public void reset(final boolean force) {
        if (arena.getConfig().getBoolean(CFG.MODULES_POINTS_GLOBAL)) {
            try {
                globalconfig.save(gcf);
            } catch (final IOException e) {
                e.printStackTrace();
            }
        } else {
            arena.getConfig().save();
        }

    }

    @Override
    public boolean cannotSelectClass(final Player player, final String className) {
        final Object o = arena.getConfig().getUnsafe("modules.points.classes." + className);

        if (o == null) {
            // no requirement, out!
            return false;
        }

        final double d = (Double) o;

        if (arena.getConfig().getBoolean(CFG.MODULES_POINTS_GLOBAL)) {
            if (globalpoints.containsKey(player.getName())) {
                return globalpoints.get(player.getName()) < d;
            }
        } else {
            if (points.containsKey(player.getName())) {
                return points.get(player.getName()) < d;
            }
        }
        return d > 0.0d;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClassChange(final PAPlayerClassChangeEvent event) {
        if (event.getArenaClass() == null || event.getArena() == null || !event.getArena().equals(arena)) {
            return;
        }
        final Object o = arena.getConfig().getUnsafe("modules.points.classes." + event.getArenaClass().getName());

        if (o == null) {
            // no requirement, out!
            return;
        }

        final double d = (Double) o;

        remove(event.getPlayer().getName(), d);
    }

    @EventHandler
    public void onPADeathEvent(PADeathEvent event) {
        this.newReward(event.getPlayer(), "DEATH");
    }

    @EventHandler
    public void onPAKillEvent(PAKillEvent event) {
        this.newReward(event.getPlayer(), "KILL");
    }

    @EventHandler
    public void onPAScoreEvent(PAGoalScoreEvent event) {
        this.newReward(event.getArenaPlayer().getPlayer(), "SCORE", event.getPoints());
    }

    @EventHandler
    public void onPAWinEvent(PAWinEvent event) {
        this.newReward(event.getPlayer(), "WIN");
    }

    @EventHandler
    public void onWinTriggerEvent(PAGoalTriggerEvent event) {
        if(event.getTriggerPlayer() != null) {
            this.newReward(event.getTriggerPlayer().getPlayer(), "TRIGGER");
        }
    }

    private void newReward(final Player player, final String rewardType) {
        this.newReward(player, rewardType, 1);
    }

    private void newReward(final Player player, final String rewardType, final long amount) {
        if (player == null) {
            PVPArena.getInstance().getLogger().warning("[Points] winner is unknown for " + this.arena.getName());
            return;
        }
        String playerName = player.getName();
        debug(arena, "new Reward: " + amount + "x " + playerName + " -> " + rewardType);
        try {

            final double value = arena.getConfig().getDouble(
                    CFG.valueOf("MODULES_POINTS_REWARD_" + rewardType), 0.0d);

            final double maybevalue = arena.getConfig().getDouble(
                    CFG.valueOf("MODULES_POINTS_REWARD_" + rewardType), -1.0d);

            if (maybevalue < 0) {
                PVPArena.getInstance().getLogger().warning("config value is not set: " + CFG.valueOf("MODULES_POINTS_REWARD_" + rewardType).getNode());
            }

            debug(arena, "9 depositing " + value + " to " + playerName);
            if (value > 0) {
                this.add(playerName, value);
                try {

                    ArenaModuleManager.announce(
                            arena,
                            Language.parse(MSG.NOTICE_PLAYERAWARDED,
                                    value + " points"), "PRIZE");
                    arena.msg(Bukkit.getPlayer(playerName), Language
                            .parse(MSG.MODULE_VAULT_YOUWON, value + " points"));

                } catch (final Exception e) {
                    // nothing
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    private void add(final String playerName, final double value) {
        if (arena.getConfig().getBoolean(CFG.MODULES_POINTS_GLOBAL)) {
            if (globalpoints.containsKey(playerName)) {
                final double d = globalpoints.get(playerName);
                globalpoints.put(playerName, value + d);
            } else {
                globalpoints.put(playerName, value);
            }
        } else {
            if (points.containsKey(playerName)) {
                final double d = points.get(playerName);
                points.put(playerName, value + d);
            } else {
                points.put(playerName, value);
            }
        }
    }

    private void remove(final String playerName, final double value) {
        if (arena.getConfig().getBoolean(CFG.MODULES_POINTS_GLOBAL)) {
            if (globalpoints.containsKey(playerName)) {
                final double d = globalpoints.get(playerName);
                globalpoints.put(playerName, d - value);
            }
        } else {
            if (points.containsKey(playerName)) {
                final double d = points.get(playerName);
                points.put(playerName, d - value);
            }
        }
    }
}
