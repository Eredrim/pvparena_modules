package net.slipcor.pvparena.modules.battlefieldguard;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.SpawnManager;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Set;

public class BattlefieldGuard extends ArenaModule {
    public static final String EXIT = "exit";
    private boolean setup;

    public BattlefieldGuard() {
        super("BattlefieldGuard");
    }

    @Override
    public String version() {
        return this.getClass().getPackage().getImplementationVersion();
    }

    @Override
    public void configParse(final YamlConfiguration config) {
        if (this.setup) {
            return;
        }
        new BattleRunnable().runTaskTimer(PVPArena.getInstance(), 20, 20);
        this.setup = true;
    }

    @Override
    public void displayInfo(final CommandSender sender) {
        sender.sendMessage(StringParser.colorVar("enterdeath", this.arena.getConfig().getBoolean(CFG.MODULES_BATTLEFIELDGUARD_ENTERDEATH)));
    }

    @Override
    public boolean hasSpawn(final String spawnName, final String teamName) {
        return EXIT.equalsIgnoreCase(spawnName);
    }

    @Override
    public Set<PASpawn> checkForMissingSpawns(final Set<PASpawn> spawns) {
        return SpawnManager.getMissingSpawns(spawns, EXIT);
    }

    @Override
    public boolean needsBattleRegion() {
        return true;
    }
}
