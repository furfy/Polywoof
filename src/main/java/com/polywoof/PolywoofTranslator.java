package com.polywoof;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import okhttp3.*;
import okhttp3.internal.annotations.EverythingIsNonNull;
import org.apache.commons.text.StringEscapeUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j

public class PolywoofTranslator
{
	private static final Map<String, Map<String, String>> cache = new HashMap<>();
	private final String URL;
	private final String token;
	private final OkHttpClient client;

	public PolywoofTranslator(OkHttpClient client, String token)
	{
		this.client = client;
		this.token = token;
		this.URL = token.endsWith(":fx") ? "https://api-free.deepl.com" : "https://api.deepl.com";
	}

	private void post(String path, RequestBody body, Callback callback) throws IOException
	{
		if(token.length() == 0) return;

		Request request = new Request.Builder()
				.addHeader("User-Agent", RuneLite.USER_AGENT + " (polywoof)")
				.addHeader("Authorization", "DeepL-Auth-Key " + token)
				.addHeader("Accept", "application/json")
				.addHeader("Content-Type", "application/x-www-form-urlencoded")
				.addHeader("Content-Length", String.valueOf(body.contentLength()))
				.url(URL + path).post(body).build();

		client.newCall(request).enqueue(callback);
	}

	private void handleError(int code) throws IOException
	{
		switch (code)
		{
			case 200:
				return;
			case 400:
				throw new IOException("Bad request");
			case 403:
				throw new IOException("Authorization failed");
			case 404:
				throw new IOException("The requested resource could not be found");
			case 413:
				throw new IOException("The request size exceeds the limit");
			case 414:
				throw new IOException("The request URL is too long");
			case 429:
				throw new IOException("Too many requests");
			case 456:
				throw new IOException("Quota exceeded");
			case 503:
				throw new IOException("Resource currently unavailable");
			default:
				throw new IOException("Internal error");
		}
	}

	public void translate(String text, String target_lang, Translate callback)
	{
		if(text.length() == 0 || target_lang.length() == 0) return;

		if(cache.containsKey(text) && cache.get(text).containsKey(target_lang))
		{
			callback.translate(cache.get(text).get(target_lang));
			return;
		}

		try
		{
			RequestBody body = new FormBody.Builder()
					.add("text", text)
					.add("target_lang", target_lang)
					.add("source_lang", "en")
					.add("preserve_formatting", "1")
					.add("tag_handling", "html")
					.add("non_splitting_tags", "br")
					.build();

			post("/v2/translate", body, new Callback()
			{
				@Override
				@EverythingIsNonNull
				public void onFailure(Call call, IOException error)
				{
					error.printStackTrace();
				}

				@Override
				@EverythingIsNonNull
				public void onResponse(Call call, Response response) throws IOException
				{
					handleError(response.code());

					JsonParser parser = new JsonParser();
					JsonElement root = parser.parse(response.body().string());
					JsonObject json = root.getAsJsonObject();

					StringBuilder output = new StringBuilder();

					for(JsonElement element : json.getAsJsonArray("translations"))
					{
						output.append(StringEscapeUtils.unescapeHtml4(element.getAsJsonObject().get("text").getAsString()));
					}

					if(!cache.containsKey(text)) cache.put(text, new HashMap<>());

					cache.get(text).put(target_lang, output.toString());
					callback.translate(cache.get(text).get(target_lang));
				}
			});
		}
		catch (IOException error)
		{
			error.printStackTrace();
		}
	}

	public void usage(Usage callback)
	{
		try
		{
			post("/v2/usage", new FormBody.Builder().build(), new Callback()
			{
				@Override
				@EverythingIsNonNull
				public void onFailure(Call call, IOException error)
				{
					error.printStackTrace();
				}

				@Override
				@EverythingIsNonNull
				public void onResponse(Call call, Response response) throws IOException
				{
					handleError(response.code());

					JsonParser parser = new JsonParser();
					JsonElement root = parser.parse(response.body().string());
					JsonObject json = root.getAsJsonObject();

					callback.usage(json.get("character_count").getAsLong(), json.get("character_limit").getAsLong());
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
		return text.replaceAll("[ ]*<br>[ ]*", " ").replaceAll("<.*?>", "");
	}

	interface Translate
	{
		void translate(String text);
	}

	interface Usage
	{
		void usage(long character_count, long character_limit);
	}
}
