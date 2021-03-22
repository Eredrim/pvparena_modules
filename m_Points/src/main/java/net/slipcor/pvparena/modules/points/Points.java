package net.slipcor.pvparena.modules.points;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.ArenaClass;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.events.PAGoalEvent;
import net.slipcor.pvparena.events.PAPlayerClassChangeEvent;
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
    public void onGoalScore(final PAGoalEvent event) {


        if (event.getArena().equals(arena)) {
            debug(arena, "[POINTS] it's us!");
            final String[] contents = event.getContents();

            String lastTrigger = "";
            for (String node : contents) {
                node = node.toLowerCase();
                if (node.contains("trigger")) {
                    lastTrigger = node.substring(8);
                    newReward(lastTrigger, "TRIGGER");
                }

                if (node.contains("playerDeath")) {
                    newReward(node.substring(12), "DEATH");
                }

                if (node.contains("playerKill")) {
                    final String[] val = node.split(":");
                    if (!val[1].equals(val[2])) {
                        newReward(val[1], "KILL");
                    }
                }

                if (node.contains("score")) {
                    final String[] val = node.split(":");
                    newReward(val[1], "SCORE", Integer.parseInt(val[3]));
                }

                if (node != null && node.isEmpty() && lastTrigger != null && !lastTrigger.isEmpty()) {
                    newReward(lastTrigger, "WIN");
                }
            }

        }
    }

    private void newReward(final String playerName, final String rewardType) {
        if (playerName == null || playerName.length() < 1) {
            PVPArena.getInstance().getLogger().warning("winner is empty string in " + arena.getName());
            return;
        }
        debug(arena, "new Reward: " + playerName + " -> " + rewardType);
        newReward(playerName, rewardType, 1);
    }

    private void newReward(final String playerName, final String rewardType, final int amount) {
        if (playerName == null || playerName.length() < 1) {
            PVPArena.getInstance().getLogger().warning("winner is empty string in " + arena.getName());
            return;
        }
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
                add(playerName, value);
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
