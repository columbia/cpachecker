/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.invariants.operators.interval.compound.tocompound;

import org.sosy_lab.cpachecker.cpa.invariants.CompoundInterval;
import org.sosy_lab.cpachecker.cpa.invariants.SimpleInterval;
import org.sosy_lab.cpachecker.cpa.invariants.operators.interval.interval.tocompound.IICOperator;

/**
 * This class represents the modulo operator for computing the
 * remainders of dividing intervals by compound states.
 */
enum ModuloOperator implements ICCOperator {

  /**
   * The singleton instance of this operator.
   */
  INSTANCE;

  @Override
  public CompoundInterval apply(SimpleInterval pFirstOperand, CompoundInterval pSecondOperand) {
    CompoundInterval result = CompoundInterval.bottom();
    for (SimpleInterval interval : pSecondOperand.getIntervals()) {
      CompoundInterval current = IICOperator.MODULO_OPERATOR.apply(pFirstOperand, interval);
      if (current != null) {
        result = result.unionWith(current);
        if (result.isTop()) {
          return result;
        }
      }
    }
    return result;
  }

}