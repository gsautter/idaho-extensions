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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.uka.ipd.idaho.gamta.util.AnalyzerDataProvider;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProviderFileBased;
import de.uka.ipd.idaho.gamta.util.CountingSet;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicNameConstants;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicNameUtils;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicNameUtils.TaxonomicAuthority;
import de.uka.ipd.idaho.plugins.taxonomicNames.TaxonomicNameUtils.TaxonomicName;
import de.uka.ipd.idaho.stringUtils.StringUtils;

/**
 * @author sautter
 *
 */
public class CatalogOfLifeLocal implements TaxonomicNameConstants {
	private static final TreeMap primaryRanks = new TreeMap(String.CASE_INSENSITIVE_ORDER);
	static {
		primaryRanks.put(KINGDOM_ATTRIBUTE, KINGDOM_ATTRIBUTE);
		primaryRanks.put(PHYLUM_ATTRIBUTE, PHYLUM_ATTRIBUTE);
		primaryRanks.put(CLASS_ATTRIBUTE, CLASS_ATTRIBUTE);
		primaryRanks.put(ORDER_ATTRIBUTE, ORDER_ATTRIBUTE);
		primaryRanks.put(FAMILY_ATTRIBUTE, FAMILY_ATTRIBUTE);
		primaryRanks.put(GENUS_ATTRIBUTE, GENUS_ATTRIBUTE);
		primaryRanks.put(SPECIES_ATTRIBUTE, SPECIES_ATTRIBUTE);
	}
	private static final LinkedHashMap ranksToLevels = new LinkedHashMap();
	private static final String[] levelsToRanks;
	private static final int orderRankLevel;
	private static final int familyRankLevel;
	private static final int genusRankLevel;
	private static final int speciesRankLevel;
	static {
		ranksToLevels.put(SUPERKINGDOM_ATTRIBUTE, new Byte((byte) ranksToLevels.size()));
		ranksToLevels.put(KINGDOM_ATTRIBUTE, new Byte((byte) ranksToLevels.size()));
		ranksToLevels.put(SUBKINGDOM_ATTRIBUTE, new Byte((byte) ranksToLevels.size()));
		ranksToLevels.put(INFRAKINGDOM_ATTRIBUTE, new Byte((byte) ranksToLevels.size()));
		
		ranksToLevels.put(SUPERPHYLUM_ATTRIBUTE, new Byte((byte) ranksToLevels.size()));
		ranksToLevels.put(PHYLUM_ATTRIBUTE, new Byte((byte) ranksToLevels.size()));
		ranksToLevels.put(SUBPHYLUM_ATTRIBUTE, new Byte((byte) ranksToLevels.size()));
		ranksToLevels.put(INFRAPHYLUM_ATTRIBUTE, new Byte((byte) ranksToLevels.size()));
		ranksToLevels.put(PARVPHYLUM_ATTRIBUTE, new Byte((byte) ranksToLevels.size()));
		
		ranksToLevels.put(SUPERCLASS_ATTRIBUTE, new Byte((byte) ranksToLevels.size()));
		ranksToLevels.put(CLASS_ATTRIBUTE, new Byte((byte) ranksToLevels.size()));
		ranksToLevels.put(SUBCLASS_ATTRIBUTE, new Byte((byte) ranksToLevels.size()));
		ranksToLevels.put(INFRACLASS_ATTRIBUTE, new Byte((byte) ranksToLevels.size()));
		ranksToLevels.put(PARVCLASS_ATTRIBUTE, new Byte((byte) ranksToLevels.size()));
		
		ranksToLevels.put(SUPERORDER_ATTRIBUTE, new Byte((byte) ranksToLevels.size()));
		ranksToLevels.put(ORDER_ATTRIBUTE, new Byte((byte) ranksToLevels.size()));
		ranksToLevels.put(SUBORDER_ATTRIBUTE, new Byte((byte) ranksToLevels.size()));
		ranksToLevels.put(INFRAORDER_ATTRIBUTE, new Byte((byte) ranksToLevels.size()));
		ranksToLevels.put(PARVORDER_ATTRIBUTE, new Byte((byte) ranksToLevels.size()));
		
		ranksToLevels.put(SUPERFAMILY_ATTRIBUTE, new Byte((byte) ranksToLevels.size()));
		ranksToLevels.put("epiFamily", new Byte((byte) ranksToLevels.size()));
		ranksToLevels.put(FAMILY_ATTRIBUTE, new Byte((byte) ranksToLevels.size()));
		ranksToLevels.put(SUBFAMILY_ATTRIBUTE, new Byte((byte) ranksToLevels.size()));
		ranksToLevels.put(INFRAFAMILY_ATTRIBUTE, new Byte((byte) ranksToLevels.size()));
		
		ranksToLevels.put(SUPERTRIBE_ATTRIBUTE, new Byte((byte) ranksToLevels.size()));
		ranksToLevels.put(TRIBE_ATTRIBUTE, new Byte((byte) ranksToLevels.size()));
		ranksToLevels.put(SUBTRIBE_ATTRIBUTE, new Byte((byte) ranksToLevels.size()));
		ranksToLevels.put(INFRATRIBE_ATTRIBUTE, new Byte((byte) ranksToLevels.size()));
		
		ranksToLevels.put(GENUS_ATTRIBUTE, new Byte((byte) ranksToLevels.size()));
		ranksToLevels.put(SUBGENUS_ATTRIBUTE, new Byte((byte) ranksToLevels.size()));
		ranksToLevels.put(INFRAGENUS_ATTRIBUTE, new Byte((byte) ranksToLevels.size()));
		ranksToLevels.put(SECTION_ATTRIBUTE, new Byte((byte) ranksToLevels.size()));
		ranksToLevels.put(SUBSECTION_ATTRIBUTE, new Byte((byte) ranksToLevels.size()));
		ranksToLevels.put(SERIES_ATTRIBUTE, new Byte((byte) ranksToLevels.size()));
		ranksToLevels.put(SUBSERIES_ATTRIBUTE, new Byte((byte) ranksToLevels.size()));
		
		ranksToLevels.put(SPECIES_ATTRIBUTE, new Byte((byte) ranksToLevels.size()));
		ranksToLevels.put(SUBSPECIES_ATTRIBUTE, new Byte((byte) ranksToLevels.size()));
		ranksToLevels.put(INFRASPECIES_ATTRIBUTE, new Byte((byte) ranksToLevels.size()));
		ranksToLevels.put(VARIETY_ATTRIBUTE, new Byte((byte) ranksToLevels.size()));
		ranksToLevels.put(SUBVARIETY_ATTRIBUTE, new Byte((byte) ranksToLevels.size()));
		ranksToLevels.put(FORM_ATTRIBUTE, new Byte((byte) ranksToLevels.size()));
		ranksToLevels.put(SUBFORM_ATTRIBUTE, new Byte((byte) ranksToLevels.size()));
		
		orderRankLevel = ((Byte) ranksToLevels.get(ORDER_ATTRIBUTE)).intValue();
		familyRankLevel = ((Byte) ranksToLevels.get(FAMILY_ATTRIBUTE)).intValue();
		genusRankLevel = ((Byte) ranksToLevels.get(GENUS_ATTRIBUTE)).intValue();
		speciesRankLevel = ((Byte) ranksToLevels.get(SPECIES_ATTRIBUTE)).intValue();
		levelsToRanks = new String[ranksToLevels.size()];
		for (Iterator rit = ranksToLevels.keySet().iterator(); rit.hasNext();) {
			String rank = ((String) rit.next());
			Byte level = ((Byte) ranksToLevels.get(rank));
			levelsToRanks[level.intValue()] = rank;
		}
	}
	
	private AnalyzerDataProvider dataProvider;
	private DataTile higherTile;
	private DataTileProxy[] speciesTiles;
	private DataTileProxy lastSpeciesTile;
	private IndexTileProxy[] indexTiles;
	private CatalogOfLifeLocal(AnalyzerDataProvider dataProvider) throws IOException {
		this.dataProvider = dataProvider;
		
		DataTileProxy higherTile = null;
		ArrayList speciesTiles = new ArrayList();
		BufferedReader dtBr = new BufferedReader(new InputStreamReader(getInputStream("data.tiles.txt"), "UTF-8"));
		for (String tileStr; (tileStr = dtBr.readLine()) != null;) {
			String[] tileData = tileStr.split("\\t");
			if (tileData.length < 3)
				continue;
			String fileName = tileData[0];
			int minId = Integer.parseInt(tileData[1], 16);
			int maxId = Integer.parseInt(tileData[2], 16);
//			int minColId = ((tileData.length < 5) ? -1 : Integer.parseInt(tileData[3], 16));
//			int maxColId = ((tileData.length < 5) ? -1 : Integer.parseInt(tileData[4], 16));
			int minColId = ((tileData.length < 5) ? -1 : parseIntBase29(tileData[3]));
			int maxColId = ((tileData.length < 5) ? -1 : parseIntBase29(tileData[4]));
			if ("data.higher.txt".equals(fileName))
//				higherTile = new DataTileProxy(this, minId, maxId, fileName);
				higherTile = new DataTileProxy(this, minId, maxId, minColId, maxColId, fileName);
//			else speciesTiles.add(new DataTileProxy(this, minId, maxId, fileName));
			else speciesTiles.add(new DataTileProxy(this, minId, maxId, minColId, maxColId, fileName));
		}
		dtBr.close();
		
		if (higherTile == null)
			throw new IOException("Unable to find record for tile with taxa above species.");
		this.higherTile = this.loadDataTile(higherTile.fileName, higherTile.minId, higherTile.maxId);
		this.speciesTiles = ((DataTileProxy[]) speciesTiles.toArray(new DataTileProxy[speciesTiles.size()]));
		
		Arrays.sort(this.speciesTiles);
		for (int t = 0; t < this.speciesTiles.length; t++) {
			DataTileProxy prevTile = ((t == 0) ? higherTile : this.speciesTiles[t-1]);
			if (this.speciesTiles[t].minId <= prevTile.maxId)
				throw new IllegalArgumentException("Data tiles must not overlap in their ID ranges, but tiles " + prevTile + " and " + this.speciesTiles[t] + " are");
		}
		
		ArrayList indexTiles = new ArrayList();
		BufferedReader itBr = new BufferedReader(new InputStreamReader(getInputStream("index.tiles.txt"), "UTF-8"));
		for (String tileStr; (tileStr = itBr.readLine()) != null;) {
			String[] tileData = tileStr.split("\\t");
			if (tileData.length < 3)
				continue;
			String fileName = tileData[0];
			byte[] minStr = encodeString(tileData[1]);
			byte[] maxStr = encodeString(tileData[2]);
			indexTiles.add(new IndexTileProxy(this, minStr, maxStr, fileName));
		}
		itBr.close();
		
		this.indexTiles = ((IndexTileProxy[]) indexTiles.toArray(new IndexTileProxy[indexTiles.size()]));
		Arrays.sort(this.indexTiles);
		for (int t = 1; t < this.indexTiles.length; t++) {
			IndexTileProxy prevTile = this.indexTiles[t-1];
			if (compareStringBytes(this.indexTiles[t].minStr, 0, this.indexTiles[t].minStr.length, prevTile.maxStr, 0, prevTile.maxStr.length, true) < 0)
				throw new IllegalArgumentException("Index tiles must not overlap in their term ranges, but tiles " + prevTile + " and " + this.indexTiles[t] + " are");
		}
	}
	
	private int maxSimultaneousSpeciesTiles = -1;
	private int speciesTileUseNumber = 0;
	synchronized DataTile getDataTileForId(int id) {
		if ((this.lastSpeciesTile != null) && this.lastSpeciesTile.containsRecord(id))
			return this.lastSpeciesTile.getTile();
		if (this.higherTile.containsRecord(id))
			return this.higherTile;
		int low = 0;
		int high = (this.speciesTiles.length - 1);
		while (low <= high) {
			int mid = ((low + high) / 2);
			if (id < this.speciesTiles[mid].minId)
				high = (mid - 1);
			else if (this.speciesTiles[mid].maxId < id)
				low = (mid + 1);
			else {
				this.lastSpeciesTile = this.speciesTiles[mid];
				this.lastSpeciesTile.lastTileUseNumber = this.speciesTileUseNumber++;
				return this.lastSpeciesTile.getTile();
			}
		}
		return null;
	}
	
	private CountingSet descendantTileCounts = null;
	int getDescendantTileCount(int id) {
		if (this.descendantTileCounts == null) {
			System.out.println("COLLECTING DESCENDANT TILE COUNTS:");
			this.descendantTileCounts = new CountingSet(new HashMap());
			for (int t = 0; t < this.speciesTiles.length; t++) {
//				System.out.println(" - " + this.speciesTiles[t].fileName);
				String tileRootEpithet = this.speciesTiles[t].fileName;
				tileRootEpithet = tileRootEpithet.substring(0, (tileRootEpithet.length() - ".0.txt".length()));
				tileRootEpithet = tileRootEpithet.substring(tileRootEpithet.lastIndexOf(".") + ".".length()); // safe even with -1
//				System.out.println("   ==> " + tileRootEpithet);
				int tileRootId = this.higherTile.findRecordId(encodeString(tileRootEpithet), ((byte) -1));
				if (tileRootId == -1) {
//					System.out.println("   ==> RECORD NOT FOUND");
					continue;
				}
//				System.out.println("   ==> " + tileRootId);
				for (; 0 < tileRootId; tileRootId = this.higherTile.getParentId(tileRootId)) {
//					System.out.println("   - counting " + this.higherTile.getEpithet(tileRootId));
					this.descendantTileCounts.add(new Integer(tileRootId));
				}
			}
			System.out.println("DESCENDANT TILE COUNTS: " + this.descendantTileCounts);
		}
		return this.descendantTileCounts.getCount(new Integer(id));
	}
	
	private int maxSimultaneousIndexTiles = -1;
	private int indexTileUseNumber = 0;
	synchronized IndexTile getIndexTileForString(byte[] str) {
		int low = 0;
		int high = (this.indexTiles.length - 1);
		while (low <= high) {
			int mid = ((low + high) / 2);
			if (compareStringBytes(str, 0, str.length, this.indexTiles[mid].minStr, 0, this.indexTiles[mid].minStr.length, true) < 0)
				high = (mid - 1);
			else if (compareStringBytes(this.indexTiles[mid].maxStr, 0, this.indexTiles[mid].maxStr.length, str, 0, str.length, true) < 0)
				low = (mid + 1);
			else {
				this.indexTiles[mid].lastTileUseNumber = this.indexTileUseNumber++;
				return this.indexTiles[mid].getTile();
			}
		}
		return null;
	}
	
	private static class IntBuffer {
		private int[] ints = new int[16];
		private int size = 0;
		IntBuffer() {}
		void add(int i) {
			if (this.size == this.ints.length)
				this.ints = doubleLength(this.ints);
			this.ints[this.size++] = i;
		}
		void addAll(int[] ints) {
			while (this.ints.length <= (this.size + ints.length))
				this.ints = doubleLength(this.ints);
			System.arraycopy(ints, 0, this.ints, this.size, ints.length);
			this.size += ints.length;
		}
		int size() {
			return this.size;
		}
		int get(int index) {
			return this.ints[index];
		}
		int[] toArray() {
			return trimLength(this.ints, this.size);
		}
		void finishResult() {
			Arrays.sort(this.ints, 0, this.size);
			int removed = 0;
			for (int i = 1; i < this.size; i++) {
				if (this.ints[i] == this.ints[i-1])
					removed++;
				else if (removed != 0)
					this.ints[i-removed] = this.ints[i];
			}
			this.size -= removed;
		}
	}
	
	static byte[] getQueryBytes(String query) {
		String nQuery = StringUtils.normalizeString(query).replaceAll("[^a-zA-Z0-9\\-]", "");
		return encodeString(nQuery);
	}
	
	TaxonRecord getRecord(int id) {
		DataTile tile = this.getDataTileForId(id);
		return tile.getRecord(id);
	}
	
	static String decodeRank(byte rank) {
		return levelsToRanks[rank];
	}
	static byte encodeRank(String rank) {
		Byte level = ((Byte) ranksToLevels.get(rank));
		return ((level == null) ? -1 : level.byteValue());
	}
	static byte getPrimaryChildRank(byte rank) {
		for (int r = (rank + 1); r < levelsToRanks.length; r++) {
			if (primaryRanks.containsKey(levelsToRanks[r]))
				return ((byte) r);
		}
		return -1;
	}
	
	String decodeAuthority(byte[] authorityBytes) {
		return getString(authorityBytes, 0, authorityBytes.length);
		/* TODO once we have authority IDs and abstraction:
		 * - get year-abstracted bytes
		 * - fill in years
		 * - decode bytes
		 */
	}
	byte[] encodeAuthority(String authority) {
		byte[] authorityBytes = new byte[authority.length()];
		storeString(authority, authorityBytes, 0);
		return authorityBytes;
		/* TODO once we have authority IDs and abstraction:
		 * - abstract years (replace with 0x7F)
		 * - find ID (not creating here, too much effort)
		 * - return ID and years in array
		 */
	}
	/*
DO NOT build authority resolver on the fly:
- only some 250K authority strings after abstracting from years
- saves tons of lookup hassle (and respective data structures) on loading
- facilitates using fixed authority string IDs
- still load authorities only on first lookup, though ...
- ... as it's still ~5MB of data overall
==> compile authorities at generation time
==> ALSO, cut back on normalization ...
==> ... as risk of data loss too high
==> DO reduce normalization ...
==> ... and then get (year abstracted and normalized) string count again

Provide hard coded IDs and expansion for specific frequent authorities (especially in botany):
- "L." =botany=> "Linnaeus, 1752"
- "L." =zoology=> "Linnaeus, 1758"
- "Berl." ==> "Berlese"
- "Fabr." ==> "Fabricius"
- ... and so forth
- collect and amend this over time
==> might even yield decent resolver for "micro-citations" (Rich)
==> might also be sensible to provide public authority abbreviation lookup ...
  ==> find way of facilitating abbreviation string lookup
    ==> fixed expansions should be few, so using map (and static initializer) very sensible
	 */
	
