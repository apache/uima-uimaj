/*
 Copyright 2011
 Ubiquitous Knowledge Processing (UKP) Lab
 Technische Universitaet Darmstadt
 All rights reserved.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package org.uimafit.factory;

import static java.util.Arrays.asList;
import static org.apache.uima.UIMAFramework.getXMLParser;
import static org.uimafit.util.ReflectionUtil.getInheritableAnnotation;
import static org.uimafit.factory.TypeSystemDescriptionFactory.resolve;
import static org.uimafit.factory.TypeSystemDescriptionFactory.scanImportsAndManifests;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.LogFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.FsIndexCollection;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.FsIndexKeyDescription;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.FsIndexCollection_impl;
import org.apache.uima.resource.metadata.impl.FsIndexDescription_impl;
import org.apache.uima.resource.metadata.impl.FsIndexKeyDescription_impl;
import org.apache.uima.resource.metadata.impl.Import_impl;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.uimafit.descriptor.FsIndex;
import org.uimafit.descriptor.FsIndexKey;

/**
 * @author Richard Eckart de Castilho
 */
public final class FsIndexFactory {
	  public static final int STANDARD_COMPARE = FsIndexKeyDescription.STANDARD_COMPARE;

	  public static final int REVERSE_STANDARD_COMPARE = FsIndexKeyDescription.STANDARD_COMPARE;

	private FsIndexFactory() {
		// Factory class
	}  
	  
	/**
	 * Create index configuration data for a given class definition using reflection and the
	 * configuration parameter annotation.
	 */
	public static FsIndexCollection createFsIndexCollection(Class<?> componentClass) {
		List<FsIndex> anFsIndexList = new ArrayList<FsIndex>();

		// Check FsIndexCollection annotation
		org.uimafit.descriptor.FsIndexCollection anIndexCollection =
			getInheritableAnnotation(org.uimafit.descriptor.FsIndexCollection.class, componentClass);
		if (anIndexCollection != null) {
			anFsIndexList.addAll(asList(anIndexCollection.fsIndexes()));
		}

		// Check FsIndex annotation
		org.uimafit.descriptor.FsIndex anFsIndex =
			getInheritableAnnotation(FsIndex.class, componentClass);
		if (anFsIndex != null) {
			if (anIndexCollection != null) {
				throw new IllegalStateException("Class ["+componentClass.getName()+"] must not " +
						"declare "+org.uimafit.descriptor.FsIndexCollection.class.getSimpleName() +
						" and "+FsIndex.class.getSimpleName()+" at the same time.");
			}

			anFsIndexList.add(anFsIndex);
		}

		FsIndexCollection_impl fsIndexCollection = new FsIndexCollection_impl();

		// Process collected FsIndex annotations
		for (FsIndex anIdx : anFsIndexList) {
			// Collect index keys
			List<FsIndexKeyDescription> keys = new ArrayList<FsIndexKeyDescription>();
			for (FsIndexKey anIndexKey : anIdx.keys()) {
				keys.add(createFsIndexKeyDescription(anIndexKey.featureName(),
						anIndexKey.comparator()));
			}

			// type and typeName must not be set at the same time
			if (!anIdx.typeName().equals(FsIndex.NO_NAME_TYPE_SET)
					&& anIdx.type() != FsIndex.NoClassSet.class){
				throw new IllegalStateException("Class ["+componentClass.getName()+"] must not " +
						"declare an "+org.uimafit.descriptor.FsIndex.class.getSimpleName() +
						" with type and typeName both set at the same time.");
			}

			String typeName;
			if (!anIdx.typeName().equals(FsIndex.NO_NAME_TYPE_SET)) {
				typeName = anIdx.typeName();
			}
			else if (anIdx.type() != FsIndex.NoClassSet.class) {
				typeName = anIdx.type().getName();
			}
			else {
				throw new IllegalStateException("Class ["+componentClass.getName()+"] must not " +
						"declare an "+org.uimafit.descriptor.FsIndex.class.getSimpleName() +
						" with neither type nor typeName set.");
			}

			fsIndexCollection.addFsIndex(createFsIndexDescription(anIdx.label(), anIdx.kind(),
					typeName, anIdx.typePriorities(),
					keys.toArray(new FsIndexKeyDescription[keys.size()])));
		}

		return fsIndexCollection;
	}

	public static FsIndexDescription createFsIndexDescription(String label, String kind,
			String typeName, boolean useTypePriorities, FsIndexKeyDescription... keys)	{
		FsIndexDescription_impl fsIndexDescription = new FsIndexDescription_impl();
		fsIndexDescription.setLabel(label);
		fsIndexDescription.setKind(kind);
		fsIndexDescription.setTypeName(typeName);
		fsIndexDescription.setKeys(keys);
		return fsIndexDescription;
	}

	public static FsIndexCollection createFsIndexCollection(FsIndexDescription...descriptions) {
		FsIndexCollection_impl fsIndexCollection = new FsIndexCollection_impl();
		fsIndexCollection.setFsIndexes(descriptions);
		return fsIndexCollection;
	}

