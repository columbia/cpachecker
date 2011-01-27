package org.sosy_lab.cpachecker.cfa.ast;

import java.util.List;

public final class IASTStandardFunctionDeclarator extends
    IASTFunctionDeclarator implements
    org.eclipse.cdt.core.dom.ast.IASTStandardFunctionDeclarator {

  private final List<IASTParameterDeclaration> parameters;
  private final boolean                        takesVarArgs;

  public IASTStandardFunctionDeclarator(final String pRawSignature,
      final IASTFileLocation pFileLocation, final IASTInitializer pInitializer,
      final IASTName pName, final IASTDeclarator pNestedDeclarator,
      final List<IASTPointerOperator> pPointerOperators,
      final List<IASTParameterDeclaration> pParameters,
      final boolean pTakesVarArgs) {
    super(pRawSignature, pFileLocation, pInitializer, pName, pNestedDeclarator,
        pPointerOperators);
    parameters = pParameters;
    takesVarArgs = pTakesVarArgs;
  }

  @Override
  @Deprecated
  public void addParameterDeclaration(
      final org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public org.eclipse.cdt.core.dom.ast.IScope getFunctionScope() {
    throw new UnsupportedOperationException();
  }

  @Override
  public IASTParameterDeclaration[] getParameters() {
    return parameters.toArray(new IASTParameterDeclaration[parameters.size()]);
  }

  @Override
  @Deprecated
  public void setVarArgs(boolean pArg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean takesVarArgs() {
    return takesVarArgs;
  }

  @Override
  @Deprecated
  public IASTStandardFunctionDeclarator copy() {
    throw new UnsupportedOperationException();
  }
}
