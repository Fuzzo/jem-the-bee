/**
    JEM, the BEE - Job Entry Manager, the Batch Execution Environment
    Copyright (C) 2012, 2013   Andrea "Stock" Stocchero
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.pepstock.jem.log.LogAppl;
import org.pepstock.jem.log.MessageException;
import org.pepstock.jem.node.configuration.ConfigKeys;
import org.pepstock.jem.node.configuration.ConfigurationException;
import org.pepstock.jem.node.sgm.DataPaths;
import org.pepstock.jem.node.sgm.DataSetPattern;
import org.pepstock.jem.node.sgm.DataSetRules;
import org.pepstock.jem.node.sgm.InvalidDatasetNameException;
import org.pepstock.jem.node.sgm.Path;
import org.pepstock.jem.node.sgm.PathsContainer;

import com.thoughtworks.xstream.XStream;

/**
 * Manages all data paths, set in configuration file, and all datasets rules defined for allocation and accessing.<br>
 * Some methods are visible because this object is serialized to job and you want to avoid that someone change the configuration 
 * 
 * @author Andrea "Stock" Stocchero
 * @version 2.1
 */
public final class DataPathsManager implements Serializable{
	
	/**
	 * Default file name for rules when not defined
	 */
	public static final String DEFAULT_RULES_FILE_NAME = "$jem_Default_Datasets_Rules.xml";
	
	/**
	 * Regular expression for ALL
	 */
	public static final String DEFAULT_REG_EX_FOR_ALL = ".*";
	
	private static final long serialVersionUID = 1L;
	
	private DataPaths dataPaths = null;

	private final Map<Pattern, PathsContainer> DATASET_RULES = new LinkedHashMap<Pattern, PathsContainer>();
	
	private File datasetRulesFile = null;
	
	private transient XStream xs = new XStream();

	/**
	 * It sets the XML definitions
	 */
    public DataPathsManager() {
		xs.alias(ConfigKeys.RULES_ALIAS, DataSetRules.class);
		xs.addImplicitCollection(DataSetRules.class, ConfigKeys.PATTERNS_ALIAS);
		xs.processAnnotations(DataSetPattern.class);
    }

	/**
	 * Loads the datapaths from JEM node configuration file 
	 * @param dataPaths the storageGroups to set
	 * @throws ConfigurationException if any error occurs
	 */
	void setDataPaths(DataPaths dataPaths) throws ConfigurationException {
		this.dataPaths = dataPaths;
		// checks is datapaths exists
		for (Path p: dataPaths.getPaths()){
			File dataPath = new File(p.getContent());
			if (!dataPath.exists()) {
				throw new ConfigurationException(NodeMessage.JEMC099E.toMessage().getFormattedMessage(dataPath));
			} else if (!dataPath.isDirectory()) {
				throw new ConfigurationException(NodeMessage.JEMC098E.toMessage().getFormattedMessage(dataPath));
			}
		}
	}

	/**
	 * Tests the file of datasets rules is correct
	 * @param fileDatasetRules file with datasets rules
	 * @return returns a dataset result
	 * @throws MessageException if any error occurs
	 * @throws FileNotFoundException if any IO error occurs
	 */
	public DatasetsRulesResult testRules(File fileDatasetRules) throws MessageException{
    	DatasetsRulesResult rules = null;
    	DataSetRules dsr;
		try {
			// parses from XML
			dsr = loadXMLDataSetRules(fileDatasetRules);
			// load using object 
			rules = loadRules(dsr, false);
			// if rules are empty, no rules and then exception
			if (rules.getRules().isEmpty()){
				throw new MessageException(NodeMessage.JEMC254E);
			}
		} catch (Exception e) {
			throw new MessageException(NodeMessage.JEMC257E, fileDatasetRules.getAbsolutePath());
		}
		return rules;
	}
	/**
	 * Loads rules from a file
	 * @param fileDatasetRules file with datasets rules
	 * @throws MessageException if any error occurs
	 * @throws FileNotFoundException if any IO error occurs
	 */
	void loadRules(File fileDatasetRules) throws MessageException{
		this.datasetRulesFile = fileDatasetRules;
    	DataSetRules dsr;
		try {
			// parses from XML
			dsr = loadXMLDataSetRules(fileDatasetRules);
			// load using object 
			DatasetsRulesResult rules = loadRules(dsr, true);
			// if rules are empty, no rules and then exception
			if (rules.getRules().isEmpty()){
				throw new MessageException(NodeMessage.JEMC254E);
			} else {
				// sets new rules only if the parsing is correct
				DATASET_RULES.clear();
				DATASET_RULES.putAll(rules.getRules());
			}
		} catch (Exception e) {
			throw new MessageException(NodeMessage.JEMC257E, fileDatasetRules.getAbsolutePath());
		}
    	LogAppl.getInstance().emit(NodeMessage.JEMC252I, DATASET_RULES.size());
    	
    }
    /**
     * Lodas rules from XML file
     * @param fileDatasetRules file with rules
     * @return datasets rules object
     * @throws Exception if any exception occurs
     */
    private DataSetRules loadXMLDataSetRules(File fileDatasetRules) throws Exception{
		FileInputStream fis = new FileInputStream(fileDatasetRules);
		try {
			return (DataSetRules) xs.fromXML(fis);
		} finally {
			try {
				fis.close();
			} catch (Exception e) {
				LogAppl.getInstance().ignore(e.getMessage(), e);
			}
		}
    }
    
