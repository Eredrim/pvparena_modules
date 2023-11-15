package net.slipcor.pvparena.modules.eventactions;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.regions.ArenaRegion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class EventActions extends ArenaModule {

    private PAListener listener;
    private static final String SETTINGS_PATH = "modules.eventactions";

    public EventActions() {
        super("EventActions");
    }

    @Override
    public String version() {
        return this.getClass().getPackage().getImplementationVersion();
    }

    @Override
    public void checkJoin(Player player) {
        // Listener is instantiated on the fly before the first join
        if (this.listener == null) {
            this.listener = new PAListener(this);
            Bukkit.getPluginManager().registerEvents(this.listener, PVPArena.getInstance());
        }
    }

    @Override
    public void reset(boolean force) {
        // Listener is removed after arena end
        if (this.listener != null) {
            HandlerList.unregisterAll(this.listener);
            this.listener = null;
        }
    }

    void catchEvent(final EventName eventName, final Player p, final Arena a) {
        this.catchEvent(eventName, p, a, new String[0]);
    }

    void catchEvent(final EventName eventName, final Player player, final Arena arena, final String... replacements) {

        if (!this.arena.equals(arena)) {
            return;
        }

        String eventSetting = String.format("%s.%s", SETTINGS_PATH, eventName.toString().toLowerCase());
        if (arena.getConfig().getUnsafe(eventSetting) == null) {
            return;
        }

        List<String> actionsForEvent = arena.getConfig().getStringList(eventSetting, new ArrayList<>());
        List<String> eachPlayerActions = new ArrayList<>();

        actionsForEvent.stream()
                .filter(item -> item.contains("%allplayers%"))
                .forEach(item -> arena.getFighters().stream()
                        .map(arenaPlayer -> item.replace("%allplayers%", arenaPlayer.getName()))
                        .forEach(eachPlayerActions::add)
                );

        actionsForEvent.addAll(eachPlayerActions);

        for (String item : actionsForEvent) {

            for (int pos=0; pos<replacements.length/2; pos+=2) {
                if (replacements[pos] == null || replacements[pos+1] == null) {
                    continue;
                }
                item = item.replace(replacements[pos], replacements[pos+1]);
            }

            if (player != null) {
                item = item.replace("%player%", player.getName());
                final ArenaPlayer aplayer = ArenaPlayer.fromPlayer(player);
                if (aplayer.getArenaTeam() != null) {
                    item = item.replace("%team%", aplayer.getArenaTeam().getName());
                    item = item.replace("%color%", aplayer.getArenaTeam().getColor().toString());
                }
            }

            if (item.contains("%players%")) {
                String coloredPlayerList = this.arena.getTeams().stream()
                        .flatMap(arenaTeam -> arenaTeam.getTeamMembers().stream().map(arenaTeam::colorizePlayer))
                        .collect(Collectors.joining(ChatColor.RESET + ", "));
                item = item.replace("%players%", coloredPlayerList);

            }

            item = item.replace("%arena%", arena.getName());
            item = ChatColor.translateAlternateColorCodes('&', item);

            final String[] split = item.split("<=>");
            if (split.length < 2) {
                PVPArena.getInstance().getLogger().warning("[PE] skipping: [" + arena.getName() + "]:event." + eventName + "=>" + item);
                continue;
            }

            this.applyAction(eventName, player, arena, split[0], split[1]);
        }
    }

    /*
    items.add("cmd<=>deop %player%");
    items.add("pcmd<=>me joins %arena%");
    items.add("brc<=>Join %arena%!");
    items.add("power<=>world,x,y,z");
    items.add("msg<=>Welcome to %arena%!");
    items.add("abrc<=>Welcome, %player%");
    items.add("clear<=>battlefield");
     */
    private void applyAction(EventName eventName, Player p, Arena a, String action, String args) {
        if ("cmd".equalsIgnoreCase(action)) {
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), args);
        } else if ("pcmd".equalsIgnoreCase(action)) {
            Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), () -> {
                if (p == null) {
                    PVPArena.getInstance().getLogger().warning("Trying to commit command for null player: " + eventName);
                } else {
                    p.performCommand(args);
                }
            }, 5L);
        } else if ("brc".equalsIgnoreCase(action)) {
            Bukkit.broadcastMessage(args);
        } else if ("abrc".equalsIgnoreCase(action)) {
            this.arena.broadcast(args);
        } else if ("clear".equalsIgnoreCase(action)) {
            final ArenaRegion ars = this.arena.getRegion(args);
            if (ars == null && "all".equals(args)) {
                this.arena.getRegions().forEach(ArenaRegion::removeEntities);
            } else if (ars != null) {
                ars.removeEntities();
            }
        } else if ("power".equalsIgnoreCase(action)) {
            try {
                PABlockLocation paLoc = new PABlockLocation(args);
                Bukkit.getScheduler().scheduleSyncDelayedTask(PVPArena.getInstance(), new EADelay(paLoc), 1L);
            } catch (IndexOutOfBoundsException exception) {
                String message = String.format("[%s] Location format of \"power\" eventAction is incorrect. Right syntax is: world,x,y,z", this.arena.getName());
                PVPArena.getInstance().getLogger().warning(message);
            }

        } else if ("msg".equalsIgnoreCase(action) && p != null) {
            p.sendMessage(args);
        }
    }
}
