package org.apache.uima.fit.legacy.converter;

import javax.enterprise.util.AnnotationLiteral;

import org.apache.uima.fit.descriptor.FsIndex;
import org.apache.uima.fit.descriptor.FsIndexKey;
import org.apache.uima.fit.legacy.AnnotationConverter;
import org.apache.uima.jcas.cas.TOP;

public class FsIndexConverter
        implements
        AnnotationConverter<org.uimafit.descriptor.FsIndex, org.apache.uima.fit.descriptor.FsIndex> {

  public FsIndexConverter() {
    // Nothing to do
  }

  public FsIndex convert(
          final org.uimafit.descriptor.FsIndex aAnnotation) {
    return new FsIndexSubstitute(aAnnotation);
  }

  public Class<org.uimafit.descriptor.FsIndex> getLegacyType() {
    return org.uimafit.descriptor.FsIndex.class;
  }
  
  public Class<FsIndex> getModernType() {
    return FsIndex.class;
  }
  
  @SuppressWarnings("serial")
  public class FsIndexSubstitute extends
          AnnotationLiteral<FsIndex> implements FsIndex {

    private org.uimafit.descriptor.FsIndex legacyAnnotation;

    public FsIndexSubstitute(org.uimafit.descriptor.FsIndex aAnnotation) {
      legacyAnnotation = aAnnotation;
    }

    public String label() {
      return legacyAnnotation.label();
    }

    public String typeName() {
      return legacyAnnotation.typeName();
    }

    public Class<? extends TOP> type() {
      return legacyAnnotation.type();
    }

    public String kind() {
      return legacyAnnotation.kind();
    }

    public FsIndexKey[] keys() {
      FsIndexKey[] result = new FsIndexKey[legacyAnnotation.keys().length];
      FsIndexKeyConverter conv = new FsIndexKeyConverter();
      int i = 0;
      for (org.uimafit.descriptor.FsIndexKey k : legacyAnnotation.keys()) {
        result[i] = conv.convert(k);
        i++;
      }
      return result;
    }

    public boolean typePriorities() {
      return legacyAnnotation.typePriorities();
    }
  }
}