	static String formatEpithet(String epithet, byte rank) {
		if ((rank <= genusRankLevel) || (rank == speciesRankLevel))
			return epithet;
		if (SUBGENUS_ATTRIBUTE.equals(levelsToRanks[rank]))
			return ("(" + epithet + ")");
		if (SECTION_ATTRIBUTE.equals(levelsToRanks[rank]))
			return ("(sect. " + epithet + ")");
		if (SUBSECTION_ATTRIBUTE.equals(levelsToRanks[rank]))
			return ("(subsect. " + epithet + ")");
		if (SERIES_ATTRIBUTE.equals(levelsToRanks[rank]))
			return ("(ser. " + epithet + ")");
		if (SUBSERIES_ATTRIBUTE.equals(levelsToRanks[rank]))
			return ("(subser. " + epithet + ")");
		if (SUBSPECIES_ATTRIBUTE.equals(levelsToRanks[rank]))
			return ("subsp. " + epithet);
		if (VARIETY_ATTRIBUTE.equals(levelsToRanks[rank]))
			return ("var. " + epithet);
		if (SUBVARIETY_ATTRIBUTE.equals(levelsToRanks[rank]))
			return ("subvar. " + epithet);
		if (FORM_ATTRIBUTE.equals(levelsToRanks[rank]))
			return ("f. " + epithet);
		if (SUBFORM_ATTRIBUTE.equals(levelsToRanks[rank]))
			return ("subf. " + epithet);
		return epithet;
	}
	
	private static class DataTileProxy implements Comparable {
		final CatalogOfLifeLocal col;
		final int minId;
		final int maxId;
		final int minColId;
		final int maxColId;
		final String fileName;
		DataTile tile = null;
		int lastTileUseNumber = 0;
//		DataTileProxy(CatalogOfLifeLocal col, int minId, int maxId, String fileName) {
		DataTileProxy(CatalogOfLifeLocal col, int minId, int maxId, int minColId, int maxColId, String fileName) {
			this.col = col;
			if (maxId < minId)
				throw new IllegalArgumentException("The maxId must be less than or equal to the minId, but values are minId: " + minId + ", maxId: " + maxId);
			this.minId = minId;
			this.maxId = maxId;
			this.minColId = minColId;
			this.maxColId = maxColId;
			this.fileName = fileName;
		}
		boolean containsRecord(int id) {
			return ((this.minId <= id) && (id <= this.maxId));
		}
		boolean spansColRecord(String colIdBase29) {
			return this.spansColRecord(parseIntBase29(colIdBase29));
		}
		boolean spansColRecord(int colId) {
			return ((this.minColId <= colId) && (colId <= this.maxColId));
		}
		boolean isTileLoaded() {
			return (this.tile != null);
		}
		DataTile getTile() {
			if (this.tile == null)
				this.tile = this.col.loadDataTile(this.fileName, this.minId, this.maxId);
			return this.tile;
		}
		public int compareTo(Object obj) {
			DataTileProxy tp = ((DataTileProxy) obj);
			if (tp.maxId < this.minId)
				return 1;
			else if (this.maxId < tp.minId)
				return -1;
			else return 0;
		}
		public String toString() {
			return (this.fileName + " [" + this.minId + "," + this.maxId + "]");
		}
	}
	
	DataTile loadDataTile(String fileName, int minId, int maxId) {
		try {
			System.out.println("Loading data tile " + fileName);
			InputStream in = getInputStream(fileName);
			return loadDataTile(in, minId, maxId, this);
		}
		catch (IOException ioe) {
			System.out.println("Error loading data tile " + fileName + ": " + ioe.getMessage());
			ioe.printStackTrace(System.out);
			return null;
		}
		finally {
			if (0 < this.maxSimultaneousSpeciesTiles)
				this.trimSpeciesTiles();
		}
	}
	private synchronized void trimSpeciesTiles() {
		ArrayList loadedSpeciesTiles = new ArrayList();
		for (int t = 0; t < this.speciesTiles.length; t++) {
			if (this.speciesTiles[t].isTileLoaded())
				loadedSpeciesTiles.add(this.speciesTiles[t]);
		}
		if (loadedSpeciesTiles.size() <= this.maxSimultaneousSpeciesTiles)
			return;
		Collections.sort(loadedSpeciesTiles, speciesTileUseOrder);
		while (this.maxSimultaneousSpeciesTiles < loadedSpeciesTiles.size()) {
			DataTileProxy dtp = ((DataTileProxy) loadedSpeciesTiles.remove(0));
			dtp.tile = null;
			System.out.println("Evicted data tile " + dtp.fileName);
		}
		System.gc();
	}
	private static final Comparator speciesTileUseOrder = new Comparator() {
		public int compare(Object obj1, Object obj2) {
			DataTileProxy dtp1 = ((DataTileProxy) obj1);
			DataTileProxy dtp2 = ((DataTileProxy) obj2);
			return (dtp1.lastTileUseNumber - dtp2.lastTileUseNumber);
		}
	};
	
	static DataTile loadDataTile(InputStream in, int minId, int maxId, CatalogOfLifeLocal col) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
		int[] recordOffsets = new int[1024];
		int recordOffsetSize = 0;
		byte[] recordBytes = new byte[32768];
		int recordByteSize = 0;
		int[] recordColIDs = new int[1024];
		byte[] byteCache = new byte[256];
		int rMinId = Integer.MAX_VALUE;
		int rMaxId = 0;
		boolean[] missingId = new boolean[maxId - minId + 1];
		Arrays.fill(missingId, true);
		int listIdCount = 0;
		int listByteCount = 0;
		for (String recordStr; (recordStr = br.readLine()) != null;) {
			//	13343	E	family	3837	0	Zygopleuridae	Wenz, 1938	19965;19966;19967	<synonymIDs>
			String[] recordData = recordStr.split("\\t");
			if (recordData.length < 6) {
				System.out.println("BAD RECORD: " + recordData);
				continue;
			}
			
			//	parse TSV
//			int id = Integer.parseInt(recordData[0], 16);
			int id;
			int colId;
			if (recordData[0].indexOf("/") == -1) /* no original CoL ID given */ {
				id = Integer.parseInt(recordData[0], 16);
				colId = 0;
				recordColIDs = null; // clear array, no use collecting any more IDs
			}
			else /* parse original CoL ID off record ID */ {
				id = Integer.parseInt(recordData[0].substring(0, recordData[0].indexOf("/")), 16);
				colId = parseIntBase29(recordData[0].substring(recordData[0].indexOf("/") + "/".length()));
			}
			rMinId = Math.min(rMinId, id);
			rMaxId = Math.max(rMaxId, id);
			missingId[id - minId] = false;
			byte rank = encodeRank(recordData[1]);
			boolean isPrimaryRank = primaryRanks.containsKey(recordData[1]);
			boolean isExtant = "X".equals(recordData[2]);
			int parentId = Integer.parseInt(recordData[3], 16);
			int validId = Integer.parseInt(recordData[4], 16);
			String epithet = recordData[5];
			String authority = ((recordData.length < 7) ? null : recordData[6]);
			int authorityLength = ((authority == null) ? 0 : authority.length());
			
			int[] childIDs = null;
			int[] synonymIDs = null;
//			int originalParentId = 0;
			String originalParentEpithet = null;
			if (validId == 0) {
				childIDs = ((recordData.length < 8) ? null : decodeIdList(recordData[7]));
				synonymIDs = ((recordData.length < 9) ? null : decodeIdList(recordData[8]));
			}
//			else if (recordData.length >= 8) try {
//				originalParentId = Integer.parseInt(recordData[7], 16);
//			}
//			catch (NumberFormatException nfe) {
//				originalParentEpithet = recordData[7];
//			}
			else if (recordData.length >= 8)
				originalParentEpithet = recordData[7];
			
			//	assemble flags
			byte flags = 0;
			if (validId == 0)
				flags |= TaxonRecord.IS_VALID;
			if (isPrimaryRank)
				flags |= TaxonRecord.IS_PRIMARY_RANK;
			if (isExtant)
				flags |= TaxonRecord.IS_EXTANT;
//			if ((originalParentId != 0) || (originalParentEpithet != null))
			if (originalParentEpithet != null)
				flags |= TaxonRecord.HAS_ORIGINAL_PARENT;
			if (childIDs != null)
				flags |= TaxonRecord.HAS_CHILDREN;
			if (synonymIDs != null)
				flags |= TaxonRecord.HAS_SYNONYMS;
			if (authorityLength != 0)
				flags |= TaxonRecord.STORES_VERBATIM_AUTHORITY;
			
			//	make sure cache large enough
			int maxBytes = (TaxonRecord.EPITHET_VALUE_OFFSET + epithet.length() + authorityLength);
			if (childIDs != null)
				maxBytes += (TaxonRecord.ID_LIST_LENGTH_SIZE + (childIDs.length * TaxonRecord.ID_SIZE));
			if (synonymIDs != null)
				maxBytes += (TaxonRecord.ID_LIST_LENGTH_SIZE + (synonymIDs.length * TaxonRecord.ID_SIZE));
//			if (originalParentId != 0)
//				maxBytes += TaxonRecord.ID_SIZE;
			if (originalParentEpithet != null)
				maxBytes += (TaxonRecord.VALUE_LENGTH_SIZE + originalParentEpithet.length());
			if (byteCache.length < maxBytes)
				byteCache = new byte[maxBytes];
			
			//	create record bytes
			int byteCacheSize = 0;
			storeInt(id, byteCache, byteCacheSize, TaxonRecord.ID_SIZE);
			byteCacheSize += TaxonRecord.ID_SIZE;
//			storeInt(colId, byteCache, byteCacheSize, TaxonRecord.ID_SIZE);
//			byteCacheSize += TaxonRecord.ID_SIZE;
			if (validId == 0) {
				storeInt(parentId, byteCache, byteCacheSize, TaxonRecord.ID_SIZE);
				byteCacheSize += TaxonRecord.ID_SIZE;
			}
			else {
				storeInt(validId, byteCache, byteCacheSize, TaxonRecord.ID_SIZE);
				byteCacheSize += TaxonRecord.ID_SIZE;
			}
			byteCache[byteCacheSize] = flags;
			byteCacheSize += 1;
			byteCache[byteCacheSize] = rank;
			byteCacheSize += 1;
			storeInt(epithet.length(), byteCache, byteCacheSize, TaxonRecord.VALUE_LENGTH_SIZE);
			byteCacheSize += TaxonRecord.VALUE_LENGTH_SIZE;
			storeInt(authorityLength, byteCache, byteCacheSize, TaxonRecord.VALUE_LENGTH_SIZE);
			byteCacheSize += TaxonRecord.VALUE_LENGTH_SIZE;
			storeString(epithet, byteCache, byteCacheSize);
			byteCacheSize += epithet.length();
			if (authorityLength != 0) {
				storeString(authority, byteCache, byteCacheSize);
				byteCacheSize += authorityLength;
			}
			if (childIDs != null) {
				int idListBytes = storeInts(childIDs, byteCache, (byteCacheSize + TaxonRecord.ID_LIST_LENGTH_SIZE));
				storeInt(idListBytes, byteCache, byteCacheSize, TaxonRecord.ID_LIST_LENGTH_SIZE);
				byteCacheSize += (TaxonRecord.ID_LIST_LENGTH_SIZE + idListBytes);
				listIdCount += childIDs.length;
				listByteCount += idListBytes;
			}
			if (synonymIDs != null) {
				int idListBytes = storeInts(synonymIDs, byteCache, (byteCacheSize + TaxonRecord.ID_LIST_LENGTH_SIZE));
				storeInt(idListBytes, byteCache, byteCacheSize, TaxonRecord.ID_LIST_LENGTH_SIZE);
				byteCacheSize += (TaxonRecord.ID_LIST_LENGTH_SIZE + idListBytes);
				listIdCount += synonymIDs.length;
				listByteCount += idListBytes;
			}
//			if (originalParentId != 0) {
//				storeInt(originalParentId, byteCache, byteCacheSize, TaxonRecord.ID_SIZE);
//				byteCacheSize += TaxonRecord.ID_SIZE;
//			}
//			else if (originalParentEpithet != null) {
//				byteCache[byteCacheSize] = ((byte) (originalParentEpithet.length() | 0x80));
//				byteCacheSize += TaxonRecord.VALUE_LENGTH_SIZE;
//				storeString(originalParentEpithet, byteCache, byteCacheSize);
//				byteCacheSize += originalParentEpithet.length();
//			}
			if (originalParentEpithet != null) {
				byteCache[byteCacheSize] = ((byte) originalParentEpithet.length());
				byteCacheSize += TaxonRecord.VALUE_LENGTH_SIZE;
				storeString(originalParentEpithet, byteCache, byteCacheSize);
				byteCacheSize += originalParentEpithet.length();
			}
			
			//	store data in tile arrays
			if (recordOffsetSize == recordOffsets.length) {
				recordOffsets = doubleLength(recordOffsets);
				if (recordColIDs != null)
					recordColIDs = doubleLength(recordColIDs);
			}
			if (recordBytes.length < (recordByteSize + byteCacheSize)) {
				byte[] cRecordBytes = new byte[recordBytes.length * 2];
				System.arraycopy(recordBytes, 0, cRecordBytes, 0, recordBytes.length);
				recordBytes = cRecordBytes;
			}
			recordOffsets[recordOffsetSize] = recordByteSize; // TODO_maybe maybe align to multiple of 4
			if (recordColIDs != null)
				recordColIDs[recordOffsetSize] = colId;
			recordOffsetSize++;
			System.arraycopy(byteCache, 0, recordBytes, recordByteSize, byteCacheSize);
			recordByteSize += byteCacheSize;
		}
		br.close();
		
		//	shrink arrays
		if (recordOffsetSize < recordOffsets.length) {
			recordOffsets = trimLength(recordOffsets, recordOffsetSize);
			if (recordColIDs != null)
				recordColIDs = trimLength(recordColIDs, recordOffsetSize);
		}
		if (recordByteSize < recordBytes.length) {
			byte[] cRecordBytes = new byte[recordByteSize];
			System.arraycopy(recordBytes, 0, cRecordBytes, 0, recordByteSize);
			recordBytes = cRecordBytes;
		}
		
		System.out.println("GOT " + recordOffsets.length + " RECORDS WITH " + recordBytes.length + " BYTES");
		System.out.println("MIN ID IS " + rMinId + ", MAX ID IS " + rMaxId);
		for (int i = 0; i < missingId.length; i++) {
			if (missingId[i])
				System.out.println("MISSING ID " + (minId + i));
		}
		System.out.println("STORED " + listIdCount + " DESCENDENT IDS IN " + listByteCount + " BYTES");
		
