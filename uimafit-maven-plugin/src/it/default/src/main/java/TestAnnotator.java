import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;

public class TestAnnotator extends JCasAnnotator_ImplBase {

  /**
   * Parameter value 1.
   */
  public static final String PARAM_VALUE_1 = "value1";
  @ConfigurationParameter(name = PARAM_VALUE_1, mandatory=true)
  private String value1;
  
  @Override
  public void process(JCas aJCas) throws AnalysisEngineProcessException {
    // Nothing to do
  }
}
