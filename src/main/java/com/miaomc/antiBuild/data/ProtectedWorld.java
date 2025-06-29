package com.miaomc.antiBuild.data;

public class ProtectedWorld {
    private final String worldName;
    private boolean antiPlace = false;
    private boolean antiBreak = false;
    private boolean antiInteraction = false;
    private boolean antiUse = false;
    private boolean antiExplosion = false;

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

    @Override
    public String toString() {
        return "ProtectedWorld{" +
                "worldName='" + worldName + '\'' +
                ", antiPlace=" + antiPlace +
                ", antiBreak=" + antiBreak +
                ", antiInteraction=" + antiInteraction +
                ", antiUse=" + antiUse +
                ", antiExplosion=" + antiExplosion +
                '}';
    }
}
