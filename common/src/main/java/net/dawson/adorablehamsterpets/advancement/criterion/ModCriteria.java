// --- ModCriteria.java ---
package net.dawson.adorablehamsterpets.advancement.criterion;


import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.advancement.criterion.Criterion;
import net.minecraft.registry.RegistryKeys;


public class ModCriteria {


    // --- 1. DeferredRegister for Criteria ---
    public static final DeferredRegister<Criterion<?>> CRITERIA = DeferredRegister.create(AdorableHamsterPets.MOD_ID, RegistryKeys.CRITERION);


    // --- 2. Change all fields to RegistrySuppliers ---
    public static final RegistrySupplier<HamsterOnShoulderCriterion> HAMSTER_ON_SHOULDER = CRITERIA.register("hamster_on_shoulder", HamsterOnShoulderCriterion::new);
    public static final RegistrySupplier<HamsterThrownCriterion> HAMSTER_THROWN = CRITERIA.register("hamster_thrown", HamsterThrownCriterion::new);
    public static final RegistrySupplier<FirstJoinCriterion> FIRST_JOIN_GUIDEBOOK_CHECK = CRITERIA.register("first_join_guidebook_check", FirstJoinCriterion::new);
    public static final RegistrySupplier<FedHamsterSteamedBeansCriterion> FED_HAMSTER_STEAMED_BEANS = CRITERIA.register("fed_hamster_steamed_beans", FedHamsterSteamedBeansCriterion::new);
    public static final RegistrySupplier<CheekPouchUnlockedCriterion> CHEEK_POUCH_UNLOCKED = CRITERIA.register("cheek_pouch_unlocked", CheekPouchUnlockedCriterion::new);
    public static final RegistrySupplier<AppliedPinkPetalCriterion> APPLIED_PINK_PETAL = CRITERIA.register("applied_pink_petal", AppliedPinkPetalCriterion::new);
    public static final RegistrySupplier<HamsterAutoFedCriterion> HAMSTER_AUTO_FED = CRITERIA.register("hamster_auto_fed", HamsterAutoFedCriterion::new);
    public static final RegistrySupplier<HamsterDiamondAlertCriterion> HAMSTER_DIAMOND_ALERT_TRIGGERED = CRITERIA.register("hamster_diamond_alert_triggered", HamsterDiamondAlertCriterion::new);
    public static final RegistrySupplier<HamsterCreeperAlertCriterion> HAMSTER_CREEPER_ALERT_TRIGGERED = CRITERIA.register("hamster_creeper_alert_triggered", HamsterCreeperAlertCriterion::new);
    public static final RegistrySupplier<HamsterPouchFilledCriterion> HAMSTER_POUCH_FILLED = CRITERIA.register("hamster_pouch_filled", HamsterPouchFilledCriterion::new);
    public static final RegistrySupplier<HamsterLedToDiamondCriterion> HAMSTER_LED_TO_DIAMOND = CRITERIA.register("hamster_led_to_diamond", HamsterLedToDiamondCriterion::new);
    public static final RegistrySupplier<HamsterFoundGoldCriterion> HAMSTER_FOUND_GOLD = CRITERIA.register("hamster_found_gold", HamsterFoundGoldCriterion::new);


    // --- 3. Main Registration Call ---
    public static void register() {
        CRITERIA.register();
        AdorableHamsterPets.LOGGER.info("Registering Mod Criteria for " + AdorableHamsterPets.MOD_ID);
    }
}