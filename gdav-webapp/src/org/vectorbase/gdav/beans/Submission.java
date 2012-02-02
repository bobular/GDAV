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
 * Submission Javabean
 * @author seth.redmond@imperial.ac.uk Seth Redmond
 * @see org.vectorbase.gdav.DBSQL.DBConnection
 */

public class Submission extends Object {
	private int submissionID;
	private String submissionName;
	private String submitted;
	private String description;
	private int noModels;
	 

	/** empty constructor */
	public Submission() {
		super();
		
		}

	/** pass basic-info constructor */
	public Submission(int newID, String newDesc, String newDate) {
		super();
		submissionID = newID;
		description = newDesc;
		submitted = newDate;
		}
	
	
	public void setSubmissionID(int newID) {
		submissionID = newID;
		}
	
	public int getSubmissionID() {
		return submissionID;
		}
	
	public void setSubmitted(String newDate) {
		submitted = newDate;
		}
	
	public String getSubmitted() {
		return submitted;
		}
	
	public void setNoModels(int newNo) {
		noModels = newNo;
		}
	
	public int getNoModels() {
		return noModels;
		}
	
	public void setDescription(String newID) {
		description = newID;
		}
	
	public String getDescription() {
		return description;
		}
	
	

	
//	private void logResultSet

	}

	


