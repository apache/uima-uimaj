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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.apache.uima.util.ProcessTrace;
import org.apache.uima.util.ProcessTraceEvent;
import org.junit.jupiter.api.Test;

public class ProcessTrace_implTest {
  /*
   * @see TestCase#setUp()
   */
  @Test
  public void testStartAndEndEvent() {
    ProcessTrace pt = new ProcessTrace_impl();
    // should be nothing on event list
    assertThat(pt.getEvents().isEmpty()).isTrue();
    // start two events
    pt.startEvent("c1", "t1", "testing");
    pt.startEvent("c1", "t2", "testing");
    // should be nothing on event list until both are closed
    assertThat(pt.getEvents().isEmpty()).isTrue();
    pt.endEvent("c1", "t2", "success");
    assertThat(pt.getEvents().isEmpty()).isTrue();
    pt.endEvent("c1", "t1", "success");
    assertThat(pt.getEvents()).hasSize(1);

    // start two more events
    pt.startEvent("c2", "t1", "testing");
    pt.startEvent("c2", "t2", "testing");
    // close one and start another
    pt.endEvent("c2", "t2", "testing");
    assertThat(pt.getEvents()).hasSize(1);
    pt.startEvent("c2", "t3", "testing");
    pt.endEvent("c2", "t3", "testing");
    assertThat(pt.getEvents()).hasSize(1);
    // start another event and then end the original event
    pt.startEvent("c2", "t4", "testing");
    pt.endEvent("c2", "t1", "success");
    assertThat(pt.getEvents()).hasSize(2);

    // verify contents of the ProcessTrace
    List<ProcessTraceEvent> evts = pt.getEvents();
    ProcessTraceEvent evt0 = evts.get(0);
    assertThat(evt0.getComponentName()).isEqualTo("c1");
    assertThat(evt0.getType()).isEqualTo("t1");
    assertThat(evt0.getDescription()).isEqualTo("testing");
    assertThat(evt0.getResultMessage()).isEqualTo("success");
    List<ProcessTraceEvent> subEvts = evt0.getSubEvents();
    ProcessTraceEvent subEvt0 = subEvts.get(0);
    assertThat(subEvt0.getComponentName()).isEqualTo("c1");
    assertThat(subEvt0.getType()).isEqualTo("t2");
    assertThat(subEvt0.getDescription()).isEqualTo("testing");
    assertThat(subEvt0.getResultMessage()).isEqualTo("success");
    assertThat(subEvt0.getSubEvents().isEmpty()).isTrue();

    ProcessTraceEvent evt1 = evts.get(1);
    assertThat(evt1.getComponentName()).isEqualTo("c2");
    assertThat(evt1.getType()).isEqualTo("t1");
    assertThat(evt1.getDescription()).isEqualTo("testing");
    assertThat(evt1.getResultMessage()).isEqualTo("success");
    assertThat(evt1.getSubEvents()).hasSize(3);
  }

  @Test
  public void testAddEvent() {
    ProcessTrace_impl pt = new ProcessTrace_impl();
    // should be nothing on event list
    assertThat(pt.getEvents().isEmpty()).isTrue();
    // add event
    pt.addEvent("c1", "t1", "testing", 0, "success");
    // should be one thing on list
    assertThat(pt.getEvents()).hasSize(1);
    // start an event
    pt.startEvent("c2", "t1", "testing");
    // add event
    pt.addEvent("c2", "t2", "testing", 0, "success");
    // should still be one thing on list
    assertThat(pt.getEvents()).hasSize(1);
    // end event that we started
    pt.endEvent("c2", "t1", "success");
    // should be 2 events on list
    assertThat(pt.getEvents()).hasSize(2);
    // 2nd event should have a sub-event
    ProcessTraceEvent evt = pt.getEvents().get(1);
    assertThat(evt.getSubEvents()).hasSize(1);
  }

  /*
   * Test for List getEventsByComponentName(String, boolean)
   */
  @Test
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
    assertThat(c1evts).hasSize(1);
    ProcessTraceEvent evt = c1evts.get(0);
    assertThat("t1").isEqualTo(evt.getType());

    // get all events for component c1
    c1evts = pt.getEventsByComponentName("c1", true);
    assertThat(c1evts).hasSize(2);
    evt = c1evts.get(1);
    assertThat("t2").isEqualTo(evt.getType());

    // get top-level events for component c2
    List<ProcessTraceEvent> c2evts = pt.getEventsByComponentName("c2", false);
    assertThat(c2evts).hasSize(1);
    evt = c2evts.get(0);
    assertThat("t1").isEqualTo(evt.getType());

    // get all events for component c2
    c2evts = pt.getEventsByComponentName("c2", true);
    assertThat(c2evts).hasSize(4);
    evt = c2evts.get(3);
    assertThat("t4").isEqualTo(evt.getType());
  }

  /*
   * Test for List getEventsByType(String, boolean)
   */
  @Test
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
    assertThat(t1evts).hasSize(2);
    ProcessTraceEvent evt = t1evts.get(0);
    assertThat("c1").isEqualTo(evt.getComponentName());
    evt = t1evts.get(1);
    assertThat("c2").isEqualTo(evt.getComponentName());

    // get all events for type t1
    t1evts = pt.getEventsByType("t1", true);
    assertThat(t1evts).hasSize(3);
    evt = t1evts.get(2);
    assertThat("c3").isEqualTo(evt.getComponentName());
  }

  /*
   * Test for ProcessTraceEvent getEvent(String, String)
   */
  @Test
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
    assertThat(evt.getComponentName()).isEqualTo("c2");
    assertThat(evt.getType()).isEqualTo("t2");

    evt = pt.getEvent("c3", "t2");
    assertThat(evt).isNull();
  }

  @Test
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
    assertThat(c1evt.getDuration()).isEqualTo(1250);
    assertThat(c2subEvt.getDuration()).isEqualTo(1000);
  }

}
