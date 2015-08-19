package whelk;

import java.util.Map;
import java.util.List;

public interface Document {
    static final String CREATED_KEY = "created";
    static final String MODIFIED_KEY = "modified";
    static final String DELETED_KEY = "deleted";
    static final String CONTENT_TYPE_KEY = "contentType";

    public String getIdentifier();
    public void setIdentifier(String id);

    public String getContentType();
    public void setContentType(String contenttype);
    public long getCreated();
    public long getModified();
    public void setModified(long ts);
    public String getChecksum();
    public String getDataset();
    public int getVersion();
    public byte[] getData();
    public List<String> getIdentifiers();
    public List<String> getDatasets();
    public void addIdentifier(String id);
    public void addDataset(String id);

    public Document withData(byte [] data);
    public long updateModified();

    public Map<String,Object> getManifest();
    public void setManifest(Map<String, Object> entry);

    public String getManifestAsJson();

    public boolean isJson();
    public boolean isDeleted();
}
