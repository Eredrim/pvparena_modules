package net.slipcor.pvparena.modules.maps;

import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.classes.PABlock;
import net.slipcor.pvparena.classes.PASpawn;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import static java.util.Objects.requireNonNull;

class MapElement {
    private final int x;
    private final int z;
    private final MapElementType type;
    private final String name;
    private final ArenaTeam team;
    private byte displayPriority;

    public MapElement(Player p, ArenaTeam team) {
        this.type = MapElementType.PLAYER;
        this.name = p.getName();
        this.team = team;
        this.displayPriority = 1;
        this.x = 0;
        this.z = 0;
    }

    public MapElement(PASpawn spawn, ArenaTeam team) {
        this.type = MapElementType.SPAWN;
        this.name = null;
        this.team = team;
        this.displayPriority = 2;
        Location location = spawn.getPALocation().toLocation();
        this.x = location.getBlockX();
        this.z = location.getBlockZ();
    }

    public MapElement(PABlock block, ArenaTeam team) {
        this.type = MapElementType.BLOCK;
        this.name = null;
        this.team = team;
        this.displayPriority = 3;
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
        return MapColor.valueOf(this.team.getColor());
    }

    public byte getDisplayPriority() {
        return this.displayPriority;
    }

    public boolean isVisible(MapElementVisibility visibility, ArenaPlayer arenaPlayer) {
        if(visibility == MapElementVisibility.ALL) {
            return true;
        }

        if(visibility == MapElementVisibility.OTHERS) {
            return !arenaPlayer.getArenaTeam().equals(this.team);
        }

        if(visibility == MapElementVisibility.TEAM) {
            return arenaPlayer.getArenaTeam().equals(this.team);
        }

        return false;
    }
}
