package org.vectorbase.gdav;

import java.io.*;
import java.text.*;
import java.lang.*;
import java.lang.reflect.*;
import java.util.*;
import java.net.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.Rectangle;

import javax.servlet.*;
import javax.servlet.http.*;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.io.IOException;
import org.xml.sax.SAXException;
import java.sql.Connection; 
import java.sql.DriverManager; 
import java.sql.SQLException; 
import java.sql.ResultSet;

import org.vectorbase.gdav.DBSQL.*;

// import javax.sql.rowset.*;
//import java.sql.*;

import org.biojava.bio.symbol.*;
import org.biojava.bio.seq.*;
import org.biojava.bio.BioException;



/**
 * the main servlet, receives initial request from user, sets up
 * connections to the DB and provides a simple search function
 * @author seth.redmond@imperial.ac.uk Seth Redmond
 * @see org.vectorbase.gdav.ServletInfo
 * @deprecated  As of release 1.0, replaced by {@link org.vectorbase.gdav.GdavServlet}
 */

public class DisplayServlet extends HttpServlet {
	DBConnection dbCon;
	HttpSession session;
	ServletInfo servInfo;
	ConfigInfo confInfo;
	ServletContext ctx;
	PrintWriter out;
	/**
	 * reads in new servletInfo, creates DBconnection
	 * @see org.vectorbase.gdav.DBSQL.DBConnection 
	 */
	public void init() {
		try {
			servInfo = new ServletInfo(this.getServletContext());
			// confInfo = new ConfigInfo(this.getServletContext());
			// makeConnections();
			}
		catch (Exception ex) {
			log("Could not read server config file\n" + ex);
			ex.printStackTrace();
			}
		try {
			makeConnections();
			}
		catch (Exception ex) {
			log("Could not read make DB connections\n" + 
				servInfo.getDbString() + "\n" + 
				servInfo.getHost() + "\n" + 
				ex);
			ex.printStackTrace();
			}
		
		try {
//			servInfo = new ServletInfo(this.getServletContext());
			confInfo = new ConfigInfo(this.getServletContext());
			}
		catch (SAXException ex) {
			log("Could not parse cols config file\n" + ex.getMessage());
			ex.printStackTrace();
			}
		catch (ParserConfigurationException ex) {
			log("parser not configured correctly\n" + ex.getMessage());
			ex.printStackTrace();
			}
		catch (IOException ex) {
			log("parser not configured correctly\n" + ex.getMessage());
			ex.printStackTrace();
			}
		
//		seqUtil = new SequenceUtil();
		}
	
	public void destroy() {
	
		try {
			dbCon.severConnection();
			}
		catch (SQLException sqlEx) {
			log("could not sever DB connection\n"+sqlEx);
			}
		super.destroy();
		}
	
	
	private void makeConnections() {
		try {
			log("URL = "+this.getServletContext().getResource("/").toString());
			dbCon = new DBConnection(servInfo.getDbString(), servInfo.getDbUser(), servInfo.getDbPass());
			log("DBconnection successfully created "+dbCon.toString()+"\n\t"+servInfo.getDbName());
			}
		catch(Exception ex) {
			log("DBconnection not created"+servInfo.getDbString() + "\n" +
			servInfo.getHost(), ex);			
			}
		
/*		try {	// create default dbconnection
			dbHash.put(servInfo.getEnsDbVersion(0), new EnsDBConnection(servInfo.getEnsDbHost(0), servInfo.getEnsDbName(0), servInfo.getEnsDbPort(0), servInfo.getEnsDbUser(0), servInfo.getEnsDbPass(0), servInfo.getEnsDbVersion(0)));
			dbHash.put("default", dbHash.get(servInfo.getEnsDbVersion(0)));
			log("EnsEMBL connection successfully created \n\t"+servInfo.getEnsDbVersion(0)+"\n\t"+servInfo.getEnsDbName(0));
			}
		catch (AdaptorException ex){
			log(("EnsEMBL connection not created "+servInfo.getEnsDbString(0)), ex);			
			}
			
		// cycle through versions, create new DB for each, store in array.
		for (int i=1; i < Array.getLength(servInfo.ensDbVersion); i++) {
			try {
				dbHash.put(servInfo.getEnsDbVersion(i), new EnsDBConnection(servInfo.getEnsDbHost(i), servInfo.getEnsDbName(i), servInfo.getEnsDbPort(i), servInfo.getEnsDbUser(i), servInfo.getEnsDbPass(i), servInfo.getEnsDbVersion(i)));
				log("EnsEMBL connection successfully created \n\t"+servInfo.getEnsDbVersion(i)+"\n\t"+servInfo.getEnsDbName(i));
				}
			catch (AdaptorException ex){
				log(("EnsEMBL connection not created "+servInfo.getEnsDbString(i)), ex);			
				}
			}
*/
		}


