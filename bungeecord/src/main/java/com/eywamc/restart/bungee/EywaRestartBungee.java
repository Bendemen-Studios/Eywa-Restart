package com.eywamc.restart.bungee;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public final class EywaRestartBungee extends Plugin {
    private static final String CHANNEL = "eywarestart:restart";
    private static final String MESSAGE_TOKEN = "EywaMC-Paper-Restart-v1";
    private boolean restarting;
    private ScheduledTask countdownTask;

    @Override
    public void onEnable() {
        ProxyServer.getInstance().registerChannel(CHANNEL);
        ProxyServer.getInstance().getPluginManager().registerCommand(
                this,
                new Command("restart") {
                    @Override
                    public void execute(CommandSender sender, String[] args) {
                        // Player commands are routed to that player's current Paper server.
                        // Only the proxy console starts a BungeeCord restart.
                        if (sender instanceof ProxiedPlayer player) {
                            requestPaperRestart(player);
                        } else {
                            startRestart(sender);
                        }
                    }
                }
        );
        getLogger().info("EywaRestart BungeeCord enabled.");
    }

    @Override
    public void onDisable() {
        ProxyServer.getInstance().unregisterChannel(CHANNEL);
        if (countdownTask != null) {
            countdownTask.cancel();
        }
    }

    private void requestPaperRestart(ProxiedPlayer player) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeUTF(MESSAGE_TOKEN);
            }
            player.getServer().sendData(CHANNEL, bytes.toByteArray());
        } catch (IOException exception) {
            player.sendMessage(TextComponent.fromLegacyText(
                    "§cCould not send the restart request to your server."
            ));
            getLogger().warning("Could not encode Paper restart request: " + exception.getMessage());
        }
    }

    private void startRestart(CommandSender sender) {
        if (restarting) {
            sender.sendMessage(TextComponent.fromLegacyText("§cEywaMC is already restarting the proxy."));
            return;
        }

        restarting = true;
        broadcastCountdown(10);

        countdownTask = ProxyServer.getInstance().getScheduler().schedule(this, () -> {
            broadcastCountdown(3);
            countdownTask = ProxyServer.getInstance().getScheduler().schedule(this, () -> {
                broadcastCountdown(2);
                countdownTask = ProxyServer.getInstance().getScheduler().schedule(this, () -> {
                    broadcastCountdown(1);
                    countdownTask = ProxyServer.getInstance().getScheduler().schedule(
                            this,
                            this::stopProxy,
                            1,
                            TimeUnit.SECONDS
                    );
                }, 1, TimeUnit.SECONDS);
            }, 1, TimeUnit.SECONDS);
        }, 7, TimeUnit.SECONDS);
    }

    private void broadcastCountdown(int seconds) {
        String suffix = seconds == 10 ? " §7seconds" : "";
        String message = "§9§lEywaMC §8| §9Restarting §7in §9" + seconds + suffix;
        ProxyServer.getInstance().broadcast(TextComponent.fromLegacyText(message));
    }

    private void stopProxy() {
        getLogger().info("Stopping EywaMC BungeeCord proxy for restart.");
        ProxyServer.getInstance().broadcast(TextComponent.fromLegacyText(
                "§9§lEywaMC §8| §7The proxy is restarting. You will be able to reconnect shortly."
        ));
        ProxyServer.getInstance().stop();
    }
}
