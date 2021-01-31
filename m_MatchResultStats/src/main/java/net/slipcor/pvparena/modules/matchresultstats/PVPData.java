package net.slipcor.pvparena.modules.matchresultstats;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * class for full access to player statistics
 */
final class PVPData {

    private final Map<String, Long> startTimes = new HashMap<>(); // player -> currentMillis
    private final Map<String, String> teams = new HashMap<>(); // player -> teamname

    private final Arena arena;

    private Integer id;

    PVPData(final Arena arena) {
        this.arena = arena;
    }

    public void join(final String playerName, final String teamName) {
        teams.put(playerName, teamName);
        startTimes.put(playerName, System.currentTimeMillis());
    }

    public void start() {
        final Set<String> names = new HashSet<>();

        for (final String name : startTimes.keySet()) {
            names.add(name);
        }

        final long time = System.currentTimeMillis();

        for (final String name : names) {
            startTimes.put(name, time);
        }
    }

    public void reset(final boolean force) {
        for (final ArenaPlayer player : arena.getFighters()) {
            if (startTimes.containsKey(player.getName())) {
                remove(player.getName(), !force);
            }
        }

        for (final String playerName : arena.getPlayedPlayers()) {
            if (startTimes.containsKey(playerName)) {
                remove(playerName, force);
            }
        }
        id = null;
    }

    public void winning(final String name) {
        remove(name, true);
    }

    public void losing(final String name) {
        remove(name, false);
    }

    private void remove(final String playerName, final boolean winning) {
        if (!startTimes.containsKey(playerName)) {
            return;
        }
        final long playMillis = System.currentTimeMillis() - startTimes.get(playerName);

        if (id == null) {
            id = MRSMySQL.getNextID();
        }

        final String team = teams.get(playerName);

        // id         --> matchId
        // playerName --> userName
        // winning    --> outcome
        // team       --> team

        MRSMySQL.save(id, arena.getName(), playerName, winning, team, playMillis / 1000);

        startTimes.remove(playerName);
        teams.remove(playerName);
    }

}
