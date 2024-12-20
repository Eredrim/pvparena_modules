package net.slipcor.pvparena.modules.powerups;

import net.slipcor.pvparena.arena.ArenaPlayer;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static net.slipcor.pvparena.config.Debugger.debug;

public class PowerupItem {
    private final String name; // PowerUp display name
    private final Material item; // item that triggers this Powerup
    private final List<PowerupEffect> effects; // Effects the Powerup has
    private final List<String> lore;
    private int remainingUses;
    private int remainingTime;

    public PowerupItem(String name, ConfigurationSection cfgSection) {
        this.name = name;
        debug("creating powerup {}", this.getName());
        Map<String, Object> itemProperties = cfgSection.getValues(false);
        this.item = Material.valueOf(cfgSection.getString("item"));
        this.remainingUses = cfgSection.getInt("uses", -1);
        this.lore = cfgSection.getStringList("lore");
        debug("item added: {}", this.getItem());
        this.effects = itemProperties.entrySet().stream()
                .filter(propCfg -> PowerupEffectType.isValidKey(propCfg.getKey()))
                .map(propCfg -> new PowerupEffect(propCfg.getKey(), (ConfigurationSection) propCfg.getValue()))
                .filter(effect -> effect.getType() != null)
                .collect(Collectors.toList());

        Integer maxFromEffects = this.effects.stream()
                .filter(effect -> effect.getType().getActivationType() == PowerupActivationType.PICKUP)
                .map(PowerupEffect::getDuration)
                .max(Integer::compareTo)
                .orElse(-1);

        this.remainingTime = Math.max(cfgSection.getInt("lifetime", -1), maxFromEffects);
    }

    /**
     * Copy constructor
     *
     * @param p the Powerup reference
     */
    public PowerupItem(PowerupItem p) {
        this.name = p.name;
        this.effects = p.effects;
        this.item = p.item;
        this.lore = p.lore;
        this.remainingTime = p.remainingTime;
        this.remainingUses = p.remainingUses;
    }

    public Material getItem() {
        return this.item;
    }

    public String getName() {
        return this.name;
    }

    public List<PowerupEffect> getEffects() {
        return this.effects;
    }

    public List<String> getLore() {
        return this.lore;
    }

    /**
     * check if any effect can be fired
     *
     * @return true if an event can be fired, false otherwise
     */
    public boolean canBeTriggered() {
        // one effect still can be triggered
        return this.remainingTime != 0 && this.remainingUses != 0;
    }

    /**
     * activate Powerup effects on pickup
     *
     * @param arenaPlayer the arenaPlayer to commit the effect on
     */
    public void activateOnPickup(ArenaPlayer arenaPlayer) {
        debug(arenaPlayer, "activating PowerUp on pickup! - {}", this.getName());
        boolean hasBeenTriggered = this.getEffects().stream()
                .map(pe -> pe.activateOnPickup(arenaPlayer))
                .reduce(false, (merged, element) -> merged || element);

        if(hasBeenTriggered && this.remainingTime == -1) {
            this.remainingUses = 0;
        }
    }

    public void activateOnClick(ArenaPlayer arenaPlayer) {
        debug(arenaPlayer, "activating PowerUp on click! - {}", this.getName());
        if (this.canBeTriggered()) {
            boolean hasBeenTriggered = this.getEffects().stream()
                    .filter(pe -> pe.getType().getActivationType() == PowerupActivationType.CLICK)
                    .map(pe -> pe.applyOnClick(arenaPlayer))
                    .reduce(false, (merged, element) -> merged || element); // needed to know if any PU has been actived on pickup without short-circuit matcher functions like anyMatch()

            if(hasBeenTriggered) {
                this.remainingUses--;
            }
        }
    }

    public void activateOnHit(ArenaPlayer attacker, ArenaPlayer defender, EntityDamageByEntityEvent event) {
        if (this.canBeTriggered()) {
            boolean hasBeenTriggered = this.getEffects().stream()
                    .map(pe -> pe.applyOnHit(attacker, defender, event))
                    .reduce(false, (merged, element) -> merged || element); // needed to know if any PU has been actived on pickup without short-circuit matcher functions like anyMatch()

            if(hasBeenTriggered) {
                this.remainingUses--;
            }
        }
    }

    public void activateOnGettingHit(ArenaPlayer attacker, ArenaPlayer defender, EntityDamageByEntityEvent event) {
        boolean hasBeenTriggered = this.getEffects().stream()
                .map(pe -> pe.applyOnGettingHit(attacker, defender, event))
                .reduce(false, (merged, element) -> merged || element); // needed to know if any PU has been actived on pickup without short-circuit matcher functions like anyMatch()

        if(hasBeenTriggered) {
            this.remainingUses--;
        }
    }

    public void toggleOnSprint(ArenaPlayer arenaPlayer, boolean isSprinting) {
        debug(arenaPlayer, "activating PowerUp on pickup! - {}", this.getName());
        boolean hasBeenTriggered = this.getEffects().stream()
                .filter(pe -> pe.getType().getActivationType() == PowerupActivationType.SPRINT)
                .map(pe -> isSprinting ? pe.applyOnSprint(arenaPlayer) : pe.removeAfterSprint(arenaPlayer))
                .reduce(false, (merged, element) -> merged || element); // needed to know if any PU has been actived on pickup without short-circuit matcher functions like anyMatch()

        if(hasBeenTriggered && isSprinting) {
            this.remainingUses--;
        }
    }

    /**
     * calculate down the duration
     */
    public void decrementTime() {
        if(this.remainingTime > 0) {
            this.remainingTime--;
        }
    }

    public void removeEffects(Player player) {
        if (this.getEffects() != null) {
            for (final PowerupEffect eff : this.getEffects()) {
                eff.removeEffect(player);
            }
        }
    }
}