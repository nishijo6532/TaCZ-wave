package com.tacz.guns.client.gui;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.gui.components.FlatColorButton;
import com.tacz.guns.client.gui.components.smith.ResultButton;
import com.tacz.guns.client.gui.components.smith.TypeButton;
import com.tacz.guns.config.sync.SyncConfig;
import com.tacz.guns.crafting.GunSmithTableIngredient;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.inventory.GunSmithTableMenu;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ClientMessageCraft;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.filter.RecipeFilter;
import com.tacz.guns.resource.pojo.data.block.TabConfig;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GunSmithTableScreen extends AbstractContainerScreen<GunSmithTableMenu> {
    private static final Identifier TEXTURE = com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "textures/gui/gun_smith_table.png");
    private static final Identifier SIDE = com.tacz.guns.util.IdHelper.id(GunMod.MOD_ID, "textures/gui/gun_smith_table_side.png");

    private final LinkedHashMap<Identifier, TabConfig> recipeKeys = new LinkedHashMap<>();
    private final Map<Identifier, List<Identifier>> recipes = new LinkedHashMap<>();
    private final Map<Identifier, GunSmithTableRecipe> recipesById = new LinkedHashMap<>();

    private int typePage;
    private @Nullable Identifier selectedType;
    private List<Identifier> selectedRecipeList = List.of();
    private int indexPage;
    private @Nullable Identifier selectedRecipeId;
    private @Nullable GunSmithTableRecipe selectedRecipe;
    private @Nullable Int2IntArrayMap playerIngredientCount;
    private boolean loggedIngredientDereferenceIssue;

    public GunSmithTableScreen(GunSmithTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 344, 186);
    }

    @Override
    public void init() {
        super.init();
        this.classifyRecipes();
        this.clearWidgets();
        this.addTypePageButtons();
        this.addTypeButtons();
        this.addIndexPageButtons();
        this.addIndexButtons();
        this.addCraftButton();
    }

    public void updateIngredientCount() {
        this.getPlayerIngredientCount(this.selectedRecipe);
    }

    @Nullable
    private RecipeManager getActiveRecipeManager() {
        if (this.minecraft != null) {
            if (this.minecraft.hasSingleplayerServer() && this.minecraft.getSingleplayerServer() != null) {
                return this.minecraft.getSingleplayerServer().getRecipeManager();
            }
        } else {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null) {
                return mc.getSingleplayerServer().getRecipeManager();
            }
        }
        return CommonAssetsManager.getInstance() != null ? CommonAssetsManager.getInstance().recipeManager : null;
    }

    private void classifyRecipes() {
        Identifier previousType = this.selectedType;
        Identifier previousRecipeId = this.selectedRecipeId;

        this.recipes.clear();
        this.recipeKeys.clear();
        this.recipesById.clear();

        Identifier blockId = this.menu.getBlockId();
        if (blockId == null) {
            this.selectedType = null;
            this.selectedRecipeId = null;
            this.selectedRecipe = null;
            this.playerIngredientCount = null;
            this.selectedRecipeList = List.of();
            return;
        }

        Map<Identifier, List<Identifier>> groupedRecipes = new LinkedHashMap<>();
        Map<Identifier, TabConfig> groupedTabs = new LinkedHashMap<>();
        TimelessAPI.getCommonBlockIndex(blockId).ifPresent(blockIndex -> {
            List<TabConfig> tabs = blockIndex.getData().getTabs();
            if (DefaultAssets.DEFAULT_BLOCK_ID.equals(blockId) && !SyncConfig.ENABLE_TABLE_FILTER.get()) {
                tabs = TabConfig.defaultTabs();
            }
            for (TabConfig tab : tabs) {
                groupedTabs.put(tab.id(), tab);
                groupedRecipes.put(tab.id(), new ArrayList<>());
            }
        });
        if (groupedTabs.isEmpty() && DefaultAssets.DEFAULT_BLOCK_ID.equals(blockId)) {
            for (TabConfig tab : TabConfig.defaultTabs()) {
                groupedTabs.put(tab.id(), tab);
                groupedRecipes.put(tab.id(), new ArrayList<>());
            }
        }

        RecipeManager recipeManager = getActiveRecipeManager();
        boolean defaultTableWithoutFilter = DefaultAssets.DEFAULT_BLOCK_ID.equals(blockId) && !SyncConfig.ENABLE_TABLE_FILTER.get();
        RecipeFilter tableFilter = TimelessAPI.getCommonBlockIndex(blockId)
                .map(blockIndex -> blockIndex.getFilter())
                .orElse(null);
        if (recipeManager != null) {
            recipeManager.getRecipes().stream()
                    .filter(GunSmithTableRecipe.class::isInstance)
                    .forEach(recipeHolder -> {
                        GunSmithTableRecipe recipe = (GunSmithTableRecipe) recipeHolder.value();
                        recipe.init();
                        Identifier recipeId = recipeHolder.id().identifier();
                        Identifier tabId = recipe.getTab() != null ? recipe.getTab() : TabConfig.TAB_EMPTY;
                        if (!isRecipeVisibleForTable(recipeId, tabId, groupedTabs, tableFilter, defaultTableWithoutFilter)) {
                            return;
                        }
                        groupedRecipes.computeIfAbsent(tabId, id -> new ArrayList<>()).add(recipeId);
                        groupedTabs.computeIfAbsent(tabId, id -> new TabConfig(id, "tacz.type.unknown.name", ItemStack.EMPTY));
                        this.recipesById.put(recipeId, recipe);
                    });
        }
        if (this.recipesById.isEmpty() && this.minecraft != null && this.minecraft.hasSingleplayerServer() && this.minecraft.getSingleplayerServer() != null) {
            Map<Identifier, GunSmithTableRecipe> compatRecipes = CommonAssetsManager.loadGunSmithRecipesFromResourceManager(this.minecraft.getSingleplayerServer().getResourceManager());
            compatRecipes.forEach((recipeId, recipe) -> {
                Identifier tabId = recipe.getTab() != null ? recipe.getTab() : TabConfig.TAB_EMPTY;
                if (!isRecipeVisibleForTable(recipeId, tabId, groupedTabs, tableFilter, defaultTableWithoutFilter)) {
                    return;
                }
                groupedRecipes.computeIfAbsent(tabId, id -> new ArrayList<>()).add(recipeId);
                groupedTabs.computeIfAbsent(tabId, id -> new TabConfig(id, "tacz.type.unknown.name", ItemStack.EMPTY));
                this.recipesById.put(recipeId, recipe);
            });
        }

        for (Map.Entry<Identifier, List<Identifier>> entry : groupedRecipes.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                this.recipes.put(entry.getKey(), entry.getValue());
                this.recipeKeys.put(entry.getKey(), groupedTabs.get(entry.getKey()));
            }
        }

        if (previousType != null && this.recipeKeys.containsKey(previousType)) {
            this.selectedType = previousType;
        } else if (!this.recipeKeys.isEmpty()) {
            this.selectedType = this.recipeKeys.keySet().iterator().next();
        } else {
            this.selectedType = null;
        }

        if (this.selectedType != null) {
            this.selectedRecipeList = this.recipes.getOrDefault(this.selectedType, List.of());
        } else {
            this.selectedRecipeList = List.of();
        }

        if (this.selectedRecipeList.isEmpty()) {
            this.selectedRecipeId = null;
            this.selectedRecipe = null;
            this.playerIngredientCount = null;
            this.indexPage = 0;
            return;
        }

        if (this.indexPage > (this.selectedRecipeList.size() - 1) / 6) {
            this.indexPage = 0;
        }

        this.selectedRecipeId = null;
        this.selectedRecipe = this.getSelectedRecipe(previousRecipeId);
        if (this.selectedRecipe != null && previousRecipeId != null && this.selectedRecipeList.contains(previousRecipeId)) {
            this.selectedRecipeId = previousRecipeId;
        }
        if (this.selectedRecipe == null || this.selectedRecipeId == null || !this.selectedRecipeList.contains(this.selectedRecipeId)) {
            Identifier firstRecipeId = this.selectedRecipeList.get(0);
            this.selectedRecipeId = firstRecipeId;
            this.selectedRecipe = this.getSelectedRecipe(firstRecipeId);
        }
        this.getPlayerIngredientCount(this.selectedRecipe);
    }

    private static boolean isRecipeVisibleForTable(Identifier recipeId, Identifier tabId, Map<Identifier, TabConfig> tabs,
                                                   @Nullable RecipeFilter filter, boolean defaultTableWithoutFilter) {
        if (defaultTableWithoutFilter) {
            return true;
        }
        if (filter != null && !filter.contains(recipeId)) {
            return false;
        }
        return tabs.containsKey(tabId);
    }

    @Nullable
    private GunSmithTableRecipe getSelectedRecipe(@Nullable Identifier recipeId) {
        if (recipeId == null) {
            return null;
        }
        GunSmithTableRecipe cachedRecipe = this.recipesById.get(recipeId);
        if (cachedRecipe != null) {
            return cachedRecipe;
        }
        RecipeManager recipeManager = getActiveRecipeManager();
        if (recipeManager == null) {
            return null;
        }
        GunSmithTableRecipe resolvedRecipe = recipeManager.getRecipes().stream()
                .filter(recipeHolder -> recipeHolder.id().identifier().equals(recipeId))
                .map(recipeHolder -> recipeHolder.value())
                .filter(GunSmithTableRecipe.class::isInstance)
                .map(GunSmithTableRecipe.class::cast)
                .findFirst()
                .orElse(null);
        if (resolvedRecipe != null) {
            this.recipesById.put(recipeId, resolvedRecipe);
        }
        return resolvedRecipe;
    }

    private void getPlayerIngredientCount(@Nullable GunSmithTableRecipe recipe) {
        if (recipe == null || this.minecraft == null || this.minecraft.player == null) {
            this.playerIngredientCount = null;
            return;
        }
        List<GunSmithTableIngredient> ingredients = recipe.getInputs();
        Int2IntArrayMap counts = new Int2IntArrayMap(ingredients.size());
        Inventory inventory = this.minecraft.player.getInventory();
        for (int i = 0; i < ingredients.size(); i++) {
            GunSmithTableIngredient ingredient = ingredients.get(i);
            int count = 0;
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (!stack.isEmpty() && ingredient.getIngredient().test(stack)) {
                    count += stack.getCount();
                }
            }
            counts.put(i, count);
        }
        this.playerIngredientCount = counts;
    }

    private void addCraftButton() {
        this.addRenderableWidget(new FlatColorButton(this.leftPos + 289, this.topPos + 162, 48, 18,
                Component.translatable("gui.tacz.gun_smith_table.craft"), b -> {
            if (this.selectedRecipe == null) {
                return;
            }
            if (this.selectedRecipeId == null) {
                return;
            }
            if (this.playerIngredientCount == null) {
                return;
            }
            boolean isCreative = this.minecraft != null && this.minecraft.player != null && this.minecraft.player.isCreative();
            List<GunSmithTableIngredient> inputs = this.selectedRecipe.getInputs();
            for (int i = 0; i < inputs.size(); i++) {
                int hasCount = this.playerIngredientCount.get(i);
                int needCount = inputs.get(i).getCount();
                if (hasCount < needCount && !isCreative) {
                    return;
                }
            }
            NetworkHandler.sendToServer(new ClientMessageCraft(this.selectedRecipeId, this.menu.containerId));
        }));
    }

    private void addTypePageButtons() {
        this.addRenderableWidget(new FlatColorButton(this.leftPos + 136, this.topPos + 4, 18, 20, Component.literal("<"), b -> {
            if (this.typePage > 0) {
                this.typePage--;
                this.init();
            }
        }));
        this.addRenderableWidget(new FlatColorButton(this.leftPos + 327, this.topPos + 4, 18, 20, Component.literal(">"), b -> {
            int maxPage = Math.max(0, (this.recipes.size() - 1) / 7);
            if (this.typePage < maxPage) {
                this.typePage++;
                this.init();
            }
        }));
    }

    private void addTypeButtons() {
        if (this.recipeKeys.isEmpty()) {
            return;
        }
        List<TabConfig> tabs = new ArrayList<>(this.recipeKeys.values());
        for (int i = 0; i < 7; i++) {
            int typeIndex = this.typePage * 7 + i;
            if (typeIndex >= tabs.size()) {
                break;
            }
            TabConfig tab = tabs.get(typeIndex);
            Identifier type = tab.id();
            TypeButton typeButton = new TypeButton(this.leftPos + 157 + 24 * i, this.topPos + 2, tab.icon(), b -> {
                this.selectedType = type;
                this.selectedRecipeList = this.recipes.getOrDefault(type, List.of());
                this.indexPage = 0;
                this.selectedRecipeId = this.selectedRecipeList.isEmpty() ? null : this.selectedRecipeList.get(0);
                this.selectedRecipe = this.getSelectedRecipe(this.selectedRecipeId);
                this.getPlayerIngredientCount(this.selectedRecipe);
                this.init();
            });
            if (type.equals(this.selectedType)) {
                typeButton.setSelected(true);
            }
            this.addRenderableWidget(typeButton);
        }
    }

    private void addIndexPageButtons() {
        this.addRenderableWidget(new FlatColorButton(this.leftPos + 143, this.topPos + 56, 96, 6, Component.literal("^"), b -> {
            if (this.indexPage > 0) {
                this.indexPage--;
                this.init();
            }
        }));
        this.addRenderableWidget(new FlatColorButton(this.leftPos + 143, this.topPos + 171, 96, 6, Component.literal("v"), b -> {
            int maxPage = this.selectedRecipeList.isEmpty() ? 0 : (this.selectedRecipeList.size() - 1) / 6;
            if (this.indexPage < maxPage) {
                this.indexPage++;
                this.init();
            }
        }));
    }

    private void addIndexButtons() {
        if (this.selectedRecipeList.isEmpty()) {
            return;
        }
        for (int i = 0; i < 6; i++) {
            int finalIndex = i + this.indexPage * 6;
            if (finalIndex >= this.selectedRecipeList.size()) {
                break;
            }
            Identifier recipeId = this.selectedRecipeList.get(finalIndex);
            GunSmithTableRecipe recipe = this.getSelectedRecipe(recipeId);
            if (recipe == null) {
                continue;
            }
            ResultButton button = this.addRenderableWidget(new ResultButton(this.leftPos + 144, this.topPos + 66 + 17 * i, recipe.getOutput(), b -> {
                this.selectedRecipeId = recipeId;
                this.selectedRecipe = recipe;
                this.getPlayerIngredientCount(this.selectedRecipe);
                this.init();
            }));
            if (this.selectedRecipeId != null && recipeId.equals(this.selectedRecipeId)) {
                button.setSelected(true);
            }
        }
    }

    private void renderIngredient(@NotNull GuiGraphicsExtractor gui) {
        if (this.selectedRecipe == null) {
            return;
        }
        List<GunSmithTableIngredient> inputs = this.selectedRecipe.getInputs();
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 2; j++) {
                int index = i * 2 + j;
                if (index >= inputs.size()) {
                    return;
                }
                int offsetX = this.leftPos + 254 + 45 * j;
                int offsetY = this.topPos + 62 + 17 * i;
                GunSmithTableIngredient ingredient = inputs.get(index);
                Ingredient itemIngredient = ingredient.getIngredient();
                ItemStack[] candidates;
                try {
                    candidates = itemIngredient.items()
                            .map(holder -> holder.value().getDefaultInstance())
                            .filter(stack -> !stack.isEmpty())
                            .toArray(ItemStack[]::new);
                } catch (UnsupportedOperationException e) {
                    if (!this.loggedIngredientDereferenceIssue) {
                        GunMod.LOGGER.warn("GunSmithTableScreen ingredient tag not ready during render, skipping candidates until tags are bound: {}", e.getMessage());
                        this.loggedIngredientDereferenceIssue = true;
                    }
                    candidates = new ItemStack[0];
                }
                if (candidates.length > 0) {
                    int itemIndex = (int) ((System.currentTimeMillis() / 1000L) % candidates.length);
                    gui.fakeItem(candidates[itemIndex], offsetX, offsetY);
                }
                int need = ingredient.getCount();
                int has = this.playerIngredientCount != null ? this.playerIngredientCount.get(index) : 0;
                boolean isCreative = this.minecraft != null && this.minecraft.player != null && this.minecraft.player.isCreative();
                int color = (isCreative || has >= need) ? 0xFFFFFFFF : 0xFFFF0000;
                String countText = isCreative ? need + "/\u221E" : need + "/" + has;
                int textX = offsetX + 17;
                int maxWidth = 28;
                gui.text(this.font, trimToWidth(countText, maxWidth), textX, offsetY + 10, color, false);
            }
        }
    }

    private String trimToWidth(String text, int maxWidth) {
        int end = 0;
        while (end < text.length() && this.font.width(text.substring(0, end + 1)) <= maxWidth) {
            end++;
        }
        return text.substring(0, end);
    }

    private void renderPreview(@NotNull GuiGraphicsExtractor gui) {
        if (this.selectedRecipe == null) {
            return;
        }
        ItemStack previewStack = this.selectedRecipe.getOutput();
        if (previewStack.isEmpty()) {
            return;
        }
        gui.pose().pushMatrix();
        gui.pose().translate(this.leftPos + 40.0f, this.topPos + 48.0f);
        gui.pose().scale(3.6f, 3.6f);
        gui.item(previewStack, 0, 0, 0);
        gui.pose().popMatrix();
    }

    @Override
    protected void extractLabels(@NotNull GuiGraphicsExtractor gui, int mouseX, int mouseY) {
    }

    @Override
    public void extractContents(@NotNull GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick) {
        gui.blit(RenderPipelines.GUI_TEXTURED, SIDE, this.leftPos, this.topPos, 0f, 0f, 134, 187, BACKGROUND_TEXTURE_WIDTH, BACKGROUND_TEXTURE_HEIGHT);
        gui.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos + 136, this.topPos + 27, 0f, 0f, 208, 160, BACKGROUND_TEXTURE_WIDTH, BACKGROUND_TEXTURE_HEIGHT);
        super.extractContents(gui, mouseX, mouseY, partialTick);

        if (this.selectedType != null) {
            TabConfig tabConfig = this.recipeKeys.get(this.selectedType);
            if (tabConfig != null) {
                gui.text(this.font, tabConfig.getName(), this.leftPos + 150, this.topPos + 32, 0xFF555555, false);
            }
        }
        this.renderPreview(gui);
        gui.centeredText(this.font, Component.translatable("gui.tacz.gun_smith_table.preview"), this.leftPos + 108, this.topPos + 5, 0xFF555555);
        gui.text(this.font, Component.translatable("gui.tacz.gun_smith_table.ingredient"), this.leftPos + 254, this.topPos + 50, 0xFF555555, false);

        this.renderIngredient(gui);

        for (var renderable : this.renderables) {
            if (renderable instanceof ResultButton resultButton) {
                resultButton.renderTooltips(stack -> gui.setTooltipForNextFrame(this.font, stack, mouseX, mouseY));
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount, double wheelDelta) {
        if (mouseX > this.leftPos + 143 && mouseX < this.leftPos + 237 && mouseY > this.topPos + 66 && mouseY < this.topPos + 151) {
            if (amount > 0) {
                this.indexPage = Math.max(0, this.indexPage - 1);
            } else {
                int maxPage = this.selectedRecipeList.isEmpty() ? 0 : (this.selectedRecipeList.size() - 1) / 6;
                this.indexPage = Math.min(maxPage, this.indexPage + 1);
            }
            this.init();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount, wheelDelta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
