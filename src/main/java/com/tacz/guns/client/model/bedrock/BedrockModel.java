package com.tacz.guns.client.model.bedrock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tacz.guns.client.model.IFunctionalRenderer;
import com.tacz.guns.client.resource.pojo.model.*;
import com.tacz.guns.compat.oculus.OculusCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;

public class BedrockModel {
    public static BedrockModel dummyModel = new BedrockModel();
    /**
     * 蟄伜お ModelRender 蟄先ｨ｡蝙狗噪 HashMap
     */
    protected final HashMap<String, ModelRendererWrapper> modelMap = new HashMap<>();
    /**
     * 蟄伜お Bones 逧・HashMap・御ｸｻ隕∵弍扈吝錘髱｢蟇ｻ謇ｾ辷ｶ鬪ｨ鬪ｼ霑幄｡悟攝譬・ｽｬ謐｢逕ｨ逧・
     */
    protected final HashMap<String, BonesItem> indexBones = new HashMap<>();
    /**
     * 蜩ｪ莠帶ｨ｡蝙矩怙隕∵ｸｲ譟薙ょ刈霓ｽ霑帷宛鬪ｨ鬪ｼ逧・ｭ宣ｪｨ鬪ｼ譏ｯ荳埼怙隕∵ｸｲ譟鍋噪
     */
    protected final List<BedrockPart> shouldRender = new LinkedList<>();
    /**
     * 蟋疲汚蛻ｰ貂ｲ譟鍋ｻ捺據譌ｶ謇ｧ陦檎噪貂ｲ譟灘勣・檎畑莠守音谿企Κ蛻・噪貂ｲ譟難ｼ悟ｦよ焔閾・
     */
    protected List<IFunctionalRenderer> delegateRenderers = new ArrayList<>();
    /**
     * 讓｡蝙狗噪荳ｭ蠢・せ
     */
    protected @Nullable Vec3 offset = null;
    /**
     * 讓｡蝙狗噪螟ｧ蟆・
     */
    protected @Nullable Vec2 size = null;

    public BedrockModel(BedrockModelPOJO pojo, BedrockVersion version) {
        if (version == BedrockVersion.LEGACY) {
            loadLegacyModel(pojo);
        }
        if (version == BedrockVersion.NEW) {
            loadNewModel(pojo);
        }
        // 蠎皮畑蜿大・
        for (ModelRendererWrapper rendererWrapper : modelMap.values()) {
            if (rendererWrapper.getModelRenderer().name != null && rendererWrapper.getModelRenderer().name.endsWith("_illuminated")) {
                rendererWrapper.getModelRenderer().illuminated = true;
            }
        }
    }

    protected BedrockModel() {
    }

    public void delegateRender(IFunctionalRenderer renderer) {
        delegateRenderers.add(renderer);
    }

    private void setRotationAngle(BedrockPart modelRenderer, float x, float y, float z) {
        modelRenderer.xRot = x;
        modelRenderer.yRot = y;
        modelRenderer.zRot = z;
        modelRenderer.setInitRotationAngle(x, y, z);
    }

