package com.eywamc.restart;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public final class EywaRestart extends JavaPlugin implements Listener, PluginMessageListener {
    private static final String PERMISSION = "eywarestart.use";
    private static final String CHANNEL = "eywarestart:restart";
    private static final String MESSAGE_TOKEN = "EywaMC-Paper-Restart-v1";
    private boolean restarting;
    private BukkitTask countdownTask;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getMessenger().registerIncomingPluginChannel(this, CHANNEL, this);
        getLogger().info("EywaRestart enabled. Permission: " + PERMISSION);
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterIncomingPluginChannel(this, CHANNEL, this);
        if (countdownTask != null) {
            countdownTask.cancel();
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!CHANNEL.equals(channel) || player == null) return;

        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(message))) {
            if (!MESSAGE_TOKEN.equals(input.readUTF())) return;
            handleRestart(player);
        } catch (IOException exception) {
            getLogger().warning("Received an invalid restart request from the proxy.");
        }
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage().substring(1).trim();
        if (!isRestartCommand(command)) return;

        event.setCancelled(true);
        handleRestart(event.getPlayer());
    }

    @EventHandler
    public void onConsoleCommand(ServerCommandEvent event) {
        if (!isRestartCommand(event.getCommand().trim())) return;

        event.setCancelled(true);
        handleRestart(event.getSender());
    }

    private boolean isRestartCommand(String rawCommand) {
        if (rawCommand.isBlank()) return false;
        String label = rawCommand.split("\\s+", 2)[0];
        if (label.startsWith("/")) label = label.substring(1);
        return label.equalsIgnoreCase("restart");
    }

    private void handleRestart(CommandSender sender) {
        if (sender instanceof Player player && !player.isOp() && !player.hasPermission(PERMISSION)) {
            player.sendMessage("§cYou do not have permission. Required permission: " + PERMISSION);
            return;
        }

        if (restarting) {
            sender.sendMessage("§cEywaMC is already restarting.");
            return;
        }

        restarting = true;
        broadcastCountdown(10);
        countdownTask = Bukkit.getScheduler().runTaskLater(this, () -> {
            broadcastCountdown(3);
            countdownTask = Bukkit.getScheduler().runTaskLater(this, () -> {
                broadcastCountdown(2);
                countdownTask = Bukkit.getScheduler().runTaskLater(this, () -> {
                    broadcastCountdown(1);
                    countdownTask = Bukkit.getScheduler().runTaskLater(this, this::performRestart, 20L);
                }, 20L);
            }, 20L);
        }, 7L * 20L);
    }

    private void broadcastCountdown(int seconds) {
        String message = "ap broadcast all <blue><b>EywaMC</blue> <dark_gray>|</dark_gray> <blue>Restarting <gray>in <blue>"
                + seconds + (seconds == 10 ? " <gray>seconds" : "");
        if (!Bukkit.dispatchCommand(Bukkit.getConsoleSender(), message)) {
            getLogger().warning("AdvancedBroadcast command failed: " + message);
        }
    }

    private void performRestart() {
        getLogger().info("Executing EywaMC restart sequence...");
        dispatchConsole("save-all");
        // Stop only this Paper server. The hosting panel/AutoPlug must restart this process.
        Bukkit.shutdown();
    }

    private void dispatchConsole(String command) {
        if (!Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)) {
            getLogger().warning("Command failed or was not found: " + command);
        }
    }
}
