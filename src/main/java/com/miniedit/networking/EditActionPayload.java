package com.miniedit.networking;

import com.miniedit.MiniEdit;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client -> server action request. */
public record EditActionPayload(byte op, String material) implements CustomPacketPayload {
	public static final byte OP_REQUEST_STATE = 0;
	public static final byte OP_SET = 1;
	public static final byte OP_WALLS = 2;
	public static final byte OP_COPY = 3;
	public static final byte OP_PASTE = 4;
	public static final byte OP_UNDO = 5;
	public static final byte OP_MATERIAL_SET = 6;
	public static final byte OP_MATERIAL_HELD = 7;

	public static final CustomPacketPayload.Type<EditActionPayload> TYPE =
		new CustomPacketPayload.Type<>(
			ResourceLocation.fromNamespaceAndPath(MiniEdit.MOD_ID, "action"));

	public static final PacketCodec<ByteBuf, EditActionPayload> CODEC = PacketCodec.of(
		(payload, buf) -> {
			buf.writeByte(payload.op());
			buf.writeUtf(payload.material(), 256);
		},
		buf -> new EditActionPayload(buf.readByte(), buf.readUtf(256)));

	@Override
	public CustomPacketPayload.Type<EditActionPayload> type() {
		return TYPE;
	}
}
