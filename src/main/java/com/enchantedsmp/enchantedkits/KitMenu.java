package com.enchantedsmp.enchantedkits;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class KitMenu {

    public static void open(ServerPlayer player) {
        SimpleContainer inventory = new SimpleContainer(27);

        inventory.setItem(10, named(Items.COMMAND_BLOCK, "Owner Kit"));
        inventory.setItem(11, named(Items.NETHER_STAR, "Co-Owner Kit"));
        inventory.setItem(12, named(Items.ENCHANTED_GOLDEN_APPLE, "Admin Kit"));
        inventory.setItem(13, named(Items.SHIELD, "Mod Kit"));
        inventory.setItem(14, named(Items.AMETHYST_SHARD, "Media Kit"));
        inventory.setItem(15, named(Items.EMERALD, "Helper+ Kit"));
        inventory.setItem(16, named(Items.IRON_INGOT, "Helper Kit"));
        inventory.setItem(22, named(Items.WOODEN_SWORD, "Default Kit"));

        player.openMenu(new SimpleMenuProvider(
            (syncId, playerInventory, p) ->
                new KitChestMenu(syncId, playerInventory, inventory),
            Component.literal("EnchantedSMP Kits")
        ));
    }

    private static ItemStack named(Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    private static class KitChestMenu extends ChestMenu {

        KitChestMenu(int syncId, Inventory playerInventory,
                     SimpleContainer container) {
            super(MenuType.GENERIC_9x3, syncId, playerInventory, container, 3);
        }

        @Override
        public void clicked(int slot, int button,
                            ContainerInput input, Player player) {

            if (slot >= 0 && slot < 27) {
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.sendSystemMessage(
                        Component.literal("Kit selected! Claim system coming next.")
                    );
                }
                return;
            }

            super.clicked(slot, button, input, player);
        }
    }
}
