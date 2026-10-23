package com.miniedit.networking;

import com.miniedit.MiniEdit;
import com.miniedit.edit.PlayerSession;
import com.miniedit.edit.RegionEdit;
import com.miniedit.edit.SessionManager;
import com.miniedit.registry.ModItems;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;

public final class MiniEditNetworking {
	private MiniEditNetworking() {}

	public static void init() {
		PayloadTypeRegistry.playC2S().register(EditActionPayload.TYPE, EditActionPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(EditStatePayload.TYPE, EditStatePayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(EditActionPayload.TYPE, (payload, ctx) -> {
			ServerPlayer player = ctx.player();
			player.server.execute(() -> handle(player, payload));
		});

		// Left click on block with the axe -> corner 1
		AttackBlockCallback.EVENT.register((player, world, hand, pos, hit) -> {
			if (!player.getItemInHand(hand).is(ModItems.EDIT_AXE)) {
				return InteractionResult.PASS;
			}
			if (world.isClientSide) {
				return InteractionResult.SUCCESS;
			}
			if (player instanceof ServerPlayer sp) {
				PlayerSession s = SessionManager.get(sp);
				s.pos1 = pos.immutable();
				MiniEditNetworking.sendState(sp, false);
				sp.displayClientMessage(Component.literal("[MiniEdit] Góc 1: " + fmt(s.pos1)), true);
			}
			return InteractionResult.SUCCESS;
		});

		// Right click on block with the axe -> corner 2
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			ItemStack stack = player.getItemInHand(hand);
			if (stack.is(ModItems.EDIT_PANEL)) {
				if (world.isClientSide) {
					return InteractionResult.SUCCESS;
				}
				if (player instanceof ServerPlayer sp) {
					MiniEditNetworking.sendState(sp, true);
				}
				return InteractionResult.SUCCESS;
			}
			if (!stack.is(ModItems.EDIT_AXE)) {
				return InteractionResult.PASS;
			}
			if (world.isClientSide) {
				return InteractionResult.SUCCESS;
			}
			if (player instanceof ServerPlayer sp) {
				PlayerSession s = SessionManager.get(sp);
				s.pos2 = hit.getBlockPos().immutable();
				MiniEditNetworking.sendState(sp, false);
				sp.displayClientMessage(Component.literal("[MiniEdit] Góc 2: " + fmt(s.pos2)), true);
			}
			return InteractionResult.SUCCESS;
		});

		// Right click in air with the panel -> open panel GUI
		UseItemCallback.EVENT.register((player, world, hand) -> {
			if (!player.getItemInHand(hand).is(ModItems.EDIT_PANEL)) {
				return InteractionResultHolder.pass(player.getItemInHand(hand));
			}
			if (world.isClientSide) {
				return InteractionResultHolder.success(player.getItemInHand(hand));
			}
			if (player instanceof ServerPlayer sp) {
				MiniEditNetworking.sendState(sp, true);
			}
			return InteractionResultHolder.success(player.getItemInHand(hand));
		});

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			SessionManager.clearAll();
			MiniEdit.LOGGER.info("[MiniEdit] sessions cleared");
		});
	}

	private static void handle(ServerPlayer player, EditActionPayload payload) {
		PlayerSession s = SessionManager.get(player);
		ServerLevel level = player.serverLevel();
		RegionEdit.OpOut out;
		switch (payload.op()) {
			case EditActionPayload.OP_REQUEST_STATE -> {
				sendState(player, false);
				return;
			}
			case EditActionPayload.OP_MATERIAL_SET -> {
				String mat = payload.material();
				if (RegionEdit.validateMaterial(mat)) {
					s.material = mat.trim();
					out = new RegionEdit.OpOut(true, 0, "Vật liệu: " + s.material);
				} else {
					out = new RegionEdit.OpOut(false, 0, "Block không hợp lệ: " + mat);
				}
			}
			case EditActionPayload.OP_MATERIAL_HELD -> {
				ItemStack held = player.getMainHandItem();
				if (held.getItem() instanceof BlockItem bi) {
					s.material = BuiltInRegistries.BLOCK.getKey(bi.getBlock()).toString();
					out = new RegionEdit.OpOut(true, 0, "Vật liệu (đang cầm): " + s.material);
				} else {
					out = new RegionEdit.OpOut(false, 0, "Hãy cầm một block trên tay trái");
				}
			}
			case EditActionPayload.OP_SET -> out = RegionEdit.set(level, s, s.material);
			case EditActionPayload.OP_WALLS -> out = RegionEdit.walls(level, s, s.material);
			case EditActionPayload.OP_COPY -> out = RegionEdit.copy(level, s);
			case EditActionPayload.OP_PASTE -> out = RegionEdit.paste(level, s,
				BlockPos.containing(player.getX(), player.getY(), player.getZ()));
			case EditActionPayload.OP_UNDO -> out = RegionEdit.undo(level, s);
			default -> out = new RegionEdit.OpOut(false, 0, "Không rõ lệnh: " + payload.op());
		}
		player.displayClientMessage(
			Component.literal("[MiniEdit] " + out.msg()), false);
		sendState(player, false);
	}

	private static String fmt(BlockPos pos) {
		return "(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")";
	}

	public static void sendState(ServerPlayer player, boolean open) {
		PlayerSession s = SessionManager.get(player);
		ServerPlayNetworking.send(player, new EditStatePayload(
			open,
			s.pos1 != null,
			s.pos1 == null ? 0 : s.pos1.getX(), s.pos1 == null ? 0 : s.pos1.getY(), s.pos1 == null ? 0 : s.pos1.getZ(),
			s.pos2 != null,
			s.pos2 == null ? 0 : s.pos2.getX(), s.pos2 == null ? 0 : s.pos2.getY(), s.pos2 == null ? 0 : s.pos2.getZ(),
			s.material,
			s.hasClipboard(),
			s.hasUndo()));
	}
}
