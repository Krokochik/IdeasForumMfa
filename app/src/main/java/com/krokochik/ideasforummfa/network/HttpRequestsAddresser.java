package com.krokochik.ideasforummfa.network;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.krokochik.ideasforummfa.model.Request;
import com.krokochik.ideasforummfa.model.Response;
import com.krokochik.ideasforummfa.resources.GS;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import javax.net.ssl.HttpsURLConnection;

import lombok.SneakyThrows;

public class HttpRequestsAddresser {

    @SneakyThrows
    public Response sendRequest(Request request) {
        URL requestUrl = new URL(GS.L_SERVER_ENDPOINT + "/" +
                request.getEndpoint().replaceAll("/", ""));
        Response response = new Response();

        HttpsURLConnection connection = ((HttpsURLConnection) requestUrl.openConnection());

        connection.setRequestMethod(request.getMethod().toString());

        if (request.getMethod().equals(Request.Method.POST)) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(new Gson().toJson(request.getBody()).getBytes(StandardCharsets.UTF_8));
            System.out.println(new Gson().toJson(request.getBody(), HashMap.class));
            outputStream.flush();
            outputStream.close();
        }

        response.setCode(connection.getResponseCode());

        // read response
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder responseBody = new StringBuilder();

        String line;
        while ((line = reader.readLine()) != null) {
            responseBody.append(line);
        } reader.close();

        System.out.println(responseBody);
        try {
            response.setBody(new Gson().fromJson(responseBody.toString(), HashMap.class));
        } catch (JsonSyntaxException e) {
            throw new UnknownError("Unexpected server answer.");
        }

        return response;
    }
}
