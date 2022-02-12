package net.slipcor.pvparena.modules.bettergears;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaClass;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.commands.AbstractArenaCommand;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.PermissionManager;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static net.slipcor.pvparena.config.Debugger.debug;

public class BetterGears extends ArenaModule {
    private static Map<String, String> defaultColors;

    private Map<ArenaTeam, Short[]> colorMap;
    private Map<ArenaClass, Short> levelMap;

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
        for (final String team : arena.getTeamNames()) {
            result.define(new String[]{team, "color"});
        }
        for (final ArenaClass aClass : arena.getClasses()) {
            result.define(new String[]{aClass.getName(), "level"});
        }
        return result;
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        // !bg [teamname] | show
        // !bg [teamname] color <R> <G> <B> | set color
        // !bg [classname] | show
        // !bg [classname] level <level> | protection level
        if (!PermissionManager.hasAdminPerm(sender) && !PermissionManager.hasBuilderPerm(sender, this.arena)) {
            this.arena.msg(sender, MSG.ERROR_NOPERM, Language.parse(MSG.ERROR_NOPERM_X_ADMIN));
            return;
        }

        if (!AbstractArenaCommand.argCountValid(sender, this.arena, args, new Integer[]{2,
                4, 6})) {
            return;
        }

        final ArenaClass c = this.arena.getClass(args[1]);

        if (c == null) {
            final ArenaTeam team = this.arena.getTeam(args[1]);
            if (team != null) {
                // !bg [teamname] | show
                // !bg [teamname] color <R> <G> <B> | set color

                if (args.length == 2) {
                    this.arena.msg(sender, Language.parse(
                            MSG.MODULE_BETTERGEARS_SHOWTEAM,
                            team.getColoredName(),
                            Arrays.toString(this.getColorMap().get(team))));
                    return;
                }

                if (args.length != 6 || !"color".equalsIgnoreCase(args[2])) {
                    this.printHelp(sender);
                    return;
                }

                try {
                    final Short[] rgb = new Short[3];
                    rgb[0] = Short.parseShort(args[3]);
                    rgb[1] = Short.parseShort(args[4]);
                    rgb[2] = Short.parseShort(args[5]);
                    this.arena.getConfig().setManually(
                            "modules.bettergears.colors." + team.getName(),
                            StringParser.joinArray(rgb, ","));
                    this.arena.getConfig().save();
                    this.getColorMap().put(team, rgb);
                    this.arena.msg(sender, Language.parse(
                            MSG.MODULE_BETTERGEARS_TEAMDONE,
                            team.getColoredName(), args[3]));
                } catch (final Exception e) {
                    this.arena.msg(sender, MSG.ERROR_NOT_NUMERIC, args[3]);
                }

                return;
            }
            // no team AND no class!

            this.arena.msg(sender, MSG.ERROR_CLASS_NOT_FOUND, args[1]);
            this.arena.msg(sender, MSG.ERROR_TEAM_NOT_FOUND, args[1]);
            this.printHelp(sender);
            return;
        }
        // !bg [classname] | show
        // !bg [classname] level <level> | protection level

        if (args.length == 2) {
            this.arena.msg(sender, MSG.MODULE_BETTERGEARS_SHOWCLASS, c.getName(), String.valueOf(this.getLevelMap().get(c)));
            return;
        }

        if (args.length != 4 || !"level".equalsIgnoreCase(args[2])) {
            this.printHelp(sender);
            return;
        }

