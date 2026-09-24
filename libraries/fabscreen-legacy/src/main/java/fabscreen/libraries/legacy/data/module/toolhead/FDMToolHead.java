package fabscreen.libraries.legacy.data.module.toolhead;

import fabscreen.libraries.legacy.data.ISlaveComputer;
import fabscreen.libraries.legacy.data.module.ToolHead;

public class FDMToolHead extends ToolHead {

   public FDMToolHead(ISlaveComputer slaveComputer, int moduleType, int toolHeadId) {
      super(slaveComputer, moduleType, toolHeadId);
   }

   @Override
   public void init() {

   }

   @Override
   public String getDisplayName() {
      return null;
   }

   @Override
   public void reset() {

   }
}
