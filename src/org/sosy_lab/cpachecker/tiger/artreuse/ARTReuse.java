/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.tiger.artreuse;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonStateElement;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.productautomaton.ProductAutomatonElement;
import org.sosy_lab.cpachecker.tiger.fql.ecp.ECPEdgeSet;
import org.sosy_lab.cpachecker.tiger.fql.ecp.translators.GuardedEdgeLabel;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;

public class ARTReuse {

  public static void modifyReachedSet(ReachedSet pReachedSet, FunctionEntryNode pEntryNode, ARGCPA pARTCPA, int pProductAutomatonIndex, NondeterministicFiniteAutomaton<GuardedEdgeLabel> pPreviousAutomaton, NondeterministicFiniteAutomaton<GuardedEdgeLabel> pCurrentAutomaton) {
    if (pReachedSet.isEmpty()) {
      AbstractState lInitialElement = pARTCPA.getInitialState(pEntryNode);
      Precision lInitialPrecision = pARTCPA.getInitialPrecision(pEntryNode);

      pReachedSet.add(lInitialElement, lInitialPrecision);
    }
    else {
      if (pPreviousAutomaton == null) {
        throw new RuntimeException();
      }

      modifyART(pReachedSet, pARTCPA, pProductAutomatonIndex, pPreviousAutomaton, pCurrentAutomaton);
    }
  }

