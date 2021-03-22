package net.slipcor.pvparena.modules.autovote;

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
import net.slipcor.pvparena.events.PAJoinEvent;
import net.slipcor.pvparena.exceptions.GameplayException;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.ArenaManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.Map.Entry;

import static net.slipcor.pvparena.config.Debugger.debug;

public class AutoVote extends ArenaModule implements Listener {
    static final Map<String, String> votes = new HashMap<>();

    AutoVoteRunnable vote;
    final Set<ArenaPlayer> players = new HashSet<>();

    public AutoVote() {
        super("AutoVote");
    }

    @Override
    public String version() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public boolean checkCommand(final String cmd) {
        return cmd.startsWith("vote") || "!av".equals(cmd) || "autovote".equals(cmd) || "votestop".equals(cmd);
    }

    @Override
    public List<String> getMain() {
        return Arrays.asList("vote", "autovote", "votestop");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!av");
    }

    @Override
    public boolean hasPerms(final CommandSender sender, final Arena arena) {
        return super.hasPerms(sender, arena) || PVPArena.hasPerms(sender, arena);
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        result.define(new String[]{"everyone"});
        result.define(new String[]{"readyup"});
        result.define(new String[]{"seconds"});
        return result;
    }

    @Override
    public void checkJoin(Player player) throws GameplayException {
        if (this.vote != null) {
            throw new GameplayException("voting");
        }

        if (!this.arena.getConfig().getBoolean(CFG.PERMS_JOINWITHSCOREBOARD)) {
            for (final Team team : player.getScoreboard().getTeams()) {
                for (final String playerName : team.getEntries()) {
                    if (playerName.equals(player.getName())) {
                        throw new GameplayException(Language.parse(MSG.ERROR_COMMAND_BLOCKED, "You already have a scoreboard!"));
                    }
                }
            }
        }
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {

        if (args[0].startsWith("vote")) {

            if (!arena.getConfig().getBoolean(CFG.PERMS_JOINWITHSCOREBOARD)) {
                final Player p = (Player) sender;

                for (final Team team : p.getScoreboard().getTeams()) {
                    for (final String playerName : team.getEntries()) {
                        if (playerName.equals(p.getName())) {
                            return;
                        }
                    }
                }
            }

            votes.put(sender.getName(), arena.getName());
            arena.msg(sender, Language.parse(MSG.MODULE_AUTOVOTE_YOUVOTED, arena.getName()));
        } else if ("votestop".equals(args[0])) {
            if (vote != null) {
                vote.cancel();
            }
        } else {
            if (!PVPArena.hasAdminPerms(sender)
                    && !PVPArena.hasCreatePerms(sender, arena)) {
                arena.msg(
                        sender,
                        Language.parse(MSG.ERROR_NOPERM,
                                Language.parse(MSG.ERROR_NOPERM_X_ADMIN)));
                return;
            }

            if (!AbstractArenaCommand.argCountValid(sender, arena, args, new Integer[]{2, 3})) {
                return;
            }

            // !av everyone
            // !av readyup X
            // !av seconds X

            if (args.length < 3 || "everyone".equals(args[1])) {
                final boolean b = arena.getConfig().getBoolean(CFG.MODULES_ARENAVOTE_EVERYONE);
                arena.getConfig().set(CFG.MODULES_ARENAVOTE_EVERYONE, !b);
                arena.getConfig().save();
                arena.msg(sender, Language.parse(MSG.SET_DONE, CFG.MODULES_ARENAVOTE_EVERYONE.getNode(), String.valueOf(!b)));
                return;
            }
            CFG c = null;
            if (args[1].equals("readyup")) {
                c = CFG.MODULES_ARENAVOTE_READYUP;
            } else if (args[1].equals("seconds")) {
                c = CFG.MODULES_ARENAVOTE_SECONDS;
            }
            if (c != null) {
                int i;
                try {
                    i = Integer.parseInt(args[2]);
                } catch (Exception e) {
                    arena.msg(sender, Language.parse(MSG.ERROR_NOT_NUMERIC, args[2]));
                    return;
                }

                arena.getConfig().set(c, i);
                arena.getConfig().save();
                arena.msg(sender, Language.parse(MSG.SET_DONE, c.getNode(), String.valueOf(i)));
                return;
            }

            arena.msg(sender, Language.parse(MSG.ERROR_ARGUMENT, args[1], "everyone | readyup | seconds"));
        }
    }

    @Override
    public void displayInfo(final CommandSender player) {
        player.sendMessage("seconds:"
                + StringParser.colorVar(arena.getConfig().getInt(CFG.MODULES_ARENAVOTE_SECONDS))
                + " | readyup: "
                + StringParser.colorVar(arena.getConfig().getInt(CFG.MODULES_ARENAVOTE_READYUP))
                + " | "
                + StringParser.colorVar("everyone", arena.getConfig().getBoolean(CFG.MODULES_ARENAVOTE_EVERYONE)));
    }

    @Override
    public void reset(final boolean force) {

        final String definition = getDefinitionFromArena(arena);

        if (definition == null) {
            removeFromVotes(arena.getName());
        } else {
            removeValuesFromVotes(definition);
        }

        if (vote == null) {

            for (final String def : ArenaManager.getShortcutValues().keySet()) {
                if (ArenaManager.getShortcutValues().get(def).equals(arena)) {

                    vote = new AutoVoteRunnable(arena,
                            arena.getConfig().getInt(CFG.MODULES_ARENAVOTE_SECONDS), this, def);
                    break;
                }
            }
            if (vote == null) {

                debug(arena, "AutoVote not setup via shortcuts, ignoring");
                vote = new AutoVoteRunnable(arena,
                        arena.getConfig().getInt(CFG.MODULES_ARENAVOTE_SECONDS), this, null);
            }
        }
    }

    private void removeValuesFromVotes(final String definition) {
        boolean done = false;
        while (!done) {
            done = true;
            for (final Entry<String, String> stringStringEntry : votes.entrySet()) {
                if (stringStringEntry.getValue().equalsIgnoreCase(definition)) {
                    votes.remove(stringStringEntry.getKey());
                    done = false;
                    break;
                }
            }
        }
    }

    private void removeFromVotes(final String name) {
        final List<String> names = ArenaManager.getShortcutDefinitions().get(name);

        if (names != null) {
            for (final String arena : names) {
                boolean done = false;
                while (!done) {
                    done = true;
                    for (final Entry<String, String> stringStringEntry : votes.entrySet()) {
                        if (stringStringEntry.getValue().equalsIgnoreCase(arena)) {
                            votes.remove(stringStringEntry.getKey());
                            done = false;
                            break;
                        }
                    }
                }
            }
        }
    }

    private static String getDefinitionFromArena(final Arena arena) {
        for (final String name : ArenaManager.getShortcutValues().keySet()) {
            if (arena.equals(ArenaManager.getShortcutValues().get(name))) {
                return name;
            }
        }

        for (final Entry<String, List<String>> name : ArenaManager.getShortcutDefinitions().entrySet()) {
            if (name.getValue().contains(arena.getName())) {
                return name.getKey();
            }
        }

        return null;
    }

    public static void commit(final String definition, final Set<ArenaPlayer> players) {
        final Map<String, String> tempVotes = new HashMap<>();

        debug("committing definition {} for {}", definition, players.size());

        final List<String> arenas = ArenaManager.getShortcutDefinitions().get(definition);

        if (arenas == null || arenas.size() < 1) {
            debug("this definition has no arenas!");
            return;
        }

        for (final Entry<String, String> voteEntry : votes.entrySet()) {
            debug("{} voted {}", voteEntry.getKey(), voteEntry.getValue());
            if (!arenas.contains(voteEntry.getValue())) {
                debug("not our business!");
                continue;
            }
            tempVotes.put(voteEntry.getKey(), voteEntry.getValue());
        }

        final HashMap<String, Integer> counts = new HashMap<>();
        int max = 0;

        String voted = null;

        for (final String name : tempVotes.values()) {
            int i = 0;
            if (counts.containsKey(name)) {
                i = counts.get(name);
            }

            counts.put(name, ++i);

            if (i > max) {
                max = i;
                voted = name;
            }
        }
        debug("max voted: {}", voted);

        Arena a = ArenaManager.getArenaByName(voted);

        if (a == null || !ArenaManager.getShortcutDefinitions().get(definition).contains(a.getName())) {
            PVPArena.getInstance().getLogger().warning("Vote resulted in NULL for result '" + voted + "'!");

            ArenaManager.advance(definition);
            debug("getting next definition value");
            a = ArenaManager.getShortcutValues().get(definition);
        }

        if (a == null) {
            debug("this didn't work oO - still null!");
            return;
        }

        ArenaManager.getShortcutValues().put(definition, a);
    }

    @Override
    public void onThisLoad() {
        boolean active = false;

        for (final Arena arena : ArenaManager.getArenas()) {
            for (final ArenaModule mod : arena.getMods()) {
                if (mod.getName().equals(getName())) {
                    Bukkit.getPluginManager().registerEvents((AutoVote) mod, PVPArena.getInstance());
                    if (!arena.getConfig().getBoolean(CFG.MODULES_ARENAVOTE_AUTOSTART)) {
                        continue;
                    }
                    Bukkit.getPluginManager().registerEvents((AutoVote) mod, PVPArena.getInstance());
                    active = true;
                }
            }
        }

        if (!active) {
            return;
        }

        class RunLater implements Runnable {

            @Override
            public void run() {
                reset(false);
            }

        }
        Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), new RunLater(), 200L);
    }

    @Override
    public void parseJoin(final Player player, final ArenaTeam team) {
        debug(arena, "adding autovote player: " + player.getName());
        players.add(ArenaPlayer.fromPlayer(player));
    }

    public boolean hasVoted(final String name) {
        return votes.containsKey(name);
    }

    @EventHandler(ignoreCancelled = true)
    public void onTryJoin(final PAJoinEvent event) {
        debug(arena, "tryJoin " + event.getPlayer().getName());
        if (vote != null) {
            debug(arena, "vote is not null! denying " + event.getPlayer().getName());
            event.setCancelled(true);
        }
    }
}
