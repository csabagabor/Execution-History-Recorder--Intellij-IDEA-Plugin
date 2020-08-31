package original.com.intellij.rt.coverage.data;

import modified.com.intellij.rt.coverage.data.ProjectData;
import original.com.intellij.rt.coverage.util.CoverageIOUtil;
import java.io.DataOutputStream;
import java.io.IOException;

public class LineData implements CoverageData {
   private final int myLineNumber;
   private String myMethodSignature;
   private int myHits = 0;
   private byte myStatus = -1;
   private String myUniqueTestName = null;
   private boolean myMayBeUnique = true;
   private JumpsAndSwitches myJumpsAndSwitches;

   public LineData(int line, String desc) {
      this.myLineNumber = line;
      this.myMethodSignature = desc;
   }

   public void touch() {
      ++this.myHits;
      this.setTestName(ProjectData.getCurrentTestName());
   }

   public int getHits() {
      return this.myHits;
   }

   JumpsAndSwitches getOrCreateJumpsAndSwitches() {
      if (this.myJumpsAndSwitches == null) {
         this.myJumpsAndSwitches = new JumpsAndSwitches();
      }

      return this.myJumpsAndSwitches;
   }

   public int getStatus() {
      if (this.myStatus != -1) {
         return this.myStatus;
      } else if (this.myHits == 0) {
         this.myStatus = 0;
         return this.myStatus;
      } else {
         if (this.myJumpsAndSwitches != null) {
            JumpData[] jumps = this.getOrCreateJumpsAndSwitches().getJumps();
            int var4;
            if (jumps != null) {
               JumpData[] var2 = jumps;
               int var3 = jumps.length;

               for(var4 = 0; var4 < var3; ++var4) {
                  JumpData jumpData = var2[var4];
                  if ((jumpData.getFalseHits() > 0 ? 1 : 0) + (jumpData.getTrueHits() > 0 ? 1 : 0) < 2) {
                     this.myStatus = 1;
                     return this.myStatus;
                  }
               }
            }

            SwitchData[] switches = this.getOrCreateJumpsAndSwitches().getSwitches();
            if (switches != null) {
               SwitchData[] var10 = switches;
               var4 = switches.length;

               for(int var11 = 0; var11 < var4; ++var11) {
                  SwitchData switchData = var10[var11];
                  if (switchData.getDefaultHits() == 0) {
                     this.myStatus = 1;
                     return this.myStatus;
                  }

                  for(int i = 0; i < switchData.getHits().length; ++i) {
                     int hit = switchData.getHits()[i];
                     if (hit == 0) {
                        this.myStatus = 1;
                        return this.myStatus;
                     }
                  }
               }
            }
         }

         this.myStatus = 2;
         return this.myStatus;
      }
   }

   public void save(DataOutputStream os) throws IOException {
      CoverageIOUtil.writeINT(os, this.myLineNumber);
      CoverageIOUtil.writeUTF(os, this.myUniqueTestName != null ? this.myUniqueTestName : "");
      CoverageIOUtil.writeINT(os, this.myHits);
      if (this.myHits > 0) {
         if (this.myJumpsAndSwitches != null) {
            this.getOrCreateJumpsAndSwitches().save(os);
         } else {
            (new JumpsAndSwitches()).save(os);
         }
      }

   }

   public void merge(CoverageData data) {
      LineData lineData = (LineData)data;
      this.myHits += lineData.myHits;
      if (this.myJumpsAndSwitches != null || lineData.myJumpsAndSwitches != null) {
         this.getOrCreateJumpsAndSwitches().merge(lineData.getOrCreateJumpsAndSwitches());
      }

      if (lineData.myMethodSignature != null) {
         this.myMethodSignature = lineData.myMethodSignature;
      }

      if (this.myStatus != -1) {
         byte status = (byte)lineData.getStatus();
         if (status > this.myStatus) {
            this.myStatus = status;
         }
      }

   }

   public JumpData addJump(int jump) {
      return this.getOrCreateJumpsAndSwitches().addJump(jump);
   }

   public JumpData getJumpData(int jump) {
      return this.getOrCreateJumpsAndSwitches().getJumpData(jump);
   }

