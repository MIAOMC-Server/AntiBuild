package com.miaomc.antiBuild.listeners;

import com.miaomc.antiBuild.AntiBuild;
import com.miaomc.antiBuild.data.DataManager;
import com.miaomc.antiBuild.data.ProtectedArea;
import com.miaomc.antiBuild.data.ProtectedWorld;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ProtectionListener implements Listener {
    private final AntiBuild plugin;
    private final DataManager dataManager;
    private final Map<UUID, Map<String, Long>> messageCooldowns;
    private final long cooldownTime;

    // 缓存的配置消息，避免重复读取配置文件
    private final Map<String, String> cachedMessages;

    // 性能优化：使用静态缓存集合
    private static final Set<Material> INTERACTIVE_MATERIALS;
    private static final Set<String> INTERACTIVE_SUFFIXES;
    private static final Map<Material, Boolean> MATERIAL_CACHE = new ConcurrentHashMap<>();
    private static final int MAX_MATERIAL_CACHE_SIZE = 500; // 限制缓存大小

    // 数据缓存，减少数据库查询
    private final Map<String, ProtectedWorld> worldCache = new ConcurrentHashMap<>();
    private final Map<String, ProtectedArea> areaCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 1000;
    private static final long CACHE_EXPIRE_TIME = 30000; // 30秒缓存过期

    static {
        // 预定义具有GUI的容器类和功能性方块
        INTERACTIVE_MATERIALS = EnumSet.of(
                Material.CHEST, Material.TRAPPED_CHEST, Material.ENDER_CHEST, Material.SHULKER_BOX,
                Material.FURNACE, Material.DISPENSER, Material.DROPPER, Material.HOPPER,
                Material.CRAFTING_TABLE, Material.ENCHANTING_TABLE, Material.BREWING_STAND,
                Material.BEACON, Material.ANVIL, Material.LEVER, Material.REPEATER,
                Material.COMPARATOR, Material.DAYLIGHT_DETECTOR, Material.NOTE_BLOCK,
                Material.JUKEBOX, Material.CAULDRON, Material.BELL, Material.FLOWER_POT,
                Material.ITEM_FRAME, Material.DRAGON_EGG, Material.END_PORTAL_FRAME
        );

        // 1.13+ 新方块支持，添加版本检测和日志记录
        List<String> newMaterials = Arrays.asList(
                "BLAST_FURNACE", "SMOKER", "GRINDSTONE", "STONECUTTER", "LOOM",
                "CARTOGRAPHY_TABLE", "FLETCHING_TABLE", "SMITHING_TABLE", "BARREL",
                "LECTERN", "COMPOSTER", "RESPAWN_ANCHOR", "LODESTONE", "GLOW_ITEM_FRAME",
                "TRIPWIRE_HOOK", "CHIPPED_ANVIL", "DAMAGED_ANVIL"
        );

        for (String materialName : newMaterials) {
            try {
                INTERACTIVE_MATERIALS.add(Material.valueOf(materialName));
            } catch (IllegalArgumentException ignored) {
                // 静默忽略不存在的材质，避免日志污染
            }
        }

        // 需要后缀匹配的方块类型
        INTERACTIVE_SUFFIXES = Set.of(
                "_DOOR", "_TRAPDOOR", "_FENCE_GATE", "_BUTTON",
                "_PRESSURE_PLATE", "_SHULKER_BOX", "_BED", "_CAULDRON"
        );
    }

    public ProtectionListener(AntiBuild plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.messageCooldowns = new ConcurrentHashMap<>();
        this.cooldownTime = plugin.getConfig().getLong("message-cooldown", 5) * 1000;
        this.cachedMessages = new ConcurrentHashMap<>();

        // 预加载和缓存配置消息
        loadConfigMessages();

        // 启动定期清理任务
        startCleanupTask();
    }

    /**
     * 预加载配置消息到缓存
     */
    private void loadConfigMessages() {
        cachedMessages.put("place", translateColors(plugin.getConfig().getString("messages.protection-messages.place")));
        cachedMessages.put("break", translateColors(plugin.getConfig().getString("messages.protection-messages.break")));
        cachedMessages.put("interaction", translateColors(plugin.getConfig().getString("messages.protection-messages.interaction")));
        cachedMessages.put("use", translateColors(plugin.getConfig().getString("messages.protection-messages.use")));
        cachedMessages.put("world-place", translateColors(plugin.getConfig().getString("messages.world-protection-messages.place")));
        cachedMessages.put("world-break", translateColors(plugin.getConfig().getString("messages.world-protection-messages.break")));
        cachedMessages.put("world-interaction", translateColors(plugin.getConfig().getString("messages.world-protection-messages.interaction")));
        cachedMessages.put("world-use", translateColors(plugin.getConfig().getString("messages.world-protection-messages.use")));
    }

    /**
     * 预处理颜色代码
     */
    private String translateColors(String message) {
        return message != null ? ChatColor.translateAlternateColorCodes('&', message) : "";
    }

    /**
     * 启动定期清理任务
     */
    private void startCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                cleanupCaches();
            }
        }.runTaskTimerAsynchronously(plugin, 6000L, 6000L); // 每5分钟执行一次
    }

    /**
     * 清理过期缓存
     */
    private void cleanupCaches() {
        long currentTime = System.currentTimeMillis();

        // 清理消息冷却缓存中的离线玩家数据
        messageCooldowns.entrySet().removeIf(entry -> {
            UUID playerId = entry.getKey();
            return plugin.getServer().getPlayer(playerId) == null;
        });

        // 限制材质缓存大小
        if (MATERIAL_CACHE.size() > MAX_MATERIAL_CACHE_SIZE) {
            MATERIAL_CACHE.clear(); // 简单粗暴的清理方式
        }

        // 清理区域和世界缓存（如果有时间戳的话）
        // 这里需要根据实际的缓存实现来调整
    }

    // 玩家离线时清理相关数据
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        messageCooldowns.remove(playerId);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        // 检查是否有绕过权限
        if (player.hasPermission("miaomc.antibuild.bypass")) {
            return;
        }

        Location location = event.getBlock().getLocation();

        // 检查区域保护（使用缓存优化）
        ProtectedArea area = getCachedProtectedArea(location);
        if (area != null && area.isAntiPlace()) {
            event.setCancelled(true);
            sendCooldownMessage(player, "place");
            return;
        }

        // 检查世界保护（使用缓存优化）
        ProtectedWorld world = getCachedProtectedWorld(location.getWorld().getName());
        if (world != null && world.isAntiPlace()) {
            event.setCancelled(true);
            sendCooldownMessage(player, "world-place");
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        // 检查是否有绕过权限
        if (player.hasPermission("miaomc.antibuild.bypass")) {
            return;
        }

        Location location = event.getBlock().getLocation();

        // 检查区域保护（使用缓存优化）
        ProtectedArea area = getCachedProtectedArea(location);
        if (area != null && area.isAntiBreak()) {
            event.setCancelled(true);
            sendCooldownMessage(player, "break");
            return;
        }

        // 检查世界保护（使用缓存优化）
        ProtectedWorld world = getCachedProtectedWorld(location.getWorld().getName());
        if (world != null && world.isAntiBreak()) {
            event.setCancelled(true);
            sendCooldownMessage(player, "world-break");
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // 性能优化：早期返回检查，最高频的检查放在前面
        if (event.getClickedBlock() == null ||
                event.getAction() != Action.RIGHT_CLICK_BLOCK ||
                player.hasPermission("miaomc.antibuild.bypass")) {
            return;
        }

        Material material = event.getClickedBlock().getType();
        boolean isInteraction = isInteractiveBlockOptimized(material);
        boolean isUse = !isInteraction && event.getItem() != null;

        // 如果既不是交互也不是使用物品，直接返回
        if (!isInteraction && !isUse) {
            return;
        }

        Location location = event.getClickedBlock().getLocation();

        // 检查区域保护（使用缓存优化）
        ProtectedArea area = getCachedProtectedArea(location);
        if (area != null) {
            if (isInteraction && area.isAntiInteraction()) {
                event.setCancelled(true);
                sendCooldownMessage(player, "interaction");
                return;
            } else if (isUse && area.isAntiUse()) {
                event.setCancelled(true);
                sendCooldownMessage(player, "use");
                return;
            }
        }

        // 检查世界保护（使用缓存优化）
        ProtectedWorld world = getCachedProtectedWorld(location.getWorld().getName());
        if (world != null) {
            if (isInteraction && world.isAntiInteraction()) {
                event.setCancelled(true);
                sendCooldownMessage(player, "world-interaction");
            } else if (isUse && world.isAntiUse()) {
                event.setCancelled(true);
                sendCooldownMessage(player, "world-use");
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityExplode(EntityExplodeEvent event) {
        Location explosionLocation = event.getLocation();
        if (explosionLocation.getWorld() == null) {
            return; // 修复空指针警告
        }

        String worldName = explosionLocation.getWorld().getName();

        // 检查世界保护（使用缓存优化）
        ProtectedWorld protectedWorld = getCachedProtectedWorld(worldName);
        if (protectedWorld != null && protectedWorld.isAntiExplosion()) {
            event.setCancelled(true);
            return;
        }

        // 批量检查区域保护 - 优化爆炸事件处理
        List<Block> protectedBlocks = new ArrayList<>();
        for (Block block : event.blockList()) {
            ProtectedArea area = getCachedProtectedArea(block.getLocation());
            if (area != null && area.isAntiExplosion()) {
                protectedBlocks.add(block);
            }
        }

        // 批量移除受保护的方块
        event.blockList().removeAll(protectedBlocks);
    }

    /**
     * 优化的消息发送方法，使用缓存的消息
     */
    private void sendCooldownMessage(Player player, String messageType) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        // 获取玩家的冷却时间映射
        Map<String, Long> playerCooldowns = messageCooldowns.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());

        // 检查是否在冷却时间内
        Long lastMessageTime = playerCooldowns.get(messageType);
        if (lastMessageTime != null && (currentTime - lastMessageTime) < cooldownTime) {
            return; // 仍在冷却时间内，不发送消息
        }

        // 使用缓存的消息发送
        String message = cachedMessages.get(messageType);
        if (message != null && !message.isEmpty()) {
            player.sendMessage(message);
        }
        playerCooldowns.put(messageType, currentTime);
    }

    /**
     * 获取缓存的保护区域，减少数据库查询
     */
    private ProtectedArea getCachedProtectedArea(Location location) {
        String key = locationToString(location);
        return areaCache.computeIfAbsent(key, k -> dataManager.getProtectedAreaAt(location));
    }

    /**
     * 获取缓存的保护世界，减少数据库查询
     */
    private ProtectedWorld getCachedProtectedWorld(String worldName) {
        return worldCache.computeIfAbsent(worldName, k -> dataManager.getProtectedWorld(worldName));
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
     * 公共方法：重新加载配置缓存
     */
    public void reloadConfig() {
        cachedMessages.clear();
        worldCache.clear();
        areaCache.clear();
        loadConfigMessages();
    }

    /**
     * 优化的方块交互检查方法
     *
     * @param material 方块材质
     * @return 是否为可交互方块
     */
    private boolean isInteractiveBlockOptimized(Material material) {
        // 先检查缓存
        Boolean cachedResult = MATERIAL_CACHE.get(material);
        if (cachedResult != null) {
            return cachedResult;
        }

        boolean result = false;

        // 直接在集合中查找，无需遍历 - O(1) 操作
        if (INTERACTIVE_MATERIALS.contains(material)) {
            result = true;
        } else {
            // 只有在直接匹配失败时才进行后缀匹配
            String materialName = material.name();
            for (String suffix : INTERACTIVE_SUFFIXES) {
                if (materialName.endsWith(suffix)) {
                    result = true;
                    break;
                }
            }
        }

        // 在添加到缓存前检查缓存大小，防止内存溢出
        if (MATERIAL_CACHE.size() < MAX_MATERIAL_CACHE_SIZE) {
            MATERIAL_CACHE.put(material, result);
        }

        return result;
    }
}
