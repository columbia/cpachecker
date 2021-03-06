/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.z3;

import java.util.List;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.TestLogManager;
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.util.NativeLibraries;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.RationalFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.OptEnvironment;
import org.sosy_lab.cpachecker.util.rationals.ExtendedRational;

import com.google.common.collect.ImmutableList;


/**
 * Tests for the opti-z3 branch.
 */
public class Z3MaximizationTest {

  private Configuration config;
  private LogManager logger;
  private Z3FormulaManager mgr;
  private Z3RationalFormulaManager rfmgr;
  private Z3BooleanFormulaManager bfmgr;

  @Before
  public void loadZ3() throws Exception {
    try {
      NativeLibraries.loadLibrary("z3j");
    } catch (UnsatisfiedLinkError t) {
      Assume.assumeNoException("libfoci.so is needed for Z3 to load", t);
    }
    config = Configuration.defaultConfiguration();
    logger = TestLogManager.getInstance();
    mgr = Z3FormulaManager.create(logger, config, null);
    rfmgr = (Z3RationalFormulaManager) mgr.getRationalFormulaManager();
    bfmgr = (Z3BooleanFormulaManager) mgr.getBooleanFormulaManager();
  }


  @Test public void testMaximizationBjornerApi() throws Exception {
    try (OptEnvironment prover = new Z3OptProver(mgr)) {

      RationalFormula x, y, obj;
      x = rfmgr.makeVariable("x");
      y = rfmgr.makeVariable("y");
      obj = rfmgr.makeVariable("obj");

      /*
        real x, y, obj
        x <= 10
        y <= 15
        obj = x + y
        x - y >= 1
       */
      List<BooleanFormula> constraints = ImmutableList.of(
          rfmgr.lessOrEquals(x, rfmgr.makeNumber("10")),
          rfmgr.lessOrEquals(y, rfmgr.makeNumber("15")),
          rfmgr.equal(obj, rfmgr.add(x, y)),
          rfmgr.greaterOrEquals(rfmgr.subtract(x, y), rfmgr.makeNumber("1"))
      );

      prover.addConstraint(bfmgr.and(constraints));
      prover.setObjective(x);

      // Maximize for x.
      OptEnvironment.OptResult response = prover.maximize();

      Assert.assertEquals(OptEnvironment.OptResult.OPT, response);

      // Check the value.
      Model model = prover.getModel();

      ExtendedRational value = (ExtendedRational)
          model.get(new Model.Constant("obj", Model.TermType.Real));

      Assert.assertEquals(ExtendedRational.ofString("19"), value);
    }

  }

}
