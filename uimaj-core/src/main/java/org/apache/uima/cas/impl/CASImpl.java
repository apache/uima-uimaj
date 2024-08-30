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

package org.apache.uima.cas.impl;

import static java.lang.String.format;
import static java.lang.System.identityHashCode;
import static org.apache.uima.cas.impl.FSIndexRepositoryImpl.INCLUDE_BAG_INDEXES;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMARuntimeException;
import org.apache.uima.UimaSerializable;
import org.apache.uima.cas.AbstractCas_ImplBase;
import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.BooleanArrayFS;
import org.apache.uima.cas.ByteArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.CasOwner;
import org.apache.uima.cas.CommonArrayFS;
import org.apache.uima.cas.ComponentInfo;
import org.apache.uima.cas.ConstraintFactory;
import org.apache.uima.cas.DoubleArrayFS;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIndexRepository;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FSMatchConstraint;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeaturePath;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.FeatureValuePath;
import org.apache.uima.cas.FloatArrayFS;
import org.apache.uima.cas.IntArrayFS;
import org.apache.uima.cas.LongArrayFS;
import org.apache.uima.cas.Marker;
import org.apache.uima.cas.SerialFormat;
import org.apache.uima.cas.ShortArrayFS;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.cas.SofaID;
import org.apache.uima.cas.StringArrayFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.admin.CASAdminException;
import org.apache.uima.cas.admin.CASFactory;
import org.apache.uima.cas.admin.CASMgr;
import org.apache.uima.cas.admin.FSIndexComparator;
import org.apache.uima.cas.admin.FSIndexRepositoryMgr;
import org.apache.uima.cas.admin.TypeSystemMgr;
import org.apache.uima.cas.impl.FSsTobeAddedback.FSsTobeAddedbackSingle;
import org.apache.uima.cas.impl.SlotKinds.SlotKind;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.cas.text.Language;
import org.apache.uima.internal.util.IntVector;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.internal.util.PositiveIntSet;
import org.apache.uima.internal.util.PositiveIntSet_impl;
import org.apache.uima.internal.util.UIMAClassLoader;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.AnnotationBase;
import org.apache.uima.jcas.cas.BooleanArray;
import org.apache.uima.jcas.cas.ByteArray;
import org.apache.uima.jcas.cas.DoubleArray;
import org.apache.uima.jcas.cas.EmptyFSList;
import org.apache.uima.jcas.cas.EmptyFloatList;
import org.apache.uima.jcas.cas.EmptyIntegerList;
import org.apache.uima.jcas.cas.EmptyList;
import org.apache.uima.jcas.cas.EmptyStringList;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FloatArray;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.LongArray;
import org.apache.uima.jcas.cas.ShortArray;
import org.apache.uima.jcas.cas.Sofa;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.impl.JCasHashMap;
import org.apache.uima.jcas.impl.JCasImpl;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.util.AutoCloseableNoException;
import org.apache.uima.util.Level;

/**
 * Implements the CAS interfaces. This class must be public because we need to be able to create
 * instance of it from outside the package. Use at your own risk. May change without notice.
 * 
 */
