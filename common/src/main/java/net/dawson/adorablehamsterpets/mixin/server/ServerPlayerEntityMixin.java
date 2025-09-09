package net.dawson.adorablehamsterpets.mixin.server;

import com.mojang.authlib.GameProfile;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.HashSet;
import java.util.OptionalInt;
import java.util.Set;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity {

    // Required constructor for the mixin to compile
    public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    /**
     * Injects into the moment a player opens any handled screen.
     * Scans the container's inventory for outdated Hamster Tips guide books and
     * upgrades them on the fly.
     */
    @Inject(
            method = "openHandledScreen",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerPlayerEntity;onScreenHandlerOpened(Lnet/minecraft/screen/ScreenHandler;)V"
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void adorablehamsterpets$onOpenScreen(@Nullable NamedScreenHandlerFactory factory, CallbackInfoReturnable<OptionalInt> cir, ScreenHandler screenHandler) {
        // --- 1. Collect all unique inventories associated with this screen ---
        // This prevents from scanning the same inventory multiple times.
        Set<Inventory> inventories = new HashSet<>();
        for (var slot : screenHandler.slots) {
            // The slot.inventory can be null for special slots, so we check.
            if (slot.inventory != null) {
                inventories.add(slot.inventory);
            }
        }

        // --- 2. Run the upgrader on each unique inventory ---
        for (Inventory inventory : inventories) {
            AdorableHamsterPets.replaceOldBooksInInventory(inventory);
        }
    }
}