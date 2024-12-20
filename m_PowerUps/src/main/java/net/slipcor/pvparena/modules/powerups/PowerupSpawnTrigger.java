package net.slipcor.pvparena.modules.powerups;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum PowerupSpawnTrigger {
    TIME,
    DEATHS;

    public static PowerupSpawnTrigger parse(String str) {
        return Arrays.stream(values())
                .filter(val -> val.name().equalsIgnoreCase(str))
                .findAny()
                .orElse(null);
    }

    public static String valuesToString() {
        return Arrays.stream(values())
                .map(PowerupSpawnTrigger::name)
                .collect(Collectors.joining(" | "));
    }
}
