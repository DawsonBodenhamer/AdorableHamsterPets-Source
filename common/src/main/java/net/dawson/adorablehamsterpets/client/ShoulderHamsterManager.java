package net.dawson.adorablehamsterpets.client;

import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.entity.ShoulderLocation;
import net.dawson.adorablehamsterpets.entity.client.feature.ShoulderHamsterState;
import net.dawson.adorablehamsterpets.entity.client.renderer.ShoulderHamsterRenderer;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Manages the client-side state of "dummy" hamster entities used for shoulder rendering.
 * This class ensures all state mutations occur on the main client thread via a tick event.
 */
public class ShoulderHamsterManager {
    // --- Caches for Dummy Entities and their States ---
    private static final Map<ShoulderLocation, HamsterEntity> dummyHamsters = new EnumMap<>(ShoulderLocation.class);
    private static final Map<ShoulderLocation, ShoulderHamsterRenderer> hamsterRenderers = new EnumMap<>(ShoulderLocation.class);
    private static final Map<ShoulderLocation, ShoulderHamsterState> hamsterStates = new EnumMap<>(ShoulderLocation.class);

    /**
     * Initializes the dummy entities and renderers.
     * This is called lazily the first time a shoulder pet is detected.
     */
    private static void initializeDummies() {
        World world = MinecraftClient.getInstance().world;
        if (world == null) return;

        EntityRendererFactory.Context context = new EntityRendererFactory.Context(
                MinecraftClient.getInstance().getEntityRenderDispatcher(),
                MinecraftClient.getInstance().getItemRenderer(),
                MinecraftClient.getInstance().getBlockRenderManager(),
                MinecraftClient.getInstance().getEntityRenderDispatcher().getHeldItemRenderer(),
                MinecraftClient.getInstance().getResourceManager(),
                MinecraftClient.getInstance().getEntityModelLoader(),
                MinecraftClient.getInstance().textRenderer
        );

        for (ShoulderLocation location : ShoulderLocation.values()) {
            HamsterEntity dummy = new HamsterEntity(ModEntities.HAMSTER.get(), world);
            dummy.setNoGravity(true);
            dummy.setSilent(true);
            dummyHamsters.put(location, dummy);
            hamsterRenderers.put(location, new ShoulderHamsterRenderer(context));
        }
    }

    /**
     * The main tick loop for managing shoulder pet states.
     * This method is called from a client tick event.
     */
    public static void clientTick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            // Clear states if player is not in world
            if (!hamsterStates.isEmpty()) hamsterStates.clear();
            return;
        }

        PlayerEntityAccessor playerAccessor = (PlayerEntityAccessor) client.player;
        if (!playerAccessor.hasAnyShoulderHamster()) {
            if (!hamsterStates.isEmpty()) hamsterStates.clear();
            return;
        }

        // Lazy initialization
        if (dummyHamsters.isEmpty()) {
            initializeDummies();
        }

        // Tick the state for each occupied slot
        for (ShoulderLocation location : ShoulderLocation.values()) {
            NbtCompound shoulderNbt = playerAccessor.getShoulderHamster(location);
            if (!shoulderNbt.isEmpty()) {
                ShoulderHamsterState state = hamsterStates.computeIfAbsent(location, l -> new ShoulderHamsterState());
                HamsterEntity dummy = dummyHamsters.get(location);
                if (dummy != null) {
                    // This is the core logic: update the state machine on the main client thread.
                    state.tick(dummy, client.player.isSprinting());
                }
            } else {
                // Remove state for empty slots
                hamsterStates.remove(location);
            }
        }
    }

    /**
     * Retrieves the cached dummy entity for a given shoulder location.
     * Used by the feature renderer.
     */
    @Nullable
    public static HamsterEntity getDummy(ShoulderLocation location) {
        return dummyHamsters.get(location);
    }

    /**
     * Retrieves the cached specialized renderer for a given shoulder location.
     * Used by the feature renderer.
     */
    @Nullable
    public static ShoulderHamsterRenderer getRenderer(ShoulderLocation location) {
        return hamsterRenderers.get(location);
    }
}