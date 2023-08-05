package net.slipcor.pvparena.modules.bettergears;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaClass;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.commands.AbstractArenaCommand;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.PermissionManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static java.lang.Short.parseShort;
import static net.slipcor.pvparena.config.Debugger.debug;

public class BetterGears extends ArenaModule {
    private final Map<ArenaTeam, Color> colorMap = new HashMap<>();

    public BetterGears() {
        super("BetterGears");
    }

    @Override
    public String version() {
        return this.getClass().getPackage().getImplementationVersion();
    }

    @Override
    public boolean checkCommand(final String s) {
        return "!bg".equals(s) || s.startsWith("bettergear");
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("bettergears");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!bg");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        if (arena == null) {
            return result;
        }

        arena.getTeamNames().forEach(team -> result.define(new String[]{team, "color"}));

        return result;
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        // !bg [teamname] | show
        // !bg [teamname] color <R> <G> <B> | set color
        if (!PermissionManager.hasAdminPerm(sender) && !PermissionManager.hasBuilderPerm(sender, this.arena)) {
            this.arena.msg(sender, MSG.ERROR_NOPERM, Language.parse(MSG.ERROR_NOPERM_X_ADMIN));
            return;
        }

        if (!AbstractArenaCommand.argCountValid(sender, this.arena, args, new Integer[]{2, 6})) {
            return;
        }


