package net.slipcor.pvparena.modules.maps;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.loadables.ArenaRegionShape;
import net.slipcor.pvparena.managers.SpawnManager;
import net.slipcor.pvparena.regions.ArenaRegion;
import net.slipcor.pvparena.regions.RegionType;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.map.MapView.Scale;
import org.bukkit.map.MinecraftFont;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static net.slipcor.pvparena.modules.maps.MapElementVisibility.*;

class ArenaMapsRenderer extends MapRenderer {
    private ArenaPlayer arenaPlayer;
    private Arena arena;
    private final MapElementVisibility showPlayers;
    private final MapElementVisibility showSpawns;
    private final MapElementVisibility showBlocks;
    private final boolean showScore;
    private final ArenaMaps mod;

    public ArenaMapsRenderer(final ArenaMaps mod) {
        super(true);
        this.arenaPlayer = null;
        this.arena = mod.getArena();
        this.mod = mod;
        this.showScore = this.arena.getConfig().getBoolean(CFG.MODULES_ARENAMAPS_SHOWSCORE);
        this.showPlayers = this.tryLoadVisibilitySetting(CFG.MODULES_ARENAMAPS_SHOWPLAYERS);
        this.showSpawns = this.tryLoadVisibilitySetting(CFG.MODULES_ARENAMAPS_SHOWSPAWNS);
        this.showBlocks = this.tryLoadVisibilitySetting(CFG.MODULES_ARENAMAPS_SHOWBLOCKS);
    }

    @Override
    public void render(@NotNull MapView mapView, @NotNull MapCanvas canvas, @NotNull Player player) {
        if (this.arenaPlayer == null) {
            // first initialisation
            Map<String, MapView> playerMaps = this.mod.getPlayerMaps();
            if (playerMaps.containsKey(player.getName()) && playerMaps.get(player.getName()) == null) {

                this.arenaPlayer = ArenaPlayer.fromPlayer(player);
                this.arena = arenaPlayer.getArena();

                playerMaps.put(player.getName(), mapView);
                this.defineMapViewScale(mapView);
            }
            return;
        }

        if (!player.getName().equals(this.arenaPlayer.getName())) {
            return;
        }

        this.setMapViewCenter(mapView, player);
        int mapCenterX = mapView.getCenterX();
        int mapCenterZ = mapView.getCenterZ();
        int scaleFactor = (int) Math.pow(2, mapView.getScale().getValue());

        // Clean previous canvas
        for (int x = 0; x < 128; x++) {
            for (int z = 0; z < 128; z++) {
                canvas.setPixel(x, z, (byte) -1);
            }
        }

        this.mod.getMapElements().forEach(mapElement -> {
            if (mapElement.getType() == MapElementType.BLOCK && mapElement.isVisible(this.showBlocks, this.arenaPlayer)) {
                // Render spawns and blocks
                renderBlock(canvas, mapCenterX, mapCenterZ, scaleFactor, mapElement);

            } else if (mapElement.getType() == MapElementType.SPAWN && mapElement.isVisible(this.showSpawns, this.arenaPlayer)) {
                // Render spawns and blocks
                renderSpawn(canvas, mapCenterX, mapCenterZ, scaleFactor, mapElement);

            } else if (mapElement.getType() == MapElementType.PLAYER && !mapElement.getName().equals(this.arenaPlayer.getName())
                    && mapElement.isVisible(this.showPlayers, this.arenaPlayer)) {
                // Render players
                renderPlayerPoint(canvas, mapCenterX, mapCenterZ, scaleFactor, mapElement);
            }
        });

        if (this.showScore && !this.arena.getGoal().isFreeForAll()) {
            this.renderScoreText(canvas);
        }
    }

