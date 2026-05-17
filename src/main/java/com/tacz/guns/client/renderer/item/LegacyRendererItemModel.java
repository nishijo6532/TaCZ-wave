package com.tacz.guns.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IBlock;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.nbt.BlockItemDataAccessor;
import com.tacz.guns.client.model.SlotModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.renderer.block.GunSmithTableRenderer;
import com.tacz.guns.client.resource.index.ClientBlockIndex;
import com.tacz.guns.init.ModItems;
import com.tacz.guns.item.AmmoBoxItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemModels;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ModelDiscovery;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.cuboid.CuboidModel;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class LegacyRendererItemModel implements ItemModel {
    private static final Identifier MODEL_TYPE_ID = com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "legacy_renderer");
    private static final Supplier<Vector3fc[]> EXTENTS = () -> new Vector3fc[]{new Vector3f(0, 0, 0), new Vector3f(1, 1, 1)};
    private static volatile boolean registered;

    private final Kind kind;
    private final Matrix4f transformation;
    private static final LegacyRendererItemModel GUN_FALLBACK_MODEL = new LegacyRendererItemModel(Kind.GUN, new Matrix4f());
    private static final LegacyRendererItemModel AMMO_FALLBACK_MODEL = new LegacyRendererItemModel(Kind.AMMO, new Matrix4f());
    private static final LegacyRendererItemModel ATTACHMENT_FALLBACK_MODEL = new LegacyRendererItemModel(Kind.ATTACHMENT, new Matrix4f());
    private static final LegacyRendererItemModel BLOCK_FALLBACK_MODEL = new LegacyRendererItemModel(Kind.GUN_SMITH_TABLE, new Matrix4f());
    private static final LegacyRendererItemModel FLAT_ITEM_FALLBACK_MODEL = new LegacyRendererItemModel(Kind.FLAT_ITEM_ICON, new Matrix4f());
    private static final AmmoBoxFallbackItemModel AMMO_BOX_FALLBACK_MODEL = new AmmoBoxFallbackItemModel();
    private static final SlotModel GUI_SLOT_MODEL = new SlotModel();

    private LegacyRendererItemModel(Kind kind, Matrix4fc transformation) {
        this.kind = kind;
        this.transformation = new Matrix4f(transformation);
    }

    public static void registerType() {
        if (registered) {
            return;
        }
        try {
            Field mapperField = ItemModels.class.getDeclaredField("ID_MAPPER");
            mapperField.setAccessible(true);
            Object mapper = mapperField.get(null);
            Method put = mapper.getClass().getMethod("put", Object.class, Object.class);
            put.invoke(mapper, MODEL_TYPE_ID, Unbaked.MAP_CODEC);
            registered = true;
        } catch (ReflectiveOperationException exception) {
            GunMod.LOGGER.error("Failed to register legacy item renderer model type", exception);
        }
    }

    public static boolean applyFallback(
            ItemStackRenderState output,
            ItemStack item,
            ItemModelResolver resolver,
            ItemDisplayContext displayContext,
            @Nullable ClientLevel level,
            @Nullable ItemOwner owner,
            int seed
    ) {
        if (item.getItem() instanceof IGun) {
            GUN_FALLBACK_MODEL.update(output, item, resolver, displayContext, level, owner, seed);
            return true;
        }
        if (item.getItem() instanceof IAmmo) {
            AMMO_FALLBACK_MODEL.update(output, item, resolver, displayContext, level, owner, seed);
            return true;
        }
        if (item.getItem() instanceof IAttachment) {
            ATTACHMENT_FALLBACK_MODEL.update(output, item, resolver, displayContext, level, owner, seed);
            return true;
        }
        if (item.getItem() instanceof BlockItemDataAccessor) {
            BLOCK_FALLBACK_MODEL.update(output, item, resolver, displayContext, level, owner, seed);
            return true;
        }
        if (item.is(ModItems.AMMO_BOX.get())) {
            AMMO_BOX_FALLBACK_MODEL.update(output, item, resolver, displayContext, level, owner, seed);
            return true;
        }
        if (displayContext == ItemDisplayContext.GUI && isFlatItemFallback(item)) {
            FLAT_ITEM_FALLBACK_MODEL.update(output, item, resolver, displayContext, level, owner, seed);
            return true;
        }
        return false;
    }

    private static boolean isFlatItemFallback(ItemStack item) {
        return item.is(ModItems.TARGET.get())
                || item.is(ModItems.STATUE.get())
                || item.is(ModItems.TARGET_MINECART.get());
    }

    @Override
    public void update(
            ItemStackRenderState output,
            ItemStack item,
            ItemModelResolver resolver,
            ItemDisplayContext displayContext,
            @Nullable ClientLevel level,
            @Nullable ItemOwner owner,
            int seed
    ) {
        output.appendModelIdentityElement(this.kind);
        if (displayContext == ItemDisplayContext.GUI) {
            Object guiIdentity = this.kind.getGuiIdentity(item);
            if (guiIdentity != null) {
                output.appendModelIdentityElement(guiIdentity);
            }
        }
        ItemStackRenderState.LayerRenderState layer = output.newLayer();
        if (item.hasFoil()) {
            ItemStackRenderState.FoilType foilType = ItemStackRenderState.FoilType.STANDARD;
            layer.setFoilType(foilType);
            output.setAnimated();
            output.appendModelIdentityElement(foilType);
        }
        layer.setExtents(EXTENTS);
        layer.setLocalTransform(this.transformation);
        layer.setupSpecialModel(this.kind.specialRenderer, new RenderArgument(item.copy(), displayContext));
    }

    private record RenderArgument(ItemStack stack, ItemDisplayContext displayContext) {
    }

    private static final class AmmoBoxFallbackItemModel implements ItemModel {
        private static final Identifier[] MODELS = {
                Identifier.parse("tacz:item/ammo_box/iron_ammo_box_open"),
                Identifier.parse("tacz:item/ammo_box/iron_ammo_box_close"),
                Identifier.parse("tacz:item/ammo_box/gold_ammo_box_open"),
                Identifier.parse("tacz:item/ammo_box/gold_ammo_box_close"),
                Identifier.parse("tacz:item/ammo_box/diamond_ammo_box_open"),
                Identifier.parse("tacz:item/ammo_box/diamond_ammo_box_close"),
                Identifier.parse("tacz:item/ammo_box/creative_ammo_box_open"),
                Identifier.parse("tacz:item/ammo_box/creative_ammo_box_close"),
                Identifier.parse("tacz:item/ammo_box/all_type_creative_ammo_box")
        };

        private final Map<Identifier, BakedAmmoBoxModel> bakedModels = new HashMap<>();

        @Override
        public void update(
                ItemStackRenderState output,
                ItemStack item,
                ItemModelResolver resolver,
                ItemDisplayContext displayContext,
                @Nullable ClientLevel level,
                @Nullable ItemOwner owner,
                int seed
        ) {
            int modelIndex = Math.max(0, Math.min(MODELS.length - 1,
                    (int) AmmoBoxItem.getStatue(item, level, owner == null ? null : owner.asLivingEntity(), seed)));
            Identifier modelId = MODELS[modelIndex];
            BakedAmmoBoxModel model = this.bakedModels.computeIfAbsent(modelId, this::bakeModel);
            if (model == null) {
                FLAT_ITEM_FALLBACK_MODEL.update(output, item, resolver, displayContext, level, owner, seed);
                return;
            }

            output.appendModelIdentityElement(this);
            output.appendModelIdentityElement(modelId);
            ItemStackRenderState.LayerRenderState layer = output.newLayer();
            if (item.hasFoil()) {
                ItemStackRenderState.FoilType foilType = ItemStackRenderState.FoilType.STANDARD;
                layer.setFoilType(foilType);
                output.setAnimated();
                output.appendModelIdentityElement(foilType);
            }

            int tint = 0xFF000000 | (AmmoBoxItem.getColor(item, 0) & 0x00FFFFFF);
            layer.tintLayers().add(tint);
            output.appendModelIdentityElement(tint);
            layer.setExtents(model.extents());
            layer.setLocalTransform(new Matrix4f());
            model.properties().applyToLayer(layer, displayContext);
            layer.prepareQuadList().addAll(model.quads().getAll());
            if (model.quads().hasMaterialFlag(2)) {
                output.setAnimated();
            }
        }

        private BakedAmmoBoxModel bakeModel(Identifier modelId) {
            try {
                ModelBaker baker = new RuntimeModelBaker(Minecraft.getInstance().getModelManager());
                ResolvedModel resolvedModel = baker.getModel(modelId);
                TextureSlots textureSlots = resolvedModel.getTopTextureSlots();
                QuadCollection quads = resolvedModel.bakeTopGeometry(textureSlots, baker, BlockModelRotation.IDENTITY);
                ModelRenderProperties properties = ModelRenderProperties.fromResolvedModel(baker, resolvedModel, textureSlots);
                Vector3fc[] extents = CuboidItemModelWrapper.computeExtents(quads.getAll());
                return new BakedAmmoBoxModel(quads, properties, () -> extents);
            } catch (Exception exception) {
                GunMod.LOGGER.warn("Failed to bake ammo box item model {}", modelId, exception);
                return null;
            }
        }

        private record BakedAmmoBoxModel(QuadCollection quads, ModelRenderProperties properties, Supplier<Vector3fc[]> extents) {
        }
    }

    private static final class RuntimeModelBaker implements ModelBaker {
        private final ModelBakery bakery;
        private final AtlasManager atlasManager;
        private final Interner interner = new RuntimeInterner();
        private final MaterialBaker materials = new RuntimeMaterialBaker();
        private final Map<Identifier, ResolvedModel> standaloneModels = new HashMap<>();

        private RuntimeModelBaker(ModelManager modelManager) throws ReflectiveOperationException {
            this.bakery = modelManager.getModelBakery();
            Field atlasManagerField = ModelManager.class.getDeclaredField("atlasManager");
            atlasManagerField.setAccessible(true);
            this.atlasManager = (AtlasManager) atlasManagerField.get(modelManager);
        }

        @Override
        public ResolvedModel getModel(Identifier location) {
            try {
                Field resolvedModelsField = ModelBakery.class.getDeclaredField("resolvedModels");
                resolvedModelsField.setAccessible(true);
                Map<?, ?> resolvedModels = (Map<?, ?>) resolvedModelsField.get(this.bakery);
                ResolvedModel model = (ResolvedModel) resolvedModels.get(location);
                if (model != null) {
                    return model;
                }
                model = this.resolveStandaloneCuboidModel(location);
                if (model != null) {
                    return model;
                }
                Field missingModelField = ModelBakery.class.getDeclaredField("missingModel");
                missingModelField.setAccessible(true);
                return (ResolvedModel) missingModelField.get(this.bakery);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Failed to resolve cuboid model " + location, exception);
            }
        }

        @Nullable
        private ResolvedModel resolveStandaloneCuboidModel(Identifier location) {
            if (!"tacz".equals(location.getNamespace()) || !location.getPath().startsWith("item/ammo_box/")) {
                return null;
            }
            if (this.standaloneModels.containsKey(location)) {
                return this.standaloneModels.get(location);
            }
            Identifier resource = location.withPath(path -> "models/" + path + ".json");
            try (BufferedReader reader = Minecraft.getInstance().getResourceManager().openAsReader(resource)) {
                CuboidModel cuboidModel = CuboidModel.fromStream(reader);
                Field missingModelField = ModelBakery.class.getDeclaredField("missingModel");
                missingModelField.setAccessible(true);
                ResolvedModel missingModel = (ResolvedModel) missingModelField.get(this.bakery);
                ModelDiscovery discovery = new ModelDiscovery(Map.of(location, cuboidModel), missingModel.wrapped());
                discovery.addSpecialModel(location, cuboidModel);
                ResolvedModel resolvedModel = discovery.resolve().get(location);
                if (resolvedModel == missingModel) {
                    this.standaloneModels.put(location, null);
                    return null;
                }
                if (resolvedModel != null) {
                    this.standaloneModels.put(location, resolvedModel);
                }
                return resolvedModel;
            } catch (IOException | RuntimeException | ReflectiveOperationException exception) {
                GunMod.LOGGER.warn("Failed to resolve standalone ammo box model {}", location, exception);
                this.standaloneModels.put(location, null);
                return null;
            }
        }

        @Override
        public BlockStateModelPart missingBlockModelPart() {
            throw new IllegalStateException("Ammo box item model baking does not use block model parts");
        }

        @Override
        public MaterialBaker materials() {
            return this.materials;
        }

        @Override
        public Interner interner() {
            return this.interner;
        }

        @Override
        public <T> T compute(SharedOperationKey<T> key) {
            return key.compute(this);
        }

        private final class RuntimeMaterialBaker implements MaterialBaker {
            @Override
            public Material.Baked get(Material material, ModelDebugName name) {
                TextureAtlasSprite sprite = atlasManager.get(new SpriteId(TextureAtlas.LOCATION_ITEMS, material.sprite()));
                return new Material.Baked(sprite, material.forceTranslucent());
            }

            @Override
            public Material.Baked reportMissingReference(String reference, ModelDebugName name) {
                TextureAtlasSprite sprite = atlasManager.get(new SpriteId(TextureAtlas.LOCATION_ITEMS, MissingTextureAtlasSprite.getLocation()));
                return new Material.Baked(sprite, false);
            }
        }

        private static final class RuntimeInterner implements Interner {
            @Override
            public Vector3fc vector(Vector3fc vector) {
                return vector;
            }

            @Override
            public BakedQuad.MaterialInfo materialInfo(BakedQuad.MaterialInfo material) {
                return material;
            }
        }
    }

    private enum Kind {
        GUN("gun") {
            private GunItemRendererWrapper renderer;

            @Override
            void render(RenderArgument argument, PoseContext context) {
                if (this.renderer == null) {
                    this.renderer = new GunItemRendererWrapper();
                }
                this.renderer.renderByItem(argument.stack(), argument.displayContext(), context.poseStack(), context.bufferSource(), context.lightCoords(), context.overlayCoords());
            }

            @Override
            Identifier getGuiTexture(ItemStack stack) {
                return TimelessAPI.getGunDisplay(stack)
                        .map(display -> display.getSlotTexture())
                        .orElseGet(MissingTextureAtlasSprite::getLocation);
            }
        },
        AMMO("ammo") {
            private AmmoItemRenderer renderer;

            @Override
            void render(RenderArgument argument, PoseContext context) {
                if (this.renderer == null) {
                    Minecraft minecraft = Minecraft.getInstance();
                    this.renderer = new AmmoItemRenderer(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
                }
                this.renderer.renderByItem(argument.stack(), argument.displayContext(), context.poseStack(), context.bufferSource(), context.lightCoords(), context.overlayCoords());
            }

            @Override
            Identifier getGuiTexture(ItemStack stack) {
                if (stack.getItem() instanceof IAmmo ammo) {
                    return TimelessAPI.getClientAmmoIndex(ammo.getAmmoId(stack))
                            .map(index -> index.getSlotTextureLocation())
                            .orElseGet(MissingTextureAtlasSprite::getLocation);
                }
                return MissingTextureAtlasSprite.getLocation();
            }
        },
        ATTACHMENT("attachment") {
            private AttachmentItemRenderer renderer;

            @Override
            void render(RenderArgument argument, PoseContext context) {
                if (this.renderer == null) {
                    Minecraft minecraft = Minecraft.getInstance();
                    this.renderer = new AttachmentItemRenderer(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
                }
                this.renderer.renderByItem(argument.stack(), argument.displayContext(), context.poseStack(), context.bufferSource(), context.lightCoords(), context.overlayCoords());
            }

            @Override
            Identifier getGuiTexture(ItemStack stack) {
                if (stack.getItem() instanceof IAttachment attachment) {
                    return TimelessAPI.getClientAttachmentIndex(attachment.getAttachmentId(stack))
                            .map(index -> index.getSlotTexture())
                            .orElseGet(MissingTextureAtlasSprite::getLocation);
                }
                return MissingTextureAtlasSprite.getLocation();
            }
        },
        GUN_SMITH_TABLE("gun_smith_table") {
            private GunSmithTableItemRenderer renderer;

            @Override
            void render(RenderArgument argument, PoseContext context) {
                if (this.renderer == null) {
                    Minecraft minecraft = Minecraft.getInstance();
                    BlockEntityRenderDispatcher dispatcher = minecraft.getBlockEntityRenderDispatcher();
                    EntityModelSet modelSet = minecraft.getEntityModels();
                    this.renderer = new GunSmithTableItemRenderer(dispatcher, modelSet);
                }
                this.renderer.renderByItem(argument.stack(), argument.displayContext(), context.poseStack(), context.bufferSource(), context.lightCoords(), context.overlayCoords());
            }

            @Override
            Object getGuiIdentity(ItemStack stack) {
                if (stack.getItem() instanceof IBlock block) {
                    return block.getBlockId(stack);
                }
                return this;
            }
        },
        FLAT_ITEM_ICON("flat_item_icon") {
            @Override
            void render(RenderArgument argument, PoseContext context) {
                Identifier texture = getGuiTexture(argument.stack());
                if (texture == null) {
                    texture = MissingTextureAtlasSprite.getLocation();
                }
                renderFlatIcon(context.poseStack(), context.bufferSource().getBuffer(RenderTypes.itemTranslucent(texture)), context.lightCoords(), context.overlayCoords());
            }

            @Override
            Identifier getGuiTexture(ItemStack stack) {
                Identifier itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
                if (itemId == null) {
                    return MissingTextureAtlasSprite.getLocation();
                }
                return itemId.withPath(path -> "textures/item/" + path + ".png");
            }
        };

        private final String key;
        private final LegacySpecialRenderer specialRenderer;

        Kind(String key) {
            this.key = key;
            this.specialRenderer = new LegacySpecialRenderer(this);
        }

        abstract void render(RenderArgument argument, PoseContext context);

        @Nullable
        Identifier getGuiTexture(ItemStack stack) {
            return null;
        }

        @Nullable
        Object getGuiIdentity(ItemStack stack) {
            return getGuiTexture(stack);
        }

        @Nullable
        static Kind byKey(String key) {
            for (Kind value : values()) {
                if (value.key.equals(key)) {
                    return value;
                }
            }
            return null;
        }
    }

    private record PoseContext(
            PoseStack poseStack,
            MultiBufferSource.BufferSource bufferSource,
            int lightCoords,
            int overlayCoords
    ) {
    }

    private static void renderFlatIcon(PoseStack poseStack, VertexConsumer buffer, int lightCoords, int overlayCoords) {
        poseStack.pushPose();
        poseStack.translate(0.5, 1.5, 0.5);
        poseStack.mulPose(Axis.ZN.rotationDegrees(180));
        GUI_SLOT_MODEL.renderToBuffer(poseStack, buffer, lightCoords, overlayCoords, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }

    private static final class LegacySpecialRenderer implements SpecialModelRenderer<RenderArgument> {
        private final Kind kind;

        private LegacySpecialRenderer(Kind kind) {
            this.kind = kind;
        }

        @Override
        public void submit(
                @Nullable RenderArgument argument,
                PoseStack poseStack,
                SubmitNodeCollector submitNodeCollector,
                int lightCoords,
                int overlayCoords,
                boolean hasFoil,
                int outlineColor
        ) {
            if (argument == null) {
                return;
            }
            if (argument.displayContext() == ItemDisplayContext.GUI
                    && this.kind == Kind.GUN_SMITH_TABLE
                    && submitGunSmithTableGuiModel(argument, poseStack, submitNodeCollector, lightCoords, overlayCoords)) {
                return;
            }
            Identifier guiTexture = this.kind.getGuiTexture(argument.stack());
            if (argument.displayContext() == ItemDisplayContext.GUI && guiTexture != null) {
                submitGuiSlotModel(poseStack, submitNodeCollector, guiTexture, lightCoords, overlayCoords);
                return;
            }
            MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
            this.kind.render(argument, new PoseContext(poseStack, bufferSource, lightCoords, overlayCoords));
            bufferSource.endBatch();
        }

        private static void submitGuiSlotModel(
                PoseStack poseStack,
                SubmitNodeCollector submitNodeCollector,
                Identifier texture,
                int lightCoords,
                int overlayCoords
        ) {
            poseStack.pushPose();
            poseStack.translate(0.5, 1.5, 0.5);
            poseStack.mulPose(Axis.ZN.rotationDegrees(180));
            RenderType renderType = RenderTypes.itemTranslucent(texture);
            submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> renderGuiSlotModel(pose, buffer, lightCoords, overlayCoords));
            poseStack.popPose();
        }

        private static void renderGuiSlotModel(PoseStack.Pose pose, VertexConsumer buffer, int lightCoords, int overlayCoords) {
            PoseStack submittedPose = new PoseStack();
            submittedPose.last().set(pose);
            GUI_SLOT_MODEL.renderToBuffer(submittedPose, buffer, lightCoords, overlayCoords, 1.0F, 1.0F, 1.0F, 1.0F);
        }

        private static boolean submitGunSmithTableGuiModel(
                RenderArgument argument,
                PoseStack poseStack,
                SubmitNodeCollector submitNodeCollector,
                int lightCoords,
                int overlayCoords
        ) {
            return GunSmithTableRenderer.getIndex(argument.stack()).map(index -> {
                if (index.getModel() == null || index.getTexture() == null) {
                    return false;
                }
                poseStack.pushPose();
                applyBlockGuiRotationAndScale(poseStack, index);
                poseStack.translate(0.65, 1, 0.5);
                poseStack.mulPose(Axis.ZN.rotationDegrees(180));
                RenderType renderType = RenderTypes.entityTranslucentCullItemTarget(index.getTexture());
                submitNodeCollector.submitCustomGeometry(poseStack, renderType,
                        (pose, buffer) -> renderGunSmithTableGuiModel(pose, buffer, index, lightCoords, overlayCoords));
                poseStack.popPose();
                return true;
            }).orElse(false);
        }

        private static void applyBlockGuiRotationAndScale(PoseStack poseStack, ClientBlockIndex index) {
            ItemTransforms transforms = index.getTransforms();
            ItemTransform transform = transforms == null ? ItemTransform.NO_TRANSFORM : transforms.getTransform(ItemDisplayContext.GUI);
            Vector3fc rotation = transform.rotation();
            Vector3fc scale = transform.scale();
            poseStack.translate(0.5F, 0.5F, 0.5F);
            poseStack.mulPose(new Quaternionf().rotationXYZ(
                    rotation.x() * (float) (Math.PI / 180.0),
                    rotation.y() * (float) (Math.PI / 180.0),
                    rotation.z() * (float) (Math.PI / 180.0)));
            poseStack.scale(scale.x(), scale.y(), scale.z());
            poseStack.translate(-0.5F, -0.5F, -0.5F);
        }

        private static void renderGunSmithTableGuiModel(
                PoseStack.Pose pose,
                VertexConsumer buffer,
                ClientBlockIndex index,
                int lightCoords,
                int overlayCoords
        ) {
            PoseStack submittedPose = new PoseStack();
            submittedPose.last().set(pose);
            int guiLight = LightCoordsUtil.pack(15, 15);
            for (BedrockPart part : index.getModel().getShouldRender()) {
                part.render(submittedPose, ItemDisplayContext.GUI, buffer, guiLight, overlayCoords);
            }
        }

        @Override
        public void getExtents(Consumer<Vector3fc> output) {
            output.accept(new Vector3f(0, 0, 0));
            output.accept(new Vector3f(1, 1, 1));
        }

        @Override
        public @Nullable RenderArgument extractArgument(ItemStack stack) {
            return new RenderArgument(stack.copy(), ItemDisplayContext.NONE);
        }
    }

    public record Unbaked(String renderer) implements ItemModel.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
                instance -> instance.group(
                        Codec.STRING.fieldOf("renderer").forGetter(Unbaked::renderer)
                ).apply(instance, Unbaked::new)
        );

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            for (Identifier model : AmmoBoxFallbackItemModel.MODELS) {
                resolver.markDependency(model);
            }
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            Kind kind = Kind.byKey(this.renderer);
            if (kind == null) {
                return context.missingItemModel(transformation);
            }
            return new LegacyRendererItemModel(kind, transformation);
        }

        @Override
        public MapCodec<? extends ItemModel.Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
