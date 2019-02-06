package com.therandomlabs.vanilladeathchest.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.therandomlabs.vanilladeathchest.config.VDCConfig;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;

public final class VDCReloadCommand {
	public static void register(CommandDispatcher<CommandSource> dispatcher) {
		dispatcher.register(Commands.literal("vdcreload").
				requires(source -> source.hasPermissionLevel(4)).
				executes(context -> execute(context.getSource())));
	}

	public static int execute(CommandSource source) {
		VDCConfig.reload();

		final MinecraftServer server = source.getServer();

		if(server != null && server.isDedicatedServer()) {
			source.sendFeedback(
					new TextComponentString("VanillaDeathChest configuration reloaded!"),
					true
			);
		} else {
			//noinspection NoTranslation
			source.sendFeedback(
					new TextComponentTranslation("commands.vdcreloadclient.success"),
					true
			);
		}

		return Command.SINGLE_SUCCESS;
	}
}