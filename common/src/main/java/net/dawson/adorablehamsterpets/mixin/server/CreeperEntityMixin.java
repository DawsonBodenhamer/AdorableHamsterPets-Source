package net.dawson.adorablehamsterpets.mixin.server;

import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreeperEntity.class)
public abstract class CreeperEntityMixin extends HostileEntity {

    // Required constructor for the mixin to compile
    protected CreeperEntityMixin(net.minecraft.entity.EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    /**
     * Injects into the end of the Creeper's goal initialization to add a new FleeEntityGoal.
     * This goal makes the Creeper flee from any nearby HamsterEntity, mimicking its behavior with cats.
     */
    @Inject(method = "initGoals", at = @At("TAIL"))
    private void adorablehamsterpets$addFleeHamsterGoal(CallbackInfo ci) {
        // Priority 3 is the same as the vanilla Cat/Ocelot flee goal.
        // The speeds (1.0, 1.2) and distance (6.0F) also match the vanilla implementation.
        this.goalSelector.add(3, new FleeEntityGoal<>(this, HamsterEntity.class, 6.0F, 1.0, 1.2));
    }
}