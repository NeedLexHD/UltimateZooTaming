package com.lex3d.ultimatezootaming.ai;

import com.lex3d.ultimatezootaming.blocks.ZooPathBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

/**
 * Navigation "de parc" : le pathfinding PREFERE les allees. Marcher hors chemin
 * recoit un malus, donc visiteurs et employes suivent naturellement les allees
 * que TU construis — slabs (dalles) et Allee de zoo — sans marquage manuel.
 * Ils ne coupent a travers la pelouse que si c'est le seul passage.
 */
public class ZooPathNavigation extends GroundPathNavigation {

    private static final float OFF_PATH_MALUS = 3.0f;

    public ZooPathNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        this.nodeEvaluator = new PathAwareEvaluator();
        this.nodeEvaluator.setCanPassDoors(true);
        return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
    }

    /** Ce bloc est-il une allee ? (dalle, ESCALIER, Allee de zoo, ou bloc configure) */
    public static boolean isPath(BlockState state) {
        if (state.getBlock() instanceof SlabBlock
                || state.getBlock() instanceof net.minecraft.world.level.block.StairBlock
                || state.getBlock() instanceof ZooPathBlock) return true;
        var id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (id == null) return false;
        var list = com.lex3d.ultimatezootaming.config.ZooServerConfig.PATH_BLOCKS.get();
        return list != null && list.contains(id.toString());
    }

    /** Alourdit le cout des cases marchables hors allee, SANS changer leur type
     *  (donc pas de comportement "porte" qui fait sautiller les mobs). */
    private static class PathAwareEvaluator extends WalkNodeEvaluator {
        @Override
        public Node getNode(int x, int y, int z) {
            Node node = super.getNode(x, y, z);
            if (node != null && (node.type == BlockPathTypes.OPEN
                    || node.type == BlockPathTypes.WALKABLE)) {
                BlockState below = this.level.getBlockState(new BlockPos(x, y - 1, z));
                BlockState here = this.level.getBlockState(new BlockPos(x, y, z));
                if (!isPath(below) && !isPath(here)) {
                    node.costMalus += OFF_PATH_MALUS;
                }
            }
            return node;
        }
    }
}
