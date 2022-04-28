package com.polywoof;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;

import java.awt.*;
import java.util.Arrays;
import java.util.LinkedList;

@Slf4j
public class PolywoofComponent implements LayoutableRenderableEntity
{
	private static final int SPACING = 4;
	private static final int PADDING = 8;

	private static int wrap = 480;
	private static boolean shadow = true;
	private static boolean outline = true;
	private static Color color = ComponentConstants.STANDARD_BACKGROUND_COLOR;
	private static Alignment alignment = Alignment.BOTTOM_LEFT;

	private final Rectangle bounds = new Rectangle();
	private final String text;
	private final Font font;
	private FontMetrics metrics;
	private AlphaComposite alpha;
	private LinkedList<String> lines;

	public PolywoofComponent(String text, Font font)
	{
		super();

		this.text = text;
		this.font = font;

		setAlpha(1f);
	}

	public static void setTextWrap(int wrap)
	{
		PolywoofComponent.wrap = wrap;
	}

	public static void setTextShadow(boolean shadow)
	{
		PolywoofComponent.shadow = shadow;
	}

	public static void setBoxOutline(boolean outline)
	{
		PolywoofComponent.outline = outline;
	}

	public static void setColor(Color color)
	{
		PolywoofComponent.color = color;
	}

	public static void setAlignment(Alignment alignment)
	{
		PolywoofComponent.alignment = alignment;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		metrics = graphics.getFontMetrics(font);

		if(lines == null)
			build();

		int offset_x = 0;
		int offset_y = 0;

		switch(alignment)
		{
			case TOP_LEFT:
				offset_x = bounds.x - PADDING;
				offset_y = bounds.y - PADDING;
				break;
			case TOP_CENTER:
				offset_x = bounds.x - bounds.width / 2;
				offset_y = bounds.y - PADDING;
				break;
			case TOP_RIGHT:
				offset_x = bounds.x + PADDING - bounds.width;
				offset_y = bounds.y - PADDING;
				break;
			case BOTTOM_LEFT:
				offset_x = bounds.x - PADDING;
				offset_y = bounds.y + PADDING - bounds.height;
				break;
			case BOTTOM_CENTER:
				offset_x = bounds.x - bounds.width / 2;
				offset_y = bounds.y + PADDING - bounds.height;
				break;
			case BOTTOM_RIGHT:
				offset_x = bounds.x + PADDING - bounds.width;
				offset_y = bounds.y + PADDING - bounds.height;
				break;
		}

		graphics.setComposite(alpha);
		graphics.setColor(color);
		graphics.fillRect(offset_x, offset_y, bounds.width, bounds.height);

		if(outline)
		{
			graphics.setColor(color.brighter());
			graphics.drawRect(offset_x + 1, offset_y + 1, bounds.width - 3, bounds.height - 3);
		}

		int offset = 0;
		int ascent = metrics.getAscent();

		graphics.setFont(font);

		for(String line : lines)
		{
			switch(alignment)
			{
				case TOP_LEFT:
					offset_x = bounds.x;
					offset_y = bounds.y + offset + ascent;
					break;
				case TOP_CENTER:
					offset_x = bounds.x - metrics.stringWidth(line) / 2;
					offset_y = bounds.y + offset + ascent;
					break;
				case TOP_RIGHT:
					offset_x = bounds.x - metrics.stringWidth(line);
					offset_y = bounds.y + offset + ascent;
					break;
				case BOTTOM_LEFT:
					offset_x = bounds.x;
					offset_y = bounds.y + offset + ascent + PADDING * 2 - bounds.height;
					break;
				case BOTTOM_CENTER:
					offset_x = bounds.x - metrics.stringWidth(line) / 2;
					offset_y = bounds.y + offset + ascent + PADDING * 2 - bounds.height;
					break;
				case BOTTOM_RIGHT:
					offset_x = bounds.x - metrics.stringWidth(line);
					offset_y = bounds.y + PADDING * 2 + offset + ascent - bounds.height;
					break;
			}

			if(shadow)
			{
				graphics.setColor(Color.BLACK);
				graphics.drawString(line, offset_x + 1, offset_y + 1);
			}

			graphics.setColor(Color.WHITE);
			graphics.drawString(line, offset_x, offset_y);

			offset += ascent + SPACING;
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
		build();
	}

	public void setAlpha(float alpha)
	{
		this.alpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
	}

	public void build()
	{
		if(metrics == null)
			return;

		lines = new LinkedList<>(Arrays.asList(text.split("\n")));

		for(int l = 0; l < lines.size(); l++)
		{
			StringBuilder compare = new StringBuilder();
			String line = lines.get(l);

			int logical = 0;
			int character = 0;

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
						character = 0;
						break;
					default:
						character = i;
						break;
				}

				compare.append(at);

				if(metrics.stringWidth(compare.toString()) > wrap)
				{
					int cut = Math.min(logical, character);

					if(cut == 0)
					{
						cut = Math.max(logical, character);

						if(cut == 0)
							break;
					}

					String beg = line.substring(0, cut + 1);
					String end = line.substring(cut + 1);

					lines.set(l, beg);

					if(end.length() > 0)
						lines.add(l + 1, end);
					break;
				}
			}
		}

		bounds.width = 0;
		bounds.height = 0;

		for(String line : lines)
		{
			bounds.width = Math.max(bounds.width, metrics.stringWidth(line) + PADDING * 2);
			bounds.height += metrics.getAscent() + SPACING;
		}

		bounds.height -= SPACING - PADDING * 2;
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
}
