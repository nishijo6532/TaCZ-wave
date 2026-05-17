package com.tacz.guns.util;

import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.api.event.common.EntityKillByGunEvent;
import com.tacz.guns.config.common.AmmoConfig;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.event.ServerMessageGunHurt;
import com.tacz.guns.network.message.event.ServerMessageGunKill;
import com.tacz.guns.util.block.ProjectileExplosion;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.LogicalSide;

import java.util.Optional;

public class ExplodeUtil {
    public static void createExplosion(Entity owner, Entity exploder, float damage, float radius, boolean knockback, boolean destroy, Vec3 hitPos) {
        // 客户端不执行
        if (!(exploder.level() instanceof ServerLevel level)) {
            return;
        }
        // 依据配置文件读取方块破坏方式
        Explosion.BlockInteraction mode = Explosion.BlockInteraction.KEEP;
        if (destroy) {
            mode = Explosion.BlockInteraction.DESTROY;
        }
        // 创建爆炸
        ProjectileExplosion explosion = new ProjectileExplosion(level, owner, exploder, null, null, hitPos.x(), hitPos.y(), hitPos.z(), damage, radius, knockback, mode);
        // 监听 forge 事件
        if (ForgeEventFactory.onExplosionStart(level, explosion)) {
            return;
        }
        // 执行爆炸逻辑
        explosion.explode();
        notifyGunExplosionHits(exploder, owner, explosion);
        explosion.finalizeExplosion(true);
        int blockCount = explosion.getToBlow().size();
        if (mode == Explosion.BlockInteraction.KEEP) {
            explosion.clearToBlow();
        }
        // 客户端发包，发送爆炸相关信息
        level.players().stream().filter(player -> Mth.sqrt((float) player.distanceToSqr(hitPos)) < AmmoConfig.EXPLOSIVE_AMMO_VISIBLE_DISTANCE.get()).forEach(player -> {
            ClientboundExplodePacket packet = new ClientboundExplodePacket(hitPos, radius, blockCount, Optional.ofNullable(explosion.getHitPlayers().get(player)), ParticleTypes.EXPLOSION, SoundEvents.GENERIC_EXPLODE, WeightedList.of());
            player.connection.send(packet);
        });
    }

    private static void notifyGunExplosionHits(Entity exploder, Entity owner, ProjectileExplosion explosion) {
        if (!(exploder instanceof EntityKineticBullet bullet)) {
            return;
        }
        LivingEntity attacker = owner instanceof LivingEntity livingEntity ? livingEntity : null;
        int attackerId = attacker == null ? 0 : attacker.getId();
        for (ProjectileExplosion.LivingHit hit : explosion.getHitLivingEntities()) {
            LivingEntity entity = hit.entity();
            if (entity == attacker) {
                continue;
            }
            if (hit.killed()) {
                ForgeEventCompat.post(new EntityKillByGunEvent(bullet, entity, attacker, bullet.getGunId(), bullet.getGunDisplayId(), hit.damage(), null, false, 1.0F, LogicalSide.SERVER));
                NetworkHandler.sendToDimension(new ServerMessageGunKill(bullet.getId(), entity.getId(), attackerId, bullet.getGunId(), bullet.getGunDisplayId(), hit.damage(), false, 1.0F), entity);
            } else {
                ForgeEventCompat.post(new EntityHurtByGunEvent.Post(bullet, entity, attacker, bullet.getGunId(), bullet.getGunDisplayId(), hit.damage(), null, false, 1.0F, LogicalSide.SERVER));
                NetworkHandler.sendToDimension(new ServerMessageGunHurt(bullet.getId(), entity.getId(), attackerId, bullet.getGunId(), bullet.getGunDisplayId(), hit.damage(), false, 1.0F), entity);
            }
        }
    }
}
