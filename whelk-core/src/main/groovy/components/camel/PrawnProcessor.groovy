package se.kb.libris.whelks.camel

import groovy.util.logging.Slf4j as Log

import se.kb.libris.whelks.*
import se.kb.libris.whelks.plugin.*

import org.apache.camel.*
import org.apache.camel.impl.*
import org.apache.camel.processor.*
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.model.dataformat.JsonLibrary

@Log
class PrawnRunner extends BasicPlugin implements Processor, WhelkAware {

    Whelk whelk
    List<Transmogrifier> transmogrifiers

    void bootstrap(String w) {
        transmogrifiers = plugins.findAll { it instanceof Transmogrifier }
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        Document doc = whelk.get(new URI(exchange.getIn().getHeader("entry:identifier")))
        if (doc) {
            log.info("Prawnrunner got ${doc.identifier} from queue.")
                String checksum = doc.checksum
                for (t in transmogrifiers) {
                    doc = t.transmogrify(doc)
                }
            if (doc.checksum != checksum) {
                log.info("Checksum updated. Now saving it again.")
                log.info("entry: ${doc.entry}")
                whelk.add(doc)
            }
        }
    }
}
