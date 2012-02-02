/*
 *   http://www.vectorbase.org
 */

/** temp compile

setenv CLASSPATH `perl -e 'print join ":", glob("/usr/local/tomcat/webapps/das/WEB-INF/lib/*.jar"), "/usr/local/tomcat/webapps/gdav/WEB-INF/classes"'`

javac org/vectorbase/gdav/dazzle/AlignmentSource.java

temp deploy

javac org/vectorbase/gdav/dazzle/AlignmentSource.java ; sudo cp -r org /usr/local/tomcat/webapps/das/WEB-INF/classes/

restart dazzle from tomcat manager

*/

package org.vectorbase.gdav.dazzle;

import org.biojava.servlets.dazzle.*;
import org.biojava.servlets.dazzle.datasource.*;

import java.sql.ResultSet;
import java.sql.SQLException; 

import java.util.*;
import java.util.regex.*;
import java.net.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.biojava.utils.*;
import org.biojava.utils.cache.*;
import org.biojava.bio.*;
import org.biojava.bio.seq.*;
import org.biojava.bio.seq.impl.*;
import org.biojava.bio.seq.db.*;
import org.biojava.bio.seq.io.*;
import org.biojava.bio.symbol.*;

/*
	import org.biojava.bio.program.gff.*;
*/


import org.vectorbase.gdav.DBSQL.*;

import org.biojava.utils.xml.*;

import java.util.regex.Pattern;

/**
 * Annotation datasource backed by a simple custom MySQL database.
 *
 * @author Thomas Down
 * @version 1.00
 */

public class AlignmentSource extends AbstractDataSource implements DazzleDataSource {
    
    private Set allTypes;

  	private String assembly;
  	private String species; /* optional for multi-species GDAVs */
  	private String linkout;

  	private final String type = "match";
  	private String groupmode = "alignment"; /* can also be "location" */

    private String dbhost;
    private String dbname;
    private String dbport;
    private String dbuser;
    private String dbpass;

    private String mapMaster;

  	private DBConnection dbCon;

    public String getDataSourceType() {
        return type;
    }
    
    public String getDataSourceVersion() {
        return "1.00";
    }
    
    public void setMapMaster(String s) {
        this.mapMaster = s;
    }

    public String getMapMaster() {
        return mapMaster;
    }

    public void setAssembly(String s) {
        assembly = s;
    }

    public void setSpecies(String s) {
        species = s;
    }

	  public void setLinkout(String s) {
        linkout = s;
    }


    public void setDbhost(String s) {
        dbhost = s;
    }

    public void setDbport(String s) {
        dbport = s;
    }

    public void setDbname(String s) {
        dbname = s;
    }

    public void setDbuser(String s) {
        dbuser = s;
    }

    public void setDbpass(String s) {
        dbpass = s;
    }

	  public void setGroupmode(String s) {
		  	groupmode = s;
		}

   
	  public void init(ServletContext ctx) 
        throws DataSourceException
    {
        super.init(ctx);
        try {
					  allTypes = new HashSet();
            allTypes.add(type);

						dbCon = new DBConnection(this.getDbString(), dbuser, dbpass);

        } catch (Exception ex) {
            throw new DataSourceException(ex, "Couldn't connect to database");
        }
    }

  	DBConnection getDBConnection()
        throws DataSourceException
    {
			try {
				dbCon.ping();
			} catch (Exception ex) {
				try {
					dbCon = new DBConnection(this.getDbString(), dbuser, dbpass);
				} catch (Exception ex2) {
            throw new DataSourceException(ex2, "Couldn't connect to database");
        }
			}
			return dbCon;
		}  

	  private String getDbString() {
		  return "jdbc:mysql://"+dbhost+":"+dbport+"/"+dbname;
		}

    public Sequence getSequence(String ref)
	    throws DataSourceException, NoSuchElementException
    {
			int length = Integer.MAX_VALUE;
			return new SimpleSequence(new DummySymbolList(DNATools.getDNA(), length), ref, ref, Annotation.EMPTY_ANNOTATION);
    }

 	  /* don't understand landmarks or mapmasters... not sure if this is OK... */
    public String getLandmarkVersion(String ref) 
	    throws DataSourceException, NoSuchElementException
    {
			return getVersion();
    }

