/*
 *                    BioJava development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the individual
 * authors.  These should be listed in @author doc comments.
 *
 * For more information on the BioJava project and its aims,
 * or to join the biojava-l mailing list, visit the home page
 * at:
 *
 *      http://www.biojava.org/
 *
 */

package org.biojava.servlets.dazzle.datasource;

import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.io.*;
import org.biojava.bio.seq.*;
import org.biojava.utils.xml.*;

/**
 * Interface for plugins which provide data and formatting behaviour
 * for DazzleServer.  Basic implementations of DazzleDataSource
 * provide annotation server functionality.  For reference servers,
 * use the sub-interface, DazzleReferenceSource.
 *
 * <h3>Life-cycle</h3>
 *
 * <p>
 * Dazzle may load and unload plugin objects at any time.  Standard
 * Dazzle installations load plugins based on <code>datasource</code>
 * elements in the <code>dazzlecfg.xml</code> file.
 * </p>
 *
 * </p>
 * In addition to implementing this interface, a typical plugin
 * class will have several Javabeans-style setFoo(x) methods for
 * setting plugin properties.  These methods are called to
 * initialize the properties specified in <code>dazzlecfg.xml</code>.
 * See the <a href="http://www.biojava.org/dazzle/deploy.html">deployment
 * guide</a> for a list of common properties.
 * </p>
 *
 * <p>
 * Once the class has been loaded and all the properties set, the
 * Dazzle servlet will call the <code>init</code> method.  Only once
 * this has returned sucessfully is the plugin considered to be
 * `in service'.  At this point, any other method may be called.
 * </p>
 *
 * <h3>Writing simple annotation sources</h3>
 *
 * <p>
 * In principle, a DAS annotation source just provides a set of
 * features annotating a given landmark.  In addition, you must
 * also provide:
 * <ul>
 * <li>Landmark versions, so that clients can detect version skew
 *     between reference and annotation servers.</li>
 * <li>Sequence lengths, allowing the server core to validate
 *     requests.</li>
 * </ul>
 * Ideally, this information would only be needed for landmark
 * sequences which this data source actually annotates.  In practice,
 * the DAS 1.0 specification requires that this information be
 * available for <em>all</em> landmarks specified by a given
 * reference server.  One way to get this data is to create
 * a <code>DASSequenceDB</code> and fetch the information directly
 * from the reference server.  See <code>GFFAnnotationSource</code>
 * for an example of this.
 * </p>
 *
 * <p>
 * When implementing an annotation source, you need to provide
 * a <code>FeatureHolder</code> containing just those extra
 * features which you want to add to a sequence.  However, BioJava
 * doesn't allow construction of features without an associated
 * sequence.  The easiest approach here is to use the BioJava
 * <code>ViewSequence</code> class, e.g.:
 *
 * <pre>
 * Sequence seq = referenceDB.get(seqID);
 * ViewSequence vseq = new ViewSequence(seq);
 * vseq.createFeature(myFeatureTemplate);
 * // add more features...
 * return vseq.getAddedFeatures();;
 * </pre>
 * 
 *
 * @author Thomas Down
 * @version 1.00
 */

public interface DazzleDataSource {
    /**
     * Initialize this datasource, in a given context.
     * You may wish to record the <code>context</code> parameter,
     * for logging or resource access.
     *
     * @param context the ServletContext in which this Dazzle installation is running
     * @throws DataSourceException if for any reason the plugin cannot enter service.
     */

    public void init(ServletContext context)
        throws DataSourceException;
        
    /**
     * Called prior to taking a datasource out of service
     */
     
    public void destroy();
        
    /**
     * Return a type for this data source.  For example,  <code>gff</code> or
     * <code>ensembl-ref</code>.  If this is not specified, the class name is
     * used.
     */
     
    public String getDataSourceType();
    
    /**
     * Return a version string for this datasource
     */
     
    public String getDataSourceVersion();

    /**
     * Return the `Map master' URL of the reference server to be annotated.  Note
     * that this is ignored for reference servers.
     */

    public String getMapMaster();

    /**
     * Return the display name of this data source.  This may be
     * shown by clients, but is not otherwise significant in the
     * DAS protocol.
     */

    public String getName();

    /**
     * Return the description of this data source.  A
     * <code>null</code> value means no description.  This may be
     * shown by clients, but is not otherwise significant in the DAS
     * protocol.  
     */

    public String getDescription();

    /**
     * Return the current version of this data represented by this
     * source.  This may be shown by clients, but is not otherwise
     * significant in the DAS protocol.  
     */

    public String getVersion();

    /**
     * Get the complete set of features for a given sequence landmark.
     * For large datasources, this might often be a `lazy fetch'
     * implementation of <code>FeatureHolder</code>
     *
     * @param ref the ID of a landmark sequence
     * @return A <code>FeatureHolder</code> containing all features on that
     *          landmark.  This might be filtered by Dazzle.
     * @throws DataSourceException if an error occurs accessing underlying
     *                             storage, or if the plugin hasn't be
     *                             correctly configured.
     * @throws NoSuchElementException if no landmark of the specified name
     *                                can be found.
     */

    public FeatureHolder getFeatures(String ref, int start, int end)
        throws DataSourceException, NoSuchElementException;

    public FeatureHolder getFeatures(String ref)
        throws DataSourceException, NoSuchElementException;


    /**
     * Get a version for a given landmark.  Note that this method
     * must return a valid version for every sequence landmark from a
     * given reference server -- even landmarks which are not
     * annotated.
     *
     * @param ref the ID of a landmark sequence
     * @return The annotated version of this landmark
     * @throws DataSourceException if an error occurs accessing underlying
     *                             storage, or if the plugin hasn't be
     *                             correctly configured.
     * @throws NoSuchElementException if no landmark of the specified name
     *                                can be found.
     */

