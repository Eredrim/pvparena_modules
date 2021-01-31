package net.slipcor.pvparena.modules.maps;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.MapInitializeEvent;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

class MapListener implements Listener {
    private final Maps maps;

    public MapListener(final Maps m) {
        maps = m;
    }

    @EventHandler
    public void onMapInit(final MapInitializeEvent event) {
        final MapView map = event.getMap();

        final MapRenderer mr = new MyRenderer(maps);
        map.addRenderer(mr);
    }
}
