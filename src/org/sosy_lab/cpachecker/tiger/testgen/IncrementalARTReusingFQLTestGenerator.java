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
package org.sosy_lab.cpachecker.tiger.testgen;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.TimeAccumulator;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.algorithm.CEGARAlgorithmWithCounterexampleInfo;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.core.reachedset.LocationMappedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.PartitionedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGStatistics;
import org.sosy_lab.cpachecker.cpa.assume.AssumeCPA;
import org.sosy_lab.cpachecker.cpa.cache.CacheCPA;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.cfapath.CFAPathCPA;
import org.sosy_lab.cpachecker.cpa.cfapath.CFAPathStandardState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.productautomaton.ProductAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.productautomaton.ProductAutomatonElement;
import org.sosy_lab.cpachecker.cpa.interpreter.InterpreterCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPARefiner;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateRefiner;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.tiger.artreuse.ARTReuse;
import org.sosy_lab.cpachecker.tiger.clustering.ClusteredElementaryCoveragePattern;
import org.sosy_lab.cpachecker.tiger.clustering.InfeasibilityPropagation;
import org.sosy_lab.cpachecker.tiger.core.CPAtiger;
import org.sosy_lab.cpachecker.tiger.core.CPAtigerResult;
import org.sosy_lab.cpachecker.tiger.fql.FQLSpecificationUtil;
import org.sosy_lab.cpachecker.tiger.fql.ast.Edges;
import org.sosy_lab.cpachecker.tiger.fql.ast.FQLSpecification;
import org.sosy_lab.cpachecker.tiger.fql.ecp.ElementaryCoveragePattern;
import org.sosy_lab.cpachecker.tiger.fql.ecp.SingletonECPEdgeSet;
import org.sosy_lab.cpachecker.tiger.fql.ecp.translators.GuardedEdgeLabel;
import org.sosy_lab.cpachecker.tiger.fql.ecp.translators.InverseGuardedEdgeLabel;
import org.sosy_lab.cpachecker.tiger.fql.ecp.translators.ToGuardedAutomatonTranslator;
import org.sosy_lab.cpachecker.tiger.fql.translators.ecp.ClusteringCoverageSpecificationTranslator;
import org.sosy_lab.cpachecker.tiger.fql.translators.ecp.CoverageSpecificationTranslator;
import org.sosy_lab.cpachecker.tiger.fql.translators.ecp.IncrementalCoverageSpecificationTranslator;
import org.sosy_lab.cpachecker.tiger.interfaces.FQLTestGenerator;
import org.sosy_lab.cpachecker.tiger.testcases.ImpreciseExecutionException;
import org.sosy_lab.cpachecker.tiger.testcases.TestCase;
import org.sosy_lab.cpachecker.tiger.testcases.TestSuite;
import org.sosy_lab.cpachecker.tiger.util.FeasibilityInformation;
import org.sosy_lab.cpachecker.tiger.util.Goal;
import org.sosy_lab.cpachecker.tiger.util.ThreeValuedAnswer;
import org.sosy_lab.cpachecker.tiger.util.Wrapper;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

/*
 * TODO AutomatonBuilder <- integrate State-Pool there to ensure correct time
 * measurements when invoking FlleSh several times in a unit test.
 *
 * TODO Incremental test goal automaton creation: extending automata (can we reuse
 * parts of the reached set?) This requires a change in the coverage check.
 * -> Handle enormous amounts of test goals.
 */

public class IncrementalARTReusingFQLTestGenerator implements FQLTestGenerator {

  private final Configuration mConfiguration;
  private final LogManager mLogManager;
  private final Wrapper mWrapper;
  private final CoverageSpecificationTranslator mCoverageSpecificationTranslator;
  private final LocationCPA mLocationCPA;
  private final CallstackCPA mCallStackCPA;
  private final AssumeCPA mAssumeCPA;
  private final CFAPathCPA mCFAPathCPA;
  private final ConfigurableProgramAnalysis mPredicateCPA;

  private final TimeAccumulator mTimeInReach;
  private int mTimesInReach;
  private final GuardedEdgeLabel mAlphaLabel;
  private final GuardedEdgeLabel mOmegaLabel;
  private final GuardedEdgeLabel mInverseAlphaLabel;
  private final Map<TestCase, CFAEdge[]> mGeneratedTestCases;

  private int mMinIndex = 0;
  private int mMaxIndex = Integer.MAX_VALUE;

  private boolean mDoRestart = false;
  private long mRestartBound = 100000000; // 100 MB

  private boolean mUseAutomatonOptimization = true;
  private boolean mUseGraphCPA = false; // TODO disabled it since it causes a bug when doing FQL queries with PASSING clause
  private boolean mReuseART = true;
  private boolean mUseInfeasibilityPropagation = true;

  private boolean mPrintPredicateStatistics = false;

  private FeasibilityInformation mFeasibilityInformation;
  private TestSuite mTestSuite;

  private PrintStream mOutput = System.out;

  private ShutdownNotifier mShutdownNotifier;

  private static IncrementalARTReusingFQLTestGenerator INSTANCE = null;

  // TODO replace this by better code architecture
  public static IncrementalARTReusingFQLTestGenerator getInstance() {
    assert (INSTANCE != null);

    return INSTANCE;
  }

  public void setOutput(PrintStream pOutput) {
    mOutput = pOutput;
  }

  public void setGoalIndices(int pMinIndex, int pMaxIndex) {
    mMinIndex = pMinIndex;
    mMaxIndex = pMaxIndex;
  }

  public void doRestart() {
    mDoRestart = true;
  }

  public void setRestartBound(long pRestartBound) {
    mRestartBound = pRestartBound;
  }

  public void setFeasibilityInformation(FeasibilityInformation pFeasibilityInformation) {
    mFeasibilityInformation = pFeasibilityInformation;
  }

  public void setTestSuite(TestSuite pTestSuite) throws InvalidConfigurationException, CPAException, ImpreciseExecutionException {
    mTestSuite = pTestSuite;
    seed(pTestSuite);
  }

