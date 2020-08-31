package original.com.intellij.rt.coverage.data;

public class BranchData {
   private final int myTotalBranches;
   private final int myCoveredBranches;

   public BranchData(int totalBranches, int touchedBranches) {
      this.myTotalBranches = totalBranches;
      this.myCoveredBranches = touchedBranches;
   }

   public int getTotalBranches() {
      return this.myTotalBranches;
   }

   public int getCoveredBranches() {
      return this.myCoveredBranches;
   }
}
