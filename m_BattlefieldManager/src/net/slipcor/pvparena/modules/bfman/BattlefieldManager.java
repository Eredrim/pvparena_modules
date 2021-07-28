package net.slipcor.pvparena.modules.bfman;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.classes.PABlock;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.classes.PALocation;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.commands.AbstractArenaCommand;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Debug;
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
        debug = new Debug(691);
    }

    @Override
    public String version() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public boolean checkCommand(final String s) {
        return arena.getEveryone().size() < 1 && ("!bm".equals(s) || s.startsWith("battlefieldm"));
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
        if (!PermissionManager.hasAdminPerm(sender)
                && !PermissionManager.hasBuilderPerm(sender, arena)) {
            arena.msg(
                    sender,
                    Language.parse(MSG.ERROR_NOPERM,
                            Language.parse(MSG.ERROR_NOPERM_X_ADMIN)));
            return;
        }

        if (!AbstractArenaCommand.argCountValid(sender, arena, args, new Integer[]{0, 1, 2})) {
            return;
        }

        if (args == null || args.length < 1) {
            // !bm -> show status!

            if (loaded == null) {
                arena.msg(sender, "No battle definition loaded!");
            } else {
                arena.msg(sender, "Loaded definition: " + ChatColor.GREEN
                        + loaded);
            }

            if (changed) {
                arena.msg(sender, ChatColor.RED + "There unsaved changes!");
            } else {
                arena.msg(sender, "No unsaved changes!");
            }

            return;
        }

        if (args.length == 1) {
            if ("clear".equals(args[0])) {
                // !bm update | update loaded definition with corrections/additions

                if (loaded == null) {
                    arena.msg(sender, Language.parse(MSG.ERROR_ERROR, "No definition loaded!"));

                    return;
                }

                arena.getSpawns().clear();
                arena.getBlocks().clear();
                arena.getRegions().clear();

                changed = false;
                loaded = null;
                return;
            }
            if (args[0].equals("update")) {
                // !bm update | update loaded definition with corrections/additions

                if (loaded == null) {
                    arena.msg(sender, Language.parse(MSG.ERROR_ERROR, "No definition loaded!"));

                    return;
                }
                /*
				if (!changed) {
					arena.msg(sender, Language.parse(MSG.ERROR_ERROR, "No definition loaded!"));

					return;
				}
				*/
                for (PASpawn spawn : arena.getSpawns()) {
                    arena.getArenaConfig().setManually(
                            "spawns." + encrypt(spawn.getName(), loaded),
                            Config.parseToString(spawn.getLocation()));
                }

                for (PABlock block : arena.getBlocks()) {
                    arena.getArenaConfig().setManually(
                            "spawns." + encrypt(block.getName(), loaded),
                            Config.parseToString(block.getLocation()));
                }

                changed = false;
                return;
            }
            // !bm [name] | load definition [name]
            Set<String> keys = arena.getArenaConfig().getKeys("spawns");

            if (keys == null) {
                return;
            }

            arena.getSpawns().clear();
            arena.getBlocks().clear();

            for (final String key : keys) {
                if (key.startsWith(loaded + "->")) {
                    final String value = (String) arena.getArenaConfig().getUnsafe("spawns." + key);
                    try {
                        final PABlockLocation loc = Config.parseBlockLocation(value);

                        final String[] split = ((String) arena.getArenaConfig().getUnsafe("spawns." + key)).split(">");
                        final String newKey = StringParser.joinArray(StringParser.shiftArrayBy(split, 1), "");
                        arena.addBlock(new PABlock(loc, newKey));
                    } catch (final IllegalArgumentException e) {
                        final PALocation loc = Config.parseLocation(value);

                        final String[] split = ((String) arena.getArenaConfig().getUnsafe("spawns." + key)).split(">");
                        final String newKey = StringParser.joinArray(StringParser.shiftArrayBy(split, 1), "");
                        arena.addSpawn(new PASpawn(loc, newKey));
                    }
                }
            }

            keys = arena.getArenaConfig().getKeys("arenaregion");

            if (keys == null) {
                return;
            }

            arena.getRegions().clear();

            for (final String key : keys) {
                if (key.startsWith(loaded + "->")) {
                    arena.addRegion(Config.parseRegion(arena, arena.getArenaConfig().getYamlConfiguration(), key));
                }
            }

            return;
        }

        // !bm save [name] | save to definition [name]

        for (final PASpawn spawn : arena.getSpawns()) {
            arena.getArenaConfig().setManually(
                    "spawns." + encrypt(spawn.getName(), args[1]),
                    Config.parseToString(spawn.getLocation()));
        }

        for (final PABlock block : arena.getBlocks()) {
            arena.getArenaConfig().setManually(
                    "spawns." + encrypt(block.getName(), args[1]),
                    Config.parseToString(block.getLocation()));
        }

        changed = false;
        loaded = args[1];
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