  public void seed(Iterable<TestCase> pTestSuite) throws InvalidConfigurationException, CPAException, ImpreciseExecutionException {
    FQLSpecification lIdStarFQLSpecification;
    try {
      lIdStarFQLSpecification = FQLSpecification.parse("COVER \"EDGES(ID)*\" PASSING EDGES(ID)*");
    } catch (Exception e1) {
      throw new RuntimeException(e1);
    }
    ElementaryCoveragePattern lIdStarPattern = mCoverageSpecificationTranslator.mPathPatternTranslator.translate(lIdStarFQLSpecification.getPathPattern());
    NondeterministicFiniteAutomaton<GuardedEdgeLabel> lAutomaton = ToGuardedAutomatonTranslator.toAutomaton(lIdStarPattern, mAlphaLabel, mInverseAlphaLabel, mOmegaLabel);
    GuardedEdgeAutomatonCPA lIdStarCPA = new GuardedEdgeAutomatonCPA(lAutomaton);

    for (TestCase lTestCase : pTestSuite) {
      CFAEdge[] lPath = reconstructPath(mWrapper.getCFA(), lTestCase, mWrapper.getEntry(), lIdStarCPA, null, mWrapper.getOmegaEdge().getSuccessor());

      mGeneratedTestCases.put(lTestCase, lPath);
    }
  }

  public IncrementalARTReusingFQLTestGenerator(String pSourceFileName, String pEntryFunction, ShutdownNotifier shutdownNotifier) {
    assert (INSTANCE == null);

    INSTANCE = this;

    mShutdownNotifier = shutdownNotifier;

    CFA lCFA;

    try {
      mConfiguration = CPAtiger.createConfiguration(pSourceFileName, pEntryFunction);
      mLogManager = new BasicLogManager(mConfiguration);

      lCFA = CPAtiger.getCFA(pSourceFileName, mConfiguration, mLogManager);
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    }

    /*
     * We have to instantiate mCoverageSpecificationTranslator before the wrapper
     * changes the underlying CFA. FQL specifications are evaluated against the
     * target graph generated during initialization of mCoverageSpecificationTranslator.
     */
    mCoverageSpecificationTranslator = new CoverageSpecificationTranslator(lCFA.getFunctionHead(pEntryFunction));

    mWrapper = new Wrapper(lCFA, pEntryFunction);

    mAlphaLabel = new GuardedEdgeLabel(new SingletonECPEdgeSet(mWrapper.getAlphaEdge()));
    mInverseAlphaLabel = new InverseGuardedEdgeLabel(mAlphaLabel);
    mOmegaLabel = new GuardedEdgeLabel(new SingletonECPEdgeSet(mWrapper.getOmegaEdge()));


    /*
     * Initialize shared CPAs.
     */
    // location CPA
    GlobalInfo.getInstance().storeCFA(lCFA);
    mLocationCPA = new LocationCPA(lCFA);


    // callstack CPA
    try {
      mCallStackCPA = new CallstackCPA(mConfiguration, mLogManager);
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    }

    // assume CPA
    mAssumeCPA = AssumeCPA.getCBMCAssume();

    // cfa path CPA
    mCFAPathCPA = CFAPathCPA.getInstance();

    // TODO make configurable
    // ... cache does not work well for big examples
    boolean lUseCache = false;

    // predicate abstraction CPA
    CPAFactory lPredicateCPAFactory = PredicateCPA.factory();
    lPredicateCPAFactory.set(lCFA, CFA.class);
    lPredicateCPAFactory.setConfiguration(mConfiguration);
    lPredicateCPAFactory.setLogger(mLogManager);
    lPredicateCPAFactory.set(shutdownNotifier, ShutdownNotifier.class);
    ReachedSetFactory lReachedSetFactory;
    try {
      lReachedSetFactory = new ReachedSetFactory(mConfiguration, mLogManager);
    } catch (InvalidConfigurationException e1) {
      throw new RuntimeException(e1);
    }
    lPredicateCPAFactory.set(lReachedSetFactory, ReachedSetFactory.class);

    try {
      ConfigurableProgramAnalysis lPredicateCPA = lPredicateCPAFactory.createInstance();

      if (lUseCache) {
        mPredicateCPA = new CacheCPA(lPredicateCPA);
      }
      else {
        mPredicateCPA = lPredicateCPA;
      }
    } catch (InvalidConfigurationException | CPAException e) {
      throw new RuntimeException(e);
    }

    mTimeInReach = new TimeAccumulator();
    mTimesInReach = 0;

    // we can collect test cases accross several run invocations and use them for coverage analysis
    // TODO output test cases from an earlier run
    mGeneratedTestCases = new HashMap<>();
  }

  @Override
  public CPAtigerResult run(String pFQLSpecification, boolean pApplySubsumptionCheck, boolean pApplyInfeasibilityPropagation, boolean pGenerateTestGoalAutomataInAdvance, boolean pCheckCorrectnessOfCoverageCheck, boolean pPedantic, boolean pAlternating) {
    return run(pFQLSpecification, pApplySubsumptionCheck, pApplyInfeasibilityPropagation, pCheckCorrectnessOfCoverageCheck, pPedantic);
  }

  private GuardedEdgeAutomatonCPA getPassingCPA(FQLSpecification pFQLSpecification) {
    if (pFQLSpecification.hasPassingClause()) {
      mOutput.println("Cache hits (1): " + mCoverageSpecificationTranslator.getOverallCacheHits());
      mOutput.println("Cache misses (1): " + mCoverageSpecificationTranslator.getOverallCacheMisses());

      ElementaryCoveragePattern lPassingClause = mCoverageSpecificationTranslator.mPathPatternTranslator.translate(pFQLSpecification.getPathPattern());

      mOutput.println("Cache hits (2): " + mCoverageSpecificationTranslator.getOverallCacheHits());
      mOutput.println("Cache misses (2): " + mCoverageSpecificationTranslator.getOverallCacheMisses());

      NondeterministicFiniteAutomaton<GuardedEdgeLabel> lAutomaton1 = ToGuardedAutomatonTranslator.toAutomaton(lPassingClause, mAlphaLabel, mInverseAlphaLabel, mOmegaLabel);

      NondeterministicFiniteAutomaton<GuardedEdgeLabel> lAutomaton2 = FQLSpecificationUtil.optimizeAutomaton(lAutomaton1, mUseAutomatonOptimization);

      return new GuardedEdgeAutomatonCPA(lAutomaton2);
    }
    else {
      return null;
    }
  }

