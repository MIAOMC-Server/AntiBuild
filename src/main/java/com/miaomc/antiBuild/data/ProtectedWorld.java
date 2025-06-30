package com.miaomc.antiBuild.data;

public class ProtectedWorld {
    private final String worldName;
    private boolean antiPlace = false;
    private boolean antiBreak = false;
    private boolean antiInteraction = false;
    private boolean antiUse = false;
    private boolean antiExplosion = false;
    private boolean antiFishing = false; // 新增防钓鱼设置
    private boolean antiAnimalInteract = false; // 新增防动物交互设置
    private boolean antiThrow = false; // 新增防投掷设置
    private boolean antiShoot = false; // 新增防射击设置
    private boolean antiTrample = false; // 新增防踩踏农作物设置

    public ProtectedWorld(String worldName) {
        this.worldName = worldName;
    }

    // Getters and Setters
    public String getWorldName() {
        return worldName;
    }

    public String getName() {
        return worldName;
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

    public boolean isAntiTrample() {
        return antiTrample;
    }

    public void setAntiTrample(boolean antiTrample) {
        this.antiTrample = antiTrample;
    }

    @Override
    public String toString() {
        return "ProtectedWorld{" +
                "worldName='" + worldName + '\'' +
                ", antiPlace=" + antiPlace +
                ", antiBreak=" + antiBreak +
                ", antiInteraction=" + antiInteraction +
                ", antiUse=" + antiUse +
                ", antiExplosion=" + antiExplosion +
                ", antiFishing=" + antiFishing +
                ", antiAnimalInteract=" + antiAnimalInteract +
                ", antiThrow=" + antiThrow +
                ", antiShoot=" + antiShoot +
                ", antiTrample=" + antiTrample +
                '}';
    }
}
