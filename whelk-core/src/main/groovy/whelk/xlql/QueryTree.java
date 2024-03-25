package whelk.xlql;

import whelk.JsonLd;

import java.util.*;
import java.util.stream.Collectors;

public class QueryTree {
    public sealed interface Node permits And, Or, Nested, Field, FreeText {
    }

    public record And(List<Node> conjuncts) implements Node {
    }

    public record Or(List<Node> disjuncts) implements Node {
    }

    public record Nested(List<Field> fields, Operator operator) implements Node {
    }

    public record Field(Path path, Operator operator, String value) implements Node {
    }

    public record FreeText(Operator operator, String value) implements Node {
    }

    public Node tree;

    public QueryTree(SimpleQueryTree sqt, Disambiguate disambiguate) {
        this.tree = sqtToQt(sqt.tree, disambiguate);
    }

    private static Node sqtToQt(SimpleQueryTree.Node sqt, Disambiguate disambiguate) {
        Disambiguate.OutsetType outset = decideOutset(sqt, disambiguate);
        return sqtToQt(sqt, disambiguate, outset);
    }

    private static Disambiguate.OutsetType decideOutset(SimpleQueryTree.Node sqtNode, Disambiguate disambiguate) {
        Set<String> givenTypes = collectGivenTypes(sqtNode);
        Set<Disambiguate.OutsetType> outset = givenTypes.stream()
                .map(disambiguate::getOutsetType)
                .collect(Collectors.toSet());

        // TODO: Review this (for now default to Resource)
        return outset.size() == 1 ? outset.stream().findFirst().get() : Disambiguate.OutsetType.RESOURCE;
    }

    private static Node sqtToQt(SimpleQueryTree.Node sqtNode, Disambiguate disambiguate, Disambiguate.OutsetType outset) {
        switch (sqtNode) {
            case SimpleQueryTree.And and -> {
                List<Node> conjuncts = and.conjuncts()
                        .stream()
                        .map(c -> sqtToQt(c, disambiguate, outset))
                        .toList();
                return new And(conjuncts);
            }
            case SimpleQueryTree.Or or -> {
                List<Node> disjuncts = or.disjuncts()
                        .stream()
                        .map(d -> sqtToQt(d, disambiguate, outset))
                        .toList();
                return new Or(disjuncts);
            }
            case SimpleQueryTree.FreeText ft -> {
                return new FreeText(ft.operator(), ft.value());
            }
            case SimpleQueryTree.PropertyValue pv -> {
                return "rdf:type".equals(pv.property())
                        ? buildTypeField(pv, disambiguate)
                        : buildField(pv, disambiguate, outset);
            }
        }
    }

    private static Field buildField(SimpleQueryTree.PropertyValue pv) {
        Path path = new Path(pv.propertyPath());
        String value = JsonLd.ID_KEY.equals(pv.propertyPath().getLast())
                ? Disambiguate.expandPrefixed(pv.value())
                : pv.value();
        return new Field(path, pv.operator(), value);
    }

    private static Node buildField(SimpleQueryTree.PropertyValue pv, String altValue) {
        return new Field(new Path(pv.propertyPath()), pv.operator(), altValue);
    }

