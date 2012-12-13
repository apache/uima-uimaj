/*
 Copyright 2009-2010
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

 getCoveredAnnotations() contains code adapted from the UIMA Subiterator class.
 */
package org.apache.uima.fit.util;

import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.Subiterator;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * Utility methods for convenient access to the {@link CAS}.
 *
 * @author Richard Eckart de Castilho
 * @author Niklas Jakob
 */
public class CasUtil {
	/**
	 * Package name of JCas wrapper classes built into UIMA.
	 */
	public static final String UIMA_BUILTIN_JCAS_PREFIX = "org.apache.uima.jcas.";

	/**
	 * Convenience method to iterator over all feature structures of a given type.
	 *
	 * @param cas
	 *            a CAS.
	 * @param type
	 *            the type.
	 * @return An iterable.
	 * @see AnnotationIndex#iterator()
	 * @deprecated use {@link #selectFS}
	 */
	@Deprecated
	public static Iterable<FeatureStructure> iterateFS(final CAS cas, final Type type) {
		return selectFS(cas, type);
	}

	/**
	 * Convenience method to iterator over all annotations of a given type.
	 *
	 * @param cas
	 *            a CAS.
	 * @param type
	 *            the type.
	 * @return An iterable.
	 * @see AnnotationIndex#iterator()
	 * @deprecated use {@link #select}
	 */
	@Deprecated
	public static Iterable<AnnotationFS> iterate(final CAS cas, final Type type) {
		return select(cas, type);
	}

	/**
	 * Get an iterator over the given feature structures type.
	 *
	 * @param <T>
	 *            the JCas type.
	 * @param cas
	 *            a CAS.
	 * @param type
	 *            a type.
	 * @return a return value.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends FeatureStructure> Iterator<T> iteratorFS(CAS cas, Type type) {
		return ((FSIterator<T>) cas.getIndexRepository().getAllIndexedFS(type));
	}

	/**
	 * Get an iterator over the given annotation type.
	 *
	 * @param <T>
	 *            the JCas type.
	 * @param cas
	 *            a CAS.
	 * @param type
	 *            a type.
	 * @return a return value.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends AnnotationFS> Iterator<T> iterator(CAS cas, Type type) {
		return ((AnnotationIndex<T>) cas.getAnnotationIndex(type)).iterator();
	}

	/**
	 * Get the CAS type for the given JCas wrapper class.
	 *
	 * @param cas
	 *            the CAS hosting the type system.
	 * @param type
	 *            the JCas wrapper class.
	 * @return the CAS type.
	 */
	public static Type getType(CAS cas, Class<?> type) {
		return getType(cas, type.getName());
	}

	/**
	 * Get the CAS type for the given name.
	 *
	 * @param aCas
	 *            the CAS hosting the type system.
	 * @param aTypename
	 *            the fully qualified type name.
	 * @return the CAS type.
	 */
	public static Type getType(CAS aCas, String aTypename) {
		String typeName = aTypename;
		if (typeName.startsWith(UIMA_BUILTIN_JCAS_PREFIX)) {
			typeName = "uima." + typeName.substring(UIMA_BUILTIN_JCAS_PREFIX.length());
		}
		final Type type = aCas.getTypeSystem().getType(typeName);
		if (type == null) {
			throw new IllegalArgumentException("Undeclared type [" + aTypename + "]");
		}
		return type;
	}

	/**
	 * Get the CAS type for the given JCas wrapper class making sure it is or inherits from
	 * {@link Annotation}.
	 *
	 * @param aCas
	 *            the CAS hosting the type system.
	 * @param aJCasClass
	 *            the JCas wrapper class.
	 * @return the CAS type.
	 */
	public static Type getAnnotationType(CAS aCas, Class<?> aJCasClass) {
		final Type type = getType(aCas, aJCasClass);
		if (!aCas.getTypeSystem().subsumes(aCas.getAnnotationType(), type)) {
			throw new IllegalArgumentException("Type [" + aJCasClass.getName()
					+ "] is not an annotation type");
		}
		return type;
	}

