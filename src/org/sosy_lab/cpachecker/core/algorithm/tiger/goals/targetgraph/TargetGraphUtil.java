/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.tiger.goals.targetgraph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

import org.jgrapht.graph.MaskFunctor;
import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.core.algorithm.tiger.TigerAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.Predicate;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.targetgraph.TargetGraph.Builder;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.targetgraph.mask.FunctionNameMaskFunctor;

public class TargetGraphUtil {

  public static Set<CFAEdge> getBasicBlockEntries(CFANode pInitialNode) {
    LinkedHashSet<CFAEdge> lBasicBlockEntries = new LinkedHashSet<>();

    HashSet<CFAEdge> lVisitedEdges = new HashSet<>();

    LinkedList<CFAEdge> lWorklist = new LinkedList<>();

    addLeavingEdgesToWorklist(pInitialNode, lWorklist);

    while (!lWorklist.isEmpty()) {
      CFAEdge lCurrentEdge = lWorklist.removeFirst();

      if (lVisitedEdges.contains(lCurrentEdge)) {
        continue;
      }

      lVisitedEdges.add(lCurrentEdge);


      LinkedList<CFAEdge> lTrace = new LinkedList<>();

      CFAEdge lCurrentTraceEdge = lCurrentEdge;

      lTrace.add(lCurrentTraceEdge);

      while (!isLastEdge(lCurrentTraceEdge)) {
        CFANode lSuccessor = lCurrentTraceEdge.getSuccessor();

        if (lSuccessor.getNumLeavingEdges() != 1) {
          throw new RuntimeException();
        }

        lCurrentTraceEdge = lSuccessor.getLeavingEdge(0);

        lTrace.add(lCurrentTraceEdge);
      }

      // assumptions are not basic block entries
      while (!lTrace.isEmpty() && lTrace.getFirst().getEdgeType().equals(CFAEdgeType.AssumeEdge)) {
        lTrace.removeFirst();
      }

      if (!lTrace.isEmpty()) {
        lCurrentEdge = lTrace.getFirst();

        for (CFAEdge lCFAEdge : lTrace) {
          CFAEdgeType lEdgeType = lCFAEdge.getEdgeType();

          if (lEdgeType.equals(CFAEdgeType.FunctionCallEdge) ||
              lEdgeType.equals(CFAEdgeType.FunctionReturnEdge)) {

            lCurrentTraceEdge = lCFAEdge;

            break;
          }
        }

        CFAEdgeType lEdgeType = lCurrentEdge.getEdgeType();

        if (!lEdgeType.equals(CFAEdgeType.FunctionCallEdge) &&
            !lEdgeType.equals(CFAEdgeType.FunctionReturnEdge)) {
          // basic block consists not only of an interprocedural cfa edge (function call edge or return edge)
          lBasicBlockEntries.add(lCurrentEdge);
        }
      }

      addLeavingEdgesToWorklist(lCurrentTraceEdge.getSuccessor(), lWorklist);
    }

    /*CFAEdge[] lEdges = new CFAEdge[lBasicBlockEntries.size()];
    lEdges = lBasicBlockEntries.toArray(lEdges);

    LinkedHashSet<CFAEdge> lBBEntries = new LinkedHashSet<CFAEdge>();

    for (int i = lEdges.length - 1; i >= 0; i--) {
      lBBEntries.add(lEdges[i]);
    }

    CFAEdge[] lEdges2 = new CFAEdge[lBBEntries.size()];
    lEdges2 = lBBEntries.toArray(lEdges2);

    for (int i = 0; i < lEdges2.length; i++) {
      //System.out.println((i + 1) + ") " + lEdges[i].toString() + " --- " + lEdges2[i].toString());
      System.out.println((i + 1) + ") " + lEdges2[i].toString());
    }

    return lBBEntries;*/
    return lBasicBlockEntries;
  }

  private static void addLeavingEdgesToWorklist(CFANode pCFANode, LinkedList<CFAEdge> pWorklist) {
    for (int lIndex = 0; lIndex < pCFANode.getNumLeavingEdges(); lIndex++) {
      CFAEdge lSuccessorEdge = pCFANode.getLeavingEdge(lIndex);
      pWorklist.add(lSuccessorEdge);
    }
  }

  private static boolean isLastEdge(CFAEdge pCFAEdge) {
    CFANode lSuccessor = pCFAEdge.getSuccessor();

    if (lSuccessor.getNumLeavingEdges() != 1) {
      return true;
    }

    if (lSuccessor.getNumEnteringEdges() != 1) {
      return true;
    }

    if (pCFAEdge.getEdgeType().equals(CFAEdgeType.FunctionCallEdge)) {
      return true;
    }

    return false;
  }

