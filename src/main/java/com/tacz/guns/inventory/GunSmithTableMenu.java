package com.tacz.guns.inventory;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.config.sync.SyncConfig;
import com.tacz.guns.crafting.GunSmithTableIngredient;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ServerMessageCraft;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.filter.RecipeFilter;
import com.tacz.guns.resource.index.CommonBlockIndex;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.List;

public class GunSmithTableMenu extends AbstractContainerMenu {
    public static final MenuType<GunSmithTableMenu> TYPE = IForgeMenuType.create((windowId, inv, data) -> {
        Identifier blockId = data.readIdentifier();
        return new GunSmithTableMenu(windowId, inv, blockId);
    });

    private final Identifier blockId;
    private final RecipeFilter filter;

    public GunSmithTableMenu(int id, Inventory inventory, @Nullable Identifier blockId) {
        super(TYPE, id);
        this.blockId = blockId;
        this.filter = TimelessAPI.getCommonBlockIndex(getBlockId()).map(CommonBlockIndex::getFilter).orElse(null);
    }

    @Nullable
    public Identifier getBlockId() {
        return blockId;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int pIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive();
    }

    @Nullable
    private GunSmithTableRecipe getRecipe(Identifier recipeId, RecipeManager recipeManager, Level level) {
        if (!DefaultAssets.DEFAULT_BLOCK_ID.equals(getBlockId()) || SyncConfig.ENABLE_TABLE_FILTER.get()) {
            if (filter != null && !filter.contains(recipeId)) {
                return null;
            }
        }

        GunSmithTableRecipe gunSmithTableRecipe = null;
        if (level.getServer() != null) {
            Map<Identifier, GunSmithTableRecipe> compatRecipes = CommonAssetsManager.loadGunSmithRecipesFromResourceManager(level.getServer().getResourceManager());
            gunSmithTableRecipe = compatRecipes.get(recipeId);
        }
        if (!isRecipeUsable(gunSmithTableRecipe)) {
            gunSmithTableRecipe = recipeManager.getRecipes().stream()
                .filter(recipeHolder -> recipeHolder.id().identifier().equals(recipeId))
                .map(recipeHolder -> recipeHolder.value())
                .filter(GunSmithTableRecipe.class::isInstance)
                .map(GunSmithTableRecipe.class::cast)
                .findFirst()
                .orElse(null);
            if (!isRecipeUsable(gunSmithTableRecipe) && level.getServer() != null) {
                gunSmithTableRecipe = CommonAssetsManager.loadGunSmithRecipesFromResourceManager(level.getServer().getResourceManager()).get(recipeId);
            }
        }
        if (!isRecipeUsable(gunSmithTableRecipe)) {
            return null;
        }
        gunSmithTableRecipe.init();
        GunSmithTableRecipe finalRecipe = gunSmithTableRecipe;

        boolean invalidTab = TimelessAPI.getCommonBlockIndex(getBlockId()).map(blockIndex ->
                blockIndex.getData().getTabs().stream().noneMatch(tab -> tab.id().equals(finalRecipe.getTab()))
        ).orElse(true);
        if (DefaultAssets.DEFAULT_BLOCK_ID.equals(getBlockId()) && !SyncConfig.ENABLE_TABLE_FILTER.get()) {
            invalidTab = false;
        }
        if (invalidTab) {
            return null;
        }
        return gunSmithTableRecipe;
    }

    private static boolean isRecipeUsable(@Nullable GunSmithTableRecipe recipe) {
        if (recipe == null || recipe.getOutput().isEmpty() || recipe.getInputs().isEmpty()) {
            return false;
        }
        return recipe.getInputs().stream()
                .allMatch(ingredient -> ingredient != null && ingredient.getIngredient() != null && !ingredient.getIngredient().isEmpty());
    }

    public void doCraft(Identifier recipeId, Player player) {
        var server = player.level().getServer();
        if (server == null) {
            return;
        }
        GunSmithTableRecipe recipe = getRecipe(recipeId, server.getRecipeManager(), player.level());
        if (recipe == null) {
            return;
        }

        player.getCapability(ForgeCapabilities.ITEM_HANDLER, null).ifPresent(handler -> {
            if (!player.isCreative()) {
                Int2IntArrayMap recordCount = new Int2IntArrayMap();
                List<GunSmithTableIngredient> ingredients = recipe.getInputs();

                for (GunSmithTableIngredient ingredient : ingredients) {
                    int count = 0;
                    for (int slotIndex = 0; slotIndex < handler.getSlots(); slotIndex++) {
                        ItemStack stack = handler.getStackInSlot(slotIndex);
                        int stackCount = stack.getCount();
                        if (!stack.isEmpty() && ingredient.getIngredient().test(stack)) {
                            count = count + stackCount;
                            if (count <= ingredient.getCount()) {
                                recordCount.put(slotIndex, stackCount);
                            } else {
                                int remaining = count - ingredient.getCount();
                                recordCount.put(slotIndex, stackCount - remaining);
                                break;
                            }
                        }
                    }
                    if (count < ingredient.getCount()) {
                        return;
                    }
                }

                for (int slotIndex : recordCount.keySet()) {
                    handler.extractItem(slotIndex, recordCount.get(slotIndex), false);
                }
            }

            Level level = player.level();
            if (!level.isClientSide()) {
                ItemStack crafted = recipe.getResultItem().copy();
                if (crafted.getItem() instanceof IGun iGun) {
                    Identifier craftedGunId = iGun.getGunId(crafted);
                    TimelessAPI.getCommonGunIndex(craftedGunId).ifPresent(gunIndex -> {
                        if (iGun.getFireMode(crafted) == FireMode.UNKNOWN && !gunIndex.getGunData().getFireModeSet().isEmpty()) {
                            iGun.setFireMode(crafted, gunIndex.getGunData().getFireModeSet().get(0));
                        }
                    });
                } else {
                }
                ItemHandlerHelper.giveItemToPlayer(player, crafted);
            }
            player.inventoryMenu.broadcastFullState();
            NetworkHandler.sendToClientPlayer(new ServerMessageCraft(this.containerId), player);
        });
    }
}
