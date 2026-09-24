package fabscreen.libraries.legacy.data.module;

import androidx.annotation.NonNull;

import fabscreen.libraries.legacy.data.ISlaveComputer;

public abstract class SnapModule {
   private int mModuleType = ModuleType.MODULE_UNKNOWN;

   protected ISlaveComputer mSlaveComputer;

   public SnapModule(ISlaveComputer slaveComputer, int moduleType) {
      mSlaveComputer = slaveComputer;
      mModuleType = moduleType;
   }

   public abstract void init();

   public int getModuleType() {
      return mModuleType;
   }

   public abstract String getDisplayName();

   @NonNull
   @Override
   public String toString() {
      return "Module: " + getDisplayName();
   }

   public static class ModuleType {
      public static final int MODULE_UNKNOWN = -1;

      public static final int TOOL_HEAD_3DP = 0;
      public static final int TOOL_HEAD_CNC = 1;
      public static final int TOOL_HEAD_LASER = 2;

      public static final int MOTION_LINEAR_MODULE_TBS_2019 = 3;

      public static final int ADDON_ENCLOSURE_LED = 4;
      public static final int ADDON_ENCLOSURE_BODY = 5;

      public static final int ADDON_MOTION_ROTARY_MODULE_2020 = 6;

      public static final int ADDON_AIR_PURIFIER = 7;
      public static final int ADDON_EMERGENCY_STOP_MODULE_2021 = 8;

      public static final int ADDON_INNER_CNC_CALIBRATION = 9;
      public static final int ADDON_ORIGINAL_TO_SM2_0_TOOL_HEAD_CONVERTER = 10;
      public static final int ADDON_ENCLOSURE_FAN = 11;

      public static final int MOTION_LINEAR_MODULE_TMC_2021 = 12;

      public static final int TOOL_HEAD_DUAL_EXTRUSION_3DP_MODULE = 13;
      public static final int TOOL_HEAD_LASER_10W = 14;
      public static final int TOOL_HEAD_CNC_200W = 15;

      public static final int ADDON_ENCLOSURE_ARTISAN_2022 = 16;
      public static final int ADDON_DRY_BOX = 17;
      public static final int ADDON_DUAL_EXTRUSION_CALIBRATOR = 18;

      public static final int TOOL_HEAD_LASER_20W = 19;
      public static final int TOOL_HEAD_LASER_40W = 20;

      public static final int MOTION_ROTARY_MODULE_2023 = 21;

      public static final int MOTION_GALVO_SYSTEM = 22;

      public static final int TOOL_HEAD_LASER_INFRARED_2W_2023 = 23;

      public static final int VIRTUAL_ADDON_SM2_0_HEATED_BED_2019 = 512;

      public static final int VIRTUAL_EXTEND_SM2_0_QUICK_SWAP_KIT = 519;

      public static final int VIRTUAL_EXTEND_SM2_0_BRACING_KIT = 522;

   }
}
