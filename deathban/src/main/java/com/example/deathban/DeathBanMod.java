package com.example.deathban;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.network.chat.Component;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.players.NameAndId; 

import java.time.Instant;
import java.util.Date;

public class DeathBanMod implements ModInitializer {

    private static final long BAN_DURATION_SECONDS = 3L * 24 * 60 * 60;

    @Override
    public void onInitialize() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) {
                
                MinecraftServer server = player.level().getServer();
                if (server == null) return;

                Instant expires = Instant.now().plusSeconds(BAN_DURATION_SECONDS);
                GameProfile profile = player.getGameProfile();

                // FIX: Swap the parameters. 
                // NameAndId in 1.21.11 is defined as (UUID id, String name)
                NameAndId target = new NameAndId(profile.id(), profile.name());

                UserBanListEntry ban = new UserBanListEntry(
                        target,
                        new Date(),
                        "DeathBan",
                        Date.from(expires),
                        "You died and lost your plot relevancy."
                );

                server.getPlayerList().getBans().add(ban);

                player.connection.disconnect(
                        Component.literal("You died and are banned for 3 days.\nYour temporary absence will result in you losing your relevancy to the plot.")
                );
            }
        });
    }
}

