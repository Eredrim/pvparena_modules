package net.slipcor.pvparena.modules.powerups;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.compatibility.AttributeAdapter;
import net.slipcor.pvparena.compatibility.EffectTypeAdapter;
import net.slipcor.pvparena.compatibility.EntityFreezeUtil;
import net.slipcor.pvparena.compatibility.ParticleAdapter;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.managers.ArenaManager;
import net.slipcor.pvparena.managers.WorkflowManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;

import static net.slipcor.pvparena.config.Debugger.debug;
import static net.slipcor.pvparena.modules.powerups.PowerUps.isPowerup;
import static org.bukkit.event.entity.EntityDamageEvent.DamageCause.THORNS;

public class PowerupEffect {
    private final int duration;
    private final PowerupEffectType type;
    private EntityType mobType;
    private final double factor;
    private final double chance;
    private final int diff;
    private final List<String> items;
    private PotionEffect potionEffect;


    /**
     * create a powerup effect object
     *
     * @param type       the effect class to create (based on PowerupType)
     * @param cfgSection the config section of effect values to set/add
     */
    public PowerupEffect(String type, ConfigurationSection cfgSection) {
        debug("adding effect {}", type);
        this.type = PowerupEffectType.parse(type);
        this.duration = cfgSection.getInt("duration", -1);
        this.factor = cfgSection.getDouble("factor", 1.0);
        this.chance = cfgSection.getDouble("chance", 1.0);
        this.diff = cfgSection.getInt("diff", 0);
        this.items = cfgSection.getStringList("items");
        String cfgMobType = cfgSection.getString("mobType");
        if(cfgMobType != null) {
            try {
                this.mobType = EntityType.valueOf(cfgMobType.toUpperCase());
            } catch (IllegalArgumentException e) {
                PVPArena.getInstance().getLogger().warning("unknown mob type: " + cfgMobType);
            }
        }

        String potionEffect = cfgSection.getString("potionEffect");
        if(potionEffect != null) {
            this.potionEffect = this.generatePotionEffect(potionEffect, this.factor, this.duration);
        }
    }

    public PowerupEffectType getType() {
        return this.type;
    }

    public int getDuration() {
        return this.duration;
    }

    /**
     * run PowerupEffect on pickup
     *
     * @param arenaPlayer the arenaPlayer to commit the effect on
     */
    public boolean activateOnPickup(ArenaPlayer arenaPlayer) {
        if (this.type.getActivationType() == PowerupActivationType.PICKUP) {
            this.applyToOwner(arenaPlayer);
            return true;
        }
        return false;
    }

    /**
     * remove PowerupEffect Potion Effect from player
     *
     * @param player the player to clear
     */
    public void removeEffect(final Player player) {
        if (this.potionEffect != null) {
            player.removePotionEffect(this.potionEffect.getType());
        }
    }