	/**
	 * Get the CAS type for the given name making sure it is or inherits from Annotation.
	 *
	 * @param aCas
	 *            the CAS hosting the type system.
	 * @param aTypeName
	 *            the fully qualified type name.
	 * @return the CAS type.
	 */
	public static Type getAnnotationType(CAS aCas, String aTypeName) {
		Type type = getType(aCas, aTypeName);
		if (!aCas.getTypeSystem().subsumes(aCas.getAnnotationType(), type)) {
			throw new IllegalArgumentException("Type [" + aTypeName + "] is not an annotation type");
		}
		return type;
	}

	/**
	 * Convenience method to iterator over all feature structures of a given type.
	 *
	 * @param array
	 *            features structure array.
	 * @param type
	 *            the type.
	 * @return A collection of the selected type.
	 */
	public static Collection<FeatureStructure> selectFS(ArrayFS array, Type type) {
		return FSCollectionFactory.create(array, type);
	}

	/**
	 * Convenience method to iterator over all annotations of a given type.
	 *
	 * @param array
	 *            features structure array.
	 * @param type
	 *            the type.
	 * @return A collection of the selected type.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Collection<AnnotationFS> select(ArrayFS array, Type type) {
		final CAS cas = array.getCAS();
		if (!cas.getTypeSystem().subsumes(cas.getAnnotationType(), type)) {
			throw new IllegalArgumentException("Type [" + type.getName()
					+ "] is not an annotation type");
		}
		return (Collection) FSCollectionFactory.create(cas, type);
	}

	/**
	 * Convenience method to iterator over all features structures.
	 *
	 * @param aCas
	 *            the CAS hosting the type system.
	 * @return A collection of the selected type.
	 */
	public static Collection<FeatureStructure> selectAllFS(final CAS aCas) {
		return selectFS(aCas, getType(aCas, CAS.TYPE_NAME_TOP));
	}

	/**
	 * Convenience method to iterator over all feature structures of a given type.
	 *
	 * @param cas
	 *            the CAS containing the type system.
	 * @param type
	 *            the type.
	 * @return A collection of the selected type.
	 */
	public static Collection<FeatureStructure> selectFS(final CAS cas, final Type type) {
		return FSCollectionFactory.create(cas, type);
	}

	/**
	 * Convenience method to iterator over all annotations.
	 *
	 * @param aCas
	 *            the CAS hosting the type system.
	 * @return A collection of the selected type.
	 */
	public static Collection<AnnotationFS> selectAll(final CAS aCas) {
		return select(aCas, getType(aCas, CAS.TYPE_NAME_ANNOTATION));
	}

	/**
	 * Convenience method to iterator over all annotations of a given type.
	 *
	 * @param cas
	 *            the CAS containing the type system.
	 * @param type
	 *            the type.
	 * @return A collection of the selected type.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Collection<AnnotationFS> select(final CAS cas, final Type type) {
		if (!cas.getTypeSystem().subsumes(cas.getAnnotationType(), type)) {
			throw new IllegalArgumentException("Type [" + type.getName()
					+ "] is not an annotation type");
		}
		return (Collection) FSCollectionFactory.create(cas, type);
	}

	/**
	 * Get a list of annotations of the given annotation type located between two annotations.
	 * Does not use subiterators and does not respect type priorities. Zero-width annotations
	 * what lie on the borders are included in the result, e.g. if the boundary annotations are
	 * [1..2] and [2..3] then an annotation [2..2] is returned. If there is a non-zero overlap
	 * between the boundary annotations, the result is empty. The method properly handles cases
	 * where the second boundary annotations occurs before the first boundary annotation by
	 * switching their roles.
	 *
	 * @param type
	 *            a UIMA type.
	 * @param ann1
	 *            the first boundary annotation.
	 * @param ann2
	 *            the second boundary annotation.
	 * @return a return value.
	 * @see Subiterator
	 */
	public static List<AnnotationFS> selectBetween(
			final Type type, final AnnotationFS ann1, final AnnotationFS ann2) {
		return selectBetween(ann1.getView(), type, ann1, ann2);
	}

