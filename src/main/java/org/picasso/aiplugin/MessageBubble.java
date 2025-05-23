package org.picasso.aiplugin;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;

import java.util.Date;

public class MessageBubble extends JPanel {

	public MessageBubble(String text, boolean fromCurrentUser) {
		setLayout(new BorderLayout());
		setBackground(fromCurrentUser ? new Color(180, 220, 255) : new Color(220, 220, 220));
		setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

		String time = new SimpleDateFormat("HH:mm:ss").format(new Date());

		JTextArea area = new JTextArea(text);
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		area.setEditable(false);
		area.setOpaque(false);
		area.setFont(new Font("SansSerif", Font.PLAIN, 11));

		JLabel timeLabel = new JLabel(time);
		timeLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
		timeLabel.setForeground(Color.DARK_GRAY);

		add(timeLabel, BorderLayout.NORTH);
		add(area, BorderLayout.CENTER);
	}
}
