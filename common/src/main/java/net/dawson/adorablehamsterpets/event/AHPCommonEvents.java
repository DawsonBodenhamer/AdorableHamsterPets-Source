package net.dawson.adorablehamsterpets.event;

import dev.architectury.event.events.common.PlayerEvent;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.mixin.accessor.SlotAccessor;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

import java.util.HashSet;
import java.util.Set;

/**
 * Central handler for common, cross-loader events.
 */
public class AHPCommonEvents {

    /**
     * Initializes and registers all common event listeners.
     */
    public static void init() {
        PlayerEvent.OPEN_MENU.register(AHPCommonEvents::onOpenMenu);
    }

    /**
     * An event listener that fires whenever a player opens any menu (inventory, chest, etc.).
     * It scans all unique inventories within the menu and upgrades any outdated guide books.
     *
     * @param player The player opening the menu.
     * @param menu The menu being opened.
     */
    private static void onOpenMenu(PlayerEntity player, ScreenHandler menu) {
        if (player.getWorld().isClient()) {
            return;
        }

        // Use a Set to avoid scanning the same inventory multiple times
        Set<Inventory> inventories = new HashSet<>();
        for (Slot slot : menu.slots) {
            // Use the Mixin Accessor to get the inventory object.
            // This works on both Fabric (inventory) and NeoForge (container) thanks to the remapper.
            Inventory inv = ((SlotAccessor) slot).adorablehamsterpets$getInventory();
            if (inv != null) {
                inventories.add(inv);
            }
        }

        // Run the upgrade logic on each unique inventory found.
        for (Inventory inv : inventories) {
            AdorableHamsterPets.replaceOldBooksInInventory(inv);
        }
    }
}