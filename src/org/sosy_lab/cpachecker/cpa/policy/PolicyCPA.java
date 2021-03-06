package org.sosy_lab.cpachecker.cpa.policy;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopJoinOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;

/**
 * Configurable-Program-Analysis implementation for policy iteration.
 */
@Options(prefix="cpa.policy")
public class PolicyCPA implements ConfigurableProgramAnalysis{

  private final PolicyAbstractDomain abstractDomain;
  private final TransferRelation transferRelation;

  private MergeOperator mergeOperator;
  private StopOperator stopOperator;

  private PrecisionAdjustment precisionAdjustment;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(PolicyCPA.class);
  }

  @SuppressWarnings("unused")
  private PolicyCPA(
        Configuration config,
        LogManager logger,
        ShutdownNotifier shutdownNotifier,
        CFA cfa
        ) throws InvalidConfigurationException {
    config.inject(this);

    FormulaManagerFactory formulaManagerFactory = new FormulaManagerFactory(config, logger, shutdownNotifier);

    FormulaManager realFormulaManager = formulaManagerFactory.getFormulaManager();
    FormulaManagerView formulaManager = new FormulaManagerView(realFormulaManager, config, logger);
    PathFormulaManager pathFormulaManager = new PathFormulaManagerImpl(
        formulaManager, config, logger, shutdownNotifier, cfa, false);
    LinearConstraintManager lcmgr = new LinearConstraintManager(formulaManager, logger);
    ValueDeterminationFormulaManager valueDeterminationFormulaManager = new ValueDeterminationFormulaManager(
        pathFormulaManager,
        formulaManager,
        config,
        logger,
        shutdownNotifier,
        cfa.getMachineModel(),
        cfa,
        realFormulaManager,
        lcmgr
    );


    abstractDomain = new PolicyAbstractDomain(
        valueDeterminationFormulaManager,
        formulaManagerFactory,
        logger,
        lcmgr
    );

    transferRelation = new PolicyTransferRelation(
        config,
        formulaManager,
        formulaManagerFactory,
        pathFormulaManager,
        logger,
        abstractDomain,
        lcmgr
    );

    mergeOperator = new MergeJoinOperator(abstractDomain);
    stopOperator = new StopJoinOperator(abstractDomain);
    precisionAdjustment = StaticPrecisionAdjustment.getInstance();
  }

  @Override
  public AbstractState getInitialState(CFANode node) {
    return PolicyAbstractState.withEmptyState(node);
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return mergeOperator;
  }

  @Override
  public StopOperator getStopOperator() {
    return stopOperator;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

  @Override
  public Precision getInitialPrecision(CFANode node) {
    return SingletonPrecision.getInstance();
  }
}
