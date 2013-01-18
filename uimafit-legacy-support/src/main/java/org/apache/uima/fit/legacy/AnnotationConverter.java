package org.apache.uima.fit.legacy;

import java.lang.annotation.Annotation;

/**
 * Annotation converters for legacy uimaFIT annotations to Apache uimaFIT annotations.
 *
 * @param <L> legacy annotation type.
 * @param <M> modern annotation type.
 */
public interface AnnotationConverter<L extends Annotation,M extends Annotation> {
  /**
   * Convert the given legacy annotation to its modern counterpart.
   * 
   * @param aAnnotation a legacy annotation.
   * @return the modern annotation.
   */
  M convert(L aAnnotation);
  
  Class<M> getModernType();
  
  Class<L> getLegacyType();
}