        final ArenaTeam team = this.arena.getTeam(args[1]);
        if (team != null) {

            if (args.length == 2) {
                this.arena.msg(sender, Language.parse(MSG.MODULE_BETTERGEARS_SHOWTEAM, team.getColoredName(), this.colorMap.get(team)));
                return;
            }

            if (args.length != 6 || !"color".equalsIgnoreCase(args[2])) {
                this.printHelp(sender);
                return;
            }

            try {
                Color color = Color.fromRGB(parseShort(args[3]), parseShort(args[4]), parseShort(args[5]));
                this.arena.getConfig().setManually(String.format("modules.bettergears.colors.%s", team.getName()), color.serialize());
                this.arena.getConfig().save();
                this.colorMap.put(team, color);
                String rgbVal = String.format("%s,%s,%s", color.getRed(), color.getGreen(), color.getBlue());
                this.arena.msg(sender, Language.parse(MSG.MODULE_BETTERGEARS_TEAMDONE, team.getColoredName(), rgbVal));
            } catch (final Exception e) {
                this.arena.msg(sender, MSG.ERROR_NOT_NUMERIC, args[3]);
            }
        }
    }

    @Override
    public void configParse(final YamlConfiguration cfg) {
        if (this.colorMap.isEmpty()) {
            this.setup();
        }
    }

    @Override
    public void displayInfo(final CommandSender sender) {
        this.colorMap.forEach((team, color) -> sender.sendMessage(String.format("%s: %s,%s,%s", team.getName(), color.getRed(), color.getGreen(), color.getBlue())));
    }

    private void equipOnNextTick(final ArenaPlayer arenaPlayer) {
        Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), () -> this.equip(arenaPlayer), 3L);
    }

    private void equip(final ArenaPlayer arenaPlayer) {

        Player player = arenaPlayer.getPlayer();
        debug(this.arena, player, "equipping better gear!");

        if (this.colorMap.isEmpty()) {
            this.setup();
        }

        Color gearColor;
        if (this.getArena().isFreeForAll()) {
            int r = new Random().nextInt(256);
            int g = new Random().nextInt(256);
            int b = new Random().nextInt(256);
            gearColor = Color.fromBGR(b, g, r);
        } else {
            gearColor = this.colorMap.get(arenaPlayer.getArenaTeam());
        }


        final ItemStack[] armorElements = new ItemStack[4];
        armorElements[0] = new ItemStack(Material.LEATHER_HELMET, 1);
        armorElements[1] = new ItemStack(Material.LEATHER_CHESTPLATE, 1);
        armorElements[2] = new ItemStack(Material.LEATHER_LEGGINGS, 1);
        armorElements[3] = new ItemStack(Material.LEATHER_BOOTS, 1);

        for (ItemStack itemStack : armorElements) {
            final LeatherArmorMeta lam = (LeatherArmorMeta) itemStack.getItemMeta();
            lam.setColor(gearColor);
            itemStack.setItemMeta(lam);
        }

        if (this.arena.getConfig().getBoolean(CFG.MODULES_BETTERGEARS_HEAD)) {
            this.replaceArmorItem(EquipmentSlot.HEAD, player, armorElements[0]);
        }

        if (this.arena.getConfig().getBoolean(CFG.MODULES_BETTERGEARS_CHEST)) {
            this.replaceArmorItem(EquipmentSlot.CHEST, player, armorElements[1]);
        }

        if (this.arena.getConfig().getBoolean(CFG.MODULES_BETTERGEARS_LEG)) {
            this.replaceArmorItem(EquipmentSlot.LEGS, player, armorElements[2]);
        }

        if (this.arena.getConfig().getBoolean(CFG.MODULES_BETTERGEARS_FOOT)) {
            this.replaceArmorItem(EquipmentSlot.FEET, player, armorElements[3]);
        }
    }

    @Override
    public void initiate(final Player player) {
        if (this.colorMap.isEmpty()) {
            this.setup();
        }
    }

    @Override
    public void lateJoin(final Player player) {
        this.equipOnNextTick(ArenaPlayer.fromPlayer(player));
    }

    @Override
    public void reset(final boolean force) {
        this.colorMap.clear();
    }

    @Override
    public void parseClassChange(Player player, ArenaClass arenaClass) {
        this.equipOnNextTick(ArenaPlayer.fromPlayer(player));
    }

    @Override
    public void parseStart() {

        if (this.colorMap.isEmpty()) {
            this.setup();
        }

        this.arena.getFighters().forEach(this::equipOnNextTick);
    }

    @Override
    public void parseRespawn(final Player player, final ArenaTeam team, final DamageCause cause, final Entity damager) {
        if (this.arena.getConfig().getBoolean(CFG.PLAYER_REFILLINVENTORY)) {
            this.equipOnNextTick(ArenaPlayer.fromPlayer(player));
        }
    }

    private void printHelp(final CommandSender sender) {
        this.arena.msg(sender, "/pa [arenaname] !bg [teamname]  | show team color");
        this.arena.msg(sender, String.format("/pa [arenaname] !bg [teamname] color %s<R> %s<G> %s<B>%s | set color", ChatColor.RED, ChatColor.GREEN, ChatColor.BLUE, ChatColor.RESET));
        this.arena.msg(sender, "/pa [arenaname] !bg [classname] | show protection level");
        this.arena.msg(sender, "/pa [arenaname] !bg [classname] level <level> | set protection level");
    }

    private void setup() {
        debug("Setting up BetterGears");

        this.arena.getTeams().forEach(team -> {
            ConfigurationSection cs = this.arena.getConfig().getConfigurationSection(String.format("modules.bettergears.colors.%s", team.getName()));
            Color color;
            if(cs == null) {
                color = ChatColorMap.valueOf(team.getColor().name()).getColor();
            } else {
                color = Color.deserialize(cs.getValues(false));
            }
            this.colorMap.put(team, color);
            debug("{} : {},{},{}", team.getName(), color.getRed(), color.getGreen(), color.getBlue());
        });
    }

    private void replaceArmorItem(EquipmentSlot slot, Player player, ItemStack setItem) {
        PlayerInventory inventory = player.getInventory();
        switch (slot) {
            case HEAD:
                inventory.setHelmet(this.getColoredItemStack(inventory.getHelmet(), setItem));
                break;
            case CHEST:
                inventory.setChestplate(this.getColoredItemStack(inventory.getChestplate(), setItem));
                break;
            case LEGS:
                inventory.setLeggings(this.getColoredItemStack(inventory.getLeggings(), setItem));
                break;
            case FEET:
                inventory.setBoots(this.getColoredItemStack(inventory.getBoots(), setItem));
                break;
        }
    }

    private ItemStack getColoredItemStack(ItemStack checkItem, ItemStack setItem) {
        if(!this.arena.getConfig().getBoolean(CFG.MODULES_BETTERGEARS_ONLYIFLEATHER)) {
            return setItem;
        } else if (checkItem != null && setItem != null && setItem.getType().equals(checkItem.getType())) {
            LeatherArmorMeta checkMeta = (LeatherArmorMeta) checkItem.getItemMeta();
            LeatherArmorMeta setMeta = (LeatherArmorMeta) setItem.getItemMeta();
            checkMeta.setColor(setMeta.getColor());
            checkItem.setItemMeta(checkMeta);
            return checkItem;
        }
        return checkItem;
    }
}
