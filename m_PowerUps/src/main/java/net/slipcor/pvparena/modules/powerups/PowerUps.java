package net.slipcor.pvparena.modules.powerups;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.classes.PALocation;
import net.slipcor.pvparena.commands.AbstractArenaCommand;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.compatibility.EntityFreezeUtil;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.RandomUtils;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.PermissionManager;
import net.slipcor.pvparena.managers.SpawnManager;
import net.slipcor.pvparena.regions.ArenaRegion;
import net.slipcor.pvparena.regions.RegionType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static net.slipcor.pvparena.config.Debugger.debug;
import static net.slipcor.pvparena.core.CollectionUtils.ofEmpty;

public class PowerUps extends ArenaModule {
    static final float DEFAULT_WALK_SPEED = 0.2f;

    private static final int POWERUP_ENCHANT_LVL = 216;
    private static final String POWERUPS_CFG = "modules.powerups.items";
    private static final String POWERUP = "powerup";
    private static final String DEFAULT_LORE = "Power Up!";

    private List<PowerupItem> availPowerUps = new ArrayList<>();
    private final Map<ArenaPlayer, PowerupItem> activePowerUps = new HashMap<>();
    private PowerupSpawnTrigger powerupSpawnTrigger;
    private int powerupFreq;

    private int deathNumber;

    private BukkitRunnable spawningTask;

    private BukkitRunnable lifetimeTask;


    public PowerUps() {
        super("Powerups");
    }

    @Override
    public String version() {
        return this.getClass().getPackage().getImplementationVersion();
    }

    /**
     * calculate a powerup and commit it
     */
    void calcPowerupSpawn() {
        if (this.availPowerUps.isEmpty()) {
            debug(this.arena, this, "no available powerups");
            return;
        }

        PowerupItem p = RandomUtils.getRandom(this.availPowerUps, new Random());
        this.commitPowerupItemSpawn(p);
        this.arena.broadcast(Language.parse(MSG.MODULE_POWERUPS_SERVER, p.getName()));
    }

    @Override
    public boolean checkCommand(final String s) {
        return "!pu".equals(s) || POWERUP.equals(s);
    }

    @Override
    public List<String> getMain() {
        return singletonList("powerups");
    }

    @Override
    public List<String> getShort() {
        return singletonList("!pu");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        result.define(new String[]{"time"});
        result.define(new String[]{"deaths"});
        result.define(new String[]{"dropspawn"});
        return result;
    }

    Map<ArenaPlayer, PowerupItem> getActivePowerUps() {
        return this.activePowerUps;
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        // !pu seconds 6
        // !pu deaths 4

        if (!PermissionManager.hasAdminPerm(sender) && !PermissionManager.hasBuilderPerm(sender, this.arena)) {
            this.arena.msg(sender, MSG.ERROR_NOPERM, Language.parse(MSG.ERROR_NOPERM_X_ADMIN));
            return;
        }

        if (!AbstractArenaCommand.argCountValid(sender, this.arena, args, new Integer[]{2, 3})) {
            return;
        }

        if ("!pu".equals(args[0]) || args[0].startsWith(POWERUP)) {
            if (args.length == 2) {
                if ("dropspawn".equalsIgnoreCase(args[1])) {
                    boolean b = this.arena.getConfig().getBoolean(CFG.MODULES_POWERUPS_DROPSPAWN);
                    this.arena.getConfig().set(CFG.MODULES_POWERUPS_DROPSPAWN, !b);
                    this.arena.getConfig().save();
                    this.arena.msg(sender, MSG.SET_DONE, CFG.MODULES_POWERUPS_DROPSPAWN.getNode(), String.valueOf(!b));
                } else {
                    this.arena.msg(sender, MSG.ERROR_ARGUMENT, args[1], "dropspawn");
                }
            } else {
                try {
                    int freq = Integer.parseInt(args[2]);
                    PowerupSpawnTrigger trigger = PowerupSpawnTrigger.parse(args[1]);
                    if (trigger != null) {
                        this.arena.getConfig().set(CFG.MODULES_POWERUPS_USAGE_TRIGGER, trigger.name());
                        this.arena.getConfig().set(CFG.MODULES_POWERUPS_USAGE_FREQ, freq);
                        this.arena.getConfig().save();
                        this.powerupSpawnTrigger = trigger;
                        this.powerupFreq = freq;
                        this.arena.msg(sender, MSG.SET_DONE, CFG.MODULES_POWERUPS_USAGE_TRIGGER.getNode(), trigger.name());
                        this.arena.msg(sender, MSG.SET_DONE, CFG.MODULES_POWERUPS_USAGE_FREQ.getNode(), freq);
                    } else {
                        this.arena.msg(sender, MSG.ERROR_ARGUMENT, args[1], PowerupSpawnTrigger.valuesToString());
                    }
                } catch (NumberFormatException e) {
                    this.arena.msg(sender, MSG.ERROR_NOT_NUMERIC, args[2]);
                }
            }
        }
    }

