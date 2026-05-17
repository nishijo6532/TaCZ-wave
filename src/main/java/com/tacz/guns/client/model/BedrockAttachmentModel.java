package com.tacz.guns.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.model.bedrock.ModelRendererWrapper;
import com.tacz.guns.client.model.functional.BeamRenderer;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.client.model.functional.TextShowRender;
import com.tacz.guns.client.resource.pojo.display.gun.TextShow;
import com.tacz.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tacz.guns.client.resource.pojo.model.BedrockVersion;
import com.tacz.guns.compat.oculus.OculusCompat;
import com.tacz.guns.util.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BedrockAttachmentModel extends BedrockAnimatedModel {
    private static final String SCOPE_VIEW_NODE = "scope_view";
    private static final String SCOPE_BODY_NODE = "scope_body";
    private static final String OCULAR_RING_NODE = "ocular_ring";
    private static final String DIVISION_NODE = "division";
    private static final String OCULAR_NODE = "ocular";
    private static final String OCULAR_SIGHT_NODE = "ocular_sight";
    private static final String OCULAR_SCOPE_NODE = "ocular_scope";
    private static final String LENS_ILLUMINATED_NODE = "lens_illuminated";
    private static final String LE_ILLUMINATED_NODE = "le_illuminated";
    private static final Pattern LASER_BEAM_PATTERN = Pattern.compile("^laser_beam(_(\\d+))?$");

    protected List<List<BedrockPart>> scopeViewPaths;
    protected @Nullable List<BedrockPart> scopeBodyPath;
    protected @Nullable List<BedrockPart> ocularRingPath;
    protected List<List<BedrockPart>> ocularNodePaths;
    protected List<Boolean> isScopeOcular;
    protected List<List<BedrockPart>> divisionNodePaths;
    protected List<List<BedrockPart>> divisionInternalDisplayPaths;
    protected List<List<BedrockPart>> scopeOverlayNodePaths;
    protected @Nullable List<List<BedrockPart>> laserBeamPaths;

    private @Nullable ItemStack currentGunItem;
    private @Nullable ItemStack attachmentItem;

    private boolean isScope = false;
    private boolean isSight = false;
    private float scopeViewRadiusModifier = 1;

    public BedrockAttachmentModel(BedrockModelPOJO pojo, BedrockVersion version) {
        super(pojo, version);
        scopeViewPaths = new ArrayList<>();
        ocularNodePaths = new ArrayList<>();
        isScopeOcular = new ArrayList<>();
        divisionNodePaths = new ArrayList<>();
        divisionInternalDisplayPaths = new ArrayList<>();
        scopeOverlayNodePaths = new ArrayList<>();
        laserBeamPaths = new ArrayList<>();

        List<BedrockPart> path = getPath(modelMap.get(SCOPE_VIEW_NODE));
        int i = 2;
        while (path != null) {
            scopeViewPaths.add(path);
            path = getPath(modelMap.get(SCOPE_VIEW_NODE + '_' + i++));
        }

        Pattern ocularPattern = Pattern.compile("^(" + OCULAR_NODE + "|" + OCULAR_SIGHT_NODE + "|" + OCULAR_SCOPE_NODE + ")(_(\\d+))?$");
        List<OcularWrapper> ocularWrappers = new ArrayList<>();
        for (Map.Entry<String, ModelRendererWrapper> entry : modelMap.entrySet()) {
            Matcher matcher = ocularPattern.matcher(entry.getKey());
            if (matcher.matches()) {
                int num = 1;
                String numStr = matcher.group(3);
                if (numStr != null) {
                    num = Integer.parseInt(numStr);
                }
                String type = matcher.group(1);
                ocularWrappers.add(new OcularWrapper(entry.getValue(), OCULAR_SCOPE_NODE.equals(type), num, ocularTypePriority(type)));
            }
            if (LASER_BEAM_PATTERN.matcher(entry.getKey()).find()) {
                addPathIfPresent(laserBeamPaths, entry.getValue());
            }
        }
        ocularWrappers.sort((left, right) -> {
            int indexCompare = Integer.compare(left.index, right.index);
            return indexCompare != 0 ? indexCompare : Integer.compare(left.priority, right.priority);
        });
        for (OcularWrapper wrapper : ocularWrappers) {
            addOcularPathIfPresent(wrapper);
        }

        ModelRendererWrapper divisionModel = modelMap.get(DIVISION_NODE);
        path = getPath(divisionModel);
        i = 2;
        while (path != null) {
            divisionNodePaths.add(path);
            collectDivisionInternalDisplayPaths(path);
            if (divisionModel != null) {
                divisionModel.setHidden(true);
            }
            divisionModel = modelMap.get(DIVISION_NODE + '_' + i++);
            path = getPath(divisionModel);
        }

        scopeBodyPath = getPath(modelMap.get(SCOPE_BODY_NODE));
        ocularRingPath = getPath(modelMap.get(OCULAR_RING_NODE));
        addPathIfPresent(scopeOverlayNodePaths, modelMap.get(LENS_ILLUMINATED_NODE));
        addPathIfPresent(scopeOverlayNodePaths, modelMap.get(LE_ILLUMINATED_NODE));
    }

    private void addPathIfPresent(List<List<BedrockPart>> paths, @Nullable ModelRendererWrapper renderer) {
        List<BedrockPart> path = getPath(renderer);
        if (path != null) {
            paths.add(path);
        }
    }

    private void addOcularPathIfPresent(OcularWrapper wrapper) {
        List<BedrockPart> path = getPath(wrapper.renderer);
        if (path != null) {
            ocularNodePaths.add(path);
            isScopeOcular.add(wrapper.isScope);
        }
    }

    private static int ocularTypePriority(String type) {
        if (OCULAR_NODE.equals(type)) {
            return 0;
        }
        if (OCULAR_SIGHT_NODE.equals(type)) {
            return 1;
        }
        return 2;
    }

    @Nullable
    public List<BedrockPart> getScopeViewPath(int viewSwitchCount) {
        if (scopeViewPaths.isEmpty()) {
            return null;
        }
        if (viewSwitchCount >= scopeViewPaths.size()) {
            return scopeViewPaths.get(0);
        }
        return scopeViewPaths.get(viewSwitchCount);
    }

    public void setIsScope(boolean isScope) { this.isScope = isScope; }
    public void setIsSight(boolean isSight) { this.isSight = isSight; }
    public boolean isScope() { return isScope; }
    public boolean isSight() { return isSight; }
    public void setScopeViewRadiusModifier(float scopeViewRadiusModifier) { this.scopeViewRadiusModifier = scopeViewRadiusModifier; }

    public void setTextShowList(Map<String, TextShow> textShowList) {
        textShowList.forEach((name, textShow) -> this.setFunctionalRenderer(name,
                bedrockPart -> new TextShowRender(this, name, textShow, currentGunItem, true)));
    }

    public void render(@Nullable ItemStack attachmentItem, ItemStack currentGunItem, PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay) {
        this.currentGunItem = currentGunItem;
        this.attachmentItem = attachmentItem;
        if (shouldUseForge64ScopeVisual(transformType)) {
            if (isSight) {
                renderForge64ScopeAndSightVisual(matrixStack, transformType, renderType, light, overlay);
            } else {
                renderForge64ScopeVisual(matrixStack, transformType, renderType, light, overlay);
            }
            renderLaserIfNeeded(attachmentItem, matrixStack, transformType);
            return;
        }
        if (isAimingInFirstPerson(transformType) && isSight) {
            renderSight(matrixStack, transformType, renderType, light, overlay);
            renderLaserIfNeeded(attachmentItem, matrixStack, transformType);
            return;
        }

        if (!isFirstPersonCameraOrTransform(transformType)) {
            if (scopeBodyPath != null) {
                renderTempPart(matrixStack, transformType, renderType, light, overlay, scopeBodyPath);
            }
            if (ocularRingPath != null) {
                renderTempPart(matrixStack, transformType, renderType, light, overlay, ocularRingPath);
            }
        }
        if (!isScope && !isSight) {
            renderLaserIfNeeded(attachmentItem, matrixStack, transformType);
        }
        super.render(matrixStack, transformType, renderType, light, overlay);
        if (isScope || isSight) {
            renderLaserIfNeeded(attachmentItem, matrixStack, transformType);
        }
    }

    private boolean shouldUseForge64ScopeVisual(ItemDisplayContext transformType) {
        return hasScopeVisualContract() && isAimingInFirstPerson(transformType);
    }

    private boolean isAimingInFirstPerson(ItemDisplayContext transformType) {
        return isFirstPersonCameraOrTransform(transformType) && getAimingProgress() > 0.01F;
    }

    private float getAimingProgress() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return 0.0F;
        }
        float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        return IClientPlayerGunOperator.fromLocalPlayer(player).getClientAimingProgress(partialTick);
    }

    private boolean isFirstPersonCameraOrTransform(ItemDisplayContext transformType) {
        if (isFirstPersonTransform(transformType)) {
            return true;
        }
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null
                && minecraft.options != null
                && minecraft.options.getCameraType().isFirstPerson()
                && minecraft.player != null
                && currentGunItem != null
                && (ItemStack.matches(currentGunItem, minecraft.player.getMainHandItem())
                || ItemStack.matches(currentGunItem, minecraft.player.getOffhandItem()));
    }

    private boolean isFirstPersonTransform(ItemDisplayContext transformType) {
        return transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || transformType == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || transformType.firstPerson();
    }

    private boolean hasScopeVisualContract() {
        return isScope || (!scopeViewPaths.isEmpty() && (scopeBodyPath != null || ocularRingPath != null));
    }

    private void renderForge64ScopeVisual(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay) {
        List<PartVisibility> hiddenParts = new ArrayList<>();
        List<IFunctionalRenderer> scopeBodyDelegateRenderers = new ArrayList<>();
        try {
            resetStencilState();
            RenderHelper.enableItemEntityStencilTest();
            GL11.glClearStencil(0);
            GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
            if (ocularRingPath != null) {
                GL11.glStencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
                GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
                renderTempPart(matrixStack, transformType, renderType, light, overlay, ocularRingPath);
            }
            renderOcularStencil(matrixStack, transformType, renderType, light, overlay, resolvePureScopeOcularFilter());
            renderOcularDepthMask(matrixStack, transformType, renderType, light, overlay, resolvePureScopeOcularFilter());
            if (scopeBodyPath != null) {
                List<PartVisibility> hiddenBodyChildren = new ArrayList<>();
                hideScopeBodyContractChildren(hiddenBodyChildren);
                GL11.glStencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
                try {
                    renderTempPart(matrixStack, transformType, renderType, light, overlay, scopeBodyPath);
                    scopeBodyDelegateRenderers.addAll(delegateRenderers);
                    delegateRenderers = new ArrayList<>();
                } finally {
                    restoreVisibility(hiddenBodyChildren);
                }
            }
            renderDivisionInScopeRange(matrixStack, transformType, renderType, light, overlay);
            GL11.glStencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
            RenderHelper.disableItemEntityStencilTest();
            hideContractParts(hiddenParts);
            super.render(matrixStack, transformType, renderType, light, overlay);
            flushDelegateRenderersAlwaysOnTop(matrixStack, transformType, renderType, light, overlay, scopeBodyDelegateRenderers);
        } finally {
            restoreVisibility(hiddenParts);
            resetStencilState();
        }
    }

    private void renderForge64ScopeAndSightVisual(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay) {
        List<PartVisibility> hiddenParts = new ArrayList<>();
        List<IFunctionalRenderer> scopeBodyDelegateRenderers = new ArrayList<>();
        try {
            Integer activeViewIndex = resolveActiveViewIndex();
            resetStencilState();
            RenderHelper.enableItemEntityStencilTest();
            GL11.glClearStencil(0);
            GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
            if (ocularRingPath != null) {
                GL11.glStencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
                GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
                renderTempPart(matrixStack, transformType, renderType, light, overlay, ocularRingPath);
            }
            renderOcularStencil(matrixStack, transformType, renderType, light, overlay, true);
            renderOcularDepthMask(matrixStack, transformType, renderType, light, overlay, true);
            if (scopeBodyPath != null) {
                List<PartVisibility> hiddenBodyChildren = new ArrayList<>();
                hideScopeBodyContractChildren(hiddenBodyChildren);
                GL11.glStencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
                try {
                    renderTempPart(matrixStack, transformType, renderType, light, overlay, scopeBodyPath);
                    scopeBodyDelegateRenderers.addAll(delegateRenderers);
                    delegateRenderers = new ArrayList<>();
                } finally {
                    restoreVisibility(hiddenBodyChildren);
                }
            }
            renderDivisionInScopeRange(matrixStack, transformType, renderType, light, overlay, true, activeViewIndex);
            renderOcularStencil(matrixStack, transformType, renderType, light, overlay, false);
            renderDivisionOnly(matrixStack, transformType, renderType, light, overlay, false, activeViewIndex);
            GL11.glStencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
            RenderHelper.disableItemEntityStencilTest();
            hideContractParts(hiddenParts);
            super.render(matrixStack, transformType, renderType, light, overlay);
            flushDelegateRenderersAlwaysOnTop(matrixStack, transformType, renderType, light, overlay, scopeBodyDelegateRenderers);
        } finally {
            restoreVisibility(hiddenParts);
            resetStencilState();
        }
    }

    private @Nullable Boolean resolvePureScopeOcularFilter() {
        boolean hasNonScope = false;
        boolean hasScope = false;
        for (Boolean value : isScopeOcular) {
            if (value) { hasScope = true; } else { hasNonScope = true; }
        }
        if (hasNonScope) { return false; }
        if (hasScope) { return true; }
        return null;
    }

    private @Nullable Integer resolveActiveViewIndex() {
        IAttachment attachment = IAttachment.getIAttachmentOrNull(attachmentItem);
        if (attachment == null) {
            return null;
        }
        return TimelessAPI.getClientAttachmentIndex(attachment.getAttachmentId(attachmentItem))
                .map(ClientAttachmentIndex::getViews)
                .filter(views -> views.length > 0)
                .map(views -> Math.max(0, views[attachment.getZoomNumber(attachmentItem) % views.length] - 1))
                .orElse(null);
    }

    private void hideContractParts(List<PartVisibility> hiddenParts) {
        hidePathEnds(scopeViewPaths, hiddenParts);
        hidePathEnds(ocularNodePaths, hiddenParts);
        hidePathEnds(divisionNodePaths, hiddenParts);
        hidePathEnds(scopeOverlayNodePaths, hiddenParts);
        hideSinglePathEndSubtree(scopeBodyPath, hiddenParts);
        hideSinglePathEnd(ocularRingPath, hiddenParts);
    }

    private void hideScopeBodyContractChildren(List<PartVisibility> hiddenParts) {
        hidePathEnds(scopeViewPaths, hiddenParts);
        hidePathEnds(ocularNodePaths, hiddenParts);
        hidePathEnds(divisionNodePaths, hiddenParts);
        hidePathEnds(scopeOverlayNodePaths, hiddenParts);
        hideSinglePathEnd(ocularRingPath, hiddenParts);
    }

    private void renderTempParts(PoseStack poseStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay, @Nullable List<List<BedrockPart>> paths) {
        if (paths == null) { return; }
        for (List<BedrockPart> path : paths) {
            if (path != null && !path.isEmpty()) {
                renderTempPart(poseStack, transformType, renderType, light, overlay, path);
            }
        }
    }

    private void hideSinglePathEnd(@Nullable List<BedrockPart> path, List<PartVisibility> hiddenParts) {
        if (path == null || path.isEmpty()) { return; }
        BedrockPart part = path.get(path.size() - 1);
        if (isAlreadyTracked(hiddenParts, part)) {
            part.visible = false;
            return;
        }
        hiddenParts.add(new PartVisibility(part, part.visible));
        part.visible = false;
    }

    private void hideSinglePathEndSubtree(@Nullable List<BedrockPart> path, List<PartVisibility> hiddenParts) {
        if (path == null || path.isEmpty()) { return; }
        List<BedrockPart> visited = new ArrayList<>();
        hidePartSubtree(path.get(path.size() - 1), hiddenParts, visited);
    }

    private void hidePartSubtree(BedrockPart part, List<PartVisibility> hiddenParts, List<BedrockPart> visited) {
        if (part == null || visited.contains(part)) { return; }
        visited.add(part);
        if (!isAlreadyTracked(hiddenParts, part)) {
            hiddenParts.add(new PartVisibility(part, part.visible));
        }
        part.visible = false;
        for (BedrockPart child : part.children) {
            hidePartSubtree(child, hiddenParts, visited);
        }
    }

    private boolean isAlreadyTracked(List<PartVisibility> hiddenParts, BedrockPart part) {
        for (PartVisibility visibility : hiddenParts) {
            if (visibility.part() == part) { return true; }
        }
        return false;
    }

    private void hidePathEnds(@Nullable List<List<BedrockPart>> paths, List<PartVisibility> hiddenParts) {
        if (paths == null) { return; }
        for (List<BedrockPart> path : paths) { hideSinglePathEnd(path, hiddenParts); }
    }

    private void restoreVisibility(List<PartVisibility> hiddenParts) {
        for (PartVisibility visibility : hiddenParts) {
            visibility.part().visible = visibility.visible();
        }
    }

    private void resetStencilState() {
        GL11.glStencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        GL11.glColorMask(true, true, true, true);
        GL11.glDepthMask(true);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        RenderHelper.disableItemEntityStencilTest();
    }

    private void renderTempPart(PoseStack poseStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay, @Nonnull List<BedrockPart> path) {
        poseStack.pushPose();
        try {
            for (int i = 0; i < path.size() - 1; ++i) {
                path.get(i).translateAndRotateAndScale(poseStack);
            }
            BedrockPart part = path.get(path.size() - 1);
            boolean wasVisible = part.visible;
            part.visible = true;
            try {
                MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
                VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);
                part.render(poseStack, transformType, vertexConsumer, light, overlay);
                if (!OculusCompat.endBatch(bufferSource)) {
                    bufferSource.endBatch(renderType);
                }
            } finally {
                part.visible = wasVisible;
            }
        } finally {
            poseStack.popPose();
        }
    }

    private void renderOcularStencil(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay, @Nullable Boolean scopeFilter) {
        if (!ocularNodePaths.isEmpty()) {
            GL11.glColorMask(false, false, false, false);
            GL11.glDepthMask(false);
            GL11.glStencilMask(0xFF);
            GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
            for (int i = ocularNodePaths.size() - 1; i >= 0; i--) {
                if (scopeFilter == null || scopeFilter == isScopeOcular.get(i)) {
                    GL11.glStencilFunc(GL11.GL_GREATER, i + 1, 0xFF);
                    renderTempPart(matrixStack, transformType, renderType, light, overlay, ocularNodePaths.get(i));
                }
            }
            GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
            GL11.glDepthMask(true);
            GL11.glColorMask(true, true, true, true);
        }
    }

    private void renderOcularDepthMask(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay, @Nullable Boolean scopeFilter) {
        if (ocularNodePaths.isEmpty()) { return; }
        boolean depthTestEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        int previousDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        try {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_ALWAYS);
            GL11.glDepthMask(true);
            GL11.glColorMask(false, false, false, false);
            for (int i = 0; i < ocularNodePaths.size(); i++) {
                if (scopeFilter == null || scopeFilter == isScopeOcular.get(i)) {
                    renderTempPart(matrixStack, transformType, renderType, light, overlay, ocularNodePaths.get(i));
                }
            }
        } finally {
            if (depthTestEnabled) {
                GL11.glEnable(GL11.GL_DEPTH_TEST);
            } else {
                GL11.glDisable(GL11.GL_DEPTH_TEST);
            }
            GL11.glDepthFunc(previousDepthFunc);
            GL11.glDepthMask(true);
            GL11.glColorMask(true, true, true, true);
        }
    }

    private void renderDivisionOnly(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay) {
        renderDivisionOnly(matrixStack, transformType, renderType, light, overlay, null);
    }

    private void renderDivisionOnly(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay, @Nullable Boolean scopeFilter) {
        renderDivisionOnly(matrixStack, transformType, renderType, light, overlay, scopeFilter, null);
    }

    private void renderDivisionOnly(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay, @Nullable Boolean scopeFilter, @Nullable Integer activeViewIndex) {
        if (!divisionNodePaths.isEmpty()) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            int pathCount = scopeFilter == null ? divisionNodePaths.size() : Math.min(divisionNodePaths.size(), ocularNodePaths.size());
            for (int i = 0; i < pathCount; i++) {
                if (scopeFilter != null && scopeFilter != isScopeOcular.get(i)) {
                    continue;
                }
                if (activeViewIndex != null && activeViewIndex != i) {
                    continue;
                }
                GL11.glStencilFunc(GL11.GL_EQUAL, i + 1, 0xFF);
                renderTempPart(matrixStack, transformType, renderType, light, overlay, divisionNodePaths.get(i));
            }
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        }
    }

    private void renderSight(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay) {
        List<PartVisibility> hiddenParts = new ArrayList<>();
        try {
            resetStencilState();
            RenderHelper.enableItemEntityStencilTest();
            GL11.glClearStencil(0);
            GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
            renderOcularStencil(matrixStack, transformType, renderType, light, overlay, false);
            renderDivisionOnly(matrixStack, transformType, renderType, light, overlay);
            GL11.glStencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
            RenderHelper.disableItemEntityStencilTest();
            if (scopeBodyPath != null) {
                renderTempPart(matrixStack, transformType, renderType, light, overlay, scopeBodyPath);
                flushDelegateRenderersAlwaysOnTop(matrixStack, transformType, renderType, light, overlay);
            }
            hideContractParts(hiddenParts);
            super.render(matrixStack, transformType, renderType, light, overlay);
            renderTempParts(matrixStack, transformType, renderType, light, overlay, scopeOverlayNodePaths);
        } finally {
            restoreVisibility(hiddenParts);
            resetStencilState();
        }
    }

    private void renderLaserIfNeeded(@Nullable ItemStack attachmentItem, PoseStack matrixStack, ItemDisplayContext transformType) {
        if (laserBeamPaths == null) { return; }
        for (var entry : laserBeamPaths) {
            BeamRenderer.renderLaserBeam(attachmentItem, matrixStack, transformType, entry);
        }
    }

    private void flushDelegateRenderersAlwaysOnTop(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay) {
        if (delegateRenderers.isEmpty()) { return; }
        List<IFunctionalRenderer> renderers = new ArrayList<>(delegateRenderers);
        delegateRenderers = new ArrayList<>();
        flushDelegateRenderersAlwaysOnTop(matrixStack, transformType, renderType, light, overlay, renderers);
    }

    private void flushDelegateRenderersAlwaysOnTop(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay, List<IFunctionalRenderer> renderers) {
        if (renderers.isEmpty()) { return; }
        boolean depthTestEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        int previousDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        boolean previousDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        try {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_ALWAYS);
            GL11.glDepthMask(false);
            MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
            VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);
            List<IFunctionalRenderer> pendingRenderers = new ArrayList<>(renderers);
            for (int pass = 0; pass < 4 && !pendingRenderers.isEmpty(); pass++) {
                for (IFunctionalRenderer renderer : pendingRenderers) {
                    renderer.render(matrixStack, vertexConsumer, transformType, light, overlay);
                }
                pendingRenderers = new ArrayList<>(delegateRenderers);
                delegateRenderers = new ArrayList<>();
            }
        } finally {
            if (depthTestEnabled) {
                GL11.glEnable(GL11.GL_DEPTH_TEST);
            } else {
                GL11.glDisable(GL11.GL_DEPTH_TEST);
            }
            GL11.glDepthFunc(previousDepthFunc);
            GL11.glDepthMask(previousDepthMask);
        }
    }

    private void renderDivisionInScopeRange(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay) {
        renderDivisionInScopeRange(matrixStack, transformType, renderType, light, overlay, resolvePureScopeOcularFilter());
    }

    private void renderDivisionInScopeRange(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay, @Nullable Boolean scopeFilter) {
        renderDivisionInScopeRange(matrixStack, transformType, renderType, light, overlay, scopeFilter, null);
    }

    private void renderDivisionInScopeRange(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay, @Nullable Boolean scopeFilter, @Nullable Integer activeViewIndex) {
        if (divisionNodePaths.isEmpty()) { return; }
        boolean depthTestEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        int previousDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        try {
            GL11.glClearStencil(0);
            GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
            renderDivisionCopiedOcularStencil(matrixStack, transformType, renderType, light, overlay, scopeFilter);
            renderDivisionCopiedOcularDepthMask(matrixStack, transformType, renderType, light, overlay, scopeFilter);
            if (depthTestEnabled) {
                GL11.glEnable(GL11.GL_DEPTH_TEST);
            } else {
                GL11.glDisable(GL11.GL_DEPTH_TEST);
            }
            GL11.glDepthFunc(GL11.GL_GREATER);
            GL11.glDepthMask(false);
            int pathCount = Math.min(divisionNodePaths.size(), ocularNodePaths.size());
            for (int i = 0; i < pathCount; i++) {
                if (scopeFilter != null && scopeFilter != isScopeOcular.get(i)) {
                    continue;
                }
                if (activeViewIndex != null && activeViewIndex != i) {
                    continue;
                }
                GL11.glStencilFunc(GL11.GL_EQUAL, i + 1, 0xFF);
                renderTempPart(matrixStack, transformType, renderType, light, overlay, divisionNodePaths.get(i));
            }
        } finally {
            if (depthTestEnabled) {
                GL11.glEnable(GL11.GL_DEPTH_TEST);
            } else {
                GL11.glDisable(GL11.GL_DEPTH_TEST);
            }
            GL11.glDepthFunc(previousDepthFunc);
            GL11.glDepthMask(true);
            GL11.glStencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
        }
    }

    private void renderDivisionCopiedOcularStencil(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay, @Nullable Boolean scopeFilter) {
        if (!ocularNodePaths.isEmpty()) {
            GL11.glColorMask(false, false, false, false);
            GL11.glDepthMask(false);
            GL11.glStencilMask(0xFF);
            GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
            for (int i = ocularNodePaths.size() - 1; i >= 0; i--) {
                if (scopeFilter == null || scopeFilter == isScopeOcular.get(i)) {
                    GL11.glStencilFunc(GL11.GL_GREATER, i + 1, 0xFF);
                    renderTempPart(matrixStack, transformType, renderType, light, overlay, ocularNodePaths.get(i));
                }
            }
            GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
            GL11.glDepthMask(true);
            GL11.glColorMask(true, true, true, true);
        }
    }

    private void renderDivisionCopiedOcularDepthMask(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, int light, int overlay, @Nullable Boolean scopeFilter) {
        if (ocularNodePaths.isEmpty()) { return; }
        boolean depthTestEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        int previousDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        try {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_ALWAYS);
            GL11.glDepthMask(true);
            GL11.glColorMask(false, false, false, false);
            for (int i = 0; i < ocularNodePaths.size(); i++) {
                if (scopeFilter == null || scopeFilter == isScopeOcular.get(i)) {
                    renderTempPart(matrixStack, transformType, renderType, light, overlay, ocularNodePaths.get(i));
                }
            }
        } finally {
            if (depthTestEnabled) {
                GL11.glEnable(GL11.GL_DEPTH_TEST);
            } else {
                GL11.glDisable(GL11.GL_DEPTH_TEST);
            }
            GL11.glDepthFunc(previousDepthFunc);
            GL11.glDepthMask(true);
            GL11.glColorMask(true, true, true, true);
        }
    }

    private void collectDivisionInternalDisplayPaths(@Nullable List<BedrockPart> divisionPath) {
        if (divisionPath == null || divisionPath.isEmpty()) { return; }
        List<BedrockPart> seedPath = new ArrayList<>(divisionPath);
        BedrockPart divisionLeaf = divisionPath.get(divisionPath.size() - 1);
        for (BedrockPart child : divisionLeaf.children) {
            collectDivisionInternalDisplayPathsRecursive(seedPath, child);
        }
    }

    private void collectDivisionInternalDisplayPathsRecursive(List<BedrockPart> parentPath, BedrockPart current) {
        List<BedrockPart> currentPath = new ArrayList<>(parentPath);
        currentPath.add(current);
        if (current.illuminated && !containsPath(divisionInternalDisplayPaths, currentPath)) {
            divisionInternalDisplayPaths.add(currentPath);
        }
        for (BedrockPart child : current.children) {
            collectDivisionInternalDisplayPathsRecursive(currentPath, child);
        }
    }

    private boolean containsPath(List<List<BedrockPart>> existingPaths, List<BedrockPart> candidate) {
        for (List<BedrockPart> existing : existingPaths) {
            if (existing.size() != candidate.size()) {
                continue;
            }
            boolean same = true;
            for (int i = 0; i < existing.size(); i++) {
                if (existing.get(i) != candidate.get(i)) {
                    same = false;
                    break;
                }
            }
            if (same) {
                return true;
            }
        }
        return false;
    }

    private static class OcularWrapper {
        public ModelRendererWrapper renderer;
        public boolean isScope;
        public int index;
        public int priority;

        public OcularWrapper(ModelRendererWrapper renderer, boolean isScope, int index, int priority) {
            this.renderer = renderer;
            this.isScope = isScope;
            this.index = index;
            this.priority = priority;
        }
    }

    private record PartVisibility(BedrockPart part, boolean visible) {
    }
}
