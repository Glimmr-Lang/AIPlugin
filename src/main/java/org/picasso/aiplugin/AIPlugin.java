package org.picasso.aiplugin;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/**
 *
 * @author hexaredecimal
 */
import javax.swing.*;

public class AIPlugin extends JPanel {

	private ChatModel gemini;
	private final JPanel messagePanel;
	private int currentRow = 0;
	private JTextArea apiKeyArea;
	private JComboBox<String> providerBox;
	private String systemPrompt = "";

	public AIPlugin() {
		setLayout(new BorderLayout());

		// Top Panel with API key and provider
		JPanel apiPanel = new JPanel(new BorderLayout());
		apiKeyArea = new JTextArea(1, 25);
		apiKeyArea.setText(System.getenv("GEMINI_API_KEY"));
		apiKeyArea.setBorder(BorderFactory.createTitledBorder("API Key"));

		providerBox = new JComboBox<>();
		providerBox.addItem("OpenAI");
		providerBox.addItem("Gemini");
		providerBox.addItem("Anthropic");
		providerBox.setBorder(BorderFactory.createTitledBorder("Provider"));

		apiPanel.add(apiKeyArea, BorderLayout.CENTER);
		apiPanel.add(providerBox, BorderLayout.EAST);
		add(apiPanel, BorderLayout.NORTH);

		// Chat message area
		messagePanel = new JPanel(new GridBagLayout());
		messagePanel.setBackground(Color.WHITE);

		JScrollPane scrollPane = new JScrollPane(messagePanel);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		add(scrollPane, BorderLayout.CENTER);

		// Input panel
		JPanel inputPanel = new JPanel(new BorderLayout());
		JTextArea inputArea = new JTextArea(2, 30);
		inputArea.setLineWrap(true);
		inputArea.setWrapStyleWord(true);
		JScrollPane inputScroll = new JScrollPane(inputArea);
		JButton sendButton = new JButton("Send");

		sendButton.addActionListener(e -> {
			String text = inputArea.getText().trim();
			if (!text.isEmpty()) {
				addMessage(text, true);
				String currentApiKey = apiKeyArea.getText().trim();

				gemini = GoogleAiGeminiChatModel.builder()
								.apiKey(currentApiKey)
								.modelName("gemini-1.5-flash")
								.build();

				JLabel typingIndicator = addTypingIndicator();

				var thread = new Thread(() -> {
					try {
						var prompt = String.format("%s\n. Use the code above as an example of the language we are working with, called PiccodeScript\n%s", systemPrompt, text); 
						ChatResponse chatResponse = gemini.chat(ChatRequest.builder()
										.messages(UserMessage.from(prompt))
										.build());

						String response = chatResponse.aiMessage().text();
						SwingUtilities.invokeLater(() -> {
							messagePanel.remove(typingIndicator);
							addMessage(response, false);
							messagePanel.revalidate();
							messagePanel.repaint();
						});
					} catch (Exception ex) {
						SwingUtilities.invokeLater(() -> {
							messagePanel.remove(typingIndicator);
							addMessage("[Error: " + ex.getMessage() + "]", false);
							messagePanel.revalidate();
							messagePanel.repaint();
						});
					}
				});
				inputArea.setText("");
				thread.start();
			}
		});

		inputPanel.add(inputScroll, BorderLayout.CENTER);
		inputPanel.add(sendButton, BorderLayout.EAST);
		add(inputPanel, BorderLayout.SOUTH);
		
		loadSystemPrompt();
	}

	public void addMessage(String text, boolean fromCurrentUser) {
		int viewportWidth = ((JScrollPane) getComponent(1)).getViewport().getWidth();
		int bubbleWidth = (viewportWidth / 2) - 20;

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = currentRow++;
		gbc.gridwidth = 2;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(5, 10, 5, 10);

		JPanel rowPanel = new JPanel(new FlowLayout(fromCurrentUser ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0));
		rowPanel.setOpaque(false);

		MessageBubble bubble = new MessageBubble(text, fromCurrentUser);
		bubble.setPreferredSize(new Dimension(bubbleWidth, bubble.getPreferredSize().height));

		rowPanel.add(bubble);
		messagePanel.add(rowPanel, gbc);

		messagePanel.revalidate();
		messagePanel.repaint();

		SwingUtilities.invokeLater(() -> {
			JScrollBar vBar = ((JScrollPane) getComponent(1)).getVerticalScrollBar();
			vBar.setValue(vBar.getMaximum());
		});
	}

	public JLabel addTypingIndicator() {
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = currentRow++;
		gbc.gridwidth = 2;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(5, 10, 5, 10);
		gbc.anchor = GridBagConstraints.WEST;

		JLabel label = new JLabel("AI is typing...");
		label.setFont(new Font("SansSerif", Font.ITALIC, 12));
		messagePanel.add(label, gbc);
		messagePanel.revalidate();
		messagePanel.repaint();

		return label;
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Chat App");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setSize(600, 700);
			frame.setLocationRelativeTo(null);
			frame.setContentPane(new AIPlugin());
			frame.setVisible(true);
		});
	}

	@Override
	public void doLayout() {
		super.doLayout();
		for (Component c : messagePanel.getComponents()) {
			if (c instanceof JPanel rowPanel) {
				for (Component inner : rowPanel.getComponents()) {
					if (inner instanceof MessageBubble bubble) {
						int width = ((JScrollPane) getComponent(1)).getViewport().getWidth();
						bubble.setPreferredSize(new Dimension((width / 2) - 20, bubble.getPreferredSize().height));
					}
				}
			}
		}
	}

	private void loadSystemPrompt() {
		var examples = new File("./examples");
		if (examples.isFile()) {
			return;
		}

		StringBuilder sb = new StringBuilder();
		sb.append("<code-samples>\n");
		for (var file : examples.listFiles()) {
			try {
				var contents = Files.readString(file.toPath());
				contents = String.format("<sample file='%s'>%s</sample>", file.getName(), contents);
				sb.append(contents.indent(4));
			} catch (IOException ex) {
				continue;
			}
		}
		sb.append("</code-samples>");
		systemPrompt = sb.toString();
	}

}
