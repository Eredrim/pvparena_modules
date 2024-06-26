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
        this.name = sName;
        this.max = iMax;
    }

    public int getCount() {
        return this.players.size();
    }

    public int getMax() {
        return this.max;
    }

    public String getName() {
        return this.name;
    }

    public void add(final ArenaPlayer player) {
        this.players.add(player);
    }

    public boolean contains(final ArenaPlayer player) {
        return this.players.contains(player);
    }

    public void remove(final ArenaPlayer player) {
        this.players.remove(player);
    }

    public void reset() {
        this.players.clear();
    }

    public Set<ArenaPlayer> getPlayers() {
        return new HashSet<>(this.players);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        ArenaSquad that = (ArenaSquad) o;
        return Objects.equals(this.name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name);
    }
}
