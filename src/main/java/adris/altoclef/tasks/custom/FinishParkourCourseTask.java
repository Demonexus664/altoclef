package adris.altoclef.tasks.custom;

import adris.altoclef.AltoClef;
import adris.altoclef.movement.AdvancedParkourModule;
import adris.altoclef.tasks.movement.GetWithinRangeOfBlockTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import adris.altoclef.util.time.TimerGame;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FinishParkourCourseTask extends Task {

    private final List<BlockPos> checkpoints;
    private final int touchRadius;
    private final TimerGame legTimeout;
    private final MovementProgressChecker progressChecker = new MovementProgressChecker();
    private int currentIndex;
    private Task currentMovement;
    private boolean fallbackEnabled;

    public FinishParkourCourseTask(List<BlockPos> checkpoints, int touchRadius, double legTimeoutSeconds) {
        this.checkpoints = new ArrayList<>(checkpoints);
        this.touchRadius = touchRadius;
        this.legTimeout = new TimerGame(legTimeoutSeconds);
    }

    @Override
    protected void onStart() {
        currentIndex = 0;
        fallbackEnabled = false;
        currentMovement = null;
        progressChecker.reset();
        legTimeout.reset();
    }

    @Override
    protected Task onTick() {
        AltoClef mod = AltoClef.getInstance();
        if (currentIndex >= checkpoints.size()) {
            setDebugState("Course complete");
            return null;
        }

        BlockPos target = checkpoints.get(currentIndex);
        if (isAtCheckpoint(mod, target)) {
            currentIndex++;
            fallbackEnabled = false;
            currentMovement = null;
            progressChecker.reset();
            legTimeout.reset();
            setDebugState(String.format(Locale.ROOT, "Checkpoint %d/%d reached", currentIndex, checkpoints.size()));
            return null;
        }

        boolean injected = false;
        AdvancedParkourModule module = mod.getAdvancedParkourModule();
        if (module != null && module.isEnabled()) {
            injected = module.tryInjectNextSegment();
        }

        if (!progressChecker.check(mod)) {
            fallbackEnabled = true;
        }
        if (legTimeout.elapsed()) {
            fallbackEnabled = true;
        }

        if (currentMovement == null || fallbackEnabled) {
            currentMovement = new GetWithinRangeOfBlockTask(target, touchRadius);
            fallbackEnabled = false;
            progressChecker.reset();
            legTimeout.reset();
        }

        if (injected) {
            setDebugState(String.format(Locale.ROOT, "Parkour segment towards checkpoint %d/%d", currentIndex + 1, checkpoints.size()));
        } else {
            setDebugState(String.format(Locale.ROOT, "Navigating to checkpoint %d/%d", currentIndex + 1, checkpoints.size()));
        }

        return currentMovement;
    }

    @Override
    public boolean isFinished() {
        return currentIndex >= checkpoints.size();
    }

    @Override
    protected void onStop(Task interruptTask) {
        currentMovement = null;
        fallbackEnabled = false;
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof FinishParkourCourseTask task) {
            return task.checkpoints.equals(checkpoints) && task.touchRadius == touchRadius;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "FinishParkourCourse";
    }

    private boolean isAtCheckpoint(AltoClef mod, BlockPos checkpoint) {
        if (mod.getPlayer() == null) {
            return false;
        }
        Vec3d playerPos = mod.getPlayer().getPos();
        Vec3d target = Vec3d.ofCenter(checkpoint);
        double radiusSq = touchRadius * touchRadius;
        return playerPos.squaredDistanceTo(target) <= radiusSq;
    }
}