    protected void loadNewModel(BedrockModelPOJO pojo) {
        assert pojo.getGeometryModelNew() != null;
        pojo.getGeometryModelNew().deco();
        if (pojo.getGeometryModelNew().getBones() == null) {
            return;
        }
        Description description = pojo.getGeometryModelNew().getDescription();
        // 譚占ｴｨ逧・柄蠎ｦ縲∝ｮｽ蠎ｦ
        int texWidth = description.getTextureWidth();
        int texHeight = description.getTextureHeight();

        List<Float> offset = description.getVisibleBoundsOffset();
        float offsetX = offset.get(0);
        float offsetY = offset.get(1);
        float offsetZ = offset.get(2);
        this.offset = new Vec3(offsetX, offsetY, offsetZ);
        float width = description.getVisibleBoundsWidth() / 2.0f;
        float height = description.getVisibleBoundsHeight() / 2.0f;
        this.size = new Vec2(width, height);

        // 蠕 indexBones 驥碁擇豕ｨ蜈･謨ｰ謐ｮ・御ｸｺ蜷守ｻｭ蝮先・ｽｬ謐｢蛛壼盾閠・
        for (BonesItem bones : pojo.getGeometryModelNew().getBones()) {
            // 蝪樒ｴ｢蠑包ｼ瑚ｿ呎弍扈吝錘髱｢蝮先・ｽｬ謐｢逕ｨ逧・
            indexBones.putIfAbsent(bones.getName(), bones);
            // 蝪槫・譁ｰ蟒ｺ逧・ｩｺ BedrockPart 螳樔ｾ・
            // 蝗荳ｺ蜷朱擇豺ｻ蜉 parent 髴隕・ｼ梧園莉･蜈亥｡樒ｩｺ蟇ｹ雎｡・檎┯蜷惹ｺ梧ｬ｡驕榊紙蜀崎ｿ幄｡梧焚謐ｮ蟄伜お
            modelMap.putIfAbsent(bones.getName(), new ModelRendererWrapper(new BedrockPart(bones.getName())));
        }

        // 蠑蟋句ｾ ModelRenderer 螳樔ｾ矩㈹髱｢蝪樊焚謐ｮ
        for (BonesItem bones : pojo.getGeometryModelNew().getBones()) {
            // 鬪ｨ鬪ｼ蜷咲ｧｰ
            String name = bones.getName();
            // 譌玖ｽｬ・悟庄閭ｽ荳ｺ遨ｺ
            @Nullable List<Float> rotation = bones.getRotation();
            // 辷ｶ鬪ｨ鬪ｼ逧・錐遘ｰ・悟庄閭ｽ荳ｺ遨ｺ
            @Nullable String parent = bones.getParent();
            // 蝪櫁ｿ・HashMap 驥碁擇逧・ｨ｡蝙句ｯｹ雎｡
            BedrockPart model = modelMap.get(name).getModelRenderer();

            // 髟懷ワ蜿よ焚
            model.mirror = bones.isMirror();

            // 譌玖ｽｬ轤ｹ
            model.setPos(convertPivot(bones, 0), convertPivot(bones, 1), convertPivot(bones, 2));

            // Nullable 譽譟･・瑚ｮｾ鄂ｮ譌玖ｽｬ隗貞ｺｦ
            if (rotation != null) {
                setRotationAngle(model, convertRotation(rotation.get(0)), convertRotation(rotation.get(1)), convertRotation(rotation.get(2)));
            }

            // Null 譽譟･・瑚ｿ幄｡檎宛鬪ｨ鬪ｼ扈大ｮ・
            if (parent != null) {
                BedrockPart parentPart = modelMap.get(parent).getModelRenderer();
                parentPart.addChild(model);
                model.parent = parentPart;
            } else {
                // 豐｡譛臥宛鬪ｨ鬪ｼ逧・ｨ｡蝙区燕霑幄｡梧ｸｲ譟・
                shouldRender.add(model);
                model.parent = null;
            }

            // 謌醍噪螟ｩ・靴ubes 霑倩・荳ｺ遨ｺ窶ｦ窶ｦ
            if (bones.getCubes() == null) {
                continue;
            }

            // 蝪槫・ Cube List
            for (CubesItem cube : bones.getCubes()) {
                List<Float> uv = cube.getUv();
                @Nullable FaceUVsItem faceUv = cube.getFaceUv();
                List<Float> size = cube.getSize();
                @Nullable List<Float> cubeRotation = cube.getRotation();
                boolean mirror = cube.isMirror();
                float inflate = cube.getInflate();

                // 蠖灘★譎ｮ騾・cube 蟄伜・
                if (cubeRotation == null) {
                    if (faceUv == null) {
                        model.cubes.add(new BedrockCubeBox(uv.get(0), uv.get(1),
                                convertOrigin(bones, cube, 0), convertOrigin(bones, cube, 1), convertOrigin(bones, cube, 2),
                                size.get(0), size.get(1), size.get(2), inflate, mirror,
                                texWidth, texHeight));
                    } else {
                        model.cubes.add(new BedrockCubePerFace(
                                convertOrigin(bones, cube, 0), convertOrigin(bones, cube, 1), convertOrigin(bones, cube, 2),
                                size.get(0), size.get(1), size.get(2), inflate,
                                texWidth, texHeight, faceUv));
                    }
                }
                // 蛻帛ｻｺ Cube ModelRender
                else {
                    BedrockPart cubeRenderer = new BedrockPart(null);
                    cubeRenderer.setPos(convertPivot(bones, cube, 0), convertPivot(bones, cube, 1), convertPivot(bones, cube, 2));
                    setRotationAngle(cubeRenderer, convertRotation(cubeRotation.get(0)), convertRotation(cubeRotation.get(1)), convertRotation(cubeRotation.get(2)));
                    if (faceUv == null) {
                        cubeRenderer.cubes.add(new BedrockCubeBox(uv.get(0), uv.get(1),
                                convertOrigin(cube, 0), convertOrigin(cube, 1), convertOrigin(cube, 2),
                                size.get(0), size.get(1), size.get(2), inflate, mirror,
                                texWidth, texHeight));
                    } else {
                        cubeRenderer.cubes.add(new BedrockCubePerFace(
                                convertOrigin(cube, 0), convertOrigin(cube, 1), convertOrigin(cube, 2),
                                size.get(0), size.get(1), size.get(2), inflate,
                                texWidth, texHeight, faceUv));
                    }

                    // 豺ｻ蜉霑帷宛鬪ｨ鬪ｼ荳ｭ
                    model.addChild(cubeRenderer);
                }
            }
        }
    }

