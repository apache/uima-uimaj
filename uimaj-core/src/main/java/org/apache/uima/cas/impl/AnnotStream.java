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

package org.apache.uima.cas.impl;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.impl.JCasImpl;

/**
 * A builder class for streams over Annotation Indexes
 * Use:
 *   AnnotStream as = new AnnotStream( optional args );
 *   as.<configure methods and args>;  // chained
 *     e.g. reverse(), boundedBy(fs), strict(true), unambiguous(true), typePriorities(false)
 *     
 *   as.stream() - followed by normal stream operations
 *   as.spliterator()
 */
public class AnnotStream <T extends AnnotationFS> implements Stream<T> {

  private final CASImpl cas;
  private TypeImpl type;
  private boolean isReverse;
  private boolean isStrict;
  private boolean isUnambiguous;
  private boolean isUseTypePriorities;
  private TOP boundingFs;
  
  AnnotStream(FsIndex_singletype fsi) {
    this.cas = fsi.casImpl;
    this.type = fsi.getTypeImpl();
  }
  
  public AnnotStream reverse() {return reverse(true);}
  public AnnotStream reverse(boolean b) {isReverse = b; return this;}
  
  public AnnotStream strict() { return strict(true); }
  public AnnotStream strict(boolean b) {isStrict = b; return this;}

  public AnnotStream useTypePriorities() { return useTypePriorities(true); }
  public AnnotStream useTypePriorities(boolean b) {isUseTypePriorities = b; return this;}

  public AnnotStream boundingFs(TOP fs) { boundingFs = fs; return this;}

  /*************************************************
   * Stream methods
   *************************************************/
  @Override
  public Iterator<T> iterator() {return null;
//  maybeMakeStream().iterator();
  }

//  @Override
//  public Spliterator<T> spliterator() {
//    // TODO Auto-generated method stub
//    return null;
//  }

  @Override
  public boolean isParallel() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public Stream<T> sequential() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Stream<T> parallel() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Stream<T> unordered() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Stream<T> onClose(Runnable closeHandler) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void close() {
    // TODO Auto-generated method stub
    
  }

  @Override
  public Stream<T> filter(Predicate<? super T> predicate) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <R> Stream<R> map(Function<? super T, ? extends R> mapper) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public IntStream mapToInt(ToIntFunction<? super T> mapper) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public LongStream mapToLong(ToLongFunction<? super T> mapper) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public DoubleStream mapToDouble(ToDoubleFunction<? super T> mapper) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public IntStream flatMapToInt(Function<? super T, ? extends IntStream> mapper) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public LongStream flatMapToLong(Function<? super T, ? extends LongStream> mapper) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public DoubleStream flatMapToDouble(Function<? super T, ? extends DoubleStream> mapper) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Stream<T> distinct() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Stream<T> sorted() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Stream<T> sorted(Comparator<? super T> comparator) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Stream<T> peek(Consumer<? super T> action) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Stream<T> limit(long maxSize) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Stream<T> skip(long n) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void forEach(Consumer<? super T> action) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void forEachOrdered(Consumer<? super T> action) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public Object[] toArray() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <A> A[] toArray(IntFunction<A[]> generator) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public T reduce(T identity, BinaryOperator<T> accumulator) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Optional<T> reduce(BinaryOperator<T> accumulator) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator,
      BinaryOperator<U> combiner) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator,
      BiConsumer<R, R> combiner) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <R, A> R collect(Collector<? super T, A, R> collector) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Optional<T> min(Comparator<? super T> comparator) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Optional<T> max(Comparator<? super T> comparator) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public long count() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public boolean anyMatch(Predicate<? super T> predicate) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean allMatch(Predicate<? super T> predicate) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean noneMatch(Predicate<? super T> predicate) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public Optional<T> findFirst() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Optional<T> findAny() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Spliterator<T> spliterator() {
    // TODO Auto-generated method stub
    return null;
  }  
  
}
