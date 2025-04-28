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
 *     * Neither the name of the Universitaet Karlsruhe (TH) / KIT nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY UNIVERSITAET KARLSRUHE (TH) / KIT AND CONTRIBUTORS 
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
package de.uka.ipd.idaho.plugins.taxonomicNames.col;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import de.uka.ipd.idaho.easyIO.util.HashUtils;
import de.uka.ipd.idaho.easyIO.util.HashUtils.MD5;
import de.uka.ipd.idaho.easyIO.util.JsonParser;
import de.uka.ipd.idaho.easyIO.utilities.TinyHttpServer;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProvider;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.gamta.util.CountingSet;
import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNodeAttributeSet;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.StandardGrammar;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicNameConstants;
import de.uka.ipd.idaho.plugins.taxonomicNames.col.CatalogOfLifeLocal.IndexEntry;
import de.uka.ipd.idaho.plugins.taxonomicNames.col.CatalogOfLifeLocal.TaxonRecord;

/**
 * @author sautter
 */
public class CatalogOfLifeDataTool implements TaxonomicNameConstants {
	
	private static TreeMap ranksToNormalForms = new TreeMap(String.CASE_INSENSITIVE_ORDER);
	static {
		ranksToNormalForms.put(KINGDOM_ATTRIBUTE, KINGDOM_ATTRIBUTE);
		ranksToNormalForms.put(SUBKINGDOM_ATTRIBUTE, SUBKINGDOM_ATTRIBUTE);
		
		ranksToNormalForms.put(PHYLUM_ATTRIBUTE, PHYLUM_ATTRIBUTE);
		ranksToNormalForms.put(SUBPHYLUM_ATTRIBUTE, SUBPHYLUM_ATTRIBUTE);
		ranksToNormalForms.put(INFRAPHYLUM_ATTRIBUTE, INFRAPHYLUM_ATTRIBUTE);
		ranksToNormalForms.put("parvPhylum", "parvPhylum");
		
		ranksToNormalForms.put(SUPERCLASS_ATTRIBUTE, SUPERCLASS_ATTRIBUTE);
		ranksToNormalForms.put(CLASS_ATTRIBUTE, CLASS_ATTRIBUTE);
		ranksToNormalForms.put(SUBCLASS_ATTRIBUTE, SUBCLASS_ATTRIBUTE);
		ranksToNormalForms.put(INFRACLASS_ATTRIBUTE, INFRACLASS_ATTRIBUTE);
		ranksToNormalForms.put("parvClass", "parvClass");
		
		ranksToNormalForms.put(SUPERORDER_ATTRIBUTE, SUPERORDER_ATTRIBUTE);
		ranksToNormalForms.put(ORDER_ATTRIBUTE, ORDER_ATTRIBUTE);
		ranksToNormalForms.put(SUBORDER_ATTRIBUTE, SUBORDER_ATTRIBUTE);
		ranksToNormalForms.put(INFRAORDER_ATTRIBUTE, INFRAORDER_ATTRIBUTE);
		ranksToNormalForms.put("parvOrder", "parvOrder");
		
		ranksToNormalForms.put(SUPERFAMILY_ATTRIBUTE, SUPERFAMILY_ATTRIBUTE);
		ranksToNormalForms.put("epiFamily", "epiFamily");
		ranksToNormalForms.put(FAMILY_ATTRIBUTE, FAMILY_ATTRIBUTE);
		ranksToNormalForms.put(SUBFAMILY_ATTRIBUTE, SUBFAMILY_ATTRIBUTE);
		
		ranksToNormalForms.put(SUPERTRIBE_ATTRIBUTE, SUPERTRIBE_ATTRIBUTE);
		ranksToNormalForms.put(TRIBE_ATTRIBUTE, TRIBE_ATTRIBUTE);
		ranksToNormalForms.put(SUBTRIBE_ATTRIBUTE, SUBTRIBE_ATTRIBUTE);
		ranksToNormalForms.put(INFRATRIBE_ATTRIBUTE, INFRATRIBE_ATTRIBUTE);
		
		ranksToNormalForms.put(GENUS_ATTRIBUTE, GENUS_ATTRIBUTE);
		ranksToNormalForms.put(SUBGENUS_ATTRIBUTE, SUBGENUS_ATTRIBUTE);
		ranksToNormalForms.put(SECTION_ATTRIBUTE, SECTION_ATTRIBUTE);
		ranksToNormalForms.put(SUBSECTION_ATTRIBUTE, SUBSECTION_ATTRIBUTE);
		ranksToNormalForms.put(SERIES_ATTRIBUTE, SERIES_ATTRIBUTE);
		ranksToNormalForms.put(SUBSERIES_ATTRIBUTE, SUBSERIES_ATTRIBUTE);
		
		ranksToNormalForms.put(SPECIES_ATTRIBUTE, SPECIES_ATTRIBUTE);
		ranksToNormalForms.put(SUBSPECIES_ATTRIBUTE, SUBSPECIES_ATTRIBUTE);
		ranksToNormalForms.put(VARIETY_ATTRIBUTE, VARIETY_ATTRIBUTE);
		ranksToNormalForms.put(SUBVARIETY_ATTRIBUTE, SUBVARIETY_ATTRIBUTE);
		ranksToNormalForms.put(FORM_ATTRIBUTE, FORM_ATTRIBUTE);
		ranksToNormalForms.put(SUBFORM_ATTRIBUTE, SUBFORM_ATTRIBUTE);
	}
	
	private static LinkedHashMap ranksToLevels = new LinkedHashMap();
	private static String[] levelsToRanks;
	private static int familyRankLevel;
	private static int genusRankLevel;
	private static int speciesRankLevel;
	static {
		ranksToLevels.put(KINGDOM_ATTRIBUTE, new Integer(ranksToLevels.size()));
		ranksToLevels.put(SUBKINGDOM_ATTRIBUTE, new Integer(ranksToLevels.size()));
		
		ranksToLevels.put(PHYLUM_ATTRIBUTE, new Integer(ranksToLevels.size()));
		ranksToLevels.put(SUBPHYLUM_ATTRIBUTE, new Integer(ranksToLevels.size()));
		ranksToLevels.put(INFRAPHYLUM_ATTRIBUTE, new Integer(ranksToLevels.size()));
		ranksToLevels.put("parvPhylum", new Integer(ranksToLevels.size()));
		
		ranksToLevels.put(SUPERCLASS_ATTRIBUTE, new Integer(ranksToLevels.size()));
		ranksToLevels.put(CLASS_ATTRIBUTE, new Integer(ranksToLevels.size()));
		ranksToLevels.put(SUBCLASS_ATTRIBUTE, new Integer(ranksToLevels.size()));
		ranksToLevels.put(INFRACLASS_ATTRIBUTE, new Integer(ranksToLevels.size()));
		ranksToLevels.put("parvClass", new Integer(ranksToLevels.size()));
		
		ranksToLevels.put(SUPERORDER_ATTRIBUTE, new Integer(ranksToLevels.size()));
		ranksToLevels.put(ORDER_ATTRIBUTE, new Integer(ranksToLevels.size()));
		ranksToLevels.put(SUBORDER_ATTRIBUTE, new Integer(ranksToLevels.size()));
		ranksToLevels.put(INFRAORDER_ATTRIBUTE, new Integer(ranksToLevels.size()));
		ranksToLevels.put("parvOrder", new Integer(ranksToLevels.size()));
		
		ranksToLevels.put(SUPERFAMILY_ATTRIBUTE, new Integer(ranksToLevels.size()));
		ranksToLevels.put("epiFamily", new Integer(ranksToLevels.size()));
		ranksToLevels.put(FAMILY_ATTRIBUTE, new Integer(ranksToLevels.size()));
		ranksToLevels.put(SUBFAMILY_ATTRIBUTE, new Integer(ranksToLevels.size()));
		
		ranksToLevels.put(SUPERTRIBE_ATTRIBUTE, new Integer(ranksToLevels.size()));
		ranksToLevels.put(TRIBE_ATTRIBUTE, new Integer(ranksToLevels.size()));
		ranksToLevels.put(SUBTRIBE_ATTRIBUTE, new Integer(ranksToLevels.size()));
		ranksToLevels.put(INFRATRIBE_ATTRIBUTE, new Integer(ranksToLevels.size()));
		
		ranksToLevels.put(GENUS_ATTRIBUTE, new Integer(ranksToLevels.size()));
		ranksToLevels.put(SUBGENUS_ATTRIBUTE, new Integer(ranksToLevels.size()));
		ranksToLevels.put(SECTION_ATTRIBUTE, new Integer(ranksToLevels.size()));
		ranksToLevels.put(SUBSECTION_ATTRIBUTE, new Integer(ranksToLevels.size()));
		ranksToLevels.put(SERIES_ATTRIBUTE, new Integer(ranksToLevels.size()));
		ranksToLevels.put(SUBSERIES_ATTRIBUTE, new Integer(ranksToLevels.size()));
		
		ranksToLevels.put(SPECIES_ATTRIBUTE, new Integer(ranksToLevels.size()));
		ranksToLevels.put(SUBSPECIES_ATTRIBUTE, new Integer(ranksToLevels.size()));
		ranksToLevels.put(VARIETY_ATTRIBUTE, new Integer(ranksToLevels.size()));
		ranksToLevels.put(SUBVARIETY_ATTRIBUTE, new Integer(ranksToLevels.size()));
		ranksToLevels.put(FORM_ATTRIBUTE, new Integer(ranksToLevels.size()));
		ranksToLevels.put(SUBFORM_ATTRIBUTE, new Integer(ranksToLevels.size()));
		
		familyRankLevel = ((Integer) ranksToLevels.get(FAMILY_ATTRIBUTE)).intValue();
		genusRankLevel = ((Integer) ranksToLevels.get(GENUS_ATTRIBUTE)).intValue();
		speciesRankLevel = ((Integer) ranksToLevels.get(SPECIES_ATTRIBUTE)).intValue();
		
		levelsToRanks = new String[ranksToLevels.size()];
		for (Iterator rit = ranksToLevels.keySet().iterator(); rit.hasNext();) {
			String rank = ((String) rit.next());
			Integer level = ((Integer) ranksToLevels.get(rank));
			levelsToRanks[level.intValue()] = rank;
		}
	}
	
	private static TreeMap primaryRanks = new TreeMap(String.CASE_INSENSITIVE_ORDER);
	static {
		primaryRanks.put(KINGDOM_ATTRIBUTE, KINGDOM_ATTRIBUTE);
		primaryRanks.put(PHYLUM_ATTRIBUTE, PHYLUM_ATTRIBUTE);
		primaryRanks.put(CLASS_ATTRIBUTE, CLASS_ATTRIBUTE);
		primaryRanks.put(ORDER_ATTRIBUTE, ORDER_ATTRIBUTE);
		primaryRanks.put(FAMILY_ATTRIBUTE, FAMILY_ATTRIBUTE);
		primaryRanks.put(GENUS_ATTRIBUTE, GENUS_ATTRIBUTE);
		primaryRanks.put(SPECIES_ATTRIBUTE, SPECIES_ATTRIBUTE);
	}
	
	private static TreeMap primaryRankToParents = new TreeMap(String.CASE_INSENSITIVE_ORDER);
	static {
		primaryRankToParents.put(PHYLUM_ATTRIBUTE, KINGDOM_ATTRIBUTE);
		primaryRankToParents.put(CLASS_ATTRIBUTE, PHYLUM_ATTRIBUTE);
		primaryRankToParents.put(ORDER_ATTRIBUTE, CLASS_ATTRIBUTE);
		primaryRankToParents.put(FAMILY_ATTRIBUTE, ORDER_ATTRIBUTE);
		primaryRankToParents.put(GENUS_ATTRIBUTE, FAMILY_ATTRIBUTE);
		primaryRankToParents.put(SPECIES_ATTRIBUTE, GENUS_ATTRIBUTE);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		
		//	get path arguments
		String command = ((args.length < 1) ? "-help" : args[0]);
		String sourcePath = ((args.length < 2) ? "-help" : args[1]);
		String destPath = ((args.length < 3) ? null : args[2]);
		
		//	execute command
		if ("-help".equalsIgnoreCase(command)) {
			System.out.println("The following commands are available in the Catalog-of-Life tool:");
			System.out.println("");
			System.out.println("-help: print this help text and exit");
			System.out.println("unzip: unzip a DarwingCore Archive dump of CoL data");
			System.out.println("scan: scan the Taxon.tsv file for non-ASCII characters, factoting in");
			System.out.println("      any character replacements specified in any charMappings.tsv");
			System.out.println("      file and any field value substitutions specified specified in");
			System.out.println("      any valueMappings.tsv file found next to it");
			System.out.println("clean: clean the Taxon.tsv file from non-ASCII characters, normalizing");
			System.out.println("       spacing along the way, creating Taxon.clean.tsv next to it, and");
			System.out.println("       factoting in any character replacements specified in any");
			System.out.println("       charMappings.tsv file and any field value substitutions");
			System.out.println("       specified specified in any valueMappings.tsv file found next to");
			System.out.println("       it");
			System.out.println("extract: extract authority and bibliographic reference data from the");
			System.out.println("         Taxon.clean.tsv (or Taxon.tsv, whichever is found)");
			System.out.println("analyze-t: analyze Taxon.clean.tsv for dangling ID links and broken");
			System.out.println("           rank hierarchies, factoring in any field value substitutions");
			System.out.println("           specified in any valueMappings.tsv file found next to it,");
			System.out.println("           also generating a list of the sizes (in taxon records) of");
			System.out.println("           trees and sub-trees of significant size to allow");
			System.out.println("           customization of data tiling, i.e., how data is distributed");
			System.out.println("           into individual files");
			System.out.println("distill: create the data files from Taxon.clean.tsv, factoring in any");
			System.out.println("         field value substitutions specified in any valueMappings.tsv");
			System.out.println("         file found next to it, as well as custom tile assignments");
			System.out.println("         specified in any dataTiling.tsv file found next to it");
			System.out.println("analyze-d: analyze data files for the frequency of terms and prefixes");
			System.out.println("           to allow customization of index tiling, i.e., how index data");
			System.out.println("           is distributed into individual files");
			System.out.println("index: create the index files from the data files, factoring in any");
			System.out.println("       custom tile boundaries specified in any indexTiling.tsv file");
			System.out.println("       found next to the data files");
			System.out.println("pack-z: zip up all the data and index files, also including a meta.txt");
			System.out.println("        descriptor file");
			System.out.println("pack-w: hash-rename all the data and index files for wed-based updates");
			System.out.println("        and prepare the associated meta.txt descriptor file");
			System.out.println("generate-z: do all of the above, in the listed sequence, packing a zip");
			System.out.println("            file in the end");
			System.out.println("generate-w: do all of the above, in the listed sequence, creating the");
			System.out.println("            hash-named files for online deployment in the end");
			System.out.println("");
			System.out.println("serve: start a small local HTTP server to serve data packed with the");
			System.out.println("       'pack-w' or 'generate-w' command via a simple JSON API");
			System.out.println("");
			System.out.println("Use '<command> -help' to print detailed help for individual commands.");
			return;
		}
		else if ("-help".equalsIgnoreCase(sourcePath)) {
			if ("generate-z".equalsIgnoreCase(command)) {
				System.out.println("'generate-z <dwcaPath> <destPath>': generate data and index files from");
				System.out.println("the contents of a DarwinCore Archive dump of CoL data");
				System.out.println("");
				System.out.println("<dwcaPath>: the path of the DarwinCore Archive to start from");
				System.out.println("<destPath>: the path of the zip file to create");
				System.out.println("");
				System.out.println("This command factors in any any character replacements specified in");
				System.out.println("any charMappings.tsv file and any field value substitutions specified");
				System.out.println("in any valueMappings.tsv file found next to the source DarwinCore");
				System.out.println("Archive. It will fail with an error if it encounters any unmapped non-");
				System.out.println("ASCII characters");
			}
			if ("generate-w".equalsIgnoreCase(command)) {
				System.out.println("'generate-z <dwcaPath> <destPath>': generate data and index files from");
				System.out.println("the contents of a DarwinCore Archive dump of CoL data");
				System.out.println("");
				System.out.println("<dwcaPath>: the path of the DarwinCore Archive to start from");
				System.out.println("<destPath>: the path of the folder to put the renamed files in");
				System.out.println("            (defaults to '<dwcaFolder>/<timestamp>' if omitted)");
				System.out.println("");
				System.out.println("This command factors in any any character replacements specified in");
				System.out.println("any charMappings.tsv file and any field value substitutions specified");
				System.out.println("in any valueMappings.tsv file found next to the source DarwinCore");
				System.out.println("Archive. It will fail with an error if it encounters any unmapped non-");
				System.out.println("ASCII characters");
			}
			else if ("unzip".equalsIgnoreCase(command)) {
				System.out.println("'unzip <dwcaPath> <destFolder>': extract the contents of a DarwinCore");
				System.out.println("Archive dump of CoL data and start the meta.txt file with the name of");
				System.out.println("the source file and credits");
				System.out.println("");
				System.out.println("<dwcaPath>: the path of the DarwinCore Archive to start from");
				System.out.println("<destFolder>: the path of the folder to unzip to (defaults to the");
				System.out.println("            folder the source DarwinCore Archive lies in if omitted)");
			}
			else if ("scan".equalsIgnoreCase(command)) {
				System.out.println("'scan <dwcaFolder> <ignorSynonyms>': scan the Taxon.tsv file for non-ASCII");
				System.out.println(" characters, factoting in any character replacements specified in any");
				System.out.println("charMappings.tsv file and any field value substitutions specified in");
				System.out.println("any valueMappings.tsv file found next to it");
				System.out.println("");
				System.out.println("<dwcaFolder>: the path of the folder the Taxon.tsv is located in");
				System.out.println("<ignorSynonyms>: ignore problems on synonyms (optional, set to '-is' to");
				System.out.println("                 activate synonym-ignoring mode)");
			}
			else if ("clean".equalsIgnoreCase(command)) {
				System.out.println("'clean <dwcaFolder>': clean the Taxon.tsv file from any non-ASCII");
				System.out.println("characters, using the character replacements specified in any");
				System.out.println("charMappings.tsv file and any field value substitutions specified in");
				System.out.println("any valueMappings.tsv file found next to it, producing Taxon.clean.tsv");
				System.out.println("");
				System.out.println("<dwcaFolder>: the path of the folder the Taxon.tsv is located in");
				System.out.println("");
				System.out.println("This command will fail with an error if it encounters any unmapped");
				System.out.println("non-ASCII characters");
			}
			else if ("extract".equalsIgnoreCase(command)) {
				System.out.println("'extract <dwcaFolder> <mode>': extract authority and bibliographic");
				System.out.println("reference data from the Taxon.clean.tsv or Taxon.tsv, whichever is");
				System.out.println("found)");
				System.out.println("");
				System.out.println("<dwcaFolder>: the path of the folder the Taxon.clean.tsv is located in");
				System.out.println("<mode>: set to '-s' (for source mode) to use the Taxon.tsv file even");
				System.out.println("        if the Taxon.clean.tsv file exists (optional argument)");
				System.out.println("");
				System.out.println("This command produces the authority.txt and references.txt files in");
				System.out.println("the same folder the source Taxon.clean.tsv (or Taxon.tsv) is located");
				System.out.println("in; the step is not required for generating the data or index files,");
				System.out.println("but exists to allow for the extraction of other data that might be of");
				System.out.println("interest");
			}
			else if ("analyze-t".equalsIgnoreCase(command)) {
				System.out.println("'analyze-t <dwcaFolder>': analyze Taxon.clean.tsv for dangling ID");
				System.out.println("links and broken rank hierarchies, factoring in any field value");
				System.out.println("substitutions specified in any valueMappings.tsv file found next to");
				System.out.println("it, also generating a list of the sizes (in taxon records) of trees");
				System.out.println("and sub-trees of significant size to allow customization of data");
				System.out.println("tiling, i.e., how data is distributed into individual files");
				System.out.println("");
				System.out.println("<dwcaFolder>: the path of the folder the Taxon.clean.tsv is located in");
			}
			else if ("distill".equalsIgnoreCase(command)) {
				System.out.println("'distill <dwcaFolder>': create the data files from Taxon.clean.tsv,");
				System.out.println("factoring in any field value substitutions specified in any");
				System.out.println("valueMappings.tsv file found next to it, as well as custom tile");
				System.out.println("assignments specified in any dataTiling.tsv file found next to it");
				System.out.println("");
				System.out.println("<dwcaFolder>: the path of the folder the Taxon.clean.tsv is located in");
				System.out.println("");
				System.out.println("On top of the individual data files, this command also produces the");
				System.out.println("data.tiles.txt meta file");
			}
			else if ("analyze-d".equalsIgnoreCase(command)) {
				System.out.println("'analyze-d <dwcaFolder>': analyze data files for the frequency of");
				System.out.println("terms and prefixes to allow customization of index tiling, i.e., how");
				System.out.println("index data is distributed into individual files");
				System.out.println("");
				System.out.println("<dwcaFolder>: the path of the folder the data files are located in");
				System.out.println("");
				System.out.println("This command fails with an error if no data.tiles.txt is found");
			}
			else if ("index".equalsIgnoreCase(command)) {
				System.out.println("'index <dwcaFolder>': create the full text index files from the data");
				System.out.println("files, factoring in any custom tile boundaries specified in any");
				System.out.println("indexTiling.tsv file found next to the data files");
				System.out.println("");
				System.out.println("<dwcaFolder>: the path of the folder the data files are located in");
				System.out.println("");
				System.out.println("This command fails with an error if no data.tiles.txt is found; on top");
				System.out.println("of the individual index files, this command also produces the");
				System.out.println("index.tiles.txt meta file");
			}
			else if ("pack-z".equalsIgnoreCase(command)) {
				System.out.println("'pack-z <dwcaFolder>': zip up all the data and index files, also");
				System.out.println("including a meta.txt descriptor file");
				System.out.println("");
				System.out.println("<dwcaFolder>: the path of the folder the data files are located in");
				System.out.println("");
				System.out.println("This command fails with an error if no data.tiles.txt or no");
				System.out.println("index.tiles.txt is found; before zipping up all the files, this");
				System.out.println("command amends the meta.txt file with the MD5 hashes of all the data");
				System.out.println("and index files");
			}
			else if ("pack-w".equalsIgnoreCase(command)) {
				System.out.println("'pack-w <dwcaFolder> <destPath>': hash-rename all the data and");
				System.out.println("index files for wed-based updates and prepare the associated meta.txt");
				System.out.println("descriptor file");
				System.out.println("");
				System.out.println("<dwcaFolder>: the path of the folder the data files are located in");
				System.out.println("<destPath>: the path of the folder to put the renamed files in");
				System.out.println("            (defaults to '<dwcaFolder>/<timestamp>' if omitted)");
				System.out.println("");
				System.out.println("This command fails with an error if no data.tiles.txt or no");
				System.out.println("index.tiles.txt is found; before copying and hash-renaming all the");
				System.out.println("files, this command amends the meta.txt file with the MD5 hashes of");
				System.out.println("all the data and index files, which it then also inserts in the names");
				System.out.println("of the copied files");
			}
			else if ("serve".equalsIgnoreCase(command)) {
				System.out.println("'serve <dwcaFolder> <port>': start a small local HTTP server on a given");
				System.out.println("port and provide a JSON API for the data in a given folder");
				System.out.println("");
				System.out.println("<colFolder>: the path of the folder the data files are located in");
				System.out.println("<port>: the port to open the server on (defaults to '49173' if omitted,");
				System.out.println("        the decimal form of '0xC015')");
				System.out.println("<verbose>: set to '-v' to activate verbose mode (optional)");
				System.out.println("");
				System.out.println("This command fails with an error if no data.tiles.txt or no");
				System.out.println("index.tiles.txt is found in the specified folder, or is the speciefied");
				System.out.println("port is occupied by another application");
			}
			else System.out.println("Invalid command '" + command + "', use '-help' to list available commands.");
			return;
		}
		else if ("generate-z".equalsIgnoreCase(command))
			generate(sourcePath, destPath, true);
		else if ("generate-w".equalsIgnoreCase(command))
			generate(sourcePath, destPath, false);
		else if ("unzip".equalsIgnoreCase(command))
			unzip(sourcePath, destPath);
		else if ("scan".equalsIgnoreCase(command))
			scan(sourcePath, "-is".equals(destPath));
		else if ("clean".equalsIgnoreCase(command))
			clean(sourcePath);
		else if ("check-mappings".equalsIgnoreCase(command))
			checkMappings(sourcePath);
		else if ("extract".equalsIgnoreCase(command))
			extract(sourcePath);
		else if ("analyze-t".equalsIgnoreCase(command))
			analyzeTaxon(sourcePath);
		else if ("distill".equalsIgnoreCase(command))
			distill(sourcePath);
		else if ("analyze-d".equalsIgnoreCase(command))
			analyzeData(sourcePath);
		else if ("index".equalsIgnoreCase(command))
			index(sourcePath);
		else if ("pack-z".equalsIgnoreCase(command))
			pack(sourcePath, destPath, true);
		else if ("pack-w".equalsIgnoreCase(command))
			pack(sourcePath, destPath, false);
		else if ("serve".equalsIgnoreCase(command)) {
			int port = 49173;
			boolean verbose = false;
			if (destPath == null) {}
			else if ("-v".equals(destPath))
				verbose = true;
			else {
				port = Integer.parseInt(destPath);
				verbose = ((args.length > 3) && "-v".equals(args[3]));
			}
			serve(sourcePath, port, verbose);
		}
		else System.out.println("Invalid command '" + command + "', use '-help' to list available commands.");
	}
	
