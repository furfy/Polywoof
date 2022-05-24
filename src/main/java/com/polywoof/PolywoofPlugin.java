package com.polywoof;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.RuneLite;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.JagexColors;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.util.ColorUtil;
import okhttp3.OkHttpClient;
import org.h2.engine.Constants;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Inject;
import java.io.File;

@Slf4j
@ParametersAreNonnullByDefault
@PluginDescriptor(name = "Polywoof", description = "Translation for almost every dialogue and some related text, so you can understand what's going on!", tags = {"helper", "language", "translator", "translation"})
public class PolywoofPlugin extends Plugin
{
	private static final File FILE = new File(RuneLite.CACHE_DIR, "polywoof" + Constants.SUFFIX_MV_FILE);

	private int dialogue;
	private String previous;
	private PolywoofTranslator translator;
	private PolywoofDB db;

	@Inject private Client client;
	@Inject private PolywoofConfig config;
	@Inject private PolywoofOverlay overlay;
	@Inject private ConfigManager configManager;
	@Inject private OverlayManager overlayManager;
	@Inject private ChatMessageManager chatMessageManager;
	@Inject private OkHttpClient okHttpClient;

	@Deprecated
	private synchronized boolean migrate()
	{
		String deprecated = configManager.getConfiguration("polywoof", "token");

		if(deprecated != null)
		{
			configManager.setConfiguration("polywoof", "key", deprecated);
			configManager.unsetConfiguration("polywoof", "token");
		}

		File file = new File(RuneLite.CACHE_DIR, "languages" + Constants.SUFFIX_MV_FILE);

		if(!file.exists())
			return false;

		if(FILE.exists())
			return file.delete();
		else
			return file.renameTo(FILE);
	}

	@Override
	protected void startUp() throws Exception
	{
		if(migrate())
			log.info("Migration is complete!");

		translator = new PolywoofTranslator(okHttpClient, config.key());

		db = new PolywoofDB("DEEPL", FILE);
		db.open(() -> translator.languages("target", db, languages -> log.info("{} languages loaded!", languages.size())));

		overlay.revalidate();
		overlay.setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
		overlay.setLayer(OverlayLayer.ABOVE_WIDGETS);
		overlay.setPriority(OverlayPriority.LOW);
		overlayManager.add(overlay);

		if(config.key().isEmpty())
		{
			String message = new ChatMessageBuilder()
				.append("Polywoof is not ready, the ")
				.append(ChatColorType.HIGHLIGHT)
				.append("API Key")
				.append(ChatColorType.NORMAL)
				.append(" is missing.")
				.build();

			chatMessageManager.queue(QueuedMessage.builder().type(ChatMessageType.CONSOLE).runeLiteFormattedMessage(message).build());
		}

		if(config.showUsage())
			showUsage();
	}

