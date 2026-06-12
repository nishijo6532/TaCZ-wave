package com.tacz.guns.client.gui.config;

import com.tacz.guns.client.renderer.crosshair.CrosshairType;
import com.tacz.guns.config.client.KeyConfig;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.config.client.ResourceConfig;
import com.tacz.guns.config.client.ZoomConfig;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.common.ForgeConfigSpec;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class TaczConfigScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private static final int BUTTON_WIDTH = 240;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ARROW_WIDTH = 24;
    private static final int ARROW_GAP = 2;

    private final @Nullable Screen parent;
    private Page page = Page.KEY;
    private final List<ScrollButton> scrollButtons = new ArrayList<>();
    private int scrollOffset;
    private int contentHeight;

    public TaczConfigScreen(@Nullable Screen parent) {
        super(GameNarrator.NO_TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.rebuildPage();
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, Component.literal("TaCZ: WAVE Config"), this.width / 2, 18, 0xFFFFFF);
        graphics.centeredText(this.font, Component.translatable(this.page.langKey), this.width / 2, 42, 0xA0A0A0);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScroll = this.maxScroll();
        if (maxScroll <= 0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        int nextOffset = Mth.clamp(this.scrollOffset - (int) Math.round(scrollY * ROW_HEIGHT), 0, maxScroll);
        if (nextOffset == this.scrollOffset) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        this.scrollOffset = nextOffset;
        this.positionScrollContent();
        return true;
    }

    private void rebuildPage() {
        this.clearWidgets();
        this.scrollButtons.clear();
        this.addPageButtons();
        int y = 0;
        switch (this.page) {
            case KEY -> {
                y = this.addBooleanRow(y, "config.tacz.client.key.hold_to_aim", KeyConfig.HOLD_TO_AIM);
                y = this.addBooleanRow(y, "config.tacz.client.key.hold_to_crawl", KeyConfig.HOLD_TO_CRAWL);
                y = this.addBooleanRow(y, "config.tacz.client.key.auto_reload", KeyConfig.AUTO_RELOAD);
            }
            case RENDER -> {
                y = this.addEnumStepperRow(y, "config.tacz.client.render.crosshair_type", RenderConfig.CROSSHAIR_TYPE, CrosshairType.values());
                y = this.addBooleanRow(y, "config.tacz.client.render.gun_hud_enable", RenderConfig.GUN_HUD_ENABLE);
                y = this.addBooleanRow(y, "config.tacz.client.render.kill_amount_enable", RenderConfig.KILL_AMOUNT_ENABLE);
                y = this.addBooleanRow(y, "config.tacz.client.render.first_person_bullet_tracer_enable", RenderConfig.FIRST_PERSON_BULLET_TRACER_ENABLE);
                y = this.addBooleanRow(y, "config.tacz.client.render.disable_interact_hud_text", RenderConfig.DISABLE_INTERACT_HUD_TEXT);
                y = this.addBooleanRow(y, "config.tacz.client.render.disable_movement_fov", RenderConfig.DISABLE_MOVEMENT_ATTRIBUTE_FOV);
                y = this.addBooleanRow(y, "config.tacz.client.render.enable_tooltip_id", RenderConfig.ENABLE_TACZ_ID_IN_TOOLTIP);
                y = this.addBooleanRow(y, "config.tacz.client.render.enable_translucent", RenderConfig.BLOCK_ENTITY_TRANSLUCENT);
                y = this.addBooleanRow(y, "config.tacz.client.render.laser_fadeout", RenderConfig.ENABLE_LASER_FADE_OUT);
            }
            case ZOOM -> {
                y = this.addDoubleStepperRow(y, "config.tacz.client.zoom.screen_distance_coefficient", ZoomConfig.SCREEN_DISTANCE_COEFFICIENT, 0.1D, 0D, 3D);
                y = this.addDoubleStepperRow(y, "config.tacz.client.zoom.zoom_sensitivity_base_multiplier", ZoomConfig.ZOOM_SENSITIVITY_BASE_MULTIPLIER, 0.1D, 0D, 2D);
            }
            case RESOURCE -> y = this.addBooleanRow(y, "config.tacz.client.resource.enable_lazy_client_asset_load", ResourceConfig.ENABLE_LAZY_CLIENT_ASSET_LOAD);
        }
        this.contentHeight = y;
        this.scrollOffset = Mth.clamp(this.scrollOffset, 0, this.maxScroll());
        this.positionScrollContent();
        this.addRenderableWidget(Button.builder(Component.literal("Done"), button -> this.onClose())
                .bounds((this.width - 120) / 2, this.height - 32, 120, BUTTON_HEIGHT)
                .build());
    }

    private void addPageButtons() {
        int totalWidth = Page.values().length * 82;
        int x = (this.width - totalWidth) / 2;
        for (Page target : Page.values()) {
            Button button = Button.builder(Component.translatable(target.langKey), b -> {
                        this.page = target;
                        this.scrollOffset = 0;
                        this.rebuildPage();
                    })
                    .bounds(x, 48, 78, BUTTON_HEIGHT)
                    .build();
            button.active = this.page != target;
            this.addRenderableWidget(button);
            x += 82;
        }
    }

    private int addBooleanRow(int y, String labelKey, ForgeConfigSpec.BooleanValue value) {
        int x = (this.width - BUTTON_WIDTH) / 2;
        Button button = Button.builder(booleanLabel(labelKey, value.get()), b -> {
                    value.set(!value.get());
                    value.save();
                    b.setMessage(booleanLabel(labelKey, value.get()));
                })
                .bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        this.addRenderableWidget(button);
        this.scrollButtons.add(new ScrollButton(button, y));
        return y + ROW_HEIGHT;
    }

    private <T extends Enum<T>> int addEnumStepperRow(int y, String labelKey, ForgeConfigSpec.EnumValue<T> value, T[] values) {
        int centerWidth = BUTTON_WIDTH - (ARROW_WIDTH + ARROW_GAP) * 2;
        int x = (this.width - BUTTON_WIDTH) / 2;
        Button left = Button.builder(Component.literal("<"), b -> this.setEnumStepperValue(value, values, -1))
                .bounds(x, y, ARROW_WIDTH, BUTTON_HEIGHT)
                .build();
        Button center = Button.builder(enumLabel(labelKey, value.get()), b -> this.openEnumSelection(labelKey, value, values))
                .bounds(x + ARROW_WIDTH + ARROW_GAP, y, centerWidth, BUTTON_HEIGHT)
                .build();
        Button right = Button.builder(Component.literal(">"), b -> this.setEnumStepperValue(value, values, 1))
                .bounds(x + ARROW_WIDTH + ARROW_GAP + centerWidth + ARROW_GAP, y, ARROW_WIDTH, BUTTON_HEIGHT)
                .build();
        this.addRenderableWidget(left);
        this.addRenderableWidget(center);
        this.addRenderableWidget(right);
        this.scrollButtons.add(new ScrollButton(left, y));
        this.scrollButtons.add(new ScrollButton(center, y));
        this.scrollButtons.add(new ScrollButton(right, y));
        return y + ROW_HEIGHT;
    }

    private int addDoubleStepperRow(int y, String labelKey, ForgeConfigSpec.DoubleValue value, double step, double min, double max) {
        int centerWidth = BUTTON_WIDTH - (ARROW_WIDTH + ARROW_GAP) * 2;
        int x = (this.width - BUTTON_WIDTH) / 2;
        Button left = Button.builder(Component.literal("<"), b -> this.setDoubleStepperValue(value, -step, min, max))
                .bounds(x, y, ARROW_WIDTH, BUTTON_HEIGHT)
                .build();
        Button center = Button.builder(doubleLabel(labelKey, value.get()), b -> this.openDoubleSelection(labelKey, value, step, min, max))
                .bounds(x + ARROW_WIDTH + ARROW_GAP, y, centerWidth, BUTTON_HEIGHT)
                .build();
        Button right = Button.builder(Component.literal(">"), b -> this.setDoubleStepperValue(value, step, min, max))
                .bounds(x + ARROW_WIDTH + ARROW_GAP + centerWidth + ARROW_GAP, y, ARROW_WIDTH, BUTTON_HEIGHT)
                .build();
        this.addRenderableWidget(left);
        this.addRenderableWidget(center);
        this.addRenderableWidget(right);
        this.scrollButtons.add(new ScrollButton(left, y));
        this.scrollButtons.add(new ScrollButton(center, y));
        this.scrollButtons.add(new ScrollButton(right, y));
        return y + ROW_HEIGHT;
    }

    private <T extends Enum<T>> void setEnumStepperValue(ForgeConfigSpec.EnumValue<T> value, T[] values, int direction) {
        int next = Math.floorMod(value.get().ordinal() + direction, values.length);
        value.set(values[next]);
        value.save();
        this.rebuildPage();
    }

    private void setDoubleStepperValue(ForgeConfigSpec.DoubleValue value, double delta, double min, double max) {
        double next = value.get() + delta;
        if (next > max + 0.0001D) {
            next = min;
        } else if (next < min - 0.0001D) {
            next = max;
        }
        value.set(Math.round(next * 100.0D) / 100.0D);
        value.save();
        this.rebuildPage();
    }

    private <T extends Enum<T>> void openEnumSelection(String labelKey, ForgeConfigSpec.EnumValue<T> value, T[] values) {
        List<SelectEntry> entries = new ArrayList<>();
        T current = value.get();
        for (T option : values) {
            entries.add(new SelectEntry(Component.literal(option.name()), () -> {
                value.set(option);
                value.save();
            }, option == current));
        }
        this.openSelectionScreen(Component.translatable(labelKey), entries);
    }

    private void openDoubleSelection(String labelKey, ForgeConfigSpec.DoubleValue value, double step, double min, double max) {
        List<SelectEntry> entries = new ArrayList<>();
        double current = value.get();
        int count = (int) Math.round((max - min) / step);
        for (int i = 0; i <= count; i++) {
            double option = Math.round((min + step * i) * 100.0D) / 100.0D;
            entries.add(new SelectEntry(Component.literal(String.format("%.2f", option)), () -> {
                value.set(option);
                value.save();
            }, Math.abs(option - current) < 0.0001D));
        }
        this.openSelectionScreen(Component.translatable(labelKey), entries);
    }

    private void openSelectionScreen(Component title, List<SelectEntry> entries) {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new SelectionScreen(title, entries));
        }
    }

    private void positionScrollContent() {
        int top = this.contentTop();
        int bottom = this.contentBottom();
        for (ScrollButton scrollButton : this.scrollButtons) {
            Button button = scrollButton.button;
            int y = top + scrollButton.baseY - this.scrollOffset;
            button.setY(y);
            button.visible = y >= top && y + BUTTON_HEIGHT <= bottom;
            button.active = button.visible;
        }
    }

    private int contentTop() {
        return 74;
    }

    private int contentBottom() {
        return this.height - 40;
    }

    private int maxScroll() {
        return Math.max(0, this.contentHeight - (this.contentBottom() - this.contentTop()));
    }

    private static Component booleanLabel(String labelKey, boolean value) {
        return Component.empty()
                .append(Component.translatable(labelKey))
                .append(": ")
                .append(Component.translatable(value ? "options.on" : "options.off"));
    }

    private static Component enumLabel(String labelKey, Enum<?> value) {
        return Component.empty()
                .append(Component.translatable(labelKey))
                .append(": ")
                .append(Component.literal(value.name()));
    }

    private static Component doubleLabel(String labelKey, double value) {
        return Component.empty()
                .append(Component.translatable(labelKey))
                .append(": ")
                .append(Component.literal(String.format("%.2f", value)));
    }

    private enum Page {
        KEY("config.tacz.client.key"),
        RENDER("config.tacz.client.render"),
        ZOOM("config.tacz.client.zoom"),
        RESOURCE("config.tacz.client.resource");

        private final String langKey;

        Page(String langKey) {
            this.langKey = langKey;
        }
    }

    private record ScrollButton(Button button, int baseY) {
    }

    private record SelectEntry(Component label, Runnable action, boolean selected) {
    }

    private class SelectionScreen extends Screen {
        private final Component selectionTitle;
        private final List<SelectEntry> entries;
        private final List<ScrollButton> optionButtons = new ArrayList<>();
        private int selectionScrollOffset;
        private int selectionContentHeight;

        private SelectionScreen(Component selectionTitle, List<SelectEntry> entries) {
            super(GameNarrator.NO_TITLE);
            this.selectionTitle = selectionTitle;
            this.entries = entries;
        }

        @Override
        protected void init() {
            this.clearWidgets();
            this.optionButtons.clear();
            int y = 0;
            int x = (this.width - BUTTON_WIDTH) / 2;
            for (SelectEntry entry : this.entries) {
                Component label = entry.selected ? Component.literal("> ").append(entry.label) : entry.label;
                Button button = Button.builder(label, b -> {
                            entry.action.run();
                            if (this.minecraft != null) {
                                this.minecraft.setScreen(TaczConfigScreen.this);
                            }
                        })
                        .bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                        .build();
                this.addRenderableWidget(button);
                this.optionButtons.add(new ScrollButton(button, y));
                y += ROW_HEIGHT;
            }
            this.selectionContentHeight = y;
            this.selectionScrollOffset = Mth.clamp(this.selectionScrollOffset, 0, this.selectionMaxScroll());
            this.positionSelectionContent();
            this.addRenderableWidget(Button.builder(Component.literal("Back"), button -> this.onClose())
                    .bounds((this.width - 120) / 2, this.height - 32, 120, BUTTON_HEIGHT)
                    .build());
        }

        @Override
        public void onClose() {
            if (this.minecraft != null) {
                this.minecraft.setScreen(TaczConfigScreen.this);
            }
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
            graphics.centeredText(this.font, this.selectionTitle, this.width / 2, 18, 0xFFFFFF);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            int maxScroll = this.selectionMaxScroll();
            if (maxScroll <= 0) {
                return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
            }
            int nextOffset = Mth.clamp(this.selectionScrollOffset - (int) Math.round(scrollY * ROW_HEIGHT), 0, maxScroll);
            if (nextOffset == this.selectionScrollOffset) {
                return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
            }
            this.selectionScrollOffset = nextOffset;
            this.positionSelectionContent();
            return true;
        }

        private void positionSelectionContent() {
            int top = this.selectionContentTop();
            int bottom = this.selectionContentBottom();
            for (ScrollButton scrollButton : this.optionButtons) {
                Button button = scrollButton.button;
                int y = top + scrollButton.baseY - this.selectionScrollOffset;
                button.setY(y);
                button.visible = y >= top && y + BUTTON_HEIGHT <= bottom;
                button.active = button.visible;
            }
        }

        private int selectionContentTop() {
            return 42;
        }

        private int selectionContentBottom() {
            return this.height - 40;
        }

        private int selectionMaxScroll() {
            return Math.max(0, this.selectionContentHeight - (this.selectionContentBottom() - this.selectionContentTop()));
        }
    }
}
