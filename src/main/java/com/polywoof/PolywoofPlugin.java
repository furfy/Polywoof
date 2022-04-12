package com.polywoof;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayManager;
import okhttp3.OkHttpClient;

import javax.inject.Inject;

@Slf4j
@PluginDescriptor( name = "Polywoof", description = "Translation for almost all NPC and non-NPC text so you can understand the story", tags = { "hint", "language", "translator", "translation" } )

public class PolywoofPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private PolywoofConfig config;

	@Inject
	private PolywoofOverlay polywoofOverlay;

	@Inject
	private OkHttpClient okHttpClient;

	private int dialog;
	private boolean notify = true;
	private String previous;
	private PolywoofTranslator translator;

	@Override
	protected void startUp() throws Exception
	{
		update(true);
		polywoofOverlay.update();
		overlayManager.add(polywoofOverlay);
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(polywoofOverlay);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged)
	{
		switch (configChanged.getKey())
		{
			case ("token"):
				update(true);
				break;
			case ("showUsage"):
				notify = true;
				break;
			case ("fontName"):
			case ("fontSize"):
				polywoofOverlay.update();
				break;
			case ("overlayPosition"):
				update(false);
				break;
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if(config.showUsage() && gameStateChanged.getGameState() == GameState.LOGGED_IN && notify)
		{
			translator.usage((character_count, character_limit) ->
			{
				String message = new ChatMessageBuilder()
						.append(ChatColorType.NORMAL)
						.append("Your current DeepL API usage is ")
						.append(ChatColorType.HIGHLIGHT)
						.append(Math.round(100f * ((float)character_count / character_limit)) + "%")
						.append(ChatColorType.NORMAL)
						.append(" of monthly quota!")
						.build();

				chatMessageManager.queue(QueuedMessage.builder().type(ChatMessageType.CONSOLE).runeLiteFormattedMessage(message).build());
			});

			notify = false;
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage chatMessage)
	{
		String text;
		String source;

		switch (chatMessage.getType())
		{
			case NPC_EXAMINE:
			case ITEM_EXAMINE:
			case OBJECT_EXAMINE:
				text = chatMessage.getMessage();
				source = "Examine";
				break;
			case GAMEMESSAGE:
				if(!config.enableChat()) return;

				text = chatMessage.getMessage();
				source = "Game";
				break;
			default:
				return;
		}

		translator.translate(translator.stripTags(text), config.language(), target ->
		{
			polywoofOverlay.put((config.sourceName() ? source + config.sourceSeparator() : "") + target);
		});
	}

	@Subscribe
	public void onOverheadTextChanged(OverheadTextChanged overheadTextChanged)
	{
		if(!config.enableOverhead()) return;

		Actor actor = overheadTextChanged.getActor();

		if(actor instanceof NPC)
		{
			String text = overheadTextChanged.getOverheadText();
			String source = actor.getName();

			translator.translate(translator.stripTags(text), config.language(), target ->
			{
				polywoofOverlay.put((config.sourceName() ? source + config.sourceSeparator() : "") + target);
			});
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded widgetLoaded)
	{
		switch (widgetLoaded.getGroupId())
		{
			case (WidgetID.DIALOG_NPC_GROUP_ID):
			case (WidgetID.DIALOG_PLAYER_GROUP_ID):
			case (WidgetID.DIALOG_SPRITE_GROUP_ID):
			case (WidgetID.DIALOG_OPTION_GROUP_ID):
			case (WidgetID.DIARY_QUEST_GROUP_ID):
			case (WidgetID.CLUE_SCROLL_GROUP_ID):
				dialog = widgetLoaded.getGroupId();
				break;
		}
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed widgetClosed)
	{
		switch (widgetClosed.getGroupId())
		{
			case (WidgetID.DIALOG_NPC_GROUP_ID):
			case (WidgetID.DIALOG_PLAYER_GROUP_ID):
			case (WidgetID.DIALOG_SPRITE_GROUP_ID):
			case (WidgetID.DIALOG_OPTION_GROUP_ID):
			case (WidgetID.DIARY_QUEST_GROUP_ID):
			case (WidgetID.CLUE_SCROLL_GROUP_ID):
				dialog = 0;
				break;
		}
	}

	@Subscribe
	public void onGameTick(GameTick gameTick)
	{
		String text;
		String source;

		switch (dialog)
		{
			case (WidgetID.DIALOG_NPC_GROUP_ID):
				Widget widgetNPCName = client.getWidget(WidgetInfo.DIALOG_NPC_NAME);
				Widget widgetNPCText = client.getWidget(WidgetInfo.DIALOG_NPC_TEXT);

				if(widgetNPCName == null || widgetNPCText == null) return;

				text = widgetNPCText.getText();
				source = widgetNPCName.getText();
				break;
			case (WidgetID.DIALOG_PLAYER_GROUP_ID):
				Actor player = client.getLocalPlayer();
				Widget widgetPlayerText = client.getWidget(WidgetInfo.DIALOG_PLAYER_TEXT);

				if(player == null || widgetPlayerText == null) return;

				text = widgetPlayerText.getText();
				source = player.getName();
				break;
			case (WidgetID.DIALOG_SPRITE_GROUP_ID):
				Widget widgetSpriteText = client.getWidget(WidgetInfo.DIALOG_SPRITE_TEXT);

				if(widgetSpriteText == null) return;

				text = widgetSpriteText.getText();
				source = "Game";
				break;
			case (WidgetID.DIALOG_OPTION_GROUP_ID):
				Widget widgetOptions = client.getWidget(WidgetInfo.DIALOG_OPTION_OPTIONS);

				if(widgetOptions == null || widgetOptions.getChildren() == null) return;

				int index = -1;
				StringBuilder options = new StringBuilder();

				for(Widget option : widgetOptions.getChildren())
				{
					if(option.getType() == WidgetType.TEXT) options.append(++index == 0 ? "" : index + ". ").append(option.getText()).append("\n");
				}

				text = options.toString();
				source = "Options";
				break;
			case (WidgetID.DIARY_QUEST_GROUP_ID):
				if(!config.enableDiary()) return;

				Widget widgetDiaryTitle = client.getWidget(WidgetInfo.DIARY_QUEST_WIDGET_TITLE);
				Widget widgetDiaryText = client.getWidget(WidgetInfo.DIARY_QUEST_WIDGET_TEXT);

				if(widgetDiaryTitle == null || widgetDiaryText == null || widgetDiaryText.getStaticChildren() == null) return;

				StringBuilder diary = new StringBuilder();

				for(Widget line : widgetDiaryText.getStaticChildren())
				{
					if(line.getType() == WidgetType.TEXT) diary.append(line.getText()).append(" ");
				}

				text = diary.toString();
				source = widgetDiaryTitle.getText();
				break;
			case (WidgetID.CLUE_SCROLL_GROUP_ID):
				if(!config.enableDiary()) return;

				Widget widgetClueText = client.getWidget(WidgetInfo.CLUE_SCROLL_TEXT);

				if(widgetClueText == null) return;

				text = widgetClueText.getText();
				source = "Clue";
				break;
			default:
				previous = null;
				polywoofOverlay.vanish(1);
				return;
		}

		if(text.equals(previous)) return; else previous = text;

		translator.translate(translator.stripTags(text), config.language(), target ->
		{
			polywoofOverlay.vanish(1);
			polywoofOverlay.set(1, (source != null && config.sourceName() ? translator.stripTags(source) + config.sourceSeparator() : "") + target);
		});
	}

	@Provides
	PolywoofConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PolywoofConfig.class);
	}

	public void update(boolean token)
	{
		polywoofOverlay.setLayer(OverlayLayer.ALWAYS_ON_TOP);
		polywoofOverlay.setPosition(config.overlayPosition());

		if(token) translator = new PolywoofTranslator(okHttpClient, config.token());
	}
}