	@Override
	protected void shutDown() throws Exception
	{
		db.close();

		overlay.clear();
		overlayManager.remove(overlay);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if(!event.getGroup().equals("polywoof"))
			return;

		switch(event.getKey())
		{
			case "language":
				if(PolywoofTranslator.languageFinder(config.language()) instanceof PolywoofTranslator.UnknownLanguage)
				{
					String message = new ChatMessageBuilder()
						.append("Your chosen language «")
						.append(ChatColorType.HIGHLIGHT)
						.append(config.language())
						.append(ChatColorType.NORMAL)
						.append("» is not found!")
						.build();

					chatMessageManager.queue(QueuedMessage.builder().type(ChatMessageType.CONSOLE).runeLiteFormattedMessage(message).build());
				}
				break;
			case "key":
				translator.update(config.key());
				translator.languages("target", db, languages -> log.info("{} languages loaded!", languages.size()));
				break;
			case "showButton":
				config.toggle(true);
				break;
			case "fontName":
			case "fontSize":
			case "textShadow":
			case "overlayColor":
			case "overlayOutline":
			case "textWrap":
			case "numberedOptions":
				overlay.revalidate();
				break;
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if(!config.toggle())
			return;

		String source;
		String origin;

		switch(event.getType())
		{
			case NPC_EXAMINE:
			case ITEM_EXAMINE:
			case OBJECT_EXAMINE:
				if(!config.test() && !config.enableExamine())
					return;

				source = null;
				origin = event.getMessage();
				break;
			case GAMEMESSAGE:
				if(!config.test() && !config.enableChat())
					return;

				source = event.getName();
				origin = event.getMessage();
				break;
			default:
				return;
		}

		if((origin = PolywoofFormatter.parse(origin, PolywoofFormatter.CHAT)) == null)
			return;

		if(config.test())
			overlay.put(dialogueFormat(source, PolywoofFormatter.filter(origin)));
		else
			translator.translate(origin, PolywoofTranslator.languageFinder(config.language()), db, text -> overlay.put(dialogueFormat(source, text)));
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		String source;
		String origin;

		Widget widget1;
		Widget widget2;
		Widget widget3;

		switch(dialogue)
		{
			case WidgetID.DIALOG_NPC_GROUP_ID:
				widget1 = client.getWidget(WidgetInfo.DIALOG_NPC_NAME);
				widget2 = client.getWidget(WidgetInfo.DIALOG_NPC_TEXT);

				if(widget1 == null || widget2 == null)
					return;

				source = widget1.getText();
				origin = widget2.getText();
				break;
			case WidgetID.DIALOG_PLAYER_GROUP_ID:
				widget1 = client.getWidget(dialogue, 4);
				widget2 = client.getWidget(WidgetInfo.DIALOG_PLAYER_TEXT);

				if(widget1 == null || widget2 == null)
					return;

				source = widget1.getText();
				origin = widget2.getText();
				break;
			case WidgetID.DIALOG_SPRITE_GROUP_ID:
				widget1 = client.getWidget(WidgetInfo.DIALOG_SPRITE_TEXT);

				if(widget1 == null)
					return;

				source = null;
				origin = widget1.getText();
				break;
			case WidgetID.DIALOG_OPTION_GROUP_ID:
				widget1 = client.getWidget(WidgetInfo.DIALOG_OPTION_OPTIONS);

				if(widget1 == null)
					return;

				source = null;
				origin = PolywoofFormatter.options(widget1.getDynamicChildren());
				break;
			case WidgetID.DIARY_QUEST_GROUP_ID:
				if(!config.test() && !config.enableDiary())
					return;

				widget1 = client.getWidget(WidgetInfo.DIARY_QUEST_WIDGET_TITLE);
				widget2 = client.getWidget(WidgetInfo.DIARY_QUEST_WIDGET_TEXT);

				if(widget1 == null || widget2 == null)
					return;

				source = widget1.getText();
				origin = PolywoofFormatter.scrolls(widget2.getStaticChildren());
				break;
			case WidgetID.CLUE_SCROLL_GROUP_ID:
				if(!config.test() && !config.enableClues())
					return;

				widget1 = client.getWidget(WidgetInfo.CLUE_SCROLL_TEXT);

				if(widget1 == null)
					return;

				source = null;
				origin = widget1.getText();
				break;
			case WidgetIDCustom.ALT_SPRITE:
				widget1 = client.getWidget(dialogue, 2);

				if(widget1 == null)
					return;

				source = null;
				origin = widget1.getText();
				break;
			case WidgetIDCustom.SIMPLE_SCROLL:
			case WidgetIDCustom.ALT_SCROLL:
				if(!config.test() && !config.enableScrolls())
					return;

				widget1 = client.getWidget(WidgetID.RESIZABLE_VIEWPORT_OLD_SCHOOL_BOX_GROUP_ID, 16);

				if(widget1 == null)
					return;

				source = null;
				origin = PolywoofFormatter.scrolls(widget1.getNestedChildren());
				break;
			case WidgetIDCustom.SIMPLE_DIALOG:
				widget1 = client.getWidget(dialogue, 1);

				if(widget1 == null)
					return;

				source = null;
				origin = widget1.getText();
				break;
			case WidgetIDCustom.SIMPLE_BOOK:
				if(!config.test() && !config.enableBooks())
					return;

				widget1 = client.getWidget(dialogue, 6);
				widget2 = client.getWidget(dialogue, 43);
				widget3 = client.getWidget(dialogue, 59);

				if(widget1 == null || widget2 == null || widget3 == null)
					return;

				source = widget1.getText();
				origin = PolywoofFormatter.scrolls(widget2.getStaticChildren(), widget3.getStaticChildren());
				break;
			case WidgetIDCustom.CONTENTS_BOOK:
				if(!config.test() && !config.enableBooks())
					return;

				widget1 = client.getWidget(dialogue, 6);
				widget2 = client.getWidget(dialogue, 44);
				widget3 = client.getWidget(dialogue, 60);

				if(widget1 == null || widget2 == null || widget3 == null)
					return;

				source = widget1.getText();
				origin = PolywoofFormatter.scrolls(widget2.getStaticChildren(), widget3.getStaticChildren());
				break;
			default:
				overlay.clear(ID.ANYTHING);
				previous = null;
				return;
		}

		if(origin.equals(previous))
			return;

		overlay.clear(ID.ANYTHING);

		if(!config.toggle())
		{
			previous = null;
			return;
		}

		previous = origin;

		if((origin = PolywoofFormatter.parse(origin, PolywoofFormatter.DIALOGUE)) == null)
			return;

		if(config.test())
			overlay.set(ID.ANYTHING, dialogueFormat(source, PolywoofFormatter.filter(origin)), dialogueType(dialogue));
		else
			translator.translate(origin, PolywoofTranslator.languageFinder(config.language()), db, text -> overlay.set(ID.ANYTHING, dialogueFormat(source, text), dialogueType(dialogue)));
	}

	@Subscribe
	public void onOverheadTextChanged(OverheadTextChanged event)
	{
		if(!config.toggle() || !config.test() && !config.enableOverhead())
			return;

		if(event.getActor() instanceof NPC)
		{
			String source = event.getActor().getName();
			String origin = event.getOverheadText();

			if((origin = PolywoofFormatter.parse(origin, PolywoofFormatter.OVERHEAD)) == null)
				return;

			if(config.test())
				overlay.put(dialogueFormat(source, PolywoofFormatter.filter(origin)));
			else
				translator.translate(origin, PolywoofTranslator.languageFinder(config.language()), db, text -> overlay.put(dialogueFormat(source, text)));
		}
	}

	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		if(!config.showButton() || !overlay.isMouseOver())
			return;

		client.createMenuEntry(1)
			.setOption("Check")
			.setTarget(ColorUtil.wrapWithColorTag("Usage", JagexColors.MENU_TARGET))
			.setType(MenuAction.RUNELITE)
			.onClick(menuEntry -> showUsage());

		client.createMenuEntry(2)
			.setOption(config.toggle() ? "Disable" : "Enable")
			.setTarget(ColorUtil.wrapWithColorTag("Plugin", JagexColors.MENU_TARGET))
			.setType(MenuAction.RUNELITE)
			.onClick(menuEntry -> config.toggle(!config.toggle()));
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		switch(event.getGroupId())
		{
			case WidgetID.DIALOG_NPC_GROUP_ID:
			case WidgetID.DIALOG_PLAYER_GROUP_ID:
			case WidgetID.DIALOG_SPRITE_GROUP_ID:
			case WidgetID.DIALOG_OPTION_GROUP_ID:
			case WidgetID.DIARY_QUEST_GROUP_ID:
			case WidgetID.CLUE_SCROLL_GROUP_ID:
			case WidgetIDCustom.ALT_SPRITE:
			case WidgetIDCustom.SIMPLE_SCROLL:
			case WidgetIDCustom.ALT_SCROLL:
			case WidgetIDCustom.SIMPLE_DIALOG:
			case WidgetIDCustom.SIMPLE_BOOK:
				dialogue = event.getGroupId();
				break;
		}
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed event)
	{
		switch(event.getGroupId())
		{
			case WidgetID.DIALOG_NPC_GROUP_ID:
			case WidgetID.DIALOG_PLAYER_GROUP_ID:
			case WidgetID.DIALOG_SPRITE_GROUP_ID:
			case WidgetID.DIALOG_OPTION_GROUP_ID:
			case WidgetID.DIARY_QUEST_GROUP_ID:
			case WidgetID.CLUE_SCROLL_GROUP_ID:
			case WidgetIDCustom.ALT_SPRITE:
			case WidgetIDCustom.SIMPLE_SCROLL:
			case WidgetIDCustom.ALT_SCROLL:
			case WidgetIDCustom.SIMPLE_DIALOG:
			case WidgetIDCustom.SIMPLE_BOOK:
				dialogue = 0;
				break;
		}
	}

