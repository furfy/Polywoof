package com.polywoof;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetType;
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

import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Inject;

@Slf4j
@ParametersAreNonnullByDefault
@PluginDescriptor(name = "Polywoof", description = "Translation for almost every dialogue and any related text, so you can understand what's going on!", tags = {"helper", "language", "translator", "translation"})
public class PolywoofPlugin extends Plugin
{
	private static final String FILENAME = String.format("%s/%s", RuneLite.CACHE_DIR, "languages");

	private int dialogue;
	private String previous;
	private PolywoofTranslator translator;
	private PolywoofDB db;

	@Inject private Client client;
	@Inject private PolywoofConfig config;
	@Inject private PolywoofOverlay overlay;
	@Inject private OverlayManager overlayManager;
	@Inject private ChatMessageManager chatMessageManager;
	@Inject private OkHttpClient okHttpClient;

	@Override
	protected void startUp() throws Exception
	{
		translator = new PolywoofTranslator(okHttpClient, config.token());

		db = new PolywoofDB("DEEPL", FILENAME);
		db.open(string -> translator.languages("target", update -> db.update(update)));

		overlay.update();
		overlay.setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
		overlay.setLayer(OverlayLayer.ABOVE_WIDGETS);
		overlay.setPriority(OverlayPriority.LOW);
		overlayManager.add(overlay);

		if(config.showUsage())
			usage();
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
			case ("token"):
				translator.update(config.token());
				translator.languages("target", update -> db.update(update));
				break;
			case ("button"):
				config.set_toggle(true);
				break;
			case ("fontName"):
			case ("fontSize"):
			case ("textShadow"):
			case ("overlayColor"):
			case ("overlayOutline"):
			case ("textWrap"):
				overlay.update();
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
				if(!config.enableExamine())
					return;

				source = "Examine";
				origin = event.getMessage();
				break;
			case GAMEMESSAGE:
				if(!config.enableChat())
					return;

				source = "Game";
				origin = event.getMessage();
				break;
			default:
				return;
		}