  private boolean applyCoverageCheck(Goal pGoal, NondeterministicFiniteAutomaton<GuardedEdgeLabel> pGoalAutomaton, GuardedEdgeAutomatonCPA pPassingCPA, CPAtigerResult.Factory pResultFactory) {
    for (Map.Entry<TestCase, CFAEdge[]> lGeneratedTestCase : mGeneratedTestCases.entrySet()) {
      TestCase lTestCase = lGeneratedTestCase.getKey();

      if (!lTestCase.isPrecise()) {
        //throw new RuntimeException();
        continue; // don't use imprecise test cases for coverage
      }

      ThreeValuedAnswer lCoverageAnswer = CPAtiger.accepts(pGoalAutomaton, lGeneratedTestCase.getValue());

      if (lCoverageAnswer.equals(ThreeValuedAnswer.ACCEPT)) {
        pResultFactory.addFeasibleTestCase(pGoal.getPattern(), lTestCase);

        return true;
      }
      else if (lCoverageAnswer.equals(ThreeValuedAnswer.UNKNOWN)) {
        // TODO reuse this CPA in run method
        GuardedEdgeAutomatonCPA lAutomatonCPA = new GuardedEdgeAutomatonCPA(pGoalAutomaton);

        try {
          if (checkCoverage(lTestCase, mWrapper.getEntry(), lAutomatonCPA, pPassingCPA, mWrapper.getOmegaEdge().getSuccessor())) {
            pResultFactory.addFeasibleTestCase(pGoal.getPattern(), lTestCase);

            return true;
          }
        } catch (InvalidConfigurationException | CPAException | ImpreciseExecutionException e) {
          throw new RuntimeException(e);
        }
      }
    }

    return false;
  }

