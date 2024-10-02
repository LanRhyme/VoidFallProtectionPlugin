package com.LanRhyme;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

// 定义插件主类，继承JavaPlugin并实现Listener接口
public class VoidFallProtection extends JavaPlugin implements Listener {

    // 插件是否启用的标志
    private boolean isEnabled = true;
    // 存储玩家UUID和掉落次数的映射
    private Map<UUID, Integer> fallCounts;
    // YAML配置文件
    private FileConfiguration dataConfig;
    // YAML配置文件对象
    private File dataFile;

    // 插件启动时调用
    @Override
    public void onEnable() {
        // 初始化掉落次数映射
        this.fallCounts = new HashMap<>();
        // 设置配置文件路径
        this.dataFile = new File(getDataFolder(), "fallCounts.yml");
        // 创建YAML配置文件对象
        this.dataConfig = new YamlConfiguration();

        // 加载掉落次数数据
        loadFallCounts();
        // 保存默认配置文件
        saveDefaultConfig();
        // 注册事件监听器
        getServer().getPluginManager().registerEvents(this, this);
        // 设置命令执行器
        this.getCommand("voidfall").setExecutor(this);
    }

    // 插件卸载时调用
    @Override
    public void onDisable() {
        // 保存掉落次数数据
        saveFallCounts();
    }

    // 加载掉落次数数据
    private void loadFallCounts() {
        // 检查配置文件是否存在
        if (dataFile.exists()) {
            try {
                // 加载配置文件
                dataConfig.load(dataFile);
                // 遍历配置文件中的键值对
                for (String key : dataConfig.getKeys(false)) {
                    UUID playerId = UUID.fromString(key); // 将字符串转换为UUID
                    int count = dataConfig.getInt(key); // 获取掉落次数
                    fallCounts.put(playerId, count); // 存储到映射中
                }
            } catch (Exception e) {
                e.printStackTrace(); // 打印异常信息
            }
        }
    }

    // 保存掉落次数数据
    private void saveFallCounts() {
        // 将掉落次数映射写入配置文件
        for (Map.Entry<UUID, Integer> entry : fallCounts.entrySet()) {
            dataConfig.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            // 保存配置文件
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace(); // 打印异常信息
        }
    }

    // 监听实体伤害事件
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        // 如果插件未启用或者受伤害的不是玩家，则不处理
        if (!isEnabled || !(event.getEntity() instanceof Player)) {
            return;
        }

        // 获取受伤害的玩家
        Player player = (Player) event.getEntity();
        // 获取玩家的位置
        Location location = player.getLocation();
        // 如果伤害原因是虚空并且玩家Y坐标小于-64，则进行处理
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID && location.getY() < -64) {
            event.setCancelled(true); // 取消伤害事件
            // 给予玩家漂浮效果
            player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 60, 10));
            // 给予玩家缓降效果
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 780, 1));
            // 增加玩家掉落次数
            incrementFallCount(player);
        }
    }

    // 增加玩家的掉落次数
    private void incrementFallCount(Player player) {
        UUID playerId = player.getUniqueId(); // 获取玩家UUID
        int count = fallCounts.getOrDefault(playerId, 0); // 获取当前掉落次数
        fallCounts.put(playerId, count + 1); // 增加掉落次数
        saveFallCounts(); // 立即保存数据
    }
    // 执行指令时调用
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // 检查是否是voidfall命令
        if (cmd.getName().equalsIgnoreCase("voidfall")) {
            // 检查参数数量
            if (args.length == 0) {
                sender.sendMessage("使用 /voidfall toggle 来切换虚空掉落保护");
                return true;
            }

            // 处理子命令
            switch (args[0].toLowerCase()) {
                case "toggle":
                    // 切换虚空掉落保护的启用状态
                    isEnabled = !isEnabled;
                    sender.sendMessage("虚空掉落保护现在 " + (isEnabled ? "已启用" : "已禁用"));
                    return true;
                case "count":
                    // 检查发送者是否是玩家
                    if (!(sender instanceof Player)) {
                        sender.sendMessage("这个命令只能由玩家执行");
                        return true;
                    }
                    // 获取玩家UUID
                    Player player = (Player) sender;
                    UUID playerId = player.getUniqueId();
                    // 获取并显示掉落次数
                    int count = fallCounts.getOrDefault(playerId, 0);
                    sender.sendMessage("你掉入虚空 " + count + " 次");
                    return true;
                case "top":
                    // 显示掉落虚空次数的排行榜
                    sender.sendMessage("自走虚空排行榜：");
                    fallCounts.entrySet().stream()
                            .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                            .limit(10) // 显示前10名
                            .forEach(entry -> {
                                String playerName = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                                sender.sendMessage(playerName + ": " + entry.getValue() + " 次");
                            });
                    return true;
                default:
                    // 未知子命令
                    sender.sendMessage("未知子命令，使用 /voidfall toggle, /voidfall count 或 /voidfall top");
                    return true;
            }
        }
        return false;
    }
}