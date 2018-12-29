package com.iota.iri.utils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.slf4j.Logger; //logger
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;

public class SlackBotFeed {

    private SlackBotFeed() {
        throw new IllegalStateException("SlackBotFeed class");
    }

    public static void reportToSlack(final String message) {

        boolean ritorno = true;
        try {

            final String request = "token=" 
                    + URLEncoder.encode("<botToken>", "UTF-8") + "&channel=" 
                    + URLEncoder.encode("#botbox", "UTF-8") + "&text=" + URLEncoder.encode(message, "UTF-8") + "&as_user=true";

            final HttpURLConnection connection = (HttpsURLConnection) (new URL("https://slack.com/api/chat.postMessage")).openConnection();
            ((HttpsURLConnection)connection).setHostnameVerifier((hostname, session) -> ritorno);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            OutputStream out = connection.getOutputStream();
            out.write(request.getBytes("UTF-8"));
            out.close();
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            InputStream inputStream = connection.getInputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }

        } catch (final Exception e) {

            log.info("Got you", e);
        }
    }
    private static final Logger log = LoggerFactory.getLogger(SlackBotFeed.class);
}
