/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.fllesh.experiments.testlocks;

import junit.framework.Assert;

import org.junit.Test;
import org.sosy_lab.cpachecker.fllesh.FlleShResult;
import org.sosy_lab.cpachecker.fllesh.Main;
import org.sosy_lab.cpachecker.fllesh.experiments.ExperimentalSeries;

public class BasicBlock2Coverage extends ExperimentalSeries {
  
  @Test
  public void test_locks_201() throws Exception {
    String[] lArguments = Main.getParameters(Main.BASIC_BLOCK_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_5.c",
                                        "main",
                                        true);
    
    FlleShResult lResult = execute(lArguments);
    
    Assert.assertEquals(1024, lResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(601, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(423, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(32, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_202() throws Exception {
    String[] lArguments = Main.getParameters(Main.BASIC_BLOCK_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_6.c",
                                        "main",
                                        true);
    
    FlleShResult lResult = execute(lArguments);
    
    Assert.assertEquals(1369, lResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(813, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(556, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(39, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_203() throws Exception {
    String[] lArguments = Main.getParameters(Main.BASIC_BLOCK_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_7.c",
                                        "main",
                                        true);
    
    FlleShResult lResult = execute(lArguments);
    
    Assert.assertEquals(1764, lResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(1057, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(707, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(52, lResult.getNumberOfTestCases());  // 55
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_204() throws Exception {
    String[] lArguments = Main.getParameters(Main.BASIC_BLOCK_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_8.c",
                                        "main",
                                        true);
    
    FlleShResult lResult = execute(lArguments);
    
    Assert.assertEquals(2209, lResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(1333, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(876, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(68, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_205() throws Exception {
    String[] lArguments = Main.getParameters(Main.BASIC_BLOCK_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_9.c",
                                        "main",
                                        true);
    
    FlleShResult lResult = execute(lArguments);
    
    Assert.assertEquals(2704, lResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(1641, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(1063, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(66, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_206() throws Exception {
    String[] lArguments = Main.getParameters(Main.BASIC_BLOCK_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_10.c",
                                        "main",
                                        true);
    
    FlleShResult lResult = execute(lArguments);
    
    Assert.assertEquals(3249, lResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(1981, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(1268, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(84, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_207() throws Exception {
    String[] lArguments = Main.getParameters(Main.BASIC_BLOCK_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_11.c",
                                        "main",
                                        true);
    
    FlleShResult lResult = execute(lArguments);
    
    Assert.assertEquals(3844, lResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(2353, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(1491, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(84, lResult.getNumberOfTestCases()); // 85
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_208() throws Exception {
    String[] lArguments = Main.getParameters(Main.BASIC_BLOCK_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_12.c",
                                        "main",
                                        true);
    
    FlleShResult lResult = execute(lArguments);
    
    Assert.assertEquals(4489, lResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(2757, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(1732, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(139, lResult.getNumberOfTestCases()); // 143
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_209() throws Exception {
    String[] lArguments = Main.getParameters(Main.BASIC_BLOCK_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_13.c",
                                        "main",
                                        true);
    
    FlleShResult lResult = execute(lArguments);
    
    Assert.assertEquals(5184, lResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(3193, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(1991, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(156, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_210() throws Exception {
    String[] lArguments = Main.getParameters(Main.BASIC_BLOCK_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_14.c",
                                        "main",
                                        true);
    
    FlleShResult lResult = execute(lArguments);
    
    Assert.assertEquals(5929, lResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(3661, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(2268, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(171, lResult.getNumberOfTestCases()); // 172
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }
  
  @Test
  public void test_locks_211() throws Exception {
    String[] lArguments = Main.getParameters(Main.BASIC_BLOCK_2_COVERAGE,
                                        "test/programs/fql/locks/test_locks_15.c",
                                        "main",
                                        true);
    
    FlleShResult lResult = execute(lArguments);
    
    Assert.assertEquals(6724, lResult.getTask().getNumberOfTestGoals());
    Assert.assertEquals(4161, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(2563, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(213, lResult.getNumberOfTestCases()); // 211
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());
  }

}
