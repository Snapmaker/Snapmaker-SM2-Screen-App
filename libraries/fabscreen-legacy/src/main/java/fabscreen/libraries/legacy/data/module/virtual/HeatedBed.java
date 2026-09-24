package fabscreen.libraries.legacy.data.module.virtual;

import fabscreen.libraries.legacy.data.ISlaveComputer;
import fabscreen.libraries.legacy.data.module.SnapModule;

public class HeatedBed extends SnapModule {
   public HeatedBed(ISlaveComputer slaveComputer, int moduleType) {
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
