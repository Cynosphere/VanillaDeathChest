/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 TheRandomLabs
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.therandomlabs.vanilladeathchest.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.therandomlabs.vanilladeathchest.VDCConfig;
import com.therandomlabs.vanilladeathchest.VanillaDeathChest;
import com.therandomlabs.vanilladeathchest.deathchest.DeathChest;
import com.therandomlabs.vanilladeathchest.util.DropsList;
import com.therandomlabs.vanilladeathchest.world.DeathChestsState;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("ConstantConditions")
@Mixin(value = LivingEntity.class, priority = Integer.MAX_VALUE)
public abstract class LivingEntityMixin implements DropsList {
	@Unique
	private final List<ItemEntity> drops = new ArrayList<>();

	@Unique
	private PlayerInventory inventory;

	@Unique
	private UUID deathChestPlayer;

	@Unique
	private BlockPos deathChestPos;

	@Override
	public List<ItemEntity> getDrops() {
		return drops;
	}

	@Inject(method = "drop", at = @At("HEAD"))
	public void dropHead(CallbackInfo info) {
		if ((Object) this instanceof PlayerEntity) {
			drops.clear();
			inventory = new PlayerInventory(null);
			((PlayerEntity) (Object) this).inventory.clone(inventory);
		}
	}

	@Inject(method = "drop", at = @At("TAIL"))
	public void dropTail(CallbackInfo info) {
		if (drops.isEmpty()) {
			return;
		}

		final LivingEntity entity = (LivingEntity) (Object) this;
		final ServerWorld world = (ServerWorld) entity.getEntityWorld();

		if (VanillaDeathChest.SPAWN_DEATH_CHESTS != null &&
				!world.getGameRules().getBoolean(VanillaDeathChest.SPAWN_DEATH_CHESTS)) {
			return;
		}

		final DeathChestsState deathChestsState = DeathChestsState.get(world);
		deathChestsState.getQueuedDeathChests().add(new DeathChest(
				world, entity.getUuid(), drops, inventory, world.getTime(), entity.getBlockPos(),
				false, true
		));
		deathChestsState.markDirty();
	}

	@Inject(method = "tick", at = @At("HEAD"))
	public void tick(CallbackInfo info) {
		if (deathChestPlayer == null) {
			return;
		}

		final LivingEntity entity = (LivingEntity) (Object) this;
		final PlayerEntity player = entity.getEntityWorld().getPlayerByUuid(deathChestPlayer);

		if ((Object) this instanceof MobEntity) {
			((MobEntity) (Object) this).setTarget(player);
		}

		if (this instanceof Angerable) {
			final Angerable angerable = (Angerable) this;
			angerable.setTarget(player);
			angerable.setAngerTime(Integer.MAX_VALUE);
		}

		final VDCConfig.DefenseEntities config = VanillaDeathChest.config().defenseEntities;

		if (config.maxSquaredDistanceFromChest == 0.0) {
			return;
		}

		final double squaredDistanceFromChest =
				deathChestPos.getSquaredDistance(entity.getPos(), true);

		if (squaredDistanceFromChest > config.maxSquaredDistanceFromChest) {
			final double squaredDistanceFromPlayer = player == null ?
					Double.MAX_VALUE : entity.getPos().squaredDistanceTo(player.getPos());

			if (config.maxSquaredDistanceFromPlayer == 0.0 ||
					squaredDistanceFromPlayer > config.maxSquaredDistanceFromPlayer) {
				entity.setPos(
						deathChestPos.getX() + 0.5,
						deathChestPos.getY() + 1.0,
						deathChestPos.getZ() + 0.5
				);
			}
		}
	}

	@Inject(method = "writeCustomDataToTag", at = @At("HEAD"))
	public void writeCustomDataToTag(CompoundTag tag, CallbackInfo info) {
		if (deathChestPlayer != null) {
			tag.put("DeathChestPlayer", NbtHelper.fromUuid(deathChestPlayer));
			tag.put("DeathChestPos", NbtHelper.fromBlockPos(deathChestPos));
		}
	}

	@Inject(method = "readCustomDataFromTag", at = @At("HEAD"))
	public void readCustomDataFromTag(CompoundTag tag, CallbackInfo info) {
		if (tag.contains("DeathChestPlayer")) {
			deathChestPlayer = NbtHelper.toUuid(tag.getCompound("DeathChestPlayer"));
			deathChestPos = NbtHelper.toBlockPos(tag.getCompound("DeathChestPos"));
		}
	}

	@Inject(method = "dropLoot", at = @At("HEAD"), cancellable = true)
	public void dropLoot(DamageSource source, boolean recentlyHit, CallbackInfo info) {
		if (deathChestPlayer != null && !VanillaDeathChest.config().defenseEntities.dropItems) {
			info.cancel();
		}
	}

	@Inject(method = "dropEquipment", at = @At("HEAD"), cancellable = true)
	public void dropEquipment(
			DamageSource source, int lootingModifier, boolean recentlyHit, CallbackInfo info
	) {
		if (deathChestPlayer != null && !VanillaDeathChest.config().defenseEntities.dropItems) {
			info.cancel();
		}
	}

	@Inject(method = "dropXp", at = @At("HEAD"), cancellable = true)
	public void dropXp(CallbackInfo info) {
		if (deathChestPlayer != null &&
				!VanillaDeathChest.config().defenseEntities.dropExperience) {
			info.cancel();
		}
	}
}