public class CASImpl extends AbstractCas_ImplBase
        implements CAS, CASMgr, LowLevelCAS, TypeSystemConstants {

  private static final String DISABLE_SUBTYPE_FSARRAY_CREATION = "uima.disable_subtype_fsarray_creation";
  static final boolean IS_DISABLE_SUBTYPE_FSARRAY_CREATION = Misc
          .getNoValueSystemProperty(DISABLE_SUBTYPE_FSARRAY_CREATION);

  private static final String TRACE_FSS = "uima.trace_fs_creation_and_updating";
  // public static final boolean IS_USE_V2_IDS = false; // if false, ids increment by 1
  private static final boolean trace = false; // debug
  public static final boolean traceFSs = // false; // debug - trace FS creation and update
          Misc.getNoValueSystemProperty(TRACE_FSS);

  public static final boolean traceCow = false; // debug - trace copy on write actions, index adds /
                                                // deletes
  private static final String traceFile = "traceFSs.log.txt";
  private static final PrintStream traceOut;
  static {
    try {
      if (traceFSs) {
        System.out.println("Creating traceFSs file in directory " + System.getProperty("user.dir"));
        traceOut = traceFSs
                ? new PrintStream(new BufferedOutputStream(new FileOutputStream(traceFile, false)))
                : null;
      } else {
        traceOut = null;
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static final boolean MEASURE_SETINT = false;

  // debug
  static final AtomicInteger casIdProvider = new AtomicInteger(0);

  // Notes on the implementation
  // ---------------------------

  // Floats are handled by casting them to ints when they are stored
  // in the heap. Conveniently, 0 casts to 0.0f, which is the default
  // value.

  public static final int NULL = 0;

  // Boolean scalar values are stored as ints in the fs heap.
  // TRUE is 1 and false is 0.
  public static final int TRUE = 1;

  public static final int FALSE = 0;

  public static final int DEFAULT_INITIAL_HEAP_SIZE = 500_000;

  public static final int DEFAULT_RESET_HEAP_SIZE = 5_000_000;

  /**
   * The UIMA framework detects (unless disabled, for high performance) updates to indexed FS which
   * update key values used as keys in indexes. Normally the framework will protect against index
   * corruption by temporarily removing the FS from the indexes, then do the update to the feature
   * value, and then addback the changed FS.
   * <p>
   * Users can use the protectIndexes() methods to explicitly control this remove - add back cycle,
   * for instance to "batch" together several updates to multiple features in a FS.
   * <p>
   * Some build processes may want to FAIL if any unprotected updates of this kind occur, instead of
   * having the framework silently recover them. This is enabled by having the framework throw an
   * exception; this is controlled by this global JVM property, which, if defined, causes the
   * framework to throw an exception rather than recover.
   */
  public static final String THROW_EXCEPTION_FS_UPDATES_CORRUPTS = "uima.exception_when_fs_update_corrupts_index";

  /**
   * @deprecate Will become package private.
   * @forRemoval 4.0.0
   */
  // public for test case use
  @Deprecated(since = "3.6.0")
  public static boolean IS_THROW_EXCEPTION_CORRUPT_INDEX = Misc
          .getNoValueSystemProperty(THROW_EXCEPTION_FS_UPDATES_CORRUPTS);

  /**
   * Define this JVM property to enable checking for invalid updates to features which are used as
   * keys by any index.
   * <ul>
   * <li>The following are the same: -Duima.check_invalid_fs_updates and
   * -Duima.check_invalid_fs_updates=true</li>
   * </ul>
   */
  public static final String REPORT_FS_UPDATES_CORRUPTS = "uima.report_fs_update_corrupts_index";

  static boolean IS_REPORT_FS_UPDATE_CORRUPTS_INDEX = IS_THROW_EXCEPTION_CORRUPT_INDEX
          || Misc.getNoValueSystemProperty(REPORT_FS_UPDATES_CORRUPTS);

  /**
   * Set this JVM property to false for high performance, (no checking); insure you don't have the
   * report flag (above) turned on - otherwise it will force this to "true".
   */
  public static final String DISABLE_PROTECT_INDEXES = "uima.disable_auto_protect_indexes";

  /**
   * the protect indexes flag is on by default, but may be turned of via setting the property.
   * 
   * This is overridden if a report is requested or the exception detection is on.
   */
  static boolean IS_DISABLED_PROTECT_INDEXES = Misc
          .getNoValueSystemProperty(DISABLE_PROTECT_INDEXES) && !IS_REPORT_FS_UPDATE_CORRUPTS_INDEX
          && !IS_THROW_EXCEPTION_CORRUPT_INDEX;

  public static final String ALWAYS_HOLD_ONTO_FSS = "uima.default_v2_id_references";
  static final boolean IS_ALWAYS_HOLD_ONTO_FSS = // debug and users of low-level cas apis with
                                                 // deserialization
          Misc.getNoValueSystemProperty(ALWAYS_HOLD_ONTO_FSS);
  // private static final int REF_DATA_FOR_ALLOC_SIZE = 1024;
  // private static final int INT_DATA_FOR_ALLOC_SIZE = 1024;
  //

  // this next seemingly non-sensical static block
  // is to force the classes needed by Eclipse debugging to load
  // otherwise, you get a com.sun.jdi.ClassNotLoadedException when
  // the class is used as part of formatting debugging messages
  static {
    new DebugNameValuePair(null, null);
    new DebugFSLogicalStructure();
  }

  private static final ThreadLocal<Boolean> defaultV2IdRefs = InheritableThreadLocal
          .withInitial(() -> null);

  public static ThreadLocal<Boolean> getDefaultV2IdRefs() {
    return defaultV2IdRefs;
  }

  // Static classes representing shared instance data
  // - shared data is computed once for all views

  /**
   * Journaling changes for computing delta cas. Each instance represents one or more changes for
   * one feature structure A particular Feature Structure may have multiple FsChange instances but
   * we attempt to minimize this
   */
  public static class FsChange {
    /** ref to the FS being modified */
    final TOP fs;
    /**
     * which feature (by offset) is modified
     */
    final BitSet featuresModified;

    final PositiveIntSet arrayUpdates;

    FsChange(TOP fs) {
      this.fs = fs;
      TypeImpl ti = fs._getTypeImpl();
      featuresModified = (ti.highestOffset == -1) ? null : new BitSet(ti.highestOffset + 1);
      arrayUpdates = (ti.isArray()) ? new PositiveIntSet_impl() : null;
    }

    void addFeatData(int v) {
      featuresModified.set(v);
    }

    void addArrayData(int v, int nbrOfConsecutive) {
      for (int i = 0; i < nbrOfConsecutive; i++) {
        arrayUpdates.add(v++);
      }
    }

    void addArrayData(PositiveIntSet indexesPlus1) {
      indexesPlus1.forAllInts(i -> arrayUpdates.add(i - 1));
    }

    @Override
    public int hashCode() {
      return 31 + ((fs == null) ? 0 : fs._id);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null || !(obj instanceof FsChange)) {
        return false;
      }
      return ((FsChange) obj).fs._id == fs._id;
    }
  }

  /**
   * Instances are put into a Stack, to remember previous state to switch back to, when switching
   * class loaders and locking the CAS
   * 
   * https://issues.apache.org/jira/browse/UIMA-6057
   */
  static class SwitchControl {
    final boolean wasLocked;
    boolean wasSwitched = false;

    SwitchControl(boolean wasLocked) {
      this.wasLocked = wasLocked;
    }
  }

  // fields shared among all CASes belong to views of a common base CAS
  static class SharedViewData {
    /**
     * map from FS ids to FSs.
     */
    private final Id2FS id2fs;
    /** set to > 0 to reuse an id, 0 otherwise */
    private int reuseId = 0;

    // Base CAS for all views
    private final CASImpl baseCAS;

    /**
     * These fields are here, not in TypeSystemImpl, because different CASes may have different
     * indexes but share the same type system They hold the same data (constant per CAS) but are
     * accessed with different indexes
     */
    private final BitSet featureCodesInIndexKeys = new BitSet(1024); // 128 bytes
    // private final BitSet featureJiInIndexKeys = new BitSet(1024); // indexed by JCas Feature
    // Index, not feature code.

    // A map from SofaNumbers which are also view numbers to IndexRepositories.
    // these numbers are dense, and start with 1. 1 is the initial view. 0 is the base cas
    ArrayList<FSIndexRepositoryImpl> sofa2indexMap;

    // @formatter:off
    /**
     * A map from Sofa numbers to CAS views.
     * number 0 - not used
     * number 1 - used for view named "_InitialView"
     * number 2-n used for other views
     * 
     * Note: this is not reset with "Cas Reset" because views (really, their associated index repos)
     * take a lot of setup for the indexes.
     * However, the maximum view count is reset; so creation of new views "reuses" these pre-setup indexRepos 
     * associated with these views.
     */
    // @formatter:on
    ArrayList<CASImpl> sofaNbr2ViewMap;

    /**
     * a set of instantiated sofaNames
     */
    private Set<String> sofaNameSet;

    // Flag that initial Sofa has been created
    private boolean initialSofaCreated = false;

    // Count of Views created in this cas
    // equals count of sofas except if initial view has no sofa.
    int viewCount;

    // The ClassLoader that should be used by the JCas to load the generated
    // FS cover classes for this CAS. Defaults to the ClassLoader used
    // to load the CASImpl class.
    private ClassLoader jcasClassLoader = this.getClass().getClassLoader();

    /*****************************
     * PEAR Support
     *****************************/
    /**
     * Only support one level of PEAR nesting; for more general approach, make this a deque
     */
    private ClassLoader previousJCasClassLoader = null;
    /**
     * Save area for suspending this while we create a base instance
     */
    private ClassLoader suspendPreviousJCasClassLoader;

    /**
     * A map from IDs to already created trampoline FSs for the base FS with that id. These are used
     * when in a Pear and retrieving a FS (via index or deref) and you want the Pear version for
     * that ID. There are potentially multiple maps - one per PEAR Classpath
     */
    private JCasHashMap id2tramp = null;
    /**
     * a map from IDs of FSs that have a Pear version, to the base (non-Pear) version used to locate
     * the base version for adding to indexes
     */
    private JCasHashMap id2base = null;
    private final Map<ClassLoader, JCasHashMap> cl2id2tramp = new IdentityHashMap<>();

    // @formatter:off
    /**
     * The current (active, switches at Pear boundaries) FsGenerators (excluding array-generators)
     * key = type code
     * read-only, unsynchronized for this CAS
     * Cache for setting this kept in TypeSystemImpl, by classloader 
     *   - shared among all CASs that use that Type System and class loader
     *   -- in turn, initialized from FSClassRegistry, once per classloader / typesystem combo 
     * 
     * Pear generators are mostly null except for instances where the PEAR has redefined the JCas
     * cover class
     */
    // @formatter:on
    private FsGenerator3[] generators;

    // @formatter:off
    /**
     * When generating a new instance of a FS in a PEAR where there's an alternate JCas class impl,
     * generate the base version, and make the alternate a trampoline to it.
     *   Note: in future, if it is known that this FS is never used outside of this PEAR, then can
     *         skip generating the double version
     */
    // @formatter:on
    private FsGenerator3[] baseGenerators;

    // If this CAS can be flushed (reset) or not.
    // often, the framework disables this before calling users code
    private boolean flushEnabled = true;

    // not final because set with reinit deserialization
    private TypeSystemImpl tsi;

    private ComponentInfo componentInfo;

    /**
     * This tracks the changes for delta cas May also in the future support Journaling by component,
     * allowing determination of which component in a flow created/updated a FeatureStructure (not
     * implmented)
     * 
     * TrackingMarkers are held on to by things outside of the Cas, to support switching from one
     * tracking marker to another (currently not used, but designed to support Component
     * Journaling).
     * 
     * We track changes on a granularity of features and for features which are arrays, which
     * element of the array (This last to enable efficient delta serializations of giant arrays of
     * things, where you've only updated a few items)
     * 
     * The FsChange doesn't store the changed data, only stores the ref info needed to get to what
     * was changed.
     */
    private MarkerImpl trackingMark;

    /**
     * Track modified preexistingFSs Note this is a map, keyed by the FS, so all changes are merged
     * when added
     */
    private Map<TOP, FsChange> modifiedPreexistingFSs;

    /**
     * This list currently only contains at most 1 element. If Journaling is implemented, it may
     * contain an element per component being journaled.
     */
    private List<MarkerImpl> trackingMarkList;

    /**
     * This stack corresponds to nested protectIndexes contexts. Normally should be very shallow.
     */
    private final ArrayList<FSsTobeAddedback> fssTobeAddedback = new ArrayList<>();

    /**
     * This version is for single fs use, by binary deserializers and by automatic mode Only one
     * user at a time is allowed.
     */
    private final FSsTobeAddedbackSingle fsTobeAddedbackSingle = (FSsTobeAddedbackSingle) FSsTobeAddedback
            .createSingle();
    /**
     * Set to true while this is in use.
     */
    boolean fsTobeAddedbackSingleInUse = false;

    /**
     * temporarily set to true by deserialization routines doing their own management of this check
     */
    boolean disableAutoCorruptionCheck = false;

    // used to generate FSIDs, increments by 1 for each use. First id == 1
    /**
     * The fsId of the last created FS used to generate FSIDs, increments by 1 for each use. First
     * id == 1
     */
    private int fsIdGenerator = 0;

    /**
     * The version 2 size on the main heap of the last created FS
     */
    private int lastFsV2Size = 1;

    /**
     * used to "capture" the fsIdGenerator value for a read-only CAS to be visible in other threads
     */
    AtomicInteger fsIdLastValue = new AtomicInteger(0);

    // mostly for debug - counts # times cas is reset
    private final AtomicInteger casResets = new AtomicInteger(0);

    // unique ID for a created CAS view, not updated if CAS is reset and reused
    private final String casId = String.valueOf(casIdProvider.incrementAndGet());

    // shared singltons, created at type system commit

    private EmptyFSList emptyFSList;
    private EmptyFloatList emptyFloatList;
    private EmptyIntegerList emptyIntegerList;
    private EmptyStringList emptyStringList;

    private FloatArray emptyFloatArray;
    private final Map<Type, FSArray> emptyFSArrayMap = new HashMap<>();
    private IntegerArray emptyIntegerArray;
    private StringArray emptyStringArray;
    private DoubleArray emptyDoubleArray;
    private LongArray emptyLongArray;
    private ShortArray emptyShortArray;
    private ByteArray emptyByteArray;
    private BooleanArray emptyBooleanArray;

    /**
     * Created at startup time, lives as long as the CAS lives Serves to reference code for binary
     * cas ser/des that used to live in this class, but was moved out
     */
    private final BinaryCasSerDes bcsd;

    /**
     * Created when doing binary or form4 non-delta (de)serialization, used in subsequent delta
     * ser/deserialization Created when doing binary or form4 non-delta ser/deserialization, used in
     * subsequent delta (de)serialization Reset with CasReset or deltaMergesComplete API call
     */
    private CommonSerDesSequential csds;

    /*************************************************
     * VERSION 2 LOW_LEVEL_API COMPATIBILITY SUPPORT *
     *************************************************/
    // @formatter:off
    /**
     * A StringSet used only to support ll_get/setInt api
     *   get adds string to this and returns the int handle
     *   set retrieves the string, given the handle
     * lazy initialized
     */
    // @formatter:on
    private StringSet llstringSet = null;

    // @formatter:off
    /**
     * A LongSet used only to support v2 ll_get/setInt api
     *   get adds long to this and returns the int handle
     *   set retrieves the long, given the handle
     * lazy initialized
     */
    // @formatter:on
    private LongSet lllongSet = null;

    // For tracing FS creation and updating, normally disabled
    private final StringBuilder traceFScreationSb = traceFSs ? new StringBuilder() : null;
    private final StringBuilder traceCowSb = traceCow ? new StringBuilder() : null;
    private int traceFSid = 0;
    private boolean traceFSisCreate;
    private final IntVector id2addr = traceFSs ? new IntVector() : null;
    private int nextId2Addr = 1; // only for tracing, to convert id's to v2 addresses
    private final int initialHeapSize;
    // @formatter:off
    /** if true, 
     *    modify fs creation to save in id2fs map
     *    modify deserializers to create fss with ids the same as the serialized form (
     *      or the V2 "address" imputed from that)
     *    modify serializers to include reachables only found via id2fs table
     * 
     * not static because is updated (see ll_enableV2IdRefs)
     */
    // @formatter:on
    private boolean isId2Fs;

    /**
     * a stack used to remember and restore previous state of cas lock and class loaders when
     * switching classloaders and locking the cas https://issues.apache.org/jira/browse/UIMA-6057
     */
    private final Deque<SwitchControl> switchControl = new ArrayDeque<>();

    // @formatter:off
    /******************************************************************************************
     * C A S   S T A T E    management                                                        *
     *    Cas state is implemented in a way to allow the Java to efficiently                  * 
     *    access the state test without synchronization or "voliatile" memory accessing,      *
     *    while at the same time, allowing for an occasional cross-thread memory invalidation *
     *    when the state is changed.  This is done using a MutableCallSite plus that          *
     *    objects "syncAll" method.                                                           *
     ******************************************************************************************/
    // @formatter:on
    private final EnumSet<CasState> casState = EnumSet.noneOf(CasState.class);

    private static final MethodType noArgBoolean = MethodType.methodType(boolean.class);
    private static final MethodHandle mh_return_false;
    private static final MethodHandle mh_return_true;
    static {
      try {
        mh_return_false = MethodHandles.lookup().findStatic(CasState.class, "return_false",
                noArgBoolean);
        mh_return_true = MethodHandles.lookup().findStatic(CasState.class, "return_true",
                noArgBoolean);
      } catch (NoSuchMethodException | IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }

    private final MutableCallSite is_updatable_callsite = new MutableCallSite(mh_return_true);
    private final MutableCallSite is_readable_callsite = new MutableCallSite(mh_return_true);

    private final MethodHandle is_updatable = is_updatable_callsite.dynamicInvoker();
    private final MethodHandle is_readable = is_readable_callsite.dynamicInvoker();

    private final MutableCallSite[] is_updatable_callsites = new MutableCallSite[] {
        is_updatable_callsite };
    private final MutableCallSite[] is_readable_callsites = new MutableCallSite[] {
        is_readable_callsite };

    private volatile Thread current_one_thread_access = null;

    private void updateCallSite(boolean desired_state, MethodHandle tester, MutableCallSite c,
            MethodHandle mh, MutableCallSite[] cs) {
      try {
        if (((boolean) tester.invokeExact()) != desired_state) {
          c.setTarget(mh);
          MutableCallSite.syncAll(cs);
        }
      } catch (Throwable e) {
        Misc.internalError(e);
      }
    }

    private synchronized boolean setCasState(CasState state, Thread thread) {
      boolean wasAdded = casState.add(state);
      if (wasAdded || (state == CasState.NO_ACCESS && thread != current_one_thread_access)) {
        switch (state) {
          case READ_ONLY:
            if (casState.contains(CasState.NO_ACCESS)) {
              break; // ignore readonly if no-access is set
            }
            updateCallSite(false, is_updatable, is_updatable_callsite, mh_return_false,
                    is_updatable_callsites);
            break;
          case NO_ACCESS:
            current_one_thread_access = thread;
            MethodHandle mh = CasState.produce_one_thread_access_test(thread);
            boolean b = true; // value ignored, needed to avoid compile warning
            try {
              b = (boolean) mh.invokeExact();
            } catch (Throwable e) {
              Misc.internalError(e);
            }
            updateCallSite(b, is_updatable, is_updatable_callsite, mh, is_updatable_callsites);
            updateCallSite(b, is_readable, is_readable_callsite, mh, is_readable_callsites);
            break;
          default:
        }
      }
      return wasAdded;
    }

    private synchronized boolean clearCasState(CasState state) {
      boolean wasRemoved = casState.remove(state);
      if (wasRemoved) {
        switch (state) {
          case READ_ONLY:
            if (casState.contains(CasState.NO_ACCESS)) {
              break;
            }
            updateCallSite(true, is_updatable, is_updatable_callsite, mh_return_true,
                    is_updatable_callsites);
            break;
          case NO_ACCESS:
            current_one_thread_access = null;
            updateCallSite(true, is_updatable, is_updatable_callsite, mh_return_true,
                    is_updatable_callsites);
            updateCallSite(true, is_readable, is_readable_callsite, mh_return_true,
                    is_readable_callsites);
            break;
          default:
        }
      }
      return wasRemoved;
    }

    private SharedViewData(CASImpl baseCAS, int initialHeapSize, TypeSystemImpl tsi) {
      this.baseCAS = baseCAS;
      this.tsi = tsi;
      this.initialHeapSize = initialHeapSize;
      bcsd = new BinaryCasSerDes(baseCAS);
      id2fs = new Id2FS(initialHeapSize);
      if (traceFSs) {
        id2addr.add(0);
      }

      Boolean v = getDefaultV2IdRefs().get();
      isId2Fs = (v == null) ? IS_ALWAYS_HOLD_ONTO_FSS : v;
    }

    void clearCasReset() {
      // fss
      fsIdGenerator = 0;
      lastFsV2Size = 1;
      id2fs.clear();

      // pear caches
      if (id2tramp != null) {
        id2tramp.clear();
      }
      if (id2base != null) {
        id2base.clear();
      }

      for (Iterator<Entry<ClassLoader, JCasHashMap>> it = cl2id2tramp.entrySet().iterator(); it
              .hasNext();) {
        Entry<ClassLoader, JCasHashMap> e = it.next();
        ClassLoader cl = e.getKey();
        e.getValue().clear();
        if (cl instanceof UIMAClassLoader) { // https://issues.apache.org/jira/browse/UIMA-5801
          if (((UIMAClassLoader) cl).isClosed()) {
            it.remove();
          }
        }
      }

      // index corruption avoidance
      fssTobeAddedback.clear();
      fsTobeAddedbackSingle.clear();
      fsTobeAddedbackSingleInUse = false;
      disableAutoCorruptionCheck = false;

      // misc
      flushEnabled = true;
      componentInfo = null;
      bcsd.clear();
      csds = null;
      llstringSet = null;
      traceFSid = 0;
      if (traceFSs) {
        traceFScreationSb.setLength(0);
        id2addr.removeAllElements();
        id2addr.add(0);
        nextId2Addr = 1;
      }

      emptyFloatList = null; // these cleared in case new ts redefines?
      emptyFSList = null;
      emptyIntegerList = null;
      emptyStringList = null;

      emptyFloatArray = null;
      emptyFSArrayMap.clear();
      emptyIntegerArray = null;
      emptyStringArray = null;
      emptyDoubleArray = null;
      emptyLongArray = null;
      emptyShortArray = null;
      emptyByteArray = null;
      emptyBooleanArray = null;

      current_one_thread_access = null;
      updateCallSite(true, is_updatable, is_updatable_callsite, mh_return_true,
              is_updatable_callsites);
      updateCallSite(true, is_readable, is_readable_callsite, mh_return_true,
              is_readable_callsites);

      clearNonSharedInstanceData();

    }

    /**
     * called by resetNoQuestions and cas complete reinit
     */
    void clearSofaInfo() {
      sofaNameSet.clear();
      initialSofaCreated = false;
    }

    // @formatter:off
    /**
     * Called from CasComplete deserialization (reinit).
     * 
     * Skips the resetNoQuestions operation of flushing the indexes, since these will be
     * reinitialized with potentially new definitions.
     * 
     * Clears additional data related to having the
     *   - type system potentially change
     *   - the features belonging to indexes change
     */
    // @formatter:on
    void clear() {
      resetNoQuestions(false); // false - skip flushing the index repos

      // type system + index spec
      tsi = null;
      featureCodesInIndexKeys.clear();
      // featureJiInIndexKeys.clear();

      /**
       * Clear the existing views, except keep the info for the initial view so that the cas
       * complete deserialization after setting up the new index repository in the base cas can
       * "refresh" the existing initial view (if present; if not present, a new one is created).
       */
      if (sofaNbr2ViewMap.size() >= 1) {
        // have initial view - preserve it
        CASImpl localInitialView = sofaNbr2ViewMap.get(1);
        sofaNbr2ViewMap.clear();
        Misc.setWithExpand(sofaNbr2ViewMap, 1, localInitialView);
        viewCount = 1;
      } else {
        sofaNbr2ViewMap.clear();
        viewCount = 0;
      }

    }

    private void resetNoQuestions(boolean flushIndexRepos) {
      casResets.incrementAndGet();
      if (trace) {
        System.out.println("CAS Reset in thread " + Thread.currentThread().getName()
                + " for CasId = " + casId + ", new reset count = " + casResets.get());
      }

      clearCasReset(); // also clears cached FSs

      if (flushIndexRepos) {
        flushIndexRepositoriesAllViews();
      }

      clearTrackingMarks();

      clearSofaInfo(); // but keep initial view, and other views
                       // because setting up the index infrastructure is expensive
      viewCount = 1; // initial view

      traceFSid = 0;
      if (traceFSs) {
        traceFScreationSb.setLength(0);
      }
      componentInfo = null; // https://issues.apache.org/jira/browse/UIMA-5097
      switchControl.clear(); // https://issues.apache.org/jira/browse/UIMA-6057
    }

    private void flushIndexRepositoriesAllViews() {
      int numViews = viewCount;
      for (int view = 1; view <= numViews; view++) {
        CASImpl tcas = (CASImpl) ((view == 1) ? getInitialView() : getViewFromSofaNbr(view));
        if (tcas != null) {
          tcas.indexRepository.flush();
        }
      }

      // safety : in case this public method is called on other than the base cas
      baseCAS.indexRepository.flush(); // for base view, other views flushed above
    }

    private void clearNonSharedInstanceData() {
      int numViews = viewCount;
      for (int view = 1; view <= numViews; view++) {
        CASImpl tcas = (CASImpl) ((view == 1) ? getInitialView() : getViewFromSofaNbr(view));
        if (tcas != null) {
          tcas.mySofaRef = null; // was in v2: (1 == view) ? -1 : 0;
          tcas.docAnnotIter = null;
        }
      }
    }

    private void clearTrackingMarks() {
      // resets all markers that might be held by things outside the Cas
      // Currently (2009) this list has a max of 1 element
      // Future impl may have one element per component for component Journaling
      if (trackingMarkList != null) {
        for (int i = 0; i < trackingMarkList.size(); i++) {
          trackingMarkList.get(i).isValid = false;
        }
      }

      trackingMark = null;
      if (null != modifiedPreexistingFSs) {
        modifiedPreexistingFSs.clear();
      }

      trackingMarkList = null;
    }

    // switches ClassLoader but does not lock CAS
    void switchClassLoader(ClassLoader newClassLoader, boolean wasLocked) {
      // https://issues.apache.org/jira/browse/UIMA-6057
      SwitchControl switchControlInstance = new SwitchControl(wasLocked);
      switchControl.push(switchControlInstance);

      if (null == newClassLoader) { // is null if no cl set
        return;
      }

      if (!(newClassLoader instanceof UIMAClassLoader)) {
        UIMAFramework.getLogger()
                .debug("Calling switchClassLoader with a classloader of type [{}] that is not "
                        + "a UIMAClassLoader may cause JCas wrappers to be loaded from the wrong "
                        + "classloader.", newClassLoader.getClass().getName());
      }

      if (newClassLoader != jcasClassLoader) {
        if (null != previousJCasClassLoader) {
          /**
           * Multiply nested classloaders not supported. Original base loader: {0}, current nested
           * loader: {1}, trying to switch to loader: {2}.
           */
          throw new CASRuntimeException(CASRuntimeException.SWITCH_CLASS_LOADER_NESTED,
                  previousJCasClassLoader, jcasClassLoader, newClassLoader);
        }
        // System.out.println("Switching to new class loader");
        previousJCasClassLoader = jcasClassLoader;
        jcasClassLoader = newClassLoader;
        switchControlInstance.wasSwitched = true;
        generators = tsi.getGeneratorsForClassLoader(newClassLoader, true); // true - isPear

        assert null == id2tramp; // is null outside of a pear
        id2tramp = cl2id2tramp.get(newClassLoader);
        if (null == id2tramp) {
          cl2id2tramp.put(newClassLoader, id2tramp = new JCasHashMap(32));
        }
        if (id2base == null) {
          id2base = new JCasHashMap(32);
        }
      }
    }

    void restoreClassLoader(boolean empty_switchControl, SwitchControl switchControlInstance) {
      if (null == previousJCasClassLoader) {
        return;
      }

      if ((empty_switchControl || switchControlInstance.wasSwitched)
              && previousJCasClassLoader != jcasClassLoader) {
        // System.out.println("Switching back to previous class loader");
        jcasClassLoader = previousJCasClassLoader;
        previousJCasClassLoader = null;
        generators = baseGenerators;
        id2tramp = null;
      }
    }

    // @formatter:off
    /**
     * The logic for this is:
     *   - normal - add 1 to the value of the previous 
     *              which is kept in fsIdGenerator
     *              Update fsIdGenerator to be this id.
     *              (maybe) set lastFsV2Size to the size of this FS in v2
     *   - pear trampolines: use the exact same id as the main fs.
     *            This value is in reuseId.  
     *            In this case, no computation of "next" is done
     *   - isId2Fs This is set if in special mode to emulate v2 addresses.  
     *       - used for backwards compatibility when LowLevelCas getFSForRef calls in use
     *       - used for debugging v2 vs v3 runs
     *       - causes fsId to be set to a value which should match the v2 address
     * Side effect: when doing v2 emulation, updates the lastFsV2Size
     * @param fs - the fs, used to compute its "size" on the v2 heap when emulating v2 addresses
     * @return the id to use
     */
    // @formatter:on
    private int getNextFsId(TOP fs) {
      if (reuseId != 0) { // for pear use
        // l.setStrongRef(fs, reuseId);
        int r = reuseId;
        reuseId = 0;
        return r; // reuseId reset to 0 by callers' try/finally block
      }

      // l.add(fs);
      // if (id2fs.size() != (2 + fsIdGenerator.get())) {
      // System.out.println("debug out of sync id generator and id2fs size");
      // }
      // assert(l.size() == (2 + fsIdGenerator));
      final int p = fsIdGenerator;

      final int r = fsIdGenerator = peekNextFsId();

      if (r < p) {
        throw new RuntimeException("UIMA Cas Internal id value overflowed maximum int value");
      }

      if (isId2Fs) {
        // this computation is partial - misses length of arrays stored on heap
        // because that info not yet available
        // It is added later via call to adjustLastFsV2size(int)
        lastFsV2Size = fs._getTypeImpl().getFsSpaceReq();
      }
      return r;
    }

    /**
     * @return the lastUsedFsId + the size of that or 1
     */
    int peekNextFsId() {
      return fsIdGenerator + lastFsV2IdIncr();
    }

    int lastFsV2IdIncr() {
      return (isId2Fs ? lastFsV2Size : 1);
    }

    private CASImpl getViewFromSofaNbr(int nbr) {
      final ArrayList<CASImpl> sn2v = sofaNbr2ViewMap;
      if (nbr < sn2v.size()) {
        return sn2v.get(nbr);
      }
      return null;
    }

    // For internal platform use only
    CASImpl getInitialView() {
      CASImpl couldBeThis = getViewFromSofaNbr(1);
      if (couldBeThis != null) {
        return couldBeThis;
      }
      // create the initial view, without a Sofa
      CASImpl aView = new CASImpl(baseCAS, (SofaFS) null);
      setViewForSofaNbr(1, aView);
      assert (viewCount <= 1);
      viewCount = 1;
      return aView;
    }

    void setViewForSofaNbr(int nbr, CASImpl view) {
      Misc.setWithExpand(sofaNbr2ViewMap, nbr, view);
    }

  }

  /**********************************************
   * C A S S T A T E m a n a g e m e n t *
   **********************************************/

  /**
   * @param state
   *          to add to the set
   * @return true if the set changed as a result of this operation
   */
  public boolean setCasState(CasState state) {
    return setCasState(state, null);
  }

  /**
   * @param state
   *          to add to the set
   * @param thread
   *          null or the thread to permit access to
   * @return true if the set changed as a result of this operation
   */
  public boolean setCasState(CasState state, Thread thread) {
    return svd.setCasState(state, thread);
  }

  /**
   * @param state
   *          to see if it is among the items in this set
   * @return true if the set contains that state
   */
  public boolean containsCasState(CasState state) {
    return svd.casState.contains(state);
  }

  /**
   * @param state
   *          to be removed
   * @return true if it was present, and is now removed
   */
  public boolean clearCasState(CasState state) {
    return svd.clearCasState(state);
  }

  boolean is_updatable() {
    try {
      return (boolean) svd.is_updatable.invokeExact();
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  boolean is_readable() {
    try {
      return (boolean) svd.is_readable.invokeExact();
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  /*****************************************************************
   * Non-shared instance data kept per CAS view incl base CAS
   *****************************************************************/
  // package protected to let other things share this info
  final SharedViewData svd; // shared view data

  // public only for cross package access
  public boolean isCasLocked() {
    return !svd.flushEnabled;
  }

  /** The index repository. Referenced by XmiCasSerializer */
  FSIndexRepositoryImpl indexRepository;

  // private Object[] currentRefDataForAlloc = new Object[REF_DATA_FOR_ALLOC_SIZE];
  // private Object[] returnRefDataForAlloc;
  // private int[] currentIntDataForAlloc = new int[INT_DATA_FOR_ALLOC_SIZE];
  // private int[] returnIntDataForAlloc;
  // private int nextRefDataOffsetForAlloc = 0;
  // private int nextIntDataOffsetForAlloc = 0;

  // @formatter:off
  /**
   * The Feature Structure for the sofa FS for this view, or
   * null
   * //-1 if the sofa FS is for the initial view, or
   * // 0 if there is no sofa FS - for instance, in the "base cas"
   */
  // @formatter:on
  private Sofa mySofaRef = null;

  /** the corresponding JCas object */
  JCasImpl jcas = null;

  // @formatter:off
  /**
   * Copies of frequently accessed data pulled up for 
   * locality of reference - only an optimization
   *   - each value needs to be reset appropriately 
   *   - getters check for null, and if null, do the get.
   */
  // @formatter:on
  private TypeSystemImpl tsi_local;

  /**
   * for Pear generation - set this to the base FS not in SharedViewData to reduce object traversal
   * when generating FSs
   */
  FeatureStructureImplC pearBaseFs = null;

  // @formatter:off
  /**
   * Optimization - keep a documentAnnotationIterator handy for getting a ref to the doc annot
   *   Initialized lazily, synchronized
   *   One per cas view
   */
  // @formatter:on
  private volatile FSIterator<Annotation> docAnnotIter = null;

  // UIMA-6199 provides access to non-indexed doc annot
  // to allow sofa setting to set the "length" of the local sofa data string
  // @see updateDocumentAnnotation() updateDocumentAnnotation.
  private volatile Annotation deserialized_doc_annot_not_indexed = null;

  // private StackTraceElement[] addbackSingleTrace = null; // for debug use only, normally
  // commented out

  // CASImpl(TypeSystemImpl typeSystem) {
  // this(typeSystem, DEFAULT_INITIAL_HEAP_SIZE);
  // }

  // // Reference existing CAS
  // // For use when creating views of the CAS
  // CASImpl(CAS cas) {
  // this.setCAS(cas);
  // this.useFSCache = false;
  // initTypeVariables();
  // }

  /*
   * Configure a new (base view) CASImpl, **not a new view** typeSystem can be null, in which case a
   * new instance of TypeSystemImpl is set up, but not committed. If typeSystem is not null, it is
   * committed (locked). ** Note: it is assumed that the caller of this will always set up the
   * initial view ** by calling
   */

  public CASImpl(TypeSystemImpl typeSystem, int initialHeapSize) {
    TypeSystemImpl ts;
    final boolean externalTypeSystem = (typeSystem != null);

    if (externalTypeSystem) {
      ts = typeSystem;
    } else {
      ts = (TypeSystemImpl) CASFactory.createTypeSystem(); // creates also new CASMetadata and
      // FSClassRegistry instances
    }

    svd = new SharedViewData(this, initialHeapSize, ts);
    // this.svd.baseCAS = this;

    // this.svd.heap = new Heap(initialHeapSize);

    if (externalTypeSystem) {
      commitTypeSystem();
    }

    svd.sofa2indexMap = new ArrayList<>();
    svd.sofaNbr2ViewMap = new ArrayList<>();
    svd.sofaNameSet = new HashSet<>();
    svd.initialSofaCreated = false;
    svd.viewCount = 0;

    svd.clearTrackingMarks();
  }

  public CASImpl() {
    this((TypeSystemImpl) null, DEFAULT_INITIAL_HEAP_SIZE);
  }

  // In May 2007, appears to have 1 caller, createCASMgr in Serialization class,
  // could have out-side the framework callers because it is public.
  public CASImpl(CASMgrSerializer ser) {
    this(ser.getTypeSystem(), DEFAULT_INITIAL_HEAP_SIZE);
    checkInternalCodes(ser);
    // assert(ts != null);
    // assert(getTypeSystem() != null);
    indexRepository = ser.getIndexRepository(this);
  }

  // Use this when creating a CAS view
  CASImpl(CASImpl cas, SofaFS aSofa) {

    // these next fields are final and must be set in the constructor
    svd = cas.svd;

    mySofaRef = (Sofa) aSofa;

    // get the indexRepository for this Sofa
    indexRepository = (mySofaRef == null) ? (FSIndexRepositoryImpl) cas.getSofaIndexRepository(1)
            : (FSIndexRepositoryImpl) cas.getSofaIndexRepository(aSofa);
    if (null == indexRepository) {
      // create the indexRepository for this CAS
      // use the baseIR to create a lightweight IR copy
      FSIndexRepositoryImpl baseIndexRepo = (FSIndexRepositoryImpl) cas.getBaseIndexRepository();
      indexRepository = new FSIndexRepositoryImpl(this, baseIndexRepo);
      // the index creation depends on "indexRepository" already being set
      baseIndexRepo.name2indexMap.keySet().stream()
              .forEach(key -> indexRepository.createIndex(baseIndexRepo, key));
      indexRepository.commit();
      // save new sofa index
      if (mySofaRef == null) {
        cas.setSofaIndexRepository(1, indexRepository);
      } else {
        cas.setSofaIndexRepository(aSofa, indexRepository);
      }
    }
  }

  // Use this when creating a CAS view
  void refreshView(CAS cas, SofaFS aSofa) {

    if (aSofa != null) {
      // save address of SofaFS
      mySofaRef = (Sofa) aSofa;
    } else {
      // this is the InitialView
      mySofaRef = null;
    }

    // toss the JCas, if it exists
    jcas = null;

    // create the indexRepository for this Sofa
    final FSIndexRepositoryImpl baseIndexRepo = (FSIndexRepositoryImpl) ((CASImpl) cas)
            .getBaseIndexRepository();
    indexRepository = new FSIndexRepositoryImpl(this, baseIndexRepo);
    // the index creation depends on "indexRepository" already being set
    baseIndexRepo.name2indexMap.keySet().stream()
            .forEach(key -> indexRepository.createIndex(baseIndexRepo, key));

    indexRepository.commit();
    // save new sofa index
    if (mySofaRef == null) {
      ((CASImpl) cas).setSofaIndexRepository(1, indexRepository);
    } else {
      ((CASImpl) cas).setSofaIndexRepository(aSofa, indexRepository);
    }
  }

  private void checkInternalCodes(CASMgrSerializer ser) throws CASAdminException {
    if ((ser.topTypeCode > 0) && (ser.topTypeCode != topTypeCode)) {
      throw new CASAdminException(CASAdminException.DESERIALIZATION_ERROR);
    }
    if (ser.featureOffsets == null) {
      return;
    }
    // if (ser.featureOffsets.length != this.svd.casMetadata.featureOffset.length) {
    // throw new CASAdminException(CASAdminException.DESERIALIZATION_ERROR);
    // }
    TypeSystemImpl tsi = getTypeSystemImpl();
    for (int i = 1; i < ser.featureOffsets.length; i++) {
      FeatureImpl fi = tsi.getFeatureForCode_checked(i);
      int adjOffset = fi.isInInt ? 0 : fi.getRangeImpl().nbrOfUsedIntDataSlots;
      if (ser.featureOffsets[i] != (fi.getOffset() + adjOffset)) {
        throw new CASAdminException(CASAdminException.DESERIALIZATION_ERROR);
      }
    }
  }

  // ----------------------------------------
  // accessors for data in SharedViewData
  // ----------------------------------------

  public boolean isId2Fs() {
    return svd.isId2Fs;
  }

  Id2FS getId2FSs() {
    return svd.id2fs;
  }

  void set_id2fs(TOP fs) {
    svd.id2fs.put(fs);
  }

  void set_reuseId(int id) {
    svd.reuseId = id;
  }

  void setLastUsedFsId(int id) {
    svd.fsIdGenerator = id;
  }

  void setLastFsV2Size(int size) {
    svd.lastFsV2Size = size;
  }

  void addSofaViewName(String id) {
    svd.sofaNameSet.add(id);
  }

  void setViewCount(int n) {
    svd.viewCount = n;
  }

  void addbackSingle(TOP fs) {
    if (!svd.fsTobeAddedbackSingleInUse) {
      Misc.internalError();
    }
    svd.fsTobeAddedbackSingle.addback(fs);
    svd.fsTobeAddedbackSingleInUse = false;
  }

  void addbackSingleIfWasRemoved(boolean wasRemoved, TOP fs) {
    if (wasRemoved) {
      addbackSingle(fs);
    }
    svd.fsTobeAddedbackSingleInUse = false;
  }

  private FSsTobeAddedback getAddback(int size) {
    if (svd.fsTobeAddedbackSingleInUse) {
      Misc.internalError();
    }
    return svd.fssTobeAddedback.get(size - 1);
  }

  FSsTobeAddedbackSingle getAddbackSingle() {
    if (svd.fsTobeAddedbackSingleInUse) {
      // System.out.println(Misc.dumpCallers(addbackSingleTrace, 2, 100));
      Misc.internalError();
    }
    // addbackSingleTrace = Thread.currentThread().getStackTrace();

    svd.fsTobeAddedbackSingleInUse = true;
    svd.fsTobeAddedbackSingle.clear(); // safety
    return svd.fsTobeAddedbackSingle;
  }

  void featureCodes_inIndexKeysAdd(int featCode/* , int registryIndex */) {
    svd.featureCodesInIndexKeys.set(featCode);
    // skip adding if no JCas registry entry for this feature
    // if (registryIndex >= 0) {
    // svd.featureJiInIndexKeys.set(registryIndex);
    // }
  }

  @Override
  public void enableReset(boolean flag) {
    svd.flushEnabled = flag;
  }

  @Override
  public final TypeSystem getTypeSystem() {
    return getTypeSystemImpl();
  }

  public final TypeSystemImpl getTypeSystemImpl() {
    if (tsi_local == null) {
      tsi_local = svd.tsi;
    }
    return tsi_local;
  }

  /**
   * Set the shared svd type system ref, in all views
   * 
   * @param ts
   */
  void installTypeSystemInAllViews(TypeSystemImpl ts) {
    svd.tsi = ts;
    final List<CASImpl> sn2v = svd.sofaNbr2ViewMap;
    if (sn2v.size() > 0) {
      for (CASImpl view : sn2v.subList(1, sn2v.size())) {
        view.tsi_local = ts;
      }
    }
    getBaseCAS().tsi_local = ts;
  }

  @Override
  public ConstraintFactory getConstraintFactory() {
    return ConstraintFactory.instance();
  }

  /**
   * Create the appropriate Feature Structure Java instance - from whatever the generator for this
   * type specifies.
   * 
   * @param type
   *          the type to create
   * @return a Java object representing the FeatureStructure impl in Java.
   */
  @Override
  public <T extends FeatureStructure> T createFS(Type type) {
    final TypeImpl ti = (TypeImpl) type;
    if (!ti.isCreatableAndNotBuiltinArray()) {
      throw new CASRuntimeException(CASRuntimeException.NON_CREATABLE_TYPE, type.getName(),
              "CAS.createFS()");
    }
    return (T) createFSAnnotCheck(ti);
  }

  private <T extends FeatureStructureImplC> T createFSAnnotCheck(TypeImpl ti) {
    if (ti.isAnnotationBaseType()) {
      // not here, will be checked later in AnnotationBase constructor
      // if (this.isBaseCas()) {
      // throw new CASRuntimeException(CASRuntimeException.DISALLOW_CREATE_ANNOTATION_IN_BASE_CAS,
      // ti.getName());
      // }
      getSofaRef(); // materialize this if not present; required for setting the sofa ref
                    // must happen before the annotation is created, for compressed form 6
                    // serialization order
                    // to insure sofa precedes the ref of it
    }

    assertTypeBelongsToCasTypesystem(ti);

    FsGenerator3 g = svd.generators[ti.getCode()]; // get generator or null

    return (g != null) ? (T) g.createFS(ti, this)
            // pear case, with no overriding pear - use base
            : (T) createFsFromGenerator(svd.baseGenerators, ti);

    //
    //
    // // not pear or no special cover class for this
    //
    //
    // TOP fs = createFsFromGenerator(svd.baseGenerators, ti);
    //
    // FsGenerator g = svd.generators[ti.getCode()]; // get pear generator or null
    // return (g != null)
    // ? (T) pearConvert(fs, g)
    // : (T) fs;
    // }
    //
    // return (T) createFsFromGenerator(svd.generators, ti);
  }

  private static final AtomicInteger strictTypeSourceCheckMessageCount = new AtomicInteger(0);

  private void assertTypeBelongsToCasTypesystem(TypeImpl ti) {
    if (tsi_local != null && ti.getTypeSystem() != tsi_local) {
      String message = String.format(
              "Creating a feature structure of type [%s](%d) from type system [%s] in CAS with "
                      + "different type system [%s] is not supported.",
              ti.getName(), ti.getCode(), format("<%,d>", identityHashCode(ti.getTypeSystem())),
              format("<%,d>", identityHashCode(tsi_local)));

      if (TypeSystemImpl.IS_ENABLE_STRICT_TYPE_SOURCE_CHECK) {
        throw new IllegalArgumentException(message);
      } else {
        Misc.decreasingWithTrace(strictTypeSourceCheckMessageCount, message,
                UIMAFramework.getLogger());
      }
    }
  }

  /**
   * Called during construction of FS. For normal FS "new" operators, if in PEAR context, make the
   * base version
   * 
   * @param fs
   * @param ti
   * @return true if made a base for a trampoline
   */
  boolean maybeMakeBaseVersionForPear(FeatureStructureImplC fs, TypeImpl ti) {
    if (!inPearContext()) {
      return false;
    }
    FsGenerator3 g = svd.generators[ti.getCode()]; // get pear generator or null
    if (g == null) {
      return false;
    }
    TOP baseFs;
    try {
      suspendPearContext();
      assertTypeBelongsToCasTypesystem(ti);
      svd.reuseId = fs._id;
      baseFs = createFsFromGenerator(svd.baseGenerators, ti);
    } finally {
      restorePearContext();
      svd.reuseId = 0;
    }
    svd.id2base.put(baseFs);
    svd.id2tramp.put(baseFs._id, (TOP) fs);
    pearBaseFs = baseFs;
    return true;
  }

  private TOP createFsFromGenerator(FsGenerator3[] gs, TypeImpl ti) {
    return gs[ti.getCode()].createFS(ti, this);
  }

  public TOP createArray(TypeImpl array_type, int arrayLength) {
    TypeImpl_array tia = (TypeImpl_array) array_type;
    TypeImpl componentType = tia.getComponentType();
    if (componentType.isPrimitive()) {
      checkArrayPreconditions(arrayLength);
      switch (componentType.getCode()) {
        case intTypeCode:
          return new IntegerArray(array_type, this, arrayLength);
        case floatTypeCode:
          return new FloatArray(array_type, this, arrayLength);
        case booleanTypeCode:
          return new BooleanArray(array_type, this, arrayLength);
        case byteTypeCode:
          return new ByteArray(array_type, this, arrayLength);
        case shortTypeCode:
          return new ShortArray(array_type, this, arrayLength);
        case longTypeCode:
          return new LongArray(array_type, this, arrayLength);
        case doubleTypeCode:
          return new DoubleArray(array_type, this, arrayLength);
        case stringTypeCode:
          return new StringArray(array_type, this, arrayLength);
        default:
          throw Misc.internalError();
      }
    }
    // return (TOP) createArrayFS(/* array_type, */ arrayLength); // for backwards compat w/ v2,
    // don't create typed arrays
    if (IS_DISABLE_SUBTYPE_FSARRAY_CREATION) {
      return (TOP) createArrayFS(arrayLength);
    } else {
      return (TOP) createArrayFS(array_type, arrayLength);
    }
  }

  /*
   * =============== These methods might be deprecated in favor of new FSArray(jcas, length) etc.
   * except that these run with the CAS, not JCas (non-Javadoc)
   * 
   * @see org.apache.uima.cas.CAS#createArrayFS(int)
   */
  @Override
  public ArrayFS createArrayFS(int length) {
    return createArrayFS(getTypeSystemImpl().fsArrayType, length);
  }

  private ArrayFS createArrayFS(TypeImpl type, int length) {
    checkArrayPreconditions(length);
    return new FSArray(type, this, length);
  }

  @Override
  public IntArrayFS createIntArrayFS(int length) {
    checkArrayPreconditions(length);
    return new IntegerArray(getTypeSystemImpl().intArrayType, this, length);
  }

  @Override
  public FloatArrayFS createFloatArrayFS(int length) {
    checkArrayPreconditions(length);
    return new FloatArray(getTypeSystemImpl().floatArrayType, this, length);
  }

  @Override
  public StringArrayFS createStringArrayFS(int length) {
    checkArrayPreconditions(length);
    return new StringArray(getTypeSystemImpl().stringArrayType, this, length);
  }

  // return true if only one sofa and it is the default text sofa
  public boolean isBackwardCompatibleCas() {
    // check that there is exactly one sofa
    if ((svd.viewCount != 1) || !svd.initialSofaCreated) {
      return false;
    }

    Sofa sofa = getInitialView().getSofa();

    // check for mime type exactly equal to "text"
    String sofaMime = sofa.getMimeType();
    if (!"text".equals(sofaMime)) {
      return false;
    }
    // check that sofaURI and sofaArray are not set
    String sofaUri = sofa.getSofaURI();
    if (sofaUri != null) {
      return false;
    }
    TOP sofaArray = sofa.getSofaArray();
    if (sofaArray != null) {
      return false;
    }
    // check that name is NAME_DEFAULT_SOFA
    String sofaname = sofa.getSofaID();
    return NAME_DEFAULT_SOFA.equals(sofaname);
  }

  int getViewCount() {
    return svd.viewCount;
  }

  FSIndexRepository getSofaIndexRepository(SofaFS aSofa) {
    return getSofaIndexRepository(aSofa.getSofaRef());
  }

  FSIndexRepositoryImpl getSofaIndexRepository(int aSofaRef) {
    if (aSofaRef >= svd.sofa2indexMap.size()) {
      return null;
    }
    return svd.sofa2indexMap.get(aSofaRef);
  }

  void setSofaIndexRepository(SofaFS aSofa, FSIndexRepositoryImpl indxRepos) {
    setSofaIndexRepository(aSofa.getSofaRef(), indxRepos);
  }

  void setSofaIndexRepository(int aSofaRef, FSIndexRepositoryImpl indxRepos) {
    Misc.setWithExpand(svd.sofa2indexMap, aSofaRef, indxRepos);
  }

  @Override
  @Deprecated
  public SofaFS createSofa(SofaID sofaID, String mimeType) {
    // extract absolute SofaName string from the ID
    SofaFS aSofa = createSofa(sofaID.getSofaID(), mimeType);
    getView(aSofa); // will create the view, needed to make the
    // resetNoQuestions and other things that
    // iterate over views work.
    return aSofa;
  }

  Sofa createSofa(String sofaName, String mimeType) {
    return createSofa(++svd.viewCount, sofaName, mimeType);
  }

  Sofa createSofa(int sofaNum, String sofaName, String mimeType) {
    if (svd.sofaNameSet.contains(sofaName)) {
      throw new CASRuntimeException(CASRuntimeException.SOFANAME_ALREADY_EXISTS, sofaName);
    }
    final boolean viewAlreadyExists = sofaNum == svd.viewCount;
    if (!viewAlreadyExists) {
      if (sofaNum == 1) { // skip the test for sofaNum == 1 - this can be set "later"
        if (svd.viewCount == 0) {
          svd.viewCount = 1;
        } // else it is == or higher, so don't reset it down
      } else { // sofa is not initial sofa - is guaranteed to be set when view created
        // if (sofaNum != this.svd.viewCount + 1) {
        // System.out.println("debug");
        // }
        assert (sofaNum == svd.viewCount + 1);
        svd.viewCount = sofaNum;
      }
    }

    Sofa sofa = new Sofa(getTypeSystemImpl().sofaType, getBaseCAS(), // view for a sofa is the
                                                                     // base cas to correspond
                                                                     // to where it gets
                                                                     // indexed
            sofaNum, sofaName, mimeType);

    getBaseIndexRepository().addFS(sofa);
    svd.sofaNameSet.add(sofaName);
    if (!viewAlreadyExists) {
      getView(sofa); // create the view that goes with this Sofa
    }
    return sofa;
  }

  boolean hasView(String name) {
    return svd.sofaNameSet.contains(name);
  }

  Sofa createInitialSofa(String mimeType) {
    Sofa sofa = createSofa(1, CAS.NAME_DEFAULT_SOFA, mimeType);

    registerInitialSofa();
    mySofaRef = sofa;
    return sofa;
  }

  void registerInitialSofa() {
    svd.initialSofaCreated = true;
  }

  boolean isInitialSofaCreated() {
    return svd.initialSofaCreated;
  }

  /**
   * @deprecated
   */
  @Override
  @Deprecated
  public SofaFS getSofa(SofaID sofaID) {
    // extract absolute SofaName string from the ID
    return getSofa(sofaID.getSofaID());
  }

  private SofaFS getSofa(String sofaName) {
    FSIterator<Sofa> iterator = svd.baseCAS.getSofaIterator();
    while (iterator.hasNext()) {
      SofaFS sofa = iterator.next();
      if (sofaName.equals(sofa.getSofaID())) {
        return sofa;
      }
    }
    throw new CASRuntimeException(CASRuntimeException.SOFANAME_NOT_FOUND, sofaName);
  }

  SofaFS getSofa(int sofaRef) {
    SofaFS aSofa = (SofaFS) this.ll_getFSForRef(sofaRef);
    if (aSofa == null) {
      CASRuntimeException e = new CASRuntimeException(CASRuntimeException.SOFAREF_NOT_FOUND);
      throw e;
    }
    return aSofa;
  }

  public int ll_getSofaNum(int sofaRef) {
    return ((Sofa) getFsFromId_checked(sofaRef)).getSofaNum();
  }

  public String ll_getSofaID(int sofaRef) {
    return ((Sofa) getFsFromId_checked(sofaRef)).getSofaID();
  }

  public String ll_getSofaDataString(int sofaAddr) {
    return ((Sofa) getFsFromId_checked(sofaAddr)).getSofaString();
  }

  public CASImpl getBaseCAS() {
    return svd.baseCAS;
  }

  @Override
  public <T extends SofaFS> FSIterator<T> getSofaIterator() {
    FSIndex<T> sofaIndex = svd.baseCAS.indexRepository.<T> getIndex(CAS.SOFA_INDEX_NAME);
    return sofaIndex.iterator();
  }

  // For internal use only
  public Sofa getSofaRef() {
    if (mySofaRef == null) {
      // create the SofaFS for _InitialView ...
      // ... and reset mySofaRef to point to it
      mySofaRef = createInitialSofa(null);
    }
    return mySofaRef;
  }

  // For internal use only
  public InputStream getSofaDataStream(SofaFS aSofa) {

    Sofa sofa = (Sofa) aSofa;
    String sd = sofa.getLocalStringData();

    if (null != sd) {
      ByteArrayInputStream bis;
      bis = new ByteArrayInputStream(sd.getBytes(StandardCharsets.UTF_8));
      return bis;

    } else if (null != aSofa.getLocalFSData()) {
      TOP fs = (TOP) sofa.getLocalFSData();
      ByteBuffer buf = null;
      switch (fs._getTypeCode()) {

        case stringArrayTypeCode: {
          StringBuilder sb = new StringBuilder();
          final String[] theArray = ((StringArray) fs)._getTheArray();

          for (int i = 0; i < theArray.length; i++) {
            if (i != 0) {
              sb.append('\n');
            }
            sb.append(theArray[i]);
          }
          return new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));
        }
        case intArrayTypeCode: {
          final int[] theArray = ((IntegerArray) fs)._getTheArray();
          (buf = ByteBuffer.allocate(theArray.length * 4)).asIntBuffer().put(theArray, 0,
                  theArray.length);
          break;
        }

        case floatArrayTypeCode: {
          final float[] theArray = ((FloatArray) fs)._getTheArray();
          (buf = ByteBuffer.allocate(theArray.length * 4)).asFloatBuffer().put(theArray, 0,
                  theArray.length);
          break;
        }

        case byteArrayTypeCode: {
          final byte[] theArray = ((ByteArray) fs)._getTheArray();
          buf = ByteBuffer.wrap(theArray);
          break;
        }

        case shortArrayTypeCode: {
          final short[] theArray = ((ShortArray) fs)._getTheArray();
          (buf = ByteBuffer.allocate(theArray.length * 2)).asShortBuffer().put(theArray, 0,
                  theArray.length);
          break;
        }

        case longArrayTypeCode: {
          final long[] theArray = ((LongArray) fs)._getTheArray();
          (buf = ByteBuffer.allocate(theArray.length * 8)).asLongBuffer().put(theArray, 0,
                  theArray.length);
          break;
        }

        case doubleArrayTypeCode: {
          final double[] theArray = ((DoubleArray) fs)._getTheArray();
          (buf = ByteBuffer.allocate(theArray.length * 8)).asDoubleBuffer().put(theArray, 0,
                  theArray.length);
          break;
        }

        default:
          throw Misc.internalError();
      }

      ByteArrayInputStream bis = new ByteArrayInputStream(buf.array());
      return bis;

    } else if (null != aSofa.getSofaURI()) {
      URL url;
      try {
        url = new URL(aSofa.getSofaURI());
        return url.openStream();
      } catch (IOException exc) {
        throw new CASRuntimeException(CASRuntimeException.SOFADATASTREAM_ERROR, exc.getMessage());
      }
    }
    return null;
  }

  @Override
  public <T extends FeatureStructure> FSIterator<T> createFilteredIterator(FSIterator<T> it,
          FSMatchConstraint cons) {
    return new FilteredIterator<>(it, cons);
  }

  public TypeSystemImpl commitTypeSystem(boolean skip_loading_user_jcas) {
    TypeSystemImpl ts = getTypeSystemImpl();
    // For CAS pools, the type system could have already been committed
    // Skip the initFSClassReg if so, because it may have been updated to a JCas
    // version by another CAS processing in the pool
    // @see org.apache.uima.cas.impl.FSClassRegistry

    // avoid race: two instances of a CAS from a pool attempting to commit the
    // ts
    // at the same time
    final ClassLoader cl = getJCasClassLoader();
    synchronized (ts) {
      // //debug
      // System.out.format("debug committing ts %s classLoader %s%n", ts.hashCode(), cl);
      if (!ts.isCommitted()) {
        ts.set_skip_loading_user_jcas(skip_loading_user_jcas);
        TypeSystemImpl tsc = ts.commit(cl);
        if (tsc != ts) {
          installTypeSystemInAllViews(tsc);
          ts = tsc;
        }
      }
    }
    svd.baseGenerators = svd.generators = ts.getGeneratorsForClassLoader(cl, false); // false - not
                                                                                     // PEAR

    createIndexRepository();
    return ts;

  }

  public TypeSystemImpl commitTypeSystem() {
    return commitTypeSystem(false);
  }

  private void createIndexRepository() {
    if (!getTypeSystemMgr().isCommitted()) {
      throw new CASAdminException(CASAdminException.MUST_COMMIT_TYPE_SYSTEM);
    }
    if (indexRepository == null) {
      indexRepository = new FSIndexRepositoryImpl(this);
    }
  }

  @Override
  public FSIndexRepositoryMgr getIndexRepositoryMgr() {
    // assert(this.cas.getIndexRepository() != null);
    return indexRepository;
  }

  @Deprecated
  public void commitFS(FeatureStructure fs) {
    getIndexRepository().addFS(fs);
  }

  @Override
  public FeaturePath createFeaturePath() {
    return new FeaturePathImpl();
  }

  // Implement the ConstraintFactory interface.

  /**
   * @see org.apache.uima.cas.admin.CASMgr#getTypeSystemMgr()
   */
  @Override
  public TypeSystemMgr getTypeSystemMgr() {
    return getTypeSystemImpl();
  }

  @Override
  public void reset() {
    if (isCasLocked()) {
      throw new CASAdminException(CASAdminException.FLUSH_DISABLED);
    }
    if (this == svd.baseCAS) {
      resetNoQuestions();
      return;
    }
    // called from a CAS view.
    // clear CAS ...
    svd.baseCAS.resetNoQuestions();
  }

  public void resetNoQuestions() {
    svd.resetNoQuestions(true);
  }

  /**
   * @deprecated Use {@link #reset reset()}instead.
   */
  @Override
  @Deprecated
  public void flush() {
    reset();
  }

  @Override
  public FSIndexRepository getIndexRepository() {
    if (this == svd.baseCAS) {
      // BaseCas has no indexes for users
      return null;
    }
    if (indexRepository.isCommitted()) {
      return indexRepository;
    }
    return null;
  }

  FSIndexRepository getBaseIndexRepository() {
    if (svd.baseCAS.indexRepository.isCommitted()) {
      return svd.baseCAS.indexRepository;
    }
    return null;
  }

  FSIndexRepositoryImpl getBaseIndexRepositoryImpl() {
    return svd.baseCAS.indexRepository;
  }

  void addSofaFsToIndex(SofaFS sofa) {
    svd.baseCAS.getBaseIndexRepository().addFS(sofa);
  }

  void registerView(Sofa aSofa) {
    mySofaRef = aSofa;
  }

  /**
   * @see org.apache.uima.cas.CAS#fs2listIterator(FSIterator)
   */
  @Override
  public <T extends FeatureStructure> ListIterator<T> fs2listIterator(FSIterator<T> it) {
    // return new FSListIteratorImpl<T>(it);
    return it; // in v3, FSIterator extends listIterator
  }

  /**
   * @see org.apache.uima.cas.admin.CASMgr#getCAS()
   */
  @Override
  public CAS getCAS() {
    if (indexRepository.isCommitted()) {
      return this;
    }
    throw new CASAdminException(CASAdminException.MUST_COMMIT_INDEX_REPOSITORY);
  }

  // public void setFSClassRegistry(FSClassRegistry fsClassReg) {
  // this.svd.casMetadata.fsClassRegistry = fsClassReg;
  // }

  // JCasGen'd cover classes use this to add their generators to the class
  // registry
  // Note that this now (June 2007) a no-op for JCasGen'd generators
  // Also previously (but not now) used in JCas initialization to copy-down super generators to
  // subtypes
  // as needed
  public FSClassRegistry getFSClassRegistry() {
    return null;
    // return getTypeSystemImpl().getFSClassRegistry();
  }

  /**
   * @param fs
   *          the Feature Structure being updated
   * @param fi
   *          the Feature of fs being updated, or null if fs is an array
   * @param arrayIndexStart
   * @param nbrOfConsecutive
   */
  private void logFSUpdate(TOP fs, FeatureImpl fi, int arrayIndexStart, int nbrOfConsecutive) {

    // log the FS

    final Map<TOP, FsChange> changes = svd.modifiedPreexistingFSs;

    // create or use last FsChange element

    FsChange change = changes.computeIfAbsent(fs, key -> new FsChange(key));

    if (fi == null) {
      Misc.assertUie(arrayIndexStart >= 0);
      change.addArrayData(arrayIndexStart, nbrOfConsecutive);
    } else {
      change.addFeatData(fi.getOffset());
    }
  }

  /**
   * @param fs
   *          the Feature Structure being updated
   * @param arrayIndexStart
   * @param nbrOfConsecutive
   */
  private void logFSUpdate(TOP fs, PositiveIntSet indexesPlus1) {

    // log the FS

    final Map<TOP, FsChange> changes = svd.modifiedPreexistingFSs;

    // create or use last FsChange element

    FsChange change = changes.computeIfAbsent(fs, key -> new FsChange(key));

    change.addArrayData(indexesPlus1);
  }

  private void logFSUpdate(TOP fs, FeatureImpl fi) {
    logFSUpdate(fs, fi, -1, -1); // indicate non-array call
  }

  /**
   * This is your link from the low-level API to the high-level API. Use this method to create a
   * FeatureStructure object from an address. Note that the reverse is not supported by public APIs
   * (i.e., there is currently no way to get at the address of a FeatureStructure. Maybe we will
   * need to change that.
   * 
   * The "create" in "createFS" is a misnomer - the FS must already be created.
   * 
   * @param id
   *          The id of the feature structure to be created.
   * @param <T>
   *          The Java class associated with this feature structure
   * @return A FeatureStructure object.
   */
  public <T extends TOP> T createFS(int id) {
    return getFsFromId_checked(id);
  }

  public int getArraySize(CommonArrayFS fs) {
    return fs.size();
  }

  @Override
  public int ll_getArraySize(int id) {
    return getArraySize(getFsFromId_checked(id));
  }

  final void setWithCheckAndJournal(TOP fs, FeatureImpl fi, Runnable setter) {
    if (fs._inSetSortedIndex()) {
      boolean wasRemoved = checkForInvalidFeatureSetting(fs, fi.getCode());
      setter.run();
      if (wasRemoved) {
        maybeAddback(fs);
      }
    } else {
      setter.run();
    }

    maybeLogUpdate(fs, fi);
  }

  public final void setWithCheckAndJournal(TOP fs, int featCode, Runnable setter) {
    if (fs._inSetSortedIndex()) {
      boolean wasRemoved = checkForInvalidFeatureSetting(fs, featCode);
      setter.run();
      if (wasRemoved) {
        maybeAddback(fs);
      }
    } else {
      setter.run();
    }

    maybeLogUpdate(fs, featCode);
  }

  // public void setWithCheck(FeatureStructureImplC fs, FeatureImpl feat, Runnable setter) {
  // boolean wasRemoved = checkForInvalidFeatureSetting(fs, feat);
  // setter.run();
  // if (wasRemoved) {
  // maybeAddback(fs);
  // }
  // }

  /**
   * This method called by setters in JCas gen'd classes when the setter must check for journaling
   * 
   * @param fs
   *          -
   * @param fi
   *          -
   * @param setter
   *          -
   */
  public final void setWithJournal(FeatureStructureImplC fs, FeatureImpl fi, Runnable setter) {
    setter.run();
    maybeLogUpdate(fs, fi);
  }

  public final boolean isLoggingNeeded(FeatureStructureImplC fs) {
    return svd.trackingMark != null && !svd.trackingMark.isNew(fs._id);
  }

  /**
   * @param fs
   *          the Feature Structure being updated
   * @param feat
   *          the feature of fs being updated, or null if fs is a primitive array
   * @param i
   *          the index being updated
   */
  public final void maybeLogArrayUpdate(FeatureStructureImplC fs, FeatureImpl feat, int i) {
    if (isLoggingNeeded(fs)) {
      this.logFSUpdate((TOP) fs, feat, i, 1);
    }
  }

  /**
   * @param fs
   *          the Feature Structure being updated
   * @param indexesPlus1
   *          - a set of indexes (plus 1) that have been update
   */
  public final void maybeLogArrayUpdates(FeatureStructureImplC fs, PositiveIntSet indexesPlus1) {
    if (isLoggingNeeded(fs)) {
      this.logFSUpdate((TOP) fs, indexesPlus1);
    }
  }

  /**
   * @param fs
   *          a primitive array FS
   * @param startingIndex
   *          -
   * @param length
   *          number of consequtive items
   */
  public final void maybeLogArrayUpdates(FeatureStructureImplC fs, int startingIndex, int length) {
    if (isLoggingNeeded(fs)) {
      this.logFSUpdate((TOP) fs, null, startingIndex, length);
    }
  }

  public final void maybeLogUpdate(FeatureStructureImplC fs, FeatureImpl feat) {
    if (isLoggingNeeded(fs)) {
      this.logFSUpdate((TOP) fs, feat);
    }
  }

  public final void maybeLogUpdate(FeatureStructureImplC fs, int featCode) {
    if (isLoggingNeeded(fs)) {
      this.logFSUpdate((TOP) fs, getFeatFromCode_checked(featCode));
    }
  }

  public final boolean isLogging() {
    return svd.trackingMark != null;
  }

  // /**
  // * Common setter code for features in Feature Structures
  // *
  // * These come in two styles: one with int values, one with Object values
  // * Object values are FS or Strings or JavaObjects
  // */
  //
  // /**
  // * low level setter
  // *
  // * @param fs the feature structure
  // * @param feat the feature to set
  // * @param value -
  // */
  //
  // public void setFeatureValue(FeatureStructureImplC fs, FeatureImpl feat, int value) {
  // fs.setIntValue(feat, value);
  //// boolean wasRemoved = checkForInvalidFeatureSetting(fs, feat.getCode());
  //// fs._intData[feat.getAdjustedOffset()] = value;
  //// if (wasRemoved) {
  //// maybeAddback(fs);
  //// }
  //// maybeLogUpdate(fs, feat);
  // }

  /**
   * version for longs, uses two slots Only called from FeatureStructureImplC after determining
   * there is no local field to use Is here because of 3 calls to things in this class
   * 
   * @param fsIn
   *          the feature structure
   * @param feat
   *          the feature to set
   * @param v
   *          -
   */
  public void setLongValue(FeatureStructureImplC fsIn, FeatureImpl feat, long v) {
    TOP fs = (TOP) fsIn;
    if (fs._inSetSortedIndex()) {
      boolean wasRemoved = checkForInvalidFeatureSetting(fs, feat.getCode());
      fs._setLongValueNcNj(feat, v);
      if (wasRemoved) {
        maybeAddback(fs);
      }

    } else {
      fs._setLongValueNcNj(feat, v);
    }
    maybeLogUpdate(fs, feat);
  }

  void setFeatureValue(int fsRef, int featureCode, TOP value) {
    getFsFromId_checked(fsRef).setFeatureValue(getFeatFromCode_checked(featureCode), value);
  }

  /**
   * internal use - special setter for setting feature values, including special handling if the
   * feature is for the sofaArray, when deserializing
   * 
   * @param fs
   *          -
   * @param feat
   *          -
   * @param value
   *          -
   */
  public static void setFeatureValueMaybeSofa(TOP fs, FeatureImpl feat, TOP value) {
    if (fs instanceof Sofa) {
      assert feat.getCode() == sofaArrayFeatCode;
      ((Sofa) fs).setLocalSofaData(value);
    } else {
      fs.setFeatureValue(feat, value);
    }
  }

  /**
   * Internal use, for cases where deserializing - special case setting sofString to skip updating
   * the document annotation
   * 
   * @param fs
   *          -
   * @param feat
   *          -
   * @param s
   *          -
   */
  public static void setFeatureValueFromStringNoDocAnnotUpdate(FeatureStructureImplC fs,
          FeatureImpl feat, String s) {
    if (fs instanceof Sofa && feat.getCode() == sofaStringFeatCode) {
      ((Sofa) fs).setLocalSofaDataNoDocAnnotUpdate(s);
    } else {
      setFeatureValueFromString(fs, feat, s);
    }
  }

  /**
   * Supports setting slots to "0" for null values
   * 
   * @param fs
   *          The feature structure to update
   * @param feat
   *          the feature to update-
   * @param s
   *          the string representation of the value, could be null
   */
  public static void setFeatureValueFromString(FeatureStructureImplC fs, FeatureImpl feat,
          String s) {
    final TypeImpl range = feat.getRangeImpl();
    if (fs instanceof Sofa) {
      // sofa has special setters
      Sofa sofa = (Sofa) fs;
      switch (feat.getCode()) {
        case sofaMimeFeatCode:
          sofa.setMimeType(s);
          break;
        case sofaStringFeatCode:
          sofa.setLocalSofaData(s);
          break;
        case sofaUriFeatCode:
          sofa.setRemoteSofaURI(s);
          break;
        default: // left empty - ignore trying to set final fields
      }
      return;
    }

    if (feat.isInInt) {
      switch (range.getCode()) {
        case floatTypeCode:
          fs.setFloatValue(feat, (s == null) ? 0F : Float.parseFloat(s));
          break;
        case booleanTypeCode:
          fs.setBooleanValue(feat, (s == null) ? false : Boolean.parseBoolean(s));
          break;
        case longTypeCode:
          fs.setLongValue(feat, (s == null) ? 0L : Long.parseLong(s));
          break;
        case doubleTypeCode:
          fs.setDoubleValue(feat, (s == null) ? 0D : Double.parseDouble(s));
          break;
        case byteTypeCode:
          fs.setByteValue(feat, (s == null) ? 0 : Byte.parseByte(s));
          break;
        case shortTypeCode:
          fs.setShortValue(feat, (s == null) ? 0 : Short.parseShort(s));
          break;
        case intTypeCode:
          fs.setIntValue(feat, (s == null) ? 0 : Integer.parseInt(s));
          break;
        default:
          fs.setIntValue(feat, (s == null) ? 0 : Integer.parseInt(s));
      }
    } else if (range.isRefType) {
      if (s == null) {
        fs.setFeatureValue(feat, null);
      } else {
        // Setting a reference value "{0}" from a string is not supported.
        throw new CASRuntimeException(CASRuntimeException.SET_REF_FROM_STRING_NOT_SUPPORTED,
                feat.getName());
      }
    } else if (range.isStringOrStringSubtype()) { // includes TypeImplSubString
      // is String or Substring
      fs.setStringValue(feat, (s == null) ? null : s);
      // } else if (range == getTypeSystemImpl().javaObjectType) {
      // fs.setJavaObjectValue(feat, (s == null) ? null : deserializeJavaObject(s));
    } else {
      Misc.internalError();
    }
  }

  // private Object deserializeJavaObject(String s) {
  // throw new UnsupportedOperationException("Deserializing JavaObjects not yet implemented");
  // }
  //
  // static String serializeJavaObject(Object s) {
  // throw new UnsupportedOperationException("Serializing JavaObjects not yet implemented");
  // }

  /*
   * This should be the only place where the encoding of floats and doubles in terms of ints is
   * specified Someday we may want to preserve NAN things using "raw" versions
   */
  public static final float int2float(int i) {
    return Float.intBitsToFloat(i);
  }

  public static final int float2int(float f) {
    return Float.floatToIntBits(f);
  }

  public static final double long2double(long l) {
    return Double.longBitsToDouble(l);
  }

  public static final long double2long(double d) {
    return Double.doubleToLongBits(d);
  }

  // Type access methods.
  public final boolean isStringType(Type type) {
    return type instanceof TypeImpl_string;
  }

  public final boolean isAbstractArrayType(Type type) {
    return isArrayType(type);
  }

  public final boolean isArrayType(Type type) {
    return ((TypeImpl) type).isArray();
  }

  public final boolean isPrimitiveArrayType(Type type) {
    return (type instanceof TypeImpl_array) && !type.getComponentType().isPrimitive();
  }

  public final boolean isIntArrayType(Type type) {
    return (type == getTypeSystemImpl().intArrayType);
  }

  public final boolean isFloatArrayType(Type type) {
    return ((TypeImpl) type).getCode() == floatArrayTypeCode;
  }

  public final boolean isStringArrayType(Type type) {
    return ((TypeImpl) type).getCode() == stringArrayTypeCode;
  }

  public final boolean isBooleanArrayType(Type type) {
    return ((TypeImpl) type).getCode() == booleanArrayTypeCode;
  }

  public final boolean isByteArrayType(Type type) {
    return ((TypeImpl) type).getCode() == byteArrayTypeCode;
  }

  public final boolean isShortArrayType(Type type) {
    return ((TypeImpl) type).getCode() == byteArrayTypeCode;
  }

  public final boolean isLongArrayType(Type type) {
    return ((TypeImpl) type).getCode() == longArrayTypeCode;
  }

  public final boolean isDoubleArrayType(Type type) {
    return ((TypeImpl) type).getCode() == doubleArrayTypeCode;
  }

  public final boolean isFSArrayType(Type type) {
    return ((TypeImpl) type).getCode() == fsArrayTypeCode;
  }

  public final boolean isIntType(Type type) {
    return ((TypeImpl) type).getCode() == intTypeCode;
  }

  public final boolean isFloatType(Type type) {
    return ((TypeImpl) type).getCode() == floatTypeCode;
  }

  public final boolean isByteType(Type type) {
    return ((TypeImpl) type).getCode() == byteTypeCode;
  }

  public final boolean isBooleanType(Type type) {
    return ((TypeImpl) type).getCode() == floatTypeCode;
  }

  public final boolean isShortType(Type type) {
    return ((TypeImpl) type).getCode() == shortTypeCode;
  }

  public final boolean isLongType(Type type) {
    return ((TypeImpl) type).getCode() == longTypeCode;
  }

  public final boolean isDoubleType(Type type) {
    return ((TypeImpl) type).getCode() == doubleTypeCode;
  }

  /*
   * Only called on base CAS
   */
  /**
   * @see org.apache.uima.cas.admin.CASMgr#initCASIndexes()
   */
  @Override
  public void initCASIndexes() throws CASException {
    final TypeSystemImpl ts = getTypeSystemImpl();
    if (!ts.isCommitted()) {
      throw new CASException(CASException.MUST_COMMIT_TYPE_SYSTEM);
    }

    FSIndexComparator comp = indexRepository.createComparator();
    comp.setType(ts.sofaType);
    comp.addKey(ts.sofaNum, FSIndexComparator.STANDARD_COMPARE);
    indexRepository.createIndex(comp, CAS.SOFA_INDEX_NAME, FSIndex.BAG_INDEX);

    comp = indexRepository.createComparator();
    comp.setType(ts.annotType);
    comp.addKey(ts.startFeat, FSIndexComparator.STANDARD_COMPARE);
    comp.addKey(ts.endFeat, FSIndexComparator.REVERSE_STANDARD_COMPARE);
    comp.addKey(indexRepository.getDefaultTypeOrder(), FSIndexComparator.STANDARD_COMPARE);
    indexRepository.createIndex(comp, CAS.STD_ANNOTATION_INDEX);
  }

  // ///////////////////////////////////////////////////////////////////////////
  // CAS support ... create CAS view of aSofa

  // For internal use only
  public CAS getView(int sofaNum) {
    return svd.getViewFromSofaNbr(sofaNum);
  }

  @Override
  public CAS getCurrentView() {
    return getView(CAS.NAME_DEFAULT_SOFA);
  }

  // ///////////////////////////////////////////////////////////////////////////
  // JCas support

  @Override
  public JCas getJCas() {
    if (jcas == null) {
      jcas = JCasImpl.getJCas(this);
    }
    return jcas;
  }

  @Override
  public JCasImpl getJCasImpl() {
    if (jcas == null) {
      jcas = JCasImpl.getJCas(this);
    }
    return jcas;
  }

  // /**
  // * Internal use only
  // *
  // * @return corresponding JCas, assuming it exists
  // */
  // public JCas getExistingJCas() {
  // return this.jcas;
  // }
  //
  // Create JCas view of aSofa
  @Override
  public JCas getJCas(SofaFS aSofa) throws CASException {
    // Create base JCas, if needed
    svd.baseCAS.getJCas();

    return getView(aSofa).getJCas();
    /*
     * // If a JCas already exists for this Sofa, return it JCas aJCas = (JCas)
     * this.svd.baseCAS.sofa2jcasMap.get(Integer.valueOf(aSofa.getSofaRef())); if (null != aJCas) {
     * return aJCas; } // Get view of aSofa CASImpl view = (CASImpl) getView(aSofa); // wrap in JCas
     * aJCas = view.getJCas(); this.sofa2jcasMap.put(Integer.valueOf(aSofa.getSofaRef()), aJCas);
     * return aJCas;
     */
  }

  @Override
  @Deprecated
  public JCas getJCas(SofaID aSofaID) throws CASException {
    SofaFS sofa = getSofa(aSofaID);
    // sofa guaranteed to be non-null by above method.
    return getJCas(sofa);
  }

  // For internal platform use only
  CASImpl getInitialView() {
    return svd.getInitialView();
  }

  @Override
  public CAS createView(String aSofaID) {
    // do sofa mapping for current component
    String absoluteSofaName = null;
    if (getCurrentComponentInfo() != null) {
      absoluteSofaName = getCurrentComponentInfo().mapToSofaID(aSofaID);
    }
    if (absoluteSofaName == null) {
      absoluteSofaName = aSofaID;
    }

    // Can't use name of Initial View
    if (CAS.NAME_DEFAULT_SOFA.equals(absoluteSofaName)) {
      throw new CASRuntimeException(CASRuntimeException.SOFANAME_ALREADY_EXISTS, aSofaID);
    }
    Sofa newSofa = createSofa(absoluteSofaName, null);
    CAS newView = getView(newSofa);
    ((CASImpl) newView).registerView(newSofa);
    return newView;
  }

  @Override
  public CAS getView(String aSofaID) {
    // do sofa mapping for current component
    String absoluteSofaName = null;
    if (getCurrentComponentInfo() != null) {
      absoluteSofaName = getCurrentComponentInfo().mapToSofaID(aSofaID);
    }
    if (absoluteSofaName == null) {
      absoluteSofaName = aSofaID;
    }

    // if this resolves to the Initial View, return view(1)...
    // ... as the Sofa for this view may not exist yet
    if (CAS.NAME_DEFAULT_SOFA.equals(absoluteSofaName)) {
      return getInitialView();
    }
    // get Sofa and switch to view
    SofaFS sofa = getSofa(absoluteSofaName);
    // sofa guaranteed to be non-null by above method
    // unless sofa doesn't exist, which will cause a throw.
    return getView(sofa);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.CAS#getView(org.apache.uima.cas.SofaFS)
   * 
   * Callers of this can have created Sofas in the CAS without views: using the old deprecated
   * createSofa apis (this is being fixed so these will create the views) via deserialization, which
   * will put the sofaFSs into the CAS without creating the views, and then call this to create the
   * views. - for deserialization: there are 2 kinds: 1 is xmi the other is binary. - for xmi: there
   * is 1.4.x compatible and 2.1 compatible. The older format can have sofaNbrs in the order 2, 3,
   * 4, 1 (initial sofa), 5, 6, 7 The newer format has them in order. For deserialized sofas, we
   * insure here that there are no duplicates. This is not done in the deserializers - they use
   * either heap dumping (binary) or generic fs creators (xmi).
   * 
   * Goal is to detect case where check is needed (sofa exists, but view not yet created). This is
   * done by looking for cases where sofaNbr &gt; curViewCount. This only works if the sofaNbrs go
   * up by 1 (except for the initial sofa) in the input sequence of calls.
   */
  @Override
  public CASImpl getView(SofaFS aSofa) {
    Sofa sofa = (Sofa) aSofa;
    final int sofaNbr = sofa.getSofaRef();
    // final Integer sofaNbrInteger = Integer.valueOf(sofaNbr);

    CASImpl aView = svd.getViewFromSofaNbr(sofaNbr);
    if (null == aView) {
      // This is the deserializer case, or the case where an older API created a
      // sofa,
      // which is now creating the associated view

      // create a new CAS view
      aView = new CASImpl(svd.baseCAS, sofa);
      svd.setViewForSofaNbr(sofaNbr, aView);
      verifySofaNameUniqueIfDeserializedViewAdded(sofaNbr, sofa);
      return aView;
    }

    // for deserialization - might be reusing a view, and need to tie new Sofa
    // to old View
    if (null == aView.mySofaRef) {
      aView.mySofaRef = sofa;
    }

    verifySofaNameUniqueIfDeserializedViewAdded(sofaNbr, aSofa);
    return aView;
  }

  // boolean isSofaView(int sofaAddr) {
  // if (mySofaRef == null) {
  // // don't create initial sofa
  // return false;
  // }
  // return mySofaRef == sofaAddr;
  // }

  /*
   * for Sofas being added (determined by sofaNbr &gt; curViewCount): verify sofa name is not
   * already present, and record it for future tests
   * 
   * Only should do the name test & update in the case of deserialized new sofas coming in. These
   * will come in, in order. Exception is "_InitialView" which could come in the middle. If it comes
   * in the middle, no test will be done for duplicates, and it won't be added to set of known
   * names. This is ok because the createVIew special cases this test. Users could corrupt an xmi
   * input, which would make this logic fail.
   */
  private void verifySofaNameUniqueIfDeserializedViewAdded(int sofaNbr, SofaFS aSofa) {
    final int curViewCount = svd.viewCount;
    if (curViewCount < sofaNbr) {
      // Only true for deserialized sofas with new views being either created,
      // or
      // hooked-up from CASes that were freshly reset, which have multiple
      // views.
      // Assume sofa numbers are incrementing by 1
      assert (sofaNbr == curViewCount + 1);
      svd.viewCount = sofaNbr;
      String id = aSofa.getSofaID();
      // final Feature idFeat =
      // getTypeSystem().getFeatureByFullName(CAS.FEATURE_FULL_NAME_SOFAID);
      // String id =
      // ll_getStringValue(((FeatureStructureImpl)aSofa).getAddress(),
      // ((FeatureImpl) idFeat).getCode());
      Misc.assertUie(svd.sofaNameSet.contains(id));
      // this.svd.sofaNameSet.add(id);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelCAS#ll_getTypeSystem()
   */
  @Override
  public LowLevelTypeSystem ll_getTypeSystem() {
    return getTypeSystemImpl().getLowLevelTypeSystem();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelCAS#ll_getIndexRepository()
   */
  @Override
  public LowLevelIndexRepository ll_getIndexRepository() {
    return indexRepository;
  }

  /**
   * 
   * @param fs
   * @param domType
   * @param featCode
   */
  private final void checkLowLevelParams(TOP fs, TypeImpl domType, int featCode) {

    checkFeature(featCode);
    checkTypeHasFeature(domType, featCode);
  }

  /**
   * Check that the featCode is a feature of the domain type
   * 
   * @param domTypeCode
   * @param featCode
   */
  private final void checkTypeHasFeature(TypeImpl domainType, int featureCode) {
    checkTypeHasFeature(domainType, getFeatFromCode_checked(featureCode));
  }

  private final void checkTypeHasFeature(TypeImpl domainType, FeatureImpl feature) {
    if (!domainType.isAppropriateFeature(feature)) {
      throw new LowLevelException(LowLevelException.FEAT_DOM_ERROR, domainType.getCode(),
              domainType.getName(), feature.getCode(), feature.getName());
    }
  }

  /**
   * Check the range is appropriate for this type/feature. Throws LowLevelException if it isn't.
   * 
   * @param domType
   *          domain type
   * @param ranType
   *          range type
   * @param feat
   *          feature
   */
  public final void checkTypingConditions(Type domType, Type ranType, Feature feat) {
    TypeImpl domainTi = (TypeImpl) domType;
    FeatureImpl fi = (FeatureImpl) feat;
    checkTypeHasFeature(domainTi, fi);
    if (!((TypeImpl) fi.getRange()).subsumes((TypeImpl) ranType)) {
      throw new LowLevelException(LowLevelException.FEAT_RAN_ERROR, fi.getCode(), feat.getName(),
              ((TypeImpl) ranType).getCode(), ranType.getName());
    }
  }

  /**
   * Validate a feature's range is a ref to a feature structure
   * 
   * @param featCode
   * @throws LowLevelException
   */

  private final void checkFsRan(FeatureImpl fi) throws LowLevelException {
    if (!fi.getRangeImpl().isRefType) {
      throw new LowLevelException(LowLevelException.FS_RAN_TYPE_ERROR, fi.getCode(), fi.getName(),
              fi.getRange().getName());
    }
  }

  private final void checkFeature(int featureCode) {
    if (!getTypeSystemImpl().isFeature(featureCode)) {
      throw new LowLevelException(LowLevelException.INVALID_FEATURE_CODE, featureCode);
    }
  }

  private TypeImpl getTypeFromCode(int typeCode) {
    return getTypeSystemImpl().getTypeForCode(typeCode);
  }

  private TypeImpl getTypeFromCode_checked(int typeCode) {
    return getTypeSystemImpl().getTypeForCode_checked(typeCode);
  }

  private FeatureImpl getFeatFromCode_checked(int featureCode) {
    return getTypeSystemImpl().getFeatureForCode_checked(featureCode);
  }

  public final <T extends TOP> T getFsFromId_checked(int fsRef) {
    T r = getFsFromId(fsRef);
    if (r == null) {
      if (fsRef == 0) {
        return null;
      }
      LowLevelException e = new LowLevelException(LowLevelException.INVALID_FS_REF, fsRef);
      // this form to enable seeing this even if the
      // throwable is silently handled.
      // System.err.println("debug " + e);
      throw e;
    }
    return r;
  }

  @Override
  public final boolean ll_isRefType(int typeCode) {
    return getTypeFromCode(typeCode).isRefType;
  }

  @Override
  public final int ll_getTypeClass(int typeCode) {
    return TypeSystemImpl.getTypeClass(getTypeFromCode(typeCode));
  }

  // backwards compatibility only
  @Override
  public final int ll_createFS(int typeCode) {
    return ll_createFS(typeCode, true);
  }

  @Override
  public final int ll_createFS(int typeCode, boolean doCheck) {
    TypeImpl ti = (TypeImpl) getTypeSystemImpl().ll_getTypeForCode(typeCode);
    if (doCheck) {
      if (ti == null || !ti.isCreatableAndNotBuiltinArray()) {
        throw new LowLevelException(LowLevelException.CREATE_FS_OF_TYPE_ERROR, typeCode);
      }
    }
    TOP fs = (TOP) createFS(ti);
    if (!fs._isPearTrampoline()) {
      setId2FsMaybeUnconditionally(fs);
    }
    return fs._id;
  }

  /**
   * used for ll_setIntValue which changes type code
   * 
   * @param ti
   *          - the type of the created FS
   * @param id
   *          - the id to use
   * @return the FS
   */
  private TOP createFsWithExistingId(TypeImpl ti, int id) {
    svd.reuseId = id;
    try {
      TOP fs = createFS(ti);
      svd.id2fs.putChange(id, fs);
      return fs;
    } finally {
      svd.reuseId = 0;
    }
  }

  /*
   * /**
   * 
   * @param arrayLength
   * 
   * @return the id of the created array
   * 
   * @see org.apache.uima.cas.impl.LowLevelCAS#ll_createArray(int, int)
   */
  @Override
  public int ll_createArray(int typeCode, int arrayLength) {
    TOP fs = createArray(getTypeFromCode_checked(typeCode), arrayLength);
    setId2FsMaybeUnconditionally(fs);
    return fs._id;
  }

  /**
   * (for backwards compatibility with V2 CASImpl) Create a temporary (i.e., per document) array FS
   * on the heap.
   * 
   * @param type
   *          The type code of the array to be created.
   * @param len
   *          The length of the array to be created.
   * @return -
   * @exception ArrayIndexOutOfBoundsException
   *              If <code>type</code> is not a type.
   */
  public int createTempArray(int type, int len) {
    return ll_createArray(type, len);
  }

  /**
   * @param arrayLength
   *          -
   * @return the id of the created array
   */
  @Override
  public int ll_createByteArray(int arrayLength) {
    TOP fs = createArray(getTypeSystemImpl().byteArrayType, arrayLength);
    set_id2fs(fs);
    return fs._id;
  }

  /**
   * @param arrayLength
   *          -
   * @return the id of the created array
   */
  @Override
  public int ll_createBooleanArray(int arrayLength) {
    TOP fs = createArray(getTypeSystemImpl().booleanArrayType, arrayLength);
    set_id2fs(fs);
    return fs._id;
  }

  /**
   * @param arrayLength
   *          -
   * @return the id of the created array
   */
  @Override
  public int ll_createShortArray(int arrayLength) {
    TOP fs = createArray(getTypeSystemImpl().shortArrayType, arrayLength);
    set_id2fs(fs);
    return fs._id;
  }

  /**
   * @param arrayLength
   *          -
   * @return the id of the created array
   */
  @Override
  public int ll_createLongArray(int arrayLength) {
    TOP fs = createArray(getTypeSystemImpl().longArrayType, arrayLength);
    set_id2fs(fs);
    return fs._id;
  }

  /**
   * @param arrayLength
   *          -
   * @return the id of the created array
   */
  @Override
  public int ll_createDoubleArray(int arrayLength) {
    TOP fs = createArray(getTypeSystemImpl().doubleArrayType, arrayLength);
    set_id2fs(fs);
    return fs._id;
  }

  /**
   * @param arrayLength
   *          -
   * @return the id of the created array
   */
  @Override
  public int ll_createArray(int typeCode, int arrayLength, boolean doChecks) {
    TypeImpl ti = getTypeFromCode_checked(typeCode);
    if (doChecks) {
      if (!ti.isArray()) {
        throw new LowLevelException(LowLevelException.CREATE_ARRAY_OF_TYPE_ERROR, typeCode,
                ti.getName());
      }
      if (arrayLength < 0) {
        throw new LowLevelException(LowLevelException.ILLEGAL_ARRAY_LENGTH, arrayLength);
      }
    }
    TOP fs = createArray(ti, arrayLength);
    set_id2fs(fs);
    return fs._id;
  }

  public void validateArraySize(int length) {
    if (length < 0) {
      /** Array size must be &gt;= 0. */
      throw new CASRuntimeException(CASRuntimeException.ILLEGAL_ARRAY_SIZE);
    }
  }

  /**
   * Safety - any time the low level API to a FS is requested, hold on to that FS until CAS reset to
   * mimic how v2 works.
   */
  @Override
  public final int ll_getFSRef(FeatureStructure fs) {
    if (null == fs) {
      return NULL;
    }
    TOP fst = (TOP) fs;
    if (fst._isPearTrampoline()) {
      return fst._id; // no need to hold on to this one - it's in jcas hash maps
    }
    // uncond. because this method can be called multiple times
    svd.id2fs.putUnconditionally(fst); // hold on to it
    return ((FeatureStructureImplC) fs)._id;
  }

  @Override
  public <T extends TOP> T ll_getFSForRef(int id) {
    return getFsFromId_checked(id);
  }

  // @formatter:off
  /**
   * Handle some unusual backwards compatibility cases
   *   featureCode = 0 - implies getting the type code
   *   feature range is int - normal
   *   feature range is a fs reference, return the id 
   *   feature range is a string: add the string if not already present to the string heap, return the int handle.
   * @param fsRef -
   * @param featureCode -
   * @return -
   */
  // @formatter:on
  @Override
  public final int ll_getIntValue(int fsRef, int featureCode) {
    TOP fs = getFsFromId_checked(fsRef);
    if (featureCode == 0) {
      return fs._getTypeImpl().getCode(); // case where the type is being requested
    }
    FeatureImpl fi = getFeatFromCode_checked(featureCode);

    SlotKind kind = fi.getSlotKind();
    switch (kind) {
      case Slot_HeapRef:
        return fs.getFeatureValue(fi)._id;

      case Slot_Boolean:
      case Slot_Byte:
      case Slot_Short:
      case Slot_Int:
      case Slot_Float:
        return fs._getIntValueNc(fi);

      case Slot_StrRef:
        return getCodeForString(fs._getStringValueNc(fi));

      case Slot_LongRef:
        return getCodeForLong(fs._getLongValueNc(fi));
      case Slot_DoubleRef:
        return getCodeForLong(CASImpl.double2long(fs._getDoubleValueNc(fi)));

      default:
        throw new CASRuntimeException(CASRuntimeException.INAPPROP_RANGE, fi.getName(), "int",
                fi.getRange().getName());
    }
  }

  // public final int ll_getIntValueFeatOffset(int fsRef, int featureOffset) {
  // return ll_getFSForRef(fsRef)._intData[featureOffset];
  // }

  @Override
  public final float ll_getFloatValue(int fsRef, int featureCode) {
    return getFsFromId_checked(fsRef).getFloatValue(getFeatFromCode_checked(featureCode));
  }

  @Override
  public final String ll_getStringValue(int fsRef, int featureCode) {
    return getFsFromId_checked(fsRef).getStringValue(getFeatFromCode_checked(featureCode));
  }

  // public final String ll_getStringValueFeatOffset(int fsRef, int featureOffset) {
  // return (String) getFsFromId_checked(fsRef)._refData[featureOffset];
  // }

  @Override
  public final int ll_getRefValue(int fsRef, int featureCode) {
    return getFsFromId_checked(fsRef).getFeatureValue(getFeatFromCode_checked(featureCode))._id();
  }

  // public final int ll_getRefValueFeatOffset(int fsRef, int featureOffset) {
  // return ((FeatureStructureImplC)getFsFromId_checked(fsRef)._refData[featureOffset]).id();
  // }

  @Override
  public final int ll_getIntValue(int fsRef, int featureCode, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, featureCode);
    }
    return ll_getIntValue(fsRef, featureCode);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelCAS#ll_getFloatValue(int, int, boolean)
   */
  @Override
  public final float ll_getFloatValue(int fsRef, int featureCode, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, featureCode);
    }
    return ll_getFloatValue(fsRef, featureCode);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelCAS#ll_getStringValue(int, int, boolean)
   */
  @Override
  public final String ll_getStringValue(int fsRef, int featureCode, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, featureCode);
    }
    return ll_getStringValue(fsRef, featureCode);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelCAS#ll_getRefValue(int, int, boolean)
   */
  @Override
  public final int ll_getRefValue(int fsRef, int featureCode, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkFsRefConditions(fsRef, featureCode);
    }
    return getFsFromId_checked(fsRef).getFeatureValue(getFeatFromCode_checked(featureCode))._id();
  }

  /**
   * This is the method all normal FS feature "setters" call before doing the set operation on
   * values where the range could be used as an index key.
   * <p>
   * If enabled, it will check if the update may corrupt any index in any view. The check tests
   * whether the feature is being used as a key in one or more indexes and if the FS is in one or
   * more corruptable view indexes.
   * <p>
   * If true, then:
   * <ul>
   * <li>it may remove and remember (for later adding-back) the FS from all corruptable indexes (bag
   * indexes are not corruptable via updating, so these are skipped). The addback occurs later
   * either via an explicit call to do so, or the end of a protectIndex block, or. (if
   * autoIndexProtect is enabled) after the individual feature update is completed.</li>
   * <li>it may give a WARN level message to the log. This enables users to implement their own
   * optimized handling of this for "high performance" applications which do not want the overhead
   * of runtime checking.</li>
   * </ul>
   * <p>
   * 
   * @param fs
   *          - the FS to test if it is in the indexes
   * @param featCode
   *          - the feature being tested
   * @return true if something may need to be added back
   */
  private boolean checkForInvalidFeatureSetting(TOP fs, int featCode) {
    if (doInvalidFeatSettingCheck(fs)) {
      if (!svd.featureCodesInIndexKeys.get(featCode)) { // skip if no index uses this feature
        return false;
      }

      boolean wasRemoved = checkForInvalidFeatureSetting2(fs);
      if (wasRemoved && doCorruptReport()) {
        featModWhileInIndexReport(fs, featCode);
      }
      return wasRemoved;
    }
    return false;
  }

  /**
   * version for deserializers, and for set document language, using their own store for toBeAdded
   * Doesn't report updating of corruptable slots.
   * 
   * @param fs
   *          -
   * @param featCode
   *          -
   * @param toBeAdded
   *          -
   * @return -
   */
  boolean checkForInvalidFeatureSetting(TOP fs, int featCode, FSsTobeAddedback toBeAdded) {
    if (doInvalidFeatSettingCheck(fs)) {
      if (!svd.featureCodesInIndexKeys.get(featCode)) { // skip if no index uses this feature
        return false;
      }

      boolean wasRemoved = removeFromCorruptableIndexAnyView(fs, toBeAdded);
      // if (wasRemoved && doCorruptReport()) {
      // featModWhileInIndexReport(fs, featCode);
      // }
      return wasRemoved;
    }
    return false;
  }

  /**
   * version for deserializers, using their own store for toBeAdded and not bothering to check for
   * particular features Doesn't report updating of corruptable slots.
   * 
   * @param fs
   *          -
   * @param featCode
   *          -
   * @param toBeAdded
   *          -
   * @return -
   */
  boolean checkForInvalidFeatureSetting(TOP fs, FSsTobeAddedback toBeAdded) {
    if (doInvalidFeatSettingCheck(fs)) {

      boolean wasRemoved = removeFromCorruptableIndexAnyView(fs, toBeAdded);
      // if (wasRemoved && doCorruptReport()) {
      // featModWhileInIndexReport(fs, null);
      // }
      return wasRemoved;
    }
    return false;
  }

  // // version of above, but using jcasFieldRegistryIndex
  // private boolean checkForInvalidFeatureSettingJFRI(TOP fs, int jcasFieldRegistryIndex) {
  // if (doInvalidFeatSettingCheck(fs) &&
  // svd.featureJiInIndexKeys.get(jcasFieldRegistryIndex)) {
  //
  // boolean wasRemoved = checkForInvalidFeatureSetting2(fs);
  //
  //// if (wasRemoved && doCorruptReport()) {
  //// featModWhileInIndexReport(fs, getFeatFromRegistry(jcasFieldRegistryIndex));
  //// }
  // return wasRemoved;
  // }
  // return false;
  // }

  private boolean checkForInvalidFeatureSetting2(TOP fs) {
    final int ssz = svd.fssTobeAddedback.size();

    // next method skips if the fsRef is not in the index (cache)
    boolean wasRemoved = removeFromCorruptableIndexAnyView(fs, (ssz > 0) ? getAddback(ssz) : // validates
                                                                                             // single
                                                                                             // not
                                                                                             // in
                                                                                             // use
            getAddbackSingle() // validates single usage at a time
    );
    if (!wasRemoved && svd.fsTobeAddedbackSingleInUse) {
      svd.fsTobeAddedbackSingleInUse = false;
    }
    return wasRemoved;
  }

  // public FeatureImpl getFeatFromRegistry(int jcasFieldRegistryIndex) {
  // return getFSClassRegistry().featuresFromJFRI[jcasFieldRegistryIndex];
  // }

  private boolean doCorruptReport() {
    return
    // skip message if wasn't removed
    // skip message if protected in explicit block
    IS_REPORT_FS_UPDATE_CORRUPTS_INDEX && svd.fssTobeAddedback.isEmpty();
  }

  /**
   * 
   * @param fs
   *          -
   * @return false if the fs is not in a set or sorted index (bit in fs), or the auto protect is
   *         disabled and we're not in an explicit protect block
   */
  private boolean doInvalidFeatSettingCheck(TOP fs) {
    if (!fs._inSetSortedIndex()) {
      return false;
    }

    final int ssz = svd.fssTobeAddedback.size();
    // skip if protection is disabled, and no explicit protection block
    if (IS_DISABLED_PROTECT_INDEXES && ssz == 0) {
      return false;
    }
    return true;
  }

  private void featModWhileInIndexReport(FeatureStructure fs, int featCode) {
    featModWhileInIndexReport(fs, getFeatFromCode_checked(featCode));
  }

  private void featModWhileInIndexReport(FeatureStructure fs, FeatureImpl fi) {
    // prepare a message which includes the feature which is a key, the fs, and
    // the call stack.
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    new Throwable().printStackTrace(pw);
    pw.close();
    String msg = String.format(
            "While FS was in the index, the feature \"%s\""
                    + ", which is used as a key in one or more indexes, "
                    + "was modified\n FS = \"%s\"\n%s%n",
            (fi == null) ? "for-all-features" : fi.getName(), fs.toString(), sw.toString());
    UIMAFramework.getLogger().log(Level.WARNING, msg);

    if (IS_THROW_EXCEPTION_CORRUPT_INDEX) {
      throw new UIMARuntimeException(UIMARuntimeException.ILLEGAL_FS_FEAT_UPDATE, new Object[] {});
    }
  }

  // @formatter:off
  /**
   * Only called if there was something removed that needs to be added back
   * 
   * skip the addback (to defer it until later) if:
   *   - running in block mode (you can tell this if svd.fssTobeAddedback.size() &gt; 0) or
   * if running in block mode, the add back is delayed until the end of the block
   * 
   * @param fs
   *          the fs to add back
   */
  // @formatter:on
  public void maybeAddback(TOP fs) {
    if (svd.fssTobeAddedback.size() == 0) {
      assert (svd.fsTobeAddedbackSingleInUse);
      svd.fsTobeAddedbackSingle.addback(fs);
      svd.fsTobeAddedbackSingleInUse = false;
    }
  }

  boolean removeFromCorruptableIndexAnyView(final TOP fs, FSsTobeAddedback toBeAdded) {
    return removeFromIndexAnyView(fs, toBeAdded, FSIndexRepositoryImpl.SKIP_BAG_INDEXES);
  }

  /**
   * This might be called from low level set int value, if we support switching types, and we want
   * to remove the old type from all indexes.
   * 
   * @param fs
   *          the fs to maybe remove
   * @param toBeAdded
   *          a place to record the removal so we can add it back later
   * @param isSkipBagIndexes
   *          is true usually, we don't need to remove/readd to bag indexes (except for the case of
   *          supporting switching types via low level set int for v2 backwards compatibility)
   * @return true if was removed from one or more indexes
   */
  boolean removeFromIndexAnyView(final TOP fs, FSsTobeAddedback toBeAdded,
          boolean isSkipBagIndexes) {
    final TypeImpl ti = ((FeatureStructureImplC) fs)._getTypeImpl();
    if (ti.isAnnotationBaseType()) {
      boolean r = removeAndRecord(fs, (FSIndexRepositoryImpl) fs._casView.getIndexRepository(),
              toBeAdded, isSkipBagIndexes);
      fs._resetInSetSortedIndex();
      return r;
    }

    // not a subtype of AnnotationBase, need to check all views (except base)
    // sofas indexed in the base view are not corruptable.

    final Iterator<CAS> viewIterator = getViewIterator();
    boolean wasRemoved = false;
    while (viewIterator.hasNext()) {
      wasRemoved |= removeAndRecord(fs,
              (FSIndexRepositoryImpl) viewIterator.next().getIndexRepository(), toBeAdded,
              isSkipBagIndexes);
    }
    fs._resetInSetSortedIndex();
    return wasRemoved;
  }

  /**
   * remove a FS from all indexes in this view (except bag indexes, if isSkipBagIndex is true)
   * 
   * @param fs
   *          the fs to be removed
   * @param ir
   *          the view
   * @param toBeAdded
   *          the place to record how many times it was in the index, per view
   * @param isSkipBagIndex
   *          set to true for corruptable removes, false for remove in all cases from all indexes
   * @return true if it was removed, false if it wasn't in any corruptable index.
   */
  private boolean removeAndRecord(TOP fs, FSIndexRepositoryImpl ir, FSsTobeAddedback toBeAdded,
          boolean isSkipBagIndex) {
    boolean wasRemoved = ir.removeFS_ret(fs, isSkipBagIndex);
    if (wasRemoved) {
      toBeAdded.recordRemove(fs, ir, 1);
    }
    return wasRemoved;
  }

  // @formatter:off
  /**
   * Special considerations:
   *   Interface with corruption checking
   *   For backwards compatibility:
   *     handle cases where feature is:
   *       int - normal
   *       0 - change type code
   *       a ref: treat int as FS "addr"
   *       not an int: handle like v2 where reasonable
   */
  // @formatter:on
  @Override
  public final void ll_setIntValue(int fsRef, int featureCode, int value) {
    TOP fs = getFsFromId_checked(fsRef);

    if (featureCode == 0) {
      switchFsType(fs, value);
      return;
    }

    FeatureImpl fi = getFeatFromCode_checked(featureCode);

    if (fs._getTypeImpl().isArray()) {
      throw new UnsupportedOperationException(
              "ll_setIntValue not permitted to set a feature of an array");
    }
    SlotKind kind = fi.getSlotKind();

    switch (kind) {
      case Slot_HeapRef:
        if (fi.getCode() == annotBaseSofaFeatCode) {
          // setting the sofa ref of an annotationBase
          // can't change this so just verify it's the same
          TOP sofa = fs.getFeatureValue(fi);
          if (sofa._id != value) {
            throw new UnsupportedOperationException(
                    "ll_setIntValue not permitted to change a sofaRef feature");
          }
          return; // if the same, just ignore, already set
        }

        TOP ref = fs._casView.getFsFromId_checked(value);
        fs.setFeatureValue(fi, ref); // does the right feature check, too
        return;

      case Slot_Boolean:
      case Slot_Byte:
      case Slot_Short:
      case Slot_Int:
      case Slot_Float:
        fs._setIntValueCJ(fi, value);
        break;

      case Slot_StrRef:
        String s = getStringForCode(value);
        if (s == null && value != 0) {
          Misc.internalError(
                  new Exception("ll_setIntValue got null string for non-0 handle: " + value));
        }
        fs._setRefValueNfcCJ(fi, getStringForCode(value));
        break;

      case Slot_LongRef:
      case Slot_DoubleRef:
        Long lng = getLongForCode(value);
        if (lng == null) {
          Misc.internalError(
                  new Exception("ll_setIntValue got null Long/Double for handle: " + value));
        }
        fs._setLongValueNfcCJ(fi, lng);
        break;

      default:
        CASRuntimeException e = new CASRuntimeException(CASRuntimeException.INAPPROP_RANGE,
                fi.getName(), "int", fi.getRange().getName());
        // System.err.println("debug " + e);
        throw e;
    }
  }

  private String getStringForCode(int i) {
    if (null == svd.llstringSet) {
      return null;
    }
    return svd.llstringSet.getStringForCode(i);
  }

  private int getCodeForString(String s) {
    if (null == svd.llstringSet) {
      svd.llstringSet = new StringSet();
    }
    return svd.llstringSet.getCodeForString(s); // avoids adding duplicates
  }

  private Long getLongForCode(int i) {
    if (null == svd.lllongSet) {
      return null;
    }
    return svd.lllongSet.getLongForCode(i);
  }

  private int getCodeForLong(long s) {
    if (null == svd.lllongSet) {
      svd.lllongSet = new LongSet();
    }
    return svd.lllongSet.getCodeForLong(s); // avoids adding duplicates
  }

  private void switchFsType(TOP fs, int aTypeCode) {
    // throw new UnsupportedOperationException();
    // case where the type is being changed
    // if the new type is a sub/super type of the existing type,
    // some field data may be copied
    // if not, no data is copied.
    //
    // Item is removed from index and re-indexed

    // to emulate what V2 did,
    // the indexing didn't change
    // all the slots were the same

    var wasRemoved = removeFromIndexAnyView(fs, getAddbackSingle(), INCLUDE_BAG_INDEXES);
    if (!wasRemoved) {
      svd.fsTobeAddedbackSingleInUse = false;
    }

    var newType = getTypeFromCode_checked(aTypeCode);
    var newClass = newType.getJavaClass();
    if ((fs instanceof UimaSerializable) || UimaSerializable.class.isAssignableFrom(newClass)) {
      // REC 2024-08-22: I wonder if it would be valid to switch from a UimaSerializable to another
      // UimaSerializable - currently this seems to be forbidden.
      throw new UnsupportedOperationException("can't switch type to/from UimaSerializable");
    }

    // Measurement - record which type gets switched to which other type count how many times
    // record which JCas cover class goes with each type
    // key = old type, new type, old jcas cover class, new jcas cover class
    // value = count
    MeasureSwitchType mst = null;
    if (MEASURE_SETINT) {
      var key = new MeasureSwitchType(fs._getTypeImpl(), newType);
      synchronized (measureSwitches) { // map access / updating must be synchronized
        mst = measureSwitches.get(key);
        if (null == mst) {
          measureSwitches.put(key, key);
          mst = key;
        }
        mst.count++;
        mst.newSubsumesOld = newType.subsumes(fs._getTypeImpl());
        mst.oldSubsumesNew = fs._getTypeImpl().subsumes(newType);
      }
    }

    if (newClass == fs._getTypeImpl().getJavaClass() || newType.subsumes(fs._getTypeImpl())) {
      // switch in place
      fs._setTypeImpl(newType);
      return;
    }

    // if types don't subsume each other, we
    // deviate a bit from V2 behavior
    // and skip copying the feature slots
    boolean isOkToCopyFeatures = // true || // debug
            fs._getTypeImpl().subsumes(newType) || newType.subsumes(fs._getTypeImpl());
    // throw new CASRuntimeException(CASRuntimeException.ILLEGAL_TYPE_CHANGE, newType.getName(),
    // fs._getTypeImpl().getName());
    TOP newFs = createFsWithExistingId(newType, fs._id); // updates id -> fs map
    // initialize fields:
    if (isOkToCopyFeatures) {
      newFs._copyIntAndRefArraysFrom(fs);
    }

    // if (wasRemoved) {
    // addbackSingle(newFs);
    // }

    // replace refs in existing FSs with new
    // will miss any fs's held by user code - no way to fix that without
    // universal indirection - very inefficient, so just accept for now
    long st = System.nanoTime();
    walkReachablePlusFSsSorted(fsItem -> {
      if (fsItem._getTypeImpl().hasRefFeature) {
        if (fsItem instanceof FSArray) {
          TOP[] a = ((FSArray) fsItem)._getTheArray();
          for (int i = 0; i < a.length; i++) {
            if (fs == a[i]) {
              a[i] = newFs;
            }
          }
          return;
        }

        final int sz = fsItem._getTypeImpl().nbrOfUsedRefDataSlots;
        for (int i = 0; i < sz; i++) {
          Object o = fsItem._getRefValueCommon(i);
          if (o == fs) {
            fsItem._setRefValueCommon(i, newFs);
            // fsItem._refData[i] = newFs;
          }
        }
      }
    }, null, // mark
            null, // null or predicate for filtering what gets added
            null); // null or type mapper, skips if not in other ts

    if (MEASURE_SETINT) {
      mst.scantime += System.nanoTime() - st;
    }
  }

  @Override
  public final void ll_setFloatValue(int fsRef, int featureCode, float value) {
    getFsFromId_checked(fsRef).setFloatValue(getFeatFromCode_checked(featureCode), value);
  }

  // public final void ll_setFloatValueNoIndexCorruptionCheck(int fsRef, int featureCode, float
  // value) {
  // setFeatureValueNoIndexCorruptionCheck(fsRef, featureCode, float2int(value));
  // }

  @Override
  public final void ll_setStringValue(int fsRef, int featureCode, String value) {
    getFsFromId_checked(fsRef).setStringValue(getFeatFromCode_checked(featureCode), value);
  }

  @Override
  public final void ll_setRefValue(int fsRef, int featureCode, int value) {
    // no index check because refs can't be keys
    setFeatureValue(fsRef, featureCode, getFsFromId_checked(value));
  }

  @Override
  public final void ll_setIntValue(int fsRef, int featureCode, int value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, featureCode);
    }
    ll_setIntValue(fsRef, featureCode, value);
  }

  @Override
  public final void ll_setFloatValue(int fsRef, int featureCode, float value,
          boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, featureCode);
    }
    ll_setFloatValue(fsRef, featureCode, value);
  }

  @Override
  public final void ll_setStringValue(int fsRef, int featureCode, String value,
          boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, featureCode);
    }
    ll_setStringValue(fsRef, featureCode, value);
  }

  @Override
  public final void ll_setCharBufferValue(int fsRef, int featureCode, char[] buffer, int start,
          int length, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, featureCode);
    }
    ll_setCharBufferValue(fsRef, featureCode, buffer, start, length);
  }

  @Override
  public final void ll_setCharBufferValue(int fsRef, int featureCode, char[] buffer, int start,
          int length) {
    ll_setStringValue(fsRef, featureCode, new String(buffer, start, length));
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelCAS#ll_copyCharBufferValue(int, int, char, int)
   */
  @Override
  public int ll_copyCharBufferValue(int fsRef, int featureCode, char[] buffer, int start) {
    String str = ll_getStringValue(fsRef, featureCode);
    if (str == null) {
      return -1;
    }

    final int len = str.length();
    final int requestedMax = start + len;
    // Check that the buffer is long enough to copy the whole string. If it isn't long enough, we
    // copy up to buffer.length - start characters.
    final int max = (buffer.length < requestedMax) ? (buffer.length - start) : len;
    for (int i = 0; i < max; i++) {
      buffer[start + i] = str.charAt(i);
    }
    return len;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelCAS#ll_getCharBufferValueSize(int, int)
   */
  @Override
  public int ll_getCharBufferValueSize(int fsRef, int featureCode) {
    String str = ll_getStringValue(fsRef, featureCode);
    return str.length();
  }

  @Override
  public final void ll_setRefValue(int fsRef, int featureCode, int value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkFsRefConditions(fsRef, featureCode);
    }
    ll_setRefValue(fsRef, featureCode, value);
  }

  public final int getIntArrayValue(IntegerArray array, int i) {
    return array.get(i);
  }

  public final float getFloatArrayValue(FloatArray array, int i) {
    return array.get(i);
  }

  public final String getStringArrayValue(StringArray array, int i) {
    return array.get(i);
  }

  public final FeatureStructure getRefArrayValue(FSArray array, int i) {
    return array.get(i);
  }

  @Override
  public final int ll_getIntArrayValue(int fsRef, int position) {
    return getIntArrayValue(((IntegerArray) getFsFromId_checked(fsRef)), position);
  }

  @Override
  public final float ll_getFloatArrayValue(int fsRef, int position) {
    return getFloatArrayValue(((FloatArray) getFsFromId_checked(fsRef)), position);
  }

  @Override
  public final String ll_getStringArrayValue(int fsRef, int position) {
    return getStringArrayValue(((StringArray) getFsFromId_checked(fsRef)), position);
  }

  @Override
  public final int ll_getRefArrayValue(int fsRef, int position) {
    return ((TOP) getRefArrayValue(((FSArray) getFsFromId_checked(fsRef)), position))._id();
  }

  private void throwAccessTypeError(int fsRef, int typeCode) {
    throw new LowLevelException(LowLevelException.ACCESS_TYPE_ERROR, fsRef, typeCode,
            getTypeSystemImpl().ll_getTypeForCode(typeCode).getName(),
            getTypeSystemImpl().ll_getTypeForCode(ll_getFSRefType(fsRef)).getName());
  }

  public final void checkArrayBounds(int fsRef, int pos) {
    final int arrayLength = ll_getArraySize(fsRef);
    if ((pos < 0) || (pos >= arrayLength)) {
      throw new ArrayIndexOutOfBoundsException(pos);
      // LowLevelException e = new LowLevelException(
      // LowLevelException.ARRAY_INDEX_OUT_OF_RANGE);
      // e.addArgument(Integer.toString(pos));
      // throw e;
    }
  }

  public final void checkArrayBounds(int arrayLength, int pos, int length) {
    if ((pos < 0) || (length < 0) || ((pos + length) > arrayLength)) {
      throw new LowLevelException(LowLevelException.ARRAY_INDEX_LENGTH_OUT_OF_RANGE,
              Integer.toString(pos), Integer.toString(length));
    }
  }

  // @formatter:off
  /**
   * Check that the fsRef is valid.
   * Check that the fs is featureCode belongs to the fs 
   * Check that the featureCode is one of the features of the domain type of the fsRef
   * feat could be primitive, string, ref to another feature
   * 
   * @param fsRef
   * @param typeCode
   * @param featureCode
   */
  // @formatter:on
  private final void checkNonArrayConditions(int fsRef, int featureCode) {
    TOP fs = getFsFromId_checked(fsRef);

    final TypeImpl domainType = (TypeImpl) fs.getType();

    // checkTypeAt(domType, fs); // since the type is from the FS, it's always OK
    checkFeature(featureCode); // checks that the featureCode is in the range of all feature codes

    TypeSystemImpl tsi = getTypeSystemImpl();
    FeatureImpl fi = tsi.getFeatureForCode_checked(featureCode);
    checkTypeHasFeature(domainType, fi); // checks that the feature code is one of the features of
                                         // the type

    // checkFsRan(fi);
  }

  private final void checkFsRefConditions(int fsRef, int featureCode) {
    TOP fs = getFsFromId_checked(fsRef);
    checkLowLevelParams(fs, fs._getTypeImpl(), featureCode); // checks type has feature

    TypeSystemImpl tsi = getTypeSystemImpl();
    FeatureImpl fi = tsi.getFeatureForCode_checked(featureCode);
    checkFsRan(fi);

    // next not needed because checkFsRan already validates this
    // checkFsRef(fsRef + this.svd.casMetadata.featureOffset[featureCode]);
  }

  // private final void checkArrayConditions(int fsRef, int typeCode,
  // int position) {
  // checkTypeSubsumptionAt(fsRef, typeCode);
  // // skip this next test because
  // // a) it's done implicitly in the bounds check and
  // // b) it fails for arrays stored outside of the main heap (e.g.,
  // byteArrays, etc.)
  // // checkFsRef(getArrayStartAddress(fsRef) + position);
  // checkArrayBounds(fsRef, position);
  // }

  private final void checkPrimitiveArrayConditions(int fsRef, int typeCode, int position) {
    if (typeCode != ll_getFSRefType(fsRef)) {
      throwAccessTypeError(fsRef, typeCode);
    }
    checkArrayBounds(fsRef, position);
  }

  @Override
  public final int ll_getIntArrayValue(int fsRef, int position, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkPrimitiveArrayConditions(fsRef, intArrayTypeCode, position);
    }
    return ll_getIntArrayValue(fsRef, position);
  }

  @Override
  public float ll_getFloatArrayValue(int fsRef, int position, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkPrimitiveArrayConditions(fsRef, floatArrayTypeCode, position);
    }
    return ll_getFloatArrayValue(fsRef, position);
  }

  @Override
  public String ll_getStringArrayValue(int fsRef, int position, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkPrimitiveArrayConditions(fsRef, stringArrayTypeCode, position);
    }
    return ll_getStringArrayValue(fsRef, position);
  }

  @Override
  public int ll_getRefArrayValue(int fsRef, int position, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkPrimitiveArrayConditions(fsRef, fsArrayTypeCode, position);
    }
    return ll_getRefArrayValue(fsRef, position);
  }

  @Override
  public void ll_setIntArrayValue(int fsRef, int position, int value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkPrimitiveArrayConditions(fsRef, intArrayTypeCode, position);
    }
    ll_setIntArrayValue(fsRef, position, value);
  }

  @Override
  public void ll_setFloatArrayValue(int fsRef, int position, float value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkPrimitiveArrayConditions(fsRef, floatArrayTypeCode, position);
    }
    ll_setFloatArrayValue(fsRef, position, value);
  }

  @Override
  public void ll_setStringArrayValue(int fsRef, int position, String value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkPrimitiveArrayConditions(fsRef, stringArrayTypeCode, position);
    }
    ll_setStringArrayValue(fsRef, position, value);
  }

  @Override
  public void ll_setRefArrayValue(int fsRef, int position, int value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkPrimitiveArrayConditions(fsRef, fsArrayTypeCode, position);
    }
    ll_setRefArrayValue(fsRef, position, value);
  }

  // ************************
  // Low Level Array Setters
  // ************************

  @Override
  public void ll_setIntArrayValue(int fsRef, int position, int value) {
    IntegerArray array = getFsFromId_checked(fsRef);
    array.set(position, value); // that set operation does required journaling
  }

  @Override
  public void ll_setFloatArrayValue(int fsRef, int position, float value) {
    FloatArray array = getFsFromId_checked(fsRef);
    array.set(position, value); // that set operation does required journaling
  }

  @Override
  public void ll_setStringArrayValue(int fsRef, int position, String value) {
    StringArray array = getFsFromId_checked(fsRef);
    array.set(position, value); // that set operation does required journaling
  }

  @Override
  public void ll_setRefArrayValue(int fsRef, int position, int value) {
    FSArray array = getFsFromId_checked(fsRef);
    array.set(position, getFsFromId_checked(value)); // that set operation does required journaling
  }

  /**
   * @param fsRef
   *          an id for a FS
   * @return the type code for this FS
   */
  @Override
  public int ll_getFSRefType(int fsRef) {
    return getFsFromId_checked(fsRef)._getTypeCode();
  }

  @Override
  public int ll_getFSRefType(int fsRef, boolean doChecks) {
    // type code is always valid
    return ll_getFSRefType(fsRef);
  }

  @Override
  public LowLevelCAS getLowLevelCAS() {
    return this;
  }

  @Override
  public int size() {
    throw new UIMARuntimeException(UIMARuntimeException.INTERNAL_ERROR);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.admin.CASMgr#getJCasClassLoader()
   */
  @Override
  public ClassLoader getJCasClassLoader() {
    return svd.jcasClassLoader;
  }

  /*
   * Called to set the overall jcas class loader to use.
   * 
   * @see org.apache.uima.cas.admin.CASMgr#setJCasClassLoader(java.lang.ClassLoader)
   */
  @Override
  public void setJCasClassLoader(ClassLoader classLoader) {
    svd.jcasClassLoader = classLoader;
  }

  public void switchClassLoader(ClassLoader newClassLoader, boolean wasLocked) {
    svd.switchClassLoader(newClassLoader, wasLocked);
  }

  // Internal use only, public for cross package use
  // Assumes: The JCasClassLoader for a CAS is set up initially when the CAS is
  // created
  // and not switched (other than by this code) once it is set.

  // Callers of this method always code the "restoreClassLoaderUnlockCAS" in
  // pairs,
  // protected as needed with try - finally blocks.
  //
  // Special handling is needed for CAS Mulipliers - they can modify a cas up to
  // the point they no longer "own" it.
  // So the try / finally approach doesn't fit

  public void switchClassLoaderLockCas(Object userCode) {
    switchClassLoaderLockCasCL(userCode.getClass().getClassLoader());
  }

  public void switchClassLoaderLockCasCL(ClassLoader newClassLoader) {
    boolean wasLocked = isCasLocked();
    // lock out CAS functions to which annotator should not have access
    enableReset(false);
    svd.switchClassLoader(newClassLoader, wasLocked);
  }

  // // internal use, public for cross-package ref
  // public boolean usingBaseClassLoader() {
  // return (this.svd.jcasClassLoader == this.svd.previousJCasClassLoader);
  // }

  public void restoreClassLoaderUnlockCas() {
    boolean empty_switchControl = svd.switchControl.isEmpty();
    SwitchControl switchControlInstance = empty_switchControl ? null : svd.switchControl.pop();
    if (empty_switchControl || !switchControlInstance.wasLocked) {
      // unlock CAS functions
      enableReset(true);
    }
    // this might be called without the switch ever being called
    svd.restoreClassLoader(empty_switchControl, switchControlInstance);

  }

  @Override
  public FeatureValuePath createFeatureValuePath(String featureValuePath)
          throws CASRuntimeException {
    return FeatureValuePathImpl.getFeaturePath(featureValuePath);
  }

  @Override
  public void setOwner(CasOwner aCasOwner) {
    CASImpl baseCas = getBaseCAS();
    if (baseCas != this) {
      baseCas.setOwner(aCasOwner);
    } else {
      super.setOwner(aCasOwner);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.AbstractCas_ImplBase#release()
   */
  @Override
  public void release() {
    CASImpl baseCas = getBaseCAS();
    if (baseCas != this) {
      baseCas.release();
    } else {
      super.release();
    }
  }

  // **********************************
  // A R R A Y C R E A T I O N
  // **********************************

  @Override
  public ByteArrayFS createByteArrayFS(int length) throws CASRuntimeException {
    checkArrayPreconditions(length);
    return new ByteArray(this.getJCas(), length);
  }

  @Override
  public BooleanArrayFS createBooleanArrayFS(int length) throws CASRuntimeException {
    checkArrayPreconditions(length);
    return new BooleanArray(this.getJCas(), length);
  }

  @Override
  public ShortArrayFS createShortArrayFS(int length) throws CASRuntimeException {
    checkArrayPreconditions(length);
    return new ShortArray(this.getJCas(), length);
  }

  @Override
  public LongArrayFS createLongArrayFS(int length) throws CASRuntimeException {
    checkArrayPreconditions(length);
    return new LongArray(this.getJCas(), length);
  }

  @Override
  public DoubleArrayFS createDoubleArrayFS(int length) throws CASRuntimeException {
    checkArrayPreconditions(length);
    return new DoubleArray(this.getJCas(), length);
  }

  @Override
  public byte ll_getByteValue(int fsRef, int featureCode) {
    return getFsFromId_checked(fsRef).getByteValue(getFeatFromCode_checked(featureCode));
  }

  @Override
  public byte ll_getByteValue(int fsRef, int featureCode, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, featureCode);
    }
    return ll_getByteValue(fsRef, featureCode);
  }

  @Override
  public boolean ll_getBooleanValue(int fsRef, int featureCode) {
    return getFsFromId_checked(fsRef).getBooleanValue(getFeatFromCode_checked(featureCode));
  }

  @Override
  public boolean ll_getBooleanValue(int fsRef, int featureCode, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, featureCode);
    }
    return ll_getBooleanValue(fsRef, featureCode);
  }

  @Override
  public short ll_getShortValue(int fsRef, int featureCode) {
    return getFsFromId_checked(fsRef).getShortValue(getFeatFromCode_checked(featureCode));
  }

  @Override
  public short ll_getShortValue(int fsRef, int featureCode, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, featureCode);
    }
    return ll_getShortValue(fsRef, featureCode);
  }

  // impossible to implement in v3; change callers
  // public long ll_getLongValue(int offset) {
  // return this.getLongHeap().getHeapValue(offset);
  // }

  @Override
  public long ll_getLongValue(int fsRef, int featureCode) {
    return getFsFromId_checked(fsRef).getLongValue(getFeatFromCode_checked(featureCode));
  }

  // public long ll_getLongValueFeatOffset(int fsRef, int offset) {
  // TOP fs = getFsFromId_checked(fsRef);
  // return fs.getLongValueOffset(offset);
  // }

  @Override
  public long ll_getLongValue(int fsRef, int featureCode, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, featureCode);
    }
    return ll_getLongValue(fsRef, featureCode);
  }

  @Override
  public double ll_getDoubleValue(int fsRef, int featureCode) {
    return getFsFromId_checked(fsRef).getDoubleValue(getFeatFromCode_checked(featureCode));
  }

  // public double ll_getDoubleValueFeatOffset(int fsRef, int offset) {
  // TOP fs = getFsFromId_checked(fsRef);
  // return fs.getDoubleValueOffset(offset);
  // }

  @Override
  public double ll_getDoubleValue(int fsRef, int featureCode, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, featureCode);
    }
    return ll_getDoubleValue(fsRef, featureCode);
  }

  @Override
  public void ll_setBooleanValue(int fsRef, int featureCode, boolean value) {
    getFsFromId_checked(fsRef).setBooleanValue(getFeatFromCode_checked(featureCode), value);
  }

  @Override
  public void ll_setBooleanValue(int fsRef, int featureCode, boolean value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, featureCode);
    }
    ll_setBooleanValue(fsRef, featureCode, value);
  }

  @Override
  public final void ll_setByteValue(int fsRef, int featureCode, byte value) {
    getFsFromId_checked(fsRef).setByteValue(getFeatFromCode_checked(featureCode), value);
  }

  @Override
  public void ll_setByteValue(int fsRef, int featureCode, byte value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, featureCode);
    }
    ll_setByteValue(fsRef, featureCode, value);
  }

  @Override
  public final void ll_setShortValue(int fsRef, int featureCode, short value) {
    getFsFromId_checked(fsRef).setShortValue(getFeatFromCode_checked(featureCode), value);
  }

  @Override
  public void ll_setShortValue(int fsRef, int featureCode, short value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, featureCode);
    }
    ll_setShortValue(fsRef, featureCode, value);
  }

  @Override
  public void ll_setLongValue(int fsRef, int featureCode, long value) {
    getFsFromId_checked(fsRef).setLongValue(getFeatFromCode_checked(featureCode), value);
  }

  @Override
  public void ll_setLongValue(int fsRef, int featureCode, long value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, featureCode);
    }
    ll_setLongValue(fsRef, featureCode, value);
  }

  @Override
  public void ll_setDoubleValue(int fsRef, int featureCode, double value) {
    getFsFromId_checked(fsRef).setDoubleValue(getFeatFromCode_checked(featureCode), value);
  }

  @Override
  public void ll_setDoubleValue(int fsRef, int featureCode, double value, boolean doTypeChecks) {
    if (doTypeChecks) {
      checkNonArrayConditions(fsRef, featureCode);
    }
    ll_setDoubleValue(fsRef, featureCode, value);
  }

  @Override
  public byte ll_getByteArrayValue(int fsRef, int position) {
    return ((ByteArray) getFsFromId_checked(fsRef)).get(position);
  }

  @Override
  public byte ll_getByteArrayValue(int fsRef, int position, boolean doTypeChecks) {
    return ll_getByteArrayValue(fsRef, position);
  }

  @Override
  public boolean ll_getBooleanArrayValue(int fsRef, int position) {
    return ((BooleanArray) getFsFromId_checked(fsRef)).get(position);
  }

  @Override
  public boolean ll_getBooleanArrayValue(int fsRef, int position, boolean doTypeChecks) {
    return ll_getBooleanArrayValue(fsRef, position);
  }

  @Override
  public short ll_getShortArrayValue(int fsRef, int position) {
    return ((ShortArray) getFsFromId_checked(fsRef)).get(position);
  }

  @Override
  public short ll_getShortArrayValue(int fsRef, int position, boolean doTypeChecks) {
    return ll_getShortArrayValue(fsRef, position);
  }

  @Override
  public long ll_getLongArrayValue(int fsRef, int position) {
    return ((LongArray) getFsFromId_checked(fsRef)).get(position);
  }

  @Override
  public long ll_getLongArrayValue(int fsRef, int position, boolean doTypeChecks) {
    return ll_getLongArrayValue(fsRef, position);
  }

  @Override
  public double ll_getDoubleArrayValue(int fsRef, int position) {
    return ((DoubleArray) getFsFromId_checked(fsRef)).get(position);
  }

  @Override
  public double ll_getDoubleArrayValue(int fsRef, int position, boolean doTypeChecks) {
    return ll_getDoubleArrayValue(fsRef, position);
  }

  @Override
  public void ll_setByteArrayValue(int fsRef, int position, byte value) {
    ((ByteArray) getFsFromId_checked(fsRef)).set(position, value);
  }

  @Override
  public void ll_setByteArrayValue(int fsRef, int position, byte value, boolean doTypeChecks) {
    ll_setByteArrayValue(fsRef, position, value);
  }

  @Override
  public void ll_setBooleanArrayValue(int fsRef, int position, boolean b) {
    ((BooleanArray) getFsFromId_checked(fsRef)).set(position, b);
  }

  @Override
  public void ll_setBooleanArrayValue(int fsRef, int position, boolean value,
          boolean doTypeChecks) {
    ll_setBooleanArrayValue(fsRef, position, value);
  }

  @Override
  public void ll_setShortArrayValue(int fsRef, int position, short value) {
    ((ShortArray) getFsFromId_checked(fsRef)).set(position, value);
  }

  @Override
  public void ll_setShortArrayValue(int fsRef, int position, short value, boolean doTypeChecks) {
    ll_setShortArrayValue(fsRef, position, value);
  }

  @Override
  public void ll_setLongArrayValue(int fsRef, int position, long value) {
    ((LongArray) getFsFromId_checked(fsRef)).set(position, value);
  }

  @Override
  public void ll_setLongArrayValue(int fsRef, int position, long value, boolean doTypeChecks) {
    ll_setLongArrayValue(fsRef, position, value);
  }

  @Override
  public void ll_setDoubleArrayValue(int fsRef, int position, double d) {
    ((DoubleArray) getFsFromId_checked(fsRef)).set(position, d);
  }

  @Override
  public void ll_setDoubleArrayValue(int fsRef, int position, double value, boolean doTypeChecks) {
    ll_setDoubleArrayValue(fsRef, position, value);
  }

  public boolean isAnnotationType(Type t) {
    return ((TypeImpl) t).isAnnotationType();
  }

  /**
   * @param t
   *          the type code to test
   * @return true if that type is subsumed by AnnotationBase type
   */
  public boolean isSubtypeOfAnnotationBaseType(int t) {
    TypeImpl ti = getTypeFromCode(t);
    return (ti == null) ? false : ti.isAnnotationBaseType();
  }

  public boolean isBaseCas() {
    return this == getBaseCAS();
  }

  @Override
  public Annotation createAnnotation(Type type, int begin, int end) {
    // duplicates a later check
    // if (this.isBaseCas()) {
    // // Can't create annotation on base CAS
    // throw new CASRuntimeException(CASRuntimeException.INVALID_BASE_CAS_METHOD,
    // "createAnnotation(Type, int, int)");
    // }
    Annotation fs = (Annotation) createFS(type);
    fs.setBegin(begin);
    fs.setEnd(end);
    return fs;
  }

  public int ll_createAnnotation(int typeCode, int begin, int end) {
    TOP fs = createAnnotation(getTypeFromCode(typeCode), begin, end);
    set_id2fs(fs); // to prevent gc from reclaiming
    return fs._id();
  }

  /**
   * The generic spec T extends AnnotationFS (rather than AnnotationFS) allows the method JCasImpl
   * getAnnotationIndex to return Annotation instead of AnnotationFS
   * 
   * @param <T>
   *          the Java class associated with the annotation index
   * @return the annotation index
   */
  @Override
  public <T extends AnnotationFS> AnnotationIndex<T> getAnnotationIndex() {
    return (AnnotationIndex<T>) indexRepository.getAnnotationIndex(getTypeSystemImpl().annotType);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.CAS#getAnnotationIndex(org.apache.uima.cas.Type)
   */
  @Override
  public <T extends AnnotationFS> AnnotationIndex<T> getAnnotationIndex(Type type)
          throws CASRuntimeException {
    return (AnnotationIndex<T>) indexRepository.getAnnotationIndex((TypeImpl) type);
  }

  /**
   * @see org.apache.uima.cas.CAS#getAnnotationType()
   */
  @Override
  public Type getAnnotationType() {
    return getTypeSystemImpl().annotType;
  }

  /**
   * @see org.apache.uima.cas.CAS#getEndFeature()
   */
  @Override
  public Feature getEndFeature() {
    return getTypeSystemImpl().endFeat;
  }

  /**
   * @see org.apache.uima.cas.CAS#getBeginFeature()
   */
  @Override
  public Feature getBeginFeature() {
    return getTypeSystemImpl().startFeat;
  }

  private <T extends AnnotationFS> T createDocumentAnnotation(int length) {
    final TypeSystemImpl ts = getTypeSystemImpl();
    // Remove any existing document annotations.
    FSIterator<T> it = this.<T> getAnnotationIndex(ts.docType).iterator();
    List<T> list = new ArrayList<>();
    while (it.isValid()) {
      list.add(it.get());
      it.moveToNext();
    }
    for (int i = 0; i < list.size(); i++) {
      getIndexRepository().removeFS(list.get(i));
    }

    return (T) createDocumentAnnotationNoRemove(length);
  }

  private <T extends Annotation> T createDocumentAnnotationNoRemove(int length) {
    T docAnnot = createDocumentAnnotationNoRemoveNoIndex(length);
    addFsToIndexes(docAnnot);
    return docAnnot;
  }

  public <T extends Annotation> T createDocumentAnnotationNoRemoveNoIndex(int length) {
    final TypeSystemImpl ts = getTypeSystemImpl();
    AnnotationFS docAnnot = createAnnotation(ts.docType, 0, length);
    docAnnot.setStringValue(ts.langFeat, CAS.DEFAULT_LANGUAGE_NAME);
    return (T) docAnnot;
  }

  public int ll_createDocumentAnnotation(int length) {
    final int fsRef = ll_createDocumentAnnotationNoIndex(0, length);
    ll_getIndexRepository().ll_addFS(fsRef);
    return fsRef;
  }

  public int ll_createDocumentAnnotationNoIndex(int begin, int end) {
    final TypeSystemImpl ts = getTypeSystemImpl();
    int fsRef = ll_createAnnotation(ts.docType.getCode(), begin, end);
    ll_setStringValue(fsRef, ts.langFeat.getCode(), CAS.DEFAULT_LANGUAGE_NAME);
    return fsRef;
  }

  // For the "built-in" instance of Document Annotation, set the
  // "end" feature to be the length of the sofa string
  // @formatter:off
  /**
   * updates the document annotation (only if the sofa's local string data != null)
   *   setting the end feature to be the length of the sofa string, if any.
   *   creates the document annotation if not present
   *   only works if not in the base cas
   * 
   */
  // @formatter:on
  public void updateDocumentAnnotation() {
    if (!mySofaIsValid() || this == svd.baseCAS) {
      return;
    }
    String newDoc = mySofaRef.getLocalStringData();
    if (null != newDoc) {
      Annotation docAnnot = getDocumentAnnotationNoCreate();
      if (docAnnot != null) {
        // use a local instance of the add-back memory because this may be called as a side effect
        // of updating a sofa
        FSsTobeAddedback tobeAddedback = FSsTobeAddedback.createSingle();
        boolean wasRemoved = this.checkForInvalidFeatureSetting(docAnnot,
                getTypeSystemImpl().endFeat.getCode(), tobeAddedback);
        docAnnot._setIntValueNfc(endFeatAdjOffset, newDoc.length());
        if (wasRemoved) {
          tobeAddedback.addback(docAnnot);
        }
      } else if (deserialized_doc_annot_not_indexed != null) {
        // UIMA-6199 provides access to non-indexed doc annot
        // to allow sofa setting to set the "length" of the local sofa data string
        // @see updateDocumentAnnotation() updateDocumentAnnotation.
        deserialized_doc_annot_not_indexed._setIntValueNfc(endFeatAdjOffset, newDoc.length());
      } else {
        // not in the index (yet)
        createDocumentAnnotation(newDoc.length());
      }
    }
    return;
  }

  /**
   * Generic issue: The returned document annotation could be either an instance of
   * DocumentAnnotation or a subclass of it, or an instance of Annotation - the Java cover class
   * used for annotations when JCas is not being used.
   */
  @Override
  public <T extends AnnotationFS> T getDocumentAnnotation() {
    T docAnnot = (T) getDocumentAnnotationNoCreate();
    if (null == docAnnot) {
      return (T) createDocumentAnnotationNoRemove(0);
    } else {
      return docAnnot;
    }
  }

  public <T extends AnnotationFS> T getDocumentAnnotationNoCreate() {
    if (this == svd.baseCAS) {
      // base CAS has no document
      return null;
    }
    FSIterator<Annotation> it = getDocAnnotIter();
    it.moveToFirst(); // revalidate in case index updated
    if (it.isValid()) {
      Annotation r = it.get();
      return (T) (inPearContext() ? pearConvert(r) : r);
    }
    return null;
  }

  private FSIterator<Annotation> getDocAnnotIter() {
    if (docAnnotIter != null) {
      return docAnnotIter;
    }
    synchronized (this) {
      if (docAnnotIter == null) {
        docAnnotIter = this.<Annotation> getAnnotationIndex(getTypeSystemImpl().docType).iterator();
      }
      return docAnnotIter;
    }
  }

  /**
   * 
   * @return the fs addr of the document annotation found via the index, or 0 if not there
   */
  public int ll_getDocumentAnnotation() {
    AnnotationFS r = getDocumentAnnotationNoCreate();
    return (r == null) ? 0 : r._id();
  }

  @Override
  public String getDocumentLanguage() {
    if (this == svd.baseCAS) {
      // base CAS has no document
      return null;
    }
    return getDocumentAnnotation().getStringValue(getTypeSystemImpl().langFeat);
  }

  @Override
  public String getDocumentText() {
    return getSofaDataString();
  }

  @Override
  public String getSofaDataString() {
    if (this == svd.baseCAS) {
      // base CAS has no document
      return null;
    }
    return mySofaIsValid() ? mySofaRef.getLocalStringData() : null;
  }

  @Override
  public FeatureStructure getSofaDataArray() {
    if (this == svd.baseCAS) {
      // base CAS has no Sofa
      return null;
    }
    return mySofaIsValid() ? mySofaRef.getLocalFSData() : null;
  }

  @Override
  public String getSofaDataURI() {
    if (this == svd.baseCAS) {
      // base CAS has no Sofa
      return null;
    }
    return mySofaIsValid() ? mySofaRef.getSofaURI() : null;
  }

  @Override
  public InputStream getSofaDataStream() {
    if (this == svd.baseCAS) {
      // base CAS has no Sofa nothin
      return null;
    }
    // return mySofaRef.getSofaDataStream(); // this just goes to the next method
    return mySofaIsValid() ? this.getSofaDataStream(mySofaRef) : null;

  }

  @Override
  public String getSofaMimeType() {
    if (this == svd.baseCAS) {
      // base CAS has no Sofa
      return null;
    }
    return mySofaIsValid() ? mySofaRef.getSofaMime() : null;
  }

  @Override
  public Sofa getSofa() {
    return mySofaRef;
  }

  /**
   * @return the addr of the sofaFS associated with this view, or 0
   */
  @Override
  public int ll_getSofa() {
    return mySofaIsValid() ? mySofaRef._id() : 0;
  }

  @Override
  public String getViewName() {
    return (this == svd.getViewFromSofaNbr(1)) ? CAS.NAME_DEFAULT_SOFA
            : mySofaIsValid() ? mySofaRef.getSofaID() : null;
  }

  private boolean mySofaIsValid() {
    return mySofaRef != null;
  }

  void setDocTextFromDeserializtion(String text) {
    if (mySofaIsValid()) {
      Sofa sofa = getSofaRef(); // creates sofa if doesn't already exist
      sofa.setLocalSofaDataNoDocAnnotUpdate(text);
    }
  }

  @Override
  public void setDocumentLanguage(String languageCode) {
    if (this == svd.baseCAS) {
      throw new CASRuntimeException(CASRuntimeException.INVALID_BASE_CAS_METHOD,
              "setDocumentLanguage(String)");
    }
    Annotation docAnnot = getDocumentAnnotation();
    FeatureImpl languageFeature = getTypeSystemImpl().langFeat;
    languageCode = Language.normalize(languageCode);
    boolean wasRemoved = this.checkForInvalidFeatureSetting(docAnnot, languageFeature.getCode(),
            getAddbackSingle());
    docAnnot.setStringValue(getTypeSystemImpl().langFeat, languageCode);
    addbackSingleIfWasRemoved(wasRemoved, docAnnot);
  }

  private void setSofaThingsMime(Consumer<Sofa> c, String msg) {
    if (this == svd.baseCAS) {
      throw new CASRuntimeException(CASRuntimeException.INVALID_BASE_CAS_METHOD, msg);
    }
    Sofa sofa = getSofaRef();
    c.accept(sofa);
  }

  @Override
  public void setDocumentText(String text) {
    setSofaDataString(text, "text");
  }

  @Override
  public void setSofaDataString(String text, String mime) throws CASRuntimeException {
    setSofaThingsMime(sofa -> sofa.setLocalSofaData(text, mime), "setSofaDataString(text, mime)");
  }

  @Override
  public void setSofaDataArray(FeatureStructure array, String mime) {
    setSofaThingsMime(sofa -> sofa.setLocalSofaData(array, mime),
            "setSofaDataArray(FeatureStructure, mime)");
  }

  @Override
  public void setSofaDataURI(String uri, String mime) throws CASRuntimeException {
    setSofaThingsMime(sofa -> sofa.setRemoteSofaURI(uri, mime), "setSofaDataURI(String, String)");
  }

  @Override
  public void setCurrentComponentInfo(ComponentInfo info) {
    // always store component info in base CAS
    svd.componentInfo = info;
  }

  ComponentInfo getCurrentComponentInfo() {
    return svd.componentInfo;
  }

  /**
   * @see org.apache.uima.cas.CAS#addFsToIndexes(FeatureStructure fs)
   */
  @Override
  public void addFsToIndexes(FeatureStructure fs) {
    // if (fs instanceof AnnotationBaseFS) {
    // final CAS sofaView = ((AnnotationBaseFS) fs).getView();
    // if (sofaView != this) {
    // CASRuntimeException e = new CASRuntimeException(
    // CASRuntimeException.ANNOTATION_IN_WRONG_INDEX, new String[] { fs.toString(),
    // sofaView.getSofa().getSofaID(), this.getSofa().getSofaID() });
    // throw e;
    // }
    // }
    indexRepository.addFS(fs);
  }

  /**
   * @see org.apache.uima.cas.CAS#removeFsFromIndexes(FeatureStructure fs)
   */
  @Override
  public void removeFsFromIndexes(FeatureStructure fs) {
    indexRepository.removeFS(fs);
  }

  /**
   * @param fs
   *          the AnnotationBase instance
   * @return the view associated with this FS where it could be indexed
   */
  public CASImpl getSofaCasView(AnnotationBase fs) {
    return fs._casView;
    // Sofa sofa = fs.getSofa();
    //
    // if (null != sofa && sofa != this.getSofa()) {
    // return (CASImpl) this.getView(sofa.getSofaNum());
    // }
    //
    // /* Note: sofa == null means annotation created from low-level APIs, without setting sofa
    // feature
    // * Ignore this for backwards compatibility */
    // return this;
  }

  @Override
  public CASImpl ll_getSofaCasView(int id) {
    return getSofaCasView(getFsFromId_checked(id));
  }

  // public Iterator<CAS> getViewIterator() {
  // List<CAS> viewList = new ArrayList<CAS>();
  // // add initial view if it has no sofa
  // if (!((CASImpl) getInitialView()).mySofaIsValid()) {
  // viewList.add(getInitialView());
  // }
  // // add views with Sofas
  // FSIterator<SofaFS> sofaIter = getSofaIterator();
  // while (sofaIter.hasNext()) {
  // viewList.add(getView(sofaIter.next()));
  // }
  // return viewList.iterator();
  // }

  /**
   * Creates the initial view (without a sofa) if not present
   * 
   * @return the number of views, excluding the base view, including the initial view (even if not
   *         initially present or no sofa)
   */
  public int getNumberOfViews() {
    CASImpl initialView = getInitialView(); // creates one if not existing, w/o sofa
    int nbrSofas = svd.baseCAS.indexRepository.getIndex(CAS.SOFA_INDEX_NAME).size();
    return initialView.mySofaIsValid() ? nbrSofas : 1 + nbrSofas;
  }

  public int getNumberOfSofas() {
    return svd.baseCAS.indexRepository.getIndex(CAS.SOFA_INDEX_NAME).size();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.CAS#getViewIterator()
   */
  @Override
  public <T extends CAS> Iterator<T> getViewIterator() {
    return new Iterator<T>() {

      final CASImpl initialView = getInitialView(); // creates one if not existing, w/o sofa

      boolean isInitialView_but_noSofa = !initialView.mySofaIsValid(); // true if has no Sofa in
                                                                       // initial view
      // but is reset to false once iterator moves
      // off of initial view.

      // if initial view has a sofa, we just use the
      // sofa iterator instead.

      final FSIterator<Sofa> sofaIter = getSofaIterator();

      @Override
      public boolean hasNext() {
        if (isInitialView_but_noSofa) {
          return true;
        }
        return sofaIter.hasNext();
      }

      @Override
      public T next() {
        if (isInitialView_but_noSofa) {
          isInitialView_but_noSofa = false; // no incr of sofa iterator because it was missing
                                            // initial view
          return (T) initialView;
        }
        return (T) getView(sofaIter.next());
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }

    };
  }

  /**
   * excludes initial view if its sofa is not valid
   * 
   * @return iterator over all views except the base view
   */
  public Iterator<CASImpl> getViewImplIterator() {
    return new Iterator<CASImpl>() {

      final CASImpl initialView = getInitialView(); // creates one if not existing, w/o sofa

      boolean isInitialView_but_noSofa = !initialView.mySofaIsValid(); // true if has no Sofa in
                                                                       // initial view
      // but is reset to false once iterator moves
      // off of initial view.

      // if initial view has a sofa, we just use the
      // sofa iterator instead.

      final FSIterator<Sofa> sofaIter = getSofaIterator();

      @Override
      public boolean hasNext() {
        if (isInitialView_but_noSofa) { // set to false once iterator moves off of first value
          return true;
        }
        return sofaIter.hasNext();
      }

      @Override
      public CASImpl next() {
        if (isInitialView_but_noSofa) {
          isInitialView_but_noSofa = false; // no incr of sofa iterator because it was missing
                                            // initial view
          return initialView;
        }
        return getView(sofaIter.next());
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }

    };
  }

  /**
   * iterate over all views in view order (by view number)
   * 
   * @param processViews
   *          action to perform on the views.
   */
  public void forAllViews(Consumer<CASImpl> processViews) {
    final int numViews = getNumberOfViews();
    for (int viewNbr = 1; viewNbr <= numViews; viewNbr++) {
      CASImpl view = (viewNbr == 1) ? getInitialView() : (CASImpl) getView(viewNbr);
      processViews.accept(view);
    }
    //
    // Iterator<CASImpl> it = getViewImplIterator();
    // while (it.hasNext()) {
    // processViews.accept(it.next());
    // }
  }

  void forAllSofas(Consumer<Sofa> processSofa) {
    FSIterator<Sofa> it = getSofaIterator();
    while (it.hasNext()) {
      processSofa.accept(it.nextNvc());
    }
  }

  /**
   * Excludes base view's ir, Includes the initial view's ir only if it has a sofa defined
   * 
   * @param processIr
   *          the code to execute
   */
  void forAllIndexRepos(Consumer<FSIndexRepositoryImpl> processIr) {
    final int numViews = getViewCount();
    for (int viewNum = 1; viewNum <= numViews; viewNum++) {
      processIr.accept(this.getSofaIndexRepository(viewNum));
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.CAS#getViewIterator(java.lang.String)
   */
  @Override
  public Iterator<CAS> getViewIterator(String localViewNamePrefix) {
    // do sofa mapping for current component
    String absolutePrefix = null;
    if (getCurrentComponentInfo() != null) {
      absolutePrefix = getCurrentComponentInfo().mapToSofaID(localViewNamePrefix);
    }
    if (absolutePrefix == null) {
      absolutePrefix = localViewNamePrefix;
    }

    // find Sofas with this prefix
    List<CAS> viewList = new ArrayList<>();
    FSIterator<Sofa> sofaIter = getSofaIterator();
    while (sofaIter.hasNext()) {
      SofaFS sofa = sofaIter.next();
      String sofaId = sofa.getSofaID();
      if (sofaId.startsWith(absolutePrefix)) {
        if ((sofaId.length() == absolutePrefix.length())
                || (sofaId.charAt(absolutePrefix.length()) == '.')) {
          viewList.add(getView(sofa));
        }
      }
    }
    return viewList.iterator();
  }

  /**
   * protectIndexes
   * 
   * Within the scope of protectIndexes, feature updates are checked, and if found to be a key, and
   * the FS is in a corruptible index, then the FS is removed from the indexes (in all necessary
   * views) (perhaps multiple times if the FS was added to the indexes multiple times), and this
   * removal is recorded on an new instance of FSsTobeReindexed appended to fssTobeAddedback.
   * 
   * Later, when the protectIndexes is closed, the tobe items are added back to the indexes.
   */
  @Override
  public AutoCloseableNoException protectIndexes() {
    FSsTobeAddedback r = FSsTobeAddedback.createMultiple(this);
    svd.fssTobeAddedback.add(r);
    return r;
  }

  void dropProtectIndexesLevel() {
    if (svd.fssTobeAddedback.isEmpty()) {
      return;
    }

    svd.fssTobeAddedback.remove(svd.fssTobeAddedback.size() - 1);
  }

  // @formatter:off
  /**
   * This design is to support normal operations where the
   *   addbacks could be nested
   * It also handles cases where nested ones were inadvertently left open
   * Three cases:
   *    1) the addbacks are the last element in the stack
   *         - remove it from the stack
   *    2) the addbacks are (no longer) in the list
   *         - leave stack alone
   *    3) the addbacks are in the list but not at the end
   *         - remove it and all later ones, calling addback on each     
   * 
   * If the "withProtectedindexes" approach is used, it guarantees proper 
   * nesting, but the Runnable can't throw checked exceptions.
   * 
   * You can do your own try-finally blocks (or use the try with resources
   * form in Java 8 to do a similar thing with no restrictions on what the
   * body can contain.
   * 
   * @param addbacks
   *          -
   */
  // @formatter:on
  void addbackModifiedFSs(FSsTobeAddedback addbacks) {
    final List<FSsTobeAddedback> listOfAddbackInfos = svd.fssTobeAddedback;

    // case 1: the addbacks are the last in the stack:
    if (listOfAddbackInfos.get(listOfAddbackInfos.size() - 1) == addbacks) {
      listOfAddbackInfos.remove(listOfAddbackInfos.size() - 1);
      addbacks.addback();
      return;
    }

    int pos = listOfAddbackInfos.indexOf(addbacks);

    // case 2: the addbacks are in the stack, but there are others following it
    if (pos >= 0) {
      for (int i = listOfAddbackInfos.size() - 1; i >= pos; i--) {
        FSsTobeAddedback toAddBack = listOfAddbackInfos.remove(i);
        toAddBack.addback();
      }
      return;
    }

    // case 3: the addbacks are not in the list - just remove them, ignore the list
    addbacks.addback();
  }

  /**
   * 
   * @param r
   *          an inner block of code to be run with
   */
  @Override
  public void protectIndexes(Runnable r) {
    AutoCloseable addbacks = protectIndexes();
    try {
      r.run();
    } finally {
      addbackModifiedFSs((FSsTobeAddedback) addbacks);
    }
  }

  /**
   * The current implementation only supports 1 marker call per CAS. Subsequent calls will throw an
   * error.
   * 
   * The design is intended to support (at some future point) multiple markers; for this to work,
   * the intent is to extend the MarkerImpl to keep track of indexes into these IntVectors
   * specifying where that marker starts/ends.
   */
  @Override
  public Marker createMarker() {
    if (isCasLocked()) {
      throw new CASAdminException(CASAdminException.FLUSH_DISABLED);
    }
    svd.trackingMark = new MarkerImpl(getLastUsedFsId() + 1, this);
    if (svd.modifiedPreexistingFSs == null) {
      svd.modifiedPreexistingFSs = new IdentityHashMap<>();
    }
    if (svd.modifiedPreexistingFSs.size() > 0) {
      errorMultipleMarkers();
    }

    if (svd.trackingMarkList == null) {
      svd.trackingMarkList = new ArrayList<>();
    } else {
      errorMultipleMarkers();
    }
    svd.trackingMarkList.add(svd.trackingMark);
    return svd.trackingMark;
  }

  private void errorMultipleMarkers() {
    throw new CASRuntimeException(CASRuntimeException.MULTIPLE_CREATE_MARKER);
  }

  // made public https://issues.apache.org/jira/browse/UIMA-2478
  public MarkerImpl getCurrentMark() {
    return svd.trackingMark;
  }

  /**
   * 
   * @return an array of FsChange items, one per modified Fs, sorted in order of fs._id
   */
  FsChange[] getModifiedFSList() {
    final Map<TOP, FsChange> mods = svd.modifiedPreexistingFSs;
    FsChange[] r = mods.values().toArray(new FsChange[mods.size()]);
    Arrays.sort(r, 0, mods.size(), (c1, c2) -> Integer.compare(c1.fs._id, c2.fs._id));
    return r;
  }

  boolean isInModifiedPreexisting(TOP fs) {
    return svd.modifiedPreexistingFSs.containsKey(fs);
  }

  @Override
  public String toString() {
    String sofa = (mySofaRef == null) ? (isBaseCas() ? "Base CAS" : "_InitialView or no Sofa")
            : mySofaRef.getSofaID();
    // (mySofaRef == 0) ? "no Sofa" :

    return this.getClass().getSimpleName() + ":" + getCasId() + "[view: " + sofa + "]";
  }

  public int getCasResets() {
    return svd.casResets.get();
  }

  /**
   * @return an identifier for this CAS, globally unique within the classloader
   */
  public String getCasId() {
    return svd.casId;
  }

  public final int getNextFsId(TOP fs) {
    return svd.getNextFsId(fs);
  }

  public void adjustLastFsV2Size_arrays(int arrayLength) {
    svd.lastFsV2Size += 1 + arrayLength; // 1 is for array length value
  }

  public void adjustLastFsV2size_nonHeapStoredArrays() {
    svd.lastFsV2Size += 2; // length and index into other special heap
  }

  /**
   * Test case use
   * 
   * @param fss
   *          the FSs to include in the id 2 fs map
   */
  public void setId2FSsMaybeUnconditionally(FeatureStructure... fss) {
    for (FeatureStructure fs : fss) {
      setId2FsMaybeUnconditionally((TOP) fs);
    }
  }

  private void setId2FsMaybeUnconditionally(TOP fs) {
    if (svd.isId2Fs) {
      svd.id2fs.putUnconditionally(fs);
    } else {
      set_id2fs(fs);
    }
  }

  // Not currently used
  // public Int2ObjHashMap<TOP, TOP> getId2FSs() {
  // return svd.id2fs.getId2fs();
  // }

  // final private int getNextFsId() {
  // return ++ svd.fsIdGenerator;
  // }

  public final int getLastUsedFsId() {
    return svd.fsIdGenerator;
  }

  public final int peekNextFsId() {
    return svd.peekNextFsId();
  }

  public final int lastV2IdIncr() {
    return svd.lastFsV2IdIncr();
  }

  /**
   * Call this to capture the current value of fsIdGenerator and make it available to other threads.
   * <p>
   * Must be called on a thread that has been synchronized with the thread used for creating FSs for
   * this CAS.
   */
  public final void captureLastFsIdForOtherThread() {
    svd.fsIdLastValue.set(svd.fsIdGenerator);
  }

  public <T extends TOP> T getFsFromId(int id) {
    return (T) svd.id2fs.get(id);
  }

  // /**
  // * plus means all reachable, plus maybe others not reachable but not yet gc'd
  // * @param action -
  // */
  // public void walkReachablePlusFSsSorted(Consumer<TOP> action) {
  // this.svd.id2fs.walkReachablePlusFSsSorted(action);
  // }

  // /**
  // * called for delta serialization - walks just the new items above the line
  // * @param action -
  // * @param fromId - the id of the first item to walk from
  // */
  // public void walkReachablePlusFSsSorted(Consumer<TOP> action, int fromId) {
  // this.svd.id2fs.walkReachablePlueFSsSorted(action, fromId);
  // }
  /**
   * find all of the FSs via the indexes plus what's reachable. sort into order by id,
   * 
   * Apply the action to those Return the list of sorted FSs
   * 
   * @param action_filtered
   *          action to perform on each item after filtering
   * @param mark
   *          null or the mark
   * @param includeFilter
   *          null or a filter (exclude items not in other type system)
   * @param typeMapper
   *          null or how to map to other type system, used to skip things missing in other type
   *          system
   * @return sorted list of all found items (ignoring mark)
   */
  public List<TOP> walkReachablePlusFSsSorted(Consumer<TOP> action_filtered, MarkerImpl mark,
          Predicate<TOP> includeFilter, CasTypeSystemMapper typeMapper) {
    List<TOP> all = new AllFSs(this, mark, includeFilter, typeMapper)
            .getAllFSsAllViews_sofas_reachable().getAllFSsSorted();
    List<TOP> filtered = filterAboveMark(all, mark);
    if (action_filtered != null) {
      for (TOP fs : filtered) {
        action_filtered.accept(fs);
      }
    }
    return all;
  }

  static List<TOP> filterAboveMark(List<TOP> all, MarkerImpl mark) {
    if (null == mark) {
      return all;
    }
    int c = Collections.binarySearch(all, TOP._createSearchKey(mark.nextFSId),
            (fs1, fs2) -> Integer.compare(fs1._id, fs2._id));
    if (c < 0) {
      c = (-c) - 1;
    }
    return all.subList(c, all.size());
  }

  // /**
  // * Get the Java class corresponding to a particular type
  // * Only valid after type system commit
  // *
  // * @param type
  // * @return
  // */
  // public <T extends FeatureStructure> Class<T> getClass4Type(Type type) {
  // TypeSystemImpl tsi = getTypeSystemImpl();
  // if (!tsi.isCommitted()) {
  // throw new CASRuntimeException(CASRuntimeException.GET_CLASS_FOR_TYPE_BEFORE_TS_COMMIT);
  // }
  //
  // }

  public static final boolean isSameCAS(CAS c1, CAS c2) {
    CASImpl ci1 = (CASImpl) c1.getLowLevelCAS();
    CASImpl ci2 = (CASImpl) c2.getLowLevelCAS();
    return ci1.getBaseCAS() == ci2.getBaseCAS();
  }

  public boolean isInCAS(FeatureStructure fs) {
    return ((TOP) fs)._casView.getBaseCAS() == getBaseCAS();
  }

  // /**
  // *
  // * @param typecode -
  // * @return Object that can be cast to either a 2 or 3 arg createFs functional interface
  // * FsGenerator or FsGeneratorArray
  // */
  // private Object getFsGenerator(int typecode) {
  // return getTypeSystemImpl().getGenerator(typecode);
  // }

  public final void checkArrayPreconditions(int len) throws CASRuntimeException {
    // Check array size.
    if (len < 0) {
      throw new CASRuntimeException(CASRuntimeException.ILLEGAL_ARRAY_SIZE);
    }
  }

  @Override
  public <T extends TOP> EmptyFSList<T> emptyFSList() {
    if (null == svd.emptyFSList) {
      svd.emptyFSList = new EmptyFSList<>(getTypeSystemImpl().fsEListType, this);
    }
    return svd.emptyFSList;
  }

  /*
   * @see org.apache.uima.cas.CAS#emptyFloatList()
   */
  @Override
  public EmptyFloatList emptyFloatList() {
    if (null == svd.emptyFloatList) {
      svd.emptyFloatList = new EmptyFloatList(getTypeSystemImpl().floatEListType, this);
    }
    return svd.emptyFloatList;
  }

  @Override
  public EmptyIntegerList emptyIntegerList() {
    if (null == svd.emptyIntegerList) {
      svd.emptyIntegerList = new EmptyIntegerList(getTypeSystemImpl().intEListType, this);
    }
    return svd.emptyIntegerList;
  }

  @Override
  public EmptyStringList emptyStringList() {
    if (null == svd.emptyStringList) {
      svd.emptyStringList = new EmptyStringList(getTypeSystemImpl().stringEListType, this);
    }
    return svd.emptyStringList;
  }

  public CommonArrayFS emptyArray(Type type) {
    switch (((TypeImpl) type).getCode()) {
      case TypeSystemConstants.booleanArrayTypeCode:
        return emptyBooleanArray();
      case TypeSystemConstants.byteArrayTypeCode:
        return emptyByteArray();
      case TypeSystemConstants.shortArrayTypeCode:
        return emptyShortArray();
      case TypeSystemConstants.intArrayTypeCode:
        return emptyIntegerArray();
      case TypeSystemConstants.floatArrayTypeCode:
        return emptyFloatArray();
      case TypeSystemConstants.longArrayTypeCode:
        return emptyLongArray();
      case TypeSystemConstants.doubleArrayTypeCode:
        return emptyDoubleArray();
      case TypeSystemConstants.stringArrayTypeCode:
        return emptyStringArray();
      default: // TypeSystemConstants.fsArrayTypeCode or any other type
        return emptyFSArray();
    }
  }

  @Override
  public FloatArray emptyFloatArray() {
    if (null == svd.emptyFloatArray) {
      svd.emptyFloatArray = new FloatArray(this.getJCas(), 0);
    }
    return svd.emptyFloatArray;
  }

  @Override
  public <T extends FeatureStructure> FSArray<T> emptyFSArray() {
    return emptyFSArray(null);
  }

  @Override
  public <T extends FeatureStructure> FSArray<T> emptyFSArray(Type type) {
    return svd.emptyFSArrayMap.computeIfAbsent(type,
            t -> (t == null) ? new FSArray(this.getJCas(), 0)
                    : new FSArray((TypeImpl) getTypeSystemImpl().getArrayType(type), this, 0));
  }

  @Override
  public IntegerArray emptyIntegerArray() {
    if (null == svd.emptyIntegerArray) {
      svd.emptyIntegerArray = new IntegerArray(this.getJCas(), 0);
    }
    return svd.emptyIntegerArray;
  }

  @Override
  public StringArray emptyStringArray() {
    if (null == svd.emptyStringArray) {
      svd.emptyStringArray = new StringArray(this.getJCas(), 0);
    }
    return svd.emptyStringArray;
  }

  @Override
  public DoubleArray emptyDoubleArray() {
    if (null == svd.emptyDoubleArray) {
      svd.emptyDoubleArray = new DoubleArray(this.getJCas(), 0);
    }
    return svd.emptyDoubleArray;
  }

  @Override
  public LongArray emptyLongArray() {
    if (null == svd.emptyLongArray) {
      svd.emptyLongArray = new LongArray(this.getJCas(), 0);
    }
    return svd.emptyLongArray;
  }

  @Override
  public ShortArray emptyShortArray() {
    if (null == svd.emptyShortArray) {
      svd.emptyShortArray = new ShortArray(this.getJCas(), 0);
    }
    return svd.emptyShortArray;
  }

  @Override
  public ByteArray emptyByteArray() {
    if (null == svd.emptyByteArray) {
      svd.emptyByteArray = new ByteArray(this.getJCas(), 0);
    }
    return svd.emptyByteArray;
  }

  @Override
  public BooleanArray emptyBooleanArray() {
    if (null == svd.emptyBooleanArray) {
      svd.emptyBooleanArray = new BooleanArray(this.getJCas(), 0);
    }
    return svd.emptyBooleanArray;
  }

  /**
   * @param rangeCode
   *          special codes for serialization use only
   * @return the empty list (shared) corresponding to the type
   */
  public EmptyList emptyList(int rangeCode) {
    return (rangeCode == CasSerializerSupport.TYPE_CLASS_INTLIST) ? emptyIntegerList()
            : (rangeCode == CasSerializerSupport.TYPE_CLASS_FLOATLIST) ? emptyFloatList()
                    : (rangeCode == CasSerializerSupport.TYPE_CLASS_STRINGLIST) ? emptyStringList()
                            : emptyFSList();
  }

  /**
   * Get an empty list from the type code of a list
   * 
   * @param typeCode
   *          -
   * @return -
   */
  public EmptyList emptyListFromTypeCode(int typeCode) {
    switch (typeCode) {
      case fsListTypeCode:
      case fsEListTypeCode:
      case fsNeListTypeCode:
        return emptyFSList();
      case floatListTypeCode:
      case floatEListTypeCode:
      case floatNeListTypeCode:
        return emptyFloatList();
      case intListTypeCode:
      case intEListTypeCode:
      case intNeListTypeCode:
        return emptyIntegerList();
      case stringListTypeCode:
      case stringEListTypeCode:
      case stringNeListTypeCode:
        return emptyStringList();
      default:
        throw new IllegalArgumentException();
    }
  }

  // /**
  // * Copies a feature, from one fs to another
  // * FSs may belong to different CASes, but must have the same type system
  // * Features must have compatible ranges
  // * The target must not be indexed
  // * The target must be a "new" (above the "mark") FS
  // * @param fsSrc source FS
  // * @param fi Feature to copy
  // * @param fsTgt target FS
  // */
  // public static void copyFeature(TOP fsSrc, FeatureImpl fi, TOP fsTgt) {
  // if (!copyFeatureExceptFsRef(fsSrc, fi, fsTgt, fi)) {
  // if (!fi.isAnnotBaseSofaRef) {
  // fsTgt._setFeatureValueNcNj(fi, fsSrc._getFeatureValueNc(fi));
  // }
  // }
  // }

  /**
   * Copies a feature from one fs to another FSs may be in different type systems Doesn't copy a
   * feature ref, but instead returns false. This is because feature refs can't cross CASes
   * 
   * @param fsSrc
   *          source FS
   * @param fiSrc
   *          feature in source to copy
   * @param fsTgt
   *          target FS
   * @param fiTgt
   *          feature in target to set
   * @return false if feature is an fsRef
   */
  public static boolean copyFeatureExceptFsRef(TOP fsSrc, FeatureImpl fiSrc, TOP fsTgt,
          FeatureImpl fiTgt) {
    switch (fiSrc.getRangeImpl().getCode()) {
      case booleanTypeCode:
        fsTgt._setBooleanValueNcNj(fiTgt, fsSrc._getBooleanValueNc(fiSrc));
        break;
      case byteTypeCode:
        fsTgt._setByteValueNcNj(fiTgt, fsSrc._getByteValueNc(fiSrc));
        break;
      case shortTypeCode:
        fsTgt._setShortValueNcNj(fiTgt, fsSrc._getShortValueNc(fiSrc));
        break;
      case intTypeCode:
        fsTgt._setIntValueNcNj(fiTgt, fsSrc._getIntValueNc(fiSrc));
        break;
      case longTypeCode:
        fsTgt._setLongValueNcNj(fiTgt, fsSrc._getLongValueNc(fiSrc));
        break;
      case floatTypeCode:
        fsTgt._setFloatValueNcNj(fiTgt, fsSrc._getFloatValueNc(fiSrc));
        break;
      case doubleTypeCode:
        fsTgt._setDoubleValueNcNj(fiTgt, fsSrc._getDoubleValueNc(fiSrc));
        break;
      case stringTypeCode:
        fsTgt._setStringValueNcNj(fiTgt, fsSrc._getStringValueNc(fiSrc));
        break;
      // case javaObjectTypeCode : fsTgt._setJavaObjectValueNcNj(fiTgt,
      // fsSrc.getJavaObjectValue(fiSrc)); break;
      // skip setting sofaRef - it's final and can't be set
      default:
        if (fiSrc.getRangeImpl().isStringSubtype()) {
          fsTgt._setStringValueNcNj(fiTgt, fsSrc._getStringValueNc(fiSrc));
          break; // does substring range check
        }
        return false;
    } // end of switch
    return true;
  }

  public static CommonArrayFS copyArray(TOP srcArray) {
    CommonArrayFS srcCA = (CommonArrayFS) srcArray;
    CommonArrayFS copy = (CommonArrayFS) srcArray._casView.createArray(srcArray._getTypeImpl(),
            srcCA.size());
    copy.copyValuesFrom(srcCA);
    return copy;
  }

  public BinaryCasSerDes getBinaryCasSerDes() {
    return svd.bcsd;
  }

  /**
   * @return the saved CommonSerDesSequential info
   */
  CommonSerDesSequential getCsds() {
    return svd.csds;
  }

  void setCsds(CommonSerDesSequential csds) {
    svd.csds = csds;
  }

  CommonSerDesSequential newCsds() {
    return svd.csds = new CommonSerDesSequential(getBaseCAS());
  }

  /**
   * A space-freeing optimization for use cases where (multiple) delta CASes are being deserialized
   * into this CAS and merged.
   */
  public void deltaMergesComplete() {
    svd.csds = null;
  }

  // @formatter:off
  /******************************************
   * PEAR support
   *   Don't modify the type system because it is in use on multiple threads
   * 
   *   Handling of id2fs for low level APIs:
   *     FSs in id2fs map are the outer non-pear ones
   *     Any gets do pear conversion if needed.
   * 
   ******************************************/
  /**
   * Convert base FS to Pear equivalent
   * 3 cases:
   *   1) no trampoline needed, no conversion, return the original fs
   *   2) trampoline already exists - return that one
   *   3) create new trampoline
   * @param aFs
   * @return
   */
  // @formatter:on
  static <T extends FeatureStructure> T pearConvert(T aFs) {
    if (null == aFs) {
      return null;
    }
    final TOP fs = (TOP) aFs;
    final CASImpl view = fs._casView;
    final TypeImpl ti = fs._getTypeImpl();
    final FsGenerator3 generator = view.svd.generators[ti.getCode()];
    if (null == generator) {
      return aFs;
    }
    return (T) view.pearConvert(fs, generator);
  }

  /**
   * Inner method - after determining there is a generator First see if already have generated the
   * pear version, and if so, use that. Otherwise, create the pear version and save in trampoline
   * table
   * 
   * @param fs
   * @param g
   * @return
   */
  private TOP pearConvert(TOP fs, FsGenerator3 g) {
    return svd.id2tramp.putIfAbsent(fs._id, k -> {

      svd.reuseId = k; // create new FS using base FS's ID
      pearBaseFs = fs;
      TOP r;
      // createFS below is modified because of pearBaseFs non-null to
      // "share" the int and data arrays
      try {
        r = g.createFS(fs._getTypeImpl(), this);
      } finally {
        svd.reuseId = 0;
        pearBaseFs = null;
      }
      assert r != null;
      if (r instanceof UimaSerializable) {
        throw new UnsupportedOperationException(
                "Pears with Alternate implementations of JCas classes implementing UimaSerializable not supported.");
        // ((UimaSerializable) fs)._save_to_cas_data(); // updates in r too
        // ((UimaSerializable) r)._init_from_cas_data();
      }
      return r;
    });
  }

  /**
   * Given a trampoline FS, return the corresponding base Fs Supports adding Fs (which must be a
   * non-trampoline version) to indexes
   * 
   * @param fs
   *          trampoline fs
   * @return the corresponding base fs
   */
  <T extends TOP> T getBaseFsFromTrampoline(T fs) {
    TOP r = svd.id2base.get(fs._id);
    assert r != null;
    return (T) r;
  }

  // *****************************************
  // DEBUGGING and TRACING
  // *****************************************

  public void traceFSCreate(FeatureStructureImplC fs) {
    StringBuilder b = svd.traceFScreationSb;
    if (b.length() > 0) {
      traceFSflush();
    }
    // normally commented-out for matching with v2
    // // mark annotations created by subiterator
    // if (fs._getTypeCode() == TypeSystemConstants.annotTypeCode) {
    // StackTraceElement[] stktr = Thread.currentThread().getStackTrace();
    // if (stktr.length > 7 &&
    // stktr[6].getClassName().equals("org.apache.uima.cas.impl.Subiterator")) {
    // b.append('*');
    // }
    // }
    svd.id2addr.add(svd.nextId2Addr);
    svd.nextId2Addr += fs._getTypeImpl().getFsSpaceReq((TOP) fs);
    traceFSfs(fs);
    svd.traceFSisCreate = true;
    if (fs._getTypeImpl().isArray()) {
      b.append(" l:").append(((CommonArrayFS) fs).size());
    }
  }

  void traceFSfs(FeatureStructureImplC fs) {
    StringBuilder b = svd.traceFScreationSb;
    svd.traceFSid = fs._id;
    b.append("c:").append(String.format("%-3s", getCasId()));
    String viewName = fs._casView.getViewName();
    if (null == viewName) {
      viewName = "base";
    }
    b.append(" v:").append(Misc.elide(viewName, 8));
    b.append(" i:").append(String.format("%-5s", geti2addr(fs._id)));
    b.append(" t:").append(Misc.elide(fs._getTypeImpl().getShortName(), 10));
  }

  void traceIndexMod(boolean isAdd, TOP fs, boolean isAddbackOrSkipBag) {
    StringBuilder b = svd.traceCowSb;
    b.setLength(0);
    b.append(isAdd ? (isAddbackOrSkipBag ? "abk_idx " : "add_idx ")
            : (isAddbackOrSkipBag ? "rmv_auto_idx " : "rmv_norm_idx "));
    // b.append(fs.toString());
    b.append(fs._getTypeImpl().getShortName()).append(":").append(fs._id);
    if (fs instanceof Annotation ann) {
      b.append(" begin: ").append(ann.getBegin());
      b.append(" end: ").append(ann.getEnd());
      b.append(" txt: \"").append(Misc.elide(ann.getCoveredText(), 10)).append("\"");
    }
    traceOut.println(b);
  }

  void traceCowCopy(FsIndex_singletype<?> index) {
    StringBuilder b = svd.traceCowSb;
    b.setLength(0);
    b.append("cow-copy:");
    b.append(" i: ").append(index);
    traceOut.println(b);
  }

  void traceCowCopyUse(FsIndex_singletype<?> index) {
    StringBuilder b = svd.traceCowSb;
    b.setLength(0);
    b.append("cow-copy-used:");
    b.append(" i: ").append(index);
    traceOut.println(b);
  }

  void traceCowReinit(String kind, FsIndex_singletype<?> index) {
    StringBuilder b = svd.traceCowSb;
    b.setLength(0);
    b.append("cow-redo: ");
    b.append(kind);
    b.append(" i: ").append(index);
    b.append(" c: ");
    b.append(Misc.getCaller());
    traceOut.println(b);
  }

  /** only used for tracing, enables tracing 2 slots for long/double */
  private FeatureImpl prevFi;

  void traceFSfeat(FeatureStructureImplC fs, FeatureImpl fi, Object v) {
    // debug
    FeatureImpl originalFi = fi;
    StringBuilder b = svd.traceFScreationSb;
    assert (b.length() > 0);
    if (fs._id != svd.traceFSid) {
      traceFSfeatUpdate(fs);
    }
    if (fi == null) { // happens on 2nd setInt call from cas copier copyfeatures for Long / Double
      switch (prevFi.getSlotKind()) {
        case Slot_DoubleRef:
          v = fs._getDoubleValueNc(prevFi);
          break; // correct double and long
        case Slot_LongRef:
          v = fs._getLongValueNc(prevFi);
          break; // correct double and long
        default:
          Misc.internalError();
      }
      fi = prevFi;
      prevFi = null;
    } else {
      prevFi = fi;
    }
    String fn = fi.getShortName();
    // correct calls done by cas copier fast loop
    if (fi.getSlotKind() == SlotKind.Slot_DoubleRef) {
      if (v instanceof Integer) {
        return; // wait till the next part is traced
      } else if (v instanceof Long) {
        v = CASImpl.long2double((long) v);
      }
    }

    if (fi.getSlotKind() == SlotKind.Slot_LongRef && (v instanceof Integer)) {
      return; // output done on the next int call
    }

    String fv = getTraceRepOfObj(fi, v);
    // if (geti2addr(fs._id).equals("79") &&
    // fn.equals("sofa")) {
    // new Throwable().printStackTrace(traceOut);
    // }
    // // debug
    // if (fn.equals("lemma") &&
    // fv.startsWith("Lemma:") &&
    // debug1cnt < 2) {
    // debug1cnt ++;
    // traceOut.println("setting lemma feat:");
    // new Throwable().printStackTrace(traceOut);
    // }
    // debug
    // if (fs._getTypeImpl().getShortName().equals("Passage") &&
    // "score".equals(fn) &&
    // debug2cnt < 5) {
    // debug2cnt++;
    // traceOut.println("setting score feat in Passage");
    // new Throwable().printStackTrace(traceOut);
    // }
    // debug
    int i_v = Math.max(0, 10 - fn.length());
    int i_n = Math.max(0, 10 - fv.length());

    fn = Misc.elide(fn, 10 + i_n, false);
    fv = Misc.elide(fv, 10 + i_v, false);
    // debug
    // if (!svd.traceFSisCreate && fn.equals("uninf.dWord") && fv.equals("XsgTokens")) {
    // traceOut.println("debug uninf.dWord:XsgTokens: " + Misc.getCallers(3, 10));
    // }
    b.append(' ').append(Misc.elide(fn + ':' + fv, 21));
    // value of a feature:
    // - "null" or
    // - if FS: type:id (converted to addr)
    // - v.toString()
  }

  // private static int debug2cnt = 0;

  /**
   * @param v
   * @return value of the feature: "null" or if FS: type:id (converted to addr) or v.toString()
   *         Note: white space in strings converted to "_' characters
   */
  private String getTraceRepOfObj(FeatureImpl fi, Object v) {
    if (v instanceof TOP) {
      TOP fs = (TOP) v;
      return Misc.elide(fs.getType().getShortName(), 5, false) + ':' + geti2addr(fs._id);
    }
    if (v == null) {
      return "null";
    }
    if (v instanceof String) {
      String s = Misc.elide((String) v, 50, false);
      return Misc.replaceWhiteSpace(s, "_");
    }
    if (v instanceof Integer) {
      int iv = (int) v;
      switch (fi.getSlotKind()) {
        case Slot_Boolean:
          return (iv == 1) ? "true" : "false";
        case Slot_Byte:
        case Slot_Short:
        case Slot_Int:
          return Integer.toString(iv);
        case Slot_Float:
          return Float.toString(int2float(iv));
        default:
          // Ignore
      }
    }
    if (v instanceof Long) {
      long vl = (long) v;
      return (fi.getSlotKind() == SlotKind.Slot_DoubleRef) ? Double.toString(long2double(vl))
              : Long.toString(vl);
    }
    return Misc.replaceWhiteSpace(v.toString(), "_");
  }

  private String geti2addr(int id) {
    if (id >= svd.id2addr.size()) {
      return Integer.toString(id) + '!';
    }
    return Integer.toString(svd.id2addr.get(id));
  }

  void traceFSfeatUpdate(FeatureStructureImplC fs) {
    traceFSflush();
    traceFSfs(fs);
    svd.traceFSisCreate = false;
  }

  public StringBuilder traceFSflush() {
    if (!traceFSs) {
      return null;
    }
    StringBuilder b = svd.traceFScreationSb;
    if (b.length() > 0) {
      traceOut.println((svd.traceFSisCreate ? "cr: " : "up: ") + b);
      b.setLength(0);
      svd.traceFSisCreate = false;
    }
    return b;
  }

  private static class MeasureSwitchType {
    TypeImpl oldType;
    TypeImpl newType;
    String oldJCasClassName;
    String newJCasClassName;
    int count = 0;
    boolean newSubsumesOld;
    boolean oldSubsumesNew;
    long scantime = 0;

    MeasureSwitchType(TypeImpl oldType, TypeImpl newType) {
      this.oldType = oldType;
      oldJCasClassName = oldType.getJavaClass().getName();
      this.newType = newType;
      newJCasClassName = newType.getJavaClass().getName();
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((newJCasClassName == null) ? 0 : newJCasClassName.hashCode());
      result = prime * result + ((newType == null) ? 0 : newType.hashCode());
      result = prime * result + ((oldJCasClassName == null) ? 0 : oldJCasClassName.hashCode());
      result = prime * result + ((oldType == null) ? 0 : oldType.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if ((obj == null) || !(obj instanceof MeasureSwitchType)) {
        return false;
      }
      MeasureSwitchType other = (MeasureSwitchType) obj;
      if (newJCasClassName == null) {
        if (other.newJCasClassName != null) {
          return false;
        }
      } else if (!newJCasClassName.equals(other.newJCasClassName)) {
        return false;
      }
      if (newType == null) {
        if (other.newType != null) {
          return false;
        }
      } else if (!newType.equals(other.newType)) {
        return false;
      }
      if (oldJCasClassName == null) {
        if (other.oldJCasClassName != null) {
          return false;
        }
      } else if (!oldJCasClassName.equals(other.oldJCasClassName)) {
        return false;
      }
      if (oldType == null) {
        if (other.oldType != null) {
          return false;
        }
      } else if (!oldType.equals(other.oldType)) {
        return false;
      }
      return true;
    }
  }

  private static final Map<MeasureSwitchType, MeasureSwitchType> measureSwitches = new HashMap<>();

  static {
    if (MEASURE_SETINT) {
      Runtime.getRuntime().addShutdownHook(new Thread(null, () -> {
        System.out.println("debug Switch Types dump, # entries: " + measureSwitches.size());
        int s1 = 0, s2 = 0, s3 = 0;
        for (MeasureSwitchType mst : measureSwitches.keySet()) {
          s1 = Math.max(s1, mst.oldType.getName().length());
          s2 = Math.max(s2, mst.newType.getName().length());
          s3 = Math.max(s3, mst.oldJCasClassName.length());
        }

        for (MeasureSwitchType mst : measureSwitches.keySet()) {
          System.out.format(
                  "count: %,6d scantime = %,7d ms,  subsumes: %s %s,   type: %-" + s1
                          + "s  newType: %-" + s2 + "s,  cl: %-" + s3 + "s, newCl: %s%n",
                  mst.count, mst.scantime / 1000000, mst.newSubsumesOld ? "n>o" : "   ",
                  mst.oldSubsumesNew ? "o>w" : "   ", mst.oldType.getName(), mst.newType.getName(),
                  mst.oldJCasClassName, mst.newJCasClassName);
        }

        // if (traceFSs) {
        // System.err.println("debug closing traceFSs output");
        // traceOut.close();
        // }
      }, "Dump SwitchTypes"));
    }

    // this is definitely needed
    if (traceFSs) {
      Runtime.getRuntime().addShutdownHook(new Thread(null, () -> {
        System.out.println("closing traceOut");
        traceOut.close();
      }, "close trace output"));
    }
  }

  /*
   * @forRemoval 4.0.0
   * 
   * @deprecated Does nothing, kept only for backwards compatibility
   */
  @Override
  @Deprecated(since = "3.0.0")
  public void setCAS(CAS cas) {
  }

  /**
   * @return true if in Pear context, or external context outside AnalysisEngine having a UIMA
   *         Extension class loader e.g., if calling a call-back routine loaded outside the AE.
   */
  boolean inPearContext() {
    return svd.previousJCasClassLoader != null;
  }

  /**
   * Pear context suspended while creating a base version, when we need to create a new FS (we need
   * to create both the base and the trampoline version)
   */
  private void suspendPearContext() {
    svd.suspendPreviousJCasClassLoader = svd.previousJCasClassLoader;
    svd.previousJCasClassLoader = null;
  }

  private void restorePearContext() {
    svd.previousJCasClassLoader = svd.suspendPreviousJCasClassLoader;
  }

  /**
   * 
   * @return the initial heap size specified or defaulted
   */
  public int getInitialHeapSize() {
    return svd.initialHeapSize;
  }

  // backwards compatibility - reinit calls
  // just the public apis
  /**
   * Deserializer for Java-object serialized instance of CASSerializer.
   * 
   * @param ser
   *          - The instance to convert back to a CAS
   */
  public void reinit(CASSerializer ser) {
    svd.bcsd.reinit(ser);
  }

  /**
   * Deserializer for CASCompleteSerializer instances - includes type system and index definitions
   * Never delta
   * 
   * @param casCompSer
   *          -
   */
  public void reinit(CASCompleteSerializer casCompSer) {
    svd.bcsd.reinit(casCompSer);
  }

  // @formatter:off
  /**
   * --------------------------------------------------------------------- 
   * see Blob Format in CASSerializer
   * 
   * This reads in and deserializes CAS data from a stream. Byte swapping may be needed if the blob
   * is from C++ -- C++ blob serialization writes data in native byte order.
   * 
   * Supports delta deserialization. For that, the the csds from the serialization event must be
   * used.
   * 
   * @param istream
   *          -
   * @return - the format of the input stream detected
   * @throws CASRuntimeException
   *           wraps IOException
   */
  // @formatter:on
  public SerialFormat reinit(InputStream istream) throws CASRuntimeException {
    return svd.bcsd.reinit(istream);
  }

  void maybeHoldOntoFS(FeatureStructureImplC fs) {
    if (svd.isId2Fs) {
      svd.id2fs.put((TOP) fs); // does an assert - prev id should not be there
    }
  }

  public void swapInPearVersion(Object[] a) {
    if (!inPearContext()) {
      return;
    }

    for (int i = 0; i < a.length; i++) {
      Object ao = a[i];
      if (ao instanceof TOP top) {
        a[i] = pearConvert(top);
      }
    }
  }

  public Collection<?> collectNonPearVersions(Collection<?> c) {
    if (c.isEmpty() || !inPearContext()) {
      return c;
    }
    ArrayList<Object> items = new ArrayList<>(c.size());
    for (Object o : c) {
      if (o instanceof TOP top) {
        items.add(pearConvert(top));
      }
    }
    return items;
  }

  public <T> Spliterator<T> makePearAware(Spliterator<T> baseSi) {
    if (!inPearContext()) {
      return baseSi;
    }

    return new Spliterator<T>() {

      @Override
      public boolean tryAdvance(Consumer<? super T> action) {
        return baseSi.tryAdvance(
                item -> action.accept((item instanceof TOP) ? (T) pearConvert((TOP) item) : item));
      }

      @Override
      public Spliterator<T> trySplit() {
        return baseSi.trySplit();
      }

      @Override
      public long estimateSize() {
        return baseSi.estimateSize();
      }

      @Override
      public int characteristics() {
        return baseSi.characteristics();
      }

    };
  }

  @Override
  public boolean is_ll_enableV2IdRefs() {
    return svd.isId2Fs;
  }

  @Override
  public AutoCloseableNoException ll_enableV2IdRefs(boolean enable) {
    final boolean restoreState = svd.isId2Fs;
    if (enable && !restoreState && svd.fsIdGenerator != 0) {
      throw new IllegalStateException("CAS must be empty when switching to V2 ID References mode.");
    }
    AutoCloseableNoException r = () -> svd.isId2Fs = restoreState;
    svd.isId2Fs = enable;
    return r;
  }

  AutoCloseableNoException ll_forceEnableV2IdRefs(boolean enable) {
    final boolean restoreState = svd.isId2Fs;
    AutoCloseableNoException r = () -> svd.isId2Fs = restoreState;
    svd.isId2Fs = enable;
    return r;
  }

  // int allocIntData(int sz) {
  //
  // if (sz > INT_DATA_FOR_ALLOC_SIZE / 4) {
  // returnIntDataForAlloc = new int[sz];
  // return 0;
  // }
  //
  // if (sz + nextIntDataOffsetForAlloc > INT_DATA_FOR_ALLOC_SIZE) {
  // // too large to fit, alloc a new one
  // currentIntDataForAlloc = new int[INT_DATA_FOR_ALLOC_SIZE];
  // nextIntDataOffsetForAlloc = 0;
  // }
  // int r = nextIntDataOffsetForAlloc;
  // nextIntDataOffsetForAlloc += sz;
  // returnIntDataForAlloc = currentIntDataForAlloc;
  // return r;
  // }
  //
  // int[] getReturnIntDataForAlloc() {
  // return returnIntDataForAlloc;
  // }
  //
  // int allocRefData(int sz) {
  //
  // if (sz > REF_DATA_FOR_ALLOC_SIZE / 4) {
  // returnRefDataForAlloc = new Object[sz];
  // return 0;
  // }
  //
  // if (sz + nextRefDataOffsetForAlloc > REF_DATA_FOR_ALLOC_SIZE) {
  // // too large to fit, alloc a new one
  // currentRefDataForAlloc = new Object[REF_DATA_FOR_ALLOC_SIZE];
  // nextRefDataOffsetForAlloc = 0;
  // }
  // int r = nextRefDataOffsetForAlloc;
  // nextRefDataOffsetForAlloc += sz;
  // returnRefDataForAlloc = currentRefDataForAlloc;
  // return r;
  // }
  //
  // Object[] getReturnRefDataForAlloc() {
  // return returnRefDataForAlloc;
  // }
  // UIMA-6199 provides access to non-indexed doc annot
  // to allow sofa setting to set the "length" of the local sofa data string
  // @see updateDocumentAnnotation() updateDocumentAnnotation.
  public void set_deserialized_doc_annot_not_indexed(Annotation doc_annot) {
    deserialized_doc_annot_not_indexed = doc_annot;
  }
}
