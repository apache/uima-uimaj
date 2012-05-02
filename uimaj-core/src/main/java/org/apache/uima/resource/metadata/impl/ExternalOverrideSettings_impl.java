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
  
  private ExternalOverrideSettings parentOverrides = null;

  /*
   * Is true if settings or imports have been declared more than once
   */
  private boolean multipleEntries = false;


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
 
      Settings_impl parentSettings = null;
      if (parentOverrides != null) {
        parentSettings = ((ExternalOverrideSettings_impl)parentOverrides).mProperties;
      }
      // Use local class that extends Java Properties to support multi-line arrays & maps
      // Also supports UTF-8 and lets first entry found override later entries.
      mProperties = new Settings_impl(parentSettings);
      
      // If top-level see if a comma-separated list of imports has been specified on the command line
      if (parentOverrides == null) {
        String fnames = System.getProperty("UimaExternalOverrides");
        if (fnames != null) {
          for (String fname : fnames.split(",")) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.CONFIG, this.getClass().getName(), "resolveImports",
                    LOG_RESOURCE_BUNDLE, "UIMA_external_overrides_load__CONFIG",
                    new Object[] { "cmdline file", fname });
            mProperties.load(new FileInputStream(fname));
          }
        }
      }
      
      // Load settings first to override any imports
      // Note: cannot use declaration order of settings vs. imports as the XmlizationInfo routine
      // puts settings first when (re-)writing the descriptor.
      if (getSettings() != null) {
        InputStream is = new ByteArrayInputStream(getSettings().getBytes("UTF-8"));
        // External overrides loaded from {0} "{1}"
        UIMAFramework.getLogger(this.getClass()).logrb(Level.CONFIG, this.getClass().getName(), "resolveImports",
                LOG_RESOURCE_BUNDLE, "UIMA_external_overrides_load__CONFIG",
                new Object[] { "inline entry", getSettings() });
        mProperties.load(is);
        is.close();
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
            UIMAFramework.getLogger(this.getClass()).logrb(Level.CONFIG, this.getClass().getName(),
                    "resolveImports", LOG_RESOURCE_BUNDLE, "UIMA_external_overrides_load__CONFIG",
                    new Object[] {"file", url} );
            mProperties.load(stream);
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
  
  public void setParentOverrides(ExternalOverrideSettings parent) {
    parentOverrides = parent;
  }
  
  /*
   * Return the object loaded with all the settings declared in the descriptor.
   * Not to be confused with getSettings which refers to the inline <settings> element in the xml
   */
/*  protected Settings_impl getSettings_impl() {
    return mProperties;
  }*/
  
  public String resolveExternalName(String name) throws ResourceConfigurationException {
    return mProperties == null ? null : mProperties.lookUp(name);
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
