package net.dawson.adorablehamsterpets;

import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.registry.client.level.entity.EntityModelLayerRegistry;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import io.netty.buffer.Unpooled;
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
import net.dawson.adorablehamsterpets.networking.ModPackets;
import net.dawson.adorablehamsterpets.screen.HamsterInventoryScreen;
import net.dawson.adorablehamsterpets.screen.ModScreenHandlers;
import net.dawson.adorablehamsterpets.sound.ModSounds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.hit.HitResult;

import java.util.HashSet;
import java.util.Set;

public class AdorableHamsterPetsClient {

    private static final Set<Integer> renderedHamsterIdsThisTick = new HashSet<>();
    private static final Set<Integer> renderedHamsterIdsLastTick = new HashSet<>();

    /**
     * Initializes general client-side features like screens, keybinds, and events.
     */
    public static void init() {
        RenderTypeRegistry.register(RenderLayer.getCutout(), ModBlocks.GREEN_BEANS_CROP.get(), ModBlocks.CUCUMBER_CROP.get(), ModBlocks.SUNFLOWER_BLOCK.get(), ModBlocks.WILD_CUCUMBER_BUSH.get(), ModBlocks.WILD_GREEN_BEAN_BUSH.get());
        ModPackets.registerS2CPackets();
        ClientTickEvent.CLIENT_POST.register(AdorableHamsterPetsClient::onEndClientTick);
        ColorHandlerRegistry.registerItemColors((stack, tintIndex) -> -1, ModItems.HAMSTER_SPAWN_EGG.get());


        InteractionEvent.RIGHT_CLICK_ITEM.register((player, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (player.getWorld().isClient && stack.isOf(ModItems.HAMSTER_GUIDE_BOOK.get())) {
                // Check if the book has content before trying to open it.
                if (stack.hasNbt() && stack.getNbt().contains("pages")) {
                    // Directly create WrittenBookContents to bypass the vanilla item check.
                    MinecraftClient.getInstance().setScreen(new BookScreen(new BookScreen.WrittenBookContents(stack)));
                    return CompoundEventResult.interrupt(true, stack);
                }
            }
            return CompoundEventResult.pass();
        });
    }

    /**
     * Registers keybindings. Separate because Forge calls this from the dedicated RegisterKeyMappingsEvent.
     */
    public static void initKeybindings() {
        ModKeyBindings.registerKeyInputs();
    }

    /**
     * Registers the screen factory. Separate because Forge needs to call it natively.
     */
    public static void initScreenHandlers() {
        MenuRegistry.registerScreenFactory(ModScreenHandlers.HAMSTER_INVENTORY_SCREEN_HANDLER.get(), HamsterInventoryScreen::new);
    }

    /**
     * Registers entity renderers. Called from a dedicated event handler.
     */
    public static void initEntityRenderers() {
        EntityRendererRegistry.register(ModEntities.HAMSTER, HamsterRenderer::new);
    }

    /**
     * Registers entity model layer definitions. Called from a dedicated event handler.
     */
    public static void initModelLayers() {
        EntityModelLayerRegistry.register(ModModelLayers.HAMSTER_SHOULDER_LAYER, HamsterShoulderModel::getTexturedModelData);
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
                    // Send an empty buffer for the throw packet
                    dev.architectury.networking.NetworkManager.sendToServer(ModPackets.THROW_HAMSTER_ID, new PacketByteBuf(Unpooled.buffer()));
                }
            }
        }

        Set<Integer> stoppedRendering = new HashSet<>(renderedHamsterIdsLastTick);
        stoppedRendering.removeAll(renderedHamsterIdsThisTick);

        for (Integer entityId : stoppedRendering) {
            // Send the render state update packet
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeInt(entityId);
            buf.writeBoolean(false); // isRendering = false
            dev.architectury.networking.NetworkManager.sendToServer(ModPackets.UPDATE_HAMSTER_RENDER_STATE_ID, buf);
        }

        renderedHamsterIdsLastTick.clear();
        renderedHamsterIdsLastTick.addAll(renderedHamsterIdsThisTick);
        renderedHamsterIdsThisTick.clear();
    }


    public static void handleStartFlightSound(int hamsterEntityId) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;
        Entity entity = client.world.getEntityById(hamsterEntityId);
        if (entity instanceof HamsterEntity hamster) {
            SoundEvent flightSound = ModSounds.getRandomSoundFrom(ModSounds.HAMSTER_FLYING_SOUNDS, hamster.getRandom());
            if (flightSound != null) {
                client.getSoundManager().play(new HamsterFlightSoundInstance(flightSound, SoundCategory.NEUTRAL, hamster));
            }
        }
    }

    public static void handleStartThrowSound(int hamsterEntityId) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;
        Entity entity = client.world.getEntityById(hamsterEntityId);
        if (entity instanceof HamsterEntity hamster) {
            client.getSoundManager().play(new HamsterThrowSoundInstance(ModSounds.HAMSTER_THROW.get(), SoundCategory.PLAYERS, hamster));
        }
    }
}