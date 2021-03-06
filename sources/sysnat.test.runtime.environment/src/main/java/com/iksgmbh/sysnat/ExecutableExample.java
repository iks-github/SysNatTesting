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
package com.iksgmbh.sysnat;

import static com.iksgmbh.sysnat.common.utils.SysNatLocaleConstants.CATEGORY_BILDNACHWEIS;
import static com.iksgmbh.sysnat.common.utils.SysNatLocaleConstants.ERROR_KEYWORD;
import static com.iksgmbh.sysnat.common.utils.SysNatLocaleConstants.NO_KEYWORD;
import static com.iksgmbh.sysnat.common.utils.SysNatLocaleConstants.PICTURE_PROOF;
import static com.iksgmbh.sysnat.common.utils.SysNatLocaleConstants.YES_KEYWORD;
import static org.junit.Assert.fail;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;

import com.iksgmbh.sysnat.common.exception.SkipTestCaseException.SkipReason;
import com.iksgmbh.sysnat.common.exception.SysNatException;
import com.iksgmbh.sysnat.common.exception.SysNatTestDataException;
import com.iksgmbh.sysnat.common.exception.UnexpectedResultException;
import com.iksgmbh.sysnat.common.exception.UnsupportedGuiEventException;
import com.iksgmbh.sysnat.common.helper.ErrorPageLauncher;
import com.iksgmbh.sysnat.common.helper.FileFinder;
import com.iksgmbh.sysnat.common.helper.HtmlLauncher;
import com.iksgmbh.sysnat.common.utils.SysNatConstants;
import com.iksgmbh.sysnat.common.utils.SysNatConstants.ResultLaunchOption;
import com.iksgmbh.sysnat.common.utils.SysNatFileUtil;
import com.iksgmbh.sysnat.common.utils.SysNatStringUtil;
import com.iksgmbh.sysnat.domain.SysNatTestData;
import com.iksgmbh.sysnat.domain.TestApplication;
import com.iksgmbh.sysnat.guicontrol.GuiControl;
import com.iksgmbh.sysnat.guicontrol.SeleniumGuiController;
import com.iksgmbh.sysnat.helper.PopupHandler;
import com.iksgmbh.sysnat.helper.ReportCreator;
import com.iksgmbh.sysnat.helper.WindowHelper;
import com.iksgmbh.sysnat.language_templates.LanguageTemplateBasics;
import com.iksgmbh.sysnat.testdataimport.TableDataParser;
import com.iksgmbh.sysnat.testdataimport.TestDataImporter;
import com.iksgmbh.sysnat.testresult.archiving.SysNatTestResultArchiver;

/**
 * Mother of all test classes that stores context information about the currentlx executed test.
 * 
 * All technical but technologically unspecific stuff belongs in here.
 * 
 * It knows the GuiControl and is therefore used by the PageObjects to trigger gui events.
 * It is also used by the LanguageTemplateContainers, e.g. to access test data.
 * 
 * @author Reik Oberrath
 */
abstract public class ExecutableExample
{
	private static final ResourceBundle ERR_MSG_BUNDLE = ResourceBundle.getBundle("bundles/ErrorMessages", Locale.getDefault());

	public static final String SMILEY_FAILED = "&#x1F61E;";
	public static final String SMILEY_WRONG = "&#x1F612;";
	public static final String SMILEY_OK = "&#x1F60A;";
	
	private static final String YES = YES_KEYWORD + " " + SMILEY_OK;
	private static final String NO = NO_KEYWORD + " " + SMILEY_WRONG;

    protected ExecutionRuntimeInfo executionInfo = ExecutionRuntimeInfo.getInstance();
    protected DateTime startTime = new DateTime();
	protected SysNatTestData testDataSets = new SysNatTestData(); 	     // preexisting data imported from extern files
	protected HashMap<String, Object> testObjects = new HashMap<>();     // data created during test execution
    protected TestDataImporter testDataImporter;
    
    private List<String> executionFilterList;
    private String XXID = null; // unique id of the executable example
	private boolean skipped = false;
	private boolean alreadyTerminated = false;
	private GuiControl guiController;
	private DateTime startDate = DateTime.now();
	private List<String> reportMessages = new ArrayList<>();
	private String setScriptToExecute;
	private String bddKeyword = "";
	private String behaviourID;

	// abstract methods
    abstract public void executeTestCase();
    abstract public String getTestCaseFileName();
    abstract public Package getTestCasePackage();
    abstract public boolean doesTestBelongToApplicationUnderTest();
    
