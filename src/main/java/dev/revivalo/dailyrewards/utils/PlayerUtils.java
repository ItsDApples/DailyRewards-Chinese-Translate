package dev.revivalo.dailyrewards.utils;

import dev.revivalo.dailyrewards.DailyRewardsPlugin;
import dev.revivalo.dailyrewards.configuration.enums.Config;
import dev.revivalo.dailyrewards.configuration.enums.Lang;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;

public class PlayerUtils {
    public static boolean isPlayerInDisabledWorld(final Player player, boolean announce) {
        final String playerWorldName = player.getWorld().getName();
        if (Config.DISABLED_WORLDS.asReplacedList(Collections.emptyMap()).stream().anyMatch(worldName -> worldName.equalsIgnoreCase(playerWorldName))) {
            if (announce) player.sendMessage(Lang.CLAIMING_IN_DISABLED_WORLD.asColoredString().replace("%world%", playerWorldName));
            return true;
        }
        return false;
    }

    public static float getPlayersPlayTimeInMinutes(final Player player) {
        return (float) (player.getStatistic(Statistic.valueOf(VersionUtils.isLegacyVersion() ? "PLAY_ONE_TICK" : "PLAY_ONE_MINUTE")) / 20.0 / 60.0);
    }

    public static void playSound(Player player, String sound){
        Sound soundToPlay;
        try {
            sound = sound.toUpperCase(Locale.ENGLISH);
            soundToPlay = Sound.valueOf(sound);
        } catch (IllegalArgumentException | NullPointerException ex){
            soundToPlay = VersionUtils.isOldVersion() ? Sound.valueOf("NOTE_PLING") : VersionUtils.isLegacyVersion() ? Sound.valueOf("BLOCK_NOTE_PLING") : Sound.valueOf("BLOCK_NOTE_BLOCK_HARP");
        }
        player.playSound(player.getLocation(), soundToPlay, 2f, 2f);
    }

    public static boolean doesPlayerHaveEnoughPlayTime(final Player player) {
        final int requiredPlayTimeInMinutes = Config.FIRST_TIME_REQUIRED_PLAY_TIME.asInt();
        if (requiredPlayTimeInMinutes != 0) {
            final float actualPlayTimeInMinutes = PlayerUtils.getPlayersPlayTimeInMinutes(player);
            if (actualPlayTimeInMinutes < requiredPlayTimeInMinutes) {
                player.sendMessage(Lang.NOT_ENOUGH_REQUIRED_TIME_TO_CLAIM.asReplacedString(new HashMap<String, String>(){{put("%requiredMinutes%", String.valueOf(requiredPlayTimeInMinutes)); put("%minutes%", String.valueOf(Math.round(requiredPlayTimeInMinutes - actualPlayTimeInMinutes)));}}));
                return false;
            }
        }
        return true;
    }

    public static CompletableFuture<OfflinePlayer> getOfflinePlayer(final UUID uuid) {
        return DailyRewardsPlugin.get().completableFuture(() -> Bukkit.getOfflinePlayer(uuid));
    }

    public static CompletableFuture<OfflinePlayer> getOfflinePlayer(final String playerName) {
        return DailyRewardsPlugin.get().completableFuture(() -> Bukkit.getOfflinePlayer(playerName));
    }

    public static void spawnFirework(Location location){
        Firework fw = (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK);
        fw.setMetadata("nodamage", new FixedMetadataValue(DailyRewardsPlugin.get(), true));
        FireworkMeta meta = fw.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder().withColor(Color.ORANGE, Color.YELLOW).withFade(Color.WHITE).with(FireworkEffect.Type.BALL).build());
        meta.setPower(1);
        fw.setFireworkMeta(meta);
    }
}
