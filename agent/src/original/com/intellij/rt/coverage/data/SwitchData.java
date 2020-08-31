package original.com.intellij.rt.coverage.data;

import original.com.intellij.rt.coverage.util.CoverageIOUtil;
import java.io.DataOutputStream;
import java.io.IOException;

public class SwitchData implements CoverageData {
   private int[] myKeys;
   private int myDefaultHits;
   private int[] myHits;

   public SwitchData(int[] keys) {
      this.myKeys = keys;
      this.myHits = new int[keys.length];
   }

   public void touch(int key) {
      if (key == -1) {
         ++this.myDefaultHits;
      } else if (key < this.myHits.length && key >= 0) {
         int var10002 = this.myHits[key]++;
      }

   }

   public int getDefaultHits() {
      return this.myDefaultHits;
   }

   public int[] getHits() {
      return this.myHits;
   }

   public void save(DataOutputStream os) throws IOException {
      CoverageIOUtil.writeINT(os, this.myDefaultHits);
      CoverageIOUtil.writeINT(os, this.myHits.length);

      for(int i = 0; i < this.myHits.length; ++i) {
         CoverageIOUtil.writeINT(os, this.myKeys[i]);
         CoverageIOUtil.writeINT(os, this.myHits[i]);
      }

   }

   public void merge(CoverageData data) {
      SwitchData switchData = (SwitchData)data;
      this.myDefaultHits += switchData.myDefaultHits;

      for(int i = Math.min(this.myHits.length, switchData.myHits.length) - 1; i >= 0; --i) {
         int[] var10000 = this.myHits;
         var10000[i] += switchData.myHits[i];
      }

      if (switchData.myHits.length > this.myHits.length) {
         int[] old = this.myHits;
         this.myHits = new int[switchData.myHits.length];
         System.arraycopy(old, 0, this.myHits, 0, old.length);
         System.arraycopy(switchData.myHits, old.length, this.myHits, old.length, this.myHits.length - old.length);
         this.myKeys = switchData.myKeys;
      }

   }

   public void setDefaultHits(int defaultHits) {
      this.myDefaultHits = defaultHits;
   }

   public void setKeysAndHits(int[] keys, int[] hits) {
      this.myKeys = keys;
      this.myHits = hits;
   }

   public int[] getKeys() {
      return this.myKeys;
   }
}