		//	finally ...
		return new DataTile(col, minId, maxId, recordOffsets, recordBytes, recordColIDs);
	}
	
	private static int[] decodeIdList(String idList) {
		if (idList.length() == 0)
			return null;
		String[] idStrs = idList.split("\\;");
		IntBuffer ids = new IntBuffer();
		for (int i = 0; i < idStrs.length; i++) {
			if (idStrs[i].indexOf("+") == -1) {
				ids.add(Integer.parseInt(idStrs[i], 16));
				continue;
			}
			int s = Integer.parseInt(idStrs[i].substring(0, idStrs[i].indexOf("+")), 16);
			ids.add(s);
			int c = Integer.parseInt(idStrs[i].substring(idStrs[i].indexOf("+") + "+".length()), 16);
			for (; c > 0; c--) {
				s++;
				ids.add(s);
			}
		}
		return ids.toArray();
	}
	
	private static class DataTile {
		final CatalogOfLifeLocal col;
		final int minId;
		final int maxId;
		final int[] recordOffsets;
		final int[] idRecordOffsets;
		final int[] idRecordColIDs;
		final int minColId;
		final int maxColId;
		final long[] colIdRecordOffsets;
		final byte[] data;
		DataTile(CatalogOfLifeLocal col, int minId, int maxId, int[] recordOffsets, byte[] data, int[] recordColIDs) {
			this.col = col;
			this.minId = minId;
			this.maxId = maxId;
			this.data = data;
			this.recordOffsets = recordOffsets;
			this.idRecordOffsets = new int[this.recordOffsets.length];
			this.idRecordColIDs = ((recordColIDs == null) ? null : new int[this.recordOffsets.length]);
			for (int r = 0; r < this.recordOffsets.length; r++) {
				int id = TaxonRecord.getId(this.data, this.recordOffsets[r]);
				this.idRecordOffsets[id - this.minId] = this.recordOffsets[r];
				if (recordColIDs != null)
					this.idRecordColIDs[id - this.minId] = recordColIDs[r];
			}
			if (recordColIDs == null) {
				this.minColId = Integer.MAX_VALUE;
				this.maxColId = Integer.MIN_VALUE;
				this.colIdRecordOffsets = null;
			}
			else {
				int minColId = Integer.MAX_VALUE;
				int maxColId = Integer.MIN_VALUE;
				this.colIdRecordOffsets = new long[this.recordOffsets.length];
				for (int r = 0; r < this.recordOffsets.length; r++) {
					int colId = recordColIDs[r];
					minColId = Math.min(minColId, colId);
					maxColId = Math.max(maxColId, colId);
					this.colIdRecordOffsets[r] = colId;
					this.colIdRecordOffsets[r] <<= 32;
					this.colIdRecordOffsets[r] |= this.recordOffsets[r];
				}
				this.minColId = minColId;
				this.maxColId = maxColId;
				Arrays.sort(this.colIdRecordOffsets);
			}
		}
		
		boolean containsRecord(int id) {
			return ((this.minId <= id) && (id <= this.maxId));
		}
		TaxonRecord getRecord(int id) {
			if (this.containsRecord(id))
				return new TaxonRecord(this, this.idRecordOffsets[id - this.minId]);
			else return this.col.getRecord(id);
		}
		boolean colSpansRecord(String colId) {
			return this.colSpansRecord(parseIntBase29(colId));
		}
		boolean colSpansRecord(int colId) {
			return ((this.minColId <= colId) && (colId <= this.maxColId));
		}
		TaxonRecord colGetRecord(String colId) {
			return this.colGetRecord(parseIntBase29(colId));
		}
		TaxonRecord colGetRecord(int colId) {
			if (this.colSpansRecord(colId)) {
				long minColId = colId;
				minColId <<= 32;
				long maxColId = (minColId | 0x00000000FFFFFFFFL);
				int low = 0;
				int high = (this.colIdRecordOffsets.length - 1);
				int pos = -1;
				while (low <= high) {
					int mid = ((low + high) / 2);
					if (this.colIdRecordOffsets[mid] < minColId)
						low = (mid + 1);
					else if (this.colIdRecordOffsets[mid] > maxColId)
						high = (mid - 1);
					else {
						pos = mid;
						break;
					}
				}
				return ((pos == -1) ? null : new TaxonRecord(this, ((int) (this.colIdRecordOffsets[pos] & 0x000000007FFFFFFFL))));
			}
			else return null;
		}
		int colGetId(int id) {
			return ((this.idRecordColIDs == null) ? -1 : this.idRecordColIDs[id - this.minId]);
		}
		
		String getEpithet(int id) {
			return TaxonRecord.getEpithet(this.data, this.idRecordOffsets[id - this.minId]);
		}
		byte[] getEpithetBytes(int id) {
			return TaxonRecord.getEpithetBytes(this.data, this.idRecordOffsets[id - this.minId]);
		}
		boolean isValidTaxon(int id) {
			return TaxonRecord.isValidTaxon(this.data, this.idRecordOffsets[id - this.minId]);
		}
		boolean isExtantTaxon(int id) {
			return TaxonRecord.isExtant(this.data, this.idRecordOffsets[id - this.minId]);
		}
		String getRank(int id) {
			byte rank = TaxonRecord.getRank(this.data, this.idRecordOffsets[id - this.minId]);
			return decodeRank(rank);
		}
		byte getRankByte(int id) {
			return TaxonRecord.getRank(this.data, this.idRecordOffsets[id - this.minId]);
		}
		boolean isPrimaryRank(int id) {
			return TaxonRecord.isPrimaryRank(this.data, this.idRecordOffsets[id - this.minId]);
		}
		int getParentId(int id) {
			if (TaxonRecord.isValidTaxon(this.data, this.idRecordOffsets[id - this.minId]))
				return TaxonRecord.getParentOrValidId(this.data, this.idRecordOffsets[id - this.minId]);
			else return -1;
		}
		int[] getChildIDs(int id) {
			return TaxonRecord.getChildIDs(this.data, this.idRecordOffsets[id - this.minId]);
		}
		int[] findDescendantIDs(int id, byte[] query, boolean prefixMatch, boolean caseSensitive, byte rank, boolean includeSynonyms) {
			int descendantTileCount;
			if ((rank != -1) && (rank < speciesRankLevel))
				descendantTileCount = 0; // only higher taxa to search, single tile
			else if (familyRankLevel <= this.getRankByte(id))
				descendantTileCount = 0; // family or below, at most 1 tile (distiller doesn't split families)
			else descendantTileCount = this.col.getDescendantTileCount(id); // check for taxa above family
			
			/* Use search through children if ...
			 * - rank above species
			 *   OR
			 * - species spread out over at most 2 tiles
			 */
			if (descendantTileCount < 3) {
				int[] childIDs = this.getChildIDs(id);
				if (childIDs == null)
					return null;
				IntBuffer seekBuffer = new IntBuffer();
				seekBuffer.addAll(childIDs);
				DataTile tile = this;
				IntBuffer matchDescendantIDs = new IntBuffer();
				for (int c = 0; c < seekBuffer.size(); c++) {
					int cid = seekBuffer.get(c);
					if (!tile.containsRecord(cid))
						tile = this.col.getDataTileForId(cid);
					byte cRank = tile.getRankByte(cid);
					if (!includeSynonyms && (rank != -1) && (rank < cRank))
						continue;
					if ((includeSynonyms || tile.isValidTaxon(cid)) && tile.recordMatches(cid, query, prefixMatch, caseSensitive, rank))
						matchDescendantIDs.add(cid);
					
					//	do we need to look at descendants of current nod?
					if (rank == -1) {} // need whole subtree
					else if (cRank < rank) {} // sought rank below that of current node
					/* avoid going to species level if out for taxa above that:
					 * - degradations of above-species taxa to species or below
					 *   are extremely rara (if any at all)
					 * - we want to stay out of species tiles if we're out for
					 *   above-species taxa */
					else if (includeSynonyms && ((speciesRankLevel <= rank) || (cRank < (speciesRankLevel - 1)))) {}
					else continue;
					
					//	add descendants
					childIDs = tile.getChildIDs(cid);
					if (childIDs != null)
						seekBuffer.addAll(childIDs);
					if (includeSynonyms && ((childIDs = tile.getSynonymIDs(cid)) != null))
						seekBuffer.addAll(childIDs);
				}
				if (matchDescendantIDs.size() == 0)
					return null;
				matchDescendantIDs.finishResult();
				return matchDescendantIDs.toArray();
			}
			
			/* Switching to full text index and upwards check if descendants spread out over 3 or more tiles
			 * ==> keeps tile cache misses at bay ...
			 * ==> ... and after all, that number of tiles _is_ kind of a selectivity estimate for given parent
			 */
			else {
				byte[] lQuery = null;
				for (int b = 0; b < query.length; b++)
					if (isLetter(query[b]) && (query[b] < 'a')) {
						lQuery = new byte[query.length];
						break;
					}
				if (lQuery == null)
					lQuery = query;
				else for (int b = 0; b < query.length; b++) {
					if (isLetter(query[b]) && (query[b] < 'a'))
						lQuery[b] = ((byte) (query[b] | 0x20));
					else lQuery[b] = query[b];
				}
				IndexTile queryTile = this.col.getIndexTileForString(lQuery);
				if (queryTile == null)
					return null;
				IndexEntry[] queryResults = queryTile.findMatches(lQuery, prefixMatch);
				if (queryResults == null)
					return null;
				IntBuffer matchIDs = new IntBuffer();
				boolean addLowerCaseResults = (!caseSensitive || (lQuery == query) /* lower case query */);
				boolean addCapitalizedResults = (!caseSensitive || (lQuery != query) /* capitalized query */);
				for (int r = 0; r < queryResults.length; r++) {
					if (addLowerCaseResults) {
						int[] ids = queryResults[r].getLowerCaseMatchIDs();
						if (ids != null)
							matchIDs.addAll(ids);
					}
					if (addCapitalizedResults) {
						int[] ids = queryResults[r].getCapitalizedMatchIDs();
						if (ids != null)
							matchIDs.addAll(ids);
					}
				}
				matchIDs.finishResult();
				if (matchIDs.size() == 0)
					return null;
				DataTile dataTile = null;
				IntBuffer matchDescendantIDs = new IntBuffer();
				for (int m = 0; m < matchIDs.size(); m++) {
					int mid = matchIDs.get(m);
					if ((dataTile == null) || !dataTile.containsRecord(mid))
						dataTile = this.col.getDataTileForId(mid);
					if ((rank != -1) && (rank != dataTile.getRankByte(mid)))
						continue;
					int mpid = dataTile.getParentId(mid);
					while (0 < mpid) {
						if (mpid == id) {
							matchDescendantIDs.add(mid);
							break;
						}
						if (!dataTile.containsRecord(mpid))
							dataTile = this.col.getDataTileForId(mpid);
						mpid = dataTile.getParentId(mpid);
					}
				}
				matchDescendantIDs.finishResult();
				return matchDescendantIDs.toArray();
			}
		}
		private boolean recordMatches(int id, byte[] query, boolean prefixMatch, boolean caseSensitive, byte rank) {
			if ((rank != -1) && (rank != this.getRankByte(id)))
				return false;
			else return (TaxonRecord.compareEpithetTo(this.data, this.idRecordOffsets[id - this.minId], query, prefixMatch, caseSensitive) == 0);
		}
		int getPrimaryParentId(int id) {
			int parentId = this.getParentId(id);
			if (parentId < 1)
				return -1;
			DataTile tile = (this.containsRecord(parentId) ? this : this.col.getDataTileForId(parentId));
			if (tile.isPrimaryRank(parentId))
				return parentId;
			else return tile.getPrimaryParentId(parentId);
		}
		int[] getPrimaryChildIDs(int id, boolean includeSynonyms) {
			byte rank = this.getRankByte(id);
			if (speciesRankLevel <= rank)
				return null; // no use seeking primary children of species or below, as species is lowest primary rank
			byte targetRank = getPrimaryChildRank(rank);
			byte cutoffRank = (includeSynonyms ? ((targetRank == speciesRankLevel) ? Byte.MAX_VALUE : ((byte) (getPrimaryChildRank(targetRank) - 1))) : targetRank);
			IntBuffer pChildIDs = new IntBuffer();
			this.addPrimaryChildIDs(id, pChildIDs, targetRank, cutoffRank, includeSynonyms);
			pChildIDs.finishResult();
			return pChildIDs.toArray();
		}
		private void addPrimaryChildIDs(int id, IntBuffer pChildIDs, byte targetRank, byte cutoffRank, boolean includeSynonyms) {
			int[] childIDs = this.getChildIDs(id);
			if (childIDs == null)
				return;
			DataTile tile = this;
			for (int c = 0; c < childIDs.length; c++) {
				if (!tile.containsRecord(childIDs[c]))
					tile = this.col.getDataTileForId(childIDs[c]);
				
				byte cRank = tile.getRankByte(childIDs[c]);
				if (cRank == targetRank)
					pChildIDs.add(childIDs[c]);
				if (cRank < cutoffRank)
					tile.addPrimaryChildIDs(childIDs[c], pChildIDs, targetRank, cutoffRank, includeSynonyms);
				
				int[] synonymIDs = (includeSynonyms ? tile.getSynonymIDs(childIDs[c]) : null);
				if (synonymIDs == null)
					continue;
				for (int s = 0; s < synonymIDs.length; s++) {
					if (!tile.containsRecord(synonymIDs[s]))
						tile = this.col.getDataTileForId(synonymIDs[s]);
					if (tile.getRankByte(synonymIDs[s]) == targetRank)
						pChildIDs.add(synonymIDs[s]);
				}
			}
		}
		Properties getHigherTaxonomy(int id) {
			return this.getHigherTaxonomy(id, false);
		}
		Properties getHigherTaxonomy(int id, boolean allRanks) {
			Properties higherTaxonomy = null;
			DataTile tile = this;
			for (int trId = (allRanks ? this.getParentId(id) : this.getPrimaryParentId(id)); trId != -1; trId = (allRanks ? tile.getParentId(trId) : tile.getPrimaryParentId(trId))) {
				if (higherTaxonomy == null)
					higherTaxonomy = new Properties();
				if (!tile.containsRecord(trId))
					tile = this.col.getDataTileForId(trId);
				higherTaxonomy.setProperty(tile.getRank(trId), tile.getEpithet(trId));
			}
			return higherTaxonomy;
		}
		int getValidTaxonId(int id) {
			if (TaxonRecord.isValidTaxon(this.data, this.idRecordOffsets[id - this.minId]))
				return id;
			return TaxonRecord.getParentOrValidId(this.data, this.idRecordOffsets[id - this.minId]);
		}
		public String getOriginalParentEpithets(int id) {
			if (TaxonRecord.isValidTaxon(this.data, this.idRecordOffsets[id - this.minId]))
				return null;
			if (!TaxonRecord.hasOriginalParent(this.data, this.idRecordOffsets[id - this.minId]))
				return null;
			return TaxonRecord.getOriginalParentEpithets(this.data, this.idRecordOffsets[id - this.minId]);
		}
		int[] getSynonymIDs(int id) {
			return TaxonRecord.getSynonymIDs(this.data, this.idRecordOffsets[id - this.minId]);
		}
		String getAuthority(int id) {
			byte[] authorityBytes = TaxonRecord.getAuthorityBytes(this.data, this.idRecordOffsets[id - this.minId]);
			if (authorityBytes == null)
				return null;
			else if (TaxonRecord.storesVerbatimAuthority(this.data, this.idRecordOffsets[id - this.minId]))
				return getString(authorityBytes, 0, authorityBytes.length);
			else return this.decodeAuthority(authorityBytes);
		}
		TaxonomicAuthority getCombinationAuthority(int id) {
			String authority = this.getAuthority(id);
			if (authority == null)
				return null;
			authority = TaxonomicNameUtils.getCombinationAuthority(authority);
			if (authority == null)
				return null;
			return TaxonomicNameUtils.parseAuthority(authority);
		}
		TaxonomicAuthority getBaseAuthority(int id) {
			String authority = this.getAuthority(id);
			if (authority == null)
				return null;
			authority = TaxonomicNameUtils.getBasionymAuthority(authority);
			if (authority == null)
				return null;
			return TaxonomicNameUtils.parseAuthority(authority);
		}
		
		String decodeAuthority(byte[] authorityBytes) {
			return this.col.decodeAuthority(authorityBytes);
		}
		int findRecordId(byte[] epithet, byte rank) {
			return this.findRecordId(epithet, rank, false, true);
		}
		int findRecordId(byte[] epithet, byte rank, boolean prefixMatch, boolean caseSensitive) {
			int pos = findFirst(epithet, this.data, this.recordOffsets, prefixMatch, caseSensitive);
			if (pos == -1)
				return -1;
			if ((rank == -1) || (rank == TaxonRecord.getRank(this.data, this.recordOffsets[pos])))
				return TaxonRecord.getId(this.data, this.recordOffsets[pos]);
			do {
				pos++;
				if (pos == this.recordOffsets.length)
					return -1;
				if (TaxonRecord.compareEpithetTo(this.data, this.recordOffsets[pos], epithet, prefixMatch, caseSensitive) != 0)
					return -1;
				if (rank == TaxonRecord.getRank(this.data, this.recordOffsets[pos]))
					return TaxonRecord.getId(this.data, this.recordOffsets[pos]);
			} while (true);
		}
		int[] findRecordIDs(byte[] epithet, byte rank) {
			return this.findRecordIDs(epithet, rank, false, true);
		}
		int[] findRecordIDs(byte[] epithet, byte rank, boolean prefixMatch, boolean caseSensitive) {
			int fPos = findFirst(epithet, this.data, this.recordOffsets, prefixMatch, caseSensitive);
			if (fPos == -1)
				return null;
			int lPos = (fPos+1);
			while (lPos < this.recordOffsets.length) {
				if (TaxonRecord.compareEpithetTo(this.data, this.recordOffsets[lPos], epithet, prefixMatch, caseSensitive) != 0)
					break;
				lPos++;
			}
			int[] ids = new int[lPos - fPos];
			for (int i = 0; i < ids.length; i++)
				ids[i] = TaxonRecord.getId(this.data, this.recordOffsets[fPos + i]);
			if (rank == -1)
				return ids;
			int rankMatches = 0;
			for (int i = 0; i < ids.length; i++) {
				if (TaxonRecord.getRank(this.data, this.idRecordOffsets[ids[i] - this.minId]) == rank)
					rankMatches++;
				else ids[i] = -1;
			}
			if (rankMatches == 0)
				return null;
			if (rankMatches == ids.length)
				return ids;
			int[] rIds = new int[rankMatches];
			for (int i = 0, ri = 0; i < ids.length; i++) {
				if (ids[i] != -1)
					rIds[ri++] = ids[i];
			}
			return rIds;
		}
		private static int findFirst(byte[] epithet, byte[] data, int[] recordOffsets, boolean prefixMatch, boolean caseSensitive) {
			int low = 0;
			int high = (recordOffsets.length - 1);
			int pos = -1;
			while (low <= high) {
				int mid = ((low + high) / 2);
				int c = TaxonRecord.compareEpithetTo(data, recordOffsets[mid], epithet, prefixMatch, caseSensitive);
				if (c < 0)
					low = (mid + 1);
				else if (c > 0)
					high = (mid - 1);
				else {
					pos = mid;
					break;
				}
			}
			if (pos == -1)
				return -1;
			while (pos != 0) {
				int c = TaxonRecord.compareEpithetTo(data, recordOffsets[pos-1], epithet, prefixMatch, caseSensitive);
				if (c == 0)
					pos--;
				else break;
			}
			return pos;
		}
	}
	
	private static class IndexTileProxy implements Comparable {
		final CatalogOfLifeLocal col;
		final byte[] minStr;
		final byte[] maxStr;
		final String fileName;
		IndexTile tile = null;
		int lastTileUseNumber = 0;
		IndexTileProxy(CatalogOfLifeLocal col, byte[] minStr, byte[] maxStr, String fileName) {
			this.col = col;
			if (compareStringBytes(maxStr, 0, maxStr.length, minStr, 0, minStr.length, true) < 0)
				throw new IllegalArgumentException("The maxString must be less than or equal to the minString, but values are minString: " + getString(minStr, 0, minStr.length) + ", maxId: " + getString(maxStr, 0, maxStr.length));
			this.minStr = minStr;
			this.maxStr = maxStr;
			this.fileName = fileName;
		}
//		boolean containsString(byte[] str) {
//			if (compareStringBytes(this.minStr, 0, this.minStr.length, str, 0, str.length, true) < 0)
//				return false;
//			else if (compareStringBytes(str, 0, str.length, this.maxStr, 0, this.maxStr.length, true) <= 0)
//				return true;
//			else return false;
//		}
		boolean isTileLoaded() {
			return (this.tile != null);
		}
		IndexTile getTile() {
			if (this.tile == null)
				this.tile = this.col.loadIndexTile(this.fileName, this.minStr, this.maxStr);
			return this.tile;
		}
		public int compareTo(Object obj) {
			IndexTileProxy tp = ((IndexTileProxy) obj);
			if (compareStringBytes(tp.maxStr, 0, tp.maxStr.length, this.minStr, 0, this.minStr.length, true) < 0)
				return 1;
			else if (compareStringBytes(this.maxStr, 0, this.maxStr.length, tp.minStr, 0, tp.minStr.length, true) < 0)
				return -1;
			else return 0;
		}
		public String toString() {
			return (this.fileName + " [" + getString(this.minStr, 0, this.minStr.length) + "," + getString(this.maxStr, 0, this.maxStr.length) + "]");
		}
	}
	
	IndexTile loadIndexTile(String fileName, byte[] minStr, byte[] maxStr) {
		try {
			System.out.println("Loading index tile " + fileName);
			InputStream in = getInputStream(fileName);
			return loadIndexTile(in, minStr, maxStr, this);
		}
		catch (IOException ioe) {
			System.out.println("Error loading index tile " + fileName + ": " + ioe.getMessage());
			ioe.printStackTrace(System.out);
			return null;
		}
		finally {
			if (0 < this.maxSimultaneousIndexTiles)
				this.trimIndexTiles();
		}
	}
	private synchronized void trimIndexTiles() {
		ArrayList loadedIndexTiles = new ArrayList();
		for (int t = 0; t < this.indexTiles.length; t++) {
			if (this.indexTiles[t].isTileLoaded())
				loadedIndexTiles.add(this.indexTiles[t]);
		}
		if (loadedIndexTiles.size() <= this.maxSimultaneousIndexTiles)
			return;
		Collections.sort(loadedIndexTiles, indexTileUseOrder);
		while (this.maxSimultaneousIndexTiles < loadedIndexTiles.size()) {
			IndexTileProxy itp = ((IndexTileProxy) loadedIndexTiles.remove(0));
			itp.tile = null;
			System.out.println("Evicted index tile " + itp.fileName);
		}
		System.gc();
	}
	private static final Comparator indexTileUseOrder = new Comparator() {
		public int compare(Object obj1, Object obj2) {
			IndexTileProxy itp1 = ((IndexTileProxy) obj1);
			IndexTileProxy itp2 = ((IndexTileProxy) obj2);
			return (itp1.lastTileUseNumber - itp2.lastTileUseNumber);
		}
	};
	
	static IndexTile loadIndexTile(InputStream in, byte[] minStr, byte[] maxStr, CatalogOfLifeLocal col) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
		int[] entryOffsets = new int[1024];
		int entryOffsetSize = 0;
		byte[] entryBytes = new byte[32768];
		int entryByteSize = 0;
		byte[] byteCache = new byte[256];
		int listIdCount = 0;
		int listByteCount = 0;
		for (String entryStr; (entryStr = br.readLine()) != null;) {
			//	yaanensis	EL:<epithetLowerCaseMatchIDs>;EC:<epithetCapitalizedMatchIDs>;PL:<prefixLowerCaseMatchIDs>;PC:<prefixCapitalizedMatchIDs>
			String[] entryData = entryStr.split("\\t");
			if (entryData.length < 2) {
				System.out.println("BAD ENTRY: " + entryData);
				continue;
			}
			
			//	parse TSV
			String entry = entryData[0];
			String matchIdListStr = entryData[1];
			int[] ecpMatchIDs = null;
			int[] elcMatchIDs = null;
			int[] pcpMatchIDs = null;
			int[] plcMatchIDs = null;
			while (matchIdListStr != null) {
				if (matchIdListStr.startsWith("EC:")) {
					matchIdListStr = matchIdListStr.substring("EC:".length());
					int idListEnd = matchIdListStr.indexOf(":");
					if (idListEnd == -1) {
						ecpMatchIDs = decodeIdList(matchIdListStr);
						matchIdListStr = null;
					}
					else {
						ecpMatchIDs = decodeIdList(matchIdListStr.substring(0, (idListEnd - ";EL".length())));
						matchIdListStr = matchIdListStr.substring(idListEnd - "EL".length());
					}
				}
				else if (matchIdListStr.startsWith("EL:")) {
					matchIdListStr = matchIdListStr.substring("EL:".length());
					int idListEnd = matchIdListStr.indexOf(":");
					if (idListEnd == -1) {
						elcMatchIDs = decodeIdList(matchIdListStr);
						matchIdListStr = null;
					}
					else {
						elcMatchIDs = decodeIdList(matchIdListStr.substring(0, (idListEnd - ";PC".length())));
						matchIdListStr = matchIdListStr.substring(idListEnd - "PC".length());
					}
				}
				else if (matchIdListStr.startsWith("PC:")) {
					matchIdListStr = matchIdListStr.substring("PC:".length());
					int idListEnd = matchIdListStr.indexOf(":");
					if (idListEnd == -1) {
						pcpMatchIDs = decodeIdList(matchIdListStr);
						matchIdListStr = null;
					}
					else {
						pcpMatchIDs = decodeIdList(matchIdListStr.substring(0, (idListEnd - ";PL".length())));
						matchIdListStr = matchIdListStr.substring(idListEnd - "PL".length());
					}
				}
				else if (matchIdListStr.startsWith("PL:")) {
					matchIdListStr = matchIdListStr.substring("PL:".length());
					int idListEnd = matchIdListStr.indexOf(":");
					if (idListEnd == -1) {
						plcMatchIDs = decodeIdList(matchIdListStr);
						matchIdListStr = null;
					}
					else {
						plcMatchIDs = decodeIdList(matchIdListStr.substring(0, (idListEnd - ";XX".length())));
						matchIdListStr = matchIdListStr.substring(idListEnd - "XX".length());
					}
				}
			}
			
			//	assemble flags
			byte flags = 0;
			if (elcMatchIDs != null)
				flags |= IndexEntry.HAS_EPITHET_MATCHES_LOWER_CASE;
			if (ecpMatchIDs != null)
				flags |= IndexEntry.HAS_EPITHET_MATCHES_CAPITALIZED;
			if (plcMatchIDs != null)
				flags |= IndexEntry.HAS_PREFIX_MATCHES_LOWER_CASE;
			if (pcpMatchIDs != null)
				flags |= IndexEntry.HAS_PREFIX_MATCHES_CAPITALIZED;
			
			//	make sure cache large enough
			int maxBytes = (IndexEntry.ENTRY_VALUE_OFFSET + entry.length());
			if (elcMatchIDs != null)
				maxBytes += (IndexEntry.ID_LIST_LENGTH_SIZE + (elcMatchIDs.length * TaxonRecord.ID_SIZE));
			if (ecpMatchIDs != null)
				maxBytes += (IndexEntry.ID_LIST_LENGTH_SIZE + (ecpMatchIDs.length * TaxonRecord.ID_SIZE));
			if (plcMatchIDs != null)
				maxBytes += (IndexEntry.ID_LIST_LENGTH_SIZE + (plcMatchIDs.length * TaxonRecord.ID_SIZE));
			if (pcpMatchIDs != null)
				maxBytes += (IndexEntry.ID_LIST_LENGTH_SIZE + (pcpMatchIDs.length * TaxonRecord.ID_SIZE));
			if (byteCache.length < maxBytes)
				byteCache = new byte[maxBytes];
			
			//	create record bytes
			int byteCacheSize = 0;
			byteCache[byteCacheSize] = flags;
			byteCacheSize += 1;
			storeInt(entry.length(), byteCache, byteCacheSize, 1);
			byteCacheSize += 1;
			storeString(entry, byteCache, byteCacheSize);
			byteCacheSize += entry.length();
			if (elcMatchIDs != null) {
				int idListBytes = storeInts(elcMatchIDs, byteCache, (byteCacheSize + IndexEntry.ID_LIST_LENGTH_SIZE));
				storeInt(idListBytes, byteCache, byteCacheSize, IndexEntry.ID_LIST_LENGTH_SIZE);
				byteCacheSize += (IndexEntry.ID_LIST_LENGTH_SIZE + idListBytes);
				listIdCount += elcMatchIDs.length;
				listByteCount += idListBytes;
			}
			if (ecpMatchIDs != null) {
				int idListBytes = storeInts(ecpMatchIDs, byteCache, (byteCacheSize + IndexEntry.ID_LIST_LENGTH_SIZE));
				storeInt(idListBytes, byteCache, byteCacheSize, IndexEntry.ID_LIST_LENGTH_SIZE);
				byteCacheSize += (IndexEntry.ID_LIST_LENGTH_SIZE + idListBytes);
				listIdCount += ecpMatchIDs.length;
				listByteCount += idListBytes;
			}
			if (plcMatchIDs != null) {
				int idListBytes = storeInts(plcMatchIDs, byteCache, (byteCacheSize + IndexEntry.ID_LIST_LENGTH_SIZE));
				storeInt(idListBytes, byteCache, byteCacheSize, IndexEntry.ID_LIST_LENGTH_SIZE);
				byteCacheSize += (IndexEntry.ID_LIST_LENGTH_SIZE + idListBytes);
				listIdCount += plcMatchIDs.length;
				listByteCount += idListBytes;
			}
			if (pcpMatchIDs != null) {
				int idListBytes = storeInts(pcpMatchIDs, byteCache, (byteCacheSize + IndexEntry.ID_LIST_LENGTH_SIZE));
				storeInt(idListBytes, byteCache, byteCacheSize, IndexEntry.ID_LIST_LENGTH_SIZE);
				byteCacheSize += (IndexEntry.ID_LIST_LENGTH_SIZE + idListBytes);
				listIdCount += pcpMatchIDs.length;
				listByteCount += idListBytes;
			}
			
			//	store data in tile arrays
			if (entryOffsetSize == entryOffsets.length)
				entryOffsets = doubleLength(entryOffsets);
			if (entryBytes.length < (entryByteSize + byteCacheSize)) {
				byte[] cRecordBytes = new byte[entryBytes.length * 2];
				System.arraycopy(entryBytes, 0, cRecordBytes, 0, entryBytes.length);
				entryBytes = cRecordBytes;
			}
			entryOffsets[entryOffsetSize++] = entryByteSize; // TODO maybe align to multiple of 4
			System.arraycopy(byteCache, 0, entryBytes, entryByteSize, byteCacheSize);
			entryByteSize += byteCacheSize;
		}
		br.close();
		
		//	shrink arrays
		if (entryOffsetSize < entryOffsets.length)
			entryOffsets = trimLength(entryOffsets, entryOffsetSize);
		if (entryByteSize < entryBytes.length) {
			byte[] cRecordBytes = new byte[entryByteSize];
			System.arraycopy(entryBytes, 0, cRecordBytes, 0, entryByteSize);
			entryBytes = cRecordBytes;
		}
		
		System.out.println("GOT " + entryOffsets.length + " ENTRIES WITH " + entryBytes.length + " BYTES");
		System.out.println("STORED " + listIdCount + " RECORD IDS IN " + listByteCount + " BYTES");
		
		//	finally ...
		return new IndexTile(col, minStr, maxStr, entryOffsets, entryBytes);
	}
	
	private static class IndexTile {
		final CatalogOfLifeLocal col;
		final byte[] minStr;
		final byte[] maxStr;
		final int[] entryOffsets;
		final byte[] data;
		IndexTile(CatalogOfLifeLocal col, byte[] minStr, byte[] maxStr, int[] entryOffsets, byte[] data) {
			this.col = col;
			this.minStr = minStr;
			this.maxStr = maxStr;
			this.entryOffsets = entryOffsets;
			this.data = data;
		}
		IndexEntry[] findMatches(byte[] str, boolean prefixMatch) {
			int start = findFirst(str, this.data, this.entryOffsets, prefixMatch);
			if (start == -1)
				return null;
			ArrayList matches = new ArrayList();
			do {
				matches.add(new IndexEntry(this, this.entryOffsets[start++]));
			}
			while ((start < this.entryOffsets.length) && (IndexEntry.compareStringTo(this.data, this.entryOffsets[start], str, prefixMatch) == 0));
			return ((IndexEntry[]) matches.toArray(new IndexEntry[matches.size()]));
		}
		private static int findFirst(byte[] str, byte[] data, int[] entryOffsets, boolean prefixMatch) {
			int low = 0;
			int high = (entryOffsets.length - 1);
			int pos = -1;
			while (low <= high) {
				int mid = ((low + high) / 2);
				int c = IndexEntry.compareStringTo(data, entryOffsets[mid], str, prefixMatch);
				if (c < 0)
					low = (mid + 1);
				else if (c > 0)
					high = (mid - 1);
				else {
					pos = mid;
					break;
				}
			}
			if (pos == -1)
				return -1;
			while (pos != 0) {
				int c = IndexEntry.compareStringTo(data, entryOffsets[pos-1], str, prefixMatch);
				if (c == 0)
					pos--;
				else break;
			}
			return pos;
		}
	}
	
	/* IndexEntry byte layout:
	 * - flags (1 byte):
	 *   - hasLowerCaseMatches
	 *   - hasCapitalizedMatches
	 *   - hasLowerCaseSynonymPrefixMatches
	 *   - hasCapitalizedSynonymPrefixMatches
	 * - epithet length (1 byte)
	 * - epithet string, all lower case (length as indicated)
	 * - ID list of taxa matching in lower case
	 * - ID list of taxa matching capitalized
	 * - ID list of synonym taxa whose prefix contains epithet in lower case
	 * - ID list of synonym taxa whose prefix contains epithet capitalized
	 * - ID list structure:
	 *   - number of IDs (1 or 2 bytes)
	 *     ==> assess this
	 *   - IDs (4 bytes each, no use compressing sequential IDs here due to low chance of there being any)
	 */
	/**
	 * An entry in the Catalog of Life full text index.
	 * 
	 * TODO JavaDoc this sucker !!!
	 * 
	 * @author sautter
	 */
	public static class IndexEntry {
		final IndexTile tile;
		final byte[] data;
		final int offset;
		IndexEntry(IndexTile tile, int offset) {
			this.tile = tile;
			this.data = this.tile.data;
			this.offset = offset;
		}
		public String getEntryString() {
			return getEntryString(this.data, this.offset);
		}
		int[] getLowerCaseMatchIDs() {
			return getEpithetMatchLowerCaseIDs(this.data, this.offset);
		}
		public TaxonRecord[] getLowerCaseMatches() {
			int[] matchIDs = getEpithetMatchLowerCaseIDs(this.data, this.offset);
			return ((matchIDs == null) ? null : this.getRecords(matchIDs));
		}
		int[] getCapitalizedMatchIDs() {
			return getEpithetMatchCapitalizedIDs(this.data, this.offset);
		}
		public TaxonRecord[] getCapitalizedMatches() {
			int[] matchIDs = getEpithetMatchCapitalizedIDs(this.data, this.offset);
			return ((matchIDs == null) ? null : this.getRecords(matchIDs));
		}
		int[] getLowerCasePrefixMatchIDs() {
			return getPrefixMatchLowerCaseIDs(this.data, this.offset);
		}
		public TaxonRecord[] getLowerCasePrefixMatches() {
			int[] matchIDs = getPrefixMatchLowerCaseIDs(this.data, this.offset);
			return ((matchIDs == null) ? null : this.getRecords(matchIDs));
		}
		int[] getCapitalizedPrefixMatchIDs() {
			return getPrefixMatchCapitalizedIDs(this.data, this.offset);
		}
		public TaxonRecord[] getCapitalizedPrefixMatches() {
			int[] matchIDs = getPrefixMatchCapitalizedIDs(this.data, this.offset);
			return ((matchIDs == null) ? null : this.getRecords(matchIDs));
		}
		private TaxonRecord[] getRecords(int[] ids) {
			TaxonRecord[] records = new TaxonRecord[ids.length];
			for (int i = 0; i < ids.length; i++)
				records[i] = this.tile.col.getRecord(ids[i]);
			return records;
		}
		
		static final int FLAGS_BYTE_OFFSET = 0; // position of flags byte from start of record
		static final byte HAS_EPITHET_MATCHES_LOWER_CASE = ((byte) 0x01);
		static boolean hasEpithetMatchesLowerCase(byte[] data, int offset) {
			return ((data[offset + FLAGS_BYTE_OFFSET] & HAS_EPITHET_MATCHES_LOWER_CASE) != 0);
		}
		static final byte HAS_EPITHET_MATCHES_CAPITALIZED = ((byte) 0x02);
		static boolean hasEpithetMatchesCapitalized(byte[] data, int offset) {
			return ((data[offset + FLAGS_BYTE_OFFSET] & HAS_EPITHET_MATCHES_CAPITALIZED) != 0);
		}
		static final byte HAS_PREFIX_MATCHES_LOWER_CASE = ((byte) 0x04);
		static boolean hasPrefixMatchesLowerCase(byte[] data, int offset) {
			return ((data[offset + FLAGS_BYTE_OFFSET] & HAS_PREFIX_MATCHES_LOWER_CASE) != 0);
		}
		static final byte HAS_PREFIX_MATCHES_CAPITALIZED = ((byte) 0x08);
		static boolean hasPrefixMatchesCapitalized(byte[] data, int offset) {
			return ((data[offset + FLAGS_BYTE_OFFSET] & HAS_PREFIX_MATCHES_CAPITALIZED) != 0);
		}
		static final int ENTRY_LENGTH_OFFSET = 1; // position of entry value length from start of record
		static final int ENTRY_LENGTH_SIZE = 1; // number of bytes used to store length of entry value
		static final int ENTRY_VALUE_OFFSET = 2; // start position of entry value from start of record
		static String getEntryString(byte[] data, int offset) {
			int entryLength = getInt(data, (offset + ENTRY_LENGTH_OFFSET), ENTRY_LENGTH_SIZE);
			return getString(data, (offset + ENTRY_VALUE_OFFSET), entryLength);
		}
		static int compareStringTo(byte[] data, int offset, byte[] str, boolean prefixMatch) {
			int entryLength = getInt(data, (offset + ENTRY_LENGTH_OFFSET), ENTRY_LENGTH_SIZE);
			return compareStringBytes(data, (offset + ENTRY_VALUE_OFFSET), (prefixMatch ? Math.min(entryLength, str.length) : entryLength), str, 0, str.length, true);
		}
		static final int ID_LIST_LENGTH_SIZE = 2; // number of bytes used to store length of ID list in bytes
		static int[] getEpithetMatchLowerCaseIDs(byte[] data, int offset) {
			if (!hasEpithetMatchesLowerCase(data, offset))
				return null;
			int entryLength = getInt(data, (offset + ENTRY_LENGTH_OFFSET), ENTRY_LENGTH_SIZE);
			int elcIdListLength = getInt(data, (offset + ENTRY_VALUE_OFFSET + entryLength), ID_LIST_LENGTH_SIZE);
			return getInts(data, (offset + ENTRY_VALUE_OFFSET + entryLength + ID_LIST_LENGTH_SIZE), elcIdListLength);
		}
		static int[] getEpithetMatchCapitalizedIDs(byte[] data, int offset) {
			if (!hasEpithetMatchesCapitalized(data, offset))
				return null;
			int entryLength = getInt(data, (offset + ENTRY_LENGTH_OFFSET), ENTRY_LENGTH_SIZE);
			int elcListLength = 0;
			if (hasEpithetMatchesLowerCase(data, offset)) {
				int elcIdListLength = getInt(data, (offset + ENTRY_VALUE_OFFSET + entryLength), ID_LIST_LENGTH_SIZE);
				elcListLength = (ID_LIST_LENGTH_SIZE + elcIdListLength);
			}
			int ecpIdListLength = getInt(data, (offset + ENTRY_VALUE_OFFSET + entryLength + elcListLength), ID_LIST_LENGTH_SIZE);
			return getInts(data, (offset + ENTRY_VALUE_OFFSET + entryLength + elcListLength + ID_LIST_LENGTH_SIZE), ecpIdListLength);
		}
		static int[] getPrefixMatchLowerCaseIDs(byte[] data, int offset) {
			if (!hasPrefixMatchesLowerCase(data, offset))
				return null;
			int entryLength = getInt(data, (offset + ENTRY_LENGTH_OFFSET), ENTRY_LENGTH_SIZE);
			int elcListLength = 0;
			if (hasEpithetMatchesLowerCase(data, offset)) {
				int elcIdListLength = getInt(data, (offset + ENTRY_VALUE_OFFSET + entryLength), ID_LIST_LENGTH_SIZE);
				elcListLength = (ID_LIST_LENGTH_SIZE + elcIdListLength);
			}
			int ecpListLength = 0;
			if (hasEpithetMatchesCapitalized(data, offset)) {
				int ecpIdListLength = getInt(data, (offset + ENTRY_VALUE_OFFSET + entryLength + elcListLength), ID_LIST_LENGTH_SIZE);
				ecpListLength = (ID_LIST_LENGTH_SIZE + ecpIdListLength);
			}
			int plcIdListLength = getInt(data, (offset + ENTRY_VALUE_OFFSET + entryLength + elcListLength + ecpListLength), ID_LIST_LENGTH_SIZE);
			return getInts(data, (offset + ENTRY_VALUE_OFFSET + entryLength + elcListLength + ecpListLength + ID_LIST_LENGTH_SIZE), plcIdListLength);
		}
		static int[] getPrefixMatchCapitalizedIDs(byte[] data, int offset) {
			if (!hasPrefixMatchesCapitalized(data, offset))
				return null;
			int entryLength = getInt(data, (offset + ENTRY_LENGTH_OFFSET), ENTRY_LENGTH_SIZE);
			int elcListLength = 0;
			if (hasEpithetMatchesLowerCase(data, offset)) {
				int elcIdListLength = getInt(data, (offset + ENTRY_VALUE_OFFSET + entryLength), ID_LIST_LENGTH_SIZE);
				elcListLength = (ID_LIST_LENGTH_SIZE + elcIdListLength);
			}
			int ecpListLength = 0;
			if (hasEpithetMatchesCapitalized(data, offset)) {
				int ecpIdListLength = getInt(data, (offset + ENTRY_VALUE_OFFSET + entryLength + elcListLength), ID_LIST_LENGTH_SIZE);
				ecpListLength = (ID_LIST_LENGTH_SIZE + ecpIdListLength);
			}
			int plcListLength = 0;
			if (hasPrefixMatchesLowerCase(data, offset)) {
				int plcIdListLength = getInt(data, (offset + ENTRY_VALUE_OFFSET + entryLength + elcListLength + ecpListLength), ID_LIST_LENGTH_SIZE);
				plcListLength = (ID_LIST_LENGTH_SIZE + plcIdListLength);
			}
			int pcpIdListLength = getInt(data, (offset + ENTRY_VALUE_OFFSET + entryLength + elcListLength + ecpListLength + plcListLength), ID_LIST_LENGTH_SIZE);
			return getInts(data, (offset + ENTRY_VALUE_OFFSET + entryLength + elcListLength + ecpListLength + plcListLength + ID_LIST_LENGTH_SIZE), pcpIdListLength);
		}
	}
	
	private static class ByteArrayCharSequence implements CharSequence {
		private byte[] data;
		private int offset;
		private int length;
		ByteArrayCharSequence(byte[] data, int offset, int length) {
			this.data = data;
			this.offset = offset;
			this.length = length;
		}
		void update(byte[] data, int offset, int length) {
			this.data = data;
			this.offset = offset;
			this.length = length;
		}
		public int length() {
			return this.length;
		}
		public char charAt(int index) {
			return ((char) (this.data[this.offset + index] & 0xFF));
		}
		public CharSequence subSequence(int start, int end) {
			return new ByteArrayCharSequence(this.data, (this.offset + start), (end - start));
		}
		public String toString() {
			StringBuffer ss = new StringBuffer();
			for (int c = 0; c < this.length; c++)
				ss.append(this.charAt(c));
			return ss.toString();
		}
	}
	
	private static class ByteSequenceMatcher {
		private Pattern pattern;
		private Matcher matcher;
		private ByteArrayCharSequence chars;
		ByteSequenceMatcher(String pattern) {
			this(Pattern.compile(pattern));
		}
		ByteSequenceMatcher(Pattern pattern) {
			this.pattern = pattern;
		}
		boolean matches(byte[] data, int offset, int length) {
			if (this.chars == null)
				this.chars = new ByteArrayCharSequence(data, offset, length);
			else this.chars.update(data, offset, length);
			if (this.matcher == null)
				this.matcher = this.pattern.matcher(this.chars);
			else this.matcher.reset();
			return this.matcher.matches();
		}
	}
	
	/* TaxonRecord byte layout:
	 * - ID (4 bytes)
	 * - parent/valid ID (4 bytes)
	 * - flags (1 byte):
	 *   - isValid
	 *   - isPrimaryRank
	 *   - isFossile ??? ==> TODO check if there is flags in source data, or emulate from tailing cross
	 *   - hasOriginalParent
	 *   - hasChildren
	 *   - hasSynonyms
	 *   - storesVerbatimAuthority (for 'L.', etc. ... no use encoding those)
	 * - rank (1 byte)
	 * - epithet length (1 byte)
	 * - authority length (1 byte)
	 * - epithet (as indicated, usually ~10 bytes)
	 * - authority (as indicated, usually 4 or 6 bytes, maybe 8 or 10):
	 *   - string ID (4 bytes)
	 *   - year(s) in order of occurrence (2 bytes each)
	 * - for valif taxa:
	 *   - list of children:
	 *     - number of bytes in list (2 bytes)
	 *     - list content:
	 *       - each ID (4 bytes)
	 *       - optional single byte < 0 indicating in low-most 7 bits (up to 127) number of immediately sequential IDs to follow
	 *   - list of synonyms (same as list of children)
	 * - for synonyms below genus:
	 *   - EITHER original parent ID (4 bytes)
	 *   - OR original parent epithet:
	 *     - 1 byte length
	 *     - original parent epithet (as indicated by length)
	 */
	/**
	 * A taxon, as a node in the tree of life.
	 * 
	 * TODO JavaDoc this sucker !!!
	 * 
	 * @author sautter
	 */
	public static class TaxonRecord {
		final DataTile tile;
		final byte[] data;
		final int offset;
		TaxonRecord(DataTile tile, int offset) {
			this.tile = tile;
			this.data = this.tile.data;
			this.offset = offset;
		}
		public int getId() {
			return getId(this.data, this.offset);
		}
		public String getOriginalColId() {
			int id = getId(this.data, this.offset);
			int colId = this.tile.colGetId(id);
			return ((colId == -1) ? null : encodeIntBase29(colId));
		}
		public String getEpithet() {
			return getEpithet(this.data, this.offset);
		}
		public boolean isValidTaxon() {
			return isValidTaxon(this.data, this.offset);
		}
		public boolean isExtantTaxon() {
			return isExtant(this.data, this.offset);
		}
		public String getRank() {
			byte rank = getRank(this.data, this.offset);
			return decodeRank(rank);
		}
		byte getRankByte() {
			return getRank(this.data, this.offset);
		}
		public boolean isPrimaryRank() {
			return isPrimaryRank(this.data, this.offset);
		}
		public TaxonRecord getParent() {
			if (isValidTaxon(this.data, this.offset)) {
				int parentId = getParentOrValidId(this.data, this.offset);
				return ((parentId == 0) ? null : this.tile.getRecord(parentId));
			}
			else return null;
		}
		public TaxonRecord[] getChildren() {
			int[] childIDs = getChildIDs(this.data, this.offset);
			if (childIDs == null)
				return null;
			TaxonRecord[] children = new TaxonRecord[childIDs.length];
			for (int c = 0; c < childIDs.length; c++)
				children[c] = this.tile.getRecord(childIDs[c]);
			return children;
		}
		public TaxonRecord getPrimaryParent() {
			TaxonRecord parent = this.getParent();
			if (parent == null)
				return null;
			else if (parent.isPrimaryRank())
				return parent;
			else return parent.getPrimaryParent();
		}
		public TaxonRecord[] getPrimaryChildren() {
			return this.getPrimaryChildren(false);
		}
		public TaxonRecord[] getPrimaryChildren(boolean includeSynonyms) {
			int[] pChildIDs = this.tile.getPrimaryChildIDs(getId(this.data, this.offset), includeSynonyms);
			if (pChildIDs == null)
				return null;
			TaxonRecord[] pChildren = new TaxonRecord[pChildIDs.length];
			for (int c = 0; c < pChildIDs.length; c++)
				pChildren[c] = this.tile.getRecord(pChildIDs[c]);
			return pChildren;
		}
		public TaxonRecord[] findDescendants(String query, boolean prefixMatch, boolean caseSensitive, String rank, boolean includeSynonyms) {
			byte qRank = encodeRank(rank);
			byte[] qStr = getQueryBytes(query);
			int[] descendantIDs = this.tile.findDescendantIDs(getId(this.data, this.offset), qStr, prefixMatch, caseSensitive, qRank, includeSynonyms);
			if (descendantIDs == null)
				return null;
			TaxonRecord[] descendants = new TaxonRecord[descendantIDs.length];
			for (int d = 0; d < descendantIDs.length; d++)
				descendants[d] = this.tile.getRecord(descendantIDs[d]);
			return descendants;
		}
		public Properties getHigherTaxonomy() {
			return this.getHigherTaxonomy(false);
		}
		public Properties getHigherTaxonomy(boolean allRanks) {
			int id = getId(this.data, this.offset);
			return this.tile.getHigherTaxonomy(id, allRanks);
		}
		public TaxonRecord getValidTaxon() {
			if (isValidTaxon(this.data, this.offset))
				return this;
			int validId = getParentOrValidId(this.data, this.offset);
			return this.tile.getRecord(validId);
		}
		public String getOriginalParentEpithets() {
			if (isValidTaxon(this.data, this.offset))
				return null;
			if (!hasOriginalParent(this.data, this.offset))
				return null;
			return getOriginalParentEpithets(this.data, this.offset);
		}
		public TaxonRecord[] getSynonyms() {
			int[] synonymIDs = getSynonymIDs(this.data, this.offset);
			if (synonymIDs == null)
				return null;
			TaxonRecord[] synonyms = new TaxonRecord[synonymIDs.length];
			for (int s = 0; s < synonymIDs.length; s++)
				synonyms[s] = this.tile.getRecord(synonymIDs[s]);
			return synonyms;
		}
		public String getAuthority() {
			byte[] authorityBytes = getAuthorityBytes(this.data, this.offset);
			if (authorityBytes == null)
				return null;
			else if (storesVerbatimAuthority(this.data, this.offset))
				return getString(authorityBytes, 0, authorityBytes.length);
			else return this.tile.decodeAuthority(authorityBytes);
		}
		public TaxonomicAuthority getCombinationAuthority() {
			String authority = this.getAuthority();
			if (authority == null)
				return null;
			authority = TaxonomicNameUtils.getCombinationAuthority(authority);
			if (authority == null)
				return null;
			return TaxonomicNameUtils.parseAuthority(authority);
		}
		public TaxonomicAuthority getBaseAuthority() {
			String authority = this.getAuthority();
			if (authority == null)
				return null;
			authority = TaxonomicNameUtils.getBasionymAuthority(authority);
			if (authority == null)
				return null;
			return TaxonomicNameUtils.parseAuthority(authority);
		}
		public String getNameString() {
			return this.getNameString(false);
		}
		public String getNameString(boolean includeAuthority) {
			StringBuffer nameString = new StringBuffer();
			byte rank = getRank(this.data, this.offset);
			int parenthesisEpithetCount = 0;
			if (genusRankLevel < rank) /* all set with own epithet in genus and above */ {
				String prefix = this.getOriginalParentEpithets();
				
				//	synonym whose original combination has parent different from ones in current tree
				if (prefix != null)
					nameString.append(prefix);
				
				//	valid taxon, or synonym has same parent as associated valid taxon
				else for (TaxonRecord pTr = (this.isValidTaxon() ? this : this.getValidTaxon()).getParent(); pTr != null; pTr = pTr.getParent()) {
					if (nameString.length() != 0)
						nameString.insert(0, " ");
					byte pRank = pTr.getRankByte();
					if (pRank < genusRankLevel)
						break;
					nameString.insert(0, formatEpithet(pTr.getEpithet(), pRank));
					if (pRank == genusRankLevel)
						break;
					if (pRank < speciesRankLevel)
						parenthesisEpithetCount++;
				}
			}
			if (nameString.length() != 0)
				nameString.append(" ");
			nameString.append(formatEpithet(this.getEpithet(), rank));
			if ((genusRankLevel < rank) && (rank < speciesRankLevel))
				parenthesisEpithetCount++;
			if (parenthesisEpithetCount > 1) {
				for (int s = 0; (s = nameString.indexOf(") (", s)) != -1;)
					nameString.replace(s, (s + ") (".length()), ", ");
			}
			if (includeAuthority) {
				String auth = this.getAuthority();
				if (auth != null) {
					nameString.append(" ");
					nameString.append(auth);
				}
			}
			return nameString.toString();
		}
		public TaxonomicName getName() {
			TaxonomicName taxName = new TaxonomicName();
			taxName.setEpithet(this.getRank(), this.getEpithet());
			String prefix = ((this.getRankByte() <= genusRankLevel) ? null : this.getOriginalParentEpithets());
			if (prefix == null) {
				for (TaxonRecord tr = (this.isValidTaxon() ? this : this.getValidTaxon()).getParent(); tr != null; tr = tr.getParent())
					taxName.setEpithet(tr.getRank(), tr.getEpithet());
			}
			else {
				String[] prefixParts = prefix.split("\\s+");
				taxName.setEpithet(GENUS_ATTRIBUTE, prefixParts[0]); // first one's always the genus
				for (int p = 1; p < prefixParts.length; p++) {
					String ppRank;
					String ppEpithet;
					if (prefixParts[p].endsWith(")")) {
						if (prefixParts[p].startsWith("(")) {
							ppRank = SUBGENUS_ATTRIBUTE;
							ppEpithet = prefixParts[p].substring("(".length(), (prefixParts[p].length() - ")".length()));
						}
						else if (prefixParts[p-1].endsWith("subsect.")) {
							ppRank = SUBSECTION_ATTRIBUTE;
							ppEpithet = prefixParts[p].substring(0, (prefixParts[p].length() - ")".length()));
						}
						else if (prefixParts[p-1].endsWith("subser.")) {
							ppRank = SUBSERIES_ATTRIBUTE;
							ppEpithet = prefixParts[p].substring(0, (prefixParts[p].length() - ")".length()));
						}
						else if (prefixParts[p-1].endsWith("sect.")) {
							ppRank = SECTION_ATTRIBUTE;
							ppEpithet = prefixParts[p].substring(0, (prefixParts[p].length() - ")".length()));
						}
						else if (prefixParts[p-1].endsWith("ser.")) {
							ppRank = SERIES_ATTRIBUTE;
							ppEpithet = prefixParts[p].substring(0, (prefixParts[p].length() - ")".length()));
						}
						else {
							ppRank = null;
							ppEpithet = null;
						}
					}
					else if ("subsp.".equals(prefixParts[p]))
						continue;
					else if ("var.".equals(prefixParts[p]))
						continue;
					else if ("subvar.".equals(prefixParts[p]))
						continue;
					else if ("f.".equals(prefixParts[p]))
						continue;
					else if ("subf.".equals(prefixParts[p]))
						continue;
					else {
						if ("subsp.".equals(prefixParts[p-1]))
							ppRank = SUBSPECIES_ATTRIBUTE;
						else if ("var.".equals(prefixParts[p-1]))
							ppRank = VARIETY_ATTRIBUTE;
						else if ("subvar.".equals(prefixParts[p-1]))
							ppRank = SUBVARIETY_ATTRIBUTE;
						else if ("f.".equals(prefixParts[p]))
							ppRank = FORM_ATTRIBUTE;
						else if ("subf.".equals(prefixParts[p]))
							ppRank = SUBFORM_ATTRIBUTE;
						else if (taxName.getEpithet(SPECIES_ATTRIBUTE) != null)
							ppRank = SUBSPECIES_ATTRIBUTE;
						else ppRank = SPECIES_ATTRIBUTE;
						ppEpithet = prefixParts[p];
					}
					if ((ppRank != null) && (ppEpithet != null) && (taxName.getEpithet(ppRank) == null))
						taxName.setEpithet(ppRank, ppEpithet);
				}
				for (TaxonRecord tr = this.getValidTaxon().getParent(); tr != null; tr = tr.getParent()) {
					if (tr.getRankByte() < genusRankLevel)
						taxName.setEpithet(tr.getRank(), tr.getEpithet());
				}
			}
			TaxonomicAuthority combAuth = this.getCombinationAuthority();
			if (combAuth != null)
				taxName.setAuthority(combAuth.name, combAuth.year);
			TaxonomicAuthority baseAuth = this.getBaseAuthority();
			if (baseAuth != null)
				taxName.setBaseAuthority(baseAuth.name, baseAuth.year);
			return taxName;
		}
		
		static final int ID_OFFSET = 0; // position of ID from start of record
		static final int ID_SIZE = 4; // number of bytes used to store IDs
		static int getId(byte[] data, int offset) {
			return getInt(data, (offset + ID_OFFSET), ID_SIZE);
		}
//		static int getOriginalColId(byte[] data, int offset) {
//			return getInt(data, (offset + ORIGINAL_COL_ID_OFFSET), ID_SIZE);
//		}
		static final int PARENT_OR_VALID_ID_OFFSET = 4; // position of parent/valid ID from start of record
		static int getParentOrValidId(byte[] data, int offset) {
			return getInt(data, (offset + PARENT_OR_VALID_ID_OFFSET), ID_SIZE);
		}
		static final int FLAGS_BYTE_OFFSET = 8; // position of flags byte from start of record
		static final byte IS_VALID = ((byte) 0x01);
		static boolean isValidTaxon(byte[] data, int offset) {
			return ((data[offset + FLAGS_BYTE_OFFSET] & IS_VALID) != 0);
		}
		static final byte IS_PRIMARY_RANK = ((byte) 0x02);
		static boolean isPrimaryRank(byte[] data, int offset) {
			return ((data[offset + FLAGS_BYTE_OFFSET] & IS_PRIMARY_RANK) != 0);
		}
		static final byte IS_EXTANT = ((byte) 0x04);
		static boolean isExtant(byte[] data, int offset) {
			return ((data[offset + FLAGS_BYTE_OFFSET] & IS_EXTANT) != 0);
		}
		static final byte HAS_ORIGINAL_PARENT = ((byte) 0x08);
		static boolean hasOriginalParent(byte[] data, int offset) {
			return ((data[offset + FLAGS_BYTE_OFFSET] & HAS_ORIGINAL_PARENT) != 0);
		}
		static final byte HAS_CHILDREN = ((byte) 0x10);
		static boolean hasChildren(byte[] data, int offset) {
			return ((data[offset + FLAGS_BYTE_OFFSET] & HAS_CHILDREN) != 0);
		}
		static final byte HAS_SYNONYMS = ((byte) 0x20);
		static boolean hasSynonyms(byte[] data, int offset) {
			return ((data[offset + FLAGS_BYTE_OFFSET] & HAS_SYNONYMS) != 0);
		}
		static final byte STORES_VERBATIM_AUTHORITY = ((byte) 0x40);
		static boolean storesVerbatimAuthority(byte[] data, int offset) {
			return ((data[offset + FLAGS_BYTE_OFFSET] & STORES_VERBATIM_AUTHORITY) != 0);
		}
		static final int EPITHET_LENGTH_OFFSET = 10; // position of epithet value length from start of record
		static final int VALUE_LENGTH_SIZE = 1; // number of bytes used to store length of epithet and authority values
		static final int EPITHET_VALUE_OFFSET = 12; // start position of epithet value from start of record
		static String getEpithet(byte[] data, int offset) {
			int epithetLength = getInt(data, (offset + EPITHET_LENGTH_OFFSET), VALUE_LENGTH_SIZE);
			return getString(data, (offset + EPITHET_VALUE_OFFSET), epithetLength);
		}
		static byte[] getEpithetBytes(byte[] data, int offset) {
			int epithetLength = getInt(data, (offset + EPITHET_LENGTH_OFFSET), VALUE_LENGTH_SIZE);
			byte[] epithetBytes = new byte[epithetLength];
			System.arraycopy(data, (offset + EPITHET_VALUE_OFFSET), epithetBytes, 0, epithetBytes.length);
			return epithetBytes;
		}
//		static boolean doesEpithetStartWith(byte[] data, int offset, byte[] prefix, boolean caseSensitive) {
//			int epithetLength = getInt(data, (offset + EPITHET_LENGTH_OFFSET), VALUE_LENGTH_SIZE);
//			return (compareStringBytes(data, (offset + EPITHET_VALUE_OFFSET), Math.min(epithetLength, prefix.length), prefix, 0, prefix.length, caseSensitive) == 0);
//		}
		static int compareEpithetTo(byte[] data, int offset, byte[] epithet, boolean prefixMatch, boolean caseSensitive) {
			int epithetLength = getInt(data, (offset + EPITHET_LENGTH_OFFSET), VALUE_LENGTH_SIZE);
			return compareStringBytes(data, (offset + EPITHET_VALUE_OFFSET), (prefixMatch ? Math.min(epithetLength, epithet.length) : epithetLength), epithet, 0, epithet.length, caseSensitive);
		}
		static final int RANK_BYTE_OFFSET = 9; // position of rank byte from start of record
		static byte getRank(byte[] data, int offset) {
			return data[offset + RANK_BYTE_OFFSET];
		}
		static final int AUTHORITY_LENGTH_OFFSET = 11; // position of authority value length from start of record
		static byte[] getAuthorityBytes(byte[] data, int offset) {
			int authorityLength = getInt(data, (offset + AUTHORITY_LENGTH_OFFSET), VALUE_LENGTH_SIZE);
			if (authorityLength == 0)
				return null;
			int epithetLength = getInt(data, (offset + EPITHET_LENGTH_OFFSET), VALUE_LENGTH_SIZE);
			byte[] authorityBytes = new byte[authorityLength];
			System.arraycopy(data, (offset + EPITHET_VALUE_OFFSET + epithetLength), authorityBytes, 0, authorityLength);
			return authorityBytes;
		}
		static final int ID_LIST_LENGTH_SIZE = 2; // number of bytes used to store length of ID list in bytes
		static int[] getChildIDs(byte[] data, int offset) {
			if (!hasChildren(data, offset))
				return null;
			int epithetLength = getInt(data, (offset + EPITHET_LENGTH_OFFSET), VALUE_LENGTH_SIZE);
			int authorityLength = getInt(data, (offset + AUTHORITY_LENGTH_OFFSET), VALUE_LENGTH_SIZE);
			int childIdListLength = getInt(data, (offset + EPITHET_VALUE_OFFSET + epithetLength + authorityLength), ID_LIST_LENGTH_SIZE);
			return getInts(data, (offset + EPITHET_VALUE_OFFSET + epithetLength + authorityLength + ID_LIST_LENGTH_SIZE), childIdListLength);
		}
		static String getOriginalParentEpithets(byte[] data, int offset) {
			if (!hasOriginalParent(data, offset))
				return null;
			int epithetLength = getInt(data, (offset + EPITHET_LENGTH_OFFSET), VALUE_LENGTH_SIZE);
			int authorityLength = getInt(data, (offset + AUTHORITY_LENGTH_OFFSET), VALUE_LENGTH_SIZE);
			int originalParentLength = getInt(data, (offset + EPITHET_VALUE_OFFSET + epithetLength + authorityLength), VALUE_LENGTH_SIZE);
			return getString(data, (offset + EPITHET_VALUE_OFFSET + epithetLength + authorityLength + VALUE_LENGTH_SIZE), originalParentLength);
		}
		static int[] getSynonymIDs(byte[] data, int offset) {
			if (!hasSynonyms(data, offset))
				return null;
			int epithetLength = getInt(data, (offset + EPITHET_LENGTH_OFFSET), VALUE_LENGTH_SIZE);
			int authorityLength = getInt(data, (offset + AUTHORITY_LENGTH_OFFSET), VALUE_LENGTH_SIZE);
			int childListLength = 0;
			if (hasChildren(data, offset)) {
				int childIdListLength = getInt(data, (offset + EPITHET_VALUE_OFFSET + epithetLength + authorityLength), ID_LIST_LENGTH_SIZE);
				childListLength = (ID_LIST_LENGTH_SIZE + childIdListLength);
			}
			int synonymIdListLength = getInt(data, (offset + EPITHET_VALUE_OFFSET + epithetLength + authorityLength + childListLength), 2);
			return getInts(data, (offset + EPITHET_VALUE_OFFSET + epithetLength + authorityLength + childListLength + ID_LIST_LENGTH_SIZE), synonymIdListLength);
		}
	}
	
	private InputStream getInputStream(String fileName) throws IOException {
		InputStream in;
		if (this.dataProvider == null) {
			String resName = CatalogOfLifeLocal.class.getName();
			resName = resName.substring(0, resName.lastIndexOf("."));
			resName = resName.replace('.', '/');
			resName = (resName + "/data/" + fileName);
			in = CatalogOfLifeLocal.class.getClassLoader().getResourceAsStream(resName);
			if (in == null)
				throw new FileNotFoundException(resName);
		}
		else in = this.dataProvider.getInputStream(fileName);
		return new BufferedInputStream(in);
	}
	
	private static final String base29chars = "23456789BCDFGHJKLMNPQRSTVWXYZ"; // characters from https://github.com/CatalogueOfLife/backend/blob/master/api/src/main/java/life/catalogue/common/id/IdConverter.java
	static String encodeIntBase29(int intPlain) {
		StringBuffer intBase29 = new StringBuffer();
		while (intPlain != 0) {
			int digit = (intPlain % base29chars.length());
			intBase29.insert(0, base29chars.charAt(digit));
			intPlain -= digit;
			intPlain /= base29chars.length();
		}
		if (intBase29.length() == 0)
			intBase29.append(base29chars.charAt(0));
		return intBase29.toString();
	}
	static int parseIntBase29(String intBase29) {
		int intPlain = 0;
		for (int c = 0; c < intBase29.length(); c++) {
			char ch = Character.toUpperCase(intBase29.charAt(c));
			int digit = base29chars.indexOf(ch);
			intPlain *= base29chars.length();
			if (digit == -1)
				throw new IllegalArgumentException("Cannot decode digit '" + ch + "' base 29 in input string '" + intBase29 + "'");
			else intPlain += digit;
		}
		return intPlain;
	}
	
	static int[] doubleLength(int[] ints) {
		int[] cInts = new int[ints.length * 2];
		System.arraycopy(ints, 0, cInts, 0, ints.length);
		return cInts;
	}
	
	static int[] trimLength(int[] ints, int length) {
		if (length < ints.length) {
			int[] cInts = new int[length];
			System.arraycopy(ints, 0, cInts, 0, cInts.length);
			return cInts;
		}
		else return ints;
	}
	