   public void touchBranch(int jump, boolean hit) {
      JumpData jumpData = this.getJumpData(jump);
      if (jumpData != null) {
         if (hit) {
            jumpData.touchTrueHit();
         } else {
            jumpData.touchFalseHit();
         }
      }

   }

   public SwitchData addSwitch(int switchNumber, int[] keys) {
      return this.getOrCreateJumpsAndSwitches().addSwitch(switchNumber, keys);
   }

   public SwitchData getSwitchData(int switchNumber) {
      return this.getOrCreateJumpsAndSwitches().getSwitchData(switchNumber);
   }

   public SwitchData addSwitch(int switchNumber, int min, int max) {
      int[] keys = new int[max - min + 1];

      for(int i = min; i <= max; keys[i - min] = i++) {
      }

      return this.addSwitch(switchNumber, keys);
   }

   public void touchBranch(int switchNumber, int key) {
      SwitchData switchData = this.getSwitchData(switchNumber);
      if (switchData != null) {
         switchData.touch(key);
      }

   }

   public int getLineNumber() {
      return this.myLineNumber;
   }

   public String getMethodSignature() {
      return this.myMethodSignature;
   }

   public void setStatus(byte status) {
      this.myStatus = status;
   }

   public void setTrueHits(int jumpNumber, int trueHits) {
      this.addJump(jumpNumber).setTrueHits(trueHits);
   }

   public void setFalseHits(int jumpNumber, int falseHits) {
      this.addJump(jumpNumber).setFalseHits(falseHits);
   }

   public void setDefaultHits(int switchNumber, int[] keys, int defaultHit) {
      this.addSwitch(switchNumber, keys).setDefaultHits(defaultHit);
   }

   public void setSwitchHits(int switchNumber, int[] keys, int[] hits) {
      this.addSwitch(switchNumber, keys).setKeysAndHits(keys, hits);
   }

   public JumpData[] getJumps() {
      return this.myJumpsAndSwitches == null ? null : this.getOrCreateJumpsAndSwitches().getJumps();
   }

   public SwitchData[] getSwitches() {
      return this.myJumpsAndSwitches == null ? null : this.getOrCreateJumpsAndSwitches().getSwitches();
   }

   public BranchData getBranchData() {
      if (this.myJumpsAndSwitches == null) {
         return null;
      } else {
         int total = 0;
         int covered = 0;
         JumpData[] jumps = this.myJumpsAndSwitches.getJumps();
         int var6;
         if (jumps != null) {
            JumpData[] var4 = jumps;
            int var5 = jumps.length;

            for(var6 = 0; var6 < var5; ++var6) {
               JumpData jump = var4[var6];
               ++total;
               if (jump.getFalseHits() > 0 && jump.getTrueHits() > 0) {
                  ++covered;
               }
            }
         }

         SwitchData[] switches = this.myJumpsAndSwitches.getSwitches();
         if (switches != null) {
            SwitchData[] var14 = switches;
            var6 = switches.length;

            for(int var15 = 0; var15 < var6; ++var15) {
               SwitchData switchData = var14[var15];
               int[] var9 = switchData.getHits();
               int var10 = var9.length;

               for(int var11 = 0; var11 < var10; ++var11) {
                  int hit = var9[var11];
                  ++total;
                  if (hit > 0) {
                     ++covered;
                  }
               }
            }
         }

         return new BranchData(total, covered);
      }
   }

   public void setHits(int hits) {
      this.myHits = hits;
   }

   public void setTestName(String testName) {
      if (testName != null) {
         if (this.myUniqueTestName == null) {
            if (this.myMayBeUnique) {
               this.myUniqueTestName = testName;
            }
         } else if (!this.myUniqueTestName.equals(testName)) {
            this.myUniqueTestName = null;
            this.myMayBeUnique = false;
         }
      }

   }

   public boolean isCoveredByOneTest() {
      return this.myUniqueTestName != null && this.myUniqueTestName.length() > 0;
   }

   public void removeJump(int jump) {
      if (this.myJumpsAndSwitches != null) {
         this.getOrCreateJumpsAndSwitches().removeJump(jump);
      }
   }

   public void fillArrays() {
      if (this.myJumpsAndSwitches != null) {
         this.getOrCreateJumpsAndSwitches().fillArrays();
      }
   }
}
