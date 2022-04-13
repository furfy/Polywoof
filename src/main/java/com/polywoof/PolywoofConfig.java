package com.polywoof;

import net.runelite.client.config.*;
import net.runelite.client.ui.overlay.OverlayPosition;

import java.awt.*;

@ConfigGroup("polywoof")
public interface PolywoofConfig extends Config
{
	@ConfigSection( name = "Primary", description = "Primary stuff", position = 0 )
	String primarySection = "primarySection";

	@ConfigItem( keyName = "language", name = "Target Language", description = "Type your desired one, «ru» for russian, «fr» french, etc", section = primarySection, position = 0 )
	default String language()
	{
		return "ru";
	}

	@ConfigItem( keyName = "token", name = "DeepL API Token", description = "This is REQUIRED, see www.DeepL.com", secret = true, section = primarySection, position = 1 )
	default String token()
	{
		return "";
	}

	@ConfigSection( name = "Behavior", description = "Behavior stuff", position = 1 )
	String behaviorSection = "behaviorSection";

	@Range( min = 1, max = 99 )
	@ConfigItem( keyName = "readingSpeed", name = "Reading Speed", description = "How quickly do you read", section = behaviorSection, position = 0 )
	default int readingSpeed()
	{
		return 10;
	}

	@ConfigItem( keyName = "enableChat", name = "Chat Messages", description = "Translate chat messages", section = behaviorSection, position = 1 )
	default boolean enableChat()
	{
		return false;
	}

	@ConfigItem( keyName = "enableOverhead", name = "Overhead Text", description = "Translate overhead text", section = behaviorSection, position = 2 )
	default boolean enableOverhead()
	{
		return false;
	}

	@ConfigItem( keyName = "enableDiary", name = "Diary and Clues", description = "Translate diary and clues", warning = "It will use a lot of resources to translate!\nMake sure you absolutely need it.", section = behaviorSection, position = 3 )
	default boolean enableDiary()
	{
		return false;
	}

	@ConfigItem( keyName = "showUsage", name = "Print API Usage", description = "See your monthly API usage on logon", section = behaviorSection, position = 4 )
	default boolean showUsage()
	{
		return true;
	}

	@ConfigSection( name = "Font", description = "Font appearance stuff", position = 2 )
	String fontSection = "fontSection";

	@ConfigItem( keyName = "fontName", name = "Font Name", description = "Checkout your fonts viewer", section = fontSection, position = 0 )
	default String fontName()
	{
		return "Consolas";
	}

	@Range( min = 1, max = 99 )
	@ConfigItem( keyName = "fontSize", name = "Font Size", description = "Because size does matter", section = fontSection, position = 1 )
	default int fontSize()
	{
		return 12;
	}

	@ConfigSection( name = "Visual", description = "Visual appearance stuff", position = 3 )
	String visualSection = "visualSection";

	@ConfigItem( keyName = "overlayPosition", name = "Position on Screen", description = "Put the thing where it belongs", section = visualSection, position = 0 )
	default OverlayPosition overlayPosition()
	{
		return OverlayPosition.BOTTOM_LEFT;
	}

	@Alpha
	@ConfigItem( keyName = "overlayColor", name = "Background Color", description = "Any color is acceptable", section = visualSection, position = 1 )
	default Color overlayColor()
	{
		return new Color(32, 32, 32, 128);
	}

	@ConfigSection( name = "Formatting", description = "Text formatting stuff", position = 4 )
	String formatSection = "formatSection";

	@Range( min = 32 )
	@ConfigItem( keyName = "wrapWidth", name = "Text Wrap Width", description = "Widest text ever", section = formatSection, position = 0 )
	default int wrapWidth()
	{
		return 480;
	}

	@ConfigItem( keyName = "sourceName", name = "Source Name", description = "Tell me who said that", section = formatSection, position = 1 )
	default boolean sourceName()
	{
		return true;
	}

	@ConfigItem( keyName = "sourceSeparator", name = "Source Separator", description = "Between source and text", section = formatSection, position = 2 )
	default String sourceSeparator()
	{
		return ": ";
	}
}
