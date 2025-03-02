package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import static org.folio.TestUtils.CREATED_RECORDS;
import static org.folio.TestUtils.ERROR;
import static org.folio.TestUtils.EXTERNAL_SYSTEM_ID;
import static org.folio.TestUtils.FAILED_RECORDS;
import static org.folio.TestUtils.FAILED_USERS;
import static org.folio.TestUtils.JSON_CONTENT_TYPE_HEADER;
import static org.folio.TestUtils.MESSAGE;
import static org.folio.TestUtils.TENANT_HEADER;
import static org.folio.TestUtils.TOKEN_HEADER;
import static org.folio.TestUtils.TOTAL_RECORDS;
import static org.folio.TestUtils.UPDATED_RECORDS;
import static org.folio.TestUtils.USERNAME;
import static org.folio.TestUtils.USER_ERROR_MESSAGE;
import static org.folio.TestUtils.USER_IMPORT;
import static org.folio.TestUtils.generateUser;
import static org.folio.rest.impl.UserImportAPIConstants.FAILED_TO_LIST_CUSTOM_FIELDS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.util.MockJson;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.folio.okapi.common.XOkapiHeaders;
import org.folio.rest.RestVerticle;
import org.folio.rest.jaxrs.model.Address;
import org.folio.rest.jaxrs.model.CustomField;
import org.folio.rest.jaxrs.model.CustomFields;
import org.folio.rest.jaxrs.model.Department;
import org.folio.rest.jaxrs.model.IncludedObjects;
import org.folio.rest.jaxrs.model.RequestPreference;
import org.folio.rest.jaxrs.model.SelectField;
import org.folio.rest.jaxrs.model.SelectFieldOption;
import org.folio.rest.jaxrs.model.SelectFieldOptions;
import org.folio.rest.jaxrs.model.User;
import org.folio.rest.jaxrs.model.UserdataimportCollection;

@RunWith(VertxUnitRunner.class)
public class UserImportAPITest {

  public static final int PORT = NetworkUtils.nextFreePort();
  public static final int MOCK_PORT = NetworkUtils.nextFreePort();
  public static final String HOST = "http://localhost";

  private Vertx vertx;
  private MockJson mock = new MockJson("mock_standard.json");

  @Before
  public void setUp(TestContext context) {
    RestAssured.port = PORT;
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

    vertx = Vertx.vertx();

    DeploymentOptions options = new DeploymentOptions()
      .setConfig(new JsonObject().put("http.port", PORT));

    DeploymentOptions mockOptions = new DeploymentOptions()
      .setConfig(new JsonObject().put("http.port", MOCK_PORT));

    vertx.deployVerticle(new RestVerticle(), options)
      .compose(x -> vertx.deployVerticle(mock, mockOptions))
      .onComplete(context.asyncAssertSuccess());
  }

  @After
  public void tearDown(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }

  @Test
  public void testImportWithoutUsers() {

    mock.setMockJsonContent("mock_content.json");

    UserdataimportCollection collection = new UserdataimportCollection();
    collection.setUsers(new ArrayList<>());
    collection.setTotalRecords(0);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo("No users to import."))
      .body(TOTAL_RECORDS, equalTo(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithAddressTypeResponseError() throws IOException {

    mock.setMockJsonContent("mock_address_types_error.json");

    List<User> users = new ArrayList<>();
    users.add(generateUser("1234567", "Amy", "Cabble", null));

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, containsString(UserImportAPIConstants.FAILED_TO_IMPORT_USERS))
      .body(ERROR, containsString(UserImportAPIConstants.FAILED_TO_LIST_ADDRESS_TYPES))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(1))
      .body(FAILED_USERS + "[0]." + EXTERNAL_SYSTEM_ID, equalTo(users.get(0).getExternalSystemId()))
      .body(FAILED_USERS + "[0]." + USERNAME, equalTo(users.get(0).getUsername()))
      .body(FAILED_USERS + "[0]." + USER_ERROR_MESSAGE, containsString(UserImportAPIConstants.FAILED_TO_LIST_ADDRESS_TYPES))
      .body(FAILED_USERS, hasSize(1))
      .statusCode(500);
  }

