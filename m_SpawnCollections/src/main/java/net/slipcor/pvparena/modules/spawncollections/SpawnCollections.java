package net.slipcor.pvparena.modules.spawncollections;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.commands.AbstractArenaCommand;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.core.CollectionUtils;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.PermissionManager;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static net.slipcor.pvparena.core.Config.parseToString;

public class SpawnCollections extends ArenaModule {
    private static final String CFG_KEY = "modules.spawnCollections";

    public SpawnCollections() {
        super("SpawnCollections");
    }

    @Override
    public String version() {
        return this.getClass().getPackage().getImplementationVersion();
    }

    @Override
    public boolean checkCommand(final String s) {
        return this.arena.getEveryone().isEmpty() && ("!sc".equals(s) || "spawncollections".equalsIgnoreCase(s));
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("spawncollections");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!sc");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<>(null);
        result.define(new String[]{"list"});
        result.define(new String[]{"save"});
        result.define(new String[]{"switch"});
        result.define(new String[]{"remove"});
        return result;
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        // !sc list | list saved spawn collections
        // !sc save [name] | save a spawn collection
        // !sc switch [name] | switch current spawn config to an existing spawn collection
        // !sc remove [name] | delete an existing spawn collection
        if (!PermissionManager.hasAdminPerm(sender) && !PermissionManager.hasBuilderPerm(sender, this.arena)) {
            this.arena.msg(sender, MSG.ERROR_NOPERM, Language.parse(MSG.ERROR_NOPERM_X_ADMIN));
            return;
        }

        if (!AbstractArenaCommand.argCountValid(sender, this.arena, args, new Integer[]{2, 3})) {
            this.arena.msg(sender, MSG.ERROR_INVALID_ARGUMENT_COUNT, String.valueOf(args.length), "[1, 2]");
            return;
        }

        if (args.length == 2 && "list".equalsIgnoreCase(args[1])) {
            // !sc list
            this.listSpawnSet(sender);
        } else {
            if ("save".equalsIgnoreCase(args[1])) {
                // !sc save [name]
                this.saveSpawnCollection(sender, args[2]);
            } else if ("switch".equalsIgnoreCase(args[1])) {
                // !sc switch [name]
                this.switchToSpawnCollection(sender, args[2]);
            } else if ("remove".equalsIgnoreCase(args[1])) {
                // !sc remove [name]
                this.removeSpawnCollection(sender, args[2]);
            } else {
                this.arena.msg(sender, MSG.ERROR_ARGUMENT, args[1], String.join(", ", this.getSubs(this.arena).getContent()));
            }
        }
    }

    private void saveSpawnCollection(CommandSender sender, String spawnSetName) {
        this.arena.getSpawns().forEach(paSpawn -> {
            String spawnSetCfgKey = String.format("%s.%s.%s", CFG_KEY, spawnSetName, paSpawn.getFullName());
            this.arena.getConfig().setManually(spawnSetCfgKey, parseToString(paSpawn.getPALocation()));
        });

        this.arena.getConfig().save();
        this.arena.msg(sender, MSG.MODULE_SPAWNCOLLECTIONS_SAVED, spawnSetName);
    }

    private void switchToSpawnCollection(CommandSender sender, String spawnSetName) {
        Config cfg = this.arena.getConfig();
        String spawnSetCfgKey = String.format("%s.%s", CFG_KEY, spawnSetName);
        Set<String> spawnKeys = cfg.getKeys(spawnSetCfgKey);

        if(CollectionUtils.isNotEmpty(spawnKeys)) {
            spawnKeys.forEach(spawnNode -> {
                String location = (String) cfg.getUnsafe(String.format("%s.%s", spawnSetCfgKey, spawnNode));
                Optional.ofNullable(PASpawn.deserialize(spawnNode, location, this.arena)).ifPresent(this.arena::setSpawn);
            });
            cfg.save();
            this.arena.msg(sender, MSG.MODULE_SPAWNCOLLECTIONS_USE, spawnSetName);
        } else {
            this.arena.msg(sender, MSG.MODULE_SPAWNCOLLECTIONS_NOTEXIST, spawnSetName);
        }
    }

    private void removeSpawnCollection(CommandSender sender, String spawnSetName) {
        Config cfg = this.arena.getConfig();
        String spawnSetCfgKey = String.format("%s.%s", CFG_KEY, spawnSetName);
        Set<String> spawnKeys = cfg.getKeys(spawnSetCfgKey);

        if(CollectionUtils.isNotEmpty(spawnKeys)) {
            if(cfg.getKeys(CFG_KEY).size() == 1) {
                cfg.setManually(CFG_KEY, null);
            } else {
                cfg.getConfigurationSection(CFG_KEY).set(spawnSetName, null);
            }
            cfg.save();
            this.arena.msg(sender, MSG.MODULE_SPAWNCOLLECTIONS_REMOVED, spawnSetName);
        } else {
            this.arena.msg(sender, MSG.MODULE_SPAWNCOLLECTIONS_NOTEXIST, spawnSetName);
        }
    }

    private void listSpawnSet(CommandSender sender) {
        Config cfg = this.arena.getConfig();
        Set<String> spawnSetKeys = cfg.getKeys(CFG_KEY);

        if(CollectionUtils.isNotEmpty(spawnSetKeys)) {
            this.arena.msg(sender, MSG.MODULE_SPAWNCOLLECTIONS_LIST);
        } else {
            this.arena.msg(sender, MSG.MODULE_SPAWNCOLLECTIONS_EMPTY);
        }
    }
}
