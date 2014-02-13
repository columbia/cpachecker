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
package org.sosy_lab.cpachecker.cpa.octagon.coefficients;

import org.sosy_lab.cpachecker.cpa.octagon.OctState;
import org.sosy_lab.cpachecker.util.octagon.NumArray;

/**
 * Class for representing Coeffecients which show the value of a variable
 * dependant on all other variables and a constant value.
 */
public interface IOctCoefficients {

  /**
   * Creates a NumArray out of the coefficient array.
   */
  NumArray getNumArray();

  /**
   * Returns the size of the coefficient list.
   */
  int size();

  IOctCoefficients expandToSize(int size, OctState oct);

  /**
   * Adds two OctCoefficients.
   * @return The new added Coefficient.
   */
  IOctCoefficients add(IOctCoefficients other);

  /**
   * Substracts two OctCoefficients.
   * @return The new substracted Coefficient.
   */
  IOctCoefficients sub(IOctCoefficients other);

  /**
   * Indicates whether the Coefficient List only consists of a constant value.
   */
  boolean hasOnlyConstantValue();

  @Override
  boolean equals(Object obj);

  @Override
  int hashCode();

  @Override
  String toString();

}