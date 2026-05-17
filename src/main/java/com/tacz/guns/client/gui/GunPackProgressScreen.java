package com.tacz.guns.client.gui;

import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProgressListener;

import javax.annotation.Nullable;

public class GunPackProgressScreen extends Screen implements ProgressListener {
    private @Nullable Component header;
    private @Nullable Component stage;
    private int progress;
    private boolean stop;

    public GunPackProgressScreen() {
        super(GameNarrator.NO_TITLE);
    }

    @Override
    protected void init() {
        Button button = Button.builder(
                Component.translatable("gui.tacz.client_gun_pack_downloader.background_download"), b -> this.stop()
        ).bounds((width - 200) / 2, 120, 200, 20).build();
        this.addRenderableWidget(button);
    }

    @Override
    public void progressStartNoAbort(Component component) {
        this.progressStart(component);
    }

    @Override
    public void progressStart(Component header) {
        this.header = Component.translatable("gui.tacz.client_gun_pack_downloader.downloading");
    }

    @Override
    public void progressStage(Component component) {
        this.stage = component;
        this.progressStagePercentage(0);
    }

    @Override
    public void progressStagePercentage(int progress) {
        this.progress = progress;
    }

    @Override
    public void stop() {
        this.stop = true;
    }

    @Override
    public void tick() {
        if (this.stop && this.getMinecraft() != null) {
            this.getMinecraft().setScreen(null);
        }
    }
}
