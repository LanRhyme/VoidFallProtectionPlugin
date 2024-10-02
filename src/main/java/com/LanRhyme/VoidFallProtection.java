package com.LanRhyme;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// VoidFallProtection插件主类
public class VoidFallProtection extends JavaPlugin {

    // 存储玩家UUID和掉落次数的映射
    private Map<UUID, Integer> fallCounts;
    // 插件是否启用的标志
    private boolean pluginEnabled;

    // 插件启用时调用此方法
    @Override
    public void onEnable() {
        // 初始化掉落次数映射
        fallCounts = new HashMap<>();
        pluginEnabled = true;

        // 加载配置文件和掉落次数数据
        loadConfig();
        loadFallCounts();

        // 注册事件监听器
        getServer().getPluginManager().registerEvents(new PlayerFallListener(this), this);
    }

    // 插件禁用时调用此方法
    @Override
    public void onDisable() {
        // 保存掉落次数数据
        saveFallCounts();
    }

    // 加载配置文件和掉落次数数据
    private void loadConfig() {
        // 保存默认配置文件，如果不存在的话
        saveDefaultConfig();
        // 加载配置文件
        reloadConfig();
    }

    // 加载掉落次数数据
    private void loadFallCounts() {
        File fallCountsFile = new File(getDataFolder(), "fallCounts.yml");
        if (!fallCountsFile.exists()) {
            // 如果文件不存在，则创建一个新的HashMap
            fallCounts = new HashMap<>();
            return;
        }

        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(fallCountsFile);
            for (String key : config.getKeys(false)) {
                fallCounts.put(UUID.fromString(key), config.getInt(key));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 保存掉落次数数据
    private void saveFallCounts() {
        File fallCountsFile = new File(getDataFolder(), "fallCounts.yml");
        YamlConfiguration config = new YamlConfiguration();

        for (Map.Entry<UUID, Integer> entry : fallCounts.entrySet()) {
            config.set(entry.getKey().toString(), entry.getValue());
        }

        try {
            config.save(fallCountsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 增加玩家的掉落次数
    public void incrementFallCount(Player player) {
        UUID playerId = player.getUniqueId();
        int count = fallCounts.getOrDefault(playerId, 0);
        fallCounts.put(playerId, count + 1);
        saveFallCounts();
    }

    // 给玩家应用药水效果
    public void applyEffects(Player player) {
        if (pluginEnabled) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 60, 50));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 300, 1));
        }
    }

    // 处理插件指令
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("voidfall")) {
            if (args.length == 0) {
                sender.sendMessage("用法: /voidfall <toggle[启用/禁用] | count[查看] | top[排行榜]>");
                return true;
            }

            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("toggle")) {
                pluginEnabled = !pluginEnabled;
                sender.sendMessage("§a§l虚空掉落保护现在" + (pluginEnabled ? "已启用" : "已禁用"));
            } else if (subCommand.equals("count")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§c这个命令只能由玩家使用");
                    return true;
                }
                Player player = (Player) sender;
                int count = fallCounts.getOrDefault(player.getUniqueId(), 0);
                sender.sendMessage("已掉入虚空 " + count + " 次");
            } else if (subCommand.equals("top")) {
                sender.sendMessage("§l§k---§r §9§l自走虚空排行榜前10名§r §l§k---");
                fallCounts.entrySet().stream()
                        .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                        .limit(10)
                        .forEach(entry -> {
                            Player player = Bukkit.getPlayer(entry.getKey());
                            if (player != null) {
                                sender.sendMessage(player.getName() + ": " + entry.getValue() + " §a次");
                            }
                        });
            } else {
                sender.sendMessage("§c未知子命令 §f用法: /voidfall <toggle[启用/禁用] | count[查看] | top[排行榜]>");
            }
            return true;
        }
        return false;
    }
}