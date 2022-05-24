package com.polywoof;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@ParametersAreNonnullByDefault
public class PolywoofOverlay extends Overlay
{
	public static final int SPACING = 4;
	public static final int CAPACITY = 10;
	public static final BufferedImage IMAGE = ImageUtil.loadImageResource(PolywoofPlugin.class, "/button.png");

	private final Map<Byte, Box> permanent = new HashMap<>(PolywoofPlugin.ID.CAPACITY);
	private final List<Box> temporary = new ArrayList<>(CAPACITY);
	private final Rectangle rectangle = new Rectangle(0, 0, 30, 30);
	private Font font;
	private AlphaComposite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);

	@Inject private Client client;
	@Inject private PolywoofConfig config;
	@Inject private TooltipManager tooltipManager;
	@Getter private boolean mouseOver;

	@Override
	public Dimension render(Graphics2D graphics)
	{
		List<Box> copy = new ArrayList<>(permanent.size() + temporary.size());

		copy.addAll(permanent.values());
		copy.addAll(temporary);

		PolywoofComponent.setPosition(getCurrentPosition());

		if(config.showButton() && !mouseOver)
		{
			graphics.setComposite(composite = composite.derive(0.1f));
			graphics.drawImage(IMAGE, rectangle.x, rectangle.y, rectangle.width, rectangle.height, null);
		}

		int offset = 0;

		for(Box box : copy)
		{
			if(box.timestamp != 0)
			{
				long difference = Math.max(0, box.timestamp - System.currentTimeMillis());

				if(difference == 0)
				{
					temporary.remove(box);
					continue;
				}

				box.component.setAlpha(Math.min(1000f, difference) / 1000f);
			}

			box.component.update(graphics);

			int x = 0;
			int y = 0;

			switch(PolywoofComponent.getAlignment())
			{
				case TOP_LEFT:
					x = 0;
					y = offset;
					offset += box.component.getSize().height + SPACING;
					break;
				case TOP_CENTER:
					x = rectangle.width / 2;
					y = offset;
					offset += box.component.getSize().height + SPACING;
					break;
				case TOP_RIGHT:
					x = rectangle.width;
					y = offset;
					offset += box.component.getSize().height + SPACING;
					break;
				case BOTTOM_LEFT:
					x = 0;
					y = offset + rectangle.height;
					offset -= box.component.getSize().height + SPACING;
					break;
				case BOTTOM_CENTER:
					x = rectangle.width / 2;
					y = offset + rectangle.height;
					offset -= box.component.getSize().height + SPACING;
					break;
				case BOTTOM_RIGHT:
					x = rectangle.width;
					y = offset + rectangle.height;
					offset -= box.component.getSize().height + SPACING;
					break;
			}

			box.component.setLocation(rectangle.x + x, rectangle.y + y);
			box.component.render(graphics);
		}

		if(config.showButton() && mouseOver)
		{
			graphics.setComposite(composite = composite.derive(1f));
			graphics.drawImage(IMAGE, rectangle.x, rectangle.y, rectangle.width, rectangle.height, null);

			if(!client.isMenuOpen())
				tooltipManager.addFront(new Tooltip("Polywoof is " + (config.toggle() ? ColorUtil.wrapWithColorTag("On", Color.GREEN) : ColorUtil.wrapWithColorTag("Off", Color.RED))));
		}

		mouseOver = false;

		return rectangle.getSize();
	}

	@Override
	public void onMouseOver()
	{
		mouseOver = true;
	}

	@Override
	public void revalidate()
	{
		font = new Font(config.fontName(), Font.PLAIN, config.fontSize());

		PolywoofComponent.setTextWrap(config.textWrap());
		PolywoofComponent.setTextShadow(config.textShadow());
		PolywoofComponent.setBoxOutline(config.overlayOutline());
		PolywoofComponent.setBackgroundColor(config.overlayColor());

		for(Box box : permanent.values())
		{
			box.component.setFontSize(config.fontSize());
			box.component.setNumberedOptions(config.numberedOptions());
			box.component.revalidate();
		}

		for(Box box : temporary)
		{
			box.component.setFontSize(config.fontSize());
			box.component.setNumberedOptions(config.numberedOptions());
			box.component.revalidate();
		}
	}

	public OverlayPosition getCurrentPosition()
	{
		return getPreferredPosition() == null ? getPosition() : getPreferredPosition();
	}

	public long readingSpeed(String text)
	{
		return (long) (1000f * text.length() * (1f / config.readingSpeed()));
	}

	public void put(String text)
	{
		if(text.isEmpty())
			return;

		if(temporary.size() >= CAPACITY)
			temporary.remove(temporary.size() - 1);

		temporary.add(0, new Box(System.currentTimeMillis() + 1500L + readingSpeed(text), new PolywoofComponent(text, font, PolywoofComponent.Options.NONE)));
	}

	public void set(byte id, String text, PolywoofComponent.Options options)
	{
		if(text.isEmpty())
			return;

		permanent.put(id, new Box(0, new PolywoofComponent(text, font, options)));
	}

	public void clear(byte id)
	{
		if(!permanent.containsKey(id))
			return;

		if(temporary.size() >= CAPACITY)
			temporary.remove(temporary.size() - 1);

		temporary.add(0, new Box(System.currentTimeMillis() + 1500L, permanent.get(id).component));
		permanent.remove(id);
	}

	public void clear()
	{
		permanent.clear();
		temporary.clear();
	}

	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	public static class Box
	{
		public final long timestamp;
		public final PolywoofComponent component;
	}
}
