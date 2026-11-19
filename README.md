# BoatBuff

A lightweight Paper plugin for **Minecraft 1.21.11** that gives boats gentle upward assistance when they meet a one-block obstacle.

## Behavior
- Works with the Bukkit/Paper boat entity, including boat variants.
- Preserves the boat's horizontal velocity; it does not teleport or dash the boat.
- Applies a small upward velocity only when the boat is moving into a solid block with headroom.
- Does not change normal player movement.

## Build
Requires Java 21 and Maven:
```bash
mvn -U clean package
```
The plugin JAR is created at `target/BoatBuff.jar`.

## Install
1. Build the project or download the JAR from the Actions artifact.
2. Put `BoatBuff.jar` in your server's `plugins` folder.
3. Restart Paper.
4. Edit `plugins/BoatBuff/config.yml` if you want to tune the step-up feel.

This is a first implementation; test it on a staging server before deploying to a live SMP.
