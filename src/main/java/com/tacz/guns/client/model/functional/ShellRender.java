package com.tacz.guns.client.model.functional;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.model.BedrockAmmoModel;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.IFunctionalRenderer;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.client.resource.pojo.display.gun.ShellEjection;
import com.tacz.guns.compat.oculus.OculusCompat;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.concurrent.ConcurrentLinkedDeque;

public class ShellRender implements IFunctionalRenderer {
    // 謚帛｣ｳ髦溷・
    private final ConcurrentLinkedDeque<Data> SHELL_QUEUE = new ConcurrentLinkedDeque<>();
    public static boolean isSelf = false;

    private final BedrockGunModel bedrockGunModel;

    public ShellRender(BedrockGunModel bedrockGunModel) {
        this.bedrockGunModel = bedrockGunModel;
    }

    public void addShell(Vector3f randomVelocity) {
        if (SHELL_QUEUE.size() > 128) {
            SHELL_QUEUE.pollFirst();
        }
        double xRandom = Math.random() * randomVelocity.x();
        double yRandom = Math.random() * randomVelocity.y();
        double zRandom = Math.random() * randomVelocity.z();
        Vector3f vector3f = new Vector3f((float) xRandom, (float) yRandom, (float) zRandom);
        SHELL_QUEUE.offerLast(new Data(System.currentTimeMillis(), vector3f));
    }

    private void renderShell(GunDisplayInstance display, GunData gunData, PoseStack poseStack, BedrockGunModel gunModel) {
        ShellEjection shellEjection = display.getShellEjection();
        if (shellEjection == null) {
            SHELL_QUEUE.clear();
            return;
        }
        TimelessAPI.getClientAmmoIndex(gunData.getAmmoId()).ifPresent(ammoIndex -> {
            BedrockAmmoModel model = ammoIndex.getShellModel();
            if (model == null) {
                return;
            }
            Identifier location = ammoIndex.getShellTextureLocation();
            if (location == null) {
                return;
            }
            long lifeTime = (long) (shellEjection.getLivingTime() * 1000);

            // 譽譟･譛画ｲ｡譛蛾怙隕∬ｸ｢蜃ｺ蜴ｻ逧・弌蛻・
            checkShellQueue(lifeTime);

            // 蜷・ｧ榊盾謨ｰ逧・執蜿・
            Vector3f initialVelocity = shellEjection.getInitialVelocity();
            Vector3f acceleration = shellEjection.getAcceleration();
            Vector3f angularVelocity = shellEjection.getAngularVelocity();

            // 郛灘ｭ倅ｸ荳・PoseStack
            for (Data data : SHELL_QUEUE) {
                if (data.normal == null && data.pose == null) {
                    data.normal = new Matrix3f(poseStack.last().normal());
                    data.pose = new Matrix4f(poseStack.last().pose());
                }
            }

            // 貂ｲ譟捺鴨螢ｳ
            gunModel.delegateRender((poseStack1, vertexConsumer1, transformType1, light, overlay) ->{
                SHELL_QUEUE.forEach(data -> renderSingleShell(transformType1, light, overlay, data, initialVelocity, acceleration, angularVelocity, model, location));
            });
        });
    }

    private void renderSingleShell(ItemDisplayContext transformType1, int light, int overlay, Data data, Vector3f initialVelocity, Vector3f acceleration, Vector3f angularVelocity, BedrockAmmoModel model, Identifier location) {
        // 蜀肴｣譟･荳谺｡
        if (data.normal == null && data.pose == null) {
            return;
        }
        // 蜈亥・蟋句喧蛻ｰ郛灘ｭ倅ｽ咲ｽｮ蜥梧悃蜷・
        PoseStack poseStack2 = new PoseStack();
        poseStack2.last().normal().mul(data.normal);
        poseStack2.last().pose().mul(data.pose);

        // 闔ｷ蜿門ｭ倡蕗譌ｶ髣ｴ蜥悟推遘榊盾謨ｰ
        long remindTime = System.currentTimeMillis() - data.timeStamp;
        double time = remindTime / 1000.0;
        Vector3f randomOffset = data.randomOffset;

        // 菴咲ｧｻ・梧ｻ｡雜ｳ譬・㊥逧・劇蜿倬溽峩郤ｿ霑仙勘
        double x = (initialVelocity.x() + randomOffset.x()) * time + 0.5 * acceleration.x() * time * time;
        double y = (initialVelocity.y() + randomOffset.y()) * time + 0.5 * acceleration.y() * time * time;
        double z = (initialVelocity.z() + randomOffset.z()) * time + 0.5 * acceleration.z() * time * time;
        poseStack2.translate(-x, -y, z);

        // 譌玖ｽｬ
        double xw = time * angularVelocity.x();
        double yw = time * angularVelocity.y();
        double zw = time * angularVelocity.z();
        poseStack2.mulPose(Axis.XN.rotationDegrees((float) xw));
        poseStack2.mulPose(Axis.YN.rotationDegrees((float) yw));
        poseStack2.mulPose(Axis.ZP.rotationDegrees((float) zw));
        poseStack2.translate(0, -1.5, 0);

        model.render(poseStack2, transformType1, RenderTypes.entityTranslucent(location), light, overlay);
    }

    private void checkShellQueue(long lifeTime) {
        if (!SHELL_QUEUE.isEmpty()) {
            Data data = SHELL_QUEUE.peekFirst();
            if ((System.currentTimeMillis() - data.timeStamp) > lifeTime) {
                SHELL_QUEUE.pollFirst();
                checkShellQueue(lifeTime);
            }
        }
    }

    @Override
    public void render(PoseStack poseStack, VertexConsumer vertexBuffer, ItemDisplayContext transformType, int light, int overlay) {
        if (OculusCompat.isRenderShadow()) {
            return;
        }
        if (!isSelf) {
            return;
        }
        ItemStack currentGunItem = bedrockGunModel.getCurrentGunItem();
        IGun iGun = IGun.getIGunOrNull(currentGunItem);
        if (iGun == null) {
            return;
        }
        GunData gunData = TimelessAPI.getClientGunIndex(iGun.getGunId(currentGunItem)).map(ClientGunIndex::getGunData).orElse(null);
        if (gunData == null) {
            return;
        }
        TimelessAPI.getGunDisplay(currentGunItem).ifPresent(display -> {
            this.renderShell(display, gunData, poseStack, bedrockGunModel);
        });

    }

    public static class Data {
        public final long timeStamp;
        public final Vector3f randomOffset;

        public Matrix3f normal = null;
        public Matrix4f pose = null;

        public Data(long timeStamp, Vector3f randomOffset) {
            this.timeStamp = timeStamp;
            this.randomOffset = randomOffset;
        }
    }
}

