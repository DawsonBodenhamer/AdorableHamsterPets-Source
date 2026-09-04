package net.dawson.adorablehamsterpets.entity;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.util.collection.DefaultedList;

public interface ImplementedInventory extends Inventory {

    DefaultedList<ItemStack> getItems();

    static DefaultedList<ItemStack> create(int size) {
        return DefaultedList.ofSize(size, ItemStack.EMPTY);
    }

    @Override
    default int size() {
        return getItems().size();
    }

    @Override
    default boolean isEmpty() {
        for (int i = 0; i < size(); i++) {
            ItemStack stack = getStack(i);
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    default ItemStack getStack(int slot) {
        return getItems().get(slot);
    }

    @Override
    default ItemStack removeStack(int slot, int amount) {
        // Use helper method that respects the 'amount' parameter.
        ItemStack result = Inventories.splitStack(getItems(), slot, amount);
        if (!result.isEmpty()) {
            markDirty();
        }
        return result;
    }

    @Override
    default ItemStack removeStack(int slot) {
        markDirty();
        return Inventories.removeStack(getItems(), slot);
    }

    @Override
    default void setStack(int slot, ItemStack stack) {
        getItems().set(slot, stack);
        markDirty();
    }

    @Override
    default void clear() {
        getItems().clear();
        markDirty();
    }

    @Override
    default void markDirty() {
        // Client-side inventories do not need to be saved.
    }

    @Override
    default int getMaxCountPerStack() {
        return 64;
    }

    @Override
    default void onOpen(PlayerEntity player) {
    }

    @Override
    default void onClose(PlayerEntity player) {
    }

    @Override
    default boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    @Override
    default boolean isValid(int slot, ItemStack stack) {
        return true;
    }

    default PropertyDelegate getPropertyDelegate() {
        return new ArrayPropertyDelegate(0);
    }
}