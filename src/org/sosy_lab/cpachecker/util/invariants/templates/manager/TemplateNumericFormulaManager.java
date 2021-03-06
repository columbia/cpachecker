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
package org.sosy_lab.cpachecker.util.invariants.templates.manager;

import java.math.BigInteger;

import org.sosy_lab.cpachecker.util.invariants.templates.TemplateNumber;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateTerm;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.RationalFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormulaManager;


abstract class TemplateNumericFormulaManager implements NumeralFormulaManager<NumeralFormula, RationalFormula> {

  private FormulaType<RationalFormula> type;
  private TemplateNumericBaseFormulaManager baseManager;
  private TemplateFormulaManager manager;

  public TemplateNumericFormulaManager(TemplateFormulaManager manager, FormulaType<RationalFormula> pType) {
    this.manager = manager;
    this.type = pType;
    baseManager = new TemplateNumericBaseFormulaManager();
  }

  @Override
  public RationalFormula makeNumber(long pI) {
    TemplateNumber N = new TemplateNumber(type, (int) pI);
    TemplateTerm T = new TemplateTerm(type);
    T.setCoefficient(N);
    return T;
  }

  @Override
  public RationalFormula makeNumber(BigInteger pI) {
    return makeNumber(pI.longValue());
  }

  @Override
  public RationalFormula makeNumber(String pI) {
    return makeNumber(Long.parseLong(pI));
  }

  @Override
  public RationalFormula makeVariable(String pVar) {
    return manager.makeVariable(type, pVar, null);
  }

  @Override
  public FormulaType<RationalFormula> getFormulaType() {
    return type;
  }

  @Override
  public RationalFormula negate(NumeralFormula pNumber) {
    return (RationalFormula) baseManager.negate(pNumber);
  }

  @Override
  public boolean isNegate(NumeralFormula pNumber) {
    return baseManager.isNegate(pNumber);
  }

  @Override
  public RationalFormula add(NumeralFormula pNumber1, NumeralFormula pNumber2) {
    return (RationalFormula) baseManager.add(pNumber1, pNumber1);
  }

  @Override
  public boolean isAdd(NumeralFormula pNumber) {
    return baseManager.isAdd(pNumber);
  }

  @Override
  public RationalFormula subtract(NumeralFormula pNumber1, NumeralFormula pNumber2) {
    return (RationalFormula) baseManager.subtract(pNumber1, pNumber1);
  }

  @Override
  public boolean isSubtract(NumeralFormula pNumber) {
    return baseManager.isSubtract(pNumber);
  }

  @Override
  public RationalFormula divide(NumeralFormula pNumber1, NumeralFormula pNumber2) {
    return (RationalFormula) baseManager.divide(pNumber1, pNumber2);
  }

  @Override
  public boolean isDivide(NumeralFormula pNumber) {
    return baseManager.isDivide(pNumber);
  }

  @Override
  public RationalFormula modulo(NumeralFormula pNumber1, NumeralFormula pNumber2) {
    return (RationalFormula) baseManager.modulo(pNumber1, pNumber1);
  }

  @Override
  public boolean isModulo(NumeralFormula pNumber) {
    return baseManager.isModulo(pNumber);
  }

  @Override
  public RationalFormula multiply(NumeralFormula pNumber1, NumeralFormula pNumber2) {
    return (RationalFormula) baseManager.multiply(pNumber1, pNumber2);
  }

  @Override
  public boolean isMultiply(NumeralFormula pNumber) {
    return baseManager.isMultiply(pNumber);
  }

  @Override
  public BooleanFormula equal(NumeralFormula pNumber1, NumeralFormula pNumber2) {
    return baseManager.equal(pNumber1, pNumber2);
  }

  @Override
  public boolean isEqual(BooleanFormula pNumber) {
    return baseManager.isEqual(pNumber);
  }

  @Override
  public BooleanFormula greaterThan(NumeralFormula pNumber1, NumeralFormula pNumber2) {
    return baseManager.greaterThan(pNumber1, pNumber2);
  }

  @Override
  public boolean isGreaterThan(BooleanFormula pNumber) {
    return baseManager.isGreaterThan(pNumber);
  }

  @Override
  public BooleanFormula greaterOrEquals(NumeralFormula pNumber1, NumeralFormula pNumber2) {
    return baseManager.greaterOrEquals(pNumber1, pNumber2);
  }

  @Override
  public boolean isGreaterOrEquals(BooleanFormula pNumber) {
    return baseManager.isGreaterOrEquals(pNumber);
  }

  @Override
  public BooleanFormula lessThan(NumeralFormula pNumber1, NumeralFormula pNumber2) {
    return baseManager.lessThan(pNumber1, pNumber2);
  }

  @Override
  public boolean isLessThan(BooleanFormula pNumber) {
    return baseManager.isLessThan(pNumber);
  }

  @Override
  public BooleanFormula lessOrEquals(NumeralFormula pNumber1, NumeralFormula pNumber2) {
    return baseManager.lessOrEquals(pNumber1, pNumber2);
  }

  @Override
  public boolean isLessOrEquals(BooleanFormula pNumber) {
    return baseManager.isLessOrEquals(pNumber);
  }

}