	/**
	 * Get a list of annotations of the given annotation type located between two annotations.
	 * Does not use subiterators and does not respect type priorities. Zero-width annotations
	 * what lie on the borders are included in the result, e.g. if the boundary annotations are
	 * [1..2] and [2..3] then an annotation [2..2] is returned. If there is a non-zero overlap
	 * between the boundary annotations, the result is empty. The method properly handles cases
	 * where the second boundary annotations occurs before the first boundary annotation by
	 * switching their roles.
	 *
	 * @param cas
	 *            a CAS.
	 * @param type
	 *            a UIMA type.
	 * @param ann1
	 *            the first boundary annotation.
	 * @param ann2
	 *            the second boundary annotation.
	 * @return a return value.
	 * @see Subiterator
	 */
	public static List<AnnotationFS> selectBetween(final CAS cas, final Type type, 
			final AnnotationFS ann1, final AnnotationFS ann2) {
		AnnotationFS left;
		AnnotationFS right;
		if (ann1.getEnd() > ann2.getBegin()) {
			left = ann2;
			right = ann1;
		}
		else {
			left = ann1;
			right = ann2;
		}
		
		int begin = left.getEnd();
		int end = right.getBegin();

		List<AnnotationFS> list = new ArrayList<AnnotationFS>();
		FSIterator<AnnotationFS> it = cas.getAnnotationIndex(type).iterator();

		// Try to seek the insertion point.
		it.moveTo(left);

		// If the insertion point is beyond the index, move back to the last.
		if (!it.isValid()) {
			it.moveToLast();
			if (!it.isValid()) {
				return list;
			}
		}

		// Ignore type priorities by seeking to the first that has the same begin
		boolean moved = false;
		while (it.isValid() && (it.get()).getBegin() >= begin) {
			it.moveToPrevious();
			moved = true;
		}

		// If we moved, then we are now on one starting before the requested begin, so we have to
		// move one ahead.
		if (moved) {
			it.moveToNext();
		}

		// If we managed to move outside the index, start at first.
		if (!it.isValid()) {
			it.moveToFirst();
		}

		// Skip annotations whose start is before the start parameter.
		while (it.isValid() && (it.get()).getBegin() < begin) {
			it.moveToNext();
		}

		boolean strict = true;
		while (it.isValid()) {
			AnnotationFS a = it.get();
			// If the start of the current annotation is past the end parameter, we're done.
			if (a.getBegin() > end) {
				break;
			}
			it.moveToNext();
			if (strict && a.getEnd() > end) {
				continue;
			}

			assert (a.getBegin() >= left.getEnd()) : "Illegal begin "
					+ a.getBegin() + " in [" + begin + ".." + end + "]";

			assert (a.getEnd() <= right.getBegin()) : "Illegal end "
					+ a.getBegin() + " in [" + begin + ".." + end + "]";


			if (!a.equals(left) && !a.equals(right)) {
				list.add(a);
			}
		}

		return unmodifiableList(list);	}
	
	/**
	 * Get a list of annotations of the given annotation type constraint by a certain annotation.
	 * Iterates over all annotations of the given type to find the covered annotations. Does not use
	 * subiterators and does not respect type prioritites. Was adapted from {@link Subiterator}.
	 * Uses the same approach except that type priorities are ignored.
	 *
	 * @param type
	 *            a UIMA type.
	 * @param coveringAnnotation
	 *            the covering annotation.
	 * @return a return value.
	 * @see Subiterator
	 */
	public static List<AnnotationFS> selectCovered(Type type, AnnotationFS coveringAnnotation) {
		return selectCovered(coveringAnnotation.getView(), type, coveringAnnotation);
	}

