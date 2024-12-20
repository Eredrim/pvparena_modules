package net.slipcor.pvparena.modules.powerups;

import java.util.Arrays;

/**
 * PowerupEffect classes
 */
public enum PowerupEffectType {

    DMG_CAUSE(PowerupActivationType.HIT_GIVEN),
    DMG_RECEIVE(PowerupActivationType.HIT_RECEIVED),
    DMG_REFLECT(PowerupActivationType.HIT_RECEIVED),
    FREEZE(PowerupActivationType.HIT_GIVEN),
    HEAL(PowerupActivationType.HIT_GIVEN),
    HEALTH(PowerupActivationType.PICKUP),
    IGNITE(PowerupActivationType.HIT_GIVEN),
    LIVES(PowerupActivationType.PICKUP),
    REPAIR(PowerupActivationType.PICKUP),
    SPAWN_MOB(PowerupActivationType.CLICK),
    SPRINT(PowerupActivationType.SPRINT),
    POTION_EFFECT(PowerupActivationType.PICKUP);

    private final PowerupActivationType activationType;

    PowerupEffectType(PowerupActivationType activationType) {
        this.activationType = activationType;
    }

    public PowerupActivationType getActivationType() {
        return this.activationType;
    }

    public static PowerupEffectType parse(String str) {
        return Arrays.stream(values())
                .filter(val -> val.name().equalsIgnoreCase(str))
                .findAny()
                .orElse(null);
    }

    public static boolean isValidKey(String str) {
        return Arrays.stream(values())
                .anyMatch(val -> val.name().equalsIgnoreCase(str));
    }
}