	private static void unzip(String dwcaPath, String destFolder) throws Exception {
		File dwca = new File(dwcaPath);
		File dest = ((destFolder == null) ? dwca.getParentFile() : new File(destFolder));
		unzip(dwca, dest);
	}
	private static void unzip(File dwca, File destFolder) throws Exception {
		ZipInputStream zip = new ZipInputStream(new BufferedInputStream(new FileInputStream(dwca)));
		for (ZipEntry ze; (ze = zip.getNextEntry()) != null;) {
			File destFile = new File(destFolder, ze.getName());
			destFile.getParentFile().mkdirs(); // we might have a sub folder
			OutputStream out = new BufferedOutputStream(new FileOutputStream(destFile));
			byte[] buffer = new byte[1024];
			for (int r; (r = zip.read(buffer, 0, buffer.length)) != -1;)
				out.write(buffer, 0, r);
			out.flush();
			out.close();
			if (ze.getTime() != -1)
				destFile.setLastModified(ze.getTime());
		}
		zip.close();
		
		File metaFile = new File(destFolder, "meta.txt");
		BufferedWriter metaBw = new BufferedWriter(new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(metaFile)), "UTF-8"));
		metaBw.write("Created from DarwingCore Archive dump of the Catalogue of Life (see http://catalogueoflife.org for details).");
		metaBw.newLine();
		metaBw.write("Dump: " + dwca.getName());
		metaBw.newLine();
		metaBw.flush();
		metaBw.close();
	}
	
	private static class MetaXml {
		private TreeMap fieldNamesByIndex = new TreeMap();
		private String[] fieldNames = null;
		int taxonIdIndex = -1;
		int parentTaxonIdIndex = -1;
		int validTaxonIdIndex = -1;
		int taxonStatusIndex = -1;
		int taxonRankIndex = -1;
		int taxonNameIndex = -1;
		int genusNameIndex = -1;
		int belowGenusNameIndex = -1;
		int speciesNameIndex = -1;
		int belowSpeciesNameIndex = -1;
		int publishedInIndex = -1;
		int nomenclaturalCodeIndex = -1;
		int remarksIndex = -1;
		int referencesIndex = -1;
		void addField(String name, int index) {
			this.fieldNamesByIndex.put(new Integer(index), name);
			if ("taxonID".equals(name))
				this.taxonIdIndex = index;
			else if ("parentNameUsageID".equals(name))
				this.parentTaxonIdIndex = index;
			else if ("acceptedNameUsageID".equals(name))
				this.validTaxonIdIndex = index;
			else if ("taxonomicStatus".equals(name))
				this.taxonStatusIndex = index;
			else if ("taxonRank".equals(name))
				this.taxonRankIndex = index;
			else if ("scientificName".equals(name))
				this.taxonNameIndex = index;
			else if ("genericName".equals(name))
				this.genusNameIndex = index;
			else if ("infragenericEpithet".equals(name))
				this.belowGenusNameIndex = index;
			else if ("specificEpithet".equals(name))
				this.speciesNameIndex = index;
			else if ("infraspecificEpithet".equals(name))
				this.belowSpeciesNameIndex = index;
			else if ("namePublishedIn".equals(name))
				this.publishedInIndex = index;
			else if ("nomenclaturalCode".equals(name))
				this.nomenclaturalCodeIndex = index;
			else if ("taxonRemarks".equals(name))
				this.remarksIndex = index;
			else if ("references".equals(name))
				this.referencesIndex = index;
		}
		void finish() {
			ArrayList fieldNameList = new ArrayList();
			for (Iterator iit = this.fieldNamesByIndex.keySet().iterator(); iit.hasNext();) {
				Integer index = ((Integer) iit.next());
				while (fieldNameList.size() < index.intValue())
					fieldNameList.add(null);
				fieldNameList.add(this.fieldNamesByIndex.get(index));
			}
			this.fieldNames = ((String[]) fieldNameList.toArray(new String[fieldNameList.size()]));
			this.fieldNamesByIndex = null;
		}
		String getFieldName(int index) {
			return this.fieldNames[index];
		}
	}
	
	static Grammar xmlGrammar = new StandardGrammar();
	static Parser xmlParser = new Parser(xmlGrammar);
	private static MetaXml getMetaXml(File dwcaFolder) throws Exception {
		File metaXmlFile = new File(dwcaFolder, "meta.xml");
		BufferedReader metaXmlBr = new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(metaXmlFile)), "UTF-8"));
		final MetaXml metaXml = new MetaXml();
		xmlParser.stream(metaXmlBr, new TokenReceiver() {
			private boolean inCore = false;
			public void storeToken(String token, int treeDepth) throws IOException {
				if (xmlGrammar.isTag(token)) {
					String type = xmlGrammar.getType(token);
					if ("core".equals(type))
						this.inCore = !xmlGrammar.isEndTag(token);
					else if (this.inCore && "field".equals(type)) {
						TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, xmlGrammar);
						String index = tnas.getAttribute("index");
						String term = tnas.getAttribute("term");
						String name = term.substring(term.lastIndexOf("/") + "/".length());
						metaXml.addField(name, Integer.parseInt(index));
					}
				}
			}
			public void close() throws IOException { /* nothing to close here */ }
		});
		metaXmlBr.close();
		metaXml.finish();
		return metaXml;
	}
	
	private static class DataValueNormalizer {
		HashSet removeTaxonIDs = new HashSet();
		HashMap replacementsByTaxonId = new HashMap();
		HashMap replacementsByFieldName = new HashMap();
		HashMap normalizersByFieldName = new HashMap();
		ArrayList charNormalizers = new ArrayList();
		void addDataValueReplacement(String taxonId, String fieldName, String dataValue, boolean isDataValuePattern, String replacementValue) {
			if ((taxonId != null) && (fieldName != null)) {
				HashMap replacementsByFieldName = ((HashMap) this.replacementsByTaxonId.get(taxonId));
				if (replacementsByFieldName == null) {
					replacementsByFieldName = new HashMap();
					this.replacementsByTaxonId.put(taxonId, replacementsByFieldName);
				}
				replacementsByFieldName.put(fieldName, normalizeSpaces(replacementValue));
			}
			else if ((fieldName != null) && (dataValue != null)) {
				if (isDataValuePattern) {
					ArrayList normalizers = ((ArrayList) this.normalizersByFieldName.get(fieldName));
					if (normalizers == null) {
						normalizers = new ArrayList();
						this.normalizersByFieldName.put(fieldName, normalizers);
					}
					normalizers.add(new PatternNormalizer(dataValue, replacementValue));
				}
				else {
					HashMap replacementsByDataValue = ((HashMap) this.replacementsByFieldName.get(fieldName));
					if (replacementsByDataValue == null) {
						replacementsByDataValue = new HashMap();
						this.replacementsByFieldName.put(fieldName, replacementsByDataValue);
					}
					replacementsByDataValue.put(dataValue, normalizeSpaces(replacementValue));
				}
			}
		}
		void addRemoval(String taxonId) {
			this.removeTaxonIDs.add(taxonId);
		}
		void addCharReplacement(String charMatcher, String matchReplacement) {
			this.charNormalizers.add(new CharNormalizer(charMatcher, matchReplacement));
		}
		boolean removeTaxon(String taxonId) {
			return this.removeTaxonIDs.contains(taxonId);
		}
		String normalizeDataValue(String taxonId, String fieldName, String dataValue) {
			if (this.replacementsByTaxonId.containsKey(taxonId)) {
				HashMap replacementsByFieldName = ((HashMap) this.replacementsByTaxonId.get(taxonId));
				if (replacementsByFieldName.containsKey(fieldName))
					return ((String) replacementsByFieldName.get(fieldName));
			}
			
			if (this.replacementsByFieldName.containsKey(fieldName)) {
				HashMap replacementsByDataValue = ((HashMap) this.replacementsByFieldName.get(fieldName));
				if (replacementsByDataValue.containsKey(dataValue))
					return ((String) replacementsByDataValue.get(dataValue));
			}
			
			for (int n = 0; n < this.charNormalizers.size(); n++) {
				ValueNormalizer vn = ((ValueNormalizer) charNormalizers.get(n));
				dataValue = vn.normalizeValue(dataValue);
			}
			dataValue = normalizeSpaces(dataValue);
			
			ArrayList normalizers = ((ArrayList) this.normalizersByFieldName.get(fieldName));
			while (true) {
				String nValue = dataValue;
				if (normalizers != null) {
					for (int n = 0; n < normalizers.size(); n++) {
						ValueNormalizer vn = ((ValueNormalizer) normalizers.get(n));
						nValue = vn.normalizeValue(nValue);
						if (dataValue != nValue)
							break; // evaluate until first match only (something else might still want a shot, and it might be before current one)
					}
				}
				if (dataValue.equals(nValue))
					return normalizeSpaces(dataValue);
				else dataValue = nValue;
			}
		}
		static String normalizeSpaces(String value) {
			if (value == null)
				return value;
			value = value.replaceAll("[\\u00A0\\u2000-\\u2008]", " "); // fancy spaces ...
			value = value.replaceAll("[\\u2009-\\u200F]", ""); // super thin or even invisible spaces ...
			value = value.replaceAll("\\,", ", ");
			value = value.replaceAll("\\;", "; ");
			value = value.replaceAll("\\.", ". ");
			value = value.replaceAll("\\:", ": ");
			value = value.replaceAll("\\s{2,}", " ");
			value = value.replaceAll("\\(\\s+", "(");
			value = value.replaceAll("\\s+\\)", ")");
			value = value.replaceAll("\\[\\s+", "[");
			value = value.replaceAll("\\s+\\]", "]");
			value = value.replaceAll("\\s+\\,", ",");
			value = value.replaceAll("\\s+\\;", ";");
			value = value.replaceAll("\\s+\\.", ".");
			value = value.replaceAll("\\s+\\:", ":");
			value = value.replaceAll("[\\u00A8\\u00B0\\u00B4\\u00B8\\u02C7\\u02D8\\u02DB\\u02D9\\u02DA\\u02DB]", ""); // standalone diacritic markers
			return value;
		}
	}
	
	private static abstract class ValueNormalizer {
		abstract String normalizeValue(String value);
	}
	private static class CharNormalizer extends ValueNormalizer {
		private Pattern pattern;
		private String replacement;
		CharNormalizer(String pattern, String replacement) {
			this.pattern = Pattern.compile(pattern);
			this.replacement = replacement;
		}
		String normalizeValue(String value) {
			return ((value == null) ? null : this.pattern.matcher(value).replaceAll(this.replacement));
		}
	}
	private static class PatternNormalizer extends ValueNormalizer {
		private boolean startMatch = true;
		private boolean endMatch = true;
		private Pattern pattern;
		private String replacement;
		PatternNormalizer(String pattern, String replacement) {
			if (replacement.equals("$_$")) {
				this.startMatch = false;
				this.endMatch = false;
				replacement = "";
			}
			if (replacement.startsWith("$_")) {
				this.startMatch = false;
				replacement = replacement.substring("$_".length());
			}
			if (replacement.endsWith("_$")) {
				this.endMatch = false;
				replacement = replacement.substring(0, (replacement.length() - "_$".length()));
			}
			this.pattern = Pattern.compile(pattern);
			this.replacement = replacement;
		}
		String normalizeValue(String value) {
			if (value == null)
				return null;
			Matcher match = this.pattern.matcher(value);
			if (!match.find())
				return value;
			if (this.startMatch && (match.start() != 0))
				return value;
			if (this.endMatch && (match.end() != value.length()))
				return value;
			return (((match.start() == 0) ? "" : value.substring(0, match.start())) + this.replacement + ((match.end() == value.length()) ? "" : value.substring(match.end()))).trim();
		}
	}
	
	private static DataValueNormalizer getDataValueNormalizer(File dwcaFolder) throws Exception {
		File cmFile = new File(dwcaFolder, "charMappings.tsv");
		DataValueNormalizer dvn = new DataValueNormalizer();
		if (cmFile.exists()) {
			BufferedReader cmBr = new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(cmFile)), "UTF-8"));
			for (String cm; (cm = cmBr.readLine()) != null;) {
				if (cm.startsWith("//"))
					continue;
				String[] cmData = cm.split("\\t");
				if (cmData.length < 2)
					continue;
				dvn.addCharReplacement(cmData[0], cmData[1]);
			}
			cmBr.close();
		}
		File vmFile = new File(dwcaFolder, "valueMappings.tsv");
		if (vmFile.exists()) {
			BufferedReader vmBr = new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(vmFile)), "UTF-8"));
			for (String vm; (vm = vmBr.readLine()) != null;) {
				if (vm.startsWith("//"))
					continue;
				String[] vmData = vm.split("\\t");
				if (vmData.length < 3)
					continue;
				if (vmData[2].startsWith("P:"))
					dvn.addDataValueReplacement(null, vmData[1], vmData[2].substring("P:".length()), true, ((vmData.length == 3) ? "" : vmData[3]));
				else if (vmData[0].length() == 0)
					dvn.addDataValueReplacement(null, vmData[1], vmData[2], false, ((vmData.length == 3) ? "" : vmData[3]));
				else if ("REMOVE".equals(vmData[2]))
					dvn.addRemoval(vmData[0]);
				else dvn.addDataValueReplacement(vmData[0], vmData[1], null, false, ((vmData.length == 3) ? "" : vmData[3]));
			}
			vmBr.close();
		}
		return dvn;
	}
	
	private static Pattern numberPattern = Pattern.compile("[0-9]+");
	private static void scan(String dwcaFolder, boolean ignoreSynonymProblems) throws Exception {
		scan(new File(dwcaFolder), ignoreSynonymProblems);
	}
	private static void scan(File dwcaFolder, boolean ignoreSynonymProblems) throws Exception {
		MetaXml metaXml = getMetaXml(dwcaFolder);
		DataValueNormalizer valueNormalizer = getDataValueNormalizer(dwcaFolder);
		
		//	scan Taxon.tsv line by line
		File taxonTsv = new File(dwcaFolder, "Taxon.tsv");
		BufferedReader tBr = new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(taxonTsv)), "UTF-8"));
		tBr.readLine(); // skip column headers
		int tLineCount = 0;
		CountingSet problemChars = new CountingSet(new TreeMap());
		CountingSet problemIDs = new CountingSet(new TreeMap());
		CountingSet problemRanks = new CountingSet(new TreeMap());
		CountingSet problemEpithets = new CountingSet(new TreeMap());
		CountingSet brokenYearAuthorities = new CountingSet(new TreeMap());
		CountingSet brokenStartAuthorities = new CountingSet(new TreeMap());
		CountingSet longAuthorities = new CountingSet(new TreeMap());
		CountingSet betweenGenusAndSpeciesNames = new CountingSet(new TreeMap());
		LinkedHashMap problemRecords = new LinkedHashMap();
		for (String tLine; (tLine = tBr.readLine()) != null;) {
			tLineCount++;
			if ((tLineCount % 5000) == 0) {
				System.out.println("Read " + tLineCount + " lines of data so far");
				if ((tLineCount % 100000) == 0)
					System.gc();
			}
			String[] tData = tLine.split("\\t");
			if (valueNormalizer.removeTaxon(tData[metaXml.taxonIdIndex]))
				continue;
			normalizeValues(metaXml, valueNormalizer, tLine, tData, false, problemChars, problemRanks, problemEpithets, brokenYearAuthorities, brokenStartAuthorities, longAuthorities, problemRecords, ignoreSynonymProblems);
			try {
				CatalogOfLifeLocal.parseIntBase29(tData[metaXml.taxonIdIndex]);
			}
			catch (IllegalArgumentException iae) {
				if (recordProblems(tData, metaXml, ignoreSynonymProblems)) {
					problemIDs.add(tData[metaXml.taxonIdIndex]);
					addProblem(problemRecords, tLine, ("invalid ID '" + tData[metaXml.taxonIdIndex] + "'"));
				}
			}
		}
		tBr.close();
		System.out.println("Found " + problemRanks.size() + " problematic ranks, " + problemRanks.elementCount() + " distinct ones:");
		for (Iterator prit = problemRanks.iterator(); prit.hasNext();) {
			String pr = ((String) prit.next());
			System.out.println("- " + pr + ": " + problemRanks.getCount(pr) + " times");
		}
		System.out.println("Found " + problemChars.size() + " problematic characters, " + problemChars.elementCount() + " distinct ones:");
		for (Iterator pcit = problemChars.iterator(); pcit.hasNext();) {
			Character pc = ((Character) pcit.next());
			System.out.println("- " + pc + " (0x" + Integer.toString(((int) pc.charValue()), 16).toUpperCase() + "): " + problemChars.getCount(pc) + " times");
		}
		System.out.println("Found " + problemIDs.size() + " problematic IDs, " + problemIDs.elementCount() + " distinct ones:");
		for (Iterator pidit = problemIDs.iterator(); pidit.hasNext();) {
			String pid = ((String) pidit.next());
			System.out.println("- " + pid + ": " + problemIDs.getCount(pid) + " times");
		}
		System.out.println("Found " + problemEpithets.size() + " problematic epithets, " + problemEpithets.elementCount() + " distinct ones:");
		for (Iterator peit = problemEpithets.iterator(); peit.hasNext();) {
			String pe = ((String) peit.next());
			System.out.println("- " + pe + ": " + problemEpithets.getCount(pe) + " times");
		}
		System.out.println("Found " + brokenYearAuthorities.size() + " authorities with broken years, " + brokenYearAuthorities.elementCount() + " distinct ones:");
		for (Iterator pait = brokenYearAuthorities.iterator(); pait.hasNext();) {
			String pa = ((String) pait.next());
			System.out.println("- " + pa + ": " + brokenYearAuthorities.getCount(pa) + " times");
		}
		System.out.println("Found " + brokenStartAuthorities.size() + " authorities with strange starts, " + brokenStartAuthorities.elementCount() + " distinct ones:");
		for (Iterator pait = brokenStartAuthorities.iterator(); pait.hasNext();) {
			String pa = ((String) pait.next());
			System.out.println("- " + pa + ": " + brokenStartAuthorities.getCount(pa) + " times");
		}
		System.out.println("Found " + longAuthorities.size() + " long authorities, " + longAuthorities.elementCount() + " distinct ones:");
		for (Iterator pait = longAuthorities.iterator(); pait.hasNext();) {
			String pa = ((String) pait.next());
			System.out.println("- " + pa + " (" + pa.length() + "): " + longAuthorities.getCount(pa) + " times");
		}
		System.out.println("Found " + betweenGenusAndSpeciesNames.size() + " names between genus and species, " + betweenGenusAndSpeciesNames.elementCount() + " distinct ones:");
		for (Iterator bgnit = betweenGenusAndSpeciesNames.iterator(); bgnit.hasNext();) {
			String bgn = ((String) bgnit.next());
			System.out.println("- " + bgn + ": " + betweenGenusAndSpeciesNames.getCount(bgn) + " times");
		}
		System.out.println("Found " + problemRecords.size() + " records with issues:");
		for (Iterator rit = problemRecords.keySet().iterator(); rit.hasNext();) {
			String tLine = ((String) rit.next());
			System.out.println(tLine);
			LinkedHashSet problems = ((LinkedHashSet) problemRecords.get(tLine));
			for (Iterator pit = problems.iterator(); pit.hasNext();)
				System.out.println(" - " + pit.next());
		}
	}
	
	private static void checkMappings(String dwcaFolder) throws Exception {
		checkMappings(new File(dwcaFolder));
	}
	private static void checkMappings(File dwcaFolder) throws Exception {
		MetaXml metaXml = getMetaXml(dwcaFolder);
		DataValueNormalizer valueNormalizer = getDataValueNormalizer(dwcaFolder);
		
		//	load raw value mappings
		File vmFile = new File(dwcaFolder, "valueMappings.tsv");
		HashMap mappingsByTaxonId = new HashMap();
		if (vmFile.exists()) {
			BufferedReader vmBr = new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(vmFile)), "UTF-8"));
			for (String vm; (vm = vmBr.readLine()) != null;) {
				if (vm.startsWith("//"))
					continue;
				String[] vmData = vm.split("\\t");
				if (vmData.length < 3)
					continue;
				String taxonId = vmData[0];
				if (taxonId.length() == 0)
					continue;
				if ("REMOVE".equals(vmData[2])) {
					mappingsByTaxonId.put(taxonId, null);
					continue;
				}
				String fieldName = vmData[1];
				String replacementValue = vmData[3];
				if (fieldName.length() != 0) {
					HashMap replacementsByFieldName = ((HashMap) mappingsByTaxonId.get(taxonId));
					if (replacementsByFieldName == null) {
						replacementsByFieldName = new HashMap();
						mappingsByTaxonId.put(taxonId, replacementsByFieldName);
					}
					replacementsByFieldName.put(fieldName, replacementValue);
				}
			}
			vmBr.close();
		}
		HashMap unusedMappingsByTaxonId = new HashMap();
		unusedMappingsByTaxonId.putAll(mappingsByTaxonId);
		
		//	scan Taxon.tsv line by line, recording usage of mapings
		File taxonTsv = new File(dwcaFolder, "Taxon.tsv");
		BufferedReader tBr = new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(taxonTsv)), "UTF-8"));
		tBr.readLine(); // skip column headers
		int tLineCount = 0;
		for (String tLine; (tLine = tBr.readLine()) != null;) {
			tLineCount++;
			if ((tLineCount % 5000) == 0) {
				System.out.println("Read " + tLineCount + " lines of data so far");
				if ((tLineCount % 100000) == 0)
					System.gc();
			}
			String[] tData = tLine.split("\\t");
			String taxonId = tData[metaXml.taxonIdIndex];
			if (unusedMappingsByTaxonId.remove(taxonId) == null)
				continue;
			HashMap mappings = ((HashMap) mappingsByTaxonId.get(taxonId));
			System.out.println(tLine);
			System.out.println(" ==> " + mappings);
			if (mappings == null)
				continue;
			normalizeValues(metaXml, valueNormalizer, tLine, tData, true, null, null, null, null, null, null, null, false);
			System.out.println(" ==> " + Arrays.toString(tData));
		}
		tBr.close();
		
		//	list obsolete mappings
		System.out.println("Found obsolete appings for " + unusedMappingsByTaxonId.size() + " taxon IDs:");
		for (Iterator otidit = unusedMappingsByTaxonId.keySet().iterator(); otidit.hasNext();) {
			String obsoleteTaxonId = ((String) otidit.next());
			HashMap mappings = ((HashMap) mappingsByTaxonId.get(obsoleteTaxonId));
			System.out.println(obsoleteTaxonId + " ==> " + mappings);
		}
	}
	
	/*
Filter/cleanup changes:
- split subspecies off species (as in 'alpina dorsoflava')
	 */
	private static Pattern speciesEpithetPattern = Pattern.compile("\\s[a-z]+(\\-[a-z]+)?\\s");
	private static Pattern utf8asAnsiPattern = Pattern.compile("(([\\u00C2-\\u00DF][\\u0080-\\u00BF])|([\\u00E0-\\u00EF][\\u0080-\\u00BF][\\u0080-\\u00BF]))");
	private static String[] normalizeValues(MetaXml metaXml, DataValueNormalizer valueNormalizer, String tLine, String[] tData, boolean clean, CountingSet problemChars, CountingSet problemRanks, CountingSet problemEpithets, CountingSet brokenYearAuthorities, CountingSet brokenStartAuthorities, CountingSet longAuthorities, LinkedHashMap problemRecords, boolean ignoreSynonymProblems) throws Exception {
		
		//	sort out viruses, their nomenclature is vastly different, data mostly contains vernacular names
		if ((tData.length > metaXml.nomenclaturalCodeIndex) && "ICVCN".equals(tData[metaXml.nomenclaturalCodeIndex]))
			return null;
		
		//	no use bothering with those, valid in other record
		if ((tData.length > metaXml.taxonStatusIndex) && ("MISAPPLIED".equals(tData[metaXml.taxonStatusIndex]) || "misapplied".equals(tData[metaXml.taxonStatusIndex])))
			return null;
		
		//	check whether or not to record problems
		boolean recordProblems = recordProblems(tData, metaXml, ignoreSynonymProblems);
		
		//	scan fields for problematic values
		String tId = tData[metaXml.taxonIdIndex];
		String tName = null;
		int tRankLevel = -1;
		for (int f = 0; f < tData.length; f++) {
			if (tData[f] == null)
				continue;
			if (tData[f].length() == 0) {
				tData[f] = null;
				continue;
			}
			if (f == metaXml.publishedInIndex) {
				if (clean)
					tData[f] = null;
				continue;
			}
			if (f == metaXml.remarksIndex) {
				if (clean)
					tData[f] = null;
				continue;
			}
			if (f == metaXml.referencesIndex) {
				if (clean)
					tData[f] = null;
				continue;
			}
			if ((tData[f] != null) && utf8asAnsiPattern.matcher(tData[f]).find()) {
				byte[] raw = tData[f].getBytes("ISO-8859-1");
				tData[f] = new String(raw, "UTF-8");
			}
			String nValue = valueNormalizer.normalizeDataValue(tId, metaXml.getFieldName(f), tData[f]);
			if (f == metaXml.taxonRankIndex) {
				if (!ranksToNormalForms.containsKey(nValue)) {
					if ((problemRanks != null) && recordProblems) {
						problemRanks.add(nValue);
						addProblem(problemRecords, tLine, "invalid rank");
					}
					if (clean) {
						System.out.println("Invalid taxon rank '" + nValue + "' in " + tLine);
						return null; // no use checking any further
					}
				}
				else {
					nValue = ((String) ranksToNormalForms.get(nValue));
					tRankLevel = ((Integer) ranksToLevels.get(nValue)).intValue();
					if (clean)
						tData[f] = nValue;
				}
				continue;
			}
			if ((nValue == null) || (nValue.trim().length() == 0)) {
				if (clean) {
					if (f == metaXml.taxonNameIndex) {
						System.out.println("Missing taxon name in " + tLine);
						return null; // no use keeping taxon without name
					}
					else tData[f] = null;
				}
				continue;
			}
			nValue = nValue.trim();
			if (clean)
				tData[f] = nValue;
			if (f == metaXml.taxonNameIndex)
				tName = nValue;
			for (int c = 0; c < nValue.length(); c++) {
				char ch = nValue.charAt(c);
				if (ch < 0x007F)
					continue;
				System.out.println("Invalid character '" + ch + "' (0x" + Integer.toString(((int) ch), 16).toUpperCase() + ") at " + c + " in " + metaXml.getFieldName(f) + " in " + tLine);
				if ((problemChars != null) && recordProblems) {
					problemChars.add(new Character(ch));
					addProblem(problemRecords, tLine, ("invalid char '" + ch + "' (0x" + Integer.toString(((int) ch), 16).toUpperCase() + ")"));
				}
				if (clean)
					return null; // no use checking any further
			}
		}
		
		if ((tName == null) || (tRankLevel == -1)) {
			System.out.println("Missing taxon name or invalid rank in " + tLine);
			return null; // no use checking any further
		}
		
		//	separate epithets and authority
		int tNameOffsets = segmentTaxonName(tName, tRankLevel, metaXml, tData);
		String tRank = levelsToRanks[tRankLevel];
		String epithetPrefix = tName.substring(0, ((tNameOffsets >>> 24) & 0xFF));
		String epithet = tName.substring(((tNameOffsets >>> 16) & 0xFF), ((tNameOffsets >>> 8) & 0xFF));
		String authority = tName.substring(tNameOffsets & 0xFF);
		
		//	check epithets of taxon name for funny morphology
		if (tRankLevel < genusRankLevel) {
			String nEpithets = valueNormalizer.normalizeDataValue(tId, "taxonName", epithet);
			if (!nEpithets.matches("[A-Z][a-z]{2,}(\\-[A-Z][a-z]{2,})?")) {
				System.out.println("Invalid " + tRank + " epithet '" + nEpithets + "' in " + tLine);
				if ((problemEpithets != null) && recordProblems) {
					problemEpithets.add(tRank + " " + nEpithets);
					addProblem(problemRecords, tLine, ("invalid " + tRank + " '" + nEpithets + "'"));
				}
			}
		}
		boolean normalizeGenus = false;
		if (genusRankLevel <= tRankLevel) {
			if ((tData.length <= metaXml.genusNameIndex) || (tData[metaXml.genusNameIndex] == null)) {
				if (speciesRankLevel <= tRankLevel)
					System.out.println("Missing " + GENUS_ATTRIBUTE + " epithet in " + tLine);
				if (clean && (genusRankLevel < tRankLevel) && (epithetPrefix.length() != 0)) {
					if (tData.length <= metaXml.genusNameIndex) {
						String[] etData = new String[metaXml.genusNameIndex + 1];
						System.arraycopy(tData, 0, etData, 0, tData.length);
						tData = etData;
					}
					if (epithetPrefix.indexOf(" ") != -1)
						tData[metaXml.genusNameIndex] = epithetPrefix.substring(0, epithetPrefix.indexOf(" ")).trim();
					else tData[metaXml.genusNameIndex] = epithetPrefix;
					return normalizeValues(metaXml, valueNormalizer, tLine, tData, clean, problemChars, problemRanks, problemEpithets, brokenYearAuthorities, brokenStartAuthorities, longAuthorities, problemRecords, ignoreSynonymProblems);
				}
				else if (clean && (genusRankLevel == tRankLevel) && (epithet.length() != 0)) {
					if (tData.length <= metaXml.genusNameIndex) {
						String[] etData = new String[metaXml.genusNameIndex + 1];
						System.arraycopy(tData, 0, etData, 0, tData.length);
						tData = etData;
					}
					tData[metaXml.genusNameIndex] = epithet;
					return normalizeValues(metaXml, valueNormalizer, tLine, tData, clean, problemChars, problemRanks, problemEpithets, brokenYearAuthorities, brokenStartAuthorities, longAuthorities, problemRecords, ignoreSynonymProblems);
				}
			}
			else {
				String nEpithet = valueNormalizer.normalizeDataValue(tId, "genericName", tData[metaXml.genusNameIndex]);
				if (!nEpithet.matches("[A-Z][a-z]{2,}(\\-[A-Za-z][a-z]{2,})?")) {
					System.out.println("Invalid " + GENUS_ATTRIBUTE + " epithet '" + nEpithet + "' in " + tLine);
					if ((problemEpithets != null) && recordProblems) {
						problemEpithets.add(GENUS_ATTRIBUTE + " " + nEpithet);
						addProblem(problemRecords, tLine, ("invalid " + GENUS_ATTRIBUTE + " '" + nEpithet + "'"));
					}
					normalizeGenus = true;
				}
				if (!tName.startsWith(nEpithet)) {
					System.out.println("Unmatched " + GENUS_ATTRIBUTE + " epithet '" + nEpithet + "' in name '" + tName + "' of " + tLine);
					if ((problemEpithets != null) && recordProblems) {
						problemEpithets.add(GENUS_ATTRIBUTE + " " + nEpithet);
						addProblem(problemRecords, tLine, ("invalid " + GENUS_ATTRIBUTE + " '" + nEpithet + "'"));
					}
				}
			}
		}
		boolean normalizeSpecies = false;
		if (speciesRankLevel <= tRankLevel) {
			if ((tData.length <= metaXml.speciesNameIndex) || (tData[metaXml.speciesNameIndex] == null)) {
				System.out.println("Missing " + SPECIES_ATTRIBUTE + " epithet in " + tLine);
				if (clean) {
					Matcher em = speciesEpithetPattern.matcher(tName); // should work in the few cases epithet is empty
					if (em.find()) {
						if (tData.length <= metaXml.speciesNameIndex) {
							String[] etData = new String[metaXml.speciesNameIndex + 1];
							System.arraycopy(tData, 0, etData, 0, tData.length);
							tData = etData;
						}
						tData[metaXml.speciesNameIndex] = em.group().trim();
						return normalizeValues(metaXml, valueNormalizer, tLine, tData, clean, problemChars, problemRanks, problemEpithets, brokenYearAuthorities, brokenStartAuthorities, longAuthorities, problemRecords, ignoreSynonymProblems);
					}
					else return null;
				}
			}
			else {
				String nEpithet = valueNormalizer.normalizeDataValue(tId, "specificEpithet", tData[metaXml.speciesNameIndex]);
				if (nEpithet.matches("([a-z]+\\')?[a-z]{3,}(\\-[a-z]{3,})?")) {}
				else if (nEpithet.matches("[a-z]+\\-[a-z]+(\\-[a-z]{3,}){0,2}")) {}
				else if (nEpithet.matches("[1-9][0-9]?\\-[a-z]{3,}(\\-[a-z]{3,})?")) {}
				else {
					System.out.println("Invalid " + SPECIES_ATTRIBUTE + " epithet '" + nEpithet + "' in " + tLine);
					if ((nEpithet.indexOf(" var. ") != -1) || (nEpithet.indexOf(" var ") != -1))
						System.out.println(VARIETY_ATTRIBUTE + " label in " + SPECIES_ATTRIBUTE + " epithet '" + nEpithet + "' in " + tLine);
					if ((nEpithet.indexOf(" f. ") != -1) || (nEpithet.indexOf(" f ") != -1))
						System.out.println(FORM_ATTRIBUTE + " label in " + SPECIES_ATTRIBUTE + " epithet '" + nEpithet + "' in " + tLine);
					if ((problemEpithets != null) && recordProblems) {
						problemEpithets.add(SPECIES_ATTRIBUTE + " " + nEpithet);
						addProblem(problemRecords, tLine, ("invalid " + SPECIES_ATTRIBUTE + " '" + nEpithet + "'"));
					}
					normalizeSpecies = true;
				}
				if (tName.indexOf(nEpithet) == -1) {
					System.out.println("Unmatched " + SPECIES_ATTRIBUTE + " epithet '" + nEpithet + "' in name '" + tName + "' of " + tLine);
					if ((problemEpithets != null) && recordProblems) {
						problemEpithets.add(SPECIES_ATTRIBUTE + " " + nEpithet);
						addProblem(problemRecords, tLine, ("invalid " + SPECIES_ATTRIBUTE + " '" + nEpithet + "'"));
					}
				}
			}
		}
		boolean normalizeBelowSpecies = false;
		if (speciesRankLevel < tRankLevel) {
			if ((tData.length <= metaXml.belowSpeciesNameIndex) || (tData[metaXml.belowSpeciesNameIndex] == null)) {
				System.out.println("Missing " + tRank + " epithet in " + tLine);
				if (clean)
					return null;
			}
			else {
				String nEpithet = valueNormalizer.normalizeDataValue(tId, "infraspecificEpithet", tData[metaXml.belowSpeciesNameIndex]);
				if (nEpithet.matches("[a-z]{3,}(\\-[a-z]{3,})?")) {}
				else if (nEpithet.matches("[a-z]+\\-[a-z]{3,}(\\-[a-z]{3,})?")) {}
				else if (nEpithet.matches("[1-9][0-9]?\\-[a-z]{3,}(\\-[a-z]{3,})?")) {}
				else {
					System.out.println("Invalid " + tRank + " epithet '" + nEpithet + "' in " + tLine);
					if ((problemEpithets != null) && recordProblems) {
						problemEpithets.add(tRank + " " + nEpithet);
						addProblem(problemRecords, tLine, ("invalid " + tRank + " '" + nEpithet + "'"));
					}
					normalizeBelowSpecies = true;
				}
				if (tName.indexOf(nEpithet) == -1) {
					System.out.println("Unmatched " + tRank + " epithet '" + nEpithet + "' in name '" + tName + "' of " + tLine);
					if ((problemEpithets != null) && recordProblems) {
						problemEpithets.add(tRank + " " + nEpithet);
						addProblem(problemRecords, tLine, ("invalid " + tRank + " '" + nEpithet + "'"));
					}
				}
			}
		}
		
		//	check authority separately
		if (authority.length() != 0) {
			String nAuthority = valueNormalizer.normalizeDataValue(tId, "taxonAuthority", authority);
			
			for (Matcher nm = numberPattern.matcher(nAuthority); nm.find();) {
				if (nm.end() - nm.start() == 4)
					continue;
				System.out.println("Invalid year '" + nm.group() + "' at " + nm.start() + " in authority '" + authority + "' in " + tLine);
				if ((brokenYearAuthorities != null) && recordProblems) {
					brokenYearAuthorities.add(authority);
					addProblem(problemRecords, tLine, ("broken authority year in '" + authority + "'"));
				}
			}
			
			if (nAuthority.length() == 0) {}
			else if (nAuthority.matches("\\(?[A-Z].*")) {}
			else if (nAuthority.matches("\\(?[a-z]\\'[A-Z].*")) {}
			else if (nAuthority.matches("\\(?v\\.\\s*[A-Z].*")) {}
			else if (nAuthority.matches("ex\\s+[A-Z].*")) {}
			else if (nAuthority.matches("hort\\.\\s*ex\\s+[A-Z].*")) {}
			else if (nAuthority.matches("\\(?(ex\\s+)?(da|de|del|della|delle|dem|den|der|des|di|do|dos|du|(d\\')|(de\\s+l\\')|el|la|le|les|nec|non|not|of|(\\'s)|(\\'t)|ten|ter|van|vanden|vander|vom|von|vonden|vonder|v\\.zu|zu|zum|zur)(\\-?|\\s*)[A-Z].*")) {}
			else if (nAuthority.matches("\\(?(ex\\s+)?((da|de|del|della|delle|dem|den|der|des|di|do|dos|du|(d\\')|(de\\s+l\\')|el|la|le|les|nec|non|not|of|(\\'s)|(\\'t)|ten|ter|van|vanden|vander|vom|von|vonden|vonder|v\\.zu|zu|zum|zur)\\s*){0,2}[A-Z].*")) {}
			else if (nAuthority.matches("\\(?\\'t\\s+[A-Z].*")) {}
			else if (nAuthority.matches("[a-z]{2,}\\,\\s*[12][0-9]{3}"))
				nAuthority = (nAuthority.substring(0, 1).toUpperCase() + nAuthority.substring(1)); // simply capitalize single last name if accompanied by year
			else {
				System.out.println("Strange authority start '" + nAuthority + "' in " + tLine);
//				System.out.println("               tName is '" + tName + "'");
				if ((brokenStartAuthorities != null) && recordProblems) {
					brokenStartAuthorities.add(nAuthority);
					addProblem(problemRecords, tLine, ("broken authority start in '" + authority + "'"));
				}
			}
			
			if (nAuthority.length() > 64) {
				System.out.println("Long authority (" + nAuthority.length() + ") '" + nAuthority + "' in " + tLine);
//				System.out.println("               tName is '" + tName + "'");
				if (longAuthorities != null)
					longAuthorities.add(nAuthority);
			}
			
			if (clean) {
				if (tRankLevel <= genusRankLevel)
					tName = (epithet + " " + nAuthority).trim();
				else if (epithetPrefix.length() == 0)
					tName = (epithet + " " + nAuthority).trim();
				else if (SUBGENUS_ATTRIBUTE.equals(tRank))
					tName = (epithetPrefix + " (" + epithet + ") " + nAuthority).trim();
				else if (SECTION_ATTRIBUTE.equals(tRank))
					tName = (epithetPrefix + " (sect. " + epithet + ") " + nAuthority).trim();
				else if (SUBSECTION_ATTRIBUTE.equals(tRank))
					tName = (epithetPrefix + " (subsect. " + epithet + ") " + nAuthority).trim();
				else if (SERIES_ATTRIBUTE.equals(tRank))
					tName = (epithetPrefix + " (ser. " + epithet + ") " + nAuthority).trim();
				else if (SUBSERIES_ATTRIBUTE.equals(tRank))
					tName = (epithetPrefix + " (subser. " + epithet + ") " + nAuthority).trim();
				else tName = (epithetPrefix + " " + epithet + " " + nAuthority).trim();
				tData[metaXml.taxonNameIndex] = tName;
			}
		}
		
		//	no use cleaning anything
		if (!clean)
			return tData;
		
		//	clean up below-species epithets containing two epithets
		if (normalizeGenus) {
			String genus = tData[metaXml.genusNameIndex];
			String nGenus = genus.replaceAll("[^A-Za-z\\-\\s]", "");
			nGenus = (nGenus.substring(0,  1).toUpperCase() + nGenus.substring(1).toLowerCase());
			tData[metaXml.genusNameIndex] = nGenus;
			if (tName.indexOf(genus) != -1) {
				tName = (tName.substring(0, tName.indexOf(genus)) + nGenus + tName.substring(tName.indexOf(genus) + genus.length()));
				tData[metaXml.taxonNameIndex] = tName;
			}
		}
		if (normalizeSpecies) {
			String species = tData[metaXml.speciesNameIndex];
			String nSpecies = species.replaceAll("[^A-Za-z0-9\\-\\s]", "");
			nSpecies = nSpecies.toLowerCase();
			nSpecies = species.replaceAll("\\s+", "-");
			nSpecies = species.replaceAll("\\-{2,}", "-");
			if (nSpecies.matches("[1-9][a-z].*"))
				nSpecies = (nSpecies.substring(0, "0".length()) + "-" + nSpecies.substring("0".length()));
			if (nSpecies.matches("[1-9][0-9][a-z].*"))
				nSpecies = (nSpecies.substring(0, "00".length()) + "-" + nSpecies.substring("00".length()));
			tData[metaXml.speciesNameIndex] = nSpecies;
			if (tName.indexOf(species) != -1) {
				tName = (tName.substring(0, tName.indexOf(species)) + nSpecies + tName.substring(tName.indexOf(species) + species.length()));
				tData[metaXml.taxonNameIndex] = tName;
			}
		}
		while (normalizeBelowSpecies) {
			String belowSpecies = tData[metaXml.belowSpeciesNameIndex];
			if (belowSpecies.startsWith("var.")) {
				tData[metaXml.belowSpeciesNameIndex] = belowSpecies.substring("var.".length()).trim();
				tData[metaXml.taxonRankIndex] = VARIETY_ATTRIBUTE;
				continue;
			}
			if (belowSpecies.indexOf(" var.") != -1) {
				tData[metaXml.belowSpeciesNameIndex] = belowSpecies.substring(belowSpecies.indexOf(" var.") + " var.".length()).trim();
				tData[metaXml.taxonRankIndex] = VARIETY_ATTRIBUTE;
				continue;
			}
			if (belowSpecies.startsWith("f.")) {
				tData[metaXml.belowSpeciesNameIndex] = belowSpecies.substring("f.".length()).trim();
				tData[metaXml.taxonRankIndex] = FORM_ATTRIBUTE;
				continue;
			}
			if (belowSpecies.indexOf(" f.") != -1) {
				tData[metaXml.belowSpeciesNameIndex] = belowSpecies.substring(belowSpecies.indexOf(" f.") + " f.".length()).trim();
				tData[metaXml.taxonRankIndex] = FORM_ATTRIBUTE;
				continue;
			}
			String nBelowSpecies = belowSpecies.replaceAll("[^A-Za-z0-9\\-\\s]", "");
			nBelowSpecies = nBelowSpecies.toLowerCase();
			nBelowSpecies = belowSpecies.replaceAll("\\s+", "-");
			nBelowSpecies = belowSpecies.replaceAll("\\-{2,}", "-");
			if (nBelowSpecies.matches("[1-9][a-z].*"))
				nBelowSpecies = (nBelowSpecies.substring(0, "0".length()) + "-" + nBelowSpecies.substring("0".length()));
			if (nBelowSpecies.matches("[1-9][0-9][a-z].*"))
				nBelowSpecies = (nBelowSpecies.substring(0, "00".length()) + "-" + nBelowSpecies.substring("00".length()));
			tData[metaXml.belowSpeciesNameIndex] = nBelowSpecies;
			if (tName.lastIndexOf(belowSpecies) != -1) {
				tName = (tName.substring(0, tName.lastIndexOf(belowSpecies)) + nBelowSpecies + tName.substring(tName.lastIndexOf(belowSpecies) + belowSpecies.length()));
				tData[metaXml.taxonNameIndex] = tName;
			}
			break;
		}
		
		//	finally ...
		return tData;
	}
	
	private static boolean recordProblems(String[] tData, MetaXml metaXml, boolean ignoreSynonymProblems) {
		if (ignoreSynonymProblems) {
			if (tData.length < metaXml.taxonStatusIndex)
				return true;
			else if ("synonym".equals(tData[metaXml.taxonStatusIndex]))
				return false;
			else if (tData[metaXml.taxonStatusIndex] == null)
				return true;
			else if (tData[metaXml.taxonStatusIndex].endsWith(" synonym"))
				return false;
			else return true;
		}
		else return true;
	}
	
	private static void addProblem(LinkedHashMap problemRecords, String tLine, String problem) {
		if (problemRecords == null)
			return;
		LinkedHashSet problems = ((LinkedHashSet) problemRecords.get(tLine));
		if (problems == null) {
			problems = new LinkedHashSet();
			problemRecords.put(tLine, problems);
		}
		problems.add(problem);
	}
	
	//	returns <prefixEnd><sigEpithetStart><sigEpithetEnd><authorityStart> (4 bytes)
	private static int segmentTaxonName(String tName, int tRankLevel, MetaXml metaXml, String[] tData) {
		String tRank = levelsToRanks[tRankLevel];
		String epithetPrefix;
		String epithet;
		String authority;
		String authorityAfter = null;
		if ((metaXml.belowSpeciesNameIndex < tData.length) && (tData[metaXml.belowSpeciesNameIndex] != null))
			authorityAfter = tData[metaXml.belowSpeciesNameIndex];
		else if ((metaXml.speciesNameIndex < tData.length) && (tData[metaXml.speciesNameIndex] != null))
			authorityAfter = tData[metaXml.speciesNameIndex];
		if ((authorityAfter != null) && (tName.indexOf(" " + authorityAfter) != -1)) {
			epithetPrefix = tName.substring(0, tName.lastIndexOf(" " + authorityAfter)).trim();
			if (epithetPrefix.endsWith(" subsp."))
				epithetPrefix = epithetPrefix.substring(0, (epithetPrefix.length() - " subsp.".length())).trim();
			else if (epithetPrefix.endsWith(" var."))
				epithetPrefix = epithetPrefix.substring(0, (epithetPrefix.length() - " var.".length())).trim();
			else if (epithetPrefix.endsWith(" subvar."))
				epithetPrefix = epithetPrefix.substring(0, (epithetPrefix.length() - " subvar.".length())).trim();
			else if (epithetPrefix.endsWith(" f."))
				epithetPrefix = epithetPrefix.substring(0, (epithetPrefix.length() - " f.".length())).trim();
			else if (epithetPrefix.endsWith(" subf."))
				epithetPrefix = epithetPrefix.substring(0, (epithetPrefix.length() - " subf.".length())).trim();
			epithet = authorityAfter;
			authority = tName.substring(tName.lastIndexOf(" " + authorityAfter) + " ".length() + authorityAfter.length()).trim();
		}
		else if ((tRankLevel <= genusRankLevel) && (tName.indexOf(" ") != -1)) {
			epithetPrefix = "";
			epithet = tName.substring(0, tName.indexOf(" ")).trim();
			authority = tName.substring(tName.indexOf(" ") + " ".length()).trim();
		}
		else if (SUBGENUS_ATTRIBUTE.equals(tRank)) {
			if ((tName.indexOf('(') != -1) && (tName.indexOf('(') < tName.indexOf(')'))) {
				epithetPrefix = tName.substring(0, tName.indexOf("(")).trim();
				epithet = tName.substring((tName.indexOf('(') + "(".length()), tName.indexOf(')')).trim();
				authority = tName.substring(tName.indexOf(')') + ")".length()).trim();
			}
			else if (tName.indexOf("subgen.") != -1) {
				epithetPrefix = tName.substring(0, tName.indexOf("subgen.")).trim();
				epithet = tName.substring(tName.indexOf("subgen.") + "subgen.".length()).trim();
				if (epithet.indexOf(' ') == -1)
					authority = "";
				else {
					authority = epithet.substring(epithet.indexOf(' ')).trim();
					epithet = epithet.substring(0, epithet.indexOf(' ')).trim();
				}
			}
			else if (tName.indexOf(" ") != -1) {
				epithetPrefix = "";
				epithet = tName.substring(0, tName.indexOf(" ")).trim();
				authority = tName.substring(tName.indexOf(" ") + " ".length()).trim();
			}
			else {
				epithetPrefix = "";
				epithet = tName;
				authority = "";
			}
		}
		else if (SECTION_ATTRIBUTE.equals(tRank)) {
			if ((tName.indexOf("(sect.") != -1) && (tName.indexOf(")") != -1)) {
				epithetPrefix = tName.substring(0, tName.indexOf("(sect.")).trim();
				epithet = tName.substring(tName.indexOf("(sect.") + "(sect.".length()).trim();
				if (epithet.indexOf(" ") == -1)
					authority = "";
				else {
					authority = epithet.substring(epithet.indexOf(' ')).trim();
					epithet = epithet.substring(0, epithet.indexOf(' ')).trim();
				}
				if (epithet.endsWith(")"))
					epithet = epithet.substring(0, (epithet.length() - ")".length())).trim();
			}
			else if (tName.indexOf("sect.") != -1) {
				epithetPrefix = tName.substring(0, tName.indexOf("sect.")).trim();
				epithet = tName.substring(tName.indexOf("sect.") + "sect.".length()).trim();
				if (epithet.indexOf(' ') == -1)
					authority = "";
				else {
					authority = epithet.substring(epithet.indexOf(' ')).trim();
					epithet = epithet.substring(0, epithet.indexOf(' ')).trim();
				}
			}
			else if (tName.indexOf(" ") != -1) {
				epithetPrefix = "";
				epithet = tName.substring(0, tName.indexOf(" ")).trim();
				authority = tName.substring(tName.indexOf(" ") + " ".length()).trim();
			}
			else {
				epithetPrefix = "";
				epithet = tName;
				authority = "";
			}
		}
		else if (SUBSECTION_ATTRIBUTE.equals(tRank)) {
			if ((tName.indexOf("(subsect.") != -1) && (tName.indexOf(")") != -1)) {
				epithetPrefix = tName.substring(0, tName.indexOf("(subsect.")).trim();
				epithet = tName.substring(tName.indexOf("(subsect.") + "(subsect.".length()).trim();
				if (epithet.indexOf(" ") == -1)
					authority = "";
				else {
					authority = epithet.substring(epithet.indexOf(' ')).trim();
					epithet = epithet.substring(0, epithet.indexOf(' ')).trim();
				}
				if (epithet.endsWith(")"))
					epithet = epithet.substring(0, (epithet.length() - ")".length())).trim();
			}
			else if (tName.indexOf("subsect.") != -1) {
				epithetPrefix = tName.substring(0, tName.indexOf("subsect.")).trim();
				epithet = tName.substring(tName.indexOf("subsect.") + "subsect.".length()).trim();
				if (epithet.indexOf(' ') == -1)
					authority = "";
				else {
					authority = epithet.substring(epithet.indexOf(' ')).trim();
					epithet = epithet.substring(0, epithet.indexOf(' ')).trim();
				}
			}
			else if (tName.indexOf(" ") != -1) {
				epithetPrefix = "";
				epithet = tName.substring(0, tName.indexOf(" ")).trim();
				authority = tName.substring(tName.indexOf(" ") + " ".length()).trim();
			}
			else {
				epithetPrefix = "";
				epithet = tName;
				authority = "";
			}
		}
		else if (SERIES_ATTRIBUTE.equals(tRank)) {
			if ((tName.indexOf("(ser.") != -1) && (tName.indexOf(")") != -1)) {
				epithetPrefix = tName.substring(0, tName.indexOf("(ser.")).trim();
				epithet = tName.substring(tName.indexOf("(ser.") + "(ser.".length()).trim();
				if (epithet.indexOf(" ") == -1)
					authority = "";
				else {
					authority = epithet.substring(epithet.indexOf(' ')).trim();
					epithet = epithet.substring(0, epithet.indexOf(' ')).trim();
				}
				if (epithet.endsWith(")"))
					epithet = epithet.substring(0, (epithet.length() - ")".length())).trim();
			}
			else if (tName.indexOf("ser.") != -1) {
				epithetPrefix = tName.substring(0, tName.indexOf("ser.")).trim();
				epithet = tName.substring(tName.indexOf("ser.") + "ser.".length()).trim();
				if (epithet.indexOf(' ') == -1)
					authority = "";
				else {
					authority = epithet.substring(epithet.indexOf(' ')).trim();
					epithet = epithet.substring(0, epithet.indexOf(' ')).trim();
				}
			}
			else if (tName.indexOf(" ") != -1) {
				epithetPrefix = "";
				epithet = tName.substring(0, tName.indexOf(" ")).trim();
				authority = tName.substring(tName.indexOf(" ") + " ".length()).trim();
			}
			else {
				epithetPrefix = "";
				epithet = tName;
				authority = "";
			}
		}
		else if (SUBSERIES_ATTRIBUTE.equals(tRank)) {
			if ((tName.indexOf("(subser.") != -1) && (tName.indexOf(")") != -1)) {
				epithetPrefix = tName.substring(0, tName.indexOf("(subser.")).trim();
				epithet = tName.substring(tName.indexOf("(subser.") + "(subser.".length()).trim();
				if (epithet.indexOf(" ") == -1)
					authority = "";
				else {
					authority = epithet.substring(epithet.indexOf(' ')).trim();
					epithet = epithet.substring(0, epithet.indexOf(' ')).trim();
				}
				if (epithet.endsWith(")"))
					epithet = epithet.substring(0, (epithet.length() - ")".length())).trim();
			}
			else if (tName.indexOf("subser.") != -1) {
				epithetPrefix = tName.substring(0, tName.indexOf("subser.")).trim();
				epithet = tName.substring(tName.indexOf("subser.") + "subser.".length()).trim();
				if (epithet.indexOf(' ') == -1)
					authority = "";
				else {
					authority = epithet.substring(epithet.indexOf(' ')).trim();
					epithet = epithet.substring(0, epithet.indexOf(' ')).trim();
				}
			}
			else if (tName.indexOf(" ") != -1) {
				epithetPrefix = "";
				epithet = tName.substring(0, tName.indexOf(" ")).trim();
				authority = tName.substring(tName.indexOf(" ") + " ".length()).trim();
			}
			else {
				epithetPrefix = "";
				epithet = tName;
				authority = "";
			}
		}
		else {
			epithetPrefix = "";
			epithet = tName;
			authority = "";
		}
		
		int prefixEnd = epithetPrefix.length();
		int epithetStart = tName.indexOf(epithet, prefixEnd);
		int epithetEnd = (epithetStart + epithet.length());
		int authorityStart = (tName.length() - authority.length());
		return ((prefixEnd << 24) | (epithetStart << 16) | (epithetEnd << 8) | authorityStart);
	}
	
	private static void clean(String dwcaFolder) throws Exception {
		clean(new File(dwcaFolder));
	}
	private static void clean(File dwcaFolder) throws Exception {
		MetaXml metaXml = getMetaXml(dwcaFolder);
		DataValueNormalizer valueNormalizer = getDataValueNormalizer(dwcaFolder);
		
		//	scan Taxon.tsv line by line
		File taxonTsv = new File(dwcaFolder, "Taxon.tsv");
		BufferedReader tBr = new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(taxonTsv)), "UTF-8"));
		File taxonCleanTsv = new File(dwcaFolder, "Taxon.clean.tsv");
		BufferedWriter tcBw = new BufferedWriter(new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(taxonCleanTsv)), "UTF-8"));
		tcBw.write(tBr.readLine()); // skip column headers
		tcBw.newLine();
		int tLineCount = 0;
		for (String tLine; (tLine = tBr.readLine()) != null;) {
			tLineCount++;
			if ((tLineCount % 5000) == 0) {
				System.out.println("Read " + tLineCount + " lines of data so far");
				if ((tLineCount % 100000) == 0)
					System.gc();
			}
			
			//	parse data
			String[] tData = tLine.split("\\t");
			if (tData.length <= metaXml.taxonIdIndex)
				continue;
			
			//	check filter
			String tId = tData[metaXml.taxonIdIndex];
			if (valueNormalizer.removeTaxon(tId)) {
				String[] rtData = new String[tData.length];
				rtData[metaXml.taxonIdIndex] = tData[metaXml.taxonIdIndex];
				rtData[metaXml.parentTaxonIdIndex] = tData[metaXml.parentTaxonIdIndex];
				rtData[metaXml.validTaxonIdIndex] = tData[metaXml.validTaxonIdIndex];
				rtData[metaXml.taxonStatusIndex] = "REMOVED TAXON";
				tData = rtData;
			}
			
			//	sanitize data
			else {
				String[] ctData = normalizeValues(metaXml, valueNormalizer, tLine, tData, true, null, null, null, null, null, null, null, false);
				if (ctData == null) {
					String[] rtData = new String[tData.length];
					rtData[metaXml.taxonIdIndex] = tData[metaXml.taxonIdIndex];
					rtData[metaXml.parentTaxonIdIndex] = tData[metaXml.parentTaxonIdIndex];
					rtData[metaXml.validTaxonIdIndex] = tData[metaXml.validTaxonIdIndex];
					rtData[metaXml.taxonStatusIndex] = "REMOVED TAXON";
					tData = rtData;
				}
				else tData = ctData;
			}
			
			//	store sanitized data
			StringBuffer tcLine = new StringBuffer(tData[0]);
			for (int f = 1; f < tData.length; f++) {
				tcLine.append("\t");
				tcLine.append((tData[f] == null) ? "" : tData[f]);
			}
			tcBw.write(tcLine.toString());
			tcBw.newLine();
		}
		tBr.close();
		tcBw.flush();
		tcBw.close();
	}
	
	private static void extract(String dwcaFolder) throws Exception {
		extract(new File(dwcaFolder));
	}
	private static void extract(File dwcaFolder) throws Exception {
		MetaXml metaXml = getMetaXml(dwcaFolder);
		
		//	scan Taxon.tsv line by line
		File taxonTsv = new File(dwcaFolder, "Taxon.tsv");
		BufferedReader tBr = new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(taxonTsv)), "UTF-8"));
		CountingSet references = new CountingSet(new TreeMap(String.CASE_INSENSITIVE_ORDER));
		CountingSet authorities = new CountingSet(new TreeMap(String.CASE_INSENSITIVE_ORDER));
		tBr.readLine(); // skip column headers
		int tLineCount = 0;
		for (String tLine; (tLine = tBr.readLine()) != null;) {
			tLineCount++;
			if ((tLineCount % 5000) == 0) {
				System.out.println("Read " + tLineCount + " lines of data so far");
				if ((tLineCount % 100000) == 0)
					System.gc();
			}
			String[] tData = tLine.split("\\t");
			
			//	extract reference
			if ((metaXml.publishedInIndex < tData.length) && (tData[metaXml.publishedInIndex] != null)) {
				String tRef = tData[metaXml.publishedInIndex].trim();
				if (tRef.length() != 0)
					references.add(tRef);
			}
			
			//	extract authority
			if ((metaXml.speciesNameIndex < tData.length) && (tData[metaXml.publishedInIndex] != null)) {
				String tRank = ((String) ranksToNormalForms.get(tData[metaXml.taxonRankIndex]));
				if (SPECIES_ATTRIBUTE.equals(tRank)) {
					String tName = tData[metaXml.taxonNameIndex];
					String tSpecies = tData[metaXml.speciesNameIndex];
					if (tName.indexOf(" " + tSpecies + " ") != -1) {
						String tAuth = tName.substring(tName.lastIndexOf(" " + tSpecies + " ") + " ".length() + tSpecies.length() + " ".length()).trim();
						if (tAuth.length() != 0)
							authorities.add(tAuth);
					}
				}
			}
		}
		tBr.close();
		
		//	store references
		File referencesTsv = new File(dwcaFolder, "References.tsv");
		BufferedWriter rBw = new BufferedWriter(new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(referencesTsv)), "UTF-8"));
		for (Iterator rit = references.iterator(); rit.hasNext();) {
			String tRef = ((String) rit.next());
			rBw.write(tRef + "\t" + references.getCount(tRef));
			rBw.newLine();
		}
		rBw.flush();
		rBw.close();
		
		//	store authorities
		File authorsTsv = new File(dwcaFolder, "Authorities.tsv");
		BufferedWriter aBw = new BufferedWriter(new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(authorsTsv)), "UTF-8"));
		for (Iterator ait = authorities.iterator(); ait.hasNext();) {
			String tAuth = ((String) ait.next());
			aBw.write(tAuth + "\t" + authorities.getCount(tAuth));
			aBw.newLine();
		}
		aBw.flush();
		aBw.close();
	}
	
	private static class TaxonData {
		final String[] data;
		final String colId;
		String epithet;
		String epithetPrefix;
		String rank;
		int rankLevel;
		boolean isExtant = false;
		String authority;
		TaxonData parent;
		LinkedHashSet children = null;
		TaxonData primaryParent;
		LinkedHashSet primaryChildren = null;
		TaxonData validTaxon = null;
		LinkedHashSet synonyms = null;
		int dataId = 0;
		String clusterName;
		TaxonData(String[] data, String colId, String epithet, String rank, int rankLevel, String authority) {
			this.data = data;
			this.colId = colId;
			this.epithet = epithet;
			this.rank = rank;
			this.rankLevel = rankLevel;
			this.authority = authority;
		}
		void addChild(TaxonData child) {
			if (this.children == null)
				this.children = new LinkedHashSet();
			this.children.add(child);
		}
		void addPrimaryChild(TaxonData child) {
			if (this.primaryChildren == null)
				this.primaryChildren = new LinkedHashSet();
			this.primaryChildren.add(child);
		}
		void addSynonym(TaxonData synonym) {
			if (this.synonyms == null)
				this.synonyms = new LinkedHashSet();
			this.synonyms.add(synonym);
		}
		int minDescendantRankLevel = -1;
		int getMinimumDescendantRankLevel() {
			if (this.minDescendantRankLevel != -1)
				return this.minDescendantRankLevel;
			this.minDescendantRankLevel = this.rankLevel;
			if (this.synonyms != null)
				for (Iterator sit = this.synonyms.iterator(); sit.hasNext();) {
					TaxonData td = ((TaxonData) sit.next());
					this.minDescendantRankLevel = Math.min(this.minDescendantRankLevel, td.rankLevel);
				}
			if (this.children != null)
				for (Iterator cit = this.children.iterator(); cit.hasNext();) {
					TaxonData td = ((TaxonData) cit.next());
					this.minDescendantRankLevel = Math.min(this.minDescendantRankLevel, td.getMinimumDescendantRankLevel());
				}
			return this.minDescendantRankLevel;
		}
		int setDataId(int dataId, int rankLevel, String clusterName, HashMap taxonDatasByIDs) {
			if (rankLevel < this.getMinimumDescendantRankLevel())
				return dataId; // not our turn just yet (and no potentially higher-up synonyms)
			if ((rankLevel == this.rankLevel) && (this.dataId < 1)) {
				// we assign IDs for taxa in species tiles from tile roots, which can be nested
				this.dataId = dataId++; // use this data ID and switch to next one in line
				this.clusterName = clusterName;
				taxonDatasByIDs.put(new Integer(this.dataId), this);
			}
			if (this.synonyms != null) // descend to synonyms (might have been degraded from higher-up ranks !!!)
				for (Iterator sit = this.synonyms.iterator(); sit.hasNext();) {
					TaxonData td = ((TaxonData) sit.next());
					dataId = td.setDataId(dataId, rankLevel, clusterName, taxonDatasByIDs);
				}
			if (this.children != null) // descend to children
				for (Iterator cit = this.children.iterator(); cit.hasNext();) {
					TaxonData td = ((TaxonData) cit.next());
					dataId = td.setDataId(dataId, rankLevel, clusterName, taxonDatasByIDs);
				}
			return dataId;
		}
		private int subTreeSize = -1;
		private int subTreeSizeSpecies = -1;
		int getSubTreeSize(boolean countAboveSpecies) {
			if (countAboveSpecies && this.subTreeSize != -1)
				return this.subTreeSize;
			if (!countAboveSpecies && (this.subTreeSizeSpecies != -1))
				return this.subTreeSizeSpecies;
			int subTreeSize = 0;
			int subTreeSizeSpecies = 0;
			ArrayList nodes = new ArrayList();
			nodes.add(this);
			for (int n = 0; n < nodes.size(); n++) {
				TaxonData node = ((TaxonData) nodes.get(n));
				subTreeSize++;
				if (countAboveSpecies || (speciesRankLevel <= node.rankLevel))
					subTreeSizeSpecies++;
				if (node.children != null) {
					for (Iterator cit = node.children.iterator(); cit.hasNext();) {
						TaxonData cNode = ((TaxonData) cit.next());
						if ((cNode.children == null) && (cNode.synonyms == null)) {
							subTreeSize++;
							if (speciesRankLevel <= cNode.rankLevel)
								subTreeSizeSpecies++;
						}
						else nodes.add(cNode);
					}
				}
				if (node.synonyms != null) {
					subTreeSize += node.synonyms.size();
					subTreeSizeSpecies += node.synonyms.size();
				}
			}
			this.subTreeSize = subTreeSize;
			this.subTreeSizeSpecies = subTreeSizeSpecies;
			return (countAboveSpecies ? this.subTreeSize : this.subTreeSizeSpecies);
		}
		public String toString() {
			return (this.rank + " " + this.epithet);
		}
	}
	
	private static void analyzeTaxon(String dwcaFolder) throws Exception {
		analyzeTaxon(new File(dwcaFolder));
	}
	private static void analyzeTaxon(File dwcaFolder) throws Exception {
		
		//	load data
		LinkedHashMap taxonDataById = getTaxonDataById(dwcaFolder);
		
		//	count out tiles
		TreeMap kingdomPathsToContentRoots = new TreeMap();
		for (Iterator tidit = taxonDataById.keySet().iterator(); tidit.hasNext();) {
			String tid = ((String) tidit.next());
			TaxonData td = ((TaxonData) taxonDataById.get(tid));
			if (!KINGDOM_ATTRIBUTE.equals(td.rank))
				continue;
			if (td.validTaxon != null)
				continue;
			ArrayList seekTaxonDataList = new ArrayList();
			seekTaxonDataList.add(td);
			for (int t = 0; t < seekTaxonDataList.size(); t++) {
				TaxonData sTd = ((TaxonData) seekTaxonDataList.get(t));
				if (familyRankLevel < sTd.rankLevel)
					continue; // however this one got here ...
				StringBuffer kingdomPath = new StringBuffer("Zzz");
				for (TaxonData pTd = sTd; pTd != null; pTd = pTd.primaryParent) {
					kingdomPath.insert(0, (pTd.epithet + "."));
					String pRank = ((String) primaryRankToParents.get(pTd.rank));
					while ((pRank != null) && (pTd.primaryParent != null) && !pRank.equals(pTd.primaryParent.rank)) {
						kingdomPath.insert(0, ("<" + pRank + ">" + "."));
						pRank = ((String) primaryRankToParents.get(pRank));
					}
				}
				int subTreeSize = sTd.getSubTreeSize(false);
				if (subTreeSize != 0)
					kingdomPathsToContentRoots.put(kingdomPath.toString(), sTd);
				if (subTreeSize < 100000)
					continue;
				if (familyRankLevel <= sTd.rankLevel)
					continue;
				if (sTd.primaryChildren == null)
					continue;
				boolean largeChild = false;
				for (Iterator pcit = sTd.primaryChildren.iterator(); pcit.hasNext();) {
					TaxonData pcTxd = ((TaxonData) pcit.next());
					int pcSubTreeSize = pcTxd.getSubTreeSize(false);
					if ((pcSubTreeSize < 100000) && (subTreeSize < (pcSubTreeSize * 2))) {
						largeChild = true;
						break;
					}
				}
				if (largeChild)
					continue;
				seekTaxonDataList.addAll(sTd.primaryChildren);
			}
		}
		System.out.println("Found " + kingdomPathsToContentRoots.size() + " sub trees sized above 100000 taxa (listed with children):");
		for (Iterator kpit = kingdomPathsToContentRoots.keySet().iterator(); kpit.hasNext();) {
			String kp = ((String) kpit.next());
			System.out.println(kp + " --> " + ((TaxonData) kingdomPathsToContentRoots.get(kp)).getSubTreeSize(false));
		}
	}
	
	private static LinkedHashMap getTaxonDataById(File dwcaFolder) throws Exception {
		MetaXml metaXml = getMetaXml(dwcaFolder);
		DataValueNormalizer valueNormalizer = null;
		
		//	scan Taxon.tsv line by line
		File taxonTsv = new File(dwcaFolder, "Taxon.clean.tsv");
		BufferedReader tBr;
		if (taxonTsv.exists()) {
			tBr = new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(taxonTsv)), "UTF-8"));
			System.out.println("Using Taxon.clean.tsv");
		}
		else {
			valueNormalizer = getDataValueNormalizer(dwcaFolder);
			taxonTsv = new File(dwcaFolder, "Taxon.tsv");
			tBr = new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(taxonTsv)), "UTF-8"));
			System.out.println("Using Taxon.tsv, normalizing on the fly");
		}
		tBr.readLine(); // skip column headers
		int tLineCount = 0;
		LinkedHashMap taxonDataById = new LinkedHashMap();
		LinkedHashMap removedTaxonIDsToParentIDs = new LinkedHashMap();
		for (String tLine; (tLine = tBr.readLine()) != null;) {
			tLineCount++;
			if ((tLineCount % 5000) == 0) {
				System.out.println("Read " + tLineCount + " lines of data so far");
				if ((tLineCount % 100000) == 0)
					System.gc();
			}
			
			//	get data
			String[] tData = tLine.split("\\t");
			
			//	null out empty fields with normalized input
			if (valueNormalizer == null) {
				for (int f = 0; f < tData.length; f++) {
					if ("".equals(tData[f]))
						tData[f] = null;
				}
			}
			
			//	check for filtered taxon ID
			else if (valueNormalizer.removeTaxon(tData[metaXml.taxonIdIndex])) {
				String[] rtData = new String[tData.length];
				rtData[metaXml.taxonIdIndex] = tData[metaXml.taxonIdIndex];
				rtData[metaXml.parentTaxonIdIndex] = tData[metaXml.parentTaxonIdIndex];
				rtData[metaXml.validTaxonIdIndex] = tData[metaXml.validTaxonIdIndex];
				rtData[metaXml.taxonStatusIndex] = "REMOVED TAXON";
				tData = rtData;
			}
			
			//	sanitize data on the fly
			else {
				String[] ctData = normalizeValues(metaXml, valueNormalizer, tLine, tData, true, null, null, null, null, null, null, null, false);
				if (ctData == null) {
					String[] rtData = new String[tData.length];
					rtData[metaXml.taxonIdIndex] = tData[metaXml.taxonIdIndex];
					rtData[metaXml.parentTaxonIdIndex] = tData[metaXml.parentTaxonIdIndex];
					rtData[metaXml.validTaxonIdIndex] = tData[metaXml.validTaxonIdIndex];
					rtData[metaXml.taxonStatusIndex] = "REMOVED TAXON";
					tData = rtData;
				}
				else tData = ctData;
			}
			
			//	check original Base29 taxon ID
			if (!"REMOVED TAXON".equals(tData[metaXml.taxonStatusIndex])) try {
				CatalogOfLifeLocal.parseIntBase29(tData[metaXml.taxonIdIndex]);
			}
			catch (IllegalArgumentException iae) {
				System.out.println("Invalid original Base29 taxon ID '" + tData[metaXml.taxonIdIndex] + "' (" + iae.getMessage() + ") in " + tLine);
				String[] rtData = new String[tData.length];
				rtData[metaXml.taxonIdIndex] = tData[metaXml.taxonIdIndex];
				rtData[metaXml.parentTaxonIdIndex] = tData[metaXml.parentTaxonIdIndex];
				rtData[metaXml.validTaxonIdIndex] = tData[metaXml.validTaxonIdIndex];
				rtData[metaXml.taxonStatusIndex] = "REMOVED TAXON";
				tData = rtData;
			}
			
			//	catch place holders of removed taxa
			if ("REMOVED TAXON".equals(tData[metaXml.taxonStatusIndex])) {
				if (tData[metaXml.parentTaxonIdIndex] == null)
					removedTaxonIDsToParentIDs.put(tData[metaXml.taxonIdIndex], tData[metaXml.validTaxonIdIndex]);
				else removedTaxonIDsToParentIDs.put(tData[metaXml.taxonIdIndex], tData[metaXml.parentTaxonIdIndex]);
				continue;
			}
			
			//	generate tree node ...
			String tName = tData[metaXml.taxonNameIndex];
			int tRankLevel = ((Integer) ranksToLevels.get(tData[metaXml.taxonRankIndex])).intValue();
			int tNameOffsets = segmentTaxonName(tName, tRankLevel, metaXml, tData);
			String tRank = levelsToRanks[tRankLevel];
			String epithetPrefix = tName.substring(0, ((tNameOffsets >>> 24) & 0xFF));
			String epithet = tName.substring(((tNameOffsets >>> 16) & 0xFF), ((tNameOffsets >>> 8) & 0xFF));
			String authority = tName.substring(tNameOffsets & 0xFF);
//			CatalogOfLifeLocal.parseIntBase29(tData[metaXml.taxonIdIndex]);
			TaxonData td = new TaxonData(tData, tData[metaXml.taxonIdIndex], epithet, tRank, tRankLevel, authority);
			td.epithetPrefix = epithetPrefix;
			
			//	... and index by ID
			taxonDataById.put(td.colId, td);
		}
		tBr.close();
		System.out.println("Loaded " + taxonDataById.size() + " taxa, filtered " + removedTaxonIDsToParentIDs.size());
		
		//	link taxa to parents and/or valid taxa
		ArrayList orphanedSubGeneraAndFamilies = new ArrayList();
		ArrayList orphanedSubspecies = new ArrayList();
		LinkedHashSet promotableSubgenera = new LinkedHashSet();
		LinkedHashSet promotableSubspecies = new LinkedHashSet();
		for (Iterator tidit = taxonDataById.keySet().iterator(); tidit.hasNext();) {
			String tid = ((String) tidit.next());
			TaxonData td = ((TaxonData) taxonDataById.get(tid));
			if (KINGDOM_ATTRIBUTE.equals(td.rank)) {} // no parents for kingdoms ...
			else if (td.data[metaXml.parentTaxonIdIndex] != null) {
				String ptid = td.data[metaXml.parentTaxonIdIndex];
				ArrayList trace = new ArrayList();
				while (removedTaxonIDsToParentIDs.containsKey(ptid)) {
					String gptid = ((String) removedTaxonIDsToParentIDs.get(ptid));
					if (ptid.equals(gptid))
						break; // catch potential endless loop
					else {
						trace.add(ptid + " > " + gptid);
						ptid = gptid;
					}
				}
				td.parent = ((TaxonData) taxonDataById.get(ptid));
				if (td.parent == null) {
					System.out.println("Unable to find parent: " + Arrays.toString(td.data));
					if (trace.size() != 0)
						System.out.println("      parent ID trace: " + trace);
				}
				else if (td.rankLevel < td.parent.rankLevel) {
					System.out.println("Inverted rank hierarchy in parent: " + Arrays.toString(td.parent.data));
					System.out.println("                            child: " + Arrays.toString(td.data));
				}
				else if (td.rankLevel == td.parent.rankLevel) {
					System.out.println("Duplicate rank hierarchy in parent: " + Arrays.toString(td.parent.data));
					if (td.parent.data[metaXml.taxonNameIndex].equals(td.data[metaXml.taxonNameIndex]))
						System.out.println("                   duplicate child: " + Arrays.toString(td.data));
					else System.out.println("                             child: " + Arrays.toString(td.data));
					if (SUBSPECIES_ATTRIBUTE.equalsIgnoreCase(td.parent.rank)) {
						promotableSubspecies.add(td.parent);
						System.out.println("                               ==> parent earmarked for promotion");
					}
					else if (SUBGENUS_ATTRIBUTE.equalsIgnoreCase(td.parent.rank)) {
						promotableSubgenera.add(td.parent);
						System.out.println("                               ==> parent earmarked for promotion");
					}
				}
				else if (td.rank.startsWith("sub")) {
					String pRank = td.rank.substring("sub".length());
					if (pRank.toLowerCase().equals(td.parent.rank))
						td.parent.addChild(td);
					else if (speciesRankLevel < td.rankLevel) {
						if (td.parent.rankLevel < speciesRankLevel) {
							td.parent = null;
							orphanedSubspecies.add(td); // we'll handle these below
						}
						else td.parent.addChild(td); // be generous with varieties, form, etc.
					}
					else if ("Tribe".equals(pRank) && td.parent.rank.toLowerCase().endsWith("family"))
						td.parent.addChild(td);
					else {
						System.out.println("Primary rank missing in ancestors: " + Arrays.toString(td.parent.data));
						td.parent = null;
						if (SUBGENUS_ATTRIBUTE.equalsIgnoreCase(td.rank)) {
							orphanedSubGeneraAndFamilies.add(td);
							System.out.println("    ==> child enqueued for search: " + Arrays.toString(td.data));
						}
						else if (SUBFAMILY_ATTRIBUTE.equalsIgnoreCase(td.rank)) {
							orphanedSubGeneraAndFamilies.add(td);
							System.out.println("    ==> child enqueued for search: " + Arrays.toString(td.data));
						}
						else System.out.println("             ==> discarding child: " + Arrays.toString(td.data));
					}
				}
				else if (td.rank.startsWith("infra")) {
					String pRank = td.rank.substring("infra".length());
					if (pRank.toLowerCase().equals(td.parent.rank))
						td.parent.addChild(td);
					else if (("sub" + pRank).equals(td.parent.rank))
						td.parent.addChild(td);
					else {
						System.out.println("Primary rank missing in ancestors: " + Arrays.toString(td.parent.data));
						System.out.println("             ==> discarding child: " + Arrays.toString(td.data));
						td.parent = null;
					}
				}
				else if (td.rank.startsWith("parv")) {
					String pRank = td.rank.substring("parv".length());
					if (pRank.toLowerCase().equals(td.parent.rank))
						td.parent.addChild(td);
					else if (("sub" + pRank).equals(td.parent.rank))
						td.parent.addChild(td);
					else if (("infra" + pRank).equals(td.parent.rank))
						td.parent.addChild(td);
					else {
						System.out.println("Primary rank missing in ancestors: " + Arrays.toString(td.parent.data));
						System.out.println("             ==> discarding child: " + Arrays.toString(td.data));
						td.parent = null;
					}
				}
				else if ((td.parent != null) && (td.parent.data[metaXml.validTaxonIdIndex] != null)) {
					System.out.println("Synonym as parent: " + Arrays.toString(td.parent.data));
					System.out.println("            child: " + Arrays.toString(td.data));
				}
				else if ((td.epithet.indexOf("_incert") != -1) ||  "'".equals(td.epithet)) {
					System.out.println("Uncertain child: " + Arrays.toString(td.data));
					System.out.println("         parent: " + Arrays.toString(td.parent.data));
				}
				else if ((td.parent.epithet.indexOf("_incert") != -1) ||  "'".equals(td.parent.epithet)) {
					td.parent.addChild(td);
					System.out.println("Uncertain parent: " + Arrays.toString(td.data));
					System.out.println("           child: " + Arrays.toString(td.parent.data));
				}
				else {
					td.parent.addChild(td);
					if ((speciesRankLevel < td.rankLevel) && (td.parent.rankLevel < speciesRankLevel)) {
						System.out.println("Species missing in ancestors: " + Arrays.toString(td.data));
						System.out.println("                      parent: " + Arrays.toString(td.parent.data));
					}
				}
			}
			if (td.data[metaXml.validTaxonIdIndex] != null) {
				String vtid = td.data[metaXml.validTaxonIdIndex];
				ArrayList trace = new ArrayList();
				while (removedTaxonIDsToParentIDs.containsKey(vtid)) {
					String tvtid = ((String) removedTaxonIDsToParentIDs.get(vtid));
					if (vtid.equals(tvtid))
						break; // catch potential endless loop
					else {
						trace.add(vtid + " > " + tvtid);
						vtid = tvtid;
					}
				}
				td.validTaxon = ((TaxonData) taxonDataById.get(vtid));
				if (td.validTaxon == null) {
					System.out.println("Unable to find valid form: " + Arrays.toString(td.data));
					if (trace.size() != 0)
						System.out.println("           valid ID trace: " + trace);
				}
				else td.validTaxon.addSynonym(td);
			}
		}
		
		//	filter detached taxa (only now that we've had a chance to attach descendants to orphaned taxa, which we might reclaim below)
		for (Iterator tidit = taxonDataById.keySet().iterator(); tidit.hasNext();) {
			String tid = ((String) tidit.next());
			TaxonData td = ((TaxonData) taxonDataById.get(tid));
			if (KINGDOM_ATTRIBUTE.equals(td.rank)) {} // no parents for kingdoms ...
			else if ((td.parent == null) && (td.validTaxon == null)) {
				System.out.println(" ==> discarding detached taxon: " + Arrays.toString(td.data));
				tidit.remove();
			}
		}
		System.out.println("Retained " + taxonDataById.size() + " taxa after parent/valid uplink");
		
		//	index orphaned taxa by assigned parents to facilitate searching missing parent in whole of assigned subtrees
		HashMap orphanedTaxaByAssignedParents = new HashMap();
		for (int t = 0; t < orphanedSubGeneraAndFamilies.size(); t++) {
			TaxonData td = ((TaxonData) orphanedSubGeneraAndFamilies.get(t));
			String ptid = td.data[metaXml.parentTaxonIdIndex];
			while (removedTaxonIDsToParentIDs.containsKey(ptid)) {
				String gptid = ((String) removedTaxonIDsToParentIDs.get(ptid));
				if (ptid.equals(gptid))
					break; // catch potential endless loop
				else ptid = gptid;
			}
			HashSet siblingTaxa = ((HashSet) orphanedTaxaByAssignedParents.get(ptid));
			if (siblingTaxa == null) {
				siblingTaxa = new HashSet();
				orphanedTaxaByAssignedParents.put(ptid, siblingTaxa);
			}
			siblingTaxa.add(td);
		}
		for (int t = 0; t < orphanedSubspecies.size(); t++) {
			TaxonData td = ((TaxonData) orphanedSubspecies.get(t));
			String ptid = td.data[metaXml.parentTaxonIdIndex];
			while (removedTaxonIDsToParentIDs.containsKey(ptid)) {
				String gptid = ((String) removedTaxonIDsToParentIDs.get(ptid));
				if (ptid.equals(gptid))
					break; // catch potential endless loop
				else ptid = gptid;
			}
			HashSet siblingTaxa = ((HashSet) orphanedTaxaByAssignedParents.get(ptid));
			if (siblingTaxa == null) {
				siblingTaxa = new HashSet();
				orphanedTaxaByAssignedParents.put(ptid, siblingTaxa);
			}
			siblingTaxa.add(td);
		}
		
		//	seek potentially synonymized parent families of subfamilies that are children of orders ...
		//	... as well as potentially synonymized parent genera of subgenera that are children of families
		for (int t = 0; t < orphanedSubGeneraAndFamilies.size(); t++) {
			TaxonData td = ((TaxonData) orphanedSubGeneraAndFamilies.get(t));
			System.out.println("Checking " + td.rank + " with missing primary parent: " + Arrays.toString(td.data));
			String ptid = td.data[metaXml.parentTaxonIdIndex];
			ArrayList trace = new ArrayList();
			while (removedTaxonIDsToParentIDs.containsKey(ptid)) {
				String gptid = ((String) removedTaxonIDsToParentIDs.get(ptid));
				if (ptid.equals(gptid))
					break; // catch potential endless loop
				else {
					trace.add(ptid + " > " + gptid);
					ptid = gptid;
				}
			}
			TaxonData pTd = ((TaxonData) taxonDataById.get(ptid));
			if (pTd == null) {
				System.out.println(" ==> unable to find assigned parent");
				if (trace.size() != 0)
					System.out.println("      parent ID trace: " + trace);
				continue;
			}
			System.out.println(" - assigned parent is " + pTd.rank + ": " + Arrays.toString(pTd.data));
			String epEpithet = null;
			if (SUBGENUS_ATTRIBUTE.equals(td.rank)) {
				String fullName = td.data[metaXml.taxonNameIndex];
				if (fullName == null) {
					System.out.println(" ==> full name missing");
					continue;
				}
				if (fullName.matches("[A-Z][a-z]+\\s*\\(\\s*[A-Z][a-z]+\\s*\\).*"))
					epEpithet = fullName.substring(0, fullName.indexOf("(")).trim();
				else {
					System.out.println(" ==> strange full name '" + fullName + "'");
					continue;
				}
			}
			else if (SUBFAMILY_ATTRIBUTE.equals(td.rank)) {
				String fullName = td.data[metaXml.taxonNameIndex];
				if (fullName == null) {
					System.out.println(" ==> full name missing");
					continue;
				}
				if (fullName.endsWith("inae"))
					epEpithet = (fullName.substring(0, (fullName.length() - "inae".length())) + "idae");
				else if (fullName.matches("[A-Z][a-z]+inae\\s+.*"))
					epEpithet = (fullName.substring(0, fullName.indexOf("inae ")) + "idae");
				else {
					System.out.println(" ==> strange full name '" + fullName + "'");
					continue;
				}
			}
			else continue; // just to be safe
			String epRank = td.rank.substring("sub".length()).toLowerCase();
			System.out.println(" - seeking intermediate " + epRank + " '" + epEpithet + "'");
			ArrayList seekNodes = new ArrayList();
			seekNodes.add(pTd); // search siblings under assigned parent
			HashSet siblingTaxa = ((HashSet) orphanedTaxaByAssignedParents.get(ptid));
			if (siblingTaxa == null)
				seekNodes.add(td); // search currently detached subtree of orphaned taxon proper ...
			else seekNodes.addAll(siblingTaxa); // ... or even all detached subtrees of orphaned siblings
			for (int n = 0; n < seekNodes.size(); n++) {
				TaxonData sTd = ((TaxonData) seekNodes.get(n));
				if (epRank.equals(sTd.rank) && epEpithet.equals(sTd.epithet)) {
					if (sTd.validTaxon != null) {
						if (sTd.validTaxon.synonyms != null)
							sTd.validTaxon.synonyms.remove(sTd);
						sTd.validTaxon = null;
					}
					pTd.addChild(sTd);
					sTd.parent = pTd;
					sTd.addChild(td);
					td.parent = sTd;
					promotableSubgenera.remove(td); // no need to promote subgenus, children will move up to descend from resurrected genus further downstream
					System.out.println(" ==> found missing parent: " + Arrays.toString(sTd.data));
					if (!taxonDataById.containsKey(td.colId)) {
						taxonDataById.put(td.colId, td);
						System.out.println(" ==> reclaimed orphaned child");
					}
					break;
				}
				else {
					if (sTd.children != null)
						seekNodes.addAll(sTd.children);
					if (sTd.synonyms != null)
						seekNodes.addAll(sTd.synonyms);
				}
			}
			if (td.parent == null)
				System.out.println(" ==> parent not found in subtree of " + seekNodes.size() + " nodes");
		}
		
		//	seek potentially synonymized parent species of subspecies that are children of genera ...
		for (int t = 0; t < orphanedSubspecies.size(); t++) {
			TaxonData td = ((TaxonData) orphanedSubspecies.get(t));
			System.out.println("Checking " + td.rank + " with missing parent species: " + Arrays.toString(td.data));
			String ptid = td.data[metaXml.parentTaxonIdIndex];
			ArrayList trace = new ArrayList();
			while (removedTaxonIDsToParentIDs.containsKey(ptid)) {
				String gptid = ((String) removedTaxonIDsToParentIDs.get(ptid));
				if (ptid.equals(gptid))
					break; // catch potential endless loop
				else {
					trace.add(ptid + " > " + gptid);
					ptid = gptid;
				}
			}
			TaxonData pTd = ((TaxonData) taxonDataById.get(ptid));
			if (pTd == null) {
				System.out.println(" ==> unable to find assigned parent");
				if (trace.size() != 0)
					System.out.println("      parent ID trace: " + trace);
				continue;
			}
			System.out.println(" - assigned parent is " + pTd.rank + ": " + Arrays.toString(pTd.data));
			String epEpithet = td.data[metaXml.speciesNameIndex];
			System.out.println(" - seeking intermediate species '" + td.data[metaXml.genusNameIndex] + " " + epEpithet + "'");
			ArrayList seekNodes = new ArrayList();
			seekNodes.add(pTd); // search siblings under assigned parent
			HashSet siblingTaxa = ((HashSet) orphanedTaxaByAssignedParents.get(ptid));
			if (siblingTaxa == null)
				seekNodes.add(td); // search currently detached subtree of orphaned taxon proper ...
			else seekNodes.addAll(siblingTaxa); // ... or even all detached subtrees of orphaned siblings
			for (int n = 0; n < seekNodes.size(); n++) {
				TaxonData sTd = ((TaxonData) seekNodes.get(n));
				if (SPECIES_ATTRIBUTE.equals(sTd.rank) && epEpithet.equals(sTd.epithet)) {
					if (sTd.validTaxon != null) {
						if (sTd.validTaxon.synonyms != null)
							sTd.validTaxon.synonyms.remove(sTd);
						sTd.validTaxon = null;
					}
					pTd.parent.addChild(sTd);
					sTd.parent = pTd;
					sTd.addChild(td);
					td.parent = sTd;
					promotableSubspecies.remove(td); // no need to promote subspecies, children will move up to descend from resurrected species further downstream
					System.out.println(" ==> found missing species: " + Arrays.toString(sTd.data));
					if (!taxonDataById.containsKey(td.colId)) {
						taxonDataById.put(td.colId, td);
						System.out.println(" ==> reclaimed orphaned child");
					}
					break;
				}
				else {
					if (sTd.children != null)
						seekNodes.addAll(sTd.children);
					if (sTd.synonyms != null)
						seekNodes.addAll(sTd.synonyms);
				}
			}
			if (td.parent != null)
				continue;
			System.out.println(" ==> parent species not found in subtree of " + seekNodes.size() + " nodes");
			pTd.addChild(td);
			td.parent = pTd;
			System.out.println(" ==> added to assigned parent for later promotion");
		}
		System.out.println("Retained " + taxonDataById.size() + " taxa after reclaiming orphans");
		
		//	promote duplicate 'Aus bus bus' subspecies back to species if parent is above species
		for (Iterator tdit = promotableSubspecies.iterator(); tdit.hasNext();) {
			TaxonData td = ((TaxonData) tdit.next());
			if (td.parent == null)
				continue;
			System.out.println("Checking duplicate subspecies for promotion: " + Arrays.toString(td.data));
			if (speciesRankLevel <= td.parent.rankLevel) {
				System.out.println(" ==> parent is species or below: " + Arrays.toString(td.parent.data));
				continue;
			}
			if (td.data[metaXml.speciesNameIndex] == null) {
				System.out.println(" ==> species epithet not found");
				continue;
			}
			if (!td.data[metaXml.speciesNameIndex].equals(td.data[metaXml.belowSpeciesNameIndex])) {
				System.out.println(" ==> epithet mismatch");
				continue;
			}
			td.rank = SPECIES_ATTRIBUTE;
			td.rankLevel = speciesRankLevel;
			td.data[metaXml.taxonRankIndex] = SPECIES_ATTRIBUTE;
			td.data[metaXml.belowSpeciesNameIndex] = null;
			if ((td.epithetPrefix != null) && td.epithetPrefix.endsWith(" " + td.epithet)) {
				td.epithetPrefix = td.epithetPrefix.substring(0, td.epithetPrefix.lastIndexOf(" ")).trim();
				td.data[metaXml.taxonNameIndex] = (td.epithetPrefix + " " + td.epithet + ((td.authority == null) ? "" : (" " + td.authority)));
				System.out.println(" ==> rank and name adjusted to species: " + Arrays.toString(td.data));
			}
			else System.out.println(" ==> rank adjusted to species: " + Arrays.toString(td.data));
		}
		
		//	promote 'Aus (Aus)' subgenera to genera if parent above genus
		for (Iterator tdit = promotableSubgenera.iterator(); tdit.hasNext();) {
			TaxonData td = ((TaxonData) tdit.next());
			if (td.parent == null)
				continue;
			System.out.println("Checking duplicate subgenus for promotion: " + Arrays.toString(td.data));
			if (genusRankLevel <= td.parent.rankLevel) {
				System.out.println(" ==> parent is genus or below: " + Arrays.toString(td.parent.data));
				continue;
			}
			if (td.data[metaXml.genusNameIndex] == null) {
				System.out.println(" ==> genus epithet not found");
				continue;
			}
			if (!td.data[metaXml.genusNameIndex].equals(td.data[metaXml.belowGenusNameIndex])) {
				System.out.println(" ==> epithet mismatch");
				continue;
			}
			td.rank = GENUS_ATTRIBUTE;
			td.rankLevel = genusRankLevel;
			td.data[metaXml.taxonRankIndex] = GENUS_ATTRIBUTE;
			td.data[metaXml.belowGenusNameIndex] = null;
			td.epithetPrefix = null;
			td.data[metaXml.taxonNameIndex] = (td.epithet + ((td.authority == null) ? "" : (" " + td.authority)));
			System.out.println(" ==> rank and name adjusted to genus: " + Arrays.toString(td.data));
		}
		
		//	find actual parent species of mis-ranked subspecies (species with spaces in epithet)
		for (Iterator tidit = taxonDataById.keySet().iterator(); tidit.hasNext();) {
			String tid = ((String) tidit.next());
			TaxonData td = ((TaxonData) taxonDataById.get(tid));
			if (td.parent == null)
				continue;
			if (td.rankLevel != speciesRankLevel)
				continue;
			if (td.epithet == null)
				continue;
			if (td.epithet.indexOf(" ") == -1)
				continue;
			System.out.println("Checking species ranked subspecies: " + Arrays.toString(td.data));
			String epEpithet = td.epithet.substring(0, td.epithet.indexOf(" "));
			System.out.println(" - seeking intermediate species '" + td.data[metaXml.genusNameIndex] + " " + epEpithet + "'");
			ArrayList seekNodes = new ArrayList();
			seekNodes.add(td.parent);
			TaxonData oParent = td.parent;
			for (int n = 0; n < seekNodes.size(); n++) {
				TaxonData sTd = ((TaxonData) seekNodes.get(n));
				if (SPECIES_ATTRIBUTE.equals(sTd.rank) && epEpithet.equals(sTd.epithet)) {
					if (sTd.validTaxon != null) {
						if (sTd.validTaxon.synonyms != null)
							sTd.validTaxon.synonyms.remove(sTd);
						sTd.validTaxon = null;
					}
					td.parent.addChild(sTd);
					sTd.parent = td.parent;
					if (td.parent.children != null)
						td.parent.children.remove(td);
					sTd.addChild(td);
					td.parent = sTd;
					System.out.println(" ==> found missing species: " + Arrays.toString(sTd.data));
					String ssEpithet = td.epithet.substring(td.epithet.indexOf(" ")).trim();
					td.data[metaXml.speciesNameIndex] = epEpithet;
					if (ssEpithet.startsWith("subsp.")) {
						ssEpithet = ssEpithet.substring("subsp.".length()).trim();
						td.rank = SUBSPECIES_ATTRIBUTE;
					}
					else if (ssEpithet.startsWith("var.")) {
						ssEpithet = ssEpithet.substring("var.".length()).trim();
						td.rank = VARIETY_ATTRIBUTE;
					}
					else if (ssEpithet.startsWith("f.")) {
						ssEpithet = ssEpithet.substring("f.".length()).trim();
						td.rank = FORM_ATTRIBUTE;
					}
					else td.rank = SUBSPECIES_ATTRIBUTE;
					td.rankLevel = ((Integer) ranksToLevels.get(td.rank)).intValue();
					td.data[metaXml.belowSpeciesNameIndex] = ssEpithet;
					td.data[metaXml.taxonRankIndex] = td.rank;
					td.epithet = ssEpithet;
					td.epithetPrefix = (td.epithetPrefix + " " + epEpithet);
					System.out.println(" ==> demoted to subspecies: " + Arrays.toString(td.data));
					break;
				}
				else {
					if (sTd.children != null)
						seekNodes.addAll(sTd.children);
					if (sTd.synonyms != null)
						seekNodes.addAll(sTd.synonyms);
				}
			}
			if (td.parent == oParent)
				System.out.println(" ==> parent not found in subtree of " + seekNodes.size() + " nodes");
		}
		
		//	check rank hierarchy of higher taxa (genus and below only)
		for (Iterator tidit = taxonDataById.keySet().iterator(); tidit.hasNext();) {
			String tid = ((String) tidit.next());
			TaxonData td = ((TaxonData) taxonDataById.get(tid));
			if (td.parent == null)
				continue;
			if ((speciesRankLevel < td.rankLevel) && (td.parent.rankLevel < speciesRankLevel)) {
				System.out.println("Species missing in ancestors: " + Arrays.toString(td.data));
				System.out.println("                      parent: " + Arrays.toString(td.parent.data));
				System.out.println("        child epithet prefix: " + td.epithetPrefix);
			}
			else if ((genusRankLevel < td.rankLevel) && (td.parent.rankLevel < genusRankLevel)) {
				System.out.println("Genus missing in ancestors: " + Arrays.toString(td.data));
				System.out.println("                    parent: " + Arrays.toString(td.parent.data));
				System.out.println("      child epithet prefix: " + td.epithetPrefix);
			}
			else continue;
			if (td.epithetPrefix == null)
				continue;
			ArrayList seekNodes = new ArrayList();
			seekNodes.add(td.parent);
			for (int n = 0; n < seekNodes.size(); n++) {
				TaxonData sTd = ((TaxonData) seekNodes.get(n));
				if ((speciesRankLevel < td.rankLevel) && (td.parent.rankLevel < speciesRankLevel)) {
					if ((sTd.rankLevel == speciesRankLevel) && td.epithetPrefix.endsWith(" " + sTd.epithet)) {
						System.out.println("               found: " + sTd);
						td.parent.children.remove(td);
						td.parent = sTd;
						sTd.addChild(td);
						break;
					}
					else if ((sTd.rankLevel < speciesRankLevel) && (sTd.children != null))
						seekNodes.addAll(sTd.children);
				}
				else if ((genusRankLevel < td.rankLevel) && (td.parent.rankLevel < genusRankLevel)) {
					if ((sTd.rankLevel == genusRankLevel) && td.epithetPrefix.equals(sTd.epithet)) {
						System.out.println("              found: " + sTd);
						td.parent.children.remove(td);
						td.parent = sTd;
						sTd.addChild(td);
						break;
					}
					else if ((sTd.rankLevel < genusRankLevel) && (sTd.children != null))
						seekNodes.addAll(sTd.children);
				}
			}
		}
		
		//	check epithet prefixes for valid taxa ...
		for (Iterator tidit = taxonDataById.keySet().iterator(); tidit.hasNext();) {
			String tid = ((String) tidit.next());
			TaxonData td = ((TaxonData) taxonDataById.get(tid));
			if (td.epithetPrefix == null)
				continue;
			if (td.parent == null)
				continue;
			if (td.rankLevel <= genusRankLevel) {
				td.epithetPrefix = null; // no prefix in genus and above
				continue;
			}
			if (td.epithetPrefix.equals(td.parent.epithet)) {
				td.epithetPrefix = null; // obvious from tree
				continue;
			}
			if (td.epithetPrefix.equals(td.parent.epithet.toLowerCase())) {
				td.epithetPrefix = null; // obvious from tree, and capitalization error in prefix
				continue;
			}
			if (td.epithetPrefix.equals(td.parent.epithet + " x")) {
				td.epithetPrefix = null; // obvious from tree, with botany specific infix
				continue;
			}
			if ((td.epithetPrefix.indexOf(" ") != -1) && td.epithetPrefix.endsWith(" " + td.parent.epithet)) {
				td.epithetPrefix = null; // obvious from tree
				continue;
			}
			if ((td.epithetPrefix.length() == 0) && SUBGENUS_ATTRIBUTE.equals(td.rank) && GENUS_ATTRIBUTE.equals(td.parent.rank)) {
				td.epithetPrefix = null; // empty prefix, will have to rely upon tree
				continue;
			}
			if ((td.epithetPrefix.indexOf(" ") != -1) && SUBGENUS_ATTRIBUTE.equals(td.parent.rank) && td.epithetPrefix.endsWith(" (" + td.parent.epithet + ")")) {
				td.epithetPrefix = null; // obvious from tree
				continue;
			}
			if ((td.epithetPrefix.indexOf(" ") != -1) && SUBGENUS_ATTRIBUTE.equals(td.parent.rank) && td.epithetPrefix.endsWith(" (" + td.parent.epithet.toLowerCase() + ")")) {
				td.epithetPrefix = null; // obvious from tree, and capitalization error in prefix
				continue;
			}
			if ((td.epithetPrefix.indexOf(" ") == -1) && SUBGENUS_ATTRIBUTE.equals(td.parent.rank) && (td.parent.parent != null) && td.epithetPrefix.equals(td.parent.parent.epithet)) {
				td.epithetPrefix = null; // obvious from tree, subgenus omitted in prefix
				continue;
			}
			if ((speciesRankLevel < td.parent.rankLevel) && (td.parent.epithetPrefix == null)) {
				TaxonData pstd = td.parent;
				while ((pstd != null) && (speciesRankLevel < pstd.rankLevel))
					pstd = pstd.parent;
				if ((pstd != null) && (pstd.rankLevel == speciesRankLevel) && td.epithetPrefix.endsWith(" " + pstd.epithet)) {
					td.epithetPrefix = null; // need to rely upon tree, parent subspecies or variety omitted in prefix
					continue;
				}
			}
			if ((speciesRankLevel < td.parent.rankLevel) && (td.parent.epithetPrefix != null) && td.parent.epithetPrefix.equals(td.epithetPrefix)) {
				td.epithetPrefix = null; // obvious from tree, parent subspecies or variety omitted in prefix
				continue;
			}
			System.out.println("Strange below-genus epithet prefix: " + Arrays.toString(td.data));
			System.out.println("                            parent: " + Arrays.toString(td.parent.data));
			System.out.println("              child epithet prefix: " + td.epithetPrefix);
			System.out.println("                    parent epithet: " + td.parent.epithet);
		}
		
		//	move parent relationships up the tree until rank order restored
		for (Iterator tidit = taxonDataById.keySet().iterator(); tidit.hasNext();) {
			String tid = ((String) tidit.next());
			TaxonData td = ((TaxonData) taxonDataById.get(tid));
			if (td.parent == null)
				continue;
			if (td.parent.rankLevel < td.rankLevel)
				continue;
			while ((td.parent != null) && (td.rankLevel <= td.parent.rankLevel))
				td.parent = td.parent.parent;
			if (td.parent == null) {
				System.out.println("Unable to find parent with rank above " + td.rank + ": " + Arrays.toString(td.data));
				System.out.println(" ==> discarding detached taxon");
			}
			else {
				td.parent.addChild(td);
				System.out.println("Corrected parent of " + td.rank + " " + td.epithet + " to " + td.parent.rank + " " + td.parent.epithet + ": " + Arrays.toString(td.data));
			}
		}
		
		//	remove taxa with '_incert' infix
		for (Iterator tidit = taxonDataById.keySet().iterator(); tidit.hasNext();) {
			String tid = ((String) tidit.next());
			TaxonData td = ((TaxonData) taxonDataById.get(tid));
			if (td.parent == null)
				continue;
			if ((td.epithet.indexOf("_incert") == -1) && !"'".equals(td.epithet))
				continue;
			if ((td.children != null) && !primaryRanks.containsKey(td.rank)) {
				for (Iterator cit = td.children.iterator(); cit.hasNext();) {
					TaxonData cTd = ((TaxonData) cit.next());
					cTd.parent = td.parent;
					td.parent.addChild(cTd);
					System.out.println("Corrected parent of " + cTd.rank + " " + cTd.epithet + " from " + td.rank + " " + td.epithet + " to " + td.parent.rank + " " + td.parent.epithet + ": " + Arrays.toString(td.data));
					if ((cTd.epithetPrefix != null) && (cTd.epithetPrefix.indexOf(td.epithet) != -1)) {
						cTd.epithetPrefix = (cTd.epithetPrefix.substring(0, cTd.epithetPrefix.indexOf(td.epithet)).trim() + cTd.epithetPrefix.substring(cTd.epithetPrefix.indexOf(td.epithet) + td.epithet.length()).trim());
						cTd.epithetPrefix = cTd.epithetPrefix.replaceAll("\\(((sect|subsect|ser|subser)\\.)?\\)", "");
						cTd.epithetPrefix = cTd.epithetPrefix.replaceAll("\\s+", " ");
						System.out.println(" ==> epithet prefix corected to " + cTd.epithetPrefix);
					}
				}
				td.children.clear();
				td.children = null;
			}
			if (td.parent.children != null)
				td.parent.children.remove(td);
			td.parent = null;
			tidit.remove();
			System.out.println("Discarding uncertain " + td.rank + " " + td.epithet + ": " + Arrays.toString(td.data));
		}
		System.out.println("Retained " + taxonDataById.size() + " taxa after filtering out uncertain ones");
		
		//	finally normalize epithets (we needed the '_' for filtering up until now)
		for (Iterator tidit = taxonDataById.keySet().iterator(); tidit.hasNext();) {
			String tid = ((String) tidit.next());
			TaxonData td = ((TaxonData) taxonDataById.get(tid));
			if (td.epithet.matches("([1-9][0-9]?\\-)?[A-Za-z\\-]+"))
				continue;
			System.out.println("Strange characters in: " + td.epithet + " " + Arrays.toString(td.data));
			System.out.print("                  HEX:");
			for (int c = 0; c < td.epithet.length(); c++) {
				char ch = td.epithet.charAt(c);
				System.out.print(" " + Integer.toString(((int) ch), 16).toUpperCase());
			}
			System.out.println();
			td.epithet = td.epithet.replaceAll("\\'", "");
			if (td.epithet.endsWith("."))
				td.epithet = td.epithet.substring(0, (td.epithet.length() - ".".length()));
			if (td.epithet.matches("([1-9][0-9]?\\-)?[A-Za-z\\-]+"))
				continue;
			if (td.validTaxon != null) {
				System.out.println("                ==> discarding synonym with strange characters");
				if (td.validTaxon.synonyms != null)
					td.validTaxon.synonyms.remove(td);
				td.validTaxon = null;
				tidit.remove();
				continue;
			}
			td.epithet = td.epithet.replaceAll("[^A-Za-z0-9]+", "-"); // also replacing dashes, so we don't get double dashes in result
			if (td.epithet.startsWith("-"))
				td.epithet = td.epithet.substring("-".length());
			if (td.epithet.endsWith("-"))
				td.epithet = td.epithet.substring(0, (td.epithet.length() - "-".length()));
			if (speciesRankLevel <= td.rankLevel)
				td.epithet = td.epithet.toLowerCase();
			System.out.println("                ==> " + td.epithet);
			if (td.epithet.matches("([1-9][0-9]?\\-)?[A-Za-z\\-]+"))
				continue;
			System.out.println("                ==> still got strange characters");
		}
		System.out.println("Retained " + taxonDataById.size() + " taxa after epithet normalization");
		
		//	reclaim discarded parents of taxa that are indexed
		ArrayList taxonIDs = new ArrayList(taxonDataById.keySet());
		for (int i = 0; i < taxonIDs.size(); i++) {
			String tid = ((String) taxonIDs.get(i));
			TaxonData td = ((TaxonData) taxonDataById.get(tid));
			if ((td.parent != null) && !taxonDataById.containsKey(td.parent.colId)) {
				taxonDataById.put(td.parent.colId, td.parent);
				System.out.println(" ==> reclaimed removed parent: " + Arrays.toString(td.parent.data));
			}
			if ((td.validTaxon != null) && !taxonDataById.containsKey(td.validTaxon.colId)) {
				taxonDataById.put(td.validTaxon.colId, td.validTaxon);
				System.out.println(" ==> reclaimed removed valid taxon: " + Arrays.toString(td.validTaxon.data));
			}
		}
		System.out.println("Retained " + taxonDataById.size() + " taxa after reclaiming parents and valid taxa");
		
		//	one last time remove all taxa with missing primary ranks in kingdom trace
		for (Iterator tidit = taxonDataById.keySet().iterator(); tidit.hasNext();) {
			String tid = ((String) tidit.next());
			TaxonData td = ((TaxonData) taxonDataById.get(tid));
			LinkedHashSet ktPrimaryRanks = new LinkedHashSet();
			LinkedHashSet ktRankGroups = new LinkedHashSet();
			ArrayList kingdomTrace = new ArrayList();
			for (TaxonData ktTd = ((td.validTaxon == null) ? td : td.validTaxon); ktTd != null; ktTd = ktTd.parent) {
				kingdomTrace.add(ktTd);
				if (ktTd.rank.startsWith("super"))
					continue; // not handling superFamilies, etc. here, as they are above primary ranks in their groups
				if (primaryRanks.containsKey(ktTd.rank)) {
					ktPrimaryRanks.add(ktTd.rank);
					ktRankGroups.add(ktTd.rank);
				}
				else if (ktTd.rank.equals(TRIBE_ATTRIBUTE) || ktTd.rank.endsWith("Tribe"))
					ktRankGroups.add(FAMILY_ATTRIBUTE);
				else if (ktTd.rank.equals(SECTION_ATTRIBUTE) || ktTd.rank.endsWith("Section"))
					ktRankGroups.add(GENUS_ATTRIBUTE);
				else if (ktTd.rank.equals(SERIES_ATTRIBUTE) || ktTd.rank.endsWith("Series"))
					ktRankGroups.add(GENUS_ATTRIBUTE);
				else if (ktTd.rank.equals(VARIETY_ATTRIBUTE) || ktTd.rank.endsWith("Variety"))
					ktRankGroups.add(SPECIES_ATTRIBUTE);
				else if (ktTd.rank.equals(FORM_ATTRIBUTE) || ktTd.rank.endsWith("Form"))
					ktRankGroups.add(SPECIES_ATTRIBUTE);
				else if (ktTd.rank.startsWith("sub"))
					ktRankGroups.add(ktTd.rank.substring("sub".length()).toLowerCase());
				else if (ktTd.rank.startsWith("infra"))
					ktRankGroups.add(ktTd.rank.substring("infra".length()).toLowerCase());
				if (KINGDOM_ATTRIBUTE.equals(ktTd.rank))
					break;
			}
			if (ktPrimaryRanks.containsAll(ktRankGroups) && ktPrimaryRanks.contains(KINGDOM_ATTRIBUTE))
				continue; // got the primary ranks from all groups we've passed
			System.out.println("Found taxon with broken hierarchy: " + Arrays.toString(td.data));
			System.out.println("                    kingdom trace: " + kingdomTrace);
			System.out.println("                      rank groups: " + ktRankGroups);
			System.out.println("                    primary ranks: " + ktPrimaryRanks);
			tidit.remove();
		}
		System.out.println("Retained " + taxonDataById.size() + " taxa after cleaning up broken kingdom traces");
		
		//	make damn sure all parents know their children, and all valid taxa know their synonyms
		for (Iterator tidit = taxonDataById.keySet().iterator(); tidit.hasNext();) {
			String tid = ((String) tidit.next());
			TaxonData td = ((TaxonData) taxonDataById.get(tid));
			if (td.parent != null)
				td.parent.addChild(td);
			if (td.validTaxon != null)
				td.validTaxon.addSynonym(td);
		}
		
		//	link taxa to primary parents, and remove non-resolving ones
		for (Iterator tidit = taxonDataById.keySet().iterator(); tidit.hasNext();) {
			String tid = ((String) tidit.next());
			TaxonData td = ((TaxonData) taxonDataById.get(tid));
			if (KINGDOM_ATTRIBUTE.equals(td.rank))
				continue; // no working on parents in kingdoms
			for (TaxonData ptd = td.parent; ptd != null;) {
				if (primaryRanks.containsKey(ptd.rank)) {
					td.primaryParent = ptd;
					break;
				}
				else if (ptd.parent != null)
					ptd = ptd.parent;
				else {
					System.out.println("Unable to find primary parent: " + Arrays.toString(td.data));
					break;
				}
			}
		}
		
		//	add primary children to parents
		for (Iterator tidit = taxonDataById.keySet().iterator(); tidit.hasNext();) {
			String tid = ((String) tidit.next());
			TaxonData td = ((TaxonData) taxonDataById.get(tid));
			if (KINGDOM_ATTRIBUTE.equals(td.rank))
				continue; // not adding kingdoms to any parents
			if (!primaryRanks.containsKey(td.rank))
				continue;
			for (TaxonData ptd = td.parent; ptd != null;) {
				ptd.addPrimaryChild(td);
				if (ptd == td.primaryParent)
					break;
				else if (ptd.parent != null)
					ptd = ptd.parent;
				else {
					System.out.println("Broken ancestor chain: " + Arrays.toString(td.data));
					System.out.println("          last parent: " + Arrays.toString(ptd.data));
					break;
				}
			}
		}
		
		//	clean out descendant taxa no longer indexed
		for (Iterator tidit = taxonDataById.keySet().iterator(); tidit.hasNext();) {
			String tid = ((String) tidit.next());
			TaxonData td = ((TaxonData) taxonDataById.get(tid));
			if (td.children != null) {
				for (Iterator cit = td.children.iterator(); cit.hasNext();) {
					TaxonData cTd = ((TaxonData) cit.next());
					if (taxonDataById.containsKey(cTd.colId))
						continue;
					cit.remove();
					System.out.println(" ==> removed discarded child: " + Arrays.toString(cTd.data));
				}
				if (td.children.isEmpty())
					td.children = null;
			}
			if (td.primaryChildren != null) {
				for (Iterator cit = td.primaryChildren.iterator(); cit.hasNext();) {
					TaxonData cTd = ((TaxonData) cit.next());
					if (taxonDataById.containsKey(cTd.colId))
						continue;
					cit.remove();
					System.out.println(" ==> removed discarded primary child: " + Arrays.toString(cTd.data));
				}
				if (td.primaryChildren.isEmpty())
					td.primaryChildren = null;
			}
			if (td.synonyms != null) {
				for (Iterator sit = td.synonyms.iterator(); sit.hasNext();) {
					TaxonData sTd = ((TaxonData) sit.next());
					if (taxonDataById.containsKey(sTd.colId))
						continue;
					sit.remove();
					System.out.println(" ==> removed discarded synonym: " + Arrays.toString(sTd.data));
				}
				if (td.synonyms.isEmpty())
					td.synonyms = null;
			}
		}
		
		//	count taxa with and without kingdom trace
		int attachedTaxa = 0;
		int detachedTaxa = 0;
		for (Iterator tidit = taxonDataById.keySet().iterator(); tidit.hasNext();) {
			String tid = ((String) tidit.next());
			TaxonData td = ((TaxonData) taxonDataById.get(tid));
			if (KINGDOM_ATTRIBUTE.equals(td.rank)) {
				attachedTaxa++;
				continue; // always attached
			}
			TaxonData kTd = null;
			for (TaxonData ptd = ((td.validTaxon == null) ? td.parent : td.validTaxon); ptd != null; ptd = ptd.parent)
				if (KINGDOM_ATTRIBUTE.equals(ptd.rank)) {
					kTd = ptd;
					break;
				}
			if (kTd == null)
				detachedTaxa++;
			else attachedTaxa++;
		}
		
		//	finally ...
		System.out.println("Retained " + taxonDataById.size() + " taxa after processing whole tree, " + attachedTaxa + " attached, " + detachedTaxa + " detached");
		return taxonDataById;
	}
	
	private static void distill(String dwcaFolder) throws Exception {
		distill(new File(dwcaFolder));
	}
	private static void distill(File dwcaFolder) throws Exception {
		
		//	load data
		LinkedHashMap taxonDataById = getTaxonDataById(dwcaFolder);
		
		//	load custom tile definitions (if any)
		HashMap kingdomPathsToClusterNames = getKingdomPathsToClusterNames(dwcaFolder);
		
		//	count out tiles
		TreeMap clusterNamesToContentRoots = new TreeMap();
		for (Iterator tidit = taxonDataById.keySet().iterator(); tidit.hasNext();) {
			String tid = ((String) tidit.next());
			TaxonData td = ((TaxonData) taxonDataById.get(tid));
			if (!KINGDOM_ATTRIBUTE.equals(td.rank))
				continue;
			if (td.validTaxon != null)
				continue;
			ArrayList seekTaxonDataList = new ArrayList();
			seekTaxonDataList.add(td);
			for (int t = 0; t < seekTaxonDataList.size(); t++) {
				TaxonData sTxd = ((TaxonData) seekTaxonDataList.get(t));
				if (familyRankLevel < sTxd.rankLevel)
					continue; // however this one got here ...
				StringBuffer kingdomPath = new StringBuffer("Zzz");
				for (TaxonData pTxd = sTxd; pTxd != null; pTxd = pTxd.primaryParent) {
					kingdomPath.insert(0, (pTxd.epithet + "."));
					String pRank = ((String) primaryRankToParents.get(pTxd.rank));
					while ((pRank != null) && (pTxd.primaryParent != null) && !pRank.equals(pTxd.primaryParent.rank)) {
						kingdomPath.insert(0, ("<" + pRank + ">" + "."));
						pRank = ((String) primaryRankToParents.get(pRank));
					}
				}
				String clusterName = ((String) kingdomPathsToClusterNames.get(kingdomPath.toString()));
				if (clusterName != null) {
					ArrayList contentRoots = ((ArrayList) clusterNamesToContentRoots.get(clusterName));
					if (contentRoots == null) {
						contentRoots = new ArrayList(4);
						clusterNamesToContentRoots.put(clusterName, contentRoots);
					}
					contentRoots.add(sTxd);
				}
				if (familyRankLevel <= sTxd.rankLevel)
					continue;
				if (sTxd.primaryChildren == null)
					continue;
				seekTaxonDataList.addAll(sTxd.primaryChildren);
			}
		}
		for (Iterator cnit = clusterNamesToContentRoots.keySet().iterator(); cnit.hasNext();) {
			String cn = ((String) cnit.next());
			ArrayList crs = ((ArrayList) clusterNamesToContentRoots.get(cn));
			System.out.println(cn + " --> " + crs);
			String rRank = null;
			for (int r = 0; r < crs.size(); r++) {
				TaxonData rTxd = ((TaxonData) crs.get(r));
				if (rRank == null)
					rRank = rTxd.rank;
				else if (!rRank.equals(rTxd.rank))
					System.out.println("Found mixed ranks: " + rRank + " vs. " + rTxd.rank);
			}
		}
		
		//	collect root taxa as starting points for ID assignment
		ArrayList rootTaxonDatas = new ArrayList();
		TreeMap genusDatasByEpithet = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		TreeMap aboveSpeciesDatasByEpithet = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		ArrayList allTaxonDatas = new ArrayList();
		for (Iterator tidit = taxonDataById.keySet().iterator(); tidit.hasNext();) {
			String tid = ((String) tidit.next());
			TaxonData td = ((TaxonData) taxonDataById.get(tid));
			if (KINGDOM_ATTRIBUTE.equals(td.rank) && (td.validTaxon == null))
				rootTaxonDatas.add(td);
			TreeMap epithetIndex;
			if (GENUS_ATTRIBUTE.equals(td.rank))
				epithetIndex = genusDatasByEpithet;
			else if (td.rankLevel < speciesRankLevel)
				epithetIndex = aboveSpeciesDatasByEpithet;
			else epithetIndex = null;
			if (epithetIndex != null) {
				if (epithetIndex.containsKey(td.epithet)) {
					System.out.println("Found duplicate " + td.rank + " " + td.epithet);
					Object tdObj = epithetIndex.get(td.epithet);
					if (tdObj instanceof ArrayList)
						((ArrayList) tdObj).add(td);
					else {
						ArrayList txdList = new ArrayList(4);
						txdList.add(tdObj);
						txdList.add(td);
						epithetIndex.put(td.epithet, txdList);
					}
				}
				else epithetIndex.put(td.epithet, td);
			}
			allTaxonDatas.add(td);
		}
		System.out.println("Found " + rootTaxonDatas.size() + " root taxa for " + allTaxonDatas.size() + " overall taxa");
		
		//	assign IDs and cluster names
		int dataId = 1;
		HashMap taxonDatasByIDs = new HashMap();
		for (Iterator rit = ranksToLevels.keySet().iterator(); rit.hasNext();) {
			String rank = ((String) rit.next());
			Integer rankLevel = ((Integer) ranksToLevels.get(rank));
			if (rankLevel == null)
				continue;
			if (rankLevel.intValue() == speciesRankLevel)
				break; // we assign those IDs per tile, so tiles stay contiguous
			for (int t = 0; t < rootTaxonDatas.size(); t++) {
				TaxonData rTxd = ((TaxonData) rootTaxonDatas.get(t));
				dataId = rTxd.setDataId(dataId, rankLevel.intValue(), "data.higher.txt", taxonDatasByIDs);
				System.out.println("Next data ID after " + rTxd.epithet + " " + rank + ": " + dataId);
			}
			System.out.println("Next data ID after " + rank + "s: " + dataId);
		}
		for (Iterator cnit = clusterNamesToContentRoots.keySet().iterator(); cnit.hasNext();) {
			String cn = ((String) cnit.next());
			ArrayList crs = ((ArrayList) clusterNamesToContentRoots.get(cn));
			System.out.println(cn + " --> " + crs);
			String cfn = ("data.species." + cn.substring(0, cn.lastIndexOf(".")));
			if (cn.endsWith(".Zzz"))
				cfn = (cfn + ".0.txt");
			else if (cn.matches(".*\\.Zz[A-I]")) {
				char cNum = cn.charAt(cn.length() - 1);
				cfn = (cfn + "." + ((int) (cNum - 'A' + 1)) + ".txt");
			}
			System.out.println(cn + " --> " + cfn);
			for (Iterator rit = ranksToLevels.keySet().iterator(); rit.hasNext();) {
				String rank = ((String) rit.next());
				Integer rankLevel = ((Integer) ranksToLevels.get(rank));
				if (rankLevel == null)
					continue;
				if (rankLevel.intValue() < speciesRankLevel)
					continue; // all IDs above species already assigned above
				for (int r = 0; r < crs.size(); r++) {
					TaxonData rTxd = ((TaxonData) crs.get(r));
					dataId = rTxd.setDataId(dataId, rankLevel.intValue(), cfn, taxonDatasByIDs);
					System.out.println("Next data ID after " + rTxd.epithet + " " + rank + ": " + dataId);
				}
			}
		}
		
		//	remove detached taxa
		Collections.sort(allTaxonDatas, new Comparator() {
			public int compare(Object obj1, Object obj2) {
				TaxonData txd1 = ((TaxonData) obj1);
				TaxonData txd2 = ((TaxonData) obj2);
				return (txd1.dataId - txd2.dataId);
			}
		});
		int firstValidId = 0;
		for (int t = 0; t < allTaxonDatas.size(); t++) {
			TaxonData td = ((TaxonData) allTaxonDatas.get(t));
			if (td.dataId == 0) {
				System.out.println("Got zero ID in " + td.rank + " " + td.epithet);
				TaxonData tTd;
				if (td.validTaxon == null) {
					System.out.println("         parent: " + td.parent);
					tTd = td.parent;
				}
				else {
					System.out.println("    valid taxon: " + td.validTaxon);
					if ((td.validTaxon.synonyms != null) && td.validTaxon.synonyms.contains(td))
						System.out.println(" ==> attached");
					else System.out.println(" ==> detached");
					tTd = td.validTaxon;
				}
				System.out.print("      ancestors:");
				while (tTd != null) {
					System.out.print(" " + tTd);
					tTd = tTd.parent;
				}
				System.out.println();
				firstValidId++;
			}
			else break;
		}
		if (firstValidId != 0)
			allTaxonDatas.subList(0, firstValidId).clear();
		
		//	create clusters
		Collections.sort(allTaxonDatas, new Comparator() {
			public int compare(Object obj1, Object obj2) {
				TaxonData txd1 = ((TaxonData) obj1);
				TaxonData txd2 = ((TaxonData) obj2);
				int c = txd1.clusterName.compareTo(txd2.clusterName);
				if (c == 0)
					c = txd1.epithet.compareToIgnoreCase(txd2.epithet);
				if (c == 0)
					c = (txd1.dataId - txd2.dataId);
				return c;
			}
		});
		String clusterFileName = null;
		HashSet clusterIDs = new HashSet();
		int clusterMinId = Integer.MAX_VALUE;
		int clusterMaxId = 0;
		int clusterMinColId = Integer.MAX_VALUE;
		int clusterMaxColId = 0;
		HashSet clusterFileNames = new HashSet();
		File tileFile = new File(dwcaFolder, "data.tiles.txt");
		BufferedWriter tileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tileFile), "UTF-8"));
		File clusterFile = null;
		BufferedWriter clusterWriter = null;
		for (int t = 0; t < allTaxonDatas.size(); t++) {
			TaxonData td = ((TaxonData) allTaxonDatas.get(t));
			if (clusterFileName == null) {
				clusterFileName = td.clusterName;
				clusterIDs.clear();
				clusterMinId = td.dataId;
				clusterMaxId = td.dataId;
				int tdColId = CatalogOfLifeLocal.parseIntBase29(td.colId);
				clusterMinColId = tdColId;
				clusterMaxColId = tdColId;
				clusterFile = new File(dwcaFolder, clusterFileName);
				clusterWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(clusterFile), "UTF-8"));
			}
			else if (clusterFileName.equals(td.clusterName)) {
				clusterMinId = Math.min(td.dataId, clusterMinId);
				clusterMaxId = Math.max(td.dataId, clusterMaxId);
				int tdColId = CatalogOfLifeLocal.parseIntBase29(td.colId);
				clusterMinColId = Math.min(tdColId, clusterMinColId);
				clusterMaxColId = Math.max(tdColId, clusterMaxColId);
			}
			else {
				clusterWriter.flush();
				clusterWriter.close();
				tileWriter.write(clusterFileName + "\t" + Integer.toString(clusterMinId, 16).toUpperCase() + "\t" + Integer.toString(clusterMaxId, 16).toUpperCase() + "\t" + CatalogOfLifeLocal.encodeIntBase29(clusterMinColId) + "\t" + CatalogOfLifeLocal.encodeIntBase29(clusterMaxColId));
				tileWriter.newLine();
//				System.out.println("Finished tile " + clusterFileName + ": " + clusterMinId + "-" + clusterMaxId);
				System.out.println("Finished tile " + clusterFileName + ": " + clusterMinId + "-" + clusterMaxId + " (" + CatalogOfLifeLocal.encodeIntBase29(clusterMinColId) + "/" + CatalogOfLifeLocal.encodeIntBase29(clusterMaxColId) + ")");
				if (clusterIDs.size() != (clusterMaxId - clusterMinId + 1)) {
					System.out.println(" - stored only " + clusterIDs.size() + " taxa in range of " + (clusterMaxId - clusterMinId + 1) + " data IDs:");
					for (int i = clusterMinId; i <= clusterMaxId; i++) {
						Integer cId = new Integer(i);
						if (clusterIDs.contains(cId))
							continue;
						TaxonData cTd = ((TaxonData) taxonDatasByIDs.get(cId));
						if (cTd == null)
							System.out.println(" - missing data ID " + cId);
						else System.out.println(" - missing data ID " + cId + ": " + cTd + " " + Arrays.toString(cTd.data));
					}
				}
				if (clusterFileNames.contains(td.clusterName))
					System.out.println("Tile " + td.clusterName + " found non-contiguous at " + td.rank + " #" + td.dataId + ": " + td.epithet);
				clusterFileNames.add(td.clusterName);
				clusterFileName = td.clusterName;
				clusterIDs.clear();
				clusterMinId = td.dataId;
				clusterMaxId = td.dataId;
				int tdColId = CatalogOfLifeLocal.parseIntBase29(td.colId);
				clusterMinColId = tdColId;
				clusterMaxColId = tdColId;
				clusterFile = new File(dwcaFolder, clusterFileName);
				clusterWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(clusterFile), "UTF-8"));
			}
			
			String linkingSuffix;
			if (td.validTaxon == null) {
				StringBuffer childIDs = new StringBuffer();
				if (td.children != null) {
					ArrayList children = new ArrayList(td.children);
					for (int c = 0; c < children.size(); c++) {
						if (((TaxonData) children.get(c)).dataId == 0)
							children.remove(c--);
					}
					Collections.sort(children, new Comparator() {
						public int compare(Object obj1, Object obj2) {
							return (((TaxonData) obj1).dataId - ((TaxonData) obj2).dataId);
						}
					});
					for (int c = 0; c < children.size(); c++) {
						if (c != 0)
							childIDs.append(";");
						childIDs.append(Integer.toString(((TaxonData) children.get(c)).dataId, 16).toUpperCase());
					}
				}
				StringBuffer synonymIDs = new StringBuffer();
				if (td.synonyms != null) {
					ArrayList synonyms = new ArrayList(td.synonyms);
					for (int s = 0; s < synonyms.size(); s++) {
						if (((TaxonData) synonyms.get(s)).dataId == 0)
							synonyms.remove(s--);
					}
					Collections.sort(synonyms, new Comparator() {
						public int compare(Object obj1, Object obj2) {
							return (((TaxonData) obj1).dataId - ((TaxonData) obj2).dataId);
						}
					});
					for (int s = 0; s < synonyms.size(); s++) {
						if (s != 0)
							synonymIDs.append(";");
						synonymIDs.append(Integer.toString(((TaxonData) synonyms.get(s)).dataId, 16).toUpperCase());
					}
				}
				linkingSuffix = (childIDs + "\t" + synonymIDs);
			}
			else linkingSuffix = td.epithetPrefix;
