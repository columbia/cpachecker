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
package org.sosy_lab.cpachecker.core.algorithm.testgen.analysis;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.testgen.StartupConfig;
import org.sosy_lab.cpachecker.core.algorithm.testgen.TestGenStatistics;
import org.sosy_lab.cpachecker.core.algorithm.testgen.model.PredicatePathAnalysisResult;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;


public class DARTLikeBasicPathSelector implements TestGenPathAnalysisStrategy {

  private TestGenStatistics stats;
  ConfigurableProgramAnalysis cpa;
  private LogManager logger;
  private BranchingHistory branchingHistory;
  private PathChecker pathChecker;


  public DARTLikeBasicPathSelector(PathChecker pPathChecker, StartupConfig config, TestGenStatistics pStats, ConfigurableProgramAnalysis pCpa) {
    super();
    this.pathChecker = pPathChecker;
    this.logger = config.getLog();
    stats = pStats;
    branchingHistory = new BranchingHistory();
  }


  @Override
  public PredicatePathAnalysisResult findNewFeasiblePathUsingPredicates(final ARGPath pExecutedPath,
      ReachedSet reachedStates)
      throws CPATransferException, InterruptedException {
    /*
     * create copy of the given path, because it will be modified with this algorithm.
     * represents the current new valid path.
     */
    ARGPath newARGPath = new ARGPath();
    for (Pair<ARGState, CFAEdge> pair : pExecutedPath) {
      newARGPath.add(pair);
    }
    int pathSize = newARGPath.size();
    int oldPathSize = branchingHistory.getPathDepths();
    //TODO a unmodifiable view would be preferable as the pathValidor argument
    //    ARGPath newARGPathView = Collections.unmodifiableList(newARGPath);
    /*
     * only by edge representation of the new path.
     */
    List<CFAEdge> newPath = Lists.newArrayList(newARGPath.asEdgesList());
    /*
     * element removed from the path in the previous iteration
     */
    Pair<ARGState, CFAEdge> lastElement = null;
    Pair<ARGState, CFAEdge> currentElement;
    long branchCounter = 0;
    long nodeCounter = 0;
    /*
     * this is a variation of the solve_path_constraint(..., path_constraint, stack) function of DART.
     *
     */
    /*
     * create a descending consuming iterator to iterate through the path from last to first, while consuming elements.
     * Elements are consumed because the new path is a subpath of the original.
     */
    Iterator<Pair<ARGState, CFAEdge>> descendingPathElements =
        Iterators.consumingIterator(newARGPath.descendingIterator());
    while (descendingPathElements.hasNext())
    {
      //      nodeCounter++;
      currentElement = descendingPathElements.next();
      CFAEdge edge = currentElement.getSecond();
      if (edge == null) {
        lastElement = currentElement;
        nodeCounter++;
        continue;
      }
      CFANode node = edge.getPredecessor();
      CFAEdge oldEdge = null;
      //num of leaving edges does not include a summary edge, so the check is valid.
      nodeCounter++;
      if (pathSize - nodeCounter <= branchingHistory.getPathDepths() -1)
      {
        if(branchingHistory.hasNext()) {
          oldEdge = branchingHistory.next();
        }
      }
      if (node.getNumLeavingEdges() != 2) {
        lastElement = currentElement;
        continue;
      }
      /*
       * current node is a branching / deciding node. select the edge that isn't represented
       * with the current path.
       */
      branchCounter++;
      CFANode decidingNode = node;
      CFAEdge wrongEdge = edge;

      /*
       * (DART: negate the path constraint)
       */
      CFAEdge otherEdge = getOtherOutgoingEdge(decidingNode, wrongEdge);
//      if(branchingHistory.isMatch(otherEdge, oldEdge))

      logger.logf(Level.INFO, "StackState: %d %d (%d)", pathSize - nodeCounter,branchingHistory.getCurrentDepths(), branchingHistory.getPathDepths());
      boolean isSameSize = pathSize - nodeCounter == branchingHistory.getPathDepths()-1;
      /*
       * (DART: the j = -1 case)
       */
      //      if(pathValidator.isVisitedBranching(newARGPath, currentElement, node, otherEdge))
      if (oldEdge!=null && isSameSize)
      {
        logger.log(Level.INFO, "Matching path length. Possibly handled this branch earlier");
        if(branchingHistory.isPredecessorMatch(otherEdge, oldEdge)) {
          logger.log(Level.INFO, "Branch on path was handled in an earlier iteration -> skipping branching.");
          lastElement = currentElement;
          continue;
        }
      }

      if (lastElement == null)
      {
        /*
         * if the last element is not set, we encountered a branching node where both paths are infeasible
         * for the current value mapping or both successors were handled already with a previous iteration.
         * (the successors are in reached and the CPAAlgorithms stops if all successors were reached before).
         */
        logger.log(Level.INFO,
            "encountered an executed path that continues into an already reached region. -> Skipping");
        lastElement = currentElement;
        continue;
      }
      logger.logf(Level.INFO, "identified valid branching (skipped branching count: %d, nodes: %d)", branchCounter,
          nodeCounter);
      //no edge found should not happen; If it does make it visible.
      assert otherEdge != null;
      /*
       * identified a decision node and selected a new edge.
       * extract the edge-list of the path and add the new edge to it.
       * Don't modify the ARGPath yet, because it is possible that the current decision is infeasible
       */
      newPath = Lists.newArrayList(newARGPath.asEdgesList());
      newPath.add(otherEdge);
      /*
       * evaluate path candidate symbolically using SMT-solving
       */
      stats.beforePathCheck();
      CounterexampleTraceInfo traceInfo = pathChecker.checkPath(newPath);
      stats.afterPathCheck();
      /*
       * check if path is feasible. If it's not continue to identify another decision node
       * If path is feasible, add the ARGState belonging to the decision node and the new edge to the ARGPath. Exit and Return result.
       */
      if (!traceInfo.isSpurious())
      {
        newARGPath.add(Pair.of(currentElement.getFirst(), otherEdge));
        //TODO maybe add the ARGState matching the "otherEdge" path if available as last element to the path.
        logger.logf(Level.INFO, "selected new path %s", newPath.toString());
        //        pathValidator.handleValidPath(newARGPath, traceInfo);
        branchingHistory.resetTo(newARGPath);
        return new PredicatePathAnalysisResult(traceInfo, currentElement.getFirst(), lastElement.getFirst(), newARGPath);
      }
      else {
        lastElement = currentElement;
        logger.logf(Level.INFO, "path candidate is infeasible");
        //        pathValidator.handleSpuriousPath(newPath);
        continue;
      }

    }
    //all possible paths explored. (DART: the j = -1 case)
    logger.logf(Level.INFO, "No possible path left to explore");
    return PredicatePathAnalysisResult.INVALID;
  }



