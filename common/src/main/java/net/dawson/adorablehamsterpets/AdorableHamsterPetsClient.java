package net.dawson.adorablehamsterpets;

import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.registry.client.level.entity.EntityModelLayerRegistry;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import net.dawson.adorablehamsterpets.accessor.PlayerEntityAccessor;
import net.dawson.adorablehamsterpets.block.ModBlocks;
import net.dawson.adorablehamsterpets.client.option.ModKeyBindings;
import net.dawson.adorablehamsterpets.client.sound.HamsterFlightSoundInstance;
import net.dawson.adorablehamsterpets.client.sound.HamsterThrowSoundInstance;
import net.dawson.adorablehamsterpets.config.AhpConfig;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.entity.client.HamsterRenderer;
import net.dawson.adorablehamsterpets.entity.client.ModModelLayers;
import net.dawson.adorablehamsterpets.entity.client.model.HamsterShoulderModel;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.dawson.adorablehamsterpets.item.ModItems;
import net.dawson.adorablehamsterpets.networking.payload.*;
import net.dawson.adorablehamsterpets.screen.HamsterInventoryScreen;
import net.dawson.adorablehamsterpets.screen.ModScreenHandlers;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.Set;

public class AdorableHamsterPetsClient {

    private static final Set<Integer> renderedHamsterIdsThisTick = new HashSet<>();
    private static final Set<Integer> renderedHamsterIdsLastTick = new HashSet<>();

    public static void init() {
        // --- Block Render Layers ---
        RenderTypeRegistry.register(RenderLayer.getCutout(), ModBlocks.GREEN_BEANS_CROP.get(), ModBlocks.CUCUMBER_CROP.get(), ModBlocks.SUNFLOWER_BLOCK.get(), ModBlocks.WILD_CUCUMBER_BUSH.get(), ModBlocks.WILD_GREEN_BEAN_BUSH.get());

        // --- Entity Rendering and Models ---
        EntityModelLayerRegistry.register(ModModelLayers.HAMSTER_SHOULDER_LAYER, HamsterShoulderModel::getTexturedModelData);
        EntityRendererRegistry.register(ModEntities.HAMSTER, HamsterRenderer::new);

        // --- Screens and Handlers ---
        MenuRegistry.registerScreenFactory(ModScreenHandlers.HAMSTER_INVENTORY_SCREEN_HANDLER.get(), HamsterInventoryScreen::new);

        // --- Keybinds ---
        ModKeyBindings.registerKeyInputs();

        // --- Client Tick Event Handler ---
        ClientTickEvent.CLIENT_POST.register(AdorableHamsterPetsClient::onEndClientTick);

        // --- Color Providers ---
        ColorHandlerRegistry.registerItemColors((stack, tintIndex) -> -1, ModItems.HAMSTER_SPAWN_EGG.get());

        // --- Guide Book Open Screen Event Handler ---
        InteractionEvent.RIGHT_CLICK_ITEM.register((player, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (player.getWorld().isClient && stack.isOf(ModItems.HAMSTER_GUIDE_BOOK.get())) {
                if (stack.contains(DataComponentTypes.WRITTEN_BOOK_CONTENT)) {
                    BookScreen.Contents contents = BookScreen.Contents.create(stack);
                    if (contents != null) {
                        MinecraftClient.getInstance().setScreen(new BookScreen(contents));
                        // Use CompoundEventResult to indicate success and consume the event
                        return CompoundEventResult.interrupt(true, stack);
                    }
                }
            }
            // Pass the event to allow other interactions
            return CompoundEventResult.pass();
        });
    }

    public static void onHamsterRendered(int entityId) {
        renderedHamsterIdsThisTick.add(entityId);
    }

