/*
 * Copyright (c) 2006-, IPD Boehm, Universitaet Karlsruhe (TH) / KIT, by Guido Sautter
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Universität Karlsruhe (TH) nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY UNIVERSITÄT KARLSRUHE (TH) / KIT AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.uka.ipd.idaho.plugins.bibRefs;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import de.uka.ipd.idaho.gamta.util.AnalyzerDataProvider;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.gamta.util.GamtaClassLoader;
import de.uka.ipd.idaho.gamta.util.GamtaClassLoader.ComponentInitializer;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefUtils.RefData;

/**
 * Abstract source of bibliographic reference meta data, mostly for wrapping
 * online data sources. Non-abstract sub classes should provide a no-argument
 * constructor passing a fixed name to super(). However, this is only a
 * suggestion, as there might be situations that necessitate a different
 * approach, e.g. loading the name form a config file. In this case, the
 * argument to super() should be a dummy value or simply null. However, the name
 * has to have its final value after the init() method returns.
 * 
 * @author sautter
 */
public abstract class BibRefDataSource implements BibRefConstants {
	
	/** the name of the data source */
	protected String name;
	
	/** the nice name of the data source, for display purposes */
	protected String label;
	
	/**
	 * the folder there the data source can store its data, e.g. configuration
	 * files to access the backing source
	 */
	protected AnalyzerDataProvider dataProvider;
	
	/** Constructor
	 * @param name the name of the data source
	 */
	public BibRefDataSource(String name) {
		this.name = name;
	}
	
	/**
	 * Make the data source know where its data is located
	 * @param adp the data folder for the data source
	 */
	public void setDataProvider(AnalyzerDataProvider adp) {
		this.dataProvider = adp;
	}
	
	/**
	 * Initialize the data source. This method is invoced after the data
	 * provider is set. It is meant to read configuration files, etc. This
	 * default implementation does nothing, sub classes are welcome to overwrite
	 * it as needed.
	 */
	public void init() {}
	
	/**
	 * Retrieve the name of the data source. The string returned by this method
	 * must not contain whitespace; best if it is strictly alphanumeric. Any
	 * reference data set returned by the getRefData() or findRefData() methods
	 * should include its source specific identifier, using the name of the data
	 * sources as the type.
	 * @return the name of the data source
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * Retrieve a nice name of the data source, for display purposes. If the
	 * label field is null, this method defaults to the name.
	 * @return a nive name for the data source
	 */
	public String getLabel() {
		return ((this.label == null) ? this.name : this.label);
	}
	
	/**
	 * Check a document ID if it matches the ID syntax of the backing source.
	 * This method serves as a filter to reduce lookups to the backing source.
	 * Its default implementation simply returns true. Sub classes are welcome
	 * to overwrite it as needed. Filters should be as restrictive as possible.
	 * @param docId the document ID to check
	 * @return true if the speciefied ID might come from the backing source,
	 *         judging by syntax only
	 */
	public boolean isSuitableID(String docId) {
		return true;
	}
	
	/**
	 * Retrieve a meta data set from the source backing the data source, using
	 * the source specific document ID as the access key. If the backing data
	 * source does not provide a meta data set for the document with the
	 * specified ID, this method should return null. If the meta data is
	 * incomplete (as per check by BibRefUtils.classify() or
	 * BibRefTypeSystem.classify() returning null because the data does not
	 * match any available reference type), this should not bother the data
	 * source. Completing the meta data sets might require user input and is
	 * therefore handled elsewhere.
	 * @param sourceDocId the source specific ID of the document to retrieve the
	 *            meta data for
	 * @return the meta data of the document with the specified ID
	 * @throws IOException
	 */
	public abstract RefData getRefData(String sourceDocId) throws IOException;
	
	/**
	 * Find the meta data sets in the source backing the data source, using
	 * known search attributes the access key, e.g. the document author, (parts
	 * of) the title, the name of the journal the document appeared in, or the
	 * name or location of the publisher who issued the document. If multiple
	 * meta data sets match, this method should return them all. Only if the
	 * search criteria are empty, this method may ignore them and return either
	 * of null or an empty array. If the backing data source does not provide
	 * any meta data sets that match the search criteria, this method may return
	 * null or an empty array. If some of the meta data sets are incomplete (as
	 * per check by BibRefUtils.classify() or BibRefTypeSystem.classify()
	 * returning null because the data does not match any available reference
	 * type), this should not bother the data source. Completing the meta data
	 * sets might require user input and is therefore handled elsewhere.
	 * @param searchData the known elements of the meta data to retrieve in full
	 * @return the meta data sets matching the specified criteria
	 * @throws IOException
	 */
	public abstract RefData[] findRefData(Properties searchData) throws IOException;
	
	/**
	 * Load the instances of this class located in the JAR files in a given
	 * folder. The data sources loaded from the JARs get their data folders set
	 * to a sub folder of the argument one, named '&lt;jarNameLessExt&gt;Data',
	 * where '&lt;jarNameLessExt&gt;' is the name of the JAR a given data
	 * source is loaded from less the '.jar' file extension. After setting the
	 * data provider, the data sources are initialized.
	 * @param rootFolder the folder to load the data sources from
	 * @return an array holding the data sources
	 */
	public static BibRefDataSource[] loadDataSources(final File rootFolder) {
		Object[] brdsObjs = GamtaClassLoader.loadComponents(rootFolder, BibRefDataSource.class, new ComponentInitializer() {
			public void initialize(Object component, String componentJarName) throws Throwable {
				if (componentJarName.toLowerCase().endsWith(".jar"))
					componentJarName = componentJarName.substring(0, (componentJarName.length() - ".jar".length()));
				BibRefDataSource brds = ((BibRefDataSource) component);
				brds.setDataProvider(new AnalyzerDataProviderFileBased(new File(rootFolder, (componentJarName + "Data"))));
				brds.init();
			}
		});
		BibRefDataSource[] brdss = new BibRefDataSource[brdsObjs.length];
		for (int s = 0; s < brdsObjs.length; s++)
			brdss[s] = ((BibRefDataSource) brdsObjs[s]);
		return brdss;
	}
}