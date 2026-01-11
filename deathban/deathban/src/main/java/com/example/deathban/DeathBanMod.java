package com.example.deathban;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerDeathCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.PlayerManager;
import net.minecraft.text.Text;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DeathBanMod implements ModInitializer {

    private final Map<UUID, Long> bannedPlayers = new HashMap<>();
    private MinecraftServer server;
    private final Gson gson = new Gson();
    private File banFile;

    @Override
    public void onInitialize() {
        System.out.println("[DeathBan] Mod loaded");

        // Set up file location
        banFile = new File("config/deathbans.json");
        loadBans();

        // Listen to server ticks
        ServerTickEvents.START_SERVER_TICK.register(this::onServerTick);

        // Listen to player deaths
        PlayerDeathCallback.EVENT.register((player, damageSource) -> {
            onPlayerDeath(player);
            return true; // allow normal death
        });
    }

    private void onServerTick(MinecraftServer server) {
        this.server = server;

        PlayerManager playerManager = server.getPlayerManager();
        for (ServerPlayerEntity player : playerManager.getPlayerList()) {
            UUID uuid = player.getUuid();
            if (bannedPlayers.containsKey(uuid)) {
                long unbanTime = bannedPlayers.get(uuid);
                long now = Instant.now().getEpochSecond();
                if (now < unbanTime) {
                    // Kick player if still banned
                    player.networkHandler.disconnect(Text.literal(
                        "You died and are now banned for 3 days and will now become irrelevant to the plot due to your absence."
                    ));
                } else {
                    // Ban expired
                    bannedPlayers.remove(uuid);
                    saveBans();
                }
            }
        }
    }

    public void onPlayerDeath(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        long now = Instant.now().getEpochSecond();
        long threeDays = 3 * 24 * 60 * 60; // 3 days in seconds
        bannedPlayers.put(uuid, now + threeDays);
        saveBans();
        System.out.println("[DeathBan] Banned " + player.getName().getString() + " for 3 days.");
    }

    // Load bans from JSON file
    private void loadBans() {
        if (!banFile.exists()) return;

        try (FileReader reader = new FileReader(banFile)) {
            Type type = new TypeToken<Map<String, Long>>() {}.getType();
            Map<String, Long> data = gson.fromJson(reader, type);
            for (Map.Entry<String, Long> entry : data.entrySet()) {
                bannedPlayers.put(UUID.fromString(entry.getKey()), entry.getValue());
            }
            System.out.println("[DeathBan] Loaded " + bannedPlayers.size() + " bans from file.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Save bans to JSON file
    private void saveBans() {
        try {
            if (!banFile.getParentFile().exists()) banFile.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(banFile)) {
                Map<String, Long> data = new HashMap<>();
                for (Map.Entry<UUID, Long> entry : bannedPlayers.entrySet()) {
                    data.put(entry.getKey().toString(), entry.getValue());
                }
                gson.toJson(data, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

