package com.polywoof;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.JagexColors;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.RenderableEntity;
import net.runelite.client.ui.overlay.components.ComponentConstants;

import javax.annotation.ParametersAreNonnullByDefault;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@ParametersAreNonnullByDefault
public class PolywoofComponent implements RenderableEntity
{
	public static final int PADDING = 8;
	public static final int SPACING = 4;
	public static final int PARAGRAPH = 8;
	public static final int OPTION = 6;

	@Setter private static int textWrap;
	@Setter private static boolean textShadow;
	@Setter private static boolean boxOutline;
	@Setter private static Color backgroundColor = ComponentConstants.STANDARD_BACKGROUND_COLOR;
	@Setter @Getter private static Alignment alignment = Alignment.BOTTOM_LEFT;

	private final List<List<String>> paragraphs = new ArrayList<>(10);
	private final Rectangle rectangle = new Rectangle();
	private final String text;
	private Font font;
	private Options options;
	private AlphaComposite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);
	private boolean revalidate;

	public PolywoofComponent(String text, Font font, Options options)
	{
		this.text = text;
		this.font = font;
		this.options = options;

		revalidate();
	}

	public static void setPosition(OverlayPosition position)
	{
		switch(position)
		{
			case TOP_LEFT:
				setAlignment(Alignment.TOP_LEFT);
				break;
			case TOP_CENTER:
				setAlignment(Alignment.TOP_CENTER);
				break;
			case TOP_RIGHT:
			case CANVAS_TOP_RIGHT:
				setAlignment(Alignment.TOP_RIGHT);
				break;
			case BOTTOM_LEFT:
				setAlignment(Alignment.BOTTOM_LEFT);
				break;
			default:
			case ABOVE_CHATBOX_RIGHT:
				setAlignment(Alignment.BOTTOM_CENTER);
				break;
			case BOTTOM_RIGHT:
				setAlignment(Alignment.BOTTOM_RIGHT);
				break;
		}
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		int x = 0;
		int y = 0;

		switch(alignment)
		{
			case TOP_LEFT:
				x = rectangle.x;
				y = rectangle.y;
				break;
			case TOP_CENTER:
				x = rectangle.x - rectangle.width / 2;
				y = rectangle.y;
				break;
			case TOP_RIGHT:
				x = rectangle.x - rectangle.width;
				y = rectangle.y;
				break;
			case BOTTOM_LEFT:
				x = rectangle.x;
				y = rectangle.y - rectangle.height;
				break;
			case BOTTOM_CENTER:
				x = rectangle.x - rectangle.width / 2;
				y = rectangle.y - rectangle.height;
				break;
			case BOTTOM_RIGHT:
				x = rectangle.x - rectangle.width;
				y = rectangle.y - rectangle.height;
				break;
		}

		graphics.setFont(font);
		graphics.setComposite(composite);
		graphics.setColor(backgroundColor);
		graphics.fillRect(x, y, rectangle.width, rectangle.height);

		if(boxOutline)
		{
			graphics.setColor(backgroundColor.darker());
			graphics.drawRect(x, y, rectangle.width - 1, rectangle.height - 1);
			graphics.setColor(backgroundColor.brighter());
			graphics.drawRect(x + 1, y + 1, rectangle.width - 3, rectangle.height - 3);
		}

		int offset = 0;
		boolean option = options != Options.NONE;

		for(List<String> paragraph : paragraphs)
		{
			for(String line : paragraph)
			{
				FontMetrics metrics = graphics.getFontMetrics(font);

				switch(alignment)
				{
					case TOP_LEFT:
					case BOTTOM_LEFT:
						if(option)
							x = rectangle.x + rectangle.width / 2 - metrics.stringWidth(line) / 2;
						else
							x = rectangle.x + PADDING;
						break;
					case TOP_CENTER:
					case BOTTOM_CENTER:
						x = rectangle.x - metrics.stringWidth(line) / 2;
						break;
					case TOP_RIGHT:
					case BOTTOM_RIGHT:
						if(option)
							x = rectangle.x - rectangle.width / 2 - metrics.stringWidth(line) / 2;
						else
							x = rectangle.x - PADDING - metrics.stringWidth(line);
						break;
				}

				switch(alignment)
				{
					case TOP_LEFT:
					case TOP_RIGHT:
					case TOP_CENTER:
						y = rectangle.y + PADDING + offset + metrics.getAscent();
						break;
					case BOTTOM_LEFT:
					case BOTTOM_CENTER:
					case BOTTOM_RIGHT:
						y = rectangle.y + PADDING + offset + metrics.getAscent() - rectangle.height;
						break;
				}

				if(textShadow)
				{
					graphics.setColor(Color.BLACK);
					graphics.drawString(line, x + 1, y + 1);
				}

				graphics.setColor(option ? JagexColors.MENU_TARGET : Color.WHITE);
				graphics.drawString(line, x, y);

				offset += metrics.getAscent() + SPACING;
			}

			offset += (options == Options.NONE ? PARAGRAPH : OPTION) - SPACING;
			option = false;
		}

		return null;
	}

	public Dimension getSize()
	{
		return rectangle.getSize();
	}

	public void setLocation(int x, int y)
	{
		rectangle.setLocation(x, y);
	}

	public void setFontSize(int size)
	{
		if(size == font.getSize())
			return;

		font = font.deriveFont((float) size);
	}

	public void setNumberedOptions(boolean numbered)
	{
		if(options == Options.NONE)
			return;

		options = numbered ? Options.NUMBERED : Options.DEFAULT;
	}

	public void setAlpha(float alpha)
	{
		composite = composite.derive(alpha);
	}

	public void update(Graphics2D graphics)
	{
		if(!revalidate)
			return;

		revalidate = false;

		paragraphs.clear();
		rectangle.setSize(0, 0);

		int index = -1;

		for(String split : text.split("\n"))
		{
			List<String> paragraph = new ArrayList<>(10);
			paragraphs.add(paragraph);

			if(options == Options.NUMBERED && ++index != 0)
				paragraph.add(String.format("%d. %s", index, split));
			else
				paragraph.add(split);

			FontMetrics metrics = graphics.getFontMetrics(font);

			for(int l = 0; l < paragraph.size(); l++)
			{
				String line = paragraph.get(l);
				int length = metrics.stringWidth(line);

				if(length <= textWrap - PADDING * 2)
				{
					rectangle.width = Math.max(rectangle.width, length);
					continue;
				}

				int wrap = 0;
				int word = 0;

				for(int i = 0; i < line.length(); i++)
				{
					if(metrics.stringWidth(line.substring(0, i + 1)) > textWrap - PADDING * 2)
					{
						int cut = Math.min(wrap, word);

						if(cut == 0)
							if((cut = Math.max(wrap, word)) == 0)
								break;

						String begin = line.substring(0, cut);
						String end = line.substring(cut);

						paragraph.set(l, begin);

						if(!end.isEmpty())
							paragraph.add(l + 1, end);

						rectangle.width = Math.max(rectangle.width, metrics.stringWidth(begin));
						break;
					}

					switch(line.charAt(i))
					{
						case ' ':
						case '.':
						case ',':
						case ':':
						case ';':
						case '-':
							wrap = i + 1;
							word = 0;
							break;
						default:
							word = i;
							break;
					}
				}
			}

			rectangle.height += metrics.getAscent() * paragraph.size() + SPACING * (paragraph.size() - 1);
		}

		rectangle.width += PADDING * 2;
		rectangle.height += PADDING * 2 + (options == Options.NONE ? PARAGRAPH : OPTION) * (paragraphs.size() - 1);

		log.info("[{}x{}]", rectangle.width, rectangle.height);
	}

	public void revalidate()
	{
		revalidate = true;
	}

	enum Alignment
	{
		TOP_LEFT,
		TOP_CENTER,
		TOP_RIGHT,
		BOTTOM_LEFT,
		BOTTOM_CENTER,
		BOTTOM_RIGHT
	}

	enum Options
	{
		NONE,
		DEFAULT,
		NUMBERED
	}
}