    /**
     * commit PowerupEffect in combat
     *
     * @param apAttacker   the attacking player to access
     * @param apDefender   the defending player to access
     * @param event      the triggering event
     * @return true if a sprint effect is available, false otherwise
     */
    public boolean applyOnHit(ArenaPlayer apAttacker, ArenaPlayer apDefender, EntityDamageByEntityEvent event) {
        debug(apAttacker, "committing entitydamagebyentityevent: " + this.type.name());

        if(this.type.getActivationType() == PowerupActivationType.HIT_GIVEN) {
            Random r = new Random();
            double hitChance = r.nextDouble();

            if(hitChance <= this.chance) {
                debug(apAttacker, "random r = {}", hitChance);
                Player attacker = apAttacker.getPlayer();
                Player defender = apDefender.getPlayer();

                if (this.type == PowerupEffectType.DMG_CAUSE) {
                    event.setDamage((int) Math.round(event.getDamage() * this.factor));
                } else if (this.type == PowerupEffectType.IGNITE) {
                    defender.setFireTicks(20);
                } else if (this.type == PowerupEffectType.FREEZE) {
                    int ticks = this.duration * 20;
                    EntityFreezeUtil.freezePlayer(defender);
                    Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), () -> EntityFreezeUtil.unfreezePlayer(defender), ticks);
                    apDefender.getArena().msg(defender, MSG.MODULE_POWERUPS_FROZEN, this.duration);
                } else if (this.type == PowerupEffectType.HEAL) {
                    if(!isPowerup(attacker.getInventory().getItemInMainHand())) {
                        // HEAL effect is applied only if hit is given with powerup item itself
                        return false;
                    }
                    event.setCancelled(true); // Disabling hit
                    playHealEffect(defender);
                    // Effect is applied to another player and should not be tracked with 'this.potionEffect'
                    PotionEffect regenEffect = this.generatePotionEffect(EffectTypeAdapter.REGENERATION, this.factor, this.duration);
                    defender.removePotionEffect(EffectTypeAdapter.REGENERATION);
                    defender.addPotionEffect(regenEffect);
                }
            } // else: chance fail :D
            return true;
        }
        return false;
    }

    public boolean applyOnGettingHit(ArenaPlayer apAttacker, ArenaPlayer apDefender, EntityDamageByEntityEvent event) {
        debug(apAttacker, "committing entitydamagebyentityevent: " + this.type.name());

        if(this.type.getActivationType() == PowerupActivationType.HIT_RECEIVED) {
            Random r = new Random();
            double hitChance = r.nextDouble();

            if (hitChance <= this.chance) {
                debug(apDefender, "random r = {}", hitChance);
                long newDamageVal = Math.round(event.getDamage() * this.factor);

                if (this.type == PowerupEffectType.DMG_RECEIVE) {
                    event.setDamage((int) newDamageVal);
                    event.setCancelled(true);
                } else if (this.type == PowerupEffectType.DMG_REFLECT && apAttacker != null && event.getCause() != THORNS) {
                    Player attacker = apAttacker.getPlayer();
                    EntityDamageByEntityEvent reflectEvent = new EntityDamageByEntityEvent(apDefender.getPlayer(), attacker, THORNS, newDamageVal);

                    attacker.damage(newDamageVal);
                    attacker.setLastDamageCause(reflectEvent);
                }
            } // else: chance fail :D
            return true;
        }
        return false;
    }

    /**
     * Apply powerup effects on sprint
     * @param arenaPlayer owner of the powerup
     * @return true if a sprint effect is available, false otherwise
     */
    public boolean applyOnSprint(ArenaPlayer arenaPlayer) {
        if (this.type == PowerupEffectType.SPRINT) {
            Random r = new Random();
            double sprintChance = r.nextDouble();

            if(sprintChance <= this.chance) {
                Player player = arenaPlayer.getPlayer();
                player.removePotionEffect(EffectTypeAdapter.SPEED);
                this.potionEffect = this.generatePotionEffect(EffectTypeAdapter.SPEED, this.factor, this.duration);
                player.addPotionEffect(this.potionEffect);
            }
            return true;
        }
        return false;
    }

    /**
     * Disable powerup effects if player stops sprinting
     * @param arenaPlayer owner of the powerup
     * @return true if there was an active sprint effect, false otherwise
     */
    public boolean removeAfterSprint(ArenaPlayer arenaPlayer) {
        if (this.type == PowerupEffectType.SPRINT) {
            Player player = arenaPlayer.getPlayer();
            this.removeEffect(player);
            return true;
        }
        return false;
    }

    /**
     * Apply powerup effects on (right) click
     * @param arenaPlayer owner of the powerup
     * @return true if a sprint effect is available, false otherwise
     */
    public boolean applyOnClick(ArenaPlayer arenaPlayer) {
        Random r = new Random();
        if (this.type == PowerupEffectType.SPAWN_MOB) {
            if (r.nextDouble() <= this.chance) {
                this.applySpawnMobEffect(arenaPlayer);
            }
            return true;
        }
        return false;
    }

    /**
     * apply PowerupEffect on arenaPlayer that owns the item
     *
     * @param arenaPlayer the arenaPlayer to commit the effect on
     */
    private void applyToOwner(ArenaPlayer arenaPlayer) {
        debug(arenaPlayer, "applying {} effect to themself", this.type);
        Random r = new Random();
        if (r.nextDouble() <= this.chance) {
            if (this.type == PowerupEffectType.HEALTH) {
                this.applyHealthEffect(arenaPlayer.getPlayer());
            } else if (this.type == PowerupEffectType.LIVES) {
                this.applyLivesEffect(arenaPlayer);
            } else if (this.type == PowerupEffectType.REPAIR) {
                this.applyRepairEffect(arenaPlayer);
            }  else if (this.type == PowerupEffectType.POTION_EFFECT && this.potionEffect != null) {
                arenaPlayer.getPlayer().addPotionEffect(this.potionEffect);
            }
        }
    }

    private void applySpawnMobEffect(ArenaPlayer arenaPlayer) {
        Player player = arenaPlayer.getPlayer();
        Location target = player.getTargetBlock(null, 8).getLocation().add(0, 1, 0);
        Entity entity = player.getWorld().spawnEntity(target, this.mobType);
        if(this.duration > 0) {
            Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), () -> {
                if(entity.isValid()) {
                    entity.remove();
                }
            }, this.duration * 20L);
        }
    }

    private void applyRepairEffect(ArenaPlayer arenaPlayer) {
        Player player = arenaPlayer.getPlayer();

        Consumer<ItemStack> applyNewDamageValue = (item) -> {
            if(item.hasItemMeta() && item.getItemMeta() instanceof Damageable) {
                Damageable meta = (Damageable) item.getItemMeta();
                short maxDurability = item.getType().getMaxDurability();
                long newDamageValue = Math.round(meta.getDamage() - maxDurability * this.factor);
                meta.setDamage((int) Math.max(0, newDamageValue));
                item.setItemMeta((ItemMeta) meta);
            }
        };

        for (String itemToRepair : this.items) {
            String ucItemToRepair = itemToRepair.toUpperCase();
            Optional<ItemStack> optionalItem;
            switch (ucItemToRepair) {
                case "HELMET":
                    optionalItem = Optional.ofNullable(player.getInventory().getHelmet());
                    break;
                case "CHESTPLATE":
                    optionalItem = Optional.ofNullable(player.getInventory().getChestplate());
                    break;
                case "LEGGINGS":
                    optionalItem = Optional.ofNullable(player.getInventory().getLeggings());
                    break;
                case "BOOTS":
                    optionalItem = Optional.ofNullable(player.getInventory().getBoots());
                    break;
                default:
                    optionalItem = Arrays.stream(player.getInventory().getContents())
                            .filter(itemStack -> itemStack.getType().name().contains(ucItemToRepair))
                            .findAny();
                    break;
            }

            optionalItem.ifPresent(applyNewDamageValue);
        }
    }

    private void applyLivesEffect(ArenaPlayer arenaPlayer) {
        Arena arena = arenaPlayer.getArena();
        int lives = WorkflowManager.handleGetLives(arena, arenaPlayer);
        ArenaTeam arenaTeam = arenaPlayer.getArenaTeam();
        if (this.diff < 0 && lives > 0) {
            int newLivesNumber = Math.max(0, (lives + this.diff));
            if(arena.isFreeForAll()) {
                arena.getGoal().getPlayerLifeMap().put(arenaPlayer, newLivesNumber);
                arena.broadcast(Language.parse(MSG.MODULE_POWERUPS_REM_LIVES_PLAYER, arenaPlayer.getName(), this.diff));
            } else {
                arena.getGoal().getTeamLifeMap().put(arenaTeam, newLivesNumber);
                arena.broadcast(Language.parse(MSG.MODULE_POWERUPS_REM_LIVES_TEAM, arenaTeam.getColoredName(), this.diff));
            }
            arena.getScoreboard().refresh();
            ArenaManager.checkAndCommit(arena, false);
        } else if (this.diff > 0 && lives > 0) {
            int newLivesNumber = lives + this.diff;
            if(arena.isFreeForAll()) {
                arena.getGoal().getPlayerLifeMap().put(arenaPlayer, newLivesNumber);
                arena.broadcast(Language.parse(MSG.MODULE_POWERUPS_ADD_LIVES_PLAYER, arenaPlayer.getName(), Math.abs(this.diff)));
            } else {
                arena.getGoal().getTeamLifeMap().put(arenaTeam, newLivesNumber);
                arena.broadcast(Language.parse(MSG.MODULE_POWERUPS_ADD_LIVES_TEAM, arenaTeam.getColoredName(), Math.abs(this.diff)));
            }
            arena.getScoreboard().refresh();
            ArenaManager.checkAndCommit(arena, false);
        }
    }

    private void applyHealthEffect(Player player) {
        double maxHealth = player.getAttribute(AttributeAdapter.MAX_HEALTH.getValue()).getBaseValue();

        if (this.diff != 0) {
            double newHealth = Math.max(0, player.getHealth() + this.diff); // Health can't be lower than 0
            player.setHealth(Math.min(newHealth, maxHealth));
        } else if (this.factor != 0) {
            long multipliedHeath = Math.round(player.getHealth() * this.factor);
            double newHealth = Math.max(0, multipliedHeath);
            player.setHealth(Math.min(newHealth, maxHealth));
        }
    }

    /**
     * Generate a new potion effect from 3 strings, assuming duration is in seconds
     * @param type Effect type
     * @param amp Effect amp
     * @param duration Effect duration in seconds
     * @return PotionEffect object
     * @throws NumberFormatException if number values can't be parsed
     * @throws NullPointerException if effect type is null (can't be parsed)
     */
    private PotionEffect generatePotionEffect(String type, double amp, int duration) throws NumberFormatException, NullPointerException {
        PotionEffectType effectType = PotionEffectType.getByName(type.toUpperCase());
        if(effectType == null) {
            throw new NullPointerException();
        }

        return this.generatePotionEffect(effectType, amp, duration);
    }

    private PotionEffect generatePotionEffect(PotionEffectType effectType, double amp, int duration) throws NumberFormatException, NullPointerException {
        int effectAmp = (int) Math.round(amp);
        return new PotionEffect(effectType, duration * 20, effectAmp - 1);
    }

    private static void playHealEffect(Player defender) {
        defender.getWorld().playSound(defender.getLocation(), Sound.ITEM_BOTTLE_EMPTY, 1, 1);
        Location particlesSpawnLoc = defender.getLocation().add(0, 0.5, 0);
        int count = 40;
        double xzOffset = 0.3;
        double yOffset = 0.6;
        double speed = 0.02;
        defender.getWorld().spawnParticle(ParticleAdapter.SPELL.getValue(), particlesSpawnLoc, count, xzOffset, yOffset, xzOffset, speed);
    }
}
