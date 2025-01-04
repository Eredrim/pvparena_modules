package net.slipcor.pvparena.modules.blockrestore;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.commands.AbstractArenaCommand;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.exceptions.GameplayException;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.PermissionManager;
import net.slipcor.pvparena.regions.ArenaRegion;
import net.slipcor.pvparena.regions.RegionType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.type.Chest;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;
import static net.slipcor.pvparena.config.Debugger.debug;
import static net.slipcor.pvparena.core.CollectionUtils.containsIgnoreCase;

public class BlockRestore extends ArenaModule implements Listener {

    private boolean listening;
    private boolean usingPaperEvents = false;

    private final TreeMap<Long, Set<ArenaBlock>> blocks = new TreeMap<>();
    private final Stack<ArenaContainer> containers = new Stack<>();
    private final Map<PABlockLocation, ArenaBlock> interactions = new HashMap<>();
    private final Set<PABlockLocation> allBlockLocations = new HashSet<>();
    private final List<Listener> listeners = new ArrayList<>();
    private final Listener restorationListener;

    private boolean restoringBlocks;
    private boolean restoringContainers;
    private boolean restoringInteractions;

    public BlockRestore() {
        super("BlockRestore");

        this.restorationListener = new RestorationListener(this);
        this.listeners.add(new CommonListener(this));

        try {
            Class.forName("com.destroystokyo.paper.event.block.BlockDestroyEvent");
            this.listeners.add(new PaperListener(this));
            this.usingPaperEvents = true;
        } catch (ClassNotFoundException ignored) {
        }
    }

    @Override
    public String version() {
        return this.getClass().getPackage().getImplementationVersion();
    }

    public boolean isRestoringBlocks() {
        return this.restoringBlocks;
    }

    public boolean isRestoringContainers() {
        return this.restoringContainers;
    }

    @Override
    public boolean checkCommand(final String s) {
        return "blockrestore".equalsIgnoreCase(s) || "!br".equalsIgnoreCase(s);
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("blockrestore");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!br");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        result.define(new String[]{"hard"});
        result.define(new String[]{"restoreblocks"});
        result.define(new String[]{"restorecontainers"});
        result.define(new String[]{"restoreinteractions"});
        result.define(new String[]{"addinv"});
        result.define(new String[]{"clearinv"});
        result.define(new String[]{"offset"});
        return result;
    }

    @Override
    public void checkJoin(Player player) throws GameplayException {
        if (this.restoringBlocks || this.restoringContainers || this.restoringInteractions) {
            throw new GameplayException("restoring");
        }
    }

