package net.slipcor.pvparena.modules.squads;

import net.slipcor.pvparena.arena.ArenaPlayer;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

class ArenaSquad {
    private final String name;
    private final int max;
    private final Set<ArenaPlayer> players = new HashSet<>();

    public ArenaSquad(final String sName, final int iMax) {
        name = sName;
        max = iMax;
    }

    public int getCount() {
        return players.size();
    }

    public int getMax() {
        return max;
    }

    public String getName() {
        return name;
    }

    public void add(final ArenaPlayer player) {
        players.add(player);
    }

    public boolean contains(final ArenaPlayer player) {
        return players.contains(player);
    }

    public void remove(final ArenaPlayer player) {
        players.remove(player);
    }

    public void reset() {
        players.clear();
    }

    public Set<ArenaPlayer> getPlayers() {
        final Set<ArenaPlayer> result = new HashSet<>();
        for (final ArenaPlayer ap : players) {
            result.add(ap);
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArenaSquad that = (ArenaSquad) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
