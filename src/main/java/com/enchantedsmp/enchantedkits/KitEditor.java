
package com.enchantedsmp.enchantedkits;

import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Locale;

public class KitMenu {

    private static final String[] RANKS = {
        "owner", "coowner", "admin", "mod",
        "media", "helperplus", "helper", "default"
    };

    private static final String[] NAMES = {
        "Owner Kit", "Co-Owner Kit", "Admin Kit", "Mod Kit",
        "Media Kit", "Helper+ Kit", "Helper Kit", "Default Kit"
    };

    private static final Item[] ICONS = {
        Items.COMMAND_BLOCK,
        Items.NETHER_STAR,
        Items.ENCHANTED_GOLDEN_APPLE,
        Items.SHIELD,
        Items.AMETHYST_SHARD,
        Items.EMERALD,
        Items.IRON_INGOT,
        Items.WOODEN_SWORD
    };

    private static final int[] SLOTS = {
        10, 11, 12, 13, 14, 15, 16, 22
    };

    public static void open(ServerPlayer player) {

        SimpleContainer inventory = new SimpleContainer(27);

        boolean isOp = KitEditor.isOperator(player);

        User user = LuckPermsProvider.get()
            .getUserManager()
            .getUser(player.getUUID());

        String playerRank = user == null ? "" :
            user.getPrimaryGroup().toLowerCase(Locale.ROOT);

        for (int i = 0; i < RANKS.length; i++) {

            boolean unlocked = isOp || playerRank.equals(RANKS[i]);

            ItemStack icon;

            if (unlocked) {
                icon = named(ICONS[i], "§a" + NAMES[i]);
            } else {
                icon = named(
                    Items.BARRIER,
                    "§c✖ " + NAMES[i] + " (Locked)"
                );
            }

            inventory.setItem(SLOTS[i], icon);
        }

        if (isOp) {
            inventory.setItem(
                26,
                named(Items.ANVIL, "§6Kit Editor (OP Only)")
            );
        }

        player.openMenu(new SimpleMenuProvider(
            (syncId, playerInventory, p) ->
                new KitChestMenu(
                    syncId,
                    playerInventory,
                    inventory
                ),
            Component.literal("EnchantedSMP Kits")
        ));
    }

    private static ItemStack named(Item item, String name) {
        ItemStack stack = new ItemStack(item);

        stack.set(
            DataComponents.CUSTOM_NAME,
            Component.literal(name)
        );

        return stack;
    }

    private static class KitChestMenu extends ChestMenu {

        KitChestMenu(
            int syncId,
            Inventory playerInventory,
            SimpleContainer container
        ) {
            super(
                MenuType.GENERIC_9x3,
                syncId,
                playerInventory,
                container,
                3
            );
        }

        @Override
        public void clicked(
            int slot,
            int button,
            ContainerInput input,
            Player player
        ) {

            if (slot >= 0 && slot < 27) {

                if (!(player instanceof ServerPlayer serverPlayer)) {
                    return;
                }

                boolean isOp = KitEditor.isOperator(serverPlayer);

                if (slot == 26 && isOp) {
                    serverPlayer.closeContainer();
                    KitEditor.open(serverPlayer);
                    return;
                }

                for (int i = 0; i < RANKS.length; i++) {

                    if (slot == SLOTS[i]) {

                        User user = LuckPermsProvider.get()
                            .getUserManager()
                            .getUser(serverPlayer.getUUID());

                        String playerRank = user == null ? "" :
                            user.getPrimaryGroup().toLowerCase(Locale.ROOT);

                        if (!isOp && !playerRank.equals(RANKS[i])) {
                            serverPlayer.sendSystemMessage(
                                Component.literal(
                                    "§c✖ You cannot claim this rank's kit!"
                                )
                            );
                            return;
                        }

                        serverPlayer.closeContainer();

                        KitClaimManager.claimArmor(
                            serverPlayer,
                            RANKS[i]
                        );

                        return;
                    }
                }

                return;
            }

            super.clicked(slot, button, input, player);
        }
    }
}
