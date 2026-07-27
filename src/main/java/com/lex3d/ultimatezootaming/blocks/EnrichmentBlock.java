package com.lex3d.ultimatezootaming.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JOUET D'ENRICHISSEMENT : de quoi occuper les animaux.
 *
 * Un enclos qui en contient voit le bien-etre de ses pensionnaires monter, et
 * les animaux viennent jouer avec (petits sauts, particules de joie).
 *
 * Quatre modeles, chacun un bloc distinct : ballon, bambou suspendu, branche a
 * grimper, pneu. Le registre par monde evite tout scan de volume : on sait
 * exactement ou ils sont, comme pour les vestiaires et les tresoreries.
 */
public class EnrichmentBlock extends HorizontalDirectionalBlock {

    /** Les differentes formes de jouet, avec leur apport de bien-etre. */
    public enum Kind {
        BALL("ball", 6),        // ballon : plait a presque tout le monde
        BAMBOO("bamboo", 6),    // bambou suspendu
        BRANCH("branch", 6),    // branche a grimper
        TIRE("tire", 6);        // pneu suspendu

        public final String key;
        public final int welfareBonus;
        Kind(String key, int bonus) { this.key = key; this.welfareBonus = bonus; }
    }

    /** Registre des jouets poses, par monde. Aucun scan : on tient la liste a jour. */
    private static final Map<net.minecraft.resources.ResourceKey<Level>, Set<BlockPos>> TOYS =
            new ConcurrentHashMap<>();

    public static Set<BlockPos> getToysIn(Level level) {
        Set<BlockPos> s = TOYS.get(level.dimension());
        return s == null ? Set.of() : s;
    }

    /** Combien de jouets distincts dans cette zone ? Plafonne pour eviter l'abus. */
    public static int countInZone(Level level, com.lex3d.ultimatezootaming.zones.ZooZone zone) {
        int n = 0;
        for (BlockPos p : getToysIn(level)) {
            if (zone.contains(p)) n++;
            if (n >= 4) break; // au-dela de 4, plus de bonus
        }
        return n;
    }

    /** Boite de selection large : les jouets debordent jusqu'aux bords du bloc
     *  (les branches notamment), il faut pouvoir viser leur silhouette entiere. */
    private static final VoxelShape SHAPE = Shapes.box(0.0, 0.0, 0.2, 1.0, 1.0, 0.8);

    private final Kind kind;

    public EnrichmentBlock(Properties p, Kind kind) {
        super(p);
        this.kind = kind;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    public Kind getKind() { return kind; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    /** Traversable : un animal doit pouvoir venir dessus sans rester bloque. */
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (!level.isClientSide()) {
            TOYS.computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet())
                    .add(pos.immutable());
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!level.isClientSide() && !state.is(next.getBlock())) {
            Set<BlockPos> s = TOYS.get(level.dimension());
            if (s != null) s.remove(pos);
        }
        super.onRemove(state, level, pos, next, moving);
    }

    /** Petites particules pour signaler que le jouet est vivant. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(12) != 0) return;
        level.addParticle(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                pos.getX() + 0.3 + random.nextDouble() * 0.4,
                pos.getY() + 0.7,
                pos.getZ() + 0.3 + random.nextDouble() * 0.4,
                0, 0.02, 0);
    }
}
