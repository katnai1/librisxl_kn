package se.kb.libris.whelks.component

import groovy.util.logging.Slf4j as Log

import java.util.concurrent.*

import org.codehaus.jackson.map.ObjectMapper

import se.kb.libris.whelks.exception.*
import se.kb.libris.whelks.plugin.*
import se.kb.libris.whelks.*

@Log
abstract class BasicComponent extends BasicPlugin implements Component {

    public static final ObjectMapper mapper = new ObjectMapper()
    static final String LAST_UPDATED = "last_updated"
    static final String LISTENER_FAILED_AT = "listener_crashed"
    static final String LISTENER_FAILED_REASON = "listener_crashed_because"
    static final String STATUS_OK = "ok"
    static final String STATUS_TERMINATED = "terminated"

    static final String STATE_FILE_SUFFIX = ".state"

    protected Map componentState
    boolean stateUpdated

    Whelk whelk
    List contentTypes

    boolean master = false

    int batchUpdateSize = 1000 // Default. May be overriden by whelk.json

    // Plugins
    Map<String,FormatConverter> formatConverters = new HashMap<String,FormatConverter>()
    Map<String,DocumentSplitter> documentSplitters = new HashMap<String,DocumentSplitter>()
    List<LinkExpander> linkExpanders = new ArrayList<LinkExpander>()
    Map<String,Component> components = new HashMap<String,Component>()

    Listener listener
    Thread listenerThread = null
    String listenerStatus = STATUS_OK
    Thread stateThread = null
    String stateStatus = STATUS_OK


    private File stateFile

    final void bootstrap(String whelkId) {
        assert whelk
        stateFile= new File("${global.WHELK_WORK_DIR}/${whelkId}_${this.id}${STATE_FILE_SUFFIX}")
        if (stateFile.exists()) {
            log.info("Loading persisted state for [${this.id}].")
            try {
                componentState = mapper.readValue(stateFile, Map)
            } catch (Exception e) {
                log.error("[${this.id}] Failed to read statefile: ${e.message}", e)
                throw new WhelkRuntimeException("Couldn't read statefile for ${whelkId}/${this.id}", e)
            }
        } else {
            componentState = [:]
            componentState[LAST_UPDATED] = 0L
        }
        componentState['componentId'] = this.id
        componentState['status'] = "started"

        componentBootstrap(whelkId)
    }

    abstract void componentBootstrap(String str)

    @Override
    public final void start() {
        assert whelk
        log.debug("[${this.id}] Loading format converters")
        for (f in plugins.findAll { it instanceof FormatConverter }) {
            formatConverters.put(f.requiredContentType, f)
        }
        log.debug("[${this.id}] Loading document splitters")
        for (d in plugins.findAll { it instanceof DocumentSplitter }) {
            documentSplitters.put(d.requiredContentType, d)
        }
        // Look for components configured for whelk. Saves having to add component to all components in plugins.
        for (c in whelk.getComponents()) {
            components.put(c.id, c)
        }

        this.listener = plugins.find { it instanceof Listener }

        log.debug("Calling onStart() on sub classes")
        onStart()

        startStateThread(stateFile, this.whelk.id)
        startListenerThread()

    }

    void onStart() {
        log.info("[${this.id}] onStart() not overridden.")
    }

    @Override
    public final URI add(final Document document) {
        try {
            long updatetime = document.timestamp
            List<Document> docs = prepareDocs([document], document.contentType)
            batchLoad(docs)
            log.debug("Calling setState")
            setState(LAST_UPDATED, updatetime)
            return new URI(document.identifier)
        } catch (DownForMaintenanceException dfme) {
            throw dfme
        } catch (Exception e) {
            log.error("[${this.id}] failed to add documents. (${e.message})", e)
            throw e
        }
    }

    @Override
    public final void bulkAdd(final List<Document> documents, String contentType) {
        log.debug("[${this.id}] bulkAdd called with ${documents.size()} documents.")
        try {
            long startBatchAt = System.currentTimeMillis()
            long updatetime = documents.last().timestamp
            log.trace("First document timestamp: ${documents.first().timestamp}")
            log.trace(" Last document timestamp: ${documents.last().timestamp}")
            log.debug("Updatetime is $updatetime")
            def docs = prepareDocs(documents, contentType)
            log.debug("[${this.id}] Calling batchload on ${this.id} with batch of ${docs.size()}")
            batchLoad(docs)
            log.debug("Bulk calling setState")
            setState(LAST_UPDATED, updatetime)
            log.debug("[${this.id}] Bulk Add completed in ${(System.currentTimeMillis()-startBatchAt)/1000} seconds.")
        } catch (DownForMaintenanceException dfme) {
            throw dfme
        } catch (Exception e) {
            log.error("[${this.id}] failed to add documents. (${e.message})", e)
            throw e
        }
    }

    @Override
    boolean handlesContent(String ctype) {
        return (ctype == "*/*" || !this.contentTypes || this.contentTypes.contains(ctype) || this.contentTypes.contains(formatConverters.get(ctype)?.resultContentType))
    }

    protected abstract void batchLoad(List<Document> docs);

    List<Document> prepareDocs(final List<Document> documents, String contentType) {
        FormatConverter fc = formatConverters.get(contentType)
        boolean shouldConvert = (fc != null || !linkExpanders.isEmpty())
        if (!shouldConvert)
            return documents
        List docs = []
        for (doc in documents) {
            if (fc) {
                doc = fc.convert(doc)
            }
            docs.add(linkExpand(doc))
        }
        return docs
    }

