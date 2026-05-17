package com.tacz.guns.client.gui.overlay;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.client.resource.pojo.display.gun.AmmoCountStyle;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.util.AttachmentDataUtils;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeLayer;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;

public class GunHudOverlay implements ForgeLayer {
    private static final Identifier SEMI = com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "textures/hud/fire_mode_semi.png");
    private static final Identifier AUTO = com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "textures/hud/fire_mode_auto.png");
    private static final Identifier BURST = com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "textures/hud/fire_mode_burst.png");

    private static final DecimalFormat CURRENT_AMMO_FORMAT = new DecimalFormat("000");
    private static final DecimalFormat CURRENT_AMMO_FORMAT_PERCENT = new DecimalFormat("000%");
    private static final DecimalFormat INVENTORY_AMMO_FORMAT = new DecimalFormat("0000");
    private static final int MAX_AMMO_COUNT = 9999;
    private static long checkAmmoTimestamp = -1L;
    private static int cacheMaxAmmoCount = 0;
    private static int cacheInventoryAmmoCount = 0;

    @Override
    public void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!RenderConfig.GUN_HUD_ENABLE.get()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof IGun iGun)) {
            return;
        }

        Identifier gunId = iGun.getGunId(stack);
        GunData gunData = TimelessAPI.getClientGunIndex(gunId).map(ClientGunIndex::getGunData).orElse(null);
        GunDisplayInstance display = TimelessAPI.getGunDisplay(stack).orElse(null);
        if (gunData == null || display == null) {
            return;
        }

        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        boolean useInventoryAmmo = iGun.useInventoryAmmo(stack);
        boolean useDummyAmmo = iGun.useDummyAmmo(stack);
        boolean overheatLocked = gunData.hasHeatData() && iGun.isOverheatLocked(stack);
        handleCacheCount(player, stack, gunData, iGun);

        int ammoCount = useInventoryAmmo
                ? cacheInventoryAmmoCount + (iGun.hasBulletInBarrel(stack) && gunData.getBolt() != Bolt.OPEN_BOLT ? 1 : 0)
                : iGun.getCurrentAmmoCount(stack) + (iGun.hasBulletInBarrel(stack) && gunData.getBolt() != Bolt.OPEN_BOLT ? 1 : 0);
        ammoCount = Math.min(ammoCount, MAX_AMMO_COUNT);

        int ammoCountColor;
        if (ammoCount < (cacheMaxAmmoCount * 0.25f) && ammoCount < 10 || overheatLocked) {
            ammoCountColor = 0xFFFF5555;
        } else {
            ammoCountColor = useInventoryAmmo && useDummyAmmo ? 0xFF55FFFF : useInventoryAmmo ? 0xFFFFFF55 : 0xFFFFFFFF;
        }
        int inventoryAmmoCountColor = (!useInventoryAmmo && useDummyAmmo) ? 0xFF55FFFF : 0xFFAAAAAA;

        String currentAmmoText;
        if (display.getAmmoCountStyle() == AmmoCountStyle.PERCENT) {
            currentAmmoText = CURRENT_AMMO_FORMAT_PERCENT.format((float) ammoCount / (cacheMaxAmmoCount == 0 ? 1f : cacheMaxAmmoCount));
        } else {
            currentAmmoText = CURRENT_AMMO_FORMAT.format(Math.max(ammoCount, 0));
        }

        String inventoryAmmoText = useInventoryAmmo ? "" : INVENTORY_AMMO_FORMAT.format(Math.max(cacheInventoryAmmoCount, 0));
        if (!useInventoryAmmo && gunData.getReloadData().isInfinite()) {
            inventoryAmmoText = "\u221e";
        }

        @Nullable Identifier hudTexture = display.getHUDTexture();
        @Nullable Identifier hudEmptyTexture = display.getHudEmptyTexture();
        if (ammoCount <= 0 || overheatLocked) {
            if (hudEmptyTexture != null) {
                hudTexture = hudEmptyTexture;
            }
        }
        if (hudTexture != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, hudTexture, width - 117, height - 44, 0f, 0f, 39, 13, 39, 13);
        }

        Font font = mc.font;
        graphics.fill(width - 75, height - 43, width - 74, height - 25, 0xFFFFFFFF);
        int mainAmmoX = width - 70;
        int mainAmmoY = height - 43;
        graphics.text(font, currentAmmoText, mainAmmoX, mainAmmoY, ammoCountColor, false);
        String reserveAmmoText = "";
        int reserveX = width - 68 + font.width(currentAmmoText);
        if (!inventoryAmmoText.isEmpty()) {
            reserveAmmoText = "/" + inventoryAmmoText;
            graphics.text(font, reserveAmmoText, reserveX, mainAmmoY, inventoryAmmoCountColor, false);
        }

        FireMode fireMode = IGun.getMainHandFireMode(player);
        Identifier fireModeTexture = switch (fireMode) {
            case AUTO -> AUTO;
            case BURST -> BURST;
            default -> SEMI;
        };
        int fireModeX = reserveX + (reserveAmmoText.isEmpty() ? 0 : font.width(reserveAmmoText) + 2);
        graphics.blit(RenderPipelines.GUI_TEXTURED, fireModeTexture, fireModeX, height - 38, 0f, 0f, 10, 10, 10, 10);
    }

    private static void handleCacheCount(LocalPlayer player, ItemStack stack, GunData gunData, IGun iGun) {
        if ((System.currentTimeMillis() - checkAmmoTimestamp) <= 50) {
            return;
        }
        checkAmmoTimestamp = System.currentTimeMillis();
        cacheMaxAmmoCount = AttachmentDataUtils.getAmmoCountWithAttachment(stack, gunData);
        if (IGunOperator.fromLivingEntity(player).needCheckAmmo()) {
            if (iGun.useDummyAmmo(stack)) {
                cacheInventoryAmmoCount = iGun.getDummyAmmoAmount(stack);
            } else {
                cacheInventoryAmmoCount = countInventoryAmmo(stack, player.getInventory());
            }
        } else {
            cacheInventoryAmmoCount = MAX_AMMO_COUNT;
        }
    }

    private static int countInventoryAmmo(ItemStack gunStack, Inventory inventory) {
        int total = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack inv = inventory.getItem(i);
            if (inv.getItem() instanceof IAmmo ammo && ammo.isAmmoOfGun(gunStack, inv)) {
                total += inv.getCount();
            }
            if (inv.getItem() instanceof IAmmoBox ammoBox && ammoBox.isAmmoBoxOfGun(gunStack, inv)) {
                if (ammoBox.isAllTypeCreative(inv) || ammoBox.isCreative(inv)) {
                    return 9999;
                }
                total += ammoBox.getAmmoCount(inv);
            }
        }
        return total;
    }
}
