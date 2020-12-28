package net.slipcor.pvparena.modules.powerups;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.classes.PALocation;
import net.slipcor.pvparena.commands.AbstractArenaCommand;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.loadables.ArenaRegion;
import net.slipcor.pvparena.loadables.ArenaRegion.RegionType;
import net.slipcor.pvparena.managers.SpawnManager;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.*;

import static net.slipcor.pvparena.config.Debugger.debug;

public class PowerupManager extends ArenaModule implements Listener {


    public static final String POWERUPS_CFG = "modules.powerups.items";
    private Powerups usesPowerups;

    private int powerupDiff;
    private int powerupDiffI;

    int SPAWN_ID = -1;

    private boolean setup;

    public PowerupManager() {
        super("Powerups");
    }

    @Override
    public String version() {
        return getClass().getPackage().getImplementationVersion();
    }

    /**
     * calculate a powerup and commit it
     */
    void calcPowerupSpawn() {
        debug("powerups?");
        if (this.usesPowerups == null) {
            return;
        }

        if (this.usesPowerups.puTotal.size() <= 0) {
            return;
        }

        debug("totals are filled");
        int random = new Random().nextInt(this.usesPowerups.puTotal.size());

        Powerup p = this.usesPowerups.puTotal.get(random);
        this.commitPowerupItemSpawn(p.item);
        this.arena.broadcast(Language.parse(MSG.MODULE_POWERUPS_SERVER, p.name));
    }

    @Override
    public boolean checkCommand(final String s) {
        return "!pu".equals(s) || s.startsWith("powerup");
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("powerups");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!pu");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        result.define(new String[]{"time"});
        result.define(new String[]{"death"});
        result.define(new String[]{"off"});
        result.define(new String[]{"dropspawn"});
        return result;
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        // !pu time 6
        // !pu death 4

        if (!PVPArena.hasAdminPerms(sender)
                && !PVPArena.hasCreatePerms(sender, arena)) {
            arena.msg(
                    sender,
                    Language.parse(MSG.ERROR_NOPERM,
                            Language.parse(MSG.ERROR_NOPERM_X_ADMIN)));
            return;
        }

        if (!AbstractArenaCommand.argCountValid(sender, arena, args, new Integer[]{2, 3})) {
            return;
        }

        if ("!pu".equals(args[0]) || args[0].startsWith("powerup")) {
            if (args.length == 2) {
                if ("off".equals(args[1])) {
                    arena.getArenaConfig().set(CFG.MODULES_POWERUPS_USAGE, args[1]);
                    arena.getArenaConfig().save();
                    arena.msg(sender, Language.parse(MSG.SET_DONE, CFG.MODULES_POWERUPS_USAGE.getNode(), args[1]));
                    return;
                }
                if (args[1].equals("dropspawn")) {
                    boolean b = arena.getArenaConfig().getBoolean(CFG.MODULES_POWERUPS_DROPSPAWN);
                    arena.getArenaConfig().set(CFG.MODULES_POWERUPS_DROPSPAWN, !b);
                    arena.getArenaConfig().save();
                    arena.msg(sender, Language.parse(MSG.SET_DONE, CFG.MODULES_POWERUPS_DROPSPAWN.getNode(), String.valueOf(!b)));
                }
                arena.msg(sender, Language.parse(MSG.ERROR_ARGUMENT, args[1], "off | dropspawn"));
                return;
            }
            final int i;
            try {
                i = Integer.parseInt(args[2]);
            } catch (final Exception e) {
                arena.msg(sender,
                        Language.parse(MSG.ERROR_NOT_NUMERIC, args[2]));
                return;
            }
            if ("time".equals(args[1]) || "death".equals(args[1])) {
                arena.getArenaConfig().set(CFG.MODULES_POWERUPS_USAGE, args[1] + ':' + i);
                arena.getArenaConfig().save();
                arena.msg(sender, Language.parse(MSG.SET_DONE, CFG.MODULES_POWERUPS_USAGE.getNode(), args[1] + ':' + i));
                return;
            }

            arena.msg(sender, Language.parse(MSG.ERROR_ARGUMENT, args[1], "time | death"));
        }
    }

    @Override
    public void parsePlayerDeath(final Player player, final EntityDamageEvent lastDamageCause) {
        if (this.usesPowerups != null) {
            if (this.arena.getArenaConfig().getString(CFG.MODULES_POWERUPS_USAGE).startsWith("death")) {
                debug("calculating powerup trigger death");
                this.powerupDiffI = ++this.powerupDiffI % this.powerupDiff;
                if (this.powerupDiffI == 0) {
                    this.calcPowerupSpawn();
                }
            }
        }
    }