    Document linkExpand(Document doc) {
        LinkExpander le = getLinkExpanderFor(doc)
        if (le) {
            log.debug("Linkexpanding ${doc.identifier}")
            return le.expand(doc)
        }
        return doc
    }

    final Listener getListener() {
        if (!this.listener) {
            throw new WhelkRuntimeException("Listener is null, but clearly required. Has start() been called?")
        }
        return this.listener
    }

    LinkExpander getLinkExpanderFor(Document doc) {
        return linkExpanders.find { it.valid(doc) }
    }

    public Map getState() { return componentState.asImmutable() }

    void setState(key, value) {
        log.debug("[${this.id}] Setting $key = $value")
        this.componentState.put(key, value)
        stateUpdated = true
        if (key == LAST_UPDATED && listener) {
            log.debug("Setting $LAST_UPDATED to ${new Date(value)}")
            log.trace("[${this.id}] Notifying listeners ..")
            listener.registerUpdate(this.id, value)
        }
    }

    long getLastUpdatedState() {
        return componentState.get(LAST_UPDATED, 0L)
    }

    void startStateThread(File stateFile, String whelkId) {
        log.info("[${this.id}] Starting thread to periodically save state.")
        if (!stateThread) {
            stateThread = Thread.start {
                boolean ok = true
                while(ok) {
                    try {
                        if (getState() && stateUpdated) {
                            log.trace("[${whelkId}-${this.id}] Saving state ...")
                            mapper.writeValue(stateFile, getState())
                            stateUpdated = false
                        }
                        Thread.sleep(3000)
                    } catch (Exception e) {
                        ok = false
                        stateStatus = "${e.message} [terminated]"
                        log.error("[${this.id}] Failed to write statefile: ${e.message}", e)
                        throw new WhelkRuntimeException("Couldn't write statefile for ${whelkId}/${this.id}", e)
                    }
                }
            }
        }
    }

    void startListenerThread() {
        if (listener && listener.hasQueue(this.id) && !listenerThread) {

            listenerThread = Thread.start {
                catchUp()

                log.info("[${this.id}] Starting notification listener.");
                boolean ok = true
                while (ok) {
                    try {
                        log.info("[${this.id}] Waiting for update ...");
                        ListenerEvent e = listener.nextUpdate(this.id)
                        log.debug("[${this.id}] Received update from component ${e.senderId} = ${e.payload}");
                        long lastUpdate = getLastUpdatedState()
                        log.trace("Lastupdate: $lastUpdate") 
                        log.trace("   Payload: ${e.payload}")
                        if (lastUpdate < e.payload || e.force) {
                            if (e.force) {
                                log.info("[${this.id}] Forced update. Component was last updated ${new Date(lastUpdate)} ($lastUpdate), but update is forced (${e.payload}).");
                                lastUpdate = e.payload
                            } else {
                                log.info("[${this.id}] Component was last updated ${new Date(lastUpdate)} ($lastUpdate). Must catch up.");
                            }
                            loadDocumentsFromComponentSince(e.senderId, lastUpdate)
                        } else {
                            log.debug("[${this.id}] Already up to date.");
                        }
                    } catch (Exception ex) {
                        log.error("[${this.id}] DISABLING LISTENER because of unexpected exception. Possible restart required.")
                        ok = false
                        listenerStatus = "${e.message} [terminated]"
                        listener.disable(this.id)
                        throw ex
                    }
                }
            }
        } else if (!listenerThread) {
            log.info("[${this.id}] No listener queue registered for me.")
            listenerStatus = "not active"
        }
    }

    void loadDocumentsFromComponentSince(String componentId, long lastUpdate) {
        if (!componentId) {
            throw new WhelkRuntimeException("[${this.id}] Can not load documents from $componentId")
        }
        long lastSuccessfulTimestamp = lastUpdate
        try {
            log.debug("Load from $componentId since $lastUpdate")
            def docs = []
            int count = 0
            for (doc in components.get(componentId).getAll(null, new Date(lastUpdate), null)) {
                docs << doc
                if ((++count % batchUpdateSize) == 0) {
                    lastSuccessfulTimestamp = docs.first().timestamp
                    bulkAdd(docs, docs.first().contentType)
                    docs = []
                }
            }
            // Remainder
            if (docs.size() > 0) {
                lastSuccessfulTimestamp = docs.first().timestamp
                log.debug("[${this.id}] Still ${docs.size()} documents left to process.")
                bulkAdd(docs, docs.first().contentType)
            }
            log.info("[${this.id}] Loaded $count document to get up to date with ${componentId}")
        } catch (Exception e) {
            setState(LISTENER_FAILED_AT, lastSuccessfulTimestamp)
            setState(LISTENER_FAILED_REASON, e?.message)
            log.error("[${this.id}] Failed to read from notification queue: ${e.message}")
            throw e
        }
    }

    void catchUp() {
        if (componentState.containsKey(LISTENER_FAILED_AT)) {
            log.info("Hmm, seems like \"${this.id}\" crashed last time ... trying to catch up.")
            for (component in listener.getMyNotifiers(this.id)) {
                loadDocumentsFromComponentSince(component, componentState.get(LISTENER_FAILED_AT))
                this.componentState.remove(LISTENER_FAILED_AT)
                this.componentState.remove(LISTENER_FAILED_REASON)
            }
        } else if (componentState.get(LAST_UPDATED, 0) > 0) {
            log.info("[${this.id}] Doing normal component catchup ...")
            for (component in listener.getMyNotifiers(this.id)) {
                loadDocumentsFromComponentSince(component, componentState.get(LAST_UPDATED))
            }
        } else {
            log.info("[${this.id}] component state has epoch as last update. Will not update automatically. YOU must do it manually.")
        }
    }
}
