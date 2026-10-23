package com.miniedit.edit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.miniedit.MiniEdit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Pure region-editing logic. Runs on the server thread only.
 */
public final class RegionEdit {
	private RegionEdit() {}

	public static final int MAX_DIM = 32;
	private static final int UPDATE_FLAGS = Block.UPDATE_NEIGHBORS | Block.UPDATE_CLIENTS;

	public record OpOut(boolean ok, int count, String msg) {}

	private static BlockState parseState(String id) {
		if (id == null) {
			return null;
		}
		String trimmed = id.trim();
		if (trimmed.indexOf(':') < 0) {
			trimmed = "minecraft:" + trimmed;
		}
		if (trimmed.length() > 128) {
			return null;
		}
		Identifier rl;
		try {
			rl = Identifier.parse(trimmed);
		} catch (Exception ex) {
			return null;
		}
		Optional<Block> block = BuiltInRegistries.BLOCK.getOptional(rl);
		if (block.isEmpty() || block.get() == Blocks.AIR) {
			return null;
		}
		return block.get().defaultBlockState();
	}

	public static String blockIdOfState(BlockState state) {
		return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
	}

	public static boolean validateMaterial(String material) {
		return parseState(material) != null;
	}

	private static BlockPos[] bounds(Level level, PlayerSession session) {
		if (!session.selectionComplete()) {
			return null;
		}
		int minX = Math.min(session.pos1.getX(), session.pos2.getX());
		int minY = Math.min(session.pos1.getY(), session.pos2.getY());
		int minZ = Math.min(session.pos1.getZ(), session.pos2.getZ());
		int maxX = Math.max(session.pos1.getX(), session.pos2.getX());
		int maxY = Math.max(session.pos1.getY(), session.pos2.getY());
		int maxZ = Math.max(session.pos1.getZ(), session.pos2.getZ());
		minY = Math.max(minY, level.getMinY());
		maxY = Math.min(maxY, level.getMaxY());
		if (maxY < minY) {
			return null;
		}
		return new BlockPos[] { new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ) };
	}

	private static BlockPos[] checkedBounds(Level level, PlayerSession session) {
		BlockPos[] b = bounds(level, session);
		if (b == null) {
			return null;
		}
		BlockPos min = b[0], max = b[1];
		if (max.getX() - min.getX() + 1 > MAX_DIM || max.getY() - min.getY() + 1 > MAX_DIM
				|| max.getZ() - min.getZ() + 1 > MAX_DIM) {
			return null;
		}
		return b;
	}

	private static void placeOne(Level level, BlockPos mutable, BlockState st, List<PlayerSession.Entry> entries) {
		BlockPos pos = mutable.immutable();
		BlockState prev = level.getBlockState(pos);
		if (prev == st) {
			return;
		}
		entries.add(new PlayerSession.Entry(pos, prev));
		level.setBlock(pos, st, UPDATE_FLAGS, 512);
	}

	public static OpOut set(Level level, PlayerSession session, String materialId) {
		BlockState st = parseState(materialId);
		if (st == null) {
			return new OpOut(false, 0, "Không tìm thấy block: " + materialId);
		}
		BlockPos[] b = checkedBounds(level, session);
		if (b == null) {
			return new OpOut(false, 0, "Cần chọn 2 góc (rìu) trong hộp 32×32×32");
		}
		List<PlayerSession.Entry> entries = session.newEntries();
		for (BlockPos p : BlockPos.betweenClosed(b[0], b[1])) {
			placeOne(level, p, st, entries);
		}
		session.pushUndo(entries);
		MiniEdit.LOGGER.info("[MiniEdit] set {} blocks of {}", entries.size(), materialId);
		return new OpOut(true, entries.size(), "Đã đặt " + entries.size() + " block: " + materialId);
	}

	public static OpOut walls(Level level, PlayerSession session, String materialId) {
		BlockState st = parseState(materialId);
		if (st == null) {
			return new OpOut(false, 0, "Không tìm thấy block: " + materialId);
		}
		BlockPos[] b = checkedBounds(level, session);
		if (b == null) {
			return new OpOut(false, 0, "Cần chọn 2 góc (rìu) trong hộp 32×32×32");
		}
		List<PlayerSession.Entry> entries = session.newEntries();
		for (BlockPos p : BlockPos.betweenClosed(b[0], b[1])) {
			boolean onWall = p.getX() == b[0].getX() || p.getX() == b[1].getX()
				|| p.getZ() == b[0].getZ() || p.getZ() == b[1].getZ();
			if (onWall) {
				placeOne(level, p, st, entries);
			}
		}
		session.pushUndo(entries);
		return new OpOut(true, entries.size(), "Đã dựng tường " + entries.size() + " block: " + materialId);
	}

	public static OpOut copy(Level level, PlayerSession session) {
		BlockPos[] b = checkedBounds(level, session);
		if (b == null) {
			return new OpOut(false, 0, "Cần chọn 2 góc (rìu) trong hộp 32×32×32");
		}
		BlockPos min = b[0];
		List<PlayerSession.CopyEntry> copy = new ArrayList<>();
		for (BlockPos p : BlockPos.betweenClosed(b[0], b[1])) {
			BlockState cur = level.getBlockState(p);
			copy.add(new PlayerSession.CopyEntry(
				p.getX() - min.getX(), p.getY() - min.getY(), p.getZ() - min.getZ(), cur));
		}
		session.clipboard = copy;
		return new OpOut(true, copy.size(), "Đã copy " + copy.size() + " block");
	}

	public static OpOut paste(Level level, PlayerSession session, BlockPos playerPos) {
		if (!session.hasClipboard()) {
			return new OpOut(false, 0, "Chưa copy gì (bấm nút Copy trước)");
		}
		BlockPos anchor = session.pos2 != null ? session.pos2.immutable() : playerPos.immutable();
		List<PlayerSession.Entry> entries = session.newEntries();
		for (PlayerSession.CopyEntry ce : session.clipboard) {
			BlockPos pos = anchor.offset(ce.dx(), ce.dy(), ce.dz());
			if (!level.isLoaded(pos)) {
				continue;
			}
			BlockState prev = level.getBlockState(pos);
			BlockState st = ce.state();
			if (prev == st) {
				continue;
			}
			entries.add(new PlayerSession.Entry(pos, prev));
			level.setBlock(pos, st, UPDATE_FLAGS, 512);
		}
		session.pushUndo(entries);
		return new OpOut(true, entries.size(), "Đã dán " + entries.size() + " block");
	}

	public static OpOut undo(Level level, PlayerSession session) {
		List<PlayerSession.Entry> last = session.popUndo();
		if (last == null) {
			return new OpOut(false, 0, "Không có gì để hoàn tác");
		}
		for (int i = last.size() - 1; i >= 0; i--) {
			PlayerSession.Entry e = last.get(i);
			level.setBlock(e.pos(), e.prev(), UPDATE_FLAGS, 512);
		}
		return new OpOut(true, last.size(), "Đã hoàn tác " + last.size() + " block");
	}

	/**
	 * Headless self-test used by CI: set 3x3x3 stone, verify, undo, verify.
	 * Returns error text on failure, null on success.
	 */
	public static String selfTest(Level level) {
		level.getChunk(0, 0);
		BlockPos a = new BlockPos(8, 70, 8);
		BlockPos b = a.offset(2, 2, 2);
		PlayerSession session = new PlayerSession();
		session.pos1 = a;
		session.pos2 = b;
		OpOut set = set(level, session, "stone");
		if (!set.ok() || set.count() != 27) {
			return "set failed: " + set.msg();
		}
		int stones = 0;
		for (BlockPos p : BlockPos.betweenClosed(a, b)) {
			if (level.getBlockState(p).getBlock() == Blocks.STONE) {
				stones++;
			}
		}
		if (stones == 27) {
			OpOut un = undo(level, session);
			if (!un.ok() || un.count() != 27) {
				return "undo failed: " + un.msg();
			}
			int stonesAfter = 0;
			for (BlockPos p : BlockPos.betweenClosed(a, b)) {
				if (level.getBlockState(p).getBlock() == Blocks.STONE) {
					stonesAfter++;
				}
			}
			if (stonesAfter < 27) {
				return null;
			}
			return "undo did not revert: still " + stonesAfter + "/27 stone";
		}
		return "expected 27 stone, got " + stones;
	}
}
