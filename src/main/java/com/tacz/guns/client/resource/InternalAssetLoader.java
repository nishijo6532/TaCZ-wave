package com.tacz.guns.client.resource;

import com.google.common.collect.Maps;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.client.animation.Animations;
import com.tacz.guns.api.client.animation.ObjectAnimation;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.resource.pojo.animation.bedrock.BedrockAnimationFile;
import com.tacz.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tacz.guns.client.resource.pojo.model.BedrockVersion;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
// 霑吩ｸｪ逅・ｮｺ荳雁ｷｲ扈丈ｸ埼怙隕∽ｺ・
public class InternalAssetLoader {
    // 譖ｳ蜈牙ｼｹ讓｡蝙・
    public static final Identifier DEFAULT_BULLET_TEXTURE = com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "textures/entity/basic_bullet.png");
    public static final Identifier DEFAULT_BULLET_MODEL = com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "models/bedrock/basic_bullet.json");
    // 蟆・・譬・攜霓ｦ
    public static final Identifier TARGET_MINECART_MODEL_LOCATION = com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "models/bedrock/target_minecart.json");
    public static final Identifier TARGET_MINECART_TEXTURE_LOCATION = com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "textures/entity/target_minecart.png");
    public static final Identifier ENTITY_EMPTY_TEXTURE = com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "textures/entity/empty.png");
    // 蟆・・譬・攜
    public static final Identifier TARGET_MODEL_LOCATION = com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "models/bedrock/target.json");
    public static final Identifier TARGET_TEXTURE_LOCATION = com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "textures/block/target.png");
    // 髮募ワ
    public static final Identifier STATUE_MODEL_LOCATION = com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "models/bedrock/statue.json");
    public static final Identifier STATUE_TEXTURE_LOCATION = com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "textures/block/statue.png");
    // 謾ｹ陬・床
    public static final Identifier SMITH_TABLE_MODEL_LOCATION = com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "models/bedrock/gun_smith_table.json");
    public static final Identifier SMITH_TABLE_TEXTURE_LOCATION = com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "textures/block/gun_smith_table.png");
    // 鮟倩ｮ､蜉ｨ逕ｻ
    private static final Identifier DEFAULT_PISTOL_ANIMATIONS_LOC = com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "animations/pistol_default.animation.json");
    private static final Identifier DEFAULT_RIFLE_ANIMATIONS_LOC = com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "animations/rifle_default.animation.json");
    // 蜀・Κ襍・ｺ千ｼ灘ｭ・
    private static final Map<Identifier, BedrockModel> BEDROCK_MODELS = Maps.newHashMap();
    private static List<ObjectAnimation> defaultPistolAnimations;
    private static List<ObjectAnimation> defaultRifleAnimations;

    public static void onResourceReload() {
        // 蜉霓ｽ鮟倩ｮ､蜉ｨ逕ｻ譁・ｻｶ
        BedrockAnimationFile pistolAnimationFile = loadAnimations(DEFAULT_PISTOL_ANIMATIONS_LOC);
        BedrockAnimationFile rifleAnimationFile = loadAnimations(DEFAULT_RIFLE_ANIMATIONS_LOC);
        defaultPistolAnimations = Animations.createAnimationFromBedrock(pistolAnimationFile);
        defaultRifleAnimations = Animations.createAnimationFromBedrock(rifleAnimationFile);

        // 蜉霓ｽ莉｣遐∫峩謗･隹・畑逧・渕蟯ｩ迚域ｨ｡蝙・
        BEDROCK_MODELS.clear();
        loadBedrockModels(InternalAssetLoader.SMITH_TABLE_MODEL_LOCATION);
        loadBedrockModels(InternalAssetLoader.TARGET_MODEL_LOCATION);
        loadBedrockModels(InternalAssetLoader.TARGET_MINECART_MODEL_LOCATION);
        loadBedrockModels(InternalAssetLoader.DEFAULT_BULLET_MODEL);
        loadBedrockModels(InternalAssetLoader.STATUE_MODEL_LOCATION);
    }

    private static BedrockAnimationFile loadAnimations(Identifier Identifier) {
        try (InputStream inputStream = Minecraft.getInstance().getResourceManager().open(Identifier)) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            JsonObject json = JsonParser.parseReader(bufferedReader).getAsJsonObject();
            return ClientAssetsManager.GSON.fromJson(json, BedrockAnimationFile.class);
        } catch (IOException | JsonSyntaxException | JsonIOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void loadBedrockModels(Identifier location) {
        try (InputStream stream = Minecraft.getInstance().getResourceManager().open(location)) {
            BedrockModelPOJO pojo = ClientAssetsManager.GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), BedrockModelPOJO.class);
            BEDROCK_MODELS.put(location, new BedrockModel(pojo, BedrockVersion.NEW));
        } catch (IOException | JsonSyntaxException | JsonIOException e) {
            e.fillInStackTrace();
        }
    }

    public static List<ObjectAnimation> getDefaultPistolAnimations() {
        return defaultPistolAnimations;
    }

    public static List<ObjectAnimation> getDefaultRifleAnimations() {
        return defaultRifleAnimations;
    }

    public static Optional<BedrockModel> getBedrockModel(Identifier location) {
        return Optional.ofNullable(BEDROCK_MODELS.get(location));
    }
}

