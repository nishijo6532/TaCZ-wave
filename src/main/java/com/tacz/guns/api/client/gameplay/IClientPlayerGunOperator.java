package com.tacz.guns.api.client.gameplay;

import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.client.gameplay.*;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * 客户端枪械操纵者
 * 目前仅用于 LocalPlayer
 */
public interface IClientPlayerGunOperator {
    /**
     * LocalPlayer 通过 Mixin 的方式实现了这个接口
     */
    static IClientPlayerGunOperator fromLocalPlayer(LocalPlayer player) {
        if (player instanceof IClientPlayerGunOperator operator) {
            return operator;
        }
        return FallbackOperator.get(player);
    }

    /**
     * 检查玩家能否开火，并执行客户端开火逻辑。
     *
     * @return 返回开火的结果
     */
    ShootResult shoot();

    /**
     * 执行客户端切枪逻辑。
     */
    void draw(ItemStack lastItem);

    /**
     * 客户端手动换弹
     */
    void bolt();

    /**
     * 客户端换弹
     */
    void reload();

    /**
     * 客户端检视
     */
    void inspect();

    /**
     * 客户端切换开火模式
     */
    void fireSelect();

    /**
     * 客户端瞄准
     */
    void aim(boolean isAim);

    /**
     * 客户端爬行
     */
    void crawl(boolean isCrawl);

    /**
     * 客户端近战（刺刀）
     */
    void melee();

    /**
     * 客户端是否处于瞄准状态
     */
    boolean isAim();

    /**
     * 是否爬行
     */
    boolean isCrawl();

    LocalPlayerDataHolder getDataHolder();

    /**
     * 客户端瞄准进度
     *
     * @return 0-1，1 代表开镜进度到 100%
     */
    float getClientAimingProgress(float partialTicks);

    /**
     * 客户端射击冷却时间
     */
    long getClientShootCoolDown();

    boolean isReadyToDraw();

    void resetDraw();

    final class FallbackOperator implements IClientPlayerGunOperator {
        private static final Map<LocalPlayer, FallbackOperator> INSTANCES = new WeakHashMap<>();
        private final LocalPlayerDataHolder data;
        private final LocalPlayerAim aim;
        private final LocalPlayerCrawl crawl;
        private final LocalPlayerBolt bolt;
        private final LocalPlayerDraw draw;
        private final LocalPlayerFireSelect fireSelect;
        private final LocalPlayerMelee melee;
        private final LocalPlayerInspect inspect;
        private final LocalPlayerReload reload;
        private final LocalPlayerShoot shoot;

        private FallbackOperator(LocalPlayer player) {
            this.data = new LocalPlayerDataHolder(player);
            this.aim = new LocalPlayerAim(data, player);
            this.crawl = new LocalPlayerCrawl(player);
            this.bolt = new LocalPlayerBolt(data, player);
            this.draw = new LocalPlayerDraw(data, player);
            this.fireSelect = new LocalPlayerFireSelect(data, player);
            this.melee = new LocalPlayerMelee(data, player);
            this.inspect = new LocalPlayerInspect(data, player);
            this.reload = new LocalPlayerReload(data, player);
            this.shoot = new LocalPlayerShoot(data, player);
        }

        private static IClientPlayerGunOperator get(LocalPlayer player) {
            synchronized (INSTANCES) {
                return INSTANCES.computeIfAbsent(player, FallbackOperator::new);
            }
        }

        @Override
        public ShootResult shoot() {
            reload.cancelReload();
            return shoot.shoot();
        }

        @Override
        public void draw(ItemStack lastItem) {
            draw.draw(lastItem);
        }

        @Override
        public void bolt() {
            bolt.bolt();
        }

        @Override
        public void reload() {
            reload.reload();
        }

        @Override
        public void inspect() {
            inspect.inspect();
        }

        @Override
        public void fireSelect() {
            fireSelect.fireSelect();
        }

        @Override
        public void aim(boolean isAim) {
            aim.aim(isAim);
        }

        @Override
        public void crawl(boolean isCrawl) {
            crawl.crawl(isCrawl);
        }

        @Override
        public void melee() {
            melee.melee();
        }

        @Override
        public boolean isAim() {
            return aim.isAim();
        }

        @Override
        public boolean isCrawl() {
            return crawl.isCrawling();
        }

        @Override
        public LocalPlayerDataHolder getDataHolder() {
            return data;
        }

        @Override
        public float getClientAimingProgress(float partialTicks) {
            return aim.getClientAimingProgress(partialTicks);
        }

        @Override
        public long getClientShootCoolDown() {
            return shoot.getClientShootCoolDown();
        }

        @Override
        public boolean isReadyToDraw() {
            return draw.readyToDraw;
        }

        @Override
        public void resetDraw() {
            draw.readyToDraw = false;
        }
    }
}
