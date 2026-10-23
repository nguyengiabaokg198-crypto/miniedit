package com.miniedit.client;

import com.miniedit.networking.EditStatePayload;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

@Environment(EnvType.CLIENT)
public class MiniEditClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(EditStatePayload.TYPE, (payload, ctx) ->
			ctx.client().execute(() -> EditPanelScreen.handleState(payload)));
	}
}
