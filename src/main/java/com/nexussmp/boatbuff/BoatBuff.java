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
        getLogger().info("BoatBuff enabled - smooth one-block step-ups are active.");
    }

    private void loadSettings() {
        stepVelocity = Math.max(0.05, Math.min(getConfig().getDouble("step-up-velocity", 0.13), 0.20));
        minimumSpeed = getConfig().getDouble("minimum-horizontal-speed", 0.035);
        lookAhead = getConfig().getDouble("look-ahead", 0.78);
        requireHeadroom = getConfig().getBoolean("require-headroom", true);
    }

    private void tickBoats() {
        if (!getConfig().getBoolean("enabled", true)) return;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Boat boat && boat.isValid() && !boat.isDead()) assistBoat(boat);
            }
        }
    }

    private void assistBoat(Boat boat) {
        Vector velocity = boat.getVelocity();
        if (Math.hypot(velocity.getX(), velocity.getZ()) < minimumSpeed) return;

        Vector forward = new Vector(velocity.getX(), 0, velocity.getZ()).normalize();
        Vector side = new Vector(-forward.getZ(), 0, forward.getX()).multiply(0.30);
        BoundingBox box = boat.getBoundingBox();
        double feetY = box.getMinY() + 0.08;
        Location center = boat.getLocation().clone().add(forward.clone().multiply(lookAhead));
        Location left = center.clone().add(side);
        Location right = center.clone().subtract(side);

        boolean blocked = isOneBlockStep(boat.getWorld(), center.getX(), feetY, center.getZ())
                || isOneBlockStep(boat.getWorld(), left.getX(), feetY, left.getZ())
                || isOneBlockStep(boat.getWorld(), right.getX(), feetY, right.getZ());
        if (!blocked) return;

        // Gentle vertical assistance only: keep vanilla horizontal momentum intact.
        boat.setVelocity(new Vector(velocity.getX(), Math.max(velocity.getY(), stepVelocity), velocity.getZ()));
    }

    private boolean isOneBlockStep(World world, double x, double feetY, double z) {
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
