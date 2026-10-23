package com.miniedit.commands;

import com.miniedit.MiniEdit;
import com.miniedit.edit.RegionEdit;
import com.miniedit.registry.ModItems;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class MiniEditCommands {
	private MiniEditCommands() {}

	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			dispatcher.register(Commands.literal("miniedit")
				.requires(src -> src.hasPermission(2))
				.then(Commands.literal("kit").executes(ctx -> giveKit(ctx.getSource())))
				.then(Commands.literal("debugtest").executes(ctx -> debugTest(ctx.getSource())))));
	}

	private static int giveKit(CommandSourceStack source) {
		ServerPlayer player = source.getPlayerOrException();
		player.getInventory().add(new ItemStack(ModItems.EDIT_AXE));
		player.getInventory().add(new ItemStack(ModItems.EDIT_PANEL));
		source.sendSuccess(() -> Component.literal(
			"[MiniEdit] Đã gửi Rìu + Bảng Xây Dựng vào túi bạn"), false);
		MiniEdit.LOGGER.info("[MiniEdit] kit given to {}", player.getName().getString());
		return 1;
	}

	private static int debugTest(CommandSourceStack source) {
		ServerLevel level = source.getServer().overworld();
		String error = RegionEdit.selfTest(level);
		if (error == null) {
			source.sendSuccess(() -> Component.literal(
				"[MiniEdit] SelfTest OK (set 27/27, undo OK)"), false);
			MiniEdit.LOGGER.info("[MiniEdit] SelfTest PASSED");
			return 1;
		}
		MiniEdit.LOGGER.error("[MiniEdit] SelfTest FAILED: {}", error);
		source.sendFailure(Component.literal("[MiniEdit] SelfTest FAILED: " + error));
		return 0;
	}
}
