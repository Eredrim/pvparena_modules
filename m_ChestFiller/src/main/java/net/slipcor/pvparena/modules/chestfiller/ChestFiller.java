package net.slipcor.pvparena.modules.chestfiller;

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
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.PermissionManager;
import net.slipcor.pvparena.regions.RegionType;
import org.bukkit.Bukkit;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.ShulkerBox;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static net.slipcor.pvparena.config.Debugger.debug;

public class ChestFiller extends ArenaModule {
    private boolean clear;
    private int minItems;
    private int maxItems;

    public ChestFiller() {
        super("ChestFiller");
    }

    @Override
    public String version() {
        return this.getClass().getPackage().getImplementationVersion();
    }

    @Override
    public boolean checkCommand(final String s) {
        return "!cf".equalsIgnoreCase(s) || s.equalsIgnoreCase("chestfiller");
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("chestfiller");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!cf");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        result.define(new String[]{"sourcelocation"});
        result.define(new String[]{"clear"});
        result.define(new String[]{"addcontainer"});
        return result;
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        if (!PermissionManager.hasAdminPerm(sender) && !PermissionManager.hasBuilderPerm(sender, this.arena)) {
            this.arena.msg(sender, MSG.ERROR_NOPERM, Language.parse(MSG.ERROR_NOPERM_X_ADMIN));
            return;
        }

        if (!AbstractArenaCommand.argCountValid(sender, this.arena, args, new Integer[]{2,3})) {
            return;
        }

        if ("sourcelocation".equalsIgnoreCase(args[1])) {
            if (!(sender instanceof Player)) {
                Arena.pmsg(sender, MSG.ERROR_ONLY_PLAYERS);
                return;
            }
            Player player = (Player) sender;
            Block b = player.getTargetBlock(null, 10);

            if (args.length == 3) {
                if("none".equalsIgnoreCase(args[2])) {
                    this.arena.getConfig().set(CFG.MODULES_CHESTFILLER_SOURCELOCATION, "none");
                    this.arena.getConfig().save();
                    this.arena.msg(sender, Language.parse(MSG.MODULE_CHESTFILLER_SOURCECHEST_REMOVED));
                } else {
                    this.arena.msg(sender, MSG.ERROR_ARGUMENT, args[2]);
                }
            } else if (b.getState() instanceof Container) {
                PABlockLocation loc = new PABlockLocation(b.getLocation());

                this.arena.getConfig().set(CFG.MODULES_CHESTFILLER_SOURCELOCATION, loc.toString());
                this.arena.getConfig().save();
                this.arena.msg(sender, MSG.MODULE_CHESTFILLER_SOURCECHEST, loc.toString());
            } else {
                this.arena.msg(sender, MSG.ERROR_NO_CONTAINER);
            }

        } else if ("addcontainer".equalsIgnoreCase(args[1])) {
            if (!(sender instanceof Player)) {
                Arena.pmsg(sender, MSG.ERROR_ONLY_PLAYERS);
                return;
            }

            Player player = (Player) sender;
            Block b = player.getTargetBlock(null, 10);

            if (b.getState() instanceof Container) {
                PABlockLocation loc = new PABlockLocation(b.getLocation());
                List<String> chestsToFill = this.arena.getConfig().getStringList(CFG.MODULES_CHESTFILLER_CONTAINERLIST);
                chestsToFill.add(loc.toString());
                this.arena.getConfig().set(CFG.MODULES_CHESTFILLER_CONTAINERLIST, chestsToFill);
                this.arena.getConfig().save();
                this.arena.msg(sender, MSG.MODULE_CHESTFILLER_ADDEDTOLIST, loc.toString());
            } else {
                this.arena.msg(sender, MSG.ERROR_NO_CONTAINER);
            }
        } else if ("clear".equals(args[1])) {
            this.arena.getConfig().set(CFG.MODULES_CHESTFILLER_CONTAINERLIST, new ArrayList<>());
            this.arena.getConfig().save();

            sender.sendMessage(Language.parse(MSG.MODULE_CHESTFILLER_CLEAR));
        } else {
            this.arena.msg(sender, MSG.ERROR_ARGUMENT, args[1]);
        }

    }