	/**
	 * Get a list of annotations of the given annotation type constraint by a certain annotation.
	 * Iterates over all annotations of the given type to find the covered annotations. Does not use
	 * subiterators and does not respect type prioritites. Was adapted from {@link Subiterator}.
	 * Uses the same approach except that type priorities are ignored.
	 *
	 * @param cas
	 *            a CAS.
	 * @param type
	 *            a UIMA type.
	 * @param coveringAnnotation
	 *            the covering annotation.
	 * @return a return value.
	 * @see Subiterator
	 */
	public static List<AnnotationFS> selectCovered(CAS cas, Type type,
			AnnotationFS coveringAnnotation) {
		int begin = coveringAnnotation.getBegin();
		int end = coveringAnnotation.getEnd();

		List<AnnotationFS> list = new ArrayList<AnnotationFS>();
		FSIterator<AnnotationFS> it = cas.getAnnotationIndex(type).iterator();

		// Try to seek the insertion point.
		it.moveTo(coveringAnnotation);

		// If the insertion point is beyond the index, move back to the last.
		if (!it.isValid()) {
			it.moveToLast();
			if (!it.isValid()) {
				return list;
			}
		}

		// Ignore type priorities by seeking to the first that has the same begin
		boolean moved = false;
		while (it.isValid() && (it.get()).getBegin() >= begin) {
			it.moveToPrevious();
			moved = true;
		}

		// If we moved, then we are now on one starting before the requested begin, so we have to
		// move one ahead.
		if (moved) {
			it.moveToNext();
		}

		// If we managed to move outside the index, start at first.
		if (!it.isValid()) {
			it.moveToFirst();
		}

		// Skip annotations whose start is before the start parameter.
		while (it.isValid() && (it.get()).getBegin() < begin) {
			it.moveToNext();
		}

		boolean strict = true;
		while (it.isValid()) {
			AnnotationFS a = it.get();
			// If the start of the current annotation is past the end parameter, we're done.
			if (a.getBegin() > end) {
				break;
			}
			it.moveToNext();
			if (strict && a.getEnd() > end) {
				continue;
			}

			assert (a.getBegin() >= coveringAnnotation.getBegin()) : "Illegal begin "
					+ a.getBegin() + " in [" + coveringAnnotation.getBegin() + ".."
					+ coveringAnnotation.getEnd() + "]";

			assert (a.getEnd() <= coveringAnnotation.getEnd()) : "Illegal end " + a.getEnd()
					+ " in [" + coveringAnnotation.getBegin() + ".." + coveringAnnotation.getEnd()
					+ "]";

			if (!a.equals(coveringAnnotation)) {
				list.add(a);
			}
		}

		return unmodifiableList(list);
	}

	/**
	 * Get a list of annotations of the given annotation type constraint by a certain annotation.
	 * Iterates over all annotations of the given type to find the covered annotations. Does not use
	 * subiterators and does not respect type prioritites. Was adapted from {@link Subiterator}.
	 * Uses the same approach except that type priorities are ignored.
	 * <p>
	 * <b>Note:</b> this is significantly slower than using
	 * {@link #selectCovered(CAS, Type, AnnotationFS)}. It is possible to use {@code 
	 * selectCovered(cas, type, new Annotation(jCas, int, int))}, but that will allocate memory
	 * in the jCas for the new annotation. If you do that repeatedly many times, memory may fill up. 
	 *
	 * @param cas
	 *            a CAS.
	 * @param type
	 *            a UIMA type.
	 * @param begin
	 *            begin offset.
	 * @param end
	 *            end offset.
	 * @return a return value.
	 * @see Subiterator
	 */
	public static List<AnnotationFS> selectCovered(CAS cas, Type type, int begin, int end) {

		List<AnnotationFS> list = new ArrayList<AnnotationFS>();
		FSIterator<AnnotationFS> it = cas.getAnnotationIndex(type).iterator();

		// Skip annotations whose start is before the start parameter.
		while (it.isValid() && (it.get()).getBegin() < begin) {
			it.moveToNext();
		}

		boolean strict = true;
		while (it.isValid()) {
			AnnotationFS a = it.get();
			// If the start of the current annotation is past the end parameter, we're done.
			if (a.getBegin() > end) {
				break;
			}
			it.moveToNext();
			if (strict && a.getEnd() > end) {
				continue;
			}

			assert (a.getBegin() >= begin) : "Illegal begin " + a.getBegin() + " in [" + begin
					+ ".." + end + "]";

			assert (a.getEnd() <= end) : "Illegal end " + a.getEnd() + " in [" + begin + ".."
					+ end + "]";

			list.add(a);
		}

		return list;
	}

