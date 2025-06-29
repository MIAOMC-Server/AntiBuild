package com.miaomc.antiBuild.data;

import org.bukkit.Location;

public class ProtectedArea {
    private final String name;
    private final String worldName;
    private Location pointA;
    private Location pointB;
    private boolean antiPlace = false;
    private boolean antiBreak = false;
    private boolean antiInteraction = false;
    private boolean antiUse = false;
    private boolean antiExplosion = false; // 新增防爆炸设置
    private boolean antiFishing = false; // 新增防钓鱼设置
    private boolean antiAnimalInteract = false; // 新增防动物交互设置
    private boolean antiThrow = false; // 新增防投掷设置
    private boolean antiShoot = false; // 新增防射击设置

    public ProtectedArea(String name, String worldName) {
        this.name = name;
        this.worldName = worldName;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public String getWorldName() {
        return worldName;
    }

    public Location getPointA() {
        return pointA;
    }

    public void setPointA(Location pointA) {
        this.pointA = pointA;
        boundsCached = false; // 重置缓存标志
    }

    public Location getPointB() {
        return pointB;
    }

    public void setPointB(Location pointB) {
        this.pointB = pointB;
        boundsCached = false; // 重置缓存标志
    }

    public boolean isAntiPlace() {
        return antiPlace;
    }

    public void setAntiPlace(boolean antiPlace) {
        this.antiPlace = antiPlace;
    }

    public boolean isAntiBreak() {
        return antiBreak;
    }

    public void setAntiBreak(boolean antiBreak) {
        this.antiBreak = antiBreak;
    }

    public boolean isAntiInteraction() {
        return antiInteraction;
    }

    public void setAntiInteraction(boolean antiInteraction) {
        this.antiInteraction = antiInteraction;
    }

    public boolean isAntiUse() {
        return antiUse;
    }

    public void setAntiUse(boolean antiUse) {
        this.antiUse = antiUse;
    }

    public boolean isAntiExplosion() {
        return antiExplosion;
    }

    public void setAntiExplosion(boolean antiExplosion) {
        this.antiExplosion = antiExplosion;
    }

    public boolean isAntiFishing() {
        return antiFishing;
    }

    public void setAntiFishing(boolean antiFishing) {
        this.antiFishing = antiFishing;
    }

    public boolean isAntiAnimalInteract() {
        return antiAnimalInteract;
    }

    public void setAntiAnimalInteract(boolean antiAnimalInteract) {
        this.antiAnimalInteract = antiAnimalInteract;
    }

    public boolean isAntiThrow() {
        return antiThrow;
    }

    public void setAntiThrow(boolean antiThrow) {
        this.antiThrow = antiThrow;
    }

    public boolean isAntiShoot() {
        return antiShoot;
    }

    public void setAntiShoot(boolean antiShoot) {
        this.antiShoot = antiShoot;
    }

    public boolean isComplete() {
        return pointA != null && pointB != null;
    }

    /**
     * 优化的包含检查方法，使用早期返回和缓存边界值
     */
    public boolean contains(Location location) {
        if (!isComplete() || location.getWorld() == null ||
                !location.getWorld().getName().equals(worldName)) {
            return false;
        }

        return location.getX() >= minX && location.getX() <= maxX &&
                location.getY() >= minY && location.getY() <= maxY &&
                location.getZ() >= minZ && location.getZ() <= maxZ;
    }

    // 性能优化：缓存边界值
    private double minX, maxX, minY, maxY, minZ, maxZ;
    private boolean boundsCached = false;

    /**
     * 缓存边界值以提高contains()方法性能
     */
    private void cacheBounds() {
        if (isComplete()) {
            minX = Math.min(pointA.getX(), pointB.getX());
            maxX = Math.max(pointA.getX(), pointB.getX());
            minY = Math.min(pointA.getY(), pointB.getY());
            maxY = Math.max(pointA.getY(), pointB.getY());
            minZ = Math.min(pointA.getZ(), pointB.getZ());
            maxZ = Math.max(pointA.getZ(), pointB.getZ());
            boundsCached = true;
        }
    }

    /**
     * 获取区域体积（用于优化排序）
     */
    public double getVolume() {
        if (!isComplete()) return 0;
        if (!boundsCached) cacheBounds();
        return (maxX - minX) * (maxY - minY) * (maxZ - minZ);
    }

    /**
     * 快速距离检查（用于优化查找）
     */
    public double getDistanceSquared(Location location) {
        if (!isComplete() || !boundsCached) {
            cacheBounds();
        }

        double dx = Math.max(0, Math.max(minX - location.getX(), location.getX() - maxX));
        double dy = Math.max(0, Math.max(minY - location.getY(), location.getY() - maxY));
        double dz = Math.max(0, Math.max(minZ - location.getZ(), location.getZ() - maxZ));

        return dx * dx + dy * dy + dz * dz;
    }

    @Override
    public String toString() {
        return "ProtectedArea{" +
                "name='" + name + '\'' +
                ", worldName='" + worldName + '\'' +
                ", complete=" + isComplete() +
                ", volume=" + getVolume() +
                '}';
    }
}