  private static void modifyART(ReachedSet pReachedSet, ARGReachedSet pARTReachedSet, int pProductAutomatonIndex, Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge> pFrontierEdges) {
    //Set<Pair<ARTElement, ARTElement>> lPathEdges = Collections.emptySet();
    //ARTStatistics.dumpARTToDotFile(new File("/home/andreas/art01.dot"), lARTCPA, pReachedSet, lPathEdges);

    for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lEdge : pFrontierEdges) {
      GuardedEdgeLabel lLabel = lEdge.getLabel();

      ECPEdgeSet lEdgeSet = lLabel.getEdgeSet();

      for (CFAEdge lCFAEdge : lEdgeSet) {
        CFANode lCFANode = lCFAEdge.getPredecessor();

        Collection<AbstractState> lAbstractElements = pReachedSet.getReached(lCFANode);

        LinkedList<AbstractState> lAbstractElements2 = new LinkedList<>();
        lAbstractElements2.addAll(lAbstractElements);

        for (AbstractState lAbstractElement : lAbstractElements2) {
          if (!pReachedSet.contains(lAbstractElement)) {
            // lAbstractElement was removed in an earlier step
            continue;
          }

          ARGState lARTElement = (ARGState)lAbstractElement;

          if (AbstractStates.extractLocation(lARTElement) != lCFANode) {
            continue;
          }

          // what's the semantics of getWrappedElement*s*()?
          CompositeState lCompositeElement = (CompositeState)lARTElement.getWrappedState();

          ProductAutomatonElement lProductAutomatonElement = (ProductAutomatonElement)lCompositeElement.get(pProductAutomatonIndex);

          GuardedEdgeAutomatonStateElement lStateElement = (GuardedEdgeAutomatonStateElement)lProductAutomatonElement.get(0);

          if (lStateElement.getAutomatonState() == lEdge.getSource()) {
            if (lARTElement.getChildren().isEmpty()) {
              // re-add element to worklist
              pReachedSet.reAddToWaitlist(lARTElement);
            }
            else {
              // by removing the children, lARTElement gets added to the
              // worklist automatically

              /* TODO add removal of only non-isomorphic parts again */
              while (!lARTElement.getChildren().isEmpty()) {
                ARGState lChildElement = lARTElement.getChildren().iterator().next();

                pARTReachedSet.removeSubtree(lChildElement);
              }
            }
          }
        }
      }
    }
  }

  private static void modifyART(ReachedSet pReachedSet, ARGCPA pARTCPA, int pProductAutomatonIndex, NondeterministicFiniteAutomaton<GuardedEdgeLabel> pPreviousAutomaton, NondeterministicFiniteAutomaton<GuardedEdgeLabel> pCurrentAutomaton) {
    ARGReachedSet lARTReachedSet = new ARGReachedSet(pReachedSet, pARTCPA);

    Pair<Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge>, Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge>> lFrontier = determineFrontier(pPreviousAutomaton, pCurrentAutomaton);

    modifyART(pReachedSet, lARTReachedSet, pProductAutomatonIndex, lFrontier.getFirst());
    modifyART(pReachedSet, lARTReachedSet, pProductAutomatonIndex, lFrontier.getSecond());
  }

  private static Pair<Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge>, Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge>> determineLocalDifference(NondeterministicFiniteAutomaton<GuardedEdgeLabel> pPreviousAutomaton, NondeterministicFiniteAutomaton<GuardedEdgeLabel> pCurrentAutomaton, NondeterministicFiniteAutomaton.State pPreviousState, NondeterministicFiniteAutomaton.State pCurrentState) {
    Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge> lE1 = new HashSet<>();
    Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge> lE2 = new HashSet<>();

    for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lEdge : pPreviousAutomaton.getOutgoingEdges(pPreviousState)) {
      boolean lFound = false;

      if (lEdge.getLabel().hasGuards()) {
        // TODO extend implementation to guards
        throw new RuntimeException("No support for guards!");
      }

      for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lOtherEdge : pCurrentAutomaton.getOutgoingEdges(pCurrentState)) {
        if (lEdge.getTarget() == lOtherEdge.getTarget()) {
          if (lEdge.getLabel().equals(lOtherEdge.getLabel())) {
            lFound = true;
          }
        }
      }

      if (!lFound) {
        lE1.add(lEdge);
      }
    }

    for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lEdge : pCurrentAutomaton.getOutgoingEdges(pCurrentState)) {
      boolean lFound = false;

      if (lEdge.getLabel().hasGuards()) {
        // TODO extend implementation to guards
        throw new RuntimeException("No support for guards!");
      }

      for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lOtherEdge : pPreviousAutomaton.getOutgoingEdges(pPreviousState)) {
        if (lEdge.getTarget() == lOtherEdge.getTarget()) {
          if (lEdge.getLabel().equals(lOtherEdge.getLabel())) {
            lFound = true;
          }
        }
      }

      if (!lFound) {
        lE2.add(lEdge);
      }
    }

    return Pair.of(lE1, lE2);
  }

  private static Pair<Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge>, Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge>> determineFrontier(NondeterministicFiniteAutomaton<GuardedEdgeLabel> pPreviousAutomaton, NondeterministicFiniteAutomaton<GuardedEdgeLabel> pCurrentAutomaton) {
    Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge> lF1 = new HashSet<>();
    Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge> lF2 = new HashSet<>();

    LinkedList<Pair<NondeterministicFiniteAutomaton.State, NondeterministicFiniteAutomaton.State>> lWorklist = new LinkedList<>();

    if (pPreviousAutomaton.getInitialState() != pCurrentAutomaton.getInitialState()) {
      throw new RuntimeException();
    }

    lWorklist.add(Pair.of(pPreviousAutomaton.getInitialState(), pCurrentAutomaton.getInitialState()));

    HashSet<Pair<NondeterministicFiniteAutomaton.State, NondeterministicFiniteAutomaton.State>> lVisited = new HashSet<>();

    while (!lWorklist.isEmpty()) {
      Pair<NondeterministicFiniteAutomaton.State, NondeterministicFiniteAutomaton.State> lCurrentPair = lWorklist.removeLast();

      if (!lVisited.contains(lCurrentPair)) {
        lVisited.add(lCurrentPair);

        // determine local difference

        Pair<Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge>, Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge>> lFrontier = determineLocalDifference(pPreviousAutomaton, pCurrentAutomaton, lCurrentPair.getFirst(), lCurrentPair.getSecond());

        if (lFrontier.getFirst().isEmpty() && lFrontier.getSecond().isEmpty()) {
          // update worklist
          for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lEdge : pPreviousAutomaton.getOutgoingEdges(lCurrentPair.getFirst())) {
            // was mit !lFrontier.getFirst().isEmpty() machen?
            lWorklist.add(Pair.of(lEdge.getTarget(), lEdge.getTarget()));
          }
        }
        else {
          lF1.addAll(lFrontier.getFirst());
          lF2.addAll(lFrontier.getSecond());
        }
      }
    }

    return Pair.of(lF1, lF2);
  }

}