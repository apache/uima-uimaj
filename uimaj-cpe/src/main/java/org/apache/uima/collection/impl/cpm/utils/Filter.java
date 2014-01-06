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

package org.apache.uima.collection.impl.cpm.utils;

import java.text.ParseException;
import java.util.LinkedList;
import java.util.Stack;
import java.util.StringTokenizer;

import org.apache.uima.UIMAFramework;
import org.apache.uima.util.Level;

/**
 * Parses the filter expression associated with a Cas Processor in the cpe descriptor
 * 
 * 
 */
public class Filter {
  Stack stack = new Stack();

  LinkedList expressionList = new LinkedList();

  protected boolean isAndFilter = false;

  protected boolean isOrFilter = true; // default filter is OR as it is least restrictive

  protected boolean filterInitialized = false;

  /**
   * Parses filter expression
   * 
   * @param expression -
   *          filter expression to parse
   * @return - list of filters
   * 
   * @throws ParseException -
   */
  public LinkedList parse(String expression) throws ParseException {
    StringTokenizer tokenizer = new StringTokenizer(expression, " !=", true);
    parseTokens(tokenizer);
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < expressionList.size(); i++) {
      Expression ex = (Expression) expressionList.get(i);
      if (ex.hasLeftPart()) {
        sb.append(ex.getLeftPart().get() + " ");
      }
      if (ex.hasOperand()) {
        sb.append(ex.getOperand().getOperand() + " ");
      }
      if (ex.hasRightPart()) {
        sb.append(ex.getRightPart().get() + " ");
      }
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_expression__FINEST",
                new Object[] { Thread.currentThread().getName(), sb.toString() });

      }
      sb.setLength(0);
    }
    return expressionList;
  }

  /**
   * Parses tokens
   * 
   * @param aTokenizer -
   *          tokenized filter expression
   * 
   * @throws ParseException -
   */
  private void parseTokens(StringTokenizer aTokenizer) throws ParseException {
    boolean leftPartInStack = false;

    while (aTokenizer.hasMoreTokens()) {
      String token = aTokenizer.nextToken();

      if ("where".equals(token)) {
        continue;
      } else if ("".equals(token.trim())) {
        continue;
      } else if ("!".equals(token)) {
        stack.push(new Operand("!"));
      } else if ("=".equals(token)) {
        Object o = stack.peek();

        if (o instanceof Operand && "!".equals(((Operand) o).getOperand())) {
          stack.pop();
          stack.push(new Operand("!="));
        }
        stack.push(new Operand("="));
      } else if ("and".equalsIgnoreCase(token) || "or".equalsIgnoreCase(token)) {
        evaluate(token);
      } else {
        if (leftPartInStack == true) {
          stack.push(new RightPart(token));
          leftPartInStack = false;
        } else {
          stack.push(new LeftPart(token));
          leftPartInStack = true;
        }
      }
    }
    if (stack.size() > 0) {
      evaluate(null);
    }
  }

  /**
   * Builds filter expression from values from the stack
   * 
   * @throws ParseException -
   */
  private void evaluate(String anOp) throws ParseException {
    Expression ex = new Expression(this);
    if (anOp != null) {
      if ("or".equalsIgnoreCase(anOp)) {
        ex.setIsOrFilter();
      } else if ("and".equalsIgnoreCase(anOp)) {
        ex.setIsAndFilter();
      }
    }
    boolean valid = false;
    while (stack.size() > 0) {
      valid = true;

      Object o = stack.pop();
      if (o instanceof LeftPart) {
        ex.setLeftPart((LeftPart) o);
      } else if (o instanceof RightPart) {
        ex.setRightPart((RightPart) o);
      } else if (o instanceof Operand) {
        ex.setOperand((Operand) o);
      } else {
        throw new ParseException("Unexpected Entity in the Stack::" + o.getClass().getName(), 0);
      }
    }
    if (valid) {
      expressionList.add(ex);
    }

  }

  /**
   * Operand
   */
  public class Operand {
    private String operand;

    public Operand(String aOp) {
      operand = aOp;
    }

    public String getOperand() {
      return operand;
    }
  }

  /**
   * Left part of filter expression
   */
  public class LeftPart {
    private String leftPart;

    public LeftPart(String aLPart) {
      leftPart = aLPart;
    }

    public String get() {
      return leftPart;
    }
  }

  /**
   * Right part of the filter expression
   */
  public class RightPart {
    private String rightPart;

    public RightPart(String aRPart) {
      rightPart = aRPart;
    }

    public String get() {
      return rightPart;
    }
  }

  /**
   * Object containing single filter
   */
  public class Expression {
    private LeftPart lP;

    private RightPart rP;

    private Operand op;

    private Filter filter = null;

    public Expression(Filter aFilter) {
      filter = aFilter;
    }

    protected void setIsOrFilter() throws ParseException {
      // Already defined as AND filter. Currently filtering is either AND or OR. No mixing is
      // supported
      if (filter.isAndFilter) {
        throw new ParseException(
                "Filter.Expression.setIsOrFilter()-Mixing <AND> and <OR> currently not supported. Choose one conjunction <AND> or disjunction <OR> in your filter.",
                0);
      }
      filter.isOrFilter = true;
      filter.filterInitialized = true;
    }

    protected void setIsAndFilter() throws ParseException {
      // Already defined as OR filter. Currently filtering is either AND or OR. No mixing is
      // supported
      if (filter.filterInitialized && filter.isOrFilter) {
        throw new ParseException(
                "Filter.Expression.setIsOrFilter()-Mixing <AND> and <OR> currently not supported. Choose one conjunction <AND> or disjunction <OR> in your filter.",
                0);
      }
      filter.isAndFilter = true;
      filter.isOrFilter = false; // turnoff default
    }

    public boolean isOrFilter() {
      return filter.isOrFilter;
    }

    public boolean isAndFilter() {
      return filter.isAndFilter;
    }

    public void setLeftPart(LeftPart aLP) {
      lP = aLP;
    }

    public void setRightPart(RightPart aRP) {
      rP = aRP;
    }

    public void setOperand(Operand aOP) {
      op = aOP;
    }

    public boolean hasLeftPart() {
      if (lP != null) {
        return true;
      }
      return false;
    }

    public boolean hasRightPart() {
      if (rP != null) {
        return true;
      }
      return false;
    }

    public boolean hasOperand() {
      if (op != null) {
        return true;
      }
      return false;
    }

    public LeftPart getLeftPart() {
      return lP;
    }

    public RightPart getRightPart() {
      return rP;
    }

    public Operand getOperand() {
      return op;
    }
  }

  public static void main(String[] args) {
    Filter filter = new Filter();
    try {
      filter.parse(args[0]);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
