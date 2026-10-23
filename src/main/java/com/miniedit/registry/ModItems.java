package com.miniedit.registry;

import com.miniedit.MiniEdit;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.core.Registry;

public final class ModItems {
	private ModItems() {}

	public static final Item EDIT_AXE = register("edit_axe",
		new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
	public static final Item EDIT_PANEL = register("edit_panel",
		new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

	public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(
		Registries.CREATIVE_MODE_TAB,
		ResourceLocation.fromNamespaceAndPath(MiniEdit.MOD_ID, "main"));

	public static final CreativeModeTab TAB = CreativeModeTab.builder()
		.title(Component.translatable("itemGroup.miniedit.main"))
		.icon(() -> new ItemStack(EDIT_PANEL))
		.displayItems((parameters, output) -> {
			output.accept(EDIT_AXE);
			output.accept(EDIT_PANEL);
		})
		.build();

	private static Item register(String path, Item item) {
		Registry.register(BuiltInRegistries.ITEM,
			ResourceLocation.fromNamespaceAndPath(MiniEdit.MOD_ID, path), item);
		return item;
	}

	public static void init() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY, TAB);
	}
}
