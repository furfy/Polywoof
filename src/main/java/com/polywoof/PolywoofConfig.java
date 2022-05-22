package com.polywoof;

import net.runelite.client.config.*;
import net.runelite.client.ui.overlay.components.ComponentConstants;

import java.awt.*;

@ConfigGroup("polywoof")
public interface PolywoofConfig extends Config
{
	@ConfigSection( name = "Primary", description = "Most important", position = 0 )
	String primarySection = "primarySection";

	@ConfigSection( name = "Translation", description = "What will be translated", position = 1 )
	String translationSection = "translationSection";

	@ConfigSection( name = "Visual", description = "Font and visual appearance", position = 2 )
	String visualSection = "visualSection";

	@ConfigSection( name = "Formatting", description = "Text formatting", position = 3 )
	String formattingSection = "formattingSection";

	@ConfigItem( keyName = "toggle", name = "", description = "", hidden = true )
	default boolean toggle()
	{
		return true;
	}

	@ConfigItem( keyName = "toggle", name = "", description = "" )
	void set_toggle(boolean toggle);

	/*
		Primary
	 */

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
	@ConfigItem( keyName = "readingSpeed", name = "Reading Skill", description = "How quickly do you read", section = primarySection, position = 2 )
	default int readingSpeed()
	{
		return 10;
	}

	@ConfigItem( keyName = "showUsage", name = "Show API Usage", description = "See your monthly API usage on logon", section = primarySection, position = 3 )
	default boolean showUsage()
	{
		return true;
	}

	@ConfigItem( keyName = "showButton", name = "Show Button", description = "Quality of life feature", section = primarySection, position = 4 )
	default boolean showButton()
	{
		return true;
	}

	@ConfigItem( keyName = "test", name = "Test Mode", description = "What makes this plugin useless", warning = "Do you really want to disable all translations?", section = primarySection, position = 5 )
	default boolean test()
	{
		return false;
	}

	/*
		Translate
	 */

	@ConfigItem( keyName = "enableExamine", name = "Examine", description = "Translate any examine", section = translationSection, position = 0 )
	default boolean enableExamine()
	{
		return true;
	}

	@ConfigItem( keyName = "enableChat", name = "Chat Messages", description = "Translate chat messages", section = translationSection, position = 1 )
	default boolean enableChat()
	{
		return true;
	}

	@ConfigItem( keyName = "enableOverhead", name = "Overhead Text", description = "Translate overhead text", section = translationSection, position = 2 )
	default boolean enableOverhead()
	{
		return false;
	}

	@ConfigItem( keyName = "enableClues", name = "Treasure Clues", description = "Translate treasure clues", section = translationSection, position = 3 )
	default boolean enableClues()
	{
		return false;
	}

	@ConfigItem( keyName = "enableDiary", name = "Quests Diary", description = "Translate quests diary", warning = "It will use a lot of resources to translate! Are you sure?", section = translationSection, position = 4 )
	default boolean enableDiary()
	{
		return false;
	}

	@ConfigItem( keyName = "enableBooks", name = "Books", description = "Translate book pages", warning = "It will use huge amount of resources to translate! Are you sure?", section = translationSection, position = 5 )
	default boolean enableBooks()
	{
		return false;
	}

	/*
		Visual
	 */

	@ConfigItem( keyName = "fontName", name = "Font Name", description = "Checkout your fonts viewer", section = visualSection, position = 0 )
	default String fontName()
	{
		return "Consolas";
	}

	@Range( min = 1, max = 99 )
	@ConfigItem( keyName = "fontSize", name = "Font Size", description = "Because size does matter", section = visualSection, position = 1 )
	default int fontSize()
	{
		return 12;
	}

	@ConfigItem( keyName = "textShadow", name = "Text Shadow", description = "Suspicious shadowy text", section = visualSection, position = 2 )
	default boolean textShadow()
	{
		return true;
	}

	@Alpha
	@ConfigItem( keyName = "overlayColor", name = "Background Color", description = "Any color is acceptable", section = visualSection, position = 3 )
	default Color overlayColor()
	{
		return ComponentConstants.STANDARD_BACKGROUND_COLOR;
	}

	@ConfigItem( keyName = "overlayOutline", name = "Show Outline", description = "Make it.. cooler", section = visualSection, position = 4 )
	default boolean overlayOutline()
	{
		return true;
	}

	/*
		Format
	 */

	@Range( min = 32 )
	@ConfigItem( keyName = "textWrap", name = "Text Wrap Width", description = "Widest text ever", section = formattingSection, position = 0 )
	default int textWrap()
	{
		return 420;
	}

	@ConfigItem( keyName = "numberedOptions", name = "Numbered Options", description = "Let's count up to ten", section = formattingSection, position = 1 )
	default boolean numberedOptions()
	{
		return true;
	}

	@ConfigItem( keyName = "sourceName", name = "Source Name", description = "Tell me who said that", section = formattingSection, position = 2 )
	default boolean sourceName()
	{
		return true;
	}

	@ConfigItem( keyName = "sourceSeparator", name = "Source Separator", description = "Between source and text", section = formattingSection, position = 3 )
	default String sourceSeparator()
	{
		return ": ";
	}
}
