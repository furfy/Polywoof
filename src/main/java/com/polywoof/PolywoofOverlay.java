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
					offset += draw(graphics, subtitle.getValue(), 10, 10 + offset, alpha, TextAlignment.TOP_LEFT) + spacing;
					break;
				case TOP_CENTER:
					offset += draw(graphics, subtitle.getValue(), 0, 10 + offset, alpha, TextAlignment.TOP_CENTER) + spacing;
					break;
				case TOP_RIGHT:
					offset += draw(graphics, subtitle.getValue(), -10, 10 + offset, alpha, TextAlignment.TOP_RIGHT) + spacing;
					break;
				case BOTTOM_LEFT:
					offset -= draw(graphics, subtitle.getValue(), 10, offset - 10, alpha, TextAlignment.BOTTOM_LEFT) + spacing;
					break;
				case ABOVE_CHATBOX_RIGHT:
					offset -= draw(graphics, subtitle.getValue(), 0, offset - 10, alpha, TextAlignment.BOTTOM_CENTER) + spacing;
					break;
				case BOTTOM_RIGHT:
					offset -= draw(graphics, subtitle.getValue(), -10, offset - 10, alpha, TextAlignment.BOTTOM_RIGHT) + spacing;
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

	public void clear()
	{
		permanent.clear();
		temporary.clear();
	}

	private int draw(Graphics2D graphics, String text, int x, int y, float alpha, TextAlignment alignment)
	{
		FontMetrics metrics = graphics.getFontMetrics(font);
		LinkedList<String> list = wrap(text, metrics);

		int ascent = metrics.getAscent();
		int width = 0;
		int height = 0;
		int padding = 8;
		int spacing = 4;

		for(String line : list)
		{
			width = Math.max(width, metrics.stringWidth(line) + padding * 2);
			height += ascent + spacing;
		}

		int offset_x = 0;
		int offset_y = 0;
		height -= spacing - padding * 2;

		switch (alignment)
		{
			case TOP_LEFT:
				offset_x = x - padding;
				offset_y = y - padding;
				break;
			case TOP_CENTER:
				offset_x = x - width / 2;
				offset_y = y - padding;
				break;
			case TOP_RIGHT:
				offset_x = x + padding - width;
				offset_y = y - padding;
				break;
			case BOTTOM_LEFT:
				offset_x = x - padding;
				offset_y = y + padding - height;
				break;
			case BOTTOM_CENTER:
				offset_x = x - width / 2;
				offset_y = y + padding - height;
				break;
			case BOTTOM_RIGHT:
				offset_x = x + padding - width;
				offset_y = y + padding - height;
				break;
		}

		Color color = config.overlayColor();
		int a = Math.round(color.getAlpha() * alpha);

		graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), a));
		graphics.fillRect(offset_x, offset_y, width, height);

		if(config.overlayOutline())
		{
			color = color.brighter();

			graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), a));
			graphics.drawRect(offset_x + 1, offset_y + 1, width - 3, height - 3);
		}

		int offset = 0;

		graphics.setFont(font);

		for(String line : list)
		{
			switch (alignment)
			{
				case TOP_LEFT:
					offset_x = x;
					offset_y = y + offset + ascent;
					break;
				case TOP_CENTER:
					offset_x = x - metrics.stringWidth(line) / 2;
					offset_y = y + offset + ascent;
					break;
				case TOP_RIGHT:
					offset_x = x - metrics.stringWidth(line);
					offset_y = y + offset + ascent;
					break;
				case BOTTOM_LEFT:
					offset_x = x;
					offset_y = y + offset + ascent + padding * 2 - height;
					break;
				case BOTTOM_CENTER:
					offset_x = x - metrics.stringWidth(line) / 2;
					offset_y = y + offset + ascent + padding * 2 - height;
					break;
				case BOTTOM_RIGHT:
					offset_x = x - metrics.stringWidth(line);
					offset_y = y + padding * 2 + offset + ascent - height;
					break;
			}

			if(config.textShadow())
			{
				color = Color.BLACK;
				a = Math.round(color.getAlpha() * alpha);

				graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), a));
				graphics.drawString(line, offset_x + 1, offset_y + 1);
			}

			color = Color.WHITE;
			a = Math.round(color.getAlpha() * alpha);

			graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), a));
			graphics.drawString(line, offset_x, offset_y);

			offset += ascent + spacing;
		}

		return height;
	}

	private LinkedList<String> wrap(String text, FontMetrics metrics)
	{
		LinkedList<String> list = new LinkedList<>(Arrays.asList(text.split("\n")));

		for(int l = 0; l < list.size(); l++)
		{
			StringBuilder compare = new StringBuilder();
			String line = list.get(l);

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

				if(metrics.stringWidth(compare.toString()) > config.wrapWidth())
				{
					int cut = Math.min(logical, character);

					if(cut == 0)
					{
						cut = Math.max(logical, character);

						if(cut == 0) break;
					}

					String beg = line.substring(0, cut + 1);
					String end = line.substring(cut + 1);

					list.set(l, beg);

					if(end.length() > 0) list.add(l + 1, end);
					break;
				}
			}
		}

		return list;
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
