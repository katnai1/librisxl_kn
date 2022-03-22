/**
 * This file was automatically generated by the TRLD transpiler.
 * Source: trld/trig/serializer.py
 */
package trld.trig;

//import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import java.io.*;

import trld.Builtins;
import trld.KeyValue;

import trld.Output;
import static trld.Common.uuid4;
import static trld.jsonld.Base.BASE;
import static trld.jsonld.Base.CONTAINER;
import static trld.jsonld.Base.CONTEXT;
import static trld.jsonld.Base.GRAPH;
import static trld.jsonld.Base.ID;
import static trld.jsonld.Base.INDEX;
import static trld.jsonld.Base.LANGUAGE;
import static trld.jsonld.Base.LIST;
import static trld.jsonld.Base.PREFIX;
import static trld.jsonld.Base.PREFIX_DELIMS;
import static trld.jsonld.Base.REVERSE;
import static trld.jsonld.Base.TYPE;
import static trld.jsonld.Base.VALUE;
import static trld.jsonld.Base.VOCAB;

public class Serializer {
  public static final String ANNOTATION = "@annotation"; // LINE: 12
  public static final Pattern WORD_START = (Pattern) Pattern.compile("^\\w*$"); // LINE: 14
  public static final Pattern PNAME_LOCAL_ESC = (Pattern) Pattern.compile("([~!$&'()*+,;=/?#@%]|^[.-]|[.-]$)"); // LINE: 15

  public static void serialize(Map<String, Object> data, Output out) {
    serialize(data, out, null);
  }
  public static void serialize(Map<String, Object> data, Output out, /*@Nullable*/ Map context) {
    serialize(data, out, context, null);
  }
  public static void serialize(Map<String, Object> data, Output out, /*@Nullable*/ Map context, /*@Nullable*/ String baseIri) {
    serialize(data, out, context, baseIri, null);
  }
  public static void serialize(Map<String, Object> data, Output out, /*@Nullable*/ Map context, /*@Nullable*/ String baseIri, Settings settings) { // LINE: 46
    settings = (settings != null ? settings : new Settings()); // LINE: 53
    SerializerState state = new SerializerState(out, settings, context, baseIri); // LINE: 54
    state.serialize(data); // LINE: 55
  }

  public static void serializeTurtle(Map<String, Object> data, Output out) {
    serializeTurtle(data, out, null);
  }
  public static void serializeTurtle(Map<String, Object> data, Output out, /*@Nullable*/ Map context) {
    serializeTurtle(data, out, context, null);
  }
  public static void serializeTurtle(Map<String, Object> data, Output out, /*@Nullable*/ Map context, /*@Nullable*/ String baseIri) {
    serializeTurtle(data, out, context, baseIri, false);
  }
  public static void serializeTurtle(Map<String, Object> data, Output out, /*@Nullable*/ Map context, /*@Nullable*/ String baseIri, Boolean union) { // LINE: 58
    Settings settings = new Settings(true, !(union)); // LINE: 65
    serialize(data, out, context, baseIri, settings); // LINE: 66
  }

  public static Map<String, String> collectPrefixes(/*@Nullable*/ Object context) { // LINE: 668
    if (!(context instanceof Map)) { // LINE: 669
      return new HashMap<>(); // LINE: 670
    }
    Map prefixes = new HashMap<>(); // LINE: 672
    for (Map.Entry<String, Object> key_value : ((Map<String, Object>) context).entrySet()) { // LINE: 673
      String key = key_value.getKey();
      Object value = key_value.getValue();
      if ((value instanceof String && PREFIX_DELIMS.contains(((String) value).substring(((String) value).length() - 1, ((String) value).length() - 1 + 1)))) { // LINE: 674
        prefixes.put(((key == null && ((Object) VOCAB) == null || key != null && (key).equals(VOCAB)) ? "" : key), (String) value); // LINE: 675
      } else if ((value instanceof Map && (((Map) value).get(PREFIX) == null && ((Object) true) == null || ((Map) value).get(PREFIX) != null && (((Map) value).get(PREFIX)).equals(true)))) { // LINE: 676
        prefixes.put(key, ((Map) value).get(ID)); // LINE: 677
      }
    }
    return prefixes; // LINE: 679
  }

  public static List asList(Object value) { // LINE: 682
    return (value instanceof List ? (List) value : new ArrayList<>(Arrays.asList(new Object[] {(Object) value}))); // LINE: 683
  }
}
