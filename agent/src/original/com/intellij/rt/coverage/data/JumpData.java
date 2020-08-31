package original.com.intellij.rt.coverage.data;

import original.com.intellij.rt.coverage.util.CoverageIOUtil;
import java.io.DataOutputStream;
import java.io.IOException;

public class JumpData implements CoverageData {
   private int myTrueHits;
   private int myFalseHits;

   public void touchTrueHit() {
      ++this.myTrueHits;
   }

   public void touchFalseHit() {
      ++this.myFalseHits;
   }

   public int getTrueHits() {
      return this.myTrueHits;
   }

   public int getFalseHits() {
      return this.myFalseHits;
   }

   public void save(DataOutputStream os) throws IOException {
      CoverageIOUtil.writeINT(os, this.myTrueHits);
      CoverageIOUtil.writeINT(os, this.myFalseHits);
   }

   public void merge(CoverageData data) {
      JumpData jumpData = (JumpData)data;
      this.myTrueHits += jumpData.myTrueHits;
      this.myFalseHits += jumpData.myFalseHits;
   }

   public void setTrueHits(int trueHits) {
      this.myTrueHits = trueHits;
   }

   public void setFalseHits(int falseHits) {
      this.myFalseHits = falseHits;
   }
}
