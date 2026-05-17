package com.tacz.guns.client.gui;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.animation.screen.RefitTransform;
import com.tacz.guns.client.gui.components.FlatColorButton;
import com.tacz.guns.client.gui.components.refit.*;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.client.sound.SoundPlayManager;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ClientMessageLaserColor;
import com.tacz.guns.network.message.ClientMessageRefitGun;
import com.tacz.guns.network.message.ClientMessageUnloadAttachment;
import com.tacz.guns.sound.SoundManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class GunRefitScreen extends Screen {
    public static final Identifier SLOT_TEXTURE = com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "textures/gui/refit_slot.png");
    public static final Identifier TURN_PAGE_TEXTURE = com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "textures/gui/refit_turn_page.png");
    public static final Identifier UNLOAD_TEXTURE = com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "textures/gui/refit_unload.png");
    public static final Identifier ICONS_TEXTURE = com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "textures/gui/refit_slot_icons.png");

    public static final int ICON_UV_SIZE = 32;
    public static final int SLOT_SIZE = 18;
    private static final int INVENTORY_ATTACHMENT_SLOT_COUNT = 8;
    private static boolean HIDE_GUN_PROPERTY_DIAGRAMS = true;

    private int currentPage = 0;

    public GunRefitScreen() {
        super(Component.literal("Gun Refit Screen"));
        RefitTransform.init();
    }

    public static int getSlotTextureXOffset(ItemStack gunItem, AttachmentType attachmentType) {
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) {
            return -1;
        }
        if (!iGun.allowAttachmentType(gunItem, attachmentType)) {
            return ICON_UV_SIZE * 6;
        }
        switch (attachmentType) {
            case GRIP -> {
                return 0;
            }
            case LASER -> {
                return ICON_UV_SIZE;
            }
            case MUZZLE -> {
                return ICON_UV_SIZE * 2;
            }
            case SCOPE -> {
                return ICON_UV_SIZE * 3;
            }
            case STOCK -> {
                return ICON_UV_SIZE * 4;
            }
            case EXTENDED_MAG -> {
                return ICON_UV_SIZE * 5;
            }
        }
        return -1;
    }

    public static int getSlotsTextureWidth() {
        return ICON_UV_SIZE * 7;
    }

    @Override
    public void init() {
        this.clearWidgets();
        // 雎ｺ・ｻ陷会｣ｰ鬩溷ｺ・ｻ・ｶ隶抵ｽｽ闖ｴ繝ｻ
        this.addAttachmentTypeButtons();
        // 雎ｺ・ｻ陷会｣ｰ陷ｿ・ｯ鬨ｾ陋ｾ繝ｻ闔会ｽｶ陋ｻ闍難ｽ｡・ｨ
        this.addInventoryAttachmentButtons();
        // 雎ｺ・ｻ陷会｣ｰ陞ｻ讓奇ｽｧ陜暦ｽｾ鬮ｫ蜊驟ｪ隰冶崟閨ｴ
        if (HIDE_GUN_PROPERTY_DIAGRAMS) {
            this.addRenderableWidget(new FlatColorButton(11, 11, 288, 16,
                    Component.translatable("gui.tacz.gun_refit.property_diagrams.show"), b -> switchHideButton()));
        } else {
            this.addRenderableWidget(new FlatColorButton(14, 14, 12, 12, Component.literal("S"), b -> {
                LocalPlayer player = Minecraft.getInstance().player;
                if (player == null || player.isSpectator()) return;
                if (IGun.mainHandHoldGun(player)) {
                    IClientPlayerGunOperator.fromLocalPlayer(player).fireSelect();
                    this.init();
                }
            }).setTooltips(Component.translatable("gui.tacz.gun_refit.property_diagrams.fire_mode.switch")));
            int buttonYOffset = GunPropertyDiagrams.getHidePropertyButtonYOffset();
            this.addRenderableWidget(new FlatColorButton(11, buttonYOffset, 288, 12,
                    Component.translatable("gui.tacz.gun_refit.property_diagrams.hide"), b -> switchHideButton()));
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        if (!HIDE_GUN_PROPERTY_DIAGRAMS) {
            GunPropertyDiagrams.draw(graphics, font, 11, 11, mouseX, mouseY);
        }

        for (Renderable renderable : this.renderables) {
            if (renderable instanceof IComponentTooltip tooltip) {
                tooltip.renderTooltip(component -> graphics.renderComponentTooltip(font, component, mouseX, mouseY, ItemStack.EMPTY));
            }
            if (renderable instanceof IStackTooltip tooltip) {
                tooltip.renderTooltip(stack -> graphics.setTooltipForNextFrame(font, stack, mouseX, mouseY));
            }
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // The refit gun is rendered by the first-person renderer before screen widgets are extracted.
        // Forge64's default screen background would blur/tile over it, hiding the preview behind the UI backdrop.
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void addInventoryAttachmentButtons() {
        LocalPlayer player = getMinecraft().player;
        if (RefitTransform.getCurrentTransformType() == AttachmentType.NONE || player == null) {
            return;
        }
        int startX = this.width - 30;
        int startY = 50;
        int pageStart = currentPage * INVENTORY_ATTACHMENT_SLOT_COUNT;
        int count = 0;
        int currentY = startY;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack inventoryItem = inventory.getItem(i);
            IAttachment attachment = IAttachment.getIAttachmentOrNull(inventoryItem);
            IGun iGun = IGun.getIGunOrNull(player.getMainHandItem());
            if (attachment != null && iGun != null && attachment.getType(inventoryItem) == RefitTransform.getCurrentTransformType()) {
                if (!iGun.allowAttachment(player.getMainHandItem(), inventoryItem)) {
                    continue;
                }
                count++;
                if (count <= pageStart) {
                    continue;
                }
                if (count > pageStart + INVENTORY_ATTACHMENT_SLOT_COUNT) {
                    continue;
                }
                InventoryAttachmentSlot button = new InventoryAttachmentSlot(startX, currentY, i, inventory, b -> {
                    int slotIndex = ((InventoryAttachmentSlot) b).getSlotIndex();
                    ItemStack attachmentItem = inventory.getItem(slotIndex);
                    IAttachment clickedAttachment = IAttachment.getIAttachmentOrNull(attachmentItem);
                    AttachmentType clickedType = clickedAttachment == null ? AttachmentType.NONE : clickedAttachment.getType(attachmentItem);
                    SoundPlayManager.playerRefitSound(inventory.getItem(slotIndex), player, SoundManager.INSTALL_SOUND);
                    ClientMessageRefitGun message = new ClientMessageRefitGun(slotIndex, inventory.getSelectedSlot(), RefitTransform.getCurrentTransformType());
                    NetworkHandler.sendToServer(message);
                });
                this.addRenderableWidget(button);
                currentY = currentY + SLOT_SIZE;
            }
        }
        int totalPage = (count - 1) / INVENTORY_ATTACHMENT_SLOT_COUNT;
        RefitTurnPageButton turnPageButtonUp = new RefitTurnPageButton(startX, startY - 10, true, b -> {
            if (currentPage > 0) {
                currentPage--;
                init();
            }
        });
        RefitTurnPageButton turnPageButtonDown = new RefitTurnPageButton(startX, startY + SLOT_SIZE * INVENTORY_ATTACHMENT_SLOT_COUNT + 2, false, b -> {
            if (currentPage < totalPage) {
                currentPage++;
                init();
            }
        });
        if (currentPage < totalPage) {
            this.addRenderableWidget(turnPageButtonDown);
        }
        if (currentPage > 0) {
            this.addRenderableWidget(turnPageButtonUp);
        }
    }

    private void addAttachmentTypeButtons() {
        LocalPlayer player = getMinecraft().player;
        if (player == null) {
            return;
        }
        IGun iGun = IGun.getIGunOrNull(player.getMainHandItem());
        if (iGun == null) {
            return;
        }
        int startX = this.width - 30;
        int startY = 10;
        Inventory inventory = player.getInventory();
        for (AttachmentType type : AttachmentType.values()) {
            if (type == AttachmentType.NONE) {
                if (RefitTransform.getCurrentTransformType() == AttachmentType.NONE) {
                    TimelessAPI.getGunDisplay(player.getMainHandItem())
                            .map(GunDisplayInstance::getLaserConfig)
                            .ifPresent(laserConfig -> {
                                if (laserConfig.canEdit()) {
                                    // 雎ｺ・ｻ陷会｣ｰ鬮滂ｽｭ陝・・・｢諛・横鬨ｾ逕ｻ蜿ｫ陜趣ｽｨ
                                    HSVSliderGroup hsvSliderGroup = new HSVSliderGroup(width-140, height-64, 120, 16, inventory, inventory.getSelectedSlot(), AttachmentType.NONE);
                                    this.addRenderableWidget(hsvSliderGroup.getHueSlider());
                                    this.addRenderableWidget(hsvSliderGroup.getSaturationSlider());
                                }});
                }
                continue;
            }
            GunAttachmentSlot button = new GunAttachmentSlot(startX, startY, type, inventory.getSelectedSlot(), inventory, b -> {
                AttachmentType buttonType = ((GunAttachmentSlot) b).getType();
                // 陞ｯ繧域｣｡髴大姓・ｸ・ｪ隶抵ｽｽ闖ｴ蝣ｺ・ｸ讎翫・髫ｶ・ｸ陞ｳ闃ｽ・｣繝ｻ繝ｻ闔会ｽｶ繝ｻ謔溘・魄溷ｩ・ｮ・､鬨ｾ陜玲ｨ奇ｽｦ繧奇ｽｧ闌ｨ・ｼ蠕｡・ｸ蝓ｼ謌托ｽｸ・ｭ隶抵ｽｽ闖ｴ髦ｪ繝ｻ
                if (!((GunAttachmentSlot) b).isAllow()) {
                    if (RefitTransform.changeRefitScreenView(AttachmentType.NONE)) {
                        this.init();
                    }
                    return;
                }
                // 霓､・ｹ陷・ｽｻ騾ｧ繝ｻ蠑崎也§辯暮ｨｾ謌托ｽｸ・ｭ騾ｧ繝ｻ・ｧ・ｽ闖ｴ謳ｾ・ｼ謔溘・鬨ｾ陜玲ｨ奇ｽｦ繧奇ｽｧ繝ｻ
                if (RefitTransform.getCurrentTransformType() == buttonType && buttonType != AttachmentType.NONE) {
                    if (RefitTransform.changeRefitScreenView(AttachmentType.NONE)) {
                        this.init();
                    }
                    return;
                }
                // 陋ｻ繝ｻ蝗ｰ鬨ｾ謌托ｽｸ・ｭ騾ｧ繝ｻ・ｧ・ｽ闖ｴ髦ｪ繝ｻ
                if (RefitTransform.changeRefitScreenView(buttonType)) {
                    this.init();
                }
            });
            if (RefitTransform.getCurrentTransformType() == type) {
                button.setSelected(true);
                // 雎ｺ・ｻ陷会｣ｰ隲｡繝ｻ譟ｻ鬩溷ｺ・ｻ・ｶ隰冶崟閨ｴ
                RefitUnloadButton unloadButton = new RefitUnloadButton(startX + 5, startY + SLOT_SIZE + 2, b -> {
                    ItemStack attachmentItem = button.getAttachmentItem();
                    if (!attachmentItem.isEmpty()) {
                        int freeSlot = inventory.getFreeSlot();
                        if (freeSlot != -1) {
                            SoundPlayManager.playerRefitSound(attachmentItem, player, SoundManager.UNINSTALL_SOUND);
                            ClientMessageUnloadAttachment message = new ClientMessageUnloadAttachment(inventory.getSelectedSlot(), RefitTransform.getCurrentTransformType());
                            NetworkHandler.sendToServer(message);
                        } else {
                            player.sendSystemMessage(Component.translatable("gui.tacz.gun_refit.unload.no_space"));
                        }
                    }
                });
                if (!button.getAttachmentItem().isEmpty()) {
                    this.addRenderableWidget(unloadButton);

                    if (button.getAttachmentItem().getItem() instanceof IAttachment iAttachment) {
                        TimelessAPI.getClientAttachmentIndex(iAttachment.getAttachmentId(button.getAttachmentItem()))
                                .map(ClientAttachmentIndex::getLaserConfig)
                                .ifPresent(laserConfig -> {
                                    if (laserConfig.canEdit()) {
                                        // 雎ｺ・ｻ陷会｣ｰ鬮滂ｽｭ陝・・・｢諛・横鬨ｾ逕ｻ蜿ｫ陜趣ｽｨ
                                        HSVSliderGroup hsvSliderGroup = new HSVSliderGroup(width-140, height-64, 120, 16, inventory, inventory.getSelectedSlot(), type);
                                        this.addRenderableWidget(hsvSliderGroup.getHueSlider());
                                        this.addRenderableWidget(hsvSliderGroup.getSaturationSlider());
                                    }});
                    }
                }
            }
            this.addRenderableWidget(button);
            startX = startX - SLOT_SIZE;
        }
    }

    @Override
    public void onClose() {
        // 陷茨ｽｳ鬮｣・ｭ騾｡遒∵島隴鯉ｽｶ繝ｻ蠕｡・ｸ隹ｺ・｡隲､・ｧ闕ｳ雍具ｽｼ・ｰ隰・ｭ幄・蝎ｪ隴滓･｢迚｡隰ｨ・ｰ隰撰ｽｮ
        LocalPlayer player = getMinecraft().player;
        if (player != null) {
            ItemStack gun = player.getMainHandItem();
            if (player.getMainHandItem().getItem() instanceof IGun) {
                ClientMessageLaserColor message = new ClientMessageLaserColor(gun, player.getInventory().getSelectedSlot());
        NetworkHandler.sendToServer(message);
            }
        }
        super.onClose();
    }

    private void switchHideButton() {
        HIDE_GUN_PROPERTY_DIAGRAMS = !HIDE_GUN_PROPERTY_DIAGRAMS;
        this.init();
    }
}


