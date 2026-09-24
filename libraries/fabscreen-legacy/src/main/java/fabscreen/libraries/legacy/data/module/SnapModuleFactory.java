package fabscreen.libraries.legacy.data.module;

import fabscreen.libraries.legacy.data.ISlaveComputer;
import fabscreen.libraries.legacy.data.module.addon.AirPurifier;
import fabscreen.libraries.legacy.data.module.addon.EmergencyStopButton;
import fabscreen.libraries.legacy.data.module.addon.Enclosure;
import fabscreen.libraries.legacy.data.module.motion.LinearModule;
import fabscreen.libraries.legacy.data.module.motion.RotaryModule;
import fabscreen.libraries.legacy.data.module.toolhead.CNCToolHead;
import fabscreen.libraries.legacy.data.module.toolhead.FDMToolHead;
import fabscreen.libraries.legacy.data.module.toolhead.LaserToolHead;
import fabscreen.libraries.legacy.data.module.virtual.BracingKit;
import fabscreen.libraries.legacy.data.module.virtual.HeatedBed;
import fabscreen.libraries.legacy.data.module.virtual.QuickSwapKit;
import fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketContent;

public class SnapModuleFactory {
   private static SnapModule create(ISlaveComputer slaveComputer, FabPacketContent.MachineStatus status, FabPacketContent.ModuleVersion moduleVersion) {
      SnapModule module;
      switch (moduleVersion.moduleType) {
         case SnapModule.ModuleType.TOOL_HEAD_3DP:
         case SnapModule.ModuleType.TOOL_HEAD_DUAL_EXTRUSION_3DP_MODULE:
            module = new FDMToolHead(slaveComputer, moduleVersion.moduleType, status.headStatus);
            break;
         case SnapModule.ModuleType.TOOL_HEAD_LASER:
         case SnapModule.ModuleType.TOOL_HEAD_LASER_10W:
         case SnapModule.ModuleType.TOOL_HEAD_LASER_20W:
         case SnapModule.ModuleType.TOOL_HEAD_LASER_40W:
         case SnapModule.ModuleType.TOOL_HEAD_LASER_INFRARED_2W_2023:
            module = new LaserToolHead(slaveComputer, moduleVersion.moduleType, status.headStatus);
            break;
         case SnapModule.ModuleType.TOOL_HEAD_CNC:
         case SnapModule.ModuleType.TOOL_HEAD_CNC_200W:
            module = new CNCToolHead(slaveComputer, moduleVersion.moduleType, status.headStatus);
            break;
         case SnapModule.ModuleType.MOTION_LINEAR_MODULE_TBS_2019:
         case SnapModule.ModuleType.MOTION_LINEAR_MODULE_TMC_2021:
            module = new LinearModule(slaveComputer, moduleVersion.moduleType);
            break;
         case SnapModule.ModuleType.ADDON_MOTION_ROTARY_MODULE_2020:
            module = new RotaryModule(slaveComputer, moduleVersion.moduleType);
            break;
         case SnapModule.ModuleType.ADDON_ENCLOSURE_BODY:
            module = new Enclosure(slaveComputer, moduleVersion.moduleType);
            break;
         case SnapModule.ModuleType.ADDON_AIR_PURIFIER:
            module = new AirPurifier(slaveComputer, moduleVersion.moduleType);
            break;
         case SnapModule.ModuleType.ADDON_EMERGENCY_STOP_MODULE_2021:
            module = new EmergencyStopButton(slaveComputer, moduleVersion.moduleType);
            break;
         case SnapModule.ModuleType.VIRTUAL_EXTEND_SM2_0_QUICK_SWAP_KIT:
            module = new QuickSwapKit(slaveComputer, moduleVersion.moduleType);
            break;
         case SnapModule.ModuleType.VIRTUAL_EXTEND_SM2_0_BRACING_KIT:
            module = new BracingKit(slaveComputer, moduleVersion.moduleType);
            break;
         case SnapModule.ModuleType.VIRTUAL_ADDON_SM2_0_HEATED_BED_2019:
            module = new HeatedBed(slaveComputer, moduleVersion.moduleType);
            break;
         case SnapModule.ModuleType.MODULE_UNKNOWN:
         default:
            module = new UnknownModule(slaveComputer, moduleVersion.moduleType);
            break;
      }
      module.init();
      return module;
   }
}
