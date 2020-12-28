package net.slipcor.pvparena.modules.projectiles;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.loadables.ArenaModule;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;

import static net.slipcor.pvparena.core.Config.CFG.*;

public class Projectiles extends ArenaModule implements Listener {

    public Projectiles() {
        super("Projectiles");
    }

    @Override
    public String version() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public void configParse(final YamlConfiguration config) {
        Bukkit.getPluginManager().registerEvents(this, PVPArena.getInstance());
    }

    @Override
    public void displayInfo(final CommandSender player) {
        Config cfg = this.arena.getArenaConfig();
        String message = String.format("%s | %s | %s | %s",
                StringParser.colorVar("snowball", cfg.getBoolean(MODULES_PROJECTILES_SNOWBALL)),
                StringParser.colorVar("egg", arena.getArenaConfig().getBoolean(CFG.MODULES_PROJECTILES_EGG)),
                StringParser.colorVar("fishHook", arena.getArenaConfig().getBoolean(CFG.MODULES_PROJECTILES_FISHHOOK)),
                StringParser.colorVar("enderPearl", arena.getArenaConfig().getBoolean(CFG.MODULES_PROJECTILES_ENDERPEARL))
        );
        player.sendMessage(message);
    }

    @Override
    public void onProjectileHit(final Player attacker, final Player defender, final ProjectileHitEvent event) {
        Config cfg = this.arena.getArenaConfig();

        Projectile projectile = event.getEntity();
        if ((projectile instanceof Snowball && cfg.getBoolean(MODULES_PROJECTILES_SNOWBALL)) ||
                (event.getEntity() instanceof Egg && cfg.getBoolean(MODULES_PROJECTILES_EGG)) ||
                (event.getEntity() instanceof FishHook && cfg.getBoolean(MODULES_PROJECTILES_FISHHOOK)) ||
                (event.getEntity() instanceof EnderPearl && cfg.getBoolean(MODULES_PROJECTILES_ENDERPEARL)))
        {
            knockbackDefender(defender, projectile);
        }
    }

    private static void knockbackDefender(Player defender, Projectile projectile) {
        defender.damage(0.05, projectile);
        defender.setVelocity(projectile.getVelocity().multiply(0.3));
    }
}
