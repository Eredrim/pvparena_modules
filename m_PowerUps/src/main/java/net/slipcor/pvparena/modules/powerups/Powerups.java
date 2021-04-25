package net.slipcor.pvparena.modules.powerups;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.slipcor.pvparena.config.Debugger.debug;

public class Powerups {
    public final Map<Player, Powerup> puActive = new HashMap<>();
    public final List<Powerup> puTotal = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public Powerups(final Map<String, Object> powerUps) {

        debug("initialising powerupmanager");
        for (final Map.Entry<String, Object> stringObjectEntry : powerUps.entrySet()) {
            debug("reading powerUps");
            Powerup powerup = new Powerup(stringObjectEntry.getKey(),
                    (HashMap<String, Object>) stringObjectEntry.getValue());
            puTotal.add(powerup);
        }
    }

    /**
     * trigger all powerups
     */
    public void tick() {
        for (final Powerup p : puActive.values()) {
            if (p.canBeTriggered()) {
                p.tick();
            }
        }
    }

}
