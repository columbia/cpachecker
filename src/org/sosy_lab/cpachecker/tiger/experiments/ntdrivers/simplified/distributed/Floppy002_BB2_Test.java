/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.tiger.experiments.ntdrivers.simplified.distributed;

import java.util.LinkedList;

import org.junit.Test;
import org.sosy_lab.cpachecker.tiger.cmdline.CPAtigerParallelized;
import org.sosy_lab.cpachecker.tiger.experiments.ExperimentalSeries;
import org.sosy_lab.cpachecker.tiger.fql.PredefinedCoverageCriteria;

public class Floppy002_BB2_Test extends ExperimentalSeries {

  @Test
  public void test001() throws Exception {
    String lCFile = "floppy_simpl3.cil.c";

    LinkedList<String> lArguments = new LinkedList<>();

    lArguments.add(PredefinedCoverageCriteria.BASIC_BLOCK_2_COVERAGE);
    lArguments.add("test/programs/fql/ntdrivers-simplified/" + lCFile);
    lArguments.add("main");
    lArguments.add("4");

    String[] lArgs = new String[lArguments.size()];
    lArguments.toArray(lArgs);

    CPAtigerParallelized.main(lArgs);
  }

}