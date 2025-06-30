package com.miaomc.antiBuild.data;

import com.miaomc.antiBuild.AntiBuild;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DataManager {
    private final AntiBuild plugin;
    private final Map<String, ProtectedArea> protectedAreas;
    private final Map<String, ProtectedWorld> protectedWorlds;
    private File dataFile;
    private FileConfiguration dataConfig;

    // 性能优化：添加空间索引缓存
    private final Map<String, List<ProtectedArea>> worldAreaCache = new ConcurrentHashMap<>();
    private final Map<String, ProtectedArea> locationCache = new ConcurrentHashMap<>();
    private static final int MAX_LOCATION_CACHE_SIZE = 10000;
    private static final long CACHE_CLEANUP_INTERVAL = 300000; // 5分钟

    // 异步保存队列
    private volatile boolean dataChanged = false;
    private BukkitRunnable autoSaveTask;

    public DataManager(AntiBuild plugin) {
        this.plugin = plugin;
        this.protectedAreas = new ConcurrentHashMap<>();  // 改为线程安全
        this.protectedWorlds = new ConcurrentHashMap<>(); // 改为线程安全
        setupDataFile();
        loadData();
        startAutoSaveTask();
        startCacheCleanupTask();
    }

    private void setupDataFile() {
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try {
                boolean created = dataFile.createNewFile();
                if (!created) {
                    plugin.getLogger().warning("数据文件可能已存在或创建失败");
                }
            } catch (IOException e) {
                plugin.getLogger().severe("无法创建数据文件: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void loadData() {
        // 加载保护区域
        if (dataConfig.contains("areas") && dataConfig.getConfigurationSection("areas") != null) {
            for (String areaName : Objects.requireNonNull(dataConfig.getConfigurationSection("areas")).getKeys(false)) {
                String path = "areas." + areaName + ".";
                String worldName = dataConfig.getString(path + "world");

                if (worldName == null) {
                    plugin.getLogger().warning("区域 " + areaName + " 的世界名称为空，跳过加载");
                    continue;
                }

                ProtectedArea area = new ProtectedArea(areaName, worldName);

                // 加载点A
                if (dataConfig.contains(path + "pointA")) {
                    World world = Bukkit.getWorld(worldName);
                    if (world != null) {
                        double x = dataConfig.getDouble(path + "pointA.x");
                        double y = dataConfig.getDouble(path + "pointA.y");
                        double z = dataConfig.getDouble(path + "pointA.z");
                        area.setPointA(new Location(world, x, y, z));
                    }
                }

                // 加载点B
                if (dataConfig.contains(path + "pointB")) {
                    World world = Bukkit.getWorld(worldName);
                    if (world != null) {
                        double x = dataConfig.getDouble(path + "pointB.x");
                        double y = dataConfig.getDouble(path + "pointB.y");
                        double z = dataConfig.getDouble(path + "pointB.z");
                        area.setPointB(new Location(world, x, y, z));
                    }
                }

                // 加载保护设置
                area.setAntiPlace(dataConfig.getBoolean(path + "antiPlace", false));
                area.setAntiBreak(dataConfig.getBoolean(path + "antiBreak", false));
                area.setAntiInteraction(dataConfig.getBoolean(path + "antiInteraction", false));
                area.setAntiUse(dataConfig.getBoolean(path + "antiUse", false));
                area.setAntiExplosion(dataConfig.getBoolean(path + "antiExplosion", false)); // 新增
                area.setAntiFishing(dataConfig.getBoolean(path + "antiFishing", false));
                area.setAntiAnimalInteract(dataConfig.getBoolean(path + "antiAnimalInteract", false));
                area.setAntiThrow(dataConfig.getBoolean(path + "antiThrow", false));
                area.setAntiShoot(dataConfig.getBoolean(path + "antiShoot", false));
                area.setAntiTrample(dataConfig.getBoolean(path + "antiTrample", false));

                protectedAreas.put(areaName, area);
            }
        }

        // 加载保护世界
        if (dataConfig.contains("worlds") && dataConfig.getConfigurationSection("worlds") != null) {
            for (String worldName : Objects.requireNonNull(dataConfig.getConfigurationSection("worlds")).getKeys(false)) {
                String path = "worlds." + worldName + ".";

                ProtectedWorld world = new ProtectedWorld(worldName);
                world.setAntiPlace(dataConfig.getBoolean(path + "antiPlace", false));
                world.setAntiBreak(dataConfig.getBoolean(path + "antiBreak", false));
                world.setAntiInteraction(dataConfig.getBoolean(path + "antiInteraction", false));
                world.setAntiUse(dataConfig.getBoolean(path + "antiUse", false));
                world.setAntiExplosion(dataConfig.getBoolean(path + "antiExplosion", false));
                world.setAntiFishing(dataConfig.getBoolean(path + "antiFishing", false));
                world.setAntiAnimalInteract(dataConfig.getBoolean(path + "antiAnimalInteract", false));
                world.setAntiThrow(dataConfig.getBoolean(path + "antiThrow", false));
                world.setAntiShoot(dataConfig.getBoolean(path + "antiShoot", false));
                world.setAntiTrample(dataConfig.getBoolean(path + "antiTrample", false));

                protectedWorlds.put(worldName, world);
            }
        }
    }

    public void saveData() {
        // 保存保护区域
        for (ProtectedArea area : protectedAreas.values()) {
            String path = "areas." + area.getName() + ".";
            dataConfig.set(path + "world", area.getWorldName());

            if (area.getPointA() != null) {
                dataConfig.set(path + "pointA.x", area.getPointA().getX());
                dataConfig.set(path + "pointA.y", area.getPointA().getY());
                dataConfig.set(path + "pointA.z", area.getPointA().getZ());
            }

            if (area.getPointB() != null) {
                dataConfig.set(path + "pointB.x", area.getPointB().getX());
                dataConfig.set(path + "pointB.y", area.getPointB().getY());
                dataConfig.set(path + "pointB.z", area.getPointB().getZ());
            }

            dataConfig.set(path + "antiPlace", area.isAntiPlace());
            dataConfig.set(path + "antiBreak", area.isAntiBreak());
            dataConfig.set(path + "antiInteraction", area.isAntiInteraction());
            dataConfig.set(path + "antiUse", area.isAntiUse());
            dataConfig.set(path + "antiExplosion", area.isAntiExplosion());
            dataConfig.set(path + "antiFishing", area.isAntiFishing());
            dataConfig.set(path + "antiAnimalInteract", area.isAntiAnimalInteract());
            dataConfig.set(path + "antiThrow", area.isAntiThrow());
            dataConfig.set(path + "antiShoot", area.isAntiShoot());
            dataConfig.set(path + "antiTrample", area.isAntiTrample());
        }

        // 保存保护世界
        for (ProtectedWorld world : protectedWorlds.values()) {
            String path = "worlds." + world.getName() + ".";
            dataConfig.set(path + "antiPlace", world.isAntiPlace());
            dataConfig.set(path + "antiBreak", world.isAntiBreak());
            dataConfig.set(path + "antiInteraction", world.isAntiInteraction());
            dataConfig.set(path + "antiUse", world.isAntiUse());
            dataConfig.set(path + "antiExplosion", world.isAntiExplosion());
            dataConfig.set(path + "antiFishing", world.isAntiFishing());
            dataConfig.set(path + "antiAnimalInteract", world.isAntiAnimalInteract());
            dataConfig.set(path + "antiThrow", world.isAntiThrow());
            dataConfig.set(path + "antiShoot", world.isAntiShoot());
            dataConfig.set(path + "antiTrample", world.isAntiTrample());
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("无法保存数据文件: " + e.getMessage());
        }
    }

    // 区域管理方法
    public void createArea(String name, String worldName) {
        protectedAreas.put(name, new ProtectedArea(name, worldName));
    }

    public ProtectedArea getArea(String name) {
        return protectedAreas.get(name);
    }

    public Set<String> getAreaNames() {
        return protectedAreas.keySet();
    }

    public boolean hasArea(String name) {
        return protectedAreas.containsKey(name);
    }

    // 世界管理方法
    public void createWorld(String name) {
        protectedWorlds.put(name, new ProtectedWorld(name));
    }

    public ProtectedWorld getWorld(String name) {
        return protectedWorlds.get(name);
    }

    public Set<String> getWorldNames() {
        return protectedWorlds.keySet();
    }

    public boolean hasWorld(String name) {
        return protectedWorlds.containsKey(name);
    }

    /**
     * 性能优化：使用缓存的位置查找方法
     */
    public ProtectedArea getProtectedAreaAt(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }

        String locationKey = locationToString(location);

        // 先检查位置缓存
        ProtectedArea cachedArea = locationCache.get(locationKey);
        if (cachedArea != null) {
            return cachedArea;
        }

        String worldName = location.getWorld().getName();

        // 使用世界索引缓存来减少查找范围
        List<ProtectedArea> worldAreas = worldAreaCache.computeIfAbsent(worldName,
                k -> protectedAreas.values().stream()
                        .filter(area -> area.getWorldName().equals(worldName))
                        .collect(ArrayList::new, ArrayList::add, ArrayList::addAll));

        // 在指定世界的区域中查找
        for (ProtectedArea area : worldAreas) {
            if (area.contains(location)) {
                // 缓存结果，但要控制缓存大小
                if (locationCache.size() < MAX_LOCATION_CACHE_SIZE) {
                    locationCache.put(locationKey, area);
                }
                return area;
            }
        }

        return null;
    }

    public ProtectedWorld getProtectedWorld(String worldName) {
        return protectedWorlds.get(worldName);
    }

    /**
     * 异步保存数据，避阻塞主线程
     */
    public void saveDataAsync() {
        if (!dataChanged) {
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                saveData();
                dataChanged = false;
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * 标记数据已更改
     */
    public void markDataChanged() {
        this.dataChanged = true;
    }

    /**
     * 启动自动保存任务
     */
    private void startAutoSaveTask() {
        autoSaveTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (dataChanged) {
                    saveDataAsync();
                }
            }
        };
        // 每30秒检查一次是否需要保存
        autoSaveTask.runTaskTimerAsynchronously(plugin, 600L, 600L);
    }

    /**
     * 启动缓存清理任务
     */
    private void startCacheCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                cleanupCache();
            }
        }.runTaskTimerAsynchronously(plugin, CACHE_CLEANUP_INTERVAL / 1000 * 20, CACHE_CLEANUP_INTERVAL / 1000 * 20);
    }

    /**
     * 清理缓存
     */
    private void cleanupCache() {
        // 清理位置缓存
        if (locationCache.size() > MAX_LOCATION_CACHE_SIZE * 0.8) {
            locationCache.clear();
        }

        // 重建世界区域缓存
        worldAreaCache.clear();
    }

    /**
     * 将位置转换为字符串键
     */
    private String locationToString(Location location) {
        return String.format("%s:%d:%d:%d",
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());
    }

    /**
     * 添加区域时更新缓存
     */
    public void addArea(ProtectedArea area) {
        protectedAreas.put(area.getName(), area);
        worldAreaCache.remove(area.getWorldName()); // 清除相关世界的缓存
        markDataChanged();
    }

    /**
     * 移除区域时更新缓存
     */
    public void removeArea(String areaName) {
        ProtectedArea removed = protectedAreas.remove(areaName);
        if (removed != null) {
            worldAreaCache.remove(removed.getWorldName()); // 清除相关世界的缓存
            locationCache.clear(); // 清除位置缓存
            markDataChanged();
        }
    }

    /**
     * 添加世界保护
     */
    public void addWorld(ProtectedWorld world) {
        protectedWorlds.put(world.getWorldName(), world);
        markDataChanged();
    }

    /**
     * 移除世界保护
     */
    public void removeWorld(String worldName) {
        if (protectedWorlds.remove(worldName) != null) {
            markDataChanged();
        }
    }

    /**
     * 关闭时清理资源
     */
    public void shutdown() {
        if (autoSaveTask != null && !autoSaveTask.isCancelled()) {
            autoSaveTask.cancel();
        }

        // 最后保存一次数据
        if (dataChanged) {
            saveData();
        }
    }

    /**
     * 获取所有区域（只读）
     */
    public Collection<ProtectedArea> getAllAreas() {
        return Collections.unmodifiableCollection(protectedAreas.values());
    }

    /**
     * 获取所有世界（只读）
     */
    public Collection<ProtectedWorld> getAllWorlds() {
        return Collections.unmodifiableCollection(protectedWorlds.values());
    }
}
