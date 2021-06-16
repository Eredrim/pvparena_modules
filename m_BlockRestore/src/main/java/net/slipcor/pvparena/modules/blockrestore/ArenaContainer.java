package net.slipcor.pvparena.modules.blockrestore;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

import static net.slipcor.pvparena.core.ItemStackUtils.cloneItemStacks;

public class ArenaContainer {
    private final Location location;
    private final ItemStack[] content;

    public ArenaContainer(Location location, ItemStack[] content) {
        this.location = location;
        this.content = cloneItemStacks(content);
    }

    public Location getLocation() {
        return this.location;
    }

    public ItemStack[] getContent() {
        return this.content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        ArenaContainer that = (ArenaContainer) o;
        return this.location.equals(that.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.location);
    }
}