  @Override
  public CounterexampleTraceInfo computePredicateCheck(ARGPath pExecutedPath) throws CPATransferException,
      InterruptedException {
    return pathChecker.checkPath(pExecutedPath.asEdgesList()
        );
  }

  /**
   * this is the same as a constraint negation.
   * @param decidingNode
   * @param wrongEdge
   * @return
   */
  private @Nullable
  CFAEdge getOtherOutgoingEdge(CFANode decidingNode, CFAEdge wrongEdge) {
    CFAEdge otherEdge = null;
    for (CFAEdge cfaEdge : CFAUtils.leavingEdges(decidingNode)) {
      if (cfaEdge.equals(wrongEdge)) {
        continue;
      } else {
        otherEdge = cfaEdge;
        break;
      }
    }
    return otherEdge;
  }

  class BranchingHistory {

    Iterator<CFAEdge> descendingEdgePath;
    int pathDepths = 0;
    int currentDepths = 0;


    public BranchingHistory() {
      descendingEdgePath = Iterators.emptyIterator();
    }


    public boolean isPredecessorMatch(CFAEdge pOtherEdge, CFAEdge pOldEdge) {
      return pOtherEdge.getPredecessor().equals(pOldEdge.getPredecessor());
    }

    public boolean isMatch(CFAEdge pOtherEdge, CFAEdge pOldEdge) {
      return pOtherEdge.equals(pOldEdge);
    }


    public void resetTo(ARGPath argPath) {
      descendingEdgePath = Iterators.transform(argPath.descendingIterator(), Pair.<CFAEdge> getProjectionToSecond());
      pathDepths = argPath.size();
      currentDepths = pathDepths;
    }


    public boolean isLastVisited(CFAEdge edgeToCheck) {
      if (descendingEdgePath.hasNext()) { return descendingEdgePath.next().getPredecessor()
          .equals(edgeToCheck.getPredecessor()); }
      return false;
    }

    public boolean hasNext(){
      return descendingEdgePath.hasNext();
    }
    public CFAEdge next(){
      currentDepths--;
      return descendingEdgePath.next();
    }

    public int getPathDepths() {
      return pathDepths;
    }

    public int getCurrentDepths() {
      return currentDepths;
    }

    public void setPathDepths(int pPathDepths) {
      pathDepths = pPathDepths;
      if(currentDepths >= pathDepths) {
        currentDepths = pathDepths;
      }
    }

  }

}