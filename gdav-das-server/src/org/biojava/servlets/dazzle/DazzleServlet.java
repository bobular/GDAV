/*
 *                    BioJava development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the individual
 * authors.  These should be listed in @author doc comments.
 *
 * For more information on the BioJava project and its aims,
 * or to join the biojava-l mailing list, visit the home page
 * at:
 *
 *      http://www.biojava.org/
 *
 */

package org.biojava.servlets.dazzle;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;
import javax.servlet.*;
import javax.servlet.http.*;

import org.biojava.bio.*;
import org.biojava.bio.seq.*;
import org.biojava.bio.symbol.*;
import org.biojava.bio.program.xff.*;

import org.biojava.utils.xml.*;

import org.biojava.servlets.dazzle.datasource.*;

import java.util.regex.Pattern;

import java.lang.reflect.Method;

/**
 * A general purpose server for the Distributed Annotation System.  Should be a
 * fully compliant implementation of DAS/1.52
 *
 * <p>
 * When the servlet is initialized, it reads an XML configuration file (dazzlecfg.xml by
 * default), which should configure one or more plugins implementing the DazzleDataSource
 * interface.  These are then served as DAS DSNs.
 * </p>
 *
 * @author Thomas Down
 * @version 1.01
 */

public class DazzleServlet extends HttpServlet {
    //
    // General configuration
    //

    public static final String DAZZLE_VERSION = "DazzleServer/1.01 (20040513; BioJava 1.4)";
    public static final String DAS_PROTOCOL_VERSION = "1.5";
    public static final String DAZZLE_CAPABILITIES = "dsn/1.0; dna/1.0; types/1.0; stylesheet/1.0; features/1.0; encoding-dasgff/1.0; encoding-xff/1.0; entry_points/1.0; error-segment/1.0; unknown-segment/1.0; feature-by-id/1.0; group-by-id/1.0; feature-by-target/1.0; match-exact/1.0; match-partial/1.0; component/1.0; sequence/1.0"; 
    public static final String DASGFF_VERSION = "1.0";
    public static final String XML_CONTENT_TYPE = "text/xml";
    
    public static final String DEFAULT_STYLESHEET = "/stylesheet.xml";
    public static final String WELCOME_MESSAGE = "/welcome.html";

    public static final boolean REGEXP_SUPPORT = false;
    public static final boolean TOLERATE_MISSING_SEGMENTS = false;

    //
    // DAS status codes
    //

    public static final int STATUS_OKAY = 200;

    public static final int STATUS_BAD_COMMAND = 400;
    public static final int STATUS_BAD_DATASOURCE = 401;
    public static final int STATUS_BAD_COMMAND_ARGUMENTS = 402;
    public static final int STATUS_BAD_REFERENCE = 403;
    public static final int STATUS_BAD_STYLESHEET = 404;
    public static final int STATUS_BAD_COORDS = 405;

    public static final int STATUS_SERVER_ERROR = 500;
    public static final int STATUS_UNSUPPORTED_FEATURE = 501;

    private static final Map errorMessages;

    static {
        errorMessages = new HashMap();
        errorMessages.put(new Integer(STATUS_BAD_COMMAND), "Bad command");
        errorMessages.put(new Integer(STATUS_BAD_DATASOURCE), "Bad datasource");
        errorMessages.put(new Integer(STATUS_BAD_COMMAND_ARGUMENTS), "Bad command arguments");
        errorMessages.put(new Integer(STATUS_BAD_REFERENCE), "Bad reference");
        errorMessages.put(new Integer(STATUS_BAD_STYLESHEET), "Bad stylesheet");
        errorMessages.put(new Integer(STATUS_BAD_COORDS), "Bad coordinates");
        errorMessages.put(new Integer(STATUS_SERVER_ERROR), "Server error");
        errorMessages.put(new Integer(STATUS_UNSUPPORTED_FEATURE), "Unimplemented feature");
    }

    //
    // Data Sources
    // 

    DazzleInstallation installation;

    //
    // Local configuration flags
    //

    private boolean gzipEncoding = true;

    /**
     * Get info about this servlet.
     */

    public String getServletInfo() {
        return DAZZLE_VERSION;
    }

    /**
     * Initialize DAS services, and load the XML configurations.
     */

    public void init(ServletConfig config)
        throws ServletException
    {
        super.init(config);
        
        String installClassName = config.getInitParameter("dazzle.installation_type");
        if (installClassName == null) {
            installClassName = "org.biojava.servlets.dazzle.datasource.BasicDazzleInstallation";
        }
        
        try {
            Class installClass = getClass().getClassLoader().loadClass(installClassName);
            installation = (DazzleInstallation) installClass.newInstance();
            installation.init(config);
        } catch (DataSourceException ex) {
            log("Error initializing installation", ex);
            throw new ServletException("Error initializing installation");
        } catch (ClassCastException ex) {
            throw new ServletException("Not a Dazzle installation: " + installClassName);
        } catch (ClassNotFoundException ex) {
            throw new ServletException("Couldn't find class: " + installClassName);
        } catch (InstantiationException ex) {
            throw new ServletException("Couldn't instantiate " + installClassName);
        } catch (IllegalAccessException ex) {
            throw new ServletException("Couldn't instantiate " + installClassName);
        }
    }
    
    /**
     * Clean up datasources
     */
    
    public void destroy() {
        super.destroy();
        installation.destroy();
    }

    /**
     * Normal DAS command.  As well as handling GETs, this method
     * also gets form-encoded POSTs.
     */

    public void doGet(HttpServletRequest req,
		      HttpServletResponse resp)
	throws ServletException, IOException
    {
        // We `filter' the query string, to accept strings separated
        // using the ';' character
        req = new DASQueryStringTranslator(req);

        // determine content-encoding for reply
        // we only implement gzip so if that's not found, we
        // fallback to plaintext.
        String encodingStr = req.getHeader("Accept-Encoding");
        
        // if gzip encoding is required, replace the HttpServletResponse with a gzip version
        // As IO is now buffered DO REMEMBER TO FLUSH BUFFERS PROPERLY.
        if (encodingStr != null && gzipEncoding)  {
            if (encodingStr.indexOf("gzip") != -1) {
		// System.err.println("Using gzip!");
                // gzip compression requested
                resp = new HttpServletResponseWrapper(resp) {
                               GZIPOutputStream gzipOut = null;
                               public PrintWriter getWriter() throws IOException {
                                   gzipOut = new GZIPOutputStream(getResponse().getOutputStream());
                                   return new PrintWriter(gzipOut) {
                                       // we return a subclassed PrintWriter which finishes GZIPOutputStream when close is called
                                       public void close() {
                                           super.close();
                                           try {
                                           gzipOut.finish();
                                           }
                                           catch (IOException ie) {
                                              System.err.println("Unexpected IOException when closing GZIPOutputStream");
                                           }
                                       }
                                   };
                               }
                           };
                resp.setHeader("Content-Encoding", "gzip");
            }
        }

        // Let ourselves be known...
        
        resp.setHeader("X-DAS-Version", DAS_PROTOCOL_VERSION);
        resp.setHeader("X-DAS-Server", DAZZLE_VERSION);
        resp.setHeader("X-DAS-Capabilities", DAZZLE_CAPABILITIES);
        
        String cmdPath = req.getPathInfo();
        if (cmdPath == null) {
            cmdPath = "";
        }
        
        StringTokenizer toke = new StringTokenizer(cmdPath, "/");
        if (! toke.hasMoreTokens()) {
            welcomePage(req, resp);
            return;
        }
        
        List nameComponents = new ArrayList();
        String command = toke.nextToken();
        while (toke.hasMoreTokens()) {
            nameComponents.add(command);
            command = toke.nextToken();
        }
        
        if (command.equals("dsn")) {
            dsnPage(nameComponents, req, resp);
            return;
        }  else if (command.equals("capabilities")) {
            capabilitiesPage(req, resp);
            return;
        } else if (command.endsWith(".dtd")) {
            sendDTD(req, resp, command);
            return;
        } else {
            DazzleDataSource dds = null;
            try {
                dds = installation.getDataSource(nameComponents, req);
            } catch (DataSourceException ex) {
                sendError(req, resp, STATUS_BAD_DATASOURCE);
                return;
            }
            
            if ("dna".equals(command)) {
                dnaCommand(req, resp, dds);
            } else if ("sequence".equals(command)) {
                sequenceCommand(req, resp, dds);
            } else if ("features".equals(command)) {
                featuresCommand(req, resp, dds);
            } else if ("types".equals(command)) {
                typesCommand(req, resp, dds);
            } else if ("entry_points".equals(command)) {
                entryPointsCommand(req, resp, dds);
            } else if ("link".equals(command)) {
                linkCommand(req, resp, dds);
            } else if ("stylesheet".equals(command)) {
                stylesheetCommand(req, resp, dds);
            } else if ("resolve".equals(command)) {
                sendError(req, resp, STATUS_UNSUPPORTED_FEATURE, "The resolve command is obsolete"); 
            } else {
                sendError(req, resp, STATUS_BAD_COMMAND, "No such command: " + command);
            }
        }
    }

