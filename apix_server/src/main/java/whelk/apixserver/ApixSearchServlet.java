package whelk.apixserver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import whelk.Document;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

public class ApixSearchServlet extends HttpServlet
{
    private static final Logger s_logger = LogManager.getLogger(ApixSearchServlet.class);

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        try { doGet2(request, response); } catch (Exception e)
        {
            s_logger.error("Failed to process GET request.", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    public void doGet2(HttpServletRequest request, HttpServletResponse response) throws Exception
    {
        Set<String> resultingIDs = search(request);

        boolean includeHold = request.getParameter("x-holdings") != null && request.getParameter("x-holdings").equalsIgnoreCase("true");

        Utils.send200Response(response, Xml.formatApixSearchResponse(new ArrayList<>(resultingIDs), includeHold, request.getParameterMap()));
    }

    private Set<String> search(HttpServletRequest request)
    {
        Set<String> results = new HashSet<>();

        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements())
        {
            String parameterName = parameterNames.nextElement();
            String parameterValue = request.getParameter(parameterName);

            if (parameterName.equalsIgnoreCase("isbn"))
            {
                String normalizedValue = parameterValue.replaceAll("-", "");
                results.addAll(Utils.s_whelk.getStorage().getSystemIDsByTypedID("ISBN", normalizedValue.toUpperCase(), 1));
                results.addAll(Utils.s_whelk.getStorage().getSystemIDsByTypedID("ISBN", normalizedValue.toLowerCase(), 1));
            } else if (parameterName.equalsIgnoreCase("issn"))
            {
                results.addAll(Utils.s_whelk.getStorage().getSystemIDsByTypedID("ISSN", parameterValue.toUpperCase(), 1));
                results.addAll(Utils.s_whelk.getStorage().getSystemIDsByTypedID("ISSN", parameterValue.toLowerCase(), 1));
            } else
            {
                results.addAll(Utils.s_whelk.getStorage().getSystemIDsByTypedID(null, parameterValue, 1));
                results.addAll(Utils.s_whelk.getStorage().getSystemIDsByTypedID(null, parameterValue, 0));
            }
        }

        Iterator<String> it = results.iterator();
        while (it.hasNext())
        {
            String systemID = it.next();
            if (!Utils.s_whelk.getStorage().getCollectionBySystemID(systemID).equals("bib"))
                it.remove();
        }

        return results;
    }
}
