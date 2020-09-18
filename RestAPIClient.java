import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.HttpStatus;
import org.apache.http.util.EntityUtils;
import org.apache.http.client.ClientProtocolException;
import java.io.IOException;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONTokener;
import org.json.JSONException;

public class RestAPIClient {

    private static final String LOGINURL = "https: //login.salesforce.com";
    private static final String GRANTTYPE = "/services/oauth2/token?grant_type = password";
    private static final String CLIENTID = "ConsumerKey";
    private static final String CLIENTSECRET = "ConsumerSecret";
    private static final String USERID = "UserName";
    private static final String PASSWORD = "PasswordandSecurityToken";
    private static final String ACCESSTOKEN = "access_token";
    private static final String INSTANCEURL = "instance_url";

    private static String instanceUrl;
    private static Header oAuthHeader;
    private static Header printHeader = new BasicHeader("X - PrettyPrint", "1");
    private static String caseId;
    private static String caseNumber;
    private static String caseSubject;
    private static String caseStatus;
    private static String caseOrigin;
    private static String casePriority;

    public static void main(String[] args) {

        HttpClient httpclient = HttpClientBuilder.create().build();

        String loginURL = LOGINURL + GRANTTYPE + " & client_id = "+CLIENTID + " & client_secret = "+CLIENTSECRET + " & username = "+USERID + " & password = "+PASSWORD;

        HttpPost httpPost = new HttpPost(loginURL);
        HttpResponse httpResponse = null;

        try {
            httpResponse = httpclient.execute(httpPost);
        } catch (ClientProtocolException clientProtocolException) {
            clientProtocolException.printStackTrace();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        final int statusCode = httpResponse.getStatusLine().getStatusCode();
        if (statusCode != HttpStatus.SC_OK) {
            System.out.println("Error authenticating to Salesforce.com platform: "+statusCode);
            return;
        }

        String httpMessage = null;
        try {
            httpMessage = EntityUtils.toString(httpResponse.getEntity());
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

        JSONObject jsonObject = null;
        String accessToken = null;
        try {
            jsonObject = (JSONObject) new JSONTokener(httpMessage).nextValue();
            accessToken = jsonObject.getString(ACCESSTOKEN);
            instanceUrl = jsonObject.getString(INSTANCEURL);
        } catch (JSONException jsonException) {
            jsonException.printStackTrace();
        }

        oAuthHeader = new BasicHeader("Authorization", "OAuth" + accessToken);

        getCases();
        createCase();

        httpPost.releaseConnection();
    }

    public static void getCases() {

        try {

            HttpClient httpClient = HttpClientBuilder.create().build();

            String finalURI = instanceUrl + "/services/data/v38.0/query?q=Select+Id+,+CaseNumber+,+Subject+,+Status+,+Origin+,+Priority+From+Case+Limit+10";
            System.out.println("Query URL: "+finalURI);
            HttpGet httpGet = new HttpGet(finalURI);
            httpGet.addHeader(oAuthHeader);
            httpGet.addHeader(printHeader);

            HttpResponse httpResponse = httpClient.execute(httpGet);

            int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                String responseString = EntityUtils.toString(httpResponse.getEntity());
                try {
                    JSONObject jsonObject = new JSONObject(responseString);
                    System.out.println("JSON result of Query: \n" + jsonObject.toString(1));
                    JSONArray jsonArray = jsonObject.getJSONArray("records");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        caseId = jsonObject.getJSONArray("records").getJSONObject(i).getString("Id");
                        caseNumber = jsonObject.getJSONArray("records").getJSONObject(i).getString("CaseNumber");
                        caseSubject = jsonObject.getJSONArray("records").getJSONObject(i).getString("Subject");
                        caseStatus = jsonObject.getJSONArray("records").getJSONObject(i).getString("Status");
                        caseOrigin = jsonObject.getJSONArray("records").getJSONObject(i).getString("Origin");
                        casePriority = jsonObject.getJSONArray("records").getJSONObject(i).getString("Priority");
                        //Since the values are available, can be used later to create objects.
                    }
                } catch (JSONException jsonException) {
                    jsonException.printStackTrace();
                }
            } else {
                System.out.print("Query was unsuccessful.Status code returned is" + statusCode);
                System.out.println(httpResponse.getEntity().getContent());
                System.exit(-1);
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public static void createCase() {

        String finalURI = instanceUrl + "/services/apexrest/Cases/";
        try {

            JSONObject newCase = new JSONObject();
            newCase.put("subject", "Smallfoot Sighting!");
            newCase.put("status", "New");
            newCase.put("origin", "Phone");
            newCase.put("priority", "Low");

            System.out.println("JSON for case record to be inserted:\n" + newCase.toString(1));

            HttpClient httpClient = HttpClientBuilder.create().build();

            HttpPost httpPost = new HttpPost(finalURI);
            httpPost.addHeader(oAuthHeader);
            httpPost.addHeader(printHeader);
            StringEntity entityBody = new StringEntity(newCase.toString(1));
            entityBody.setContentType("application / json");
            httpPost.setEntity(entityBody);

            HttpResponse httpResponse = httpClient.execute(httpPost);

            int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                String responseString = EntityUtils.toString(httpResponse.getEntity());
                caseId = responseString;
                System.out.println("New Case Id from response: "+caseId);
            } else {
                System.out.println("Insertion unsuccessful.Status code returned is" + statusCode);
            }
        } catch (JSONException jsonException) {
            System.out.println("Issue creating JSON or processing results");
            jsonException.printStackTrace();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

}