  @Test
  public void testImportWithPatronGroupResponseError() throws IOException {

    mock.setMockJsonContent("mock_patron_groups_error.json");

    List<User> users = new ArrayList<>();
    users.add(generateUser("1234567", "Amy", "Cabble", null));

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.FAILED_TO_IMPORT_USERS))
      .body(ERROR, containsString(UserImportAPIConstants.FAILED_TO_LIST_PATRON_GROUPS))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(1))
      .body(FAILED_USERS + "[0]." + EXTERNAL_SYSTEM_ID, equalTo(users.get(0).getExternalSystemId()))
      .body(FAILED_USERS + "[0]." + USERNAME, equalTo(users.get(0).getUsername()))
      .body(FAILED_USERS + "[0]." + USER_ERROR_MESSAGE, containsString(UserImportAPIConstants.FAILED_TO_LIST_PATRON_GROUPS))
      .body(FAILED_USERS, hasSize(1))
      .statusCode(500);
  }

  @Test
  public void testImportWithUserCreation() throws IOException {

    mock.setMockJsonContent("mock_user_creation.json");

    List<User> users = new ArrayList<>();
    users.add(generateUser("1234567", "Amy", "Cabble", null));

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(1))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(0))
      .body(FAILED_USERS, hasSize(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserCreationWithoutPersonalData() throws IOException {

    mock.setMockJsonContent("mock_user_creation_without_personal_data.json");

    User user = generateUser("1234567", "Amy", "Cabble", null);
    user.setPersonal(null);
    List<User> users = new ArrayList<>();
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(1))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(0))
      .body(FAILED_USERS, hasSize(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserCreationWithNonExistingPatronGroup() throws IOException {

    mock.setMockJsonContent("mock_user_creation_with_non_existing_patron_group.json");

    User user = generateUser("1234567", "Amy", "Cabble", null);
    user.setPatronGroup("nonExistingTestPatronGroup");
    List<User> users = new ArrayList<>();
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(1))
      .body(FAILED_USERS, hasSize(1))
      .body(FAILED_USERS + "[0]." + EXTERNAL_SYSTEM_ID, equalTo(users.get(0).getExternalSystemId()))
      .body(FAILED_USERS + "[0]." + USERNAME, equalTo(users.get(0).getUsername()))
      .body(FAILED_USERS + "[0]." + USER_ERROR_MESSAGE,
        equalTo("Patron group does not exist in the system: [nonExistingTestPatronGroup]"))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserWithoutExternalSystemId() throws IOException {

    mock.setMockJsonContent("mock_user_creation_without_externalsystemid.json");

    List<User> users = new ArrayList<>();
    User testUser = generateUser("1234567", "Amy", "Cabble", null);
    testUser.setExternalSystemId(null);
    users.add(testUser);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body("errors.parameters", hasSize(1))
      .statusCode(422);
  }

  /*
   * This test does not reflect real-time environment currently.
   */
  //  @Test
  public void testImportWithUserWithEmptyExternalSystemId() throws IOException {

    mock.setMockJsonContent("mock_user_creation_with_empty_externalsystemid.json");

    List<User> users = new ArrayList<>();
    User testUser = generateUser("1234567", "Amy", "Cabble", null);
    testUser.setExternalSystemId("");
    users.add(testUser);
    users.add(testUser);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(2))
      .body(CREATED_RECORDS, equalTo(1))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(1))
      .body(FAILED_USERS, hasSize(1))
      .body(FAILED_USERS + "[0]." + EXTERNAL_SYSTEM_ID, equalTo(testUser.getExternalSystemId()))
      .body(FAILED_USERS + "[0]." + USERNAME, equalTo(testUser.getUsername()))
      .body(FAILED_USERS + "[0]." + USER_ERROR_MESSAGE,
        equalTo(UserImportAPIConstants.FAILED_TO_CREATE_NEW_USER_WITH_EXTERNAL_SYSTEM_ID + testUser.getExternalSystemId()))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserWithoutUsername() throws IOException {

    mock.setMockJsonContent("mock_user_creation.json");

    List<User> users = new ArrayList<>();
    User testUser = generateUser("1234567", "Amy", "Cabble", null);
    testUser.setUsername(null);
    users.add(testUser);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body("errors.parameters", hasSize(1))
      .statusCode(422);
  }

  @Test
  public void testImportWithUserCreationAndPermissionError() throws IOException {

    mock.setMockJsonContent("mock_user_creation_with_permission_error.json");

    List<User> users = new ArrayList<>();
    users.add(generateUser("1234567", "Amy", "Cabble", null));

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(1))
      .body(FAILED_USERS, hasSize(1))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserSearchError() throws IOException {

    mock.setMockJsonContent("mock_user_search_error.json");

    List<User> users = new ArrayList<>();
    users.add(generateUser("1234567", "Amy", "Cabble", null));

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(1))
      .body(FAILED_USERS + "[0]." + EXTERNAL_SYSTEM_ID, equalTo(users.get(0).getExternalSystemId()))
      .body(FAILED_USERS + "[0]." + USERNAME, equalTo(users.get(0).getUsername()))
      .body(FAILED_USERS + "[0]." + USER_ERROR_MESSAGE, containsString(
        UserImportAPIConstants.FAILED_TO_PROCESS_USER_SEARCH_RESULT + UserImportAPIConstants.ERROR_MESSAGE
          + UserImportAPIConstants.FAILED_TO_PROCESS_USER_SEARCH_RESPONSE))
      .body(FAILED_USERS, hasSize(1))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserCreationError()  {

    mock.setMockJsonContent("mock_user_creation_error.json");

    List<User> users = new ArrayList<>();
    users.add(generateUser("0000", "Error", "Error", null));

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(1))
      .body(FAILED_USERS + "[0]." + EXTERNAL_SYSTEM_ID, equalTo(users.get(0).getExternalSystemId()))
      .body(FAILED_USERS + "[0]." + USERNAME, equalTo(users.get(0).getUsername()))
      .body(FAILED_USERS + "[0]." + USER_ERROR_MESSAGE, containsString(
        UserImportAPIConstants.FAILED_TO_CREATE_NEW_USER_WITH_EXTERNAL_SYSTEM_ID + users.get(0).getExternalSystemId()))
      .body(FAILED_USERS, hasSize(1))
      .statusCode(200);
  }

  /*
   * This test does not work as expected because the user creation endpoint can only be mocked once in a JSON file.
   * The solution couldtestImportWithUserAddressUpdate be to check the body of the input and decide if the response should be success or failure.
   */
  @Test
  public void testImportWithMoreUserCreation() throws IOException {

    mock.setMockJsonContent("mock_multiple_user_creation.json");

    List<User> users = new ArrayList<>();
    users.add(generateUser("1", "11", "12", null));
    users.add(generateUser("2", "21", "22", null));
    users.add(generateUser("3", "31", "32", null));
    users.add(generateUser("4", "41", "42", null));
    users.add(generateUser("5", "51", "52", null));
    users.add(generateUser("6", "61", "62", null));
    users.add(generateUser("7", "71", "72", null));
    users.add(generateUser("8", "81", "82", null));
    users.add(generateUser("9", "91", "92", null));
    users.add(generateUser("10", "101", "102", null));

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(10);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(10))
      .body(CREATED_RECORDS, equalTo(10))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(0))
      .body(FAILED_USERS, hasSize(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserUpdate()  {

    mock.setMockJsonContent("mock_user_update.json");

    List<User> users = new ArrayList<>();
    User user = generateUser("89101112", "User", "Update", "58512926-9a29-483b-b801-d36aced855d3");
    user.getPersonal().setPreferredFirstName("Preferred User");
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(1))
      .body(FAILED_RECORDS, equalTo(0))
      .body(FAILED_USERS, hasSize(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserUpdateAndWrongSchemaInUserSearchResult() throws IOException {

    mock.setMockJsonContent("mock_user_update_with_wrong_user_schema_in_search_result.json");

    List<User> users = new ArrayList<>();
    users.add(generateUser("89101112", "User", "Update", "58512926-9a29-483b-b801-d36aced855d3"));

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1)
      .withDeactivateMissingUsers(false);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(1))
      .body(FAILED_USERS, hasSize(1))
      .body(FAILED_USERS + "[0]." + EXTERNAL_SYSTEM_ID, equalTo(users.get(0).getExternalSystemId()))
      .body(FAILED_USERS + "[0]." + USERNAME, equalTo(users.get(0).getUsername()))
      .body(FAILED_USERS + "[0]." + USER_ERROR_MESSAGE, equalTo(
        UserImportAPIConstants.FAILED_TO_PROCESS_USER_SEARCH_RESULT + UserImportAPIConstants.USER_SCHEMA_MISMATCH))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserUpdateAndWrongSchemaInUserSearchResultWithDeactivation() throws IOException {

    mock.setMockJsonContent("mock_user_update_with_wrong_user_schema_in_search_result_with_deactivation.json");

    List<User> users = new ArrayList<>();
    users.add(generateUser("89101112", "User", "Update", "58512926-9a29-483b-b801-d36aced855d3"));

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1)
      .withDeactivateMissingUsers(true);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.FAILED_TO_IMPORT_USERS))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(1))
      .body(FAILED_USERS, hasSize(1))
      .body(FAILED_USERS + "[0]." + EXTERNAL_SYSTEM_ID, equalTo(users.get(0).getExternalSystemId()))
      .body(FAILED_USERS + "[0]." + USERNAME, equalTo(users.get(0).getUsername()))
      .body(FAILED_USERS + "[0]." + USER_ERROR_MESSAGE, equalTo(UserImportAPIConstants.USER_SCHEMA_MISMATCH))
      .statusCode(500);
  }

  @Test
  public void testImportWithUserUpdateError() {

    mock.setMockJsonContent("mock_user_update_error.json");

    List<User> users = new ArrayList<>();
    users.add(generateUser("89101112", "User", "Update", "228f3e79-9ebf-47a4-acaa-e8ffdff81ace"));

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(1))
      .body(FAILED_USERS + "[0]." + EXTERNAL_SYSTEM_ID, equalTo(users.get(0).getExternalSystemId()))
      .body(FAILED_USERS + "[0]." + USERNAME, equalTo(users.get(0).getUsername()))
      .body(FAILED_USERS + "[0]." + USER_ERROR_MESSAGE,
        containsString(UserImportAPIConstants.FAILED_TO_UPDATE_USER_WITH_EXTERNAL_SYSTEM_ID + users.get(0).getExternalSystemId()))
      .body(FAILED_USERS, hasSize(1))
      .statusCode(200);
  }

  @Test
  public void testImportWithMoreUserUpdateAndDeactivation() throws IOException {

    mock.setMockJsonContent("mock_user_update_and_deactivation.json");

    List<User> users = new ArrayList<>();
    users.add(generateUser("11", "111", "112", null));
    users.add(generateUser("12", "121", "122", null));
    users.add(generateUser("13", "131", "132", null));
    users.add(generateUser("14", "141", "142", null));
    users.add(generateUser("15", "151", "152", null));
    users.add(generateUser("16", "161", "162", null));
    users.add(generateUser("17", "171", "172", null));
    users.add(generateUser("18", "181", "182", null));
    users.add(generateUser("19", "191", "192", null));
    users.add(generateUser("110", "1101", "1102", null));

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(10)
      .withDeactivateMissingUsers(true)
      .withUpdateOnlyPresentFields(false);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo("Deactivated missing users."))
      .body(TOTAL_RECORDS, equalTo(10))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(10))
      .body(FAILED_RECORDS, equalTo(0))
      .body(FAILED_USERS, hasSize(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithMoreUserUpdate() throws IOException {

    mock.setMockJsonContent("mock_more_user_update.json");

    List<User> users = new ArrayList<>();
    users.add(generateUser("11", "111", "112", null));
    users.add(generateUser("12", "121", "122", null));
    users.add(generateUser("13", "131", "132", null));
    users.add(generateUser("14", "141", "142", null));
    users.add(generateUser("15", "151", "152", null));
    users.add(generateUser("16", "161", "162", null));
    users.add(generateUser("17", "171", "172", null));
    users.add(generateUser("18", "181", "182", null));
    users.add(generateUser("19", "191", "192", null));
    users.add(generateUser("110", "1101", "1102", null));
    users.add(generateUser("11x", "111x", "112x", null));
    users.add(generateUser("12x", "121x", "122x", null));
    users.add(generateUser("13x", "131x", "132x", null));
    users.add(generateUser("14x", "141x", "142x", null));
    users.add(generateUser("15x", "151x", "152x", null));
    users.add(generateUser("16x", "161x", "162x", null));
    users.add(generateUser("17x", "171x", "172x", null));
    users.add(generateUser("18x", "181x", "182x", null));
    users.add(generateUser("19x", "191x", "192x", null));
    users.add(generateUser("110x", "1101x", "1102x", null));

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(10)
      .withDeactivateMissingUsers(true)
      .withUpdateOnlyPresentFields(false);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(20))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(20))
      .body(FAILED_RECORDS, equalTo(0))
      .body(FAILED_USERS, hasSize(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserAddressUpdate() throws IOException {

    mock.setMockJsonContent("mock_import_with_address_update.json");

    List<User> users = new ArrayList<>();
    User user = generateUser("30313233", "User", "Address", "2cbf64a1-5904-4748-ae77-3d0605e911e7");
    Address address = new Address()
      .withAddressLine1("Test first line")
      .withCity("Test city")
      .withRegion("Test region")
      .withPostalCode("12345")
      .withAddressTypeId("Home")
      .withPrimaryAddress(Boolean.FALSE);
    List<Address> addresses = new ArrayList<>();
    addresses.add(address);
    user.getPersonal().setAddresses(addresses);
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1)
      .withUpdateOnlyPresentFields(true);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(1))
      .body(FAILED_RECORDS, equalTo(0))
      .body(FAILED_USERS, hasSize(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithExistingUserAddress() throws IOException {

    mock.setMockJsonContent("mock_import_with_existing_address.json");

    List<User> users = new ArrayList<>();
    User user = generateUser("30313233", "User", "Address", "2cbf64a1-5904-4748-ae77-3d0605e911e7");
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1)
      .withUpdateOnlyPresentFields(true);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(1))
      .body(FAILED_RECORDS, equalTo(0))
      .body(FAILED_USERS, hasSize(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserAddressAdd() throws IOException {

    mock.setMockJsonContent("mock_import_with_address_add.json");

    List<User> users = new ArrayList<>();
    User user = generateUser("30313233", "User", "Address", "2cbf64a1-5904-4748-ae77-3d0605e911e7");
    Address address = new Address()
      .withAddressLine1("Test first line")
      .withCity("Test city")
      .withRegion("Test region")
      .withPostalCode("12345")
      .withAddressTypeId("Home")
      .withPrimaryAddress(Boolean.FALSE);

    Address address2 = new Address()
      .withAddressLine1("Test first line2")
      .withCity("Test city2")
      .withRegion("Test region2")
      .withPostalCode("123452")
      .withAddressTypeId("Home2")
      .withPrimaryAddress(Boolean.FALSE);
    List<Address> addresses = new ArrayList<>();
    addresses.add(address);
    addresses.add(address2);
    user.getPersonal().setAddresses(addresses);
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1)
      .withUpdateOnlyPresentFields(true);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(1))
      .body(FAILED_RECORDS, equalTo(0))
      .body(FAILED_USERS, hasSize(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserAddressRewrite() throws IOException {

    mock.setMockJsonContent("mock_import_with_address_rewrite.json");

    List<User> users = new ArrayList<>();
    User user = generateUser("34353637", "User2", "Address2", "da4106eb-ec94-49ce-8019-9cc89281091c");
    Address address = new Address();
    address.setAddressLine1("Test first line");
    address.setCity("Test city");
    address.setRegion("Test region");
    address.setPostalCode("12345");
    address.setAddressTypeId("Home");
    address.setPrimaryAddress(Boolean.TRUE);
    List<Address> addresses = new ArrayList<>();
    addresses.add(address);
    user.getPersonal().setAddresses(addresses);
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1)
      .withUpdateOnlyPresentFields(false);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(1))
      .body(FAILED_RECORDS, equalTo(0))
      .body(FAILED_USERS, hasSize(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithPrefixedUserCreation() throws IOException {

    mock.setMockJsonContent("mock_prefixed_user_creation.json");

    List<User> users = new ArrayList<>();
    users.add(generateUser("17181920", "Test", "User", null));

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1)
      .withSourceType("test");

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(1))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(0))
      .body(FAILED_USERS, hasSize(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithPrefixedUserUpdate() throws IOException {

    mock.setMockJsonContent("mock_prefixed_user_update.json");

    List<User> users = new ArrayList<>();
    users.add(generateUser("21222324", "User2", "Update2", "a3436a5f-707a-4005-804d-303220dd035b"));

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1)
      .withDeactivateMissingUsers(false)
      .withSourceType("test2");

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(1))
      .body(FAILED_RECORDS, equalTo(0))
      .body(FAILED_USERS, hasSize(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithDeactivateInSourceType() throws IOException {

    mock.setMockJsonContent("mock_deactivate_in_source_type.json");

    List<User> users = new ArrayList<>();
    users.add(generateUser("2526272829", "User2", "Deactivate2", null));

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1)
      .withDeactivateMissingUsers(true)
      .withSourceType("test3");

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo("Deactivated missing users."))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(1))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(0))
      .body(FAILED_USERS, hasSize(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithDeactivateInSourceTypeWithDeactivationError() throws IOException {

    mock.setMockJsonContent("mock_deactivate_in_source_type_with_deactivation_error.json");

    List<User> users = new ArrayList<>();
    users.add(generateUser("2526272829", "User2", "Deactivate2", null));

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1)
      .withDeactivateMissingUsers(true)
      .withSourceType("test3");

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo("Deactivated missing users."))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(1))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(0))
      .body(FAILED_USERS, hasSize(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithNoNeedToDeactivate() throws IOException {

    mock.setMockJsonContent("mock_no_need_to_deactivate.json");

    List<User> users = new ArrayList<>();
    users.add(generateUser("987654321", "User3", "Deactivate3", null));

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1)
      .withDeactivateMissingUsers(true)
      .withSourceType("test4");

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(1))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(0))
      .body(FAILED_USERS, hasSize(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserSearchErrorWhenDeactivating() throws IOException {

    mock.setMockJsonContent("mock_deactivate_search_error.json");

    List<User> users = new ArrayList<>();
    users.add(generateUser("987612345", "User4", "Deactivate4", null));

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1)
      .withDeactivateMissingUsers(true)
      .withSourceType("test5");

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.FAILED_TO_IMPORT_USERS))
      .body(ERROR, containsString(UserImportAPIConstants.FAILED_TO_IMPORT_USERS + UserImportAPIConstants.ERROR_MESSAGE
        + UserImportAPIConstants.FAILED_TO_PROCESS_USER_SEARCH_RESULT))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(1))
      .body(FAILED_USERS + "[0]." + EXTERNAL_SYSTEM_ID, equalTo(users.get(0).getExternalSystemId()))
      .body(FAILED_USERS + "[0]." + USERNAME, equalTo(users.get(0).getUsername()))
      .body(FAILED_USERS + "[0]." + USER_ERROR_MESSAGE, containsString(
        UserImportAPIConstants.FAILED_TO_IMPORT_USERS + UserImportAPIConstants.ERROR_MESSAGE
          + UserImportAPIConstants.FAILED_TO_PROCESS_USER_SEARCH_RESULT))
      .body(FAILED_USERS, hasSize(1))
      .statusCode(500);
  }

  @Test
  public void testImportWithUserCreationErrorWhenDeactivating() throws IOException {

    mock.setMockJsonContent("mock_user_creation_error_when_deactivating.json");

    List<User> users = new ArrayList<>();
    users.add(generateUser("0000", "Error", "Error", null));

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1)
      .withDeactivateMissingUsers(true);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(
        UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY + " " + UserImportAPIConstants.USER_DEACTIVATION_SKIPPED))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(1))
      .body(FAILED_USERS + "[0]." + EXTERNAL_SYSTEM_ID, equalTo(users.get(0).getExternalSystemId()))
      .body(FAILED_USERS + "[0]." + USERNAME, equalTo(users.get(0).getUsername()))
      .body(FAILED_USERS + "[0]." + USER_ERROR_MESSAGE, equalTo(
        UserImportAPIConstants.FAILED_TO_CREATE_NEW_USER_WITH_EXTERNAL_SYSTEM_ID + users.get(0).getExternalSystemId()))
      .body(FAILED_USERS, hasSize(1))
      .statusCode(200);
  }

  @Test
  public void testImportWithServicePointsResponseError() throws IOException {

    mock.setMockJsonContent("mock_service_points_error.json");

    List<User> users = new ArrayList<>();
    users.add(generateUser("1234567", "Amy", "Cabble", null));

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.FAILED_TO_IMPORT_USERS))
      .body(ERROR, containsString(UserImportAPIConstants.FAILED_TO_LIST_SERVICE_POINTS))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(1))
      .body(FAILED_USERS + "[0]." + EXTERNAL_SYSTEM_ID, equalTo(users.get(0).getExternalSystemId()))
      .body(FAILED_USERS + "[0]." + USERNAME, equalTo(users.get(0).getUsername()))
      .body(FAILED_USERS + "[0]." + USER_ERROR_MESSAGE, containsString(UserImportAPIConstants.FAILED_TO_LIST_SERVICE_POINTS))
      .body(FAILED_USERS, hasSize(1))
      .statusCode(500);
  }

  @Test
  public void testImportWithNewPreferenceCreation() throws IOException {
    mock.setMockJsonContent("mock_user_creation_with_new_preference.json");

    List<User> users = new ArrayList<>();
    User user = generateUser("1234567", "Amy", "Cabble", null);
    Address address = new Address()
      .withAddressLine1("Test first line")
      .withCity("Test city")
      .withRegion("Test region")
      .withPostalCode("12345")
      .withAddressTypeId("Returns")
      .withPrimaryAddress(Boolean.FALSE);
    List<Address> addresses = new ArrayList<>();
    addresses.add(address);
    user.getPersonal().setAddresses(addresses);
    user.setRequestPreference(
      new RequestPreference()
        .withHoldShelf(RequestPreference.HoldShelf.TRUE)
        .withDelivery(true)
        .withDefaultServicePointId("59646a99-4074-4ee5-bfd4-86f3fc7717da")
        .withDefaultDeliveryAddressTypeId("Returns")
        .withFulfillment(RequestPreference.Fulfillment.DELIVERY)
    );
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(1))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(0))
      .body(FAILED_USERS, hasSize(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserPreferenceDeliveryIsFalseAndFulfillmentSpecified() throws IOException {
    mock.setMockJsonContent("mock_user_creation_with_new_preference.json");

    List<User> users = new ArrayList<>();
    User user = generateUser("1234567", "Amy", "Cabble", null);
    user.setRequestPreference(
      new RequestPreference()
        .withHoldShelf(RequestPreference.HoldShelf.TRUE)
        .withDelivery(false)
        .withDefaultServicePointId("59646a99-4074-4ee5-bfd4-86f3fc7717da")
        .withDefaultDeliveryAddressTypeId("71628bf4-1962-4dff-a8f2-11108ab532cc")
        .withFulfillment(RequestPreference.Fulfillment.DELIVERY)
    );
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(1))
      .body(FAILED_USERS + "[0]." + EXTERNAL_SYSTEM_ID, equalTo(users.get(0).getExternalSystemId()))
      .body(FAILED_USERS + "[0]." + USERNAME, equalTo(users.get(0).getUsername()))
      .body(FAILED_USERS + "[0]." + USER_ERROR_MESSAGE,
        containsString(UserImportAPIConstants.FAILED_USER_PREFERENCE_VALIDATION + "fulfillment must be not specified"))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserPreferenceDeliveryIsFalseAndAddressTypeSpecified() throws IOException {
    mock.setMockJsonContent("mock_user_creation_with_new_preference.json");

    List<User> users = new ArrayList<>();
    User user = generateUser("1234567", "Amy", "Cabble", null);
    user.setRequestPreference(
      new RequestPreference()
        .withHoldShelf(RequestPreference.HoldShelf.TRUE)
        .withDelivery(false)
        .withDefaultServicePointId("59646a99-4074-4ee5-bfd4-86f3fc7717da")
        .withDefaultDeliveryAddressTypeId("71628bf4-1962-4dff-a8f2-11108ab532cc")
    );
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(1))
      .body(FAILED_USERS + "[0]." + EXTERNAL_SYSTEM_ID, equalTo(users.get(0).getExternalSystemId()))
      .body(FAILED_USERS + "[0]." + USERNAME, equalTo(users.get(0).getUsername()))
      .body(FAILED_USERS + "[0]." + USER_ERROR_MESSAGE, containsString(
        UserImportAPIConstants.FAILED_USER_PREFERENCE_VALIDATION + "defaultDeliveryAddressTypeId must be not specified"))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserPreferenceInvalidDefaultServicePoint() throws IOException {
    mock.setMockJsonContent("mock_user_creation_with_new_preference.json");

    List<User> users = new ArrayList<>();
    User user = generateUser("1234567", "Amy", "Cabble", null);
    user.setRequestPreference(
      new RequestPreference()
        .withHoldShelf(RequestPreference.HoldShelf.TRUE)
        .withDelivery(false)
        .withDefaultServicePointId("59646a99-4074-4ee5-bfd4-86f3fc7717d")
    );
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body("errors.parameters", hasSize(1))
      .statusCode(422);
  }

  @Test
  public void testImportWithUserPreferenceDeliveryIsTrueAndInvalidAddressType() throws IOException {
    mock.setMockJsonContent("mock_user_creation_with_new_preference.json");

    List<User> users = new ArrayList<>();
    User user = generateUser("1234567", "Amy", "Cabble", null);
    user.setRequestPreference(
      new RequestPreference()
        .withHoldShelf(RequestPreference.HoldShelf.TRUE)
        .withDelivery(true)
        .withDefaultServicePointId("59646a99-4074-4ee5-bfd4-86f3fc7717z")
        .withDefaultDeliveryAddressTypeId("Returns")
        .withFulfillment(RequestPreference.Fulfillment.DELIVERY)
    );
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body("errors.parameters", hasSize(1))
      .statusCode(422);
  }

  @Test
  public void testImportWithUserPreferenceDefaultServicePointNotFound() throws IOException {
    mock.setMockJsonContent("mock_user_creation_with_new_preference.json");

    List<User> users = new ArrayList<>();
    User user = generateUser("1234567", "Amy", "Cabble", null);
    user.setRequestPreference(
      new RequestPreference()
        .withHoldShelf(RequestPreference.HoldShelf.TRUE)
        .withDelivery(false)
        .withDefaultServicePointId("00000000-0000-1000-a000-000000000000")
    );
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(FAILED_USERS + "[0]." + EXTERNAL_SYSTEM_ID, equalTo(users.get(0).getExternalSystemId()))
      .body(FAILED_USERS + "[0]." + USERNAME, equalTo(users.get(0).getUsername()))
      .body(FAILED_USERS + "[0]." + USER_ERROR_MESSAGE, containsString(
        UserImportAPIConstants.FAILED_USER_PREFERENCE_VALIDATION + "Provided defaultServicePointId value does not exist"))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserPreferenceDeliveryIsTrueAndAddressTypeNotFound() throws IOException {
    mock.setMockJsonContent("mock_user_creation_with_new_preference.json");

    List<User> users = new ArrayList<>();
    User user = generateUser("1234567", "Amy", "Cabble", null);
    user.setRequestPreference(
      new RequestPreference()
        .withHoldShelf(RequestPreference.HoldShelf.TRUE)
        .withDelivery(true)
        .withDefaultServicePointId("59646a99-4074-4ee5-bfd4-86f3fc7717da")
        .withDefaultDeliveryAddressTypeId("11111111-1111-1111-b111-111111111111")
        .withFulfillment(RequestPreference.Fulfillment.DELIVERY)
    );
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(FAILED_USERS + "[0]." + EXTERNAL_SYSTEM_ID, equalTo(users.get(0).getExternalSystemId()))
      .body(FAILED_USERS + "[0]." + USERNAME, equalTo(users.get(0).getUsername()))
      .body(FAILED_USERS + "[0]." + USER_ERROR_MESSAGE, containsString(
        UserImportAPIConstants.FAILED_USER_PREFERENCE_VALIDATION
          + "Provided defaultDeliveryAddressTypeId value does not exist"))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserPreferenceDeliveryIsTrueAndFulfillmentIsNull() throws IOException {
    mock.setMockJsonContent("mock_user_creation_with_new_preference.json");

    List<User> users = new ArrayList<>();
    User user = generateUser("1234567", "Amy", "Cabble", null);
    user.setRequestPreference(
      new RequestPreference()
        .withHoldShelf(RequestPreference.HoldShelf.TRUE)
        .withDelivery(true)
        .withDefaultServicePointId("59646a99-4074-4ee5-bfd4-86f3fc7717da")
        .withDefaultDeliveryAddressTypeId("71628bf4-1962-4dff-a8f2-11108ab532cc")
        .withFulfillment(null)
    );
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(FAILED_USERS + "[0]." + EXTERNAL_SYSTEM_ID, equalTo(users.get(0).getExternalSystemId()))
      .body(FAILED_USERS + "[0]." + USERNAME, equalTo(users.get(0).getUsername()))
      .body(FAILED_USERS + "[0]." + USER_ERROR_MESSAGE,
        containsString(UserImportAPIConstants.FAILED_USER_PREFERENCE_VALIDATION + "fulfillment must not be null"))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserUpdateAndNewPreferenceCreation() throws IOException {
    mock.setMockJsonContent("mock_user_update_with_new_preference_creation.json");

    List<User> users = new ArrayList<>();
    User user = generateUser("89101112", "User", "Update", "58512926-9a29-483b-b801-d36aced855d3");
    user.setRequestPreference(
      new RequestPreference()
        .withHoldShelf(RequestPreference.HoldShelf.TRUE)
        .withDelivery(false)
        .withDefaultServicePointId("59646a99-4074-4ee5-bfd4-86f3fc7717da")
    );
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(1))
      .body(FAILED_RECORDS, equalTo(0))
      .body(FAILED_USERS, hasSize(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserUpdateAndExistingPreferenceUpdate() throws IOException {
    mock.setMockJsonContent("mock_user_update_with_preference_update.json");

    List<User> users = new ArrayList<>();
    User user = generateUser("89101112", "User", "Update", "58512926-9a29-483b-b801-d36aced855d3");
    Address address = new Address()
      .withAddressLine1("Test first line")
      .withCity("Test city")
      .withRegion("Test region")
      .withPostalCode("12345")
      .withAddressTypeId("Returns")
      .withPrimaryAddress(Boolean.FALSE);
    List<Address> addresses = new ArrayList<>();
    addresses.add(address);
    user.getPersonal().setAddresses(addresses);
    user.setRequestPreference(
      new RequestPreference()
        .withHoldShelf(RequestPreference.HoldShelf.TRUE)
        .withDelivery(true)
        .withDefaultServicePointId("59646a99-4074-4ee5-bfd4-86f3fc7717da")
        .withDefaultDeliveryAddressTypeId("Returns")
        .withFulfillment(RequestPreference.Fulfillment.DELIVERY)
    );
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(1))
      .body(FAILED_RECORDS, equalTo(0))
      .body(FAILED_USERS, hasSize(0))
      .statusCode(200);

  }

  @Test
  public void testImportUserWithNoPreferencesWithUpdateOnlyPresentFieldAndExistingPreferenceNotDelete() throws IOException {
    mock.setMockJsonContent("mock_user_update_with_preference_not_delete.json");

    List<User> users = new ArrayList<>();
    User user = generateUser("89101112", "User", "Update", "58512926-9a29-483b-b801-d36aced855d3");
    Address address = new Address()
      .withAddressLine1("Test first line")
      .withCity("Test city")
      .withRegion("Test region")
      .withPostalCode("12345")
      .withAddressTypeId("Returns")
      .withPrimaryAddress(Boolean.FALSE);
    List<Address> addresses = new ArrayList<>();
    addresses.add(address);
    user.getPersonal().setAddresses(addresses);
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1)
      .withUpdateOnlyPresentFields(true);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(1))
      .body(FAILED_RECORDS, equalTo(0))
      .body(FAILED_USERS, hasSize(0))
      .statusCode(200);
  }

  @Test
  public void testImportUserWithNoPreferencesWithoutUpdateOnlyPresentFieldAndExistingPreferenceDelete() throws IOException {
    mock.setMockJsonContent("mock_user_update_with_preference_delete.json");

    List<User> users = new ArrayList<>();
    User user = generateUser("89101112", "User", "Update", "58512926-9a29-483b-b801-d36aced855d3");
    Address address = new Address()
      .withAddressLine1("Test first line")
      .withCity("Test city")
      .withRegion("Test region")
      .withPostalCode("12345")
      .withAddressTypeId("Returns")
      .withPrimaryAddress(Boolean.FALSE);
    List<Address> addresses = new ArrayList<>();
    addresses.add(address);
    user.getPersonal().setAddresses(addresses);
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1)
      .withUpdateOnlyPresentFields(false);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(1))
      .body(FAILED_RECORDS, equalTo(0))
      .body(FAILED_USERS, hasSize(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserUpdateAndWrongPreferenceAddressType() throws IOException {
    mock.setMockJsonContent("mock_user_update_with_preference_update.json");

    List<User> users = new ArrayList<>();
    User user = generateUser("89101112", "User", "Update", "58512926-9a29-483b-b801-d36aced855d3");
    Address address = new Address()
      .withAddressLine1("Test first line")
      .withCity("Test city")
      .withRegion("Test region")
      .withPostalCode("12345")
      .withAddressTypeId("Returns")
      .withPrimaryAddress(Boolean.FALSE);
    List<Address> addresses = new ArrayList<>();
    addresses.add(address);
    user.getPersonal().setAddresses(addresses);
    user.setRequestPreference(
      new RequestPreference()
        .withHoldShelf(RequestPreference.HoldShelf.TRUE)
        .withDelivery(true)
        .withDefaultServicePointId("59646a99-4074-4ee5-bfd4-86f3fc7717da")
        .withDefaultDeliveryAddressTypeId("Claim")
        .withFulfillment(RequestPreference.Fulfillment.DELIVERY)
    );
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(FAILED_USERS + "[0]." + EXTERNAL_SYSTEM_ID, equalTo(users.get(0).getExternalSystemId()))
      .body(FAILED_USERS + "[0]." + USERNAME, equalTo(users.get(0).getUsername()))
      .body(FAILED_USERS + "[0]." + USER_ERROR_MESSAGE, containsString(
        UserImportAPIConstants.FAILED_USER_PREFERENCE_VALIDATION
          + "Provided defaultDeliveryAddressTypeId value does not exist in user addresses collection"))
      .statusCode(200);
  }

  @Test
  public void testImportWithUserUpdateWithNoPreference() throws IOException {
    mock.setMockJsonContent("mock_user_update_with_no_user_preference.json");

    List<User> users = new ArrayList<>();
    User user = generateUser("89101112", "User", "Update", "58512926-9a29-483b-b801-d36aced855d3");
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(1))
      .body(FAILED_RECORDS, equalTo(0))
      .body(FAILED_USERS, hasSize(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithDepartmentsResponseError() throws IOException {

    mock.setMockJsonContent("mock_departments_error.json");

    List<User> users = new ArrayList<>();
    User user = generateUser("1234567", "Amy", "Cabble", null);
    user.setDepartments(Set.of("Accounting"));
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.FAILED_TO_IMPORT_USERS))
      .body(ERROR, containsString(UserImportAPIConstants.FAILED_TO_LIST_DEPARTMENTS))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(1))
      .body(FAILED_USERS + "[0]." + EXTERNAL_SYSTEM_ID, equalTo(users.get(0).getExternalSystemId()))
      .body(FAILED_USERS + "[0]." + USERNAME, equalTo(users.get(0).getUsername()))
      .body(FAILED_USERS + "[0]." + USER_ERROR_MESSAGE, containsString(UserImportAPIConstants.FAILED_TO_LIST_DEPARTMENTS))
      .body(FAILED_USERS, hasSize(1))
      .statusCode(500);
  }

  @Test
  public void testImportWithDepartmentsOnNewUserCreation() throws IOException {
    mock.setMockJsonContent("mock_user_creation_with_existng_department.json");

    List<User> users = new ArrayList<>();
    User user = generateUser("1234567", "Amy", "Cabble", null);
    user.setDepartments(Set.of("Accounting"));
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(1))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(0))
      .body(FAILED_USERS, hasSize(0))
      .statusCode(200);
  }

  @Test
  public void testImportDepartmentsOnUserUpdate() throws IOException {
    mock.setMockJsonContent("mock_user_update_with_existing_departments.json");

    List<User> users = new ArrayList<>();
    User user = generateUser("89101112", "User", "Update", "58512926-9a29-483b-b801-d36aced855d3");
    user.setDepartments(Set.of("Accounting", "History"));
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(1))
      .body(FAILED_RECORDS, equalTo(0))
      .body(FAILED_USERS, hasSize(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithDepartmentsCreation() throws IOException {
    mock.setMockJsonContent("mock_user_creation_with_new_department.json");

    List<User> users = new ArrayList<>();
    User user = generateUser("1234567", "Amy", "Cabble", null);
    user.setDepartments(Set.of("Financial"));
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withIncluded(new IncludedObjects().withDepartments(
        Set.of(new Department().withName("Financial").withCode("FIN"))
      ))
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(1))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(0))
      .body(FAILED_USERS, hasSize(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithDepartmentsUpdating() throws IOException {
    mock.setMockJsonContent("mock_user_creation_with_update_department.json");

    List<User> users = new ArrayList<>();
    User user = generateUser("1234567", "Amy", "Cabble", null);
    user.setDepartments(Set.of("Financial Accounting"));
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withIncluded(new IncludedObjects().withDepartments(
        Set.of(new Department().withName("Financial Accounting").withCode("ACC"))
      ))
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(1))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(0))
      .body(FAILED_USERS, hasSize(0))
      .statusCode(200);
  }

  @Test
  public void testImportWithDepartmentsThatNotExisted() throws IOException {

    mock.setMockJsonContent("mock_user_creation_with_new_department.json");

    List<User> users = new ArrayList<>();
    User user = generateUser("1234567", "Amy", "Cabble", null);
    user.setDepartments(Set.of("Financial", "Chemistry"));
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(1))
      .body(FAILED_USERS + "[0]." + EXTERNAL_SYSTEM_ID, equalTo(users.get(0).getExternalSystemId()))
      .body(FAILED_USERS + "[0]." + USERNAME, equalTo(users.get(0).getUsername()))
      .body(FAILED_USERS + "[0]." + USER_ERROR_MESSAGE,
        containsString("Departments do not exist in the system: [Chemistry, Financial]"))
      .body(FAILED_USERS, hasSize(1))
      .statusCode(200);
  }

  @Test
  public void testImportUsersWithExistedCustomFieldOptions() throws IOException {
    mock.setMockJsonContent("mock_user_creation_with_custom_fields.json");

    List<User> users = new ArrayList<>();
    User user = generateUser("1234567", "Amy", "Cabble", null)
      .withCustomFields(new CustomFields().withAdditionalProperty("department_1", "Design"));
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(1))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(0))
      .body(FAILED_USERS, hasSize(0))
      .statusCode(200);
  }

  @Test
  public void testImportUsersWithNotExistedCustomFieldOptions() throws IOException {
    mock.setMockJsonContent("mock_user_creation_with_custom_fields.json");

    List<User> users = new ArrayList<>();
    User user = generateUser("1234567", "Amy", "Cabble", null)
      .withCustomFields(new CustomFields().withAdditionalProperty("department_1", "Development"));
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(1))
      .body(FAILED_USERS, hasSize(1))
      .body(FAILED_USERS + "[0]." + EXTERNAL_SYSTEM_ID, equalTo(users.get(0).getExternalSystemId()))
      .body(FAILED_USERS + "[0]." + USERNAME, equalTo(users.get(0).getUsername()))
      .body(FAILED_USERS + "[0]." + USER_ERROR_MESSAGE,
        containsString("Custom field's options do not exist in the system: [refId = department_1, options: [Development]]."))
      .statusCode(200);
  }

  @Test
  public void testImportUsersWithMultiFieldOptionsOneOfIsNotExist() throws IOException {
    mock.setMockJsonContent("mock_user_creation_with_multi_custom_fields.json");

    String refId = "department_1";
    List<User> users = new ArrayList<>();
    User user = generateUser("1234567", "Amy", "Cabble", null)
      .withCustomFields(new CustomFields().withAdditionalProperty(refId,
        Arrays.asList("Development", "Design")));
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1)
      .withIncluded(new IncludedObjects()
        .withCustomFields(Set.of(new CustomField()
            .withRefId(refId)
            .withSelectField(new SelectField()
              .withOptions(new SelectFieldOptions()
                .withValues(
                  List.of(new SelectFieldOption().withValue("Design"), new SelectFieldOption().withValue("Development"))
                )
              )
            )
          )
        )
      );

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.USERS_WERE_IMPORTED_SUCCESSFULLY))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(1))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(0))
      .body(FAILED_USERS, hasSize(0))
      .statusCode(200);
  }

  @Test
  public void testImportUsersWithCustomFieldOptionsWithRequestError() throws IOException {
    mock.setMockJsonContent("mock_user_creation_with_custom_fields_failed_get.json");

    List<User> users = new ArrayList<>();
    User user = generateUser("1234567", "Amy", "Cabble", null)
      .withCustomFields(new CustomFields().withAdditionalProperty("department_1",
        Arrays.asList("Development", "Design")));
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1);

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.FAILED_TO_IMPORT_USERS))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(1))
      .body(FAILED_USERS, hasSize(1))
      .body(FAILED_USERS + "[0]." + USER_ERROR_MESSAGE, containsString(FAILED_TO_LIST_CUSTOM_FIELDS))
      .statusCode(500);
  }

  @Test
  public void testImportUsersWithCustomFieldAndTryingUpdateNotExistedCustomField() throws IOException {
    mock.setMockJsonContent("mock_user_update_not_existed_custom_field.json");

    List<User> users = new ArrayList<>();
    String refId = "department_1";
    User user = generateUser("1234567", "Amy", "Cabble", null)
      .withCustomFields(new CustomFields().withAdditionalProperty(refId,
        Collections.singletonList("Development")));
    users.add(user);

    UserdataimportCollection collection = new UserdataimportCollection()
      .withUsers(users)
      .withTotalRecords(1)
      .withIncluded(new IncludedObjects()
        .withCustomFields(Set.of(new CustomField()
            .withRefId(refId)
            .withSelectField(new SelectField()
              .withOptions(new SelectFieldOptions()
                .withValues(
                  List.of(new SelectFieldOption().withValue("Design"), new SelectFieldOption().withValue("Development"))
                )
              )
            )
          )
        )
      );

    given()
      .header(TENANT_HEADER)
      .header(TOKEN_HEADER)
      .header(new Header(XOkapiHeaders.URL, getOkapiUrl()))
      .header(JSON_CONTENT_TYPE_HEADER)
      .body(collection)
      .post(USER_IMPORT)
      .then()
      .body(MESSAGE, equalTo(UserImportAPIConstants.FAILED_TO_IMPORT_USERS))
      .body(TOTAL_RECORDS, equalTo(1))
      .body(CREATED_RECORDS, equalTo(0))
      .body(UPDATED_RECORDS, equalTo(0))
      .body(FAILED_RECORDS, equalTo(1))
      .body(FAILED_USERS, hasSize(1))
      .body(FAILED_USERS + "[0]." + USER_ERROR_MESSAGE,
        containsString("Custom fields do not exist in the system: [department_1]."))
      .statusCode(500);
  }

  private String getOkapiUrl() {
    return HOST + ":" + MOCK_PORT;
  }
}