    protected void loadLegacyModel(BedrockModelPOJO pojo) {
        assert pojo.getGeometryModelLegacy() != null;
        pojo.getGeometryModelLegacy().deco();
        if (pojo.getGeometryModelLegacy().getBones() == null) {
            return;
        }

        // 譚占ｴｨ逧・柄蠎ｦ縲∝ｮｽ蠎ｦ
        int texWidth = pojo.getGeometryModelLegacy().getTextureWidth();
        int texHeight = pojo.getGeometryModelLegacy().getTextureHeight();

        List<Float> offset = pojo.getGeometryModelLegacy().getVisibleBoundsOffset();
        float offsetX = offset.get(0);
        float offsetY = offset.get(1);
        float offsetZ = offset.get(2);
        this.offset = new Vec3(offsetX, offsetY, offsetZ);
        float width = pojo.getGeometryModelLegacy().getVisibleBoundsWidth() / 2.0f;
        float height = pojo.getGeometryModelLegacy().getVisibleBoundsHeight() / 2.0f;
        this.size = new Vec2(width, height);

        // 蠕 indexBones 驥碁擇豕ｨ蜈･謨ｰ謐ｮ・御ｸｺ蜷守ｻｭ蝮先・ｽｬ謐｢蛛壼盾閠・
        for (BonesItem bones : pojo.getGeometryModelLegacy().getBones()) {
            // 蝪樒ｴ｢蠑包ｼ瑚ｿ呎弍扈吝錘髱｢蝮先・ｽｬ謐｢逕ｨ逧・
            indexBones.putIfAbsent(bones.getName(), bones);
            // 蝪槫・譁ｰ蟒ｺ逧・ｩｺ ModelRenderer 螳樔ｾ・
            // 蝗荳ｺ蜷朱擇豺ｻ蜉 parent 髴隕・ｼ梧園莉･蜈亥｡樒ｩｺ蟇ｹ雎｡・檎┯蜷惹ｺ梧ｬ｡驕榊紙蜀崎ｿ幄｡梧焚謐ｮ蟄伜お
            modelMap.putIfAbsent(bones.getName(), new ModelRendererWrapper(new BedrockPart(bones.getName())));
        }

        // 蠑蟋句ｾ ModelRenderer 螳樔ｾ矩㈹髱｢蝪樊焚謐ｮ
        for (BonesItem bones : pojo.getGeometryModelLegacy().getBones()) {
            // 鬪ｨ鬪ｼ蜷咲ｧｰ・梧ｳｨ諢丞屏荳ｺ蜷朱擇蜉ｨ逕ｻ逧・怙隕・ｼ悟､ｴ驛ｨ縲∵焔驛ｨ縲∬・驛ｨ遲蛾ｪｨ鬪ｼ蜻ｽ蜷榊ｿ・｡ｻ譏ｯ蝗ｺ螳壽ｭｻ逧・
            String name = bones.getName();
            // 譌玖ｽｬ轤ｹ・悟庄閭ｽ荳ｺ遨ｺ
            @Nullable List<Float> rotation = bones.getRotation();
            // 辷ｶ鬪ｨ鬪ｼ逧・錐遘ｰ・悟庄閭ｽ荳ｺ遨ｺ
            @Nullable String parent = bones.getParent();
            // 蝪櫁ｿ・HashMap 驥碁擇逧・ｨ｡蝙句ｯｹ雎｡
            BedrockPart model = modelMap.get(name).getModelRenderer();

            // 髟懷ワ蜿よ焚
            model.mirror = bones.isMirror();

            // 譌玖ｽｬ轤ｹ
            model.setPos(convertPivot(bones, 0), convertPivot(bones, 1), convertPivot(bones, 2));

            // Nullable 譽譟･・瑚ｮｾ鄂ｮ譌玖ｽｬ隗貞ｺｦ
            if (rotation != null) {
                setRotationAngle(model, convertRotation(rotation.get(0)), convertRotation(rotation.get(1)), convertRotation(rotation.get(2)));
            }

            // Null 譽譟･・瑚ｿ幄｡檎宛鬪ｨ鬪ｼ扈大ｮ・
            if (parent != null) {
                modelMap.get(parent).getModelRenderer().addChild(model);
            } else {
                // 豐｡譛臥宛鬪ｨ鬪ｼ逧・ｨ｡蝙区燕霑幄｡梧ｸｲ譟・
                shouldRender.add(model);
            }

            // 謌醍噪螟ｩ・靴ubes 霑倩・荳ｺ遨ｺ窶ｦ窶ｦ
            if (bones.getCubes() == null) {
                continue;
            }

            // 蝪槫・ Cube List
            for (CubesItem cube : bones.getCubes()) {
                List<Float> uv = cube.getUv();
                List<Float> size = cube.getSize();
                boolean mirror = cube.isMirror();
                float inflate = cube.getInflate();

                model.cubes.add(new BedrockCubeBox(uv.get(0), uv.get(1),
                        convertOrigin(bones, cube, 0), convertOrigin(bones, cube, 1), convertOrigin(bones, cube, 2),
                        size.get(0), size.get(1), size.get(2), inflate, mirror,
                        texWidth, texHeight));
            }
        }
    }

