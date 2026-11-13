package com.retrotech.blockentity;

import com.retrotech.block.HorizontalMachineBlock;
import com.retrotech.energy.EnergyTier;
import com.retrotech.menu.MachineKind;
import com.retrotech.menu.MachineMenu;
import com.retrotech.pipe.PipeBlockEntity;
import com.retrotech.registry.ModBlockEntities;
import com.retrotech.registry.ModBlocks;
import com.retrotech.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Quarry al estilo BuildCraft.
 *
 * 1. Construye un marco de 11x11 detrás de ella (el frente mira hacia el jugador que la colocó).
 * 2. Excava el área interior de 9x9, capa por capa, hasta el fondo del mundo. No toca fluidos ni bedrock.
 * 3. Manda lo que saca a tuberías o cofres pegados; si no hay, lo tira arriba de la quarry.
 *
 * Necesita energía (motores, generadores o cables): 40 EU + 20 EU por punto de dureza por bloque.
 */
public class QuarryBlockEntity extends MachineBlockEntity {
    public static final int PHASE_FRAME = 0;
    public static final int PHASE_MINE = 1;
    public static final int PHASE_DONE = 2;

    private static final int SIZE = 9;
    private static final int FRAME_COST = 25;
    private static final ItemStack TOOL = new ItemStack(Items.DIAMOND_PICKAXE);

    private int phase = PHASE_FRAME;
    private int cursor;
    private int mineY = Integer.MIN_VALUE;
    private int cooldown;
    @Nullable
    private BlockPos target;

