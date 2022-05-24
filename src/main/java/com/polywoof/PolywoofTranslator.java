package com.polywoof;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import okhttp3.*;
import org.apache.commons.text.StringEscapeUtils;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@ParametersAreNonnullByDefault
public class PolywoofTranslator
{
	private static final JsonParser parser = new JsonParser();
	private static final List<Language> trusted = new ArrayList<>(30);
	private static final List<Language> offline = languageLoader(PolywoofPlugin.class, "/languages.json");
	private final OkHttpClient client;
	private String URL;
	private String key;

	public PolywoofTranslator(OkHttpClient client, String auth)
	{
		this.client = client;
		this.update(auth);
	}

	public static List<Language> languageLoader(Class<?> c, String resource)
	{
		try(InputStream stream = c.getResourceAsStream(resource))
		{
			if(stream == null)
				throw new Exception("Failed to load languages from the resource");

			JsonArray json = parser.parse(new InputStreamReader(stream)).getAsJsonArray();
			List<Language> output = new ArrayList<>(json.size());

			for(JsonElement element : json)
				output.add(new OfflineLanguage(element.getAsJsonObject().get("language").getAsString(), element.getAsJsonObject().get("name").getAsString()));

			return output;
		}
		catch(Exception error)
		{
			error.printStackTrace();
			return new ArrayList<>(0);
		}
	}

	public static Language languageFinder(String search)
	{
		String string = search.trim().toUpperCase();

		for(Language language : trusted)
			if(language.code.equalsIgnoreCase(string))
				return language;

		for(Language language : trusted)
			if(language.name.toUpperCase().startsWith(string))
				return language;

		for(Language language : offline)
			if(language.code.equalsIgnoreCase(string))
				return language;

		for(Language language : offline)
			if(language.name.toUpperCase().startsWith(string))
				return language;

		return new UnknownLanguage(string);
	}

	private static void handleCode(int code) throws Exception
	{
		switch(code)
		{
			case 200:
				return;
			case 400:
				throw new Exception("Bad request");
			case 403:
				throw new Exception("Authorization failed");
			case 404:
				throw new Exception("The requested resource could not be found");
			case 413:
				throw new Exception("The request size exceeds the limit");
			case 414:
				throw new Exception("The request URL is too long");
			case 429:
				throw new Exception("Too many requests");
			case 456:
				throw new Exception("Quota exceeded");
			case 503:
				throw new Exception("Resource currently unavailable");
			default:
				throw new Exception("Internal error");
		}
	}

	private void post(String path, RequestBody request, Receive callback)
	{
		if(key.isEmpty())
			return;

		try
		{
			Request headers = new Request.Builder()
				.addHeader("User-Agent", RuneLite.USER_AGENT + " (polywoof)")
				.addHeader("Authorization", "DeepL-Auth-Key " + key)
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
				public void onResponse(Call call, Response response)
				{
					try(ResponseBody body = response.body())
					{
						handleCode(response.code());

						if(body == null)
							return;

						callback.receive(body.string());
					}
					catch(Exception error)
					{
						error.printStackTrace();
					}
				}
			});
		}
		catch(IOException error)
		{
			error.printStackTrace();
		}
	}

	public void translate(String text, Language language, PolywoofDB db, Translate callback)
	{
		if(text.isEmpty() || language instanceof UnknownLanguage)
			return;

		if(!db.status())
		{
			translate(text, language, callback);
			return;
		}

		db.select(text, language, string ->
		{
			if(string == null)
				translate(text, language, insert ->
				{
					db.insert(text, insert, language, () -> log.info("[{}] INSERT", language.code));
					callback.translate(insert);
				});
			else
			{
				log.info("[{}] SELECT", language.code);
				callback.translate(string);
			}
		});
	}

	public void translate(String text, Language language, Translate callback)
	{
		String string = PolywoofFormatter.filter(text);

		if(string.isEmpty() || !(language instanceof TrustedLanguage))
			return;

		RequestBody request = new FormBody.Builder()
			.add("text", string)
			.add("target_lang", language.code)
			.add("source_lang", "en")
			.add("preserve_formatting", "1")
			.add("tag_handling", "html")
			.add("non_splitting_tags", "br")
			.build();

		post("/v2/translate", request, body ->
		{
			JsonObject json = parser.parse(body).getAsJsonObject();
			StringBuilder output = new StringBuilder(100);

			for(JsonElement element : json.getAsJsonArray("translations"))
				output.append(StringEscapeUtils.unescapeHtml4(element.getAsJsonObject().get("text").getAsString()));

			callback.translate(output.toString());
		});
	}

	public void languages(String type, PolywoofDB db, Languages callback)
	{
		if(!db.status())
		{
			languages(type, callback);
			return;
		}

		languages(type, update ->
		{
			db.update(update, language -> log.info("[{}] UPDATE", language.code));
			callback.languages(update);
		});
	}

	public void languages(String type, Languages callback)
	{
		post("/v2/languages", new FormBody.Builder().add("type", type).build(), body ->
		{
			JsonArray json = parser.parse(body).getAsJsonArray();

			synchronized(trusted)
			{
				trusted.clear();

				for(JsonElement element : json)
					trusted.add(new TrustedLanguage(element.getAsJsonObject()));

				callback.languages(trusted);
			}
		});
	}

	public void usage(Usage callback)
	{
		post("/v2/usage", new FormBody.Builder().build(), body ->
		{
			JsonObject json = parser.parse(body).getAsJsonObject();
			callback.usage(json.get("character_count").getAsLong(), json.get("character_limit").getAsLong());
		});
	}

	public void update(String auth)
	{
		URL = auth.endsWith(":fx") ? "https://api-free.deepl.com" : "https://api.deepl.com";
		key = auth;
	}

	public interface Receive
	{
		void receive(String body) throws Exception;
	}

	public interface Translate
	{
		void translate(String text);
	}

	public interface Languages
	{
		void languages(List<Language> languages);
	}

	public interface Usage
	{
		void usage(long characterCount, long characterLimit);
	}

	public static class TrustedLanguage extends Language
	{
		public TrustedLanguage(JsonObject object)
		{
			super(object.get("language").getAsString(), object.get("name").getAsString());
		}
	}

	public static class OfflineLanguage extends Language
	{
		public OfflineLanguage(String code, String name)
		{
			super(code, name);
		}
	}

	public static class UnknownLanguage extends Language
	{
		public UnknownLanguage(String code)
		{
			super(code, "Unknown");
		}
	}

	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	public static class Language
	{
		public final String code;
		public final String name;
	}
}
