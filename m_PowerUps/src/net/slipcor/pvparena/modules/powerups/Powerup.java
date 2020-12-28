package net.slipcor.pvparena.modules.powerups;

import net.slipcor.pvparena.PVPArena;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerVelocityEvent;

import java.util.HashMap;
import java.util.Map;

import static net.slipcor.pvparena.config.Debugger.debug;

class Powerup {
    public final String name; // PowerUp display name
    public final Material item; // item that triggers this Powerup
    private PowerupEffect[] effects; // Effects the Powerup has

    /**
     * construct a powerup instance
     *
     * @param pName     the powerup name
     * @param puEffects the powerup effects
     */
    @SuppressWarnings("unchecked")
    public Powerup(final String pName, final Map<String, Object> puEffects) {
        name = pName;
        debug("creating powerup {}", pName);
        item = Material.valueOf((String) puEffects.get("item"));
        debug("item added: {}", item);
        int count = 0;
        for (final Map.Entry<String, Object> stringObjectEntry1 : puEffects.entrySet()) {
            final PowerupType pec = PowerupEffect.parseClass(stringObjectEntry1.getKey());
            if (pec == null) {
                if (!"item".equals(stringObjectEntry1.getKey())) {
                    PVPArena.getInstance().getLogger().warning("unknown effect class: " + stringObjectEntry1.getKey());
                }
                continue;
            }
            final PowerupEffect pe = new PowerupEffect(stringObjectEntry1.getKey(),
                    (HashMap<String, Object>) stringObjectEntry1.getValue(),
                    PowerupEffect.parsePotionEffect(stringObjectEntry1.getKey()));
            if (pe.type == null) {
                continue;
            }
            count++;
        }
        debug("effects found: {}", count);
        if (count < 1) {
            return;
        }

        effects = new PowerupEffect[count];

        count = 0;
        for (final Map.Entry<String, Object> stringObjectEntry : puEffects.entrySet()) {
            final PowerupType pec = PowerupEffect.parseClass(stringObjectEntry.getKey());
            if (pec == null) {
                continue;
            }
            final PowerupEffect pe = new PowerupEffect(stringObjectEntry.getKey(),
                    (HashMap<String, Object>) stringObjectEntry.getValue(),
                    PowerupEffect.parsePotionEffect(stringObjectEntry.getKey()));
            if (pe.type == null) {
                continue;
            }
            effects[count++] = pe;
        }
    }

    /**
     * second constructor, referencing instead of creating
     *
     * @param p the Powerup reference
     */
    public Powerup(final Powerup p) {
        name = p.name;
        effects = p.effects;
        item = p.item;
    }

    /**
     * check if a powerup has active effects
     *
     * @return true if an effect still is active, false otherwise
     */
    public boolean isActive() {
        for (final PowerupEffect pe : effects) {
            if (pe.active) {
                return true;
            }
        }
        return false;
    }

    /**
     * check if a powerup running effect is running
     *
     * @param peClass the class to check
     * @return true if an effect still is active, false otherwise
     */
    public boolean isEffectActive(final PowerupType peClass) {
        for (final PowerupEffect pe : effects) {
            if (pe.uses != 0 && pe.duration != 0) {
                if (pe.type.equals(peClass))
                    return true;
            }
        }
        return false;
    }

    /**
     * check if any effect can be fired
     *
     * @return true if an event can be fired, false otherwise
     */
    public boolean canBeTriggered() {
        for (final PowerupEffect pe : effects) {
            if (pe.uses != 0 && pe.duration != 0) {
                return true; // one effect still can be triggered
            }
        }
        return false;
    }

    /**
     * initiate Powerup effects
     *
     * @param player the player to commit the effect on
     */
    public void activate(final Player player) {
        debug(player, "activating! - {}", name);
        for (final PowerupEffect pe : effects) {
            if (pe.uses != 0 && pe.duration != 0) {
                pe.init(player);
            }
        }
    }

    /**
     * commit PowerupEffect in combat
     *
     * @param attacker the attacking player to access
     * @param defender the defending player to access
     * @param event    the triggering event
     * @param isAttacker is the player attacking
     */
    public void commit(final Player attacker, final Player defender,
                       final EntityDamageByEntityEvent event, final boolean isAttacker) {
        debug(attacker, "committing effects:");
        debug(defender, "committing effects:");
        for (final PowerupEffect pe : effects) {
            if (pe.uses != 0 && pe.duration != 0) {
                pe.commit(attacker, defender, event, isAttacker);
            }
        }
    }

    /**
     * commit all PowerupEffects
     *
     * @param event the triggering event
     */
    public void commit(final EntityRegainHealthEvent event) {
        for (final PowerupEffect pe : effects) {
            if (pe.uses != 0 && pe.duration != 0) {
                if (pe.type.equals(PowerupType.HEAL))
                    pe.commit(event);
            }
        }
    }

    /**
     * commit all PowerupEffects
     *
     * @param event the triggering event
     */
    public void commit(final PlayerVelocityEvent event) {
        for (final PowerupEffect pe : effects) {
            if (pe.uses != 0 && pe.duration != 0) {
                if (pe.type.equals(PowerupType.HEAL))
                    pe.commit(event);
            }
        }
    }

    /**
     * calculate down the duration
     */
    public void tick() {
        for (final PowerupEffect pe : effects) {
            if (pe.uses != 0 && pe.duration > 0) {
                pe.duration--;
            }
        }
    }

    public void deactivate(final Player player) {
        if (effects == null) {
            return;
        }
        for (final PowerupEffect eff : effects) {
            eff.removeEffect(player);
        }
    }
}