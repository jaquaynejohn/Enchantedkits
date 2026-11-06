
package com.enchantedsmp.enchantedkits;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonParser;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
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

import com.mojang.serialization.JsonOps;

import java.nio.file.Files;
import java.nio.file.Path;

public class KitEditor {

    private static final String[] RANKS = {
        "owner", "coowner", "admin", "mod",
        "media", "helperplus", "helper", "default"
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

    private static final Path DIRECTORY =
        FabricLoader.getInstance().getConfigDir()
            .resolve("enchantedkits/kits");

    private static final Gson GSON =
        new GsonBuilder().setPrettyPrinting().create();

    public static boolean isOperator(ServerPlayer player) {
        return player.level().getServer()
            .getPlayerList()
            .isOp(new NameAndId(player.getGameProfile()));
    }

    public static void open(ServerPlayer player) {
        if (!isOperator(player)) {
            player.sendSystemMessage(
                Component.literal("You cannot use the Kit Editor!")
            );
            return;
        }

        SimpleContainer inventory = new SimpleContainer(27);

        for (int i = 0; i < RANKS.length; i++) {
            inventory.setItem(
                10 + i,
                named(ICONS[i], "§6Edit " + RANKS[i] + " kit")
            );
        }

        player.openMenu(new SimpleMenuProvider(
            (id, playerInventory, p) ->
                new RankMenu(id, playerInventory, inventory),
            Component.literal("EnchantedKits Editor")
        ));
    }

    private static void openKit(ServerPlayer player, String rank) {
        if (!isOperator(player)) return;

        SimpleContainer inventory = new SimpleContainer(54);
        loadItems(player, rank, inventory);

        inventory.setItem(
            49,
            named(Items.BARRIER, "§cReset Custom Items")
        );

        inventory.setItem(
            53,
            named(Items.LIME_DYE, "§aSave and Close")
        );

        player.openMenu(new SimpleMenuProvider(
            (id, playerInventory, p) ->
                new EditorMenu(id, playerInventory, inventory, rank),
            Component.literal("Editing " + rank)
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

    private static Path getFile(String rank) {
        return DIRECTORY.resolve(rank + ".json");
    }

    private static void loadItems(
        ServerPlayer player,
        String rank,
        SimpleContainer inventory
    ) {
        Path file = getFile(rank);

        if (!Files.exists(file)) return;

        try {
            JsonArray array;

            try (var reader = Files.newBufferedReader(file)) {
                array = JsonParser.parseReader(reader).getAsJsonArray();
            }

            var ops = player.level().registryAccess()
                .createSerializationContext(JsonOps.INSTANCE);

            for (int i = 0; i < Math.min(45, array.size()); i++) {
                final int slot = i;

                if (array.get(i).isJsonNull()) continue;

                ItemStack.CODEC.parse(ops, array.get(i))
                    .result()
                    .ifPresent(stack ->
                        inventory.setItem(slot, stack)
                    );
            }

        } catch (Exception exception) {
            player.sendSystemMessage(
                Component.literal("Could not load saved kit.")
            );
            exception.printStackTrace();
        }
    }

    private static void saveItems(
        ServerPlayer player,
        String rank,
        SimpleContainer inventory
    ) {
        try {
            Files.createDirectories(DIRECTORY);

            JsonArray array = new JsonArray();

            var ops = player.level().registryAccess()
                .createSerializationContext(JsonOps.INSTANCE);

            for (int i = 0; i < 45; i++) {
                ItemStack stack = inventory.getItem(i);

                if (stack.isEmpty()) {
                    array.add(JsonNull.INSTANCE);
                } else {
                    array.add(
                        ItemStack.CODEC.encodeStart(ops, stack)
                            .getOrThrow()
                    );
                }
            }

            try (var writer =
                     Files.newBufferedWriter(getFile(rank))) {
                GSON.toJson(array, writer);
            }

            player.sendSystemMessage(
                Component.literal("§a" + rank + " kit saved!")
            );

        } catch (Exception exception) {
            player.sendSystemMessage(
                Component.literal("§cCould not save kit.")
            );
            exception.printStackTrace();
        }
    }

    public static void giveSavedItems(
        ServerPlayer player,
        String rank
    ) {
        SimpleContainer inventory = new SimpleContainer(54);
        loadItems(player, rank, inventory);

        for (int i = 0; i < 45; i++) {
            ItemStack stack = inventory.getItem(i);

            if (!stack.isEmpty()) {
                player.getInventory().add(stack.copy());
            }
        }
    }

    private static class RankMenu extends ChestMenu {

        RankMenu(
            int id,
            Inventory playerInventory,
            SimpleContainer inventory
        ) {
            super(MenuType.GENERIC_9x3, id, playerInventory, inventory, 3);
        }

        @Override
        public void clicked(
            int slot,
            int button,
            ContainerInput input,
            Player player
        ) {
            if (!(player instanceof ServerPlayer sp)) return;

            if (!isOperator(sp)) {
                sp.closeContainer();
                return;
            }

            if (slot >= 10 && slot <= 17) {
                openKit(sp, RANKS[slot - 10]);
                return;
            }

            if (slot >= 0 && slot < 27) return;

            super.clicked(slot, button, input, player);
        }
    }

    private static class EditorMenu extends ChestMenu {

        private final String rank;
        private final SimpleContainer inventory;

        EditorMenu(
            int id,
            Inventory playerInventory,
            SimpleContainer inventory,
            String rank
        ) {
            super(MenuType.GENERIC_9x6, id, playerInventory, inventory, 6);
            this.rank = rank;
            this.inventory = inventory;
        }

        @Override
        public void clicked(
            int slot,
            int button,
            ContainerInput input,
            Player player
        ) {
            if (!(player instanceof ServerPlayer sp)) return;

            if (!isOperator(sp)) {
                sp.closeContainer();
                return;
            }

            if (slot == 49) {
                for (int i = 0; i < 45; i++) {
                    inventory.setItem(i, ItemStack.EMPTY);
                }

                saveItems(sp, rank, inventory);
                sp.sendSystemMessage(
                    Component.literal("§cCustom kit items reset!")
                );
                return;
            }

            if (slot == 53) {
                saveItems(sp, rank, inventory);
                sp.closeContainer();
                return;
            }

            if (slot >= 45 && slot < 54) return;

            super.clicked(slot, button, input, player);
        }

        @Override
        public void removed(Player player) {
            super.removed(player);

            if (player instanceof ServerPlayer sp && isOperator(sp)) {
                saveItems(sp, rank, inventory);
            }
        }
    }
}

