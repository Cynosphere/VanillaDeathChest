package com.therandomlabs.vanilladeathchest.api.deathchest;

import java.util.Map;
import java.util.UUID;
import com.therandomlabs.vanilladeathchest.api.listener.DeathChestRemoveListener;
import com.therandomlabs.vanilladeathchest.world.storage.VDCSavedData;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.dimdev.riftloader.RiftLoader;

public final class DeathChestManager {
	private DeathChestManager() {}

	public static void addDeathChest(World world, UUID playerID, long creationTime,
			BlockPos pos, boolean isDoubleChest) {
		final VDCSavedData data = VDCSavedData.get(world);
		final Map<BlockPos, DeathChest> deathChests = data.getDeathChests();
		final DeathChest deathChest = new DeathChest(playerID, creationTime, pos, isDoubleChest);

		deathChests.put(pos, deathChest);

		if(isDoubleChest) {
			deathChests.put(pos.east(), deathChest);
		}

		data.markDirty();
	}

	public static boolean isDeathChest(World world, BlockPos pos) {
		return getDeathChest(world, pos) != null;
	}

	public static DeathChest getDeathChest(World world, BlockPos pos) {
		if(world.getBlockState(pos).getBlock() != Blocks.CHEST) {
			return null;
		}

		final Map<BlockPos, DeathChest> deathChests = VDCSavedData.get(world).getDeathChests();
		DeathChest deathChest = deathChests.get(pos);

		if(deathChest == null) {
			return null;
		}

		return deathChest;
	}

	public static DeathChest removeDeathChest(World world, BlockPos pos) {
		final Map<BlockPos, DeathChest> deathChests = VDCSavedData.get(world).getDeathChests();
		final DeathChest chest = deathChests.remove(pos);

		final BlockPos west;
		final BlockPos east;

		if(chest.isDoubleChest()) {
			if(chest.getPos().equals(pos)) {
				west = pos;
				east = pos.east();

				deathChests.remove(east);
			} else {
				west = pos.west();
				east = pos;

				deathChests.remove(west);
			}
		} else {
			west = pos;
			east = null;
		}

		for(DeathChestRemoveListener listener :
				RiftLoader.instance.getListeners(DeathChestRemoveListener.class)) {
			listener.onDeathChestRemove(chest, west, east);
		}

		return chest;
	}
}