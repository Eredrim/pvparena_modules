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
        this.suspect = fighter;
        this.spectators.add(spectator);
        ArenaPlayer.fromPlayer(spectator).setTelePass(true);
        this.listener = listener;
    }

    public void debugSpectators(Arena arena) {
        this.spectators.forEach(spec -> debug(arena, spec.getName()));
    }

    public void update(final Player s) {
        if (!this.spectators.contains(s)) {
            this.spectators.add(s);

            class LaterRun implements Runnable {
                @Override
                public void run() {
                    s.setHealth(SpectateWrapper.this.suspect.getHealth() > 0 ? SpectateWrapper.this.suspect.getHealth() : 1);

                    InventoryManager.clearInventory(s);
                    s.getInventory().setContents(SpectateWrapper.this.suspect.getInventory().getContents());
                    s.updateInventory();

                    s.teleport(SpectateWrapper.this.suspect.getLocation());

                    for (final ArenaPlayer arenaPlayer : SpectateWrapper.this.listener.rs.getArena().getEveryone()) {
                        arenaPlayer.getPlayer().hidePlayer(PVPArena.getInstance(), s);
                    }
                    s.hidePlayer(PVPArena.getInstance(), SpectateWrapper.this.suspect);
                }
            }
            Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), new LaterRun(), 5L);
        }
    }

    public void update() {
        for (final Player s : this.spectators) {

            class LaterRun implements Runnable {
                private final Player s;

                LaterRun(final Player p) {
                    this.s = p;
                }

                @Override
                public void run() {
                    this.s.setHealth(SpectateWrapper.this.suspect.getHealth() > 0 ? SpectateWrapper.this.suspect.getHealth() : 1);

                    InventoryManager.clearInventory(this.s);
                    this.s.getInventory().setContents(SpectateWrapper.this.suspect.getInventory().getContents());
                    this.s.updateInventory();

                    this.s.teleport(SpectateWrapper.this.suspect.getLocation());

                    for (final ArenaPlayer arenaPlayer : SpectateWrapper.this.listener.rs.getArena().getEveryone()) {
                        arenaPlayer.getPlayer().hidePlayer(PVPArena.getInstance(), this.s);
                    }
                    this.s.hidePlayer(PVPArena.getInstance(), SpectateWrapper.this.suspect);
                }
            }
            Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), new LaterRun(s), 5L);
        }
    }

    public Player getSuspect() {
        return this.suspect;
    }

    public boolean hasSpectator(final Player p) {
        return this.spectators.contains(p);
    }

	/*
	 * ------------------------------------
	 * ------------------------------------
	 * ------------------------------------
	 * 
	 */


    public void closeInventory() {
        for (final Player p : this.spectators) {
            p.closeInventory();
        }
    }

    public void openInventory(final Inventory inventory) {
        for (final Player p : this.spectators) {
            p.openInventory(inventory);
        }
    }

    public void removeSpectator(final Player spectator) {
        this.spectators.remove(spectator);
        if (this.spectators.size() < 1) {
            this.listener.spectated_players.remove(this.suspect);
        }
    }

    public void selectItem(final int newSlot) {
		for (final Player p : this.spectators) {
			p.getInventory().setHeldItemSlot(newSlot);
		}
    }

    public void stopSpectating() {
        for (final Player p : this.spectators) {
            if (this.listener.spectated_players.size() < 1) {
                Bukkit.getServer().dispatchCommand(p, "pa leave");
            } else {
                this.listener.switchPlayer(p, this.suspect, true);
            }
        }
        this.spectators.clear();
    }

    public void updateHealth() {
        class LaterRun implements Runnable {
            @Override
            public void run() {
                for (final Player p : SpectateWrapper.this.spectators) {
                    p.setHealth(SpectateWrapper.this.suspect.getHealth() > 0 ? SpectateWrapper.this.suspect.getHealth() : 1);
                }
            }
        }
        Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), new LaterRun(), 5L);
    }

    public void updateInventory() {
        class LaterRun implements Runnable {
            @Override
            public void run() {
                for (final Player p : SpectateWrapper.this.spectators) {
                    InventoryManager.clearInventory(p);
                    p.getInventory().setContents(SpectateWrapper.this.suspect.getInventory().getContents());
                    p.updateInventory();
                }
            }
        }
        Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), new LaterRun(), 5L);
    }

    public void updateLocation() {
        for (final Player p : this.spectators) {
            p.teleport(this.suspect.getLocation());
        }
    }

    public void stopHard() {
        for (final Player p : this.spectators) {
            Bukkit.getServer().dispatchCommand(p, "pa leave");
        }
    }
}
