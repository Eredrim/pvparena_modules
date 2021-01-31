package net.slipcor.pvparena.modules.turrets;

import net.slipcor.pvparena.classes.PALocation;
import org.bukkit.entity.*;

import java.util.HashMap;
import java.util.Map;

class Turret {
    private final double yaw;
    private final double offset;
    private static final Map<String, Class<? extends Projectile>> types = new HashMap<>();
    private final Class<? extends Projectile> type;

    static {
        types.put("fire", Fireball.class);
        types.put("snow", Snowball.class);
        types.put("large", LargeFireball.class);
        types.put("small", SmallFireball.class);
        types.put("wither", WitherSkull.class);
    }

    public Turret(final String name, final PALocation loc, final double offset) {
        yaw = loc.getYaw();
        this.offset = offset < 10 ? 10 : offset; // safety. less than 10 degrees is nonsense :p
        for (final Map.Entry<String, Class<? extends Projectile>> stringClassEntry : types.entrySet()) {
            if (name.contains(stringClassEntry.getKey())) {
                type = stringClassEntry.getValue();
                return;
            }
        }
        type = Arrow.class;
    }

    Class<? extends Projectile> getType() {
        return type;
    }

    public boolean cancelMovement(final float oldYaw) {
        float yaw = oldYaw;
        // yaw : 0-360

        yaw -= this.yaw; // diff; -360>diff>360

        yaw = Math.abs(yaw); // abs; 0>=diff>360

        if (yaw > 180) {
            // if we have greater difference than 180 deg
            // => swap diff, because we get nearer again
            yaw = 360 - yaw; // 180 results in 180, 360 results in 0
        }

        // yaw now is the absolute degree difference from this to that

        return yaw > offset;
    }

    public float getYaw() {
        return (float) (yaw);
    }
}
