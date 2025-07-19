package net.dawson.adorablehamsterpets.mixin.accessor;

import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Invoker interface to grant access to the protected static method
 * {@code getCommonNodeType} in {@link LandPathNodeMaker}.
 * This allows the use of vanilla's internal logic for determining a block's
 * pathfinding type without altering any vanilla code.
 */
@Mixin(LandPathNodeMaker.class)
public interface LandPathNodeMakerInvoker {

    /**
     * Invokes the protected static method {@code getCommonNodeType}.
     * @param world The world view.
     * @param pos The position to check.
     * @return The determined PathNodeType.
     */
    @Invoker("getCommonNodeType")
    static PathNodeType callGetCommonNodeType(BlockView world, BlockPos pos) {
        // This is a Mixin Invoker; the body is empty and will be implemented at runtime.
        throw new AssertionError();
    }

    /**
     * Invokes the protected static method {@code findFleePosition}. This is used by goals
     * to find a suitable random position away from a target.
     * @param entity The entity that is fleeing.
     * @param xRange The horizontal range for the search.
     * @param yRange The vertical range for the search.
     * @param targetPos The position to flee from.
     * @return A Vec3d representing a safe flee position, or null if none was found.
     */
    @Nullable
    @Invoker("findFleePosition")
    static Vec3d callFindFleePosition(PathAwareEntity entity, int xRange, int yRange, Vec3d targetPos) {
        // This is a Mixin Invoker; the body is empty and will be implemented at runtime.
        throw new AssertionError();
    }
}