/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.uima.tools.bnd;

import static java.util.Collections.emptyList;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.resource.metadata.FsIndexCollection;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;
import org.apache.uima.util.XMLizable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.header.Attrs;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.AnalyzerPlugin;
import aQute.bnd.service.Plugin;
import aQute.lib.converter.Converter;
import aQute.service.reporter.Reporter;

@BndPlugin(name = "UIMA", parameters = UimaBndPlugin.Configuration.class)
public class UimaBndPlugin
    implements AnalyzerPlugin, Plugin
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final Pattern XML_FILE = Pattern.compile(".*\\.xml");
    private static final Pattern QN = Pattern
            .compile("[_A-Za-z$][_A-Za-z0-9$]*(\\.[_A-Za-z$][_A-Za-z0-9$]*)*");

    private final static XMLParser PARSER = UIMAFramework.getXMLParser();

    private Reporter reporter;
    private UimaBndPlugin.Configuration configuration;

    @Override
    public void setProperties(Map<String, String> aMap) throws Exception
    {
        configuration = Converter.cnv(UimaBndPlugin.Configuration.class, aMap);
    }

    @Override
    public void setReporter(Reporter aReporter)
    {
        reporter = aReporter;
    }

    @Override
    public boolean analyzeJar(Analyzer analyzer) throws Exception
    {
        var jar = analyzer.getJar();
        var resources = jar.getResources();

        var importsProcessed = 0;
        if (resources != null) {
            for (var entry : resources.entrySet()) {
                var path = entry.getKey();
                var resource = entry.getValue();

                try {
                    if (XML_FILE.matcher(path).matches()) {
                        importsProcessed += analyzeXmlFile(analyzer, path, resource, 0);
                    }
                }
                catch (Exception e) {
                    analyzer.error("Unexpected exception in processing resource (%s): %s", path, e);
                }
            }
        }

        LOG.info("UIMA bnd plugin processed {} imports", importsProcessed);

        return false;
    }

    private int analyzeXmlFile(Analyzer analyzer, String path, Resource resource, int level) throws Exception
    {
        var desc = readUimaDescriptor(resource);
        if (desc == null) {
            return 0;
        }

        LOG.debug("{}Found {}: {}", repeat("  ", level), desc.getClass().getSimpleName(), path);
        var imports = getImportsFromDescriptor(desc);

        var importsProcessed = 0;
        for (var imp : imports) {
            if (imp.getName() != null) {
                handleImportByName(analyzer, path, imp, level);
                importsProcessed++;
                continue;
            }

            if (imp.getLocation() != null) {
                handleImportByLocation(imp);
                continue;
            }

            LOG.warn(
                    "Found UIMA import without name and location - ignoring, please fix your type system description");
        }

        return importsProcessed;
    }

    private void handleImportByLocation(Import imp)
    {
        LOG.warn("Ignoring UIMA import by location (please only use import-by-name): {}",
                imp.getLocation());
    }

    private void handleImportByName(Analyzer analyzer, String path, Import imp, int level) throws Exception
    {
        var tsdPackage = imp.getName();
        int lastSeparatorPosition = tsdPackage.lastIndexOf('.');
        if (lastSeparatorPosition >= 0) {
            // Cut the name of the XML file and keep only the package
            tsdPackage = tsdPackage.substring(0, lastSeparatorPosition);
        }

        var pack = analyzer.getPackageRef(tsdPackage);
        if (!QN.matcher(pack.getFQN()).matches()) {
            analyzer.warning("Import does not seem to refer to a package (%s): %s", path, pack);
        }

        var alreadyKnownImport = analyzer.getReferred().containsKey(pack);
        if (!alreadyKnownImport) {
            var attrs = new Attrs();
            analyzer.getReferred().put(pack, attrs);
        }

        LOG.debug("{}Found UIMA import by name: {} {}", repeat("  ", level), tsdPackage, alreadyKnownImport ? "" : "(new)");
        
        var importedResourcePath = imp.getName().replace('.', '/') + ".xml";
        var importedResource = analyzer.findResource(importedResourcePath);
        if (importedResource == null) {
            analyzer.warning("Imported resource not found on classpath: {}", importedResourcePath);
            return;
        }

        if (configuration == null || configuration.transitive(false)) {
            analyzeXmlFile(analyzer, importedResourcePath, importedResource, level + 1);
        }
    }

    private List<Import> getImportsFromDescriptor(XMLizable desc)
    {
        if (desc instanceof TypeSystemDescription tsd) {
            return asList(tsd.getImports());
        }

        if (desc instanceof TypePriorities prio) {
            return asList(prio.getImports());
        }

        if (desc instanceof FsIndexCollection idxc) {
            return asList(idxc.getImports());
        }

        if (desc instanceof AnalysisEngineDescription aed) {
            var imports = new ArrayList<Import>();
            imports.addAll(
                    getImportsFromDescriptor(aed.getAnalysisEngineMetaData().getTypeSystem()));
            imports.addAll(
                    getImportsFromDescriptor(aed.getAnalysisEngineMetaData().getTypePriorities()));
            imports.addAll(getImportsFromDescriptor(
                    aed.getAnalysisEngineMetaData().getFsIndexCollection()));
            return imports;
        }

        return emptyList();
    }

    private XMLizable readUimaDescriptor(Resource resource) throws Exception
    {
        try (var in = resource.openInputStream()) {
            return PARSER.parse(new XMLInputSource(in));
        }
        catch (InvalidXMLException e) {
            // Probably not a type system description - ignore
        }

        return null;
    }

    private static <T> List<T> asList(T[] aList)
    {
        if (aList == null) {
            return emptyList();
        }

        return Arrays.asList(aList);
    }
    
    private static String repeat(String aString, int aCount) {
      if (aCount == 0) {
        return "";
      }
      
      var buf = new StringBuilder();
      for (var i = 0; i < aCount; i++) {
        buf.append(aString);
      }

      return buf.toString();
    }

    public static interface Configuration
    {
        boolean transitive(boolean aDefault);
    }
}