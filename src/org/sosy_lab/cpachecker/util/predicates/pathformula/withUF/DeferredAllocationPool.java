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
package org.sosy_lab.cpachecker.util.predicates.pathformula.withUF;

import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;

/**
 * This class is used to keep information about already performed memory allocations of unknown type, e.g.
 *
 *   <pre>
 *   void *tmp_0 = malloc(size); // tmp_0 is a pointer variable, allocation type is unknown
 *   ...
 *   void *tmp_2 = tmp_0; // Now tmp_2 is also a pointer variable corresponding to the same allocation
 *   struct s* ps = (struct s*)tmp_2; // Now the actual type of the allocation is revealed
 *   </pre>
 *
 * <p>
 * When the type of the allocation is revealed (by the context in which one of the pointer variables is used),
 * the actual allocation occurs (pointer targets are added to the set).
 * </p>
 * <p>
 * Several base variables can fall within the same pool in case of merging, e.g.:
 *
 *   <pre>
 *   void *tmp_0;
 *   if (condition) {
 *     tmp_0 = malloc(size1); // Corresponds to a fake base variable __VERIFIER_successfull_alloc0
 *   } else {
 *     tmp_0 = malloc(size2); // Corresponds to another fake base variable __VERIFIER_successfull_alloc1
 *   }
 *   ... (struct s*) tmp_0 // Both base variables are allocated here as (struct s)
 *                         // (but their addresses can be different!)
 *   </pre>
 */
 public class DeferredAllocationPool {

  private DeferredAllocationPool(final PersistentList<String> pointerVariables,
                                 final boolean isZeroing,
                                 final CIntegerLiteralExpression size,
                                 final PersistentList<String> baseVariables) {
    this.pointerVariables = pointerVariables;
    this.isZeroing = isZeroing;
    this.size = size;
    this.baseVariables = baseVariables;
  }

  DeferredAllocationPool(final String pointerVariable,
                         final boolean isZeroing,
                         final CIntegerLiteralExpression size,
                         final String baseVariable) {
    this(PersistentList.of(pointerVariable), isZeroing, size, PersistentList.of(baseVariable));
  }

  private DeferredAllocationPool(final DeferredAllocationPool predecessor,
                                 final PersistentList<String> pointerVariables) {
    this(pointerVariables,
         predecessor.isZeroing,
         predecessor.size,
         predecessor.baseVariables);
  }

  public PersistentList<String> getPointerVariables() {
    return pointerVariables;
  }

  public PersistentList<String> getBaseVariables() {
    return baseVariables;
  }

  public boolean wasAllocationZeroing() {
    return isZeroing;
  }

  public CIntegerLiteralExpression getSize() {
    return size;
  }

  DeferredAllocationPool addPointerVariable(final String pointerVariable) {
    return new DeferredAllocationPool(this, this.pointerVariables.with(pointerVariable));
  }

  DeferredAllocationPool removePointerVariable(final String pointerVariable) {
    return new DeferredAllocationPool(this, pointerVariables.without(pointerVariable));
  }

  DeferredAllocationPool mergeWith(final DeferredAllocationPool other) {
    return new DeferredAllocationPool(PersistentList.merge(this.pointerVariables, other.pointerVariables),
                                      this.isZeroing && other.isZeroing,
                                      this.size != null && other.size != null ?
                                        this.size.getValue().equals(other.size.getValue()) ? this.size : null :
                                        this.size != null ? this.size : other.size,
                                      PersistentList.merge(this.baseVariables, other.baseVariables));
  }

  @Override
  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof DeferredAllocationPool)) {
      return false;
    }
    final DeferredAllocationPool otherPool = (DeferredAllocationPool) other;
    if (pointerVariables.containsAll(otherPool.pointerVariables) &&
        otherPool.pointerVariables.containsAll(pointerVariables)) {
      return true;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    int result = 0;
    for (final String s : pointerVariables) {
      result += s.hashCode();
    }
    return result;
  }

  private final PersistentList<String> pointerVariables;
  private final boolean isZeroing;
  private final CIntegerLiteralExpression size;
  private final PersistentList<String> baseVariables;
}