        try {
            final short l = Short.parseShort(args[3]);
            this.arena.getConfig().setManually(
                    "modules.bettergears.levels." + c.getName(), l);
            this.arena.getConfig().save();
            this.getLevelMap().put(c, l);
            this.arena.msg(sender, MSG.MODULE_BETTERGEARS_CLASSDONE, c.getName(), args[3]);
        } catch (final Exception e) {
            this.arena.msg(sender, MSG.ERROR_NOT_NUMERIC, args[3]);
        }
    }

    @Override
    public void configParse(final YamlConfiguration cfg) {
        for (final ArenaClass c : this.arena.getClasses()) {
            if (cfg.get("modules.bettergears.levels." + c.getName()) == null) {
                cfg.set("modules.bettergears.levels." + c.getName(),
                        this.parseClassNameToDefaultProtection(c.getName()));
            }
        }
        for (final ArenaTeam t : this.arena.getTeams()) {
            if (cfg.get("modules.bettergears.colors." + t.getName()) == null) {
                cfg.set("modules.bettergears.colors." + t.getName(),
                        this.parseTeamColorStringToRGB(t.getColor().name()));
            }
        }
        if (this.getColorMap().isEmpty()) {
            this.setup();
        }
    }

    @Override
    public void displayInfo(final CommandSender sender) {
        for (final Map.Entry<ArenaTeam, Short[]> arenaTeamEntry : this.colorMap.entrySet()) {
            final Short[] colors = arenaTeamEntry.getValue();
            sender.sendMessage(arenaTeamEntry.getKey().getName() + ": " +
                    StringParser.joinArray(colors, ""));
        }

        for (final Map.Entry<ArenaClass, Short> arenaClassShortEntry : this.levelMap.entrySet()) {
            sender.sendMessage(arenaClassShortEntry.getKey().getName() + ": " + arenaClassShortEntry.getValue());
        }
    }

    void equip(final ArenaPlayer arenaPlayer) {

        Player player = arenaPlayer.getPlayer();
        debug(this.arena, player, "equipping better gear!");

        if (this.getColorMap().isEmpty()) {
            this.setup();
        }

        final short r;
        final short g;
        final short b;

        if (this.getArena().isFreeForAll()) {
            r = (short) new Random().nextInt(256);
            g = (short) new Random().nextInt(256);
            b = (short) new Random().nextInt(256);
        } else {
            r = this.getColorMap().get(arenaPlayer.getArenaTeam())[0];
            g = this.getColorMap().get(arenaPlayer.getArenaTeam())[1];
            b = this.getColorMap().get(arenaPlayer.getArenaTeam())[2];
        }


        final ItemStack[] isArmor = new ItemStack[4];
        isArmor[0] = new ItemStack(Material.LEATHER_HELMET, 1);
        isArmor[1] = new ItemStack(Material.LEATHER_CHESTPLATE, 1);
        isArmor[2] = new ItemStack(Material.LEATHER_LEGGINGS, 1);
        isArmor[3] = new ItemStack(Material.LEATHER_BOOTS, 1);

        final Color c = Color.fromBGR(b, g, r);
        for (int i = 0; i < 4; i++) {
            final LeatherArmorMeta lam = (LeatherArmorMeta) isArmor[i].getItemMeta();

            lam.setColor(c);
            isArmor[i].setItemMeta(lam);
        }

        Short s = this.getLevelMap().get(arenaPlayer.getArenaClass());

        if (s == null) {
            final String autoClass = this.getArena().getConfig().getString(CFG.READY_AUTOCLASS);
            final ArenaClass ac = this.getArena().getClass(autoClass);
            s = this.getLevelMap().get(ac);
        }

        if(s != null && s > 0) {
            isArmor[0].addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, s);
            isArmor[0].addUnsafeEnchantment(Enchantment.PROTECTION_EXPLOSIONS, s);
            isArmor[0].addUnsafeEnchantment(Enchantment.PROTECTION_FALL, s);
            isArmor[0].addUnsafeEnchantment(Enchantment.PROTECTION_FIRE, s);
            isArmor[0].addUnsafeEnchantment(Enchantment.PROTECTION_PROJECTILE, s);
        }

        if (this.arena.getConfig().getBoolean(CFG.MODULES_BETTERGEARS_HEAD)) {
            this.replaceArmorItem(EquipmentSlot.HEAD, player, isArmor[0]);
        }

        if (this.arena.getConfig().getBoolean(CFG.MODULES_BETTERGEARS_CHEST)) {
            this.replaceArmorItem(EquipmentSlot.CHEST, player, isArmor[1]);
        }

        if (this.arena.getConfig().getBoolean(CFG.MODULES_BETTERGEARS_LEG)) {
            this.replaceArmorItem(EquipmentSlot.LEGS, player, isArmor[2]);
        }

        if (this.arena.getConfig().getBoolean(CFG.MODULES_BETTERGEARS_FOOT)) {
            this.replaceArmorItem(EquipmentSlot.FEET, player, isArmor[3]);
        }
    }

    private Map<ArenaTeam, Short[]> getColorMap() {
        if (this.colorMap == null) {
            this.colorMap = new HashMap<>();
        }
        return this.colorMap;
    }

    private Map<ArenaClass, Short> getLevelMap() {
        if (this.levelMap == null) {
            this.levelMap = new HashMap<>();
        }
        return this.levelMap;
    }

    @Override
    public void initiate(final Player player) {
        if (this.getColorMap().isEmpty()) {
            this.setup();
        }
    }

    @Override
    public void lateJoin(final Player player) {
        this.equip(ArenaPlayer.fromPlayer(player));
    }

    @Override
    public void reset(final boolean force) {
        this.getColorMap().clear();
        this.getLevelMap().clear();
    }

    @Override
    public void parseClassChange(Player player, ArenaClass arenaClass) {
        this.equip(ArenaPlayer.fromPlayer(player));
    }

    @Override
    public void parseStart() {

        if (this.getColorMap().isEmpty()) {
            this.setup();
        }
        // debug();

        for (final ArenaPlayer ap : this.arena.getFighters()) {
            new EquipRunnable(ap, this);
        }
    }

    private Short parseClassNameToDefaultProtection(final String name) {
        if ("Tank".equals(name)) {
            return 10;
        }
        if (name.equals("Swordsman")) {
            return 4;
        }
        if (name.equals("Ranger")) {
            return 1;
        }
        if (name.equals("Pyro")) {
            return 1;
        }
        return null;
    }

    @Override
    public void parseRespawn(final Player player, final ArenaTeam team, final DamageCause cause,
                             final Entity damager) {
        final ArenaPlayer ap = ArenaPlayer.fromPlayer(player);
        if (this.arena.getConfig().getBoolean(CFG.PLAYER_REFILLINVENTORY)) {
            new EquipRunnable(ap, this);
        }
    }

    private Short[] parseRGBToShortArray(final Object o) {
        final Short[] result = new Short[3];
        result[0] = 255;
        result[1] = 255;
        result[2] = 255;

        debug("parsing RGB: {}", o);

        if (!(o instanceof String)) {
            return result;
        }

        final String s = (String) o;

        if (s != null && s.isEmpty() || !s.contains(",")
                || s.split(",").length < 3) {
            return result;
        }

        try {
            final String[] split = s.split(",");
            result[0] = Short.parseShort(split[0]);
            result[1] = Short.parseShort(split[1]);
            result[2] = Short.parseShort(split[2]);
        } catch (final Exception e) {
        }
        return result;
    }

    private String parseTeamColorStringToRGB(final String name) {
        if (defaultColors == null) {
            defaultColors = new HashMap<>();

            defaultColors.put("BLACK", "0,0,0");
            defaultColors.put("DARK_BLUE", "0,0,153");
            defaultColors.put("DARK_GREEN", "0,68,0");
            defaultColors.put("DARK_AQUA", "0,153,153");
            defaultColors.put("DARK_RED", "153,0,0");

            defaultColors.put("DARK_PURPLE", "153,0,153");
            defaultColors.put("GOLD", "0,0,0");
            defaultColors.put("GRAY", "153,153,153");
            defaultColors.put("DARK_GRAY", "68,68,68");
            defaultColors.put("BLUE", "0,0,255");
            defaultColors.put("GREEN", "0,255,0");

            defaultColors.put("AQUA", "0,255,255");
            defaultColors.put("RED", "255,0,0");
            defaultColors.put("LIGHT_PURPLE", "255,0,255");
            defaultColors.put("PINK", "255,0,255");
            defaultColors.put("YELLOW", "255,255,0");
            defaultColors.put("WHITE", "255,255,255");
        }

        final String s = defaultColors.get(name);
        debug("team {} : {}", name, s);
        return s == null ? "255,255,255" : s;
    }

    private void printHelp(final CommandSender sender) {
        this.arena.msg(sender, "/pa [arenaname] !bg [teamname]  | show team color");
        this.arena.msg(sender,
                "/pa [arenaname] !bg [teamname] color " + ChatColor.RED + "<R> " + ChatColor.GREEN + "<G> " + ChatColor.BLUE + "<B>" + ChatColor.RESET + " | set color");
        this.arena.msg(sender,
                "/pa [arenaname] !bg [classname] | show protection level");
        this.arena.msg(sender,
                "/pa [arenaname] !bg [classname] level <level> | set protection level");
    }

    private void setup() {
        debug("Setting up BetterGears");

        for (final ArenaClass c : this.arena.getClasses()) {
            Short s = 0;
            try {
                s = Short
                        .valueOf(String.valueOf(this.arena.getConfig()
                                .getUnsafe(
                                        "modules.bettergears.levels."
                                                + c.getName())));
                debug("{} : {}", c.getName(), s);
            } catch (final Exception e) {
            }
            this.getLevelMap().put(c, s);
        }

        for (final ArenaTeam t : this.arena.getTeams()) {
            final Short[] s = this.parseRGBToShortArray(this.arena.getConfig().getUnsafe(
                    "modules.bettergears.colors." + t.getName()));
            this.getColorMap().put(t, s);
            debug("{} : {}", t.getName(), StringParser.joinArray(s, ","));
        }
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
