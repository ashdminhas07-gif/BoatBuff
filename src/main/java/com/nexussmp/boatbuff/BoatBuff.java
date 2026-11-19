package com.nexussmp.boatbuff;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public final class BoatBuff extends JavaPlugin {
    private double stepVelocity;
    private double minimumSpeed;
    private double lookAhead;
    private boolean requireHeadroom;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();
        Bukkit.getScheduler().runTaskTimer(this, this::tickBoats, 1L, 1L);
        getLogger().info("BoatBuff enabled - obstacle step assistance is active.");
    }

    private void loadSettings() {
        stepVelocity = Math.max(0.20, Math.min(getConfig().getDouble("step-up-velocity", 0.34), 0.42));
        minimumSpeed = getConfig().getDouble("minimum-horizontal-speed", 0.035);
        lookAhead = Math.max(0.35, Math.min(getConfig().getDouble("look-ahead", 0.55), 0.85));
        requireHeadroom = getConfig().getBoolean("require-headroom", true);
    }

    private void tickBoats() {
        if (!getConfig().getBoolean("enabled", true)) return;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Boat boat && boat.isValid() && !boat.isDead()) {
                    assistBoat(boat);
                }
            }
        }
    }

    private void assistBoat(Boat boat) {
        Vector velocity = boat.getVelocity();
        double horizontalSpeed = Math.hypot(velocity.getX(), velocity.getZ());
        if (horizontalSpeed < minimumSpeed) return;

        // Do not keep boosting a boat while it is already rising over an obstacle.
        if (velocity.getY() > 0.035) return;

        Vector forward = new Vector(velocity.getX(), 0, velocity.getZ()).normalize();
        Vector side = new Vector(-forward.getZ(), 0, forward.getX()).multiply(0.34);
        BoundingBox box = boat.getBoundingBox();

        // Sample just in front of the boat's nose, at the bottom of its collision box.
        double feetY = box.getMinY() + 0.12;
        Location front = boat.getLocation().clone().add(forward.multiply(lookAhead));
        Location[] samples = {
                front,
                front.clone().add(side),
                front.clone().subtract(side)
        };

        for (Location sample : samples) {
            if (isObstacle(boat.getWorld(), sample.getX(), feetY, sample.getZ())) {
                // Preserve vanilla horizontal momentum; only add the vertical step impulse.
                boat.setVelocity(new Vector(velocity.getX(), stepVelocity, velocity.getZ()));
                return;
            }
        }
    }

    private boolean isObstacle(World world, double x, double feetY, double z) {
        int bx = (int) Math.floor(x);
        int by = (int) Math.floor(feetY);
        int bz = (int) Math.floor(z);

        Block obstacle = world.getBlockAt(bx, by, bz);
        if (obstacle.isPassable() || obstacle.isLiquid()) return false;

        if (requireHeadroom) {
            Block above = world.getBlockAt(bx, by + 1, bz);
            if (!above.isPassable() || above.isLiquid()) return false;
        }
        return true;
    }
}
