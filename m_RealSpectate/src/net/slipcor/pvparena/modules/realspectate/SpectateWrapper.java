package net.slipcor.pvparena.modules.realspectate;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.managers.InventoryManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashSet;
import java.util.Set;

import static net.slipcor.pvparena.config.Debugger.debug;

class SpectateWrapper {
    private final Player suspect;
    private final Set<Player> spectators = new HashSet<>();
    private final RealSpectateListener listener;

    public SpectateWrapper(final Player spectator, final Player fighter, final RealSpectateListener listener) {
        suspect = fighter;
        spectators.add(spectator);
        ArenaPlayer.parsePlayer(spectator.getName()).setTelePass(true);
        this.listener = listener;
    }

    public void debugSpectators(Arena arena) {
        for (final Player spec : spectators) {
            debug(arena, spec.getName());
        }
    }

    public void update(final Player s) {
        if (!spectators.contains(s)) {
            spectators.add(s);

            class LaterRun implements Runnable {
                @Override
                public void run() {
                    s.setHealth(suspect.getHealth() > 0 ? suspect.getHealth() : 1);

                    InventoryManager.clearInventory(s);
                    s.getInventory().setContents(suspect.getInventory().getContents());
                    s.updateInventory();

                    s.teleport(suspect.getLocation());

                    for (final ArenaPlayer ap : listener.rs.getArena().getEveryone()) {
                        ap.get().hidePlayer(s);
                    }
                    s.hidePlayer(suspect);
                }
            }
            Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), new LaterRun(), 5L);
        }
    }

    public void update() {
        for (final Player s : spectators) {

            class LaterRun implements Runnable {
                private final Player s;

                LaterRun(final Player p) {
                    s = p;
                }

                @Override
                public void run() {
                    s.setHealth(suspect.getHealth() > 0 ? suspect.getHealth() : 1);

                    InventoryManager.clearInventory(s);
                    s.getInventory().setContents(suspect.getInventory().getContents());
                    s.updateInventory();

                    s.teleport(suspect.getLocation());

                    for (final ArenaPlayer ap : listener.rs.getArena().getEveryone()) {
                        ap.get().hidePlayer(s);
                    }
                    s.hidePlayer(suspect);
                }
            }
            Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), new LaterRun(s), 5L);
        }
    }

    public Player getSuspect() {
        return suspect;
    }

    public boolean hasSpectator(final Player p) {
        return spectators.contains(p);
    }

	/*
	 * ------------------------------------
	 * ------------------------------------
	 * ------------------------------------
	 * 
	 */


    public void closeInventory() {
        for (final Player p : spectators) {
            p.closeInventory();
        }
    }

    public void openInventory(final Inventory inventory) {
        for (final Player p : spectators) {
            p.openInventory(inventory);
        }
    }

    public void removeSpectator(final Player spectator) {
        spectators.remove(spectator);
        if (spectators.size() < 1) {
            listener.spectated_players.remove(suspect);
        }
    }

    public void selectItem(final int newSlot) {
		for (final Player p : spectators) {
			p.getInventory().setHeldItemSlot(newSlot);
		}
    }

    public void stopSpectating() {
        for (final Player p : spectators) {
            if (listener.spectated_players.size() < 1) {
                Bukkit.getServer().dispatchCommand(p, "pa leave");
            } else {
                listener.switchPlayer(p, suspect, true);
            }
        }
        spectators.clear();
    }

    public void updateHealth() {
        class LaterRun implements Runnable {
            @Override
            public void run() {
                for (final Player p : spectators) {
                    p.setHealth(suspect.getHealth() > 0 ? suspect.getHealth() : 1);
                }
            }
        }
        Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), new LaterRun(), 5L);
    }

    public void updateInventory() {
        class LaterRun implements Runnable {
            @Override
            public void run() {
                for (final Player p : spectators) {
                    InventoryManager.clearInventory(p);
                    p.getInventory().setContents(suspect.getInventory().getContents());
                    p.updateInventory();
                }
            }
        }
        Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), new LaterRun(), 5L);
    }

    public void updateLocation() {
        for (final Player p : spectators) {
            p.teleport(suspect.getLocation());
        }
    }

    public void stopHard() {
        for (final Player p : spectators) {
            Bukkit.getServer().dispatchCommand(p, "pa leave");
        }
    }
}