  /*
   * The order in which we traverse the CFA here determines the order
   * in which test goals are enumerated later on.
   */
  public static TargetGraph cfa(CFANode pInitialNode) {
    if (pInitialNode == null) {
      throw new IllegalArgumentException();
    }

    if (pInitialNode.getFunctionName().equals(TigerAlgorithm.CPAtiger_MAIN)) {
      throw new IllegalArgumentException("Do not start target graph construction inside wrapper code!");
    }

    Builder lBuilder = new Builder();

    HashMap<CFANode, Node> lNodeMapping = new HashMap<>();

    LinkedList<CFANode> lWorklist = new LinkedList<>();
    Set<CFANode> lVisitedNodes = new HashSet<>();

    lWorklist.add(pInitialNode);

    Node lInitialNode = new Node(pInitialNode);
    lBuilder.addInitialNode(lInitialNode);
    lBuilder.addNode(lInitialNode);

    lNodeMapping.put(pInitialNode, lInitialNode);

    while (!lWorklist.isEmpty()) {
      //CFANode lCFANode = lWorklist.removeFirst();
      // TODO removeLast() worked for many examples very well (testlocks are exceptions)
      CFANode lCFANode = lWorklist.removeLast();

      lVisitedNodes.add(lCFANode);

      Node lNode = lNodeMapping.get(lCFANode);

      // determine successors
      int lNumberOfLeavingEdges = lCFANode.getNumLeavingEdges();

      FunctionSummaryEdge lCallToReturnEdge = lCFANode.getLeavingSummaryEdge();

      if (lNumberOfLeavingEdges == 0 && lCallToReturnEdge == null) {
       // assert(lCFANode instanceof FunctionExitNode);

        lBuilder.addFinalNode(lNode);
      }
      else {
        for (int lEdgeIndex = 0; lEdgeIndex < lNumberOfLeavingEdges; lEdgeIndex++) {
          CFAEdge lEdge = lCFANode.getLeavingEdge(lEdgeIndex);
          CFANode lSuccessor = lEdge.getSuccessor();

          if (lSuccessor.getFunctionName().equals(TigerAlgorithm.CPAtiger_MAIN)) {
            // we will not consider wrapper code in target graphs
            continue;
          }

          Node lSuccessorNode;

          if (lVisitedNodes.contains(lSuccessor)) {
            lSuccessorNode = lNodeMapping.get(lSuccessor);
          }
          else {
            lSuccessorNode = new Node(lSuccessor);

            lNodeMapping.put(lSuccessor, lSuccessorNode);
            lBuilder.addNode(lSuccessorNode);

            lWorklist.add(lSuccessor);
          }

          lBuilder.addEdge(lNode, lSuccessorNode, lEdge);
        }

        if (lCallToReturnEdge != null) {
          CFANode lSuccessor = lCallToReturnEdge.getSuccessor();

          Node lSuccessorNode;

          if (lVisitedNodes.contains(lSuccessor)) {
            lSuccessorNode = lNodeMapping.get(lSuccessor);
          }
          else {
            lSuccessorNode = new Node(lSuccessor);

            lNodeMapping.put(lSuccessor, lSuccessorNode);
            lBuilder.addNode(lSuccessorNode);

            lWorklist.add(lSuccessor);
          }

          lBuilder.addEdge(lNode, lSuccessorNode, lCallToReturnEdge);
        }
      }
    }

    return lBuilder.build();
  }
  /*public static TargetGraph cfa(CFANode pInitialNode) {
    if (pInitialNode == null) {
      throw new IllegalArgumentException();
    }

    Builder lBuilder = new Builder();

    HashMap<CFANode, Node> lNodeMapping = new HashMap<CFANode, Node>();

    Set<CFANode> lWorklist = new LinkedHashSet<CFANode>();
    Set<CFANode> lVisitedNodes = new HashSet<CFANode>();

    lWorklist.add(pInitialNode);

    Node lInitialNode = new Node(pInitialNode);
    lBuilder.addInitialNode(lInitialNode);
    lBuilder.addNode(lInitialNode);

    lNodeMapping.put(pInitialNode, lInitialNode);

    while (!lWorklist.isEmpty()) {
      CFANode lCFANode = lWorklist.iterator().next();
      lWorklist.remove(lCFANode);

      lVisitedNodes.add(lCFANode);

      Node lNode = lNodeMapping.get(lCFANode);

      // determine successors
      int lNumberOfLeavingEdges = lCFANode.getNumLeavingEdges();

      CallToReturnEdge lCallToReturnEdge = lCFANode.getLeavingSummaryEdge();

      if (lNumberOfLeavingEdges == 0 && lCallToReturnEdge == null) {
        assert(lCFANode instanceof CFAFunctionExitNode);

        lBuilder.addFinalNode(lNode);
      }
      else {
        for (int lEdgeIndex = 0; lEdgeIndex < lNumberOfLeavingEdges; lEdgeIndex++) {
          CFAEdge lEdge = lCFANode.getLeavingEdge(lEdgeIndex);
          CFANode lSuccessor = lEdge.getSuccessor();

          Node lSuccessorNode;

          if (lVisitedNodes.contains(lSuccessor)) {
            lSuccessorNode = lNodeMapping.get(lSuccessor);
          }
          else {
            lSuccessorNode = new Node(lSuccessor);

            lNodeMapping.put(lSuccessor, lSuccessorNode);
            lBuilder.addNode(lSuccessorNode);

            lWorklist.add(lSuccessor);
          }

          lBuilder.addEdge(lNode, lSuccessorNode, lEdge);
        }

        if (lCallToReturnEdge != null) {
          CFANode lSuccessor = lCallToReturnEdge.getSuccessor();

          Node lSuccessorNode;

          if (lVisitedNodes.contains(lSuccessor)) {
            lSuccessorNode = lNodeMapping.get(lSuccessor);
          }
          else {
            lSuccessorNode = new Node(lSuccessor);

            lNodeMapping.put(lSuccessor, lSuccessorNode);
            lBuilder.addNode(lSuccessorNode);

            lWorklist.add(lSuccessor);
          }

          lBuilder.addEdge(lNode, lSuccessorNode, lCallToReturnEdge);
        }
      }
    }

    return lBuilder.build();
  }*/

