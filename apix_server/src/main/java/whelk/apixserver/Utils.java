package whelk.apixserver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import se.kb.libris.util.marc.Controlfield;
import se.kb.libris.util.marc.MarcFieldComparator;
import se.kb.libris.util.marc.MarcRecord;
import se.kb.libris.util.marc.io.MarcXmlRecordReader;
import whelk.Document;
import whelk.IdGenerator;
import whelk.JsonLd;
import whelk.Whelk;
import whelk.component.ElasticSearch;
import whelk.component.PostgreSQLComponent;
import whelk.converter.MarcJSONConverter;
import whelk.converter.marc.JsonLD2MarcXMLConverter;
import whelk.converter.marc.MarcFrameConverter;
import whelk.util.LegacyIntegrationTools;
import whelk.util.PropertyLoader;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class Utils
{
    static final String APIX_BASEURI = "https://apix.libris.kb.se/apix";
    static final String APIX_SYSTEM_CODE = "APIX";
    static Whelk s_whelk;
    static JsonLd s_jsonld; // For model driven behaviour
    private static JsonLD2MarcXMLConverter s_toMarcConverter = new JsonLD2MarcXMLConverter();
    private static MarcFrameConverter s_toJsonLdConverter = new MarcFrameConverter();
    private static final Logger s_logger = LogManager.getLogger(Utils.class);

    static
    {
        Properties configuration = PropertyLoader.loadProperties("secret");
        PostgreSQLComponent postgreSqlComponent =
                new PostgreSQLComponent(configuration.getProperty("sqlUrl"), configuration.getProperty("sqlMaintable"));
        ElasticSearch elasticSearch = new ElasticSearch(
                configuration.getProperty("elasticHost"),
                configuration.getProperty("elasticCluster"),
                configuration.getProperty("elasticIndex"));
        s_whelk = new Whelk(postgreSqlComponent, elasticSearch);
        s_whelk.loadCoreData();
        Map displayData = s_whelk.getDisplayData();
        Map vocabData = s_whelk.getVocabData();
        s_jsonld = new JsonLd(displayData, vocabData);
    }

    static String convertToMarcXml(Document document) throws TransformerException, IOException
    {
        try
        {
            return (String) s_toMarcConverter.convert(document.data, document.getShortId()).get(JsonLd.getNON_JSON_CONTENT_KEY());
        }
        catch (Exception | Error e)
        {
            return null;
        }
    }

    /**
     * itemOfSystemId (a fnrgl-string) is only relevant when converting a holding record. Set to null otherwise.
     */
    static Document convertToRDF(String marcXmlString, String expectedCollection, String itemOfSystemId) throws IOException
    {
        try
        {
            InputStream marcXmlInputStream = new ByteArrayInputStream(marcXmlString.getBytes("UTF-8"));
            MarcXmlRecordReader reader = new MarcXmlRecordReader(marcXmlInputStream, "/record");
            MarcRecord marcRecord = reader.readRecord();

            String generatedId = IdGenerator.generate();

            // Delete any existing 001 fields (the incoming record is not allowed to decide it's own libris ID).
            if (marcRecord.getControlfields("001").size() != 0)
            {
                marcRecord.getFields().remove(marcRecord.getControlfields("001").get(0));
            }

            // The conversion process needs a 001 field to work correctly.
            if (marcRecord.getControlfields("001").size() == 0)
                marcRecord.addField(marcRecord.createControlfield("001", generatedId));

            // If this is a holding record, 004 needs to contain the correct bib id to link with
            if (itemOfSystemId != null && expectedCollection.equals("hold"))
            {
                List<Controlfield> cfs = (List<Controlfield>) marcRecord.getControlfields("004");

                if (cfs.isEmpty())
                    marcRecord.addField(marcRecord.createControlfield("004", String.valueOf(itemOfSystemId)), MarcFieldComparator.strictSorted);
                else if (!String.valueOf(itemOfSystemId).equals(cfs.get(0).getData()))
                {
                    s_logger.error("Cannot accept incoming marc hold record. Marc field 004 did not match the bibid the holding was for.");
                    return null;
                }
            }

            Map convertedData = s_toJsonLdConverter.convert(MarcJSONConverter.toJSONMap(marcRecord), generatedId);
            Document document = new Document(convertedData);

            String contentClassifiedAsCollection = LegacyIntegrationTools.determineLegacyCollection(document, s_jsonld);

            if (contentClassifiedAsCollection.equals(expectedCollection))
                return document;
            return null;
        } catch (Throwable e)
        {
            s_logger.error("Conversion from MARC failed.", e);
            return null;
        }
    }

    static Document getXlDocument(String bibId, String collection)
    {
        String xlUri = mapApixIDtoXlUri(bibId, collection);
        String xlShortId = s_whelk.getStorage().getSystemIdByIri(xlUri);
        if (xlShortId == null)
            return null;
        Document document = s_whelk.getStorage().load(xlShortId);

        if (document.getDeleted())
            return null;
        return document;
    }

    static String mapApixIDtoXlUri(String apixID, String collection)
    {
        // strictly numerical positive id, less than 15 chars means an old voyager ID (bibid)
        if (apixID.matches("\\d+") && apixID.length() < 15)
        {
            String voyagerIdUri = "http://libris.kb.se/" + collection + "/" + apixID;
            return s_whelk.getStorage().getRecordId(voyagerIdUri);
        }
        else
            return Document.getBASE_URI().toString() + apixID;
    }

    static String[] getPathSegmentParameters(HttpServletRequest request)
    {
        String pathInfo = request.getPathInfo().trim();
        if (pathInfo.startsWith("/"))
            pathInfo = pathInfo.substring(1);
        if (pathInfo.endsWith("/"))
            pathInfo = pathInfo.substring(0, pathInfo.length()-1);
        return pathInfo.split("/");
    }

    static boolean validateParameters(HttpServletResponse response, String[] pathSegments, int expectedParameterCount)
            throws TransformerException, IOException
    {
        if (pathSegments.length != expectedParameterCount)
        {
            send200Response(response, Xml.formatApixErrorResponse("Expected " + expectedParameterCount +
                    " segments after /cat/ but there was " + pathSegments.length, ApixCatServlet.ERROR_PARAM_COUNT));
            return false;
        }
        if ( pathSegments.length > 0 && !pathSegments[0].equals("libris") )
        {
            send200Response(response, Xml.formatApixErrorResponse("Database path segment must be \"libris\"", ApixCatServlet.ERROR_DB_NOT_LIBRIS));
            return false;
        }
        if ( pathSegments.length > 1 &&
                !pathSegments[1].equals("bib") && !pathSegments[1].equals("auth") && !pathSegments[1].equals("hold"))
        {
            send200Response(response,
                    Xml.formatApixErrorResponse("Collection segment must be \"bib\", \"auth\" or \"bib\"",  ApixCatServlet.ERROR_BAD_COLLECTION));
            return false;
        }
        if ( pathSegments.length > 3 && !pathSegments[3].equals("newhold"))
        {
            send200Response(response,
                    Xml.formatApixErrorResponse("Unknown extra path segment: " + pathSegments[3],  ApixCatServlet.ERROR_EXTRA_PARAM));
            return false;
        }
        return true;
    }

    static void send200Response(HttpServletResponse response, String message) throws IOException
    {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/xml");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        out.print(message);
        out.close();
    }

    static void send201Response(HttpServletResponse response, String uri) throws IOException
    {
        response.setStatus(HttpServletResponse.SC_CREATED);
        response.setHeader("Location", uri);
        response.setCharacterEncoding("UTF-8");
    }

    static void send303Response(HttpServletResponse response, String uri) throws IOException
    {
        response.setStatus(HttpServletResponse.SC_SEE_OTHER);
        response.setHeader("Location", uri);
        response.setCharacterEncoding("UTF-8");
    }
}
