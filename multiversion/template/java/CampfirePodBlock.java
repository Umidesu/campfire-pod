package com.deniz.campfirepod;

import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.world.item.alchemy.Potions;
//#if v>=(1,20,5)
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.PotionContents;
//#else
//$import net.minecraft.world.item.alchemy.PotionUtils;
//#endif
//#if v>=(1,20,3) and v<(26,3)
import com.mojang.serialization.MapCodec;
//#endif
//#if v>=(1,21,11)
import net.minecraft.resources.Identifier;
//#else
//$import net.minecraft.resources.ResourceLocation;
//#endif
//#if v>=(1,21,2)
import net.minecraft.world.level.ScheduledTickAccess;
//#elif v>=(1,20,5)
//$import net.minecraft.world.ItemInteractionResult;
//#endif

public class CampfirePodBlock extends Block {
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 3);
    public static final BooleanProperty PURIFIED = BooleanProperty.create("purified");
    //#if v>=(1,21,2)
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    //#else
    //$public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    //#endif
    public static final int BOIL_TICKS = 200;
    static final String[] TIERS = {"leather", "copper", "iron", "gold", "diamond", "netherite"};

    static final VoxelShape SHAPE = Stream.of(
            Block.box(-8, 11, 6, 24, 15, 10),
            Block.box(3, -5, 3, 13, 3, 13)).reduce(Shapes.empty(), Shapes::or).optimize();
    // Collision = bar + pot only (legs have no collision), shifted down 16.
    static final VoxelShape COLLISION = SHAPE;

    static VoxelShape rotate(VoxelShape shape, Direction facing) {
        int turns = switch (facing) { case EAST -> 1; case SOUTH -> 2; case WEST -> 3; default -> 0; };
        VoxelShape[] out = {Shapes.empty()};
        shape.forAllBoxes((x1, y1, z1, x2, y2, z2) -> {
            double ax = x1, az = z1, bx = x2, bz = z2;
            for (int i = 0; i < turns; i++) { double nax = 1 - bz, nbx = 1 - az; az = ax; bz = bx; ax = nax; bx = nbx; }
            out[0] = Shapes.or(out[0], Shapes.box(ax, y1, az, bx, y2, bz));
        });
        return out[0].optimize();
    }
    static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class), COLLISIONS = new EnumMap<>(Direction.class);
    static { for (Direction d : Direction.Plane.HORIZONTAL) { SHAPES.put(d, rotate(SHAPE, d)); COLLISIONS.put(d, rotate(COLLISION, d)); } }

    //#if v>=(1,20,3) and v<(26,3)
    public static final MapCodec<CampfirePodBlock> CODEC = simpleCodec(CampfirePodBlock::new);
    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    //#endif

    public CampfirePodBlock(BlockBehaviour.Properties s) {
        super(s);
        registerDefaultState(getStateDefinition().any().setValue(LEVEL, 0).setValue(PURIFIED, false).setValue(FACING, Direction.NORTH));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(LEVEL, PURIFIED, FACING); }

    @Override public VoxelShape getShape(BlockState st, BlockGetter w, BlockPos p, CollisionContext c) { return SHAPES.get(st.getValue(FACING)); }
    @Override public VoxelShape getCollisionShape(BlockState st, BlockGetter w, BlockPos p, CollisionContext c) { return COLLISIONS.get(st.getValue(FACING)); }
    //#if v>=(1,21,2)
    @Override public boolean propagatesSkylightDown(BlockState st) { return true; }
    //#else
    //$@Override public boolean propagatesSkylightDown(BlockState st, BlockGetter w, BlockPos p) { return true; }
    //#endif

    // ---- placement ----
    @Override public boolean canSurvive(BlockState st, LevelReader w, BlockPos p) {
        return w.getBlockState(p.below()).is(BlockTags.CAMPFIRES);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState st = defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
        return st.canSurvive(ctx.getLevel(), ctx.getClickedPos()) ? st : null;
    }
    //#if v>=(1,21,2)
    @Override public BlockState updateShape(BlockState st, LevelReader w, ScheduledTickAccess ticks, BlockPos p, Direction dir, BlockPos np, BlockState ns, RandomSource r) {
        if (dir == Direction.DOWN && !st.canSurvive(w, p)) return Blocks.AIR.defaultBlockState();
        if (dir == Direction.DOWN && needsBoil(st, w, p)) ticks.scheduleTick(p, this, BOIL_TICKS);
        return super.updateShape(st, w, ticks, p, dir, np, ns, r);
    }
    //#else
    //$@Override public BlockState updateShape(BlockState st, Direction dir, BlockState ns, LevelAccessor w, BlockPos p, BlockPos np) {
    //$    if (dir == Direction.DOWN && !st.canSurvive(w, p)) return Blocks.AIR.defaultBlockState();
    //$    if (dir == Direction.DOWN && needsBoil(st, w, p)) w.scheduleTick(p, this, BOIL_TICKS);
    //$    return super.updateShape(st, dir, ns, w, p, np);
    //$}
    //#endif

    // ---- boiling ----
    public static boolean litCampfireBelow(BlockGetter w, BlockPos p) {
        BlockState b = w.getBlockState(p.below());
        return b.is(BlockTags.CAMPFIRES) && b.getOptionalValue(BlockStateProperties.LIT).orElse(false);
    }
    static boolean needsBoil(BlockState st, BlockGetter w, BlockPos p) {
        return st.getValue(LEVEL) > 0 && !st.getValue(PURIFIED) && litCampfireBelow(w, p);
    }
    @Override public void onPlace(BlockState st, Level w, BlockPos p, BlockState old, boolean notify) {
        if (needsBoil(st, w, p)) w.scheduleTick(p, this, BOIL_TICKS);
    }
    @Override public void tick(BlockState st, ServerLevel w, BlockPos p, RandomSource r) {
        if (needsBoil(st, w, p)) {
            w.setBlockAndUpdate(p, st.setValue(PURIFIED, true));
            w.playSound(null, p, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.3f, 1.6f);
        }
    }
    @Override public void animateTick(BlockState st, Level w, BlockPos p, RandomSource r) {
        if (!needsBoil(st, w, p)) return;
        double y = p.getY() + (-4 + 2 * st.getValue(LEVEL)) / 16.0;
        for (int i = 0; i < 3; i++)
            w.addParticle(ParticleTypes.BUBBLE_POP, p.getX() + 0.25 + r.nextDouble() * 0.5, y, p.getZ() + 0.25 + r.nextDouble() * 0.5, 0, 0.02, 0);
        if (r.nextInt(4) == 0)
            w.playLocalSound(p.getX() + 0.5, y, p.getZ() + 0.5, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.BLOCKS, 0.4f, 0.9f + r.nextFloat() * 0.2f, false);
    }

    // ---- comparator ----
    @Override public boolean hasAnalogOutputSignal(BlockState st) { return true; }
    //#if v>=(1,21,9)
    @Override public int getAnalogOutputSignal(BlockState st, Level w, BlockPos p, Direction d) { return st.getValue(LEVEL); }
    //#else
    //$@Override public int getAnalogOutputSignal(BlockState st, Level w, BlockPos p) { return st.getValue(LEVEL); }
    //#endif

    // ---- interaction ----
    //#if v>=(1,21,11)
    static Identifier id(String ns, String path) { return Identifier.fromNamespaceAndPath(ns, path); }
    //#elif v>=(1,21)
    //$static ResourceLocation id(String ns, String path) { return ResourceLocation.fromNamespaceAndPath(ns, path); }
    //#else
    //$static ResourceLocation id(String ns, String path) { return new ResourceLocation(ns, path); }
    //#endif
    static Item tan(String path) {
        //#if v>=(1,21,2)
        return BuiltInRegistries.ITEM.getValue(id("toughasnails", path));
        //#else
        //$return BuiltInRegistries.ITEM.get(id("toughasnails", path));
        //#endif
    }
    static boolean isWaterBottle(ItemStack s) {
        //#if v>=(1,20,5)
        return s.is(Items.POTION) && s.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.WATER);
        //#else
        //$return s.is(Items.POTION) && PotionUtils.getPotion(s) == Potions.WATER;
        //#endif
    }
    static ItemStack waterBottle() {
        //#if v>=(1,20,5)
        return PotionContents.createItemStack(Items.POTION, Potions.WATER);
        //#else
        //$return PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER);
        //#endif
    }
    static String itemId(ItemStack s) { return BuiltInRegistries.ITEM.getKey(s.getItem()).toString(); }
    static boolean is(ItemStack s, String tanPath) { return itemId(s).equals("toughasnails:" + tanPath); }
    static boolean client(Level w) {
        //#if v>=(1,21,9)
        return w.isClientSide();
        //#else
        //$return w.isClientSide;
        //#endif
    }

    // Result abstraction: SUCCESS, SKIP (don't fall through to the empty-hand action), PASS (fall through).
    //#if v>=(1,21,2)
    static InteractionResult ok(Level w) { return InteractionResult.SUCCESS; }
    static final InteractionResult SKIP = InteractionResult.PASS, PASS = InteractionResult.TRY_WITH_EMPTY_HAND;
    //#elif v>=(1,20,5)
    //$static ItemInteractionResult ok(Level w) { return ItemInteractionResult.sidedSuccess(client(w)); }
    //$static final ItemInteractionResult SKIP = ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION, PASS = ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    //#else
    //$static InteractionResult ok(Level w) { return InteractionResult.sidedSuccess(client(w)); }
    //$static final InteractionResult SKIP = InteractionResult.PASS, PASS = InteractionResult.PASS;
    //#endif

    //#if v>=(1,21,2)
    private InteractionResult
    //#elif v>=(1,20,5)
    //$private ItemInteractionResult
    //#else
    //$private InteractionResult
    //#endif
    finish(Level w, BlockPos p, Player pl, InteractionHand h, ItemStack in, ItemStack out, BlockState ns, SoundEvent snd, boolean fill) {
        if (!client(w)) {
            Item used = in.getItem();
            pl.setItemInHand(h, ItemUtils.createFilledResult(in, pl, out));
            pl.awardStat(Stats.ITEM_USED.get(used));
            w.setBlockAndUpdate(p, ns);
            w.playSound(null, p, snd, SoundSource.BLOCKS, 1.0f, 1.0f);
            w.gameEvent(null, fill ? GameEvent.FLUID_PLACE : GameEvent.FLUID_PICKUP, p);
        }
        return ok(w);
    }

    //#if v>=(1,21,2)
    @Override public InteractionResult useItemOn(ItemStack s, BlockState st, Level w, BlockPos p, Player pl, InteractionHand h, BlockHitResult hit) {
    //#elif v>=(1,20,5)
    //$@Override public ItemInteractionResult useItemOn(ItemStack s, BlockState st, Level w, BlockPos p, Player pl, InteractionHand h, BlockHitResult hit) {
    //#else
    //$@Override public InteractionResult use(BlockState st, Level w, BlockPos p, Player pl, InteractionHand h, BlockHitResult hit) {
    //$    ItemStack s = pl.getItemInHand(h);
    //#endif
        int lvl = st.getValue(LEVEL); boolean pur = st.getValue(PURIFIED);
        if (s.is(Items.WATER_BUCKET))
            return finish(w, p, pl, h, s, new ItemStack(Items.BUCKET), st.setValue(LEVEL, 3).setValue(PURIFIED, false), SoundEvents.BUCKET_EMPTY, true);
        boolean dirty = is(s, "dirty_water_bottle"), purBottle = is(s, "purified_water_bottle");
        if (isWaterBottle(s) || dirty || purBottle) {
            if (lvl >= 3) return SKIP;
            boolean np = purBottle && (lvl == 0 || pur);
            return finish(w, p, pl, h, s, new ItemStack(Items.GLASS_BOTTLE), st.setValue(LEVEL, lvl + 1).setValue(PURIFIED, np), SoundEvents.BOTTLE_EMPTY, true);
        }
        if (s.is(Items.GLASS_BOTTLE)) {
            if (lvl == 0) return PASS;
            ItemStack out = pur ? new ItemStack(tan("purified_water_bottle")) : waterBottle();
            return finish(w, p, pl, h, s, out, st.setValue(LEVEL, lvl - 1).setValue(PURIFIED, lvl - 1 > 0 && pur), SoundEvents.BOTTLE_FILL, false);
        }
        if (s.is(Items.BUCKET)) {
            if (lvl < 3) return PASS;
            return finish(w, p, pl, h, s, new ItemStack(Items.WATER_BUCKET), st.setValue(LEVEL, 0).setValue(PURIFIED, false), SoundEvents.BUCKET_FILL, false);
        }
        // canteens: one level fills a canteen completely (same as TaN's cauldron / rain collector behaviour)
        String id = itemId(s);
        for (String t : TIERS) {
            boolean empty = id.equals("toughasnails:empty_" + t + "_canteen");
            boolean plain = id.equals("toughasnails:" + t + "_water_canteen");
            boolean purC = id.equals("toughasnails:" + t + "_purified_water_canteen");
            boolean dirtyC = id.equals("toughasnails:" + t + "_dirty_water_canteen");
            if (!(empty || plain || purC || dirtyC)) continue;
            if (lvl == 0) return PASS;
            if (dirtyC || (plain && pur) || (purC && !pur)) return SKIP; // mixed water types
            if (!empty && s.getDamageValue() == 0) return SKIP; // already full
            Item target = tan(t + (pur ? "_purified_water_canteen" : "_water_canteen"));
            //#if v>=(1,20,5)
            ItemStack out = s.transmuteCopy(target, 1);
            //#else
            //$ItemStack out = new ItemStack(target);
            //$if (s.getTag() != null) out.setTag(s.getTag().copy());
            //#endif
            out.setDamageValue(0);
            return finish(w, p, pl, h, s, out, st.setValue(LEVEL, lvl - 1).setValue(PURIFIED, lvl - 1 > 0 && pur), SoundEvents.BOTTLE_FILL, false);
        }
        return PASS;
    }
    @Override public BlockState rotate(BlockState st, Rotation r) { return st.setValue(FACING, r.rotate(st.getValue(FACING))); }
    @Override public BlockState mirror(BlockState st, Mirror m) { return st.rotate(m.getRotation(st.getValue(FACING))); }
}
