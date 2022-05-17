package com.polywoof;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

@Slf4j
public class PolywoofOverlay extends Overlay
{
	private static final int SPACING = 4;
	private static final BufferedImage BUTTON = ImageUtil.loadImageResource(PolywoofPlugin.class, "/button.png");

	private final Map<Integer, Component> permanent = new LinkedHashMap<>();
	private final LinkedList<Component> temporary = new LinkedList<>();
	private final Rectangle button = new Rectangle(0, 0, 30, 30);
	private Font font;

	@Inject
	private Client client;

	@Inject
	private PolywoofConfig config;

	@Inject
	private TooltipManager tooltipManager;

	@Override
	public Dimension render(Graphics2D graphics)
	{
		int offset = 0;
		OverlayPosition position = getPreferredPosition();
		LinkedList<Component> copy = new LinkedList<>();

		if(position == null)
			position = getPosition();

		copy.addAll(permanent.values());
		copy.addAll(temporary);

		if(config.button() && !getBounds().contains(client.getMouseCanvasPosition().getX(), client.getMouseCanvasPosition().getY()))
		{
			graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f));
			graphics.drawImage(BUTTON, button.x, button.y, button.width, button.height, null);
		}

		for(Component component : copy)
		{
			float alpha = 1f;

			if(component.getTimestamp() != null)
			{
				long difference = Math.max(0, component.getTimestamp() - System.currentTimeMillis());

				if(difference == 0L)
				{
					temporary.remove(component);
					continue;
				}

				alpha = Math.min(1000f, difference) / 1000f;
			}

			component.getComponent().build(graphics);

			switch(position)
			{
				case TOP_LEFT:
					PolywoofComponent.setAlignment(PolywoofComponent.Alignment.TOP_LEFT);
					component.getComponent().setLocation(button.x, button.y + offset);

					offset += component.getComponent().getBounds().height + SPACING;
					break;
				case TOP_CENTER:
					PolywoofComponent.setAlignment(PolywoofComponent.Alignment.TOP_CENTER);
					component.getComponent().setLocation(button.x + button.width / 2, button.y + offset);

					offset += component.getComponent().getBounds().height + SPACING;
					break;
				case TOP_RIGHT:
				case CANVAS_TOP_RIGHT:
					PolywoofComponent.setAlignment(PolywoofComponent.Alignment.TOP_RIGHT);
					component.getComponent().setLocation(button.x + button.width, button.y + offset);

					offset += component.getComponent().getBounds().height + SPACING;
					break;
				case BOTTOM_LEFT:
					PolywoofComponent.setAlignment(PolywoofComponent.Alignment.BOTTOM_LEFT);
					component.getComponent().setLocation(button.x, button.y + offset + button.height);

					offset -= component.getComponent().getBounds().height + SPACING;
					break;
				default:
				case ABOVE_CHATBOX_RIGHT:
					PolywoofComponent.setAlignment(PolywoofComponent.Alignment.BOTTOM_CENTER);
					component.getComponent().setLocation(button.x + button.width / 2, button.y + offset + button.height);

					offset -= component.getComponent().getBounds().height + SPACING;
					break;
				case BOTTOM_RIGHT:
					PolywoofComponent.setAlignment(PolywoofComponent.Alignment.BOTTOM_RIGHT);
					component.getComponent().setLocation(button.x + button.width, button.y + offset + button.height);

					offset -= component.getComponent().getBounds().height + SPACING;
					break;
			}

			component.getComponent().setAlpha(alpha);
			component.getComponent().render(graphics);
		}

		if(config.button() && getBounds().contains(client.getMouseCanvasPosition().getX(), client.getMouseCanvasPosition().getY()))
		{
			graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
			graphics.drawImage(BUTTON, button.x, button.y, button.width, button.height, null);

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
		if(text.length() > 0)
			temporary.addFirst(new Component(System.currentTimeMillis() + 1500L + readingSpeed(text), new PolywoofComponent(text, font)));
	}

	public void set(Integer id, String text)
	{
		if(text.length() > 0)
			permanent.put(id, new Component(null, new PolywoofComponent(text, font)));
	}

	public void vanish(Integer id)
	{
		if(permanent.containsKey(id))
		{
			temporary.addFirst(new Component(System.currentTimeMillis() + 1500L, permanent.get(id).getComponent()));
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

		for(Component component : permanent.values())
			component.getComponent().build();

		for(Component component : temporary)
			component.getComponent().build();
	}

	public void clear()
	{
		permanent.clear();
		temporary.clear();
	}

	@AllArgsConstructor
	private static class Component
	{
		@Getter
		private final Long timestamp;

		@Getter
		@NonNull
		private final PolywoofComponent component;
	}
}
