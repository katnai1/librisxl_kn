package se.kb.libris.whelks.component;

import java.net.URI;
import java.util.*;
import se.kb.libris.whelks.Query;
import se.kb.libris.whelks.Document;
import se.kb.libris.whelks.SearchResult;

public interface Index extends Component {
    public void index(Document d);
    public void index(Iterable<Document> d);
    public SearchResult query(Query query);
    public void delete(URI uri);    
}
