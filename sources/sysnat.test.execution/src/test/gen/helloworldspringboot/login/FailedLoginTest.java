package helloworldspringboot.login;

import org.junit.Test;
import org.junit.After;
import org.junit.Before;

import com.iksgmbh.sysnat.testcasejavatemplate.SysNatTestCase;
import com.iksgmbh.sysnat.language_templates.helloworldspringboot.LanguageTemplatesBasics_HelloWorldSpringBoot;
import com.iksgmbh.sysnat.language_templates.common.LanguageTemplatesCommon;
import com.iksgmbh.sysnat.language_templates.common.LanguageTemplatesDownload;
import com.iksgmbh.sysnat.language_templates.common.LanguageTemplatesPDFValidation;

/**
 * Executable Example for TestApplication 'HelloWorldSpringBoot'.
 * Autogenerated by SysNat.
 */
@SuppressWarnings("unused")
public class FailedLoginTest extends SysNatTestCase
{
	
	protected LanguageTemplatesBasics_HelloWorldSpringBoot languageTemplatesBasics_HelloWorldSpringBoot;
	protected LanguageTemplatesCommon languageTemplatesCommon;
	protected LanguageTemplatesDownload languageTemplatesDownload;
	protected LanguageTemplatesPDFValidation languageTemplatesPDFValidation;
	
	
	@Before
	public void setUp() 
	{
		super.setUp();
		languageTemplatesBasics_HelloWorldSpringBoot = new LanguageTemplatesBasics_HelloWorldSpringBoot(this);
		languageTemplatesCommon = new LanguageTemplatesCommon(this);
		languageTemplatesDownload = new LanguageTemplatesDownload(this);
		languageTemplatesPDFValidation = new LanguageTemplatesPDFValidation(this);
	}

	@After
	public void shutdown() 
	{
		if ( ! isSkipped() && executionInfo.isApplicationStarted()) {
			if (languageTemplatesBasics_HelloWorldSpringBoot != null) languageTemplatesBasics_HelloWorldSpringBoot.gotoStartPage();
		}
		languageTemplatesBasics_HelloWorldSpringBoot = null;
		languageTemplatesCommon = null;
		languageTemplatesDownload = null;
		languageTemplatesPDFValidation = null;
		super.shutdown();
	}

	
	@Test
	@Override
	public void executeTestCase() 
	{
		try {		
			languageTemplatesCommon.startNewXX("<filename>");
			languageTemplatesCommon.defineAndCheckExecutionFilter("<path>");

			// arrange block
			languageTemplatesCommon.startNewTestPhase("Arrange");
			languageTemplatesBasics_HelloWorldSpringBoot.isPageVisible("Form Page");
			languageTemplatesBasics_HelloWorldSpringBoot.clickMainMenuItem("Logout");
			languageTemplatesBasics_HelloWorldSpringBoot.isPageVisible("Login Page");

			// act block
			languageTemplatesCommon.startNewTestPhase("Act");
			languageTemplatesBasics_HelloWorldSpringBoot.loginWith("Peter", "wrongPW");

			// assert block
			languageTemplatesCommon.startNewTestPhase("Assert");
			languageTemplatesBasics_HelloWorldSpringBoot.isPageVisible("Error Page");
			languageTemplatesBasics_HelloWorldSpringBoot.isTextDislayed("ErrorMessage", "Invalid User Login Data.");

			// cleanup block
			languageTemplatesCommon.startNewTestPhase("Cleanup");
			languageTemplatesBasics_HelloWorldSpringBoot.clickButton("Back");
			languageTemplatesBasics_HelloWorldSpringBoot.relogin();
			
			closeCurrentTestCaseWithSuccess();
		} catch (Throwable e) {
			super.handleThrowable(e);
		}
	}
	
}
