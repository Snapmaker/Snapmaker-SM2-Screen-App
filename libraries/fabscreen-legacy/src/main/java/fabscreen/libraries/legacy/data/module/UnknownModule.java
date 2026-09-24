package fabscreen.libraries.legacy.data.module;

import fabscreen.libraries.legacy.data.ISlaveComputer;

public class UnknownModule extends SnapModule {
   public UnknownModule(ISlaveComputer slaveComputer, int moduleType) {
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
