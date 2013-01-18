package org.apache.uima.fit.legacy.converter;

import javax.enterprise.util.AnnotationLiteral;

import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.legacy.AnnotationConverter;

public class TypeCapabilityConverter
        implements
        AnnotationConverter<org.uimafit.descriptor.TypeCapability, org.apache.uima.fit.descriptor.TypeCapability> {

  public TypeCapabilityConverter() {
    // Nothing to do
  }

  public TypeCapability convert(
          final org.uimafit.descriptor.TypeCapability aAnnotation) {
    return new TypeCapabilitySubstitute(aAnnotation);
  }

  public Class<org.uimafit.descriptor.TypeCapability> getLegacyType() {
    return org.uimafit.descriptor.TypeCapability.class;
  }
  
  public Class<TypeCapability> getModernType() {
    return TypeCapability.class;
  }
  
  @SuppressWarnings("serial")
  public class TypeCapabilitySubstitute extends
          AnnotationLiteral<TypeCapability> implements TypeCapability {

    private org.uimafit.descriptor.TypeCapability legacyAnnotation;
    
    public TypeCapabilitySubstitute(org.uimafit.descriptor.TypeCapability aAnnotation) {
      legacyAnnotation = aAnnotation;
    }

    public String[] inputs() {
      return legacyAnnotation.inputs();
    }

    public String[] outputs() {
      return legacyAnnotation.outputs();
    }
  }
}
