package net.slipcor.pvparena.modules.maps;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.classes.PACheck;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.managers.SpawnManager;
import org.bukkit.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.map.*;
import org.bukkit.map.MapView.Scale;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class MyRenderer extends MapRenderer {
    private static final HashMap<ChatColor, Byte> colors = new HashMap<>();
    private String playerName;
    private Arena arena;
    private static final YamlConfiguration playerMaps;
    private boolean showPlayers;
    private boolean showSpawns;
    private boolean showLives;
    private final Maps maps;

    public MyRenderer(final Maps m) {
        playerName = null;
        arena = null;
        maps = m;
    }

    static {
        colors.put(ChatColor.AQUA, MapPalette.matchColor(0, 255, 255));
        colors.put(ChatColor.BLACK, MapPalette.matchColor(0, 0, 0));
        colors.put(ChatColor.BLUE, MapPalette.matchColor(0, 0, 255));
        colors.put(ChatColor.DARK_AQUA, MapPalette.matchColor(0, 128, 128));
        colors.put(ChatColor.DARK_BLUE, MapPalette.matchColor(0, 0, 128));
        colors.put(ChatColor.DARK_GRAY, MapPalette.matchColor(128, 128, 128));
        colors.put(ChatColor.DARK_GREEN, MapPalette.matchColor(0, 128, 0));
        colors.put(ChatColor.DARK_PURPLE, MapPalette.matchColor(128, 0, 128));
        colors.put(ChatColor.DARK_RED, MapPalette.matchColor(128, 0, 0));
        colors.put(ChatColor.GOLD, MapPalette.matchColor(128, 128, 0));
        colors.put(ChatColor.GRAY, MapPalette.matchColor(192, 192, 192));
        colors.put(ChatColor.GREEN, MapPalette.matchColor(0, 255, 0));
        colors.put(ChatColor.LIGHT_PURPLE, MapPalette.matchColor(255, 0, 255));
        colors.put(ChatColor.RED, MapPalette.matchColor(255, 0, 0));
        colors.put(ChatColor.WHITE, MapPalette.matchColor(255, 255, 255));
        colors.put(ChatColor.YELLOW, MapPalette.matchColor(255, 255, 0));

        PVPArena.getInstance().getDataFolder().mkdir();


        final File configFile = new File(PVPArena.getInstance().getDataFolder(), "maps.yml");
        if (!(configFile.exists())) {
            try {
                configFile.createNewFile();
            } catch (final Exception e) {
                PVPArena.getInstance().getLogger().severe(
                        "Error when creating map file.");
            }
        }

        playerMaps = new YamlConfiguration();
        try {
            playerMaps.load(configFile);
        } catch (final InvalidConfigurationException | IOException e1) {
            e1.printStackTrace();
        }
    }

    private static void savePlayers() {
        try {
            playerMaps.save(new File(PVPArena.getInstance().getDataFolder(), "maps.yml"));
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    private static final Set<Integer> done = new HashSet<>();

    @Override
    public void render(final MapView map, final MapCanvas canvas, final Player player) {
        if (playerName == null) {
            // map.removeRenderer(this);
            // eventual first initialisation
            if (maps.hasCustomMap(player.getName()) && !done.contains(map.getId())) {

                playerName = player.getName();
                arena = ArenaPlayer.parsePlayer(playerName).getArena();
                if ((playerMaps.get(player.getName()) == null)) {
                    playerMaps.set(player.getName(), map.getId());
                    savePlayers();
                } else if (playerMaps.getInt(playerName) != map.getId()) {
                    // wrong id, return
                    return;
                }

                map.setScale(Scale.CLOSE);

                done.add(map.getId());

                showSpawns = arena.getArenaConfig().getBoolean(CFG.MODULES_ARENAMAPS_SHOWSPAWNS);
                showPlayers = arena.getArenaConfig().getBoolean(CFG.MODULES_ARENAMAPS_SHOWPLAYERS);
                showLives = arena.getArenaConfig().getBoolean(CFG.MODULES_ARENAMAPS_SHOWLIVES);
            }
            return;
        }

        if (!player.getName().equals(playerName)) {
            return;
        }

        if (arena != null && arena.getArenaConfig().getBoolean(CFG.MODULES_ARENAMAPS_ALIGNTOPLAYER)) {

            map.setCenterX(player.getLocation().getBlockX());
            map.setCenterZ(player.getLocation().getBlockZ());
        } else if (arena != null) {
            final PABlockLocation loc = SpawnManager.getRegionCenter(arena);
            map.setCenterX(loc.getX());
            map.setCenterZ(loc.getZ());
        } else {
            PVPArena.getInstance().getLogger().severe("arena null");
        }
        final int mapcenterx = map.getCenterX();
        final int mapcenterz = map.getCenterZ();

        for (int x = 0; x < 128; x++) {
            for (int z = 0; z < 128; z++) {
                canvas.setPixel(x, z, (byte) -1);
            }
        }

        final Iterable<MapItem> items = maps.getItems();

        if (showSpawns) {

            for (final MapItem item : items) {
                if (item.isPlayer()) {
                    continue;
                }

                // JUST SPAWNS

                final byte color = colors.get(item.getColor());

                final byte outline = color;

                final int mapX = ((item.getX() - mapcenterx) / 2) + 64;
                final int mapZ = ((item.getZ() - mapcenterz) / 2) + 64;

                if ((mapX >= 1) && (mapX < 127) && (mapZ >= 1) && (mapZ < 127)) {
                    canvas.setPixel(mapX, mapZ, color);

                    canvas.setPixel(mapX - 1, mapZ - 1, outline);
                    canvas.setPixel(mapX, mapZ - 1, outline);
                    canvas.setPixel(mapX + 1, mapZ - 1, outline);
                    canvas.setPixel(mapX - 1, mapZ, outline);
                    canvas.setPixel(mapX + 1, mapZ, outline);
                    canvas.setPixel(mapX - 1, mapZ + 1, outline);
                    canvas.setPixel(mapX, mapZ + 1, outline);
                    canvas.setPixel(mapX + 1, mapZ + 1, outline);
                }
            }
        }

        if (showPlayers) {
            for (final MapItem item : items) {
                if (!item.isPlayer()) {
                    continue;
                }

                // JUST PLAYERS

                final byte color = colors.get(item.getColor());

                final byte outline = MapPalette.matchColor(0, 0, 0);

                final int mapX = ((item.getX() - mapcenterx) / 2) + 64;
                final int mapZ = ((item.getZ() - mapcenterz) / 2) + 64;

                if ((mapX >= 1) && (mapX < 127) && (mapZ >= 1) && (mapZ < 127)) {
                    canvas.setPixel(mapX, mapZ, color);
                    if (item.getName().equals(playerName)) {
                        continue;
                    }

                    canvas.setPixel(mapX - 1, mapZ - 1, outline);
                    canvas.setPixel(mapX, mapZ - 1, outline);
                    canvas.setPixel(mapX + 1, mapZ - 1, outline);
                    canvas.setPixel(mapX - 1, mapZ, outline);
                    canvas.setPixel(mapX + 1, mapZ, outline);
                    canvas.setPixel(mapX - 1, mapZ + 1, outline);
                    canvas.setPixel(mapX, mapZ + 1, outline);
                    canvas.setPixel(mapX + 1, mapZ + 1, outline);
                }
            }
        }

        if (!showLives) {
            return;
        }

        final Map<String, Integer> lives = new HashMap<>();

        for (final ArenaTeam team : arena.getTeams()) {
            if ("free".equals(team.getName())) {
                continue;
            }
            for (final ArenaPlayer ap : team.getTeamMembers()) {
                lives.put(team.getName(), PACheck.handleGetLives(ap.getArena(), ap));
                break;
            }
        }

        String string = "";

        for (final Map.Entry<String, Integer> stringIntegerEntry : lives.entrySet()) {
            if (string != null && !string.isEmpty()) {
                string += " | ";
            }
            string += calculate(stringIntegerEntry.getKey(), stringIntegerEntry.getValue());
        }
        try {
            canvas.drawText(0, 10, MinecraftFont.Font, string);
        } catch (final Exception e) {
            canvas.drawText(0, 10, MinecraftFont.Font, "invalid team name");
        }
    }

    private String calculate(final String s, final Integer i) {
        return s + ": " + binaryToRoman(i);
    }

    // Parallel arrays used in the conversion process.
    private static final String[] RCODE = {"M", "CM", "D", "CD", "C", "XC", "L",
            "XL", "X", "IX", "V", "IV", "I"};
    private static final int[] BVAL = {1000, 900, 500, 400, 100, 90, 50,
            40, 10, 9, 5, 4, 1};

    private String binaryToRoman(int binary) {
        String roman = "";         // Roman notation will be accumualated here.

        // Loop from biggest value to smallest, successively subtracting,
        // from the binary value while adding to the roman representation.
        for (int i = 0; i < RCODE.length; i++) {
            while (binary >= BVAL[i]) {
                binary -= BVAL[i];
                roman += RCODE[i];
            }
        }
        return roman;
    }

    public static Short getId(final String sPlayer) {
        if (playerMaps.get(sPlayer) == null) {
            return Short.MIN_VALUE;
        }
        return (short) playerMaps.getInt(sPlayer);
    }
}