	/**
	 * Reads Get string from URL. 
	 * If model_id is given returns model, 
	 * if search string is given performs search 
	 * if submission_id is given returns submission details
	 * if none are given returns all submission details? 
	 * all searches are performed via {@link org.vectorbase.gdav.DBSQL.DBConnection} 
	 */
	public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws IOException, ServletException {

		session = request.getSession(true);

		String[] path = request.getServletPath().split("/");		
		if(Array.getLength(path) > 2) {
			response.sendRedirect(servInfo.getHost()+"/"+path[1]+"/");
			}

		checkHealth();

		response.setContentType("text/html");
		out = response.getWriter();

		out.println("<html>");
		out.println("<head>");
		out.println("<link rel=stylesheet type=text/css href="+servInfo.getHost()+"/stylesheets/stylesheet.css>");
		out.println("<title>"+confInfo.getTitle()+"</title>");

		String query = request.getParameter("q");
//		String submissionID = request.getParameter("s");
		int submissionID = -1;
		if(request.getParameter("s") != null) {submissionID = Integer.parseInt(request.getParameter("s"));}
//		String modelID = request.getParameter("m");
		int modelID = -1;
		if(request.getParameter("m") != null) {modelID = Integer.parseInt(request.getParameter("m"));}

		out.print("<div class=leftpanel>");
		printLogo();
		out.print("</div>");
		printMenu();
		printSearchForm();

		out.print("<div class=mainpanel>");
		try {
			if (modelID != -1) {
				if(modelID==0) {
					printAllModels();}
				else {
					printModel(modelID);}
				}
			else if (submissionID != -1) { 	
				if(submissionID ==0) {
					printAllSubmissions();
					}
				else {
					printSubmission(submissionID);
					}
				}
			else if (query != null) 	{ 	String indexString = request.getParameter("i");
								int index=0;
								if (indexString != null) {
									index = Integer.parseInt(indexString);}
								doSearch(query);
					}
			else {					printAllSubmissions();}

			}
		catch (SQLException ex) {
			// handle any errors
			out.println("<title>gdav: error</title>");
			out.println("</head><body>");
			out.println("SQLException: <pre>" + ex.getMessage());
			out.println("SQLState: " + ex.getSQLState());
			out.println("VendorError: " + ex.getErrorCode()+"</pre>");
			}			
		catch (Exception ex) {
			ex.printStackTrace(out);
			}		
		out.print("</div>"); //class=mainpanel
		
		out.println("</body>");
		out.println("</html>");	

		}
		
