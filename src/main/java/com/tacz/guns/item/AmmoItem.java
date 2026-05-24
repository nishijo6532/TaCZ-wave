package com.tacz.guns.item;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.api.item.nbt.AmmoItemDataAccessor;
import com.tacz.guns.client.renderer.item.AmmoItemRenderer;
import com.tacz.guns.client.resource.ClientAssetsManager;
import com.tacz.guns.client.resource.index.ClientAmmoIndex;
import com.tacz.guns.client.resource.pojo.PackInfo;
import com.tacz.guns.resource.index.CommonAmmoIndex;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import com.tacz.guns.client.renderer.compat.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class AmmoItem extends Item implements AmmoItemDataAccessor {
    public AmmoItem() {
        this(new Properties().stacksTo(1));
    }

    public AmmoItem(Properties properties) {
        super(properties);
    }

    public int getMaxStackSize(ItemStack stack) {
        if (stack.getItem() instanceof IAmmo iAmmo) {
            return TimelessAPI.getCommonAmmoIndex(iAmmo.getAmmoId(stack))
                    .map(CommonAmmoIndex::getStackSize).orElse(1);
        }
        return 1;
    }

    @Override
    @Nonnull
    public Component getName(@Nonnull ItemStack stack) {
        Identifier ammoId = this.getAmmoId(stack);
        Optional<ClientAmmoIndex> ammoIndex = TimelessAPI.getClientAmmoIndex(ammoId);
        if (ammoIndex.isPresent()) {
            return Component.translatable(ammoIndex.get().getName());
        }
        return super.getName(stack);
    }

    public static NonNullList<ItemStack> fillItemCategory() {
        NonNullList<ItemStack> stacks = NonNullList.create();
        TimelessAPI.getAllCommonAmmoIndex().stream().sorted(idNameSort()).forEach(entry -> {
            ItemStack itemStack = AmmoItemBuilder.create().setId(entry.getKey()).build();
            stacks.add(itemStack);
        });
        return stacks;
    }

    private static Comparator<Map.Entry<Identifier, CommonAmmoIndex>> idNameSort() {
        return Comparator.comparingInt(entry -> entry.getValue().getSort());
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            AmmoItemRenderer renderer;
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    Minecraft minecraft = Minecraft.getInstance();
                    renderer = new AmmoItemRenderer(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
                }
                return renderer;
            }
        });
    }

    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> components, TooltipFlag isAdvanced) {
        Identifier ammoId = this.getAmmoId(stack);
        TimelessAPI.getClientAmmoIndex(ammoId).ifPresent(index -> {
            String tooltipKey = index.getTooltipKey();
            if (tooltipKey != null) {
                components.add(Component.translatable(tooltipKey).withStyle(ChatFormatting.GRAY));
            }
        });

        PackInfo packInfoObject = ClientAssetsManager.INSTANCE.getPackInfo(ammoId);
        if (packInfoObject != null) {
            MutableComponent component = Component.translatable(packInfoObject.getName()).withStyle(ChatFormatting.BLUE).withStyle(ChatFormatting.ITALIC);
            components.add(component);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> consumer, TooltipFlag isAdvanced) {
        List<Component> components = new ArrayList<>();
        appendHoverText(stack, null, components, isAdvanced);
        components.forEach(consumer);
    }
}

