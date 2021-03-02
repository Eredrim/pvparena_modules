package net.slipcor.pvparena.modules.eventactions;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.classes.PABlock;
import net.slipcor.pvparena.commands.PAA_Edit;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.loadables.ArenaRegion;
import net.slipcor.pvparena.managers.SpawnManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class EventActions extends ArenaModule {
    private boolean setup;

    public EventActions() {
        super("EventActions");
    }

    @Override
    public String version() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public boolean checkCommand(final String s) {
        return "setpower".equals(s.toLowerCase());
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("setpower");
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        final Arena a = PAA_Edit.activeEdits.get(sender.getName() + "_power");

        if (a == null) {
            PAA_Edit.activeEdits.put(sender.getName() + "_power", arena);
            arena.msg(sender, Language.parse(MSG.SPAWN_SET_START, "power"));
        } else {
            PAA_Edit.activeEdits.remove(sender.getName() + "_power");
            arena.msg(sender, Language.parse(MSG.SPAWN_SET_DONE, "power"));
        }
    }


    @Override
    public void configParse(final YamlConfiguration config) {
        if (setup) {
            return;
        }
        Bukkit.getPluginManager().registerEvents(new PAListener(this), PVPArena.getInstance());
        setup = true;
    }

    void catchEvent(final String string, final Player p, final Arena a) {
        catchEvent(string, p, a, new String[0]);
    }

    void catchEvent(final String string, final Player p, final Arena a, final String... replacements) {

        if (a == null || !a.equals(arena)) {
            return;
        }

        if (a.getArenaConfig().getUnsafe("event." + string) == null) {
            return;
        }

        final List<String> items = a.getArenaConfig().getStringList("event." + string, new ArrayList<String>());

        final List<String> eachPlayer = new ArrayList<>();

        for (String item : items) {
            if (item.contains("%allplayers%")) {
                for (ArenaPlayer arenaPlayer : a.getFighters()) {
                    eachPlayer.add(item.replace("%allplayers%", arenaPlayer.getName()));
                }
            }
        }

        items.addAll(eachPlayer);

        for (String item : items) {

            for (int pos=0; pos<replacements.length/2; pos+=2) {
                if (replacements[pos] == null || replacements[pos+1] == null) {
                    continue;
                }
                item = item.replace(replacements[pos], replacements[pos+1]);
            }

            if (p != null) {
                item = item.replace("%player%", p.getName());
                final ArenaPlayer aplayer = ArenaPlayer.parsePlayer(p.getName());
                if (aplayer.getArenaTeam() != null) {
                    item = item.replace("%team%", aplayer.getArenaTeam().getName());
                    item = item.replace("%color%", aplayer.getArenaTeam().getColor().toString());
                }
            }

            if (item.contains("%players%")) {
                final String[] players = new String[arena.getFighters().size()];
                int pos = 0;

                for (final ArenaTeam team : arena.getTeams()) {
                    for (final ArenaPlayer arenaPlayer : team.getTeamMembers()) {
                        players[pos++] = team.colorizePlayer(arenaPlayer.getPlayer());
                    }
                }
                item = item.replace("%players%", StringParser.joinArray(players, ChatColor.RESET + ", "));

            }

            item = item.replace("%arena%", a.getName());
            item = ChatColor.translateAlternateColorCodes('&', item);

            final String[] split = item.split("<=>");
            if (split.length < 2) {
                PVPArena.getInstance().getLogger().warning("[PE] skipping: [" + a.getName() + "]:event." + string + "=>" + item);
                continue;
            }
            /*
			items.add("cmd<=>deop %player%");
			items.add("pcmd<=>me joins %arena%");
			items.add("brc<=>Join %arena%!");
			items.add("power<=>power1");
			items.add("switch<=>switch1");
			items.add("msg<=>Welcome to %arena%!");
			items.add("abrc<=>Welcome, %player%");
			items.add("clear<=>battlefield");
			
			
			items.add("brc<=>we had minimum players!<=>minplayers");
			 */

            if (split.length == 3) {
                if ("minplayers".equals(split[2])) {
                    if (arena.getPlayedPlayers().size() < arena.getArenaConfig().getInt(CFG.ITEMS_MINPLAYERS)) {
                        return;
                    }
                }
            }

            if ("cmd".equalsIgnoreCase(split[0])) {
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), split[1]);
            } else if ("pcmd".equalsIgnoreCase(split[0])) {
                class RunLater implements Runnable {

                    @Override
                    public void run() {
                        if (p == null) {
                            PVPArena.getInstance().getLogger().warning("Trying to commit command for null player: " + string);
                        } else {
                            p.performCommand(split[1]);
                        }
                    }

                }
                Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), new RunLater(), 5L);
            } else if ("brc".equalsIgnoreCase(split[0])) {
                Bukkit.broadcastMessage(split[1]);
            } else if ("abrc".equalsIgnoreCase(split[0])) {
                arena.broadcast(split[1]);
            } else if ("clear".equalsIgnoreCase(split[0])) {
                final ArenaRegion ars = arena.getRegion(split[1]);
                if (ars == null && "all".equals(split[1])) {
                    for (final ArenaRegion region : arena.getRegions()) {
                        region.removeEntities();
                    }
                } else if (ars != null) {
                    ars.removeEntities();
                }
            } else if ("power".equalsIgnoreCase(split[0])) {
                for (final PABlock loc : SpawnManager.getPABlocksContaining(a, split[1])) {
                    if (loc.getName().contains("powerup")) {
                        continue;
                    }
                    Bukkit.getScheduler().scheduleSyncDelayedTask(PVPArena.getInstance(), new EADelay(loc.getLocation()), 1L);
                }

            } else if ("msg".equalsIgnoreCase(split[0]) && p != null) {
                p.sendMessage(split[1]);
            }
        }
    }
}
