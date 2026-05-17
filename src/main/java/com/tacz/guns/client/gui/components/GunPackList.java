package com.tacz.guns.client.gui.components;

import com.tacz.guns.client.gui.GunSmithTableScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GunPackList extends Button {
    private final Set<String> namespaceSet = new LinkedHashSet<>();
    private String searchText = "";
    private boolean byHandSelected = false;

    public GunPackList(Minecraft pMinecraft, int pWidth, int pHeight, int pY0, int pY1, int pItemHeight,
                       Map<Identifier, List<Identifier>> recipes, GunSmithTableScreen screen) {
        super(0, pY0, pWidth, pY1 - pY0, Component.literal(""), b -> {
        }, DEFAULT_NARRATION);
        recipes.values().forEach(ids -> ids.forEach(id -> namespaceSet.add(id.getNamespace())));
    }

    public void updateSize(int width, int height, int y0, int y1) {
        this.setWidth(width);
        this.setY(y0);
        this.height = y1 - y0;
    }

    public void setLeftPos(int leftPos) {
        this.setX(leftPos);
    }

    public Set<String> namespaceList() {
        return namespaceSet;
    }

    public String getSearchText() {
        return searchText;
    }

    public boolean isByHandSelected() {
        return byHandSelected;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor guiGraphicsExtractor, int i, int i1, float v) {
    }
}