//	private static int getInt(byte[] data, int offset) {
//		return getInt(data, offset, 4);
//	}
	static int getInt(byte[] data, int offset, int length) {
		int i = 0;
		for (int b = 0; b < length; b++) {
			i <<= 8;
			i |= (data[offset + b] & 0xFF);
		}
		return i;
	}
	
//	private static void storeInt(int i, byte[] data, int offset) {
//		storeInt(i, data, offset, 4);
//	}
	private static void storeInt(int i, byte[] data, int offset, int length) {
		for (int b = (length - 1); b >= 0; b--) {
			int ib = (i & 0x000000FF);
			if (0x0000007F < ib)
				ib -= 0x00000100;
			data[offset + b] = ((byte) ib);
			i >>>= 8;
		}
	}
	
	static int[] getInts(byte[] data, int offset, int listLength) {
		return getInts(data, offset, listLength, 4);
	}
	private static int[] getInts(byte[] data, int offset, int listLength, int intLength) {
		IntBuffer ints = new IntBuffer();
		for (int i = 0; i < listLength;) {
			int s = getInt(data, (offset + i), intLength);
			i += intLength;
			ints.add(s);
			if ((i < listLength) && (data[offset + i] < 0)) {
				int c = (data[offset + i] & 0x7F);
				i++;
				for (; c > 0; c--) {
					s++;
					ints.add(s);
				}
			}
		}
		return ints.toArray();
	}
	
	private static int storeInts(int[] ints, byte[] data, int offset) {
		return storeInts(ints, data, offset, 4);
	}
	private static int storeInts(int[] ints, byte[] data, int offset, int intLength) {
		if ((ints == null) || (ints.length == 0))
			return 0;
		Arrays.sort(ints);
		int listEnd = offset;
		for (int i = 0; i < ints.length;) {
			storeInt(ints[i], data, listEnd, intLength);
			listEnd += intLength;
			int c = 0;
			while (((i + c + 1) < ints.length) && (c < 0x7F)) {
				if (ints[i + c + 1] == (ints[i + c] + 1))
					c++;
				else break;
			}
			i++;
			if (c != 0) {
				data[listEnd] = ((byte) (c | 0x80));
				listEnd++;
				i += c;
			}
		}
		return (listEnd - offset);
	}
	
	static String getString(byte[] data, int offset, int length) {
		char[] str = new char[length];
		for (int c = 0; c < str.length; c++)
			str[c] = ((char) (data[offset + c] & 0x00FF));
		return new String(str);
	}
	
	static int compareStringBytes(byte[] data1, int offset1, int length1, byte[] data2, int offset2, int length2, boolean caseSensitive) {
		for (int b = 0; b < Math.min(length1, length2); b++) {
			byte b1 = data1[offset1 + b];
			byte b2 = data2[offset2 + b];
			if (b1 == b2)
				continue;
			if (caseSensitive)
				return (b1 - b2);
			/* setting 32-bit (0x20), the 'lower case bit', is basically
			 * Character.toLowerCase() in Basic Latin, but way faster */
			if (isLetter(b1) && isLetter(b2)) {
				b1 = ((byte) (b1 | 0x20));
				b2 = ((byte) (b2 | 0x20));
				if (b1 == b2)
					continue;
			}
			return (b1 - b2);
		}
		return (length1 - length2);
	}
	static boolean isLetter(byte ch) {
		/* going high-to-low evaluates fewer conditions for lower case
		 * letters, which are far more frequent than upper case ones */
		if ('z' < ch)
			return false;
		else if ('a' <= ch)
			return true;
		else if ('Z' < ch)
			return false;
		else if ('A' <= ch)
			return true;
		else return false;
	}
	
	private static byte[] encodeString(String str) {
		byte[] bytes = new byte[str.length()];
		storeString(str, bytes, 0);
		return bytes;
	}
	
	private static void storeString(String str, byte[] data, int offset) {
		for (int c = 0; c < str.length(); c++) {
			char ch = str.charAt(c);
			if (0x007F < ch)
				throw new IllegalArgumentException("Invalid character " + ch + " (0x" + Integer.toString(((int) ch), 16).toUpperCase() + ") in '" + str + "' at " + c);
			data[offset + c] = ((byte) (ch & 0x00FF));
		}
	}
	
	/**
	 * Retrieve the local in-memory representation of the Catalog of Life data.
	 * If no instance has been created so far, it is created by the call to
	 * this method. There only ever is a single instance.
	 * @return the catalog of life instance
	 */
	public static CatalogOfLifeLocal getInstance() {
		return getInstance((AnalyzerDataProvider) null);
	}
	
	/**
	 * Retrieve the local in-memory representation of the Catalog of Life data.
	 * If no instance has been created so far, it is created by the call to
	 * this method. There only ever is a single instance. If the argument data
	 * path is not null and no instance has been created yet, it is created
	 * from the data found in the argument folder.
	 * @param dataPath the folder to load data from
	 * @return the catalog of life instance
	 */
	public static CatalogOfLifeLocal getInstance(File dataPath) {
		return getInstance((dataPath == null) ? null : new AnalyzerDataProviderFileBased(dataPath));
	}
	
	/**
	 * Retrieve the local in-memory representation of the Catalog of Life data.
	 * If no instance has been created so far, it is created by the call to
	 * this method. There only ever is a single instance. If the argument data
	 * provider is not null and no instance has been created yet, it is created
	 * from the data offered by the argument provider.
	 * @param dataProvider the data provider to load data through
	 * @return the catalog of life instance
	 */
	public static CatalogOfLifeLocal getInstance(AnalyzerDataProvider dataProvider) {
		if (instance == null) try {
			instance = new CatalogOfLifeLocal(dataProvider);
		}
		catch (Exception e) {
			throw new RuntimeException(("Failed to initialize CoL-Local: " + e.getMessage()), e);
		}
		return instance;
	}
	private static CatalogOfLifeLocal instance = null;
	
	/**
	 * Set the maximum number of species tiles to simultaneously hold in memory
	 * without evicting any. If this number is negative or 0, the number is not
	 * limited, and all tiles remain in memory once they have been loaded.
	 * Setting this threshold to a positive number enacts a least recently used
	 * eviction strategy. The default value for this limit is -1. Setting it to
	 * 0 will pre-fetch all tiles.
	 * @param msst the maximum number of species tiles to simultaneously hold
	 */
	public void setMaxSimultaneousSpeciesTiles(int msst) {
		if (this.maxSimultaneousSpeciesTiles == msst)
			return;
		boolean checkEvictions = ((0 < msst) && ((this.maxSimultaneousSpeciesTiles < 1) || (msst < this.maxSimultaneousSpeciesTiles)));
		this.maxSimultaneousSpeciesTiles = msst;
		if (checkEvictions)
			this.trimSpeciesTiles();
		else if (this.maxSimultaneousSpeciesTiles == 0) {
			for (int t = 0; t < this.speciesTiles.length; t++)
				this.speciesTiles[t].getTile();
		}
	}
	
	/**
	 * Set the maximum number of index tiles to simultaneously hold in memory
	 * without evicting any. If this number is negative or 0, the number is not
	 * limited, and all tiles remain in memory once they have been loaded.
	 * Setting this threshold to a positive number enacts a least recently used
	 * eviction strategy. The default value for this limit is -1. Setting it to
	 * 0 will pre-fetch all tiles.
	 * @param msit the maximum number of index tiles to simultaneously hold
	 */
	public void setMaxSimultaneousIndexTiles(int msit) {
		if (this.maxSimultaneousIndexTiles == msit)
			return;
		boolean checkEvictions = ((0 < msit) && ((this.maxSimultaneousIndexTiles < 1) || (msit < this.maxSimultaneousIndexTiles)));
		this.maxSimultaneousIndexTiles = msit;
		if (checkEvictions)
			this.trimIndexTiles();
		else if (this.maxSimultaneousIndexTiles == 0) {
			for (int t = 0; t < this.indexTiles.length; t++)
				this.indexTiles[t].getTile();
		}
	}
	
	/**
	 * Retrieve a taxon record from the Catalog of Life via its original base
	 * 29 identifier. If the argument identifier does not match any taxon
	 * record, this method returns null.
	 * @param colIdBase29 the original base 29 identifier assigned by CoL
	 * @return the taxon record with the argument original ID
	 */
	public TaxonRecord getTaxonRecord(String colIdBase29) {
		int colId = parseIntBase29(colIdBase29);
		
		//	check higher tile first thing (always loaded)
		if (this.higherTile.colSpansRecord(colId)) {
			TaxonRecord tr = this.higherTile.colGetRecord(colId);
			if (tr != null)
				return tr;
		}
		
		//	check currently loaded tiles (saves lots of IO on hits)
		ArrayList toLoadTiles = new ArrayList();
		for (int t = 0; t < this.speciesTiles.length; t++) {
			if (!this.speciesTiles[t].spansColRecord(colId))
				continue;
			if (this.speciesTiles[t].isTileLoaded()) {
				DataTile tile = this.speciesTiles[t].getTile();
				TaxonRecord tr = tile.colGetRecord(colId);
				if (tr != null)
					return tr;
			}
			else toLoadTiles.add(this.speciesTiles[t]);
		}
		
		//	check remaining tiles that might contain target ID
		for (int t = 0; t < toLoadTiles.size(); t++) {
			DataTileProxy speciesTile = ((DataTileProxy) toLoadTiles.get(t));
			DataTile tile = speciesTile.getTile();
			TaxonRecord tr = tile.colGetRecord(colId);
			if (tr != null)
				return tr;
		}
		return null;
	}
	
	/**
	 * Full text search the Catalog of Life. This method only searches through
	 * taxon names, however, not through authorities. The argument query must
	 * not contain any spaces, i.e., represent (a prefix of) a single epithet,
	 * as any semantic relationship between multiple epithets can be ambiguous
	 * and is better to figure out in client code. The search is case
	 * insensitive, but the four categories of taxon records available from the
	 * returned index entries allow client code to track the type of the match
	 * and to retrieve the taxon records it needs.
	 * @param query the query to execute
	 * @param prefixMatch allow prefix matches (false means exact match)
	 * @return an array holding the index entries matching the query
	 */
	public IndexEntry[] searchTaxonRecords(String query, boolean prefixMatch) {
		String lQuery = query.toLowerCase();
		byte[] qQuery = getQueryBytes(lQuery);
		IndexTile indexTile = this.getIndexTileForString(qQuery);
		return ((indexTile == null) ? null : indexTile.findMatches(qQuery, prefixMatch));
	}
	
	/**
	 * Find taxon records in the Catalog of Life. This method only searches
	 * through taxon names, however, not through authorities. The argument
	 * query must not contain any spaces, i.e., represent (a prefix of) a
	 * single epithet, as any semantic relationship between multiple epithets
	 * can be ambiguous and is better to figure out in client code.
	 * @param query the query to execute
	 * @param rank the rank of the sought taxon records (may be null)
	 * @return an array holding the taxon records matching the query
	 */
	public TaxonRecord[] findTaxonRecords(String query, String rank) {
		return this.findTaxonRecords(query, rank, rank);
	}
	
	/**
	 * Find taxon records in the Catalog of Life. This method only searches
	 * through taxon names, however, not through authorities. The argument
	 * query must not contain any spaces, i.e., represent (a prefix of) a
	 * single epithet, as any semantic relationship between multiple epithets
	 * can be ambiguous and is better to figure out in client code. The argument
	 * ranks are treated inclusively, with <code>fromRank</code> indicating
	 * the highest desired rank in the result, and <code>toRank</code> the
	 * lowest desired rank; either one or both may be null.
	 * @param query the query to execute
	 * @param fromRank the highest rank to consider
	 * @param toRank the lowest rank to consider
	 * @return an array holding the taxon records matching the query
	 */
	public TaxonRecord[] findTaxonRecords(String query, String fromRank, String toRank) {
		return this.findTaxonRecords(query, fromRank, toRank, false, true, false);
	}
	
	/**
	 * Find taxon records in the Catalog of Life. This method only searches
	 * through taxon names, however, not through authorities. The argument
	 * query must not contain any spaces, i.e., represent (a prefix of) a
	 * single epithet, as any semantic relationship between multiple epithets
	 * can be ambiguous and is better to figure out in client code.
	 * @param query the query to execute
	 * @param rank the rank of the sought taxon records (may be null)
	 * @param prefixMatch allow prefix matches (false means exact match)
	 * @param caseSensitive search case sensitive?
	 * @param includeSynonyms include synonyms in the result?
	 * @return an array holding the taxon records matching the query
	 */
	public TaxonRecord[] findTaxonRecords(String query, String rank, boolean prefixMatch, boolean caseSensitive, boolean includeSynonyms) {
		return this.findTaxonRecords(query, rank, rank, prefixMatch, caseSensitive, includeSynonyms);
	}
	
	/**
	 * Find taxon records in the Catalog of Life. This method only searches
	 * through taxon names, however, not through authorities. The argument
	 * query must not contain any spaces, i.e., represent (a prefix of) a
	 * single epithet, as any semantic relationship between multiple epithets
	 * can be ambiguous and is better to figure out in client code. The argument
	 * ranks are treated inclusively, with <code>fromRank</code> indicating
	 * the highest desired rank in the result, and <code>toRank</code> the
	 * lowest desired rank; either one or both may be null.
	 * @param query the query to execute
	 * @param fromRank the highest rank to consider
	 * @param toRank the lowest rank to consider
	 * @param prefixMatch allow prefix matches (false means exact match)
	 * @param caseSensitive search case sensitive?
	 * @param includeSynonyms include synonyms in the result?
	 * @return an array holding the taxon records matching the query
	 */
	public TaxonRecord[] findTaxonRecords(String query, String fromRank, String toRank, boolean prefixMatch, boolean caseSensitive, boolean includeSynonyms) {
		byte qFromRank = encodeRank(fromRank);
		byte qToRank = encodeRank(toRank);
		if (qToRank == -1)
			qToRank = Byte.MAX_VALUE;
		int[] matchIDs;
		
		//	only above species, can use higher tile directly
		if (qToRank < speciesRankLevel) {
			
			//	do lookup
			byte[] qQuery = getQueryBytes(query);
			matchIDs = this.higherTile.findRecordIDs(qQuery, ((qFromRank == qToRank) ? qFromRank : -1), prefixMatch, caseSensitive);
			if (matchIDs == null)
				return null;
			
			//	filter for rank
			if ((qFromRank != qToRank) && ((qToRank - qFromRank) != 128)) {
				IntBuffer rankMatchIDs = new IntBuffer();
				for (int m = 0; m < matchIDs.length; m++) {
					byte mRank = this.higherTile.getRankByte(matchIDs[m]);
					if ((qFromRank <= mRank) && (mRank <= qToRank))
						rankMatchIDs.add(matchIDs[m]);
				}
				rankMatchIDs.finishResult();
				if (rankMatchIDs.size() == 0)
					return null;
				matchIDs = rankMatchIDs.toArray();
			}
			
			//	filter for synonyms
			if (!includeSynonyms) {
				IntBuffer validMatchIDs = new IntBuffer();
				for (int m = 0; m < matchIDs.length; m++) {
					if (this.higherTile.isValidTaxon(matchIDs[m]))
						validMatchIDs.add(matchIDs[m]);
				}
				validMatchIDs.finishResult();
				if (validMatchIDs.size() == 0)
					return null;
				matchIDs = validMatchIDs.toArray();
			}
		}
		
		//	use full text index otherwise
		else {
			
			//	do lookup
			String lQuery = query.toLowerCase();
			byte[] qQuery = getQueryBytes(lQuery);
			IndexTile indexTile = this.getIndexTileForString(qQuery);
			if (indexTile == null)
				return null;
			IndexEntry[] qResults = indexTile.findMatches(qQuery, prefixMatch);
			if (qResults == null)
				return null;
			
			//	filter for case
			IntBuffer qMatchIDs = new IntBuffer();
			boolean addLowerCaseResults = (!caseSensitive || lQuery.equals(query) /* lower case query */);
			boolean addCapitalizedResults = (!caseSensitive || !lQuery.equals(query) /* capitalized query */);
			for (int r = 0; r < qResults.length; r++) {
				if (addLowerCaseResults) {
					int[] ids = qResults[r].getLowerCaseMatchIDs();
					if (ids != null)
						qMatchIDs.addAll(ids);
					if (includeSynonyms) {
						ids = qResults[r].getLowerCasePrefixMatchIDs();
						if (ids != null)
							qMatchIDs.addAll(ids);
					}
				}
				if (addCapitalizedResults) {
					int[] ids = qResults[r].getCapitalizedMatchIDs();
					if (ids != null)
						qMatchIDs.addAll(ids);
					if (includeSynonyms) {
						ids = qResults[r].getCapitalizedPrefixMatchIDs();
						if (ids != null)
							qMatchIDs.addAll(ids);
					}
				}
			}
			qMatchIDs.finishResult();
			if (qMatchIDs.size() == 0)
				return null;
			
			//	filter for rank
			if ((qFromRank != qToRank) && ((qToRank - qFromRank) != 128)) {
				DataTile dataTile = null;
				IntBuffer qRankMatchIDs = new IntBuffer();
				for (int m = 0; m < qMatchIDs.size(); m++) {
					int mid = qMatchIDs.get(m);
					if ((dataTile == null) || !dataTile.containsRecord(mid))
						dataTile = this.getDataTileForId(mid);
					byte mRank = dataTile.getRankByte(mid);
					if ((qFromRank <= mRank) && (mRank <= qToRank))
						qRankMatchIDs.add(mid);
				}
				qRankMatchIDs.finishResult();
				if (qRankMatchIDs.size() == 0)
					return null;
				qMatchIDs = qRankMatchIDs;
			}
			
			//	filter for synonyms
			if (!includeSynonyms) {
				DataTile dataTile = null;
				IntBuffer qValidMatchIDs = new IntBuffer();
				for (int m = 0; m < qMatchIDs.size(); m++) {
					int mid = qMatchIDs.get(m);
					if ((dataTile == null) || !dataTile.containsRecord(mid))
						dataTile = this.getDataTileForId(mid);
					if (dataTile.isValidTaxon(mid))
						qValidMatchIDs.add(mid);
				}
				qValidMatchIDs.finishResult();
				if (qValidMatchIDs.size() == 0)
					return null;
				qMatchIDs = qValidMatchIDs;
			}
			matchIDs = qMatchIDs.toArray();
		}
		
		//	assemble and return results
		TaxonRecord[] results = new TaxonRecord[matchIDs.length];
		DataTile dataTile = null;
		for (int m = 0; m < matchIDs.length; m++) {
			if ((dataTile == null) || !dataTile.containsRecord(matchIDs[m]))
				dataTile = this.getDataTileForId(matchIDs[m]);
			results[m] = dataTile.getRecord(matchIDs[m]);
		}
		return results;
	}
	
	/** strict stale taxon mode for multi-nomial match */
	public static final char STALE_TAXON_MODE_STRICT = 'S';
	
	/** intermediate stale taxon mode for multi-nomial match */
	public static final char STALE_TAXON_MODE_INTERMEDIATE = 'I';
	
	/** lenient stale taxon mode for multi-nomial match */
	public static final char STALE_TAXON_MODE_LENIENT = 'L';
	
	/**
	 * Check if a taxon record and its parents up to the genus match the
	 * epithets of a multi-nomial. The first epithet in the argument array
	 * is expected to be the genus. If the argument taxon record is a synonym
	 * and has its original parent epithets as a string (because they differ
	 * from the parents of the linked valid taxon), this method matches against
	 * that string; otherwise, it matches against the parent records of the
	 * argument taxon record. In the latter case, the
	 * <code>staleRecordMode</code> controls how to handle taxon records that
	 * occur in the path from the argument taxon record (or its linked valid
	 * taxon) up to the genus in between two other taxon records matching
	 * adjacent epithets: <code>S</code> indicates strict mode, i.e. all taxon
	 * records in the path must match an epithet in the argument array;
	 * <code>I</code> indicates tolerance for unmatched intermediate-rank taxon
	 * records, e.g. a subgenus present in the taxon record tree, but not in
	 * the argument epithet array; <code>L</code> indicates lenient mode, i.e.
	 * any unmatched taxon record will simply be ignored.
	 * @param tr the taxon record to match
	 * @param epithets the epithets to match against
	 * @param fromEpithet the index of the first epithet in the array to match
	 *            (inclusive)
	 * @param staleTaxonMode how to handle taxon records that occur in the
	 *            path to the genus in between two other taxon records matching
	 *            adjacent epithets?
	 * @param caseSensitive match case sensitive?
	 * @return true if the argument taxon record matches the (indicated portion
	 *            of) argument array of epithets
	 */
	public static boolean matchesEpithetPrefix(TaxonRecord tr, String[] epithets, int fromEpithet, char staleTaxonMode, boolean caseSensitive) {
		return matchesEpithetPrefix(tr, epithets, fromEpithet, epithets.length, staleTaxonMode, false, caseSensitive);
	}
	
	/**
	 * Check if a taxon record and its parents up to the genus match the
	 * epithets of a multi-nomial. The first epithet in the argument array
	 * is expected to be the genus. If the argument taxon record is a synonym
	 * and has its original parent epithets as a string (because they differ
	 * from the parents of the linked valid taxon), this method matches against
	 * that string; otherwise, it matches against the parent records of the
	 * argument taxon record. In the latter case, the
	 * <code>staleRecordMode</code> controls how to handle taxon records that
	 * occur in the path from the argument taxon record (or its linked valid
	 * taxon) up to the genus in between two other taxon records matching
	 * adjacent epithets: <code>S</code> indicates strict mode, i.e. all taxon
	 * records in the path must match an epithet in the argument array;
	 * <code>I</code> indicates tolerance for unmatched intermediate-rank taxon
	 * records, e.g. a subgenus present in the taxon record tree, but not in
	 * the argument epithet array; <code>L</code> indicates lenient mode, i.e.
	 * any unmatched taxon record will simply be ignored.
	 * @param tr the taxon record to match
	 * @param epithets the epithets to match against
	 * @param fromEpithet the index of the first epithet in the array to match
	 *            (inclusive)
	 * @param staleTaxonMode how to handle taxon records that occur in the
	 *            path to the genus in between two other taxon records matching
	 *            adjacent epithets?
	 * @param prefixMatch allow prefix matches (false means exact match)
	 * @param caseSensitive match case sensitive?
	 * @return true if the argument taxon record matches the (indicated portion
	 *            of) argument array of epithets
	 */
	public static boolean matchesEpithetPrefix(TaxonRecord tr, String[] epithets, int fromEpithet, char staleTaxonMode, boolean prefixMatch, boolean caseSensitive) {
		return matchesEpithetPrefix(tr, epithets, fromEpithet, epithets.length, staleTaxonMode, caseSensitive);
	}
	
	/**
	 * Check if a taxon record and its parents up to the genus match the
	 * epithets of a multi-nomial. The first epithet in the argument array
	 * is expected to be the genus. If the argument taxon record is a synonym
	 * and has its original parent epithets as a string (because they differ
	 * from the parents of the linked valid taxon), this method matches against
	 * that string; otherwise, it matches against the parent records of the
	 * argument taxon record. In the latter case, the
	 * <code>staleRecordMode</code> controls how to handle taxon records that
	 * occur in the path from the argument taxon record (or its linked valid
	 * taxon) up to the genus in between two other taxon records matching
	 * adjacent epithets: <code>S</code> indicates strict mode, i.e. all taxon
	 * records in the path must match an epithet in the argument array;
	 * <code>I</code> indicates tolerance for unmatched intermediate-rank taxon
	 * records, e.g. a subgenus present in the taxon record tree, but not in
	 * the argument epithet array; <code>L</code> indicates lenient mode, i.e.
	 * any unmatched taxon record will simply be ignored.
	 * @param tr the taxon record to match
	 * @param epithets the epithets to match against
	 * @param fromEpithet the index of the first epithet in the array to match
	 *            (inclusive)
	 * @param toEpithet the index of the last epithet in the array to match
	 *            (exclusive)
	 * @param staleTaxonMode how to handle taxon records that occur in the
	 *            path to the genus in between two other taxon records matching
	 *            adjacent epithets?
	 * @param caseSensitive match case sensitive?
	 * @return true if the argument taxon record matches the (indicated portion
	 *            of) argument array of epithets
	 */
	public static boolean matchesEpithetPrefix(TaxonRecord tr, String[] epithets, int fromEpithet, int toEpithet, char staleTaxonMode, boolean caseSensitive) {
		return matchesEpithetPrefix(tr, epithets, fromEpithet, toEpithet, staleTaxonMode, false, caseSensitive);
	}
	
	/**
	 * Check if a taxon record and its parents up to the genus match the
	 * epithets of a multi-nomial. The first epithet in the argument array
	 * is expected to be the genus. If the argument taxon record is a synonym
	 * and has its original parent epithets as a string (because they differ
	 * from the parents of the linked valid taxon), this method matches against
	 * that string; otherwise, it matches against the parent records of the
	 * argument taxon record. In the latter case, the
	 * <code>staleRecordMode</code> controls how to handle taxon records that
	 * occur in the path from the argument taxon record (or its linked valid
	 * taxon) up to the genus in between two other taxon records matching
	 * adjacent epithets: <code>S</code> indicates strict mode, i.e. all taxon
	 * records in the path must match an epithet in the argument array;
	 * <code>I</code> indicates tolerance for unmatched intermediate-rank taxon
	 * records, e.g. a subgenus present in the taxon record tree, but not in
	 * the argument epithet array; <code>L</code> indicates lenient mode, i.e.
	 * any unmatched taxon record will simply be ignored.
	 * @param tr the taxon record to match
	 * @param epithets the epithets to match against
	 * @param fromEpithet the index of the first epithet in the array to match
	 *            (inclusive)
	 * @param toEpithet the index of the last epithet in the array to match
	 *            (exclusive)
	 * @param staleTaxonMode how to handle taxon records that occur in the
	 *            path to the genus in between two other taxon records matching
	 *            adjacent epithets?
	 * @param prefixMatch allow prefix matches (false means exact match)
	 * @param caseSensitive match case sensitive?
	 * @return true if the argument taxon record matches the (indicated portion
	 *            of) argument array of epithets
	 */
	public static boolean matchesEpithetPrefix(TaxonRecord tr, String[] epithets, int fromEpithet, int toEpithet, char staleTaxonMode, boolean prefixMatch, boolean caseSensitive) {
		
		//	only checking beyond end of epithets
		if (epithets.length <= fromEpithet)
			return true;
		else if (fromEpithet < 0)
			fromEpithet = 0;
		
		//	any range left to check?
		if (toEpithet <= fromEpithet)
			return true;
		
		//	match most significant epithet against taxon record proper
		if (toEpithet >= epithets.length) {
//			String trEpithet = tr.getEpithet();
//			if (epithets[epithets.length - 1].equals(trEpithet))
//				toEpithet = (epithets.length - 1);
//			else if (!caseSensitive && epithets[epithets.length - 1].equalsIgnoreCase(trEpithet))
//				toEpithet = (epithets.length - 1);
//			else return false;
			if (matches(tr.getEpithet(), epithets[epithets.length - 1], prefixMatch, caseSensitive))
				toEpithet = (epithets.length - 1);
			else return false;
		}
		
		//	any range left to check?
		if (toEpithet <= fromEpithet)
			return true;
		
		//	prepare matching other epithets against original prefix or against taxon records on path up to genus
		int pTrId;
		DataTile tile = tr.tile;
		String originalParent = tr.getOriginalParentEpithets();
		if (originalParent != null) {
			originalParent = originalParent.replaceAll("[\\(\\)]", " "); // remove prentheses from subgenus, section, etc.
			pTrId = -1;
			if (!caseSensitive) /* saves us repeated case conversions below */ {
				originalParent = originalParent.toLowerCase();
				String[] lEpithets = new String[epithets.length];
				for (int e = 0; e < epithets.length; e++)
					lEpithets[e] = epithets[e].toLowerCase();
				epithets = lEpithets;
			}
		}
		else if (tr.isValidTaxon())
			pTrId = tile.getParentId(tr.getId());
		else {
			int vTrId = tile.getValidTaxonId(tr.getId());
			if (!tile.containsRecord(vTrId))
				tile = tile.col.getDataTileForId(vTrId);
			if (tile == null)
				return false; // invalid valid ID, we must have run out of the tree (e.g. getting parent of kingdom) ...
			pTrId = tile.getParentId(vTrId);
		}
		boolean needSpeciesMatch = ((staleTaxonMode != STALE_TAXON_MODE_LENIENT) && (speciesRankLevel < tr.getRankByte()));
		for (int e = (toEpithet - 1); e >= fromEpithet; e--) {
			
			//	check against taxon record path up to genus
			if (originalParent == null) {
				if (!tile.containsRecord(pTrId))
					tile = tile.col.getDataTileForId(pTrId);
				if (tile == null)
					return false; // we must have run out of the tree (e.g. getting parent of kingdom) ...
				
				//	match current epithet against current taxon record
				String trEpithet = tile.getEpithet(pTrId);
//				if (caseSensitive ? trEpithet.equalsIgnoreCase(epithets[e]) : trEpithet.equals(epithets[e])) /* match */ {
//					if (tile.getRankByte(pTrId) == speciesRankLevel)
//						needSpeciesMatch = false;
//				}
				if (matches(trEpithet, epithets[e], prefixMatch, caseSensitive)) /* match */ {
					if (tile.getRankByte(pTrId) == speciesRankLevel)
						needSpeciesMatch = false;
				}
				else if (staleTaxonMode == STALE_TAXON_MODE_STRICT) // cannot ignore mismatch in strict mode
					return false;
				else if ((staleTaxonMode == STALE_TAXON_MODE_INTERMEDIATE) && tile.isPrimaryRank(pTrId)) // cannot ignore primary-rank mismatch in intermediate mode
					return false;
				else if (tile.getRankByte(pTrId) <= genusRankLevel) // we're at or beyond the genus already with epithets left
					return false;
				else e++; // counter loop decement to test current epithet against next taxon record up the path
				
				//	switch to next taxon record up the path
				pTrId = tile.getParentId(pTrId);
			}
			
			//	check against remaining original parent
			else {
				
				//	match current epithet against current end of original prefix
				if (originalParent.equals(epithets[e]))
					originalParent = ""; // expended it all, no need for truncating
				else if (prefixMatch && originalParent.startsWith(epithets[e])) // we have a match somewhere further to the left, truncate matched epithet and what comes after it
					originalParent = ""; // expended it all, no need for truncating
				else if (originalParent.endsWith(" " + epithets[e])) // we have a suffix match, truncate matched epithet
					originalParent = originalParent.substring(0, (originalParent.length() - " ".length() - epithets[e].length())).trim();
				else if (staleTaxonMode == STALE_TAXON_MODE_STRICT) // cannot ignore mismatch in strict mode
					return false;
				else if (originalParent.lastIndexOf(" " + epithets[e] + " ") != -1) // we have a match somewhere further to the left, truncate matched epithet and what comes after it
					originalParent = originalParent.substring(0, originalParent.lastIndexOf(" " + epithets[e] + " ")).trim();
				else if (prefixMatch && (originalParent.lastIndexOf(" " + epithets[e]) != -1)) // we have a match somewhere further to the left, truncate matched epithet and what comes after it
					originalParent = originalParent.substring(0, originalParent.lastIndexOf(" " + epithets[e])).trim();
				else if (originalParent.startsWith(epithets[e] + " ")) {// we have a match at the start, truncate matched epithet and what comes after it
					if (needSpeciesMatch || (e != 0)) // last possible match right at start, has to be genus for match
						return false;
					else originalParent = ""; // expended it all, no need for truncating
				}
				else return false;
				
				//	truncate any lables that might remain from below-species intermediate-rank epithets
				if (originalParent.endsWith(" subsp."))
					originalParent = originalParent.substring(0, (originalParent.length() - " subsp.".length())).trim();
				else if (originalParent.endsWith(" var."))
					originalParent = originalParent.substring(0, (originalParent.length() - " var.".length())).trim();
				else if (originalParent.endsWith(" subvar."))
					originalParent = originalParent.substring(0, (originalParent.length() - " subvar.".length())).trim();
				else if (originalParent.endsWith(" f."))
					originalParent = originalParent.substring(0, (originalParent.length() - " f.".length())).trim();
				else if (originalParent.endsWith(" subf."))
					originalParent = originalParent.substring(0, (originalParent.length() - " subf.".length())).trim();
				
				//	truncate any lables that might remain from above-species intermediate-rank epithets
				else if (originalParent.endsWith(" subg."))
					originalParent = originalParent.substring(0, (originalParent.length() - " subg.".length())).trim();
				else if (originalParent.endsWith(" subgen."))
					originalParent = originalParent.substring(0, (originalParent.length() - " subgen.".length())).trim();
				else if (originalParent.endsWith(" sect."))
					originalParent = originalParent.substring(0, (originalParent.length() - " sect.".length())).trim();
				else if (originalParent.endsWith(" subsect."))
					originalParent = originalParent.substring(0, (originalParent.length() - " subsect.".length())).trim();
				else if (originalParent.endsWith(" ser."))
					originalParent = originalParent.substring(0, (originalParent.length() - " ser.".length())).trim();
				else if (originalParent.endsWith(" subser."))
					originalParent = originalParent.substring(0, (originalParent.length() - " subser.".length())).trim();
				
				//	no label remaining, must have matched species, unless we're on first (genus) epithet already
				else if (needSpeciesMatch && (e != 0) && (originalParent.length() != 0) && epithets[e].equals(epithets[e].toLowerCase()))
					needSpeciesMatch = false;
			}
		}
		
		//	we went all the way to the second or first epithet without finding the species we needed
		if ((fromEpithet < 2) && needSpeciesMatch)
			return false;
		
		//	got all we needed, or didn't get far enough to the left to know for sure if anything is missing
		return true;
	}
	private static boolean matches(String str, String query, boolean prefixMatch, boolean caseSensitive) {
		if (query.equals(str))
			return true;
		if (!caseSensitive && query.equalsIgnoreCase(str))
			return true;
		if (prefixMatch && str.startsWith(query))
			return true;
		if (prefixMatch && !caseSensitive && str.toLowerCase().startsWith(query.toLowerCase()))
			return true;
		return false;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
//		int plain = 123456;
//		String base29 = "7DTMQ";//encodeIntBase29(plain);
////		System.out.println(plain + " ==> " + base29);
//		plain = parseIntBase29(base29);
//		System.out.println(base29 + " ==> " + plain);
//		System.out.println(plain + " ==> " + encodeIntBase29(plain));
//		if (true)
//			return;
		
//		File higher = new File("E:/Projektdaten/CoL2021/data.higher.txt");
//		InputStream dataIn = new BufferedInputStream(new FileInputStream(higher));
//		DataTile dataTile = loadDataTile(dataIn, 1, 227500, null);
//		
//		File indexA = new File("E:/Projektdaten/CoL2021/index.a.txt");
//		InputStream indexIn = new BufferedInputStream(new FileInputStream(indexA));
//		IndexTile indexTile = loadIndexTile(indexIn, "a".getBytes(), "azzzzz".getBytes(), null);
//		
//		AnalyzerDataProvider dataProvider = new AnalyzerDataProviderFileBased(new File("E:/Projektdaten/CoL2021/Data2021"));
		AnalyzerDataProvider dataProvider = new AnalyzerDataProviderFileBased(new File("E:/Projektdaten/CoL2021"));
//		long maxMem = Runtime.getRuntime().maxMemory();
//		long freeMem = Runtime.getRuntime().freeMemory();
//		System.out.println("Max memory is " + maxMem + ", " + freeMem + " free" + ", " + (maxMem - freeMem) + " used");
//		System.gc();
//		freeMem = Runtime.getRuntime().freeMemory();
//		System.out.println("Max memory is " + maxMem + ", " + freeMem + " free" + ", " + (maxMem - freeMem) + " used");
		CatalogOfLifeLocal col = new CatalogOfLifeLocal(dataProvider);
//		for (int t = 0; t < col.speciesTiles.length; t++) {
//			System.out.println("Loading " + col.speciesTiles[t].fileName);
//			col.speciesTiles[t].getTile();
//		}
//		freeMem = Runtime.getRuntime().freeMemory();
//		System.out.println("Max memory is " + maxMem + ", " + freeMem + " free" + ", " + (maxMem - freeMem) + " used");
//		System.gc();
//		freeMem = Runtime.getRuntime().freeMemory();
//		System.out.println("Max memory is " + maxMem + ", " + freeMem + " free" + ", " + (maxMem - freeMem) + " used");
//		for (int t = 0; t < col.indexTiles.length; t++) {
//			System.out.println("Loading " + col.indexTiles[t].fileName);
//			col.indexTiles[t].getTile();
//		}
//		freeMem = Runtime.getRuntime().freeMemory();
//		System.out.println("Max memory is " + maxMem + ", " + freeMem + " free" + ", " + (maxMem - freeMem) + " used");
//		System.gc();
//		freeMem = Runtime.getRuntime().freeMemory();
//		System.out.println("Max memory is " + maxMem + ", " + freeMem + " free" + ", " + (maxMem - freeMem) + " used");
		
		DataTile dataTile = col.higherTile;
		String epithet = "Orchis";
		col.setMaxSimultaneousIndexTiles(3);
		col.setMaxSimultaneousSpeciesTiles(3);
//		int id = tile.findRecordId(epithet.getBytes(), ((byte) -1));
		int id = dataTile.findRecordId(epithet.toLowerCase().getBytes(), ((byte) -1), false, false);
		System.out.println(epithet + " ==> " + id);
		System.out.println(dataTile.getHigherTaxonomy(id));
		System.out.println(Arrays.toString(dataTile.getChildIDs(id)));
		System.out.println(Arrays.toString(dataTile.getSynonymIDs(id)));
		TaxonRecord rootTr = dataTile.getRecord(id);
//		ArrayList trs = new ArrayList();
//		trs.add(rootTr);
//		for (int r = 0; r < trs.size(); r++) {
//			TaxonRecord tr = ((TaxonRecord) trs.get(r));
//			System.out.println(tr.getRank() + " " + tr.getEpithet() + " " + tr.getAuthority());
//			if (speciesRankLevel < encodeRank(tr.getRank()))
//				System.out.println(tr.getHigherTaxonomy());
//			TaxonRecord[] children = tr.getChildren();
//			if (children != null)
//				trs.addAll(Arrays.asList(children));
//		}
//		TaxonRecord[] dMatches = rootTr.findDescendants("brancifortii", true, false, SPECIES_ATTRIBUTE, false);
////		TaxonRecord[] dMatches = col.findTaxonRecords("Pheidole", null, true, false, true);
////		TaxonRecord[] dMatches = col.findTaxonRecords("PHEIDOLE", FAMILY_ATTRIBUTE, null, false, false, true);
//		for (int m = 0; m < dMatches.length; m++)
//			System.out.println(dMatches[m].getRank() + " " + dMatches[m].getNameString() + " " + dMatches[m].getAuthority());
		
		TaxonRecord[] familyTrs = col.findTaxonRecords("Formicidae", FAMILY_ATTRIBUTE);
		LinkedHashSet colIDs = new LinkedHashSet();
		for (int f = 0; f < familyTrs.length; f++) {
//			System.out.println(familyTrs[f].getEpithet() + " " + familyTrs[f].getHigherTaxonomy());
			System.out.println(familyTrs[f].getId() + "/" + familyTrs[f].getOriginalColId() + ": " + familyTrs[f].getEpithet() + " " + familyTrs[f].getHigherTaxonomy());
			colIDs.add(familyTrs[f].getOriginalColId());
			TaxonRecord[] genusTrs = familyTrs[f].getPrimaryChildren(false);
			for (int g = 0; g < genusTrs.length; g++) {
//				System.out.println("  " + genusTrs[g].getEpithet() + " " + (genusTrs[g].isValidTaxon() ? "[valid]" : "[synonym of " + genusTrs[g].getValidTaxon().getNameString(false) + "]"));
				System.out.println("  " + genusTrs[g].getId() + "/" + genusTrs[g].getOriginalColId() + ": " + genusTrs[g].getEpithet() + " " + (genusTrs[g].isValidTaxon() ? "[valid]" : "[synonym of " + genusTrs[g].getValidTaxon().getNameString(false) + "]"));
				TaxonRecord[] speciesTrs = genusTrs[g].getPrimaryChildren(false);
				colIDs.add(genusTrs[g].getOriginalColId());
				for (int s = 0; s < speciesTrs.length; s++) {
					System.out.println("    " + speciesTrs[s].getId() + "/" + speciesTrs[s].getOriginalColId() + ": " + speciesTrs[s].getEpithet() + " " + (speciesTrs[s].isValidTaxon() ? "[valid]" : "[synonym of " + speciesTrs[s].getValidTaxon().getNameString(false) + "]") + ", " + speciesTrs[s].getNameString(false));
					colIDs.add(speciesTrs[s].getOriginalColId());
				}
			}
		}
		colIDs.remove(null);
		ArrayList lookupResults = new ArrayList(colIDs.size());
		long lookupStart = System.currentTimeMillis();
		for (Iterator idit = colIDs.iterator(); idit.hasNext();) {
			String colId = ((String) idit.next());
			TaxonRecord tr = col.getTaxonRecord(colId);
//			if (tr == null)
//				System.out.println(colId + " ==> " + null);
//			else System.out.println(colId + " ==> " + tr.getId() + "/" + tr.getOriginalColId() + ": " + tr.getEpithet() + " " + (tr.isValidTaxon() ? "[valid]" : "[synonym of " + tr.getValidTaxon().getNameString(false) + "]") + ", " + tr.getNameString(false));
			if (tr == null)
				lookupResults.add(colId + " ==> " + null);
			else lookupResults.add(colId + " ==> " + tr.getId() + "/" + tr.getOriginalColId() + ": " + tr.getEpithet() + " " + (tr.isValidTaxon() ? "[valid]" : "[synonym of " + tr.getValidTaxon().getNameString(false) + "]") + ", " + tr.getNameString(false));
		}
		System.out.println(colIDs.size() + " lookups done in " + (System.currentTimeMillis() - lookupStart) + "ms");
		for (int r = 0; r < lookupResults.size(); r++)
			System.out.println(lookupResults.get(r));
		
//		String query = "Formicinae";
//		String lQuery = query.toLowerCase();
//		byte[] str = getQueryBytes(lQuery);
//		IndexTile indexTile = col.getIndexTileForString(str);
//		IndexEntry[] matches = indexTile.findMatches(str, false);
//		for (int m = 0; m < matches.length; m++) {
//			System.out.println(matches[m].getEntryString() + ":");
//			TaxonRecord[] lc = matches[m].getLowerCaseMatches();
//			System.out.println(" ==> " + Arrays.toString(lc));
//			TaxonRecord[] cp = matches[m].getCapitalizedMatches();
//			System.out.println(" ==> " + Arrays.toString(cp));
//		}
	}
}
