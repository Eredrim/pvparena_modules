package net.slipcor.pvparena.modules.maps;

import org.bukkit.ChatColor;
import org.bukkit.map.MapPalette;

import java.awt.*;

@SuppressWarnings({"deprecation", "unused"})
public enum MapColor {
    AQUA(Color.decode("#55FFFF")),
    BLACK(Color.decode("#000000")),
    BLUE(Color.decode("#5555FF")),
    DARK_AQUA(Color.decode("#00AAAA")),
    DARK_BLUE(Color.decode("#0000AA")),
    DARK_GRAY(Color.decode("#555555")),
    DARK_GREEN(Color.decode("#00AA00")),
    DARK_PURPLE(Color.decode("#AA00AA")),
    DARK_RED(Color.decode("#AA0000")),
    GOLD(Color.decode("#FFAA00")),
    GRAY(Color.decode("#AAAAAA")),
    GREEN(Color.decode("#55FF55")),
    LIGHT_PURPLE(Color.decode("#FF55FF")),
    RED(Color.decode("#FF5555")),
    WHITE(Color.decode("#FFFFFF")),
    YELLOW(Color.decode("#FFFF55"));

    private final Color color;

    MapColor(Color color) {
        this.color = color;
    }

    /**
     * Returns the byte color value for drawing maps
     */
    public byte getByteValue() {
        return MapPalette.matchColor(this.color);
    }

    /**
     * Returns the alternative byte color value for drawing maps (darker or brighter)
     */
    public byte getAltByteValue() {
        if(this.name().startsWith("DARK") || this == BLACK) {
            return MapPalette.matchColor(this.color.brighter());
        }
        return MapPalette.matchColor(this.color.darker());
    }

    public static MapColor valueOf(ChatColor chatColor) {
        return valueOf(chatColor.name());
    }
}