  private CPAtigerResult run(String pFQLSpecification, boolean pApplySubsumptionCheck, boolean pApplyInfeasibilityPropagation, boolean pCheckReachWhenCovered, boolean pPedantic) {

    int lNumberOfTestGoals;

    FQLSpecification lFQLSpecification = FQLSpecificationUtil.getFQLSpecification(pFQLSpecification);

    Pair<Boolean, LinkedList<Edges>> lInfeasibilityPropagation;

    if (mUseInfeasibilityPropagation) {
      lInfeasibilityPropagation = InfeasibilityPropagation.canApplyInfeasibilityPropagation(lFQLSpecification);
    }
    else {
      lInfeasibilityPropagation = Pair.of(Boolean.FALSE, null);
    }

    ElementaryCoveragePattern[] lGoalPatterns;

    GuardedEdgeAutomatonCPA lPassingCPA = getPassingCPA(lFQLSpecification);

    // set up utility variables
    int lFeasibleTestGoalsTimeSlot = 0;
    int lInfeasibleTestGoalsTimeSlot = 1;

    TimeAccumulator lTimeAccu = new TimeAccumulator(2);

    TimeAccumulator lTimeReach = new TimeAccumulator();
    TimeAccumulator lTimeCover = new TimeAccumulator();


    boolean lUseGraphCPAOld = mUseGraphCPA;

    if (lInfeasibilityPropagation.getFirst()) {
      // deactivate graph search ... experiments showed graph search useless when we use infeasibility propagation
      // TODO investigate that issue in more detail
      mUseGraphCPA = false;

      CFANode lInitialNode = this.mAlphaLabel.getEdgeSet().iterator().next().getSuccessor();

      ClusteringCoverageSpecificationTranslator lTranslator = new ClusteringCoverageSpecificationTranslator(mCoverageSpecificationTranslator.mPathPatternTranslator, lInfeasibilityPropagation.getSecond(), lInitialNode);

      lNumberOfTestGoals = lTranslator.getNumberOfTestGoals();

      mOutput.println("Number of Test Goals: " + lNumberOfTestGoals);
      mOutput.print("Generating Patterns ... ");

      lGoalPatterns = lTranslator.createElementaryCoveragePatternsAndClusters();

      /*IncrementalCoverageSpecificationTranslator lTranslator2 = new IncrementalCoverageSpecificationTranslator(mCoverageSpecificationTranslator.mPathPatternTranslator);
      Iterator<ElementaryCoveragePattern> lGoalIterator = lTranslator2.translate(lFQLSpecification.getCoverageSpecification());
      for (int lGoalIndex = 0; lGoalIndex < lNumberOfTestGoals; lGoalIndex++) {
        ClusteredElementaryCoveragePattern lGoal1 = (ClusteredElementaryCoveragePattern)lGoalPatterns[lGoalIndex];
        ECPConcatenation lGoal2 = (ECPConcatenation)lGoalIterator.next();

        ECPConcatenation lGoal1Prime = (ECPConcatenation)lGoal1.getWrappedPattern();

        if (lGoal2.size() != lGoal1Prime.size()) {
          System.out.println("sizes " + lGoal2.size() + " vs. " + lGoal1Prime.size());
          //throw new RuntimeException("sizes " + lGoal2.size() + " vs. " + lGoal1Prime.size());
        }

        for (int u = 0; u < lGoal2.size(); u++) {
          if (!lGoal2.get(u).equals(lGoal1Prime.get(u))) {
            throw new RuntimeException("moohoohoo");
          }
        }

        if (!lGoal1Prime.equals(lGoal2)) {
          throw new RuntimeException("unequality at index " + lGoalIndex);
        }
      }*/

      mOutput.println("done.");
    }
    else {
      IncrementalCoverageSpecificationTranslator lTranslator = new IncrementalCoverageSpecificationTranslator(mCoverageSpecificationTranslator.mPathPatternTranslator);

      mOutput.println("Determining the number of test goals ...");

      lNumberOfTestGoals = lTranslator.getNumberOfTestGoals(lFQLSpecification.getCoverageSpecification());
      mOutput.println("Number of test goals: " + lNumberOfTestGoals);

      Iterator<ElementaryCoveragePattern> lGoalIterator = lTranslator.translate(lFQLSpecification.getCoverageSpecification());

      lGoalPatterns = new ElementaryCoveragePattern[lNumberOfTestGoals];

      for (int lGoalIndex = 0; lGoalIndex < lNumberOfTestGoals; lGoalIndex++) {
        lGoalPatterns[lGoalIndex] = lGoalIterator.next();
      }
    }


    int lNumberOfCFAInfeasibleGoals = 0;

    ReachedSet lPredicateReachedSet = new LocationMappedReachedSet(Waitlist.TraversalMethod.DFS); // TODO why does TOPSORT not exist anymore?
    NondeterministicFiniteAutomaton<GuardedEdgeLabel> lPreviousGoalAutomaton = null;

    ReachedSet lGraphReachedSet = new LocationMappedReachedSet(Waitlist.TraversalMethod.DFS);
    NondeterministicFiniteAutomaton<GuardedEdgeLabel> lPreviousGraphGoalAutomaton = null;

    CPAtigerResult.Factory lResultFactory = CPAtigerResult.factory();

    int lIndex = 0;

    boolean pHadProgress = false;

    long[] lGoalRuntime = new long[lNumberOfTestGoals];
    InfeasibilityPropagation.Prediction[] lGoalPrediction = new InfeasibilityPropagation.Prediction[lNumberOfTestGoals];

    for (int i = 0; i < lGoalRuntime.length; i++) {
      lGoalRuntime[i] = -1; // value indicating invalid value
      lGoalPrediction[i] = InfeasibilityPropagation.Prediction.UNKNOWN; // value indicating unknown prediction
    }

    //while (lGoalIterator.hasNext()) {
    while (lIndex < lGoalPatterns.length) {
      if (lIndex > 0) {
        mOutput.println("Goal #" + (lIndex) + " needed " + lGoalRuntime[lIndex - 1] + " ms");
      }

      long lStartTime = System.currentTimeMillis();

      if (mDoRestart) {
        throw new RuntimeException("IMPLEMENT RESTART FUNCTIONALITY PROPERLY");

        /*try {
          Sigar lSigar = new Sigar();

          Mem lMemory = lSigar.getMem();

          if (pHadProgress && lMemory.getFree() < mRestartBound) {
            mOutput.println("SHUTDOWN TEST GENERATION");

            lResultFactory.setUnfinished();

            break;
          }
        } catch (SigarException e) {
          throw new RuntimeException(e);
        }*/
      }

      lIndex++;


      if (lGoalPrediction[lIndex - 1].equals(InfeasibilityPropagation.Prediction.INFEASIBLE)) {
        mOutput.println("Predicted as infeasible!");

        mFeasibilityInformation.setStatus(lIndex, FeasibilityInformation.FeasibilityStatus.INFEASIBLE);

        continue;
      }


      //ElementaryCoveragePattern lGoalPattern = lGoalIterator.next();
      ElementaryCoveragePattern lGoalPattern = lGoalPatterns[lIndex - 1];

      mOutput.println("Processing test goal #" + lIndex + " of " + lNumberOfTestGoals + " test goals.");

      // TODO change while loop to for loop
      if (mMinIndex > lIndex) {
        mOutput.println("Skipped.");
        continue;
      }

      if (mMaxIndex < lIndex) {
        // we do not have to enumerate unnecessary test goals
        mOutput.println("Stop test goal enumeration.");
        break;
      }

      if (mFeasibilityInformation.isKnown(lIndex)) {
        mOutput.println("Stored information: " + mFeasibilityInformation.getStatus(lIndex));
        continue;
      }

      pHadProgress = true;

      Goal lGoal = new Goal(lGoalPattern, mAlphaLabel, mInverseAlphaLabel, mOmegaLabel);

      NondeterministicFiniteAutomaton<GuardedEdgeLabel> lGoalAutomaton = FQLSpecificationUtil.optimizeAutomaton(lGoal.getAutomaton(), mUseAutomatonOptimization);

      lTimeAccu.proceed();

      boolean lIsCovered = false;

      if (pApplySubsumptionCheck) {
        lIsCovered = applyCoverageCheck(lGoal, lGoalAutomaton, lPassingCPA, lResultFactory);
      }

      if (lIsCovered) {
        mOutput.println("Goal #" + lIndex + " is covered by an existing test case!");

        mFeasibilityInformation.setStatus(lIndex, FeasibilityInformation.FeasibilityStatus.FEASIBLE);

        InfeasibilityPropagation.Prediction lCurrentPrediction = lGoalPrediction[lIndex - 1];

        switch (lCurrentPrediction) {
        case UNKNOWN:
          break;
        default:
          throw new RuntimeException("missmatching prediction: " + lCurrentPrediction);
        }

        if (!pCheckReachWhenCovered) {
          long lEndTime = System.currentTimeMillis();

          lGoalRuntime[lIndex - 1] = lEndTime - lStartTime;

          lTimeAccu.pause(lFeasibleTestGoalsTimeSlot);

          continue;
        }
      }

      GuardedEdgeAutomatonCPA lAutomatonCPA = new GuardedEdgeAutomatonCPA(lGoalAutomaton);

      lTimeReach.proceed();

      boolean lReachableViaGraphSearch = false;

      if (!lAutomatonCPA.getAutomaton().getFinalStates().isEmpty()) {
        if (mUseGraphCPA) {
          lReachableViaGraphSearch = reachGraphSearch(lPreviousGraphGoalAutomaton, lGraphReachedSet, lAutomatonCPA, mWrapper.getEntry(), lPassingCPA);

          lPreviousGraphGoalAutomaton = lAutomatonCPA.getAutomaton();
        }
        else {
          lReachableViaGraphSearch = true;
        }
      }

      CounterexampleInfo lCounterexampleTraceInfo = null;

      if (lReachableViaGraphSearch) {
        lCounterexampleTraceInfo = reach(mWrapper.getCFA(), lPredicateReachedSet, lPreviousGoalAutomaton, lAutomatonCPA, mWrapper.getEntry(), lPassingCPA);

        // lPredicateReachedSet and lPreviousGoalAutomaton have to be in-sync.
        lPreviousGoalAutomaton = lAutomatonCPA.getAutomaton();
      }
      else {
        lNumberOfCFAInfeasibleGoals++;
      }

      lTimeReach.pause();

      if (lCounterexampleTraceInfo == null || lCounterexampleTraceInfo.isSpurious()) {
        mOutput.println("Goal #" + lIndex + " is infeasible!");

        if (lIsCovered) {
          throw new RuntimeException("Inconsistent result of coverage check and reachability analysis!");
        }

        mFeasibilityInformation.setStatus(lIndex, FeasibilityInformation.FeasibilityStatus.INFEASIBLE);

        lResultFactory.addInfeasibleTestCase(lGoal.getPattern());

        lTimeAccu.pause(lInfeasibleTestGoalsTimeSlot);

        long lEndTime = System.currentTimeMillis();

        lGoalRuntime[lIndex - 1] = lEndTime - lStartTime;

        if (lReachableViaGraphSearch && lInfeasibilityPropagation.getFirst()) {
          HashSet<CFAEdge> lTargetEdges = new HashSet<>();

          ClusteredElementaryCoveragePattern lClusteredPattern = (ClusteredElementaryCoveragePattern)lGoalPattern;

          ListIterator<ClusteredElementaryCoveragePattern> lRemainingPatterns = lClusteredPattern.getRemainingElementsInCluster();

          //int lTmpIndex = lIndex + 1;
          // TODO check again!
          int lTmpIndex = lIndex; // caution lIndex starts at 0

          while (lRemainingPatterns.hasNext()) {
            //System.out.println("lTmpIndex: " + lTmpIndex);

            InfeasibilityPropagation.Prediction lPrediction = lGoalPrediction[lTmpIndex];

            ClusteredElementaryCoveragePattern lRemainingPattern = lRemainingPatterns.next();

            if (lPrediction.equals(InfeasibilityPropagation.Prediction.UNKNOWN)) {
              lTargetEdges.add(lRemainingPattern.getLastSingletonCFAEdge());
            }

            lTmpIndex++;
          }

          Collection<CFAEdge> lFoundEdges = InfeasibilityPropagation.dfs2(lClusteredPattern.getCFANode(), lClusteredPattern.getLastSingletonCFAEdge(), lTargetEdges);

          lRemainingPatterns = lClusteredPattern.getRemainingElementsInCluster();

          //lTmpIndex = lIndex + 1;
          lTmpIndex = lIndex;

          int lPredictedElements = 0;

          while (lRemainingPatterns.hasNext()) {
            InfeasibilityPropagation.Prediction lPrediction = lGoalPrediction[lTmpIndex];

            ClusteredElementaryCoveragePattern lRemainingPattern = lRemainingPatterns.next();

            if (lPrediction.equals(InfeasibilityPropagation.Prediction.UNKNOWN)) {
              if (!lFoundEdges.contains(lRemainingPattern.getLastSingletonCFAEdge())) {
                lGoalPrediction[lTmpIndex] = InfeasibilityPropagation.Prediction.INFEASIBLE;
                lPredictedElements++;
              }
            }

            lTmpIndex++;
          }

          mOutput.println("(" + lPredictedElements + ")");
        }
      }
      else {
        lTimeCover.proceed();

        TestCase lTestCase = TestCase.fromCounterexample(lCounterexampleTraceInfo, mLogManager);

        mTestSuite.add(lTestCase);

        if (lTestCase.isPrecise()) {
          CFAEdge[] lCFAPath = null;

          boolean lIsPrecise = true;

          try {
            lCFAPath = reconstructPath(mWrapper.getCFA(), lTestCase, mWrapper.getEntry(), lAutomatonCPA, lPassingCPA, mWrapper.getOmegaEdge().getSuccessor());
          } catch (InvalidConfigurationException | CPAException e) {
            throw new RuntimeException(e);
          } catch (ImpreciseExecutionException e) {
            lIsPrecise = false;
            lTestCase = e.getTestCase();

            if (pPedantic) {
              throw new RuntimeException(e);
            }
          }

          if (lIsPrecise) {
            mOutput.println("Goal #" + lIndex + " is feasible!");

            lResultFactory.addFeasibleTestCase(lGoal.getPattern(), lTestCase);

            // we only add precise test cases for coverage analysis
            mGeneratedTestCases.put(lTestCase, lCFAPath);

            mFeasibilityInformation.setStatus(lIndex, FeasibilityInformation.FeasibilityStatus.FEASIBLE);



            InfeasibilityPropagation.Prediction lCurrentPrediction = lGoalPrediction[lIndex - 1];

            if (!lCurrentPrediction.equals(InfeasibilityPropagation.Prediction.UNKNOWN)) {
              throw new RuntimeException("missmatching prediction");
            }
          }
          else {
            mOutput.println("Goal #" + lIndex + " lead to an imprecise execution!");

            lResultFactory.addImpreciseTestCase(lTestCase);

            mFeasibilityInformation.setStatus(lIndex, FeasibilityInformation.FeasibilityStatus.IMPRECISE);

            InfeasibilityPropagation.Prediction lCurrentPrediction = lGoalPrediction[lIndex - 1];

            if (!lCurrentPrediction.equals(InfeasibilityPropagation.Prediction.UNKNOWN)) {
              throw new RuntimeException("missmatching prediction");
            }
          }
        }
        else {
          mOutput.println("Goal #" + lIndex + " is imprecise!");

          lResultFactory.addImpreciseTestCase(lTestCase);

          mFeasibilityInformation.setStatus(lIndex, FeasibilityInformation.FeasibilityStatus.IMPRECISE);

          InfeasibilityPropagation.Prediction lCurrentPrediction = lGoalPrediction[lIndex - 1];

          if (!lCurrentPrediction.equals(InfeasibilityPropagation.Prediction.UNKNOWN)) {
            throw new RuntimeException("missmatching prediction");
          }
/*
          if (!pPedantic) {
          //if (true) {
            // TODO implement reconstruction of path
            throw new RuntimeException();
          }*/
        }

        lTimeAccu.pause(lFeasibleTestGoalsTimeSlot);
        lTimeCover.pause();

        long lEndTime = System.currentTimeMillis();

        lGoalRuntime[lIndex - 1] = lEndTime - lStartTime;
      }
    }

    mOutput.println("Goal #" + (lIndex) + " needed " + lGoalRuntime[lIndex - 1] + " ms");

    mOutput.println("Number of CFA infeasible test goals: " + lNumberOfCFAInfeasibleGoals);

    mOutput.println("Time in reach: " + mTimeInReach.getSeconds());
    mOutput.println("Mean time of reach: " + (mTimeInReach.getSeconds()/mTimesInReach) + " s");

    // TODO remove ... look at statistics
    //System.out.println("#abstraction elements: " + mPredicateCPA.getAbstractionElementFactory().getNumberOfCreatedAbstractionElements());
    //System.out.println("#nonabstraction elements: " + NonabstractionElement.INSTANCES);

    CPAtigerResult lResult = lResultFactory.create(lTimeReach.getSeconds(), lTimeCover.getSeconds(), lTimeAccu.getSeconds(lFeasibleTestGoalsTimeSlot), lTimeAccu.getSeconds(lInfeasibleTestGoalsTimeSlot));

    /*if (lResult.getNumberOfTestGoals() != lNumberOfTestGoals) {
      throw new RuntimeException();
    }*/

    printStatistics(lResultFactory);

    // print per goal runtimes
    /*Arrays.sort(lGoalRuntime);
    for (int i = 0; i < lGoalRuntime.length; i++) {
      System.out.println("#" + (i + 1) + ": " + lGoalRuntime[i] + " ms");
    }*/

    if (lInfeasibilityPropagation.getFirst()) {
      mUseGraphCPA = lUseGraphCPAOld;
    }

    return lResult;
  }

