/**
    JEM, the BEE - Job Entry Manager, the Batch Execution Environment
    Copyright (C) 2012-2015   Andrea "Stock" Stocchero
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.pepstock.jem.node;

import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.pepstock.jem.Jcl;
import org.pepstock.jem.PreJob;
import org.pepstock.jem.commands.JemURLStreamHandler;
import org.pepstock.jem.commands.JemURLStreamHandlerFactory;
import org.pepstock.jem.factories.JclFactory;
import org.pepstock.jem.factories.JclFactoryException;
import org.pepstock.jem.factories.JemFactory;
import org.pepstock.jem.log.LogAppl;

/**
 * Creates a new JCL object to load into PreJob object (using static methods)
 * 
 * @author Andrea "Stock" Stocchero
 * @version 1.0
 */
public class Factory {

	/**
	 * To avoid any instantiation
	 */
	private Factory() {
		
	}

	/**
	 * Checks if there's a factory loaded with type, specified submitting the
	 * job. If not, an exception will throw. Then it calls the
	 * <code>createJcl</code> method of JemFactory and load Prejob with new JCL
	 * object.
	 * 
	 * @see org.pepstock.jem.PreJob#getJclType()
	 * @see org.pepstock.jem.factories.JemFactory#createJcl(String)
	 * @param prejob Prejob object instance, used to load JCL object
	 * @throws JclFactoryException if the factory is not found or the factory
	 *             has an exception creating and validating the JCL source
	 */
	public static void loadJob(PreJob prejob) throws JclFactoryException {
		// checks and load (if necessary)
		// the JCL content from GFS
		// if JEM URL has been set
		loadJclFromURL(prejob);
		Jcl jcl = null;
		// prejob without type
		if (prejob.getJclType() == null){
			jcl = Factory.scanAllJclFactories(prejob);
		} else if (!Main.FACTORIES_LIST.containsKey(prejob.getJclType().toLowerCase())) {
			// JCL type is normalized using lower case
			throw new JclFactoryException(NodeMessage.JEMC143E.toMessage().getFormattedMessage(prejob.getJclType().toLowerCase()));
		} else {
			// get factory from map, loaded during startup of node
			JemFactory factory = Main.FACTORIES_LIST.get(prejob.getJclType().toLowerCase());
			// creates JCL object using the factory
			jcl = Factory.createJcl(prejob.getJclContent(), prejob.getJob().getInputArguments(), factory);
		}
		// sets JCL type
		jcl.setType(prejob.getJclType().toLowerCase());
		// sets JCL to JOB object, inside of PreJob container
		prejob.getJob().setJcl(jcl);
		prejob.getJob().setName(jcl.getJobName());
	}

	/**
	 * Creates a new JCL object by JCL factory, previously loaded
	 * 
	 * @see org.pepstock.jem.factories.JclFactory#createJcl(String)
	 * @param content JCL source code string
	 * @param factory JCLFactory
	 * @return JCL object
	 * @throws JclFactoryException the factory has an exception creating and
	 *             validating the JCL source
	 */
	private static Jcl createJcl(String content, List<String> inputArguments, JclFactory factory) throws JclFactoryException {
		try {
			return factory.createJcl(content, inputArguments);
		} catch (Exception e) {
			// it catches here if
			// there is any error related to reflection
			throw new JclFactoryException(e.getMessage(), e);
		}
	}
	
	/**
	 * Scans all factories because the PreJob ahs got JclType set to null.
	 * @param prejob prejob to check
	 * @return JCL instance
	 * @throws JclFactoryException if no factory is able to create the JCL
	 */
	private static Jcl scanAllJclFactories(PreJob prejob) throws JclFactoryException{
		for (JemFactory factory : Main.FACTORIES_LIST.values()){
			try {
				Jcl jcl =  Factory.createJcl(prejob.getJclContent(), prejob.getJob().getInputArguments(), factory);
				prejob.setJclType(factory.getType());
				return jcl;
				// Exception class must be caught 
			} catch (Exception e) {
            	// debug
            	LogAppl.getInstance().debug(e.getMessage(), e);
			}
		}
		throw new JclFactoryException(NodeMessage.JEMC143E.toMessage().getFormattedMessage("null"));
	}
	
	/**
	 * Checks if who submitted the job used the JEM URL, to get the content
	 * from JEM GFS.
	 * 
	 * @param prejob prejob to check
	 * @throws JclFactoryException if the URL is malformed
	 */
	private static void loadJclFromURL(PreJob prejob) throws JclFactoryException{
		// only URL is not null and url is JEM URL
		if (prejob.getUrl() != null && prejob.getUrl().startsWith(JemURLStreamHandlerFactory.PROTOCOL)){
			try {
				// gets URL using the JEM URL Stream handler
				URL url = new URL(prejob.getUrl());
				// reads the content from GFS
				StringWriter sw = new StringWriter();
				// open Stream using JEM URL Stream handler
				IOUtils.copy(url.openStream(), sw);
				// sets content to pre job
				prejob.setJclContent(sw.getBuffer().toString());
			} catch (MalformedURLException e) {
				throw new JclFactoryException(prejob.getUrl(), e);
			} catch (IOException e) {
				throw new JclFactoryException(prejob.getUrl(), e);
			}
		}
	}
	
	/**
	 * Gets the GFS tag from JEM URL, if there is
	 * @param prejob job to submit
	 * @return the GFS tag from JEM URL otherwise null.
	 */
	static String getGfsFromURL(PreJob prejob){
		// only URL is not null and url is JEM URL
		if (prejob.getUrl() != null && prejob.getUrl().startsWith(JemURLStreamHandlerFactory.PROTOCOL)){
			return StringUtils.substringBetween(prejob.getUrl(), JemURLStreamHandler.SEMICOLONS);
		}
		return null;
	}
	
}