package net.slipcor.pvparena.modules.maps;

import net.slipcor.pvparena.classes.PABlock;
import net.slipcor.pvparena.classes.PASpawn;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import static java.util.Objects.requireNonNull;

class MapElement {
    private final int x;
    private final int z;
    private final MapElementType type;
    private final String name;
    private final ChatColor color;

    public MapElement(Player p, ChatColor c) {
        this.type = MapElementType.PLAYER;
        this.color = c;
        this.name = p.getName();
        this.x = 0;
        this.z = 0;
    }

    public MapElement(PASpawn spawn, ChatColor c) {
        this.type = MapElementType.SPAWN;
        this.name = null;
        this.color = c;
        Location location = spawn.getPALocation().toLocation();
        this.x = location.getBlockX();
        this.z = location.getBlockZ();
    }

    public MapElement(PABlock block, ChatColor c) {
        this.type = MapElementType.BLOCK;
        this.name = null;
        this.color = c;
        this.x = block.getLocation().getX();
        this.z = block.getLocation().getZ();
    }

    public int getX() {
        if (this.type == MapElementType.PLAYER) {
            try {
                return requireNonNull(Bukkit.getPlayerExact(this.name)).getLocation().getBlockX();
            } catch (final NullPointerException ignored) {
            }
        }
        return this.x;
    }

    public int getZ() {
        if (this.type == MapElementType.PLAYER) {
            try {
                return requireNonNull(Bukkit.getPlayerExact(this.name)).getLocation().getBlockZ();
            } catch (final NullPointerException ignored) {
            }
        }
        return this.z;
    }

    public String getName() {
        return this.name;
    }

    public MapElementType getType() {
        return this.type;
    }

    public MapColor getMapColor() {
        return MapColor.valueOf(this.color);
    }
}
