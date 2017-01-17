/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.uima.util.impl;

import java.util.List;

import org.junit.Assert;
import junit.framework.TestCase;

import org.apache.uima.util.ProcessTrace;
import org.apache.uima.util.ProcessTraceEvent;


public class ProcessTrace_implTest extends TestCase {

  /**
   * Constructor for ProcessTrace_implTest.
   * 
   * @param arg0
   */
  public ProcessTrace_implTest(String arg0) {
    super(arg0);
  }

  /*
   * @see TestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();
  }

  public void testStartAndEndEvent() {
    ProcessTrace pt = new ProcessTrace_impl();
    // should be nothing on event list
    Assert.assertTrue(pt.getEvents().isEmpty());
    // start two events
    pt.startEvent("c1", "t1", "testing");
    pt.startEvent("c1", "t2", "testing");
    // should be nothing on event list until both are closed
    Assert.assertTrue(pt.getEvents().isEmpty());
    pt.endEvent("c1", "t2", "success");
    Assert.assertTrue(pt.getEvents().isEmpty());
    pt.endEvent("c1", "t1", "success");
    Assert.assertEquals(1, pt.getEvents().size());

    // start two more events
    pt.startEvent("c2", "t1", "testing");
    pt.startEvent("c2", "t2", "testing");
    // close one and start another
    pt.endEvent("c2", "t2", "testing");
    Assert.assertEquals(1, pt.getEvents().size());
    pt.startEvent("c2", "t3", "testing");
    pt.endEvent("c2", "t3", "testing");
    Assert.assertEquals(1, pt.getEvents().size());
    // start another event and then end the original event
    pt.startEvent("c2", "t4", "testing");
    pt.endEvent("c2", "t1", "success");
    Assert.assertEquals(2, pt.getEvents().size());

    // verify contents of the ProcessTrace
    List<ProcessTraceEvent> evts = pt.getEvents();
    ProcessTraceEvent evt0 = evts.get(0);
    Assert.assertEquals("c1", evt0.getComponentName());
    Assert.assertEquals("t1", evt0.getType());
    Assert.assertEquals("testing", evt0.getDescription());
    Assert.assertEquals("success", evt0.getResultMessage());
    List<ProcessTraceEvent> subEvts = evt0.getSubEvents();
    ProcessTraceEvent subEvt0 = subEvts.get(0);
    Assert.assertEquals("c1", subEvt0.getComponentName());
    Assert.assertEquals("t2", subEvt0.getType());
    Assert.assertEquals("testing", subEvt0.getDescription());
    Assert.assertEquals("success", subEvt0.getResultMessage());
    Assert.assertTrue(subEvt0.getSubEvents().isEmpty());

    ProcessTraceEvent evt1 = evts.get(1);
    Assert.assertEquals("c2", evt1.getComponentName());
    Assert.assertEquals("t1", evt1.getType());
    Assert.assertEquals("testing", evt1.getDescription());
    Assert.assertEquals("success", evt1.getResultMessage());
    Assert.assertEquals(3, evt1.getSubEvents().size());
  }

  public void testAddEvent() {
    ProcessTrace_impl pt = new ProcessTrace_impl();
    // should be nothing on event list
    Assert.assertTrue(pt.getEvents().isEmpty());
    // add event
    pt.addEvent("c1", "t1", "testing", 0, "success");
    // should be one thing on list
    Assert.assertEquals(1, pt.getEvents().size());
    // start an event
    pt.startEvent("c2", "t1", "testing");
    // add event
    pt.addEvent("c2", "t2", "testing", 0, "success");
    // should still be one thing on list
    Assert.assertEquals(1, pt.getEvents().size());
    // end event that we started
    pt.endEvent("c2", "t1", "success");
    // should be 2 events on list
    Assert.assertEquals(2, pt.getEvents().size());
    // 2nd event should have a sub-event
    ProcessTraceEvent evt = pt.getEvents().get(1);
    Assert.assertEquals(1, evt.getSubEvents().size());
  }

  /*
   * Test for List getEventsByComponentName(String, boolean)
   */
  public void testGetEventsByComponentName() {
    ProcessTrace pt = new ProcessTrace_impl();
    // create some events
    pt.startEvent("c1", "t1", "testing");
    pt.startEvent("c1", "t2", "testing");
    pt.endEvent("c1", "t2", "success");
    pt.endEvent("c1", "t1", "success");
    pt.startEvent("c2", "t1", "testing");
    pt.startEvent("c2", "t2", "testing");
    pt.endEvent("c2", "t2", "testing");
    pt.startEvent("c2", "t3", "testing");
    pt.endEvent("c2", "t3", "testing");
    pt.startEvent("c2", "t4", "testing");
    pt.endEvent("c2", "t1", "success");

    // get top-level events for component c1
    List<ProcessTraceEvent> c1evts = pt.getEventsByComponentName("c1", false);
    Assert.assertEquals(1, c1evts.size());
    ProcessTraceEvent evt = c1evts.get(0);
    Assert.assertEquals(evt.getType(), "t1");

    // get all events for component c1
    c1evts = pt.getEventsByComponentName("c1", true);
    Assert.assertEquals(2, c1evts.size());
    evt = c1evts.get(1);
    Assert.assertEquals(evt.getType(), "t2");

    // get top-level events for component c2
    List<ProcessTraceEvent> c2evts = pt.getEventsByComponentName("c2", false);
    Assert.assertEquals(1, c2evts.size());
    evt = c2evts.get(0);
    Assert.assertEquals(evt.getType(), "t1");

    // get all events for component c2
    c2evts = pt.getEventsByComponentName("c2", true);
    Assert.assertEquals(4, c2evts.size());
    evt = c2evts.get(3);
    Assert.assertEquals(evt.getType(), "t4");
  }

