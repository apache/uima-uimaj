package org.apache.uima.fit.examples.experiment.pos;

import java.io.File;
import java.io.IOException;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasConsumer_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.CasIOUtil;
import org.apache.uima.jcas.JCas;

public class XmiWriter extends JCasConsumer_ImplBase {

  public static final String PARAM_OUTPUT_DIRECTORY = "outputDirectory";

  @ConfigurationParameter(name = PARAM_OUTPUT_DIRECTORY, mandatory = true)
  private File outputDirectory;
  private int count = 1;

  @Override
  public void process(JCas aJCas) throws AnalysisEngineProcessException {
    try {
      CasIOUtil.writeXmi(aJCas, new File(outputDirectory, count + ".xmi"));
      count++;
    }
    catch (IOException e) {
      throw new AnalysisEngineProcessException(e);
    }
  }
}
