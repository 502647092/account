package com.mengcraft.account;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created on 15-10-26.
 */
public class EventBlocker implements Listener {

    private final List<UUID> locked = new ArrayList<>();

    @EventHandler(priority = EventPriority.MONITOR)
    public void handle(PlayerLoginEvent event) {
        if (event.getResult() == PlayerLoginEvent.Result.ALLOWED) {
            locked.add(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void handle(PlayerMoveEvent event) {
        if (isLocked(event.getPlayer().getUniqueId())) {
            Location from = event.getFrom();
            from.setPitch(event.getTo().getPitch());
            from.setYaw(event.getTo().getYaw());

            event.setTo(from);
        }
    }

    @EventHandler
    public void handle(PlayerInteractEvent event) {
        if (isLocked(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void handle(PlayerDropItemEvent event) {
        if (isLocked(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void handle(PlayerPickupItemEvent event) {
        if (isLocked(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void handle(PlayerQuitEvent event) {
        if (isLocked(event.getPlayer().getUniqueId())) {
            unlock(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void handle(InventoryOpenEvent event) {
        if (isLocked(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void handle(EntityDamageEvent event) {
        if (a(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void handle(EntityDamageByEntityEvent event) {
        if (a(event.getDamager())) {
            event.setCancelled(true);
        }
    }

    private boolean a(Entity entity) {
        return entity instanceof Player && isLocked(entity.getUniqueId());
    }

    @EventHandler
    public void handle(FoodLevelChangeEvent event) {
        if (isLocked(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void handle(AsyncPlayerChatEvent event) {
        if (isLocked(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void handle(PlayerCommandPreprocessEvent event) {
        if (isLocked(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    public boolean isLocked(UUID uuid) {
        return locked.contains(uuid);
    }

    public boolean unlock(UUID uuid) {
        return locked.remove(uuid);
    }

}
