package org.apache.uima.util.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.uima.UIMAFramework;
import org.apache.uima.util.Level;

/**
 * Class that reads properties files containing external parameter overrides used by the ExternalOverrideSettings_impl
 * class.
 * 
 * Similar to java.util.Properties but: 
 *    supports UTF-8 files 
 *    reverses priority in that duplicate key values are ignored, i.e. values cannot be changed 
 *    arrays are represented as strings, e.g. '[elem1,elem2]', and can span multiple lines
 *    '\' can be used to escape $ [ , ] and the line-end
 * 
 * @author burn
 * 
 */

public class Settings_impl {

  protected static final String LOG_RESOURCE_BUNDLE = "org.apache.uima.impl.log_messages";

  private BufferedReader rdr;

  private Map<String, String> map;

  public Settings_impl() {
    map = new HashMap<String, String>();
  }

  /**
   * Get the value of a property with the specified key
   */
  public String getProperty(String key) {
    return map.get(key);
  }

  /*
   * Return a set of keys of all properties in the map
   */
  public Set<String> getKeys() {
    return map.keySet();
  }

  /*
   * Load properties from an input stream Existing properties are not replaced (unlike java.util.Properties) May be
   * called multiple times
   */
  public void load(InputStream in) throws IOException {
    // Process each logical line (after blanks & comments removed and continuations extended)
    rdr = new BufferedReader(new InputStreamReader(in, "UTF-8"));
    String line;
    while ((line = getLine()) != null) {
      // Split into two -- on '=' or ':' or white-space
      String[] parts = line.trim().split("\\s*[:=\\s]\\s*", 2);
      String name = parts[0];
      String value;
      // When RHS is empty get a split only for the := separators
      if (parts.length == 1) {
        value = "";
      } else {
        value = parts[1];
        if (value.length() > 0 && value.charAt(0) == '[') {
          value = getArray(value);
        }
      }
      if (!map.containsKey(name)) {
        map.put(name, value);
      } else {
        // Key {0} already in use ... ignoring value "{1}"
        UIMAFramework.getLogger(this.getClass()).logrb(Level.WARNING, this.getClass().getName(), "load",
                LOG_RESOURCE_BUNDLE, "UIMA_external_override_ignored__WARNING", new Object[] { name, value });
      }
    }
  }

  /*
   * Create a string representing an array from one or more logical lines
   * Assert: line length > 0
   */
  private String getArray(String line) throws IOException {

    int iend = line.indexOf(']');
    while (iend >= 0 && isEscaped(line, iend)) {
      iend = line.indexOf(']', iend + 1);
    }
    if (iend >= 0) {
      // Found the closing ']' - remainder of line must be empty
      if (line.substring(iend + 1, line.length()).trim().length() > 0) {
        throw new IOException("Syntax error - invalid characters after ']'");
      }
      return line;
    }

    // Trim each logical line as may be a single array element
    // If line doesn't end with a , add one and append the next line(s)
    // Don't add a , if line has only '[' or ']'
    String nextline = getLine();
    if (nextline == null) {
      throw new IOException("Premature EOF - missing ']'");
    }
    iend = line.length() - 1;
    if ((line.charAt(iend) == ',' && !isEscaped(line, iend)) || 
            line.equals("[") || nextline.trim().charAt(0) == ']') {
      return line + getArray(nextline.trim());
    } else {
      return line + "," + getArray(nextline.trim());
    }
  }

  /*
   * Reads a logical line from the input stream following the Java Properties class rules Ignore blank lines or comments
   * (first non-blank is '#' or '!') An un-escaped final '\' marks a continuation line
   */
  private String getLine() throws IOException {
    String line = rdr.readLine();
    if (line == null) {
      return null;
    }
    // If line is blank or a comment get another & check it again
    String trimmed = line.trim();
    if (trimmed.length() == 0 || trimmed.charAt(0) == '#' || trimmed.charAt(0) == '!') {
      return getLine();
    }
    // Append further lines if should be continued
    // Don't trim as could change what a final \ is escaping
    return extendLine(line);
  }

  /*
   * If line should be continued read another and append it
   */
  private String extendLine(String line) throws IOException {
    // Check if line-end is escaped
    if (!isEscaped(line, line.length())) {
      return line;
    }
    // Line must be continued ... remove the final \ and append the next line, etc.
    int ilast = line.length() - 1;
    String next = rdr.readLine();
    if (next == null) {
      return line.substring(0, ilast);
    }
    return line.substring(0, ilast) + extendLine(next);
  }

  /*
   * Check if a character in the string is escaped, i.e. preceded by an odd number of '\'s
   */
  public boolean isEscaped(String line, int ichar) {
    int i = ichar - 1;
    while (i >= 0 && line.charAt(i) == '\\') {
      --i;
    }
    // Difference will be one more than number of '\'s
    return ((ichar - i) % 2 == 0);
  }

}
