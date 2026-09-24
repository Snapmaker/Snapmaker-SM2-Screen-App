package fabscreen.libraries.legacy.data.version;


import static fabscreen.libraries.legacy.data.serial.fabpacket.FabPacketContent.*;

import java.util.ArrayList;

import io.reactivex.subjects.BehaviorSubject;

public class VersionRequirementManager {
    private BehaviorSubject<ArrayList<VersionRequirement>> mVersionRequirementSubject = BehaviorSubject.createDefault(new ArrayList<>());

    public VersionRequirementManager() {

        mVersionRequirementSubject.onNext(createDefaultVersionRequirement());
    }

    public ArrayList<VersionRequirement> createDefaultVersionRequirement() {
        // Hardcode version requirements here.
        // TODO: Use string resource to initialize module name instead of hardcode string.
        ArrayList<VersionRequirement> requirements = new ArrayList<>();
        requirements.add(new VersionRequirement(ModuleVersion.TYPE_MODULE_TOOL_HEAD_3DP, "3D Printing Module", "V1.9.2"));
        requirements.add(new VersionRequirement(ModuleVersion.TYPE_MODULE_TOOL_HEAD_CNC, "CNC Module", "V1.9.2"));
        requirements.add(new VersionRequirement(ModuleVersion.TYPE_MODULE_TOOL_HEAD_LASER_1600, "Laser Module", "V1.9.2"));
        requirements.add(new VersionRequirement(ModuleVersion.TYPE_MODULE_LINEAR_MODULE_2_0, "Linear Module", "V1.9.2"));
        requirements.add(new VersionRequirement(ModuleVersion.TYPE_MODULE_ADD_ON_ENCLOSURE, "Enclosure", "V1.9.2"));
        requirements.add(new VersionRequirement(ModuleVersion.TYPE_MODULE_ADD_ON_ROTARY_MODULE, "Rotary Module", "V1.9.2"));
        requirements.add(new VersionRequirement(ModuleVersion.TYPE_MODULE_ADD_ON_EMERGENCY_STOP_BUTTON, "Emergency Stop Button", "V1.9.2"));
        requirements.add(new VersionRequirement(ModuleVersion.TYPE_MODULE_LINEAR_MODULE_2_5, "Linear Module", "V1.9.2"));

        return requirements;
    }

    public VersionRequirement getRequirementByModule(ModuleVersion moduleVersion) {
        for (VersionRequirement versionRequirement : mVersionRequirementSubject.getValue()) {
            if (versionRequirement.type == moduleVersion.moduleType) {
                return versionRequirement;
            }
        }

        // Could not find requirement.
        return null;
    }
}
