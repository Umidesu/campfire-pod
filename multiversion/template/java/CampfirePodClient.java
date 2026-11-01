package com.deniz.campfirepod;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.world.level.block.Block;
//#if v>=(26,1)
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
//#else
//$import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
//#endif
//#if v>=(26,1)
//#elif v>=(1,21,6)
//$import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
//$import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
//#else
//$import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
//$import net.minecraft.client.renderer.RenderType;
//#endif

public class CampfirePodClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        Block[] pods = ModBlocks.PODS.toArray(new Block[0]);
        //#if v>=(26,1)
        // 26.1+: the chunk render layer is chosen per quad from sprite transparency, so the (translucent)
        // water_still quads render translucent automatically; no registration exists or is needed.
        BlockColorRegistry.register(List.of(new BlockTintSource() {
            @Override public int color(BlockState state) { return state.getValue(CampfirePodBlock.PURIFIED) ? 0xFF7FC8FF : 0xFF3F76E4; }
            @Override public Set<Property<?>> relevantProperties() { return Set.of(CampfirePodBlock.PURIFIED); }
        }), pods);
        //#else
        //#if v>=(1,21,6)
        //$BlockRenderLayerMap.putBlocks(ChunkSectionLayer.TRANSLUCENT, pods);
        //#else
        //$BlockRenderLayerMap.INSTANCE.putBlocks(RenderType.translucent(), pods);
        //#endif
        //$ColorProviderRegistry.BLOCK.register((state, world, pos, tint) ->
        //$    tint == 0 ? (state.getValue(CampfirePodBlock.PURIFIED) ? 0x7FC8FF : 0x3F76E4) : -1, pods);
        //#endif
    }
}