	/**
	 * Get a list of annotations of the given annotation type constraint by a certain annotation.
	 * Iterates over all annotations to find the covering annotations.
	 *
	 * <p>
	 * <b>Note:</b> this is <b>REALLY SLOW!</b> You don't want to use this. Instead, consider
	 * using {@link #indexCovering(CAS, Type, Type)} or a {@link ContainmentIndex}.
	 *
	 * @param cas
	 *            a CAS.
	 * @param type
	 *            a UIMA type.
	 * @param begin
	 *            begin offset.
	 * @param end
	 *            end offset.
	 * @return a return value.
	 */
	public static List<AnnotationFS> selectCovering(CAS cas, Type type, int begin, int end) {

		TypeSystem ts = cas.getTypeSystem();
		List<AnnotationFS> list = new ArrayList<AnnotationFS>();
		FSIterator<AnnotationFS> iter = cas.getAnnotationIndex().iterator();
		while (iter.hasNext()) {
			AnnotationFS a = iter.next();
			if ((a.getBegin() <= begin) && (a.getEnd() >= end)
					&& ((type == null) || (ts.subsumes(type, a.getType())))) {
				list.add(a);
			}
		}
		return list;
	}

	/**
	 * Create an index for quickly lookup up the annotations covering a particular annotation. This
	 * is preferable to using {@link #selectCovering(CAS, Type, int, int)} because the overhead of
	 * scanning the CAS occurs only when the index is build. Subsequent lookups to the index are
	 * fast.
	 *
	 * @param cas
	 *            a CAS.
	 * @param type
	 *            type to create the index for - this is used in lookups.
	 * @param coveringType
	 *            type of covering annotations.
	 * @return the index.
	 */
	public static Map<AnnotationFS, Collection<AnnotationFS>> indexCovering(CAS cas, Type type,
			Type coveringType) {
		Map<AnnotationFS, Collection<AnnotationFS>> index = new HashMap<AnnotationFS, Collection<AnnotationFS>>() {
			private static final long serialVersionUID = 1L;

			@Override
			public Collection<AnnotationFS> get(Object paramObject) {
				Collection<AnnotationFS> res = super.get(paramObject);
				if (res == null) {
					return emptyList();
				}
				else {
					return res;
				}
			}
		};
		for (AnnotationFS s : select(cas, coveringType)) {
			for (AnnotationFS u : selectCovered(cas, type, s)) {
				Collection<AnnotationFS> c = index.get(u);
				if (c == EMPTY_LIST) {
					c = new LinkedList<AnnotationFS>();
					index.put(u, c);
				}
				c.add(s);
			}
		}
		return unmodifiableMap(index);
	}

