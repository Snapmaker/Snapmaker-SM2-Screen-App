package fabscreen.libraries.legacy.data.module;


import fabscreen.libraries.legacy.data.ISlaveComputer;

public abstract class ToolHead extends SnapModule {
    private int mToolHeadId = ToolHeadID.HEAD_UNPLUGGED;

    public ToolHead(ISlaveComputer slaveComputer, int moduleType, int toolHeadId) {
        super(slaveComputer, moduleType);
        mToolHeadId = toolHeadId;
    }

    public ToolHead(ISlaveComputer slaveComputer, int moduleType) {
        this(slaveComputer, moduleType, ToolHeadID.HEAD_UNPLUGGED);
    }

    public static boolean isToolHeadModule(Object o) {
       return o instanceof ToolHead;
    }

    public abstract void reset();

    public int getToolHeadId() {
        return mToolHeadId;
    }

    public static class ToolHeadID {
       public static final int HEAD_UNPLUGGED = 0;
       public static final int HEAD_3DP = 1;
       public static final int HEAD_CNC = 2;
       public static final int HEAD_LASER = 3;
       public static final int HEAD_LASER_10W = 4;
       public static final int HEAD_3DP_DUAL_EXTRUDER = 5;
       public static final int HEAD_LASER_20W = 6;
       public static final int HEAD_LASER_40W = 7;
       public static final int HEAD_CNC_200W = 8;
    }
}
