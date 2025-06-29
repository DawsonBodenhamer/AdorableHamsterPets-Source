package net.dawson.adorablehamsterpets.accessor;

import net.minecraft.nbt.NbtCompound;

/**
 * Accessor interface to expose custom methods injected into PlayerEntity by PlayerEntityMixin.
 * This allows other parts of the mod to safely call these methods without illegally
 * referencing the mixin class directly.
 */
public interface PlayerEntityAccessor {
    NbtCompound getHamsterShoulderEntity();
    void setHamsterShoulderEntity(NbtCompound nbt);

    int ahp_getLastGoldMessageIndex();
    void ahp_setLastGoldMessageIndex(int index);
}