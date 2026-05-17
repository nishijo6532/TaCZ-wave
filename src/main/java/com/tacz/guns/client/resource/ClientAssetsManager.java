package com.tacz.guns.client.resource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.tacz.guns.api.client.animation.gltf.AnimationStructure;
import com.tacz.guns.api.vmlib.LuaAnimationConstant;
import com.tacz.guns.api.vmlib.LuaGunAnimationConstant;
import com.tacz.guns.api.vmlib.LuaLibrary;
import com.tacz.guns.client.resource.manager.DisplayManager;
import com.tacz.guns.client.resource.manager.GltfManager;
import com.tacz.guns.client.resource.manager.PackInfoManager;
import com.tacz.guns.client.resource.manager.SoundAssetsManager;
import com.tacz.guns.client.resource.pojo.CommonTransformObject;
import com.tacz.guns.client.resource.pojo.PackInfo;
import com.tacz.guns.client.resource.pojo.animation.bedrock.AnimationKeyframes;
import com.tacz.guns.client.resource.pojo.animation.bedrock.BedrockAnimationFile;
import com.tacz.guns.client.resource.pojo.animation.bedrock.SoundEffectKeyframes;
import com.tacz.guns.client.resource.pojo.display.ammo.AmmoDisplay;
import com.tacz.guns.client.resource.pojo.display.attachment.AttachmentDisplay;
import com.tacz.guns.client.resource.pojo.display.block.BlockDisplay;
import com.tacz.guns.client.resource.pojo.display.gun.GunDisplay;
import com.tacz.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tacz.guns.client.resource.pojo.model.CubesItem;
import com.tacz.guns.client.resource.serialize.AnimationKeyframesSerializer;
import com.tacz.guns.client.resource.serialize.ItemStackSerializer;
import com.tacz.guns.client.resource.serialize.SoundEffectKeyframesSerializer;
import com.tacz.guns.client.resource.serialize.Vector3fSerializer;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.manager.JsonDataManager;
import com.tacz.guns.resource.manager.ScriptManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.luaj.vm2.LuaTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 陞ｳ・｢隰鯉ｽｷ驕ｶ・ｯ隘阪・・ｺ蜊・ｽｮ・｡騾・・蜍｣<br/>
 * 隰・隴帷判譽ｯ陋ｹ繝ｻ・ｵ繝ｻ・ｺ蜊・ｽｼ轣假ｽｭ莨懈Β雎・ｽ､
 */
