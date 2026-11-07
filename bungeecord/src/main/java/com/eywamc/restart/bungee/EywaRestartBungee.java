package com.eywamc.restart.bungee;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;

import java.util.concurrent.TimeUnit;

public final class EywaRestartBungee extends Plugin {
    private static final String PERMISSION = "eywarestart.use";
    private boolean restarting;
    private ScheduledTask countdownTask;

    @Override
    public void onEnable() {
        ProxyServer.getInstance().getPluginManager().registerCommand(
                this,
                new Command("restart", PERMISSION) {
                    @Override
                    public void execute(CommandSender sender, String[] args) {
                        startRestart(sender);
                    }
                }
        );
        getLogger().info("EywaRestart BungeeCord enabled.");
    }

    @Override
    public void onDisable() {
        if (countdownTask != null) {
            countdownTask.cancel();
        }
    }

    private void startRestart(CommandSender sender) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(TextComponent.fromLegacyText("§cJe hebt geen toestemming om de proxy te herstarten."));
            return;
        }

        if (restarting) {
            sender.sendMessage(TextComponent.fromLegacyText("§cEywaMC is al bezig met een proxy-restart."));
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
                "§9§lEywaMC §8| §7De proxy wordt opnieuw gestart. Je kunt zo weer verbinden."
        ));
        ProxyServer.getInstance().stop();
    }
}