    /**
     * commit the powerup item spawn
     *
     * @param item the material to spawn
     */
    private void commitPowerupItemSpawn(final Material item) {
        debug("dropping item?");

        final Set<ArenaRegion> regions = this.arena.getRegionsByType(RegionType.BATTLE);


        if (regions.size() < 1 || this.arena.getArenaConfig().getBoolean(CFG.MODULES_POWERUPS_DROPSPAWN)) {
            if (!this.arena.getArenaConfig().getBoolean(CFG.MODULES_POWERUPS_DROPSPAWN)) {
                PVPArena.getInstance().getLogger().warning("You have deactivated 'dropspawn' but have no BATTLE region. Attempting to find powerup drop spawns!");
            }
            this.dropItemOnSpawn(item);

        } else {
            ArenaRegion ar = regions.iterator().next();

            final PABlockLocation min = ar.getShape().getMinimumLocation();
            final PABlockLocation max = ar.getShape().getMaximumLocation();

            final Random r = new Random();

            final int x = r.nextInt(max.getX() - min.getX());
            final int z = r.nextInt(max.getZ() - min.getZ());

            final World w = Bukkit.getWorld(min.getWorldName());
            Location dropLoc = w.getHighestBlockAt(min.getX() + x, min.getZ() + z).getRelative(BlockFace.UP).getLocation();

            w.dropItem(dropLoc, this.getTaggedItem(item)).setVelocity(new Vector());
        }
    }

    @Override
    public void configParse(final YamlConfiguration config) {
        if (!setup) {
            Bukkit.getPluginManager().registerEvents(this, PVPArena.getInstance());
            setup = true;
        }
        final Map<String, Object> powerupItems = new HashMap<>();
        ConfigurationSection powerupsCfgSection = config.getConfigurationSection(POWERUPS_CFG);

        if (powerupsCfgSection != null) {
            powerupItems.putAll(this.getItemMapFromConfig(powerupsCfgSection));
        }

        if (powerupItems.size() < 1) {
            return;
        }

        final String pu = arena.getArenaConfig().getString(CFG.MODULES_POWERUPS_USAGE, "off");

        final String[] ss = pu.split(":");
        if (pu.startsWith("death") || pu.startsWith("time")) {
            powerupDiff = Integer.parseInt(ss[1]);
            usesPowerups = new Powerups(powerupItems);
        } else {
            String errorMsg = String.format("Error activating powerup module : %s has unknown value", CFG.MODULES_POWERUPS_USAGE);
            PVPArena.getInstance().getLogger().warning(errorMsg);
        }

        config.options().copyDefaults(true);
    }

    private Map<String, Object> getItemMapFromConfig(ConfigurationSection cfgSection) {
        Map<String, Object> sectionMap = cfgSection.getValues(false);
        Map<String, Object> returnedMap = new HashMap<>();
        sectionMap.forEach((k, v) -> {
            if(v instanceof ConfigurationSection) {
                returnedMap.put(k, this.getItemMapFromConfig((ConfigurationSection) v));
            } else {
                returnedMap.put(k, v);
            }
        });
        return returnedMap;
    }

    @Override
    public void displayInfo(final CommandSender player) {
        player.sendMessage("usage: "
                + StringParser.colorVar(usesPowerups != null)
                + '('
                + StringParser.colorVar(arena.getArenaConfig().getString(CFG.MODULES_POWERUPS_USAGE))
                + ')');
    }

    /**
     * drop an item at a powerup spawn point
     *
     * @param item the item to drop
     */
    private void dropItemOnSpawn(final Material item) {
        debug("calculating item spawn location");
        List<PALocation> allowedLocations = new ArrayList<>(SpawnManager.getSpawnsContaining(arena, "powerup"));

        if (allowedLocations.size() < 1) {
            PVPArena.getInstance().getLogger().warning("No valid powerup spawns found!");
            return;
        }

        int random = new Random().nextInt(allowedLocations.size());
        Location dropLoc = allowedLocations.get(random).toLocation().add(0, 0.5, 0);
        debug("dropping item on spawn: {}", dropLoc);
        dropLoc.getWorld().dropItem(dropLoc, this.getTaggedItem(item)).setVelocity(new Vector());;
    }

    @Override
    public boolean hasSpawn(final String s) {
        return s.toLowerCase().startsWith("powerup");
    }

    private static final String POWERUPSTRING = ChatColor.RED + "Power\nUp";

