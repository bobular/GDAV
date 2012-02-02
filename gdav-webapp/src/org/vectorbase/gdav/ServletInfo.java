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
import java.sql.SQLException; 
import java.sql.ResultSet;

import org.vectorbase.gdav.DBSQL.*;
import org.xml.sax.*;

//import com.mysql.jdbc.*;

/**
 *
 * reads all relevant information from the compulsory (gdav.xml) config file
 * stores this information and provides accessor classes for this
 * and other information. 
 * @author seth.redmond@imperial.ac.uk Seth Redmond
 */


public class ServletInfo {
	
	private String host;

	private String remoteheaderurl;
	private String remoteheaderstart;
	private String remoteheaderstop;

	private String dbHost;
	private String dbName;
	private String dbUser;
	private String dbPass;
	private int dbPort;

/*	private String[] ensDbHost;
	private String[] ensDbName;
	private String[] ensDbUser;
	private String[] ensDbPass;
	private int[] ensDbPort;
	public String[] ensDbVersion;
*/	
	
	private String documentRoot = "webapps/gdav/";
		
	private static String xml_config = "/gdav.xml";
	
	/** constructor - requires servlet context to retrieve config file */ 			
	public ServletInfo (ServletContext ctx) throws SAXException, ParserConfigurationException, IOException{
		super();
	    	InputSource is = new InputSource(ctx.getResourceAsStream(xml_config));
		DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Element node = parser.parse(is).getDocumentElement();
		parseNode(node);
		}

	/** constructor - requires InputSource of config file */ 			
	public ServletInfo (InputSource is) throws SAXException, ParserConfigurationException, IOException{
		super();
		DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Element node = parser.parse(is).getDocumentElement();
		parseNode(node);
		}

	/** parse db information from gdav.xml */ 		
	private void parseNode(Element node) {
		for(Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child.getNodeName().equalsIgnoreCase("host")) {
				host = child.getFirstChild().getNodeValue();
				}
			else if (child.getNodeName().equalsIgnoreCase("remoteheader")) {
				for(Node grandChild = child.getFirstChild(); grandChild != null; grandChild = grandChild.getNextSibling()) {
					if (grandChild.getNodeName().equalsIgnoreCase("url")) {
						remoteheaderurl = grandChild.getFirstChild().getNodeValue();}
					else if (grandChild.getNodeName().equalsIgnoreCase("start")) {
						remoteheaderstart = grandChild.getFirstChild().getNodeValue();}
					else if (grandChild.getNodeName().equalsIgnoreCase("stop")) {
						remoteheaderstop = grandChild.getFirstChild().getNodeValue();}
				}
			}
			else if (child.getNodeName().equalsIgnoreCase("db")) {
				for(Node grandChild = child.getFirstChild(); grandChild != null; grandChild = grandChild.getNextSibling()) {
					if (grandChild.getNodeName().equalsIgnoreCase("dbhost")) {
						dbHost = grandChild.getFirstChild().getNodeValue();}
					else if (grandChild.getNodeName().equalsIgnoreCase("dbname")) {
						dbName = grandChild.getFirstChild().getNodeValue();}
					else if (grandChild.getNodeName().equalsIgnoreCase("dbuser")) {
						dbUser = grandChild.getFirstChild().getNodeValue();}
					else if (grandChild.getNodeName().equalsIgnoreCase("dbpass")) {
						dbPass = grandChild.getFirstChild().getNodeValue();}
					else if (grandChild.getNodeName().equalsIgnoreCase("dbport")) {
						dbPort = new Integer(grandChild.getFirstChild().getNodeValue()).intValue();}
					}
				}
/*			if (child.getNodeName().equalsIgnoreCase("DNAdb")) {
				int arrIndex = -1;
				if (child.getAttributes().getNamedItem("default").getNodeValue().equalsIgnoreCase("true")) {
					arrIndex = 0;}
				else {
					ensVersions++; arrIndex = ensVersions;}

				ensDbVersion[arrIndex] = child.getAttributes().getNamedItem("version").getNodeValue();
				for(Node grandChild = child.getFirstChild(); grandChild != null; grandChild = grandChild.getNextSibling()) {
					if (grandChild.getNodeName().equalsIgnoreCase("host")) {
						ensDbHost[arrIndex] = grandChild.getFirstChild().getNodeValue();}
					else if (grandChild.getNodeName().equalsIgnoreCase("name")) {
						ensDbName[arrIndex] = grandChild.getFirstChild().getNodeValue();}
					else if (grandChild.getNodeName().equalsIgnoreCase("user")) {
						ensDbUser[arrIndex] = grandChild.getFirstChild().getNodeValue();}
					else if (grandChild.getNodeName().equalsIgnoreCase("pass")) {
						ensDbPass[arrIndex] = grandChild.getFirstChild().getNodeValue();}
					else if (grandChild.getNodeName().equalsIgnoreCase("port")) {
						ensDbPort[arrIndex] = new Integer(grandChild.getFirstChild().getNodeValue()).intValue();}
					}
				}
*/
			}	
		File root = new File(documentRoot);
		documentRoot = root.getAbsolutePath();	
		}
	/** return root URL for website. This is defined in the tillview.xml file in order to 
	allow the site to be run from the root, or for aliases to be employed*/
	public String getHost() {
		return this.host; }

	/** return host name for gdav DB*/
	public String getDbHost() {
		return this.dbHost; }

	/** return name for gdav DB*/
	public String getDbName() {
		return this.dbName; }

	/** return user name for gdav DB*/
	public String getDbUser() {
		return this.dbUser; }

	/** return password for gdav DB*/
	public String getDbPass() {
		return this.dbPass; }

	/** return port for gdav DB*/
	public int getDbPort() {
		return this.dbPort; }

	/** return full url string (jdbc:mysql//<i>host</i>:<i>port</i>/<i>name</i>?user=<i>user</i>&pass=<i>pass</i>) for gdav DB*/
	public String getDbString() {
		return "jdbc:mysql://"+dbHost+":"+dbPort+"/"+dbName;
		}

	
	/** return document root for gdav.xml*/
	public String getDocumentRoot() {
		return this.documentRoot; }


	/** return the header HTML grabbed from a remote URL */
	public String getRemoteHeader() {
		StringBuilder header = new StringBuilder();

		try {
			URL vb = new URL(remoteheaderurl);
			BufferedReader in = new BufferedReader(
				new InputStreamReader(vb.openStream()));
			Boolean inHeader = false;
			String inputLine;

			while ((inputLine = in.readLine()) != null) {
				if (!inHeader && inputLine.contains(remoteheaderstart)) {
					inHeader = true;
				}	else if (inHeader && inputLine.contains(remoteheaderstop)) {
					inHeader = false;
				}

				if (inHeader) {
					header.append(inputLine);
					header.append("\n");
				}
			}
			in.close();
		} catch (Exception ex) {
			// do nothing at the moment
		}

		return header.toString();
	}

	}
