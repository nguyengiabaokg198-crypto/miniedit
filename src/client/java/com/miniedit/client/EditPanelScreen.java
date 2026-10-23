package com.miniedit.client;

import com.miniedit.networking.EditActionPayload;
import com.miniedit.networking.EditStatePayload;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public class EditPanelScreen extends Screen {
	private static final int PANEL_W = 244;
	private static final int PANEL_H = 200;

	private static final String[] MATERIALS = {
		"stone", "cobblestone", "dirt", "grass_block",
		"oak_planks", "spruce_planks", "glass", "bricks",
		"stone_bricks", "sandstone", "oak_log", "oak_leaves",
		"white_concrete", "black_concrete", "redstone_block"
	};

	private static String pos1Text = "(chưa chọn)";
	private static String pos2Text = "(chưa chọn)";
	private static String materialText = "stone";
	private static String statusText = "";

	private int bx;
	private int by;

	public EditPanelScreen() {
		super(Component.literal("MiniEdit"));
	}

	@Override
	protected void init() {
		super.init();
		bx = (this.width - PANEL_W) / 2;
		by = Math.max(10, (this.height - PANEL_H) / 2);

		// Material grid: 4 columns x 4 rows (15 blocks + "held block" button)
		for (int i = 0; i < 16; i++) {
			int col = i % 4;
			int row = i / 4;
			int x = bx + 6 + (col * 59);
			int y = by + 58 + (row * 19);
			if (i < MATERIALS.length) {
				final String mat = MATERIALS[i];
				this.addRenderableWidget(Button.builder(
					Component.literal(shortName(mat)),
					b -> sendAction(EditActionPayload.OP_MATERIAL_SET, mat))
					.bounds(x, y, 56, 17).build());
			} else {
				this.addRenderableWidget(Button.builder(
					Component.literal("Tay đang cầm"),
					b -> sendAction(EditActionPayload.OP_MATERIAL_HELD, ""))
					.bounds(x, y, 56, 17).build());
			}
		}

		// Action row
		int ay = by + 58 + (4 * 19) + 5;
		String[] labels = {"Đặt", "Tường", "Copy", "Dán", "Undo"};
		byte[] ops = {
			EditActionPayload.OP_SET, EditActionPayload.OP_WALLS,
			EditActionPayload.OP_COPY, EditActionPayload.OP_PASTE,
			EditActionPayload.OP_UNDO
		};
		for (int i = 0; i < ops.length; i++) {
			final byte op = ops[i];
			this.addRenderableWidget(Button.builder(
				Component.literal(labels[i]),
				b -> sendAction(op, ""))
				.bounds(bx + 6 + (i * 46), ay, 44, 17).build());
		}

		// Refresh (also closes/reopens to sync state)
		this.addRenderableWidget(Button.builder(
			Component.literal("Làm mới"),
			b -> sendAction(EditActionPayload.OP_REQUEST_STATE, ""))
			.bounds(bx + 6, ay + 21, 44, 16).build());

		sendAction(EditActionPayload.OP_REQUEST_STATE, "");
	}

	private static String shortName(String id) {
		int idx = id.indexOf(':');
		return idx >= 0 ? id.substring(idx + 1) : id;
	}

	private static void sendAction(byte op, String material) {
		ClientPlayNetworking.send(new EditActionPayload(op, material));
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		g.fill(0, 0, this.width, this.height, 0x66000000);
		g.fill(bx, by, bx + PANEL_W, by + PANEL_H, 0xF0121626);
		g.renderOutline(bx, by, PANEL_W, PANEL_H, 0xFFFFC93C);
		super.render(g, mouseX, mouseY, delta);

		g.drawCenteredString(this.font, "MINIEDIT", bx + PANEL_W / 2, by + 8, 0xFFFFC93C);
		g.drawString(this.font, "Góc 1: " + pos1Text, bx + 8, by + 22, 0xFFECECEC);
		g.drawString(this.font, "Góc 2: " + pos2Text, bx + 8, by + 34, 0xFFECECEC);
		g.drawString(this.font, "Vật liệu: " + materialText, bx + 8, by + 46, 0xFF9FE7FF);
		g.drawString(this.font, statusText, bx + 128, by + 46, 0xFF37D06C);
		g.drawString(this.font, "Chọn 2 góc bằng Rìu, rồi bấm nút",
			bx + 8, by + PANEL_H - 12, 0xFF90A0B5);
	}

	public static void handleState(EditStatePayload p) {
		pos1Text = p.pos1Set() ? "(" + p.x1() + ", " + p.y1() + ", " + p.z1() + ")" : "(chưa chọn)";
		pos2Text = p.pos2Set() ? "(" + p.x2() + ", " + p.y2() + ", " + p.z2() + ")" : "(chưa chọn)";
		materialText = p.material();
		statusText = (p.hasCopy() ? "Copy OK" : "Copy -- ") + "  " + (p.hasUndo() ? "Undo OK" : "Undo --");
		if (p.open()) {
			Minecraft.getInstance().setScreen(new EditPanelScreen());
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