    /**
     * Saves the default rules file, only if data path is 1 and no rule is defined
     * @param fileDatasetRules file where stores rules
     * @throws Exception if any excpetion occurs
     */
    void saveXMLDataSetRules(File fileDatasetRules){
    	// creates object with datarules
		DataSetRules rules = new DataSetRules();
		DataSetPattern pattern = new DataSetPattern();
		pattern.setPathName(Main.DATA_PATHS_MANAGER.getDataPathsNames().get(0));
		// puts ALL as default rule
		pattern.getPatterns().add(DEFAULT_REG_EX_FOR_ALL);
		rules.getPatterns().add(pattern);

		// saves rules
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(fileDatasetRules);
			xs.toXML(rules, fos);
		} catch (FileNotFoundException e) {
			LogAppl.getInstance().ignore(e.getMessage(), e);
		} finally {
			try {
				if (fos != null){
					fos.close();
				}
			} catch (Exception e) {
				LogAppl.getInstance().ignore(e.getMessage(), e);
			}
		}
    }

    /**
     * Loads all rules, relating them to data path
     * @param dsr object with all datasets rules definition
     * @return the result of parsing
     */
    private DatasetsRulesResult loadRules(DataSetRules dsr, boolean printWarnings){
    	// initializes the result with the right container
    	DatasetsRulesResult result = new DatasetsRulesResult();
    	Map<Pattern, PathsContainer> newRules = new LinkedHashMap<Pattern, PathsContainer>();
    	List<String> warnings = new ArrayList<String>();
    	result.setRules(newRules);
    	result.setWarnings(warnings);
    	
    	// scans all rules
		for (DataSetPattern dsPattern : dsr.getPatterns()){
			for (String pattern : dsPattern.getPatterns()){
				// creates patterns
				Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
				// gets teh data path name
				// if is not defined, creates a warning
				String name = dsPattern.getPathName();
				if (name != null){
					Path mp = getPath(name);
					if (mp != null){
						// gets path definition
						// creating a paths cont
						PathsContainer mps = new PathsContainer();
						mps.setCurrent(mp);
						// checks if old is defined
						if (dsPattern.getOldPathName() != null && dsPattern.getOldPathName().trim().length() > 0){
							// checks if exists teh old path
							Path old = getPath(dsPattern.getOldPathName());
							mps.setOld(old);
							if (old == null){
								addWarningMessage(warnings, printWarnings, NodeMessage.JEMC255W, dsPattern.getOldPathName());	
							}
						}
						// adds rules
						newRules.put(p, mps);
					} else {
						addWarningMessage(warnings, printWarnings, NodeMessage.JEMC255W, name);	
					}
				} else {
					addWarningMessage(warnings, printWarnings, NodeMessage.JEMC256W, null);	
				}
			}
		}
		return result;
    }

    /**
     * Adds warning message to a list
     * @param warnings collection of warnings
     * @param printWarnings if <code>true</code>, puts message to log otherwise add to the collection
     * @param message message to display
     * @param name parameter to use to foramt the message
     */
    private void addWarningMessage(List<String> warnings, boolean printWarnings, NodeMessage message, String name){
    	// if prints, use log
		if (printWarnings){
			LogAppl.getInstance().emit(message, name);
		// if warnings collection already has got the message, skip it	
		} else if (!warnings.contains(message)){
			String outputMessage = (name == null) ? message.toMessage().getMessage() : message.toMessage().getFormattedMessage(name);
			warnings.add(outputMessage);
		}
    }
    
	/**
	 * Gets the configured data paths by its logical name  
	 * @param name logical name of data path
	 * @return defined path or null
	 */
    private Path getPath(String name){
    	for (Path mp : dataPaths.getPaths()){
    		if (name.equalsIgnoreCase(mp.getName())){
    			return mp;
    		}
    	}
    	return null;
    }

	/**
	 * @return the datasetRulesFile
	 */
	public File getDatasetRulesFile() {
		return datasetRulesFile;
	}

	/**
     * Gets the path container checking the rules pattern with file name of dataset
     * @param fileName file name of dataset
     * @return path container
	 * @throws InvalidDatasetNameException if file name doesn't match with defined rules
     */
    public PathsContainer getPaths(String fileName) throws InvalidDatasetNameException{
    	if (fileName != null){
    		for (Entry<Pattern, PathsContainer> entry : DATASET_RULES.entrySet()){
    			if (entry.getKey().matcher(fileName).matches()){
    				return entry.getValue();
    			}
    		}
    	}
    	throw new InvalidDatasetNameException(fileName);
    }
    
    /**
     * Returns the absolute path passing data path name argument
     * @param name data path argument
     * @return absolute path
     */
    public String getAbsoluteDataPathByName(String name){
    	Path path = getPath(name);
    	return path == null ? null : path.getContent();
    }
    
    /**
     * Returns the name of data path to use for file name argument
     * @param fileName file name to use to get data path name
     * @return the data path name
     */
    public String getAbsoluteDataPathName(String fileName){
    	return retrieveDataPath(fileName, true);
    }
    
    /**
     * Returns the absolute data path to use for file name argument
     * @param fileName file name to use to get the absolute data path
     * @return the the absolute data path
     */
    public String getAbsoluteDataPath(String fileName){
    	return retrieveDataPath(fileName, false);
    }
    
    /**
     * Returns the absolute data path or data path name using file name argument
     * @param fileName file name to use to get info
     * @param names if <code>true</code>, get data path name otherwise absolute data path  
     * @return if names is set to <code>true</code>, get data path name otherwise absolute data path 
     */
    private String retrieveDataPath(String fileName, boolean name){
    	String file = FilenameUtils.normalize(fileName.endsWith(File.separator) ? fileName : fileName+File.separator, true);
    	for (Path mp : dataPaths.getPaths()){
    		String pathToCheck = FilenameUtils.normalize(mp.getContent().endsWith(File.separator) ? mp.getContent() : mp.getContent()+File.separator, true);
    		if (StringUtils.startsWithIgnoreCase(file, pathToCheck)){
    			return name ? mp.getName() : mp.getContent();
    		}
    	}
    	return null;
    }
    
    /**
     * Returns a list of string with data path name defined in JEM configuration file
     * @return a list of string with data path name defined in JEM configuration file
     */
    public List<String> getDataPathsNames(){
    	return loadDataPaths(true);
    }
    
    /**
     * Returns a list of string with complete data path defined in JEM configuration file
     * @return a list of string with complete data path defined in JEM configuration file
     */
    public List<String> getDataPaths(){
    	return loadDataPaths(false);
    }
    
    /**
     * Loads data path information on a list
     * @param names if <code>true</code>, loads data path names otherwise absolute data paths 
     * @return if names is set to <code>true</code>, loads data path names otherwise absolute data paths 
     */
    private List<String> loadDataPaths(boolean names){
    	List<String> groups = new LinkedList<String>();
    	for (Path mp : dataPaths.getPaths()){
    		groups.add(names ? mp.getName() : FilenameUtils.normalize(mp.getContent(), true));
    	}
    	return groups;	
    }
}