    @Override
    public void parsePlayerDeath(final Player player, final EntityDamageEvent lastDamageCause) {
        if (!this.availPowerUps.isEmpty() && this.powerupSpawnTrigger == PowerupSpawnTrigger.DEATHS) {
            debug(this.arena, this, "calculating powerup trigger death");
            this.deathNumber++;
            if (this.deathNumber % this.powerupFreq == 0) {
                this.calcPowerupSpawn();
            }
        }

        EntityFreezeUtil.unfreezePlayer(player);
        this.cancelCurrentPowerUp(ArenaPlayer.fromPlayer(player));
    }

    /**
     * commit the powerup item spawn
     *
     * @param puItem the PowerupItem to spawn
     */
    private void commitPowerupItemSpawn(PowerupItem puItem) {
        final Set<ArenaRegion> regions = this.arena.getRegionsByType(RegionType.BATTLE);


        if (regions.isEmpty() || this.arena.getConfig().getBoolean(CFG.MODULES_POWERUPS_DROPSPAWN)) {
            if (!this.arena.getConfig().getBoolean(CFG.MODULES_POWERUPS_DROPSPAWN)) {
                PVPArena.getInstance().getLogger().warning("You have deactivated 'dropspawn' but have no BATTLE region. " +
                        "Attempting to find powerup drop spawns!");
            }
            this.dropItemOnSpawn(puItem);

        } else {
            ArenaRegion ar = RandomUtils.getRandom(regions, new Random());

            final PABlockLocation min = ar.getShape().getMinimumLocation();
            final PABlockLocation max = ar.getShape().getMaximumLocation();

            final Random random = new Random();

            final int x = random.nextInt(max.getX() - min.getX());
            final int z = random.nextInt(max.getZ() - min.getZ());

            final World world = Bukkit.getWorld(min.getWorldName());
            Location dropLoc = world.getHighestBlockAt(min.getX() + x, min.getZ() + z).getRelative(BlockFace.UP).getLocation();

            world.dropItem(dropLoc, this.getTaggedItem(puItem)).setVelocity(new Vector());
        }
    }

    @Override
    public void configParse(final YamlConfiguration config) {
        ConfigurationSection powerupsCfgSection = config.getConfigurationSection(POWERUPS_CFG);
        if (powerupsCfgSection != null) {
            try {
                this.availPowerUps = powerupsCfgSection.getValues(false).entrySet().stream()
                        .map(cfgEntry -> new PowerupItem(cfgEntry.getKey(), (ConfigurationSection) cfgEntry.getValue()))
                        .collect(Collectors.toList());
            } catch (Exception e) {
                String msg = String.format("Error while reading '%s' config of arena '%s', thrown exception:", POWERUPS_CFG, this.arena.getName());
                PVPArena.getInstance().getLogger().log(Level.SEVERE, msg, e);
            }
        } else {
            this.availPowerUps = new ArrayList<>();
        }


        Config arenaCfg = this.arena.getConfig();
        this.powerupSpawnTrigger = PowerupSpawnTrigger.parse(arenaCfg.getString(CFG.MODULES_POWERUPS_USAGE_TRIGGER));

        if (this.powerupSpawnTrigger != null) {
            this.powerupFreq = arenaCfg.getInt(CFG.MODULES_POWERUPS_USAGE_FREQ);
        } else {
            String errorMsg = String.format("Error activating powerup module : %s has unknown value", CFG.MODULES_POWERUPS_USAGE_TRIGGER);
            PVPArena.getInstance().getLogger().warning(errorMsg);
        }
    }