    public QuarryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.QUARRY.get(), pos, state, MachineKind.QUARRY, EnergyTier.MV, 10000, 128, 0);
    }

    // ---------- Geometría del área ----------

    private Direction back() {
        return getBlockState().getValue(HorizontalMachineBlock.FACING).getOpposite();
    }

    private Direction side() {
        return back().getClockWise();
    }

    /** Posición del marco (i, j de 0 a 10). */
    private BlockPos framePos(int i, int j) {
        return worldPosition.relative(back(), 1 + i).relative(side(), j - 5);
    }

    /** Posición dentro del área a excavar (i, j de 0 a 8). */
    private BlockPos innerPos(int i, int j, int y) {
        BlockPos p = worldPosition.relative(back(), 2 + i).relative(side(), j - 4);
        return new BlockPos(p.getX(), y, p.getZ());
    }

    /** Rectángulo interior (para el dibujo del taladro). */
    public AABB getInnerArea() {
        BlockPos a = innerPos(0, 0, worldPosition.getY());
        BlockPos b = innerPos(SIZE - 1, SIZE - 1, worldPosition.getY());
        return new AABB(Math.min(a.getX(), b.getX()), a.getY(), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()), a.getY(), Math.max(a.getZ(), b.getZ()));
    }

    public int getPhase() {
        return phase;
    }

    @Nullable
    public BlockPos getTarget() {
        return target;
    }

    // ---------- Lógica ----------

    @Override
    public void serverTick() {
        if (level == null) return;
        if (mineY == Integer.MIN_VALUE) mineY = worldPosition.getY();

        boolean working = false;
        if (cooldown > 0) {
            cooldown--;
            working = phase != PHASE_DONE;
        } else if (phase == PHASE_FRAME) {
            working = buildFrame();
        } else if (phase == PHASE_MINE) {
            working = mine();
        }

        progress = mineY;
        progressMax = phase;
        setLit(working);
    }

    private boolean buildFrame() {
        int total = 11 * 11;
        while (cursor < total) {
            int i = cursor / 11, j = cursor % 11;
            if (i != 0 && i != 10 && j != 0 && j != 10) {
                cursor++;
                continue;
            }
            BlockPos p = framePos(i, j);
            BlockState state = level.getBlockState(p);
            if (state.is(ModBlocks.QUARRY_FRAME.get())) {
                cursor++;
                continue;
            }
            if (!state.canBeReplaced()) {
                // Hay algo en el camino: lo saca primero
                if (state.getDestroySpeed(level, p) < 0) {
                    cursor++;
                    continue;
                }
                int cost = mineCost(state, p);
                if (energy.getEnergyStored() < cost) return false;
                energy.consume(cost);
                breakBlock(p, state);
                cooldown = 2;
                return true;
            }
            if (energy.getEnergyStored() < FRAME_COST) return false;
            energy.consume(FRAME_COST);
            level.setBlock(p, ModBlocks.QUARRY_FRAME.get().defaultBlockState(), Block.UPDATE_ALL);
            cursor++;
            cooldown = 2;
            return true;
        }
        phase = PHASE_MINE;
        cursor = 0;
        mineY = worldPosition.getY();
        setChanged();
        return true;
    }

    private boolean mine() {
        // Busca el próximo bloque a romper (saltar aire y fluidos no cuesta energía)
        for (int checked = 0; checked < SIZE * SIZE * 2; checked++) {
            if (cursor >= SIZE * SIZE) {
                cursor = 0;
                mineY--;
                if (mineY < level.getMinBuildHeight()) {
                    phase = PHASE_DONE;
                    target = null;
                    syncToClient();
                    return false;
                }
            }
            int i = cursor / SIZE;
            int j = (i % 2 == 0) ? cursor % SIZE : SIZE - 1 - cursor % SIZE; // recorrido en zigzag
            BlockPos p = innerPos(i, j, mineY);
            BlockState state = level.getBlockState(p);
            if (!shouldMine(state, p)) {
                cursor++;
                continue;
            }

            if (!p.equals(target)) {
                target = p;
                syncToClient();
            }
            int cost = mineCost(state, p);
            if (energy.getEnergyStored() < cost) return false;
            energy.consume(cost);
            breakBlock(p, state);
            cursor++;
            cooldown = 3;
            setChanged();
            return true;
        }
        setChanged();
        return true;
    }

    private boolean shouldMine(BlockState state, BlockPos p) {
        if (state.isAir()) return false;
        if (state.getBlock() instanceof LiquidBlock) return false;
        if (state.is(ModBlocks.QUARRY_FRAME.get())) return false;
        if (p.equals(worldPosition)) return false;
        return state.getDestroySpeed(level, p) >= 0;
    }

    private int mineCost(BlockState state, BlockPos p) {
        return 40 + (int) (Math.max(0, state.getDestroySpeed(level, p)) * 20);
    }

    private void breakBlock(BlockPos p, BlockState state) {
        if (!(level instanceof ServerLevel server)) return;
        BlockEntity be = level.getBlockEntity(p);
        List<ItemStack> drops = Block.getDrops(state, server, p, be, null, TOOL);
        level.destroyBlock(p, false);
        for (ItemStack drop : drops) output(drop);
    }

    /** Manda el item a una tubería pegada, después a un inventario (no máquinas), y si no, lo tira arriba. */
    private void output(ItemStack stack) {
        for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {
            for (Direction dir : Direction.values()) {
                BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(dir));
                if (neighbor == null) continue;
                boolean isPipe = neighbor instanceof PipeBlockEntity;
                if (pass == 0 && !isPipe) continue;
                if (pass == 1 && (isPipe || neighbor instanceof MachineBlockEntity)) continue;
                IItemHandler handler = neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite())
                        .orElse(null);
                if (handler == null) continue;
                stack = ItemHandlerHelper.insertItemStacked(handler, stack, false);
                if (stack.isEmpty()) return;
            }
        }
        if (!stack.isEmpty()) {
            ItemEntity entity = new ItemEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.2,
                    worldPosition.getZ() + 0.5, stack);
            entity.setDeltaMovement(0, 0.1, 0);
            level.addFreshEntity(entity);
        }
    }

    @Override
    protected boolean canReceiveFrom(@Nullable Direction side) {
        return true;
    }

    @Override
    protected boolean canExtractFrom(@Nullable Direction side) {
        return false;
    }

    @Override
    protected MenuType<MachineMenu> getMenuType() {
        return ModMenus.QUARRY.get();
    }

    // ---------- Guardado y sincronización ----------

    private void syncToClient() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Phase", phase);
        tag.putInt("Cursor", cursor);
        tag.putInt("MineY", mineY);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        phase = tag.getInt("Phase");
        cursor = tag.getInt("Cursor");
        mineY = tag.contains("MineY") ? tag.getInt("MineY") : Integer.MIN_VALUE;
        target = tag.contains("Target") ? BlockPos.of(tag.getLong("Target")) : null;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Phase", phase);
        if (target != null) tag.putLong("Target", target.asLong());
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }
}
