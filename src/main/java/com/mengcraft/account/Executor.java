package com.mengcraft.account;

import com.mengcraft.account.entity.AppAccountEvent;
import com.mengcraft.account.entity.User;
import com.mengcraft.account.lib.ArrayVector;
import com.mengcraft.account.lib.SecureUtil;
import com.mengcraft.account.lib.StringUtil;
import com.mengcraft.simpleorm.EbeanHandler;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import static com.mengcraft.account.entity.AppAccountEvent.*;

public class Executor implements Listener {

    private final Map<String, User> userMap = Account.DEFAULT.getUserMap();
    private final ExecutorService pool = Account.DEFAULT.getPool();

    private final EventBlocker blocker = new EventBlocker() {

        @EventHandler
        public void handle(PlayerCommandPreprocessEvent event) {
            if (isLocked(event.getPlayer().getUniqueId())) {
                String[] d = StringUtil.DEF.split(event.getMessage());
                ArrayVector<String> vector = new ArrayVector<>(d);
                String c = vector.next();
                if (c.equals("/l") || c.equals("/login")) {
                    login(event.getPlayer(), vector);
                }
                if (c.equals("/r") || c.equals("/reg") || c.equals("/register")) {
                    register(event.getPlayer(), vector);
                }
                event.setCancelled(true);
            }
        }

    };

    private Main main;
    private EbeanHandler source;

    private String[] contents;
    private int castInterval;

    public void bind(Main main, EbeanHandler source) {
        if (getMain() != main) {
            setContents(main.getConfig().getStringList("broadcast.content"));
            setMain(main);
            getMain().getServer()
                    .getPluginManager()
                    .registerEvents(this, main);
            getMain().getServer()
                    .getPluginManager()
                    .registerEvents(blocker, main);
            setCastInterval(main.getConfig().getInt("broadcast.interval"));
            setSource(source);
        }
    }

    public void setCastInterval(int castInterval) {
        this.castInterval = castInterval;
    }

    private class MessageHandler extends BukkitRunnable {

        private final Player player;
        private final UUID uuid;

        private MessageHandler(Player player) {
            this.player = player;
            this.uuid = player.getUniqueId();
        }

        public void run() {
            if (player.isOnline() && isLocked(uuid))
                player.sendMessage(contents);
            else
                cancel(); // Cancel if player exit or unlocked.
        }

    }

    @EventHandler
    public void handle(AsyncPlayerPreLoginEvent event) {
        if (event.getName().length() > 15) {
            event.setLoginResult(Result.KICK_OTHER);
            event.setKickMessage("用户名长度不能大于15位");
        } else if (!event.getName().matches("[\\w]+")) {
            event.setLoginResult(Result.KICK_OTHER);
            event.setKickMessage("用户名只能包含英文数字下划线");
        }
    }

    @EventHandler
    public void handle(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        getTask().runTaskLater(getMain(), () -> {
            if (player.isOnline() && isLocked(player.getUniqueId())) {
                event.getPlayer().kickPlayer(ChatColor.DARK_RED + "未登录");
                if (main.isLogEvent()) pool.execute(() -> {
                    source.save(of(player, LOG_FAILURE));
                });
            }
        }, 600);
        new MessageHandler(player).runTaskTimer(main, 0, castInterval);
    }

    private Main getMain() {
        return main;
    }

    private void setMain(Main main) {
        this.main = main;
    }

    private ExecutorService getPool() {
        return pool;
    }

    private EbeanHandler getSource() {
        return source;
    }

    private void setSource(EbeanHandler source) {
        this.source = source;
    }

    private Map<String, User> getUserMap() {
        return userMap;
    }

    private BukkitScheduler getTask() {
        return getMain().getServer().getScheduler();
    }

    private void register(Player player, ArrayVector<String> vector) {
        if (vector.remain() == 2) {
            String name = player.getName();
            User user = getUserMap().get(name);
            String secure = vector.next();
            if (user != null && !user.valid() && secure.equals(vector.next())) {
                a(user, name, secure, player);
                player.sendMessage(ChatColor.GREEN + "注册成功");
                if (main.isLogEvent()) pool.execute(() -> {
                    source.save(of(player, AppAccountEvent.REG_SUCCESS));
                });
            } else {
                player.sendMessage(ChatColor.DARK_RED + "注册失败");
                if (main.isLogEvent()) pool.execute(() -> {
                    source.save(of(player, AppAccountEvent.REG_FAILURE));
                });
            }
        }
    }

    private void login(Player player, ArrayVector<String> vector) {
        if (vector.remain() != 0) {
            User user = getUserMap().get(player.getName());
            if (user != null && user.valid() && user.valid(vector.next())) {
                blocker.unlock(player.getUniqueId());
                player.sendMessage(ChatColor.GREEN + "登陆成功");
                if (main.isLogEvent()) pool.execute(() -> {
                    source.save(of(player, LOG_SUCCESS));
                });
            } else {
                player.sendMessage(ChatColor.DARK_RED + "密码错误");
            }
        }
    }

    private void a(User user, String name, String secure, Player player) {
        SecureUtil util = SecureUtil.DEFAULT;
        String salt = util.random(3);
        try {
            user.setPassword(util.digest(util.digest(secure) + salt));
        } catch (Exception e) {
            getMain().getLogger().warning(e.toString());
        }
        user.setSalt(salt);
        user.setUsername(name);
        user.setRegip(player.getAddress().getAddress().getHostAddress());
        user.setRegdate(nowSec());
        getPool().execute(() -> getSource().save(user));

        blocker.unlock(player.getUniqueId());
    }

    private int nowSec() {
        return (int) (System.currentTimeMillis() / 1000);
    }

    private boolean isLocked(UUID uuid) {
        return blocker.isLocked(uuid);
    }

    private void setContents(List<String> list) {
        contents = list.toArray(new String[list.size()]);
    }

}
