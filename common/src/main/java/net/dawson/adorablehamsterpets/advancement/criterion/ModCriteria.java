package net.dawson.adorablehamsterpets.advancement.criterion;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.advancement.criterion.Criterion;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModCriteria {
    public static final HamsterOnShoulderCriterion HAMSTER_ON_SHOULDER = register("hamster_on_shoulder", new HamsterOnShoulderCriterion());
    public static final HamsterThrownCriterion HAMSTER_THROWN = register("hamster_thrown", new HamsterThrownCriterion());
    public static final FirstJoinCriterion FIRST_JOIN_GUIDEBOOK_CHECK = register("first_join_guidebook_check", new FirstJoinCriterion());
    public static final FedHamsterSteamedBeansCriterion FED_HAMSTER_STEAMED_BEANS = register("fed_hamster_steamed_beans", new FedHamsterSteamedBeansCriterion());
    public static final CheekPouchUnlockedCriterion CHEEK_POUCH_UNLOCKED = register("cheek_pouch_unlocked", new CheekPouchUnlockedCriterion());
    public static final AppliedPinkPetalCriterion APPLIED_PINK_PETAL = register("applied_pink_petal", new AppliedPinkPetalCriterion());
    public static final HamsterAutoFedCriterion HAMSTER_AUTO_FED = register("hamster_auto_fed", new HamsterAutoFedCriterion());
    public static final HamsterDiamondAlertCriterion HAMSTER_DIAMOND_ALERT_TRIGGERED = register("hamster_diamond_alert_triggered", new HamsterDiamondAlertCriterion());
    public static final HamsterCreeperAlertCriterion HAMSTER_CREEPER_ALERT_TRIGGERED = register("hamster_creeper_alert_triggered", new HamsterCreeperAlertCriterion());
    public static final HamsterPouchFilledCriterion HAMSTER_POUCH_FILLED = register("hamster_pouch_filled", new HamsterPouchFilledCriterion());
    public static final HamsterLedToDiamondCriterion HAMSTER_LED_TO_DIAMOND = register("hamster_led_to_diamond", new HamsterLedToDiamondCriterion());


    private static <T extends Criterion<?>> T register(String name, T criterion) {
        return Registry.register(Registries.CRITERION, Identifier.of(AdorableHamsterPets.MOD_ID, name), criterion);
    }

    /**
     * Registers all custom criteria for the mod.
     */
    public static void registerCriteria() {
        AdorableHamsterPets.LOGGER.info("Registering Mod Criteria for " + AdorableHamsterPets.MOD_ID);
        // Static initializers handle the actual registration.
    }
}