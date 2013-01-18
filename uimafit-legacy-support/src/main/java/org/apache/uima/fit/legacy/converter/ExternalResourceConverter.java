package org.apache.uima.fit.legacy.converter;

import javax.enterprise.util.AnnotationLiteral;

import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.legacy.AnnotationConverter;
import org.apache.uima.resource.Resource;

public class ExternalResourceConverter
        implements
        AnnotationConverter<org.uimafit.descriptor.ExternalResource, org.apache.uima.fit.descriptor.ExternalResource> {

  public ExternalResourceConverter() {
    // Nothing to do
  }

  public ExternalResource convert(
          final org.uimafit.descriptor.ExternalResource aAnnotation) {
    return new ExternalResourceSubstitute(aAnnotation);
  }

  public Class<org.uimafit.descriptor.ExternalResource> getLegacyType() {
    return org.uimafit.descriptor.ExternalResource.class;
  }
  
  public Class<ExternalResource> getModernType() {
    return ExternalResource.class;
  }
  
  @SuppressWarnings("serial")
  public class ExternalResourceSubstitute extends
          AnnotationLiteral<ExternalResource> implements ExternalResource {

    private org.uimafit.descriptor.ExternalResource legacyAnnotation;
    
    public ExternalResourceSubstitute(org.uimafit.descriptor.ExternalResource aAnnotation) {
      legacyAnnotation = aAnnotation;
    }

    public String key() {
      return legacyAnnotation.key();
    }

    public Class<? extends Resource> api() {
      return legacyAnnotation.api();
    }

    public boolean mandatory() {
      return legacyAnnotation.mandatory();
    }
  }
}
