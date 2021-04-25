package net.slipcor.pvparena.modules.skins;

import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.*;
import net.slipcor.pvparena.PVPArena;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class LibsDisguiseHandler {
    public void parseRespawn(Player player) {
        if (DisguiseAPI.isDisguised(player)) {
            DisguiseAPI.undisguiseToAll(player);
        }
    }

    public void parseTeleport(Player player, String disguise) {

        final EntityType entityType = EntityType.fromName(disguise);
        if (entityType == null) {
            PVPArena.getInstance().getLogger().warning(String.format("Skins: Entity type %s doesn't exist !", disguise));
            return;
        }
        DisguiseType disguiseType = DisguiseType.getType(entityType);

        TargetedDisguise targetedDisguise;
        if (disguiseType.isPlayer()) {
            targetedDisguise = new PlayerDisguise(disguise);
        } else if (disguiseType.isMob()) {
            targetedDisguise = new MobDisguise(disguiseType, false);
        } else if (disguiseType.isMisc()) {
            targetedDisguise = new MiscDisguise(disguiseType);
        } else {
            PVPArena.getInstance().getLogger().warning(String.format("Skins: Entity type %s is not supported.", disguise));
            return;
        }

        if (DisguiseAPI.isDisguised(player)) {
            DisguiseAPI.undisguiseToAll(player);
        }
        new LibsDisguiseRunnable(player, targetedDisguise).runTaskLater(PVPArena.getInstance(), 3L);

    }

    public void unload(Player player) {
        DisguiseAPI.undisguiseToAll(player);
    }
}
