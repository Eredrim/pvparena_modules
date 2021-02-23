package net.slipcor.pvparena.modules.powerups;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.listeners.EntityListener;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.managers.ArenaManager;
import net.slipcor.pvparena.managers.WorkflowManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static net.slipcor.pvparena.config.Debugger.debug;

class PowerupEffect {
    boolean active;
    int uses = -1;
    int duration = -1;
    PowerupType type;
    private String mobtype;
    private double factor = 1.0;
    private double chance = 1.0;
    private int diff;
    private final List<String> items = new ArrayList<>();
    private PotionEffect potEff;

    /**
     * create a powerup effect class
     *
     * @param eClass       the effect class to create
     * @param puEffectVals the map of effect values to set/add
     */
    public PowerupEffect(final String eClass, final Map<String, Object> puEffectVals,
                         final PotionEffect effect) {
        debug("adding effect {}", eClass);
        type = parseClass(eClass);
        potEff = effect;

        debug("effect class is {}", type);
        for (final Map.Entry<String, Object> stringObjectEntry : puEffectVals.entrySet()) {
            if ("uses".equals(stringObjectEntry.getKey())) {
                uses = (Integer) stringObjectEntry.getValue();
                debug("uses: {}", uses);
            } else if ("duration".equals(stringObjectEntry.getKey())) {
                duration = (Integer) stringObjectEntry.getValue();
                debug("duration: {}", duration);
            } else if ("factor".equals(stringObjectEntry.getKey())) {
                factor = (Double) stringObjectEntry.getValue();
                debug("factor: {}", factor);
            } else if ("chance".equals(stringObjectEntry.getKey())) {
                chance = (Double) stringObjectEntry.getValue();
                debug("chance: {}", chance);
            } else if ("diff".equals(stringObjectEntry.getKey())) {
                diff = (Integer) stringObjectEntry.getValue();
                debug("diff: {}", diff);
            } else if ("items".equals(stringObjectEntry.getKey())) {
                items.add((String) stringObjectEntry.getValue());
                debug("items: {}", items);
            } else if ("type".equals(stringObjectEntry.getKey())) {
                // mob type
                mobtype = (String) stringObjectEntry.getValue();
                debug("type: {}", type.name());
            } else {
                debug("undefined effect class value: {}", stringObjectEntry.getKey());
            }
        }
    }

    /**
     * get the PowerupEffect class from name
     *
     * @param s the class name
     * @return a powerup effect
     */
    public static PowerupType parseClass(final String s) {
        for (final PowerupType c : PowerupType.values()) {
            if (c.name().equalsIgnoreCase(s)) {
                return c;
            }
            if (s.toUpperCase().startsWith("POTION.")) {
                return PowerupType.POTEFF;
            }
        }
        return null;
    }

    /**
     * initiate PowerupEffect
     *
     * @param player the player to commit the effect on
     */
    public void init(final Player player) {
        if (uses == 0) {
            return;
        }
        if (uses > 0) {
            active = true;
            uses--;
        } else {
            active = true;
        }

        debug(player, "initiating - " + type.name());

        if (duration == 0) {
            active = false;
        }

        if (type.isActivatedOnPickup()) {
            commit(player);
        }

        if (potEff != null) {
            player.addPotionEffect(potEff);
        }
    }

    /**
     * remove PowerupEffect Potion Effect from player
     *
     * @param player the player to clear
     */
    public void removeEffect(final Player player) {
        if (potEff != null) {
            player.addPotionEffect(new PotionEffect(potEff.getType(), 0, 0));
        }
    }

    /**
     * commit PowerupEffect in combat
     *
     * @param attacker   the attacking player to access
     * @param defender   the defending player to access
     * @param event      the triggering event
     * @param isAttacker is the player attacking
     */
    public void commit(final Player attacker, final Player defender,
                       final EntityDamageByEntityEvent event, final boolean isAttacker) {
        debug(attacker, "committing entitydamagebyentityevent: " + type.name());
        if (!isAttacker && type == PowerupType.DMG_RECEIVE) {
            final Random r = new Random();
            final Float v = r.nextFloat();
            debug(defender, "random r = " + v);
            if (v <= chance) {
                event.setDamage((int) Math.round(event.getDamage() * factor));
            } // else: chance fail :D
        } else if (isAttacker && type == PowerupType.DMG_CAUSE) {
            final Random r = new Random();
            final Float v = r.nextFloat();
            debug(attacker, "random r = " + v);
            if (v <= chance) {
                event.setDamage((int) Math.round(event.getDamage() * factor));
            } // else: chance fail :D
        } else if (!isAttacker && type == PowerupType.DMG_REFLECT) {
            if (attacker == null) {
                return;
            }
            final Random r = new Random();
            final Float v = r.nextFloat();
            debug(attacker, "random r = " + v);
            debug(defender, "random r = " + v);
            if (v <= chance) {
                final EntityDamageByEntityEvent reflectEvent = new EntityDamageByEntityEvent(
                        defender, attacker, event.getCause(),
                        Math.round(event.getDamage() * factor));
                new EntityListener().onEntityDamageByEntity(reflectEvent);
            } // else: chance fail :D
        } else if (!isAttacker && type == PowerupType.IGNITE) {
            final Random r = new Random();
            final Float v = r.nextFloat();
            debug(defender, "random r = " + v);
            if (v <= chance) {
                defender.setFireTicks(20);
            } // else: chance fail :D
        } else {
            debug("unexpected fight powerup effect: {}", type.name());
        }
    }