	/**
	 * Create an index for quickly lookup up the annotations covered by a particular annotation. This
	 * is preferable to using {@link #selectCovered(CAS, Type, int, int)} because the overhead of
	 * scanning the CAS occurs only when the index is build. Subsequent lookups to the index are
	 * fast.
	 *
	 * @param cas
	 *            a CAS.
	 * @param type
	 *            type to create the index for - this is used in lookups.
	 * @param coveredType
	 *            type of covering annotations.
	 * @return the index.
	 */
	public static Map<AnnotationFS, Collection<AnnotationFS>> indexCovered(CAS cas, Type type,
			Type coveredType) {
		Map<AnnotationFS, Collection<AnnotationFS>> index = new HashMap<AnnotationFS, Collection<AnnotationFS>>() {
			private static final long serialVersionUID = 1L;

			@Override
			public Collection<AnnotationFS> get(Object paramObject) {
				Collection<AnnotationFS> res = super.get(paramObject);
				if (res == null) {
					return emptyList();
				}
				else {
					return res;
				}
			}
		};
		for (AnnotationFS s : select(cas, type)) {
			for (AnnotationFS u : selectCovered(cas, coveredType, s)) {
				Collection<AnnotationFS> c = index.get(s);
				if (c == EMPTY_LIST) {
					c = new LinkedList<AnnotationFS>();
					index.put(s, c);
				}
				c.add(u);
			}
		}
		return unmodifiableMap(index);
	}
	/**
	 * This method exists simply as a convenience method for unit testing. It is not very efficient
	 * and should not, in general be used outside the context of unit testing.
	 *
	 * @param cas
	 *            a CAS containing the feature structure.
	 * @param type
	 *            a UIMA type.
	 * @param index
	 *            this can be either positive (0 corresponds to the first annotation of a type) or
	 *            negative (-1 corresponds to the last annotation of a type.)
	 * @return an annotation of the given type
	 */
	public static FeatureStructure selectFSByIndex(CAS cas, Type type, int index) {
		FSIterator<FeatureStructure> i = cas.getIndexRepository().getAllIndexedFS(type);
		int n = index;
		i.moveToFirst();
		if (n > 0) {
			while (n > 0 && i.isValid()) {
				i.moveToNext();
				n--;
			}
		}
		if (n < 0) {
			i.moveToLast();
			while (n < -1 && i.isValid()) {
				i.moveToPrevious();
				n++;
			}
		}

		return i.isValid() ? i.get() : null;
	}

	/**
	 * This method exists simply as a convenience method for unit testing. It is not very efficient
	 * and should not, in general be used outside the context of unit testing.
	 *
	 * @param cas
	 *            a CAS containing the annotation.
	 * @param type
	 *            a UIMA type.
	 * @param index
	 *            this can be either positive (0 corresponds to the first annotation of a type) or
	 *            negative (-1 corresponds to the last annotation of a type.)
	 * @return an annotation of the given type
	 */
	public static AnnotationFS selectByIndex(CAS cas, Type type, int index) {
		if (!cas.getTypeSystem().subsumes(cas.getAnnotationType(), type)) {
			throw new IllegalArgumentException("Type [" + type.getName()
					+ "] is not an annotation type");
		}
		FSIterator<AnnotationFS> i = cas.getAnnotationIndex(type).iterator();
		int n = index;
		i.moveToFirst();
		if (n > 0) {
			while (n > 0 && i.isValid()) {
				i.moveToNext();
				n--;
			}
		}
		if (n < 0) {
			i.moveToLast();
			while (n < -1 && i.isValid()) {
				i.moveToPrevious();
				n++;
			}
		}

		return i.isValid() ? i.get() : null;
	}

	/**
	 * Get the single instance of the specified type from the JCas.
	 *
	 * @param cas
	 *            a CAS containing the annotation.
	 * @param type
	 *            a UIMA type.
	 * @return the single instance of the given type. throws IllegalArgumentException if not exactly
	 *         one instance if the given type is present.
	 */
	public static FeatureStructure selectSingle(CAS cas, Type type) {
		FSIterator<FeatureStructure> iterator = cas.getIndexRepository().getAllIndexedFS(type);

		if (!iterator.hasNext()) {
			throw new IllegalArgumentException("CAS does not contain any [" + type.getName() + "]");
		}

		FeatureStructure result = iterator.next();

		if (iterator.hasNext()) {
			throw new IllegalArgumentException("CAS contains more than one [" + type.getName()
					+ "]");
		}

		return result;
	}

