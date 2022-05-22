package com.polywoof;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import okhttp3.*;
import org.apache.commons.text.StringEscapeUtils;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.util.LinkedList;

@Slf4j
@ParametersAreNonnullByDefault
public class PolywoofTranslator
{
	private static final LinkedList<Language> support = new LinkedList<>();
	private final OkHttpClient client;
	private String URL;
	private String token;

	public PolywoofTranslator(OkHttpClient client, String token)
	{
		this.client = client;

		this.update(token);
	}

	public static Language language(String target)
	{
		for(Language language : support)
			if(language.code.equalsIgnoreCase(target) || language.name.toLowerCase().startsWith(target.toLowerCase()))
				return language;

		return null;
	}

	public static String stripTags(String text)
	{
		return text.replaceAll("<br>", " ").replaceAll("<.*?>", "").replaceAll("[ ]{2,}", " ").trim();
	}

	private static void handleError(int code) throws IOException
	{
		switch(code)
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

	private void post(String path, RequestBody request, Receive callback)
	{
		if(token.isEmpty())
			return;

		try
		{
			Request headers = new Request.Builder()
					.addHeader("User-Agent", RuneLite.USER_AGENT + " (polywoof)")
					.addHeader("Authorization", "DeepL-Auth-Key " + token)
					.addHeader("Accept", "application/json")
					.addHeader("Content-Type", "application/x-www-form-urlencoded")
					.addHeader("Content-Length", String.valueOf(request.contentLength()))
					.url(URL + path)
					.post(request)
					.build();

			client.newCall(headers).enqueue(new Callback()
			{
				@Override
				public void onFailure(Call call, IOException error)
				{
					error.printStackTrace();
				}

				@Override
				public void onResponse(Call call, Response response) throws IOException
				{
					try(ResponseBody body = response.body())
					{
						if(body == null)
							return;

						handleError(response.code());
						callback.receive(body.string());
					}
				}
			});
		}
		catch(IOException error)
		{
			error.printStackTrace();
		}
	}

	public void translate(String text, @Nullable Language target_lang, PolywoofDB db, Translate callback)
	{
		if(text.isEmpty() || target_lang == null)
			return;

		if(!db.status())
		{
			translate(text, target_lang, callback);
			return;
		}

		db.select(text, target_lang, string ->
		{
			if(string == null)
				translate(text, target_lang, insert ->
				{
					db.insert(text, insert, target_lang);
					callback.translate(insert);
				});
			else
				callback.translate(string);
		});
	}

	public void translate(String text, @Nullable Language target_lang, Translate callback)
	{
		String string = stripTags(text);

		if(string.isEmpty() || target_lang == null)
			return;

		RequestBody request = new FormBody.Builder()
				.add("text", string)
				.add("target_lang", target_lang.code)
				.add("source_lang", "en")
				.add("preserve_formatting", "1")
				.add("tag_handling", "html")
				.add("non_splitting_tags", "br")
				.build();

		post("/v2/translate", request, body ->
		{
			JsonParser parser = new JsonParser();
			JsonElement root = parser.parse(body);
			JsonObject json = root.getAsJsonObject();

			StringBuilder output = new StringBuilder();

			for(JsonElement element : json.getAsJsonArray("translations"))
				output.append(StringEscapeUtils.unescapeHtml4(element.getAsJsonObject().get("text").getAsString()));

			callback.translate(output.toString());
		});
	}

	public void usage(Usage callback)
	{
		post("/v2/usage", new FormBody.Builder().build(), body ->
		{
			JsonParser parser = new JsonParser();
			JsonElement root = parser.parse(body);
			JsonObject json = root.getAsJsonObject();

			callback.usage(json.get("character_count").getAsLong(), json.get("character_limit").getAsLong());
		});
	}

	public void languages(String type, Languages callback)
	{
		post("/v2/languages", new FormBody.Builder().add("type", type).build(), body ->
		{
			JsonParser parser = new JsonParser();
			JsonElement root = parser.parse(body);
			JsonArray json = root.getAsJsonArray();

			LinkedList<Language> output = new LinkedList<>();

			for(JsonElement element : json)
				output.add(new Language(element.getAsJsonObject()));

			support.clear();
			support.addAll(output);

			callback.languages(output);
		});
	}

	public void update(String token)
	{
		this.URL = token.endsWith(":fx") ? "https://api-free.deepl.com" : "https://api.deepl.com";
		this.token = token;
	}

	interface Receive
	{
		void receive(String body);
	}

	interface Translate
	{
		void translate(String text);
	}

	interface Usage
	{
		void usage(long character_count, long character_limit);
	}

	interface Languages
	{
		void languages(LinkedList<Language> support);
	}

	public static class Language
	{
		public final String code;
		public final String name;

		public Language(JsonObject object)
		{
			this.code = object.get("language").getAsString();
			this.name = object.get("name").getAsString();
		}
	}
}
