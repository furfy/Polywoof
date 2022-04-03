package com.polywoof;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

@Slf4j

public class PolywoofThread extends Thread
{
    private final String origin;
    private final String target;
    private final Translation callback;
    private final OkHttpClient client;

    private final String url;
    private final String token;
    private final String jsonArray;
    private final String jsonString;

    private static final Map<String, Map<String, String>> cache = new HashMap<>();

    interface Translation
    {
        void translation(String target);
    }

    public PolywoofThread(String origin, String target, Translation callback, OkHttpClient client, String url, String token, String jsonArray, String jsonString)
    {
        super();

        this.origin = origin.replace("<br>", "\n");
        this.target = target;
        this.callback = callback;
        this.client = client;

        this.url = url;
        this.token = token;
        this.jsonArray = jsonArray;
        this.jsonString = jsonString;

        this.start();
    }

    @Override
    public void run()
    {
        if(token.equals("")) return;

        if(!cache.containsKey(origin) || !cache.get(origin).containsKey(target))
        {
            try
            {
                Request request = new Request.Builder().url(String.format(url, URLEncoder.encode(token, "UTF-8"), URLEncoder.encode(origin, "UTF-8"), URLEncoder.encode(target, "UTF-8"))).build();
                Response response = client.newCall(request).execute();

                JsonParser parser = new JsonParser();
                JsonElement root = parser.parse(response.body().string());
                JsonObject json = root.getAsJsonObject();

                StringBuilder output = new StringBuilder();

                for(JsonElement element : json.getAsJsonArray(jsonArray))
                {
                    output.append(element.getAsJsonObject().get(jsonString).getAsString());
                }

                if(!cache.containsKey(origin)) cache.put(origin, new HashMap<>());

                cache.get(origin).put(target, output.toString());
            }
            catch (Exception error)
            {
                error.printStackTrace();

                return;
            }
        }

        callback.translation(cache.get(origin).get(target));
    }
}
