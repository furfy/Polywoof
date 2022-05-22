package com.polywoof;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

@Slf4j
@ParametersAreNonnullByDefault
public class PolywoofOverlay extends Overlay
{
	private static final int SPACING = 4;
	private static final BufferedImage IMAGE = ImageUtil.loadImageResource(PolywoofPlugin.class, "/button.png");

	private final Map<Integer, Box> permanent = new LinkedHashMap<>();
	private final LinkedList<Box> temporary = new LinkedList<>();
	private final Rectangle button = new Rectangle(0, 0, 30, 30);
	private Font font;

	@Inject private Client client;
	@Inject private PolywoofConfig config;
	@Inject private TooltipManager tooltipManager;

	@Override
	public Dimension render(Graphics2D graphics)
	{
		int offset = 0;
		LinkedList<Box> copy = new LinkedList<>();

		copy.addAll(permanent.values());
		copy.addAll(temporary);

		PolywoofComponent.setPosition(getPreferredPosition() == null ? getPosition() : getPreferredPosition());

		if(config.showButton() && !getBounds().contains(client.getMouseCanvasPosition().getX(), client.getMouseCanvasPosition().getY()))
		{
			graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f));
			graphics.drawImage(IMAGE, button.x, button.y, button.width, button.height, null);
		}

		for(Box box : copy)
		{
			int x = 0;
			int y = 0;

			if(box.timestamp != 0L)
			{
				long difference = Math.max(0, box.timestamp - System.currentTimeMillis());

				if(difference == 0L)
				{
					temporary.remove(box);
					continue;
				}

				box.component.setAlpha(Math.min(1000f, difference) / 1000f);
			}

			box.component.update(graphics);

			switch(PolywoofComponent.getAlignment())
			{
				case TOP_LEFT:
					x = button.x;
					y = button.y + offset;
					offset += box.component.getBounds().height + SPACING;
					break;
				case TOP_CENTER:
					x = button.x + button.width / 2;
					y = button.y + offset;
					offset += box.component.getBounds().height + SPACING;
					break;
				case TOP_RIGHT:
					x = button.x + button.width;
					y = button.y + offset;
					offset += box.component.getBounds().height + SPACING;
					break;
				case BOTTOM_LEFT:
					x = button.x;
					y = button.y + offset + button.height;
					offset -= box.component.getBounds().height + SPACING;
					break;
				case BOTTOM_CENTER:
					x = button.x + button.width / 2;
					y = button.y + offset + button.height;
					offset -= box.component.getBounds().height + SPACING;
					break;
				case BOTTOM_RIGHT:
					x = button.x + button.width;
					y = button.y + offset + button.height;
					offset -= box.component.getBounds().height + SPACING;
					break;
			}

			box.component.setLocation(x, y);
			box.component.render(graphics);
		}

		if(config.showButton() && getBounds().contains(client.getMouseCanvasPosition().getX(), client.getMouseCanvasPosition().getY()))
		{
			graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
			graphics.drawImage(IMAGE, button.x, button.y, button.width, button.height, null);

			if(!client.isMenuOpen())
				tooltipManager.addFront(new Tooltip("Polywoof is " + (config.toggle() ? ColorUtil.wrapWithColorTag("On", Color.GREEN) : ColorUtil.wrapWithColorTag("Off", Color.RED))));
		}

		return button.getSize();
	}

	public long readingSpeed(String text)
	{
		return (long) (1000f * text.length() * (1f / config.readingSpeed()));
	}

	public void put(String text)
	{
		if(!text.isEmpty())
			temporary.addFirst(new Box(System.currentTimeMillis() + 1500L + readingSpeed(text), new PolywoofComponent(text, font, PolywoofComponent.Options.NONE)));
	}

	public void set(Integer id, String text, PolywoofComponent.Options options)
	{
		if(!text.isEmpty())
			permanent.put(id, new Box(0, new PolywoofComponent(text, font, options)));
	}

	public void vanish(Integer id)
	{
		if(permanent.containsKey(id))
		{
			temporary.addFirst(new Box(System.currentTimeMillis() + 1500L, permanent.get(id).component));
			permanent.remove(id);
		}
	}

	public void update()
	{
		font = new Font(config.fontName(), Font.PLAIN, config.fontSize());

		PolywoofComponent.setTextWrap(config.textWrap());
		PolywoofComponent.setTextShadow(config.textShadow());
		PolywoofComponent.setBoxOutline(config.overlayOutline());
		PolywoofComponent.setBackgroundColor(config.overlayColor());

		for(Box box : permanent.values())
			box.component.update();

		for(Box box : temporary)
			box.component.update();
	}

	public void clear()
	{
		permanent.clear();
		temporary.clear();
	}

	@AllArgsConstructor
	private static class Box
	{
		private final long timestamp;
		private final PolywoofComponent component;
	}
}