	/** handle post calls (returns default response) */ 	
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException  {
		session = request.getSession(true);

		String[] path = request.getServletPath().split("/");		
		if(Array.getLength(path) > 2) {
			response.sendRedirect(servInfo.getHost()+"/"+path[1]+"/");
			}

		checkHealth();

		response.setContentType("text/html");
		out = response.getWriter();

		out.println("<html>");
		out.println("<head>");
		out.println("<link rel=stylesheet type=text/css href="+servInfo.getHost()+"/stylesheets/stylesheet.css>");
		out.println("<title>Genome Delinked Annotation Viewer</title>");

		String query = request.getParameter("q");
		String submission_id = request.getParameter("s");
		String model_id = request.getParameter("m");

		out.print("<div class=leftpanel>");
		printLogo();
		out.print("</div>");
		printMenu();
		printSearchForm();

		out.print("<div class=mainpanel>");

		try {
			printAllSubmissions();
			}
		catch (SQLException ex) {
			// handle any errors
			out.println("<title>gdav: error</title>");
			out.println("</head><body>");
			out.println("SQLException: <pre>" + ex.getMessage());
			out.println("SQLState: " + ex.getSQLState());
			out.println("VendorError: " + ex.getErrorCode()+"</pre>");
			}			
		catch (Exception ex) {
			ex.printStackTrace(out);
			}		
		out.print("&nbsp;</div>"); //class=mainpanel
		out.println("</body>");
		out.println("</html>");	
		}
	
	private void checkHealth() {	
		try {dbCon.ping();}	// CHECK SERVER IS STILL ALIVE
		catch (SQLException ex) {
			makeConnections();
			log("connection died, reconnecting to all...");
			}
		session.setAttribute("DBconnection",dbCon);
		// ResourceBundle rb = ResourceBundle.getBundle("LocalStrings",request.getLocale());
		}
	
	private void printLogo() {
		out.print("<div class=logopanel><img src=\""+servInfo.getHost()+"/images/logo.jpg\" class=logo></div>");
		}	
	
	private void printMenu() {
		out.print("<div class=menupanel>");
		out.print("<table class=menu>");
		out.print("<tr><td class=menu><a href="+servInfo.getHost()+"/?m=0>Browse All Models</a></td></tr>");
		out.print("<tr><td class=menu><a href="+servInfo.getHost()+"/?s=0>Browse All Submissions</a></td></tr>");
		out.print("</table>&nbsp;</div>");
		}
		
	private void printSearchForm() {
		out.print("<div class=formpanel> ");
		out.print("<form name=searchAll action="+servInfo.getHost()+" method=get>");
		out.print("<input type=text size=40 name=q class=searchBox > ");
		out.print("<input type=submit value=\"search all models\" > ");
		out.print("</form>");
		out.print("&nbsp;</div>");
		}



	private void printModel(int modelID)  throws SQLException {
		printModelDetails(dbCon.getModelByID(modelID));
		}
		
	private void printAllModels()  throws SQLException {
		printModelSummary(dbCon.getAllModels());
		}
		
	private void printModelSummary(ResultSet rs)  throws SQLException {
		out.println("<table class=modelSummary>");
//		int index = 0;
		while (rs.next()) {
//			index++;
			/* model_id, model_name, spp, description */
			int modelID = rs.getInt("model_id");
			String modelName = rs.getString("model_name");
			String modelSpp = rs.getString("spp");
			String modelDesc = rs.getString("description");
			out.println("<tr>");
			out.println(	"<td class=modelSummary valign=top>" +
					"<a href="+servInfo.getHost()+"/?m="+modelID+">"+modelName+"</a>" +
					"</td><td class=modelSummary>" +
					modelSpp +
					"</td><td class=modelSummary>" +
					modelDesc +
					"</td>");
//			out.println("<td>"+index+"</td>");
			out.println("</tr>");
			}
		out.println("</table>");
		}
	
