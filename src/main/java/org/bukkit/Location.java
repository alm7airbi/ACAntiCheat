package org.bukkit;

/**
 * Minimal location stub to let Paper-oriented listeners compile in this
 * offline environment. Real servers should rely on the Paper/Bukkit
 * implementation which also exposes worlds, yaw/pitch, etc.
 */
public class Location {
    private Object world;
    private double x;
    private double y;
    private double z;

    public Location(double x, double y, double z) {
        this(null, x, y, z);
    }

    public Location(Object world, double x, double y, double z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public Object getWorld() {
        return world;
    }
}
