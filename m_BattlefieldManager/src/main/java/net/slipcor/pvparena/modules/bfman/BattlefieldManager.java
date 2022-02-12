package net.slipcor.pvparena.modules.bfman;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.classes.PABlock;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.classes.PALocation;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.commands.AbstractArenaCommand;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.PermissionManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class BattlefieldManager extends ArenaModule {
    private String loaded;
    private boolean changed;

    public BattlefieldManager() {
        super("BattlefieldManager");
    }

    @Override
    public String version() {
        return this.getClass().getPackage().getImplementationVersion();
    }

    @Override
    public boolean checkCommand(final String s) {
        return this.arena.getEveryone().isEmpty() && ("!bm".equals(s) || s.startsWith("battlefieldm"));
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("battlefieldmanager");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!bm");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        result.define(new String[]{"clear"});
        result.define(new String[]{"update"});
        result.define(new String[]{"save"});
        return result;
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        // !bm | show the currently loaded battle definitions
        // !bm [name] | load definition [name]
        // !bm clear | start defining a new definition
        // !bm update | update loaded definition with corrections/additions
        // !bm save [name] | save to definition [name]
        if (!PermissionManager.hasAdminPerm(sender) && !PermissionManager.hasBuilderPerm(sender, this.arena)) {
            this.arena.msg(sender, MSG.ERROR_NOPERM, Language.parse(MSG.ERROR_NOPERM_X_ADMIN));
            return;
        }

        if (!AbstractArenaCommand.argCountValid(sender, this.arena, args, new Integer[]{0, 1, 2})) {
            return;
        }

        if (args.length < 1) {
            // !bm -> show status!

            if (this.loaded == null) {
                this.arena.msg(sender, "No battle definition loaded!");
            } else {
                this.arena.msg(sender, "Loaded definition: " + ChatColor.GREEN
                        + this.loaded);
            }

            if (this.changed) {
                this.arena.msg(sender, ChatColor.RED + "There unsaved changes!");
            } else {
                this.arena.msg(sender, "No unsaved changes!");
            }

            return;
        }

        if (args.length == 1) {
            if ("clear".equals(args[0])) {
                // !bm update | update loaded definition with corrections/additions

                if (this.loaded == null) {
                    this.arena.msg(sender, MSG.ERROR_ERROR, "No definition loaded!");
                    return;
                }

                this.arena.getSpawns().clear();
                this.arena.getBlocks().clear();
                this.arena.getRegions().clear();

                this.changed = false;
                this.loaded = null;
                return;

            } else if ("update".equals(args[0])) {
                // !bm update | update loaded definition with corrections/additions

                if (this.loaded == null) {
                    this.arena.msg(sender, MSG.ERROR_ERROR, "No definition loaded!");
                    return;
                }

                for (PASpawn spawn : this.arena.getSpawns()) {
                    this.arena.getConfig().setManually(
                            "spawns." + this.encrypt(spawn.getName(), this.loaded),
                            Config.parseToString(spawn.getPALocation()));
                }

                for (PABlock block : this.arena.getBlocks()) {
                    this.arena.getConfig().setManually(
                            "spawns." + this.encrypt(block.getName(), this.loaded),
                            Config.parseToString(block.getLocation()));
                }

                this.changed = false;
                return;
            }
            // !bm [name] | load definition [name]
            Set<String> keys = this.arena.getConfig().getKeys("spawns");

            if (keys == null) {
                return;
            }

            this.arena.getSpawns().clear();
            this.arena.getBlocks().clear();

            for (final String key : keys) {
                if (key.startsWith(this.loaded + "->")) {
                    final String value = (String) this.arena.getConfig().getUnsafe("spawns." + key);
                    try {
                        final PABlockLocation loc = Config.parseBlockLocation(value);

                        final String[] split = ((String) this.arena.getConfig().getUnsafe("spawns." + key)).split(">");
                        final String newKey = StringParser.joinArray(StringParser.shiftArrayBy(split, 1), "");
                        this.arena.addBlock(new PABlock(loc, newKey, null));
                    } catch (final IllegalArgumentException e) {
                        final PALocation loc = Config.parseLocation(value);

                        final String[] split = ((String) this.arena.getConfig().getUnsafe("spawns." + key)).split(">");
                        final String newKey = StringParser.joinArray(StringParser.shiftArrayBy(split, 1), "");
                        this.arena.setSpawn(new PASpawn(loc, newKey, null, null));
                    }
                }
            }

            keys = this.arena.getConfig().getKeys("arenaregion");

            if (keys == null) {
                return;
            }

            this.arena.getRegions().clear();

            for (final String key : keys) {
                if (key.startsWith(this.loaded + "->")) {
                    this.arena.addRegion(Config.parseRegion(this.arena, this.arena.getConfig().getYamlConfiguration(), key));
                }
            }

            return;
        }

        // !bm save [name] | save to definition [name]

        for (final PASpawn spawn : this.arena.getSpawns()) {
            this.arena.getConfig().setManually(
                    "spawns." + this.encrypt(spawn.getName(), args[1]),
                    Config.parseToString(spawn.getPALocation()));
        }

        for (final PABlock block : this.arena.getBlocks()) {
            this.arena.getConfig().setManually(
                    "spawns." + this.encrypt(block.getName(), args[1]),
                    Config.parseToString(block.getLocation()));
        }

        this.changed = false;
        this.loaded = args[1];
    }

    private String encrypt(final String name, final String definition) {
        final StringBuilder buff = new StringBuilder(name);
        buff.append('-');

        for (final char c : definition.toCharArray()) {
            buff.append('>');
            buff.append(c);
        }

        return buff.toString();
    }

    @Override
    public boolean needsBattleRegion() {
        return true;
    }
}
