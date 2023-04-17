package net.slipcor.pvparena.modules.maps;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.MapInitializeEvent;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

class MapListener implements Listener {
    private final ArenaMaps module;

    public MapListener(final ArenaMaps mod) {
        this.module = mod;
    }

    @EventHandler
    public void onMapInit(final MapInitializeEvent event) {
        MapView mapView = event.getMap();
        mapView.setUnlimitedTracking(true);

        MapRenderer mr = new ArenaMapsRenderer(this.module);
        mapView.addRenderer(mr);
    }
}
