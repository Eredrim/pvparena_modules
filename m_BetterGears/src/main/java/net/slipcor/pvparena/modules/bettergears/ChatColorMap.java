package net.slipcor.pvparena.modules.bettergears;

import org.bukkit.Color;

@SuppressWarnings({"deprecation", "unused"})
public enum ChatColorMap {
    AQUA(Color.fromRGB(85, 255, 255)),
    BLACK(Color.BLACK),
    BLUE(Color.fromRGB(85, 85, 255)),
    DARK_AQUA(Color.fromRGB(0, 170, 170)),
    DARK_BLUE(Color.fromRGB(0, 0, 170)),
    DARK_GRAY(Color.fromRGB(85, 85, 85)),
    DARK_GREEN(Color.fromRGB(0, 170, 0)),
    DARK_PURPLE(Color.fromRGB(170, 0, 170)),
    DARK_RED(Color.fromRGB(170, 0, 0)),
    GOLD(Color.fromRGB(255, 170, 0)),
    GRAY(Color.fromRGB(170, 170, 170)),
    GREEN(Color.fromRGB(85, 255, 85)),
    LIGHT_PURPLE(Color.fromRGB(55, 85, 255)),
    RED(Color.fromRGB(255, 85, 85)),
    WHITE(Color.WHITE),
    YELLOW(Color.fromRGB(255, 255, 85));

    private final Color color;

    ChatColorMap(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return this.color;
    }
}