    @Override
    public void checkSpectate(Player player) throws GameplayException {
        this.checkJoin(player);
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        // !br hard
        // !br restoreblocks
        // !br restorecontainers
        // !br restoreinteractions
        // !br addinv
        // !br clearinv
        // !br offset X

        if (!PermissionManager.hasAdminPerm(sender) && !PermissionManager.hasBuilderPerm(sender, this.arena)) {
            Arena.pmsg(sender, MSG.ERROR_NOPERM, Language.parse(MSG.ERROR_NOPERM_X_ADMIN));
            return;
        }

        if (!AbstractArenaCommand.argCountValid(sender, this.arena, args, new Integer[]{2, 3})) {
            return;
        }

        if ("addinv".equalsIgnoreCase(args[1])) {
            if (!(sender instanceof Player)) {
                Arena.pmsg(sender, MSG.ERROR_ONLY_PLAYERS);
                return;
            }

            Player player = (Player) sender;
            Block b = player.getTargetBlock(null, 10);

            if (b.getState() instanceof Container) {
                PABlockLocation loc = new PABlockLocation(b.getLocation());
                List<String> containerList = this.arena.getConfig().getStringList(CFG.MODULES_BLOCKRESTORE_CONTAINERLIST);
                containerList.add(loc.toString());
                this.arena.getConfig().set(CFG.MODULES_BLOCKRESTORE_CONTAINERLIST, containerList);
                this.arena.getConfig().save();
                this.arena.msg(sender, MSG.MODULE_BLOCKRESTORE_ADDEDTOLIST, loc.toString());
            } else {
                this.arena.msg(sender, MSG.ERROR_NO_CONTAINER);
            }
            return;
        }

        if ("clearinv".equalsIgnoreCase(args[1])) {

            this.arena.getConfig().set(CFG.MODULES_BLOCKRESTORE_CONTAINERLIST, new ArrayList<>());
            this.arena.getConfig().save();
            Arena.pmsg(sender, MSG.MODULE_BLOCKRESTORE_CLEARINVDONE);
            return;
        }

        if (containsIgnoreCase(asList("hard", "restorecontainers", "restoreblocks", "restoreinteractions"), args[1])) {
            final CFG cfg;
            if ("hard".equalsIgnoreCase(args[1])) {
                cfg = CFG.MODULES_BLOCKRESTORE_HARD;
            } else if (args[1].contains("blocks")) {
                cfg = CFG.MODULES_BLOCKRESTORE_RESTOREBLOCKS;
            } else if (args[1].contains("interactions")) {
                cfg = CFG.MODULES_BLOCKRESTORE_RESTOREINTERACTIONS;
            } else {
                cfg = CFG.MODULES_BLOCKRESTORE_RESTORECONTAINERS;
            }
            final boolean currentValue = this.arena.getConfig().getBoolean(cfg);

            this.arena.getConfig().set(cfg, !currentValue);
            this.arena.getConfig().save();
            this.arena.msg(sender, MSG.SET_DONE, cfg.getNode(), String.valueOf(!currentValue));

            return;
        }

        if ("offset".equalsIgnoreCase(args[1])) {
            if (!AbstractArenaCommand.argCountValid(sender, this.arena, args, new Integer[]{3})) {
                return;
            }

            final int i;
            try {
                i = Integer.parseInt(args[2]);
            } catch (final Exception e) {
                this.arena.msg(sender, MSG.ERROR_NOT_NUMERIC, args[2]);
                return;
            }

            this.arena.getConfig().set(CFG.MODULES_BLOCKRESTORE_OFFSET, i);
            this.arena.getConfig().save();
            this.arena.msg(sender, MSG.SET_DONE, CFG.MODULES_BLOCKRESTORE_OFFSET.getNode(), String.valueOf(i));
        }
    }

    @Override
    public void displayInfo(final CommandSender player) {
        player.sendMessage(StringParser.colorVar("hard", this.isHardModeEnabled())
                + " | " + StringParser.colorVar("blocks", this.isBlockRestoreEnabled())
                + " | " + StringParser.colorVar("chests", this.isContainerRestoreEnabled())
                + " | " + StringParser.colorVar("interactions", this.isInteractionRestoreEnabled())
                + " | offset " + this.arena.getConfig().getInt(CFG.MODULES_BLOCKRESTORE_OFFSET));
    }

    @Override
    public boolean needsBattleRegion() {
        return true;
    }

    @Override
    public void onEntityExplode(final EntityExplodeEvent event) {
        if (this.arena.isFightInProgress() && !this.isHardModeEnabled() && this.isBlockRestoreEnabled()) {
            this.saveBlocks(event.blockList());
        }
    }

    @Override
    public void onBlockBreak(final Block block) {
        if(this.arena.isFightInProgress()) {
            debug("block break in blockRestore");
            if (this.isHardModeEnabled() || !this.isBlockRestoreEnabled()) {
                debug("{} || blockRestore.hard: {}", this.arena, this.isHardModeEnabled());
            } else {
                this.saveBlock(block);
            }
        }
    }

    @Override
    public void onBlockPiston(final BlockPistonExtendEvent event) {
        if (this.arena.isFightInProgress() && !this.isHardModeEnabled() && this.isBlockRestoreEnabled()) {
            if(event.isSticky()) {
                // For sticky pistons, stuck block is not saved to map
                event.getBlocks().stream()
                        .filter(block -> !event.getBlock().getRelative(event.getDirection()).equals(block))
                        .forEach(this::saveBlock);
            } else {
                event.getBlocks().forEach(this::saveBlock);
            }
        }
    }

