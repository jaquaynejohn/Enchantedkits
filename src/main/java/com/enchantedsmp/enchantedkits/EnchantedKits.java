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
                .then(Commands.literal("rank")
                    .executes(context -> {

                        var player = context.getSource().getPlayerOrException();

                        var luckPerms = LuckPermsProvider.get();
                        User user = luckPerms.getUserManager().getUser(player.getUUID());

                        if (user == null) {
                            context.getSource().sendSuccess(
                                () -> Component.literal("Your LuckPerms data is not loaded."),
                                false
                            );
                            return 0;
                        }

                        String rank = user.getPrimaryGroup();

                        context.getSource().sendSuccess(
                            () -> Component.literal("Your EnchantedSMP rank: " + rank),
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
