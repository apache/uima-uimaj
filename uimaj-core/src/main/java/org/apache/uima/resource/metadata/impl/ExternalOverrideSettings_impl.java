/**
 * 
 */
package org.apache.uima.resource.metadata.impl;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.UIMAFramework;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.ExternalOverrideSettings;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.Level;
import org.apache.uima.util.impl.Settings_impl;

/**
 * Class created by the XML parser representing the externalOverrideSettings element.
 * Contains the imported settings files and the inline settings.
 * 
 * @author burn
 *
 */
public class ExternalOverrideSettings_impl extends MetaDataObject_impl implements ExternalOverrideSettings {

  /**
   * resource bundle for log messages
   */
  protected static final String LOG_RESOURCE_BUNDLE = "org.apache.uima.impl.log_messages";
  
  private static final long serialVersionUID = 1632935723327541095L;
  
  private Import[] mImports = null;
  
  private String mSettings = null;
  
  private Settings_impl mProperties = null;

  /*
   * Is true if settings or imports have been declared more than once
   */
  private boolean multipleEntries = false;

  /*
   * Regex that matches ${...}
   * non-greedy so stops on first '}' -- hence key cannot contain '}'
   */
  private Pattern evalPattern = Pattern.compile("\\$\\{.*?\\}");

  /* (non-Javadoc)
   * @see org.apache.uima.resource.metadata.ExternalOverrideSettings#getImport()
   */
  public Import[] getImports() {
    return mImports;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.resource.metadata.ExternalOverrideSettings#getSettings()
   */
  public String getSettings() {
    return mSettings;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.resource.metadata.ExternalOverrideSettings#getSettings(org.apache.uima.resource.metadata.NameValuePair[])
   */
  public void setSettings(String aSettings) {
    if (mSettings != null) {
      multipleEntries = true;
      return;
    }
    mSettings = aSettings;
  }

  /* 
   * When this is cloned the mImports array is copied by super.clone and then MetaDataObject_impl
   * copies all arrays and cslls this method.  So must not complain if given identical data.  
   */
  public void setImports(Import[] aImports) {
    if (mImports != null) {
      if (Arrays.equals(aImports, mImports)) {
        return;
      }
      multipleEntries = true;
      return;
    }
    mImports = aImports;
  }

  /* 
   * Look up value for external name from the external override settings.
   * Perform one substitution pass on ${key} substrings.  Undefined keys get the empty string.
   * Recursively evaluate the value to be substituted.  NOTE: infinite loops not detected!
   * To avoid evaluation and get ${key} in the output use a property to generate the $, e.g. 
   *   $   = $
   *   key = ${$}{key}
   * The Properties class processes the \ escape character so it must be doubled to survive
   */
  public String resolveExternalName(String name) {
    String value;
    if (mProperties == null || (value = mProperties.getProperty(name)) == null) {
      return null;
    }
    Matcher matcher = evalPattern.matcher(value);
    StringBuilder result = new StringBuilder(value.length() + 100);
    int lastEnd = 0;
    while (matcher.find()) {
      // Check if the $ is escaped
      if (mProperties.isEscaped(value,matcher.start())) {
        result.append(value.substring(lastEnd, matcher.start() + 1));
        lastEnd = matcher.start() + 1;  // copy the escaped $ and restart after it
      } else {
        result.append(value.substring(lastEnd, matcher.start()));
        lastEnd = matcher.end();
        String val = resolveExternalName(value.substring(matcher.start() + 2, lastEnd - 1));
        if (val != null) {    // If variable is undefined replace with nothing
          result.append(val);
        }
      }
    }
    if (lastEnd == 0) {
      return value;
    } else {
      result.append(value.substring(lastEnd));
      return result.toString();
    }
  }

  /* (non-Javadoc)
   * @see org.apache.uima.resource.metadata.ExternalOverrideSettings#resolveImports()
   */
  public void resolveImports() throws ResourceConfigurationException {
    resolveImports(UIMAFramework.newDefaultResourceManager());
  }

  /* 
   * Resolve imports and load properties files in declaration order for top-down priority
   * Also load inline settings to override or follow the imports
   */
  public void resolveImports(ResourceManager aResourceManager) throws ResourceConfigurationException {
    try {
      if (multipleEntries) {
        throw new InvalidXMLException(InvalidXMLException.DUPLICATE_ELEMENT_FOUND, 
                new Object[] { "<settings> or <imports>", "<externalOverrideSettings>"});
      }
 
      // Use local class that extends Java Properties to support multi-line arrays & maps
      // Also supports UTF-8 and lets first entry found override later entries.
      mProperties = new Settings_impl();
      
      // First see if an import specified on the command line
      String fname = System.getProperty("UimaExternalOverrides");
      if (fname != null) {
        mProperties.load(new FileInputStream(fname));
        UIMAFramework.getLogger(this.getClass()).logrb(Level.CONFIG, this.getClass().getName(),
                "resolveImports", LOG_RESOURCE_BUNDLE, "UIMA_external_overrides_loaded__CONFIG",
                new Object[] {"cmdline file", fname} );
      }
      
      // Load settings first to override any imports
      // Note: cannot use declaration order of settings vs. imports as the XmlizationInfo routine
      // puts settings first when (re-)writing the descriptor.
      if (getSettings() != null) {
        InputStream is = new ByteArrayInputStream(getSettings().getBytes("UTF-8"));
        mProperties.load(is);
        is.close();
        // External overrides loaded from {0} "{1}"
        UIMAFramework.getLogger(this.getClass()).logrb(Level.CONFIG, this.getClass().getName(), "resolveImports",
                LOG_RESOURCE_BUNDLE, "UIMA_external_overrides_loaded__CONFIG",
                new Object[] { "inline entry", getSettings() });
      }

      // Load imported files in declared order of declaration ... first overrides later ones
      Import[] imports = getImports();
      if (imports != null) {
        InputStream stream = null;
        for (int i = 0; i < imports.length; ++i) {
          try {
            ((Import_impl)imports[i]).setSuffix(".settings");    // Format is similar to java.util.Properties
            URL url = imports[i].findAbsoluteUrl(aResourceManager);
            stream = url.openStream();
            mProperties.load(stream);
            UIMAFramework.getLogger(this.getClass()).logrb(Level.CONFIG, this.getClass().getName(),
                    "resolveImports", LOG_RESOURCE_BUNDLE, "UIMA_external_overrides_loaded__CONFIG",
                    new Object[] {"file", url} );
          } catch (InvalidXMLException e) {
            throw new ResourceConfigurationException(e);
          } finally {
            try {
              if (stream != null) {
                stream.close();
              }
            } catch (IOException e1) {
              UIMAFramework.getLogger(this.getClass()).log(Level.SEVERE, "", e1);
            }
          }
        }
      }
      
    } catch (IOException e) {
      throw new ResourceConfigurationException(e);
    } catch (InvalidXMLException e) {
      throw new ResourceConfigurationException(e);
    }
  }
  
  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  // NOTE:  This will cause any re-write of a descriptor to order settings before imports
  //        which may not have been the original ordering !!
  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("externalOverrideSettings",
          new PropertyXmlInfo[] { new PropertyXmlInfo("settings", false),
              new PropertyXmlInfo("imports", false)});

}
