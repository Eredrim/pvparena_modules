package net.slipcor.pvparena.modules.maps;

import static java.util.Arrays.stream;

public enum MapElementVisibility {
    ALL,
    TEAM,
    OTHERS,
    NONE;

    public static String[] stringValues() {
        return stream(values()).map(Enum::name).toArray(String[]::new);
    }
}
