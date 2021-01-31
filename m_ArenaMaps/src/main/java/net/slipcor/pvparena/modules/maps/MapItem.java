package net.slipcor.pvparena.modules.maps;

import net.slipcor.pvparena.classes.PABlockLocation;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

class MapItem {
    private final int x;
    private final int z;
    private final boolean player;
    private final String name;
    private final ChatColor color;

    public MapItem(final Player p, final ChatColor c) {
        player = true;
        color = c;
        name = p.getName();
        x = 0;
        z = 0;
    }

    public MapItem(final PABlockLocation coord, final ChatColor c) {
        player = false;
        name = null;
        color = c;
        x = coord.getX();
        z = coord.getZ();
    }

    public int getX() {
        if (player) {
            try {
                return Bukkit.getPlayerExact(name).getLocation().getBlockX();
            } catch (final NullPointerException e) {

            }
        }
        return x;
    }

    public int getZ() {
        if (player) {
            try {
                return Bukkit.getPlayerExact(name).getLocation().getBlockZ();
            } catch (final NullPointerException e) {

            }
        }
        return z;
    }

    public ChatColor getColor() {
        return color;
    }

    public boolean isPlayer() {
        return player;
    }

    public String getName() {
        return name;
    }
}
