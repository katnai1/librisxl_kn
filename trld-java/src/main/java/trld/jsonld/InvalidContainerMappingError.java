/**
 * This file was automatically generated by the TRLD transpiler.
 * Source: trld/jsonld/context.py
 */
package trld.jsonld;

//import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import java.io.*;

import trld.Builtins;
import trld.KeyValue;

import trld.jsonld.LoadDocumentCallback;
import trld.jsonld.LoadDocumentOptions;
import static trld.jsonld.Docloader.getDocumentLoader;
import static trld.platform.Common.resolveIri;
import static trld.platform.Common.warning;
import static trld.jsonld.Base.*;
import static trld.jsonld.Context.*;


public class InvalidContainerMappingError extends JsonLdError {
  public InvalidContainerMappingError() { };
  public InvalidContainerMappingError(String msg) { super(msg); };
}