	public void setUp() 
	{
		if ( ! executionInfo.isTestEnvironmentInitialized()) 
		{
			boolean ok = initTestEnvironment();
			if ( ! ok ) {
				System.err.println("Test application could not be started or test environment failed to be initialized correctly.");
				System.exit(1);
			}
		} else {
			reinitTestEnvironment();
		}
	}
	
	private void reinitTestEnvironment() 
	{
		if (System.getProperty("isWebApplication", "false").equals("true"))
		{
			setGuiController(executionInfo.getGuiController());
	        getGuiController().reloadCurrentPage();  // prepare gui for next test
		}
	}
	
	private boolean initTestEnvironment() 
	{
		testDataImporter = new TestDataImporter(executionInfo.getTestdataDir());
		initShutDownHook();

		if (System.getProperty("isWebApplication", "false").equals("true"))
		{
			setGuiController(new SeleniumGuiController());
			executionInfo.setGuiController( getGuiController() );
			boolean applicationStarted = false;
			applicationStarted = login();
			if ( ! applicationStarted ) return false;
    		executionInfo.setApplicationStarted(applicationStarted);
			PopupHandler.setTestCase(this);
		} else {
			executionInfo.setApplicationStarted(true);
		}
		
		executionInfo.setTestEnvironmentInitialized();
		return true;
	}

	protected boolean isSkipped() {
		return skipped;
	}

	protected String getExecDuration() 
	{
		DateTime now = DateTime.now();
		long passedSeconds = (now.getMillis()-startDate.getMillis()) / 1000;
		long minutes = passedSeconds/60;
		long seconds = passedSeconds - (minutes * 60);
		return minutes + "min and " + seconds + "sec";
	}

	public void setReportMessages(List<String> messages) {
		reportMessages = messages;
	}

	public List<String> getReportMessages() {
		return reportMessages;
	}

	public void setXXID(String aXXID) {
		this.XXID = aXXID;
	}
	
	public String getXXID() {
		return XXID;
	}
    
	public void addReportMessage(String message) 
	{
		if (bddKeyword == null || bddKeyword.isEmpty()) {
			reportMessages.add(message.trim());  
		} else {
			// reportMessages.add(message.trim());  TODO Decide whether and how the BDD-Keyword enters the report
			reportMessages.add("<b>" + bddKeyword + "</b> " + message.trim()); 
		}
	}

	public void terminateTestCase(String message) 
	{
		if (! alreadyTerminated) {
			alreadyTerminated = true;
			fail( message );
		}
	}

	public String extractSelectorValue(String message) 
	{
		String marker = "\"selector\":\"";
		int pos = message.indexOf(marker);
		
		if (pos == -1) 
		{
			marker = "Das Element mit der technischen Identifizierung";
			pos = message.indexOf(marker);
			if (pos == -1) {
				return message;
			}
			String toReturn = message.substring(pos + marker.length());
			pos = toReturn.indexOf("konnte auf der aktuellen Seite nicht gefunden werden.");
			toReturn = toReturn.substring(0, pos);
			return toReturn;
		} 
		else 
		{
			String toReturn = message.substring(pos + marker.length());
			pos = toReturn.indexOf('\"');
			toReturn = toReturn.substring(0, pos);
			return toReturn;
			
		}
	}
	
    public void takeScreenshot(String filename)
    {
    	if ( ! filename.endsWith(".png")) {
    		filename += ".png";
    	}
    	
        File scrFile = null;
        try {
            scrFile = getGuiController().takeScreenShot();
        } catch (Exception e) {
            System.out.println("Error taking screenshot: " + e.getMessage());
            return;
        }

        try {
            String screenshotFileName = executionInfo.getReportFolder() 
            		                    + File.separator + XXID
            		                    + File.separator + filename;
			FileUtils.copyFile(scrFile, new File(screenshotFileName));
        } catch (Exception e) {
        	System.out.println("Error saving screenshot: " + e.getMessage());
        } finally {
            if (scrFile != null) {
                try {
					FileUtils.forceDelete(scrFile);
				} catch (IOException e) {
					e.printStackTrace();
				}
            }
        }
    }
	
	public void closeCurrentTestCaseWithSuccess() 
	{
		if (reportMessages != null)  {
			System.out.println("Test Result: OK");
			executionInfo.addTestMessagesOK(getCheckedXXId(), reportMessages, behaviourID);
		}
	}

	public void terminateWrongTestCase() {
		System.out.println("Test Result: Failed Assertion");
		executionInfo.addTestMessagesWRONG(getCheckedXXId(), reportMessages, behaviourID);
	}

