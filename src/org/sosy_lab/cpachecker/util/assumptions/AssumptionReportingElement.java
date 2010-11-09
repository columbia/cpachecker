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
package org.sosy_lab.cpachecker.util.assumptions;


/**
 * Interface to implement in order for an object to be able to
 * contribute invariants to the invariant construction.
 *
 * @author g.theoduloz
 */
public interface AssumptionReportingElement {

  /**
   * Get the assumption that the given abstract element
   * wants to report for its containing node's location.
   *
   * @return an assumption representing the assumptions to generate
   *         for the given element, or the value null representing
   *         the assumption true.
   */
  public Assumption getAssumption();

}