	public static FsIndexKeyDescription createFsIndexKeyDescription(String featureName) {
		return createFsIndexKeyDescription(featureName, STANDARD_COMPARE);
	}

	public static FsIndexKeyDescription createFsIndexKeyDescription(String featureName,
			int comparator) {
		FsIndexKeyDescription_impl key = new FsIndexKeyDescription_impl();
		key.setFeatureName(featureName);
		key.setComparator(comparator);
		key.setTypePriority(false);
		return key;
	}

	/**
	 * System property indicating which locations to scan for index descriptions. A list of
	 * locations may be given separated by ";".
	 */
	public static final String FS_INDEX_IMPORT_PATTERN = "org.uimafit.fsindex.import_pattern";

	/**
	 * Index manifest location.
	 */
	public static final String FS_INDEX_MANIFEST_PATTERN = "classpath*:META-INF/org.uimafit/fsindexes.txt";

	private static String[] indexDescriptorLocations;

	/**
	 * Creates a {@link FsIndexCollection} from descriptor names.
	 *
	 * @param descriptorNames
	 *            The fully qualified, Java-style, dotted descriptor names.
	 * @return a {@link FsIndexCollection} that includes the indexes from all of the specified files.
	 */
	public static FsIndexCollection createFsIndexCollection(String... descriptorNames) {
		List<Import> imports = new ArrayList<Import>();
		for (String descriptorName : descriptorNames) {
			Import imp = new Import_impl();
			imp.setName(descriptorName);
			imports.add(imp);
		}
		Import[] importArray = new Import[imports.size()];

		FsIndexCollection fsIndexCollection = new FsIndexCollection_impl();
		fsIndexCollection.setImports(imports.toArray(importArray));
		return fsIndexCollection;
	}

	/**
	 * Creates a {@link FsIndexCollection} from a descriptor file
	 *
	 * @param descriptorURIs
	 *            The descriptor file paths.
	 * @return A {@link FsIndexCollection} that includes the indexes from all of the specified files.
	 */
	public static FsIndexCollection createTypeSystemDescriptionFromPath(
			String... descriptorURIs) {
		List<Import> imports = new ArrayList<Import>();
		for (String descriptorURI : descriptorURIs) {
			Import imp = new Import_impl();
			imp.setLocation(descriptorURI);
			imports.add(imp);
		}
		Import[] importArray = new Import[imports.size()];

		FsIndexCollection fsIndexCollection = new FsIndexCollection_impl();
		fsIndexCollection.setImports(imports.toArray(importArray));
		return fsIndexCollection;
	}

	/**
	 * Creates a {@link FsIndexCollection} from all index descriptions that can be found via the
	 * {@link #FS_INDEX_IMPORT_PATTERN} or via the {@code META-INF/org.uimafit/fsindexes.txt} files
	 * in the classpath.
	 *
	 * @return the auto-scanned indexes.
	 */
	public static FsIndexCollection createFsIndexCollection()
			throws ResourceInitializationException {
		List<FsIndexDescription> fsIndexList = new ArrayList<FsIndexDescription>();
		for (String location : scanIndexDescriptors()) {
			try {
				XMLInputSource xmlInput = new XMLInputSource(location);
				FsIndexCollection fsIdxCol = getXMLParser().parseFsIndexCollection(xmlInput);
				fsIdxCol.resolveImports();
				fsIndexList.addAll(asList(fsIdxCol.getFsIndexes()));
				LogFactory.getLog(FsIndexFactory.class).debug(
						"Detected index at [" + location + "]");
			}
			catch (IOException e) {
				throw new ResourceInitializationException(e);
			}
			catch (InvalidXMLException e) {
				LogFactory.getLog(TypeSystemDescription.class).warn(
						"[" + location + "] is not a index descriptor file. Ignoring.", e);
			}
		}

		return createFsIndexCollection(fsIndexList.toArray(new FsIndexDescription[fsIndexList
				.size()]));
	}

	/**
	 * Get all currently accessible index descriptor locations. A scan is actually only
	 * performed on the first call and the locations are cached. To force a re-scan use
	 * {@link #forceIndexDescriptorsScan()}.
	 *
	 * @return an array of locations.
	 * @throws ResourceInitializationException
	 *             if the locations could not be resolved.
	 */
	public static String[] scanIndexDescriptors() throws ResourceInitializationException {
		if (indexDescriptorLocations == null) {
			indexDescriptorLocations = resolve(scanImportsAndManifests(FS_INDEX_MANIFEST_PATTERN,
					FS_INDEX_IMPORT_PATTERN));
		}
		return indexDescriptorLocations;
	}

	/**
	 * Force rescan of type descriptors. The next call to {@link #scanIndexDescriptors()} will rescan
	 * all auto-import locations.
	 */
	public static void forceIndexDescriptorsScan() {
		indexDescriptorLocations = null;
	}
}