    private ItemStack getTaggedItem(final Material material) {
        ItemStack itemStack = new ItemStack(material);
        final ItemMeta meta = itemStack.getItemMeta();

        meta.setDisplayName(POWERUPSTRING);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    private boolean isPowerup(final ItemStack item) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return false;
        }
        return item.getItemMeta().getDisplayName().equals(POWERUPSTRING);
    }

    @Override
    public void onEntityDamageByEntity(final Player attacker,
                                       final Player defender, final EntityDamageByEntityEvent event) {
        if (usesPowerups != null) {
            debug(attacker, "committing powerup triggers");
            debug(defender, "committing powerup triggers");
            Powerup p = usesPowerups.puActive.get(attacker);
            if (p != null && p.canBeTriggered()) {

                p.commit(attacker, defender, event, true);
            }
            p = usesPowerups.puActive.get(defender);
            if (p != null && p.canBeTriggered()) {
                p.commit(attacker, defender, event, false);
            }
        }

    }

    @Override
    public void onEntityRegainHealth(final EntityRegainHealthEvent event) {
        if (usesPowerups != null && event.getEntity() instanceof Player) {
            debug("regaining health");
            final Powerup p = usesPowerups.puActive.get(event.getEntity());
            if (p != null) {
                if (p.canBeTriggered()) {
                    if (p.isEffectActive(PowerupType.HEAL)) {
                        event.setCancelled(true);
                        p.commit(event);
                    }
                }
            }

        }
    }

    @Override
    @EventHandler
    public void onPlayerPickupItem(final EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof  Player)) {
            return;
        }
        final Player player = (Player) event.getEntity();
        final ArenaPlayer ap = ArenaPlayer.parsePlayer(player.getName());
        if (!arena.equals(ap.getArena())) {
            return;
        }
        if (usesPowerups != null && isPowerup(event.getItem().getItemStack())) {
            debug(player, "onPlayerPickupItem: fighting player");
            debug(player, "item: " + event.getItem().getItemStack().getType());
            for (final Powerup p : usesPowerups.puTotal) {
                debug(player, "is it " + p.item + '?');
                if (event.getItem().getItemStack().getType() == p.item) {
                    debug(player, "yes!");
                    final Powerup newP = new Powerup(p);
                    if (usesPowerups.puActive.containsKey(player)) {
                        usesPowerups.puActive.get(player).deactivate(player);
                        usesPowerups.puActive.remove(player);
                    }
                    usesPowerups.puActive.put(player, newP);
                    arena.broadcast(Language.parse(MSG.MODULE_POWERUPS_PLAYER,
                            player.getName(), newP.name));
                    event.setCancelled(true);
                    event.getItem().remove();
                    if (newP.canBeTriggered()) {
                        newP.activate(player); // activate for the first time
                    }

                    return;
                }
            }
        }
    }

    @Override
    public void onPlayerVelocity(final PlayerVelocityEvent event) {
        debug(event.getPlayer(), "inPlayerVelocity: fighting player");
        if (usesPowerups != null) {
            final Powerup p = usesPowerups.puActive.get(event.getPlayer());
            if (p != null) {
                if (p.canBeTriggered()) {
                    if (p.isEffectActive(PowerupType.JUMP)) {
                        p.commit(event);
                    }
                }
            }
        }
    }

    @EventHandler
    public void parseMove(final PlayerMoveEvent event) {

        // debug.i("onPlayerMove: fighting player!");
        if (usesPowerups != null) {
            //debug.i("parsing move");
            final Powerup p = usesPowerups.puActive.get(event.getPlayer());
            if (p != null) {
                if (p.canBeTriggered()) {
                    if (p.isEffectActive(PowerupType.FREEZE)) {
                        debug(event.getPlayer(), "freeze in effect, cancelling!");
                        event.setCancelled(true);
                    }
                    if (p.isEffectActive(PowerupType.SPRINT)) {
                        debug(event.getPlayer(), "sprint in effect, sprinting!");
                        event.getPlayer().setSprinting(true);
                    }
                    if (p.isEffectActive(PowerupType.SLIP)) {
                        //TODO add slippery effect!
                    }
                }
            }
        }
    }

    /**
     * powerup tick, tick each arena that uses powerups
     */
    protected void powerupTick() {
        if (usesPowerups != null) {
            usesPowerups.tick();
        }
    }

    @Override
    public void reset(final boolean force) {
        if (SPAWN_ID > -1) {
            Bukkit.getScheduler().cancelTask(SPAWN_ID);
        }
        SPAWN_ID = -1;
        if (usesPowerups != null) {
            usesPowerups.puActive.clear();
        }
    }

    @Override
    public void parseStart() {
        if (usesPowerups != null) {
            final String pu = arena.getArenaConfig().getString(CFG.MODULES_POWERUPS_USAGE);
            final String[] ss = pu.split(":");
            if (pu.startsWith("time")) {
                // arena.powerupTrigger = "time";
                powerupDiff = Integer.parseInt(ss[1]);
            } else {
                return;
            }

            debug("using powerups : {} : ", arena.getArenaConfig().getString(CFG.MODULES_POWERUPS_USAGE), powerupDiff);
            if (powerupDiff > 0) {
                debug("powerup time trigger!");
                powerupDiff *= 20; // calculate ticks to seconds
                // initiate autosave timer
                SPAWN_ID = Bukkit
                        .getServer()
                        .getScheduler()
                        .scheduleSyncRepeatingTask(PVPArena.getInstance(),
                                new PowerupRunnable(this), powerupDiff,
                                powerupDiff);
            }
        }
    }
}