    public FeatureHolder getFeatures(String chr, int start, int end)
	    throws NoSuchElementException, DataSourceException
    {
			/* no way to get start, end currently - so we get all features per chromosome! */

/*			int start = 1;
				int end = Integer.MAX_VALUE; */

			/* regexp to grab chromosome start end
			Pattern cse = Pattern.compile("^(.+):(\\d+),(\\d+)");
			Matcher m = cse.matcher(ref);
			try {
				chr = m.group(1);
				start = Integer.parseInt(m.group(2));
				end = Integer.parseInt(m.group(3));
			} catch (IllegalStateException ex)
			{
				throw new DataSourceException(ex, "Cannot parse coordinates " + ref);
			}
			*/

			Sequence seq = getSequence(chr);
			ViewSequence vseq = new ViewSequence(seq);
			try {
				// System.err.printf("ass chr start end %s %s %d %d\n", assembly, chr, start, end);
				ResultSet models = species == null ? this.getDBConnection().getModelsByLocation(assembly, chr, start, end) : this.getDBConnection().getModelsByLocationSpecies(assembly, chr, start, end, species);
				while (models.next()) {
					int model_id = models.getInt("model_id");

					ResultSet aligns = this.getDBConnection().getAlignsForModel(model_id);
					while (aligns.next()) {
						int alig_id = aligns.getInt("alignment_id");
						int fstart = aligns.getInt("start");
						int fend = aligns.getInt("end");
						int str = aligns.getInt("strand");
						float score = aligns.getFloat("score");
						int pos = str >= 0 ? fstart : fend;
						String group = groupmode.equals("location") ? chr+":"+fstart+","+fend+","+str : Integer.toString(alig_id);
						String cigar = aligns.getString("cigar");
						/* if there's no cigar line, create a fake single exon */

						if (cigar == null || cigar.length() == 0) {
							cigar = "M"+Integer.toString(fend-fstart+1);
						}

						/***
								cigar line for exon/intron structure should look like
								M 1000 D 200 M 500
								all spaces are optional, there should only be space (or no space)
								between the letter and the number
						***/

						Pattern p = Pattern.compile("([MDI])\\s*(\\d+)");
						Matcher m = p.matcher(cigar);
						int exonNumber = 1;

						while (m.find()) {
							String action = m.group(1);
							int bases = Integer.parseInt(m.group(2));

							if (action.equals("M")) {
								/* put in an exon */

								StrandedFeature.Template templ = new StrandedFeature.Template();
								templ.strand = str > 0 ? StrandedFeature.POSITIVE :
									str < 0 ? StrandedFeature.NEGATIVE : 
									StrandedFeature.UNKNOWN;

								templ.location = str >= 0 ? new RangeLocation(pos, pos + bases - 1) :
                          									new RangeLocation(pos - bases + 1, pos);
								templ.type = type;
								templ.source = aligns.getString("method");

								templ.annotation = new SmallAnnotation();
								templ.annotation.setProperty("id", models.getString("model_name"));
								templ.annotation.setProperty("exonNumber", exonNumber++);
								templ.annotation.setProperty("score", Float.toString(score));
								templ.annotation.setProperty("url", linkout+Integer.toString(model_id));
								templ.annotation.setProperty("group", group);
								templ.annotation.setProperty("summary", "click on the link to view annotations for this feature");
								vseq.createFeature(templ);

								pos += str >= 0 ? bases : -bases;
							} else if (action.equals("D")) {
								/* move along */
								pos += str >= 0 ? bases : -bases;
							} else if (action.equals("I")) {
								/* just ignore these */
							}
						}
					}
				}
        return vseq.getAddedFeatures();
			} catch (SQLException ex) {
				throw new DataSourceException(ex, "Error retrieving alignments from database " + dbname);
			} catch (BioException ex) {
				throw new DataSourceException(ex, "Error annotating sequence " + chr);
			} catch (ChangeVetoException ex) {
				throw new DataSourceException("ViewSequence isn't accepting features :(");
			}	    
    }


    public String getFeatureID(Feature f) {
        Annotation anno = f.getAnnotation();
        if (anno.containsProperty("id") && anno.containsProperty("exonNumber")) {
            Object idProp = anno.getProperty("id");
						Object exonNumberProp = anno.getProperty("exonNumber");

						return idProp.toString()+".exon."+exonNumberProp.toString();
        }
        
        return null;
    }

    public List getGroups(Feature f) {
        Annotation anno = f.getAnnotation();
        if (anno.containsProperty("group") && anno.containsProperty("id")) {
            Object groupProp = anno.getProperty("group");
            Object idProp = anno.getProperty("id");

						String notes = "";
						if (anno.containsProperty("score")) {
							Object scProp = anno.getProperty("score");
							notes = "Score: "+scProp.toString();
						}

						return Collections.singletonList(
							new DASGFFGroup(
								groupProp.toString(), // group id
								type,       // type
								idProp.toString()+" ("+groupProp.toString()+")", // label
								getLinkouts(f),    // linkouts
								Collections.singletonList(notes) // notes
                )
							);
        }
        return Collections.EMPTY_LIST;
		}
    
    public Set getAllTypes() {
        return Collections.unmodifiableSet(allTypes);
    }

	public Map getLinkouts(Feature f) {
		Map links = new HashMap();
		links.put("more details", getFirstFeatureAnnotation(f, "url"));
		return links;
	}
/*
    public Map getLinkouts(Feature f) {
        Map anno = f.getAnnotation().asMap();
        Map links = new HashMap();
        for (Iterator i = anno.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry me = (Map.Entry) i.next();
            String key = me.getKey().toString();
            if (key.startsWith("href:")) {
                Object o = me.getValue();
                if (o instanceof List) {
                    links.put(key.substring(5), ((List) o).get(0));
                } else {
                    links.put(key.substring(5), o);
                }
            }
        }
        
        return links;
    }
*/

    public String getScore(Feature f) {
        Annotation ann = f.getAnnotation();
        if (ann.containsProperty("score")) {
            return ann.getProperty("score").toString();
        } else {
            return null;
        }
    }

	public String getFirstFeatureAnnotation(Feature f, String atype) {
		Annotation anno = f.getAnnotation();
		if (anno.containsProperty(atype)) {
			Object idProp = anno.getProperty(atype);
			if (idProp instanceof List) {
				return ((List) idProp).get(0).toString();
			} else {
				return idProp.toString();
			}
		}
		return null;
	}

	public List getFeatureNotes(Feature f) {
		// in future possibly add second note with image HTML
		Annotation anno = f.getAnnotation();
		if (anno.containsProperty("summary")) {
			Object idProp = anno.getProperty("summary");
			if (idProp instanceof List) {
				return (List)idProp;
			} else {
        List result = new ArrayList<String>();
        result.add(idProp.toString());
				return result;
			}
		}
		return null;
	}


}
