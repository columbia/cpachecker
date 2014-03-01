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
package org.sosy_lab.cpachecker.cpa.value.refiner;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisPrecision;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.MemoryLocation;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.AssumptionUseDefinitionCollector;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisFeasibilityChecker;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.Precisions;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

/**
 * Refiner implementation that extracts a precision increment solely based on syntactical information from error traces.
 */
@Options(prefix="cpa.value.refiner")
public class ValueAnalysisUseDefinitionBasedRefiner extends AbstractARGBasedRefiner implements Statistics, StatisticsProvider {
  // statistics
  private int numberOfRefinements           = 0;
  private int numberOfSuccessfulRefinements = 0;

  private final CFA cfa;

  private final LogManager logger;

  public static ValueAnalysisUseDefinitionBasedRefiner create(ConfigurableProgramAnalysis cpa) throws CPAException, InvalidConfigurationException {
    if (!(cpa instanceof WrapperCPA)) {
      throw new InvalidConfigurationException(ValueAnalysisUseDefinitionBasedRefiner.class.getSimpleName() + " could not find the ValueAnalysisCPA");
    }

    ValueAnalysisCPA valueAnalysisCpa = ((WrapperCPA)cpa).retrieveWrappedCpa(ValueAnalysisCPA.class);
    if (valueAnalysisCpa == null) {
      throw new InvalidConfigurationException(ValueAnalysisUseDefinitionBasedRefiner.class.getSimpleName() + " needs a ValueAnalysisCPA");
    }

    ValueAnalysisUseDefinitionBasedRefiner refiner = initialiseRefiner(cpa, valueAnalysisCpa);
    valueAnalysisCpa.getStats().addRefiner(refiner);

    return refiner;
  }

  private static ValueAnalysisUseDefinitionBasedRefiner initialiseRefiner(
      ConfigurableProgramAnalysis cpa, ValueAnalysisCPA pValueAnalysisCpa)
          throws CPAException, InvalidConfigurationException {
    Configuration config = pValueAnalysisCpa.getConfiguration();
    LogManager logger    = pValueAnalysisCpa.getLogger();

    return new ValueAnalysisUseDefinitionBasedRefiner(
        config,
        logger,
        pValueAnalysisCpa.getShutdownNotifier(),
        cpa,
        pValueAnalysisCpa.getStaticRefiner(),
        pValueAnalysisCpa.getCFA());
  }

  protected ValueAnalysisUseDefinitionBasedRefiner(
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final ConfigurableProgramAnalysis pCpa,
      ValueAnalysisStaticRefiner pValueAnalysisStaticRefiner,
      final CFA pCfa) throws CPAException, InvalidConfigurationException {
    super(pCpa);
    pConfig.inject(this);

    cfa    = pCfa;
    logger = pLogger;
  }

  @Override
  protected CounterexampleInfo performRefinement(final ARGReachedSet reached, final ARGPath errorPath)
      throws CPAException, InterruptedException {

    // if path is infeasible, try to refine the precision
    if (isPathFeasable(errorPath)) {
      return CounterexampleInfo.feasible(errorPath, null);
    }

    else if (performValueAnalysisRefinement(reached, errorPath)) {
      return CounterexampleInfo.spurious();
    }

    throw new RefinementFailedException(Reason.RepeatedCounterexample, errorPath);
  }

  /**
   * This method performs an value-analysis refinement.
   *
   * @param reached the current reached set
   * @param errorPath the current error path
   * @returns true, if the value-analysis refinement was successful, else false
   * @throws CPAException when value-analysis interpolation fails
   */
  private boolean performValueAnalysisRefinement(final ARGReachedSet reached, final ARGPath errorPath) throws CPAException, InterruptedException {
    numberOfRefinements++;

    UnmodifiableReachedSet reachedSet             = reached.asReachedSet();
    Precision precision                           = reachedSet.getPrecision(reachedSet.getLastState());
    ValueAnalysisPrecision valueAnalysisPrecision = Precisions.extractPrecisionByType(precision, ValueAnalysisPrecision.class);

    List<CFAEdge> cfaTrace = Lists.newArrayList();
    for(Pair<ARGState, CFAEdge> elem : errorPath) {
      cfaTrace.add(elem.getSecond());
    }

    Multimap<CFANode, MemoryLocation> increment = HashMultimap.create();
    for(String var : new AssumptionUseDefinitionCollector().obtainUseDefInformation(cfaTrace)) {
      String[] s = var.split("::");

      // just add to BOGUS LOCATION
      increment.put(cfaTrace.get(0).getSuccessor(), (s.length == 1)
                                                      ? MemoryLocation.valueOf(s[0])
                                                      : MemoryLocation.valueOf(s[0], s[1], 0));
    }

    // no increment - Refinement was not successful
    if(increment.isEmpty()) {
      return false;
    }

    ValueAnalysisPrecision refinedValueAnalysisPrecision = new ValueAnalysisPrecision(valueAnalysisPrecision, increment);

    ArrayList<Precision> refinedPrecisions = new ArrayList<>(2);
    refinedPrecisions.add(refinedValueAnalysisPrecision);

    ArrayList<Class<? extends Precision>> newPrecisionTypes = new ArrayList<>(2);
    newPrecisionTypes.add(ValueAnalysisPrecision.class);

    numberOfSuccessfulRefinements++;
    reached.removeSubtree(errorPath.get(1).getFirst(), refinedPrecisions, newPrecisionTypes);
    return true;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(this);
  }

  @Override
  public String getName() {
    return "ValueAnalysisUseDefinitionBasedRefiner";
  }

  @Override
  public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
    out.println("  number of refinements:                      " + numberOfRefinements);
    out.println("  number of successful refinements:           " + numberOfSuccessfulRefinements);
  }

  /**
   * This method checks if the given path is feasible, when doing a full-precision check.
   *
   * @param path the path to check
   * @return true, if the path is feasible, else false
   * @throws CPAException if the path check gets interrupted
   */
  boolean isPathFeasable(ARGPath path) throws CPAException {
    try {
      // create a new ValueAnalysisChecker, which does check the given path at full precision
      ValueAnalysisFeasibilityChecker checker = new ValueAnalysisFeasibilityChecker(logger, cfa);

      return checker.isFeasible(path);
    }
    catch (InterruptedException | InvalidConfigurationException e) {
      throw new CPAException("counterexample-check failed: ", e);
    }
  }
}