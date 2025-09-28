package adris.altoclef.movement;

import adris.altoclef.AltoClef;
import adris.altoclef.Config;
import adris.altoclef.control.InputControls;
import adris.altoclef.control.SlotHandler;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.Slot;
import adris.altoclef.util.time.TimerGame;
import baritone.api.pathing.calc.IPath;
import baritone.api.utils.BetterBlockPos;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.TwistingVinesBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.block.WeepingVinesBlock;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class AdvancedParkourModule {

    private static final int MAX_PREVIEW_NODES = 6;

    private final AltoClef mod;
    private final Config config;
    private final List<ParkourSegment> segments;
    private ActiveSegment activeSegment;
    private final TimerGame segmentCooldown = new TimerGame(0.2);

    public AdvancedParkourModule(AltoClef mod, Config config) {
        this.mod = mod;
        this.config = config;
        this.segments = Arrays.asList(
                new SprintJumpSegment(),
                new HeadHitterSegment(),
                new LadderGrabSegment(),
                new GapWithPlacementSegment()
        );
        segmentCooldown.reset();
    }

    public boolean isEnabled() {
        return config.parkourMode != ParkourMode.OFF;
    }

    public void setEnabled(boolean enabled) {
        if (enabled) {
            if (config.parkourMode == ParkourMode.OFF) {
                config.parkourMode = ParkourMode.BASIC;
            }
        } else {
            config.parkourMode = ParkourMode.OFF;
            cancelActiveSegment();
        }
    }

    public boolean tryInjectNextSegment() {
        if (!isEnabled()) {
            cancelActiveSegment();
            return false;
        }
        if (!AltoClef.inGame() || mod.getPlayer() == null) {
            cancelActiveSegment();
            return false;
        }
        if (activeSegment != null) {
            if (activeSegment.tick()) {
                return true;
            }
            cancelActiveSegment();
        }
        if (!segmentCooldown.elapsed()) {
            return false;
        }
        Optional<PathPreview> previewOpt = buildPreview();
        if (previewOpt.isEmpty()) {
            return false;
        }
        PathPreview preview = previewOpt.get();
        for (ParkourSegment segment : segments) {
            if (!segment.isAvailable(config.parkourMode)) {
                continue;
            }
            if (segment.matches(preview)) {
                ActiveSegment created = segment.create(preview);
                if (created != null) {
                    activeSegment = created;
                    activeSegment.tick();
                    segmentCooldown.reset();
                    return true;
                }
            }
        }
        return false;
    }

    private void cancelActiveSegment() {
        if (activeSegment != null) {
            activeSegment.stop();
            activeSegment = null;
        }
    }

    private Optional<PathPreview> buildPreview() {
        Optional<IPath> pathOptional = mod.getClientBaritone().getPathingBehavior().getPath();
        if (pathOptional.isEmpty()) {
            return Optional.empty();
        }
        IPath path = pathOptional.get();
        List<BetterBlockPos> positions = path.positions();
        if (positions.isEmpty()) {
            return Optional.empty();
        }
        ClientPlayerEntity player = mod.getPlayer();
        if (player == null) {
            return Optional.empty();
        }
        BlockPos playerPos = player.getBlockPos();
        int closestIndex = 0;
        double closestSq = Double.MAX_VALUE;
        for (int i = 0; i < positions.size(); i++) {
            BetterBlockPos node = positions.get(i);
            double distSq = playerPos.getSquaredDistance(node);
            if (distSq < closestSq) {
                closestSq = distSq;
                closestIndex = i;
            }
        }
        List<BlockPos> upcoming = new ArrayList<>();
        for (int i = closestIndex + 1; i < Math.min(positions.size(), closestIndex + MAX_PREVIEW_NODES); i++) {
            upcoming.add(new BlockPos(positions.get(i)));
        }
        if (upcoming.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new PathPreview(playerPos, upcoming));
    }

    private static Iterable<BlockPos> iterateTowards(BlockPos start, BlockPos end) {
        List<BlockPos> result = new ArrayList<>();
        int stepX = Integer.compare(end.getX(), start.getX());
        int stepY = Integer.compare(end.getY(), start.getY());
        int stepZ = Integer.compare(end.getZ(), start.getZ());
        BlockPos cursor = start;
        while (!cursor.equals(end)) {
            cursor = cursor.add(stepX, stepY, stepZ);
            result.add(cursor);
        }
        return result;
    }

    private float yawTo(BlockPos target) {
        ClientPlayerEntity player = mod.getPlayer();
        if (player == null) {
            return 0.0f;
        }
        Vec3d eye = player.getEyePos();
        double dx = target.getX() + 0.5 - eye.x;
        double dz = target.getZ() + 0.5 - eye.z;
        return (float) (MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(dz, dx)) - 90.0));
    }

    private float pitchTo(BlockPos target) {
        ClientPlayerEntity player = mod.getPlayer();
        if (player == null) {
            return 0.0f;
        }
        Vec3d eye = player.getEyePos();
        double dx = target.getX() + 0.5 - eye.x;
        double dz = target.getZ() + 0.5 - eye.z;
        double dy = target.getY() + 0.1 - eye.y;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        return (float) MathHelper.wrapDegrees(-(float) Math.toDegrees(Math.atan2(dy, horizontal)));
    }

    private abstract class ParkourSegment {
        private final ParkourMode requiredMode;

        protected ParkourSegment(ParkourMode requiredMode) {
            this.requiredMode = requiredMode;
        }

        boolean isAvailable(ParkourMode currentMode) {
            return currentMode.ordinal() >= requiredMode.ordinal();
        }

        abstract boolean matches(PathPreview preview);

        abstract ActiveSegment create(PathPreview preview);
    }

    private static class PathPreview {
        private final BlockPos current;
        private final List<BlockPos> upcoming;

        private PathPreview(BlockPos current, List<BlockPos> upcoming) {
            this.current = current;
            this.upcoming = upcoming;
        }

        BlockPos getCurrent() {
            return current;
        }

        BlockPos getNext() {
            return upcoming.get(0);
        }

        BlockPos getLast() {
            return upcoming.get(upcoming.size() - 1);
        }

        List<BlockPos> getUpcoming() {
            return upcoming;
        }

        double horizontalDistanceTo(BlockPos target) {
            double dx = target.getX() - current.getX();
            double dz = target.getZ() - current.getZ();
            return Math.sqrt(dx * dx + dz * dz);
        }
    }

    private abstract static class ActiveSegment {
        private boolean started;
        private boolean stopped;

        boolean tick() {
            if (!started) {
                onStart();
                started = true;
            }
            if (stopped) {
                return false;
            }
            boolean keepRunning = onTick();
            if (!keepRunning) {
                stop();
            }
            return !stopped;
        }

        void stop() {
            if (!stopped) {
                onStop();
                stopped = true;
            }
        }

        protected abstract void onStart();

        protected abstract boolean onTick();

        protected abstract void onStop();
    }

    private class SprintJumpSegment extends ParkourSegment {
        private static final int SEGMENT_TICKS = 6;

        SprintJumpSegment() {
            super(ParkourMode.BASIC);
        }

        @Override
        boolean matches(PathPreview preview) {
            BlockPos next = preview.getNext();
            double horizontal = preview.horizontalDistanceTo(next);
            int elevation = next.getY() - preview.getCurrent().getY();
            if (horizontal < 1.5 || horizontal > 3.5) {
                if (!(horizontal >= 1.0 && horizontal <= 1.5 && elevation == 1)) {
                    return false;
                }
            }
            if (!isLandingSafe(next)) {
                return false;
            }
            return isGapTraversable(preview.getCurrent(), next);
        }

        @Override
        ActiveSegment create(PathPreview preview) {
            return new ActiveSegment() {
                private int ticks = SEGMENT_TICKS;

                @Override
                protected void onStart() {
                    InputControls controls = mod.getInputControls();
                    controls.hold(baritone.api.utils.input.Input.MOVE_FORWARD);
                    controls.hold(baritone.api.utils.input.Input.SPRINT);
                    float yaw = yawTo(preview.getNext());
                    float pitch = pitchTo(preview.getNext());
                    if (config.smoothingEnabled) {
                        MovementSmoothing.rotateSmooth(yaw, pitch, 120);
                        MovementSmoothing.sleepJitter(config.smoothingMinJitterMs, config.smoothingMaxJitterMs);
                        int delay = MovementSmoothing.consumePendingJitterTicks();
                        MovementSmoothing.schedule(() -> controls.tryPress(baritone.api.utils.input.Input.JUMP), delay);
                    } else {
                        mod.getInputControls().tryPress(baritone.api.utils.input.Input.JUMP);
                        mod.getInputControls().forceLook(yaw, pitch);
                    }
                }

                @Override
                protected boolean onTick() {
                    InputControls controls = mod.getInputControls();
                    controls.hold(baritone.api.utils.input.Input.MOVE_FORWARD);
                    controls.hold(baritone.api.utils.input.Input.SPRINT);
                    ticks--;
                    return ticks > 0;
                }

                @Override
                protected void onStop() {
                    InputControls controls = mod.getInputControls();
                    controls.release(baritone.api.utils.input.Input.MOVE_FORWARD);
                    controls.release(baritone.api.utils.input.Input.SPRINT);
                }
            };
        }
    }

    private class HeadHitterSegment extends ParkourSegment {
        private static final int SEGMENT_TICKS = 8;

        HeadHitterSegment() {
            super(ParkourMode.BASIC);
        }

        @Override
        boolean matches(PathPreview preview) {
            BlockPos current = preview.getCurrent();
            BlockPos next = preview.getNext();
            if (next.getY() != current.getY()) {
                return false;
            }
            for (BlockPos step : iterateTowards(current, next)) {
                BlockPos head = step.up();
                BlockPos ceiling = head.up();
                BlockState headState = mod.getWorld().getBlockState(head);
                BlockState ceilingState = mod.getWorld().getBlockState(ceiling);
                if (!headState.isAir()) {
                    return false;
                }
                if (ceilingState.isAir()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        ActiveSegment create(PathPreview preview) {
            return new ActiveSegment() {
                private int ticks = SEGMENT_TICKS;

                @Override
                protected void onStart() {
                    InputControls controls = mod.getInputControls();
                    controls.hold(baritone.api.utils.input.Input.MOVE_FORWARD);
                    controls.hold(baritone.api.utils.input.Input.SPRINT);
                    float yaw = yawTo(preview.getNext());
                    if (config.smoothingEnabled) {
                        MovementSmoothing.rotateSmooth(yaw, mod.getPlayer().getPitch(), 100);
                        MovementSmoothing.sleepJitter(config.smoothingMinJitterMs, config.smoothingMaxJitterMs);
                        int delay = MovementSmoothing.consumePendingJitterTicks();
                        MovementSmoothing.schedule(() -> controls.tryPress(baritone.api.utils.input.Input.JUMP), delay);
                    } else {
                        mod.getInputControls().forceLook(yaw, mod.getPlayer().getPitch());
                        controls.tryPress(baritone.api.utils.input.Input.JUMP);
                    }
                }

                @Override
                protected boolean onTick() {
                    InputControls controls = mod.getInputControls();
                    controls.hold(baritone.api.utils.input.Input.MOVE_FORWARD);
                    controls.hold(baritone.api.utils.input.Input.SPRINT);
                    controls.hold(baritone.api.utils.input.Input.JUMP);
                    ticks--;
                    return ticks > 0;
                }

                @Override
                protected void onStop() {
                    InputControls controls = mod.getInputControls();
                    controls.release(baritone.api.utils.input.Input.MOVE_FORWARD);
                    controls.release(baritone.api.utils.input.Input.SPRINT);
                    controls.release(baritone.api.utils.input.Input.JUMP);
                }
            };
        }
    }

    private class LadderGrabSegment extends ParkourSegment {
        private static final int SEGMENT_TICKS = 12;

        LadderGrabSegment() {
            super(ParkourMode.ADVANCED);
        }

        @Override
        boolean matches(PathPreview preview) {
            for (BlockPos pos : preview.getUpcoming()) {
                if (isClimbable(pos)) {
                    return true;
                }
                for (BlockPos offset : horizontalNeighbors(pos)) {
                    if (isClimbable(offset)) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        ActiveSegment create(PathPreview preview) {
            BlockPos target = preview.getNext();
            return new ActiveSegment() {
                private int ticks = SEGMENT_TICKS;

                @Override
                protected void onStart() {
                    InputControls controls = mod.getInputControls();
                    controls.hold(baritone.api.utils.input.Input.MOVE_FORWARD);
                    controls.hold(baritone.api.utils.input.Input.SPRINT);
                    float yaw = yawTo(target);
                    float pitch = -30.0f;
                    if (config.smoothingEnabled) {
                        MovementSmoothing.rotateSmooth(yaw, pitch, 120);
                    } else {
                        mod.getInputControls().forceLook(yaw, pitch);
                    }
                    controls.tryPress(baritone.api.utils.input.Input.JUMP);
                }

                @Override
                protected boolean onTick() {
                    InputControls controls = mod.getInputControls();
                    controls.hold(baritone.api.utils.input.Input.MOVE_FORWARD);
                    controls.hold(baritone.api.utils.input.Input.SPRINT);
                    controls.hold(baritone.api.utils.input.Input.JUMP);
                    controls.hold(baritone.api.utils.input.Input.SNEAK);
                    ticks--;
                    return ticks > 0;
                }

                @Override
                protected void onStop() {
                    InputControls controls = mod.getInputControls();
                    controls.release(baritone.api.utils.input.Input.MOVE_FORWARD);
                    controls.release(baritone.api.utils.input.Input.SPRINT);
                    controls.release(baritone.api.utils.input.Input.JUMP);
                    controls.release(baritone.api.utils.input.Input.SNEAK);
                }
            };
        }

        private List<BlockPos> horizontalNeighbors(BlockPos pos) {
            return List.of(pos.north(), pos.south(), pos.east(), pos.west());
        }

        private boolean isClimbable(BlockPos pos) {
            Block block = mod.getWorld().getBlockState(pos).getBlock();
            return block instanceof LadderBlock || block instanceof VineBlock
                    || block instanceof TwistingVinesBlock || block instanceof WeepingVinesBlock;
        }
    }

    private class GapWithPlacementSegment extends ParkourSegment {
        private static final int SEGMENT_TICKS = 12;

        GapWithPlacementSegment() {
            super(ParkourMode.ADVANCED);
        }

        @Override
        boolean matches(PathPreview preview) {
            BlockPos next = preview.getNext();
            double horizontal = preview.horizontalDistanceTo(next);
            if (horizontal < 3.5 || horizontal > 4.5) {
                return false;
            }
            if (!isLandingSafe(next)) {
                return false;
            }
            if (!isGapTraversable(preview.getCurrent(), next)) {
                return false;
            }
            return getHotbarBlock().isPresent();
        }

        @Override
        ActiveSegment create(PathPreview preview) {
            Optional<HotbarSelection> hotbar = getHotbarBlock();
            if (hotbar.isEmpty()) {
                return null;
            }
            BlockPos landing = preview.getNext();
            HotbarSelection selection = hotbar.get();
            return new ActiveSegment() {
                private int ticks = SEGMENT_TICKS;
                private final int previousSlot = mod.getPlayer().getInventory().selectedSlot;

                @Override
                protected void onStart() {
                    InputControls controls = mod.getInputControls();
                    controls.hold(baritone.api.utils.input.Input.MOVE_FORWARD);
                    controls.hold(baritone.api.utils.input.Input.SPRINT);
                    selectHotbarSlot(selection);
                    float yaw = yawTo(landing);
                    float pitch = 70.0f;
                    if (config.smoothingEnabled) {
                        MovementSmoothing.rotateSmooth(yaw, pitch, 140);
                        MovementSmoothing.sleepJitter(config.smoothingMinJitterMs, config.smoothingMaxJitterMs);
                        int delay = MovementSmoothing.consumePendingJitterTicks();
                        MovementSmoothing.schedule(() -> controls.tryPress(baritone.api.utils.input.Input.JUMP), delay);
                        MovementSmoothing.schedule(() -> controls.tryPress(baritone.api.utils.input.Input.CLICK_RIGHT), delay + 2);
                    } else {
                        mod.getInputControls().forceLook(yaw, pitch);
                        controls.tryPress(baritone.api.utils.input.Input.JUMP);
                        MovementSmoothing.schedule(() -> controls.tryPress(baritone.api.utils.input.Input.CLICK_RIGHT), 2);
                    }
                }

                @Override
                protected boolean onTick() {
                    InputControls controls = mod.getInputControls();
                    controls.hold(baritone.api.utils.input.Input.MOVE_FORWARD);
                    controls.hold(baritone.api.utils.input.Input.SPRINT);
                    ticks--;
                    return ticks > 0;
                }

                @Override
                protected void onStop() {
                    InputControls controls = mod.getInputControls();
                    controls.release(baritone.api.utils.input.Input.MOVE_FORWARD);
                    controls.release(baritone.api.utils.input.Input.SPRINT);
                    mod.getPlayer().getInventory().selectedSlot = previousSlot;
                }
            };
        }

        private Optional<HotbarSelection> getHotbarBlock() {
            Optional<Slot> hotbarSlot = StorageHelper.getSlotWithThrowawayBlock(mod, true);
            if (hotbarSlot.isPresent()) {
                Slot slot = hotbarSlot.get();
                ItemStack stack = StorageHelper.getItemStackInSlot(slot);
                if (stack.getItem() instanceof BlockItem) {
                    return Optional.of(new HotbarSelection(slot.getInventorySlot(), stack.getItem()));
                }
            }
            Optional<Slot> anySlot = StorageHelper.getSlotWithThrowawayBlock(mod, false);
            if (anySlot.isPresent()) {
                ItemStack stack = StorageHelper.getItemStackInSlot(anySlot.get());
                Item item = stack.getItem();
                if (item instanceof BlockItem) {
                    return Optional.of(new HotbarSelection(-1, item));
                }
            }
            return Optional.empty();
        }

        private void selectHotbarSlot(HotbarSelection selection) {
            if (selection.hotbarSlot >= 0 && selection.hotbarSlot < 9) {
                mod.getPlayer().getInventory().selectedSlot = selection.hotbarSlot;
            } else {
                SlotHandler slotHandler = mod.getSlotHandler();
                slotHandler.forceEquipItem(selection.item);
            }
        }
    }

    private boolean isLandingSafe(BlockPos pos) {
        BlockState below = mod.getWorld().getBlockState(pos.down());
        return below.isSolidBlock(mod.getWorld(), pos.down());
    }

    private boolean isGapTraversable(BlockPos start, BlockPos end) {
        for (BlockPos step : iterateTowards(start, end)) {
            if (step.equals(end)) {
                break;
            }
            BlockState state = mod.getWorld().getBlockState(step);
            if (!state.isAir()) {
                return false;
            }
        }
        return true;
    }

    private static class HotbarSelection {
        private final int hotbarSlot;
        private final Item item;

        private HotbarSelection(int hotbarSlot, Item item) {
            this.hotbarSlot = hotbarSlot;
            this.item = item;
        }
    }
}
