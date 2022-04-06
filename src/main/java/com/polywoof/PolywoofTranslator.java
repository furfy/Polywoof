package com.polywoof;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

@Slf4j

public class PolywoofTranslator
{
    private final String url;
    private final String token;
    private final String jsonArray;
    private final String jsonString;

    private final OkHttpClient client;

    private static final Map<String, Map<String, String>> cache = new HashMap<>();

    interface Translation
    {
        void translation(String target);
    }

    public PolywoofTranslator(String url, String token, String jsonArray, String jsonString, OkHttpClient client)
    {
        this.client = client;
        this.url = url;
        this.token = token;
        this.jsonArray = jsonArray;
        this.jsonString = jsonString;
    }

    public void translate(String origin, String target, Translation callback)
    {
        if(token.isEmpty()) return;

        if(cache.containsKey(origin) && cache.get(origin).containsKey(target))
        {
            callback.translation(cache.get(origin).get(target));
            return;
        }

        try
        {
            Request request = new Request.Builder().url(String.format(url, URLEncoder.encode(token, "UTF-8"), URLEncoder.encode(origin, "UTF-8"), URLEncoder.encode(target, "UTF-8"))).build();
            Call call = client.newCall(request);

            call.enqueue(new Callback()
            {
                @Override
                public void onFailure(Call call, IOException error)
                {
                    error.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException
                {
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
                    callback.translation(cache.get(origin).get(target));
                }
            });
        }
        catch (IOException error)
        {
            error.printStackTrace();
        }
    }

    public String stripTags(String text)
    {
        return text.replaceAll("<br>", " ").replaceAll("<.*?>", "");
    }
}
