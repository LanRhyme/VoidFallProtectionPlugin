package com.LanRhyme;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class PlayerFallListener implements Listener {
    private final VoidFallProtection plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public PlayerFallListener(VoidFallProtection plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location loc = player.getLocation();

        // 检查玩家是否在虚空区域
        if (loc.getY() < -64) {
            UUID playerId = player.getUniqueId();

            // 获取当前时间
            long currentTime = System.currentTimeMillis();
            // 检查玩家是否在冷却时间内
            if (!cooldowns.containsKey(playerId) || (currentTime - cooldowns.get(playerId)) >= 5000) {
                // 更新玩家的冷却时间
                cooldowns.put(playerId, currentTime);

                // 增加玩家的掉落次数
                plugin.incrementFallCount(player);
                // 给玩家应用药水效果
                plugin.applyEffects(player);
                // 发送警告消息
                player.sendMessage("§c§l警告：你掉入了虚空！");
            }
        }
    }
}
