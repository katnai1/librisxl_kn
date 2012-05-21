package se.kb.libris.whelks;

import java.io.InputStream;
import java.net.URI;
import java.util.Date;

public interface Document {
    public URI getIdentifier();
    public String getVersion();
    public Date getTimestamp();
        
    public byte[] getData();
    public byte[] getData(long offset, long length);
    public InputStream getDataAsStream();
    public InputStream getDataAsStream(long offset, long length);
    public String getContentType();
    public long getSize();
    
    public Iterable<? extends Link> getLinks();
    public Iterable<? extends Key> getKeys();
    public Iterable<? extends Tag> getTags();
    public Iterable<? extends Description> getDescriptions();
    
    public Document tag(URI type, String value);
    public Document untag(URI type, String value);
    
    public Document withIdentifier(URI uri);
    public Document withData(byte[] data);
    public Document withDataAsStream(InputStream data);
    public Document withContentType(String contentType);
    public Document withLinks(Iterable<? extends Link> links);
    public Document withKeys(Iterable<? extends Key> keys);
    public Document withDescriptions(Iterable<? extends Description> descriptions);
}
