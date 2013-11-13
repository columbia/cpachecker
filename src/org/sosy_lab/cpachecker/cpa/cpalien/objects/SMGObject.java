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
package org.sosy_lab.cpachecker.cpa.cpalien.objects;


public class SMGObject {
  final private int size;
  final private String label;

  static final SMGObject nullObject = new SMGObject(0, "NULL");

  static public final SMGObject getNullObject() {
    return nullObject;
  }

  protected SMGObject(int pSize, String pLabel) {
    size = pSize;
    label = pLabel;
  }

  protected SMGObject(SMGObject pOther) {
    size = pOther.size;
    label = pOther.label;
  }

  public String getLabel() {
    return label;
  }

  public int getSize() {
    return size;
  }

  public boolean notNull() {
    return (! equals(nullObject));
  }

  public boolean isAbstract() {
    if (equals(nullObject)) {
      return false;
    }

    throw new UnsupportedOperationException("isAbstract() called on SMGObject instance, not on a subclass");
  }
}