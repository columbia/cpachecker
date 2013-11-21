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
package org.sosy_lab.cpachecker.cpa.validvars;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;


public class ValidVarsTransferRelation implements TransferRelation{

  private final TransferRelation wrappedTransfer;

  public ValidVarsTransferRelation(TransferRelation pWrappedTransfer){
    wrappedTransfer = pWrappedTransfer;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(AbstractState pState, Precision pPrecision,
      CFAEdge pCfaEdge) throws CPATransferException, InterruptedException {

    ValidVarsState state = (ValidVarsState)pState;
    ValidVars validVariables = state.getValidVariables();

    switch (pCfaEdge.getEdgeType()) {
    case MultiEdge:
      Collection<AbstractState> predecessors, successors;
      predecessors = Collections.singleton(pState);

      for(CFAEdge edge: ((MultiEdge)pCfaEdge).getEdges()){
        successors = new ArrayList<>();
        for(AbstractState predState: predecessors){
          successors.addAll(getAbstractSuccessors(predState, pPrecision, edge));
        }

        predecessors = successors;
      }

      return predecessors;
    case FunctionCallEdge:
      validVariables.extendLocalVars(pCfaEdge.getSuccessor().getFunctionName(),
          ((FunctionEntryNode) pCfaEdge.getSuccessor()).getFunctionParameterNames());
      break;
    case FunctionReturnEdge:
      validVariables = validVariables.removeVarsOfFunction(pCfaEdge.getPredecessor().getFunctionName());
      break;
    case DeclarationEdge:
      CDeclaration declaration = ((CDeclarationEdge) pCfaEdge).getDeclaration();
      if(declaration.isGlobal()){
        validVariables = validVariables.extendGlobalVars(declaration.getName());
      } else {
        validVariables =
            validVariables.extendLocalVars(pCfaEdge.getPredecessor().getFunctionName(), declaration.getName());
      }
      break;
    default:
      break;
    }
      // consider successors of wrapped state
      Collection<? extends AbstractState> wrappedSuccessors =
          wrappedTransfer.getAbstractSuccessors(state.getWrappedState(), pPrecision, pCfaEdge);

    ArrayList<AbstractState> successors = new ArrayList<>(wrappedSuccessors.size());

    for (AbstractState successor : wrappedSuccessors) {
      successors.add(new ValidVarsState(successor, validVariables));
    }

    return successors;
  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState pState, List<AbstractState> pOtherStates,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException, InterruptedException {

    ValidVarsState state = (ValidVarsState) pState;
    Collection<? extends AbstractState> wrappedStrengthen =
        wrappedTransfer.strengthen(state.getWrappedState(), pOtherStates, pCfaEdge, pPrecision);

    ArrayList<AbstractState> successors = new ArrayList<>(wrappedStrengthen.size());

    for (AbstractState successor : wrappedStrengthen) {
      successors.add(new ValidVarsState(successor, state.getValidVariables()));
    }

    return successors;
  }

}