  /*
   * Test for List getEventsByType(String, boolean)
   */
  public void testGetEventsByType() {
    ProcessTrace pt = new ProcessTrace_impl();
    // create some events
    pt.startEvent("c1", "t1", "testing");
    pt.startEvent("c1", "t2", "testing");
    pt.endEvent("c1", "t2", "success");
    pt.endEvent("c1", "t1", "success");
    pt.startEvent("c2", "t1", "testing");
    pt.startEvent("c2", "t2", "testing");
    pt.endEvent("c2", "t2", "testing");
    pt.startEvent("c3", "t1", "testing");
    pt.endEvent("c3", "t1", "testing");
    pt.startEvent("c2", "t3", "testing");
    pt.endEvent("c2", "t1", "success");

    // get top-level events of type t1
    List<ProcessTraceEvent> t1evts = pt.getEventsByType("t1", false);
    Assert.assertEquals(2, t1evts.size());
    ProcessTraceEvent evt = t1evts.get(0);
    Assert.assertEquals(evt.getComponentName(), "c1");
    evt = t1evts.get(1);
    Assert.assertEquals(evt.getComponentName(), "c2");

    // get all events for type t1
    t1evts = pt.getEventsByType("t1", true);
    Assert.assertEquals(3, t1evts.size());
    evt = t1evts.get(2);
    Assert.assertEquals(evt.getComponentName(), "c3");
  }

  /*
   * Test for ProcessTraceEvent getEvent(String, String)
   */
  public void testGetEvent() {
    ProcessTrace_impl pt = new ProcessTrace_impl();
    // create some events
    pt.startEvent("c1", "t1", "testing");
    pt.startEvent("c1", "t2", "testing");
    pt.endEvent("c1", "t2", "success");
    pt.endEvent("c1", "t1", "success");
    pt.startEvent("c2", "t1", "testing");
    pt.startEvent("c2", "t2", "testing");
    pt.endEvent("c2", "t2", "testing");
    pt.startEvent("c3", "t1", "testing");
    pt.endEvent("c3", "t1", "testing");
    pt.startEvent("c2", "t3", "testing");
    pt.endEvent("c2", "t1", "success");

    ProcessTraceEvent evt = pt.getEvent("c2", "t2");
    Assert.assertEquals("c2", evt.getComponentName());
    Assert.assertEquals("t2", evt.getType());

    evt = pt.getEvent("c3", "t2");
    Assert.assertNull(evt);
  }

  public void testAggregate() {
    // create two ProcessTrace objects
    ProcessTrace_impl pt1 = new ProcessTrace_impl();
    pt1.addEvent("c1", "t1", "testing", 1000, "success");
    pt1.startEvent("c2", "t1", "testing");
    pt1.addEvent("c2", "t2", "testing", 500, "success");
    pt1.endEvent("c2", "t1", "success");

    ProcessTrace_impl pt2 = new ProcessTrace_impl();
    pt2.startEvent("c2", "t1", "testing");
    pt2.addEvent("c2", "t2", "testing", 500, "success");
    pt2.endEvent("c2", "t1", "success");
    pt2.addEvent("c1", "t1", "testing", 250, "success");

    pt1.aggregate(pt2);
    ProcessTraceEvent c1evt = pt1.getEvents().get(0);
    ProcessTraceEvent c2evt = pt1.getEvents().get(1);
    ProcessTraceEvent c2subEvt = c2evt.getSubEvents().get(0);
    Assert.assertEquals(1250, c1evt.getDuration());
    Assert.assertEquals(1000, c2subEvt.getDuration());
  }

}
