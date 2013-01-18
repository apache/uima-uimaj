package org.apache.uima.fit.legacy.converter;

import javax.enterprise.util.AnnotationLiteral;

import org.apache.uima.fit.descriptor.FsIndex;
import org.apache.uima.fit.descriptor.FsIndexCollection;
import org.apache.uima.fit.legacy.AnnotationConverter;

public class FsIndexCollectionConverter
        implements
        AnnotationConverter<org.uimafit.descriptor.FsIndexCollection, org.apache.uima.fit.descriptor.FsIndexCollection> {

  public FsIndexCollectionConverter() {
    // Nothing to do
  }

  public FsIndexCollection convert(
          final org.uimafit.descriptor.FsIndexCollection aAnnotation) {
    return new FsIndexCollectionSubstitute(aAnnotation);
  }

  public Class<org.uimafit.descriptor.FsIndexCollection> getLegacyType() {
    return org.uimafit.descriptor.FsIndexCollection.class;
  }
  
  public Class<FsIndexCollection> getModernType() {
    return FsIndexCollection.class;
  }
  
  @SuppressWarnings("serial")
  public class FsIndexCollectionSubstitute extends
          AnnotationLiteral<FsIndexCollection> implements FsIndexCollection {

    private org.uimafit.descriptor.FsIndexCollection legacyAnnotation;
    
    public FsIndexCollectionSubstitute(org.uimafit.descriptor.FsIndexCollection aAnnotation) {
      legacyAnnotation = aAnnotation;
    }

    public FsIndex[] fsIndexes() {
      FsIndex[] result = new FsIndex[legacyAnnotation.fsIndexes().length];
      FsIndexConverter conv = new FsIndexConverter();
      int i = 0;
      for (org.uimafit.descriptor.FsIndex k : legacyAnnotation.fsIndexes()) {
        result[i] = conv.convert(k);
        i++;
      }
      return result;
    }
  }
}
