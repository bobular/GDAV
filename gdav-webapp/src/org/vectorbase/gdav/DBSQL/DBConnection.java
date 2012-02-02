package org.vectorbase.gdav.DBSQL;

import java.io.*;
import java.text.*;
import java.util.*;
import java.net.*;
import java.awt.*;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.io.IOException;
import org.xml.sax.SAXException;

//import java.sql.Connection; 
import java.sql.DriverManager; 
import java.sql.SQLException; 
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.Statement;

import com.mysql.jdbc.*;

import org.vectorbase.gdav.beans.*;
//import java.sql.*;

/**
 * Connection to gdav via JDBC
 * @author seth.redmond@imperial.ac.uk Seth Redmond
 * <p><STRONG>At Risk</STRONG>this modules is at risk of deletion / significant modification</p>
 */


public class DBConnection {

	Connection conn;
    	
	/** make new connection from "host:port/dbname" string with default user /no password*/ 
	public DBConnection(String dbString) throws SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        	super();
        	Class.forName("com.mysql.jdbc.Driver").newInstance();
		conn = (com.mysql.jdbc.Connection)DriverManager.getConnection(dbString);
		}

		
	/** make new connection from "host:port/dbname" string with explicit user / password*/ 
    	public DBConnection(String dbString, String user, String password) throws SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        	super();
        	Class.forName("com.mysql.jdbc.Driver").newInstance();
			conn = (com.mysql.jdbc.Connection)DriverManager.getConnection(dbString, user, password);
		}

	/** forcibly close JDBC connection*/ 
	public void severConnection() throws SQLException{
		conn.close();
		}	
		

	/** get all annotation submissions covered in db*/	
	public ResultSet getAllSubs() throws SQLException{
		Statement stmt = null;
		ResultSet rs = null;
    	stmt = conn.createStatement();
    	rs = stmt.executeQuery("SELECT submission.submission_id, description, date_format(submitted,'%e-%b-%y') as submitted, count(distinct model_id) as no_models FROM submission left join annotation using (submission_id) group by submission.submission_id");
		return rs;
		}

	/** get all annotation submissions covered in db*/	
	public ResultSet getSubByID(int submissionID) throws SQLException{
		PreparedStatement stmt = conn.prepareStatement("SELECT submission.submission_id, description, date_format(submitted,'%e-%b-%y') as submitted, count(distinct model_id) as no_models FROM submission left join annotation using (submission_id) where submission_id = ? group by submission.submission_id");
		stmt.setInt(1, submissionID);
		return stmt.executeQuery();
		}

	/** get all models in db*/	
	public ResultSet getAllModels() throws SQLException{
		Statement stmt = null;
		ResultSet rs = null;
    	stmt = conn.createStatement();
    	rs = stmt.executeQuery("SELECT model.model_id, model.model_name, model.spp, model.description FROM model order by model_id");
		return rs;
		}

	/** get one model in db*/	
	public ResultSet getModelByID(int modelID) throws SQLException{
		PreparedStatement stmt = conn.prepareStatement("SELECT model.model_id, model.model_name, model.spp, model.description, model.sequence FROM model where model_id = ?");
		stmt.setInt(1, modelID);
		return stmt.executeQuery();
		}

	/** get (hopefully one) model in db by name */	
	public ResultSet getModelByName(String model_name) throws SQLException{
		PreparedStatement stmt = conn.prepareStatement("SELECT model.model_id, model.model_name, model.spp, model.description, model.sequence FROM model where model_name = ?");
		stmt.setString(1, model_name);
		return stmt.executeQuery();
		}


	/** get models in db matching name */	
	public ResultSet getModelsWithNameLike(String str) throws SQLException{
		PreparedStatement stmt = conn.prepareStatement("SELECT model.model_id, model.model_name, model.spp, model.description, model.sequence FROM model WHERE model_name LIKE ?");
		stmt.setString(1, str);
		return stmt.executeQuery();
		}

	/** get model by submission_id*/	
	public ResultSet getModelsBySub(int subID) throws SQLException{
		PreparedStatement stmt = conn.prepareStatement("SELECT model.model_id, model.model_name, model.spp, model.description FROM model join annotation using (model_id) where submission_id = ? group by model.model_id");
		stmt.setInt(1, subID);
		return stmt.executeQuery();
		}

	/** get models in db that *overlap* location*/	
	public ResultSet getModelsByLocation(String spp, String chr, int start, int end) throws SQLException{
		PreparedStatement stmt = conn.prepareStatement("SELECT DISTINCT model.model_id, model.model_name, model.spp, model.description FROM model join alignment using (model_id) where alignment.spp = ? AND chr = ? AND end >= ? AND start <= ?");
		stmt.setString(1, spp);
		stmt.setString(2, chr);
		stmt.setInt(3, start);
		stmt.setInt(4, end);
		return stmt.executeQuery();
		}

	/** get models in db that *overlap* location, for a particular model species */	
	public ResultSet getModelsByLocationSpecies(String spp, String chr, int start, int end, String species) throws SQLException{
		PreparedStatement stmt = conn.prepareStatement("SELECT DISTINCT model.model_id, model.model_name, model.spp, model.description FROM model join alignment using (model_id) where alignment.spp = ? AND chr = ? AND end >= ? AND start <= ? AND model.spp = ?");
		stmt.setString(1, spp);
		stmt.setString(2, chr);
		stmt.setInt(3, start);
		stmt.setInt(4, end);
		stmt.setString(5, species);
		return stmt.executeQuery();
		}

	/** search all fulltext fields in all models*/	
	public ResultSet searchAll(String search) throws SQLException{
		PreparedStatement stmt = null;
		ResultSet rs = null;
		stmt = conn.prepareStatement("select SUM(score) as sum_score, model_id, model_name, spp, description from ((select match (annotation_value) against (?) as score, model.model_id, model.model_name, model.spp, model.description from annotation left join model using (model_id) where match (annotation_value) against (?)) UNION ALL (select match (model_name, spp, model.description) against (?) as score, model.model_id, model.model_name, model.spp, model.description from model where match (model_name, spp, model.description) against (?)) UNION ALL (select match (submission.description) against (?) as score, model.model_id, model.model_name, model.spp, model.description from submission left join annotation using (submission_id) left join model using (model_id) where match (submission.description) against (?))) temp group by model_id order by sum_score desc");

		for (int i=1;  i<=6; i++) stmt.setString(i, search);
		
		return stmt.executeQuery();

		}

  // is this really necessary?  howabout List.size from the result of the above?
	/** count results for all fulltext fields in all models*/	
	public int countSearchAll(String search) throws SQLException{

		// use code from searchAll if this is required
		return -1;
		}	

