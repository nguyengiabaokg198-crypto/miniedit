package com.miniedit.edit;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Per-player build session (server side): selection corners, current material,
 * clipboard and undo history.
 */
public final class PlayerSession {
	public BlockPos pos1;
	public BlockPos pos2;
	public String material = "stone";
	public List<CopyEntry> clipboard;

	public record Entry(BlockPos pos, BlockState prev) {}
	public record CopyEntry(int dx, int dy, int dz, BlockState state) {}

	private static final int MAX_UNDO_OPS = 4;
	private static final int MAX_UNDO_ENTRIES = 65536;

	private final Deque<List<Entry>> undoStack = new ArrayDeque<>();

	public boolean selectionComplete() {
		return pos1 != null && pos2 != null;
	}

	public boolean hasClipboard() {
		return clipboard != null && !clipboard.isEmpty();
	}

	public boolean hasUndo() {
		return !undoStack.isEmpty();
	}

	public int undoOps() {
		return undoStack.size();
	}

	public void pushUndo(List<Entry> ops) {
		if (ops == null || ops.isEmpty()) {
			return;
		}
		undoStack.push(ops);
		while (undoStack.size() > MAX_UNDO_OPS
				|| totalEntries() > MAX_UNDO_ENTRIES) {
			if (undoStack.size() > 1) {
				undoStack.removeLast();
				if (totalEntries() <= MAX_UNDO_ENTRIES) {
					continue;
				}
			} else {
				List<Entry> top = undoStack.peek();
				if (top != null && top.size() > MAX_UNDO_ENTRIES) {
					undoStack.clear();
					return;
				}
				break;
			}
		}
	}

	private int totalEntries() {
		int total = 0;
		for (List<Entry> ops : undoStack) {
			total += ops.size();
		}
		return total;
	}

	public List<Entry> popUndo() {
		return undoStack.isEmpty() ? null : undoStack.pop();
	}

	public List<Entry> newEntries() {
		return new ArrayList<>();
	}
}
