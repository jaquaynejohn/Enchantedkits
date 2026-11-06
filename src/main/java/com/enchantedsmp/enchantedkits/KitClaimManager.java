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

public class KitClaimManager {

    private static final long COOLDOWN = 24L * 60 * 60 * 1000;

    private static final Path FILE = FabricLoader.getInstance()
        .getConfigDir().resolve("enchantedkits-cooldowns.properties");

    private static final Properties COOLDOWNS = new Properties();

    private static final String[] RANKS = {
        "owner", "coowner", "admin", "mod",
        "media", "helperplus", "helper", "default"
    };

    static {
        load();
    }

    public static void claimArmor(ServerPlayer player, String selectedRank) {

        selectedRank = selectedRank.toLowerCase(Locale.ROOT);

        if (!java.util.Arrays.asList(RANKS).contains(selectedRank)) {
            player.sendSystemMessage(Component.literal("Invalid kit."));
            return;
        }

        boolean isOp = player.level().getServer().getPlayerList()
    .isOp(player.getGameProfile());

        User user = LuckPermsProvider.get()
            .getUserManager().getUser(player.getUUID());

        if (user == null && !isOp) {
            player.sendSystemMessage(Component.literal(
                "Your LuckPerms data is not loaded."
            ));
            return;
        }

        String playerRank = user == null ? "" :
            user.getPrimaryGroup().toLowerCase(Locale.ROOT);

        if (!isOp && !playerRank.equals(selectedRank)) {
            player.sendSystemMessage(Component.literal(
                "You can only claim your own rank kit!"
            ));
            return;
        }

        boolean noCooldown =
            selectedRank.equals("owner") ||
            selectedRank.equals("coowner") ||
            selectedRank.equals("admin");

        String key = player.getUUID() + ":" + selectedRank;
        long now = System.currentTimeMillis();

        if (!noCooldown) {
            long nextClaim = Long.parseLong(
                COOLDOWNS.getProperty(key, "0")
            );

            if (nextClaim > now) {
                long remaining = (nextClaim - now) / 1000;
                long hours = remaining / 3600;
                long minutes = (remaining % 3600) / 60;

                player.sendSystemMessage(Component.literal(
                    "Your kit is on cooldown! Time remaining: "
                    + hours + "h " + minutes + "m"
                ));
                return;
            }
        }

        int result = player.getServer().getCommands()
            .performPrefixedCommand(
                player.createCommandSourceStack(),
                "function enchantedkits:kitarmor_" + selectedRank
            );

        if (result <= 0) {
            player.sendSystemMessage(Component.literal(
                "The kit could not be given. Check that its function exists."
            ));
            return;
        }

        if (!noCooldown) {
            COOLDOWNS.setProperty(
                key, String.valueOf(now + COOLDOWN)
            );
            save();
        }

        player.sendSystemMessage(Component.literal(
            "You claimed the " + selectedRank + " kit!"
        ));
    }

    private static void load() {
        try {
            if (Files.exists(FILE)) {
                try (InputStream input = Files.newInputStream(FILE)) {
                    COOLDOWNS.load(input);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void save() {
        try {
            Files.createDirectories(FILE.getParent());

            try (OutputStream output = Files.newOutputStream(FILE)) {
                COOLDOWNS.store(output, "EnchantedKits cooldowns");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
