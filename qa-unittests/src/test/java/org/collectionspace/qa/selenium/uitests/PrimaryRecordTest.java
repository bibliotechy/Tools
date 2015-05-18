package org.collectionspace.qa.selenium.uitests;

import com.thoughtworks.selenium.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import com.thoughtworks.selenium.webdriven.WebDriverBackedSelenium;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

import static org.collectionspace.qa.selenium.uitests.Utilities.*;
import static org.junit.Assert.*;

@RunWith(value = Parameterized.class)
public class PrimaryRecordTest {

    static WebDriverBackedSelenium selenium;
    public static String AFTER_DELETE_URL = "findedit.html";
    private int primaryType;

    public PrimaryRecordTest(int number) {
        this.primaryType = number;
    }

    @Parameters
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
			//comment out here to limit tests
            {Record.INTAKE},
            {Record.LOAN_IN},
            {Record.LOAN_OUT},
            {Record.ACQUISITION},
            {Record.MEDIA}, 
            {Record.OBJECT_EXIT},
            {Record.GROUP},
            {Record.CATALOGING},
            {Record.MOVEMENT}
        };
        return Arrays.asList(data);
    }

    @BeforeClass
    public static void init() throws Exception {

        WebDriver driver =new  FirefoxDriver();
        if (System.getProperty("baseurl") != null) {
            BASE_URL = System.getProperty("baseurl");
        }
        selenium = new WebDriverBackedSelenium(driver, BASE_URL);


        //log in:
        login(selenium);

        //autogenerate a movement record so that we have an urn::value to put in the required field for movement records
        String locationAuthorityURN = getLocationURN(selenium);
        System.out.println("URN for location authority: "+locationAuthorityURN);
        Record.setField(Record.MOVEMENT, Record.getRequiredFieldSelector(Record.MOVEMENT), locationAuthorityURN);
    }

    /**
     * TEST: tests the creation of a new record via the create new page:
     * 1) Open create new page
     * 2) Select record in question via radio button
     * 3) Click Create
     *
     * X) Expect correct record page to be loaded
     * 
     * @throws Exception
     */
    @Test
    public void testCreateNew() throws Exception {
        System.out.println("* Starting test: testCreateNew");
        log("  CREATE NEW: Testing creating new " + Record.getRecordTypePP(primaryType) + "\n");
        selenium.open("createnew.html");
        elementPresent("css=:input[value='" + Record.getRecordTypeShort(primaryType) + "']", selenium);
        selenium.click("css=:radio[value='" + Record.getRecordTypeShort(primaryType) + "']");
        selenium.click("//input[@value='Create']");
        log("  CREATE NEW: expect correct record page to load and pattern chooser to show\n");
        waitForRecordLoad(primaryType, selenium);
        assertEquals(Record.getRecordTypePP(primaryType), selenium.getText("css=#title-bar .record-type"));
        System.out.println("* Ending test: testCreateNew");
    }

    /**
     * TEST: Tests the save functionality:
     * 1) Create a new record
     * 2) Fill out form with default values
     * 3) Save the record
     * 4) Check that the fields are as expected
     * 
     * @throws Exception
     */
    @Test
    public void testPrimarySave() throws Exception {
        System.out.println("* Starting test: testPrimarySave");
        String primaryID = Record.getRecordTypeShort(primaryType) + (new Date().getTime());

        log("  " + Record.getRecordTypePP(primaryType) + ": test fill out record and save\n");
        selenium.open(Record.getRecordTypeShort(primaryType) + ".html");
        waitForRecordLoad(primaryType, selenium);

        fillForm(primaryType, primaryID, selenium);
        //save record
        log("  " + Record.getRecordTypePP(primaryType) + ": expect save success message and that all fields are valid\n");
		save(selenium);
		if (selenium.isElementPresent("css=.csc-confirmationDialog .saveButton")){
			selenium.click("css=.csc-confirmationDialog .saveButton");
		}
        //check values:
        verifyFill(primaryType, primaryID, selenium);
        //Uncomment below for debugging - gives you 30 secs to check everything is working
        //Thread.sleep(1000 * 30);
        System.out.println("* Ending test: testPrimarySave");
    }

    /**
     * TEST: Tests save functionality when the fields are empty
     *
     * PRE-REQUISITE: an already saved record is loaded
     * 1) Select the first option for all dropdowns
     * 2) Write the empty string in all fields
     * 3) Save
     * X) Expect no ID warning
     * 4) Fill out ID and save
     * X) Expect successful message
     * X) Expect all fields to be empty except for ID, Expect drop-downs to have index 0 selected
     * 
     * @throws Exception
     */
    @Test
    public void testRemovingValues() throws Exception {
        System.out.println("* Starting test: testRemovingValues");
        //generate a record

        //HACK - app layer script to create a new record doesn't always work
        //String generatedID = generateRecord(primaryType, selenium);
        String generatedID = Record.getRecordTypeShort(primaryType) + (new Date().getTime());

        //log(Record.getRecordTypePP(primaryType) + ": test fill out record and save\n");
        selenium.open(Record.getRecordTypeShort(primaryType) + ".html");
        waitForRecordLoad(primaryType, selenium);

        fillForm(primaryType, generatedID, selenium);
        //save record
        //log(Record.getRecordTypePP(primaryType) + ": expect save success message and that all fields are valid\n");
        save(selenium);
        if (selenium.isElementPresent("css=.csc-confirmationDialog .saveButton")){
            selenium.click("css=.csc-confirmationDialog .saveButton");
        }


        //goto some collectionspace page with a search box - and open new record
        //selenium.open("createnew.html");        
        //open(primaryType, generatedID, selenium);
        //Delete contents of all fields:
        clearForm(primaryType, selenium);
        //save record - and expect error due to missing ID
        selenium.click("//input[@value='Save']"); 
		if (selenium.isElementPresent("css=.csc-confirmationDialog .saveButton")){
			selenium.click("css=.csc-confirmationDialog .saveButton");
		}
        //expect error message due to missing required field\n");
        elementPresent("CSS=.cs-message-error", selenium);
        assertEquals(Record.getRequiredFieldMessage(primaryType), selenium.getText("CSS=.cs-message-error #message"));
        //Enter ID and save - expect successful
        selenium.type(Record.getIDSelector(primaryType), generatedID);
        //Also make sure that required field is filled out -- put generatedID in this field too
        selenium.type(Record.getRequiredFieldSelector(primaryType), generatedID);
        save(selenium);
        //check values:
        verifyClear(primaryType, selenium);
        System.out.println("* Ending test: testRemovingValues");
    }

    /**
     * TEST: test record deletion
     *
     * 1) Create new reocrd with a unique ID (based on timestamp)
     * 2) Save the record
     * 3) Click delete button
     * 4) Click cancel
     * 5) Click delete button
     * 6) Click close
     * 7) Click delete button and confirm
     * X) Expect successmessage
     * 8) Click OK on alert box telling successful delete
     * 9) Search for the record
     * X) Expect not found
     * 
     * @throws Exception
     */
    @Test
    public void testDeleteRecord() throws Exception {
        System.out.println("* Starting test: testDeleteRecord");
        log("  DELETING :  " + Record.getRecordTypePP(primaryType) + "\n");
        String uniqueID = Record.getRecordTypeShort(primaryType) + (new Date().getTime());
        createAndSave(primaryType, uniqueID, selenium);
        //test delete confirmation - Close and Cancel
        selenium.click("deleteButton");
        textPresent("Confirmation", selenium);
        assertTrue(selenium.isTextPresent("Delete this "+Record.getRecordTypePP(primaryType) +"?"));
        selenium.click("//img[@alt='close dialog']");
        selenium.click("deleteButton");
        elementPresent("css=input.cs-confirmationDialogButton-cancel", selenium);
        selenium.click("css=input.cs-confirmationDialogButton-cancel");
        selenium.click("deleteButton");
        
        //Test  successfull delete
        elementPresent("css=.cs-confirmationDialog input.cs-confirmationDialogButton-act", selenium);
        selenium.click("css=.cs-confirmationDialog input.cs-confirmationDialogButton-act");
        textPresent(Record.getRecordTypePP(primaryType) +" successfully deleted", selenium);
        selenium.click("css=.cs-confirmationDialog input.cs-confirmationDialogButton");
        selenium.waitForPageToLoad(MAX_WAIT);
        //expect redirect to AFTER_DELETE_URL 
        assertEquals(BASE_URL + AFTER_DELETE_URL, selenium.getLocation());
        //check that the record is indeed deleted
        elementPresent("css=.cs-searchBox :input[value='Search']", selenium);
        selenium.select("recordTypeSelect-selection", "label=" + Record.getRecordTypePP(primaryType));
        selenium.type("css=.cs-searchBox :input[name='searchQuery-1']", uniqueID);
        selenium.click("css=.cs-searchBox :input[value='Search']");
        //removing the following line to prevent selenium from hanging on page load - JJM 2/15/12
        //selenium.waitForPageToLoad(MAX_WAIT);
        //expect no results when searching for the record\n");
        textPresent("Found 0 records for " + uniqueID, selenium);
        log("  ASSERTING : link=" + uniqueID +"\n");
        assertFalse(selenium.isElementPresent("link=" + uniqueID));
        System.out.println("* Ending test: testDeleteRecord");
    }

    /**
     * TEST: Test Cancel button functionality
     *
     * 1) Create a new record and save
     * 2) Fill out fields with known values
     * 3) Save
     * 4) Modify all fields
     * 5) Click cancel
     * X) Expect all values are back to their previous value
     * 
     * @throws Exception
     */
    @Test
    public void testCancel() throws Exception {
        System.out.println("* Starting test: testCancel");
        //create record and fill out all fields
        String uniqueID = Record.getRecordTypeShort(primaryType) + (new Date().getTime());
        createAndSave(primaryType, uniqueID, selenium);
        fillForm(primaryType, uniqueID, selenium);
        save(selenium);
        //modify all fields
        clearForm(primaryType, selenium);
        //click cancel and expect content to change to original\n");
        selenium.click("//input[@value='Cancel changes']");
        //Wait a few seconds for page to reload. Records with small number of fields
        //may get false positives
        Thread.sleep(3000);
        verifyFill(primaryType, uniqueID, selenium);
        System.out.println("* Ending test: testCancel");
    }

    /**
     * TEST: Test leave page warnings
     *
     * 1) Create new record and save
     * 2) Edit field and attempt to navigate away
     * 3) Expect dialog and cancel it
     * 4) Navigate away
     * 5) Expect dialog and close it
     * 6) Navigate away
     * 7) Expect dialog and click Save
     * 8) Navigate away
     * 9) Check that changes have been saved
     * 10) Reopen record
     * 11) Edit field and attempt to navigate away
     * 12) Click dont save
     * 13) Expect changes not to be saved
     * @throws Exception
     */
    @Test
    public void testLeavePageWarning() throws Exception {
        System.out.println("* Starting test: testLeavePageWarning");
        //create
        String uniqueID = Record.getRecordTypeShort(primaryType) + (new Date().getTime());
        String modifiedID = uniqueID + "modified";
        createAndSave(primaryType, uniqueID, selenium);
        //Test close and cancel buttons of dialog
        navigateWarningClose(primaryType, modifiedID, selenium);
        System.out.println("  CLOSE SUCCESS!!");
        //Test 'Save' button - expect it was properly saved
        navigateWarningSave(primaryType, modifiedID, selenium);
        System.out.println("  SAVE SUCCESS!!");
        //go to the record again:
        selenium.click("link=" + modifiedID);
        waitForRecordLoad(primaryType, selenium);
        //Test 'Dont Save' button
        navigateWarningDontSave(primaryType, uniqueID, selenium);
        System.out.println("* Ending test: testLeavePageWarning");
    }
}