    /**
     * 蝓ｺ蟯ｩ迚育噪譌玖ｽｬ荳ｭ蠢・ｮ｡邂玲婿蠑丞柱 Java 迚井ｸ榊､ｪ荳譬ｷ・碁怙隕∬ｿ幄｡瑚ｽｬ謐｢
     * <p>
     * 螯よ棡譛臥宛讓｡蝙・
     * <li>x・頚 譁ｹ蜷托ｼ壽悽讓｡蝙句攝譬・- 辷ｶ讓｡蝙句攝譬・
     * <li>y 譁ｹ蜷托ｼ夂宛讓｡蝙句攝譬・- 譛ｬ讓｡蝙句攝譬・
     * <p>
     * 螯よ棡豐｡譛臥宛讓｡蝙・
     * <li>x・頚 譁ｹ蜷台ｸ榊序
     * <li>y 譁ｹ蜷托ｼ・4 - 譛ｬ讓｡蝙句攝譬・
     *
     * @param index 譏ｯ xyz 逧・頭荳荳ｪ・警 譏ｯ 0・軽 譏ｯ 1・頚 譏ｯ 2
     */
    protected float convertPivot(BonesItem bones, int index) {
        if (bones.getParent() != null) {
            if (index == 1) {
                return indexBones.get(bones.getParent()).getPivot().get(index) - bones.getPivot().get(index);
            } else {
                return bones.getPivot().get(index) - indexBones.get(bones.getParent()).getPivot().get(index);
            }
        } else {
            if (index == 1) {
                return 24 - bones.getPivot().get(index);
            } else {
                return bones.getPivot().get(index);
            }
        }
    }

    protected float convertPivot(BonesItem parent, CubesItem cube, int index) {
        assert cube.getPivot() != null;
        if (index == 1) {
            return parent.getPivot().get(index) - cube.getPivot().get(index);
        } else {
            return cube.getPivot().get(index) - parent.getPivot().get(index);
        }
    }

