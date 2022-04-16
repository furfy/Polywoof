package com.polywoof;

import net.runelite.client.config.*;
import net.runelite.client.ui.overlay.OverlayPosition;

import java.awt.*;

@ConfigGroup("polywoof")
public interface PolywoofConfig extends Config
{
	String warning = "It will use a lot of resources to translate! Are you sure?";

	@ConfigSection( name = "Primary", description = "Primary stuff", position = 0 )
	String primarySection = "primarySection";

	@ConfigSection( name = "Behavior", description = "Behavior stuff", position = 1 )
	String behaviorSection = "behaviorSection";

	@ConfigSection( name = "Font", description = "Font appearance stuff", position = 2 )
	String fontSection = "fontSection";

	@ConfigSection( name = "Visual", description = "Visual appearance stuff", position = 3 )
	String visualSection = "visualSection";

	@ConfigSection( name = "Formatting", description = "Text formatting stuff", position = 4 )
	String formatSection = "formatSection";

	@ConfigItem( keyName = "language", name = "Target Language Code", description = "Type your desired one, «ru» for russian, «fr» french, etc", section = primarySection, position = 0 )
	default String language()
	{
		return "ru";
	}

	@ConfigItem( keyName = "token", name = "DeepL API Token", description = "This is REQUIRED, see www.DeepL.com", secret = true, section = primarySection, position = 1 )
	default String token()
	{
		return "";
	}

	@Range( min = 1, max = 99 )
	@ConfigItem( keyName = "readingSpeed", name = "Reading Speed", description = "How quickly do you read", section = behaviorSection, position = 0 )
	default int readingSpeed()
	{
		return 10;
	}

	@ConfigItem( keyName = "enableExamine", name = "Any Examine", description = "Translate any examine", section = behaviorSection, position = 1 )
	default boolean enableExamine()
	{
		return true;
	}

	@ConfigItem( keyName = "enableChat", name = "Chat Messages", description = "Translate chat messages", section = behaviorSection, position = 2 )
	default boolean enableChat()
	{
		return true;
	}

	@ConfigItem( keyName = "enableOverhead", name = "Overhead Text", description = "Translate overhead text", section = behaviorSection, position = 3 )
	default boolean enableOverhead()
	{
		return true;
	}

	@ConfigItem( keyName = "enableClues", name = "Treasure Clues", description = "Translate treasure clues", section = behaviorSection, position = 4 )
	default boolean enableClues()
	{
		return false;
	}

	@ConfigItem( keyName = "enableDiary", name = "Quests Diary", description = "Translate quests diary", warning = warning, section = behaviorSection, position = 5 )
	default boolean enableDiary()
	{
		return false;
	}

	@ConfigItem( keyName = "enableBooks", name = "Book Pages", description = "Translate book pages", warning = warning, section = behaviorSection, position = 6 )
	default boolean enableBooks()
	{
		return false;
	}

	@ConfigItem( keyName = "showUsage", name = "Print API Usage", description = "See your monthly API usage on logon", section = behaviorSection, position = 7 )
	default boolean showUsage()
	{
		return true;
	}

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
