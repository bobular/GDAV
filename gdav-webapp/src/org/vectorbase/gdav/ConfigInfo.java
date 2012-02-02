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

import org.xml.sax.*;
import java.util.Hashtable;

//import com.mysql.jdbc.*;

/**
 *
 * reads all relevant information from the optional (config.xml) config file
 * stores this information and provides accessor classes for this
 * and other information. 
 * @author seth.redmond@imperial.ac.uk Seth Redmond
 */


public class ConfigInfo {
	
	private String documentRoot = "webapps/gdav/";
	private String title = "Genome De-linked Annotation Viewer";
	
	private static String xml_config = "/config.xml";
	
	private Hashtable colLinks = new Hashtable();
	private Hashtable colClasses = new Hashtable();
	private Hashtable colDisplay = new Hashtable();
	private Hashtable sppLinks = new Hashtable();
	
	
	/** search for config.xml file, parse for column / display configuration details*/
	public ConfigInfo (ServletContext ctx) throws SAXException, ParserConfigurationException, IOException{
		super();
	    	InputSource is = new InputSource(ctx.getResourceAsStream(xml_config));
		DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Element node = parser.parse(is).getDocumentElement();
		parseNode(node);
		}
	
	/** 'emergency' constructor - uses default values for webapp title and provides no column configuration*/
	public ConfigInfo() {
		super();
		}	
	
	private void parseNode(Element node) {
		for(Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child.getNodeName().equalsIgnoreCase("col")) {
				String hyperlink; 
				String cssClass;
				String colName;
					colName = (String)child.getAttributes().getNamedItem("name").getNodeValue();
					Boolean display = Boolean.parseBoolean((String)child.getAttributes().getNamedItem("display").getNodeValue());
					colDisplay.put(colName.toLowerCase(), display);
					
				for(Node grandChild = child.getFirstChild(); grandChild != null; grandChild = grandChild.getNextSibling()) {
					if (grandChild.getNodeName().equalsIgnoreCase("hyperlink")) {
						hyperlink = grandChild.getFirstChild().getNodeValue(); // not working?? 
					//	try {
						colLinks.put(colName.toLowerCase(), hyperlink);
					/*		}
						catch (NullPointerException ex) {
							throw new NullPointerException("\n\n" + colName +"!!\n"+hyperlink + "!!\n\n");
							}
					*/
					} else if (grandChild.getNodeName().equalsIgnoreCase("class")) {
						cssClass = grandChild.getFirstChild().getNodeValue(); // not working?? 
						colClasses.put(colName.toLowerCase(), cssClass);
					}

					}

				}
			if (child.getNodeName().equalsIgnoreCase("alignSpp")) {
				String hyperlink; 
				String spp;
					spp = (String)child.getAttributes().getNamedItem("name").getNodeValue();
					
				for(Node grandChild = child.getFirstChild(); grandChild != null; grandChild = grandChild.getNextSibling()) {
					if (grandChild.getNodeName().equalsIgnoreCase("hyperlink")) {
						hyperlink = grandChild.getFirstChild().getNodeValue(); // not working?? 
					//	try {
						sppLinks.put(spp, hyperlink);
					/*		}
						catch (NullPointerException ex) {
							throw new NullPointerException("\n\n" + colName +"!!\n"+hyperlink + "!!\n\n");
							}
					*/
						}

					}

				}
			if (child.getNodeName().equalsIgnoreCase("title")) {
				title = child.getFirstChild().getNodeValue();
				}
			}
		File root = new File(documentRoot);
		documentRoot = root.getAbsolutePath();	
		}

	/** get Hashtable of display [true/false] values for cols*/
	public Hashtable getColDisplays() {
		return this.colDisplay; }
	
	/** get individual display boolean for column (default true) */ 
	public Boolean getColDisplay(String colName) {
		Boolean retVal = (Boolean)colDisplay.get(colName.toLowerCase());
		if(retVal == null) {retVal = true;}
		return retVal;
		}

	/** get Hashtable of hypertext formats for cols (http://www.website.com/urlFormat.html?id=####)*/
	public Hashtable getColLinks() {
		return this.colLinks;}

	/** get individual link for column */
	public String getColLink(String colName) {
		String retVal = (String)colLinks.get(colName.toLowerCase());
		return retVal;
		}

	/** get Hashtable of CSS classes for cols */
	public Hashtable getColClasses() {
		return this.colClasses;}

	/** get individual CSS class for column */
	public String getColClass(String colName) {
		String retVal = (String)colClasses.get(colName.toLowerCase());
		return retVal;
		}

	/** return root URL for website. This is defined in the tillview.xml file in order to 
	allow the site to be run from the root, or for aliases to be employed*/
	public String getDocumentRoot() {
		return this.documentRoot; }

	/** return title for website*/
	public String getTitle() {
		return this.title; }
	
	public Hashtable getSppLinks() {
		return sppLinks;}
	
	}
	
 