    /**
     * 蝓ｺ蟯ｩ迚亥柱 Java 迚域悽逧・婿蝮苓ｵｷ蟋句攝譬・ｹ滉ｸ堺ｸ閾ｴ・繰ava 譏ｯ逶ｸ蟇ｹ蝮先・ｼ瑚御ｸ・y 蛟ｼ譁ｹ蜷台ｸ堺ｸ閾ｴ縲・
     * 蝓ｺ蟯ｩ迚域弍扈晏ｯｹ蝮先・ｼ瑚御ｸ・y 譁ｹ蜷第悃荳翫・
     * 蜈ｶ螳樔ｸ､閠・ｧ・ｾ句ｾ育ｮ蜊包ｼ御ｽ・弍謌第伽莠・ｸ荳句壕・梧燕譏守區蜥句屓莠九・
     * <li>螯よ棡譏ｯ x・頚 霓ｴ・碁ぅ荵亥宵髴隕∵婿蝮苓ｵｷ蟋句攝譬・㍼蜴ｻ譌玖ｽｬ轤ｹ蝮先・
     * <li>螯よ棡譏ｯ y 霓ｴ・梧雷霓ｬ轤ｹ蝮先・㍼蜴ｻ譁ｹ蝮苓ｵｷ蟋句攝譬・ｼ悟・蜃丞悉譁ｹ蝮礼噪 y 髟ｿ蠎ｦ
     *
     * @param index 譏ｯ xyz 逧・頭荳荳ｪ・警 譏ｯ 0・軽 譏ｯ 1・頚 譏ｯ 2
     */
    protected float convertOrigin(BonesItem bone, CubesItem cube, int index) {
        if (index == 1) {
            return bone.getPivot().get(index) - cube.getOrigin().get(index) - cube.getSize().get(index);
        } else {
            return cube.getOrigin().get(index) - bone.getPivot().get(index);
        }
    }

    protected float convertOrigin(CubesItem cube, int index) {
        assert cube.getPivot() != null;
        if (index == 1) {
            return cube.getPivot().get(index) - cube.getOrigin().get(index) - cube.getSize().get(index);
        } else {
            return cube.getOrigin().get(index) - cube.getPivot().get(index);
        }
    }

    /**
     * 蝓ｺ蟯ｩ迚育畑逧・弍蠎ｦ・繰ava 迚育畑逧・弍蠑ｧ蠎ｦ・瑚ｿ吩ｸｪ霓ｬ謐｢蠕育ｮ蜊・
     */
    protected float convertRotation(float degree) {
        return (float) (degree * Math.PI / 180);
    }

    public BedrockPart getNode(String nodeName) {
        ModelRendererWrapper rendererWrapper = modelMap.get(nodeName);
        if (rendererWrapper != null) {
            return rendererWrapper.getModelRenderer();
        } else {
            return null;
        }
    }

    public BonesItem getBone(String name) {
        return indexBones.get(name);
    }

    public void render(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay) {
        render(matrixStack, transformType, renderType, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    public void render(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay, float red, float green, float blue, float alpha) {
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer builder = bufferSource.getBuffer(renderType);

        matrixStack.pushPose();
        for (BedrockPart model : shouldRender) {
            model.render(matrixStack, transformType, builder, light, overlay, red, green, blue, alpha);
        }
        matrixStack.popPose();
        if (!OculusCompat.endBatch(bufferSource)) {
            bufferSource.endBatch(renderType);
        }

        for (IFunctionalRenderer renderer : delegateRenderers) {
            renderer.render(matrixStack, builder, transformType, light, overlay);
        }
        delegateRenderers = new ArrayList<>();
    }

    protected List<BedrockPart> getPath(@Nullable ModelRendererWrapper rendererWrapper) {
        if (rendererWrapper == null) {
            return null;
        }
        BedrockPart part = rendererWrapper.getModelRenderer();
        List<BedrockPart> path = new ArrayList<>();
        Stack<BedrockPart> stack = new Stack<>();
        do {
            stack.push(part);
            part = part.getParent();
        } while (part != null);
        while (!stack.isEmpty()) {
            part = stack.pop();
            path.add(part);
        }
        return path;
    }

    @Nullable
    public Vec3 getOffset() {
        return offset;
    }

    @Nullable
    public Vec2 getSize() {
        return size;
    }

    public List<BedrockPart> getShouldRender() {
        return shouldRender;
    }

    public HashMap<String, BonesItem> getIndexBones() {
        return indexBones;
    }
}

