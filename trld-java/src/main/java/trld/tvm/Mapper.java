/**
 * This file was automatically generated by the TRLD transpiler.
 * Source: trld/tvm/mapper.py
 */
package trld.tvm;

//import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import java.io.*;

import trld.Builtins;
import trld.KeyValue;

import static trld.jsonld.Base.CONTEXT;
import static trld.jsonld.Base.GRAPH;
import static trld.jsonld.Base.ID;
import static trld.jsonld.Base.TYPE;
import static trld.jsonld.Base.VOCAB;
import static trld.jsonld.Base.asList;

public class Mapper {

  public static Object mapTo(Map targetMap, Object indata) {
    return mapTo(targetMap, indata, false);
  }
  public static Object mapTo(Map targetMap, Object indata, Boolean dropUnmapped) { // LINE: 8
    Object result = (indata instanceof Map ? new HashMap<>() : new ArrayList<>()); // LINE: 9
    modify(targetMap, indata, result, dropUnmapped); // LINE: 10
    return result; // LINE: 11
  }

  protected static void modify(Map targetMap, Object ino, Object outo, Boolean dropUnmapped) { // LINE: 14
    if (ino instanceof Map) { // LINE: 15
      for (Map.Entry<String, Object> k_v : ((Map<String, Object>) ino).entrySet()) { // LINE: 16
        String k = k_v.getKey();
        Object v = k_v.getValue();
        modifyPair(targetMap, k, v, outo, dropUnmapped); // LINE: 17
      }
    } else if (ino instanceof List) { // LINE: 18
      Integer i = 0; // LINE: 19
      for (Object v : (List) ino) { // LINE: 20
        modifyPair(targetMap, i, v, outo, dropUnmapped); // LINE: 21
        i += 1;
      }
    }
  }

  protected static void modifyPair(Map targetMap, Object k, Object v, Object outo, Boolean dropUnmapped) { // LINE: 25
    Map<Object, Object> mapo = map(targetMap, k, v, dropUnmapped); // LINE: 26
    for (Map.Entry<Object, Object> mapk_mapv : mapo.entrySet()) { // LINE: 28
      Object mapk = mapk_mapv.getKey();
      Object mapv = mapk_mapv.getValue();
      Object outv; // LINE: 29
      if (mapv instanceof List) { // LINE: 30
        outv = new ArrayList<>(); // LINE: 31
        modify(targetMap, (List) mapv, outv, dropUnmapped); // LINE: 32
        mapv = (List) outv; // LINE: 33
      } else if (mapv instanceof Map) { // LINE: 34
        outv = new HashMap<>(); // LINE: 35
        modify(targetMap, (Map) mapv, outv, dropUnmapped); // LINE: 36
        mapv = (Map) outv; // LINE: 37
      }
      if (outo instanceof Map) { // LINE: 39
        if (((Map) outo).containsKey(mapk)) { // LINE: 40
          List values = (List) asList(((Map) outo).get(mapk)); // LINE: 41
          values.addAll(asList(mapv));
          mapv = (Object) values; // LINE: 43
        }
        ((Map) outo).put(mapk, mapv); // LINE: 45
      } else {
        ((List) outo).add(mapv); // LINE: 47
      }
    }
  }

  protected static Map map(Map targetMap, Object key, Object value) {
    return map(targetMap, key, value, false);
  }
  protected static Map map(Map targetMap, Object key, Object value, Boolean dropUnmapped) { // LINE: 50
    Object somerule = (Object) targetMap.get(key); // LINE: 51
    if ((dropUnmapped && key instanceof String && !((String) key).substring(0, 0 + 1).equals("@") && somerule == null)) { // LINE: 53
      return new HashMap<>(); // LINE: 54
    }
    if (value instanceof List) { // LINE: 56
      List<Object> remapped = new ArrayList<>(); // LINE: 57
      for (Object v : (List) value) { // LINE: 58
        Object item = ((v instanceof String && targetMap.containsKey(v)) ? targetMap.get(v) : v); // LINE: 59
        if (item instanceof List) { // LINE: 60
          remapped.addAll((List) item);
        } else {
          remapped.add(item); // LINE: 63
        }
      }
      value = (List) remapped; // LINE: 64
    }
    if (somerule == null) { // LINE: 66
      return Builtins.mapOf(key, value); // LINE: 67
    }
    Map out = new HashMap<>(); // LINE: 69
    Set<String> mappedKeypaths = new HashSet(); // LINE: 71
    for (Object rule : asList(somerule)) { // LINE: 73
      if (rule instanceof String) { // LINE: 74
        out.put((String) rule, value); // LINE: 75
        break; // LINE: 76
      }
      if (rule instanceof Map) { // LINE: 78
        List<Map> objectvalues = (List<Map>) value; // LINE: 79
        /*@Nullable*/ String property = (/*@Nullable*/ String) ((Map) rule).get("property"); // LINE: 81
        /*@Nullable*/ String propertyFrom = (/*@Nullable*/ String) ((Map) rule).get("propertyFrom"); // LINE: 82
        if (propertyFrom != null) { // LINE: 85
          Map first = (Map) objectvalues.get(0); // LINE: 86
          List<Map> propertyFromObject = (List<Map>) first.get(propertyFrom); // LINE: 87
          property = (String) propertyFromObject.get(0).get(ID); // LINE: 88
        }
        if (targetMap.containsKey(property)) { // LINE: 90
          property = (String) asList(targetMap.get(property)).get(0); // LINE: 91
        }
        List<Object> outvalue = new ArrayList<>(); // LINE: 93
        /*@Nullable*/ String valueFrom = (/*@Nullable*/ String) ((Map) rule).get("valueFrom"); // LINE: 96
        if (valueFrom != null) { // LINE: 97
          for (Map v : objectvalues) { // LINE: 98
            assert v instanceof Map;
            /*@Nullable*/ Map<String, String> match = (/*@Nullable*/ Map<String, String>) ((Map) rule).get("match"); // LINE: 100
            if ((match == null || (match.containsKey(TYPE) && ((List) ((Map) v).get(TYPE)).stream().anyMatch(t -> (t == null && ((Object) match.get(TYPE)) == null || t != null && (t).equals(match.get(TYPE))))))) { // LINE: 101
              Object vv = (Object) ((Map) v).get(valueFrom); // LINE: 103
              if (vv instanceof List) { // LINE: 104
                for (Object m : (List) vv) { // LINE: 105
                  outvalue.add(m); // LINE: 106
                }
              } else {
                outvalue.add(vv); // LINE: 108
              }
            }
          }
        } else {
          outvalue = (List<Object>) value; // LINE: 110
        }
        List<Object> mappedvalue = new ArrayList<>(); // LINE: 113
        for (Object v : outvalue) { // LINE: 114
          mappedvalue.add((v instanceof String ? targetMap.getOrDefault((String) v, (String) v) : v)); // LINE: 115
        }
        outvalue = mappedvalue; // LINE: 116
        if ((property != null && (outvalue != null && outvalue.size() > 0))) { // LINE: 118
          if (valueFrom != null) { // LINE: 119
            String mappedKey = key + " " + valueFrom; // LINE: 120
            if (mappedKeypaths.contains(mappedKey)) { // LINE: 123
              continue; // LINE: 124
            }
            mappedKeypaths.add(mappedKey); // LINE: 126
          }
          out.put(property, outvalue); // LINE: 128
        }
      }
    }
    return out; // LINE: 133
  }
}
