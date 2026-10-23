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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class MiniEditNetworking {
	private MiniEditNetworking() {}

	public static void init() {
		PayloadTypeRegistry.serverboundPlay().register(EditActionPayload.TYPE, EditActionPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(EditStatePayload.TYPE, EditStatePayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(EditActionPayload.TYPE, (payload, ctx) -> {
			ctx.server().executeIfPossible(() -> handle(ctx.player(), payload));
		});

		// Left click on block with the axe -> corner 1
		AttackBlockCallback.EVENT.register((player, world, hand, pos, hit) -> {
			ItemStack stack = player.getItemInHand(hand);
			if (stack.getItem() != ModItems.EDIT_AXE) {
				return InteractionResult.PASS;
			}
			if (world.isClientSide()) {
				return InteractionResult.SUCCESS;
			}
			if (player instanceof ServerPlayer sp) {
				PlayerSession s = SessionManager.get(sp);
				s.pos1 = pos.immutable();
				sendState(sp, false);
				sp.sendOverlayMessage(Component.literal("[MiniEdit] Góc 1: " + fmt(s.pos1)));
			}
			return InteractionResult.SUCCESS;
		});

		// Right click on block with the axe -> corner 2, with the panel -> open panel
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			ItemStack stack = player.getItemInHand(hand);
			if (stack.getItem() == ModItems.EDIT_PANEL) {
				if (world.isClientSide()) {
					return InteractionResult.SUCCESS;
				}
				if (player instanceof ServerPlayer sp) {
					sendState(sp, true);
				}
				return InteractionResult.SUCCESS;
			}
			if (stack.getItem() != ModItems.EDIT_AXE) {
				return InteractionResult.PASS;
			}
			if (world.isClientSide()) {
				return InteractionResult.SUCCESS;
			}
			if (player instanceof ServerPlayer sp) {
				PlayerSession s = SessionManager.get(sp);
				s.pos2 = hit.getBlockPos().immutable();
				sendState(sp, false);
				sp.sendOverlayMessage(Component.literal("[MiniEdit] Góc 2: " + fmt(s.pos2)));
			}
			return InteractionResult.SUCCESS;
		});

		// Right click in air with the panel -> open panel GUI
		UseItemCallback.EVENT.register((player, world, hand) -> {
			if (player.getItemInHand(hand).getItem() != ModItems.EDIT_PANEL) {
				return InteractionResult.PASS;
			}
			if (world.isClientSide()) {
				return InteractionResult.SUCCESS;
			}
			if (player instanceof ServerPlayer sp) {
				sendState(sp, true);
			}
			return InteractionResult.SUCCESS;
		});

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			SessionManager.clearAll();
			MiniEdit.LOGGER.info("[MiniEdit] sessions cleared");
		});
	}

	private static void handle(ServerPlayer player, EditActionPayload payload) {
		PlayerSession s = SessionManager.get(player);
		Level level = player.level();
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
					out = new RegionEdit.OpOut(false, 0, "Hãy cầm một block trên tay phải");
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
		player.sendSystemMessage(Component.literal("[MiniEdit] " + out.msg()));
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