	private void printModelDetails(ResultSet rs)  throws SQLException {
		out.println("<table class=modelDetails>");
		while (rs.next()) {
			/* model_id, model_name, spp, description */
			int modelID = rs.getInt("model_id");
			String modelName = rs.getString("model_name");
			String modelSpp = rs.getString("spp");
			String modelDesc = rs.getString("description");
			out.println(	"<tr><td class=modelDetails width=35% valign=top>" +
					"<h1>"+ modelName + "</h1>" +
					modelSpp +
					"<br>" +
					modelDesc +
					"</td></tr>");
			out.println("<tr><td>");
			try {
				printFormatSeq(rs.getString("sequence"));
				}
			catch (	IllegalSymbolException ex) {
				out.print(ex.getMessage());
				}	
			catch (	IllegalAlphabetException ex) {
				out.print(ex.getMessage());
				}	
			out.println("</td></tr>");
			
		// PRINT ALIGNMENTS
			ResultSet aligns = dbCon.getAlignsForModel(modelID);
			out.println("<tr><td class=modelDetails>");
			out.println("<h2>alignments</h2>");
			out.println("<table class=alignment><tr><th class=alignment>species</th><th class=alignment>location</th></tr>");
			while (aligns.next()) {
				String locationString = aligns.getString("location_string");
				String alignmentSpp = aligns.getString("spp");
				out.println("<tr><td class=alignment>"+alignmentSpp+"</td><td class=alignment>"+locationString+"</td></tr>");
				}
			out.println("</table>");
			out.println("</td></tr>");


		// PRINT ANNOTATIONS
			ResultSet subs = dbCon.getSubsForModel(modelID);
			out.println("<tr><td class=modelDetails>");
			out.println("<h2>annotations:</h2>");
			while (subs.next()) {
				String submissionDesc = subs.getString("description");
				int submissionID = subs.getInt("submission_id");
				ResultSet titles = dbCon.getTitlesForSub(submissionID);
				ArrayList titleArray = new ArrayList();
	
				while (titles.next()) {
					titleArray.add(titles.getString("title")); 
					}
				out.println("<h3>"+submissionDesc+"</h3>");
				printModelSubDetails(dbCon.getAnnoForModelAndSub(modelID, submissionID), titleArray.toArray());
				}
			out.println("</td></tr>");
			}
		out.println("</table>");
		}
	
	private void printFormatSeq(String seq) throws IllegalSymbolException, IllegalAlphabetException {
		String formattedString = "";
		char[] chararray = seq.toCharArray();
		int seqLength = Array.getLength(chararray);
		
		int lineLength = 70;

		int aaLength = ((seqLength - (seqLength%3))/3);

		int i=0;
		out.print("<div class=seq>");
		while (i < seqLength) {
			// ensure end of line doesn't overshoot refseq length:
			int endline;
			if (i+lineLength > seqLength) 
				{endline = seqLength;}
			else
				{endline = (i+lineLength);}
			/* phase 3-k */

			if(isDNA(seq)) {
				for (int k=2; k>=0; k--) {
					for (int j=i; j< endline; j++) {
						char[] codon = new char[3];

						if ((((j+k)%3)==0) && (j-1 >= 0) && (j+2 <= seqLength)) {
							out.print(getAA(seq.substring(j-1,j+2)));
							}
						else if ((((j+k-1)%3)==0) && (j-2 >= 0)) {
							out.print("]");
							}
						else if ((((j+k-2)%3)==0)  && (j+3 <= seqLength) ) {
							out.print("[");
							}

						else {
							out.print("&nbsp;");
							}
						if ((j+1) % 10 == 0) 
							{out.print("&nbsp;");}						   
						}

					out.print("<br>");
					}
				}
			
			for (int j=i; j< endline; j++) {
				out.print(chararray[j]);
				if ((j+1) % 10 == 0) 
					{out.print("&nbsp;");}						   
				}
			out.print("<br>");
					
			i = endline;
			}
		out.print("</div>");
		log("dna seq formatted");
		
		}
	
	private String getAA(String codonString) throws IllegalSymbolException, IllegalAlphabetException{
		//make a 'codon'
		SymbolList codon = DNATools.createDNA(codonString);
		return RNATools.translate(DNATools.toRNA(codon)).seqString();
		}
		
		
	private Boolean isDNA(String seq) {
		return false;
		}
	