    private static void renderSpawn(@NotNull MapCanvas canvas, int mapCenterX, int mapCenterZ, int scaleFactor, MapElement mapElement) {
        byte color = mapElement.getMapColor().getByteValue();

        int mapX = ((mapElement.getX() - mapCenterX) / scaleFactor) + 64;
        int mapZ = ((mapElement.getZ() - mapCenterZ) / scaleFactor) + 64;

        if ((mapX >= 1) && (mapX < 127) && (mapZ >= 1) && (mapZ < 127)) {
            canvas.setPixel(mapX, mapZ, MapColor.WHITE.getByteValue());

            canvas.setPixel(mapX - 1, mapZ - 1, color);
            canvas.setPixel(mapX, mapZ - 1, color);
            canvas.setPixel(mapX + 1, mapZ - 1, color);
            canvas.setPixel(mapX - 1, mapZ, color);
            canvas.setPixel(mapX + 1, mapZ, color);
            canvas.setPixel(mapX - 1, mapZ + 1, color);
            canvas.setPixel(mapX, mapZ + 1, color);
            canvas.setPixel(mapX + 1, mapZ + 1, color);
        }
    }

    private static void renderBlock(@NotNull MapCanvas canvas, int mapCenterX, int mapCenterZ, int scaleFactor, MapElement mapElement) {
        byte color = mapElement.getMapColor().getAltByteValue();
        byte outline = mapElement.getMapColor().getByteValue();

        int mapX = ((mapElement.getX() - mapCenterX) / scaleFactor) + 64;
        int mapZ = ((mapElement.getZ() - mapCenterZ) / scaleFactor) + 64;

        if ((mapX >= 1) && (mapX < 127) && (mapZ >= 1) && (mapZ < 127)) {
            // Point
            canvas.setPixel(mapX, mapZ, color);
            canvas.setPixel(mapX-1, mapZ-1, color);
            canvas.setPixel(mapX+1, mapZ-1, color);
            canvas.setPixel(mapX-1, mapZ+1, color);
            canvas.setPixel(mapX+1, mapZ+1, color);

            // Outline
            canvas.setPixel(mapX-2, mapZ-2, outline);
            canvas.setPixel(mapX-1, mapZ-2, outline);
            canvas.setPixel(mapX+1, mapZ-2, outline);
            canvas.setPixel(mapX+2, mapZ-2, outline);
            canvas.setPixel(mapX-2, mapZ-1, outline);
            canvas.setPixel(mapX, mapZ-1, outline);
            canvas.setPixel(mapX+2, mapZ-1, outline);
            canvas.setPixel(mapX-1, mapZ, outline);
            canvas.setPixel(mapX+1, mapZ, outline);
            canvas.setPixel(mapX-2, mapZ+1, outline);
            canvas.setPixel(mapX, mapZ+1, outline);
            canvas.setPixel(mapX+2, mapZ+1, outline);
            canvas.setPixel(mapX-2, mapZ+2, outline);
            canvas.setPixel(mapX-1, mapZ+2, outline);
            canvas.setPixel(mapX+1, mapZ+2, outline);
            canvas.setPixel(mapX+2, mapZ+2, outline);
        }
    }

    private static void renderPlayerPoint(@NotNull MapCanvas canvas, int mapCenterX, int mapCenterZ, int scaleFactor, MapElement mapElement) {
        byte color = mapElement.getMapColor().getByteValue();
        byte altColor = mapElement.getMapColor().getAltByteValue();
        byte outline = MapColor.BLACK.getByteValue();

        int mapX = ((mapElement.getX() - mapCenterX) / scaleFactor) + 64;
        int mapZ = ((mapElement.getZ() - mapCenterZ) / scaleFactor) + 64;

        if ((mapX >= 1) && (mapX < 127) && (mapZ >= 1) && (mapZ < 127)) {
            // Point
            canvas.setPixel(mapX, mapZ, altColor);
            canvas.setPixel(mapX+1, mapZ, color);
            canvas.setPixel(mapX, mapZ+1, color);
            canvas.setPixel(mapX+1, mapZ+1, color);

            // Outline
            canvas.setPixel(mapX, mapZ-1, outline);
            canvas.setPixel(mapX+1, mapZ-1, outline);
            canvas.setPixel(mapX-1, mapZ, outline);
            canvas.setPixel(mapX+2, mapZ, outline);
            canvas.setPixel(mapX-1, mapZ+1, outline);
            canvas.setPixel(mapX+2, mapZ+1, outline);
            canvas.setPixel(mapX, mapZ+2, outline);
            canvas.setPixel(mapX+1, mapZ+2, outline);
        }
    }

