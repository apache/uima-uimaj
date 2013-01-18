package org.apache.uima.fit.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;

public interface LegacySupportPlugin {
  /**
   * Checks if a legacy version of the given modern annotation is present.
   * 
   * @param aObject an object that might have a legacy annotation.
   * @param aAnnotationClass the modern annotation type.
   * @return {@code true} if a legacy version of the annotation is present.
   */
  boolean isAnnotationPresent(AccessibleObject aObject, Class<? extends Annotation> aAnnotationClass);

  /**
   * Checks if a legacy version of the given modern annotation is present.
   * 
   * @param aObject an object that might have a legacy annotation.
   * @param aAnnotationClass the modern annotation type.
   * @return {@code true} if a legacy version of the annotation is present.
   */
  boolean isAnnotationPresent(Class<?> aObject, Class<? extends Annotation> aAnnotationClass);

  /**
   * Gets the annotation from the given object. Instead of looking for the given modern annotation,
   * this method looks for a legacy version of the annotation, converts it to a modern annotation
   * and returns that.
   * 
   * @param aObject an object that has a legacy annotation.
   * @param aAnnotationClass the modern annotation type.
   * @return an instance of the modern annotation filled with the data from the legacy annotation.
   */
  <L extends Annotation, M extends Annotation> M getAnnotation(AccessibleObject aObject, Class<M> aAnnotationClass);

  /**
   * Gets the annotation from the given object. Instead of looking for the given modern annotation,
   * this method looks for a legacy version of the annotation, converts it to a modern annotation
   * and returns that.
   * 
   * @param aObject an object that has a legacy annotation.
   * @param aAnnotationClass the modern annotation type.
   * @return an instance of the modern annotation filled with the data from the legacy annotation.
   */
  <L extends Annotation, M extends Annotation> M getAnnotation(Class<?> aObject, Class<M> aAnnotationClass);
}
