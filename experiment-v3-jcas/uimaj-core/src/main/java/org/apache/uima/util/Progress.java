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

package org.apache.uima.util;

/**
 * Progress statistics for a process. This is represented by an amount completed, a total amount (if
 * known), and a unit. There are some predefined unit types ({@link #BYTES}, {@link #ENTITIES}),
 * but any unit can be used.
 * 
 * 
 */
public interface Progress extends java.io.Serializable {

  /**
   * The amount completed, in terms of units specified by {@link #getUnit()}.
   * 
   * @return the amount completed
   */
  public long getCompleted();

  /**
   * The total amount being processed, in terms of units specified by {@link #getUnit()}. For some
   * processes, this information may not be available - in these cases, -1 will be returned.
   * 
   * @return the total amount, -1 if not known
   */
  public long getTotal();

  /**
   * The unit type represented by the {@link #getCompleted()} and {@link #getTotal()} numbers. There
   * are some predefined unit types ({@link #BYTES}, {@link #ENTITIES}), but any unit can be
   * used.
   * 
   * @return the unit
   */
  public String getUnit();

  /**
   * Returns true if the progress statistics are approximate, for example if the total number of
   * entities in the collection is not known.
   * 
   * @return true if the statistics are approximate, false if they are exact
   */
  public boolean isApproximate();

  /**
   * The predefined unit type "entities". An entity is the thing being processed, for example a
   * document. When this unit is used, the amount completed and total amount represent a number of
   * entities.
   */
  public String ENTITIES = "entities";

  /**
   * The predefined unit type "bytes". When this unit is used, the amount completed and total amount
   * represent the size of the data in bytes.
   */
  public String BYTES = "bytes";
}
