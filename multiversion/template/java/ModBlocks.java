package com.deniz.campfirepod;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
//#if v>=(26,1)
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
//#else
//$import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
//#endif

public class ModBlocks {
    /** All pod blocks in log order (oak, spruce, birch, ...). */
    public static final List<Block> PODS = new ArrayList<>();
    public static final List<Item> POD_ITEMS = new ArrayList<>();

    public static final Block OAK_CAMPFIRE_POD = register("oak_campfire_pod", MapColor.WOOD, SoundType.WOOD);
    /** Spruce variant keeps the original id so existing worlds keep their pods. */
    public static final Block CAMPFIRE_POD = register("campfire_pod", MapColor.PODZOL, SoundType.WOOD);
    public static final Block BIRCH_CAMPFIRE_POD = register("birch_campfire_pod", MapColor.SAND, SoundType.WOOD);
    public static final Block JUNGLE_CAMPFIRE_POD = register("jungle_campfire_pod", MapColor.DIRT, SoundType.WOOD);
    public static final Block ACACIA_CAMPFIRE_POD = register("acacia_campfire_pod", MapColor.COLOR_ORANGE, SoundType.WOOD);
    public static final Block DARK_OAK_CAMPFIRE_POD = register("dark_oak_campfire_pod", MapColor.COLOR_BROWN, SoundType.WOOD);
    public static final Block MANGROVE_CAMPFIRE_POD = register("mangrove_campfire_pod", MapColor.COLOR_RED, SoundType.WOOD);
    public static final Block CHERRY_CAMPFIRE_POD = register("cherry_campfire_pod", MapColor.TERRACOTTA_WHITE, SoundType.CHERRY_WOOD);
    public static final Block CRIMSON_CAMPFIRE_POD = register("crimson_campfire_pod", MapColor.CRIMSON_STEM, SoundType.NETHER_WOOD);
    public static final Block WARPED_CAMPFIRE_POD = register("warped_campfire_pod", MapColor.WARPED_STEM, SoundType.NETHER_WOOD);

    private static Block register(String name, MapColor color, SoundType sounds) {
        var id = CampfirePodBlock.id("campfire_pod", name);
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of().mapColor(color).instrument(NoteBlockInstrument.BASS)
            .strength(2.5f).sound(sounds).noOcclusion()
            .isSuffocating((s, l, p) -> false)
            //#if v>=(26,3)
            .isViewBlocking((s, l, p, box) -> false);
            //#else
            //$.isViewBlocking((s, l, p) -> false);
            //#endif
        Item.Properties itemProps = new Item.Properties();
        //#if v>=(1,21,2)
        props = props.setId(ResourceKey.create(Registries.BLOCK, id));
        itemProps = itemProps.setId(ResourceKey.create(Registries.ITEM, id)).useBlockDescriptionPrefix();
        //#endif
        Block block = Registry.register(BuiltInRegistries.BLOCK, id, new CampfirePodBlock(props));
        POD_ITEMS.add(Registry.register(BuiltInRegistries.ITEM, id, new BlockItem(block, itemProps)));
        PODS.add(block);
        return block;
    }

    public static void init() {
        //#if v>=(26,1)
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(e -> POD_ITEMS.forEach(e::accept));
        //#else
        //$ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(e -> POD_ITEMS.forEach(e::accept));
        //#endif
    }
}
