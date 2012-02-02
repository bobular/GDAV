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
 * Model Javabean
 * @author seth.redmond@imperial.ac.uk Seth Redmond
 * @see org.vectorbase.gdav.DBSQL.DBConnection
 */

public class Model extends Object {
	private int modelID;
	private String modelName;
	private String spp;
	private String description;
	private String seq;
	
	private List annotationTables = new ArrayList();
	private List alignments = new ArrayList();
	


	/** empty constructor */
	public Model() {
		super();
		
		}

	/** pass basic-info constructor */
	public Model(int newID, String newName, String newSpp, String newDesc) {
		super();
		modelID = newID;
		modelName = newName;
		spp = newSpp;
		description = newDesc;
		}
	
	
	public void setModelID(int newID) {
		modelID = newID;
		}
	
	public int getModelID() {
		return modelID;
		}
	
	public void setModelName(String newName) {
		modelName = newName;
		}
	
	public String getModelName() {
		return modelName;
		}
	
	public void setSpp(String newID) {
		spp = newID;
		}
	
	public String getSpp() {
		return spp;
		}
	
	/** returns ArrayList of  {@link org.vectorbase.gdav.beans.AnnotationTable} objects*/
	public List getAnnotationTables() {
		return annotationTables;
		}

	public void addAnnotationTable(AnnotationTable annotationTable) {
		annotationTables.add(annotationTable);
		}

	/** returns ArrayList of  {@link org.vectorbase.gdav.beans.Alignment} objects*/
	public List getAlignments() {
		return alignments;
		}

	public void addAlignment(Alignment alignment) {
		alignments.add(alignment);
		}



	public void setDescription(String newID) {
		description = newID;
		}
	
	public String getDescription() {
		return description;
		}
	
	public void setSeq(String newID) {
		seq = newID;
		}
	
	public String getSeq() {
		return seq;
		}
	
	/** returns seq formatted for ease of reading (10-base blocks, forced newline after 70bp)*/
	public String getFormatSeq() throws Exception{
		return formatSeq(seq);
		}
	
	private String formatSeq(String seq) throws IllegalSymbolException, IllegalAlphabetException {
		String returnString = "";
		char[] chararray = seq.toCharArray();
		int seqLength = Array.getLength(chararray);
		
		int lineLength = 70;

		int aaLength = ((seqLength - (seqLength%3))/3);

		int i=0;
		returnString += ("<div class=seq>");
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
							returnString += (getAA(seq.substring(j-1,j+2)));
							}
						else if ((((j+k-1)%3)==0) && (j-2 >= 0)) {
							returnString += ("]");
							}
						else if ((((j+k-2)%3)==0)  && (j+3 <= seqLength) ) {
							returnString += ("[");
							}

						else {
							returnString += ("&nbsp;");
							}
						if ((j+1) % 10 == 0) 
							{returnString += ("&nbsp;");}						   
						}

					returnString += ("<br>");
					}
				}
			
			for (int j=i; j< endline; j++) {
				returnString += (chararray[j]);
				if ((j+1) % 10 == 0) 
					{returnString += ("&nbsp;");}						   
				}
			returnString += ("&nbsp;&nbsp;&nbsp;&nbsp;"+endline+"<br>");
					
			i = endline;
			}
		returnString += ("</div>");
		return returnString;
		}
	
	private String getAA(String codonString) throws IllegalSymbolException, IllegalAlphabetException{
		//make a 'codon'
		SymbolList codon = DNATools.createDNA(codonString);
		return RNATools.translate(DNATools.toRNA(codon)).seqString();
		}
		
		
	private Boolean isDNA(String seq) {
		return false;
		}
	

	
//	private void logResultSet

	}

	


