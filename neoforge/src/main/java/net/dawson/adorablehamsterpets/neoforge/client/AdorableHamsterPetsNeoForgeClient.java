package net.dawson.adorablehamsterpets.neoforge.client;


import net.dawson.adorablehamsterpets.AdorableHamsterPetsClient;
import net.dawson.adorablehamsterpets.client.option.ModKeyBindings;
import net.dawson.adorablehamsterpets.entity.ModEntities;
import net.dawson.adorablehamsterpets.entity.client.HamsterRenderer;
import net.dawson.adorablehamsterpets.entity.client.ModModelLayers;
import net.dawson.adorablehamsterpets.entity.client.feature.HamsterShoulderFeatureRenderer;
import net.dawson.adorablehamsterpets.entity.client.model.HamsterShoulderModel;
import net.dawson.adorablehamsterpets.screen.ModScreenHandlers;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.SkinTextures;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.dawson.adorablehamsterpets.screen.HamsterInventoryScreen;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;


public final class AdorableHamsterPetsNeoForgeClient {


    private AdorableHamsterPetsNeoForgeClient() {}


    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // General setup.
        event.enqueueWork(AdorableHamsterPetsClient::init);
    }

    /**
     * Register key mappings using the NeoForge event.
     */
    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        // Construct the key mapping if it hasn’t been created yet
        if (ModKeyBindings.THROW_HAMSTER_KEY == null) {
            ModKeyBindings.init();
        }
        // Use the event to register the key mapping
        event.register(ModKeyBindings.THROW_HAMSTER_KEY);
    }

    /**
     * Listens for the RegisterMenuScreensEvent to safely register our custom screen factory.
     * This is the correct time to do this on NeoForge.
     * @param event The screen registration event.
     */
    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModScreenHandlers.HAMSTER_INVENTORY_SCREEN_HANDLER.get(), HamsterInventoryScreen::new);
    }


    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.HAMSTER.get(), HamsterRenderer::new);
    }


    /**
     * Listens for the RegisterLayerDefinitions event to safely register our custom model layers.
     * This is the correct time to do this on NeoForge.
     * @param event The layer definition registration event.
     */
    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        // NATIVELY register the model layer
        event.registerLayerDefinition(ModModelLayers.HAMSTER_SHOULDER_LAYER, HamsterShoulderModel::getTexturedModelData);
    }


    /**
     * Listens for the AddLayers event to safely add the shoulder hamster feature renderer
     * to the default and slim player models. This event runs after layer definitions are registered.
     * @param event The layer addition event.
     */
    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        // Get the renderers for both the default ("wide") and "slim" player models
        LivingEntityRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> defaultSkin = event.getSkin(SkinTextures.Model.WIDE);
        LivingEntityRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> slimSkin = event.getSkin(SkinTextures.Model.SLIM);


        if (defaultSkin != null) {
            defaultSkin.addFeature(new HamsterShoulderFeatureRenderer(defaultSkin, event.getContext().getModelLoader()));
        }


        if (slimSkin != null) {
            slimSkin.addFeature(new HamsterShoulderFeatureRenderer(slimSkin, event.getContext().getModelLoader()));
        }
    }
}