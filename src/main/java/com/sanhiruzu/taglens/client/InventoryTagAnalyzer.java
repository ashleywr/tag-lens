package com.sanhiruzu.taglens.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class InventoryTagAnalyzer {
    private static final Comparator<TagCount> TAG_ORDER = Comparator
            .comparingInt(TagCount::matches)
            .reversed()
            .thenComparing(entry -> entry.id().toString());

    private InventoryTagAnalyzer() {
    }

    static Analysis analyze(AbstractContainerScreen<?> screen, Inventory playerInventory) {
        List<Slot> slots = selectSlots(screen, playerInventory);
        Map<ResourceLocation, Integer> counts = new HashMap<>();
        int occupiedSlots = 0;

        for (Slot slot : slots) {
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) {
                continue;
            }

            occupiedSlots++;
            stack.getTags().forEach(tag -> counts.merge(tag.location(), 1, Integer::sum));
        }

        List<TagCount> tags = counts.entrySet().stream()
                .map(entry -> new TagCount(entry.getKey(), entry.getValue()))
                .sorted(TAG_ORDER)
                .toList();
        return new Analysis(occupiedSlots, tags);
    }

    private static List<Slot> selectSlots(AbstractContainerScreen<?> screen, Inventory playerInventory) {
        List<Slot> menuSlots = screen.getMenu().slots;
        if (screen instanceof InventoryScreen) {
            return menuSlots.stream()
                    .filter(slot -> slot.container == playerInventory)
                    .toList();
        }

        List<Slot> containerSlots = new ArrayList<>();
        for (Slot slot : menuSlots) {
            if (slot.container != playerInventory) {
                containerSlots.add(slot);
            }
        }

        // Some modded menus proxy the player inventory through a wrapper. If every
        // slot looks external, analyzing all of them is more useful than hiding the panel.
        return containerSlots.isEmpty() ? menuSlots : containerSlots;
    }

    record Analysis(int occupiedSlots, List<TagCount> tags) {
    }

    record TagCount(ResourceLocation id, int matches) {
    }
}
