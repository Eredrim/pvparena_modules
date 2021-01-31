package net.slipcor.pvparena.modules.powerups;

/**
 * PowerupEffect classes
 */
public enum PowerupType {

    DMG_CAUSE(false),
    DMG_RECEIVE(false),
    DMG_REFLECT(false),
    FREEZE(true),
    HEAL(false),
    HEALTH(true),
    IGNITE(false),
    LIVES(true),
    PORTAL(true),
    REPAIR(true),
    SLIP(true),
    SPAWN_MOB(true),
    SPRINT(true),
    JUMP(false),
    POTEFF(true);

    final boolean activatedOnPickup;

    PowerupType(final boolean pickup) {
        this.activatedOnPickup = pickup;
    }

    public boolean isActivatedOnPickup() {
        return activatedOnPickup;
    }
}