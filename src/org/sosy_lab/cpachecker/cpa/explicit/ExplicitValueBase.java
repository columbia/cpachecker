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
package org.sosy_lab.cpachecker.cpa.explicit;

/**
 * Base class for values that can be tracked by the ExplicitCPA.
 *
 * Traditionally, ExplicitCPA would only keep track of long type values.
 * For the future, floats, symbolic values, and SMG nodes should
 * also be supported.
 */
public interface ExplicitValueBase {
  public boolean isNumericValue();

  /** Return the ExplicitNumericValue if this is a numeric value, null otherwise. **/
  public ExplicitNumericValue asNumericValue();

  /** Return the long value if this is a long value, null otherwise. **/
  public Long asLong();
}