    /**
     * commit PowerupEffect on player
     *
     * @param player the player to commit the effect on
     * @return true if the commit succeeded, false otherwise
     */
    boolean commit(final Player player) {

        debug(player, "committing " + type.name());
        final Random r = new Random();
        if (r.nextFloat() <= chance) {
            if (this.type == PowerupType.HEALTH) {
                if (this.diff > 0) {
                    double newHealth = player.getHealth() + this.diff;
                    double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
                    player.setHealth(Math.min(newHealth, maxHealth));
                } else {
                    player.setHealth((int) Math.round(player.getHealth() * this.factor));
                }
                return true;
            } else if (type == PowerupType.LIVES) {
                final ArenaPlayer ap = ArenaPlayer.parsePlayer(player.getName());
                final int lives = WorkflowManager.handleGetLives(ap.getArena(), ap);
                if (lives + diff > 0) {
                    ap.get().damage(1000.0d);
                } else {
                    final ArenaTeam team = ap.getArenaTeam();
                    final Arena arena = ap.getArena();
                    ArenaModuleManager.announce(
                            arena,
                            Language.parse(MSG.FIGHT_KILLED_BY, player.getName(),
                                    arena.parseDeathCause(player,
                                            DamageCause.MAGIC, player)), "LOSER");

                    if (arena.getArenaConfig().getBoolean(CFG.USES_DEATHMESSAGES)) {
                        arena.broadcast(Language.parse(MSG.FIGHT_KILLED_BY,
                                team.colorizePlayer(player) + ChatColor.YELLOW,
                                arena.parseDeathCause(player,
                                        DamageCause.MAGIC, player)));
                    }
                    // needed so player does not get found when dead
                    arena.removePlayer(player, arena.getArenaConfig().getString(CFG.TP_LOSE), true, false);
                    ap.getArenaTeam().remove(ap);

                    ArenaManager.checkAndCommit(arena, false);
                }

                return true;
            } else if (type == PowerupType.PORTAL) {
                potEff = new PotionEffect(PotionEffectType.CONFUSION, duration * 20, 2);
                return true;
            } else if (type == PowerupType.REPAIR) {
                for (String i : items) {
                    i = i.toUpperCase();
                    ItemStack is = null;
                    if (i.contains("HELM")) {
                        is = player.getInventory().getHelmet();
                    } else if (i.contains("CHEST") || i.contains("PLATE")) {
                        is = player.getInventory().getChestplate();
                    } else if (i.contains("LEGGINS")) {
                        is = player.getInventory().getLeggings();
                    } else if (i.contains("BOOTS")) {
                        is = player.getInventory().getBoots();
                    } else if (i.contains("SWORD")) {
                        is = player.getInventory().getItemInHand();
                    }
                    if (is == null) {
                        continue;
                    }

                    if (diff > 0) {
                        if (is.getDurability() + diff > Byte.MAX_VALUE) {
                            is.setDurability(Byte.MAX_VALUE);
                        } else {
                            is.setDurability((short) (is.getDurability() + diff));
                        }
                    }
                }
                return true;
            } else if (type == PowerupType.SPAWN_MOB) {
                return true;
            } else if (type == PowerupType.SPRINT) {
                player.setSprinting(true);
                potEff = new PotionEffect(PotionEffectType.SPEED, duration * 20, 2);
                return true;
            }
        }
        debug("unexpected {}", type.name());
        return false;
    }

    /**
     * commit PowerupEffect on health gain
     *
     * @param event the triggering event
     */
    public void commit(final EntityRegainHealthEvent event) {
        debug(event.getEntity(), "committing entityregainhealthevent " + type.name());
        if (type == PowerupType.HEAL) {
            final Random r = new Random();
            if (r.nextFloat() <= chance) {
                event.setAmount((int) Math.round(event.getAmount() * factor));
                ((Player) event.getEntity()).setSaturation(20);
                ((Player) event.getEntity()).setFoodLevel(20);
            } // else: chance fail :D
        } else {
            debug("unexpected fight heal effect: {}", type.name());
        }
    }

    /**
     * commit PowerupEffect on velocity event
     *
     * @param event the triggering event
     */
    public void commit(final PlayerVelocityEvent event) {
        debug(event.getPlayer(), "committing velocityevent " + type.name());
        if (type == PowerupType.HEAL) {
            final Random r = new Random();
            if (r.nextFloat() <= chance) {
                event.setVelocity(event.getVelocity().multiply(factor));
            } // else: chance fail :D
        } else {
            debug("unexpected jump effect: {}", type.name());
        }
    }

    /**
     * Get the PotionEffect of a PotionEffect class string
     *
     * @param eClass the class string to parse
     * @return the PotionEffect or null
     */
    public static PotionEffect parsePotionEffect(String eClass) {
        eClass = eClass.replace("POTION.", "");

        // POTION.BLA:1 <--- duration
        // POTION.BLA:1:1 <--- amplifyer

        int duration = 1;
        int amplifyer = 1;

        if (eClass.contains(":")) {
            final String[] s = eClass.split(":");

            eClass = s[0];
            try {
                duration = Integer.parseInt(s[1]);
            } catch (final Exception e) {
                Arena.pmsg(Bukkit.getConsoleSender(), Language.parse(MSG.MODULE_POWERUPS_INVALIDPUEFF,
                        eClass));
            }

            if (s.length > 2) {

                try {
                    amplifyer = Integer.parseInt(s[2]);
                } catch (final Exception e) {
                    Arena.pmsg(Bukkit.getConsoleSender(), Language.parse(MSG.MODULE_POWERUPS_INVALIDPUEFF,
                            eClass));
                }
            }
        }

        for (final PotionEffectType pet : PotionEffectType.values()) {
            if (pet == null) {
                continue;
            }
            debug("parsing PET {}", pet);
            if (pet.getName() == null) {
                continue;
            }
            debug("parsing PET {}", pet);
            if (pet.getName().equals(eClass)) {
                return new PotionEffect(pet, duration, amplifyer);
            }
        }
        return null;
    }
}
