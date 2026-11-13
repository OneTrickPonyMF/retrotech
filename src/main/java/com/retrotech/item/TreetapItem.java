package com.retrotech.item;

import com.retrotech.block.RubberLogBlock;
import com.retrotech.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Grifo: click derecho sobre la mancha húmeda de un tronco de caucho para sacar 1 a 3 de resina.
 */
public class TreetapItem extends Item {
    public TreetapItem(Properties properties) {
        super(properties.durability(16));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Direction face = context.getClickedFace();

        if (!(state.getBlock() instanceof RubberLogBlock)
                || state.getValue(RubberLogBlock.RESIN) != RubberLogBlock.Resin.WET
                || face != state.getValue(RubberLogBlock.FACING)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(RubberLogBlock.RESIN, RubberLogBlock.Resin.DRY), Block.UPDATE_ALL);
            int amount = 1 + level.random.nextInt(3);
            Block.popResourceFromFace(level, pos, face, new ItemStack(ModItems.STICKY_RESIN.get(), amount));
            level.playSound(null, pos, SoundEvents.SLIME_BLOCK_PLACE, SoundSource.BLOCKS, 0.8F, 1.2F);

            Player player = context.getPlayer();
            if (player != null) {
                context.getItemInHand().hurtAndBreak(1, player, p -> p.broadcastBreakEvent(context.getHand()));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
