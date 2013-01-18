package org.apache.uima.fit.legacy.converter;

import javax.enterprise.util.AnnotationLiteral;

import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.legacy.AnnotationConverter;

public class ConfigurationParameterConverter
        implements
        AnnotationConverter<org.uimafit.descriptor.ConfigurationParameter, org.apache.uima.fit.descriptor.ConfigurationParameter> {

  public ConfigurationParameterConverter() {
    // Nothing to do
  }

  public ConfigurationParameter convert(
          final org.uimafit.descriptor.ConfigurationParameter aAnnotation) {
    return new ConfigurationParameterSubstitute(aAnnotation);
  }

  public Class<org.uimafit.descriptor.ConfigurationParameter> getLegacyType() {
    return org.uimafit.descriptor.ConfigurationParameter.class;
  }
  
  public Class<ConfigurationParameter> getModernType() {
    return ConfigurationParameter.class;
  }
  
  @SuppressWarnings("serial")
  public class ConfigurationParameterSubstitute extends
          AnnotationLiteral<ConfigurationParameter> implements ConfigurationParameter {

    private org.uimafit.descriptor.ConfigurationParameter legacyAnnotation;
    
    public ConfigurationParameterSubstitute(org.uimafit.descriptor.ConfigurationParameter aAnnotation) {
      legacyAnnotation = aAnnotation;
    }
    
    public String name() {
      return legacyAnnotation.name();
    }

    public String description() {
      return legacyAnnotation.description();
    }

    public boolean mandatory() {
      return legacyAnnotation.mandatory();
    }

    public String[] defaultValue() {
      return legacyAnnotation.defaultValue();
    }
  }
}