  private void printStatistics(CPAtigerResult.Factory pResultFactory) {
    mOutput.println("Generated Test Cases:");

    for (TestCase lTestCase : pResultFactory.getTestCases()) {
      mOutput.println(lTestCase);
    }

    mOutput.println("INTERN:");
    mOutput.println("#Goals: " + mFeasibilityInformation.getNumberOfTestgoals() + ", #Feasible: " + mFeasibilityInformation.getNumberOfFeasibleTestgoals() + ", #Infeasible: " + mFeasibilityInformation.getNumberOfInfeasibleTestgoals() + ", #Imprecise: " + mFeasibilityInformation.getNumberOfImpreciseTestgoals());
  }

  public PredicatePrecision mPrecision;
  /*public ImmutableSetMultimap.Builder<CFANode, AbstractionPredicate> mBuilder = new ImmutableSetMultimap.Builder<>();
  public HashSet<AbstractionPredicate> mGlobalPredicates = new HashSet<>();*/

  private CounterexampleInfo reach(CFA pCFA, ReachedSet pReachedSet, NondeterministicFiniteAutomaton<GuardedEdgeLabel> pPreviousAutomaton, GuardedEdgeAutomatonCPA pAutomatonCPA, FunctionEntryNode pEntryNode, GuardedEdgeAutomatonCPA pPassingCPA) {
    mTimeInReach.proceed();
    mTimesInReach++;

    /*
     * CPAs should be arranged in a way such that frequently failing CPAs, i.e.,
     * CPAs that are not able to produce successors, are treated first such that
     * the compound CPA stops applying further transfer relations early. Here, we
     * have to choose between the number of times a CPA produces no successors and
     * the computational effort necessary to determine that there are no successors.
     */

    LinkedList<ConfigurableProgramAnalysis> lComponentAnalyses = new LinkedList<>();
    lComponentAnalyses.add(mLocationCPA);

    lComponentAnalyses.add(mCallStackCPA);

    List<ConfigurableProgramAnalysis> lAutomatonCPAs = new ArrayList<>(2);

    if (pPassingCPA != null) {
      lAutomatonCPAs.add(pPassingCPA);
    }

    lAutomatonCPAs.add(pAutomatonCPA);

    int lProductAutomatonIndex = lComponentAnalyses.size();
    lComponentAnalyses.add(ProductAutomatonCPA.create(lAutomatonCPAs, false));

    int lPredicateCPAIndex = lComponentAnalyses.size();

    lComponentAnalyses.add(mPredicateCPA);

    lComponentAnalyses.add(mAssumeCPA);

    ARGCPA lARTCPA;
    try {
      // create composite CPA
      CPAFactory lCPAFactory = CompositeCPA.factory();
      lCPAFactory.setChildren(lComponentAnalyses);
      lCPAFactory.setConfiguration(mConfiguration);
      lCPAFactory.setLogger(mLogManager);
      lCPAFactory.set(pCFA, CFA.class);

      ConfigurableProgramAnalysis lCPA = lCPAFactory.createInstance();

      // create ART CPA
      CPAFactory lARTCPAFactory = ARGCPA.factory();
      lARTCPAFactory.setChild(lCPA);
      lARTCPAFactory.setConfiguration(mConfiguration);
      lARTCPAFactory.setLogger(mLogManager);

      lARTCPA = (ARGCPA)lARTCPAFactory.createInstance();
/*
      lARTCPA.precisionAdjustment.setCache(mInfeasibilityCache);
      lARTCPA.precisionAdjustment.setPredicateCPAIndex(lPredicateCPAIndex);
      lARTCPA.precisionAdjustment.setAutomatonCPAIndex(lProductAutomatonIndex);*/
    } catch (InvalidConfigurationException | CPAException e) {
      throw new RuntimeException(e);
    }

    CPAAlgorithm lBasicAlgorithm;
    try {
      lBasicAlgorithm = new CPAAlgorithm(lARTCPA, mLogManager, mConfiguration, ShutdownNotifier.create());
    } catch (InvalidConfigurationException e1) {
      throw new RuntimeException(e1);
    }

    PredicateCPARefiner lRefiner;
    try {
      lRefiner = PredicateRefiner.cpatiger_create(lARTCPA);
    } catch (CPAException | InvalidConfigurationException e) {
      throw new RuntimeException(e);
    }

    CEGARAlgorithmWithCounterexampleInfo lAlgorithm;
    try {
      lAlgorithm = new CEGARAlgorithmWithCounterexampleInfo(lBasicAlgorithm, lRefiner, mConfiguration, mLogManager);
    } catch (InvalidConfigurationException | CPAException e) {
      throw new RuntimeException(e);
    }

    ARGStatistics lARTStatistics;
    try {
      lARTStatistics = new ARGStatistics(mConfiguration, lARTCPA);
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    }
    Set<Statistics> lStatistics = new HashSet<>();
    lStatistics.add(lARTStatistics);
    lAlgorithm.collectStatistics(lStatistics);

    if (mReuseART) {
      ARTReuse.modifyReachedSet(pReachedSet, pEntryNode, lARTCPA, lProductAutomatonIndex, pPreviousAutomaton, pAutomatonCPA.getAutomaton());

      // TODO activate predicate update again
      System.err.println("TODO: activate predicate update again!");

      if (mPrecision != null) {
        for (AbstractState lWaitlistElement : pReachedSet.getWaitlist()) {
          //for (AbstractElement lWaitlistElement : pReachedSet) {

          Precision lOldPrecision = pReachedSet.getPrecision(lWaitlistElement);
          Precision lNewPrecision = Precisions.replaceByType(lOldPrecision, mPrecision, PredicatePrecision.class);

          pReachedSet.updatePrecision(lWaitlistElement, lNewPrecision);
        }
      }
    }
    else {
      pReachedSet = new LocationMappedReachedSet(Waitlist.TraversalMethod.DFS); // TODO why does TOPSORT not exist anymore?

      AbstractState lInitialElement = lARTCPA.getInitialState(pEntryNode);
      Precision lInitialPrecision = lARTCPA.getInitialPrecision(pEntryNode);

      pReachedSet.add(lInitialElement, lInitialPrecision);
    }

    Pair<Boolean, CounterexampleInfo> lResult;

    try {
      lResult = lAlgorithm.runWithCounterexample(pReachedSet);

      assert lResult.getFirst();
    } catch (CPAException | InterruptedException e) {
      throw new RuntimeException(e);
    }

    CounterexampleInfo lCounterexampleInfo;

    if (pReachedSet.getLastState() != null && ((ARGState)pReachedSet.getLastState()).isTarget()) {
      lCounterexampleInfo = lResult.getSecond();

      assert(lCounterexampleInfo != null);
      assert(!lCounterexampleInfo.isSpurious());
    }
    else {
      // we have shown the infeasibility of the test goal (given the assumption that the algorithm completed)
      lCounterexampleInfo = null; // TODO I guess this can't stay like that
    }

    System.err.println("TODO: handle PredicatePrecision!");
    /*
    mPrecision = new PredicatePrecision(mBuilder.build(), mGlobalPredicates);

    if (mPrintPredicateStatistics) {
      printPredicateStatistics(pReachedSet, lPredicateCPAIndex);
    }*/

    mTimeInReach.pause();

    return lCounterexampleInfo;
  }