    @Override
    public void displayInfo(final CommandSender player) {
        player.sendMessage(
                String.format(
                        "Enabled: %s | Trigger: %s | Frequency: %s",
                        StringParser.colorVar(this.powerupFreq != 0),
                        this.powerupSpawnTrigger.name(),
                        this.powerupFreq
                )
        );
    }

    /**
     * drop an item at a powerup spawn point
     *
     * @param puItem the PowerUp item to drop
     */
    private void dropItemOnSpawn(PowerupItem puItem) {
        debug(this.arena, this, "calculating item spawn location");
        List<PALocation> allowedLocations = new ArrayList<>(SpawnManager.getSpawnsContaining(this.arena, POWERUP));

        if (allowedLocations.isEmpty()) {
            PVPArena.getInstance().getLogger().warning("No valid powerup spawns found!");
            return;
        }

        Location dropLoc = RandomUtils.getRandom(allowedLocations, new Random()).toLocation().add(0, 0.5, 0);
        debug(this.arena, this, "dropping item on spawn: {}", dropLoc);
        World world = dropLoc.getWorld();
        BoundingBox areaToClear = BoundingBox.of(dropLoc, 0.5, 0.5, 0.5);
        Collection<Entity> existingPowerUps = world.getNearbyEntities(areaToClear, e -> e instanceof Item && isPowerup(((Item) e).getItemStack()));
        existingPowerUps.forEach(Entity::remove);
        world.dropItem(dropLoc, this.getTaggedItem(puItem)).setVelocity(new Vector());
    }

    @Override
    public boolean hasSpawn(final String s, final String teamName) {
        return s.toLowerCase().startsWith(POWERUP);
    }

