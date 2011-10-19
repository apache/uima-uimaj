package org.apache.uima.caseditor.editor.searchStrategy;

import java.util.Map;
import java.util.TreeMap;

import org.apache.uima.caseditor.CasEditorPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

public class TypeSystemSearchStrategyFactory {

  private static final String SEARCH_STRATEGY_EXTENSION = "org.apache.uima.caseditor.searchstrategy";

  private static TypeSystemSearchStrategyFactory instance;

  private Map<Integer, ITypeSystemSearchStrategy> searchStrategies = new TreeMap<Integer, ITypeSystemSearchStrategy>();

  private TypeSystemSearchStrategyFactory() {

    IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(
            SEARCH_STRATEGY_EXTENSION);

    for (IConfigurationElement element : config) {

      if ("searchStrategy".equals(element.getName())) {

        // extract id element
        String id = element.getAttribute("id");
        String priority = element.getAttribute("priority");
        String description = element.getAttribute("description");

        Object searchStrategyObject;
        try {
          searchStrategyObject = element.createExecutableExtension("class");
        } catch (CoreException e) {
          CasEditorPlugin.log("Failed to load search strategy with id: " + id, e);
          searchStrategyObject = null;
        }

        if (searchStrategyObject instanceof ITypeSystemSearchStrategy) {
          searchStrategies.put(Integer.parseInt(priority),
                  (ITypeSystemSearchStrategy) searchStrategyObject);
        }
      }
    }

  }

  public static TypeSystemSearchStrategyFactory instance() {

    if (instance == null) {
      instance = new TypeSystemSearchStrategyFactory();
    }

    return instance;
  }

  public Map<Integer, ITypeSystemSearchStrategy> getSearchStrategies() {
    return searchStrategies;
  }

}