	private void printModelSubDetails(ResultSet rs, Object[] cols) throws SQLException{
		out.println("<table class=annotation>");
		out.print("<tr>");
		
		Boolean[] displayColIndex = new Boolean[Array.getLength(cols)];
		String[] colLinksIndex = new String[Array.getLength(cols)];
		
/*	UNCOMMENT TO PRINT COL DEBUG INFO
		log(	confInfo.getColDisplays().toString() +"\n" + 
			Array.getLength(cols) + " -> " + Array.getLength(displayColIndex));
		log(confInfo.getColLinks().toString() +"\n" +
			Array.getLength(cols) + " -> " + Array.getLength(colLinksIndex));
		String colStatus = "";
*/		
		for (int i=0; i< Array.getLength(cols); i++) {
/*	UNCOMMENT TO PRINT COL DEBUG INFO	
			colStatus += 	"\t"+(String)cols[i]+
					" - "+confInfo.getColDisplay((String)cols[i])+
					"\t\t"+confInfo.getColLink((String)cols[i])+
					"\n";	
*/						
			displayColIndex[i] = confInfo.getColDisplay((String)cols[i]);
			if(displayColIndex[i]) {
				colLinksIndex[i] = confInfo.getColLink((String)cols[i]);
				out.print("<th class=annotation>"+(String)cols[i]+"</th>");}
			}
/*	UNCOMMENT TO PRINT COL DEBUG INFO
		log("\n"+colStatus);
*/
		out.println("</tr>");
		int colNo=0;
		while (rs.next()) {
			colNo++;
			String value = rs.getString("annotation_value");
			
			if (colNo == 1) {out.print("<tr>");}
			if (displayColIndex[(colNo-1)]) {
				out.print("<td class=annotation>");
				if(value != null) {
					if (colLinksIndex[(colNo-1)] != null) {
						out.print(	"<a href=\"" + 
								(String)colLinksIndex[colNo-1].replace("####",value)
								+"\">"+value+"</a></td>");
						}
					else {
						out.print(value);
						}
					}
				else {
					out.print("&nbsp;");}
				out.print("</td>");
				}
			if (colNo == Array.getLength(cols)) {
				out.println("</tr>");
				colNo = 0;
				}
			}
		out.println("</table>");
		}

	private void printAlignmentDetails(ResultSet rs) {
		
		}
	
	private void printSubmission(int submissionID)  throws SQLException {
		printSubmissionDetails(dbCon.getSubByID(submissionID));
		printModelSummary(dbCon.getModelsBySub(submissionID));
		}
		
	private void printAllSubmissions()  throws SQLException {
		printSubmissionDetails(dbCon.getAllSubs());
		}

	private void printSubmissionDetails(ResultSet rs)  throws SQLException {
		out.println("<table class=submission>");
		out.println(	"<tr><th class=submission>" +
				"date submitted: " +
				"</th><th class=submission>" +
				"no. models" +
				"</th><th class=submission>" +
				"description" +
				"</th></tr>");
		while (rs.next()) {
			/* submission_id, description, submitted (date), no_models */
			int submissionID = rs.getInt("submission_id");
			String submitted = rs.getString("submitted");
			int noModels = rs.getInt("no_models");
			String submissionDesc = rs.getString("description");
			out.println(	"<tr><td class=submission>" +
					/*"submission: "+submissionID + */
					"("+submitted+")"+
					"</td><td class=submission>" +
					"<a href=\""+servInfo.getHost()+"/?s="+submissionID+"\">"+noModels +" models"+"</a>"+
					"</td><td class=submission>" +
					submissionDesc +
					"</td></tr>");
			}
		out.println("</table>");
		}
		
	
	private void doSearch(String query)  throws SQLException {
		out.println("<span class=searchTerm> "+dbCon.countSearchAll(query)+" models returned for search: <i>"+query+"</i></span>");
		// ResultSet = dbCon.searchAll(query);
		
		printModelSummary(dbCon.searchAll(query));}
		
	
//	private void logResultSet

	}

	


