package com.miniedit.registry;

import com.miniedit.MiniEdit;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

public final class ModItems {
	private ModItems() {}

	private static Item register(String path, java.util.function.UnaryOperator<Item.Properties> settings) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
			Identifier.fromNamespaceAndPath(MiniEdit.MOD_ID, path));
		Item item = new Item(settings.apply(new Item.Properties()).setId(key));
		Registry.register(BuiltInRegistries.ITEM, key, item);
		return item;
	}

	public static final Item EDIT_AXE = register("edit_axe",
		p -> p.stacksTo(1).rarity(Rarity.RARE));
	public static final Item EDIT_PANEL = register("edit_panel",
		p -> p.stacksTo(1).rarity(Rarity.RARE));

	public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(
		Registries.CREATIVE_MODE_TAB,
		Identifier.fromNamespaceAndPath(MiniEdit.MOD_ID, "main"));

	public static final CreativeModeTab TAB = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
		.title(Component.translatable("itemGroup.miniedit.main"))
		.icon(() -> new ItemStack(EDIT_PANEL))
		.displayItems((parameters, output) -> {
			output.accept(EDIT_AXE);
			output.accept(EDIT_PANEL);
		})
		.build();

	public static void init() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY, TAB);
	}
}
