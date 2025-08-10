package net.dawson.adorablehamsterpets.config;

import me.fzzyhmstrs.fzzy_config.annotations.ClientModifiable;
import me.fzzyhmstrs.fzzy_config.annotations.RootConfig;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.config.ConfigAction;
import me.fzzyhmstrs.fzzy_config.config.ConfigGroup;
import me.fzzyhmstrs.fzzy_config.screen.widget.TextureIds;
import me.fzzyhmstrs.fzzy_config.util.Translatable;
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedCondition;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedEnum;
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
    @Translatable.Desc("How Many, Where, and How Often?  Note: Some of these settings require re-logging into your world to take effect.")
    public ConfigGroup hamsterSpawning = new ConfigGroup("hamsterSpawning", true);

    @Translatable.Name("Spawn Weight")
    @Translatable.Desc("Adjusts hamster spawn frequency. Higher = more chaos. 1 = blissful silence.")
    public ValidatedInt spawnWeight = new ValidatedInt(30, 100, 1);

    @Translatable.Name("Max Group Size")
    @Translatable.Desc("Maximum hamsters per spawn group. Because sometimes one just isn't cute enough.")
    public ValidatedInt maxGroupSize = new ValidatedInt(1, 10, 1);

    @Translatable.Name("Vanilla Biome Tags")
    @Translatable.Desc("A list of biome tags where hamsters can spawn. Format: 'mod_id:tag_name'. For example, 'minecraft:is_forest'.")
    public List<String> spawnBiomeTags = new ArrayList<>(List.of(
            "minecraft:is_beach",
            "minecraft:is_badlands",
            "minecraft:is_savanna",
            "minecraft:is_jungle",
            "minecraft:is_forest",
            "minecraft:is_taiga",
            "minecraft:is_mountain"
    ));

    @Translatable.Name("Convention Biome Tags")
    @Translatable.Desc("A list of 'c:' convention biome tags where hamsters can spawn. Used for broad mod compatibility. By default, this includes most overworld tags as a 'catch-all', and the filtering for different hamster variants in each biome is hard coded.")
    public List<String> spawnBiomeConventionTags = new ArrayList<>(List.of(
            "c:is_cold",
            "c:is_hot",
            "c:is_temperate",
            "c:is_dry",
            "c:is_wet",
            "c:is_dense_vegetation",
            "c:is_sparse_vegetation"
    ));

    @Translatable.Name("Include Specific Biomes")
    @Translatable.Desc("A list of specific biome IDs to ALWAYS allow spawns in, even if they don't match the tags above. Format: 'mod_id:biome_name'. For example, 'minecraft:plains'.")
    public List<String> includeBiomes = new ArrayList<>(List.of(
            // Specific Biomes from old isKeyInSpawnList
            "minecraft:snowy_plains", "minecraft:snowy_taiga", "minecraft:snowy_slopes",
            "minecraft:frozen_peaks", "minecraft:jagged_peaks", "minecraft:grove",
            "minecraft:frozen_river", "minecraft:snowy_beach", "minecraft:frozen_ocean",
            "minecraft:deep_frozen_ocean", "minecraft:ice_spikes", "minecraft:cherry_grove",
            "minecraft:lush_caves", "minecraft:dripstone_caves", "minecraft:deep_dark",
            "minecraft:swamp", "minecraft:mangrove_swamp", "minecraft:desert",
            "minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow",
            "minecraft:old_growth_birch_forest", "minecraft:windswept_hills",
            "minecraft:windswept_gravelly_hills", "minecraft:windswept_forest",
            "minecraft:windswept_savanna", "minecraft:stony_peaks", "minecraft:sparse_jungle",
            "minecraft:bamboo_jungle", "minecraft:stony_shore", "minecraft:mushroom_fields",
            "minecraft:deep_dark", "minecraft:forest", "minecraft:birch_forest", "minecraft:dark_forest",
            "minecraft:taiga", "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga",
            "minecraft:savanna", "minecraft:savanna_plateau", "minecraft:badlands",
            "minecraft:eroded_badlands", "minecraft:wooded_badlands", "minecraft:beach",

            "terralith:desert_canyon", "terralith:cave/andesite_caves", "terralith:cave/crystal_caves", "terralith:cave/deep_caves", "terralith:cave/desert_caves", "terralith:cave/diorite_caves", "terralith:cave/frostfire_caves", "terralith:cave/fungal_caves", "terralith:cave/granite_caves", "terralith:cave/ice_caves", "terralith:cave/infested_caves", "terralith:cave/mantle_caves", "terralith:cave/thermal_caves", "terralith:cave/tuff_caves", "terralith:cave/underground_jungle",
            "terralith:alpha_islands_winter", "terralith:alpha_islands", "terralith:alpine_grove", "terralith:alpine_highlands", "terralith:amethyst_canyon", "terralith:amethyst_rainforest", "terralith:ancient_sands", "terralith:arid_highlands", "terralith:ashen_savanna",
            "terralith:basalt_cliffs", "terralith:birch_taiga", "terralith:blooming_plateau", "terralith:blooming_valley", "terralith:brushland", "terralith:bryce_canyon", "terralith:caldera", "terralith:cloud_forest", "terralith:cold_shrubland",
            "terralith:desert_oasis", "terralith:desert_spires", "terralith:emerald_peaks", "terralith:forested_highlands", "terralith:fractured_savanna", "terralith:frozen_cliffs", "terralith:glacial_chasm", "terralith:granite_cliffs",
            "terralith:gravel_beach", "terralith:gravel_desert", "terralith:haze_mountain", "terralith:highlands", "terralith:hot_shrubland", "terralith:ice_marsh", "terralith:jungle_mountains",
            "terralith:lavender_forest", "terralith:lavender_valley", "terralith:lush_desert", "terralith:lush_valley", "terralith:mirage_isles", "terralith:moonlight_grove", "terralith:moonlight_valley", "terralith:mountain_steppe",
            "terralith:orchid_swamp", "terralith:painted_mountains", "terralith:red_oasis", "terralith:rocky_jungle", "terralith:rocky_mountains", "terralith:rocky_shrubland",
            "terralith:sakura_grove", "terralith:sakura_valley", "terralith:sandstone_valley", "terralith:savanna_badlands", "terralith:savanna_slopes", "terralith:scarlet_mountains",
            "terralith:shield_clearing", "terralith:shield", "terralith:shrubland", "terralith:siberian_grove", "terralith:siberian_taiga",
            "terralith:skylands_autumn", "terralith:skylands_spring", "terralith:skylands_summer", "terralith:skylands_winter", "terralith:skylands",
            "terralith:snowy_badlands", "terralith:snowy_cherry_grove", "terralith:snowy_maple_forest", "terralith:snowy_shield",
            "terralith:steppe", "terralith:stony_spires", "terralith:temperate_highlands", "terralith:tropical_jungle", "terralith:valley_clearing",
            "terralith:volcanic_crater", "terralith:volcanic_peaks", "terralith:warm_river", "terralith:warped_mesa", "terralith:white_cliffs", "terralith:white_mesa",
            "terralith:windswept_spires", "terralith:wintry_forest", "terralith:wintry_lowlands", "terralith:yellowstone", "terralith:yosemite_cliffs", "terralith:yosemite_lowlands",

            "biomesoplenty:wasteland", "biomesoplenty:wasteland_steppe",
            "biomesoplenty:mediterranean_forest", "biomesoplenty:mystic_grove", "biomesoplenty:orchard", "biomesoplenty:pumpkin_patch",
            "biomesoplenty:redwood_forest", "biomesoplenty:seasonal_forest", "biomesoplenty:woodland",
            "biomesoplenty:floodplain", "biomesoplenty:fungal_jungle", "biomesoplenty:rainforest", "biomesoplenty:rocky_rainforest",

            "byg:lush_stacks", "byg:orchard", "byg:frosted_coniferous_forest", "byg:allium_fields", "byg:amaranth_fields", "byg:rose_fields",
            "byg:temperate_grove", "byg:coconino_meadow", "byg:skyris_vale", "byg:prairie", "byg:autumnal_valley", "byg:cardinal_tundra", "byg:firecracker_shrubland",
            "byg:allium_shrubland", "byg:amaranth_grassland", "byg:araucaria_savanna", "byg:aspen_boreal", "byg:atacama_outback", "byg:baobab_savanna",
            "byg:basalt_barrera", "byg:bayou", "byg:black_forest", "byg:canadian_shield", "byg:cika_woods", "byg:coniferous_forest",
            "byg:crimson_tundra", "byg:cypress_swamplands", "byg:dacite_ridges", "byg:dacite_shore", "byg:dead_sea", "byg:ebony_woods",
            "byg:enchanted_tangle", "byg:eroded_borealis", "byg:firecracker_chaparral", "byg:forgotten_forest", "byg:fragment_jungle",
            "byg:frosted_taiga", "byg:howling_peaks", "byg:ironwood_gour", "byg:jacaranda_jungle", "byg:maple_taiga", "byg:mojave_desert",
            "byg:overgrowth_woodlands", "byg:pumpkin_valley", "byg:rainbow_beach", "byg:red_rock_valley", "byg:redwood_thicket",
            "byg:rugged_badlands", "byg:sakura_grove", "byg:shattered_glacier", "byg:sierra_badlands", "byg:skyrise_vale",
            "byg:tropical_rainforest", "byg:weeping_witch_forest", "byg:white_mangrove_marshes", "byg:windswept_desert", "byg:zelkova_forest"
    ));

    @ConfigGroup.Pop
    @Translatable.Name("Exclude Specific Biomes")
    @Translatable.Desc("A list of specific biome IDs to NEVER allow spawns in, even if they match a tag. This overrides all other settings. Format: 'mod_id:biome_name'. For example, 'minecraft:plains'.")
    public List<String> excludeBiomes = new ArrayList<>(List.of("mod_id:biome_name"));

    // --- Taming & Breeding Settings ---
    @Translatable.Name("Taming & Breeding Settings")
    @Translatable.Desc("Convince a hamster to love you—and occasionally accept a roommate.")
    public ConfigGroup tamingAndBreeding = new ConfigGroup("tamingAndBreeding", true);

    @ConfigGroup.Pop
    @Translatable.Name("Taming Chance")
    @Translatable.Desc("Taming difficulty (1 in X chance). Higher = more cucumbers sacrificed to fuzzy freeloaders.")
    public ValidatedInt tamingChanceDenominator = new ValidatedInt(3, 20, 1);

    // --- Shoulder Hamster Settings ---
    @Translatable.Name("Shoulder Hamster Settings")
    @Translatable.Desc("Settings for your fuzzy parrot of doom.")
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

    @Translatable.Name("Diamond Detection Radius (Blocks)")
    @Translatable.Desc("How close you need to be before the squeak says \"bling.\"")
    public ValidatedDouble shoulderDiamondDetectionRadius = new ValidatedDouble(10.0, 20.0, 5.0);

    @Translatable.Name("Dismount Button")
    @Translatable.Desc("Choose what action dismounts the hamster. 'SNEAK_KEY' uses your sneak key, obviously. 'CUSTOM_KEYBIND' uses a separate key you must set in Controls > Key Binds.")
    public DismountTriggerType dismountTriggerType = DismountTriggerType.SNEAK_KEY;

    @Translatable.Name("Button‑Press Behavior")
    @Translatable.Desc("Choose whether a single press or a quick double‑tap dismounts the hamster.")
    public ValidatedEnum<DismountPressType> dismountPressType =
            new ValidatedEnum<>(DismountPressType.SINGLE_PRESS);

    private final ValidatedField<Boolean> isDoubleTap =
            dismountPressType.map(
                    pt -> pt == DismountPressType.DOUBLE_TAP,
                    b -> b ? DismountPressType.DOUBLE_TAP : DismountPressType.SINGLE_PRESS
            );

    @ConfigGroup.Pop
    @Translatable.Name("Double-Tap Delay (Ticks)")
    @Translatable.Desc("Max time between sneak key presses to count as a double-tap. (20 ticks = 1 second)")
    public ValidatedCondition<Integer> doubleTapDelayTicks =
            new ValidatedInt(10, 40, 5)
                    .toCondition(
                            isDoubleTap,
                            Text.literal("Only available when Button-Press Behavior is set to DOUBLE_TAP."),
                            () -> 10
                    );

    // --- Hamster Yeet Settings ---
    @Translatable.Name("Hamster Yeet Settings")
    @Translatable.Desc("For when you need a furry, surprisingly aerodynamic solution.")
    public ConfigGroup yeetSettings = new ConfigGroup("yeetSettings", true);

    @Translatable.Name("Throw Velocity")
    @Translatable.Desc("The base throw speed of your furry projectile.")
    public ValidatedDouble hamsterThrowVelocity = new ValidatedDouble(1.5, 5.0, 0.1);

    @ConfigGroup.Pop
    @Translatable.Name("Throw Velocity (Buffed)")
    @Translatable.Desc("The throw speed of your furry projectile when under the influence of Steamed Green Beans. Goes from 'yeet' to 'yote'.")
    public ValidatedDouble hamsterThrowVelocityBuffed = new ValidatedDouble(2.5, 5.0, 0.1);

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

    // --- Worldgen: Bush & Sunflower Stuff ---
    @Translatable.Name("Worldgen: Bush & Sunflower Stuff")
    @Translatable.Desc("For The Aspiring Landscape Artist. Note: Most of these settings require re-logging into your world to take effect, and it's unlikely you will see changes in chunks that have already been generated.")
    public ConfigGroup worldGenMisc = new ConfigGroup("worldGenMisc", true);

    @Translatable.Name("Wild Bush Regrowth Modifier")
    @Translatable.Desc("Higher = slower, lower = faster. Makes perfect sense.")
    public ValidatedDouble wildBushRegrowthModifier = new ValidatedDouble(1.0, 5.0, 0.1);

    // --- Sunflower Settings ---
    @Translatable.Name("Sunflower Settings")
    @Translatable.Desc("Custom sunflowers, because the vanilla ones just weren’t fabulous enough. Only changes fresh chunks.")
    public ConfigGroup sunflowerSettings = new ConfigGroup("sunflowerSettings", true);

    @Translatable.Name("Sunflower Seed Regrowth Speed")
    @Translatable.Desc("Higher = slower, lower = faster. Photosynthesis is hard, okay?")
    public ValidatedDouble sunflowerRegrowthModifier = new ValidatedDouble(1.0, 5.0, 0.1);

    @Translatable.Name("Convention Biome Tags")
    @Translatable.Desc("A list of biome tags where these custom Sunflowers can replace vanilla ones. The 'c:is_plains' tag provides wide compatibility with modded biomes.")
    public List<String> sunflowerBiomeTags = new ArrayList<>(List.of(
            "c:is_plains",
            "c:is_temperate",
            "c:is_hot",
            "c:is_dry"
    ));

    @ConfigGroup.Pop
    @Translatable.Name("Specific Biomes")
    @Translatable.Desc("Specific biome IDs where these sunflowers can replace the vanilla ones. Format: 'mod_id:biome_name'. They’re picky.")
    public List<String> sunflowerBiomes = new ArrayList<>(List.of("minecraft:sunflower_plains"));

    // --- Cucumber Bush Settings ---
    @Translatable.Name("Cucumber Bush Settings")
    @Translatable.Desc("Wild cucumbers, for when you need emergency salads in the savanna. Only changes fresh chunks.")
    public ConfigGroup cucumberBushSettings = new ConfigGroup("cucumberBushSettings", true);

    @Translatable.Name("Cucumber Bush Rarity")
    @Translatable.Desc("1 in X chunks. Lower numbers means cucumbers take over the planet.")
    public ValidatedInt wildCucumberBushRarity = new ValidatedInt(24, 100, 1);

    @Translatable.Name("Vanilla Biome Tags")
    @Translatable.Desc("Biome tags where cucumbers feel at home. Format: 'mod_id:tag_name', for example: 'minecraft:is_jungle'.")
    public List<String> cucumberBushTags = new ArrayList<>(List.of("minecraft:is_jungle"));

    @Translatable.Name("Convention Biome Tags")
    @Translatable.Desc("Convention tags for maximum mod-pack harmony. Format: 'c:tag_name', for example: 'c:is_temperate'.")
    public List<String> cucumberBushConventionTags = new ArrayList<>(List.of(
            "c:is_temperate",
            "c:is_hot",
            "c:is_dry"
    ));

    @Translatable.Name("Specific Biomes")
    @Translatable.Desc("Specific biome IDs where cucumbers can sprout. Format: 'mod_id:biome_name', for example: 'minecraft:savanna'.")
    public List<String> cucumberBushBiomes = new ArrayList<>(List.of(
            "minecraft:plains",
            "minecraft:sunflower_plains",
            "minecraft:savanna",
            "minecraft:savanna_plateau",
            "minecraft:forest",
            "minecraft:birch_forest",
            "minecraft:meadow",
            "minecraft:wooded_badlands",
            "minecraft:jungle",
            "minecraft:sparse_jungle",
            "minecraft:bamboo_jungle"
    ));

    @ConfigGroup.Pop
    @Translatable.Name("Specific Exclusions")
    @Translatable.Desc("Biomes where cucumbers are absolutely NOT allowed. Overrides everything else. Format: 'mod_id:biome_name', for example: 'minecraft:ocean'.")
    public List<String> cucumberBushExclusions = new ArrayList<>(List.of(
            "minecraft:swamp",
            "minecraft:mangrove_swamp",
            "minecraft:mushroom_fields",
            "minecraft:ocean",
            "minecraft:deep_ocean",
            "minecraft:warm_ocean",
            "minecraft:stony_peaks"
    ));

    // --- Green Bean Bush Settings ---
    @Translatable.Name("Green Bean Bush Settings")
    @Translatable.Desc("Legumes with attitude. Tuned for that perfect mid-game caffeine hit. Only changes fresh chunks.")
    public ConfigGroup greenBeanBushSettings = new ConfigGroup("greenBeanBushSettings", true);

    @Translatable.Name("Green Bean Bush Rarity")
    @Translatable.Desc("1 in X chunks. Lower = beanpocalypse. For those of you in the back, it means they'll spam everywhere.")
    public ValidatedInt wildGreenBeanBushRarity = new ValidatedInt(24, 100, 1);

    @Translatable.Name("Vanilla Biome Tags")
    @Translatable.Desc("Biome tags for bean growth. Empty by default—choose wisely. Format: 'mod_id:tag_name', for example: 'minecraft:is_jungle'.")
    public List<String> greenBeanBushTags = new ArrayList<>(List.of("mod_id:biome_name"));

    @Translatable.Name("Convention Biome Tags")
    @Translatable.Desc("Convention tags for mod-friendly bean spam. Format: 'c:tag_name', for example: 'c:is_wet'.")
    public List<String> greenBeanBushConventionTags = new ArrayList<>(List.of(
            "c:is_wet",
            "c:is_temperate"
    ));

    @Translatable.Name("Specific Biomes")
    @Translatable.Desc("Specific biomes where beans sprout like gossip in chat. Format: 'mod_id:biome_name', for example: 'minecraft:swamp'.")
    public List<String> greenBeanBushBiomes = new ArrayList<>(List.of(
            "minecraft:swamp",
            "minecraft:mangrove_swamp",
            "minecraft:lush_caves",
            "minecraft:flower_forest"
    ));

    @ConfigGroup.Pop
    @ConfigGroup.Pop
    @Translatable.Name("Specific Exclusions")
    @Translatable.Desc("Absolutely no beans here, thank you very much. Overrides all other settings. Format: 'mod_id:biome_name', for example: 'minecraft:beach'.")
    public List<String> greenBeanBushExclusions = new ArrayList<>(List.of(
            "minecraft:beach",
            "minecraft:birch_forest",
            "minecraft:cherry_grove",
            "minecraft:dark_forest",
            "minecraft:deep_ocean",
            "minecraft:dripstone_caves",
            "minecraft:forest",
            "minecraft:meadow",
            "minecraft:ocean",
            "minecraft:old_growth_birch_forest",
            "minecraft:plains",
            "minecraft:river",
            "minecraft:sunflower_plains"
    ));
}