package net.slipcor.pvparena.modules.skins;

import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MiscDisguise;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import me.libraryaddict.disguise.disguisetypes.PlayerDisguise;
import net.slipcor.pvparena.PVPArena;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class LibsDisguiseHandler {
    public void parseRespawn(Player player) {
        if (DisguiseAPI.isDisguised(player)) {
            DisguiseAPI.undisguiseToAll(player);
        }
    }

    public void parseTeleport(Player player, String disguise) {
        try {
            DisguiseType type =
                    DisguiseType.getType(EntityType.fromName(disguise));
            try {
                MobDisguise md = new MobDisguise(type, false);
            } catch (Exception e) {
                MiscDisguise md = new MiscDisguise(type);
            }
        } catch (Exception|Error e) {
            try {
                PlayerDisguise pd = new PlayerDisguise(disguise);
                if (DisguiseAPI.isDisguised(player)) {
                    DisguiseAPI.undisguiseToAll(player);
                }

                Bukkit.getScheduler().scheduleSyncDelayedTask(PVPArena.getInstance(), new LibsDisguiseRunnable(player, pd), 3L);
            } catch (Exception|Error e2) {
                e2.printStackTrace();
            }
        }
    }

    public void unload(Player player) {
        DisguiseAPI.undisguiseToAll(player);
    }
}