	public void finishSkippedTestCase(SkipReason skipReason) 
	{
		skipped = true;
		XXID = null;
		reportMessages.clear();
		if (skipReason == SkipReason.ACTIVATION_STATE) {
			System.out.println("Skipped because set inactive.");
		}
		
		if (skipReason == SkipReason.EXECUTION_FILTER) {
			System.out.println("Skipped due to current execution filter(s) " + executionInfo.getExecutionFilterList());
		}
		
		if (skipReason == SkipReason.APPLICATION_TO_TEST) {
			System.out.println("Skipped due to current test application: " + executionInfo.getTestApplicationName());
		}	
	}
	
	public void failWithMessage(String message) 
	{
		if (executionInfo.isXXIdAlreadyUsed(getCheckedXXId())) {
			return;
		}
		if ( ! message.contains(ERROR_KEYWORD) )  {
			message = ERROR_KEYWORD + ": " + message;
		}
		System.out.println("Test Result: Technical Error");
		reportMessages.add(message);
		executionInfo.addTestMessagesFAILED(getCheckedXXId(), reportMessages, behaviourID);
		terminateTestCase(message);
	}

	
	public LanguageTemplateBasics getApplicationSpecificLanguageTemplates() 
    {
    	final String applicationUnderTest = executionInfo.getTestApplicationName();
    	final String testappl = applicationUnderTest.toLowerCase();
    	final String expectedClassName = "com.iksgmbh.sysnat.language_templates." + testappl + "."
                                         + "LanguageTemplatesBasics_" + applicationUnderTest;
    	try {
			final Class<?> type =  Class.forName(expectedClassName);
			final Constructor<?> constructor = type.getConstructor(ExecutableExample.class);
			final Object toReturn = constructor.newInstance(this);
			return (LanguageTemplateBasics) toReturn;
		} 
    	catch (ClassNotFoundException e) 
    	{
    		String errorMessage = "For TestApplication '" + executionInfo.getTestApplicationName() 
                                   + "' the expected LanguageTemplateContainer '" + expectedClassName + "' has not been found.";
            System.err.println(errorMessage);
            throw new UnsupportedGuiEventException(errorMessage);
    		
    	} catch (Exception e) 
    	{
    		String errorMessage = "An instance of the LanguageTemplateContainer (" + expectedClassName + ") "
    				                + "for TestApplication '" + executionInfo.getTestApplicationName() 
    		                        + ".java' could not be created.";
    		System.err.println(errorMessage);
    		throw new UnsupportedGuiEventException(errorMessage);
		}
	}
    
