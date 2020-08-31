package original.com.intellij.rt.coverage.data;

import original.com.intellij.rt.coverage.util.CoverageIOUtil;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JumpsAndSwitches implements CoverageData {
   private List<JumpData> myJumps;
   private JumpData[] myJumpsArray;
   private List<SwitchData> mySwitches;
   private SwitchData[] mySwitchesArray;

   public JumpData[] getJumps() {
      return this.myJumpsArray;
   }

   public SwitchData[] getSwitches() {
      return this.mySwitchesArray;
   }

   public JumpData addJump(int jump) {
      if (this.myJumps == null) {
         this.myJumps = new ArrayList();
      }

      if (this.myJumps.size() <= jump) {
         for(int i = this.myJumps.size(); i <= jump; ++i) {
            this.myJumps.add(new JumpData());
         }
      }

      return (JumpData)this.myJumps.get(jump);
   }

   public JumpData getJumpData(int jump) {
      return this.myJumpsArray == null ? null : this.myJumpsArray[jump];
   }

   public SwitchData addSwitch(int switchNumber, int[] keys) {
      if (this.mySwitches == null) {
         this.mySwitches = new ArrayList();
      }

      SwitchData switchData = new SwitchData(keys);
      if (this.mySwitches.size() <= switchNumber) {
         for(int i = this.mySwitches.size(); i < switchNumber; ++i) {
            this.mySwitches.add(new SwitchData(new int[0]));
         }

         if (this.mySwitches.size() == switchNumber) {
            this.mySwitches.add(switchData);
         }
      }

      return (SwitchData)this.mySwitches.get(switchNumber);
   }

   public SwitchData getSwitchData(int switchNumber) {
      return this.mySwitchesArray == null ? null : this.mySwitchesArray[switchNumber];
   }

   public void save(DataOutputStream os) throws IOException {
      CoverageIOUtil.writeINT(os, this.myJumpsArray != null ? this.myJumpsArray.length : 0);
      int var3;
      int var4;
      if (this.myJumpsArray != null) {
         JumpData[] var2 = this.myJumpsArray;
         var3 = var2.length;

         for(var4 = 0; var4 < var3; ++var4) {
            JumpData aMyJumpsArray = var2[var4];
            aMyJumpsArray.save(os);
         }
      }

      CoverageIOUtil.writeINT(os, this.mySwitchesArray != null ? this.mySwitchesArray.length : 0);
      if (this.mySwitchesArray != null) {
         SwitchData[] var6 = this.mySwitchesArray;
         var3 = var6.length;

         for(var4 = 0; var4 < var3; ++var4) {
            SwitchData aMySwitchesArray = var6[var4];
            aMySwitchesArray.save(os);
         }
      }

   }

   public void removeJump(int jump) {
      if (jump > 0 && jump <= this.myJumps.size()) {
         this.myJumps.remove(jump - 1);
      }

   }

   public void fillArrays() {
      int i;
      if (this.myJumps != null) {
         this.myJumpsArray = new JumpData[this.myJumps.size()];

         for(i = 0; i < this.myJumps.size(); ++i) {
            this.myJumpsArray[i] = (JumpData)this.myJumps.get(i);
         }

         this.myJumps = null;
      }

      if (this.mySwitches != null) {
         this.mySwitchesArray = new SwitchData[this.mySwitches.size()];

         for(i = 0; i < this.mySwitches.size(); ++i) {
            this.mySwitchesArray[i] = (SwitchData)this.mySwitches.get(i);
         }

         this.mySwitches = null;
      }

   }

   public void merge(CoverageData data) {
      JumpsAndSwitches jumpsData = (JumpsAndSwitches)data;
      if (jumpsData.myJumpsArray != null) {
         if (this.myJumpsArray == null) {
            this.myJumpsArray = new JumpData[jumpsData.myJumpsArray.length];
         } else if (this.myJumpsArray.length < jumpsData.myJumpsArray.length) {
            JumpData[] extJumpsArray = new JumpData[jumpsData.myJumpsArray.length];
            System.arraycopy(this.myJumpsArray, 0, extJumpsArray, 0, this.myJumpsArray.length);
            this.myJumpsArray = extJumpsArray;
         }

         mergeJumps(this.myJumpsArray, jumpsData.myJumpsArray);
      }

      if (jumpsData.mySwitchesArray != null) {
         if (this.mySwitchesArray == null) {
            this.mySwitchesArray = new SwitchData[jumpsData.mySwitchesArray.length];
         } else if (this.mySwitchesArray.length < jumpsData.mySwitchesArray.length) {
            SwitchData[] extJumpsArray = new SwitchData[jumpsData.mySwitchesArray.length];
            System.arraycopy(this.mySwitchesArray, 0, extJumpsArray, 0, this.mySwitchesArray.length);
            this.mySwitchesArray = extJumpsArray;
         }

         mergeSwitches(this.mySwitchesArray, jumpsData.mySwitchesArray);
      }

   }

   private static void mergeSwitches(SwitchData[] myArray, SwitchData[] array) {
      for(int i = 0; i < array.length; ++i) {
         SwitchData switchData = myArray[i];
         if (switchData == null) {
            if (array[i] == null) {
               continue;
            }

            switchData = new SwitchData(array[i].getKeys());
            myArray[i] = switchData;
         }

         switchData.merge(array[i]);
      }

   }

   private static void mergeJumps(JumpData[] myArray, JumpData[] array) {
      for(int i = 0; i < array.length; ++i) {
         JumpData switchData = myArray[i];
         if (switchData == null) {
            if (array[i] == null) {
               continue;
            }

            switchData = new JumpData();
            myArray[i] = switchData;
         }

         switchData.merge(array[i]);
      }

   }
}
