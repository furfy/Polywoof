package com.polywoof;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@ParametersAreNonnullByDefault
class PolywoofFormatter
{
	public static final ParsePattern[] CHAT =
	{
		new ParsePattern("^[0-9,]+ x (Coins\\.)$", "%s"),
		new ParsePattern("^Your .+ lap count is: <col=[0-9A-f]+>[0-9,]+</col>\\.$"),
		new ParsePattern("^You can inflict [0-9,]+ more points of damage before a ring will shatter\\.$"),
		new ParsePattern("^(Your reward is:) <col=[0-9A-f]+>[0-9,]+</col> x (<col=[0-9A-f]+>.+</col>\\.)$", "%s %s"),
		new ParsePattern("^(Your reward is:) <col=[0-9A-f]+>[0-9,]+</col> x (<col=[0-9A-f]+>.+</col> and it has been placed on the ground\\.)$", "%s %s"),
		new ParsePattern("^You have opened the Brimstone chest once\\.$"),
		new ParsePattern("^You have opened the Brimstone chest [0-9,]+ times\\.$"),
		new ParsePattern("^Congratulations, you've just advanced your .+ level\\. You are now level [0-9,]+\\.$"),
		new ParsePattern("^Congratulations, you've reached a total level of [0-9,]+\\.$"),
		new ParsePattern("^Congratulations, you've completed .+ combat task: <col=[0-9A-f]+>.+</col>\\.$"),
		new ParsePattern("^<col=[0-9A-f]+>Well done! You have completed .+ task in the .+ area\\. Your Achievement Diary has been updated\\.</col>$"),
		new ParsePattern("^(<col=[0-9A-f]+>You're assigned to kill </col>.+<col=[0-9A-f]+>); only </col>[0-9,]+<col=[0-9A-f]+> more to go\\.$", "%s."),
		new ParsePattern("^<col=[0-9A-f]+>You've completed </col>[0-9,]+ tasks <col=[0-9A-f]+>in a row and currently have a total of </col>[0-9,]+ points<col=[0-9A-f]+>\\.$"),
		new ParsePattern("^(<col=[0-9A-f]+>You have completed your task! You killed</col>) [0-9,]+ (.+<col=[0-9A-f]+>\\.) You gained</col> [0-9,]+ xp<col=[0-9A-f]+>\\.$", "%s %s"),
		new ParsePattern("^<col=[0-9A-f]+>You've completed </col>[0-9,]+ tasks <col=[0-9A-f]+>and received </col>[0-9,]+ points<col=[0-9A-f]+>, giving you a total of </col>[0-9,]+<col=[0-9A-f]+>; return to a Slayer master\\.</col>$"),
		new ParsePattern("^You've been awarded [0-9,]+ bonus Runecraft XP for closing the rift\\.$"),
		new ParsePattern("^Amount of rifts you have closed: <col=[0-9A-f]+>[0-9,]+</col>\\.$"),
		new ParsePattern("^Total elemental energy: <col=[0-9A-f]+>[0-9,]+</col>\\. Total catalytic energy:  <col=[0-9A-f]+>[0-9,]+</col>\\.$"),
		new ParsePattern("^Elemental energy attuned: <col=[0-9A-f]+>[0-9,]+</col>\\. Catalytic energy attuned: <col=[0-9A-f]+>[0-9,]+</col>\\.$"),
		new ParsePattern("^<col=[0-9A-f]+>.+ received a drop: .+</col>$")
	};

	public static final ParsePattern[] DIALOGUE =
	{
		new ParsePattern("^(Congratulations, you've just advanced your .+ level\\.) You are now level [0-9,]+\\.$", "%s"),
		new ParsePattern("^(Your brain power serves you well!<br>You have been awarded) [0-9,]+ (.+ experience!)$", "%s %s"),
		new ParsePattern("^(Your new task is to kill) [0-9,]+ (.+\\.)$", "%s %s"),
		new ParsePattern("^(You're currently assigned to kill .+); only<br>[0-9,]+ more to go\\. Your reward point tally is [0-9,]+\\.$", "%s."),
		new ParsePattern("^Select an Option\nExchange '.+': 5 coins\n.*Cancel$")
	};

	public static final ParsePattern[] OVERHEAD =
	{
		new ParsePattern("^[0-9,]+$")
	};

	public static String parse(String origin, ParsePattern[] patterns)
	{
		for(ParsePattern pattern : patterns)
		{
			Matcher matcher = pattern.match.matcher(origin);

			if(matcher.matches())
			{
				if(pattern.replacement == null)
					return null;

				Object[] groups = new Object[matcher.groupCount()];

				for(int i = 0; i < matcher.groupCount(); i++)
					groups[i] = matcher.group(i + 1);

				return String.format(pattern.replacement, groups);
			}
		}

		return origin;
	}

	public static String filter(String origin)
	{
		return origin.replaceAll("<br>", " ").replaceAll("<.*?>", "").replaceAll("[ ]{2,}", " ").trim();
	}

	public static String options(Widget[] widgets)
	{
		StringBuilder builder = new StringBuilder(100);
		Paragraph paragraph = Paragraph.NONE;

		for(Widget widget : widgets)
			if(widget.getType() == WidgetType.TEXT && !widget.getText().isEmpty())
			{
				switch(paragraph)
				{
					case INSERT:
						builder.append("\n");
						break;
				}

				builder.append(widget.getText());
				paragraph = Paragraph.INSERT;
			}

		return builder.toString();
	}

	public static String scrolls(Widget[] ... widgetsArray)
	{
		StringBuilder builder = new StringBuilder(100);
		Paragraph paragraph = Paragraph.NONE;

		for(Widget[] widgets : widgetsArray)
			for(Widget widget : widgets)
				if(widget.getType() == WidgetType.TEXT)
					if(widget.getText().isEmpty())
					{
						switch(paragraph)
						{
							case IGNORE:
								paragraph = Paragraph.INSERT;
								break;
						}
					}
					else
					{
						switch(paragraph)
						{
							case IGNORE:
								builder.append(" ");
								break;
							case INSERT:
								builder.append("\n");
								break;
						}

						builder.append(widget.getText());
						paragraph = Paragraph.IGNORE;
					}

		return builder.toString();
	}

	public static class ParsePattern
	{
		public final Pattern match;
		public final String replacement;

		public ParsePattern(String match)
		{
			this(match, null);
		}

		public ParsePattern(String match, @Nullable String replacement)
		{
			this.match = Pattern.compile(match, Pattern.DOTALL);
			this.replacement = replacement;
		}
	}

	enum Paragraph
	{
		NONE,
		IGNORE,
		INSERT
	}
}