    @Override
    public void onBlockPlace(final Block block, final Material mat) {
        if (this.arena.isFightInProgress() && !this.isHardModeEnabled() && this.isBlockRestoreEnabled()) {
            this.saveBlock(block, mat);
        }
    }

    @Override
    public boolean onPlayerInteract(PlayerInteractEvent event) {
        if (this.arena.isFightInProgress() && !this.isHardModeEnabled() && this.isInteractionRestoreEnabled()) {
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                Block clickedBlock = event.getClickedBlock();
                BlockData clickedBlockData = clickedBlock.getBlockData();
                PABlockLocation blockLocation = new PABlockLocation(clickedBlock.getLocation());

                boolean isInBattleRegion = this.arena.getRegions().stream()
                        .filter(reg -> reg.getType() == RegionType.BATTLE)
                        .anyMatch(reg -> reg.getShape().contains(blockLocation));

                if(isInBattleRegion) {

                    if(clickedBlockData instanceof Openable || asList(Material.LEVER, Material.DAYLIGHT_DETECTOR, Material.CAKE).contains(clickedBlock.getType())) {

                        if(clickedBlockData instanceof Bisected) {
                            Bisected bisected = (Bisected) clickedBlockData;

                            if(bisected.getHalf() == Bisected.Half.TOP) {
                                blockLocation.setY(blockLocation.getY() - 1);
                            }
                        }

                        if(!this.interactions.containsKey(blockLocation)) {
                            this.interactions.put(blockLocation, new ArenaBlock(clickedBlock));
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void parseStart() {
        if (!this.listening) {
            this.listeners.forEach(listener ->
                    Bukkit.getPluginManager().registerEvents(listener, PVPArena.getInstance())
            );
            this.listening = true;
        }

        List<ArenaRegion> restorableRegions = this.arena.getRegions().stream()
                .filter(Objects::nonNull)
                .filter(r -> r.getType() == RegionType.BATTLE)
                .collect(Collectors.toList());

        if (restorableRegions.isEmpty()) {
            debug("no battlefield region, skipping restoreContainers");
            return;
        }

        this.loadAllBlocksIfNeeded(restorableRegions);

        if(this.isContainerRestoreEnabled()) {
            this.saveContainers();
        }
    }

    @Override
    public void reset(final boolean force) {
        final int delay = this.arena.getConfig().getInt(CFG.MODULES_BLOCKRESTORE_OFFSET);

        // Disable battlefield listeners
        this.listeners.forEach(HandlerList::unregisterAll);
        this.listening = false;

        if (this.isBlockRestoreEnabled() && !this.blocks.isEmpty()) {
            debug("resetting blocks");
            try {
                this.restoringBlocks = true;
                // Enable restoration listener to block battlefield changes
                Bukkit.getPluginManager().registerEvents(this.restorationListener, PVPArena.getInstance());

                BukkitRunnable restoreRunnable = new RestoreBlockRunnable(this, this.blocks);
                restoreRunnable.runTaskTimer(PVPArena.getInstance(), 0, delay);
            } catch (final IllegalPluginAccessException e) {
                this.instantlyRestoreArena();
            }
        }

        if (this.isContainerRestoreEnabled() && !this.containers.isEmpty()) {
            debug("resetting chests");
            try {
                this.restoringContainers = true;
                new RestoreContainerRunnable(this, this.containers).runTaskTimer(PVPArena.getInstance(), 0, delay);
            } catch (final IllegalPluginAccessException e) {
                new RestoreContainerRunnable(this, this.containers).runOnce();
            }
        }

        if (this.isBlockRestoreEnabled() && !this.interactions.isEmpty()) {
            debug("resetting interactions");
            try {
                this.restoringInteractions = true;
                new RestoreInteractionsRunnable(this, this.interactions.values()).runTaskTimer(PVPArena.getInstance(), 0, delay);
            } catch (final IllegalPluginAccessException e) {
                this.interactions.values().forEach(ArenaBlock::reset);
                this.interactions.clear();
                this.restoringInteractions = false;
            }
        }

        this.allBlockLocations.clear();
    }

    /**
     * Used to signal end of block restoring
     */
    void finishBlockRestore() {
        this.restoringBlocks = false;
        HandlerList.unregisterAll(this.restorationListener);
    }

    /**
     * Used to signal end of block restoring
     */
    void finishContainerRestore() {
        this.restoringContainers = false;
    }

    void finishInteractionRestore() {
        this.restoringInteractions = false;
    }

    /**
     * save a block to be restored (block destroy)
     *
     * @param block the block to save
     */
    void saveBlock(Block block) {
        this.addBlockToMap(new ArenaBlock(block));

        if(!this.usingPaperEvents) {
            this.addBlocksToMap(this.getLinkedBlocksToSave(block));
        }
    }

    /**
     * save a block to be restored (block placed)
     *
     * @param block the block to save
     */
    void saveBlock(Block block, Material material) {
        this.addBlockToMap(new ArenaBlock(block, material));
    }

    /**
     * save a block list to be restored (explosion)
     * @param blockList List of blocks
     */
    void saveBlocks(List<Block> blockList) {
        this.addBlocksToMap(blockList.stream().map(ArenaBlock::new).collect(toSet()));

        if(!this.usingPaperEvents) {
            Set<ArenaBlock> linkedBlocks = new HashSet<>();
            blockList.forEach(block -> linkedBlocks.addAll(this.getLinkedBlocksToSave(block)));
            long nextTick = System.currentTimeMillis() + 5;

            // grouped linked blocks have to be regened at least tick to avoid being reloaded before their support block
            this.blocks.put(nextTick, linkedBlocks);
        }
    }

    /**
     * Retrieve linked blocks which can be broken when a block next to it is broken.
     * This is an alternative method for non paper servers
     * Only works with doors and not solid blocks on the top, along with all directional blocks (excepting containers)
     * on sides and bottom of the block.
     * @param block the main block.
     */
    private Set<ArenaBlock> getLinkedBlocksToSave(Block block) {
        Set<ArenaBlock> newBlocksSet = new HashSet<>();
        if(block.getType().isSolid()) {
            Block upBlock = block.getRelative(BlockFace.UP);
            if(!upBlock.getType().isSolid() || Tag.DOORS.isTagged(upBlock.getType())) {
                newBlocksSet.add(new ArenaBlock(upBlock));
            }

            asList(BlockFace.DOWN, BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH).forEach(blockFace -> {
                BlockData linkedBlockData = block.getRelative(blockFace).getBlockData();
                if(linkedBlockData instanceof Directional && !(linkedBlockData instanceof Container) && ((Directional) linkedBlockData).getFacing() == blockFace) {
                   newBlocksSet.add(new ArenaBlock(block.getRelative(blockFace)));
                }
            });
        }
        return newBlocksSet;
    }

    private void addBlockToMap(ArenaBlock arenaBlock) {
        long currentTime = System.currentTimeMillis();
        if(!this.blocks.isEmpty() && currentTime < this.blocks.lastKey() + 5) {
            this.blocks.lastEntry().getValue().add(arenaBlock);
        } else {
            this.blocks.put(currentTime, Stream.of(arenaBlock).collect(toSet()));
        }
    }

    private void addBlocksToMap(Set<ArenaBlock> blockSet) {
        long currentTime = System.currentTimeMillis();
        if(!this.blocks.isEmpty() && currentTime < this.blocks.lastKey() + 5) {
            this.blocks.lastEntry().getValue().addAll(blockSet);
        } else {
            this.blocks.put(currentTime, blockSet);
        }
    }

    private void loadAllBlocksIfNeeded(List<ArenaRegion> regionList) {
        Config config = this.arena.getConfig();
        boolean blockMode = config.getBoolean(CFG.MODULES_BLOCKRESTORE_RESTOREBLOCKS);
        boolean containerMode = config.getBoolean(CFG.MODULES_BLOCKRESTORE_RESTORECONTAINERS);
        boolean hardMode = config.getBoolean(CFG.MODULES_BLOCKRESTORE_HARD);
        boolean needsContainerRegistration = config.getStringList(CFG.MODULES_BLOCKRESTORE_CONTAINERLIST).isEmpty();

        if((containerMode && needsContainerRegistration) || (blockMode && hardMode)) {
            this.allBlockLocations.clear();
            regionList.forEach(arenaRegion -> this.allBlockLocations.addAll(arenaRegion.getShape().getAllBlocks()));

            if(blockMode && hardMode) {
                this.allBlockLocations.forEach(loc -> {
                    World world = this.arena.getWorld();
                    this.saveBlock(world.getBlockAt(loc.getX(), loc.getY(), loc.getZ()));
                });
            }
        }
    }

    /**
     * Instantly restore all blocks of an arena
     */
    private void instantlyRestoreArena() {
        this.blocks.values().forEach(blockSet ->
                blockSet.forEach(ArenaBlock::reset)
        );
        this.blocks.clear();
        this.finishBlockRestore();
    }

    private void saveContainers() {
        Config config = this.arena.getConfig();
        if (config.getStringList(CFG.MODULES_BLOCKRESTORE_CONTAINERLIST).isEmpty()) {
            debug("NO inventories");
            this.containers.clear();

            List<String> inventoryLocations = new ArrayList<>();
            this.allBlockLocations.forEach(paBlockLocation -> {
                boolean isSaved = this.addToSavedContainers(paBlockLocation);
                if(isSaved) {
                    inventoryLocations.add(paBlockLocation.toString());
                }
            });

            config.set(CFG.MODULES_BLOCKRESTORE_CONTAINERLIST, inventoryLocations);
            config.save();

        } else {

            debug("reading inventories");
            config.getStringList(CFG.MODULES_BLOCKRESTORE_CONTAINERLIST).stream()
                    .map(PABlockLocation::new)
                    .forEach(this::addToSavedContainers);
        }
    }

    private boolean isHardModeEnabled() {
        return this.arena.getConfig().getBoolean(CFG.MODULES_BLOCKRESTORE_HARD);
    }

    private boolean isContainerRestoreEnabled() {
        return this.arena.getConfig().getBoolean(CFG.MODULES_BLOCKRESTORE_RESTORECONTAINERS);
    }

    private boolean isBlockRestoreEnabled() {
        return this.arena.getConfig().getBoolean(CFG.MODULES_BLOCKRESTORE_RESTOREBLOCKS);
    }

    private boolean isInteractionRestoreEnabled() {
        return this.arena.getConfig().getBoolean(CFG.MODULES_BLOCKRESTORE_RESTOREINTERACTIONS);
    }

    private boolean addToSavedContainers(PABlockLocation blockLocation) {
        Location location = blockLocation.toLocation();
        Block block = location.getBlock();
        if(block.getState() instanceof Container) {
            Container container = (Container) block.getState();

            // Skipping second part of double chests
            if(block.getBlockData() instanceof Chest) {
                Chest chestData = (Chest) block.getBlockData();
                if(chestData.getType() == Chest.Type.RIGHT) {
                    return false;
                }
            }

            this.containers.push(new ArenaContainer(location, container.getInventory().getContents()));
            return true;
        }
        return false;
    }

    void saveBlockIfInBattleground(Block toCheck) {
        for (final ArenaRegion region : this.arena.getRegionsByType(RegionType.BATTLE)) {
            if (region.getShape().contains(new PABlockLocation(toCheck.getLocation()))) {
                this.saveBlock(toCheck);
            }
        }
    }

    /**
     * Saved block to map in the same set than the previous ones
     * Only used for indirectly destroyed blocks to restore them without physics issues
     * @param toCheck Destroyed block to check
     */
    void saveBlockWithPreviousEntryIfInBattleground(Block toCheck) {
        for (final ArenaRegion region : this.arena.getRegionsByType(RegionType.BATTLE)) {
            if (region.getShape().contains(new PABlockLocation(toCheck.getLocation()))) {
                ArenaBlock blockToAdd = new ArenaBlock(toCheck);
                if(this.blocks.isEmpty()) {
                    this.blocks.put(System.currentTimeMillis(), Stream.of(blockToAdd).collect(toSet()));
                } else {
                    this.blocks.lastEntry().getValue().add(blockToAdd);
                }
            }
        }
    }
}
