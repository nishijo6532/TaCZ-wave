package com.tacz.guns.init;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.tacz.guns.GunMod;
import com.tacz.guns.particles.BulletHoleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, GunMod.MOD_ID);

    public static final RegistryObject<ParticleType<BulletHoleOption>> BULLET_HOLE = PARTICLE_TYPES.register("bullet_hole", () -> new ModParticleType<>(false, BulletHoleOption.CODEC));

    @SuppressWarnings("deprecation")
    private static class ModParticleType<T extends ParticleOptions> extends ParticleType<T> {
        private final MapCodec<T> codec;
        private final StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec;

        public ModParticleType(boolean overrideLimiter, Codec<T> codec) {
            super(overrideLimiter);
            this.codec = MapCodec.assumeMapUnsafe(codec);
            this.streamCodec = ByteBufCodecs.fromCodecWithRegistries(codec);
        }

        @Override
        public @NotNull MapCodec<T> codec() {
            return this.codec;
        }

        @Override
        public @NotNull StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec() {
            return this.streamCodec;
        }
    }
}
