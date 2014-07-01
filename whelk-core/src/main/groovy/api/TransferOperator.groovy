package se.kb.libris.whelks.api

import groovy.util.logging.Slf4j as Log

import org.codehaus.jackson.map.*
import org.codehaus.jackson.map.SerializationConfig.Feature

import java.util.concurrent.*

import se.kb.libris.whelks.*
import se.kb.libris.whelks.component.*
import se.kb.libris.whelks.exception.*
import se.kb.libris.whelks.importers.*
import se.kb.libris.whelks.plugin.*

import se.kb.libris.conch.Tools

@Log
@Deprecated
class TransferOperator extends AbstractOperator {
    String oid = "transfer"

    // Unique for this operator
    String fromStorage = null
    String toStorage = null

    boolean showSpinner = false

    @Override
    void setParameters(Map parameters) {
        super.setParameters(parameters)
        this.fromStorage = parameters.get("fromStorage", null)?.first()
        this.toStorage = parameters.get("toStorage", null)?.first()
        this.showSpinner = parameters.get("showSpinner", false)
        assert fromStorage
        assert toStorage
    }

    void doRun(long startTime) {
        Storage targetStorage = whelk.getStorages().find { it.id == toStorage }
        Storage sourceStorage = whelk.getStorages().find { it.id == fromStorage }
        assert targetStorage, sourceStorage
        List docs = []
        log.info("Transferring data from $fromStorage to $toStorage")
        boolean versioningOriginalSetting = targetStorage.versioning
        targetStorage.versioning = false
        for (doc in sourceStorage.getAll(dataset)) {
            log.trace("Storing doc ${doc.identifier} with type ${doc.contentType}")
            if (!doc.entry.deleted) {
                docs << doc
            } else {
                log.warn("Document ${doc.identifier} is deleted. Don't try to add it.")
            }
            runningTime = System.currentTimeMillis() - startTime
            if (count++ % 10000 == 0) {
                targetStorage.bulkAdd(docs, docs.first().contentType)
                docs = []
            }
            if (showSpinner) {
                def velocityMsg = "Current velocity: ${count/(runningTime/1000)}."
                Tools.printSpinner("Transferring from ${fromStorage} to ${toStorage}. ${count} documents transferred sofar. $velocityMsg", count)
            }
            if (cancelled) {
                break
            }
        }

        if (docs.size() > 0) {
            targetStorage.bulkAdd(docs, docs.first().contentType)
        }
        log.info("Transferred $count documents in ${((System.currentTimeMillis() - startTime)/1000)} seconds." as String)
        targetStorage.versioning = versioningOriginalSetting
        operatorState=OperatorState.FINISHING
    }

    @Override
    Map getStatus() {
        def map = super.getStatus()
        if (hasRun) {
            if (fromStorage) {
                map.get("lastrun").put("fromStorage", fromStorage)
                map.get("lastrun").put("toStorage", toStorage)
            }
        } else {
            if (fromStorage) {
                map.put("fromStorage", fromStorage)
                map.put("toStorage", toStorage)
            }
        }
        return map
    }
}
