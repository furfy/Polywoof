package com.polywoof;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
public class PolywoofOverlay extends Overlay
{
	@Inject
	private Client client;

	@Inject
	private PolywoofConfig config;

	@Inject
	private TooltipManager tooltipManager;

	private static final int MARGIN = 12;
	private static final int SPACING = 4;
	private static final BufferedImage BUTTON = ImageUtil.loadImageResource(PolywoofPlugin.class, "/button.png");

	private final Map<Integer, PolywoofComponent> permanent = new LinkedHashMap<>();
	private final Map<Long, PolywoofComponent> temporary = new LinkedHashMap<>();
	private final Rectangle button = new Rectangle(0, 0, 30, 30);
	private Font font;

	@Override
	public Dimension render(Graphics2D graphics)
	{
		int offset = 0;
		Map<Long, PolywoofComponent> copy = new LinkedHashMap<>();

		for(Map.Entry<Integer, PolywoofComponent> entry : permanent.entrySet())
			copy.put(0L, entry.getValue());

		copy.putAll(temporary);

		if(config.button() && !getBounds().contains(client.getMouseCanvasPosition().getX(), client.getMouseCanvasPosition().getY()))
		{
			graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f));
			graphics.drawImage(BUTTON, button.x, button.y, button.width, button.height, null);
		}

		for(Map.Entry<Long, PolywoofComponent> entry : copy.entrySet())
		{
			float alpha = 1f;
			OverlayPosition position = getPreferredPosition();

			if(position == null)
				position = getPosition();

			if(entry.getKey() != 0)
			{
				long difference = Math.max(0, entry.getKey() - System.currentTimeMillis());

				if(difference == 0)
				{
					temporary.remove(entry.getKey());
					continue;
				}

				alpha = Math.min(1000f, difference) / 1000f;
			}

			switch(position)
			{
				case TOP_LEFT:
					PolywoofComponent.setAlignment(PolywoofComponent.Alignment.TOP_LEFT);
					entry.getValue().setLocation(MARGIN, MARGIN + offset);
					offset += entry.getValue().getBounds().height + SPACING;
					break;
				case TOP_CENTER:
					PolywoofComponent.setAlignment(PolywoofComponent.Alignment.TOP_CENTER);
					entry.getValue().setLocation(button.width / 2, MARGIN + offset);
					offset += entry.getValue().getBounds().height + SPACING;
					break;
				case TOP_RIGHT:
					PolywoofComponent.setAlignment(PolywoofComponent.Alignment.TOP_RIGHT);
					entry.getValue().setLocation(button.width - MARGIN, MARGIN + offset);
					offset += entry.getValue().getBounds().height + SPACING;
					break;
				case BOTTOM_LEFT:
					PolywoofComponent.setAlignment(PolywoofComponent.Alignment.BOTTOM_LEFT);
					entry.getValue().setLocation(MARGIN, offset - MARGIN + button.height);
					offset -= entry.getValue().getBounds().height + SPACING;
					break;
				case BOTTOM_RIGHT:
					PolywoofComponent.setAlignment(PolywoofComponent.Alignment.BOTTOM_RIGHT);
					entry.getValue().setLocation(button.width - MARGIN, offset - MARGIN + button.height);
					offset -= entry.getValue().getBounds().height + SPACING;
					break;
				default:
					PolywoofComponent.setAlignment(PolywoofComponent.Alignment.BOTTOM_CENTER);
					entry.getValue().setLocation(button.width / 2, offset - MARGIN + button.height);
					offset -= entry.getValue().getBounds().height + SPACING;
					break;
			}

			entry.getValue().setAlpha(alpha);
			entry.getValue().render(graphics);
		}

		if(config.button() && getBounds().contains(client.getMouseCanvasPosition().getX(), client.getMouseCanvasPosition().getY()))
		{
			graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
			graphics.drawImage(BUTTON, button.x, button.y, button.width, button.height, null);

			if(!client.isMenuOpen())
				tooltipManager.addFront(new Tooltip("Polywoof is " + (config.toggle() ? "<col=00ff00>On" : "<col=ff0000>Off") + "</col>"));
		}

		return button.getSize();
	}

	public void put(String text)
	{
		if(text.length() > 0)
			temporary.put(System.currentTimeMillis() + 1500L + (long) (1000f * text.length() * (1f / config.readingSpeed())), new PolywoofComponent(text, font));
	}

	public void set(Integer id, String text)
	{
		if(text.length() > 0)
			permanent.put(id, new PolywoofComponent(text, font));
	}

	public void vanish(Integer id)
	{
		if(permanent.containsKey(id))
		{
			temporary.put(System.currentTimeMillis() + 1500L, permanent.get(id));
			permanent.remove(id);
		}
	}

	public void update()
	{
		font = new Font(config.fontName(), Font.PLAIN, config.fontSize());

		PolywoofComponent.setTextWrap(config.textWrap());
		PolywoofComponent.setTextShadow(config.textShadow());
		PolywoofComponent.setBoxOutline(config.overlayOutline());
		PolywoofComponent.setColor(config.overlayColor());

		for(Map.Entry<Integer, PolywoofComponent> entry : permanent.entrySet())
			entry.getValue().build();

		for(Map.Entry<Long, PolywoofComponent> entry : temporary.entrySet())
			entry.getValue().build();
	}

	public void clear()
	{
		permanent.clear();
		temporary.clear();
	}
}