		if(config.test())
			overlay.put(separator(source) + PolywoofTranslator.stripTags(origin));
		else
			translator.translate(origin, PolywoofTranslator.language(config.language()), db, text -> overlay.put(separator(source) + text));
	}

	@Subscribe
	public void onOverheadTextChanged(OverheadTextChanged event)
	{
		if(!config.toggle() || !config.enableOverhead())
			return;

		Actor actor = event.getActor();

		if(actor instanceof NPC)
		{
			String source;
			String origin = event.getOverheadText();

			if(actor.getName() == null)
				source = "Game";
			else
				source = actor.getName();

			if(config.test())
				overlay.put(separator(source) + PolywoofTranslator.stripTags(origin));
			else
				translator.translate(origin, PolywoofTranslator.language(config.language()), db, text -> overlay.put(separator(source) + text));
		}
	}

	/*
		11 - Alt Sprite
		222 - Scroll Text
		229 - Other Dialog
		392 - Book
	 */

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
			case 11:
			case 229:
			case 392:
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
			case 11:
			case 229:
			case 392:
				dialogue = 0;
				break;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		String source;
		String origin;

		Widget widget1;
		Widget widget2;
		Widget widget3;

		Paragraph paragraph = Paragraph.NONE;

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

				source = "Game";
				origin = widget1.getText();
				break;
			case WidgetID.DIALOG_OPTION_GROUP_ID:
				widget1 = client.getWidget(WidgetInfo.DIALOG_OPTION_OPTIONS);

				if(widget1 == null)
					return;

				int index = -1;
				StringBuilder options = new StringBuilder();

				for(Widget children : widget1.getDynamicChildren())
					if(children.getType() == WidgetType.TEXT && !children.getText().isEmpty())
					{
						switch(paragraph)
						{
							case TYPE:
								options.append("\n");
								break;
						}

						paragraph = Paragraph.TYPE;
						options.append(children.getText());
					}

				source = "Options";
				origin = options.toString();
				break;
			case WidgetID.DIARY_QUEST_GROUP_ID:
				if(!config.enableDiary())
					return;

				widget1 = client.getWidget(WidgetInfo.DIARY_QUEST_WIDGET_TITLE);
				widget2 = client.getWidget(WidgetInfo.DIARY_QUEST_WIDGET_TEXT);

				if(widget1 == null || widget2 == null)
					return;

				StringBuilder diary = new StringBuilder();

				for(Widget children : widget2.getStaticChildren())
					if(children.getType() == WidgetType.TEXT)
						if(children.getText().isEmpty())
						{
							switch(paragraph)
							{
								case SKIP:
									paragraph = Paragraph.TYPE;
									break;
							}
						}
						else
						{
							switch(paragraph)
							{
								case SKIP:
									diary.append(" ");
									break;
								case TYPE:
									diary.append("\n");
									break;
							}

							paragraph = Paragraph.SKIP;
							diary.append(children.getText());
						}

				source = widget1.getText();
				origin = diary.toString();
				break;
			case WidgetID.CLUE_SCROLL_GROUP_ID:
				if(!config.enableClues())
					return;

				widget1 = client.getWidget(WidgetInfo.CLUE_SCROLL_TEXT);

				if(widget1 == null)
					return;

				source = "Clue";
				origin = widget1.getText();
				break;
			case 11:
				widget1 = client.getWidget(dialogue, 2);

				if(widget1 == null)
					return;

				source = "Game";
				origin = widget1.getText();
				break;
			case 229:
				widget1 = client.getWidget(dialogue, 1);

				if(widget1 == null)
					return;

				source = "Game";
				origin = widget1.getText();
				break;
			case 392:
				if(!config.enableBooks())
					return;

				widget1 = client.getWidget(dialogue, 6);
				widget2 = client.getWidget(dialogue, 43);
				widget3 = client.getWidget(dialogue, 59);

				if(widget1 == null || widget2 == null || widget3 == null)
					return;

				Widget[] pages = {widget2, widget3};
				StringBuilder book = new StringBuilder();

				for(Widget page : pages)
					for(Widget children : page.getStaticChildren())
						if(children.getType() == WidgetType.TEXT)
							if(children.getText().isEmpty())
							{
								switch(paragraph)
								{
									case SKIP:
										paragraph = Paragraph.TYPE;
										break;
								}
							}
							else
							{
								switch(paragraph)
								{
									case SKIP:
										book.append(" ");
										break;
									case TYPE:
										book.append("\n");
										break;
								}

								paragraph = Paragraph.SKIP;
								book.append(children.getText());
							}

				source = widget1.getText();
				origin = book.toString();
				break;
			default:
				previous = null;

				overlay.vanish(1);
				return;
		}

		if(origin.equals(previous))
			return;

		overlay.vanish(1);

		if(!config.toggle())
		{
			previous = null;
			return;
		}

		previous = origin;

		if(config.test())
			overlay.set(1, separator(source) + PolywoofTranslator.stripTags(origin), options(dialogue));
		else
			translator.translate(origin, PolywoofTranslator.language(config.language()), db, text -> overlay.set(1, separator(source) + text, options(dialogue)));
	}

	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		if(!config.showButton() || !overlay.getBounds().contains(client.getMouseCanvasPosition().getX(), client.getMouseCanvasPosition().getY()))
			return;

		client.createMenuEntry(1)
				.setOption("Check")
				.setTarget(ColorUtil.wrapWithColorTag("Usage", JagexColors.MENU_TARGET))
				.setType(MenuAction.RUNELITE)
				.onClick(menuEntry -> usage());

		client.createMenuEntry(2)
				.setOption(config.toggle() ? "Disable" : "Enable")
				.setTarget(ColorUtil.wrapWithColorTag("Translation", JagexColors.MENU_TARGET))
				.setType(MenuAction.RUNELITE)
				.onClick(menuEntry -> config.set_toggle(!config.toggle()));
	}

	@Provides
	PolywoofConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PolywoofConfig.class);
	}

	public PolywoofComponent.Options options(int dialogue)
	{
		if(dialogue == WidgetID.DIALOG_OPTION_GROUP_ID)
			return config.numberedOptions() ? PolywoofComponent.Options.NUMBERED : PolywoofComponent.Options.DEFAULT;

		return PolywoofComponent.Options.NONE;
	}

	public String separator(String source)
	{
		return config.sourceName() ? PolywoofTranslator.stripTags(source) + config.sourceSeparator() : "";
	}

	public void usage()
	{
		translator.usage((character_count, character_limit) ->
		{
			String message = new ChatMessageBuilder()
					.append(ChatColorType.NORMAL)
					.append("Your current DeepL API usage is ")
					.append(ChatColorType.HIGHLIGHT)
					.append(Math.round(100f * ((float) character_count / character_limit)) + "%")
					.append(ChatColorType.NORMAL)
					.append(" of monthly quota!")
					.build();

			chatMessageManager.queue(QueuedMessage.builder().type(ChatMessageType.CONSOLE).runeLiteFormattedMessage(message).build());
		});
	}

	private enum Paragraph
	{
		NONE,
		SKIP,
		TYPE
	}
}