    private static void onEndClientTick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            renderedHamsterIdsThisTick.clear();
            renderedHamsterIdsLastTick.clear();
            return;
        }

        if (ModKeyBindings.THROW_HAMSTER_KEY.wasPressed()) {
            final AhpConfig currentConfig = AdorableHamsterPets.CONFIG;
            if (!currentConfig.enableHamsterThrowing) {
                client.player.sendMessage(Text.literal("Hamster throwing is disabled in config."), true);
            } else {
                boolean lookingAtReachableBlock = client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.BLOCK;
                boolean hasShoulderHamsterClient = !((PlayerEntityAccessor) client.player).getHamsterShoulderEntity().isEmpty();

                if (!lookingAtReachableBlock && hasShoulderHamsterClient) {
                    dev.architectury.networking.NetworkManager.sendToServer(new ThrowHamsterPayload());
                }
            }
        }

        Set<Integer> stoppedRendering = new HashSet<>(renderedHamsterIdsLastTick);
        stoppedRendering.removeAll(renderedHamsterIdsThisTick);

        for (Integer entityId : stoppedRendering) {
            dev.architectury.networking.NetworkManager.sendToServer(new UpdateHamsterRenderStatePayload(entityId, false));
        }

        renderedHamsterIdsLastTick.clear();
        renderedHamsterIdsLastTick.addAll(renderedHamsterIdsThisTick);
        renderedHamsterIdsThisTick.clear();
    }

    public static void handleStartFlightSound(StartHamsterFlightSoundPayload payload) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;
        Entity entity = client.world.getEntityById(payload.hamsterEntityId());
        if (entity instanceof HamsterEntity hamster) {
            SoundEvent flightSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_FLYING_SOUNDS, hamster.getRandom());
            if (flightSound != null) {
                client.getSoundManager().play(new HamsterFlightSoundInstance(flightSound, SoundCategory.NEUTRAL, hamster));
            }
        }
    }

    public static void handleStartThrowSound(StartHamsterThrowSoundPayload payload) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;
        Entity entity = client.world.getEntityById(payload.hamsterEntityId());
        if (entity instanceof HamsterEntity hamster) {
            client.getSoundManager().play(new HamsterThrowSoundInstance(ModSounds.HAMSTER_THROW.get(), SoundCategory.PLAYERS, hamster));
        }
    }

    /**
     * Handles incoming sound requests from the server, triggered by animation keyframes.
     * <p>
     * This method is called on the client thread when a {@link HamsterAnimationSoundPayload}
     * is received. It looks up the specified hamster entity in the client's world and
     * plays a sound based on the provided sound ID. This approach ensures that all
     * client-side sound classes (like {@code PositionedSoundInstance}) are only ever
     * referenced on the client, preventing server-side crashes.
     *
     * @param payload The data packet containing the target hamster's entity ID and the
     *                string identifier for the sound to be played (e.g., "hamster_step_sound").
     */
    public static void handleAnimationSound(HamsterAnimationSoundPayload payload) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        Entity entity = client.world.getEntityById(payload.hamsterEntityId());
        if (!(entity instanceof HamsterEntity hamster)) return;

        switch (payload.soundId()) {
            case "hamster_step_sound":
                BlockPos pos = hamster.getBlockPos();
                BlockState blockState = hamster.getWorld().getBlockState(pos.down());

                if (blockState.isAir()) {
                    blockState = hamster.getWorld().getBlockState(pos.down(2));
                }
                if (!blockState.isAir()) {
                    BlockSoundGroup group = blockState.getSoundGroup();
                    SoundEvent stepSound = group.getStepSound();

                    float volume = blockState.isOf(Blocks.GRAVEL)
                            ? (0.10F * 0.60F)   // If GRAVEL, set the volume to 60% of the default (0.06F).
                            : 0.10F;            // If not GRAVEL, use the default volume (0.10F).
                    client.getSoundManager().play(
                            new PositionedSoundInstance(
                                    stepSound, SoundCategory.NEUTRAL, volume,
                                    group.getPitch() * 1.5F, hamster.getRandom(),
                                    hamster.getX(), hamster.getY(), hamster.getZ()
                            )
                    );
                }
                break;

            case "hamster_beg_bounce":
                SoundEvent bounceSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_BOUNCE_SOUNDS, hamster.getRandom());
                if (bounceSound != null) {
                    float basePitch = hamster.getSoundPitch();
                    float randomPitchAddition = hamster.getRandom().nextFloat() * 0.2f;
                    float finalPitch = (basePitch * 1.2f) + randomPitchAddition;

                    client.getSoundManager().play(
                            new PositionedSoundInstance(
                                    bounceSound, SoundCategory.NEUTRAL, 0.6f, finalPitch,
                                    hamster.getRandom(), hamster.getX(), hamster.getY(), hamster.getZ()
                            )
                    );
                }
                break;
        }
    }

    /**
     * Handles incoming particle requests from the server, triggered by animation keyframes.
     * <p>
     * This method is called on the client thread when a {@link HamsterAnimationParticlePayload}
     * is received. It finds the appropriate {@link HamsterRenderer} for the target entity
     * and calls a method on it to trigger the particle spawning logic in the next render frame.
     * This client-side handling is necessary because only the client can accurately calculate
     * the world position of an animated bone, which is then sent back to the server for
     * authoritative particle spawning.
     *
     * @param payload The data packet containing the target hamster's entity ID and the
     *                string identifier for the particle effect (e.g., "attack_poof").
     */
    public static void handleAnimationParticle(HamsterAnimationParticlePayload payload) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        Entity entity = client.world.getEntityById(payload.hamsterEntityId());
        if (!(entity instanceof HamsterEntity)) return;

        var renderer = client.getEntityRenderDispatcher().getRenderer(entity);
        if (renderer instanceof HamsterRenderer hamsterRenderer) {
            switch (payload.particleId()) {
                case "attack_poof":
                    hamsterRenderer.triggerAttackParticleSpawn();
                    break;
                case "seeking_dust":
                    hamsterRenderer.triggerSeekingDustSpawn();
                    break;
            }
        }
    }
}