public enum ClientAssetsManager {
    INSTANCE;
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Identifier.class, new IdentifierAdapter())
            .registerTypeAdapter(ItemTransform.class, new ItemTransformAdapter())
            .registerTypeAdapter(ItemTransforms.class, new ItemTransformsAdapter())
            .registerTypeAdapter(CubesItem.class, new CubesItem.Deserializer())
            .registerTypeAdapter(Vector3f.class, new Vector3fSerializer())
            .registerTypeAdapter(CommonTransformObject.class, new CommonTransformObject.Serializer())
            .registerTypeAdapter(ItemStack.class, new ItemStackSerializer())
            .registerTypeAdapter(AnimationKeyframes.class, new AnimationKeyframesSerializer())
            .registerTypeAdapter(SoundEffectKeyframes.class, new SoundEffectKeyframesSerializer())
            .create();

    // 隴ｫ・ｪ隴ｴ・ｰ陞ｻ諷包ｽ､・ｺ隰ｨ・ｰ隰撰ｽｮ
    private JsonDataManager<GunDisplay> gunDisplay;
    // 陟托ｽｹ髣包ｽｯ陞ｻ諷包ｽ､・ｺ隰ｨ・ｰ隰撰ｽｮ
    private JsonDataManager<AmmoDisplay> ammoDisplay;
    // 鬩溷ｺ・ｻ・ｶ陞ｻ諷包ｽ､・ｺ隰ｨ・ｰ隰撰ｽｮ
    private JsonDataManager<AttachmentDisplay> attachmentDisplay;
    // 隴・ｽｹ陜ｮ諤懶ｽｱ諷包ｽ､・ｺ隰ｨ・ｰ隰撰ｽｮ
    private JsonDataManager<BlockDisplay> blockDisplay;
    // 陷ｴ貅ｷ・ｧ蜿･貂戊浣・ｩ霑壼沺・ｨ・｡陜吶・
    private JsonDataManager<BedrockModelPOJO> bedrockModel;
    // 陜難ｽｺ陝ｯ・ｩ霑壼沺・ｨ・｡陜吝唱蜍倬包ｽｻ
    private JsonDataManager<BedrockAnimationFile> bedrockAnimation;
    // gltf 陷会ｽｨ騾包ｽｻ
    private GltfManager gltfAnimation;
    // 陞ｳ・｢隰鯉ｽｷ驕ｶ・ｯ髢ｼ螢ｽ謔ｽ
    private final List<LuaLibrary> libList = List.of(new LuaAnimationConstant(), new LuaGunAnimationConstant());
    private ScriptManager scriptManager;
    // 鬮ｻ・ｳ隰ｨ繝ｻ
    private SoundAssetsManager soundAssetsManager;
    // 隴ｫ・ｪ陋ｹ繝ｻ繝ｻ隰ｨ・ｰ隰撰ｽｮ
    private PackInfoManager packInfo;

    private List<PreparableReloadListener> listeners;

    public void reloadAndRegister(Consumer<PreparableReloadListener> register) {
        if (listeners == null) {
            listeners = new ArrayList<>();
            gunDisplay = register(new DisplayManager<>(GunDisplay.class, GSON, "display/guns", "GunDisplayLoader"));
            ammoDisplay = register(new DisplayManager<>(AmmoDisplay.class, GSON, "display/ammo", "AmmoDisplayLoader"));
            attachmentDisplay = register(new DisplayManager<>(AttachmentDisplay.class, GSON, "display/attachments", "AttachmentDisplayLoader"));
            blockDisplay = register(new DisplayManager<>(BlockDisplay.class, GSON, "display/blocks", "BlockDisplayLoader"));
            bedrockModel = register(new JsonDataManager<>(BedrockModelPOJO.class, GSON, "geo_models", "BedrockModelLoader"));
            bedrockAnimation = register(new JsonDataManager<>(BedrockAnimationFile.class, GSON, new FileToIdConverter("animations", ".animation.json"), "BedrockAnimationLoader"));
            gltfAnimation = register(new GltfManager());
            scriptManager = register(new ScriptManager(new FileToIdConverter("scripts", ".lua"), libList));
            soundAssetsManager = register(new SoundAssetsManager());
            packInfo = register(new PackInfoManager());
        }
        listeners.forEach(register);
    }

    private <T extends PreparableReloadListener> T register(T listener) {
        listeners.add(listener);
        return listener;
    }

    @Nullable
    public GunDisplay getGunDisplay(Identifier id) {
        return gunDisplay.getData(id);
    }

    public Set<Map.Entry<Identifier, GunDisplay>> getGunDisplays() {
        return gunDisplay.getAllData().entrySet();
    }

    @Nullable
    public AttachmentDisplay getAttachmentDisplay(Identifier id) {
        return attachmentDisplay.getData(id);
    }

    @Nullable
    public AmmoDisplay getAmmoDisplay(Identifier id) {
        return ammoDisplay.getData(id);
    }

    @Nullable
    public BlockDisplay getBlockDisplay(Identifier id) {
        return blockDisplay.getData(id);
    }

    @Nullable
    public BedrockModelPOJO getBedrockModelPOJO(Identifier id) {
        return bedrockModel.getData(id);
    }

    @Nullable
    public BedrockAnimationFile getBedrockAnimations(Identifier id) {
        return bedrockAnimation.getData(id);
    }

    @Nullable
    public LuaTable getScript(Identifier id) {
        return scriptManager.getScript(id);
    }

    @Nullable
    public AnimationStructure getGltfAnimation(Identifier id) {
        return gltfAnimation.getGltfAnimation(id);
    }

    @Nullable
    public SoundAssetsManager.SoundData getSoundBuffers(Identifier id) {
        return soundAssetsManager.getData(id);
    }

    @Nullable
    public PackInfo getPackInfo(String namespace) {
        return packInfo.getData(namespace);
    }

    @Nullable
    public PackInfo getPackInfo(@Nullable Identifier namespace) {
        if (namespace == null) {
            return null;
        }
        return packInfo.getData(namespace.getNamespace());
    }

    public static void reloadAllPack() {
        try {
            Minecraft.getInstance().reloadResourcePacks().get();
            // 陞ｯ繧域｣｡髴第ｨ顔｣∬崕・ｰ陞溷｣ｻ・ｺ・ｺ雋ゑｽｸ隰後・
            if (ServerLifecycleHooks.getCurrentServer() == null) {
                // 鬩･讎奇ｽｻ・ｺ驍擾ｽ｢陟代・
                ClientIndexManager.reload();
            } else {
                // 騾ｶ・ｴ隰暦ｽ･陋ｻ・ｷ隴・ｽｰdata
                CommonAssetsManager.reloadAllPack();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final class IdentifierAdapter implements JsonSerializer<Identifier>, JsonDeserializer<Identifier> {
        @Override
        public JsonElement serialize(Identifier src, java.lang.reflect.Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }

        @Override
        public Identifier deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json == null || json.isJsonNull()) {
                return null;
            }
            if (json.isJsonPrimitive()) {
                Identifier id = Identifier.tryParse(json.getAsString());
                if (id == null) {
                    throw new JsonParseException("Invalid identifier: " + json);
                }
                return id;
            }
            if (json.isJsonObject()) {
                JsonObject object = json.getAsJsonObject();
                if (object.has("id")) {
                    Identifier id = Identifier.tryParse(object.get("id").getAsString());
                    if (id == null) {
                        throw new JsonParseException("Invalid identifier: " + json);
                    }
                    return id;
                }
                if (object.has("namespace") && object.has("path")) {
                    String value = object.get("namespace").getAsString() + ":" + object.get("path").getAsString();
                    Identifier id = Identifier.tryParse(value);
                    if (id == null) {
                        throw new JsonParseException("Invalid identifier: " + json);
                    }
                    return id;
                }
            }
            throw new JsonParseException("Invalid identifier: " + json);
        }
    }

    private static final class ItemTransformAdapter implements JsonDeserializer<ItemTransform> {
        @Override
        public ItemTransform deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json == null || json.isJsonNull()) {
                return ItemTransform.NO_TRANSFORM;
            }
            JsonObject object = json.getAsJsonObject();
            Vector3f rotation = parseOrDefault(object, "rotation", new Vector3f(0, 0, 0), context);
            Vector3f translation = parseOrDefault(object, "translation", new Vector3f(0, 0, 0), context);
            Vector3f scale = parseOrDefault(object, "scale", new Vector3f(1, 1, 1), context);
            return new ItemTransform(rotation, translation, scale);
        }

        private static Vector3f parseOrDefault(JsonObject object, String key, Vector3f fallback, JsonDeserializationContext context) {
            JsonElement element = object.get(key);
            if (element == null || element.isJsonNull()) {
                return fallback;
            }
            return context.deserialize(element, Vector3f.class);
        }
    }

    private static final class ItemTransformsAdapter implements JsonDeserializer<ItemTransforms> {
        @Override
        public ItemTransforms deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json == null || json.isJsonNull()) {
                return ItemTransforms.NO_TRANSFORMS;
            }
            JsonObject object = json.getAsJsonObject();
            ItemTransform thirdLeft = parseOrDefault(object, "thirdperson_lefthand", context);
            ItemTransform thirdRight = parseOrDefault(object, "thirdperson_righthand", context);
            ItemTransform firstLeft = parseOrDefault(object, "firstperson_lefthand", context);
            ItemTransform firstRight = parseOrDefault(object, "firstperson_righthand", context);
            ItemTransform head = parseOrDefault(object, "head", context);
            ItemTransform gui = parseOrDefault(object, "gui", context);
            ItemTransform ground = parseOrDefault(object, "ground", context);
            ItemTransform fixed = parseOrDefault(object, "fixed", context);
            ItemTransform fixedFromBottom = parseOrDefault(object, "fixed_from_bottom", context);
            return new ItemTransforms(thirdLeft, thirdRight, firstLeft, firstRight, head, gui, ground, fixed, fixedFromBottom);
        }

        private static ItemTransform parseOrDefault(JsonObject object, String key, JsonDeserializationContext context) {
            JsonElement element = object.get(key);
            if (element == null || element.isJsonNull()) {
                return ItemTransform.NO_TRANSFORM;
            }
            return context.deserialize(element, ItemTransform.class);
        }
    }
}

