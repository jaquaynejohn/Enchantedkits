
package com.enchantedsmp.enchantedkits;

import net.fabricmc.loader.api.FabricLoader;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

public class KitClaimManager {

    private static final long COOLDOWN =
        24L * 60L * 60L * 1000L;

    private static final Set<String> RANKS = Set.of(
        "owner", "coowner", "admin", "mod",
        "media", "helperplus", "helper", "default"
    );

    private static final Path FILE =
        FabricLoader.getInstance().getConfigDir()
            .resolve("enchantedkits-cooldowns.properties");

    private static final Properties COOLDOWNS = new Properties();

    static {
        load();
    }

    public static void claimArmor(
        ServerPlayer player,
        String selectedRank
    ) {
        String rank = selectedRank.toLowerCase(Locale.ROOT);

        if (!RANKS.contains(rank)) {
            player.sendSystemMessage(
                Component.literal("Invalid kit.")
            );
            return;
        }

        boolean isOp = KitEditor.isOperator(player);

        User user = LuckPermsProvider.get()
            .getUserManager()
            .getUser(player.getUUID());

        if (user == null && !isOp) {
            player.sendSystemMessage(
                Component.literal("Your LuckPerms data is not loaded.")
            );
            return;
        }

        String playerRank = user == null ? "" :
            user.getPrimaryGroup().toLowerCase(Locale.ROOT);

        if (!isOp && !playerRank.equals(rank)) {
            player.sendSystemMessage(
                Component.literal(
                    "You can only claim your own rank kit!"
                )
            );
            return;
        }

        boolean noCooldown =
            rank.equals("owner") ||
            rank.equals("coowner") ||
            rank.equals("admin");

        String key = player.getUUID() + ":" + rank;
        long now = System.currentTimeMillis();

        if (!noCooldown) {
            long nextClaim = Long.parseLong(
                COOLDOWNS.getProperty(key, "0")
            );

            if (nextClaim > now) {
                long remaining = (nextClaim - now) / 1000;
                long hours = remaining / 3600;
                long minutes = (remaining % 3600) / 60;

                player.sendSystemMessage(
                    Component.literal(
                        "Kit cooldown: " + hours + "h "
                            + minutes + "m remaining."
                    )
                );
                return;
            }
        }

        player.level().getServer().getCommands()
            .performPrefixedCommand(
                player.createCommandSourceStack(),
                "function enchantedkits:kitarmor_" + rank
            );

        KitEditor.giveSavedItems(player, rank);

        if (!noCooldown) {
            COOLDOWNS.setProperty(
                key,
                String.valueOf(now + COOLDOWN)
            );
            save();
        }

        player.sendSystemMessage(
            Component.literal("You claimed the " + rank + " kit!")
        );
    }

    private static void load() {
        try {
            if (Files.exists(FILE)) {
                try (InputStream input = Files.newInputStream(FILE)) {
                    COOLDOWNS.load(input);
                }
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private static void save() {
        try {
            Files.createDirectories(FILE.getParent());

            try (OutputStream output = Files.newOutputStream(FILE)) {
                COOLDOWNS.store(
                    output,
                    "EnchantedKits cooldowns"
                );
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}
