package com.polywoof;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.overlay.Overlay;

import javax.inject.Inject;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j

public class PolywoofOverlay extends Overlay
{
    @Inject
    private PolywoofConfig config;

    private Font font;
    private final Map<Integer, String> permament = new LinkedHashMap<>();
    private final Map<Long, String> subtitles = new LinkedHashMap<>();

    public enum TextAlignment
    {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        int offset = 0;
        int spacing = 4;

        Map<Long, String> copy = new LinkedHashMap<>();

        for(Map.Entry<Integer, String> subtitle : permament.entrySet())
        {
            copy.put(0L, subtitle.getValue());
        }

        copy.putAll(subtitles);

        for(Map.Entry<Long, String> subtitle : copy.entrySet())
        {
            float alpha = 1f;

            if(subtitle.getKey() != 0)
            {
                long difference = Math.max(0, subtitle.getKey() - System.currentTimeMillis());

                if(difference == 0)
                {
                    subtitles.remove(subtitle.getKey());
                    continue;
                }

                alpha = Math.min(1000f, difference) / 1000f;
            }
            switch (getPosition())
            {
                case TOP_LEFT:
                case TOP_CENTER:
                    offset += draw(graphics, subtitle.getValue(), font, 10, 10 + offset, alpha, TextAlignment.TOP_LEFT) + spacing;
                    break;
                case TOP_RIGHT:
                case CANVAS_TOP_RIGHT:
                    offset += draw(graphics, subtitle.getValue(), font, -10, 10 + offset, alpha, TextAlignment.TOP_RIGHT) + spacing;
                    break;
                case BOTTOM_LEFT:
                    offset -= draw(graphics, subtitle.getValue(), font, 10, offset - 10, alpha, TextAlignment.BOTTOM_LEFT) + spacing;
                    break;
                case BOTTOM_RIGHT:
                case ABOVE_CHATBOX_RIGHT:
                    offset -= draw(graphics, subtitle.getValue(), font, -10, offset - 10, alpha, TextAlignment.BOTTOM_RIGHT) + spacing;
                    break;
            }
        }

        return null;
    }

    public void put(String text)
    {
        subtitles.put(System.currentTimeMillis() + 10000, text);
    }

    public void set(Integer id, String text)
    {
        permament.put(id, text);
    }

    public void vanish(Integer id)
    {
        if(permament.containsKey(id))
        {
            subtitles.put(System.currentTimeMillis() + 1500, permament.get(id));
            permament.remove(id);
        }
    }

    public void update()
    {
        font = new Font(config.font(), Font.PLAIN, config.size());
    }

    private int draw(Graphics2D graphics, String text, Font font, int x, int y, float alpha, TextAlignment alignment)
    {
        FontMetrics metrics = graphics.getFontMetrics(font);
        String[] lines = text.split("\n");
        int ascent = metrics.getAscent();
        int padding = 8;
        int spacing = 4;
        int width = 0;
        int height = padding * 2;

        for(String line : lines)
        {
            width = Math.max(width, metrics.stringWidth(line) + padding * 2);
            height += ascent + spacing;
        }

        height -= spacing;
        Color c1 = config.color();

        graphics.setColor(new Color(c1.getRed(), c1.getGreen(), c1.getBlue(), Math.round(c1.getAlpha() * alpha)));

        switch (alignment)
        {
            case TOP_LEFT:
                graphics.fillRect(x - padding, y - padding, width, height);
                break;
            case TOP_RIGHT:
                graphics.fillRect(x + padding - width, y - padding, width, height);
                break;
            case BOTTOM_LEFT:
                graphics.fillRect(x - padding, y + padding - height, width, height);
                break;
            case BOTTOM_RIGHT:
                graphics.fillRect(x + padding - width, y + padding - height, width, height);
                break;
        }

        Color c2 = Color.WHITE;

        graphics.setFont(font);
        graphics.setColor(new Color(c2.getRed(), c2.getGreen(), c2.getBlue(), Math.round(c2.getAlpha() * alpha)));

        int offset = 0;

        for(String line : lines)
        {
            switch (alignment)
            {
                case TOP_LEFT:
                    graphics.drawString(line, x, y + offset + ascent);
                    break;
                case TOP_RIGHT:
                    graphics.drawString(line, x - metrics.stringWidth(line), y + offset + ascent);
                    break;
                case BOTTOM_LEFT:
                    graphics.drawString(line, x, y + offset + ascent + padding * 2 - height);
                    break;
                case BOTTOM_RIGHT:
                    graphics.drawString(line, x - metrics.stringWidth(line), y + padding * 2 + offset + ascent - height);
                    break;
            }

            offset += ascent + spacing;
        }

        return height;
    }
}