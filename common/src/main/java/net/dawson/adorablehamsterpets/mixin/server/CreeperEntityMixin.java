package net.dawson.adorablehamsterpets.mixin.server;

import net.dawson.adorablehamsterpets.config.AcornFluteCreeperMode;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.flute.FlutePerformanceManager;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps a creeper harmlessly attentive while a qualifying Acorn Flute riff is playing nearby.
 * The target and ordinary movement goals remain untouched; only fuse state is suppressed.
 */
@Mixin(CreeperEntity.class)
public abstract class CreeperEntityMixin {

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Shadows and Synchronized State
     * ────────────────────────────────────────────────────────────────────────────*/

    @Shadow @Final private static TrackedData<Integer> FUSE_SPEED;
    @Shadow @Final private static TrackedData<Boolean> IGNITED;
    @Shadow private int lastFuseTime;
    @Shadow private int currentFuseTime;

    @Unique
    private static final TrackedData<Boolean> AHP_FUSE_SUPPRESSED =
            DataTracker.registerData(CreeperEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Tracker Initialization
     * ────────────────────────────────────────────────────────────────────────────*/

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void adorablehamsterpets$addFuseSuppressionTracker(CallbackInfo ci) {
        ((CreeperEntity) (Object) this).getDataTracker().startTracking(AHP_FUSE_SUPPRESSED, false);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Fuse Lifecycle
     * ────────────────────────────────────────────────────────────────────────────*/

    @Inject(method = "tick", at = @At("HEAD"))
    private void adorablehamsterpets$updateFuseSuppression(CallbackInfo ci) {
        CreeperEntity creeper = (CreeperEntity) (Object) this;
        DataTracker tracker = creeper.getDataTracker();

        // Server owns the area query; the tracker carries the result to client rendering.
        if (!creeper.getWorld().isClient()) {
            tracker.set(AHP_FUSE_SUPPRESSED, adorablehamsterpets$shouldSuppressFuse(creeper));
        }

        if (tracker.get(AHP_FUSE_SUPPRESSED)) {
            adorablehamsterpets$resetFuse(tracker);
        }
    }

    @Inject(method = "setFuseSpeed", at = @At("HEAD"), cancellable = true)
    private void adorablehamsterpets$blockSuppressedFuseSpeed(int fuseSpeed, CallbackInfo ci) {
        CreeperEntity creeper = (CreeperEntity) (Object) this;
        if (!creeper.getDataTracker().get(AHP_FUSE_SUPPRESSED)) return;

        adorablehamsterpets$resetFuse(creeper.getDataTracker());
        ci.cancel();
    }

    @Inject(method = "ignite", at = @At("HEAD"), cancellable = true)
    private void adorablehamsterpets$rejectSuppressedIgnition(CallbackInfo ci) {
        CreeperEntity creeper = (CreeperEntity) (Object) this;
        if (!creeper.getDataTracker().get(AHP_FUSE_SUPPRESSED)
                && (creeper.getWorld().isClient()
                || !adorablehamsterpets$shouldSuppressFuse(creeper))) {
            return;
        }

        adorablehamsterpets$resetFuse(creeper.getDataTracker());
        ci.cancel();
    }

    @Inject(method = "getClientFuseTime", at = @At("HEAD"), cancellable = true)
    private void adorablehamsterpets$hideSuppressedFuse(
            float tickDelta, CallbackInfoReturnable<Float> cir) {
        CreeperEntity creeper = (CreeperEntity) (Object) this;
        if (creeper.getDataTracker().get(AHP_FUSE_SUPPRESSED)) cir.setReturnValue(0.0F);
    }

    /* ──────────────────────────────────────────────────────────────────────────────
     *        Private Helpers
     * ────────────────────────────────────────────────────────────────────────────*/

    @Unique
    private static boolean adorablehamsterpets$shouldSuppressFuse(CreeperEntity creeper) {
        AcornFluteCreeperMode mode = Configs.AHP_MAIN.acornFluteCreeperMode.get();
        if (mode == null || !FlutePerformanceManager.isAffectedByNormalRiff(creeper)) return false;
        return mode == AcornFluteCreeperMode.ALL
                || (mode == AcornFluteCreeperMode.CHARGED_ONLY && creeper.shouldRenderOverlay());
    }

    @Unique
    private void adorablehamsterpets$resetFuse(DataTracker tracker) {
        tracker.set(FUSE_SPEED, -1);
        tracker.set(IGNITED, false);
        this.currentFuseTime = 0;
        this.lastFuseTime = 0;
    }
}
