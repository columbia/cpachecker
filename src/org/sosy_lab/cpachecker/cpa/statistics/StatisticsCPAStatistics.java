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
package org.sosy_lab.cpachecker.cpa.statistics;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.sosy_lab.common.JSON;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;

/**
 * The StatisticsCPAStatistics implements the Statistics interface and takes care of printing out the analysis results.
 * With the cpa.statistics.statisticsCPAFile you can print out results to a target file in the json format.
 * With the cpa.statistics.printOut option you can print out the results to the standard output in a human readable format.
 */
@Options(prefix="cpa.statistics")
public class StatisticsCPAStatistics implements Statistics  {

  @Option(description="target file to hold the statistics")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path statisticsCPAFile = null;
  private final StatisticsCPA cpa;

  @Option(description="set to true if you want to print the statistics in the standard output.")
  private boolean printOut = true;

  public StatisticsCPAStatistics(Configuration config, StatisticsCPA cpa) throws InvalidConfigurationException {
    config.inject(this);
    this.cpa = cpa;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
    StatisticsData statistics;
    if (cpa.isAnalysis()) {
      statistics = cpa.getFactory().getGlobalAnalysis();
    } else {
      StatisticsState lastState = (StatisticsState)reached.getLastState();
      if (lastState == null) {
        for (AbstractState abstractState : reached.asCollection()) {
          if (abstractState != null) {
            lastState = (StatisticsState)reached.getLastState();
          }
        }
      }

      out.println("StatisticsCPA");
      statistics = lastState.getStatistics();
    }

    Map<String, Object> jsonMap = new HashMap<>();
    for (Entry<StatisticsProvider, StatisticsDataProvider> entry : statistics) {
      StatisticsProvider provider = entry.getKey();
      StatisticsDataProvider data = entry.getValue();
      String propName = provider.getPropertyName();
      Object value = data.getPropertyValue();
      String mergeInfo = "";
      if (!cpa.isAnalysis()) {
        String mergeType = provider.getMergeType();
        mergeInfo = "_" + mergeType;
        // Save in json with merge type
        Map<String, Object> innerJsonMap;
        if (jsonMap.containsKey(propName)) {
          innerJsonMap = (Map<String, Object>)jsonMap.get(propName);
        } else {
          innerJsonMap = new HashMap<>();
          jsonMap.put(propName, innerJsonMap);
        }
        innerJsonMap.put(mergeType, value);
      } else {
        // Save in json without merge type
        jsonMap.put(propName, value);
      }
      if (printOut) {
        out.println("\t" + propName + mergeInfo + ": " + value);
      }
    }

    if (statisticsCPAFile != null) {
      try {
        JSON.writeJSONString(jsonMap, statisticsCPAFile);
      } catch (IOException e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public String getName() {
    return "StatisticsCPA";
  }

}
