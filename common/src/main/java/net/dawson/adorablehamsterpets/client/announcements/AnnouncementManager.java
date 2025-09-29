package net.dawson.adorablehamsterpets.client.announcements;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.serialization.JsonOps;
import dev.architectury.platform.Platform;
import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.client.book.ClientBookRegistry;
import vazkii.patchouli.common.book.Book;
import vazkii.patchouli.common.book.BookRegistry;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class AnnouncementManager {
    public static final AnnouncementManager INSTANCE = new AnnouncementManager();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String GITHUB_RAW_URL = "https://raw.githubusercontent.com/DawsonBodenhamer/AdorableHamsterPets-Public/main/announcements/";

    private Path stateFilePath;
    private Path manifestCacheFilePath;
    private ClientAnnouncementState clientState;
    private AnnouncementManifest manifest;
    private HttpClient httpClient;
    private boolean hasRefreshedThisSession = false;
    private boolean manifestJustLoaded = false;
    private boolean manifestLoaded = false;
    private final Set<Identifier> deferredReadMarks = new HashSet<>();

    private AnnouncementManager() {
        this.clientState = ClientAnnouncementState.createDefault();
        this.manifest = AnnouncementManifest.empty();
    }

    /**
     * Gets the current client-side announcement state.
     *
     * @return The current ClientAnnouncementState record.
     */
    public ClientAnnouncementState getClientState() {
        return this.clientState;
    }

    /**
     * Determines the canonical "reason" for a notification based on its properties
     * and the current game state, independent of whether it's currently pending.
     * This provides a stable context for the AnnouncementScreen.
     *
     * @param announcementId The ID of the announcement to check.
     * @return The reason string (e.g., "update_available", "mandatory_message").
     */
    public String getCanonicalReasonForAnnouncement(String announcementId) {
        Announcement announcement = getAnnouncementById(announcementId);
        if (announcement == null) return "unknown"; // Fallback for safety

        Semver installedVersion = Semver.parse(Platform.getMod(AdorableHamsterPets.MOD_ID).getVersion().toString());
        Semver latestVersion = Semver.parse(manifest.latest_version());
        Semver lastAckVersion = Semver.parse(clientState.last_acknowledged_update());
        Semver messageVersion = Semver.parse(announcement.semver());

        // 1. Is it the primary "Update Available" notification?
        // This is the highest priority reason.
        if (installedVersion.compareTo(latestVersion) < 0 && announcement.semver().equals(latestVersion.toString())) {
            return PendingNotification.UPDATE_AVAILABLE;
        }

        // 2. Is it a mandatory "What's New" for a version the user has but hasn't acknowledged?
        if (announcement.mandatory() && "update".equals(announcement.kind())) {
            boolean versionIsRelevant = messageVersion.compareTo(installedVersion) <= 0;
            boolean isUnacknowledged = messageVersion.compareTo(lastAckVersion) > 0;
            if (versionIsRelevant && isUnacknowledged) {
                return PendingNotification.MANDATORY_MESSAGE;
            }
        }

        // 3. Is it an optional announcement?
        if ("announcement".equals(announcement.kind())) {
            return PendingNotification.NEW_ANNOUNCEMENT;
        }

        // 4. Fallback to its basic kind if none of the specific contexts apply.
        return announcement.kind();
    }

    /**
     * Queues a virtual entry's ID to be marked as read in Patchouli's data
     * at a later, safer time (i.e., once a world is loaded).
     *
     * @param entryId The Identifier of the virtual BookEntry.
     */
    public void queueDeferredReadMark(Identifier entryId) {
        this.deferredReadMarks.add(entryId);
        AdorableHamsterPets.LOGGER.debug("[Announcements] Queued deferred read mark for entry: {}", entryId);
    }

    /**
     * Processes all queued deferred read marks. This method is called when the client
     * player joins a world, ensuring Patchouli's book data is fully loaded and safe to access.
     */
    public void processDeferredReadMarks() {
        if (deferredReadMarks.isEmpty()) {
            return;
        }

        AdorableHamsterPets.LOGGER.debug("[Announcements] Processing {} deferred read marks...", deferredReadMarks.size());
        // Get the book from the common BookRegistry's public map.
        Book book = BookRegistry.INSTANCE.books.get(Identifier.of(AdorableHamsterPets.MOD_ID, "hamster_tips_guide_book"));
        if (book == null) {
            AdorableHamsterPets.LOGGER.error("[Announcements] Could not process deferred read marks: Hamster Tips book not found.");
            return;
        }

        int successCount = 0;
        for (Identifier entryId : deferredReadMarks) {
            // Access the public 'entries' map directly.
            BookEntry entry = book.getContents().entries.get(entryId);
            if (entry != null) {
                PatchouliIntegration.setEntryAsRead(entry);
                successCount++;
            } else {
                AdorableHamsterPets.LOGGER.warn("[Announcements] Could not find virtual entry for deferred read mark: {}", entryId);
            }
        }

        if (successCount > 0) {
            AdorableHamsterPets.LOGGER.debug("[Announcements] Successfully processed {} deferred read marks.", successCount);
        }

        deferredReadMarks.clear();
    }

    public void acknowledgeManifestLoad() {
        this.manifestJustLoaded = false;
    }

    /**
     * Ensures the manifest is only fetched once per game session.
     * This is called from the TitleScreen mixin.
     */
    public CompletableFuture<Void> refreshManifestOnce() {
        if (!hasRefreshedThisSession) {
            hasRefreshedThisSession = true;
            return refreshManifest();
        }
        // If already refreshed, return a future that's already completed.
        return CompletableFuture.completedFuture(null);
    }

    public Announcement getAnnouncementById(String id) {
        return manifest.messages().stream()
                .filter(a -> a.id().equals(id))
                .findFirst()
                .orElse(null);
    }

    public List<Announcement> getAllManifestMessages() {
        return manifest != null ? manifest.messages() : List.of();
    }

    public void initialize() {
        AdorableHamsterPets.LOGGER.info("[Announcements] Initializing AnnouncementManager..."); // LOG 1: Start
        this.httpClient = HttpClient.newHttpClient();
        Path configDir = Platform.getConfigFolder().resolve(AdorableHamsterPets.MOD_ID);
        this.stateFilePath = configDir.resolve("announcements.json");
        this.manifestCacheFilePath = configDir.resolve("manifest.cache.json");
        AdorableHamsterPets.LOGGER.debug("[Announcements] State file path resolved to: {}", stateFilePath.toAbsolutePath()); // LOG 2: Path
        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            AdorableHamsterPets.LOGGER.error("[Announcements] CRITICAL: Failed to create config directory for announcements at {}", configDir.toAbsolutePath(), e);
        }
        loadState();
        loadCachedManifest();
        processExpiredSnoozes();
        AdorableHamsterPets.LOGGER.info("[Announcements] Initialization complete."); // LOG 3: Finish
    }

    private void processExpiredSnoozes() {
        Instant now = Instant.now();
        Map<String, Instant> newSnoozedIds = new HashMap<>(clientState.snoozed_ids());

        // Use removeIf for safe concurrent modification
        newSnoozedIds.entrySet().removeIf(entry -> {
            if (now.isAfter(entry.getValue())) {
                // Snooze has expired
                Identifier entryId = Identifier.of(AdorableHamsterPets.MOD_ID, "announcement_" + entry.getKey());
                PatchouliIntegration.setEntryAsUnread(entryId);
                return true; // Remove this entry from the map
            }
            return false;
        });

        if (newSnoozedIds.size() != clientState.snoozed_ids().size()) {
            clientState = new ClientAnnouncementState(
                    clientState.seen_ids(),
                    newSnoozedIds,
                    clientState.last_acknowledged_update(),
                    clientState.opt_out_announcements(),
                    clientState.manifest_etag(),
                    clientState.manifest_last_modified()
            );
            saveState();
        }
    }

    public void reEnableOptionalAnnouncements() {
        setOptOut(false);
    }

    public void markAsSeen(String id) {
        Set<String> newSeenIds = new HashSet<>(clientState.seen_ids());
        if (newSeenIds.add(id)) {
            clientState = new ClientAnnouncementState(
                    newSeenIds,
                    clientState.snoozed_ids(),
                    clientState.last_acknowledged_update(),
                    clientState.opt_out_announcements(),
                    clientState.manifest_etag(),
                    clientState.manifest_last_modified()
            );
            saveState();
        }
    }

    public void setLastAcknowledgedUpdate(String version) {
        Semver currentAck = Semver.parse(clientState.last_acknowledged_update());
        Semver newAck = Semver.parse(version);
        if (newAck.compareTo(currentAck) > 0) {
            clientState = new ClientAnnouncementState(
                    clientState.seen_ids(),
                    clientState.snoozed_ids(),
                    newAck.toString(),
                    clientState.opt_out_announcements(),
                    clientState.manifest_etag(),
                    clientState.manifest_last_modified()
            );
            saveState();
        }
    }

    public void setSnooze(String id, int days) {
        Instant snoozeUntil = Instant.now().plus(days, ChronoUnit.DAYS);
        Map<String, Instant> newSnoozedIds = new HashMap<>(clientState.snoozed_ids());
        newSnoozedIds.put(id, snoozeUntil);

        clientState = new ClientAnnouncementState(
                clientState.seen_ids(),
                newSnoozedIds, // Pass the NEW snoozed map
                clientState.last_acknowledged_update(),
                clientState.opt_out_announcements(),
                clientState.manifest_etag(),
                clientState.manifest_last_modified()
        );
        saveState();
    }

    public void setOptOut(boolean optOut) {
        if (clientState.opt_out_announcements() != optOut) {
            clientState = new ClientAnnouncementState(
                    clientState.seen_ids(),
                    clientState.snoozed_ids(),
                    clientState.last_acknowledged_update(),
                    optOut,
                    clientState.manifest_etag(),
                    clientState.manifest_last_modified()
            );
            saveState();
        }
    }

    private void loadState() {
        AdorableHamsterPets.LOGGER.debug("[Announcements] Attempting to load state from {}...", stateFilePath.toAbsolutePath()); // LOG 4: Load Start
        if (Files.exists(stateFilePath)) {
            AdorableHamsterPets.LOGGER.debug("[Announcements] announcements.json found. Reading file."); // LOG 5a: File Found
            try (FileReader reader = new FileReader(stateFilePath.toFile())) {
                ClientAnnouncementState.CODEC.parse(JsonOps.INSTANCE, GSON.fromJson(reader, com.google.gson.JsonElement.class))
                        .resultOrPartial(AdorableHamsterPets.LOGGER::error)
                        .ifPresent(state -> this.clientState = state);
            } catch (IOException e) {
                AdorableHamsterPets.LOGGER.error("[Announcements] CRITICAL: Failed to load announcement state from existing file.", e);
            }
        } else {
            AdorableHamsterPets.LOGGER.info("[Announcements] announcements.json not found. Creating default state file."); // LOG 5b: File Not Found
            saveState(); // Create default file if it doesn't exist
        }
    }

    private void saveState() {
        AdorableHamsterPets.LOGGER.debug("[Announcements] Attempting to save state..."); // LOG 6: Save Start
        ClientAnnouncementState.CODEC.encodeStart(JsonOps.INSTANCE, this.clientState)
                .resultOrPartial(error -> AdorableHamsterPets.LOGGER.error("[Announcements] CRITICAL: Failed to encode client state to JSON: {}", error)) // LOG 7: Encode Error
                .ifPresent(jsonElement -> {
                    AdorableHamsterPets.LOGGER.debug("[Announcements] State encoded successfully. Writing to file: {}", stateFilePath.toAbsolutePath()); // LOG 8: Writing
                    try (FileWriter writer = new FileWriter(stateFilePath.toFile())) {
                        GSON.toJson(jsonElement, writer);
                        AdorableHamsterPets.LOGGER.debug("[Announcements] Successfully saved announcement state."); // LOG 9: Success
                    } catch (IOException e) {
                        AdorableHamsterPets.LOGGER.error("[Announcements] CRITICAL: FAILED TO SAVE ANNOUNCEMENT STATE TO FILE.", e); // LOG 10: Write Error
                    }
                });
    }

    public void resetClientState() {
        this.clientState = ClientAnnouncementState.createDefault();
        saveState();
        PatchouliIntegration.clearAllVirtualEntriesFromHistory();
        AdorableHamsterPets.LOGGER.debug("Client announcement state has been reset.");
    }

    private void loadCachedManifest() {
        if (Files.exists(manifestCacheFilePath)) {
            try (FileReader reader = new FileReader(manifestCacheFilePath.toFile())) {
                AnnouncementManifest.CODEC.parse(JsonOps.INSTANCE, GSON.fromJson(reader, com.google.gson.JsonElement.class))
                        .resultOrPartial(AdorableHamsterPets.LOGGER::error)
                        .ifPresent(cachedManifest -> {
                            this.manifest = cachedManifest;
                            this.manifestLoaded = true; // Mark as loaded from cache
                            AdorableHamsterPets.LOGGER.info("[Announcements] Loaded cached manifest with {} messages.", manifest.messages().size());
                        });
            } catch (IOException e) {
                AdorableHamsterPets.LOGGER.error("[Announcements] Failed to load cached manifest.", e);
            }
        }
    }

    private void saveManifestToCache() {
        AnnouncementManifest.CODEC.encodeStart(JsonOps.INSTANCE, this.manifest)
                .resultOrPartial(error -> AdorableHamsterPets.LOGGER.error("[Announcements] Failed to encode manifest for caching: {}", error))
                .ifPresent(jsonElement -> {
                    try (FileWriter writer = new FileWriter(manifestCacheFilePath.toFile())) {
                        GSON.toJson(jsonElement, writer);
                        AdorableHamsterPets.LOGGER.debug("[Announcements] Successfully saved manifest to cache.");
                    } catch (IOException e) {
                        AdorableHamsterPets.LOGGER.error("[Announcements] FAILED TO SAVE MANIFEST TO CACHE.", e);
                    }
                });
    }

    public record PendingNotification(String reason, Announcement announcement) {
        public static final String UPDATE_AVAILABLE = "update_available";
        public static final String NEW_ANNOUNCEMENT = "new_announcement";
        public static final String MANDATORY_MESSAGE = "mandatory_message";
    }

    public CompletableFuture<Void> refreshManifest() {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(GITHUB_RAW_URL + "manifest.json"))
                .GET();

        clientState.manifest_etag().ifPresent(etag -> requestBuilder.header("If-None-Match", etag));
        clientState.manifest_last_modified().ifPresent(lastModified -> requestBuilder.header("If-Modified-Since", lastModified));

        return httpClient.sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    AdorableHamsterPets.LOGGER.debug("[Announcements] Manifest fetch completed with status code {}. Current screen: {}", response.statusCode(), MinecraftClient.getInstance().currentScreen);
                    if (response.statusCode() == 200) { // OK
                        AdorableHamsterPets.LOGGER.debug("[Announcements] Fetched new manifest.");
                        AnnouncementManifest.CODEC.parse(JsonOps.INSTANCE, GSON.fromJson(response.body(), com.google.gson.JsonElement.class))
                                .resultOrPartial(AdorableHamsterPets.LOGGER::error)
                                .ifPresent(newManifest -> {
                                    this.manifest = newManifest;
                                    this.manifestLoaded = true; // Mark as loaded
                                    saveManifestToCache();

                                    Optional<String> etag = response.headers().firstValue("ETag");
                                    Optional<String> lastModified = response.headers().firstValue("Last-Modified");

                                    this.clientState = new ClientAnnouncementState(
                                            clientState.seen_ids(),
                                            clientState.snoozed_ids(),
                                            clientState.last_acknowledged_update(),
                                            clientState.opt_out_announcements(),
                                            etag,
                                            lastModified
                                    );
                                    saveState();
                                    // Set the flag instead of reloading immediately.
                                    this.manifestJustLoaded = true;

                                    // If already in a world, it's safe to reload now.
                                    if (MinecraftClient.getInstance().world != null) {
                                        MinecraftClient.getInstance().execute(() -> {
                                            ClientBookRegistry.INSTANCE.reload();
                                            acknowledgeManifestLoad(); // Acknowledge it immediately
                                        });
                                    }
                                });
                    } else if (response.statusCode() == 304) { // Not Modified
                        AdorableHamsterPets.LOGGER.info("[Announcements] Manifest is up to date (304 Not Modified).");
                    } else {
                        AdorableHamsterPets.LOGGER.warn("[Announcements] Failed to fetch manifest, status code: {}", response.statusCode());
                    }
                }).exceptionally(e -> {
                    AdorableHamsterPets.LOGGER.error("[Announcements] Exception while fetching manifest. Using cached version.", e);
                    return null;
                });
    }

    // Fetch markdown content
    public CompletableFuture<String> fetchMarkdown(String relativePath) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GITHUB_RAW_URL + relativePath))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return response.body();
                    }
                    AdorableHamsterPets.LOGGER.warn("Failed to fetch markdown from '{}', status code: {}", relativePath, response.statusCode());
                    return "# Fetch Failed\nCould not load announcement content.";
                })
                .exceptionally(e -> {
                    AdorableHamsterPets.LOGGER.error("Exception while fetching markdown from '" + relativePath + "'", e);
                    return "# Network Error\nAn exception occurred while fetching content.";
                });
    }

    public List<PendingNotification> getPendingNotifications() {
        if (!this.manifestLoaded) {
            return Collections.emptyList(); // Guard against race condition
        }
        AdorableHamsterPets.LOGGER.trace("[Announcements] Running getPendingNotifications check...");
        List<PendingNotification> pending = new ArrayList<>();
        if (manifest == null || manifest.messages().isEmpty()) {
            AdorableHamsterPets.LOGGER.error("[Announcements] -> Check failed: Manifest is null or empty.");
            return pending; // Nothing to do if manifest is not loaded
        }

        Semver installedVersion = Semver.parse(Platform.getMod(AdorableHamsterPets.MOD_ID).getVersion().toString());
        Semver latestVersion = Semver.parse(manifest.latest_version());
        Semver lastAckVersion = Semver.parse(clientState.last_acknowledged_update());
        Instant now = Instant.now();

        AdorableHamsterPets.LOGGER.trace("[Announcements] -> Versions: Installed={}, Latest={}, LastAck={}", installedVersion, latestVersion, lastAckVersion);
        AdorableHamsterPets.LOGGER.trace("[Announcements] -> Snooze until: {}. Opt-out: {}",  clientState.opt_out_announcements());

        // --- 1. Check for New Major/Minor Update ---
        boolean newUpdateAvailable = installedVersion.compareTo(latestVersion) < 0;
        String latestUpdateId = "update-" + latestVersion;
        boolean isSnoozed = clientState.snoozed_ids().getOrDefault(latestUpdateId, Instant.EPOCH).isAfter(now);
        AdorableHamsterPets.LOGGER.trace("[Announcements] -> Update check: newUpdateAvailable={}, isSnoozed={}", newUpdateAvailable, isSnoozed);
        if (newUpdateAvailable && !isSnoozed) {
            manifest.messages().stream()
                    .filter(a -> "update".equals(a.kind()) && latestVersion.toString().equals(a.semver()))
                    .findFirst()
                    .ifPresent(announcement -> {
                        pending.add(new PendingNotification(PendingNotification.UPDATE_AVAILABLE, announcement));
                        AdorableHamsterPets.LOGGER.trace("[Announcements] -> ADDED (Update Available): id='{}', semver='{}'", announcement.id(), announcement.semver());
                    });
        }

        // --- 2. Check for All Other Messages (Announcements and Missed Mandatory Updates) ---
        AdorableHamsterPets.LOGGER.trace("[Announcements] -> Scanning all {} messages for mandatory/optional...", manifest.messages().size());
        for (Announcement message : manifest.messages()) {
            if (clientState.seen_ids().contains(message.id())) {
                continue;
            }
            // Check if this specific message is snoozed
            if (clientState.snoozed_ids().getOrDefault(message.id(), Instant.EPOCH).isAfter(now)) {
                continue;
            }

            Semver messageVersion = Semver.parse(message.semver());

            // --- 2a. Optional Announcements ---
            if ("announcement".equals(message.kind())) {
                if (!clientState.opt_out_announcements()) {
                    pending.add(new PendingNotification(PendingNotification.NEW_ANNOUNCEMENT, message));
                    AdorableHamsterPets.LOGGER.trace("[Announcements] -> ADDED (Optional Announcement): id='{}'", message.id());
                } else {
                    AdorableHamsterPets.LOGGER.trace("[Announcements] -> SKIPPED (Opted Out): id='{}'", message.id());
                }
                continue; // Processed as announcement, move to next message
            }

            // --- 2b. Mandatory "What's New" for Current or Past Versions ---
            // This triggers for updates the user has installed but not yet acknowledged.
            if (message.mandatory() && "update".equals(message.kind())) {
                boolean versionIsRelevant = messageVersion.compareTo(installedVersion) <= 0;
                boolean isUnacknowledged = messageVersion.compareTo(lastAckVersion) > 0;

                // Condition: The message version is <= the installed version AND > the last acknowledged version.
                if (versionIsRelevant && isUnacknowledged) {
                    boolean alreadyPendingAsUpdate = newUpdateAvailable && message.semver().equals(latestVersion.toString());
                    if (!alreadyPendingAsUpdate) {
                        pending.add(new PendingNotification(PendingNotification.MANDATORY_MESSAGE, message));
                        AdorableHamsterPets.LOGGER.trace("[Announcements] -> ADDED (Mandatory Message): id='{}', semver='{}'", message.id(), message.semver());
                    } else {
                        AdorableHamsterPets.LOGGER.trace("[Announcements] -> SKIPPED (Duplicate Update): id='{}'", message.id());
                    }
                }
            }
        }

        // --- 3. Sort by publication date, newest first ---
        pending.sort(Comparator.comparing((PendingNotification p) -> p.announcement().published()).reversed());
        AdorableHamsterPets.LOGGER.trace("[Announcements] -> Final pending count: {}", pending.size());
        return pending;
    }

    /**
     * Generates the primary tooltip text for a given notification.
     *
     * @param notification The notification to generate text for.
     * @return The formatted Text component for the tooltip.
     */
    public static Text getTooltipTextForNotification(PendingNotification notification) {
        return switch (notification.reason()) {
            case PendingNotification.UPDATE_AVAILABLE ->
                    Text.translatable("tooltip.adorablehamsterpets.hud.update_available", notification.announcement().semver());
            case PendingNotification.NEW_ANNOUNCEMENT ->
                    Text.translatable("tooltip.adorablehamsterpets.hud.new_announcement");
            default -> Text.translatable("tooltip.adorablehamsterpets.hud.whats_new", notification.announcement().semver());
        };
    }
}