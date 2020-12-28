package net.slipcor.pvparena.modules.specialjoin;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.commands.PAA_Edit;
import net.slipcor.pvparena.commands.PAA_Setup;
import net.slipcor.pvparena.commands.PAG_Join;
import net.slipcor.pvparena.commands.PAG_Spectate;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.loadables.ArenaModule;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.IllegalPluginAccessException;

import java.util.*;

import static net.slipcor.pvparena.config.Debugger.debug;

public class SpecialJoin extends ArenaModule implements Listener {
    public SpecialJoin() {
        super("SpecialJoin");
    }

    private static final Map<PABlockLocation, Arena> places = new HashMap<>();
    private static final Map<String, Arena> selections = new HashMap<>();
    private boolean setup;

    @Override
    public String version() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public boolean checkCommand(final String s) {
        return "setjoin".equals(s.toLowerCase());
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("setjoin");
    }

    @Override
    public void configParse(final YamlConfiguration config) {
        try {
            final List<String> res = config.getStringList("modules.specialjoin.places");
            for (final String s : res) {
                places.put(Config.parseBlockLocation(s), arena);
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        if (!PVPArena.hasAdminPerms(sender)
                && !PVPArena.hasCreatePerms(sender, arena)) {
            arena.msg(sender,
                    Language.parse(MSG.ERROR_NOPERM, Language.parse(MSG.ERROR_NOPERM_X_ADMIN)));
            return;
        }

        // /pa [arenaname] setjoin

        if (selections.containsKey(sender.getName())) {
            // remove & announce
            selections.remove(sender.getName());
            arena.msg(sender,
                    Language.parse(MSG.MODULE_SPECIALJOIN_STOP));
        } else {
            // add & announce
            selections.put(sender.getName(), arena);
            arena.msg(sender,
                    Language.parse(MSG.MODULE_SPECIALJOIN_START));
        }
    }

    @Override
    public void onThisLoad() {
        if (!setup) {
            Bukkit.getPluginManager().registerEvents(this, PVPArena.getInstance());
            setup = true;
        }
    }

    @EventHandler
    public static void onSpecialJoin(final PlayerInteractEvent event) {
        if (event.isCancelled()) {
            debug(event.getPlayer(), "PIA cancelled!");
            return;
        }
        debug(event.getPlayer(), "PIA!");


        if (event.getHand() == null || event.getHand().equals(EquipmentSlot.OFF_HAND)) {
            debug(event.getPlayer(), "exiting: offhand or physical");
            return;
        }

        if (PAA_Edit.activeEdits.containsKey(event.getPlayer().getName()) ||
                PAA_Setup.activeSetups.containsKey(event.getPlayer().getName())) {
            debug(event.getPlayer(), "edit mode. OUT!");
            return;
        }
        if (event.getAction() == Action.PHYSICAL) {

            debug(event.getPlayer(), "Join via pressure plate!");

            if (event.getPlayer() == null) {
                debug(event.getPlayer(), "wth?");
                return;
            }
            final PABlockLocation loc = new PABlockLocation(event.getPlayer().getLocation());

            PABlockLocation find = null;

            for (final PABlockLocation l : places.keySet()) {
                if (l.getWorldName().equals(loc.getWorldName())
                        && l.getDistance(loc) < 0.1f) {
                    find = l;
                }
            }

            if (find == null) {
                debug(event.getPlayer(), "not contained!");
                return;
            }
            final PAG_Join j = new PAG_Join();
            j.commit(places.get(find), event.getPlayer(), new String[0]);
            return;
        }

        if (!event.hasBlock()) {
            debug(event.getPlayer(), "not has block, out!");
            return;
        }

        if (selections.containsKey(event.getPlayer().getName())) {
            debug(event.getPlayer(), "selection contains!");

            final Material mat = event.getClickedBlock().getType();
            final BlockState bState = event.getClickedBlock().getState();
            final String place;

            if (mat.name().contains("_PRESSURE_PLATE")) {
                place = mat.name();
            } else if (mat.name().contains("_BUTTON") || mat == Material.LEVER) {
                place = mat.name();
            } else if (bState instanceof Sign) {
                place = mat.name();
            } else {
                return;
            }
            final Arena a = selections.get(event.getPlayer().getName());
            places.put(new PABlockLocation(event.getClickedBlock().getLocation()), a);
            selections.remove(event.getPlayer().getName());
            a.msg(event.getPlayer(),
                    Language.parse(MSG.MODULE_SPECIALJOIN_DONE, place));
            event.setCancelled(true);
            update(a);
            return;
        }

        final PABlockLocation loc = new PABlockLocation(event.getClickedBlock().getLocation());

        PABlockLocation find = null;

        for (final PABlockLocation l : places.keySet()) {
            if (l.getWorldName().equals(loc.getWorldName())
                    && l.getDistance(loc) < 0.1f) {
                find = l;
            }
        }


        if (find == null) {
            debug(event.getPlayer(), "places does not contain!");
            return;
        }

        event.setCancelled(true);

        final PAG_Join j = new PAG_Join();

        final Material mat = event.getClickedBlock().getType();
        final BlockState bState = event.getClickedBlock().getState();

        if (mat.name().contains("_BUTTON") || mat == Material.LEVER) {
            j.commit(places.get(find), event.getPlayer(), new String[0]);
        } else if (bState instanceof Sign) {
            final Sign s = (Sign) bState;
            final String[] arr = new String[1];
            arr[0] = s.getLine(1); // second line


            if (s.getLine(2) != null && !s.getLine(2).isEmpty()) {
                final PAG_Spectate jj = new PAG_Spectate();
                jj.commit(places.get(find), event.getPlayer(), new String[0]);
            } else {
                j.commit(places.get(find), event.getPlayer(), arr);
            }

        }
    }

    @Override
    public void parseJoin(final CommandSender sender, final ArenaTeam team) {
        updateSignDisplay();
    }

    @Override
    public void parsePlayerLeave(final Player player, final ArenaTeam team) {
        updateSignDisplay();
    }

    @Override
    public void parseStart() {
        updateSignDisplay();
    }

    @Override
    public void reset(final boolean force) {
        updateSignDisplay();
    }

    private static void update(final Arena a) {
        final List<String> locs = new ArrayList<>();
        for (final Map.Entry<PABlockLocation, Arena> paBlockLocationArenaEntry : places.entrySet()) {
            if (a.getName().equals(paBlockLocationArenaEntry.getValue().getName())) {
                locs.add(Config.parseToString(paBlockLocationArenaEntry.getKey()));
            }
        }
        a.getArenaConfig().setManually("modules.specialjoin.places", locs);
        a.getArenaConfig().save();
    }

    private static void updateSignDisplay() {
        class RunLater implements Runnable {

            @Override
            public void run() {

                for (final Map.Entry<PABlockLocation, Arena> paBlockLocationArenaEntry : places.entrySet()) {
                    final Arena arena = paBlockLocationArenaEntry.getValue();
                    if (!arena.getArenaConfig().getBoolean(CFG.MODULES_SPECIALJOIN_SHOWPLAYERS)) {
                        continue;
                    }

                    final BlockState state = paBlockLocationArenaEntry.getKey().toLocation().getBlock().getState();

                    if (!(state instanceof Sign)) {
                        continue;
                    }

                    final Sign sign = (Sign) state;

                    String line = (arena.isFightInProgress() ? ChatColor.GREEN.toString() : arena.isLocked() ? ChatColor.RED.toString() : ChatColor.GOLD.toString()) + arena.getFighters().size();

                    final int maxPlayers = arena.getArenaConfig().getInt(CFG.READY_MAXPLAYERS);

                    if (maxPlayers > 0) {
                        line += " / " + maxPlayers;
                    }

                    sign.setLine(3, line);
                    sign.update();
                }
            }
        }
        try {
            Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), new RunLater(), 3L);
        } catch (final IllegalPluginAccessException e) {
        }
    }
}
