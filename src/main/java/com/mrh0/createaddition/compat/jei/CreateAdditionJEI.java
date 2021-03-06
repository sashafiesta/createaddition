package com.mrh0.createaddition.compat.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.mrh0.createaddition.CreateAddition;
import com.mrh0.createaddition.index.CABlocks;
import com.mrh0.createaddition.recipe.rolling.RollingRecipe;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.ResourceLocation;

@JeiPlugin
public class CreateAdditionJEI implements IModPlugin {

	private static final ResourceLocation ID = new ResourceLocation(CreateAddition.MODID, "jei_plugin");

	@Override
	@Nonnull
	public ResourceLocation getPluginUid() {
		return ID;
	}
	
	public IIngredientManager ingredientManager;
	final List<CreateRecipeCategory<?>> ALL = new ArrayList<>();
	
	final CreateRecipeCategory<?> rolling = register("rolling", RollingMillCategory::new)
		.recipes(RollingRecipe.TYPE)
		.catalyst(CABlocks.ROLLING_MILL::get)
		.build();
	
	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		ALL.forEach(registration::addRecipeCategories);
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		ingredientManager = registration.getIngredientManager();
		ALL.forEach(c -> c.recipes.forEach(s -> registration.addRecipes(s.get(), c.getUid())));
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		ALL.forEach(c -> c.recipeCatalysts.forEach(s -> registration.addRecipeCatalyst(s.get(), c.getUid())));
	}
	
	private <T extends IRecipe<?>> CategoryBuilder<T> register(String name, Supplier<CreateRecipeCategory<T>> supplier) {
		return new CategoryBuilder<T>(name, supplier);
	}
	
	private class CategoryBuilder<T extends IRecipe<?>> {
		CreateRecipeCategory<T> category;

		CategoryBuilder(String name, Supplier<CreateRecipeCategory<T>> category) {
			this.category = category.get();
			this.category.setCategoryId(name);
		}

		CategoryBuilder<T> catalyst(Supplier<IItemProvider> supplier) {
			return catalystStack(() -> new ItemStack(supplier.get()
				.asItem()));
		}

		CategoryBuilder<T> catalystStack(Supplier<ItemStack> supplier) {
			category.recipeCatalysts.add(supplier);
			return this;
		}

		CategoryBuilder<T> recipes(IRecipeType<?> recipeTypeEntry) {
			category.recipes.add(() -> findRecipesByType(recipeTypeEntry));
			return this;
		}

		CreateRecipeCategory<T> build() {
			ALL.add(category);
			return category;
		}
	}
	
	static List<IRecipe<?>> findRecipesByType(IRecipeType<?> type) {
		return findRecipes(r -> r.getType() == type);
	}
	
	@SuppressWarnings("resource")
	static List<IRecipe<?>> findRecipes(Predicate<IRecipe<?>> predicate) {
		return Minecraft.getInstance().world.getRecipeManager()
			.getRecipes()
			.stream()
			.filter(predicate)
			.collect(Collectors.toList());
	}
}
