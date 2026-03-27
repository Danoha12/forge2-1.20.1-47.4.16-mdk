package com.trolmastercard.sexmod.world;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Ported from dw.java (1.12.2 - 1.20.1)
 * A fire block variant that does NOT spread - used by the sexmod for special fire effects.
 *
 * 1.12.2 extended BlockFire; in 1.20.1 we extend BaseFireBlock (since FireBlock is sealed).
 * Registration now uses DeferredRegister.
 */
public class SexmodFireBlock extends BaseFireBlock {

    public static final DeferredRegister<net.minecraft.world.level.block.Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, "sexmod");

    public static final RegistryObject<SexmodFireBlock> INSTANCE = BLOCKS.register(
            "fire", () -> new SexmodFireBlock(Properties.copy(Blocks.FIRE))
    );

    protected SexmodFireBlock(Properties properties) {
        super(properties, 1.0f);
    }

    /** Override to suppress fire spread entirely. */
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // no-op: this fire does not spread
    }

    @Override
    protected boolean canBurnOnTop(LevelReader level, BlockPos pos) {
        return true;
    }

    /** Call from your mod's FMLCommonSetupEvent or mod constructor to register the block. */
    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
