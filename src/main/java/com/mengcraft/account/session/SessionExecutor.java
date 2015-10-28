package com.mengcraft.account.session;

import com.mengcraft.account.Account;
import com.mengcraft.account.EventBlocker;
import com.mengcraft.account.Main;
import com.mengcraft.account.entity.AppAccountEvent;
import com.mengcraft.simpleorm.EbeanHandler;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import static com.mengcraft.account.entity.AppAccountEvent.of;
import static com.mengcraft.account.session.SessionServer.CACHED_MAP;

/**
 * Created on 15-10-23.
 */
public class SessionExecutor implements PluginMessageListener, Listener {

    private final EventBlocker blocker = new EventBlocker();
    private final Main main;
    private final EbeanHandler source;

    public SessionExecutor(Main main, EbeanHandler source) {
        this.main = main;
        this.source = source;
    }

    public EventBlocker getBlocker() {
        return blocker;
    }

    @Override
    public void onPluginMessageReceived(String label, Player player, byte[] buffer) {
        main.getLogger().info("[SessionExecutor] Succeed handle a payload packet!");
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(buffer))) {
            if (input.read() == 2) {
                Session cached = CACHED_MAP.get(player.getName());
                Session gotten = new Session(input.readInt(), input.readInt(), input.readInt());
                if (cached != null && cached.equals(gotten)) success(player);
                else failing(player);
            }
        } catch (IOException e) {
            main.getLogger().info("[SessionExecutor] Exception when received a payload message! " + e.getMessage());
        }
    }

    @EventHandler
    public void handle(AsyncPlayerPreLoginEvent event) {
        if (!CACHED_MAP.has(event.getName())) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, "Request a session first!");
        }
    }

    @EventHandler
    public void handle(PlayerJoinEvent event) {
        main.sync(() -> {
            if (blocker.isLocked(event.getPlayer().getUniqueId())) failing(event.getPlayer());
        }, main.getSessionWait());
    }

    private void success(Player player) {
        if (main.isLogEvent()) Account.DEFAULT.getPool().execute(() -> {
            source.insert(of(player, AppAccountEvent.LOG_SUCCESS));
        });
        blocker.unlock(player.getUniqueId());
    }

    private void failing(Player player) {
        if (main.isLogEvent()) Account.DEFAULT.getPool().execute(() -> {
            source.insert(of(player, AppAccountEvent.LOG_FAILURE));
        });
        player.kickPlayer(ChatColor.RED + "Error while check session!");
    }

}
