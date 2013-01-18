package org.apache.uima.fit.legacy.converter;

import javax.enterprise.util.AnnotationLiteral;

import org.apache.uima.fit.descriptor.OperationalProperties;
import org.apache.uima.fit.legacy.AnnotationConverter;

public class OperationalPropertiesConverter
        implements
        AnnotationConverter<org.uimafit.descriptor.OperationalProperties, org.apache.uima.fit.descriptor.OperationalProperties> {

  public OperationalPropertiesConverter() {
    // Nothing to do
  }

  public OperationalProperties convert(
          final org.uimafit.descriptor.OperationalProperties aAnnotation) {
    return new OperationalPropertiesSubstitute(aAnnotation);
  }

  public Class<org.uimafit.descriptor.OperationalProperties> getLegacyType() {
    return org.uimafit.descriptor.OperationalProperties.class;
  }
  
  public Class<OperationalProperties> getModernType() {
    return OperationalProperties.class;
  }
  
  @SuppressWarnings("serial")
  public class OperationalPropertiesSubstitute extends
          AnnotationLiteral<OperationalProperties> implements OperationalProperties {

    private org.uimafit.descriptor.OperationalProperties legacyAnnotation;
    
    public OperationalPropertiesSubstitute(org.uimafit.descriptor.OperationalProperties aAnnotation) {
      legacyAnnotation = aAnnotation;
    }

    public boolean multipleDeploymentAllowed() {
      return legacyAnnotation.multipleDeploymentAllowed();
    }

    public boolean modifiesCas() {
      return legacyAnnotation.modifiesCas();
    }

    public boolean outputsNewCases() {
      return legacyAnnotation.outputsNewCases();
    }
  }
}
