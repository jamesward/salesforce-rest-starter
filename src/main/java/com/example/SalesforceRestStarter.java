package com.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SalesforceRestStarter {

    private static final String TOKEN_URL =  "https://login.salesforce.com/services/oauth2/token";

    public static void main(String[] args) throws Exception {

        String username;
        String password;
        String consumerKey;
        String consumerSecret;

        if (args.length == 4) {
            username = args[0];
            password = args[1];
            consumerKey = args[2];
            consumerSecret = args[3];
        }
        else if (System.console() != null) {
            System.out.print("Salesforce Username: ");
            username = System.console().readLine();

            System.out.print("Salesforce Password: ");
            password = new String(System.console().readPassword());

            System.out.print("Salesforce Consumer Key: ");
            consumerKey = System.console().readLine();

            System.out.print("Salesforce Consumer Secret: ");
            consumerSecret = new String(System.console().readPassword());
        }
        else {
            throw new Exception("You need to specify username, password, consumer key, and consumer secret");
        }

        try {
            // login
            final CloseableHttpClient httpclient = HttpClients.createDefault();

            final List<NameValuePair> loginParams = new ArrayList<NameValuePair>();
            loginParams.add(new BasicNameValuePair("client_id", consumerKey));
            loginParams.add(new BasicNameValuePair("client_secret", consumerSecret));
            loginParams.add(new BasicNameValuePair("grant_type", "password"));
            loginParams.add(new BasicNameValuePair("username", username));
            loginParams.add(new BasicNameValuePair("password", password));

            final HttpPost post = new HttpPost(TOKEN_URL);
            post.setEntity(new UrlEncodedFormEntity(loginParams));

            final HttpResponse loginResponse = httpclient.execute(post);

            // parse
            final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

            final JsonNode loginResult = mapper.readValue(loginResponse.getEntity().getContent(), JsonNode.class);
            final String accessToken = loginResult.get("access_token").asText();
            final String instanceUrl = loginResult.get("instance_url").asText();

            // query contacts
            final URIBuilder builder = new URIBuilder(instanceUrl);
            builder.setPath("/services/data/v39.0/query/").setParameter("q", "SELECT Id, Name FROM Contact");

            final HttpGet get = new HttpGet(builder.build());
            get.setHeader("Authorization", "Bearer " + accessToken);

            final HttpResponse queryResponse = httpclient.execute(get);

            final JsonNode queryResults = mapper.readValue(queryResponse.getEntity().getContent(), JsonNode.class);

            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(queryResults));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

}
