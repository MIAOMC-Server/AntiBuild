package com.miaomc.antiBuild;

import com.miaomc.antiBuild.commands.AntiBuildCommand;
import com.miaomc.antiBuild.data.DataManager;
import com.miaomc.antiBuild.listeners.ProtectionListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class AntiBuild extends JavaPlugin {
    private DataManager dataManager;
    private ProtectionListener protectionListener;

    @Override
    public void onEnable() {
        // 保存默认配置文件
        saveDefaultConfig();

        // 初始化数据管理器
        dataManager = new DataManager(this);

        // 注册命令
        AntiBuildCommand commandExecutor = new AntiBuildCommand(this, dataManager);
        getCommand("antibuild").setExecutor(commandExecutor);
        getCommand("antibuild").setTabCompleter(commandExecutor);

        // 注册事件监听器
        protectionListener = new ProtectionListener(this, dataManager);
        getServer().getPluginManager().registerEvents(protectionListener, this);

        getLogger().info("AntiBuild 插件已启用！");
    }

    @Override
    public void onDisable() {
        // 保存数据
        if (dataManager != null) {
            dataManager.shutdown(); // 使用新的shutdown方法
            getLogger().info("数据已保存");
        }

        getLogger().info("AntiBuild 插件已禁用！");
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    /**
     * 重新加载插件配置
     */
    public void reloadPluginConfig() {
        reloadConfig();
        if (protectionListener != null) {
            protectionListener.reloadConfig();
        }
        getLogger().info("配置已重新加载！");
    }
}
