/*
 * Copyright 2018 IKS Gesellschaft fuer Informations- und Kommunikationssysteme mbH
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.iksgmbh.sysnat.helper.docval;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.iksgmbh.sysnat.helper.docval.domain.DocumentCompareIgnoreConfig;


/**
 * Compares two directories and finds PDF-files that correspond to each other by name.
 * Corresponding PDFs are compared with each other line by line using the {@link DocumentComparer}.
 * The IGNORE_CONFIG can be configurated to define lines to be excluded from this comparison.
 * 
 * @author Reik Oberrath
 */
public class PdfDirectoryComparer 
{
	private static final DocumentCompareIgnoreConfig IGNORE_CONFIG = buildIgnoreConfig();
	
	private static boolean allOK;

	public static DocumentCompareIgnoreConfig buildIgnoreConfig() {
		// configure the PdfCompareIgnoreConfig using its with-methods
		// in order to customize your comparison
		return new DocumentCompareIgnoreConfig();
	}

	// #################################################################
	//               P U B L I C    M E T H O D S
	// #################################################################
	

	public static List<String> doYourJob(String dir1, String dir2) 
	{
		System.out.println("Comparing directory " + dir1);
		System.out.println("     with directory " + dir2);
		final List<FilePair> filesToCompare = findFilesToCompare(dir1, dir2);
		final List<String> result = new ArrayList<String>();
		
		if (filesToCompare == null) {
			result.add("At least one directory does not exist!");
			return result;
		}
		
		if (filesToCompare.size() == 0) {
			System.out.println("There no files found to compare!");
			result.add("No matching files found in directories !");
			return result;
		}
		
		System.out.println("");
		System.out.println("There are " + filesToCompare.size() + " files to compare:");
		allOK = true;

		filesToCompare.forEach(filePair -> {
			try {
				//System.out.print(".");
				System.out.println(filePair.f1.getName());
				compare(filePair, result);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		
		System.out.println("");
		if (allOK) System.out.println("There are no relevant differences between the " + filesToCompare.size() + " PDF-files compared.");
		return result;
	}


	private static void compare(FilePair filePair, List<String> result) throws IOException 
	{
		DocumentComparer pdfComparer = new DocumentComparer(filePair.f1.getAbsolutePath());
		String differenceReport = pdfComparer.getDifferenceReport(filePair.f2.getAbsolutePath(), IGNORE_CONFIG);
		String comparisonResult = "Comparing " + filePair.f1.getName() + " with " + filePair.f2.getName() + ": "; 
		if ( differenceReport.isEmpty() ) {
			comparisonResult += " OK";
		} else {
			comparisonResult += System.getProperty("line.separator") + differenceReport;
			allOK = false;
		}
		result.add(comparisonResult);
	}
	

	private static List<FilePair> findFilesToCompare(String dir1, String dir2) 
	{
		final List<FilePair> comparisons = new ArrayList<>();  
		final File folder1 = new File(dir1);
		final File folder2 = new File(dir2);
		
		if ( ! folder1.exists() || ! folder2.exists()) {
			return null;
		}
		
		final File[] children1 = folder1.listFiles();
		final File[] children2 = folder2.listFiles();

		for (File child1 : children1) 
		{
			for (File child2 : children2) 
			{
				if (child1.getName().equals(child2.getName())) {
					if (child1.getName().toLowerCase().endsWith(".pdf")) {
						comparisons.add(new FilePair(child1, child2));
					}
					break;
				}
			}
		}
		
		return comparisons;
	}

	
	static class FilePair 
	{
		public File f1;
		public File f2;
		
		public FilePair(File f1, File f2) {
			this.f1 = f1;
			this.f2 = f2;
		}
	}
}
