package com.polywoof;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;

import javax.annotation.ParametersAreNonnullByDefault;
import java.awt.*;
import java.util.Arrays;
import java.util.LinkedList;

@Slf4j
@ParametersAreNonnullByDefault
public class PolywoofComponent implements LayoutableRenderableEntity
{
	private static final int PADDING = 8;
	private static final int SPACING = 4;
	private static final int PARAGRAPH = 8;
	private static final int OPTION = 6;

	private final LinkedList<LinkedList<String>> paragraphs = new LinkedList<>();
	private final Rectangle bounds = new Rectangle();
	private final Options options;
	private final String text;
	private final Font font;
	private FontMetrics metrics;
	private AlphaComposite alpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);

	@Setter private static int textWrap;
	@Setter private static boolean textShadow;
	@Setter private static boolean boxOutline;
	@Setter private static Color backgroundColor = ComponentConstants.STANDARD_BACKGROUND_COLOR;

	@Setter
	@Getter
	private static Alignment alignment = Alignment.BOTTOM_LEFT;

	public PolywoofComponent(String text, Font font, Options options)
	{
		this.text = text;
		this.font = font;
		this.options = options;
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
		int offset = 0;

		switch(alignment)
		{
			case TOP_LEFT:
				x = bounds.x;
				y = bounds.y;
				break;
			case TOP_CENTER:
				x = bounds.x - bounds.width / 2;
				y = bounds.y;
				break;
			case TOP_RIGHT:
				x = bounds.x - bounds.width;
				y = bounds.y;
				break;
			case BOTTOM_LEFT:
				x = bounds.x;
				y = bounds.y - bounds.height;
				break;
			case BOTTOM_CENTER:
				x = bounds.x - bounds.width / 2;
				y = bounds.y - bounds.height;
				break;
			case BOTTOM_RIGHT:
				x = bounds.x - bounds.width;
				y = bounds.y - bounds.height;
				break;
		}

		graphics.setComposite(alpha);
		graphics.setColor(backgroundColor);
		graphics.fillRect(x, y, bounds.width, bounds.height);

		if(boxOutline)
		{
			graphics.setColor(backgroundColor.darker());
			graphics.drawRect(x, y, bounds.width - 1, bounds.height - 1);
			graphics.setColor(backgroundColor.brighter());
			graphics.drawRect(x + 1, y + 1, bounds.width - 3, bounds.height - 3);
		}

		for(LinkedList<String> paragraph : paragraphs)
		{
			for(String line : paragraph)
			{
				switch(alignment)
				{
					case TOP_LEFT:
						x = bounds.x + PADDING;
						y = bounds.y + PADDING + offset + metrics.getAscent();
						break;
					case TOP_CENTER:
						x = bounds.x - metrics.stringWidth(line) / 2;
						y = bounds.y + PADDING + offset + metrics.getAscent();
						break;
					case TOP_RIGHT:
						x = bounds.x - PADDING - metrics.stringWidth(line);
						y = bounds.y + PADDING + offset + metrics.getAscent();
						break;
					case BOTTOM_LEFT:
						x = bounds.x + PADDING;
						y = bounds.y + PADDING + offset + metrics.getAscent() - bounds.height;
						break;
					case BOTTOM_CENTER:
						x = bounds.x - metrics.stringWidth(line) / 2;
						y = bounds.y + PADDING + offset + metrics.getAscent() - bounds.height;
						break;
					case BOTTOM_RIGHT:
						x = bounds.x - PADDING - metrics.stringWidth(line);
						y = bounds.y + PADDING + offset + metrics.getAscent() - bounds.height;
						break;
				}

				graphics.setFont(font);

				if(textShadow)
				{
					graphics.setColor(Color.BLACK);
					graphics.drawString(line, x + 1, y + 1);
				}

				graphics.setColor(Color.WHITE);
				graphics.drawString(line, x, y);

				offset += metrics.getAscent() + SPACING;
			}

			offset += (options == Options.NONE ? PARAGRAPH : OPTION) - SPACING;
		}

		return bounds.getSize();
	}

	@Override
	public Rectangle getBounds()
	{
		return bounds;
	}

	@Override
	public void setPreferredLocation(Point location)
	{
		setLocation(location.x, location.y);
	}

	@Override
	public void setPreferredSize(Dimension dimension)
	{
		setSize(dimension.width);
	}

	public void setLocation(int x, int y)
	{
		bounds.setLocation(x, y);
	}

	public void setSize(int width)
	{
		setTextWrap(width);
		update();
	}

	public void setAlpha(float alpha)
	{
		this.alpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
	}

	public void update(Graphics2D graphics)
	{
		metrics = graphics.getFontMetrics(font);

		if(paragraphs.isEmpty())
			update();
	}

	public void update()
	{
		if(metrics == null)
			return;

		int index = -1;
		LinkedList<String> list = new LinkedList<>(Arrays.asList(text.split("\n")));

		paragraphs.clear();

		for(String paragraph : list)
		{
			LinkedList<String> lines = new LinkedList<>();

			if(options == Options.NUMBERED)
				lines.add(++index == 0 ? paragraph : String.format("%d. %s", index, paragraph));
			else
				lines.add(paragraph);

			for(int l = 0; l < lines.size(); l++)
			{
				StringBuilder compare = new StringBuilder();
				String line = lines.get(l);

				int logical = 0;
				int typical = 0;

				for(int i = 0; i < line.length(); i++)
				{
					char at = line.charAt(i);

					switch(at)
					{
						case ' ':
						case '.':
						case ',':
						case ':':
						case ';':
						case '-':
							logical = i;
							typical = 0;
							break;
						default:
							typical = i;
							break;
					}

					compare.append(at);

					if(metrics.stringWidth(compare.toString()) > textWrap)
					{
						int cut = Math.min(logical, typical);

						if(cut == 0)
						{
							cut = Math.max(logical, typical);

							if(cut == 0)
								break;
						}

						String begin = line.substring(0, cut + 1);
						String end = line.substring(cut + 1);

						lines.set(l, begin);

						if(!end.isEmpty())
							lines.add(l + 1, end);
						break;
					}
				}
			}

			paragraphs.add(lines);
		}

		bounds.width = 0;
		bounds.height = PADDING * 2 + (options == Options.NONE ? PARAGRAPH : OPTION) * (paragraphs.size() - 1);

		for(LinkedList<String> paragraph : paragraphs)
		{
			for(String line : paragraph)
				bounds.width = Math.max(bounds.width, metrics.stringWidth(line) + PADDING * 2);

			bounds.height += metrics.getAscent() * paragraph.size() + SPACING * (paragraph.size() - 1);
		}
	}

	public enum Alignment
	{
		TOP_LEFT,
		TOP_CENTER,
		TOP_RIGHT,
		BOTTOM_LEFT,
		BOTTOM_CENTER,
		BOTTOM_RIGHT
	}

	public enum Options
	{
		NONE,
		DEFAULT,
		NUMBERED
	}
}
