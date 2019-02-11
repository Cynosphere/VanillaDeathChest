package com.therandomlabs.vanilladeathchest.listener;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import com.google.common.collect.Queues;
import com.therandomlabs.vanilladeathchest.api.listener.PlayerDropAllItemsListener;
import com.therandomlabs.vanilladeathchest.config.VDCConfig;
import com.therandomlabs.vanilladeathchest.util.DeathChestPlacer;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.dimdev.rift.listener.ServerTickable;

public class DeathChestPlaceHandler implements PlayerDropAllItemsListener, ServerTickable {
	private static final Map<DimensionType, Queue<DeathChestPlacer>> PLACERS =
			new EnumMap<>(DimensionType.class);

	@Override
	public boolean onPlayerDropAllItems(World world, EntityPlayer player, List<EntityItem> drops) {
		if(drops.isEmpty()) {
			return true;
		}

		final GameRules gameRules = world.getGameRules();

		if(gameRules.getBoolean("keepInventory") || (!VDCConfig.misc.gameRuleName.isEmpty() &&
				!gameRules.getBoolean(VDCConfig.misc.gameRuleName))) {
			return true;
		}

		final Queue<DeathChestPlacer> placers = getPlacers(world);
		placers.add(new DeathChestPlacer(world, player, drops));

		return false;
	}

	@Override
	public void serverTick(MinecraftServer server) {
		for(World world : server.worlds) {
			worldTick(world);
		}
	}

	public static Queue<DeathChestPlacer> getPlacers(World world) {
		synchronized(PLACERS) {
			return PLACERS.computeIfAbsent(
					world.dimension.getType(),
					key -> Queues.newConcurrentLinkedQueue()
			);
		}
	}

	private static void worldTick(World world) {
		final Queue<DeathChestPlacer> placers = getPlacers(world);
		final List<DeathChestPlacer> toReadd = new ArrayList<>();
		DeathChestPlacer placer;

		while((placer = placers.poll()) != null) {
			if(!placer.run()) {
				toReadd.add(placer);
			}
		}

		placers.addAll(toReadd);
	}
}