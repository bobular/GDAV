package org.vectorbase.gdav.beans;

import java.io.*;
import java.text.*;
import java.lang.*;
import java.lang.reflect.*;
import java.util.*;
import java.net.*;

// import org.vectorbase.gdav.DBSQL.*;
// import org.vectorbase.gdav.*;

import org.biojava.bio.symbol.*;
import org.biojava.bio.seq.*;
import org.biojava.bio.BioException;


/**
 * Annotation Javabean
 * @author seth.redmond@imperial.ac.uk Seth Redmond
 * @see org.vectorbase.gdav.DBSQL.DBConnection
 */

public class AnnotationTable extends Object {
	private int submissionID;
	private List cols;
	private Hashtable displayCol = new Hashtable();
	private Hashtable colLinks = new Hashtable();
	private Hashtable colClasses = new Hashtable();
	/** list of AnnotationRow objects*/
	private List rows = new ArrayList(); 
	private String description;

	/** empty constructor */
	public AnnotationTable() {
		super();
		
		}

	/** pass basic-info constructor */
	public AnnotationTable(List cols, String desc) {
		super();
		setCols(cols);
		setDescription(desc);
		}
	
/*	public AnnotationTable(Object[] objectCols, String desc) {
		super();
		String[] cols = new String[Array.getLength(objectCols)];
		for (int i=0; i< Array.getLength(objectCols); i++) {
			cols[i] = (String)objectCols[i];
			}
		setCols(cols);
		setDescription(desc);
		}
*/	
	
	public void setDescription(String newDescription) {
		description = newDescription;
		}
	
	public String getDescription() {
		return description;
		}
	
	public void setCols(List newCols) {
		cols = newCols;
		// validateColumnDisplay();		
		}
	
	/** returns ArrayList of column titles (String)*/
	public List getCols() {
		return cols;
		}

	public void setColumnDisplay(String colName, Boolean displayVal) {
		displayCol.put(colName, displayVal);
		}
	
	public Boolean getColumnDisplay(String colName) {
		Boolean retVal = (Boolean)displayCol.get(colName);
		if(retVal == null) 	{return true;}
		else 			{return retVal;}			
		}

	/** returns Hashtable of Boolean objects, set via {@link org.vectorbase.gdav.ConfigInfo} object*/
	public Hashtable getColumnDisplays() {
		return displayCol;
		}

	public void setColumnLink(String colName, String linkStyle) {
		if(linkStyle != null) {
			colLinks.put(colName, linkStyle);
			}
		}
	
	/** returns individual column link */
	public String getColumnLink(String colName) {
		String retVal = (String)colLinks.get(colName);
		return retVal;
		}
	
	/** returns Hashtable of String objects for column link formats,
	  * set via {@link org.vectorbase.gdav.ConfigInfo} object
	  * links should be in format: "http://host.name/?query=####" 
	  * hashes will be replaced by result of 
	  * {@link org.vectorbase.gdav.ConfigInfo}: getAnnotation 
	  */
	public Hashtable getColumnLinks() {
		return colLinks;
		}


	public void setColumnClass(String colName, String colClass) {
		if(colClass != null) {
			colClasses.put(colName, colClass);
			}
		}
	
	/** returns individual column css class */
	public String getColumnClass(String colName) {
		String retVal = (String)colClasses.get(colName);
		return retVal;
		}
	
	/** returns Hashtable of String objects for column css classes
	  * set via {@link org.vectorbase.gdav.ConfigInfo} object
	  */
	public Hashtable getColumnClasses() {
		return colClasses;
		}


	public void addRow(AnnotationRow newRow) {
		rows.add(newRow);
		}
	
	public List getRows() {
		return rows;
		}
	
	
//	private void logResultSet

	}

	


