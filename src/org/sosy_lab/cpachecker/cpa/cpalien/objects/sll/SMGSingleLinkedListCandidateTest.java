/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.cpalien.objects.sll;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.cpalien.AnonymousTypes;
import org.sosy_lab.cpachecker.cpa.cpalien.CLangSMG;
import org.sosy_lab.cpachecker.cpa.cpalien.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.cpalien.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.cpalien.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.cpalien.objects.SMGRegion;

import com.google.common.collect.Iterables;


public class SMGSingleLinkedListCandidateTest {

  @Test
  public void basicTest() {
    SMGObject object = new SMGRegion(8, "object");
    SMGSingleLinkedListCandidate candidate = new SMGSingleLinkedListCandidate(object, 4, 2);

    Assert.assertSame(object, candidate.getStart());
    Assert.assertEquals(4, candidate.getOffset());
    Assert.assertEquals(2, candidate.getLength());

    candidate.addLength(4);
    Assert.assertEquals(4, candidate.getOffset());
    Assert.assertEquals(6, candidate.getLength());
  }

  @Test
  public void isCompatibleWithTest() {
    SMGObject object8_1 = new SMGRegion(8, "object 1");
    SMGObject object8_2 = new SMGRegion(8, "object 2");
    SMGObject object16 = new SMGRegion(16, "object 3");

    SMGSingleLinkedListCandidate candidate8_1 = new SMGSingleLinkedListCandidate(object8_1, 4, 2);
    SMGSingleLinkedListCandidate candidate8_2 = new SMGSingleLinkedListCandidate(object8_2, 4, 8);
    SMGSingleLinkedListCandidate candidate16 = new SMGSingleLinkedListCandidate(object16, 4, 2);

    Assert.assertTrue(candidate8_1.isCompatibleWith(candidate8_2));
    Assert.assertTrue(candidate8_2.isCompatibleWith(candidate8_1));
    Assert.assertFalse(candidate16.isCompatibleWith(candidate8_1));
    Assert.assertFalse(candidate8_1.isCompatibleWith(candidate16));

    candidate8_2 = new SMGSingleLinkedListCandidate(object8_2, 6, 2);
    Assert.assertFalse(candidate8_1.isCompatibleWith(candidate8_2));
  }

  @Test
  public void executeOnSimpleList() {
    CLangSMG smg = new CLangSMG(MachineModel.LINUX64);
    Integer value = TestHelpers.createList(smg, 5);

    SMGRegion globalVar = new SMGRegion(8, "pointer");
    SMGEdgeHasValue hv = new SMGEdgeHasValue(AnonymousTypes.dummyPointer, 8, globalVar, value);
    smg.addGlobalObject(globalVar);
    smg.addHasValueEdge(hv);

    SMGObject startObject = smg.getPointer(value).getObject();
    SMGSingleLinkedListCandidate candidate = new SMGSingleLinkedListCandidate(startObject, 8, 4);

    CLangSMG abstractedSmg = candidate.execute(smg);
    Set<SMGObject> heap = abstractedSmg.getHeapObjects();
    Assert.assertEquals(3, heap.size());
    SMGObject pointedObject = abstractedSmg.getPointer(value).getObject();
    Assert.assertTrue(pointedObject instanceof SMGSingleLinkedList);
    Assert.assertTrue(pointedObject.isAbstract());
    SMGSingleLinkedList segment = (SMGSingleLinkedList)pointedObject;
    Assert.assertEquals(16, segment.getSize());
    Assert.assertEquals(4, segment.getLength());
    Assert.assertEquals(8, segment.getOffset());
    Set<SMGEdgeHasValue> outboundEdges = abstractedSmg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(segment));
    Assert.assertEquals(1, outboundEdges.size());
    SMGEdgeHasValue onlyOutboundEdge = Iterables.getOnlyElement(outboundEdges);
    Assert.assertEquals(8, onlyOutboundEdge.getOffset());
    Assert.assertSame(AnonymousTypes.dummyPointer, onlyOutboundEdge.getType());

    SMGObject stopper = abstractedSmg.getPointer(onlyOutboundEdge.getValue()).getObject();
    Assert.assertTrue(stopper instanceof SMGRegion);
    SMGRegion stopperRegion = (SMGRegion)stopper;
    Assert.assertEquals(16, stopperRegion.getSize());
    outboundEdges = abstractedSmg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(stopperRegion));
    Assert.assertEquals(1, outboundEdges.size());
    onlyOutboundEdge = Iterables.getOnlyElement(outboundEdges);
    Assert.assertEquals(0, onlyOutboundEdge.getValue());
    Assert.assertEquals(0, onlyOutboundEdge.getOffset());
    Assert.assertEquals(16, onlyOutboundEdge.getSizeInBytes(abstractedSmg.getMachineModel()));
  }
}