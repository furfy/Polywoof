package com.polywoof;

import lombok.Setter;
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

	private final Rectangle bounds = new Rectangle();
	private final String text;
	private final Font font;
	private FontMetrics metrics;
	private AlphaComposite alpha;
	private LinkedList<String> lines;

	@Setter
	private static int textWrap = 480;

	@Setter
	private static boolean textShadow = true;

	@Setter
	private static boolean boxOutline = true;

	@Setter
	private static Color backgroundColor = ComponentConstants.STANDARD_BACKGROUND_COLOR;

	@Setter
	private static Alignment alignment = Alignment.BOTTOM_LEFT;

	public PolywoofComponent(String text, Font font)
	{
		super();

		this.text = text;
		this.font = font;

		setAlpha(1f);
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

		for(String line : lines)
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

	public void build(Graphics2D graphics)
	{
		metrics = graphics.getFontMetrics(font);

		if(lines == null)
			build();
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
		bounds.height = metrics.getAscent() * lines.size() + SPACING * (lines.size() - 1) + PADDING * 2;

		for(String line : lines)
			bounds.width = Math.max(bounds.width, metrics.stringWidth(line) + PADDING * 2);
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
