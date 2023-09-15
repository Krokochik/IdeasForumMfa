package com.krokochik.ideasforummfa.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.krokochik.ideasforummfa.model.Request;
import com.krokochik.ideasforummfa.model.Response;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.HttpsURLConnection;

import lombok.SneakyThrows;

public class HttpRequestsAddresser {

    @SneakyThrows
    @Contract("_,_,_ -> !null")
    public Response sendRequest(@Nullable JsonObject body, URL url, Request.Method method) {
        Response response = new Response();
        HttpsURLConnection connection = ((HttpsURLConnection) url.openConnection());

        connection.setRequestMethod(method.name());


        if (method.equals(Request.Method.POST) && body != null) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(body.toString().getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            outputStream.close();
        }

        response.setCode((short) connection.getResponseCode());

        /*
         *  com.android.okhttp.internal.huc.HttpURLConnectionImpl.getInputStream()
         *  may produce java.io.FileNotFoundException
         *  if the server is absolutely shutdown
         * */
        try {
            // read response
            BufferedReader reader;
            if (connection.getResponseCode() >= 400) {
                reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            } else {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            }
            StringBuilder responseBody = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                responseBody.append(line);
            }
            reader.close();

            try {
                response.setBody(new Gson().fromJson(responseBody.toString(), HashMap.class));
            } catch (JsonSyntaxException e) {
                throw new Exception("Unexpected server answer.");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        System.out.println(response.toString());
        return response;
    }

    @SneakyThrows
    @Contract("_ -> !null")
    public Response sendRequest(Request request) {
        AtomicReference<JsonObject> body = new AtomicReference<>(null);
        if (request.getBody() != null) {
            body.set(new JsonObject());
            request.getBody().forEach((k, v) -> {
                body.get().add(k, new JsonPrimitive(v));
            });
        }
        return sendRequest(body.get(), request.getUrl(), request.getMethod());
    }
}