    public void sleep(int millis) 
    {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // ignore
        }
    }
    
	//##########################################################################################
	//               G U I   C O N T R O L   D E L E G A T I O N   M E T H O D S
	//##########################################################################################

	
    public void inputText(String fieldName, String value) {
    	getGuiController().insertText(value, fieldName);
    }
    
    public void inputEmail(String fieldName, String emailAdress) {
    	getGuiController().inputEmail(fieldName, emailAdress);
    }

	public void clickTableCellLink(String xPathToCell) {
		((SeleniumGuiController)getGuiController()).clickTableCellLink(xPathToCell);
		
	}
    
    public void clickButton(String id) {
    	clickElement(id);
    }
    
    public void clickButton(String id, int timeOutInMillis) {
    	clickElement(id, timeOutInMillis);
    }
    

    /**
     * Assumes that the text referenced by xpath consists of lines of text separated by '\n'
     * @param xpath
     * @return number of lines
     */
    public int getNumberOfSelectableEntries(String xpath)  {
    	return getGuiController().getNumberOfLinesInTextArea(xpath);
    }

    public void selectEntry(String xpath, int index)  {
    	getGuiController().selectComboboxEntry(xpath, index);
    }

    public String getTextForId(String id) {
    	return getGuiController().getText(id);
    }
    
    private boolean login() 
    {
		final TestApplication testApplication = executionInfo.getTestApplication();
		String startParam = testApplication.getStartParameterValue();
		boolean applicationStarted = getGuiController().init(startParam);
		
		if (applicationStarted) 
		{
	    	if ("false".equalsIgnoreCase( System.getProperty("sysnat.dummy.test.run"))) 
	    	{
	    		getApplicationSpecificLanguageTemplates().doLogin(testApplication.getLoginParameter());
	    	} 
	    	else 
	    	{
	    		System.err.println("ATTENSION: This is a dummy test run!");
	    		executionInfo.setApplicationStarted(true);
	    		applicationStarted = true;
	    	}
		} 
		else 
		{
			String errorMessage = ERR_MSG_BUNDLE.getString("AppNotAvailable").replace("XY", startParam);
			String hintMessage = ERR_MSG_BUNDLE.getString("AppNotAvailableHint");
			ErrorPageLauncher.doYourJob(errorMessage, hintMessage,
					                    ERR_MSG_BUNDLE.getString("InitialisationError"));
		}
    	
    	return applicationStarted;
    }
	
    public void clickLink(String valueCandidate)
    {
    	String value = getTestDataValue(valueCandidate);
    	getGuiController().clickLink(value);
    }

    public void clickLink(String valueCandidate, String idToScrollIntoView)
    {
    	String value = getTestDataValue(valueCandidate);
    	getGuiController().clickLink(value, idToScrollIntoView);
    }

    /**
     * If link occurs more than once, the positionOfOccurrence tells which occurrence to click.
     * @param valueCandidate
     * @param positionOfOccurrence
     */
    public void clickLink(String valueCandidate, int positionOfOccurrence)
    {
    	String value = getTestDataValue(valueCandidate);
    	getGuiController().clickLink(value, positionOfOccurrence);
    }

    public void clickLinkIfPossible(String valueCandidate)
    {
       try {
          clickLink(valueCandidate);
       } catch (UnsupportedGuiEventException e) {
    	   addCommentToReport(e.getMessage());
       } catch (SysNatException e) {
          // ignore this action
       }
    } 

    public void chooseFromComboBoxByIndex(String id, int index) {
    	getGuiController().selectComboboxEntry(id, index);
    }

    public void chooseFromComboBoxByValue(String id, String value) {
    	getGuiController().selectComboboxEntry(id, value);
    }
    
    public void clickElement(final String elementAsString, int timeoutInMillis)
    {
	    String elementText = getTestDataValue(elementAsString);
	    //clickElement(elementText);
    	String errMsg = getGuiController().clickElement(elementAsString, timeoutInMillis);
		if (errMsg != null) {
			failWithMessage(errMsg);
		}
		addReportMessage("Es wurde auf <b>" + elementText + "</b> geklickt.");
    }
    
    public void clickElement(final String elementAsString)
    {
    	clickElement(elementAsString, executionInfo.getDefaultGuiElementTimeout());
    }

    public boolean isElementReadyToUse(String elementId) {
    	return getGuiController().isElementReadyToUse(elementId);
    }
    
    public boolean isElementAvailable(String elementId) {
    	return getGuiController().isElementAvailable(elementId);
    }

	public int countNumberOfRowsInTable(String tableClass) {
		return getGuiController().getNumberOfRows(tableClass);
	}

	public int countNumberOfColumnsInTable(String tableClass) {
		return getGuiController().getNumberOfColumns(tableClass);
	}
	
	public void selectFromDropdown(String fieldName, String value) {
		getGuiController().selectComboboxEntry(fieldName, value);
	}

	public boolean isEntryInComboboxDropdownAvailable(String elementIdentifier, String value) {
		return getGuiController().isEntryInComboboxDropdownAvailable(elementIdentifier, value);
	}

	public void chooseOption(String elementIdentifier, int position) {
		getGuiController().selectRadioButton(elementIdentifier, position);
	}

	public void clickFirstButtonsOf(String... buttonIDs) 
	{
		RuntimeException excectionToThrow = null;
		for (String buttonID : buttonIDs) {
			try {			
				String errMsg = getGuiController().clickElement(buttonID);
				if (errMsg != null) {
					failWithMessage(errMsg);
				}
				return;
			} catch (RuntimeException e) {
				excectionToThrow = e;
			}
		}
		throw excectionToThrow;
	}

	public boolean isCheckboxSelected(String checkboxId) 
	{
		return getGuiController().isSelected(checkboxId);
	}


	public void clickCheckbox(String checkboxId) 
	{
		String errMsg = getGuiController().clickElement(checkboxId);
		if (errMsg != null) {
			failWithMessage(errMsg);
		}
	}

	public boolean isTabActive(String tabId) {
		String selectedTabName = getSelectedTabName();
		return tabId.equals(selectedTabName);
	}
	
	public boolean isTabValid(String tabId) {
		return ((SeleniumGuiController) getGuiController()).isTabValid(tabId);
	}
	
	public boolean isErrorTab(String tabId) 
	{
		return ((SeleniumGuiController) getGuiController()).isErrorTab(tabId);
	}
	

	public String getTextForElement(String elementIdentifier) {
		return getGuiController().getText(elementIdentifier);
	}

	public void clickElementInTable(String tableIdentifier, int rowNumber, int columnNumber) {
		getGuiController().getTableCell(tableIdentifier, rowNumber, columnNumber);
	}

	public void clickTab(String tabIdentifier, String tabName) {
		getGuiController().clickTab(tabIdentifier, tabName);
	}
	
	private String getSelectedTabName() {
		return getGuiController().getSelectedTabName();
	}
	
	public boolean isFailedLoginLabelVisible() {
		return getGuiController().isElementAvailable("validationErrors");
	}
	public void waitUntilElementIsAvailable(String elementIdentifier) {
		getGuiController().waitUntilElementIsAvailable(elementIdentifier);
		
	}
	
	public void clickMenuHeader(String elementIndentifier) 
	{
		String errMsg = getGuiController().clickElement(elementIndentifier);
		if (errMsg != null) {
			failWithMessage(errMsg);
		}
	}
	
	public void closeCurrentTab() {
		((SeleniumGuiController)getGuiController()).closeCurrentTab();
	}
	
	public void downloadPdf() 
	{
		int numberOfOpenBrowserFrames = guiController.getNumberOfOpenApplicationWindows();
		boolean isPdfDisplayedInBrowserTab = waitUntilPdfGenerationIsFinished(numberOfOpenBrowserFrames);
		if (isPdfDisplayedInBrowserTab)  // PDF is shown in browser tab, now download it
		{
			int numberOfPDFs = SysNatFileUtil.findDownloadFiles("PDF").size();
			startDownload();  // does not work always - reason unclear
			SysNatFileUtil.waitUntilNumberOfPdfFilesIs(numberOfPDFs + 1);
			
			closeCurrentTab();
		} else {
			// do nothing, PDF has been directly saved in download dir
		}
	}
	
	private boolean waitUntilPdfGenerationIsFinished(int originalNumberOfBrowserWindows) 
	{
		long startTime = new Date().getTime();
		boolean tryAgain = true;
		long waitPeriodInSeconds = 0;

		while (tryAgain) {
			try {
				int numberOfBrowserFrames = getGuiController().getNumberOfOpenApplicationWindows();
				int numberOfOpenTabs = ((SeleniumGuiController)getGuiController()).getNumberOfOpenTabs();
				if (originalNumberOfBrowserWindows + 1 == numberOfBrowserFrames) {
					getGuiController().switchToLastWindow();
				}
				boolean isDownloadButtonAvailable = getGuiController().isElementAvailable("download");
				if ((numberOfOpenTabs == 2 || originalNumberOfBrowserWindows + 1 == numberOfBrowserFrames)
				        && isDownloadButtonAvailable) {
					return true;
				}
			} catch (Exception e) {
				// do nothing
			}

			sleep(1000);
			waitPeriodInSeconds = (new Date().getTime() - startTime) / 1000;
			if (waitPeriodInSeconds > executionInfo.getDefaultPrintTimeout()) {
				addReportMessage(
				        "<b>Der Druckversuch wurde nach " + waitPeriodInSeconds + " Sekunden abgebrochen!</b>");
				return false;
			}
		}
		
		return false;
	}
	
	public void setTickToCheckboxIfPossible(String checkboxId) 
	{
		if ( getGuiController().isElementAvailable(checkboxId) 
			 && getGuiController().isElementReadyToUse(checkboxId) 
			 && isCheckboxSelected(checkboxId) ) 
		{
			clickCheckbox(checkboxId);
		}
	}
	
	public String[] getSelectableEntries(String id) {
		return getTextForId(id).split("\n");
	}
	
	public String getSelectedComboBoxEntry(String elementId) {
		return getGuiController().getSelectedComboBoxEntry(elementId);
	}
	
	public String getTableCell(String tableIndentifier, int rowNumber, int columnNumber) {
		return getGuiController().getTableCell(tableIndentifier, rowNumber, columnNumber);
	}
	
	public String getTableCell(String tableIndentifier, String contentOfFirstCellInRow, int columnNumber) 
	{
		int numberOfRows = getGuiController().getNumberOfRows(tableIndentifier);
		for (int rowNumber=1; rowNumber<=numberOfRows; rowNumber++) 
		{
			String cellContent = getGuiController().getTableCell(tableIndentifier, rowNumber, 1);
			if (cellContent.equals(contentOfFirstCellInRow))
			{
				return getGuiController().getTableCell(tableIndentifier, rowNumber, columnNumber);
			}
		}
		throw new UnsupportedGuiEventException("Tabelle <b>" + tableIndentifier + "</b> enthält "
				                               + "in der ersten Spalte keinen Eintrag für <b>" + contentOfFirstCellInRow + "</b>.");
	}
	
	
	//##########################################################################################
	//                       P R I V A T E    M E T H O D S
	//##########################################################################################


	private String getCheckedXXId() 
	{
		if (XXID == null)  {
			XXID = getTestCaseFileName();
			terminateTestCase("Für den Test " + XXID + " wurde keine XXID angegeben!");
		}
		return XXID;
	}

	protected void initShutDownHook() 
	{
		if (executionInfo.isShutDownHookAdded()) {
			return;
		}
			
		executionInfo.setShutDownHookAdded();
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() 
			{
				System.out.println(SysNatConstants.SYS_OUT_SEPARATOR);
				
				if (executionInfo.isTestEnvironmentInitialized()) {
					createReportHtml();
				}
				
				if (executionInfo.getNumberOfAllExecutedXXs() == 0) {
					System.out.println("No test executed. Check execution filter and executable examples.");
				} else {
					System.out.println("Done with executing " + executionInfo.getTestReportName() + ".");
				}
		    	
	    		if (executionInfo.getGuiController() != null)  {
	    			executionInfo.getGuiController().closeGUI();
	    		}
			}
		});
	}
	
	
    /**
    * Creates a big overview report with all details and a short overview report for ALM archiving. 
     * Note: single detail reports for each test case have been already created in _NatSpecTemplate.shutdown()
    */
    private void createReportHtml() 
    {
        // full overview report
        final String fullReport = ReportCreator.createFullOverviewReport();
        final String fullOverviewReportFilename = ReportCreator.getFullOverviewReportFilename();
        SysNatFileUtil.writeFile(fullOverviewReportFilename, fullReport);
  		
        ResultLaunchOption resultLaunchOption = ExecutionRuntimeInfo.getInstance().getResultLaunchOption();
		if (resultLaunchOption == ResultLaunchOption.Testing || resultLaunchOption == ResultLaunchOption.Both) {
			HtmlLauncher.doYourJob( fullOverviewReportFilename );
    	}
          
        // small report
        final String shortOverview = ReportCreator.createShortOverviewReport();
        SysNatFileUtil.writeFile( ReportCreator.getShortOverviewReportFilename(), shortOverview);
          
         if (executionInfo.isTestReportToArchive()) {
              SysNatTestResultArchiver.doYourJob( executionInfo.getReportFolder() );
         } else {
                System.out.println("Archiving test results is omitted.");
         }
         
         // save executed Scripts to document what exactly has been executed for further analysis
         copyExecutedNLFilesToReportDir();
         String zipFileName = executionInfo.getTestReportName() + "-" +
        		              executionInfo.getStartPointOfTimeAsFileStringForFileName() + ".zip";
         
         SysNatFileUtil.createZipFile(executionInfo.getReportFolder(), 
        		                      new File(executionInfo.getReportFolderAsString(), 
        		                    		   zipFileName));
    }

	private void copyExecutedNLFilesToReportDir() 
	{
		String reportDir = executionInfo.getReportFolder().getAbsolutePath();
        String scriptDir = reportDir + "/executedNLFiles";
        new File(scriptDir).mkdirs();
        
        final List<File> nlsFiles = collectExecutedNLFiles();
        nlsFiles.forEach(file -> SysNatFileUtil.copyFileToTargetDir(file.getAbsolutePath(), scriptDir));
	}
	
	private List<File> collectExecutedNLFiles() 
	{
		final List<File> toReturn = new ArrayList<>();
		List<String> executedNLFiles = executionInfo.getExecutedNLFiles();
		executedNLFiles.forEach(filename -> toReturn.add(findExecutedNLFile(filename)));
		toReturn.removeAll(Collections.singleton(null));
		return toReturn;
	}
	
	private File findExecutedNLFile(final String filename) 
	{
		String testCaseDir = executionInfo.getExecutableExampleDir() + "/" + executionInfo.getTestApplicationName();
		
		FilenameFilter scriptFileFilter = new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				return name.equals(filename);
			}
		};
		
		List<File> result = FileFinder.searchFilesRecursively(new File(testCaseDir), scriptFileFilter);
		
		if (result.size() != 1) {
			System.err.println("Cannot find unique file '" + filename + "' in directory: " + testCaseDir);
			return null;
		}
		
		return result.get(0);
	}
	
	public void answerQuestion(final String question, final boolean  ok)
	{
		if (ok) { 
			addReportMessage(question + YES); 
		} else {
			addReportMessage(question + NO);
			throw new UnexpectedResultException();
		}
	}
	
	public int getNumberOfBrowserWindows() {
		return ((SeleniumGuiController)getGuiController()).getNumberOfBrowserWindows();
	}

	public void waitUntilNumberOfBrowserWindowsIs(int expectedNumber) 
	{
	    int numberOfBrowserWindows = getNumberOfBrowserWindows();
	    int secondCounter = 0;
	    
	    while (numberOfBrowserWindows > expectedNumber) 
	    {
	    	sleep(1000);
	    	secondCounter++;
	    	numberOfBrowserWindows = getNumberOfBrowserWindows();
	    	if (secondCounter > executionInfo.getDefaultPrintTimeout()) {
	    		addReportMessage("<b>Der Druckversuch wurde nach " + executionInfo.getDefaultPrintTimeout() + " Sekunden abgebrochen!</b>");
	    		return;
	    	}
	    }
    	
	    addReportMessage("//Der Test musste " + secondCounter + " Sekunden auf den Druck warten.");
	}
	
	private void startDownload() 
	{
		int numberOfAvailableWindows1 = WindowHelper.getNumberOfAvailableWindows();
		((SeleniumGuiController)getGuiController()).clickDownloadButton();
		sleep(2000); // give time for open dialog to appear and to set focus on the OK button
		int numberOfAvailableWindows2 = WindowHelper.getNumberOfAvailableWindows();
		if (numberOfAvailableWindows1 < numberOfAvailableWindows2) {
			sendPressTabEvent();
			sleep(1000);
			sendPressTabEvent();
			sleep(1000);		 	
			sendPressSpaceEvent();  // this ticks the checkbox "Diese Aktion für alle Dateien dieses Typs auführen"
			sleep(1000);
			sendPressTabEvent();
			sleep(1000);
			sendPressEnterEvent(); // this handles the open dialog by activating the ok button
			sleep(1000);
		}
		
	}

	private void sendPressTabEvent() 
	{
		try {
			Robot robot = new Robot();
			robot.keyPress(KeyEvent.VK_TAB);
			robot.keyRelease(KeyEvent.VK_TAB);
		} catch (AWTException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void sendPressEnterEvent() 
	{
		try {
			Robot robot = new Robot();
			robot.keyPress(KeyEvent.VK_ENTER);
			robot.keyRelease(KeyEvent.VK_ENTER);
		} catch (AWTException e) {
			throw new RuntimeException(e);
		}
	}

	private void sendPressSpaceEvent() 
	{
		try {
			Robot robot = new Robot();
			robot.keyPress(KeyEvent.VK_SPACE);
			robot.keyRelease(KeyEvent.VK_SPACE);
		} catch (AWTException e) {
			throw new RuntimeException(e);
		}
	}
	
	protected void sendPressTabBackwardsEvent() 
	{
		try {
			Robot robot = new Robot();
			robot.keyPress(KeyEvent.VK_SHIFT);
			robot.keyRelease(KeyEvent.VK_TAB);
			robot.keyRelease(KeyEvent.VK_TAB);
			robot.keyRelease(KeyEvent.VK_SHIFT);
		} catch (AWTException e) {
			throw new RuntimeException(e);
		}
	}
	
	public String getPictureProofName() 
	{
		final String fileNamePrefix = getTestCaseFileName() + PICTURE_PROOF;
		final String reportFolder = ExecutionRuntimeInfo.getInstance().getReportFolder().getAbsolutePath();
		final int numberOfExistingBildnachweise = 
				SysNatFileUtil.getNumberOfFilesStartingWith(fileNamePrefix,
						                                    reportFolder);
		return fileNamePrefix + (numberOfExistingBildnachweise + 1);
	}
	
	public boolean hasBildnachweisCategory() 
	{
		for (String filter : getExecutionFilterList()) {
			if (CATEGORY_BILDNACHWEIS.equals(filter)) {
				return true;
			}
		}
		
		return false;
	}
	
	public List<String> buildExecutionFilterList(String executionFilterOfThisTestCase) 
	{
		setExecutionFilter(SysNatStringUtil.getExecutionFilterAsList(executionFilterOfThisTestCase, getXXID()));
		return getExecutionFilterList();
	}
	
	public List<String> getExecutionFilterList() {
		return executionFilterList;
	}
	
	public void setExecutionFilter(List<String> filters) {
		this.executionFilterList = filters;
	}
	
	public GuiControl getGuiController() {
		return guiController;
	}
	
	public void setGuiController(GuiControl guiController) {
		this.guiController = guiController;
	}
	
	public String getTagName(String elementIdentifier) {
		return guiController.getTagName(elementIdentifier);
	}
		
	public List<String> getPackageNames() 
	{
		final List<String> toReturn = new ArrayList<String>();
		Package packageOfTestClass = getTestCasePackage();
		if (packageOfTestClass == null) {
			return toReturn;
		}
		
		String[] splitResult = getTestCasePackage().getName().split("\\.");
		
		for (String string : splitResult) {
			toReturn.add(string);
		}
		return toReturn;
	}
	
	public void setTestData(final SysNatTestData data) {
		testDataSets = data;
	}
	
	public SysNatTestData getTestData() {
		return testDataSets;
	}
	
	public String getTestDataValue(String valueCandidate) 
	{
		if ( ! valueCandidate.contains(SysNatConstants.DC) ) {
			// valueCandidate is a hard coded value no fieldname reference
			return valueCandidate; 
		}

		String valueReference = valueCandidate;
		if (valueReference.startsWith(SysNatConstants.DC)) {
			String fieldName = valueReference.substring(2);
			return testDataSets.findValueForValueReference(fieldName);
		}
		
		String[] splitResult = valueReference.split(SysNatConstants.DC);
		
		if (splitResult.length != 2) {
			throw new SysNatTestDataException("Cannot parse reference to value: " + valueReference);
		}

		String datasetName = testDataSets.findDatasetNameForDatasetReference(splitResult[0]);
		String fieldName = splitResult[1];
		return testDataSets.getValue(datasetName, fieldName);
	}
	
	
	public boolean isTextCurrentlyDisplayed(String text) {
		return guiController.isTextCurrentlyDisplayed(text);
	}
	
	
	public Hashtable<String, Properties> importTestData(final String testdata) 
	{
		final Hashtable<String, Properties> loadedDatasets;
		
		if (testdata.contains(SysNatConstants.LINE_SEPARATOR)) 
		{
			List<Properties> datasets = TableDataParser.doYourJob(getTestCaseFileName(), testdata);
			loadedDatasets = new Hashtable<>();
			for (int i = 0; i < datasets.size(); i++) {
				loadedDatasets.put("TestData" + (i+1), datasets.get(0));
			}
		} 
		else {
			loadedDatasets = getTestDataImporter().loadTestdata(testdata);
			reportMessages.add("The data file <b>" + testdata + "</b> has been imported "
					           + "with <b>" + loadedDatasets.size() + "</b> datasets.");
		}
		loadedDatasets.forEach( (datasetName, dataset) -> testDataSets.addDataset(datasetName, dataset));	
		
		return loadedDatasets;
	}
	
	public TestDataImporter getTestDataImporter() 
	{
		if (testDataImporter == null) {
			testDataImporter = new TestDataImporter(executionInfo.getTestdataDir());
		}
		return testDataImporter;
	}

	public void addLinewiseToReportMessageAsComment(String toReport) 
	{
		String[] splitResult = toReport.split(System.getProperty("line.separator"));
		addLinewiseToReportMessageAsComment(Arrays.asList(splitResult));
	}

	public void addLinewiseToReportMessageAsComment(List<String> commentLines) 
	{
		for (String line : commentLines) {
			addCommentToReport(line);
		}
	}

	public void addCommentToReport(String commentLine) {
		addReportMessage("//" + commentLine);
	}
	
	public List<String> executeCommandAndListOutput(String command) 
	{
		List<String> toReturn = new ArrayList<>();
		try {
			Process p = Runtime.getRuntime().exec("cmd /c " + command);
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line = "";			
			while ((line = reader.readLine())!= null) {
				toReturn.add(line);
			}
			p.waitFor();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return toReturn;
	}
	
	public void setScriptToExecute(String scriptName) {
		this.setScriptToExecute = scriptName;
	}

	public String getScriptToExecute() {
		return setScriptToExecute;
	}

	public void setBddKeyword(String bddKeyword) {
		this.bddKeyword = bddKeyword;
	}


	public void storeTestObject(String name, Object o) {
		testObjects.put(name.toUpperCase(), o);
	}

	public Object getTestObject(String name) {
		return testObjects.get(name.toUpperCase());
	}

	public HashMap<String, Object> getTestObjects() {
		return testObjects;
	}

	public boolean doesTestObjectExist(String objectName) {
		return getTestObject(objectName) != null;
	}
	
	public void setTestObjects(HashMap<String, Object> hashMap) {
		testObjects = hashMap;
	}

	public void setBehaviorID(String aBehaviourID) 
	{
		behaviourID = aBehaviourID;
		if (bddKeyword != null && ! bddKeyword.isEmpty() ) {
			executionInfo.addToKnownFeatures(aBehaviourID);
		}
	}
	
	public String getBehaviorID() {
		return behaviourID;
	}
	
	
	public List<String> getNlxxFilePathAsList() {
		return new ArrayList<>();  // to be overwritten !
	}

}