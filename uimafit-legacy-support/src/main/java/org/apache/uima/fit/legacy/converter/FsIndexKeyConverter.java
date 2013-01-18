package org.apache.uima.fit.legacy.converter;

import javax.enterprise.util.AnnotationLiteral;

import org.apache.uima.fit.descriptor.FsIndexKey;
import org.apache.uima.fit.legacy.AnnotationConverter;

public class FsIndexKeyConverter
        implements
        AnnotationConverter<org.uimafit.descriptor.FsIndexKey, org.apache.uima.fit.descriptor.FsIndexKey> {

  public FsIndexKeyConverter() {
    // Nothing to do
  }

  public FsIndexKey convert(
          final org.uimafit.descriptor.FsIndexKey aAnnotation) {
    return new FsIndexKeySubstitute(aAnnotation);
  }

  public Class<org.uimafit.descriptor.FsIndexKey> getLegacyType() {
    return org.uimafit.descriptor.FsIndexKey.class;
  }
  
  public Class<FsIndexKey> getModernType() {
    return FsIndexKey.class;
  }
  
  @SuppressWarnings("serial")
  public class FsIndexKeySubstitute extends
          AnnotationLiteral<FsIndexKey> implements FsIndexKey {

    private org.uimafit.descriptor.FsIndexKey legacyAnnotation;
    
    public FsIndexKeySubstitute(org.uimafit.descriptor.FsIndexKey aAnnotation) {
      legacyAnnotation = aAnnotation;
    }

    public String featureName() {
      return legacyAnnotation.featureName();
    }

    public int comparator() {
      return legacyAnnotation.comparator();
    }
  }
}
