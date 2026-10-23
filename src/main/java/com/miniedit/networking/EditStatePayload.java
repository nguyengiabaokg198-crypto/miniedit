package com.miniedit.networking;

import com.miniedit.MiniEdit;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server -> client session state (also used to open the panel). */
public record EditStatePayload(
		boolean open,
		boolean pos1Set, int x1, int y1, int z1,
		boolean pos2Set, int x2, int y2, int z2,
		String material, boolean hasCopy, boolean hasUndo) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<EditStatePayload> TYPE =
		new CustomPacketPayload.Type<>(
			Identifier.fromNamespaceAndPath(MiniEdit.MOD_ID, "state"));

	public static final StreamCodec<ByteBuf, EditStatePayload> CODEC = StreamCodec.of(
		(buf, p) -> {
			buf.writeBoolean(p.open());
			buf.writeBoolean(p.pos1Set());
			buf.writeInt(p.x1());
			buf.writeInt(p.y1());
			buf.writeInt(p.z1());
			buf.writeBoolean(p.pos2Set());
			buf.writeInt(p.x2());
			buf.writeInt(p.y2());
			buf.writeInt(p.z2());
			buf.writeUtf(p.material(), 128);
			buf.writeBoolean(p.hasCopy());
			buf.writeBoolean(p.hasUndo());
		},
		buf -> new EditStatePayload(
			buf.readBoolean(),
			buf.readBoolean(), buf.readInt(), buf.readInt(), buf.readInt(),
			buf.readBoolean(), buf.readInt(), buf.readInt(), buf.readInt(),
			buf.readUtf(128), buf.readBoolean(), buf.readBoolean()));

	@Override
	public CustomPacketPayload.Type<EditStatePayload> type() {
		return TYPE;
	}
}
