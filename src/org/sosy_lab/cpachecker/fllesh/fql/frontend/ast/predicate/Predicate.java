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
package org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.predicate;

import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.ASTVisitor;
import org.sosy_lab.cpachecker.fllesh.fql.frontend.ast.FQLNode;

public class Predicate implements FQLNode {
  public static enum Comparison {
    GREATER_OR_EQUAL,
    GREATER,
    EQUAL,
    LESS_OR_EQUAL,
    LESS,
    NOT_EQUAL
  }

  private Comparison mComparison;
  private Term mLeftTerm;
  private Term mRightTerm;
  private String mString;

  public Predicate(Term pLeftTerm, Comparison pComparison, Term pRightTerm) {
    assert(pLeftTerm != null);
    assert(pComparison != null);
    assert(pRightTerm != null);

    mLeftTerm = pLeftTerm;
    mComparison = pComparison;
    mRightTerm = pRightTerm;

    String lComparisonString = null;

    switch (mComparison) {
    case GREATER_OR_EQUAL:
      lComparisonString = ">=";
      break;
    case GREATER:
      lComparisonString = ">";
      break;
    case EQUAL:
      lComparisonString = "==";
      break;
    case LESS_OR_EQUAL:
      lComparisonString = "<=";
      break;
    case LESS:
      lComparisonString = "<";
      break;
    case NOT_EQUAL:
      lComparisonString = "!=";
      break;
    }

    mString = mLeftTerm.toString() + " " + lComparisonString + " " + mRightTerm.toString();
  }

  public Comparison getComparison() {
    return mComparison;
  }

  public Term getLeftTerm() {
    return mLeftTerm;
  }

  public Term getRightTerm() {
    return mRightTerm;
  }

  @Override
  public String toString() {
    return mString;
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }

    if (pOther == null) {
      return false;
    }

    if (pOther.getClass() == getClass()) {
      Predicate lOther = (Predicate)pOther;

      return (mLeftTerm.equals(lOther.mLeftTerm) && mRightTerm.equals(lOther.mRightTerm) && mComparison.equals(lOther.mComparison));
    }

    return false;
  }

  @Override
  public int hashCode() {
    return 3045820 + mLeftTerm.hashCode() + mComparison.hashCode() + mRightTerm.hashCode();
  }

  @Override
  public <T> T accept(ASTVisitor<T> pVisitor) {
    assert(pVisitor != null);

    return pVisitor.visit(this);
  }

}
