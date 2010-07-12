package org.sosy_lab.cpachecker.fllesh;

import org.sosy_lab.cpachecker.fllesh.ecp.ElementaryCoveragePattern;
import org.sosy_lab.cpachecker.fllesh.ecp.translators.GuardedEdgeLabel;
import org.sosy_lab.cpachecker.fllesh.ecp.translators.ToGuardedAutomatonTranslator;
import org.sosy_lab.cpachecker.fllesh.util.Automaton;

public class Goal {

  private ElementaryCoveragePattern mPattern;
  private Automaton<GuardedEdgeLabel> mAutomaton;
  private boolean mContainsPredicates;
  
  public Goal(ElementaryCoveragePattern pPattern, Wrapper pWrapper) {
    mPattern = pPattern;
    mAutomaton = ToGuardedAutomatonTranslator.toAutomaton(mPattern, pWrapper.getAlphaEdge(), pWrapper.getOmegaEdge());
    
    mContainsPredicates = false;
    
    for (Automaton<GuardedEdgeLabel>.Edge lEdge : mAutomaton.getEdges()) {
      GuardedEdgeLabel lLabel = lEdge.getLabel();

      // pAutomaton only contains predicates as guards anymore (by construction)
      if (lLabel.hasGuards()) {
        mContainsPredicates = true;
      }
    }
  }
  
  public ElementaryCoveragePattern getPattern() {
    return mPattern;
  }
  
  public Automaton<GuardedEdgeLabel> getAutomaton() {
    return mAutomaton;
  }
  
  public boolean containsPredicates() {
    return mContainsPredicates;
  }
  
}