    private void renderScoreText(@NotNull MapCanvas canvas) {
        StringBuilder strBuilder = new StringBuilder();
        this.arena.getGoal().getTeamLifeMap().forEach((team, lives) -> {
            if (strBuilder.length() > 0) {
                strBuilder.append(" | ");
            }
            strBuilder.append(String.format("%s: %s",team.getName(), lives));
        });
        try {
            canvas.drawText(2, 2, MinecraftFont.Font, strBuilder.toString());
        } catch (final Exception e) {
            canvas.drawText(2, 2, MinecraftFont.Font, "invalid team name");
        }
    }

    private void setMapViewCenter(@NotNull MapView mapView, @NotNull Player player) {
        if (this.arena.getConfig().getBoolean(CFG.MODULES_ARENAMAPS_ALIGNTOPLAYER)) {
            mapView.setCenterX(player.getLocation().getBlockX());
            mapView.setCenterZ(player.getLocation().getBlockZ());
        } else {
            Set<ArenaRegion> arenaRegions = this.arena.getRegionsByType(RegionType.BATTLE);

            if(arenaRegions.size() == 1) {
                ArenaRegion battleRegion = arenaRegions.iterator().next();
                PABlockLocation center = battleRegion.getShape().getCenter();
                mapView.setCenterX(center.getX());
                mapView.setCenterZ(center.getZ());
            } else if (arenaRegions.size() > 1) {
                PABlockLocation center = PABlockLocation.getMidpoint(arenaRegions.stream()
                        .map(r -> r.getShape().getCenter())
                        .collect(Collectors.toList())
                );
                mapView.setCenterX(center.getX());
                mapView.setCenterZ(center.getZ());
            } else {
                PABlockLocation spawnCenter = SpawnManager.getRegionCenter(this.arena);
                mapView.setCenterX(spawnCenter.getX());
                mapView.setCenterZ(spawnCenter.getZ());
            }
        }
    }

    private void defineMapViewScale(@NotNull MapView mapView) {
        Scale bestScale = Scale.CLOSEST;
        Set<ArenaRegion> arenaRegions = this.arena.getRegionsByType(RegionType.BATTLE);

        if(arenaRegions.size() == 1) {
            ArenaRegionShape shape = arenaRegions.iterator().next().getShape();
            PABlockLocation min = shape.getMinimumLocation();
            PABlockLocation max = shape.getMaximumLocation();
            int maxXDistance = Math.abs(min.getX() - max.getX());
            int maxZDistance = Math.abs(min.getZ() - max.getZ());
            bestScale = getBestScale(Math.max(maxXDistance, maxZDistance));
        } else if (arenaRegions.size() > 1) {
            Integer minX = null;
            Integer maxX = null;
            Integer minZ = null;
            Integer maxZ = null;
            for(ArenaRegion battleRegion : arenaRegions) {
                ArenaRegionShape shape = battleRegion.getShape();
                PABlockLocation min = shape.getMinimumLocation();
                PABlockLocation max = shape.getMaximumLocation();
                minX = (minX == null) ? min.getX() : Math.min(min.getX(), minX);
                maxX = (maxX == null) ? max.getX() : Math.max(max.getX(), maxX);
                minZ = (minZ == null) ? min.getZ() : Math.min(min.getZ(), minZ);
                maxZ = (maxZ == null) ? max.getZ() : Math.max(max.getZ(), maxZ);
            }
            int maxXDistance = Math.abs(minX - maxX);
            int maxZDistance = Math.abs(minZ - maxZ);
            bestScale = getBestScale(Math.max(maxXDistance, maxZDistance));
        }
        mapView.setScale(bestScale);
    }

    private static MapView.Scale getBestScale(int distance) {
        if (distance <= 128) {
            return Scale.CLOSEST;
        } else if (distance <= 256) {
            return Scale.CLOSE;
        } else if (distance <= 512) {
            return Scale.NORMAL;
        } else if (distance <= 1024) {
            return Scale.FAR;
        }
        return Scale.FARTHEST;
    }

    private MapElementVisibility tryLoadVisibilitySetting(CFG cfgEntry) {
        try {
            return valueOf(this.arena.getConfig().getString(cfgEntry).toUpperCase());
        } catch (IllegalArgumentException e) {
            PVPArena.getInstance().getLogger().warning("[%s] ArenaMaps setting \"%s\" has an incorrect value. It will be ignored for this game.");
            return NONE;
        }
    }
}