	@Provides
	PolywoofConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PolywoofConfig.class);
	}

	private String dialogueFormat(@Nullable String source, String origin)
	{
		if(!config.sourceName() || source == null || source.isEmpty())
			return origin;

		return String.format("%s%s%s", PolywoofFormatter.filter(source), config.sourceSeparator(), origin);
	}

	private PolywoofComponent.Options dialogueType(int dialogue)
	{
		if(dialogue == WidgetID.DIALOG_OPTION_GROUP_ID)
			return config.numberedOptions() ? PolywoofComponent.Options.NUMBERED : PolywoofComponent.Options.DEFAULT;

		return PolywoofComponent.Options.NONE;
	}

	public void showUsage()
	{
		translator.usage((characterCount, characterLimit) ->
		{
			String message = new ChatMessageBuilder()
				.append(ChatColorType.NORMAL)
				.append("Your current DeepL API usage is ")
				.append(ChatColorType.HIGHLIGHT)
				.append(Math.round(100f * ((float) characterCount / characterLimit)) + "%")
				.append(ChatColorType.NORMAL)
				.append(" of the monthly quota!")
				.build();

			chatMessageManager.queue(QueuedMessage.builder().type(ChatMessageType.CONSOLE).runeLiteFormattedMessage(message).build());
		});
	}

	public static class WidgetIDCustom
	{
		public static final int ALT_SPRITE = 11;
		public static final int SIMPLE_SCROLL = 220;
		public static final int ALT_SCROLL = 222;
		public static final int SIMPLE_DIALOG = 229;
		public static final int SIMPLE_BOOK = 392;
		public static final int CONTENTS_BOOK = 680;
	}

	public static class ID
	{
		public static final int CAPACITY = ID.class.getFields().length - 1;
		public static final byte ANYTHING = 1;
	}
}
