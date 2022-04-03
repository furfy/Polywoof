package com.polywoof;

import net.runelite.client.config.*;
import net.runelite.client.ui.overlay.OverlayPosition;

import java.awt.*;

@ConfigGroup("polywoof")
public interface PolywoofConfig extends Config
{
	@ConfigSection( name = "Primary", description = "General stuff", position = 0 )
	String primarySection = "primarySection";

	@ConfigItem( keyName = "language", name = "Language", description = "Type your desired one", section = primarySection, position = 0 )
	default String language()
	{
		return "RU";
	}

	@ConfigItem( keyName = "token", name = "DeepL API Token", description = "This is REQUIRED", secret = true, section = primarySection, position = 1 )
	default String token()
	{
		return "";
	}

	@ConfigSection( name = "Font", description = "Appearance stuff", position = 2 )
	String fontSection = "fontSection";

	@ConfigItem( keyName = "font", name = "Font", description = "Checkout your fonts viewer", section = fontSection, position = 0 )
	default String font()
	{
		return "Consolas";
	}

	@Range( min = 1 )
	@ConfigItem( keyName = "size", name = "Size", description = "Because size does matter", section = fontSection, position = 1 )
	default int size()
	{
		return 12;
	}

	@ConfigSection( name = "Appearance", description = "Visual stuff", position = 3 )
	String visualSection = "visualSection";

	@ConfigItem( keyName = "position", name = "Position", description = "Put the thing where it belongs", section = visualSection, position = 0 )
	default OverlayPosition position()
	{
		return OverlayPosition.BOTTOM_LEFT;
	}

	@Alpha
	@ConfigItem( keyName = "color", name = "Color", description = "Background color for subtitles", section = visualSection, position = 1 )
	default Color color()
	{
		return new Color(32, 32, 32, 128);
	}

	@ConfigSection( name = "Formatting", description = "Format stuff", position = 4 )
	String formatSection = "formatSection";

	@ConfigItem( keyName = "source", name = "Source", description = "Tell me who said that", section = formatSection, position = 0 )
	default boolean source()
	{
		return true;
	}

	@ConfigItem( keyName = "separator", name = "Separator", description = "Between source and text", section = formatSection, position = 1 )
	default String separator()
	{
		return ": ";
	}

	@ConfigSection( name = "Experimental", description = "Go away", position = 5, closedByDefault = true )
	String experimental = "experimental";

	@ConfigItem( keyName = "url", name = "URL", description = "1$ - token\n2$ - text\n3$ - language", section = experimental, position = 0 )
	default String url()
	{
		return "https://api-free.deepl.com/v2/translate?auth_key=%1$s&text=%2$s&target_lang=%3$s&source_lang=en&preserve_formatting=1&split_sentences=1";
	}

	@ConfigItem( keyName = "jsonArray", name = "Json array key", description = "{ ?:[ sentence:text, sentence:text.. ] }", section = experimental, position = 1 )
	default String jsonArray()
	{
		return "translations";
	}

	@ConfigItem( keyName = "jsonString", name = "Json string key", description = "{ array:[ sentence:?, sentence:?.. ] }", section = experimental, position = 2 )
	default String jsonString()
	{
		return "text";
	}
}
