package com.snapmaker.fabscreen.data;

import com.snapmaker.fabscreen.FabScreenApplication;

import fabscreen.libraries.legacy.data.ISlaveComputer;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketContent;
import io.reactivex.Observable;


public class XYZMoveController {
    public enum Direction {FORWARD, BACKWARD, LEFT, RIGHT, UP, DOWN}

    private final ISlaveComputer mSlaveComputer;

    private static class XYZMoveControllerHolder {
        private static final XYZMoveController INSTANCE = new XYZMoveController();
    }

    private XYZMoveController() {
        mSlaveComputer = FabScreenApplication.getInstance().getModel().getSlaveComputer();
    }

    public static XYZMoveController getInstance() {
        return XYZMoveControllerHolder.INSTANCE;
    }

    public Observable<FabPacketContent.GcodeResponse> moveByStep(Direction direction, float stepWidth) {
        switch (direction) {
            case FORWARD:
                return mSlaveComputer.sendGcode("G91")
                        .flatMap(res -> mSlaveComputer.sendGcode("G0 Y-" + stepWidth + " F3000"))
                        .flatMap(res -> mSlaveComputer.sendGcode("G90"));
            case BACKWARD:
                return mSlaveComputer.sendGcode("G91")
                        .flatMap(res -> mSlaveComputer.sendGcode("G0 Y+" + stepWidth + " F3000"))
                        .flatMap(res -> mSlaveComputer.sendGcode("G90"));
            case LEFT:
                return mSlaveComputer.sendGcode("G91")
                        .flatMap(res -> mSlaveComputer.sendGcode("G0 X-" + stepWidth + " F3000"))
                        .flatMap(res -> mSlaveComputer.sendGcode("G90"));
            case RIGHT:
                return mSlaveComputer.sendGcode("G91")
                        .flatMap(res -> mSlaveComputer.sendGcode("G0 X+" + stepWidth + " F3000"))
                        .flatMap(res -> mSlaveComputer.sendGcode("G90"));
            case UP:
                return mSlaveComputer.sendGcode("G91")
                        .flatMap(res -> mSlaveComputer.sendGcode("G0 Z+" + stepWidth + " F1800"))
                        .flatMap(res -> mSlaveComputer.sendGcode("G90"));

            case DOWN:
                return mSlaveComputer.sendGcode("G91")
                        .flatMap(res -> mSlaveComputer.sendGcode("G0 Z-" + stepWidth + " F1800"))
                        .flatMap(res -> mSlaveComputer.sendGcode("G90"));
        }
        return null;
    }
}