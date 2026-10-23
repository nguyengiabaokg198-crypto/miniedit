package com.miniedit;

import com.miniedit.commands.MiniEditCommands;
import com.miniedit.networking.MiniEditNetworking;
import com.miniedit.registry.ModItems;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MiniEdit implements ModInitializer {
	public static final String MOD_ID = "miniedit";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModItems.init();
		MiniEditNetworking.init();
		MiniEditCommands.init();
		LOGGER.info("[MiniEdit] initialized: selection axe + building panel ready");
	}
}
