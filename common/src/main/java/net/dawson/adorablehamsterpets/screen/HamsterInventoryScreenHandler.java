package net.dawson.adorablehamsterpets.screen;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.screen.slot.HamsterSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.jetbrains.annotations.Nullable;

public class HamsterInventoryScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    @Nullable
    private final HamsterEntity hamsterEntityInstance;

    // This is now the single constructor for both server and client.
    // The client receives the entity instance via the extended menu factory.
    public HamsterInventoryScreenHandler(int syncId, PlayerInventory playerInventory, @Nullable HamsterEntity hamsterEntity) {
        // CORRECTED: Call .get() on the RegistrySupplier
        super(ModScreenHandlers.HAMSTER_INVENTORY_SCREEN_HANDLER.get(), syncId);

        if (hamsterEntity != null) {
            this.inventory = hamsterEntity;
            this.hamsterEntityInstance = hamsterEntity;
            checkSize(this.inventory, 6);
        } else {
            // Fallback for client if entity is somehow not found
            AdorableHamsterPets.LOGGER.warn("Could not find HamsterEntity on client, using empty inventory.");
            this.inventory = new SimpleInventory(6);
            this.hamsterEntityInstance = null;
        }

        this.inventory.onOpen(playerInventory.player);
        setupSlots(playerInventory);
    }

    private void setupSlots(PlayerInventory playerInventory) {
        // Hamster Cheek Pouch Slots
        this.addSlot(new HamsterSlot(this.inventory, 0, 26, 95));
        this.addSlot(new HamsterSlot(this.inventory, 1, 44, 95));
        this.addSlot(new HamsterSlot(this.inventory, 2, 62, 95));
        this.addSlot(new Slot(new SimpleInventory(1), 0, 80, 95) { // Gap Slot
            @Override public boolean canInsert(ItemStack stack) { return false; }
            @Override public boolean canTakeItems(PlayerEntity playerEntity) { return false; }
            @Override public boolean isEnabled() { return false; }
        });
        this.addSlot(new HamsterSlot(this.inventory, 3, 98, 95));
        this.addSlot(new HamsterSlot(this.inventory, 4, 116, 95));
        this.addSlot(new HamsterSlot(this.inventory, 5, 134, 95));

        // Player Inventory & Hotbar
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 140 + i * 18));
            }
        }
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 198));
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        ItemStack itemStack = ItemStack.EMPTY;
        // CORRECTED: Removed redundant cast and null check
        Slot slot = this.slots.get(slotIndex);
        if (slot.hasStack()) {
            ItemStack itemStack2 = slot.getStack();
            itemStack = itemStack2.copy();

            int hamsterInvSize = 6;
            int gapSlotIndex = 3;
            int totalHamsterAreaSlots = hamsterInvSize + 1;

            // CORRECTED: Inlined redundant local variable
            if (slotIndex < totalHamsterAreaSlots && slotIndex != gapSlotIndex) {
                if (!this.insertItem(itemStack2, totalHamsterAreaSlots, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotIndex >= totalHamsterAreaSlots) {
                if (this.hamsterEntityInstance != null && this.hamsterEntityInstance.isItemDisallowed(itemStack2)) {
                    return ItemStack.EMPTY;
                }
                if (!this.insertItem(itemStack2, 0, gapSlotIndex, false)) {
                    if (!this.insertItem(itemStack2, gapSlotIndex + 1, totalHamsterAreaSlots, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            } else {
                return ItemStack.EMPTY;
            }

            if (itemStack2.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }

            if (itemStack2.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTakeItem(player, itemStack2);
        }

        return itemStack;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        this.inventory.onClose(player);
    }
}