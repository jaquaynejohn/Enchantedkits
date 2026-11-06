package com.enchantedsmp.enchantedkits;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;

public class EnchantedKits implements ModInitializer {

    @Override
    public void onInitialize() {

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            dispatcher.register(Commands.literal("kit")
                .executes(context -> {
                    var player = context.getSource().getPlayerOrException();

                    var luckPerms = LuckPermsProvider.get();
                    User user = luckPerms.getUserManager().getUser(player.getUUID());

                    if (user == null) {
                        context.getSource().sendSuccess(
                            () -> Component.literal("Your rank could not be loaded."),
                            false
                        );
                        return 0;
                    }

                    String rank = user.getPrimaryGroup();

                    context.getSource().sendSuccess(
                        () -> Component.literal(
                            "§6§lEnchantedSMP Kits\n" +
                            "§7Your Rank: §e" + rank + "\n" +
                            "§7Available command: §a/kit claim\n" +
                            "§8More kit options coming soon!"
                        ),
                        false
                    );

                    return 1;
                })

                .then(Commands.literal("rank")
                    .executes(context -> {
                        var player = context.getSource().getPlayerOrException();
                        var luckPerms = LuckPermsProvider.get();
                        User user = luckPerms.getUserManager().getUser(player.getUUID());

                        if (user == null) return 0;

                        context.getSource().sendSuccess(
                            () -> Component.literal(
                                "Your EnchantedSMP rank: " + user.getPrimaryGroup()
                            ),
                            false
                        );

                        return 1;
                    })
                )
            );
        });

        System.out.println("EnchantedKits loaded!");
    }
}
