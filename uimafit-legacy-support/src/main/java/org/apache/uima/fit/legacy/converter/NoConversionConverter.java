package org.apache.uima.fit.legacy.converter;

import java.lang.annotation.Annotation;

import org.apache.uima.fit.legacy.AnnotationConverter;

/**
 * Fallback converter that does not convert anything.
 * 
 * @author Richard Eckart de Castilho
 */
public class NoConversionConverter implements AnnotationConverter<Annotation, Annotation> {

  private static NoConversionConverter instance = null;
  
  public NoConversionConverter() {
    // Nothing to do
  }

  public Annotation convert(Annotation aAnnotation) {
    return null;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public Class<Annotation> getModernType() {
    return (Class) NoAnnotation.class;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public Class<Annotation> getLegacyType() {
    return (Class) NoAnnotation.class;
  }
  
  public static NoConversionConverter getInstance() {
    if (instance == null) {
      instance = new NoConversionConverter();
    }
    return instance;
  }
  
  private @interface NoAnnotation {};
}
