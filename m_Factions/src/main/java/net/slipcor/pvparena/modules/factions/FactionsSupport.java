package net.slipcor.pvparena.modules.factions;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.loadables.ArenaModule;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

public class FactionsSupport extends ArenaModule {

    public FactionsSupport() {
        super("Factions");
    }

    private boolean setup;

    @Override
    public String version() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public void configParse(final YamlConfiguration config) {
        if (setup) {
            return;
        }
        Bukkit.getPluginManager().registerEvents(new FactionsListener(this), PVPArena.getInstance());
        setup = true;
    }
}