    /**
     * The only POSTs we support are form-encoded requests, which
     * get delegated to doGet.  Anything else is an error.
     */

    public void doPost(HttpServletRequest req,
		      HttpServletResponse resp)
	    throws ServletException, IOException
    {
        String contentType = req.getContentType();
        StringTokenizer ctToke = new StringTokenizer(contentType, "/ ");
        String ctmedia = null, ctsubtype = null;
        if (ctToke.hasMoreTokens()) {
            ctmedia = ctToke.nextToken().toLowerCase();
        }
        if (ctToke.hasMoreTokens()) {
            ctsubtype = ctToke.nextToken().toLowerCase();
        }
        
        if ("application".equals(ctmedia) && "x-www-form-urlencoded".equals(ctsubtype)) {
            doGet(req, resp);
            return;
        }
        
        resp.setHeader("X-DAS-Version", DAS_PROTOCOL_VERSION);
        resp.setHeader("X-DAS-Server", DAZZLE_VERSION);
        
        sendError(
            req, 
            resp,
            STATUS_BAD_COMMAND_ARGUMENTS,
            "Bad POSTed content type: " + contentType
        );
    }

    //
    // Useful error-handling code.
    //

    private void sendError(HttpServletRequest req,
			   HttpServletResponse resp,
			   int statusCode) 
	    throws ServletException, IOException
    {
        log("DAS Error: status=" + statusCode);
        
        resp.setIntHeader("X-DAS-Status", statusCode);
        resp.setContentType("text/plain");
        PrintWriter pw = resp.getWriter();
        String msg = (String) errorMessages.get(new Integer(statusCode));
        pw.println(msg + " (" + statusCode + ")");
        pw.println();
        pw.println("URL: " + fullURL(req));
    }

    private void sendError(HttpServletRequest req,
			   HttpServletResponse resp,
			   int statusCode,
			   String message)
	throws ServletException, IOException
    {
        log("DAS Error: " + message);
        
        resp.setIntHeader("X-DAS-Status", statusCode);
        resp.setContentType("text/plain");
        PrintWriter pw = resp.getWriter();
        String msg = (String) errorMessages.get(new Integer(statusCode));
        pw.println(msg + " (" + statusCode + ")");
        pw.println();
        pw.println("URL: " + fullURL(req));
        pw.println();
        pw.println(message);
    }

    private void sendError(HttpServletRequest req,
			   HttpServletResponse resp,
			   int statusCode,
			   Throwable exception)
	throws ServletException, IOException
    {
        log("DAS Error", exception);
        
        resp.setIntHeader("X-DAS-Status", statusCode);
        resp.setContentType("text/plain");
        PrintWriter pw = resp.getWriter();
        String msg = (String) errorMessages.get(new Integer(statusCode));
        pw.println(msg + " (" + statusCode + ")");
        pw.println();
        pw.println("URL: " + fullURL(req));
        pw.println();
        exception.printStackTrace(pw);
    }

    private String fullURL(HttpServletRequest req) {
        StringBuffer u = HttpUtils.getRequestURL(req);
        String q = req.getQueryString();
        if (q != null) {
            u.append('?');
            u.append(req.getQueryString());
        }
        return u.toString();
    }

    // 
    // Welcome page
    //

    private void welcomePage(HttpServletRequest req,
			     HttpServletResponse resp)
	throws ServletException, IOException
    {
        List dataSourceIDs = null;
        Map dataSources = new HashMap();
        try {
            dataSourceIDs = new ArrayList(installation.getDataSourceIDs(Collections.EMPTY_LIST, req));
            for (Iterator i = dataSourceIDs.iterator(); i.hasNext(); ) {
                String id = (String) i.next();
                try {
                    dataSources.put(id, installation.getDataSource(Collections.nCopies(1, id), req));
                } catch (DataSourceException ex) {
                    i.remove();
                    log("*** WARNING *** Lost a data source " + id);
                }
            }
        } catch (DataSourceException ex) {
            sendError(req, resp, STATUS_SERVER_ERROR, ex);
            return;
        }
        
        resp.setIntHeader("X-DAS-Status", STATUS_OKAY);
        resp.setContentType("text/html");
        PrintWriter pw = resp.getWriter();
        pw.println("<html>");
        pw.println("<head>");
        pw.println("<title>DAS Server information</title>");
        pw.println("</head>");
        pw.println("<body>");
        pw.println("<h1>" + DAZZLE_VERSION + "</h1>");
        
        // Information about this installation
        
        InputStream is = getServletContext().getResourceAsStream(WELCOME_MESSAGE);
        if (is != null) {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                pw.println(line);
            }
        }
        
        // DSN list
        
        pw.println("<h2>Available data sources</h2>");
        pw.println("<table border=\"1\">");
        pw.println("<tr><th>ID</th> <th>Description</th> <th>Version</th> <th>Reference?</th><th>Plugin</th></tr>");
        for (Iterator i = dataSourceIDs.iterator(); i.hasNext(); ) {
            String id = (String) i.next();
            DazzleDataSource dds = (DazzleDataSource) dataSources.get(id);
            
            pw.println("<tr>");
            pw.println("  <td>" + id + "</td>");
            pw.println("  <td>" + dds.getDescription() + "</td>");
            pw.println("  <td>" + dds.getVersion() + "</td>");
            pw.println("  <td>" + ((dds instanceof DazzleReferenceSource) ? "Yes" : "No") + "</td>");
            pw.println("  <td>" + dds.getDataSourceType() + "/" + dds.getDataSourceVersion() + "</td>");
            pw.println("</tr>");
        }
        pw.println("</table>");
        
