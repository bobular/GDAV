package org.vectorbase.gdav;

import java.io.*;
import java.text.*;
import java.lang.*;
import java.lang.reflect.*;
import java.util.*;
import java.net.*;

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
import org.vectorbase.gdav.beans.*;
// import javax.sql.rowset.*;
//import java.sql.*;

//import org.biojava.bio.symbol.*;
//import org.biojava.bio.seq.*;
//import org.biojava.bio.BioException;



/**
 * <p>the main servlet, receives initial request from user, sets up
 * connections to the DB and provides a simple search function. <br> by default handles get requests as follows: 
 * <ul>
 * <li>If model_id is given retrieves all details for single {@link org.vectorbase.gdav.beans.Model} - 
 * places bean in context (attribute: 'model') - model.jsp <li/>
 * <li>If search string is given performs search, returns ArrayList of 
 * {@link org.vectorbase.gdav.beans.Model} objects with minimal info (attribute: 'models') - models.jsp <li/>
 * <li>if model_id=0 returns all {@link org.vectorbase.gdav.beans.Model} objects in DB with minimal info (context: models) - models.jsp <li/>
 * <li>if submission_id is given retrieves single {@link org.vectorbase.gdav.beans.Submission} (context:submission) 
 * and all models in summary with minimal info (context: models) - submission.jsp <li>
 * <li>if submission_id=0, retrieves all  {@link org.vectorbase.gdav.beans.Submission} objects (context: submissions) - submissions.jsp <li> 
 * all searches are performed via {@link org.vectorbase.gdav.DBSQL.DBConnection} 
 * </ul></p>

 * @author seth.redmond@imperial.ac.uk Seth Redmond
 * @see org.vectorbase.gdav.ServletInfo
 */

public class GdavServlet extends HttpServlet {
	DBConnection dbCon;
	HttpSession session;
	ServletInfo servInfo;
	ConfigInfo confInfo;
	ServletContext ctx;
	PrintWriter out;
	String welcomeJSP = "/welcome.jsp";
	String modelListJSP = "/models.jsp";
	String modelDetailsJSP = "/model.jsp";
	String submissionListJSP = "/submissions.jsp";
	String submissionDetailsJSP = "/submission.jsp";
	/**
	 * reads in configFiles: {@link org.vectorbase.gdav.ServletInfo}  (DB urls and basic config details) 
	 * and {@link org.vectorbase.gdav.ConfigInfo} (column links and display configuration), 
	 * creates {@link org.vectorbase.gdav.DBSQL.DBConnection} <br />
	 * prints errors, if any, to log
	 */
	public void init() {
		try {
			ctx = this.getServletContext();
			servInfo = new ServletInfo(ctx);
			// confInfo = new ConfigInfo(this.getServletContext());
			// makeConnections();
			ctx.setAttribute("root",servInfo.getHost());
			ctx.setAttribute("remoteheader",servInfo.getRemoteHeader());
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
			confInfo = new ConfigInfo(this.getServletContext());
			}
		catch (SAXException ex) {
			log("Could not parse cols config file\n" + ex.getMessage());
			ex.printStackTrace();
/*			try {
				confInfo = new ConfigInfo(this.getServletContext());}
			catch (SAXException ex) {
				log("Could not create null config file\n" + ex.getMessage());
				ex.printStackTrace();		
				}
*/
			confInfo = new ConfigInfo();
			}
		catch (ParserConfigurationException ex) {
			log("parser not configured correctly\n" + ex.getMessage());
			ex.printStackTrace();
			confInfo = new ConfigInfo();
			}
		catch (IOException ex) {
			log("parser not configured correctly (file not found?)\n" + ex.getMessage());
			ex.printStackTrace();
			confInfo = new ConfigInfo();
			}
		ctx.setAttribute("title",confInfo.getTitle());
		ctx.setAttribute("configInfo",confInfo);
		}
	