  private boolean checkCoverage(TestCase pTestCase, FunctionEntryNode pEntry, GuardedEdgeAutomatonCPA pCoverAutomatonCPA, GuardedEdgeAutomatonCPA pPassingAutomatonCPA, CFANode pEndNode) throws InvalidConfigurationException, CPAException, ImpreciseExecutionException {
    LinkedList<ConfigurableProgramAnalysis> lComponentAnalyses = new LinkedList<>();
    lComponentAnalyses.add(mLocationCPA);

    List<ConfigurableProgramAnalysis> lAutomatonCPAs = new ArrayList<>(2);

    // test goal automata CPAs
    if (pPassingAutomatonCPA != null) {
      lAutomatonCPAs.add(pPassingAutomatonCPA);
    }

    lAutomatonCPAs.add(pCoverAutomatonCPA);

    int lProductAutomatonCPAIndex = lComponentAnalyses.size();
    lComponentAnalyses.add(ProductAutomatonCPA.create(lAutomatonCPAs, false));

    // call stack CPA
    lComponentAnalyses.add(mCallStackCPA);

    // explicit CPA
    InterpreterCPA lInterpreterCPA = new InterpreterCPA(pTestCase.getInputs());
    lComponentAnalyses.add(lInterpreterCPA);

    // assume CPA
    lComponentAnalyses.add(mAssumeCPA);


    CPAFactory lCPAFactory = CompositeCPA.factory();
    lCPAFactory.setChildren(lComponentAnalyses);
    lCPAFactory.setConfiguration(mConfiguration);
    lCPAFactory.setLogger(mLogManager);
    ConfigurableProgramAnalysis lCPA = lCPAFactory.createInstance();

    CPAAlgorithm lAlgorithm = new CPAAlgorithm(lCPA, mLogManager, mConfiguration, mShutdownNotifier);

    AbstractState lInitialElement = lCPA.getInitialState(pEntry);
    Precision lInitialPrecision = lCPA.getInitialPrecision(pEntry);

    ReachedSet lReachedSet = new PartitionedReachedSet(Waitlist.TraversalMethod.DFS); // TODO why does TOPSORT not exist anymore?
    lReachedSet.add(lInitialElement, lInitialPrecision);

    try {
      lAlgorithm.run(lReachedSet);
    } catch (CPAException | InterruptedException e) {
      throw new RuntimeException(e);
    }

    // TODO sanity check by assertion
    CompositeState lEndNode = (CompositeState)lReachedSet.getLastState();

    if (lEndNode == null) {
      return false;
    }
    else {
      if (((LocationState)lEndNode.get(0)).getLocationNode().equals(pEndNode)) {
        // location of last element is at end node

        AbstractState lProductAutomatonElement = lEndNode.get(lProductAutomatonCPAIndex);

        if (lProductAutomatonElement instanceof Targetable) {
          Targetable lTargetable = (Targetable)lProductAutomatonElement;

          return lTargetable.isTarget();
        }

        return false;
      }

      return false;
    }
  }

