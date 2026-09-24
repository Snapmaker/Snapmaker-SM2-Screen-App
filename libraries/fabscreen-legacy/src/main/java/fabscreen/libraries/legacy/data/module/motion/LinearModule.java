package fabscreen.libraries.legacy.data.module.motion;

import fabscreen.libraries.legacy.data.ISlaveComputer;
import fabscreen.libraries.legacy.data.module.SnapModule;

public class LinearModule extends SnapModule {
   public LinearModule(ISlaveComputer slaveComputer, int moduleType) {
      super(slaveComputer, moduleType);
   }

   @Override
   public void init() {

   }

   @Override
   public String getDisplayName() {
      return null;
   }
}