	/**
	 * Return an annotation preceding or following of a given reference annotation.
	 * 
	 * @param type
	 *            a type.
	 * @param annotation
	 *            anchor annotation
	 * @param index
	 *            relative position to access. A negative value selects a preceding annotation
	 *            while a positive number selects a following annotation.
	 * @return the addressed annotation.
	 */
	public static AnnotationFS selectSingleRelative(Type type,
			AnnotationFS annotation, int index) {
		return selectSingleRelative(annotation.getView(), type, annotation, index);
	}
	
	/**
	 * Return an annotation preceding or following of a given reference annotation.
	 * 
	 * @param cas
	 *            a CAS containing the annotation.
	 * @param type
	 *            a type.
	 * @param annotation
	 *            anchor annotation
	 * @param index
	 *            relative position to access. A negative value selects a preceding annotation
	 *            while a positive number selects a following annotation.
	 * @return the addressed annotation.
	 * @throws IndexOutOfBoundsException if the relative index points beyond the type index bounds.
	 */
	public static AnnotationFS selectSingleRelative(CAS cas, Type type,
			AnnotationFS annotation, int index) {
		if (!cas.getTypeSystem().subsumes(cas.getAnnotationType(), type)) {
			throw new IllegalArgumentException("Type [" + type.getName()
					+ "] is not an annotation type");
		}

		// move to first previous annotation
		FSIterator<AnnotationFS> itr = cas.getAnnotationIndex(type).iterator();
		itr.moveTo(annotation);

		if (index < 0) {
			// make sure we're past the beginning of the reference annotation
			while (itr.isValid() && itr.get().getEnd() > annotation.getBegin()) {
				itr.moveToPrevious();
			}
			
			for (int i = 0; i < (-index - 1) && itr.isValid(); ++i, itr.moveToPrevious()) {
				// Seeking
			}
			
			if (!itr.isValid()) {
				throw new IndexOutOfBoundsException("Reached end of index while seeking.");
			}
			else {
				return itr.get();
			}
		}
		else if (index > 0) {
			// make sure we're past the end of the reference annotation
			while (itr.isValid() && itr.get().getBegin() < annotation.getEnd()) {
				itr.moveToNext();
			}
			
			for (int i = 0; i < (index - 1) && itr.isValid(); ++i, itr.moveToPrevious()) {
				// Seeking
			}
			
			if (!itr.isValid()) {
				throw new IndexOutOfBoundsException("Reached end of index while seeking.");
			}
			else {
				return itr.get();
			}
		}
		else {
			return annotation;
		}
	}

	/**
	 * Returns the n annotations preceding the given annotation
	 *
	 * @param cas
	 *            a CAS.
	 * @param type
	 *            a UIMA type.
	 * @param annotation
	 *            anchor annotation
	 * @param count
	 *            number of annotations to collect
	 * @return List of aType annotations preceding anchor annotation
	 */
	public static List<AnnotationFS> selectPreceding(CAS cas, Type type, AnnotationFS annotation,
			int count) {
		if (!cas.getTypeSystem().subsumes(cas.getAnnotationType(), type)) {
			throw new IllegalArgumentException("Type [" + type.getName()
					+ "] is not an annotation type");
		}

		// move to first previous annotation
		FSIterator<AnnotationFS> itr = cas.getAnnotationIndex(type).iterator();
		itr.moveTo(annotation);

		// make sure we're past the beginning of the reference annotation
		while (itr.isValid() && itr.get().getEnd() > annotation.getBegin()) {
			itr.moveToPrevious();
		}

		// add annotations from the iterator into the result list
		List<AnnotationFS> precedingAnnotations = new LinkedList<AnnotationFS>();
		for (int i = 0; i < count && itr.isValid(); ++i, itr.moveToPrevious()) {
			precedingAnnotations.add(itr.get());
		}

		// return in correct order
		Collections.reverse(precedingAnnotations);
		return precedingAnnotations;
	}

