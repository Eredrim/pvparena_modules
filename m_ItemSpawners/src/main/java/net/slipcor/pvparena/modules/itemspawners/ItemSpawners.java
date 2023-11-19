package net.slipcor.pvparena.modules.itemspawners;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.commands.AbstractArenaCommand;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.ItemStackUtils;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.PermissionManager;
import net.slipcor.pvparena.managers.SpawnManager;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static net.slipcor.pvparena.core.Utils.getSerializableItemStacks;

public class ItemSpawners extends ArenaModule {
    private static final String CFG_PATH = "modules.itemspawners";
    private int interval = 0;
    private final Map<String, List<ItemStack>> spawnItemLists = new HashMap<>();
    private BukkitRunnable runnable;

    public ItemSpawners() {
        super("ItemSpawners");
    }

    @Override
    public String version() {
        return this.getClass().getPackage().getImplementationVersion();
    }

    @Override
    public boolean checkCommand(final String s) {
        return "!is".equalsIgnoreCase(s) || s.equalsIgnoreCase("itemspawners");
    }
    @Override
    public List<String> getMain() {
        return singletonList("itemspawners");
    }

    @Override
    public List<String> getShort() {
        return singletonList("!is");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        SpawnManager.getPASpawnsStartingWith(this.arena, "item").forEach(spawn -> {
            result.define(new String[]{"setItems", spawn.getName(), "hand"});
            result.define(new String[]{"setItems", spawn.getName(), "inventory"});
        });
        return result;
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        if (!PermissionManager.hasAdminPerm(sender) && !PermissionManager.hasBuilderPerm(sender, this.arena)) {
            this.arena.msg(sender, Language.MSG.ERROR_NOPERM, Language.parse(Language.MSG.ERROR_NOPERM_X_ADMIN));
            return;
        }

        if (!(sender instanceof Player)) {
            this.arena.msg(sender, Language.MSG.ERROR_ONLY_PLAYERS);
            return;
        }

        if (!AbstractArenaCommand.argCountValid(sender, this.arena, args, new Integer[]{4})) {
            return;
        }

        if("setItems".equalsIgnoreCase(args[1])) {
            PASpawn itemSpawn = SpawnManager.getPASpawnByExactName(this.arena, args[2]);
            if(itemSpawn != null) {
                String node = String.format("%s.%s", CFG_PATH, itemSpawn.getName());
                Config config = this.arena.getConfig();

                if ("hand".equalsIgnoreCase(args[3])) {
                    ItemStack item = ((Player) sender).getInventory().getItemInMainHand();
                    config.setManually(node, getSerializableItemStacks(item));
                    config.save();
                    this.spawnItemLists.put(itemSpawn.getName(), singletonList(item));
                    this.arena.msg(sender, Language.MSG.SET_DONE, node, item.getType().name());

                } else if ("inventory".equalsIgnoreCase(args[3])) {
                    final ItemStack[] items = ((Player) sender).getInventory().getContents();
                    config.setManually(node, getSerializableItemStacks(items));
                    config.save();
                    this.spawnItemLists.put(itemSpawn.getName(), Arrays.asList(items));
                    this.arena.msg(sender, Language.MSG.SET_DONE, node, "inventory");

                } else {
                    this.arena.msg(sender, Language.MSG.SET_ITEMS_NOT);
                }
            } else {
                this.arena.msg(sender, Language.MSG.SPAWN_UNKNOWN, args[2]);
            }
        } else {
            this.arena.msg(sender, Language.MSG.ERROR_COMMAND_UNKNOWN);
        }
    }


    @Override
    public void configParse(YamlConfiguration yamlConfig) {
        this.interval = this.arena.getConfig().getInt(CFG.MODULES_ITEMSPAWNERS_INTERVAL);
        this.arena.getConfig().getKeys(CFG_PATH).stream()
                .filter(key -> key.startsWith("item"))
                .forEach(key -> {
                    String path = String.format("%s.%s", CFG_PATH, key);
                    List<?> configItemList = yamlConfig.getList(path);
                    List<ItemStack> itemStackList = Arrays.stream(ItemStackUtils.getItemStacksFromConfig(configItemList))
                            .collect(Collectors.toList());
                    this.spawnItemLists.put(key, itemStackList);
                });
    }

    @Override
    public void displayInfo(final CommandSender sender) {
        sender.sendMessage(String.format("interval: %d", this.interval));
        this.spawnItemLists.forEach((key, value) -> {
            String itemsString = value.stream()
                    .map(item -> item.getType().name())
                    .collect(Collectors.joining(", ", String.format("%s: ", key), ""));
            sender.sendMessage(itemsString);
        });
    }

    @Override
    public boolean hasSpawn(final String s, final String teamName) {
        return s.toLowerCase().startsWith("item");
    }

    @Override
    public void reset(final boolean force) {
        if(this.runnable != null) {
            this.runnable.cancel();
            this.runnable = null;
        }
    }

    @Override
    public void parseStart() {
        if (this.interval > 0) {
            ofNullable(this.runnable).ifPresent(BukkitRunnable::cancel);
            this.runnable = new ItemSpawnRunnable(this.arena, this.spawnItemLists);
            long tickTime = this.interval * 20L;
            this.runnable.runTaskTimer(PVPArena.getInstance(), tickTime, tickTime);
        }
    }
}
