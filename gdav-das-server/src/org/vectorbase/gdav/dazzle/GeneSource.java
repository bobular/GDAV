/*
 *   http://www.vectorbase.org
 */

/** temp compile

setenv CLASSPATH `perl -e 'print join ":", glob("/usr/local/tomcat/webapps/das/WEB-INF/lib/*.jar"), "/usr/local/tomcat/webapps/gdav/WEB-INF/classes"'`

javac org/vectorbase/gdav/dazzle/GeneSource.java

temp deploy

javac org/vectorbase/gdav/dazzle/GeneSource.java ; sudo cp -r org /usr/local/tomcat/webapps/das/WEB-INF/classes/

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

import org.biojava.bio.seq.ProteinTools;

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

public class GeneSource extends AbstractDataSource implements DazzleDataSource {
    
    private Set allTypes;

  	private String linkout;
  	private String excludeAnnotationSets;

  	private final String type = "BS:01032";

    private String dbhost;
    private String dbname;
    private String dbport;
    private String dbuser;
    private String dbpass;

    private String mapMaster;

  	private DBConnection dbCon;

    public String getDataSourceType() {
        return "alignment";
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

	  public void setLinkout(String s) {
        linkout = s;
    }

	  public void setExcludeAnnotationSets(String s) {
        excludeAnnotationSets = s;
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
			return null;
    }

 	  /* don't understand landmarks or mapmasters... not sure if this is OK... */
    public String getLandmarkVersion(String ref) 
	    throws DataSourceException, NoSuchElementException
    {
			return getVersion();
    }

    public FeatureHolder getFeatures(String gene_id)
	    throws NoSuchElementException, DataSourceException {

//System.err.println("trying model_name="+gene_id);

			FeatureHolder result = null;
			try {
				/* it's not really a protein sequence */
				result = ProteinTools.createProteinSequence("GENE", gene_id); 

				ResultSet modelRs = this.getDBConnection().getModelsWithNameLike(gene_id+"-%");

				while (modelRs.next()) {
					int modelID = modelRs.getInt("model_id");
					String modelName = modelRs.getString("model_name");

					ResultSet subRs = this.getDBConnection().getSubsForModel(modelID);
					while (subRs.next()) {
						int subID = subRs.getInt("submission_id");
						String subName = subRs.getString("description");

						if (excludeAnnotationSets == null || !Pattern.matches(excludeAnnotationSets, subName)) {
							Feature.Template template = new Feature.Template();
							template.type = type;
							template.location = new RangeLocation(0, 0);
							template.annotation = new SimpleAnnotation();

							LinkedHashMap<Integer,StringBuilder> annots = new LinkedHashMap<Integer,StringBuilder>();

							ResultSet annotRs = this.getDBConnection().getAnnoForModelAndSub(modelID, subID);
							while (annotRs.next()) {
								int rowID = annotRs.getInt("row_id");
								String key = annotRs.getString("title");
								String value = annotRs.getString("annotation_value");
								if (annots.get(rowID) == null) {
									annots.put(rowID, new StringBuilder());
								}
								annots.get(rowID).append(key + " = " + value + "; ");
							}
							
							for (int rowID : annots.keySet()) {
								template.annotation.setProperty("summary", annots.get(rowID).toString());
								template.annotation.setProperty("url", linkout+modelID);
								template.annotation.setProperty("experimentName", subName + "; " + modelName);
								result.createFeature(template);
							}
						}
					}
				}

			} catch(Exception ex) {
				//						System.err.println("exception: "+ex.toString());
			}
			return result;
		}

	  public String getFeatureID(Feature f) {
			return getFirstFeatureAnnotation(f, "experimentName");
		}

    public List getGroups(Feature f) {
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

    public String getScore(Feature f) {
        Annotation ann = f.getAnnotation();
        if (ann.containsProperty("score")) {
            return ann.getProperty("score").toString();
        } else {
            return null;
        }
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

	public String getTypeDescription(String type) {
		return "Experiment";
	}

}