//			clusterWriter.write(Integer.toString(td.dataId, 16).toUpperCase() + 
			clusterWriter.write(Integer.toString(td.dataId, 16).toUpperCase() + "/" + td.colId.toUpperCase() + 
					"\t" + td.rank + 
					"\t" + (td.isExtant ? "X" : "E") +
					"\t" + ((td.parent == null) ? 0 : Integer.toString(td.parent.dataId, 16).toUpperCase()) +
					"\t" + ((td.validTaxon == null) ? 0 : Integer.toString(td.validTaxon.dataId, 16).toUpperCase()) +
					"\t" + td.epithet +
					"\t" + ((td.authority == null) ? "" : td.authority) +
					"\t" + ((linkingSuffix == null) ? "" : linkingSuffix) +
					"");
			clusterWriter.newLine();
			clusterIDs.add(new Integer(td.dataId));
		}
		clusterWriter.flush();
		clusterWriter.close();
		tileWriter.write(clusterFileName + "\t" + Integer.toString(clusterMinId, 16).toUpperCase() + "\t" + Integer.toString(clusterMaxId, 16).toUpperCase() + "\t" + CatalogOfLifeLocal.encodeIntBase29(clusterMinColId) + "\t" + CatalogOfLifeLocal.encodeIntBase29(clusterMaxColId));
		tileWriter.newLine();
		tileWriter.flush();
		tileWriter.close();
		System.out.println("Finished tile " + clusterFileName + ": " + clusterMinId + "-" + clusterMaxId + " (" + CatalogOfLifeLocal.encodeIntBase29(clusterMinColId) + "/" + CatalogOfLifeLocal.encodeIntBase29(clusterMaxColId) + ")");
		if (clusterIDs.size() != (clusterMaxId - clusterMinId + 1)) {
			System.out.println(" - stored only " + clusterIDs.size() + " taxa in range of " + (clusterMaxId - clusterMinId + 1) + " data IDs:");
			for (int i = clusterMinId; i <= clusterMaxId; i++) {
				Integer cId = new Integer(i);
				if (clusterIDs.contains(cId))
					continue;
				TaxonData cTd = ((TaxonData) taxonDatasByIDs.get(cId));
				if (cTd == null)
					System.out.println(" - missing data ID " + cId);
				else System.out.println(" - missing data ID " + cId + ": " + cTd + " " + Arrays.toString(cTd.data));
			}
		}
	}
	private static HashMap defaultKingdomPathsToClusterNames = new HashMap();
	static {
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Arachnida.Zzz", "Animalia.Arthropoda.ZzA");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Copepoda.Zzz", "Animalia.Arthropoda.ZzA");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Insecta.Coleoptera.Buprestidae.Zzz", "Animalia.Arthropoda.Insecta.Coleoptera.ZzA");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Insecta.Coleoptera.Carabidae.Zzz", "Animalia.Arthropoda.Insecta.Coleoptera.Carabidae.Zzz");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Insecta.Coleoptera.Cerambycidae.Zzz", "Animalia.Arthropoda.Insecta.Coleoptera.Cerambycidae.Zzz");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Insecta.Coleoptera.Curculionidae.Zzz", "Animalia.Arthropoda.Insecta.Coleoptera.ZzA");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Insecta.Coleoptera.Staphylinidae.Zzz", "Animalia.Arthropoda.Insecta.Coleoptera.Staphylinidae.Zzz");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Insecta.Coleoptera.Zzz", "Animalia.Arthropoda.Insecta.Coleoptera.Zzz");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Insecta.Diptera.Asilidae.Zzz", "Animalia.Arthropoda.Insecta.Diptera.ZzA");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Insecta.Diptera.Bombyliidae.Zzz", "Animalia.Arthropoda.Insecta.Diptera.ZzA");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Insecta.Diptera.Cecidomyiidae.Zzz", "Animalia.Arthropoda.Insecta.Diptera.ZzA");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Insecta.Diptera.Ceratopogonidae.Zzz", "Animalia.Arthropoda.Insecta.Diptera.ZzA");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Insecta.Diptera.Chironomidae.Zzz", "Animalia.Arthropoda.Insecta.Diptera.ZzA");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Insecta.Diptera.Dolichopodidae.Zzz", "Animalia.Arthropoda.Insecta.Diptera.ZzA");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Insecta.Diptera.Limoniidae.Zzz", "Animalia.Arthropoda.Insecta.Diptera.ZzA");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Insecta.Diptera.Muscidae.Zzz", "Animalia.Arthropoda.Insecta.Diptera.ZzA");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Insecta.Diptera.Syrphidae.Zzz", "Animalia.Arthropoda.Insecta.Diptera.ZzA");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Insecta.Diptera.Tabanidae.Zzz", "Animalia.Arthropoda.Insecta.Diptera.ZzA");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Insecta.Diptera.Tachinidae.Zzz", "Animalia.Arthropoda.Insecta.Diptera.ZzA");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Insecta.Diptera.Tephritidae.Zzz", "Animalia.Arthropoda.Insecta.Diptera.ZzA");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Insecta.Diptera.Zzz", "Animalia.Arthropoda.Insecta.Diptera.Zzz");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Insecta.Hemiptera.Aphididae.Zzz", "Animalia.Arthropoda.Insecta.Hemiptera.ZzA");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Insecta.Hemiptera.Cicadellidae.Zzz", "Animalia.Arthropoda.Insecta.Hemiptera.ZzA");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Insecta.Hemiptera.Cicadidae.Zzz", "Animalia.Arthropoda.Insecta.Hemiptera.ZzA");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Insecta.Hemiptera.Miridae.Zzz", "Animalia.Arthropoda.Insecta.Hemiptera.ZzA");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Insecta.Hemiptera.Reduviidae.Zzz", "Animalia.Arthropoda.Insecta.Hemiptera.ZzA");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Insecta.Hemiptera.Zzz", "Animalia.Arthropoda.Insecta.Hemiptera.Zzz");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Insecta.Hymenoptera.Braconidae.Zzz", "Animalia.Arthropoda.Insecta.Hymenoptera.ZzA");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Insecta.Hymenoptera.Crabronidae.Zzz", "Animalia.Arthropoda.Insecta.Hymenoptera.ZzA");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Insecta.Hymenoptera.Formicidae.Zzz", "Animalia.Arthropoda.Insecta.Hymenoptera.ZzA");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Insecta.Hymenoptera.Ichneumonidae.Zzz", "Animalia.Arthropoda.Insecta.Hymenoptera.ZzA");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Insecta.Hymenoptera.Pteromalidae.Zzz", "Animalia.Arthropoda.Insecta.Hymenoptera.ZzA");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Insecta.Hymenoptera.Zzz", "Animalia.Arthropoda.Insecta.Hymenoptera.Zzz");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Insecta.Lepidoptera.Erebidae.Zzz", "Animalia.Arthropoda.Insecta.Lepidoptera.ZzA");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Insecta.Lepidoptera.Geometridae.Zzz", "Animalia.Arthropoda.Insecta.Lepidoptera.ZzA");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Insecta.Lepidoptera.Lycaenidae.Zzz", "Animalia.Arthropoda.Insecta.Lepidoptera.ZzA");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Insecta.Lepidoptera.Crambidae.Zzz", "Animalia.Arthropoda.Insecta.Lepidoptera.ZzB");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Insecta.Lepidoptera.Hesperiidae.Zzz", "Animalia.Arthropoda.Insecta.Lepidoptera.ZzB");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Insecta.Lepidoptera.Noctuidae.Zzz", "Animalia.Arthropoda.Insecta.Lepidoptera.ZzB");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Insecta.Lepidoptera.Nymphalidae.Zzz", "Animalia.Arthropoda.Insecta.Lepidoptera.ZzB");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Insecta.Lepidoptera.Tortricidae.Zzz", "Animalia.Arthropoda.Insecta.Lepidoptera.ZzB");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Insecta.Lepidoptera.Zzz", "Animalia.Arthropoda.Insecta.Lepidoptera.Zzz");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Insecta.Neuroptera.Zzz", "Animalia.Arthropoda.Insecta.ZzA");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Insecta.Orthoptera.Zzz", "Animalia.Arthropoda.Insecta.ZzA");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Insecta.Thysanoptera.Zzz", "Animalia.Arthropoda.Insecta.ZzA");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Insecta.Zzz", "Animalia.Arthropoda.Insecta.Zzz");
		defaultKingdomPathsToClusterNames.put("Animalia.Arthropoda.Zzz", "Animalia.Arthropoda.Zzz");
		defaultKingdomPathsToClusterNames.put("Animalia.Chordata.Amphibia.Zzz", "Animalia.Chordata.ZzA");
		defaultKingdomPathsToClusterNames.put("Animalia.Chordata.Aves.Zzz", "Animalia.Chordata.ZzA");
		defaultKingdomPathsToClusterNames.put("Animalia.Chordata.Mammalia.Zzz", "Animalia.Chordata.ZzA");
		defaultKingdomPathsToClusterNames.put("Animalia.Chordata.Squamata.Zzz", "Animalia.Chordata.ZzA");
		defaultKingdomPathsToClusterNames.put("Animalia.Chordata.Zzz", "Animalia.Chordata.Zzz");
		defaultKingdomPathsToClusterNames.put("Animalia.Mollusca.Gastropoda.Neogastropoda.Zzz", "Animalia.Mollusca.Gastropoda.ZzA");
		defaultKingdomPathsToClusterNames.put("Animalia.Mollusca.Gastropoda.Stylommatophora.Zzz", "Animalia.Mollusca.Gastropoda.ZzA");
		defaultKingdomPathsToClusterNames.put("Animalia.Mollusca.Gastropoda.Zzz", "Animalia.Mollusca.Gastropoda.Zzz");
		defaultKingdomPathsToClusterNames.put("Animalia.Mollusca.Zzz", "Animalia.Mollusca.Zzz");
		defaultKingdomPathsToClusterNames.put("Animalia.Annelida.Zzz", "Animalia.ZzA");
		defaultKingdomPathsToClusterNames.put("Animalia.Bryozoa.Zzz", "Animalia.ZzA");
		defaultKingdomPathsToClusterNames.put("Animalia.Echinodermata.Zzz", "Animalia.ZzA");
		defaultKingdomPathsToClusterNames.put("Animalia.Nematoda.Zzz", "Animalia.ZzA");
		defaultKingdomPathsToClusterNames.put("Animalia.Zzz", "Animalia.Zzz");
		defaultKingdomPathsToClusterNames.put("Archaea.Zzz", "Archaea.Zzz");
		defaultKingdomPathsToClusterNames.put("Bacteria.Zzz", "Bacteria.Zzz");
		defaultKingdomPathsToClusterNames.put("Chromista.Zzz", "Chromista.Zzz");
		defaultKingdomPathsToClusterNames.put("Fungi.Ascomycota.Dothideomycetes.Zzz", "Fungi.Ascomycota.ZzA");
		defaultKingdomPathsToClusterNames.put("Fungi.Ascomycota.Sordariomycetes.Zzz", "Fungi.Ascomycota.ZzA");
		defaultKingdomPathsToClusterNames.put("Fungi.Ascomycota.Zzz", "Fungi.Ascomycota.Zzz");
		defaultKingdomPathsToClusterNames.put("Fungi.Zzz", "Fungi.Zzz");
		defaultKingdomPathsToClusterNames.put("Plantae.Tracheophyta.Liliopsida.Asparagales.Zzz", "Plantae.Tracheophyta.Liliopsida.Asparagales.Zzz");
		defaultKingdomPathsToClusterNames.put("Plantae.Tracheophyta.Liliopsida.Poales.Zzz", "Plantae.Tracheophyta.Liliopsida.Poales.Zzz");
		defaultKingdomPathsToClusterNames.put("Plantae.Tracheophyta.Liliopsida.Zzz", "Plantae.Tracheophyta.Liliopsida.Zzz");
		defaultKingdomPathsToClusterNames.put("Plantae.Tracheophyta.Magnoliopsida.Apiales.Zzz", "Plantae.Tracheophyta.Magnoliopsida.ZzE");
		defaultKingdomPathsToClusterNames.put("Plantae.Tracheophyta.Magnoliopsida.Asterales.Zzz", "Plantae.Tracheophyta.Magnoliopsida.Asterales.Zzz");
		defaultKingdomPathsToClusterNames.put("Plantae.Tracheophyta.Magnoliopsida.Boraginales.Zzz", "Plantae.Tracheophyta.Magnoliopsida.ZzE");
		defaultKingdomPathsToClusterNames.put("Plantae.Tracheophyta.Magnoliopsida.Brassicales.Zzz", "Plantae.Tracheophyta.Magnoliopsida.ZzE");
		defaultKingdomPathsToClusterNames.put("Plantae.Tracheophyta.Magnoliopsida.Caryophyllales.Zzz", "Plantae.Tracheophyta.Magnoliopsida.ZzC");
		defaultKingdomPathsToClusterNames.put("Plantae.Tracheophyta.Magnoliopsida.Ericales.Zzz", "Plantae.Tracheophyta.Magnoliopsida.ZzB");
		defaultKingdomPathsToClusterNames.put("Plantae.Tracheophyta.Magnoliopsida.Fabales.Zzz", "Plantae.Tracheophyta.Magnoliopsida.ZzB");
		defaultKingdomPathsToClusterNames.put("Plantae.Tracheophyta.Magnoliopsida.Gentianales.Zzz", "Plantae.Tracheophyta.Magnoliopsida.ZzA");
		defaultKingdomPathsToClusterNames.put("Plantae.Tracheophyta.Magnoliopsida.Lamiales.Zzz", "Plantae.Tracheophyta.Magnoliopsida.Lamiales.Zzz");
		defaultKingdomPathsToClusterNames.put("Plantae.Tracheophyta.Magnoliopsida.Malpighiales.Zzz", "Plantae.Tracheophyta.Magnoliopsida.ZzD");
		defaultKingdomPathsToClusterNames.put("Plantae.Tracheophyta.Magnoliopsida.Malvales.Zzz", "Plantae.Tracheophyta.Magnoliopsida.ZzD");
		defaultKingdomPathsToClusterNames.put("Plantae.Tracheophyta.Magnoliopsida.Myrtales.Zzz", "Plantae.Tracheophyta.Magnoliopsida.ZzC");
		defaultKingdomPathsToClusterNames.put("Plantae.Tracheophyta.Magnoliopsida.Ranunculales.Zzz", "Plantae.Tracheophyta.Magnoliopsida.ZzE");
		defaultKingdomPathsToClusterNames.put("Plantae.Tracheophyta.Magnoliopsida.Rosales.Zzz", "Plantae.Tracheophyta.Magnoliopsida.ZzA");
		defaultKingdomPathsToClusterNames.put("Plantae.Tracheophyta.Magnoliopsida.Sapindales.Zzz", "Plantae.Tracheophyta.Magnoliopsida.ZzD");
		defaultKingdomPathsToClusterNames.put("Plantae.Tracheophyta.Magnoliopsida.Saxifragales.Zzz", "Plantae.Tracheophyta.Magnoliopsida.ZzE");
		defaultKingdomPathsToClusterNames.put("Plantae.Tracheophyta.Magnoliopsida.Solanales.Zzz", "Plantae.Tracheophyta.Magnoliopsida.ZzE");
		defaultKingdomPathsToClusterNames.put("Plantae.Tracheophyta.Magnoliopsida.Zzz", "Plantae.Tracheophyta.Magnoliopsida.Zzz");
		defaultKingdomPathsToClusterNames.put("Plantae.Zzz", "Plantae.Zzz");
		defaultKingdomPathsToClusterNames.put("Protozoa.Zzz", "Protozoa.Zzz");
	}
	private static HashMap getKingdomPathsToClusterNames(File dwcaFolder) throws Exception {
		File dataTileFile = new File(dwcaFolder, "dataTiling.tsv");
		if (!dataTileFile.exists())
			return defaultKingdomPathsToClusterNames;
		
		//	read data tile starts
		HashMap kingdomPathsToClusterNames = new HashMap();
		BufferedReader dtsBr = new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(dataTileFile)), "UTF-8"));
		for (String dtl; (dtl = dtsBr.readLine()) != null;) {
			String[] dtd = dtl.split("\\t");
			if (dtd.length > 1)
				kingdomPathsToClusterNames.put(dtd[0], dtd[1]);
		}
		dtsBr.close();
		return kingdomPathsToClusterNames;
	}
	
	private static void analyzeData(String dwcaFolder) throws Exception {
		analyzeData(new File(dwcaFolder));
	}
	private static void analyzeData(File dwcaFolder) throws Exception {
		
		//	load epithet index
		HashMap epithetsToTaxonIDs = indexDataTiles(dwcaFolder);
		
		//	create sorted list of normalized epithets
		ArrayList epithets = new ArrayList(epithetsToTaxonIDs.keySet());
		Collections.sort(epithets);
		
		//	assess prefix frequencies
		CountingSet epithetPrefixCounts = new CountingSet(new HashMap());
		for (int pl = 1;; pl++) {
			boolean nothingNew = true;
			for (int e = 0; e < epithets.size(); e++) {
				String epithet = ((String) epithets.get(e));
				if (epithet.length() < pl)
					continue;
				if ((pl != 1) && epithetPrefixCounts.getCount(epithet.substring(0, (pl - 1)).toLowerCase()) < 5000)
					continue; // small enough a subset already
				epithetPrefixCounts.add(epithet.substring(0, pl).toLowerCase());
				nothingNew = false;
			}
			if (nothingNew)
				break;
		}
		
		//	print overview
		ArrayList epithetPrefixes = new ArrayList(epithetPrefixCounts);
		Collections.sort(epithetPrefixes, String.CASE_INSENSITIVE_ORDER);
		for (int p = 0; p < epithetPrefixes.size(); p++) {
			String prefix = ((String) epithetPrefixes.get(p));
			System.out.println(prefix + ": " + epithetPrefixCounts.getCount(prefix));
		}
	}
	
	private static HashMap indexDataTiles(File dwcaFolder) throws Exception {
		
		//	read data tile names
		ArrayList tileFileNames = new ArrayList();
		File dataTileFile = new File(dwcaFolder, "data.tiles.txt");
		BufferedReader dtBr = new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(dataTileFile)), "UTF-8"));
		for (String dtl; (dtl = dtBr.readLine()) != null;)
			tileFileNames.add(dtl.substring(0, dtl.indexOf("\t")));
		dtBr.close();
		System.out.println("data.tiles.txt" + " loaded");
		
		//	set up multi-map
		HashMap epithetsToTaxonIDs = new HashMap() {
			public Object put(Object key, Object value) {
				Object oldValue = this.get(key);
				if (oldValue == null)
					super.put(key, value);
				else if (oldValue instanceof ArrayList)
					((ArrayList) oldValue).add(value);
				else {
					ArrayList valueList = new ArrayList(4);
					valueList.add(oldValue);
					valueList.add(value);
					super.put(key, valueList);
				}
				return oldValue;
			}
		};
		
		//	process data tiles
		int idCount = 0;
		for (int tf = 0; tf < tileFileNames.size(); tf++) {
			String tileFileName = ((String) tileFileNames.get(tf));
			BufferedReader tBr = new BufferedReader(new InputStreamReader(new FileInputStream(new File(dwcaFolder, tileFileName)), "UTF-8"));
			int fIdCount = 0;
			for (String tl; (tl = tBr.readLine()) != null;) {
				idCount++;
				fIdCount++;
				
				//	parse data (keep indexes in sync with data output above !!!)
				//	TODO use constants, in CoL-Local proper !!!
				String[] td = tl.split("\\t");
				if (td.length < 6)
					continue;
				String id = td[0];
				if (id.indexOf("/") != -1)
					id = id.substring(0, id.indexOf("/"));
				String epithet = td[5];
				
				//	normalize and index epithet (we're indexing in all-lower-case)
				String lEpithet = epithet.toLowerCase();
				String nEpithet = lEpithet.replaceAll("[^a-zA-Z0-9\\-]", ""); // minus is kind of frequent, but spelling might vary with dot and high comma ...
				String idStr = ((epithet.equals(lEpithet) ? "EL:" : "EC:") + id);
				epithetsToTaxonIDs.put(nEpithet, idStr);
				
				//	index original epithet prefix in synonyms
				int validId = Integer.parseInt(td[4], 16);
				if ((validId != 0) && (td.length >= 8) && (td[7].length() != 0)) {
					TokenSequence originalPrefix = Gamta.newTokenSequence(td[7], null);
					for (int t = 0; t < originalPrefix.size(); t++) {
						String opToken = originalPrefix.valueAt(t);
						if (opToken.length() < 3)
							continue;
						if ("subsp".equals(opToken) || "var".equals(opToken) || "subvar".equals(opToken) || "f".equals(opToken) || "subf".equals(opToken))
							continue;
						String lOpToken = opToken.toLowerCase();
						String nOpToken = lOpToken.replaceAll("[^a-zA-Z0-9\\-]", ""); // minus is kind of frequent, but spelling might vary with dot and high comma ...
						idStr = ((opToken.equals(lOpToken) ? "PL:" : "PC:") + id);
						epithetsToTaxonIDs.put(nOpToken, idStr);
					}
				}
			}
			tBr.close();
			System.out.println(tileFileName + " read, got " + fIdCount + " IDs, for " + idCount + " IDs and " + epithetsToTaxonIDs.size() + " epithets in total");
		}
		System.out.println("Data files read, got " + idCount + " IDs and " + epithetsToTaxonIDs.size() + " epithets in total");
		
		//	finally ...
		return epithetsToTaxonIDs;
	}
	
	private static void index(String dwcaFolder) throws Exception {
		index(new File(dwcaFolder));
	}
	private static void index(File dwcaFolder) throws Exception {
		
		//	load epithet index
		HashMap epithetsToTaxonIDs = indexDataTiles(dwcaFolder);
		
		//	create sorted list of normalized epithets
		ArrayList epithets = new ArrayList(epithetsToTaxonIDs.keySet());
		Collections.sort(epithets);
		
		//	read custom index tile starts
		String[] indexTileStarts = getIndexTileStarts(dwcaFolder);
		
		//	distribute index data across tiles
		ArrayList indexTileListLines = new ArrayList();
		int indexTileEndIndex = 1;
		int indexTileEpithetCount = 0;
		int indexTileIdCount = 0;
		File indexTileFile = new File(dwcaFolder, "index.0.txt");
		BufferedWriter indexTileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(indexTileFile), "UTF-8"));
		for (int e = 0; e < epithets.size(); e++) {
			String epithet = ((String) epithets.get(e));
			Object idObj = epithetsToTaxonIDs.get(epithet);
			if (idObj == null)
				continue;
			if ((indexTileEndIndex < indexTileStarts.length) && (indexTileStarts[indexTileEndIndex].compareTo(epithet) <= 0)) {
				indexTileWriter.flush();
				indexTileWriter.close();
				indexTileListLines.add(indexTileFile.getName() + "\t" + indexTileStarts[indexTileEndIndex - 1] + "\t" + getIndexTileEnd(indexTileStarts[indexTileEndIndex]) + "\t" + indexTileEpithetCount + "\t" + indexTileIdCount);
				System.out.println("Finished index tile " + indexTileFile.getName() + " with " + indexTileEpithetCount + " terms and " + indexTileIdCount + " IDs");
				indexTileEpithetCount = 0;
				indexTileIdCount = 0;
				indexTileFile = new File(dwcaFolder, ("index." + indexTileStarts[indexTileEndIndex++] + ".txt"));
				indexTileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(indexTileFile), "UTF-8"));
			}
			String idStr;
			if (idObj instanceof String) {
				idStr = ((String) idObj);
				indexTileIdCount++;
			}
			else {
				ArrayList idList = ((ArrayList) idObj);
				Collections.sort(idList);
				StringBuffer idSb = new StringBuffer();
				String idPrefix = null;
				for (int i = 0; i < idList.size(); i++) {
					if (i != 0)
						idSb.append(";");
					String id = ((String) idList.get(i));
					String idPref = id.substring(0, "EL:".length());
					id = id.substring(idPref.length());
					if ((idPrefix == null) || !idPrefix.equals(idPref)) {
						idSb.append(idPref);
						idPrefix = idPref;
					}
					idSb.append(id);
				}
				idStr = idSb.toString();
				indexTileIdCount += idList.size();
			}
			indexTileWriter.write(epithet + "\t" + idStr);
			indexTileWriter.newLine();
			indexTileEpithetCount++;
		}
		indexTileWriter.flush();
		indexTileWriter.close();
		indexTileListLines.add(indexTileFile.getName() + "\t" + indexTileStarts[indexTileEndIndex - 1] + "\t" + getIndexTileEnd(null) + "\t" + indexTileEpithetCount + "\t" + indexTileIdCount);
		System.out.println("Finished index tile " + indexTileFile.getName() + " with " + indexTileEpithetCount + " terms and " + indexTileIdCount + " IDs");
		
		//	write index tile overview (don't do this on the fly)
		File indexTileListFile = new File(dwcaFolder, "index.tiles.txt");
		BufferedWriter itlBw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(indexTileListFile), "UTF-8"));
		for (int l = 0; l < indexTileListLines.size(); l++) {
			String itl = ((String) indexTileListLines.get(l));
			itlBw.write(itl);
			itlBw.newLine();
		}
		itlBw.flush();
		itlBw.close();
		System.out.println("Finished index tile list with " + indexTileListLines.size() + " entries");
	}
	private static String[] defaultIndexTileStarts = {"0", "a", "an", "b", "c", "ci", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "mf", "n", "o", "p", "pf", "pr", "q", "r", "s", "sm", "t", "u", "v", "w", "x", "y", "z"};
	private static String[] getIndexTileStarts(File dwcaFolder) throws Exception {
		File indexTileStartFile = new File(dwcaFolder, "indexTiling.tsv");
		if (!indexTileStartFile.exists())
			return defaultIndexTileStarts;
		
		//	read data tile starts
		ArrayList indexTileStartList = new ArrayList();
		BufferedReader itsBr = new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(indexTileStartFile)), "UTF-8"));
		for (String itsl; (itsl = itsBr.readLine()) != null;)
			indexTileStartList.add(itsl);
		itsBr.close();
		String[] indexTileStarts = ((String[]) indexTileStartList.toArray(new String[indexTileStartList.size()]));
		Arrays.sort(indexTileStarts);
		return indexTileStarts;
	}
	private static String getIndexTileEnd(String nextIndexTileStart) {
		if (nextIndexTileStart == null)
			return "zzzzzz";
		else if ("a".equals(nextIndexTileStart))
			return "0zzzzz";
		else if (nextIndexTileStart.length() == 1)
			return (((char) (nextIndexTileStart.charAt(0) - 1)) + "zzzzz");
		else if (nextIndexTileStart.length() == 2)
			return (nextIndexTileStart.substring(0, 1) + ((char) (nextIndexTileStart.charAt(1) - 1)) + "zzzz");
		else if (nextIndexTileStart.length() == 3)
			return (nextIndexTileStart.substring(0, 2) + ((char) (nextIndexTileStart.charAt(2) - 1)) + "zzz");
		else if (nextIndexTileStart.length() == 4)
			return (nextIndexTileStart.substring(0, 3) + ((char) (nextIndexTileStart.charAt(3) - 1)) + "zz");
		else return null;
	}
	
	private static SimpleDateFormat generationTimestampFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm", Locale.US) {
		{this.setTimeZone(TimeZone.getTimeZone("UTC"));}
	};
	private static void pack(String dwcaFolder, String destPath, boolean zip) throws Exception {
		pack(new File(dwcaFolder), destPath, zip);
	}
	private static void pack(File dwcaFolder, String destPath, boolean zip) throws Exception {
		
		//	store existing meta.txt lines
		ArrayList metaTxtLines = new ArrayList();
		File metaTxtFile = new File(dwcaFolder, "meta.txt");
		BufferedReader mtBr = new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(metaTxtFile)), "UTF-8"));
		for (String mtl; (mtl = mtBr.readLine()) != null;) {
			mtl = mtl.trim();
			if (mtl.length() != 0)
				metaTxtLines.add(mtl);
		}
		mtBr.close();
		System.out.println("meta.txt read");
		
		//	add timestamp
		long generateTime = System.currentTimeMillis();
		String generateTimestamp = generationTimestampFormat.format(new Date(generateTime));
		metaTxtLines.add("Generated: " + generateTimestamp);
		
		//	collect tile file names for output
		ArrayList tileFileNames = new ArrayList();
		HashMap tileFileHashes = new HashMap();
		tileFileNames.add("data.tiles.txt");
		tileFileNames.add("index.tiles.txt");
		
		//	compute hash values for all data files ...
		File dataTileFile = new File(dwcaFolder, "data.tiles.txt");
		String dataTileHash = computeHash(dataTileFile);
		metaTxtLines.add("data.tiles.txt" + "\t" + dataTileHash + "\t" + dataTileFile.lastModified());
		tileFileHashes.put("data.tiles.txt", dataTileHash);
		System.out.println("data.tiles.txt" + " hashed");
		BufferedReader dtBr = new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(dataTileFile)), "UTF-8"));
		for (String dtl; (dtl = dtBr.readLine()) != null;) {
			String dtFileName = dtl.substring(0, dtl.indexOf("\t"));
			File dtFile = new File(dwcaFolder, dtFileName);
			String dtFileHash = computeHash(dtFile);
			metaTxtLines.add(dtFileName + "\t" + dtFileHash + "\t" + dtFile.lastModified());
			tileFileHashes.put(dtFileName, dtFileHash);
			tileFileNames.add(dtFileName);
			System.out.println(dtFileName + " hashed");
		}
		dtBr.close();
		
		//	... as well as all index files
		File indexTileFile = new File(dwcaFolder, "index.tiles.txt");
		String indexTileHash = computeHash(indexTileFile);
		metaTxtLines.add("index.tiles.txt" + "\t" + indexTileHash + "\t" + indexTileFile.lastModified());
		tileFileHashes.put("index.tiles.txt", indexTileHash);
		System.out.println("index.tiles.txt" + " hashed");
		BufferedReader itBr = new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(indexTileFile)), "UTF-8"));
		for (String itl; (itl = itBr.readLine()) != null;) {
			String itFileName = itl.substring(0, itl.indexOf("\t"));
			File itFile = new File(dwcaFolder, itFileName);
			String itFileHash = computeHash(itFile);
			metaTxtLines.add(itFileName + "\t" + itFileHash + "\t" + itFile.lastModified());
			tileFileHashes.put(itFileName, itFileHash);
			tileFileNames.add(itFileName);
			System.out.println(itFileName + " hashed");
		}
		itBr.close();
		
		//	create ZIP ...
		if (zip) {
			File outFile = ((destPath == null) ? new File(dwcaFolder, "CatalogOfLifeLocal.data.zip") : new File(destPath));
			ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outFile)));
			
			//	write meta.txt first for fast access
			ZipEntry ze = new ZipEntry("meta.txt");
			ze.setTime(generateTime);
			out.putNextEntry(ze);
			for (int l = 0; l < metaTxtLines.size(); l++) {
				String mtl = ((String) metaTxtLines.get(l));
				out.write(mtl.getBytes("UTF-8"));
				out.write("\r\n".getBytes("UTF-8"));
			}
			out.flush();
			out.closeEntry();
			System.out.println("meta.txt" + " added to " + outFile.getName());
			
			//	write tile files
			for (int f = 0; f < tileFileNames.size(); f++) {
				String tfn = ((String) tileFileNames.get(f));
				ze = new ZipEntry(tfn);
				ze.setTime(generateTime);
				out.putNextEntry(ze);
				BufferedInputStream tfIn = new BufferedInputStream(new FileInputStream(new File(dwcaFolder, tfn)));
				byte[] buffer = new byte[1024];
				for (int r; (r = tfIn.read(buffer, 0, buffer.length)) != -1;)
					out.write(buffer, 0, r);
				tfIn.close();
				out.flush();
				out.closeEntry();
				System.out.println(tfn + " added to " + outFile.getName());
			}
			
			//	finally ...
			out.flush();
			out.close();
			System.out.println(outFile.getName() + " finished");
		}
		
		//	... or hash based copy
		else {
			File outFolder = ((destPath == null) ? new File(dwcaFolder, generateTimestamp) : new File(destPath));
			outFolder.mkdirs();
			
			//	write meta.txt
			File outFile = new File(outFolder, "meta.txt");
			BufferedWriter outBw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8"));
			for (int l = 0; l < metaTxtLines.size(); l++) {
				String mtl = ((String) metaTxtLines.get(l));
				outBw.write(mtl);
				outBw.newLine();
			}
			outBw.flush();
			outBw.close();
			outFile.setLastModified(generateTime);
			System.out.println("meta.txt written to " + outFile.getName());
			
			//	write tile files
			for (int f = 0; f < tileFileNames.size(); f++) {
				String tfn = ((String) tileFileNames.get(f));
				String tfh = ((String) tileFileHashes.get(tfn));
				String outFileName = ((tfn.indexOf(".") == -1) ? (tfn + "." + tfh) : (tfn.substring(0, tfn.lastIndexOf(".")) + "." + tfh + tfn.substring(tfn.lastIndexOf("."))));
				outFile = new File(outFolder, outFileName);
				BufferedInputStream tfIn = new BufferedInputStream(new FileInputStream(new File(dwcaFolder, tfn)));
				BufferedOutputStream tfOut = new BufferedOutputStream(new FileOutputStream(new File(outFolder, outFileName)));
				byte[] buffer = new byte[1024];
				for (int r; (r = tfIn.read(buffer, 0, buffer.length)) != -1;)
					tfOut.write(buffer, 0, r);
				tfIn.close();
				tfOut.flush();
				tfOut.close();
				System.out.println(tfn + " written to " + outFile.getName());
			}
			System.out.println(outFolder.getAbsolutePath() + " finished");
		}
	}
	private static String computeHash(File file) throws Exception {
		BufferedInputStream fin = new BufferedInputStream(new FileInputStream(file));
		byte[] buffer = new byte[1024];
		MD5 md5 = new HashUtils.MD5();
		for (int r; (r = fin.read(buffer, 0, buffer.length)) != -1;)
			md5.update(buffer, 0, r);
		fin.close();
		return md5.digestString();
	}
	
	private static void generate(String dwcaPath, String destPath, boolean zip) throws Exception {
		File dwca = new File(dwcaPath);
		File dwcaFolder = dwca.getParentFile();
		unzip(dwca, dwcaFolder);
		distill(dwcaFolder);
		index(dwcaFolder);
		pack(dwcaFolder, destPath, zip);
	}
	
	private static void serve(String colPath, int port, boolean verbose) throws Exception {
		
		//	get CoL-Local instance
		File dataPath = new File(colPath);
		
		//	load meta.txt
		final LinkedHashMap dataNamesToHashes = new LinkedHashMap();
		try {
			BufferedReader metaBr = new BufferedReader(new InputStreamReader(new FileInputStream(new File(dataPath, "meta.txt")), "UTF-8"));
			for (String metaLine; (metaLine = metaBr.readLine()) != null;) {
				if (metaLine.startsWith("Dump:"))
					continue;
				if (metaLine.startsWith("Generated:"))
					continue;
				String[] tileData = metaLine.split("\t");
				if (2 <= tileData.length)
					dataNamesToHashes.put(tileData[0], tileData[1]);
			}
			metaBr.close();
		}
		catch (IOException ioe) {
			System.out.println("ColLocalProvider: Error loading metadata: " + ioe.getMessage());
			ioe.printStackTrace(System.out);
			throw new RuntimeException(ioe); // no use even loading without our data
		}
		
		//	initialize CoL-Local from own data provider
		CatalogOfLifeLocal col = CatalogOfLifeLocal.getInstance(new AnalyzerDataProviderFileBased(dataPath) {
			public boolean isDataAvailable(String dataName) {
				return super.isDataAvailable(this.prepareDataName(dataName));
			}
			public InputStream getInputStream(String dataName) throws IOException {
				return super.getInputStream(this.prepareDataName(dataName));
			}
			public URL getURL(String dataName) throws IOException {
				return null; // no need for web access in local data structure
			}
			public boolean isDataEditable() {
				return false; // no editing at the hands of the lookup data structure
			}
			public boolean isDataEditable(String dataName) {
				return false; // no editing at the hands of the lookup data structure
			}
			public OutputStream getOutputStream(String dataName) throws IOException {
				return null; // no editing at the hands of the lookup data structure
			}
			public boolean deleteData(String name) {
				return false; // no editing at the hands of the lookup data structure
			}
			public String[] getDataNames() {
				return ((String[]) dataNamesToHashes.keySet().toArray(new String[dataNamesToHashes.size()]));
			}
			public boolean equals(AnalyzerDataProvider adp) {
				return this.getAbsolutePath().equals(adp.getAbsolutePath());
			}
			private String prepareDataName(String dataName) {
				String dataHash = ((String) dataNamesToHashes.get(dataName));
				if (dataHash == null)
					return dataName;
				else if (dataName.indexOf(".") == -1)
					return (dataName + "." + dataHash);
				return (dataName.substring(0, dataName.lastIndexOf(".")) + "." + dataHash + dataName.substring(dataName.lastIndexOf(".")));
			}
		});
		
		//	start server
		ColLocalServer srv = new ColLocalServer(port, col);
		srv.setVerbose(verbose);
		srv.start();
		System.out.println("Use 'verbose' or 'silent' to toggle logging");
		System.out.println("Use 'exit' to shut down server");
		
		//	wait for exit command
		BufferedReader sysIn = new BufferedReader(new InputStreamReader(System.in));
		for (String input; (input = sysIn.readLine()) != null;) {
			if ("exit".equals(input)) {
				srv.close();
				break;
			}
			else if ("verbose".equals(input))
				srv.setVerbose(true);
			else if ("silent".equals(input))
				srv.setVerbose(false);
			else {
				System.out.println("Invalid command '" + "'");
				System.out.println("- use 'verbose' or 'silent' to toggle logging");
				System.out.println("- use 'exit' to shut down server");
			}
		}
	}
	
	private static class ColLocalServer extends TinyHttpServer {
		CatalogOfLifeLocal col;
		ColLocalServer(int port, CatalogOfLifeLocal col) throws IOException {
			super("ColLocalServer", port);
			this.col = col;
		}
		protected void serviceGet(String path, String queryParamStr, Properties headers, InputStream request, OutputStream response) throws Exception {
			ByteArrayOutputStream responseData = null;
			Writer responseWriter = null;
			String error = null;
			try {
				if (path.startsWith("/id/")) {
					TaxonRecord tr = this.col.getRecord(Integer.parseInt(path.substring("/id/".length())));
					if (tr != null) {
						responseData = new ByteArrayOutputStream();
						responseWriter = new OutputStreamWriter(responseData, "UTF-8");
						this.writeTaxonRecord(tr, responseWriter, true, true);
					}
				}
				else if ("/find".equals(path)) {
					Properties queryParams = this.parseQuery(queryParamStr);
					String query = queryParams.getProperty("query");
					String[] queryParts = null;
					if ((query != null) && (query.indexOf(" ") != -1)) {
						queryParts = query.split("\\s+");
						query = query.substring(query.lastIndexOf(" ") + " ".length());
					}
//					String rank = queryParams.getProperty("rank");
					String fromRank = queryParams.getProperty("fromRank", queryParams.getProperty("rank", KINGDOM_ATTRIBUTE));
					String toRank = queryParams.getProperty("toRank", queryParams.getProperty("rank", SUBFORM_ATTRIBUTE));
					boolean prefixMatch = "true".equals(queryParams.getProperty("prefix", "false"));
					boolean caseSensitive = !"false".equals(queryParams.getProperty("matchCase", "true"));
					boolean includeSynonyms = "true".equals(queryParams.getProperty("synonyms", "false"));
					boolean expandLinked = "true".equals(queryParams.getProperty("expandLinked", "false"));
					boolean includeHigher = "true".equals(queryParams.getProperty("includeHigher", "false"));
					TaxonRecord[] trs = ((query == null) ? null : this.col.findTaxonRecords(query, fromRank, toRank, prefixMatch, caseSensitive, includeSynonyms));
					responseData = new ByteArrayOutputStream();
					responseWriter = new OutputStreamWriter(responseData, "UTF-8");
					responseWriter.write("[");
					if (trs != null) {
						boolean addSeparator = false;
						for (int r = 0; r < trs.length; r++)
							if (this.matches(trs[r], queryParts, prefixMatch, caseSensitive)) {
								if (addSeparator)
									responseWriter.write(",");
								this.writeTaxonRecord(trs[r], responseWriter, includeHigher, expandLinked);
								addSeparator = true;
							}
					}
					responseWriter.write("]");
				}
				else if ("/search".equals(path)) {
					Properties queryParams = this.parseQuery(queryParamStr);
					String query = queryParams.getProperty("query");
					String[] queryParts = null;
					if ((query != null) && (query.indexOf(" ") != -1)) {
						queryParts = query.split("\\s+");
						query = query.substring(query.lastIndexOf(" ") + " ".length());
					}
					boolean prefixMatch = "true".equals(queryParams.getProperty("prefix", "false"));
					boolean caseSensitive = !"false".equals(queryParams.getProperty("matchCase", "true"));
					boolean includeSynonyms = "true".equals(queryParams.getProperty("synonyms", "false"));
					boolean expandLinked = "true".equals(queryParams.getProperty("expandLinked", "false"));
					boolean includeHigher = "true".equals(queryParams.getProperty("includeHigher", "false"));
					IndexEntry[] ies = ((query == null) ? null : this.col.searchTaxonRecords(query, prefixMatch));
					responseData = new ByteArrayOutputStream();
					responseWriter = new OutputStreamWriter(responseData, "UTF-8");
					responseWriter.write("[");
					if (ies != null) {
						boolean addLowerCaseResults = (!caseSensitive || query.equals(query.toLowerCase()) /* lower case query */);
						boolean addCapitalizedResults = (!caseSensitive || !query.equals(query.toLowerCase()) /* capitalized query */);
//						boolean includeSynonyms = false;
						HashSet resIDs = new HashSet();
						boolean addSeparator = false;
						for (int e = 0; e < ies.length; e++) {
							int[] ids;
							if (addLowerCaseResults) {
								ids = ies[e].getLowerCaseMatchIDs();
								for (int i = 0; (ids != null) && (i < ids.length); i++)
									if (resIDs.add(new Integer(ids[i]))) {
										TaxonRecord tr = this.col.getRecord(ids[i]);
										if (this.matches(tr, queryParts, prefixMatch, caseSensitive)) {
											if (addSeparator)
												responseWriter.write(",");
											this.writeTaxonRecord(tr, responseWriter, includeHigher, expandLinked);
											addSeparator = true;
										}
									}
								if (includeSynonyms) {
									ids = ies[e].getLowerCasePrefixMatchIDs();
									for (int i = 0; (ids != null) && (i < ids.length); i++)
										if (resIDs.add(new Integer(ids[i]))) {
											TaxonRecord tr = this.col.getRecord(ids[i]);
											if (this.matches(tr, queryParts, prefixMatch, caseSensitive)) {
												if (addSeparator)
													responseWriter.write(",");
												this.writeTaxonRecord(tr, responseWriter, includeHigher, expandLinked);
												addSeparator = true;
											}
										}
								}
							}
							if (addCapitalizedResults) {
								ids = ies[e].getCapitalizedMatchIDs();
								for (int i = 0; (ids != null) && (i < ids.length); i++)
									if (resIDs.add(new Integer(ids[i]))) {
										TaxonRecord tr = this.col.getRecord(ids[i]);
										if (this.matches(tr, queryParts, prefixMatch, caseSensitive)) {
											if (addSeparator)
												responseWriter.write(",");
											this.writeTaxonRecord(tr, responseWriter, includeHigher, expandLinked);
											addSeparator = true;
										}
									}
								if (includeSynonyms) {
									ids = ies[e].getCapitalizedPrefixMatchIDs();
									for (int i = 0; (ids != null) && (i < ids.length); i++)
										if (resIDs.add(new Integer(ids[i]))) {
											TaxonRecord tr = this.col.getRecord(ids[i]);
											if (this.matches(tr, queryParts, prefixMatch, caseSensitive)) {
												if (addSeparator)
													responseWriter.write(",");
												this.writeTaxonRecord(tr, responseWriter, includeHigher, expandLinked);
												addSeparator = true;
											}
										}
								}
							}
						}
					}
					responseWriter.write("]");
				}
				else {
					String docResName = CatalogOfLifeDataTool.class.getName();
					docResName = docResName.substring(0, docResName.lastIndexOf('.'));
					docResName = docResName.replaceAll("\\.", "/");
					docResName = (docResName + "/api.html");
					InputStream docIn = CatalogOfLifeDataTool.class.getClassLoader().getResourceAsStream(docResName);
					responseData = new ByteArrayOutputStream();
					for (int r; (r = docIn.read()) != -1;)
						responseData.write(r);
					docIn.close();
				}
			}
			catch (Exception e) {
				System.out.println("Error performing query: " + e.getMessage());
				e.printStackTrace(System.out);
				error = e.getMessage();
			}
			
			//	send response
			if (responseData != null) {
				this.writeStatus(200, "OK", response);
				if (responseWriter == null)
					this.writeHeader("Content-Type", "text/html; charset=UTF-8", response);
				else {
					responseWriter.flush();
					this.writeHeader("Content-Type", "application/json; charset=UTF-8", response);
				}
				this.writeHeader("Content-Length", ("" + responseData.size()), response);
			}
			else if (error != null) {
				this.writeStatus(500, null, response);
				this.writeHeader("Content-Type", "text/plain; charset=UTF-8", response);
				this.writeHeader("Content-Length", ("" + error.length()), response);
			}
			else {
				this.writeStatus(404, null, response);
				this.writeHeader("Content-Type", "text/plain; charset=UTF-8", response);
				this.writeHeader("Content-Length", ("" + "Not Found".length()), response);
			}
			this.writeDateHeader(-1, response);
			this.writeHeader("Server", "CoL-Local Tiny HTTP", response);
			this.writeHeader("Connection", "close", response);
			this.writeLineBreak(response);
			if (responseData != null) {
				responseData.writeTo(response);
				this.writeLineBreak(response);
			}
			else if (error != null) {
				response.write(error.getBytes());
				this.writeLineBreak(response);
			}
			else {
				response.write("Not Found".getBytes());
				this.writeLineBreak(response);
			}
			response.flush();
		}
		private boolean matches(TaxonRecord tr, String[] queryPrefix, boolean prefixMatch, boolean caseSensitive) {
			return ((queryPrefix == null) || CatalogOfLifeLocal.matchesEpithetPrefix(tr, queryPrefix, 0, queryPrefix.length, CatalogOfLifeLocal.STALE_TAXON_MODE_INTERMEDIATE, prefixMatch, caseSensitive));
		}
		private void writeTaxonRecord(TaxonRecord tr, Writer response, boolean includeHigher, boolean expandLinked) throws IOException {
			this.writeTaxonRecord(tr, true, response, includeHigher, expandLinked);
		}
		private void writeTaxonRecord(TaxonRecord tr, boolean includeLinks, Writer response, boolean includeHigher, boolean expandLinked) throws IOException {
			response.write("{");
			response.write("\"id\": " + tr.getId() + ",");
			response.write("\"name\": \"" + JsonParser.escape(tr.getNameString(false)) + "\",");
			String auth = tr.getAuthority();
			if (auth != null)
				response.write("\"authority\": \"" + JsonParser.escape(auth) + "\",");
			if (tr.isValidTaxon()) {
				response.write("\"isSynonym\": false,");
				if (includeLinks) {
					int pTrId = TaxonRecord.getParentOrValidId(tr.data, tr.offset);
					if (pTrId == 0) {}
					else if (expandLinked) {
						TaxonRecord pTr = tr.getParent();
						response.write("\"parent\": ");
						this.writeTaxonRecord(pTr, false, response, false, false);
						response.write(",");
					}
					else response.write("\"parentId\": " + pTrId + ",");
					if (includeHigher)
						for (TaxonRecord hTr = tr.getPrimaryParent(); hTr != null; hTr = hTr.getPrimaryParent()) {
							if (expandLinked) {
								response.write("\"" + hTr.getRank() + "\": ");
								this.writeTaxonRecord(hTr, false, response, false, false);
								response.write(",");
							}
							else response.write("\"" + hTr.getRank() + "\": \"" + JsonParser.escape(hTr.getNameString(false)) + "\",");
						}
					int[] cTrIDs = TaxonRecord.getChildIDs(tr.data, tr.offset);
					if ((cTrIDs != null) && (cTrIDs.length != 0)) {
						response.write("\"" + (expandLinked ? "children" : "childIDs") + "\": [");
						for (int c = 0; c < cTrIDs.length; c++) {
							if (c != 0)
								response.write(",");
							if (expandLinked) {
								TaxonRecord cTr = this.col.getRecord(cTrIDs[c]);
								this.writeTaxonRecord(cTr, false, response, false, false);
							}
							else response.write("" + cTrIDs[c]);
						}
						response.write("],");
					}
					int[] sTrIDs = TaxonRecord.getSynonymIDs(tr.data, tr.offset);
					if ((sTrIDs != null) && (sTrIDs.length != 0)) {
						response.write("\"" + (expandLinked ? "synonyms" : "synonymIDs") + "\": [");
						for (int s = 0; s < sTrIDs.length; s++) {
							if (s != 0)
								response.write(",");
							if (expandLinked) {
								TaxonRecord sTr = this.col.getRecord(sTrIDs[s]);
								this.writeTaxonRecord(sTr, false, response, false, false);
							}
							else response.write("" + sTrIDs[s]);
						}
						response.write("],");
					}
				}
			}
			else {
				response.write("\"isSynonym\": true,");
				int vTrId = TaxonRecord.getParentOrValidId(tr.data, tr.offset);
				if (vTrId == 0) {}
				else if (expandLinked) {
					TaxonRecord vTr = this.col.getRecord(vTrId);
					response.write("\"valid\": ");
					this.writeTaxonRecord(vTr, false, response, false, false);
					response.write(",");
				}
				else response.write("\"validId\": " + vTrId + ",");
				if (includeHigher && (vTrId != 0)) {
					TaxonRecord vTr = this.col.getRecord(vTrId);
					for (TaxonRecord hTr = vTr.getPrimaryParent(); hTr != null; hTr = hTr.getPrimaryParent()) {
						if (expandLinked) {
							response.write("\"" + hTr.getRank() + "\": ");
							this.writeTaxonRecord(hTr, false, response, false, false);
							response.write(",");
						}
						else response.write("\"" + hTr.getRank() + "\": \"" + JsonParser.escape(hTr.getNameString(false)) + "\",");
					}
				}
			}
			response.write("\"rank\": \"" + tr.getRank() + "\"");
			response.write("}");
		}
	}
	
	//	HELPER FOR IN-IDE TESTING
	private static class TestHelper {
		public static void main(String[] args) throws Exception {
//			String tl = "HLYR\t346X\tnull\tnull\t1140\tACCEPTED\tspecies\tAsplenium x ligusticum Bernardello, Marchetti & van den Heede, 2012\tAsplenium\tligusticum\theede\t\t\tICN\t\t\t";
//			String[] td = tl.split("\\t");
//			MetaXml metaXml = getMetaXml("E:/Projektdaten/CoL2021");
//			DataValueNormalizer valueNormalizer = getDataValueNormalizer("E:/Projektdaten/CoL2021");
//			String[] nTd = normalizeValues(metaXml, valueNormalizer, tl, td, true, null, null, null, null, null, null);
//			System.out.println(Arrays.toString(nTd));
			args = new String[3];
			args[0] = "serve";
			args[1] = "E:/Projektdaten/CoL2021/2021-07-23-00-49";
//			args[2] = "36667";
			args[2] = "-v";
			CatalogOfLifeDataTool.main(args);
		}
	}
}