  public static TargetGraph union(TargetGraph pTargetGraph1, TargetGraph pTargetGraph2) {
    if (pTargetGraph1 == null || pTargetGraph2 == null) {
      throw new IllegalArgumentException();
    }

    Builder lBuilder = new Builder(pTargetGraph1);

    lBuilder.addInitialNodes(pTargetGraph2.initialNodes());
    lBuilder.addFinalNodes(pTargetGraph2.finalNodes());
    lBuilder.addNodes(pTargetGraph2.getNodes());
    lBuilder.addEdges(pTargetGraph2.getEdges());

    return lBuilder.build();
  }

  public static TargetGraph intersect(TargetGraph pTargetGraph1, TargetGraph pTargetGraph2) {
    if (pTargetGraph1 == null || pTargetGraph2 == null) {
      throw new IllegalArgumentException();
    }

    Builder lBuilder = new Builder();

    for (Node lNode : pTargetGraph1.getNodes()) {
      if (pTargetGraph2.contains(lNode)) {
        lBuilder.addNode(lNode);
      }
    }

    for (Edge lEdge : pTargetGraph1.getEdges()) {
      if (pTargetGraph2.contains(lEdge)) {
        lBuilder.addEdge(lEdge);
      }
    }

    for (Node lInitialNode : pTargetGraph1.initialNodes()) {
      if (pTargetGraph2.isInitialNode(lInitialNode)) {
        lBuilder.addInitialNode(lInitialNode);
      }
    }

    for (Node lFinalNode : pTargetGraph1.finalNodes()) {
      if (pTargetGraph2.isFinalNode(lFinalNode)) {
        lBuilder.addFinalNode(lFinalNode);
      }
    }

    return lBuilder.build();
  }

  public static TargetGraph minus(TargetGraph pTargetGraph1, TargetGraph pTargetGraph2) {
    if (pTargetGraph1 == null || pTargetGraph2 == null) {
      throw new IllegalArgumentException();
    }

    Builder lBuilder = new Builder();

    for (Edge lEdge : pTargetGraph1.getEdges()) {
      if (!pTargetGraph2.contains(lEdge)) {
        lBuilder.addEdge(lEdge);
      }
    }

    for (Node lNode : pTargetGraph1.getNodes()) {
      if (!pTargetGraph2.contains(lNode) || pTargetGraph1.getNumberOfOutgoingEdges(lNode) != 0 || pTargetGraph1.getNumberOfIncomingEdges(lNode) != 0) {
        lBuilder.addNode(lNode);

        if (pTargetGraph1.isInitialNode(lNode)) {
          lBuilder.addInitialNode(lNode);
        }

        if (pTargetGraph1.isFinalNode(lNode)) {
          lBuilder.addFinalNode(lNode);
        }
      }
    }

    return lBuilder.build();
  }