    @Override
    public void configParse(YamlConfiguration yamlConfig) {
        Config config = this.arena.getConfig();
        this.clear = config.getBoolean(CFG.MODULES_CHESTFILLER_CLEAR);
        this.maxItems = Integer.parseInt(String.valueOf(config.getInt(CFG.MODULES_CHESTFILLER_MAXITEMS)));
        this.minItems = Integer.parseInt(String.valueOf(config.getInt(CFG.MODULES_CHESTFILLER_MINITEMS)));
    }

    @Override
    public void displayInfo(final CommandSender sender) {
        String content = this.arena.getConfig().getString(CFG.MODULES_CHESTFILLER_SOURCELOCATION);
        sender.sendMessage("items: " + (content.equals("none")?
                StringParser.getItems(this.arena.getConfig().getItems(CFG.MODULES_CHESTFILLER_ITEMS)) :
                content));
        sender.sendMessage(String.format("max: %d | min: %d | clear: %s",
                this.arena.getConfig().getInt(CFG.MODULES_CHESTFILLER_MAXITEMS),
                this.arena.getConfig().getInt(CFG.MODULES_CHESTFILLER_MINITEMS),
                this.arena.getConfig().getBoolean(CFG.MODULES_CHESTFILLER_CLEAR)));

    }

    @Override
    public boolean needsBattleRegion() {
        return true;
    }

    @Override
    public void parseStart() {
        Config config = this.arena.getConfig();

        ItemStack[] fillingContent = this.getFillingContent();
        if (fillingContent == null) {
            return;
        }

        if(config.getStringList(CFG.MODULES_CHESTFILLER_CONTAINERLIST).isEmpty()) {
            this.registerBattleContainers();
        }

        // Run on next tick to let blockRestore saving chests content before
        Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), () -> {
            debug("[ChestFiller] filling containers on startup");
            config.getStringList(CFG.MODULES_CHESTFILLER_CONTAINERLIST).stream()
                    .map(PABlockLocation::new)
                    .forEach(loc -> this.fill(loc, fillingContent));
        }, 1);
    }

    private ItemStack[] getFillingContent() {
        String chest = this.arena.getConfig().getDefinedString(CFG.MODULES_CHESTFILLER_SOURCELOCATION);
        ItemStack[] contents = null;
        if(chest != null) {
            try {
                PABlockLocation loc = new PABlockLocation(chest);
                Container c = (Container) loc.toLocation().getBlock().getState();
                contents = Arrays.stream(c.getInventory().getContents())
                        .filter(itemStack -> itemStack != null && !itemStack.getType().isAir())
                        .map(ItemStack::clone)
                        .toArray(ItemStack[]::new);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                contents = this.arena.getConfig().getItems(CFG.MODULES_CHESTFILLER_ITEMS);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
        return contents;
    }

    private static boolean isDefaultKindOfChest(PABlockLocation blockLoc) {
        Block block = blockLoc.toLocation().getBlock();
        if(block.getBlockData() instanceof org.bukkit.block.data.type.Chest) {
            // Skipping second part of double chests
            return ((org.bukkit.block.data.type.Chest) block.getBlockData()).getType() != org.bukkit.block.data.type.Chest.Type.RIGHT;
        }
        return block.getState() instanceof ShulkerBox || block.getState() instanceof Barrel;
    }

    private void registerBattleContainers() {
        debug("[ChestFiller] creating default chests to fill");
        final List<String> result = this.arena.getRegionsByType(RegionType.BATTLE).stream()
                .flatMap(battleRegion -> battleRegion.getShape().getAllBlocks().stream()
                        .filter(ChestFiller::isDefaultKindOfChest)
                        .map(PABlockLocation::toString)
                )
                .collect(Collectors.toList());


        this.arena.getConfig().set(CFG.MODULES_CHESTFILLER_CONTAINERLIST, result);
        this.arena.getConfig().save();
    }

    private void fill(PABlockLocation loc, ItemStack[] fillingContent) {
        BlockState blockState = loc.toLocation().getBlock().getState();
        if(blockState instanceof Container) {
            Container c = (Container) blockState;
            if (this.clear) {
                c.getInventory().clear();
            }

            final Random r = new Random();
            int bound = Math.max(this.maxItems - this.minItems, 0);

            // if min == max or min > max, use max
            int count = (bound == 0) ? this.maxItems : r.nextInt(bound+1) + this.minItems;

            for (int i=0; i < count; i++) {
                int d = r.nextInt(fillingContent.length);
                c.getInventory().addItem(fillingContent[d].clone());
            }
        }
    }
}