	/**
	 * Returns the n annotations following the given annotation
	 *
	 * @param cas
	 *            a CAS.
	 * @param type
	 *            a UIMA type.
	 * @param annotation
	 *            anchor annotation
	 * @param count
	 *            number of annotations to collect
	 * @return List of aType annotations following anchor annotation
	 */
	public static List<AnnotationFS> selectFollowing(CAS cas, Type type, AnnotationFS annotation,
			int count) {
		if (!cas.getTypeSystem().subsumes(cas.getAnnotationType(), type)) {
			throw new IllegalArgumentException("Type [" + type.getName()
					+ "] is not an annotation type");
		}

		// move to first previous annotation
		FSIterator<AnnotationFS> itr = cas.getAnnotationIndex(type).iterator();
		itr.moveTo(annotation);

		// make sure we're past the end of the reference annotation
		while (itr.isValid() && itr.get().getBegin() < annotation.getEnd()) {
			itr.moveToNext();
		}

		// add annotations from the iterator into the result list
		List<AnnotationFS> followingAnnotations = new LinkedList<AnnotationFS>();
		for (int i = 0; i < count && itr.isValid(); ++i, itr.moveToNext()) {
			followingAnnotations.add(itr.get());
		}
		return followingAnnotations;
	}

	/**
	 * Convenience method to get the specified view or a default view if the requested view does not
	 * exist. The default can also be {@code null}.
	 *
	 * @param cas
	 *            a CAS
	 * @param viewName
	 *            the requested view.
	 * @param fallback
	 *            the default view if the requested view does not exist.
	 * @return the requested view or the default if the requested view does not exist.
	 */
	public static CAS getView(CAS cas, String viewName, CAS fallback) {
		CAS view;
		try {
			view = cas.getView(viewName);
		}
		catch (CASRuntimeException e) {
			// use fall-back view instead
			view = fallback;
		}
		return view;
	}

	/**
	 * Convenience method to get the specified view or create a new view if the requested view does
	 * not exist.
	 *
	 * @param cas
	 *            a CAS
	 * @param viewName
	 *            the requested view.
	 * @param create
	 *            the view is created if it does not exist.
	 * @return the requested view
	 * @throws IllegalArgumentException
	 *             if the view does not exist and is not to be created.
	 */
	public static CAS getView(CAS cas, String viewName, boolean create) {
		CAS view;
		try {
			view = cas.getView(viewName);
		}
		catch (CASRuntimeException e) {
			// View does not exist
			if (create) {
				view = cas.createView(viewName);
			}
			else {
				throw new IllegalArgumentException("No view with name [" + viewName + "]");
			}
		}

		return view;
	}

	/**
	 * Fetch the text covered by the specified annotations and return it as a list of strings.
	 *
	 * @param <T>
	 *            UIMA JCas type.
	 * @param iterable
	 *            annotation container.
	 * @return list of covered strings.
	 */
	public static <T extends AnnotationFS> List<String> toText(Iterable<T> iterable) {
		return toText(iterable.iterator());
	}

	/**
	 * Fetch the text covered by the specified annotations and return it as a list of strings.
	 *
	 * @param <T>
	 *            UIMA JCas type.
	 * @param iterator
	 *            annotation iterator.
	 * @return list of covered strings.
	 */
	public static <T extends AnnotationFS> List<String> toText(Iterator<T> iterator) {
		List<String> text = new ArrayList<String>();
		while (iterator.hasNext()) {
			AnnotationFS a = iterator.next();
			text.add(a.getCoveredText());
		}
		return text;
	}
}
