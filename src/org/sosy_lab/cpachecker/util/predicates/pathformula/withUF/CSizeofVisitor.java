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
package org.sosy_lab.cpachecker.util.predicates.pathformula.withUF;

import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.MachineModel.BaseSizeofVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeVisitor;

class CSizeofVisitor extends BaseSizeofVisitor
                                   implements CTypeVisitor<Integer, IllegalArgumentException> {

  public CSizeofVisitor(final MachineModel machineModel,
                         final FormulaEncodingWithUFOptions options) {
    super(machineModel);
    this.options = options;
  }

  @Override
  public Integer visit(final CArrayType t) throws IllegalArgumentException {
    Integer length = CTypeUtils.getArrayLength(t);

    if (length == null) {
      length = options.defaultArrayLength();
    }

    final int sizeOfType = t.getType().accept(this);
    return length * sizeOfType;
  }

  private final FormulaEncodingWithUFOptions options;
}