/* get model aspects: */

	/** get all submissions in db for one model*/	
	public ResultSet getSubsForModel(int model_id) throws SQLException{
		PreparedStatement stmt = conn.prepareStatement("SELECT DISTINCT submission.submission_id as submission_id, submission.description, date_format(submission.submitted,'%e-%b-%y') as submitted FROM submission join annotation using (submission_id) where model_id = ? order by submission.submitted asc");
		stmt.setInt(1, model_id);
		return stmt.executeQuery();
		}

	/** get all records in db for one model and one submission*/	
	public ResultSet getTitlesForSub(int submission_id) throws SQLException{
		PreparedStatement stmt = conn.prepareStatement("SELECT DISTINCT col_id, title FROM col where submission_id = ? order by col_id");
		stmt.setInt(1, submission_id);
		return stmt.executeQuery();
		}

	/** get all records in db for one model and one submission*/	
	public ResultSet getAnnoForModelAndSub(int model_id, int submission_id) throws SQLException{
		PreparedStatement stmt = conn.prepareStatement("SELECT row_id, col_id, title, annotation_value FROM annotation join col using (submission_id, col_id) where model_id = ? and col.submission_id = ? order by row_id, col_id");
		stmt.setInt(1, model_id);
		stmt.setInt(2, submission_id);
		return stmt.executeQuery();
		}
	
	/** get all alignments in db for one model*/	
	public ResultSet getAlignsForModel(int model_id) throws SQLException{
		PreparedStatement stmt = conn.prepareStatement("SELECT alignment_id, spp, chr, start, end, strand, cigar, score, method, CONCAT(chr,\":\",start,\"-\",end,\":\",strand) as location_string FROM alignment where model_id = ? order by spp asc");
		stmt.setInt(1, model_id);
		return stmt.executeQuery();
		}
	
	
	/** get all alignments in db for one model*/	
	public ResultSet getSeqForModel(int model_id) throws SQLException{
		PreparedStatement stmt = conn.prepareStatement("SELECT model.model_name, model.description, model.seq FROM model where model_id = ?");
		stmt.setInt(1, model_id);
		return stmt.executeQuery();
		}
		
	/** check if DBconnection has been closed by MySQL server, throw SQLException if closed*/
	public void ping() throws SQLException {
		try {
			ResultSet ping = conn.createStatement().executeQuery("SELECT 1");
		} catch (Exception ex) {
			throw new SQLException("connection closed");}
		}
	}
