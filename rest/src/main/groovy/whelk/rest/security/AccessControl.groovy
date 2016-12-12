package whelk.rest.security
import groovy.util.logging.Slf4j as Log

import whelk.Document
import whelk.JsonLd
import whelk.exception.ModelValidationException

@Log
class AccessControl {

    boolean checkDocument(Document newdoc, Document olddoc, Map userPrivileges) {
        def privs = null
        if (isHolding(newdoc)) {
            if (newdoc) {
                JsonLd.validateItemModel(newdoc)
                def sigel = JsonLd.frame(newdoc.id, newdoc.data).about.heldBy.notation
                log.debug("User tries to change a holding for sigel ${sigel}.")

                privs = userPrivileges.authorization.find { it.sigel == sigel }
                log.trace("User has these privs for ${sigel}: $privs")
                if (!privs?.xlreg) {
                    log.debug("User does not have sufficient privileges.")
                    return false
                }
            }
            if (olddoc && !olddoc.deleted && isHolding(olddoc)) {
                def currentSigel = JsonLd.frame(olddoc.id, olddoc.data).about.heldBy.notation
                if (currentSigel) {
                    log.trace("Checking sigel privs for existing document.")
                    privs = userPrivileges.authorization.find { it.sigel == currentSigel }
                    log.trace("User has these privs for current sigel ${sigel}: $privs")
                    if (!privs?.xlreg) {
                        log.debug("User does NOT have enough privileges.")
                        return false
                    }
                }
            }
        } else {
            privs = userPrivileges.authorization.find { it.kat == true }
            if (!privs?.kat) {
                log.info("User does NOT have privileges to edit bib or auth.")
                return false
            }
        }

        if (newdoc) {
            // FIXME what should we do here?
            // newdoc.manifest.lastChangeBy = userPrivileges.username
        }
        log.debug("User is authorized to make the change.")
        return true
    }

    boolean isHolding(Document doc) {
      // FIXME this needs to be implemented in whelk-core
      return false
    }
}
