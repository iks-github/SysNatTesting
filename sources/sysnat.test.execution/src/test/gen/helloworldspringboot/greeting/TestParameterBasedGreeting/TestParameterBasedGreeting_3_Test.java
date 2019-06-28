package helloworldspringboot.greeting.TestParameterBasedGreeting;

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
public class TestParameterBasedGreeting_3_Test extends SysNatTestCase
{
	private static final String BEHAVIOR_ID = "HelloWorldTestData";
	
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
			languageTemplatesCommon.startNewXX("TestParameterBasedGreeting_3");
			languageTemplatesCommon.defineAndCheckExecutionFilter("<path>");
			languageTemplatesCommon.setTestData("HelloWorldTestData_GreetingValidationData3");

			// arrange block
			languageTemplatesCommon.startNewTestPhase("Arrange");
			languageTemplatesCommon.executeScript("EnterGreetingData");

			// act block
			languageTemplatesCommon.startNewTestPhase("Act");
			languageTemplatesBasics_HelloWorldSpringBoot.clickButton("Greet");

			// assert block
			languageTemplatesCommon.startNewTestPhase("Assert");
			languageTemplatesBasics_HelloWorldSpringBoot.isTextDislayed("GreetingResult", "HelloWorldTestData_GreetingValidationData3::GreetingResult");
			
			closeCurrentTestCaseWithSuccess();
		} catch (Throwable e) {
			super.handleThrowable(e);
		}
	}
	
}
