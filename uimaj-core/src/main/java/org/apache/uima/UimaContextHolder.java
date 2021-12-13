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
package org.apache.uima;

import static java.util.Collections.synchronizedMap;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.uima.internal.util.Misc;

/**
 * This class holds the UimaContext for the current thread, or a parent thread. The getContext
 * method may be used by any plain Java class invoked by an annotator, The POJO must run in the same
 * thread or a child thread of the annotator.
 * 
 * For example a POJO can access the shared External Override Settings with: String paramValue =
 * UimaContextHolder.getContext().getSetting(paramName);
 */
public class UimaContextHolder {

  private static final String TRACK_CONTEXT_HOLDER_TRACKING = "uima.enable_context_holder_tracking";
  private static final String CONTEXT_HOLDER_REFERENCE_TYPE = "uima.context_holder_reference_type";

  private static final boolean IS_TRACK_CONTEXT_HOLDER_TRACKING = Misc
          .getNoValueSystemProperty(TRACK_CONTEXT_HOLDER_TRACKING);

  private static final ContextHolderReferenceType CONTEXT_HOLDER_REFERENCE_TYPE_VALUE = ContextHolderReferenceType
          .valueOf(System.getProperty(CONTEXT_HOLDER_REFERENCE_TYPE,
                  ContextHolderReferenceType.WEAK.toString()));

  private static final InheritableThreadLocal<Object> THREAD_LOCAL_CONTEXT = new InheritableThreadLocal<>();

  private static final Map<UimaContext, StackTraceElement[]> CONTEXT_SETTERS = IS_TRACK_CONTEXT_HOLDER_TRACKING
          ? synchronizedMap(new WeakHashMap<>())
          : null;

  private UimaContextHolder() {
    // No instances
  }

  /**
   * Get the UimaContext for this thread
   * 
   * @return the thread-specific UimaContext
   */
  public static UimaContext getContext() {
    Object obj = THREAD_LOCAL_CONTEXT.get();

    if (!(obj instanceof Reference)) {
      return (UimaContext) obj;
    }

    @SuppressWarnings("unchecked")
    Reference<UimaContext> ref = (Reference<UimaContext>) obj;
    UimaContext context = ref.get();
    if (context == null) {
      THREAD_LOCAL_CONTEXT.set(null);
    }

    return context;
  }

  /**
   * Sets the UimaContext for the current thread.
   * <p>
   * NOTE - Should be used only by the UIMA Framework.
   * 
   * @param uimaContext
   *          - new UimaContext for this thread
   * @return - previous UimaContext for this thread
   */
  public static UimaContext setContext(UimaContext uimaContext) {
    Object prevContextObj = THREAD_LOCAL_CONTEXT.get();
    @SuppressWarnings("unchecked")
    UimaContext prevContext = prevContextObj instanceof Reference
            ? ((Reference<UimaContext>) prevContextObj).get()
            : (UimaContext) prevContextObj;

    if (uimaContext == null) {
      // Clear context
      THREAD_LOCAL_CONTEXT.set(null);
      if (prevContext != null && CONTEXT_SETTERS != null) {
        CONTEXT_SETTERS.remove(prevContext);
      }
    } else {
      // Set context with the configured reference type
      THREAD_LOCAL_CONTEXT.set(makeRef(uimaContext));
      if (CONTEXT_SETTERS != null) {
        CONTEXT_SETTERS.put(uimaContext, new Exception().getStackTrace());
      }
    }

    return prevContext;
  }

  private static Object makeRef(UimaContext aContext) {
    switch (CONTEXT_HOLDER_REFERENCE_TYPE_VALUE) {
      case SOFT:
        return new SoftReference<>(aContext);
      case WEAK:
        return new WeakReference<>(aContext);
      case STRONG:
        return aContext;
      default:
        throw new IllegalArgumentException(
                "Unsupported reference type: [" + CONTEXT_HOLDER_REFERENCE_TYPE_VALUE + "]");
    }
  }

  /**
   * Clears the UimaContext entry for the current thread
   * <p>
   * NOTE - Should be used only by the UIMA Framework.
   */
  public static void clearContext() {
    if (CONTEXT_SETTERS != null) {
      Object prevContextObj = THREAD_LOCAL_CONTEXT.get();
      @SuppressWarnings("unchecked")
      UimaContext prevContext = prevContextObj instanceof Reference
              ? ((Reference<UimaContext>) prevContextObj).get()
              : (UimaContext) prevContextObj;

      if (prevContext != null) {
        CONTEXT_SETTERS.remove(prevContext);
      }
    }

    THREAD_LOCAL_CONTEXT.set(null);
  }

  private enum ContextHolderReferenceType {
    STRONG, WEAK, SOFT;
  }
}
