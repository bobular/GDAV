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

public class AnnotationRow extends Object {
	private int row;
	/** list of hashtables (colName -> value) */
	private Hashtable values = new Hashtable(); 
	private AnnotationTable table;	

	/** empty constructor */
	public AnnotationRow() {
		super();
		
		}

	/** pass basic-info constructor */
	public AnnotationRow(AnnotationTable newTable) {
		super();
		table = newTable;
		}
	
	
	public void setAnnotation(String colName, String annotation) {
		if(annotation != null) {
			values.put(colName, annotation);
			}
		else {
			values.put(colName,"&nbsp;");
			}
		}
	
	public String getAnnotation(String colName) {
		return (String)values.get(colName);
		}
	
	public Hashtable getAnnotations() {
		return values;
		}

/*	public Hashtable getAnnotationLinks() {
		Hashtable links = new Hashtable();
		for(int i=0; i<values.size(); i++) {
			String value = values.get(i);
			String link = table.getColumnLink(value);
			if(link != null) {
				link.replace("####",value)
				links.put(					
				}
			}
		return links;
		}

*/
	public int getAnnoCount() {
		return values.size();
		}
	
	public void setTable(AnnotationTable newTable) {
		table = newTable;
		}

	public AnnotationTable getTable() {
		return table;
		}
	
		
//	private void logResultSet

	}

	


