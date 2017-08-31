package org.apache.uima.cas;

/**
 * There are 4 kinds of comparators
 *   for the combinations of comparing
 *     - with or without the "id"
 *     - with or without type order (with only includes typeOrder if there is such a key included) 
 */
public enum FSComparators {
    WITH_ID,             // include the id in the comparator
    WITHOUT_ID,          // no          id in the comparator
    WITH_TYPE_ORDER,     // include the typeOrder in the comparator
    WITHOUT_TYPE_ORDER   // no          typeOrder in the comparator
}