  public static TargetGraph predicate(TargetGraph pTargetGraph, Predicate pPredicate) {
    if (pTargetGraph == null || pPredicate == null) {
      throw new IllegalArgumentException();
    }

    Predicate lNegatedPredicate = new Predicate(pPredicate.getPredicate().negate());
    Builder lBuilder = new Builder();

    // 1) duplicate vertices

    HashMap<Node, Pair<Node, Node>> lMap = new HashMap<>();

    for (Node lNode : pTargetGraph.getNodes()) {
      Node lTrueNode = new Node(lNode);
      lTrueNode.addPredicate(pPredicate);
      lBuilder.addNode(lTrueNode);

      Node lFalseNode = new Node(lNode);
      lFalseNode.addPredicate(lNegatedPredicate);
      lBuilder.addNode(lFalseNode);

      Pair<Node, Node> lPair = Pair.of(lTrueNode, lFalseNode);

      lMap.put(lNode, lPair);
    }

    for (Node lNode : pTargetGraph.initialNodes()) {
      Pair<Node, Node> lPair = lMap.get(lNode);

      lBuilder.addInitialNode(lPair.getFirst());
      lBuilder.addInitialNode(lPair.getSecond());
    }

    for (Node lNode : pTargetGraph.finalNodes()) {
      Pair<Node, Node> lPair = lMap.get(lNode);

      lBuilder.addFinalNode(lPair.getFirst());
      lBuilder.addFinalNode(lPair.getSecond());
    }

    // 2) replicate edges

    for (Edge lEdge : pTargetGraph.getEdges()) {
      Node lSourceNode = lEdge.getSource();
      Pair<Node, Node> lSourcePair = lMap.get(lSourceNode);

      Node lTargetNode = lEdge.getTarget();
      Pair<Node, Node> lTargetPair = lMap.get(lTargetNode);

      Node lSourceTrueNode = lSourcePair.getFirst();
      Node lSourceFalseNode = lSourcePair.getSecond();

      Node lTargetTrueNode = lTargetPair.getFirst();
      Node lTargetFalseNode = lTargetPair.getSecond();

      lBuilder.addEdge(lSourceTrueNode, lTargetTrueNode, lEdge.getCFAEdge());
      lBuilder.addEdge(lSourceTrueNode, lTargetFalseNode, lEdge.getCFAEdge());
      lBuilder.addEdge(lSourceFalseNode, lTargetTrueNode, lEdge.getCFAEdge());
      lBuilder.addEdge(lSourceFalseNode, lTargetFalseNode, lEdge.getCFAEdge());
    }

    return lBuilder.build();
  }

  /*
   * Returns a target graph that retains all nodes and edges in pTargetGraph that
   * belong the the function given by pFunctionName. The set of initial nodes is
   * changed to the set of nodes in the resulting target graph that contain a
   * CFAFunctionDefinitionNode. The set of final nodes is changed to the set of
   * nodes in the resulting target graph that contain a CFAFunctionExitNode.
   */
  public static TargetGraph applyFunctionNameFilter(TargetGraph pTargetGraph, String pFunctionName) {
    if (pTargetGraph == null || pFunctionName == null) {
      throw new IllegalArgumentException();
    }

    MaskFunctor<Node, Edge> lMaskFunctor = new FunctionNameMaskFunctor(pFunctionName);

    Builder lBuilder = new Builder(pTargetGraph, lMaskFunctor);

    for (Node lNode : lBuilder.nodes()) {
      CFANode lCFANode = lNode.getCFANode();

      if (lCFANode instanceof CFunctionEntryNode) {
        lBuilder.addInitialNode(lNode);
      }

      if (lCFANode instanceof FunctionExitNode) {
        lBuilder.addFinalNode(lNode);
      }
    }

    return lBuilder.build();
  }

  public static TargetGraph applyStandardEdgeBasedFilter(TargetGraph pTargetGraph, MaskFunctor<Node, Edge> pMaskFunctor) {
    if (pTargetGraph == null || pMaskFunctor == null) {
      throw new IllegalArgumentException();
    }

    Builder lBuilder = new Builder(pTargetGraph, pMaskFunctor);

    for (Edge lEdge : lBuilder.edges()) {
      lBuilder.addInitialNode(lEdge.getSource());
      lBuilder.addFinalNode(lEdge.getTarget());
    }

    return lBuilder.build();
  }

}