        pw.println("</body>");
        pw.println("</html>");
        pw.close();
    }    

    //
    // DSN command
    //

    private void dsnPage(List nameComponents,
			 HttpServletRequest req,
			 HttpServletResponse resp)
	throws ServletException, IOException
    {
        List dataSourceIDs = null;
        Map dataSources = new HashMap();
        try {
            dataSourceIDs = new ArrayList(installation.getDataSourceIDs(nameComponents, req));
            for (Iterator i = dataSourceIDs.iterator(); i.hasNext(); ) {
                String id = (String) i.next();
                List nc = new ArrayList(nameComponents);
                nc.add(id);
                try {
                    dataSources.put(id, installation.getDataSource(nc, req));
                } catch (DataSourceException ex) {
                    i.remove();
                    log("*** WARNING *** Lost a data source " + id);
                }
            }
        } catch (DataSourceException ex) {
            sendError(req, resp, STATUS_SERVER_ERROR, ex);
            return;
        }
        
        resp.setIntHeader("X-DAS-Status", STATUS_OKAY);
        resp.setContentType(XML_CONTENT_TYPE);
        PrintWriter pw = resp.getWriter();
        XMLWriter xw = new PrettyXMLWriter(pw);
        
        xw.printRaw("<?xml version='1.0' standalone='no' ?>");
        xw.printRaw("<!DOCTYPE DASDSN SYSTEM 'dasdsn.dtd' >");
        
        xw.openTag("DASDSN");
        for (Iterator i = dataSourceIDs.iterator(); i.hasNext(); ) {
            String id = (String) i.next();
            DazzleDataSource dataSource = (DazzleDataSource) dataSources.get(id);
            
            xw.openTag("DSN");
            xw.openTag("SOURCE");
            xw.attribute("id", id);
            String version = dataSource.getVersion();
            if (version != null) {
                xw.attribute("version", version);
            }
            xw.print(dataSource.getName());
            xw.closeTag("SOURCE");
            xw.openTag("MAPMASTER");
            if (dataSource instanceof DazzleReferenceSource) {
                String ref = HttpUtils.getRequestURL(req).toString();
                ref = ref.substring(0, ref.length() - 3) + id + "/";
                xw.print(ref);
            } else {
                xw.print(dataSource.getMapMaster());
            }
            xw.closeTag("MAPMASTER");
            String description = dataSource.getDescription();
            if (description != null) {
                xw.openTag("DESCRIPTION");
                xw.print(description);
                xw.closeTag("DESCRIPTION");
            }
            xw.closeTag("DSN");
        }
        xw.closeTag("DASDSN");
        pw.close();
    }

    //
    // CAPABILITIES command
    //

    private void capabilitiesPage(HttpServletRequest req,
				  HttpServletResponse resp)
	throws ServletException, IOException
    {
        resp.setIntHeader("X-DAS-Status", STATUS_OKAY);
        resp.setContentType(XML_CONTENT_TYPE);
        PrintWriter pw = resp.getWriter();
        XMLWriter xw = new PrettyXMLWriter(pw);
        
        xw.printRaw("<?xml version='1.0' standalone='no' ?>");
        
        xw.openTag("capabilities");
        sendCapability(xw, "featureTable", "dasgff");
        sendCapability(xw, "featureTable", "xff");
        xw.closeTag("capabilities");
        pw.close();
    }

    private void sendCapability(XMLWriter xw,
				String type, 
				String value)
	throws IOException
    {
        xw.openTag("capability");
        xw.attribute("type", type);
        if (value != null) {
            xw.attribute("value", value);
        }
        xw.closeTag("capability");
    }

    /**
     * Generalized routine for extracting segments from a query string.
     *
     * @return A List<Segment> giving all the valid segments specified by
     *          the query (may be empty), or <code>null</code> is an error
     *          occurred during processing (in which case a DAS error document
     *          will already have been sent to the client).
     */

    private List getSegments(
        DazzleDataSource dds,
        HttpServletRequest req,
        HttpServletResponse resp
    )
	    throws IOException, ServletException
    {
        List segments = new ArrayList();
        
        try {
            DazzleDataSource.MatchType matchType = DazzleDataSource.MATCH_EXACT;
            {
                String matchString = req.getParameter("match");
                if (matchString != null) {
                    if (matchString.equalsIgnoreCase("exact")) {
                        matchType = DazzleDataSource.MATCH_EXACT;
                    } else if (matchString.equalsIgnoreCase("partial")) {
                        matchType = DazzleDataSource.MATCH_PARTIAL;
                    } else {
                        sendError(req, resp, STATUS_BAD_COMMAND_ARGUMENTS);
                        return null;
                    }
                }
            }
            
            //
            // Old style
            //
            
            String ref = req.getParameter("ref");
            if (ref != null) {
                String starts = req.getParameter("start");
                String stops = req.getParameter("stop");
                
                if (starts == null) {
                    segments.add(new Segment(ref));
                } else {
                    segments.add(new Segment(ref, Integer.parseInt(starts), Integer.parseInt(stops)));
                }
            }
            
            //
            // New style...
            //
            
            String[] newSegs = req.getParameterValues("segment");
            if (newSegs != null) {
                for (int i = 0; i < newSegs.length; ++i) {
                    String newSeg = newSegs[i];
                    StringTokenizer toke = new StringTokenizer(newSeg, ":,");
                    String newRef = toke.nextToken();
                    if (toke.hasMoreTokens()) {
                        String starts = toke.nextToken();
                        String stops = toke.nextToken();
                        segments.add(new Segment(newRef, Integer.parseInt(starts), Integer.parseInt(stops)));
                    } else {
                        segments.add(new Segment(newRef));
                    }
                }
            }
            
            String[] featureSegs = req.getParameterValues("feature_id");
            if (featureSegs != null) {
                for (int i = 0; i < featureSegs.length; ++i) {
                    String featureID = featureSegs[i];
                    FeatureHolder featureInstances = dds.getFeaturesByID(featureID, matchType);
                    for (Iterator fi = featureInstances.features(); fi.hasNext(); ) {
                        Feature f = (Feature) fi.next();
                        segments.add(new Segment(f.getSequence().getName(), f.getLocation().getMin(), f.getLocation().getMax()));
                    }
                }
            }
            
            String[] groupSegs = req.getParameterValues("group_id");
            if (groupSegs != null) {
                Map locsBySeqName = new HashMap();
                for (int i = 0; i < groupSegs.length; ++i) {
                    String groupID = groupSegs[i];
                    FeatureHolder groupInstances = dds.getFeaturesByGroup(groupID, matchType);
                    for (Iterator fi = groupInstances.features(); fi.hasNext(); ) {
                        Feature f = (Feature) fi.next();
                        String seqName = f.getSequence().getName();
                        List locs = (List) locsBySeqName.get(seqName);
                        if (locs == null) {
                            locs = new ArrayList();
                            locsBySeqName.put(seqName, locs);
                        }
                        locs.add(f.getLocation());
                    }
                }
                for (Iterator seqi = locsBySeqName.entrySet().iterator(); seqi.hasNext(); ) {
                    Map.Entry seqme = (Map.Entry) seqi.next();
                    String seqName = (String) seqme.getKey();
                    Location locs = LocationTools.union((List) seqme.getValue());
                    segments.add(new Segment(seqName, locs.getMin(), locs.getMax()));
                }
            }
        } catch (NumberFormatException ex) {
            sendError(req, resp, STATUS_BAD_COMMAND_ARGUMENTS, ex);
            return null;
        } catch (NoSuchElementException ex) {
            sendError(req, resp, STATUS_BAD_COMMAND_ARGUMENTS, ex);
            return null;
        } catch (DataSourceException ex) {
            sendError(req, resp, STATUS_SERVER_ERROR, ex);
            return null;
        }
        
        return segments;
    }

    //
    // DNA command
    //

    private void dnaCommand(HttpServletRequest req,
			    HttpServletResponse resp,
			    DazzleDataSource dds)
	throws IOException, ServletException
    {
        if (! (dds instanceof DazzleReferenceSource)) {
            sendError(req, resp, STATUS_UNSUPPORTED_FEATURE, "Not a reference source");
            return;
        }
        
        DazzleReferenceSource drs = (DazzleReferenceSource) dds;
        
        List segments = getSegments(dds, req, resp);
        if (segments == null) {
            return;
        }
        if (segments.size() == 0) {
            sendError(req, resp, STATUS_BAD_COMMAND_ARGUMENTS, "No segments specified for dna command");
            return;
        }
        
        // Fetch and validate the requests.
        
        Map segmentResults = new HashMap();
        for (Iterator i = segments.iterator(); i.hasNext(); ) {
            Segment seg = (Segment) i.next();
            
            try {
                Sequence seq = drs.getSequence(seg.getReference());
                if (seq.getAlphabet() != DNATools.getDNA()) {
                    sendError(req, resp, STATUS_SERVER_ERROR, "Sequence " + seg.toString() + " is not in the DNA alphabet");
                    return;
                }
                if (seg.isBounded()) {
                    if (seg.getMin() < 1 || seg.getMax() > seq.length()) {
                        sendError(req, resp, STATUS_BAD_COORDS, "Segment " + seg.toString() + " doesn't fit sequence of length " + seq.length());
                        return;
                    }
                }
                segmentResults.put(seg, seq);
            } catch (NoSuchElementException ex) {
                sendError(req, resp, STATUS_BAD_REFERENCE, ex);
                return;
                // segmentResults.put(seg, null);
            } catch (DataSourceException ex) {
                sendError(req, resp, STATUS_SERVER_ERROR, ex);
                return;
            }
        }
        
        //
        // Looks okay -- generate the response document
        //
        
        resp.setIntHeader("X-DAS-Status", STATUS_OKAY);
        resp.setContentType(XML_CONTENT_TYPE);
        PrintWriter pw = resp.getWriter();
        XMLWriter xw = new PrettyXMLWriter(pw);
        
        xw.printRaw("<?xml version='1.0' standalone='no' ?>");
        xw.printRaw("<!DOCTYPE DASDNA SYSTEM 'dasdna.dtd' >");
        
        try {
            xw.openTag("DASDNA");
            for (Iterator i = segmentResults.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry me = (Map.Entry) i.next();
                Segment seg = (Segment) me.getKey();
                Sequence seq = (Sequence) me.getValue();
                
                xw.openTag("SEQUENCE");
                xw.attribute("id", seg.getReference());
                xw.attribute("version", drs.getLandmarkVersion(seg.getReference()));
                if (seg.isBounded()) {
                    xw.attribute("start", "" + seg.getStart());
                    xw.attribute("stop", "" + seg.getStop());
                } else {
                    xw.attribute("start", "" + 1);
                    xw.attribute("stop", "" + seq.length());
                }
                
                SymbolList syms = seq;
                if (seg.isBounded()) {
                    syms = syms.subList(seg.getMin(), seg.getMax());
                }
                if (seg.isInverted()) {
                    syms = DNATools.reverseComplement(syms);
                }
                
                xw.openTag("DNA");
                xw.attribute("length", "" + syms.length());
                
                for (int pos = 1; pos <= syms.length(); pos += 60) {
                    int maxPos = Math.min(syms.length(), pos + 59);
                    xw.println(syms.subStr(pos, maxPos));
                }
                
                xw.closeTag("DNA");
                xw.closeTag("SEQUENCE");
            }
            xw.closeTag("DASDNA");
            pw.close();
        } catch (Exception ex) {
            log("Error writing DNA document", ex);
            throw new ServletException(ex);
        }
    }

    //
    // Sequence command
    //

    private void sequenceCommand(HttpServletRequest req,
			                     HttpServletResponse resp,
                                 DazzleDataSource dds)
	    throws IOException, ServletException
    {
        if (! (dds instanceof DazzleReferenceSource)) {
            sendError(req, resp, STATUS_UNSUPPORTED_FEATURE, "Not a reference source");
            return;
        }
        
        DazzleReferenceSource drs = (DazzleReferenceSource) dds;
        
        List segments = getSegments(dds, req, resp);
        if (segments == null) {
            return;
        }
        if (segments.size() == 0) {
            sendError(req, resp, STATUS_BAD_COMMAND_ARGUMENTS, "No segments specified for sequence command");
            return;
        }
        
        // Fetch and validate the requests.
        
        Map segmentResults = new HashMap();
        for (Iterator i = segments.iterator(); i.hasNext(); ) {
            Segment seg = (Segment) i.next();
            
            try {
                Sequence seq = drs.getSequence(seg.getReference());
                if (seg.isBounded()) {
                    if (seg.getMin() < 1 || seg.getMax() > seq.length()) {
                        sendError(req, resp, STATUS_BAD_COORDS, "Segment " + seg.toString() + " doesn't fit sequence of length " + seq.length());
                        return;
                    }
                }
                segmentResults.put(seg, seq);
            } catch (NoSuchElementException ex) {
                sendError(req, resp, STATUS_BAD_REFERENCE, ex);
                return;
                // segmentResults.put(seg, null);
            } catch (DataSourceException ex) {
                sendError(req, resp, STATUS_SERVER_ERROR, ex);
                return;
            }
        }
        
        //
        // Looks okay -- generate the response document
        //
        
        resp.setIntHeader("X-DAS-Status", STATUS_OKAY);
        resp.setContentType(XML_CONTENT_TYPE);
        PrintWriter pw = resp.getWriter();
        XMLWriter xw = new PrettyXMLWriter(pw);
        
        xw.printRaw("<?xml version='1.0' standalone='no' ?>");
        xw.printRaw("<!DOCTYPE DASSEQUENCE SYSTEM 'dassequence.dtd' >");
        
        try {
            xw.openTag("DASSEQUENCE");
            for (Iterator i = segmentResults.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry me = (Map.Entry) i.next();
                Segment seg = (Segment) me.getKey();
                Sequence seq = (Sequence) me.getValue();
                
                xw.openTag("SEQUENCE");
                xw.attribute("id", seg.getReference());
                xw.attribute("version", drs.getLandmarkVersion(seg.getReference()));
                if (seg.isBounded()) {
                    xw.attribute("start", "" + seg.getStart());
                    xw.attribute("stop", "" + seg.getStop());
                } else {
                    xw.attribute("start", "" + 1);
                    xw.attribute("stop", "" + seq.length());
                }
                String molType = seq.getAlphabet().getName();
                if (seq.getAlphabet() == DNATools.getDNA()) {
                    molType = "DNA";
                } else if (seq.getAlphabet() == RNATools.getRNA()) {
                    molType = "ssRNA";
                } else if (seq.getAlphabet() == ProteinTools.getAlphabet() || seq.getAlphabet() == ProteinTools.getTAlphabet()) {
                    molType = "Protein";
                }
                xw.attribute("moltype", molType);
                
                SymbolList syms = seq;
                if (seg.isBounded()) {
                    syms = syms.subList(seg.getMin(), seg.getMax());
                }
                if (seg.isInverted()) {
                    syms = DNATools.reverseComplement(syms);
                }
                
                for (int pos = 1; pos <= syms.length(); pos += 60) {
                    int maxPos = Math.min(syms.length(), pos + 59);
                    xw.println(syms.subStr(pos, maxPos));
                }
                xw.closeTag("SEQUENCE");
            }
            xw.closeTag("DASSEQUENCE");
            pw.close();
        } catch (Exception ex) {
            log("Error writing DNA document", ex);
            throw new ServletException(ex);
        }
    }
    
    //
    // FEATURES command
    //

    private void featuresCommand(HttpServletRequest req,
				 HttpServletResponse resp,
				 DazzleDataSource dds)
	throws IOException, ServletException
    {
        List segments = getSegments(dds, req, resp);
        if (segments == null) {
            return;
        }
        
        String[] type = req.getParameterValues("type");
        String[] category = req.getParameterValues("category");
        String encoding = req.getParameter("encoding");
        if (encoding == null) {
            encoding = "dasgff";
        }
        boolean categorize = ("yes".equals(req.getParameter("categorize"))); // WHY WHY WHY?
        FeatureFilter generalFilter = null;
        try {
            generalFilter = featuresOutput_buildGeneralFilter(dds, type, category);
        } catch (PatternSyntaxException ex) {
            sendError(req, resp, STATUS_BAD_COMMAND_ARGUMENTS, ex);
            return;
        }
        
        // Fetch and validate the requests.
        
        Map segmentResults = new HashMap();
        for (Iterator i = segments.iterator(); i.hasNext(); ) {
            Segment seg = (Segment) i.next();
            
            try {

							/* find out if we need to call the 3-arg getFeatures or not */
							/* see the class Segment defined in this file */

                FeatureHolder features = seg.getStart() != Integer.MIN_VALUE && seg.getStop() != Integer.MAX_VALUE ? dds.getFeatures(seg.getReference(), seg.getStart(), seg.getStop()) : dds.getFeatures(seg.getReference());

                int length = dds.getLandmarkLength(seg.getReference());
                if (seg.isBounded() && (seg.getMin() < 1 || seg.getMax() > length)) {
                    // sendError(req, resp, STATUS_BAD_COORDS, "Segment " + seg.toString() + " doesn't fit sequence of length " + length);
                    // return;
                    segmentResults.put(seg, "Segment " + seg.toString() + " doesn't fit sequence of length " + length);
                } else {
                    segmentResults.put(seg, features);
                }
            } catch (NoSuchElementException ex) {
                if (TOLERATE_MISSING_SEGMENTS) {
                    log("Ugh, requested segment " + seg.getReference() + " was missing, but we're just going to ignore it.  Heigh ho");
                } else {
                    segmentResults.put(seg, "Segment " + seg.getReference() + " was not found.");
                }
            } catch (DataSourceException ex) {
                sendError(req, resp, STATUS_SERVER_ERROR, ex);
                return;
            } catch (AbstractMethodError ex) {

						}

        }
        
        //
        // Looks okay -- generate the response document
        //
        
        if (encoding.equalsIgnoreCase("dasgff")) {
            featuresOutput_dasgff(req, resp, dds, segmentResults, generalFilter, categorize);
        } else if (encoding.equalsIgnoreCase("xff")) {
            featuresOutput_xff(req, resp, dds, segmentResults, generalFilter, categorize);
        } else {
            sendError(req, resp, STATUS_BAD_COMMAND_ARGUMENTS, "Bad features encoding: " + encoding);
            return;
        }
    }
    
    private FeatureFilter featuresOutput_buildSegmentFilter(FeatureFilter ff,
    Segment seg)
    {
        if (seg.isBounded()) {
            FeatureFilter newff = new FeatureFilter.OverlapsLocation(new RangeLocation(seg.getMin(),
            seg.getMax()));
            if (ff != FeatureFilter.all) {
                ff = new FeatureFilter.And(ff, newff);
            } else {
                ff = newff;
            }
        }
        
        return ff;
    }
    
    private FeatureFilter featuresOutput_buildGeneralFilter(DazzleDataSource dds,
    String[] type,
    String[] category) 
    throws PatternSyntaxException
    {
        FeatureFilter ff = FeatureFilter.all;
        Set allTypes = dds.getAllTypes();
        
        if (type != null) {
            Set types = new HashSet();
            for (int t = 0; t < type.length; ++t) {
                if (REGEXP_SUPPORT) {
                    boolean added = false;
                    Pattern typesRE = Pattern.compile(type[t]);
                    for (Iterator i = allTypes.iterator(); i.hasNext(); ) {
                        String aType = (String) i.next();
                        if (typesRE.matcher(aType).matches()) {
                            types.add(aType);
                            added = true;
                        }
                    }
                    
                    if (!added) {
                        types.add(type[t]);
                    }
                } else {
                    types.add(type[t]);
                }
            }
            
            
            for (Iterator i = types.iterator(); i.hasNext(); ) {
                FeatureFilter newff = new FeatureFilter.ByType((String) i.next());
                if (ff == FeatureFilter.all) {
                    ff = newff;
                } else {
                    ff = new FeatureFilter.Or(ff, newff);
                }
            }
        }
        if (category != null) {
            for (int t = 0; t < category.length; ++t) {
                if ("component".equals(category[t])) {
                    FeatureFilter newff = new FeatureFilter.ByClass(ComponentFeature.class);
                    if (ff != FeatureFilter.all) {
                        ff = new FeatureFilter.Or(ff, newff);
                    } else {
                        ff = newff;
                    }
                } else {
                    if (ff == FeatureFilter.all) {
                        ff = FeatureFilter.none;
                    }
                }
            }
        }
        
        // 
        // Ensure that we don't descend past components (this required quite recent BioJava)
        //
        
        boolean descendThroughComponents = false;
        
        if (!descendThroughComponents) {
            FeatureFilter notComponentDescendants = new FeatureFilter.Not(
            new FeatureFilter.ByAncestor(
            new FeatureFilter.ByClass(ComponentFeature.class)
            )
            );
            if (ff == FeatureFilter.all) {
                ff = notComponentDescendants;
            } else {
                ff = new FeatureFilter.And(ff, notComponentDescendants);
            }
        }
        
        return ff;
    }

    /**
     * Actual FEATURES document emitter for DASGFF.
     */

    private void writeDasgffFeature(DazzleDataSource dds, 
				    XMLWriter xw,
				    Feature feature,
				    List groups,
				    Location forcedLocation,
				    String forcedID,
				    boolean categorize)
	    throws IOException, ServletException
    {
        xw.openTag("FEATURE");
        if (forcedID == null) {
            xw.attribute("id", dds.getFeatureID(feature));
        } else {
            xw.attribute("id", forcedID);
        }
        String label = dds.getFeatureLabel(feature);
        if (label != null) {
            xw.attribute("label", label);
        }
        
        xw.openTag("TYPE");
        xw.attribute("id", feature.getType());
        if (feature instanceof ComponentFeature) {
            if (categorize) {
                xw.attribute("category", "component");
            }
            xw.attribute("reference", "yes");
            
            
            boolean subParts = (feature.filter(new FeatureFilter.ByClass(ComponentFeature.class), false).countFeatures() > 0);
            xw.attribute("subparts", subParts ? "yes" : "no");
        } else {
            if (categorize) {
                xw.attribute("category", "default");
            }
        }
        String description = dds.getTypeDescription(feature.getType());
        if (description == null) {
            description = feature.getType();
        }
        xw.print(description);  // todo: map this nicely
        xw.closeTag("TYPE");
        
        xw.openTag("METHOD");
        xw.attribute("id", feature.getSource());
        xw.print(feature.getSource());
        xw.closeTag("METHOD");
        
        {
            Location loc = forcedLocation;
            if (loc == null) {
                loc = feature.getLocation();
            }
            
            xw.openTag("START");
            xw.print("" + loc.getMin());
            xw.closeTag("START");
            
            xw.openTag("END");
            xw.print("" + loc.getMax());
            xw.closeTag("END");
        }
        
        xw.openTag("SCORE");
        String score = dds.getScore(feature);
        if (score != null) {
            xw.print(score);
        } else {
            xw.print("-");
        }
        xw.closeTag("SCORE");
        
        xw.openTag("ORIENTATION");
        // System.err.print("Doing feature of type " + feature.getClass().toString() + ": ");
        StrandedFeature.Strand strand = StrandedFeature.UNKNOWN;
        if (feature instanceof StrandedFeature) {
            strand = ((StrandedFeature) feature).getStrand();
        } else if (feature.getParent() instanceof StrandedFeature) {
            strand = ((StrandedFeature) feature.getParent()).getStrand();
        }
        // System.err.println(strand.toString());
        
        if (strand == StrandedFeature.POSITIVE) {
            xw.print("+");
        } else if (strand == StrandedFeature.NEGATIVE) {
            xw.print("-");
        } else {
            xw.print("0");
        }
        xw.closeTag("ORIENTATION");
        
        xw.openTag("PHASE");
        FramedFeature.ReadingFrame phase = dds.getPhase(feature);
        if (phase == null) {
            xw.print("-");
        } else {
            xw.print("" + phase.getFrame());
        }
        xw.closeTag("PHASE");
        
        List notes = dds.getFeatureNotes(feature);
        for (Iterator ni = notes.iterator(); ni.hasNext(); ) {
            String note = ni.next().toString();
            xw.openTag("NOTE");
            xw.print(note);
            xw.closeTag("NOTE");
        }
        
        if (feature instanceof ComponentFeature) {
            ComponentFeature cf = (ComponentFeature) feature;
            Location cfl = cf.getComponentLocation();
            
            xw.openTag("TARGET");
            xw.attribute("id", cf.getComponentSequence().getName());
            xw.attribute("start", "" + cfl.getMin());
            xw.attribute("stop", "" + cfl.getMax());
            xw.closeTag("TARGET");
        }
        
        // Now generate DAS-0.999 linkouts.
        
        if (forcedID == null) {
            Map linkouts = dds.getLinkouts(feature);
            if (linkouts != null && linkouts.size() > 0) {
                for (Iterator li = linkouts.entrySet().iterator(); li.hasNext(); ) {
                    Map.Entry link = (Map.Entry) li.next();
                    String linkRole = (String) link.getKey();
                    String linkURI = (String) link.getValue();
                    
                    xw.openTag("LINK");
                    xw.attribute("href", linkURI);
                    xw.print(linkRole);
                    xw.closeTag("LINK");
                } 
            }
        } else {
            // We don't really have an identity ourselves, so should just be linked via the group.
        }
        
        for (Iterator gi = groups.iterator(); gi.hasNext(); ) {
            Object groupingObject = gi.next();
            String gid;
            String type;
            String glabel;
            Map links;
            List groupNotes;
            if (groupingObject instanceof Feature) {
                Feature parentF = (Feature) groupingObject;
                gid = dds.getFeatureID(parentF);
                if (gid == null) {
                    gid = "" + parentF.hashCode();
                }
                type = parentF.getType();
                links = dds.getLinkouts(parentF);
                glabel = dds.getFeatureLabel(parentF);
                groupNotes = Collections.EMPTY_LIST;
            } else if (groupingObject instanceof DASGFFGroup) {
                DASGFFGroup group = (DASGFFGroup) groupingObject;
                gid = group.getGID();
                type = group.getType();
                links = group.getLinkMap();
                glabel = group.getLabel();
                groupNotes = group.getNotes();
            } else {
                throw new BioRuntimeException("Bad grouping object " + groupingObject.toString());
            }
            
            xw.openTag("GROUP");
            xw.attribute("id", /* feature.getType() + "-" + */ gid);
            xw.attribute("type", type);
            if (glabel != null) {
                xw.attribute("label", glabel);
            }
            if (links != null && links.size() > 0) {
                for (Iterator li = links.entrySet().iterator(); li.hasNext(); ) {
                    Map.Entry link = (Map.Entry) li.next();
                    String linkRole = (String) link.getKey();
                    String linkURI = (String) link.getValue();
                    
                    xw.openTag("LINK");
                    xw.attribute("href", linkURI);
                    xw.print(linkRole);
                    xw.closeTag("LINK");
                }
                for (Iterator ni = groupNotes.iterator(); ni.hasNext(); ) {
                    xw.openTag("NOTE");
                    xw.print((String) ni.next());
                    xw.closeTag("NOTE");
                }
            }
            xw.closeTag("GROUP");
        }
        
        xw.closeTag("FEATURE");	
    }
				    

    private void featuresOutput_dasgff(HttpServletRequest req,
				       HttpServletResponse resp,
				       DazzleDataSource dds,
				       Map segmentResults,
				       FeatureFilter generalFilter,
				       boolean categorize)
	throws IOException, ServletException
    {
        resp.setIntHeader("X-DAS-Status", STATUS_OKAY);
        resp.setContentType(XML_CONTENT_TYPE);
        PrintWriter pw = resp.getWriter();
        XMLWriter xw = new PrettyXMLWriter(pw);
        
        xw.printRaw("<?xml version='1.0' standalone='no' ?>");
        xw.printRaw("<!DOCTYPE DASGFF SYSTEM 'dasgff.dtd' >");
        
        try {
            xw.openTag("DASGFF");
            xw.openTag("GFF");
            xw.attribute("version", DASGFF_VERSION);
            xw.attribute("href", fullURL(req));
            
            for (Iterator i = segmentResults.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry me = (Map.Entry) i.next();
                Segment seg = (Segment) me.getKey();
                Object segv = me.getValue();
                if (segv instanceof FeatureHolder) {
                    FeatureHolder features = (FeatureHolder) segv;
                    
                    xw.openTag("SEGMENT");
                    xw.attribute("id", seg.getReference());
                    xw.attribute("version", dds.getLandmarkVersion(seg.getReference()));
                    if (seg.isBounded()) {
                        xw.attribute("start", "" + seg.getStart());
                        xw.attribute("stop", "" + seg.getStop());
                    } else {
                        xw.attribute("start", "1");
                        xw.attribute("stop", "" + dds.getLandmarkLength(seg.getReference()));
                    }
                    // TODO: Labels here?
                    
                    FeatureFilter ff = featuresOutput_buildSegmentFilter(generalFilter, seg);
                    features = features.filter(ff, true);
                    
                    List nullList = Collections.nCopies(1, null);
                    
                    for (Iterator fi = features.features(); fi.hasNext(); ) {
                        Feature feature = (Feature) fi.next();
                        
                        if (dds.getShatterFeature(feature)) {
                            int idSeed = 1;
                            String baseID = dds.getFeatureID(feature);
                            for (Iterator bi = feature.getLocation().blockIterator(); bi.hasNext(); ) {
                                Location shatterSpan = (Location) bi.next();
                                writeDasgffFeature(dds, xw, feature, Collections.singletonList(feature), shatterSpan, baseID + "-" + (idSeed++), categorize);
                            }
                        } else {
                            List groups = dds.getGroups(feature);
                            writeDasgffFeature(dds, xw, feature, groups, null, null, categorize);
                        }
                    }
                    
                    xw.closeTag("SEGMENT");
                } else if (segv instanceof String) {
                    xw.openTag("ERRORSEGMENT");
                    xw.attribute("id", seg.getReference());
                    if (seg.isBounded()) {
                        xw.attribute("start", "" + seg.getStart());
                        xw.attribute("stop", "" + seg.getStop());
                    } 
                    xw.closeTag("ERRORSEGMENT");
                } else if (segv == null) {
                    xw.openTag("UNKNOWNSEGMENT");
                    xw.attribute("id", seg.getReference());
                    xw.closeTag("UNKNOWNSEGMENT");
                }
            }
            
            xw.closeTag("GFF");
            xw.closeTag("DASGFF");
            pw.close();
        } catch (Exception ex) {
            log("Error writing DASGFF FEATURES document", ex);
            throw new ServletException(ex);
        }
    }

    /**
     * Wrapper for the standard XFF-writing code
     */

    private void featuresOutput_xff(HttpServletRequest req,
				    HttpServletResponse resp,
				    DazzleDataSource dds,
				    Map segmentResults,
				    FeatureFilter generalFilter,
				    boolean categorize)
	throws IOException, ServletException
    {
        resp.setIntHeader("X-DAS-Status", STATUS_OKAY);
        resp.setContentType(XML_CONTENT_TYPE);
        PrintWriter pw = resp.getWriter();
        XMLWriter xw = new PrettyXMLWriter(pw);
        
        xw.printRaw("<?xml version='1.0' standalone='no' ?>");
        /* xw.printRaw("<!DOCTYPE DASDSN SYSTEM 'dasdsn.dtd' >"); */
        
        XFFWriter xffw = new XFFWriter(new DataSourceXFFHelper(dds));
        try {
            xw.openTag("DASFEATURES");
            
            for (Iterator i = segmentResults.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry me = (Map.Entry) i.next();
                Segment seg = (Segment) me.getKey();
                Object segv = me.getValue();
                if (segv instanceof FeatureHolder) {
                    FeatureHolder features = (FeatureHolder) segv;
                    
                    xw.openTag("SEGMENT");
                    xw.attribute("id", seg.getReference());
                    xw.attribute("version", dds.getLandmarkVersion(seg.getReference()));
                    if (seg.isBounded()) {
                        xw.attribute("start", "" + seg.getStart());
                        xw.attribute("stop", "" + seg.getStop());
                    } else {
                        xw.attribute("start", "1");
                        xw.attribute("stop", "" + dds.getLandmarkLength(seg.getReference()));
                    }
                    // TODO: Labels here?
                    
                    FeatureFilter ff = featuresOutput_buildSegmentFilter(generalFilter, seg);
                    features = features.filter(ff, false); // FIXME!
                    
                    xffw.writeFeatureSet(features, xw);
                    
                    xw.closeTag("SEGMENT");
                } else if (segv instanceof String) {
                    xw.openTag("ERRORSEGMENT");
                    xw.attribute("id", seg.getReference());
                    if (seg.isBounded()) {
                        xw.attribute("start", "" + seg.getStart());
                        xw.attribute("stop", "" + seg.getStop());
                    } 
                    xw.closeTag("ERRORSEGMENT");
                } else if (segv == null) {
                    xw.openTag("UNKNOWNSEGMENT");
                    xw.attribute("id", seg.getReference());
                    xw.closeTag("UNKNOWNSEGMENT");
                }
            }
            
            xw.closeTag("DASFEATURES");
            pw.close();
        } catch (Exception ex) {
            log("Error writing XFF FEATURES document", ex);
            throw new ServletException(ex);
        }
    }

    private class DataSourceXFFHelper implements XFFHelper {
        private DazzleDataSource dds;
        
        DataSourceXFFHelper(DazzleDataSource dds) {
            this.dds = dds;
        }
        
        public String getFeatureID(Feature f) {
            return dds.getFeatureID(f);
        }
        
        public void writeDetails(XMLWriter xw, Feature f)
            throws IOException
        {
            // General annotation-writing from Dazzle 0.08.  Is this a good idea?
            
            Annotation a = f.getAnnotation();
            for (Iterator ai = a.keys().iterator(); ai.hasNext(); ) {
                Object key =  ai.next();
                if (! (key instanceof String))
                    continue;
                    Object value = a.getProperty(key);
                    if (! (value instanceof String))
                        continue;
                        
                        
                        xw.openTag("biojava:prop");
                        xw.attribute("key", (String) key);
                        xw.print((String) value);
                        xw.closeTag("biojava:prop");
            }
            
            // Link-writing.  Since 0.93, this is no longer the datasource's responsibility
            
            Map linkouts = dds.getLinkouts(f);
            if (linkouts != null && linkouts.size() > 0) {
                xw.openTag("das:links");
                xw.attribute("xmlns:das", "http://www.biojava.org/dazzle");
                xw.attribute("xmlns:xlink", "http://www.w3.org/1999/xlink");
                for (Iterator li = linkouts.entrySet().iterator(); li.hasNext(); ) {
                    Map.Entry link = (Map.Entry) li.next();
                    String linkRole = (String) link.getKey();
                    String linkURI = (String) link.getValue();
                    
                    xw.openTag("das:link");
                    xw.attribute("xlink:role", linkRole);
                    xw.attribute("xlink:href", linkURI);
                    xw.closeTag("das:link");
                } 
                xw.closeTag("das:links");
            }
            
            // Any other stuff the datasource wants
            
            dds.writeXFFDetails(xw, f);
        }
    }

    //
    // TYPES command
    //

    private void typesCommand(HttpServletRequest req,
			      HttpServletResponse resp,
			      DazzleDataSource dds)
	throws IOException, ServletException
    {
        List segments = getSegments(dds, req, resp);
        if (segments == null) {
            return;
        }
        
        String[] type = req.getParameterValues("type");
        String[] category = req.getParameterValues("category");
        
        // Fetch and validate the requests.
        
        Map segmentResults = new HashMap();
        for (Iterator i = segments.iterator(); i.hasNext(); ) {
            Segment seg = (Segment) i.next();
            
            try {
                int length = dds.getLandmarkLength(seg.getReference());
                if (seg.isBounded() && (seg.getMin() < 1 || seg.getMax() > length)) {
                    // sendError(req, resp, STATUS_BAD_COORDS, "Segment " + seg.toString() + " doesn't fit sequence of length " + length);
                    // return;
                    segmentResults.put(seg, "Segment " + seg.toString() + " doesn't fit sequence of length " + length);
                } else {
                    Map typeCounts = new HashMap();
                    String[] typesToCalculate;
                    
                    if (category == null) {
                        String[] _types = type;
                        List unhandledTypes = new ArrayList();
                        if (_types == null) {
                            _types = (String[]) dds.getAllTypes().toArray(new String[0]);
                        }
                        for (int t = 0; t < _types.length; ++t) {
                            String _type = _types[t];
                            int cntValue;
                            if (seg.isBounded()) {
                                cntValue = dds.countFeatures(seg.getReference(),
                                seg.getMin(),
                                seg.getMax(),
                                _type);
                            } else {
                                cntValue = dds.countFeatures(seg.getReference(), _type);
                            }
                            
                            if (cntValue != DazzleDataSource.COUNT_CALCULATE) {
                                if (cntValue >= 0) {
                                    typeCounts.put(_type, new Integer(cntValue));
                                } else {
                                    typeCounts.put(_type, null);
                                }
                            } else {
                                unhandledTypes.add(_type);
                            }
                        }
                        
                        if (typeCounts.size() == 0) {
                            typesToCalculate = type;
                        } else {
                            typesToCalculate = (String[]) unhandledTypes.toArray(new String[0]);
                        }
                    } else {
                        typesToCalculate = type;
                    }
                    
                    if (typesToCalculate == null || typesToCalculate.length > 0) {
                        FeatureFilter generalFilter = null;
                        try {
                            generalFilter = featuresOutput_buildGeneralFilter(dds, typesToCalculate, category);
                        } catch (PatternSyntaxException ex) {
                            sendError(req, resp, STATUS_BAD_COMMAND_ARGUMENTS, ex);
                            return;
                        }
                        
												FeatureHolder features = seg.getStart() != Integer.MIN_VALUE && seg.getStop() != Integer.MAX_VALUE ? dds.getFeatures(seg.getReference(), seg.getStart(), seg.getStop()) : dds.getFeatures(seg.getReference());

                        FeatureFilter ff = featuresOutput_buildSegmentFilter(generalFilter, seg);
                        for (Iterator fi = features.filter(ff, true).features(); fi.hasNext(); ) {
                            Feature feature = (Feature) fi.next();
                            String t = feature.getType();
                            Integer cnt = (Integer) typeCounts.get(t);
                            if (cnt != null) {
                                typeCounts.put(t, new Integer(cnt.intValue() + 1));
                            } else {
                                typeCounts.put(t, new Integer(1));
                            }
                        }
                    }
                    
                    segmentResults.put(seg, typeCounts);
                }
            } catch (NoSuchElementException ex) {
                if (TOLERATE_MISSING_SEGMENTS) {
                    log("Ugh, requested segment " + seg.getReference() + " was missing, but we're just going to ignore it.  Heigh ho");
                } else {
                    // sendError(req, resp, STATUS_BAD_REFERENCE, ex);
                    // return;
                    segmentResults.put(seg, "Couldn't find segment " + seg.getReference());
                }
            } catch (DataSourceException ex) {
                sendError(req, resp, STATUS_SERVER_ERROR, ex);
                return;
            }
        }
        
        //
        // Looks okay -- generate the response document
        //
        
        resp.setIntHeader("X-DAS-Status", STATUS_OKAY);
        resp.setContentType(XML_CONTENT_TYPE);
        PrintWriter pw = resp.getWriter();
        XMLWriter xw = new PrettyXMLWriter(pw);
        
        
        xw.printRaw("<?xml version='1.0' standalone='no' ?>");
        xw.printRaw("<!DOCTYPE DASTYPES SYSTEM 'dastypes.dtd' >");
        
        try {
            xw.openTag("DASTYPES");
            xw.openTag("GFF");
            xw.attribute("version", DASGFF_VERSION);
            xw.attribute("href", fullURL(req));
            
            if (segmentResults.size() > 0) {
                for (Iterator si = segmentResults.entrySet().iterator(); si.hasNext(); ) {
                    Map.Entry me = (Map.Entry) si.next();
                    Segment seg = (Segment) me.getKey();
                    Object segv = me.getValue();
                    if (segv instanceof Map) {
                        Map types = (Map) segv;
                        typesCommand_writeSegment(xw, dds, seg, types);
                    } else if (segv instanceof String) {
                        xw.openTag("ERRORSEGMENT");
                        xw.attribute("id", seg.getReference());
                        if (seg.isBounded()) {
                            xw.attribute("start", "" + seg.getStart());
                            xw.attribute("stop", "" + seg.getStop());
                        } 
                        xw.closeTag("ERRORSEGMENT");
                    } else if (segv == null) {
                        xw.openTag("UNKNOWNSEGMENT");
                        xw.attribute("id", seg.getReference());
                        xw.closeTag("UNKNOWNSEGMENT");
                    }	
                }
            } else {
                Map types = new HashMap();
                for (Iterator i = dds.getAllTypes().iterator(); i.hasNext(); ) {
                    String t = (String) i.next();
                    types.put(t, null);
                }
                typesCommand_writeSegment(xw, dds, null, types);
            }
            
            xw.closeTag("GFF");
            xw.closeTag("DASTYPES");
            pw.close();
        } catch (Exception ex) {
            log("Error writing DASGFF TYPES document", ex);
            throw new ServletException(ex);
        }
    }

    private void typesCommand_writeSegment(XMLWriter xw,
					   DazzleDataSource dds,
					   Segment seg,
					   Map types)
	throws IOException, DataSourceException
    {
        xw.openTag("SEGMENT");
        if (seg != null) {
            xw.attribute("id", seg.getReference());
            xw.attribute("version", dds.getLandmarkVersion(seg.getReference()));
            if (seg.isBounded()) {
                xw.attribute("start", "" + seg.getStart());
                xw.attribute("stop", "" + seg.getStop());
            } else {
                xw.attribute("start", "1");
                xw.attribute("stop", "" + dds.getLandmarkLength(seg.getReference()));
            }
        } else {
            xw.attribute("version", dds.getVersion());
        }
        
        for (Iterator i = types.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry me = (Map.Entry) i.next();
            String type = (String) me.getKey();
            Integer count = (Integer) me.getValue();
            xw.openTag("TYPE");
            xw.attribute("id", type);
            if (count != null) {
                xw.print(count.toString());
            }
            xw.closeTag("TYPE");
        }
        xw.closeTag("SEGMENT");
    }

    //
    // ENTRY_POINTS command
    //

    private void entryPointsCommand(HttpServletRequest req,
				    HttpServletResponse resp,
				    DazzleDataSource dds)
	throws IOException, ServletException
    {
        if (! (dds instanceof DazzleReferenceSource)) {
            sendError(req, resp, STATUS_UNSUPPORTED_FEATURE, "Not a reference source");
            return;
        }
        DazzleReferenceSource drs = (DazzleReferenceSource) dds;
        
        resp.setIntHeader("X-DAS-Status", STATUS_OKAY);
        resp.setContentType(XML_CONTENT_TYPE);
        PrintWriter pw = resp.getWriter();
        XMLWriter xw = new PrettyXMLWriter(pw);
        
        xw.printRaw("<?xml version='1.0' standalone='no' ?>");
        xw.printRaw("<!DOCTYPE DASEP SYSTEM 'dasep.dtd' >");
        
        try {
            xw.openTag("DASEP");
            xw.openTag("ENTRY_POINTS");
            xw.attribute("href", fullURL(req));
            xw.attribute("version", drs.getVersion());
            
            Set entryPoints = drs.getEntryPoints();
            for (Iterator i = entryPoints.iterator(); i.hasNext(); ) {
                String ep = (String) i.next();
                xw.openTag("SEGMENT");
                xw.attribute("id", ep);
                xw.attribute("size", "" + drs.getLandmarkLength(ep));
                xw.attribute("subparts", hasSubparts(ep, drs) ? "yes" : "no");
                xw.closeTag("SEGMENT");
            }
            
            xw.closeTag("ENTRY_POINTS");
            xw.closeTag("DASEP");
            pw.close();
        } catch (Exception ex) {
            log("Error writing ENTRY_POINTS document", ex);
            throw new ServletException(ex);
        }
    }

    private boolean hasSubparts(String ref, DazzleReferenceSource drs) 
        throws DataSourceException, NoSuchElementException
    {
        return (drs.getSequence(ref).filter(new FeatureFilter.ByClass(ComponentFeature.class), false).countFeatures() > 0);
    }

    //
    // STYLESHEET command
    //
    
    private void stylesheetCommand(HttpServletRequest req,
				   HttpServletResponse resp,
				   DazzleDataSource dds)
	throws ServletException, IOException
    {
        String stylesheetPath = dds.getStylesheet();
        InputStream styleSheet = null;
        
        if (stylesheetPath == null) {
            stylesheetPath = DEFAULT_STYLESHEET;
        }
        styleSheet = getServletContext().getResourceAsStream(stylesheetPath);
        if (styleSheet == null) {
            sendError(req, resp, STATUS_BAD_STYLESHEET);
            return;
        }   
        
        resp.setHeader("X-DAS-Status", "200");
        resp.setContentType(XML_CONTENT_TYPE);
        resp.setHeader("Content-Encoding", "plain");
        OutputStream os = resp.getOutputStream();
        byte[] buffer = new byte[256];
        int bufMax = 0;
        while (bufMax >= 0) {
            bufMax = styleSheet.read(buffer);
            if (bufMax > 0)
                os.write(buffer, 0, bufMax);
        }
        os.flush();
    }

    //
    // DTD pseudocommand
    //

    private void sendDTD(HttpServletRequest req,
			 HttpServletResponse resp,
			 String dtdName)
	throws ServletException, IOException
    {   
        String dtdPath = "org/biodas/das1/" + dtdName;
        InputStream dtd = getClass().getClassLoader().getResourceAsStream(dtdPath);
        
        if (dtd == null) {
            throw new ServletException("No such DTD: " + dtdName);
        }
        
        resp.setContentType(XML_CONTENT_TYPE);
        resp.setHeader("Content-Encoding", "plain");
        OutputStream os = resp.getOutputStream();
        byte[] buffer = new byte[256];
        int bufMax = 0;
        while (bufMax >= 0) {
            bufMax = dtd.read(buffer);
            if (bufMax > 0)
                os.write(buffer, 0, bufMax);
        }
        os.flush();	
    }

    //
    // LINK command
    //

    private void linkCommand(HttpServletRequest req,
			     HttpServletResponse resp,
			     DazzleDataSource dds)
	throws ServletException, IOException
    {
        String field = req.getParameter("field");
        String id = req.getParameter("id");
        if (field == null || id == null) {
            sendError(req, resp, STATUS_BAD_COMMAND_ARGUMENTS, "Missing parameter");
            return;
        }
        
        try {
            boolean response = dds.doLink(req, resp, field, id);
            if (! response) {
                resp.setHeader("X-DAS-Status", "200");
                resp.setContentType("text/html");
                PrintWriter pw = resp.getWriter();
                pw.println("<h1>No extra information about this object</h1><p>field: " + field + "<br />id: " + id + "</p>");
                pw.close();
            }
        } catch (NoSuchElementException ex) {
            sendError(req, resp, STATUS_BAD_COMMAND_ARGUMENTS, "Link failed");
            return;
        } catch (DataSourceException ex) {
            sendError(req, resp, STATUS_SERVER_ERROR, ex);
            return;
        }
    }

    /**
     * Memento for representing a segment in a DAS request.
     */

    private static class Segment {
        private String ref;
        private int start;
        private int stop;
        
        public Segment(String ref) {
            this.ref = ref;
            this.start = Integer.MIN_VALUE;
            this.stop = Integer.MAX_VALUE;
        }
        
        public Segment(String ref, int start, int stop) {
            this.ref = ref;
            this.start = start;
            this.stop = stop;
        }
        
        public boolean isBounded() {
            return (start != Integer.MIN_VALUE);
        }
        
        public boolean isInverted() {
            return (start > stop);
        }
        
        public String getReference() {
            return ref;
        }
        
        public int getStart() {
            return start;
        }
        
        public int getStop() {
            return stop;
        }
        
        public int getMin() {
            return Math.min(start, stop);
        }
        
        public int getMax() {
            return Math.max(start, stop);
        }
        
        public String toString() {
            if (isBounded()) {
                return ref + ':' + start + ',' + stop;
            } else {
                return ref;
            }
        }
    }
}