    private static Node buildField(SimpleQueryTree.PropertyValue pv, Disambiguate disambiguate, Disambiguate.OutsetType outset) {
        boolean isAccuratePath = pv.propertyPath().size() > 1;

        Path path = new Path(pv.propertyPath());
        Operator operator = pv.operator();
        String value = pv.value();

        if (disambiguate.isObjectProperty(pv.property()) && !disambiguate.isVocabTerm(pv.property())) {
            /*
             If "vocab term" interpret the value as is, e.g. issuanceType: "Serial" or encodingLevel: "marc:FullLevel".
             Otherwise, when object property, append either @id or _str to the path.
             */
            String expanded = Disambiguate.expandPrefixed(pv.value());
            if (JsonLd.looksLikeIri(expanded)) {
                path.appendId();
                value = expanded;
            } else {
                path.appendUnderscoreStr();
            }
        }

        if (isAccuratePath) {
            return new Field(path, operator, value);
        }

        path.expandChainAxiom(disambiguate);

        String domain = disambiguate.getDomain(pv.property());

        Disambiguate.DomainCategory domainCategory = disambiguate.getDomainCategory(domain);
        if (domainCategory == Disambiguate.DomainCategory.ADMIN_METADATA) {
            path.prependMeta();
        }

        return switch (outset) {
            case WORK -> {
                switch (domainCategory) {
                    case INSTANCE, EMBODIMENT -> {
                        // The property p appears only on instance, modify path to @reverse.instanceOf.p...
                        path.setWorkToInstancePath();
                        yield newFields(path, operator, value, disambiguate);
                    }
                    case CREATION_SUPER, UNKNOWN -> {
                        // The property p may appear on instance, add alternative path @reverse.instanceOf.p...
                        List<Node> altFields = new ArrayList<>();
                        Path copy = path.copy();
                        copy.setWorkToInstancePath();
                        altFields.add(newFields(path, operator, value, disambiguate));
                        altFields.add(newFields(copy, operator, value, disambiguate));
                        yield operator == Operator.NOT_EQUALS ? new And(altFields) : new Or(altFields);
                    }
                    default -> {
                        yield newFields(path, operator, value, disambiguate);
                    }
                }
            }
            case INSTANCE -> {
                switch (domainCategory) {
                    case WORK -> {
                        // The property p appears only work, modify path to instanceOf.p...
                        path.setInstanceToWorkPath();
                        yield newFields(path, operator, value, disambiguate);
                    }
                    case CREATION_SUPER, UNKNOWN -> {
                        // The property p may appear on work, add alternative path instanceOf.p...
                        List<Node> altFields = new ArrayList<>();
                        Path copy = path.copy();
                        copy.setInstanceToWorkPath();
                        altFields.add(newFields(path, operator, value, disambiguate));
                        altFields.add(newFields(copy, operator, value, disambiguate));
                        yield operator == Operator.NOT_EQUALS ? new And(altFields) : new Or(altFields);
                    }
                    default -> {
                        yield newFields(path, operator, value, disambiguate);
                    }
                }
            }
            case RESOURCE -> newFields(path, operator, value, disambiguate);
        };
    }

    static Node newFields(Path path, Operator operator, String value, Disambiguate disambiguate) {
        Field f = new Field(path, operator, value);

        if (path.defaultFields.isEmpty()) {
            return f;
        }

        List<Field> fields = new ArrayList<>(List.of(f));

        path.defaultFields.forEach(df -> {
                    Path dfPath = new Path(df.path());
                    if (disambiguate.isObjectProperty(df.path().getLast()) && JsonLd.looksLikeIri(df.value())) {
                        dfPath.appendId();
                    }
                    fields.add(new Field(dfPath, operator, df.value()));
                }
        );

        return new Nested(fields, operator);
    }

    private static Node buildTypeField(SimpleQueryTree.PropertyValue pv, Disambiguate disambiguate) {
        Set<String> altTypes = "Work".equals(pv.value())
                ? disambiguate.workTypes
                : ("Instance".equals(pv.value()) ? disambiguate.instanceTypes : Collections.emptySet());

        if (altTypes.isEmpty()) {
            return buildField(pv);
        }

        List<Node> altFields = altTypes.stream()
                .sorted()
                .map(type -> buildField(pv, type))
                .toList();

        return pv.operator() == Operator.NOT_EQUALS ? new And(altFields) : new Or(altFields);
    }

    public static Set<String> collectGivenTypes(SimpleQueryTree.Node sqt) {
        return collectGivenTypes(sqt, new HashSet<>());
    }

    private static Set<String> collectGivenTypes(SimpleQueryTree.Node sqtNode, Set<String> types) {
        switch (sqtNode) {
            case SimpleQueryTree.And and -> and.conjuncts().forEach(c -> collectGivenTypes(c, types));
            case SimpleQueryTree.Or or -> or.disjuncts().forEach(d -> collectGivenTypes(d, types));
            case SimpleQueryTree.PropertyValue pv -> {
                if (List.of("rdf:type").equals(pv.propertyPath())) {
                    types.add(pv.value());
                }
            }
            case SimpleQueryTree.FreeText ignored -> {
                // Nothing to do here
            }
        }

        return types;
    }
}
