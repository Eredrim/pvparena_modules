package net.slipcor.pvparena.modules.betterclasses;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.compatibility.Constants;
import net.slipcor.pvparena.core.StringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static net.slipcor.pvparena.core.CollectionUtils.isNotEmpty;

public class BetterClassDef {

    static final String PERM_EFFECTS_KEY = "permEffects";
    static final String MAX_TEAM_PLAYERS_KEY = "maxTeamPlayers";
    static final String MAX_GLOBAL_PLAYERS_KEY = "maxGlobalPlayers";
    static final String NEEDED_EXP_LEVEL_KEY = "neededEXPLevel";
    static final String RESPAWN_COMMAND_KEY = "respawnCommand";

    private Set<PotionEffect> permEffects;
    private int maxTeamPlayers = -1;
    private int maxGlobalPlayers = -1;
    private int neededEXPLevel = 0;
    private String respawnCommand;

    public BetterClassDef(Set<PotionEffect> permEffects, int maxTeamPlayers, int maxGlobalPlayers, int neededEXPLevel, String respawnCommand) {
        this.permEffects = permEffects;
        this.maxTeamPlayers = maxTeamPlayers;
        this.maxGlobalPlayers = maxGlobalPlayers;
        this.neededEXPLevel = neededEXPLevel;
        this.respawnCommand = respawnCommand;
    }

    public Set<PotionEffect> getPermEffects() {
        return this.permEffects;
    }

    public void setPermEffects(Set<PotionEffect> permEffects) {
        this.permEffects = permEffects;
    }

    public int getMaxTeamPlayers() {
        return this.maxTeamPlayers;
    }

    public void setMaxTeamPlayers(int maxTeamPlayers) {
        this.maxTeamPlayers = maxTeamPlayers;
    }

    public int getMaxGlobalPlayers() {
        return this.maxGlobalPlayers;
    }

    public void setMaxGlobalPlayers(int maxGlobalPlayers) {
        this.maxGlobalPlayers = maxGlobalPlayers;
    }

    public int getNeededEXPLevel() {
        return this.neededEXPLevel;
    }

    public void setNeededEXPLevel(int neededEXPLevel) {
        this.neededEXPLevel = neededEXPLevel;
    }

    public String getRespawnCommand() {
        return this.respawnCommand;
    }

    public void setRespawnCommand(String respawnCommand) {
        this.respawnCommand = respawnCommand;
    }

    public static BetterClassDef convertFromConfig(ConfigurationSection configSection) {
        Set<PotionEffect> permEffects = new HashSet<>();
        ofNullable(configSection.getConfigurationSection(PERM_EFFECTS_KEY)).ifPresent(cs -> {
            Map<String, Object> effectMap = cs.getValues(false);
            effectMap.forEach((key, value) -> {
                PotionEffectType effectType = PotionEffectType.getByName(key);
                if(effectType != null && value instanceof Integer) {
                    PotionEffect effect = new PotionEffect(effectType, Constants.INFINITE_EFFECT_DURATION, (int) value - 1);
                    permEffects.add(effect);
                } else {
                    PVPArena.getInstance().getLogger().warning(String.format("[BetterClasses] Potion effect %s:%s has an invalid format", key, value));
                }
            });
        });

        int maxTeamPlayers = configSection.getInt(MAX_TEAM_PLAYERS_KEY, -1);
        int maxGlobalPlayers = configSection.getInt(MAX_GLOBAL_PLAYERS_KEY, -1);
        int neededEXPLevel = configSection.getInt(NEEDED_EXP_LEVEL_KEY, 0);
        String respawnCommand = configSection.getString(RESPAWN_COMMAND_KEY);
        return new BetterClassDef(permEffects, maxTeamPlayers, maxGlobalPlayers, neededEXPLevel, respawnCommand);
    }

    public Map<String, Object> convertToConfig() {
        Map<String, Object> configMap = new HashMap<>();

        if(isNotEmpty(this.permEffects)) {
            Map<String, Integer> effectMap = this.permEffects.stream()
                            .collect(toMap(pEff -> pEff.getType().getName(), potionEffect -> potionEffect.getAmplifier() + 1));
            configMap.put(PERM_EFFECTS_KEY, effectMap);
        }

        configMap.put(MAX_TEAM_PLAYERS_KEY, this.maxTeamPlayers);
        configMap.put(MAX_GLOBAL_PLAYERS_KEY, this.maxGlobalPlayers);
        configMap.put(NEEDED_EXP_LEVEL_KEY, this.neededEXPLevel);

        if(StringUtils.notBlank(this.respawnCommand)) {
            configMap.put(RESPAWN_COMMAND_KEY, this.respawnCommand);
        }

        return configMap;
    }
}
