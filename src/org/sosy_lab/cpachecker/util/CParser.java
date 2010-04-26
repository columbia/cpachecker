/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.util;

import java.io.IOException;
import java.util.Map;

import org.eclipse.cdt.core.dom.ICodeReaderFactory;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.gnu.c.GCCLanguage;
import org.eclipse.cdt.core.dom.parser.c.ANSICParserExtensionConfiguration;
import org.eclipse.cdt.core.dom.parser.c.GCCParserExtensionConfiguration;
import org.eclipse.cdt.core.dom.parser.c.ICParserExtensionConfiguration;
import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.core.parser.CodeReader;
import org.eclipse.cdt.core.parser.ICodeReaderCache;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.ParserFactory;
import org.eclipse.core.runtime.CoreException;

/**
 * Class that encapsulates the usage of the Eclipse CDT parser.
 */
public final class CParser {

  /**
   * Enum for clients of this class to choose the C dialect the parser uses.
   * Each instance has a ILanguage instance that can be used to parser C with
   * the given dialect.
   */
  public static enum Dialect {
    C99(new ANSICParserExtensionConfiguration()),
    GNUC(GCCParserExtensionConfiguration.getInstance());

    private final ILanguage language;

    private Dialect(ICParserExtensionConfiguration parserConfig) {
      this.language = new CLanguage(parserConfig);
    }

    public ILanguage getLanguage() {
      return language;
    }
  }

  /**
   * Private class extending the Eclipse CDT class that is the starting point
   * for using the parser.
   * Supports choise of parser dialect.
   */
  private static class CLanguage extends GCCLanguage {

    private final ICParserExtensionConfiguration parserConfig;

    public CLanguage(ICParserExtensionConfiguration parserConfig) {
      this.parserConfig = parserConfig;
    }

    @Override
    protected ICParserExtensionConfiguration getParserExtensionConfiguration() {
      return parserConfig;
    }
  }

  /**
   * Private class that creates CodeReaders for files. Caching is not supported.
   * TODO: Errors are ignored currently.
   */
  private static class StubCodeReaderFactory implements ICodeReaderFactory {

    private static ICodeReaderFactory instance = new StubCodeReaderFactory();

    @Override
    public int getUniqueIdentifier() {
      throw new UnsupportedOperationException();
    }

    @Override
    public CodeReader createCodeReaderForTranslationUnit(String path) {
      try {
        return new CodeReader(path);
      } catch (IOException e) {
        e.printStackTrace();
        return null;
      }
    }

    @Override
    public ICodeReaderCache getCodeReaderCache() {
      throw new UnsupportedOperationException();
    }
    public CodeReader createCodeReaderForInclusion(String arg0) {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Private class that tells the Eclipse CDT scanner that no macros and include
   * paths have been defined externally.
   */
  private static class StubScannerInfo implements IScannerInfo {

    private static IScannerInfo instance = new StubScannerInfo();

    @Override
    public Map<String, String> getDefinedSymbols() {
      // the externally defined pre-processor macros
      return null;
    }

    @Override
    public String[] getIncludePaths() {
      return null;
    }
  }

  private CParser() {} // utility class should not be instantiated

  /**
   * Parse the content of a file into an AST with the Eclipse CDT parser.
   *
   * @param fileName  The file to parse.
   * @return The AST.
   * @throws IOException If file cannot be read.
   * @throws CoreException If Eclipse C parser throws an exception.
   */
  public static IASTTranslationUnit parseFile(String filename, Dialect dialect) throws IOException, CoreException {
    return parse(new CodeReader(filename), dialect);
  }

  /**
   * Parse the content of a String into an AST with the Eclipse CDT parser.
   *
   * @param code  The code to parse.
   * @return The AST.
   * @throws CoreException If Eclipse C parser throws an exception.
   */
  public static IASTTranslationUnit parseString(String code, Dialect dialect) throws CoreException {
    return parse(new CodeReader(code.toCharArray()), dialect);
  }

  private static IASTTranslationUnit parse(CodeReader codeReader, Dialect dialect) throws CoreException {
    IParserLogService parserLog = ParserFactory.createDefaultLogService();

    ILanguage lang = dialect.getLanguage();

    return lang.getASTTranslationUnit(codeReader, StubScannerInfo.instance,
                                      StubCodeReaderFactory.instance, null, parserLog);
  }
}