    private ItemStack getTaggedItem(PowerupItem puItem) {
        ItemStack itemStack = new ItemStack(puItem.getItem());
        ItemMeta meta = itemStack.getItemMeta();

        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addEnchant(Enchantment.BINDING_CURSE, POWERUP_ENCHANT_LVL, true);
        meta.setDisplayName(puItem.getName());
        meta.setLore(ofEmpty(puItem.getLore()).orElse(singletonList(DEFAULT_LORE)));
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public static boolean isPowerup(final ItemStack item) {
        if (item != null && item.hasItemMeta()) {
            return item.getItemMeta().getEnchantLevel(Enchantment.BINDING_CURSE) == POWERUP_ENCHANT_LVL;
        }
        return false;
    }

    @Override
    public void onEntityDamageByEntity(Player attacker, Player defender, EntityDamageByEntityEvent event) {
        if (!this.activePowerUps.isEmpty()) {
            ArenaPlayer apAttacker = ArenaPlayer.fromPlayer(attacker);
            ArenaPlayer apDefender = ArenaPlayer.fromPlayer(defender);
            debug(apAttacker, this, "committing powerup triggers");
            debug(apDefender, this, "committing powerup triggers");

            PowerupItem p = this.activePowerUps.get(apAttacker);
            if (p != null && p.canBeTriggered()) {
                p.activateOnHit(apAttacker, apDefender, event);
            }

            p = this.activePowerUps.get(apDefender);
            if (p != null && p.canBeTriggered()) {
                p.activateOnGettingHit(apAttacker, apDefender, event);
            }
        }

    }

    @Override
    public void onPlayerPickupItem(final EntityPickupItemEvent event) {
        Player player = (Player) event.getEntity();
        ArenaPlayer ap = ArenaPlayer.fromPlayer(player);
        ItemStack pickedUpItem = event.getItem().getItemStack();

        if (isPowerup(pickedUpItem)) {
            debug(ap, this, "onPlayerPickupItem - item: {}", pickedUpItem.getType());
            this.availPowerUps.stream()
                    .filter(pu -> pu.getItem() == pickedUpItem.getType())
                    .findAny()
                    .ifPresent(availPowerUp -> {
                        PowerupItem newPowerUp = new PowerupItem(availPowerUp);
                        this.cancelCurrentPowerUp(ap);

                        player.getInventory().addItem(pickedUpItem);
                        this.activePowerUps.put(ap, newPowerUp);
                        this.arena.broadcast(Language.parse(MSG.MODULE_POWERUPS_PLAYER, player.getName(), newPowerUp.getName()));
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
                        event.setCancelled(true);
                        event.getItem().remove();

                        if (newPowerUp.canBeTriggered()) {
                            newPowerUp.activateOnPickup(ap); // activate pickup effects for the first time
                        }
                    });
        }
    }

    @Override
    public boolean onPlayerInteract(PlayerInteractEvent event) {
        if (isPowerup(event.getItem())) {
            event.setCancelled(true);

            if(event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                ArenaPlayer ap = ArenaPlayer.fromPlayer(event.getPlayer());
                final PowerupItem p = this.activePowerUps.get(ap);
                if (p != null && p.canBeTriggered()) {
                    p.activateOnClick(ap);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onPlayerToggleSprint(PlayerToggleSprintEvent sprintEvent) {
        ArenaPlayer ap = ArenaPlayer.fromPlayer(sprintEvent.getPlayer());
        if (!this.activePowerUps.isEmpty()) {
            final PowerupItem p = this.activePowerUps.get(ap);
            if (p != null) {
                if(sprintEvent.isSprinting()) {
                    if(p.canBeTriggered()) {
                        p.toggleOnSprint(ap, true);
                    }
                } else {
                    p.toggleOnSprint(ap, false);
                }
            }
        }
    }

    @Override
    public void onBlockPlace(Block block, Material mat) {
        super.onBlockPlace(block, mat);
    }

    @Override
    public void reset(final boolean force) {
        if (this.spawningTask != null && !this.spawningTask.isCancelled()) {
            this.spawningTask.cancel();
        }

        if (this.lifetimeTask != null && !this.lifetimeTask.isCancelled()) {
            this.lifetimeTask.cancel();
        }

        this.deathNumber = 0;
        this.spawningTask = null;
        this.lifetimeTask = null;
        this.activePowerUps.clear();
    }

    @Override
    public void parseStart() {
        debug("using powerups : {} : ", this.powerupSpawnTrigger, this.powerupFreq);
        if (this.powerupFreq > 0) {
            debug("powerup time trigger!");
            long oneSecond = 20L;
            long tickNumber = this.powerupFreq * oneSecond; // calculate seconds to ticks
            // initiate autosave timer
            this.spawningTask = new PowerupSpawnRunnable(this);
            this.spawningTask.runTaskTimer(PVPArena.getInstance(), tickNumber, tickNumber);
            this.lifetimeTask = new PowerupLifetimeRunnable(this);
            this.lifetimeTask.runTaskTimer(PVPArena.getInstance(), oneSecond, oneSecond);
        }
    }

    static void removePowerupItemForPlayer(ArenaPlayer arenaPlayer) {
        PlayerInventory inventory = arenaPlayer.getPlayer().getInventory();
        Arrays.stream(inventory.getContents())
                .filter(PowerUps::isPowerup)
                .findAny()
                .ifPresent(inventory::remove);
    }

    static void resetFreezeEffectIfNeeded(Player player) {
        if(player.getWalkSpeed() == 0) {
            player.setWalkSpeed(DEFAULT_WALK_SPEED);
        }
    }

    private void cancelCurrentPowerUp(ArenaPlayer ap) {
        PowerupItem activePuForPlayer = this.activePowerUps.get(ap);
        if (activePuForPlayer != null) {
            activePuForPlayer.removeEffects(ap.getPlayer());
            this.activePowerUps.remove(ap);
            removePowerupItemForPlayer(ap);
        }
    }
}
