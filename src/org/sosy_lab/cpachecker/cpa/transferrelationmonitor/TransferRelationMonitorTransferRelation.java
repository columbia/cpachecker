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
package org.sosy_lab.cpachecker.cpa.transferrelationmonitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;

import org.sosy_lab.cpachecker.core.algorithm.CEGARAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

@Options(prefix="trackabstractioncomputation")
public class TransferRelationMonitorTransferRelation implements TransferRelation {

  private final AbstractDomain domain;
  private final TransferRelation transferRelation;
  private TransferCallable tc = new TransferCallable();

  private static int noOfStops = 0;

  @Option(name="limit")
  private long timeLimit = 0; // given in milliseconds

  @Option(name="pathcomputationlimit")
  private long timeLimitForPath = 0;

  public TransferRelationMonitorTransferRelation(ConfigurableProgramAnalysis pWrappedCPA,
      Configuration config) throws InvalidConfigurationException {
    config.inject(this);

    transferRelation = pWrappedCPA.getTransferRelation();
    domain = pWrappedCPA.getAbstractDomain();
  }

  @Override
  public Collection<TransferRelationMonitorElement> getAbstractSuccessors(
      AbstractElement pElement, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException {
    TransferRelationMonitorElement element = (TransferRelationMonitorElement)pElement;
    Collection<? extends AbstractElement> successors = null;
    long timeOfExecution = 0;
    long start = 0;
    long end = 0;

    AbstractElement wrappedElement = element.getWrappedElement();

    // set the edge and element
    tc.setEdge(pCfaEdge);
    tc.setElement(wrappedElement);
    tc.setPrecision(pPrecision);
    Future<Collection<? extends AbstractElement>> future = CEGARAlgorithm.executor.submit(tc);
    try{
      start = System.currentTimeMillis();
      if(timeLimit == 0){
        successors = future.get();
      }
      // here we get the result of the post computation but there is a time limit
      // given to complete the task specified by timeLimit
      else{
        successors = future.get(timeLimit, TimeUnit.MILLISECONDS);
      }
      end = System.currentTimeMillis();
    } catch (TimeoutException exc){
      TransferRelationMonitorElement bottom = new TransferRelationMonitorElement(null, null);
      bottom.setAsStopElement();
      return Collections.emptySet();
    } catch (InterruptedException exc) {
      exc.printStackTrace();
    } catch (ExecutionException exc) {
      exc.printStackTrace();
    }

    timeOfExecution = end-start;

    assert(successors != null);
    if (successors.isEmpty()) {
      return Collections.emptySet();
    }

    Collection<TransferRelationMonitorElement> wrappedSuccessors = new ArrayList<TransferRelationMonitorElement>();
    for (AbstractElement absElement : successors) {
      if (absElement.equals(domain.getBottomElement())) {
        // omit bottom element from successors list
        continue;
      }
      TransferRelationMonitorElement successorElem = new TransferRelationMonitorElement(element.getCpa(), absElement);
      successorElem.setTransferTime(timeOfExecution);
      successorElem.setTotalTime(element.isIgnore(), element.getTotalTimeOnThePath());
//      if(!successorElem.isIgnore()){
        if(timeLimitForPath > 0 &&
            successorElem.getTotalTimeOnThePath() > timeLimitForPath){
          noOfStops++;
          if(noOfStops % 20 == 0){
            successorElem.resetTotalTime();
          }
          else{
            return Collections.emptySet();
          }
        }
//      }
      wrappedSuccessors.add(successorElem);
    }
    return wrappedSuccessors;
  }

  @Override
  public Collection<? extends AbstractElement> strengthen(AbstractElement element,
      List<AbstractElement> otherElements, CFAEdge cfaEdge,
      Precision precision) {
    TransferRelationMonitorElement monitorElement = (TransferRelationMonitorElement)element;
    AbstractElement wrappedElement = monitorElement.getWrappedElement();
    List<AbstractElement> retList = new ArrayList<AbstractElement>();

    try {
      Collection<? extends AbstractElement> wrappedList = transferRelation.strengthen(wrappedElement, otherElements, cfaEdge, precision);
      // if the returned list is null return null
      if(wrappedList == null)
        return null;
      // TODO we assume that only one element is returned or empty set to represent bottom
      assert(wrappedList.size() < 2);
      // if bottom return empty list
      if(wrappedList.size() == 0){
        return retList;
      }

      AbstractElement wrappedReturnElement = wrappedList.iterator().next();
      retList.add(new TransferRelationMonitorElement(monitorElement.getCpa(), wrappedReturnElement));
      return retList;
    } catch (CPATransferException e) {
      e.printStackTrace();
    }

    return null;
  }

  private class TransferCallable implements Callable<Collection<? extends AbstractElement>>{

    CFAEdge cfaEdge;
    AbstractElement abstractElement;
    Precision precision;

    public TransferCallable() {

    }

    @Override
    public Collection<? extends AbstractElement> call() throws Exception {
      return transferRelation.getAbstractSuccessors(abstractElement, precision, cfaEdge);
    }

    public void setEdge(CFAEdge pCfaEdge){
      cfaEdge = pCfaEdge;
    }

    public void setElement(AbstractElement pAbstractElement){
      abstractElement = pAbstractElement;
    }

    public void setPrecision(Precision pPrecision){
      precision = pPrecision;
    }
  }

}
