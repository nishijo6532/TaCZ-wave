package com.tacz.guns.client.gui.components.refit;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.util.AttachmentDataUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class GunPropertyDiagrams {
    public static int getHidePropertyButtonYOffset() {
        int[] startYOffset = new int[]{49};
        AttachmentPropertyManager.getModifiers().forEach((key, value) -> {
            startYOffset[0] += value.getDiagramsDataSize() * 10;
        });
        return startYOffset[0];
    }

    public static void draw(GuiGraphicsExtractor graphics, Font font, int x, int y, int mouseX, int mouseY) {
        graphics.fill(x, y, x + 288, y + getHidePropertyButtonYOffset() - 11, 0xAF222222);

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ItemStack gunItem = player.getMainHandItem();
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) {
            return;
        }
        AttachmentCacheProperty cacheProperty = IGunOperator.fromLivingEntity(player).getCacheProperty();
        if (cacheProperty == null) {
            return;
        }
        Identifier gunId = iGun.getGunId(gunItem);
        TimelessAPI.getCommonGunIndex(gunId).ifPresent(index -> {
            GunData gunData = index.getGunData();
            FireMode fireMode = iGun.getFireMode(gunItem);

            int barStartX = x + 83;
            int barMaxWidth = 120;
            int barEndX = barStartX + barMaxWidth;

            int barBackgroundColor = 0xFF000000;
            int barBaseColor = 0xFFFFFFFF;
            int barPositivelyColor = 0xFF_55FF55;
            int barNegativeColor = 0xFF_FF5555;

            int fontColor = 0xFFCCCCCC;
            int nameTextStartX = x + 5;
            int valueTextStartX = x + 210;

            int[] yOffset = new int[]{y + 5};

            // 蟆・・讓｡蠑・
            MutableComponent fireModeText = Component.translatable("gui.tacz.gun_refit.property_diagrams.fire_mode");
            if (fireMode == FireMode.AUTO) {
                fireModeText.append(Component.translatable("gui.tacz.gun_refit.property_diagrams.auto"));
            } else if (fireMode == FireMode.SEMI) {
                fireModeText.append(Component.translatable("gui.tacz.gun_refit.property_diagrams.semi"));
            } else if (fireMode == FireMode.BURST) {
                fireModeText.append(Component.translatable("gui.tacz.gun_refit.property_diagrams.burst"));
            } else {
                fireModeText.append(Component.translatable("gui.tacz.gun_refit.property_diagrams.unknown"));
            }

            graphics.text(font, fireModeText, nameTextStartX+12, yOffset[0], fontColor, false);
            drawRowTooltip(graphics, font, mouseX, mouseY, x, yOffset[0], fireModeText);

            yOffset[0] += 10;


            // 蠑ｹ蛹｣螳ｹ驥・
            if (iGun.useInventoryAmmo(gunItem)) {
                // 螯よ棡菴ｿ逕ｨ閭悟桁逶ｴ隸ｻ・悟・逶ｴ謗･譏ｾ遉ｺ貊｡譚｡蜥・INV 逧・・ｳｨ
                graphics.text(font, Component.translatable("gui.tacz.gun_refit.property_diagrams.ammo_capacity"), nameTextStartX, yOffset[0], fontColor, false);
                graphics.fill(barStartX, yOffset[0] + 2, barEndX, yOffset[0] + 6, barBackgroundColor);
                graphics.fill(barStartX, yOffset[0] + 2, barStartX + barMaxWidth, yOffset[0] + 6, barBaseColor);
                graphics.text(font, Component.literal("INV"), valueTextStartX, yOffset[0], fontColor, false);
                drawRowTooltip(graphics, font, mouseX, mouseY, x, yOffset[0],
                        Component.translatable("gui.tacz.gun_refit.property_diagrams.ammo_capacity"),
                        Component.literal("INV"));
            } else {
                int barrelBulletAmount = (iGun.hasBulletInBarrel(gunItem) && index.getGunData().getBolt() != Bolt.OPEN_BOLT) ? 1 : 0;
                int ammoAmount = gunData.getAmmoAmount() + barrelBulletAmount;
                double ammoAmountPercent = Math.min(ammoAmount / 100.0, 1);
                int ammoLength = (int) (barStartX + barMaxWidth * ammoAmountPercent);
                int maxAmmoCount = AttachmentDataUtils.getAmmoCountWithAttachment(gunItem, index.getGunData()) + barrelBulletAmount;
                int addAmmoCount = Math.max(maxAmmoCount - ammoAmount, 0);
                int addAmmoCountLength = (int) (barMaxWidth * addAmmoCount / 100.0);

                graphics.text(font, Component.translatable("gui.tacz.gun_refit.property_diagrams.ammo_capacity"), nameTextStartX, yOffset[0], fontColor, false);
                graphics.fill(barStartX, yOffset[0] + 2, barEndX, yOffset[0] + 6, barBackgroundColor);
                graphics.fill(barStartX, yOffset[0] + 2, ammoLength, yOffset[0] + 6, barBaseColor);
                if (addAmmoCount > 0) {
                    int barRight = Math.min(ammoLength + addAmmoCountLength, barEndX);
                    graphics.fill(ammoLength, yOffset[0] + 2, barRight, yOffset[0] + 6, barPositivelyColor);
                    drawRowTooltip(graphics, font, mouseX, mouseY, x, yOffset[0],
                            Component.translatable("gui.tacz.gun_refit.property_diagrams.ammo_capacity"),
                            Component.literal(String.format("%d -> %d (+%d)", ammoAmount, maxAmmoCount, addAmmoCount)));
                    String baseText = String.format("%d", ammoAmount);
                    String addText = String.format(" (+%d)", addAmmoCount);
                    graphics.text(font, baseText, valueTextStartX, yOffset[0], fontColor, false);
                    graphics.text(font, addText, valueTextStartX + font.width(baseText), yOffset[0], barPositivelyColor, false);
                } else {
                    graphics.text(font, String.format("%d", ammoAmount), valueTextStartX, yOffset[0], fontColor, false);
                    drawRowTooltip(graphics, font, mouseX, mouseY, x, yOffset[0],
                            Component.translatable("gui.tacz.gun_refit.property_diagrams.ammo_capacity"),
                            Component.literal(String.format("%d", ammoAmount)));
                }
            }

            yOffset[0] += 10;


            // 霍大ｰ・ｻｶ霑・
            float sprintTime = gunData.getSprintTime();
            double sprintTimePercent = Mth.clamp(sprintTime / 0.5, 0, 1);
            int sprintLength = (int) (barStartX + barMaxWidth * sprintTimePercent);
            String sprintValueText = String.format("%.2fs", sprintTime);

            graphics.text(font, Component.translatable("gui.tacz.gun_refit.property_diagrams.sprint_time"), nameTextStartX, yOffset[0], fontColor, false);
            graphics.fill(barStartX, yOffset[0] + 2, barEndX, yOffset[0] + 6, barBackgroundColor);
            graphics.fill(barStartX, yOffset[0] + 2, sprintLength, yOffset[0] + 6, barBaseColor);
            graphics.text(font, sprintValueText, valueTextStartX, yOffset[0], fontColor, false);
            drawRowTooltip(graphics, font, mouseX, mouseY, x, yOffset[0],
                    Component.translatable("gui.tacz.gun_refit.property_diagrams.sprint_time"),
                    Component.literal(sprintValueText));

            yOffset[0] += 10;

            AttachmentPropertyManager.getModifiers().forEach((key, value) -> value.getPropertyDiagramsData(gunItem, gunData, cacheProperty).forEach(data -> {
                double defaultPercent = data.defaultPercent();
                double modifierPercent = data.modifierPercent();
                double modifier = data.modifier().doubleValue();
                String titleKey = data.titleKey();
                String positivelyString = data.positivelyString();
                String negativeString = data.negativeString();
                String defaultString = data.defaultString();
                boolean positivelyBetter = data.positivelyBetter();

                defaultPercent = Mth.clamp(defaultPercent, 0, 1);
                int defaultLength = (int) (barStartX + barMaxWidth * defaultPercent);
                int modifierLength = Mth.clamp(defaultLength + (int) (barMaxWidth * modifierPercent), barStartX, barEndX);

                graphics.text(font, Component.translatable(titleKey), nameTextStartX, yOffset[0], fontColor, false);
                graphics.fill(barStartX, yOffset[0] + 2, barEndX, yOffset[0] + 6, barBackgroundColor);
                graphics.fill(barStartX, yOffset[0] + 2, defaultLength, yOffset[0] + 6, barBaseColor);
                if (modifier > 0) {
                    int barColor = positivelyBetter ? barPositivelyColor : barNegativeColor;
                    graphics.fill(defaultLength, yOffset[0] + 2, modifierLength, yOffset[0] + 6, barColor);
                    graphics.text(font, positivelyString, valueTextStartX, yOffset[0], fontColor, false);
                    drawRowTooltip(graphics, font, mouseX, mouseY, x, yOffset[0],
                            Component.translatable(titleKey),
                            Component.literal(positivelyString));
                } else if (modifier < 0) {
                    int barColor = positivelyBetter ? barNegativeColor : barPositivelyColor;
                    graphics.fill(modifierLength, yOffset[0] + 2, defaultLength, yOffset[0] + 6, barColor);
                    graphics.text(font, negativeString, valueTextStartX, yOffset[0], fontColor, false);
                    drawRowTooltip(graphics, font, mouseX, mouseY, x, yOffset[0],
                            Component.translatable(titleKey),
                            Component.literal(negativeString));
                } else {
                    graphics.text(font, defaultString, valueTextStartX, yOffset[0], fontColor, false);
                    drawRowTooltip(graphics, font, mouseX, mouseY, x, yOffset[0],
                            Component.translatable(titleKey),
                            Component.literal(defaultString));
                }
                yOffset[0] += 10;
            }));
        });
    }

    private static void drawRowTooltip(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY,
                                       int x, int y, Component... lines) {
        if (mouseX >= x && mouseX < x + 288 && mouseY >= y && mouseY < y + 10) {
            graphics.renderComponentTooltip(font, List.of(lines), mouseX, mouseY, ItemStack.EMPTY);
        }
    }
}