    public String getLandmarkVersion(String ref)
        throws DataSourceException, NoSuchElementException;

    /**
     * Get the length of a landmark sequence.  Note that this method
     * must return a valid value for every sequence landmark from a
     * given reference server -- even landmarks which are not
     * annotated.
     *
     * @param ref the ID of a landmark sequence
     * @return The length of this sequence
     * @throws DataSourceException if an error occurs accessing underlying
     *                             storage, or if the plugin hasn't be
     *                             correctly configured.
     * @throws NoSuchElementException if no landmark of the specified name
     *                                can be found.
     */

    public int getLandmarkLength(String ref)
        throws DataSourceException, NoSuchElementException;

    //
    // Types system
    //

    /**
     * Return the set of all type IDs served by this datasource.  This is used
     * by the no-args version of the <code>types</code> DAS command.
     */

    public Set getAllTypes();

    /**
     * Return a description string for a given type.  May be null (indicating
     * that the description is the same as the type ID).
     */

    public String getTypeDescription(String type);

    //
    // Stuff to help write correct information about features
    //

    /**
     * Return an ID string for a feature.  This /may/ be <code>null</code>, but
     * the resulting datasource may then not comply perfectly with DAS 1.0.
     */

    public String getFeatureID(Feature f);
    
    /**
     * Return a display label for a feature.
     */
     
    public String getFeatureLabel(Feature f);
    
    /**
     * Get the NOTE string to use for the specified feature.  If no notes
     * are applicable, return an empty list.
     */
    
    public List getFeatureNotes(Feature f);
    
    /**
     * Get the phase for a feature
     */
     
    public FramedFeature.ReadingFrame getPhase(Feature f);
    
    /**
     * Return a score representation for this feature, or
     * <code>null</code> if not applicable.
     *
     * @since 0.97
     */

    public String getScore(Feature f);

    /**
     * Find the features matching a given ID
     */
     
    public FeatureHolder getFeaturesByID(String id, MatchType matchType)
        throws DataSourceException;
        
    /**
     * Find the features matching a given group ID
     */
   
    public FeatureHolder getFeaturesByGroup(String id, MatchType matchType)
        throws DataSourceException;
    
    //
    // Stylesheet support.
    //

    /**
     * Return a path to an XML stylesheet document, relative to the
     * the top-level of the current webapp's directory structure.  Note
     * that so long as the file pointed to by this string exists, it
     * is transmitted to the client without any further checcking, so
     * use with care.
     *
     * @return a path to an XML DAS stylesheet document.
     */

    public String getStylesheet();

    //
    // Embedded links
    //

    /**
     * Return a set of links out from a feature.  This is a map
     * which can contain zero or more role -> url mappings.
     *
     * @param f A feature, which will have been obtained from this datasource
     * @return a <code>Map</code> of link URLs, or the empty Map.
     * @since 0.93
     */

    public Map getLinkouts(Feature f);

    //
    // LINK command support
    //

    /**
     * Handle a DAS link command.  The plugin can send any data it wishes
     * in response to this command (including HTTP redirects).  If
     * DazzleServlet catches a DataSourceException or a NoSuchElementException
     * it will generate standard error pages.  If the method returns <code>false</code>
     * a pre-canned `No extra information' page will be returned.  Otherwise, it is
     * assumed that a response has been generated.
     *
     * @param req The servlet request
     * @param resp The servlet response, to which any data can be written
     * @param field The type of entity that is being linked to.
     * @param id The identifier of the entity linked to.
     *
     * @return <code>true</code> if a response has been generated.
     */

    public boolean doLink(HttpServletRequest req,
		       HttpServletResponse resp,
		       String field,
		       String id)
	throws ServletException, IOException, DataSourceException, NoSuchElementException;

    /**
     * Write out additional `detail' elements in the XFF features dump.
     */

    public void writeXFFDetails(XMLWriter xw, Feature f)
        throws IOException;

    public final static int COUNT_CALCULATE = -1;
    public final static int COUNT_DONTRETURN = -2;

    /**
     * Optional optimization of the counting of features in a given region.
     * Can return <code>COUNT_CALCULATE</code>, <code>COUNT_DONTRETURN</code>,
     * or a positive integer count.
     *
     * @since 0.94
     */

    public int countFeatures(String reference,
			     String type)
	throws DataSourceException, NoSuchElementException;

    /**
     * Optional optimization of the counting of features in a given region.
     * Can return <code>COUNT_CALCULATE</code>, <code>COUNT_DONTRETURN</code>,
     * or a positive integer count.
     *
     * @since 0.94
     */

    public int countFeatures(String reference,
			     int start,
			     int end,
			     String type)
        throws DataSourceException, NoSuchElementException;	

        
    /**
     * Return a list of DASGFFGroup objects of which this feature is a member.
     * This replaces the old getSpoofedGroups method.
     *
     * @since 1.0
     */
        
    public List getGroups(Feature f);

    /**
     * Determine if a non-contiguous feature should be shattered into multiple blocks.
     * When the DASGFF generator finds a feature who's BioJava location is not
     * contiguous, it calls this method.  If it returns true, the BioJava feature
     * is shattered into multiple DAS features, one for each contiguous span
     * of the location.
     */
    
    public boolean getShatterFeature(Feature f);
    
    public static final class MatchType {
        private String name;
        
        private MatchType(String name) {
            this.name = name;
        }
        
        public String toString() {
            return "MatchType(" + name + ")";
        }
    }
    
    public final static MatchType MATCH_EXACT = new MatchType("exact");
    public final static MatchType MATCH_PARTIAL = new MatchType("partial");
}


