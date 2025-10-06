package net.dawson.adorablehamsterpets.client.gui;

import net.dawson.adorablehamsterpets.AdorableHamsterPets;
import net.dawson.adorablehamsterpets.client.announcements.Announcement;
import net.dawson.adorablehamsterpets.client.announcements.AnnouncementManager;
import net.dawson.adorablehamsterpets.client.announcements.ClientAnnouncementState;
import net.dawson.adorablehamsterpets.client.announcements.PatchouliIntegration;
import net.dawson.adorablehamsterpets.config.Configs;
import net.dawson.adorablehamsterpets.mixin.accessor.ScreenWidgetAdder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import vazkii.patchouli.client.book.BookEntry;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class AnnouncementScreen extends Screen {

    // --- UI Constants ---
    private static final Identifier BACKGROUND_TEXTURE = Identifier.of(AdorableHamsterPets.MOD_ID, "textures/gui/announcement_ui.png");
    private static final Identifier SCROLLBAR_TEXTURE = Identifier.of(AdorableHamsterPets.MOD_ID, "textures/gui/announcement_ui_scroll_bar.png");
    private static final int BACKGROUND_WIDTH = 256;
    private static final int BACKGROUND_HEIGHT = 256;
    private static final int CONTENT_X_OFFSET = 17;
    private static final int CONTENT_Y_OFFSET = 35;
    private static final int CONTENT_WIDTH = 221;
    private static final int CONTENT_HEIGHT = 204;
    private static final int SCROLLBAR_X_OFFSET = 242;
    private static final int SCROLLBAR_START_Y = 28;
    private static final int SCROLLBAR_END_Y = 221;

    private final Announcement announcement;
    private MarkdownRenderer markdownRenderer;
    private String markdownContent = "Loading...";
    private double scrollY = 0.0;

    // --- UI Layout Fields ---
    private int guiLeft;
    private int guiTop;
    @Nullable private Style hoveredStyle = null;
    private final Screen parentScreen;
    private final BookEntry virtualEntry;
    private final String reason;

    public AnnouncementScreen(Announcement announcement, String reason, @Nullable Screen parentScreen, BookEntry virtualEntry) {
        super(Text.literal(announcement.title()));
        this.announcement = announcement;
        this.reason = reason;
        this.parentScreen = parentScreen;
        this.virtualEntry = virtualEntry;
    }

    @Override
    protected void init() {
        super.init();

        // --- UI Layout Calculation ---
        this.guiLeft = (this.width - BACKGROUND_WIDTH) / 2;
        this.guiTop = (this.height - BACKGROUND_HEIGHT) / 2;

        // --- Markdown Renderer Initialization ---
        this.markdownRenderer = new MarkdownRenderer(markdownContent, this.guiLeft + CONTENT_X_OFFSET, this.guiTop + CONTENT_Y_OFFSET, CONTENT_WIDTH);

        AnnouncementManager.INSTANCE.fetchMarkdown(announcement.markdown()).thenAccept(content -> {
            this.client.execute(() -> {
                this.markdownContent = content;
                this.markdownRenderer = new MarkdownRenderer(content, this.guiLeft + CONTENT_X_OFFSET, this.guiTop + CONTENT_Y_OFFSET, CONTENT_WIDTH);
            });
        });

        // --- "←" Close/Back Button ---
        int closeButtonX = this.guiLeft - 10; // Slightly overlapping the left side of the GUI
        int closeButtonY = this.guiTop + 5; // Slightly below the top of the GUI
        // Use ScreenWidgetAdder accessor to add the widget for cross-loader compatibility
        ((ScreenWidgetAdder)(Object)this).adorablehamsterpets$addWidget(ButtonWidget.builder(Text.literal("←").formatted(Formatting.BOLD), button -> this.returnToBook())
                .dimensions(closeButtonX, closeButtonY, 20, 16)
                .tooltip(Tooltip.of(Text.translatable("gui.adorablehamsterpets.announcement.button.close.tooltip")))
                .build());

        // --- Dynamic Button Creation ---
        List<ButtonWidget.Builder> buttonBuilders = new ArrayList<>();
        int buttonWidth = 100;
        int buttonPadding = 7;

        // 1. "Dismiss" button: Show for "What's New" and optional announcements, but NOT for "Update Available".
        if (!AnnouncementManager.PendingNotification.UPDATE_AVAILABLE.equals(this.reason)) {
            buttonBuilders.add(ButtonWidget.builder(Text.translatable("gui.adorablehamsterpets.announcement.button.dismiss"), button -> {
                PatchouliIntegration.setEntryAsRead(this.virtualEntry);
                AnnouncementManager.INSTANCE.markAsSeen(announcement.id());
                if (announcement.mandatory()) {
                    AnnouncementManager.INSTANCE.setLastAcknowledgedUpdate(announcement.semver());
                }
                this.returnToBook();
            }).tooltip(Tooltip.of(Text.translatable("gui.adorablehamsterpets.announcement.button.dismiss.tooltip"))));
        }

        // 2. "Disable These" button: Only show for non-mandatory (optional) announcements.
        if (!announcement.mandatory()) {
            buttonBuilders.add(ButtonWidget.builder(Text.translatable("gui.adorablehamsterpets.announcement.button.disable"), button -> {
                AnnouncementManager.INSTANCE.setOptOut(true);
                this.close();
            }).tooltip(Tooltip.of(Text.translatable("gui.adorablehamsterpets.announcement.button.disable.tooltip"))));
        }

        // 3. "Remind me Later" and "Changelog" buttons: Only for update-related announcements.
        if ("update".equals(announcement.kind())) {
            // "Remind me Later" = ONLY for "Update Available".
            if (AnnouncementManager.PendingNotification.UPDATE_AVAILABLE.equals(this.reason)) {
                // Build the tooltip dynamically based on snooze state.
                MutableText remindLaterTooltip = Text.translatable("gui.adorablehamsterpets.announcement.button.remind_later.tooltip", Configs.AHP.snoozeUpdateReminderDays.get());
                ClientAnnouncementState state = AnnouncementManager.INSTANCE.getClientState();
                Instant snoozeUntil = state.snoozed_ids().get(announcement.id());

                if (snoozeUntil != null && snoozeUntil.isAfter(Instant.now())) {
                    Duration remaining = Duration.between(Instant.now(), snoozeUntil);
                    remindLaterTooltip.append("\n\n").append(formatDuration(remaining));
                }

                buttonBuilders.add(ButtonWidget.builder(Text.translatable("gui.adorablehamsterpets.announcement.button.remind_later"), button -> {
                    // --- Primary Action (Always Safe) ---
                    AnnouncementManager.INSTANCE.setSnooze(announcement.id(), Configs.AHP.snoozeUpdateReminderDays.get());

                    // --- Context-Aware Patchouli Action (Must be deferred if on title screen) ---
                    if (this.parentScreen instanceof TitleScreen) {
                        AnnouncementManager.INSTANCE.queueDeferredReadMark(this.virtualEntry.getId());
                    } else {
                        PatchouliIntegration.setEntryAsRead(this.virtualEntry);
                    }

                    this.returnToBook();
                }).tooltip(Tooltip.of(remindLaterTooltip)));
            }

            // "Changelog" button is for ANY update note.
            String changelogUrl = String.format("https://modrinth.com/mod/adorable-hamster-pets/version/%s-1.20.1+fabric", announcement.semver());
            buttonBuilders.add(ButtonWidget.builder(Text.translatable("gui.adorablehamsterpets.announcement.button.changelog"), button -> {
                Util.getOperatingSystem().open(URI.create(changelogUrl));
            }).tooltip(Tooltip.of(Text.translatable("gui.adorablehamsterpets.announcement.button.changelog.tooltip"))));
        }

        // 4. Discord Button (always present)
        buttonBuilders.add(ButtonWidget.builder(Text.translatable("gui.adorablehamsterpets.announcement.button.discord"), button -> {
            Util.getOperatingSystem().open(URI.create("https://discord.gg/w54mk5bqdf"));
        }).tooltip(Tooltip.of(Text.translatable("config.adorablehamsterpets.main.helpAndResources.joinDiscord.desc"))));


        // --- Dynamic Layout Calculation ---
        int totalButtonWidth = buttonBuilders.size() * buttonWidth + Math.max(0, buttonBuilders.size() - 1) * buttonPadding;
        int startX = (this.width / 2) - (totalButtonWidth / 2);
        int buttonY = this.guiTop + BACKGROUND_HEIGHT + 7; // Position below the main UI

        // --- Build and Add Buttons ---
        for (int i = 0; i < buttonBuilders.size(); i++) {
            ButtonWidget.Builder builder = buttonBuilders.get(i);
            int currentX = startX + i * (buttonWidth + buttonPadding);
            // Use ScreenWidgetAdder accessor to add the widget for cross-loader compatibility
            ((ScreenWidgetAdder)(Object)this).adorablehamsterpets$addWidget(builder.dimensions(currentX, buttonY, buttonWidth, 20).build());
        }
    }

    /**
     * Formats a Duration into a human-readable string (e.g., "2 days, 5 hours").
     *
     * @param duration The duration to format.
     * @return A Text component representing the formatted duration.
     */
    private Text formatDuration(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();

        List<String> parts = new ArrayList<>();
        if (days > 0) {
            parts.add(days + (days == 1 ? " day" : " days"));
        }
        if (hours > 0) {
            parts.add(hours + (hours == 1 ? " hour" : " hours"));
        }
        // Only show minutes if the total duration is less than a day for brevity
        if (days == 0 && minutes > 0) {
            parts.add(minutes + (minutes == 1 ? " minute" : " minutes"));
        }

        String formattedString = String.join(", ", parts);
        if (formattedString.isEmpty()) {
            formattedString = "less than a minute";
        }

        return Text.translatable("tooltip.adorablehamsterpets.hud.snooze_remaining", formattedString)
                .formatted(Formatting.GRAY);
    }

    /**
     * Closes this screen and returns the client to the parent Patchouli screen.
     */
    private void returnToBook() {
        if (this.client != null) {
            // If parentScreen is null, it means it was opened from the HUD, so return the player to the game.
            this.client.setScreen(this.parentScreen);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {

        // --- 1. Update Hovered Style ---
        // Find what style is under the cursor. If it's a link, store it.
        Text textAtMouse = this.getTextAt(mouseX, mouseY);
        this.hoveredStyle = (textAtMouse != null && textAtMouse.getStyle().getClickEvent() != null) ? textAtMouse.getStyle() : null;

        // --- 2. Render Background ---
        // This draws the background texture once, at the bottom of the render stack.
        this.renderBackground(context);

        // --- 2. Render Markdown Content within Scissor Box ---
        context.enableScissor(
                this.guiLeft + CONTENT_X_OFFSET,
                this.guiTop + CONTENT_Y_OFFSET,
                this.guiLeft + CONTENT_X_OFFSET + CONTENT_WIDTH,
                this.guiTop + CONTENT_Y_OFFSET + CONTENT_HEIGHT
        );
        // Pass the currently hovered style to the renderer so it can adjust colors dynamically.
        markdownRenderer.render(context, (int) scrollY, this.hoveredStyle);
        context.disableScissor();

        // --- 3. Render Scrollbar ---
        int maxScroll = Math.max(0, markdownRenderer.getTotalHeight() - CONTENT_HEIGHT);
        if (maxScroll > 0) {
            int scrollbarX = this.guiLeft + SCROLLBAR_X_OFFSET;
            int scrollbarTravel = SCROLLBAR_END_Y - SCROLLBAR_START_Y;
            double scrollPercent = this.scrollY / maxScroll;
            int scrollbarY = this.guiTop + SCROLLBAR_START_Y + (int) (scrollPercent * scrollbarTravel);
            context.drawTexture(SCROLLBAR_TEXTURE, scrollbarX, scrollbarY, 0, 0, 4, 25, 4, 25);
        }

        // --- 4. Render Dynamic Title ---
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        float defaultScale = 1.3f;
        int maxWidth = 222; // Total is 242, so this creates 10 pixels of padding on each side of the title
        Text boldTitle = this.title.copy().formatted(Formatting.BOLD);
        int titleWidth = MinecraftClient.getInstance().textRenderer.getWidth(boldTitle);
        float finalScale = defaultScale;

        if (titleWidth * defaultScale > maxWidth) {
            finalScale = (float) maxWidth / titleWidth;
        }

        matrices.translate(this.width / 2.0, this.guiTop + 9, 0);
        matrices.scale(finalScale, finalScale, 1.0f);
        context.drawText(MinecraftClient.getInstance().textRenderer, boldTitle, -titleWidth / 2, 0, 0x323232, false);
        matrices.pop();


        // --- 5. Render Widgets (Buttons) ---
        // This replicates the logic from `super.render()` to draw all elements added via ScreenWidgetAdder accessor for cross loader compatibility
        // but crucially, it does NOT call `renderBackground()` again.
        for (Element element : this.children()) {
            if (element instanceof Drawable drawable) {
                drawable.render(context, mouseX, mouseY, delta);
            }
        }
    }

    @Override
    public void renderBackground(DrawContext context) {
        // Render the default dark overlay
        super.renderBackground(context);

        // Custom Background Texture
        context.drawTexture(BACKGROUND_TEXTURE, this.guiLeft, this.guiTop, 0, 0, BACKGROUND_WIDTH, BACKGROUND_HEIGHT);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int maxScroll = Math.max(0, markdownRenderer.getTotalHeight() - CONTENT_HEIGHT);
        this.scrollY = MathHelper.clamp(this.scrollY - amount * 10, 0, maxScroll);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Get Style from Text
            Text clickedText = this.getTextAt(mouseX, mouseY);
            if (clickedText != null) {
                Style style = clickedText.getStyle();
                // Let the screen's native handler process the click event
                if (this.handleTextClick(style)) {
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() {
        return true;
    }

    @Override
    public void close() {
        this.returnToBook();
    }

    @Nullable
    public Text getTextAt(double x, double y) {
        // --- 1. Calculate Mouse Position Relative to Scrollable Content ---
        int scrollOffset = (int) (this.guiTop + CONTENT_Y_OFFSET - this.scrollY);
        int relativeY = (int) y - scrollOffset;

        if (relativeY < 0) {
            return null; // Mouse is above the content area
        }

        int currentY = 0;
        for (String originalLine : markdownRenderer.lines) {
            String trimmedLine = originalLine.trim();
            int lineHeight;
            int startX = this.guiLeft + CONTENT_X_OFFSET;
            int contentWidth = markdownRenderer.width;
            String content = trimmedLine;
            boolean isListItem = trimmedLine.startsWith("- ") || trimmedLine.startsWith("* ") || trimmedLine.matches("^\\d+\\.\\s.*");

            // --- 2. Calculate the Height of the Current Line Block ---
            if (trimmedLine.isEmpty()) {
                lineHeight = textRenderer.fontHeight / 2;
            } else if (trimmedLine.startsWith("#")) {
                int level = 0;
                while (level < trimmedLine.length() && trimmedLine.charAt(level) == '#') level++;
                String text = trimmedLine.substring(level).trim();
                float scale = Math.max(1.0f, 2.0f - (level - 1) * 0.25f);
                int scaledWidth = (int) (markdownRenderer.width / scale);
                MutableText styledText = Text.literal(text).setStyle(Style.EMPTY.withBold(true));
                List<OrderedText> wrappedLines = MinecraftClient.getInstance().textRenderer.wrapLines(styledText, scaledWidth);
                int heightOfLines = wrappedLines.size() * (int) (textRenderer.fontHeight * scale);
                int totalSpacing = Math.max(0, wrappedLines.size() - 1) * MarkdownRenderer.LINE_SPACING;
                lineHeight = heightOfLines + totalSpacing + MarkdownRenderer.HEADING_BOTTOM_MARGIN;
            } else if (trimmedLine.equals("---")) {
                lineHeight = MarkdownRenderer.DIVIDER_HEIGHT;
            } else {
                int indentationLevel = MarkdownRenderer.getIndentationLevel(originalLine);
                if (isListItem) {
                    contentWidth = markdownRenderer.width - ((indentationLevel + 1) * MarkdownRenderer.LIST_INDENT);
                    startX += (indentationLevel * MarkdownRenderer.LIST_INDENT) + MarkdownRenderer.LIST_INDENT;
                    if (trimmedLine.matches("^\\d+\\.\\s.*")) {
                        content = trimmedLine.substring(trimmedLine.indexOf('.') + 1).trim();
                    } else {
                        content = trimmedLine.substring(1).trim();
                    }
                } else {
                    contentWidth = markdownRenderer.width - (indentationLevel * MarkdownRenderer.LIST_INDENT);
                    startX += indentationLevel * MarkdownRenderer.LIST_INDENT;
                    content = originalLine.substring(indentationLevel * MarkdownRenderer.SPACES_PER_INDENT_LEVEL);
                }
                MutableText styledText = markdownRenderer.parseLineToText(content, null);
                List<OrderedText> wrappedLines = textRenderer.wrapLines(styledText, contentWidth);
                lineHeight = wrappedLines.size() * (textRenderer.fontHeight + MarkdownRenderer.LINE_SPACING);
            }

            // --- 3. Check if Mouse is Within This Line Block's Vertical Bounds ---
            if (relativeY >= currentY && relativeY < currentY + lineHeight) {
                // --- 4. Find the Specific Style (for paragraphs and list items) ---
                if (!trimmedLine.isEmpty() && !trimmedLine.startsWith("#") && !trimmedLine.equals("---")) {
                    // Re-calculate contentWidth and startX for this specific block type to pass to the text handler
                    int indentationLevel = MarkdownRenderer.getIndentationLevel(originalLine);
                    if (isListItem) {
                        contentWidth = markdownRenderer.width - ((indentationLevel + 1) * MarkdownRenderer.LIST_INDENT);
                    } else {
                        contentWidth = markdownRenderer.width - (indentationLevel * MarkdownRenderer.LIST_INDENT);
                    }

                    MutableText styledText = markdownRenderer.parseLineToText(content, null);
                    List<OrderedText> wrappedLines = textRenderer.wrapLines(styledText, contentWidth);
                    int yInBlock = relativeY - currentY;
                    int lineIndex = yInBlock / (textRenderer.fontHeight + MarkdownRenderer.LINE_SPACING);

                    if (lineIndex >= 0 && lineIndex < wrappedLines.size()) {
                        OrderedText orderedText = wrappedLines.get(lineIndex);
                        int relativeX = (int) x - startX;
                        Style style = this.client.textRenderer.getTextHandler().getStyleAt(orderedText, relativeX);
                        if (style != null && style.getClickEvent() != null) {
                            return Text.literal("").setStyle(style);
                        }
                    }
                }
                return null; // Inside the block's height, but not on a clickable element
            }

            // --- 5. Move to the Next Line Block ---
            currentY += lineHeight;
        }
        return null;
    }
}