	/** severs DBConnection, calls parent destroy method  {@link org.vectorbase.gdav.DBSQL.DBConnection}*/
	public void destroy() {
	
		try {
			dbCon.severConnection();
			}
		catch (SQLException sqlEx) {
			log("could not sever DB connection\n"+sqlEx);
			}
		super.destroy();
		}
	
	
	/** gets new  {@link org.vectorbase.gdav.DBSQL.DBConnection} for object - called automatically on init*/
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
		
		}


	/** handles get calls (see intro)*/
	public void doGet(HttpServletRequest request,
                      HttpServletResponse response) throws IOException, ServletException {

		session = request.getSession(true);

		String[] path = request.getServletPath().split("/");		
		if(Array.getLength(path) > 2) {
			response.sendRedirect(servInfo.getHost()+"/"+path[1]+"/");
			}

		checkHealth();

		response.setContentType("text/html");

		String query = request.getParameter("q");
		int submissionID = -1;
		if(request.getParameter("s") != null) {submissionID = Integer.parseInt(request.getParameter("s"));}
		int modelID = -1;
		if(request.getParameter("m") != null) {modelID = Integer.parseInt(request.getParameter("m"));}

		try {
			if (modelID != -1) {
				if(modelID==0) {
					printAllModels(request, response);}
				else {
					printModel(modelID, request, response);}
				}
			else if (submissionID != -1) { 	
				if(submissionID ==0) {
					printAllSubmissions(request, response);
					}
				else {
					printSubmission(submissionID, request, response);
					}
				}
			else if (query != null) 	{ 	String indexString = request.getParameter("i");
								int index=0;
								if (indexString != null) {
									index = Integer.parseInt(indexString);}
								doSearch(query, request, response);
					}
			else {		//printAllSubmissions(request, response);
					printWelcome(request, response);
					}

			}
		catch (SQLException ex) {
			// handle any errors
			out = response.getWriter();
			out.println("<title>gdav: error</title>");
			out.println("</head><body>");
			out.println("SQLException: <pre>" + ex.getMessage());
			out.println("SQLState: " + ex.getSQLState());
			out.println("VendorError: " + ex.getErrorCode()+"</pre>");
			}			
		catch (Exception ex) {
			ex.printStackTrace(out);
			}		

		}
		
	/** handle post calls (returns default welcome page) */ 			
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException  {
		session = request.getSession(true);

		String[] path = request.getServletPath().split("/");		
		if(Array.getLength(path) > 2) {
			response.sendRedirect(servInfo.getHost()+"/"+path[1]+"/");
			}

		checkHealth();

		response.setContentType("text/html");

		try {
			//printAllSubmissions(request, response);
			printWelcome(request, response);
			}
		catch (SQLException ex) {
			// handle any errors
			out = response.getWriter();
			out.println("<title>gdav: error</title>");
			out.println("</head><body>");
			out.println("SQLException: <pre>" + ex.getMessage());
			out.println("SQLState: " + ex.getSQLState());
			out.println("VendorError: " + ex.getErrorCode()+"</pre>");
			}			
		catch (Exception ex) {
			ex.printStackTrace(out);
			}		
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
	

	private void printModel(int modelID, HttpServletRequest request, HttpServletResponse response)  throws SQLException, IOException, ServletException {
		ctx.setAttribute("query",null);
		
		ResultSet rs =  dbCon.getModelByID(modelID);
		rs.next();
		Model model = new Model(modelID, rs.getString("model_name"), rs.getString("spp"),
		rs.getString("description"));
		model.setSeq(rs.getString("sequence"));
		ctx.setAttribute("model",model);
		if (request.getParameter("oq") != null) { ctx.setAttribute("query",request.getParameter("oq")); }

		ResultSet alignRs = dbCon.getAlignsForModel(modelID);
		while (alignRs.next()) {
			model.addAlignment(new Alignment(	alignRs.getString("spp"),
								alignRs.getString("chr"),
								alignRs.getInt("start"),
								alignRs.getInt("end"), 
								alignRs.getInt("strand"),
								alignRs.getString("cigar"),
								alignRs.getInt("score"),
								alignRs.getString("method")
							)
					);
			}

		ResultSet subRs = dbCon.getSubsForModel(modelID);
		while (subRs.next()) {
//			Submission sub = new Submission(subRs.getInt("submission_id"), subRs.getString("description"), subRs.getString("submitted"));
			int subID = subRs.getInt("submission_id");
			ResultSet cols = dbCon.getTitlesForSub(subID);
			ArrayList colTitles = new ArrayList();
			while (cols.next()) {
				colTitles.add(cols.getString("title"));
				
				}
			AnnotationTable table = new AnnotationTable(colTitles, subRs.getString("description"));
			for (int i=0; i<colTitles.size(); i++) {
				table.setColumnDisplay((String)colTitles.get(i), confInfo.getColDisplay((String)colTitles.get(i)));
				table.setColumnLink((String)colTitles.get(i), confInfo.getColLink((String)colTitles.get(i)));
				table.setColumnClass((String)colTitles.get(i), confInfo.getColClass((String)colTitles.get(i)));
				}
//			log(table.getColumnDisplays().toString());
			
			ResultSet rows = dbCon.getAnnoForModelAndSub(modelID, subID);
			Hashtable rowHash = new Hashtable();
			while (rows.next()) {
				int rowID = rows.getInt("row_id");
				if(rowHash.get(rowID) == null) {
//					log("new row " + rowID);
					rowHash.put(rowID, new AnnotationRow(table));
					}
				
				((AnnotationRow)rowHash.get(rowID)).setAnnotation(rows.getString("title"), rows.getString("annotation_value"));
//				log("adding annotation "+rows.getString("title")+"::"+rows.getString("annotation_value")+" to row "+table.getDescription());
				}
			for(int i=1; i<=rowHash.size(); i++) {
//				log("\tadding row to table "+table.getDescription());
				table.addRow((AnnotationRow)rowHash.get(i));
				}
			
			model.addAnnotationTable(table);
			}
		
		request.getRequestDispatcher(modelDetailsJSP).forward(request, response);
		}
	
	private void printWelcome(HttpServletRequest request, HttpServletResponse response)  throws SQLException, IOException, ServletException {
		ctx.setAttribute("query",null);
		ctx.setAttribute("model",null);
		request.getRequestDispatcher(welcomeJSP).forward(request, response);
		}
		
				
	private void printAllModels(HttpServletRequest request, HttpServletResponse response)  throws SQLException, IOException, ServletException {
		ctx.setAttribute("query",null);
		List models = modelsRSToArrayList(dbCon.getAllModels());
		request.setAttribute("models",models);
		
		request.getRequestDispatcher(modelListJSP).forward(request, response);
		}
		
	private void printSubmission(int submissionID, HttpServletRequest request, HttpServletResponse response)  throws SQLException, IOException, ServletException {
		List subs = subsRSToArrayList(dbCon.getSubByID(submissionID));
		request.setAttribute("submission",subs);
		List models = modelsRSToArrayList(dbCon.getModelsBySub(submissionID));
		request.setAttribute("models",models);
		request.getRequestDispatcher(submissionDetailsJSP).forward(request, response);
		}
		
	private void printAllSubmissions(HttpServletRequest request, HttpServletResponse response)  throws SQLException, IOException, ServletException {
		List subs = subsRSToArrayList(dbCon.getAllSubs());
		request.setAttribute("submissions",subs);
		log(subs.toString());
		request.getRequestDispatcher(submissionListJSP).forward(request, response);
		}
	
	private void doSearch(String query, HttpServletRequest request, HttpServletResponse response)  throws SQLException, IOException, ServletException {
		ctx.setAttribute("query",query);
		ctx.setAttribute("model",null);
		List models = modelsRSToArrayList(dbCon.searchAll(query));
		int noModels = models.size(); // dbCon.countSearchAll(query);
		if (noModels == 1) { // && quicksearch=1 ?
			Model model = (Model) models.get(0);
			response.setStatus(response.SC_MOVED_TEMPORARILY);
			response.setHeader("Location", servInfo.getHost()+"/?m="+model.getModelID()+"&oq="+query);
			// printModel(model.getModelID(), request, response);		
		} else {
			request.setAttribute("noModels",noModels);
			request.setAttribute("models",models);
			request.getRequestDispatcher(modelListJSP).forward(request, response);
		}
	}
		
	private List subsRSToArrayList(ResultSet subRs) throws SQLException {
		List subs = new ArrayList();
		while (subRs.next()) {
			Submission sub = new Submission(subRs.getInt("submission_id"), subRs.getString("description"), subRs.getString("submitted"));
			try {
				sub.setNoModels(subRs.getInt("no_models"));
				}
			catch(SQLException ex) {
				log("Sub "+sub.getSubmissionID()+": noModels is null \n\t"+ex.getMessage());
				}
			log(sub.getDescription());
			subs.add(sub);
			}
		return subs;
		}	

	private List modelsRSToArrayList(ResultSet rs) throws SQLException {
		List models = new ArrayList();
		while (rs.next()) {
			Model model = new Model(rs.getInt("model_id"), rs.getString("model_name"), rs.getString("spp"),
			rs.getString("description"));
			models.add(model);
			}
		return models;
		}	
//	private void logResultSet

	}

	


