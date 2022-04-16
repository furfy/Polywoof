package com.polywoof;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.overlay.Overlay;

import javax.inject.Inject;
import java.awt.*;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

@Slf4j

public class PolywoofOverlay extends Overlay
{
	@Inject
	private PolywoofConfig config;

	private final Map<Integer, String> permanent = new LinkedHashMap<>();
	private final Map<Long, String> temporary = new LinkedHashMap<>();
	private Font font;

	@Override
	public Dimension render(Graphics2D graphics)
	{
		int offset = 0;
		int spacing = 4;

		Map<Long, String> copy = new LinkedHashMap<>();

		for(Map.Entry<Integer, String> subtitle : permanent.entrySet())
		{
			copy.put(0L, subtitle.getValue());
		}

		copy.putAll(temporary);

		for(Map.Entry<Long, String> subtitle : copy.entrySet())
		{
			float alpha = 1f;

			if(subtitle.getKey() != 0)
			{
				long difference = Math.max(0, subtitle.getKey() - System.currentTimeMillis());

				if(difference == 0)
				{
					temporary.remove(subtitle.getKey());
					continue;
				}

				alpha = Math.min(1000f, difference) / 1000f;
			}

			switch (getPosition())
			{
				case TOP_LEFT:
					offset += draw(graphics, subtitle.getValue(), font, 10, 10 + offset, alpha, TextAlignment.TOP_LEFT) + spacing;
					break;
				case TOP_CENTER:
					offset += draw(graphics, subtitle.getValue(), font, 0, 10 + offset, alpha, TextAlignment.TOP_CENTER) + spacing;
					break;
				case TOP_RIGHT:
				case CANVAS_TOP_RIGHT:
					offset += draw(graphics, subtitle.getValue(), font, -10, 10 + offset, alpha, TextAlignment.TOP_RIGHT) + spacing;
					break;
				case BOTTOM_LEFT:
					offset -= draw(graphics, subtitle.getValue(), font, 10, offset - 10, alpha, TextAlignment.BOTTOM_LEFT) + spacing;
					break;
				case ABOVE_CHATBOX_RIGHT:
					offset -= draw(graphics, subtitle.getValue(), font, 0, offset - 10, alpha, TextAlignment.BOTTOM_CENTER) + spacing;
					break;
				case BOTTOM_RIGHT:
					offset -= draw(graphics, subtitle.getValue(), font, -10, offset - 10, alpha, TextAlignment.BOTTOM_RIGHT) + spacing;
					break;
			}
		}

		return null;
	}

	public void put(String text)
	{
		if(text.length() > 0) temporary.put(2000L + System.currentTimeMillis() + (long)(1000f * text.length() * (1f / config.readingSpeed())), text);
	}

	public void set(Integer id, String text)
	{
		if(text.length() > 0) permanent.put(id, text);
	}

	public void vanish(Integer id)
	{
		if(permanent.containsKey(id))
		{
			temporary.put(System.currentTimeMillis() + 1500, permanent.get(id));
			permanent.remove(id);
		}
	}

	public void update()
	{
		font = new Font(config.fontName(), Font.PLAIN, config.fontSize());
	}

	private int draw(Graphics2D graphics, String text, Font font, int x, int y, float alpha, TextAlignment alignment)
	{
		LinkedList<String> lines = new LinkedList<>(Arrays.asList(text.split("\n")));
		FontMetrics metrics = graphics.getFontMetrics(font);

		int ascent = metrics.getAscent();
		int padding = 8;
		int spacing = 4;
		int width = 0;
		int height = padding * 2;

		for(int l = 0; l < lines.size(); l++)
		{
			StringBuilder compare = new StringBuilder();
			String line = lines.get(l);

			int logical = 0;
			int character = 0;

			for(int i = 0; i < line.length(); i++)
			{
				char at = line.charAt(i);

				switch (at)
				{
					case ' ':
					case '.':
					case ',':
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

				if(metrics.stringWidth(compare.toString()) > config.wrapWidth())
				{
					int cut = Math.min(logical, character);

					if(cut == 0)
					{
						cut = Math.max(logical, character);

						if(cut == 0) break;
					}

					lines.set(l, line.substring(0, cut + 1).trim());

					String sub = line.substring(cut + 1).trim();

					if(sub.length() > 0) lines.add(l + 1, sub);
					break;
				}
			}
		}

		for(String line : lines)
		{
			width = Math.max(width, metrics.stringWidth(line) + padding * 2);
			height += ascent + spacing;
		}

		height -= spacing;
		Color c1 = config.overlayColor();

		graphics.setColor(new Color(c1.getRed(), c1.getGreen(), c1.getBlue(), Math.round(c1.getAlpha() * alpha)));

		switch (alignment)
		{
			case TOP_LEFT:
				graphics.fillRect(x - padding, y - padding, width, height);
				break;
			case TOP_CENTER:
				graphics.fillRect(x - width / 2, y - padding, width, height);
				break;
			case TOP_RIGHT:
				graphics.fillRect(x + padding - width, y - padding, width, height);
				break;
			case BOTTOM_LEFT:
				graphics.fillRect(x - padding, y + padding - height, width, height);
				break;
			case BOTTOM_CENTER:
				graphics.fillRect(x - width / 2, y + padding - height, width, height);
				break;
			case BOTTOM_RIGHT:
				graphics.fillRect(x + padding - width, y + padding - height, width, height);
				break;
		}

		int offset = 0;
		Color c2 = Color.WHITE;

		graphics.setFont(font);
		graphics.setColor(new Color(c2.getRed(), c2.getGreen(), c2.getBlue(), Math.round(c2.getAlpha() * alpha)));

		for(String line : lines)
		{
			switch (alignment)
			{
				case TOP_LEFT:
					graphics.drawString(line, x, y + offset + ascent);
					break;
				case TOP_CENTER:
					graphics.drawString(line, x - metrics.stringWidth(line) / 2, y + offset + ascent);
					break;
				case TOP_RIGHT:
					graphics.drawString(line, x - metrics.stringWidth(line), y + offset + ascent);
					break;
				case BOTTOM_LEFT:
					graphics.drawString(line, x, y + offset + ascent + padding * 2 - height);
					break;
				case BOTTOM_CENTER:
					graphics.drawString(line, x - metrics.stringWidth(line) / 2, y + offset + ascent + padding * 2 - height);
					break;
				case BOTTOM_RIGHT:
					graphics.drawString(line, x - metrics.stringWidth(line), y + padding * 2 + offset + ascent - height);
					break;
			}

			offset += ascent + spacing;
		}

		return height;
	}

	public enum TextAlignment
	{
		TOP_LEFT,
		TOP_CENTER,
		TOP_RIGHT,
		BOTTOM_LEFT,
		BOTTOM_CENTER,
		BOTTOM_RIGHT
	}
}
