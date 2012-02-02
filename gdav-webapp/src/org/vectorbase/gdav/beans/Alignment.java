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
 * Alignment Javabean
 * @author seth.redmond@imperial.ac.uk Seth Redmond
 * @see org.vectorbase.gdav.DBSQL.DBConnection
 */

public class Alignment extends Object {
	private String spp;
	private String chr;
	private int start;
	private int end;
	private int strand;
	private String cigar;
	private int score;
	private String method;

	/** empty constructor */
	public Alignment() {
		super();
		
		}

	/** pass basic-info constructor */
	public Alignment(String newSpp, String newChr, int newStart, int newEnd, int newStrand, String newCigar, int
	newScore, String newMethod) {
		super();
		spp = newSpp;
		chr = newChr;
		start = newStart;
		end = newEnd;
		strand = newStrand;
		cigar = newCigar;
		score = newScore;
		method = newMethod;
		}
	
	public void setMethod(String newMethod) {
		method = newMethod;
		}
	
	public String getMethod() {
		return method;
		}
		
	public void setSpp(String newSpp) {
		spp = newSpp;
		}
	
	public String getSpp() {
		return spp;
		}

	public void setChr(String newChr) {
		chr = newChr;
		}
	
	public String getChr() {
		return chr;
		}
	
	
	public void setStart(int newStart) {
		start = newStart;
		}
	
	public int getStart() {
		return start;
		}
	
	public void setEnd(int newEnd) {
		end = newEnd;
		}
	
	public int getEnd() {
		return end;
		}
	
	public void setStrand(int newStrand) {
		strand = newStrand;
		}
	
	public int getStrand() {
		return strand;
		}
	
	public void setCigar(String newCigar) {
		cigar = newCigar;
		}
	
	public String getCigar() {
		return cigar;
		}

	public void setScore(int newScore) {
		score = newScore;
		}
	
	public int getScore() {
		return score;
		}

	public String getLocation() {
		return chr +":"+start+"-"+end;
		}

	/** use cigar line to split alignment into constituent high-scoring pairs*/
	public List getHSPs() {
		List hsps = new ArrayList();
		
		// DO SOMETHING...
		
		return hsps;
		}

	
//	private void logResultSet

	}

	


