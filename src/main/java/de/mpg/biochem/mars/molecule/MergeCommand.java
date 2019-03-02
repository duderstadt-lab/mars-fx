/*******************************************************************************
 * Copyright (C) 2019, Karl Duderstadt
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package de.mpg.biochem.mars.molecule;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.log.LogService;
import org.scijava.menu.MenuConstants;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.format.DataFormatDetector;
import com.fasterxml.jackson.core.format.DataFormatMatcher;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;

import de.mpg.biochem.mars.util.LogBuilder;

@Plugin(type = Command.class, label = "Merge Archives", menu = {
		@Menu(label = MenuConstants.PLUGINS_LABEL, weight = MenuConstants.PLUGINS_WEIGHT,
				mnemonic = MenuConstants.PLUGINS_MNEMONIC),
		@Menu(label = "MoleculeArchive Suite", weight = MenuConstants.PLUGINS_WEIGHT,
			mnemonic = 's'),
		@Menu(label = "Molecule Utils", weight = 1,
			mnemonic = 'm'),
		@Menu(label = "Merge Archives", weight = 90, mnemonic = 'm')})
public class MergeCommand extends DynamicCommand {
	@Parameter
	private LogService logService;
	
    @Parameter
    private StatusService statusService;
	
	@Parameter
    private MoleculeArchiveService moleculeArchiveService;
	
	@Parameter
    private UIService uiService;
	
	@Parameter(label="Directory", style="directory")
    private File directory;
	
	@Parameter(label="Use smile encoding")
	private boolean smileEncoding = true;
	
	ArrayList<MoleculeArchiveProperties> allArchiveProps;
	ArrayList<ArrayList<MARSImageMetaData>> allMetaDataItems;
	
	ArrayList<String> metaUIDs;
	
	@Override
	public void run() {				
		LogBuilder builder = new LogBuilder();
		
		String log = builder.buildTitleBlock("Merge Archives");
		builder.addParameter("Directory", directory.getAbsolutePath());
		builder.addParameter("Use smile encoding", String.valueOf(smileEncoding));
		log += builder.buildParameterList();
		logService.info(log);
		
		 // create new filename filter
        FilenameFilter fileNameFilter = new FilenameFilter() {
  
           @Override
           public boolean accept(File dir, String name) {
              if(name.lastIndexOf('.')>0) {
              
                 // get last index for '.' char
                 int lastIndex = name.lastIndexOf('.');
                 
                 // get extension
                 String str = name.substring(lastIndex);
                 
                 // match path name extension
                 if(str.equals(".yama")) {
                    return true;
                 }
              }
              
              return false;
           }
        };
		
		File[] archiveFileList = directory.listFiles(fileNameFilter);
		if (archiveFileList.length > 0) {
			allArchiveProps = new ArrayList<MoleculeArchiveProperties>();
			allMetaDataItems = new ArrayList<ArrayList<MARSImageMetaData>>();
			metaUIDs = new ArrayList<String>();
			
			for (File file: archiveFileList) {
				try {
					loadArchiveDetails(file);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			//Check for duplicate ImageMetaData items
			for (ArrayList<MARSImageMetaData> archiveMetaList : allMetaDataItems) {
				for (MARSImageMetaData metaItem : archiveMetaList) {
					String metaUID = metaItem.getUID();
					if (metaUIDs.contains(metaUID)) {
						logService.info("Duplicate ImageMetaData record " + metaUID + " found.");
						logService.info("Are you trying to merge copies of the same dataset?");
						logService.info("Please resolve the conflict and run the merge command again.");
						logService.info(builder.endBlock(false));
						return;
					} else {
						metaUIDs.add(metaUID);
					}
				}
			}
		
			//No conflicts found so we start building and writing the merged file
			//First we need to build the global MoleculeArchiveProperties
			int numMolecules = 0; 
			int numImageMetaData = 0;
			String globalComments = "";
			int count = 0;
			for (MoleculeArchiveProperties archiveProperties : allArchiveProps) {
				numMolecules += archiveProperties.getNumberOfMolecules();
				numImageMetaData += archiveProperties.getNumImageMetaData();
				globalComments += "Comments from Merged Archive " + archiveFileList[count].getName() + ":\n" + archiveProperties.getComments() + "\n";
				count++;
			}
				
			MoleculeArchiveProperties newArchiveProperties = new MoleculeArchiveProperties();
			newArchiveProperties.setNumberOfMolecules(numMolecules);
			newArchiveProperties.setNumImageMetaData(numImageMetaData);
			newArchiveProperties.setComments(globalComments);
			
			//Now we just need to write the file starting with the new MoleculeArchiveProperties
			File fileOUT = new File(directory.getAbsolutePath() + "/merged.yama");
			try {
				OutputStream stream = new BufferedOutputStream(new FileOutputStream(fileOUT));
				
				JsonGenerator jGenerator;
				if (smileEncoding) {
					SmileFactory jfactory = new SmileFactory();
					jGenerator = jfactory.createGenerator(stream);
				} else {
					JsonFactory jfactory = new JsonFactory();
					jGenerator = jfactory.createGenerator(stream, JsonEncoding.UTF8);
				}
				
				//We have to have a starting { for the json...
				jGenerator.writeStartObject();
				
				newArchiveProperties.toJSON(jGenerator);
				
				jGenerator.writeArrayFieldStart("ImageMetaData");
				for (ArrayList<MARSImageMetaData> archiveMetaList : allMetaDataItems) {
					for (MARSImageMetaData metaItem : archiveMetaList) {
						metaItem.toJSON(jGenerator);
					}
				}	
				jGenerator.writeEndArray();
				
				//Now we need to loop through all molecules in all archives and save them to the merged archive.
				jGenerator.writeArrayFieldStart("Molecules");
				for (File file: archiveFileList) {
					try {
						mergeMolecules(file, jGenerator);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				jGenerator.writeEndArray();
				
				//Now we need to add the corresponding global closing bracket } for the json format...
				jGenerator.writeEndObject();
				jGenerator.close();
				
				//flush and close streams...
				stream.flush();
				stream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
			logService.info("Merged " + archiveFileList.length + " yama files into the output archive merged.yama");
			logService.info("In total " + newArchiveProperties.getNumImageMetaData() + " Datasets were merged.");
			logService.info("In total " + newArchiveProperties.getNumberOfMolecules() + " molecules were merged.");
			logService.info(builder.endBlock(true));
		} else {
			logService.info("No .yama files in this directory.");
			logService.info(builder.endBlock(false));
		}
	}
	
	public void loadArchiveDetails(File file) throws JsonParseException, IOException {
		//First load MoleculeArchiveProperties
		InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
		
		//Here we automatically detect the format of the JSON file
		//Can be JSON text or Smile encoded binary file...
		JsonFactory jsonF = new JsonFactory();
		SmileFactory smileF = new SmileFactory(); 
		DataFormatDetector det = new DataFormatDetector(new JsonFactory[] { jsonF, smileF });
	    DataFormatMatcher match = det.findFormat(inputStream);
	    JsonParser jParser = match.createParserWithMatch();
		
		jParser.nextToken();
		jParser.nextToken();
		if ("MoleculeArchiveProperties".equals(jParser.getCurrentName())) {
			allArchiveProps.add(new MoleculeArchiveProperties(jParser, null));
		} else {
			logService.info("The file " + file.getName() + " have to MoleculeArchiveProperties. Is this a proper yama file?");
			return;
		}
		
		ArrayList<MARSImageMetaData> metaArchiveList = new ArrayList<MARSImageMetaData>();
		
		//Next load ImageMetaData items
		while (jParser.nextToken() != JsonToken.END_OBJECT) {
			String fieldName = jParser.getCurrentName();
			if ("ImageMetaData".equals(fieldName)) {
				while (jParser.nextToken() != JsonToken.END_ARRAY) {
					metaArchiveList.add(new MARSImageMetaData(jParser));
				}
			}
			
			if ("Molecules".equals(fieldName)) {
				allMetaDataItems.add(metaArchiveList);
				//We first have to check all ImageMetaData items to ensure there are no duplicates...
				jParser.close();
				inputStream.close();
				return;
			}
		}
	}
	
	//Method which takes all molecule records from a single archive and adds them together by directly streaming 
	//them to the merged.yama file through jGenerator...
	public void mergeMolecules(File file, JsonGenerator jGenerator) throws JsonParseException, IOException {
		//First load MoleculeArchiveProperties
		InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
		
		//Here we automatically detect the format of the JSON file
		//Can be JSON text or Smile encoded binary file...
		JsonFactory jsonF = new JsonFactory();
		SmileFactory smileF = new SmileFactory(); 
		DataFormatDetector det = new DataFormatDetector(new JsonFactory[] { jsonF, smileF });
	    DataFormatMatcher match = det.findFormat(inputStream);
	    JsonParser jParser = match.createParserWithMatch();
		
	    //We need to parse all the way to the molecule records since we already added everything else...
	    //For the moment I just parse again all the first parts and do nothing with them
	    //I guess it would be better to save an index of all the open files..
		jParser.nextToken();
		jParser.nextToken();
		if ("MoleculeArchiveProperties".equals(jParser.getCurrentName())) {
			new MoleculeArchiveProperties(jParser, null);
		}
		
		//Next load ImageMetaData items
		while (jParser.nextToken() != JsonToken.END_OBJECT) {
			String fieldName = jParser.getCurrentName();
			if ("ImageMetaData".equals(fieldName)) {
				while (jParser.nextToken() != JsonToken.END_ARRAY) {
					new MARSImageMetaData(jParser);
				}
			}
			
			if ("Molecules".equals(fieldName)) {
				while (jParser.nextToken() != JsonToken.END_ARRAY) {
					//Read in molecule record
					Molecule molecule = new Molecule(jParser);
					
					//write out molecule record
					molecule.toJSON(jGenerator);
				}
			}
		}
		jParser.close();
		inputStream.close();
	}
	
	//Getters and Setters
	public void setDirectory(String dir) {
		directory = new File(dir);
	}
	
	public void setDirectory(File directory) {
		this.directory = directory;
	}
	
	public File getDirectory() {
		return directory;
	}
	
	public void setSmileEncoding(boolean smileEncoding) {
		this.smileEncoding = smileEncoding;
	}
	
	public boolean getSmileEncoding() {
		return smileEncoding;
	}
}