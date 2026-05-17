package com.tacz.guns.sound;

import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ServerMessageSound;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.network.PacketDistributor;

public class SoundManager {
    /**
     * 蟆・・髻ｳ謨茨ｼ瑚・蟾ｱ閭ｽ蜷ｬ隗・
     */
    public static String SHOOT_SOUND = "shoot";
    /**
     * 蜈ｶ莉也自螳ｶ蜷ｬ蛻ｰ逧・棯螢ｰ
     */
    public static String SHOOT_3P_SOUND = "shoot_3p";
    /**
     * 豸磯浹蝎ｨ髻ｳ謨・
     */
    public static String SILENCE_SOUND = "silence";
    /**
     * 蜈ｶ莉也自螳ｶ蜷ｬ蛻ｰ逧・ｶ磯浹蝎ｨ譫ｪ螢ｰ
     */
    public static String SILENCE_3P_SOUND = "silence_3p";
    /**
     * 霑第・蛻ｺ蛻髻ｳ謨・
     */
    public static String MELEE_BAYONET = "melee_bayonet";
    /**
     * 霑第・謗ｨ莠ｺ髻ｳ謨・
     */
    public static String MELEE_PUSH = "melee_push";
    /**
     * 霑第・譫ｪ諡也ｸ莠ｺ髻ｳ謨・
     */
    public static String MELEE_STOCK = "melee_stock";
    /**
     * 豐｡譛牙ｭ仙ｼｹ譌ｶ・檎ｩｺ蜃ｻ逧・｣ｰ髻ｳ
     */
    public static String DRY_FIRE_SOUND = "dry_fire";
    /**
     * 遨ｺ莉捺困蠑ｹ螢ｰ髻ｳ
     */
    public static String RELOAD_EMPTY_SOUND = "reload_empty";
    /**
     * 謌俶惘謐｢蠑ｹ螢ｰ髻ｳ
     */
    public static String RELOAD_TACTICAL_SOUND = "reload_tactical";
    /**
     * 遨ｺ莉捺｣隗・｣ｰ髻ｳ
     */
    public static String INSPECT_EMPTY_SOUND = "inspect_empty";
    /**
     * 譎ｮ騾壽｣隗・｣ｰ髻ｳ
     */
    public static String INSPECT_SOUND = "inspect";
    /**
     * 蛻・棯蛻・・螢ｰ髻ｳ
     */
    public static String DRAW_SOUND = "draw";
    /**
     * 蛻・棯蛻・・逧・｣ｰ髻ｳ
     */
    public static String PUT_AWAY_SOUND = "put_away";
    /**
     * 諡画灘｣ｰ髻ｳ
     */
    public static String BOLT_SOUND = "bolt";
    /**
     * 蛻・困蠑蜈ｳ讓｡蠑冗噪螢ｰ髻ｳ
     */
    public static String FIRE_SELECT = "fire_select";
    /**
     * 辷・､ｴ蜃ｻ荳ｭ螢ｰ髻ｳ
     */
    public static String HEAD_HIT_SOUND = "head_hit";
    /**
     * 譎ｮ騾壼・荳ｭ螢ｰ髻ｳ
     */
    public static String FLESH_HIT_SOUND = "flesh_hit";
    /**
     * 蜃ｻ譚逧・｣ｰ髻ｳ
     */
    public static String KILL_SOUND = "kill";
    /**
     * 蜊ｸ霓ｽ驟堺ｻｶ逧・｣ｰ髻ｳ・檎畑莠朱・莉ｶ逧・
     */
    public static String UNINSTALL_SOUND = "uninstall";
    /**
     * 陬・ｽｽ驟堺ｻｶ逧・｣ｰ髻ｳ・檎畑莠朱・莉ｶ逧・
     */
    public static String INSTALL_SOUND = "install";

//    public static void sendSoundToNearby(LivingEntity sourceEntity, int distance, Identifier gunId, String soundName, float volume, float pitch) {
//        sendSoundToNearby(sourceEntity, distance, gunId, DefaultAssets.DEFAULT_GUN_DISPLAY_ID, soundName, volume, pitch);
//    }

    public static void sendSoundToNearby(LivingEntity sourceEntity, int distance, Identifier gunId, Identifier gunDisplayId, String soundName, float volume, float pitch) {
        if (sourceEntity.level() instanceof ServerLevel serverLevel) {
            BlockPos pos = sourceEntity.blockPosition();
            ServerMessageSound soundMessage = new ServerMessageSound(sourceEntity.getId(), gunId, gunDisplayId, soundName, volume, pitch, distance);
            serverLevel.getChunkSource().chunkMap.getPlayers(new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4), false).stream()
                    .filter(p -> p.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < distance * distance)
                    .filter(p -> p.getId() != sourceEntity.getId())
                .forEach(p -> NetworkHandler.CHANNEL.send(soundMessage, PacketDistributor.PLAYER.with(p)));
        }
    }
}

