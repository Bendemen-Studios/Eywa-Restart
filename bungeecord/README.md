# EywaRestart BungeeCord

A BungeeCord proxy plugin for the EywaMC network.

## Features
- Starts with `/restart` from console or a player with `eywarestart.use`.
- Network-wide countdown: 10, 3, 2, 1.
- Broadcasts directly through the proxy (no AdvancedBroadcast dependency).
- Stops the complete proxy gracefully after the countdown.
- Does not run `kickall`.

## Install
1. Build with `mvn -f bungeecord/pom.xml clean package`, or download the GitHub Actions artifact.
2. Put `EywaRestart-BungeeCord-1.0.0.jar` in the BungeeCord `plugins` folder.
3. Restart the proxy.
4. Grant `eywarestart.use` to the desired staff group.

## Important: automatic restart
This plugin gracefully stops the complete proxy using the BungeeCord API. Your process manager (for example Pterodactyl) must be configured to start the proxy again after it exits. A plugin cannot safely guarantee that the host starts a stopped Java process.
