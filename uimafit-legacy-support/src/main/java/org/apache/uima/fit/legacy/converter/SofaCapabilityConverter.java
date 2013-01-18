package org.apache.uima.fit.legacy.converter;

import javax.enterprise.util.AnnotationLiteral;

import org.apache.uima.fit.descriptor.SofaCapability;
import org.apache.uima.fit.legacy.AnnotationConverter;

public class SofaCapabilityConverter
        implements
        AnnotationConverter<org.uimafit.descriptor.SofaCapability, org.apache.uima.fit.descriptor.SofaCapability> {

  public SofaCapabilityConverter() {
    // Nothing to do
  }

  public SofaCapability convert(
          final org.uimafit.descriptor.SofaCapability aAnnotation) {
    return new SofaCapabilitySubstitute(aAnnotation);
  }

  public Class<org.uimafit.descriptor.SofaCapability> getLegacyType() {
    return org.uimafit.descriptor.SofaCapability.class;
  }
  
  public Class<SofaCapability> getModernType() {
    return SofaCapability.class;
  }
  
  @SuppressWarnings("serial")
  public class SofaCapabilitySubstitute extends
          AnnotationLiteral<SofaCapability> implements SofaCapability {

    private org.uimafit.descriptor.SofaCapability legacyAnnotation;
    
    public SofaCapabilitySubstitute(org.uimafit.descriptor.SofaCapability aAnnotation) {
      legacyAnnotation = aAnnotation;
    }

    public String[] inputSofas() {
      return legacyAnnotation.inputSofas();
    }

    public String[] outputSofas() {
      return legacyAnnotation.outputSofas();
    }
  }
}
