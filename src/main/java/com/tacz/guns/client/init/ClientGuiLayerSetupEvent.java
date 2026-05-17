package com.tacz.guns.client.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.client.event.PreventsHotbarEvent;
import com.tacz.guns.client.event.RenderCrosshairEvent;
import com.tacz.guns.client.gui.overlay.GunHudOverlay;
import com.tacz.guns.client.gui.overlay.HeatBarOverlay;
import com.tacz.guns.client.gui.overlay.InteractKeyTextOverlay;
import com.tacz.guns.client.gui.overlay.KillAmountOverlay;
import com.tacz.guns.client.input.*;
import com.tacz.guns.client.resource.ClientAssetsManager;
import com.tacz.guns.client.tooltip.ClientAmmoBoxTooltip;
import com.tacz.guns.client.tooltip.ClientAttachmentItemTooltip;
import com.tacz.guns.client.tooltip.ClientBlockItemTooltip;
import com.tacz.guns.client.tooltip.ClientGunTooltip;
import com.tacz.guns.compat.playeranimator.PlayerAnimatorCompat;
import com.tacz.guns.inventory.tooltip.AmmoBoxTooltip;
import com.tacz.guns.inventory.tooltip.AttachmentItemTooltip;
import com.tacz.guns.inventory.tooltip.BlockItemTooltip;
import com.tacz.guns.inventory.tooltip.GunTooltip;
import net.minecraft.client.DeltaTracker;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GunMod.MOD_ID)
public class ClientGuiLayerSetupEvent {
    @SubscribeEvent
    public static void onRegisterGuiLayers(AddGuiOverlayLayersEvent event) {
        var layers = event.getLayeredDraw();
        layers.addConditionTo(ForgeLayeredDraw.PRE_SLEEP_STACK, ForgeLayeredDraw.HOTBAR_AND_DECOS, () -> !PreventsHotbarEvent.shouldHideHotbar());
        layers.addConditionTo(ForgeLayeredDraw.PRE_SLEEP_STACK, ForgeLayeredDraw.CROSSHAIR, () -> !RenderCrosshairEvent.shouldHideVanillaCrosshair());

        layers.addAbove(ForgeLayeredDraw.PRE_SLEEP_STACK, id("tac_gun_hud_overlay"), ForgeLayeredDraw.HOTBAR_AND_DECOS, new GunHudOverlay());
        layers.addAbove(ForgeLayeredDraw.PRE_SLEEP_STACK, id("tac_heat_bar"), ForgeLayeredDraw.HOTBAR_AND_DECOS, new HeatBarOverlay());
        layers.addAbove(ForgeLayeredDraw.PRE_SLEEP_STACK, id("tac_kill_amount_overlay"), ForgeLayeredDraw.HOTBAR_AND_DECOS, new KillAmountOverlay());
        layers.addAbove(ForgeLayeredDraw.PRE_SLEEP_STACK, id("tac_interact_key_overlay"), ForgeLayeredDraw.CROSSHAIR, new InteractKeyTextOverlay());
        layers.addAbove(ForgeLayeredDraw.PRE_SLEEP_STACK, id("tac_crosshair_overlay"), ForgeLayeredDraw.CROSSHAIR,
                (gg, dt) -> RenderCrosshairEvent.extractCrosshairLayer(gg, partial(dt)));
    }

    @SubscribeEvent
    public static void onClientResourceReload(RegisterClientReloadListenersEvent event) {
        PlayerAnimatorCompat.init();
        ClientAssetsManager.INSTANCE.reloadAndRegister(event::registerReloadListener);
        if (PlayerAnimatorCompat.isInstalled()) {
            PlayerAnimatorCompat.registerReloadListener(event::registerReloadListener);
        }
    }

    @SubscribeEvent
    public static void onClientTooltipSetup(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(GunTooltip.class, ClientGunTooltip::new);
        event.register(AmmoBoxTooltip.class, ClientAmmoBoxTooltip::new);
        event.register(AttachmentItemTooltip.class, ClientAttachmentItemTooltip::new);
        event.register(BlockItemTooltip.class, ClientBlockItemTooltip::new);
    }

    @SubscribeEvent
    public static void onClientKeySetup(RegisterKeyMappingsEvent event) {
        event.register(InspectKey.INSPECT_KEY);
        event.register(ReloadKey.RELOAD_KEY);
        event.register(ShootKey.SHOOT_KEY);
        event.register(InteractKey.INTERACT_KEY);
        event.register(FireSelectKey.FIRE_SELECT_KEY);
        event.register(AimKey.AIM_KEY);
        event.register(CrawlKey.CRAWL_KEY);
        event.register(RefitKey.REFIT_KEY);
        event.register(ZoomKey.ZOOM_KEY);
        event.register(MeleeKey.MELEE_KEY);
        event.register(ConfigKey.OPEN_CONFIG_KEY);
    }

    private static Identifier id(String path) {
        return com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, path);
    }

    private static float partial(DeltaTracker dt) {
        return dt.getGameTimeDeltaPartialTick(false);
    }
}
