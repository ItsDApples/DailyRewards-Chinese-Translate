package dev.revivalo.dailyrewards.user;

import dev.revivalo.dailyrewards.DailyRewardsPlugin;
import dev.revivalo.dailyrewards.api.event.ReminderReceiveEvent;
import dev.revivalo.dailyrewards.configuration.data.DataManager;
import dev.revivalo.dailyrewards.hook.HookManager;
import dev.revivalo.dailyrewards.manager.Setting;
import dev.revivalo.dailyrewards.manager.reward.RewardType;
import dev.revivalo.dailyrewards.manager.reward.task.JoinNotificationTask;
import dev.revivalo.dailyrewards.util.PermissionUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class UserHandler implements Listener {
    private static final Map<UUID, User> usersHashMap = new ConcurrentHashMap<>();

    private final JoinNotificationTask joinNotificationTask;

    public UserHandler() {
        this.joinNotificationTask = new JoinNotificationTask(usersHashMap);
        this.joinNotificationTask.get()
                        .runTaskTimerAsynchronously(DailyRewardsPlugin.get(), 5, 5);

        DailyRewardsPlugin.get().registerListeners(this);
    }

    public static User addUser(final User user){
        usersHashMap.put(user.getPlayer().getUniqueId(), user);
        return user;
    }

    public static User getUser(final UUID uuid){
        return usersHashMap.get(uuid);
    }

    @Nullable
    public static User getUser(@Nullable Player player) {
        return Optional.ofNullable(player)
                .map(Player::getUniqueId)
                .map(UserHandler::getUser)
                .orElse(null);
    }

    public static User removeUser(final UUID uuid){
        final User user = usersHashMap.remove(uuid);

        if (user != null) {
            DataManager.updateValues(uuid, user, user.getData());
        }

        return user;
    }

    @EventHandler (priority = EventPriority.HIGH)
    private void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        DataManager.loadPlayerDataAsync(player, data -> {

            User user = UserHandler.addUser(
                    new User(
                            player,
                            data
                    )
            );

            if (!HookManager.isAuthUsed()) {
                if (DailyRewardsPlugin.getRewardManager().processAutoClaimForUser(user)) {
                    return;
                }
            }

            if (!PermissionUtil.hasPermission(player, PermissionUtil.Permission.JOIN_NOTIFICATION_SETTING)) {
                return;
            }

            if (!user.hasSettingEnabled(Setting.JOIN_NOTIFICATION)) {
                return;
            }

            final Set<RewardType> availableRewards = user.getAvailableRewards();
            if (availableRewards.isEmpty()) {
                return;
            }

            ReminderReceiveEvent reminderReceiveEvent = new ReminderReceiveEvent(player, availableRewards);
            Bukkit.getPluginManager().callEvent(reminderReceiveEvent);

            if (reminderReceiveEvent.isCancelled()) {
                return;
            }

            joinNotificationTask.addUser(user);
        });
    }

    @EventHandler
    private void onQuit(PlayerQuitEvent event) {
        User user = UserHandler.removeUser(event.getPlayer().getUniqueId());
        joinNotificationTask.getPlayerRewardCheckTimes().remove(user);
    }
}