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

import java.util.function.Supplier;

import com.therandomlabs.vanilladeathchest.VanillaDeathChest;
import com.therandomlabs.vanilladeathchest.util.DeathChestBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(Block.class)
public final class BlockMixin {
	@Unique
	private static BlockPos brokenDeathChest;

	@SuppressWarnings("ConstantConditions")
	@Inject(method = "onBreak", at = @At("HEAD"))
	private void onBreak(
			World world, BlockPos pos, BlockState state, PlayerEntity player, CallbackInfo info
	) {
		if ((Object) this instanceof ShulkerBoxBlock || !state.hasBlockEntity()) {
			return;
		}

		final BlockEntity blockEntity = world.getBlockEntity(pos);

		if (blockEntity instanceof DeathChestBlockEntity &&
				((DeathChestBlockEntity) blockEntity).getDeathChest() != null) {
			brokenDeathChest = pos;
		}
	}

	@Inject(
			method = "dropStack(Lnet/minecraft/world/World;Ljava/util/function/Supplier;" +
					"Lnet/minecraft/item/ItemStack;)V",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/World;" +
							"spawnEntity(Lnet/minecraft/entity/Entity;)Z"
			),
			cancellable = true,
			locals = LocalCapture.CAPTURE_FAILHARD
	)
	private static void dropStack(
			World world, Supplier<ItemEntity> itemEntitySupplier, ItemStack stack,
			CallbackInfo callback, ItemEntity itemEntity
	) {
		if (itemEntity.getBlockPos().equals(brokenDeathChest) &&
				!VanillaDeathChest.getConfig().misc.dropDeathChests) {
			brokenDeathChest = null;
			callback.cancel();
		}
	}
}
