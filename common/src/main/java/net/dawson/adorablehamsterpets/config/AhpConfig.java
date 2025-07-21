package net.dawson.adorablehamsterpets.config;

import me.fzzyhmstrs.fzzy_config.annotations.ClientModifiable;
import me.fzzyhmstrs.fzzy_config.annotations.RootConfig;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.config.ConfigAction;
import me.fzzyhmstrs.fzzy_config.config.ConfigGroup;
import me.fzzyhmstrs.fzzy_config.screen.widget.TextureIds;
import me.fzzyhmstrs.fzzy_config.util.Translatable;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Root-level, single-file config for Adorable Hamster Pets.
 */
@Translatable.Name("Adorable Hamster Pets")
@Translatable.Desc("Questionable Configuration Options")
@RootConfig
public class AhpConfig extends Config {

    public AhpConfig() {
        super(Identifier.of(AdorableHamsterPets.MOD_ID, "main"));
    }

    // --- Help & Other Distractions ---
    @Translatable.Name("Help & Other Distractions")
    @Translatable.Desc("Buttons for when you’re lost, bored, or met a bug that’s not just existential hamster angst.")
    public ConfigGroup helpAndResources = new ConfigGroup("helpAndResources", false);

    @ClientModifiable
    @Translatable.Name("I Lost My Book!")
    @Translatable.Desc("Misplaced your invaluable tome of rodent wisdom? Click here. I won't tell anyone.")
    public ConfigAction giveGuideBook = new ConfigAction.Builder()
            .title(Text.translatable("config.adorablehamsterpets.main.helpAndResources.giveGuideBook"))
            .desc(Text.translatable("config.adorablehamsterpets.main.helpAndResources.giveGuideBook.desc"))
            .decoration(TextureIds.INSTANCE.getDECO_BOOK())
            .build(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                    "/function adorablehamsterpets:technical/give_guide_book"));

    @ClientModifiable
    @Translatable.Name("Report a Bug")
    @Translatable.Desc("Found a game-breaking issue? Or a hamster phasing through the floor? Let me know on Github. The more details, the better. And believe it or not, I do check this frequently.")
    public ConfigAction reportBug = new ConfigAction.Builder()
            .title(Text.translatable("config.adorablehamsterpets.main.helpAndResources.reportBug"))
            .desc(Text.translatable("config.adorablehamsterpets.main.helpAndResources.reportBug.desc"))
            .decoration(TextureIds.INSTANCE.getDECO_LINK())
            .build(new ClickEvent(ClickEvent.Action.OPEN_URL,
                    "https://github.com/DawsonBodenhamer/Adorable-Hamster-Pets-1.21/issues"));

    @ClientModifiable
    @Translatable.Name("Join Discord")
    @Translatable.Desc("Join 'The Hamster Pouch' official Discord server. A place to share screenshots, get support, or just witness the ongoing development chaos. You're invited.")
    public ConfigAction joinDiscord = new ConfigAction.Builder()
            .title(Text.translatable("config.adorablehamsterpets.main.helpAndResources.joinDiscord"))
            .desc(Text.translatable("config.adorablehamsterpets.main.helpAndResources.joinDiscord.desc"))
            .decoration(TextureIds.INSTANCE.getDECO_BUTTON_CLICK())
            .build(new ClickEvent(ClickEvent.Action.OPEN_URL,
                    "https://discord.gg/w54mk5bqdf"));

    @ClientModifiable
    @ConfigGroup.Pop
    @Translatable.Name("Visit My Website")
    @Translatable.Desc("Shameless plug for my other, less-rodent-focused work. Click if you dare.")
    public ConfigAction visitWebsite = new ConfigAction.Builder()
            .title(Text.translatable("config.adorablehamsterpets.main.helpAndResources.visitWebsite"))
            .desc(Text.translatable("config.adorablehamsterpets.main.helpAndResources.visitWebsite.desc"))
            .decoration(TextureIds.INSTANCE.getDECO_LINK())
            .build(new ClickEvent(ClickEvent.Action.OPEN_URL,
                    "https://www.fortheking.design"));

    // --- UI & Quality of Life ---
    @Translatable.Name("UI & Quality of Life")
    @Translatable.Desc("Because Sanity is Overrated")
    public ConfigGroup uiPreferences = new ConfigGroup("uiPreferences", true);

    @ClientModifiable
    @Translatable.Name("Enable Auto Guidebook Delivery")
    @Translatable.Desc("Hand-delivers the sacred texts on first login. Read it—or don’t. I'm not your conscience.")
    public boolean enableAutoGuidebookDelivery = true;

    @ClientModifiable
    @Translatable.Name("Enable Mod Item Tooltips")
    @Translatable.Desc("Helpful whispers on what the heck that cucumber is for.")
    public boolean enableItemTooltips = true;

    @ClientModifiable
    @Translatable.Name("Enable Shoulder Dismount Messages")
    @Translatable.Desc("Little status mumbles when your co-pilot disembarks.")
    public boolean enableShoulderDismountMessages = true;

    @ClientModifiable
    @ConfigGroup.Pop
    @Translatable.Name("Enable Jade Hamster Debug Info")
    @Translatable.Desc("More stats than anyone asked for. Defaults to off—mercifully.")
    public boolean enableJadeHamsterDebugInfo = false;

    // --- Core Feature Toggles ---
    @Translatable.Name("Core Feature Toggles")
    @Translatable.Desc("Fundamental hamster hijinks— fiddle at your own risk.")
    public ConfigGroup core = new ConfigGroup("core", true);

    @Translatable.Name("Enable Hamster Throwing")
    @Translatable.Desc("Do we yeet the hamster? ('G' by default).")
    public boolean enableHamsterThrowing = true;

    @Translatable.Name("Require Food Mix to Unlock Cheeks")
    @Translatable.Desc("Gate cheek-pouch storage behind gourmet cuisine, because drama.")
    public boolean requireFoodMixToUnlockCheeks = true;

    @ClientModifiable
    @ConfigGroup.Pop
    @Translatable.Name("Use 'Hampter' as Default Name")
    @Translatable.Desc("Changes the default entity name from 'Hamster' to 'Hampter'. Note: This has no visible effect in vanilla Minecraft, as mobs don't show nameplates by default. It's primarily for use with mods like Auto Leveling that display entity names.")
    public boolean useHampterName = false;

    // --- Core Cooldown Settings ---
    @Translatable.Name("Core Cooldown Settings")
    @Translatable.Desc("Mandatory hamster union breaks between heroic stunts.")
    public ConfigGroup cooldowns = new ConfigGroup("cooldowns", true);

    @Translatable.Name("Cleaning Frequency")
    @Translatable.Desc("How often a sitting hamster gets the sudden urge to clean. It's a 1-in-X chance per tick, so lower numbers mean a higher chance for cleaning. For example, 1200 means on average, it'll clean about once a minute. 300 ≈ every 15 secs, and 5000 ≈ every 4 mins. Congratulations— now you know enough to be dangerous.")
    public ValidatedInt cleaningChanceDenominator = new ValidatedInt(1200, 5000, 300);

    @Translatable.Name("Throw Cooldown (Ticks)")
    @Translatable.Desc("Time-out after using your living projectile. (20 ticks = 1 s)")
    public ValidatedInt hamsterThrowCooldown = new ValidatedInt(2400, 20 * 60 * 10, 20);

    @Translatable.Name("Green Bean Buff Cooldown (Ticks)")
    @Translatable.Desc("When the sugar rush ends, force a breather. (20 ticks = 1 s)")
    public ValidatedInt steamedGreenBeansBuffCooldown = new ValidatedInt(6000, 20 * 60 * 10, 20);

    @Translatable.Name("Enable Diamond Seeking Cooldown?")
    @Translatable.Desc("Force a cool-down after striking it rich. Off by default, since this can't happen again anyway without another mount/dismount on the shoulder.")
    public boolean enableIndependentDiamondSeekCooldown = false;

    @Translatable.Name("Diamond Seeking Cooldown (Ticks)")
    @Translatable.Desc("Cooldown before your hamster can go on another treasure hunt. (20 ticks = 1 s)")
    public ValidatedInt independentOreSeekCooldownTicks = new ValidatedInt(2400, 6000, 20);

    @Translatable.Name("Diamond Thievery Cooldown (Ticks)")
    @Translatable.Desc("Mandatory time-out after a successful heist to prevent serial kleptomania. (20 ticks = 1s). WARNING: Increasing this cooldown can dramatically change the diamond stealing mechanic, since that AI goal sometimes re-runs multiple times in a row when the hamster has trouble pathfinding to the item that it wants to steal. So instead of increasing this, you should probably just stop dropping your diamonds on the ground everywhere, butter fingers.")
    public ValidatedInt stealCooldownTicks = new ValidatedInt(100, 6000, 20);

    @ConfigGroup.Pop
    @Translatable.Name("Breeding Cooldown (Ticks)")
    @Translatable.Desc("Hamsters need their space. (20 ticks = 1 s)")
    public ValidatedInt breedingCooldownTicks = new ValidatedInt(6000, 24000, 600);

    // --- Spawn Settings ---
    @Translatable.Name("Spawn Settings")
    @Translatable.Desc("How Many, How Often?")
    public ConfigGroup hamsterSpawning = new ConfigGroup("hamsterSpawning", true);

    @Translatable.Name("Spawn Weight")
    @Translatable.Desc("Adjusts hamster spawn frequency. Higher = more chaos. 1 = blissful silence.")
    public ValidatedInt spawnWeight = new ValidatedInt(30, 100, 1);

    @ConfigGroup.Pop
    @Translatable.Name("Max Group Size")
    @Translatable.Desc("Maximum hamsters per spawn group. Because sometimes one just isn't cute enough.")
    public ValidatedInt maxGroupSize = new ValidatedInt(1, 10, 1);

    // --- Taming & Breeding Settings ---
    @Translatable.Name("Taming & Breeding Settings")
    @Translatable.Desc("Convince a hamster to love you—and occasionally accept a roommate.")
    public ConfigGroup tamingAndBreeding = new ConfigGroup("tamingAndBreeding", true);

    @ConfigGroup.Pop
    @Translatable.Name("Taming Chance")
    @Translatable.Desc("Taming difficulty (1 in X chance). Higher = more cucumbers sacrificed to fuzzy freeloaders.")
    public ValidatedInt tamingChanceDenominator = new ValidatedInt(3, 20, 1);

    // --- Shoulder Feature Settings ---
    @Translatable.Name("Shoulder Feature Settings")
    @Translatable.Desc("Change how the fuzzy parrot of doom whispers danger—and diamonds—into your ear.")
    public ConfigGroup shoulder = new ConfigGroup("shoulder", true);

    @Translatable.Name("Enable Creeper Detection")
    @Translatable.Desc("May save your inventory. Or your ears.")
    public boolean enableShoulderCreeperDetection = true;

    @Translatable.Name("Creeper Detection Radius (Blocks)")
    @Translatable.Desc("Adjust paranoia levels.")
    public ValidatedDouble shoulderCreeperDetectionRadius = new ValidatedDouble(16.0, 16.0, 1.0);

    @Translatable.Name("Enable Diamond Detection")
    @Translatable.Desc("Because who doesn’t enjoy unsolicited financial advice from a rodent?")
    public boolean enableShoulderDiamondDetection = true;

    @ConfigGroup.Pop
    @Translatable.Name("Diamond Detection Radius (Blocks)")
    @Translatable.Desc("How close you need to be before the squeak says \"bling.\"")
    public ValidatedDouble shoulderDiamondDetectionRadius = new ValidatedDouble(10.0, 20.0, 5.0);

    // --- Independent Diamond Seeking Settings ---
    @Translatable.Name("Independent Diamond Seeking Settings")
    @Translatable.Desc("Unleash free-range prospectors. What could go wrong?")
    public ConfigGroup independentDiamondSeeking = new ConfigGroup("independentDiamondSeeking", true);

    @Translatable.Name("Enable Independent Diamond Seeking")
    @Translatable.Desc("Permit hamsters to embark on solo get-rich-quick schemes?")
    public boolean enableIndependentDiamondSeeking = true;

    @Translatable.Name("Diamond Seek Scan Radius (Blocks)")
    @Translatable.Desc("How far a hamster scans once it’s decided to play prospector.")
    public ValidatedInt diamondSeekRadius = new ValidatedInt(10, 20, 5);

    @ConfigGroup.Pop
    @Translatable.Name("Gold 'Mistake' Chance")
    @Translatable.Desc("The probability (0.0 to 1.0) that a hamster will seek gold instead of diamond, if both are available. At 0.5, it's a coin toss. At 1.0, it's guaranteed hamster sulking.")
    public ValidatedFloat goldMistakeChance = new ValidatedFloat(0.33f, 1.0f, 0.0f);

    // --- Diamond Stealing Behavior Settings---
    @Translatable.Name("Diamond Stealing Behavior Settings")
    @Translatable.Desc("For when your hamster develops a taste for the finer things in life. Can be configured so they steal any item— even from other mods, but they only steal diamonds by default.")
    public ConfigGroup diamondStealing = new ConfigGroup("diamondStealing", true);

    @Translatable.Name("Enable Diamond Stealing")
    @Translatable.Desc("Permits hamsters to engage in spontaneous, high-stakes games of keep-away with your valuables. A chase ensues. Obviously.")
    public boolean enableDiamondStealing = true;

    @Translatable.Name("Stealable Items")
    @Translatable.Desc("A list of item IDs hamsters find irresistible. Format: 'mod_id:item_id'. Example: 'minecraft:diamond'.")
    public List<String> stealableItems = new ArrayList<>(List.of("minecraft:diamond"));

    @Translatable.Name("Pounce Chance")
    @Translatable.Desc("Probability (0.1 to 1.0) a hamster will succumb to temptation. High by default. You shouldn't leave your diamonds lying around anyway.")
    public ValidatedFloat diamondPounceChance = new ValidatedFloat(0.75f, 1.0f, 0.1f);

    @Translatable.Name("Minimum Flee Distance (Blocks)")
    @Translatable.Desc("The hamster's personal space bubble.")
    public ValidatedInt minFleeDistance = new ValidatedInt(5, 20, 1);

    @Translatable.Name("Maximum Flee Distance (Blocks)")
    @Translatable.Desc("The maximum distance before the hamster gets bored and stops running to taunt you.")
    public ValidatedInt maxFleeDistance = new ValidatedInt(20, 40, 5);

    @Translatable.Name("Minimum Steal Duration (Seconds)")
    @Translatable.Desc("The shortest amount of time the hamster will entertain this little game before getting bored and dropping your stuff.")
    public ValidatedInt minStealDurationSeconds = new ValidatedInt(5, 240, 1);

    @ConfigGroup.Pop
    @Translatable.Name("Maximum Steal Duration (Seconds)")
    @Translatable.Desc("The longest your cardio session can last before the hamster's attention span gives out.")
    public ValidatedInt maxStealDurationSeconds = new ValidatedInt(15, 300, 5);

    // --- Tamed Sleep Settings ---
    @Translatable.Name("Tamed Sleep Settings")
    @Translatable.Desc("Even digital rodents need beauty sleep— adjust according to your patience levels.")
    public ConfigGroup tamedSleepSettings = new ConfigGroup("tamedSleepSettings", true);

    @Translatable.Name("Threat Radius (Blocks)")
    @Translatable.Desc("How close a hostile mob can get before a hamster wakes up from it's power nap.")
    public ValidatedInt tamedSleepThreatDetectionRadiusBlocks = new ValidatedInt(8, 32, 1);

    @Translatable.Name("Require Daytime?")
    @Translatable.Desc("Night-owl hamsters? Your choice.")
    public boolean requireDaytimeForTamedSleep = true;

    @Translatable.Name("Min Sit Time Before Drowsy (Secs)")
    @Translatable.Desc("Minimum seconds before a sitting hamster gets sleepy.")
    public ValidatedInt tamedQuiescentSitMinSeconds = new ValidatedInt(120, 300, 1);

    @ConfigGroup.Pop
    @Translatable.Name("Max Sit Time Before Drowsy (Secs)")
    @Translatable.Desc("Maximum seconds before the inevitable deep snooze.")
    public ValidatedInt tamedQuiescentSitMaxSeconds = new ValidatedInt(180, 600, 2);

    // --- Combat & Damage Settings ---
    @Translatable.Name("Combat & Damage Settings")
    @Translatable.Desc("Squeak-first, ask questions later. Dial in the rodent kung fu.")
    public ConfigGroup combat = new ConfigGroup("combat", true);

    @Translatable.Name("Melee Damage")
    @Translatable.Desc("Tamed hamster melee damage. Mostly for show, let's be honest.")
    public ValidatedDouble meleeDamage = new ValidatedDouble(2.0, 40.0, 0.0);

    @ConfigGroup.Pop
    @Translatable.Name("Throw Damage")
    @Translatable.Desc("Damage dealt by thrown hamster. Surprisingly effective against Creepers. How convenient.")
    public ValidatedDouble hamsterThrowDamage = new ValidatedDouble(20.0, 40.0, 0.0);

    // --- Food Healing Settings ---
    @Translatable.Name("Food Healing Settings")
    @Translatable.Desc("Nutrition— isn't it wonderful. Tweaks to snacks.")
    public ConfigGroup foodHealing = new ConfigGroup("foodHealing", true);

    @Translatable.Name("Food Mix")
    @Translatable.Desc("Healing amount from Hamster Food Mix. The good stuff.")
    public ValidatedFloat hamsterFoodMixHealing = new ValidatedFloat(4.0f, 10.0f, 0.0f);

    @ConfigGroup.Pop
    @Translatable.Name("Standard Food")
    @Translatable.Desc("Healing from basic seeds/crops. Better than nothing… probably.")
    public ValidatedFloat standardFoodHealing = new ValidatedFloat(2.0f, 5.0f, 0.0f);

    // --- Cheese Food Settings ---
    @Translatable.Name("Cheese Settings")
    @Translatable.Desc("Cheese... the gooey wonder. Some people think it's overpowered. I disagree. Obviously.")
    public ConfigGroup cheeseHealing = new ConfigGroup("cheeseHealing", true);

    @Translatable.Name("Cheese Nutrition")
    @Translatable.Desc("How many little hunger shanks the cheese restores. Vanilla cooked steak is 8. I know you're thinking of moving it to 20, you monster.")
    public ValidatedInt cheeseNutrition = new ValidatedInt(8, 20, 0);

    @ConfigGroup.Pop
    @Translatable.Name("Cheese Saturation")
    @Translatable.Desc("How long the hunger effect lasts. Cooked steak is 0.8. Don't get too crazy. Or do. I'm not your conscience.")
    public ValidatedFloat cheeseSaturation = new ValidatedFloat(0.8f, 2.0f, 0.0f);

    // --- Green Bean Buff Settings ---
    @Translatable.Name("Green Bean Buff Settings")
    @Translatable.Desc("Nutrition, but make it dramatic. Tweaks to caffeine-bean highs.")
    public ConfigGroup greenBeanBuffs = new ConfigGroup("greenBeanBuffs", true);

    @Translatable.Name("Duration (Ticks)")
    @Translatable.Desc("Steamed beans: power that fades faster than your attention span.")
    public ValidatedInt greenBeanBuffDuration = new ValidatedInt(3600, 20 * 60 * 10, 20);

    @Translatable.Name("Speed Level")
    @Translatable.Desc("Because someone has to go fast.")
    public ValidatedInt greenBeanBuffAmplifierSpeed = new ValidatedInt(1, 4, 0);

    @Translatable.Name("Strength Level")
    @Translatable.Desc("Slightly mightier nibbles.")
    public ValidatedInt greenBeanBuffAmplifierStrength = new ValidatedInt(1, 4, 0);

    @Translatable.Name("Absorption Level")
    @Translatable.Desc("Extra fluff padding for those daring dives.")
    public ValidatedInt greenBeanBuffAmplifierAbsorption = new ValidatedInt(1, 4, 0);

    @ConfigGroup.Pop
    @Translatable.Name("Regen Level")
    @Translatable.Desc("Heals minor paper-cuts (and fragile egos).")
    public ValidatedInt greenBeanBuffAmplifierRegen = new ValidatedInt(0, 4, 0);

    // --- Wild Bush & Sunflower Settings ---
    @Translatable.Name("Wild Bush & Sunflower Settings")
    @Translatable.Desc("For The Aspiring Landscape Artist")
    public ConfigGroup worldGenAdjustments = new ConfigGroup("worldGenAdjustments", true);

    @Translatable.Name("Sunflower Seed Regrowth Speed")
    @Translatable.Desc("Higher = slower, lower = faster. Makes perfect sense.")
    public ValidatedDouble sunflowerRegrowthModifier = new ValidatedDouble(1.0, 5.0, 0.1);

    @Translatable.Name("Wild Bush Regrowth Modifier")
    @Translatable.Desc("Higher = slower, lower = faster. Still makes perfect sense.")
    public ValidatedDouble wildBushRegrowthModifier = new ValidatedDouble(1.0, 5.0, 0.1);

    @Translatable.Name("Green Bean Bush Rarity")
    @Translatable.Desc("1 in X chunks. Low numbers may cause shrub spam.")
    public ValidatedInt wildGreenBeanBushRarity = new ValidatedInt(24, 100, 1);

    @ConfigGroup.Pop
    @Translatable.Name("Cucumber Bush Rarity")
    @Translatable.Desc("1 in X chunks. Low numbers may cause shrub spam.")
    public ValidatedInt wildCucumberBushRarity = new ValidatedInt(24, 100, 1);
}