  private CFAEdge[] reconstructPath(CFA pCFA, TestCase pTestCase, FunctionEntryNode pEntry, GuardedEdgeAutomatonCPA pCoverAutomatonCPA, GuardedEdgeAutomatonCPA pPassingAutomatonCPA, CFANode pEndNode) throws InvalidConfigurationException, CPAException, ImpreciseExecutionException {
    LinkedList<ConfigurableProgramAnalysis> lComponentAnalyses = new LinkedList<>();
    lComponentAnalyses.add(mLocationCPA);

    List<ConfigurableProgramAnalysis> lAutomatonCPAs = new ArrayList<>(2);

    // test goal automata CPAs
    if (pPassingAutomatonCPA != null) {
      lAutomatonCPAs.add(pPassingAutomatonCPA);
    }

    lAutomatonCPAs.add(pCoverAutomatonCPA);

    int lProductAutomatonCPAIndex = lComponentAnalyses.size();
    lComponentAnalyses.add(ProductAutomatonCPA.create(lAutomatonCPAs, false));

    // call stack CPA
    lComponentAnalyses.add(mCallStackCPA);

    // explicit CPA
    InterpreterCPA lInterpreterCPA = new InterpreterCPA(pTestCase.getInputs());
    lComponentAnalyses.add(lInterpreterCPA);

    // CFA path CPA
    int lCFAPathCPAIndex = lComponentAnalyses.size();
    lComponentAnalyses.add(mCFAPathCPA);

    // assume CPA
    lComponentAnalyses.add(mAssumeCPA);


    CPAFactory lCPAFactory = CompositeCPA.factory();
    lCPAFactory.setChildren(lComponentAnalyses);
    lCPAFactory.setConfiguration(mConfiguration);
    lCPAFactory.setLogger(mLogManager);
    lCPAFactory.set(pCFA, CFA.class);
    ConfigurableProgramAnalysis lCPA = lCPAFactory.createInstance();

    CPAAlgorithm lAlgorithm = new CPAAlgorithm(lCPA, mLogManager, mConfiguration, mShutdownNotifier);

    AbstractState lInitialElement = lCPA.getInitialState(pEntry);
    Precision lInitialPrecision = lCPA.getInitialPrecision(pEntry);

    ReachedSet lReachedSet = new PartitionedReachedSet(Waitlist.TraversalMethod.DFS); // TODO why does TOPSORT not exist anymore?
    lReachedSet.add(lInitialElement, lInitialPrecision);

    try {
      lAlgorithm.run(lReachedSet);
    } catch (CPAException | InterruptedException e) {
      throw new RuntimeException(e);
    }

    // TODO sanity check by assertion
    CompositeState lEndNode = (CompositeState)lReachedSet.getLastState();

    if (lEndNode == null) {
      throw new ImpreciseExecutionException(pTestCase, pCoverAutomatonCPA, pPassingAutomatonCPA);
    }

    if (!((LocationState)lEndNode.get(0)).getLocationNode().equals(pEndNode)) {
      throw new ImpreciseExecutionException(pTestCase, pCoverAutomatonCPA, pPassingAutomatonCPA);
    }

    AbstractState lProductAutomatonElement = lEndNode.get(lProductAutomatonCPAIndex);

    if (!(lProductAutomatonElement instanceof Targetable)) {
      throw new RuntimeException();
    }

    Targetable lTargetable = (Targetable)lProductAutomatonElement;

    if (!lTargetable.isTarget()) {
      throw new RuntimeException();
    }

    CFAPathStandardState lPathElement = (CFAPathStandardState)lEndNode.get(lCFAPathCPAIndex);

    return lPathElement.toArray();
  }

  private boolean reachGraphSearch(NondeterministicFiniteAutomaton<GuardedEdgeLabel> pPreviousAutomaton, ReachedSet pReachedSet, GuardedEdgeAutomatonCPA pAutomatonCPA, FunctionEntryNode pEntryNode, GuardedEdgeAutomatonCPA pPassingCPA) {
    mTimeInReach.proceed();
    mTimesInReach++;

    /*
     * CPAs should be arranged in a way such that frequently failing CPAs, i.e.,
     * CPAs that are not able to produce successors, are treated first such that
     * the compound CPA stops applying further transfer relations early. Here, we
     * have to choose between the number of times a CPA produces no successors and
     * the computational effort necessary to determine that there are no successors.
     */

    LinkedList<ConfigurableProgramAnalysis> lComponentAnalyses = new LinkedList<>();
    lComponentAnalyses.add(mLocationCPA);

    lComponentAnalyses.add(mCallStackCPA);

    List<ConfigurableProgramAnalysis> lAutomatonCPAs = new ArrayList<>(2);

    if (pPassingCPA != null) {
      lAutomatonCPAs.add(pPassingCPA);
    }

    lAutomatonCPAs.add(pAutomatonCPA);

    int lProductAutomatonIndex = lComponentAnalyses.size();
    lComponentAnalyses.add(ProductAutomatonCPA.create(lAutomatonCPAs, false));

    lComponentAnalyses.add(mAssumeCPA);

    ConfigurableProgramAnalysis lCPA;
    ARGCPA lARTCPA;
    try {
      // create composite CPA
      CPAFactory lCPAFactory = CompositeCPA.factory();
      lCPAFactory.setChildren(lComponentAnalyses);
      lCPAFactory.setConfiguration(mConfiguration);
      lCPAFactory.setLogger(mLogManager);
      lCPA = lCPAFactory.createInstance();

      // create ART CPA
      CPAFactory lARTCPAFactory = ARGCPA.factory();
      lARTCPAFactory.setChild(lCPA);
      lARTCPAFactory.setConfiguration(mConfiguration);
      lARTCPAFactory.setLogger(mLogManager);

      lARTCPA = (ARGCPA)lARTCPAFactory.createInstance();
      //lARTCPA.precisionAdjustment.deactivate();
    } catch (InvalidConfigurationException | CPAException e) {
      throw new RuntimeException(e);
    }

    if (mReuseART) {
      ARTReuse.modifyReachedSet(pReachedSet, pEntryNode, lARTCPA, lProductAutomatonIndex, pPreviousAutomaton, pAutomatonCPA.getAutomaton());
    }
    else {
      pReachedSet = new LocationMappedReachedSet(Waitlist.TraversalMethod.DFS);

      AbstractState lInitialElement = lARTCPA.getInitialState(pEntryNode);
      Precision lInitialPrecision = lARTCPA.getInitialPrecision(pEntryNode);

      pReachedSet.add(lInitialElement, lInitialPrecision);
    }

    CPAAlgorithm lBasicAlgorithm;
    try {
      lBasicAlgorithm = new CPAAlgorithm(lARTCPA, mLogManager, mConfiguration, null);
    } catch (InvalidConfigurationException e1) {
      throw new RuntimeException(e1);
    }

    ARGStatistics lARTStatistics;
    try {
      lARTStatistics = new ARGStatistics(mConfiguration, lARTCPA);
    } catch (InvalidConfigurationException e) {
      throw new RuntimeException(e);
    }
    Set<Statistics> lStatistics = new HashSet<>();
    lStatistics.add(lARTStatistics);
    lBasicAlgorithm.collectStatistics(lStatistics);


    try {
      lBasicAlgorithm.run(pReachedSet);
    } catch (CPAException | InterruptedException e) {
      throw new RuntimeException(e);
    }

    ARGState lLastARTElement = (ARGState)pReachedSet.getLastState();

    CompositeState lLastElement = (CompositeState)lLastARTElement.getWrappedState();
    ProductAutomatonElement lProductAutomatonElement = (ProductAutomatonElement)lLastElement.get(lProductAutomatonIndex);

    mTimeInReach.pause();

    return lProductAutomatonElement.isFinalState();
  }
}
