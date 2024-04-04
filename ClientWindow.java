import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.security.SecureRandom;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;

public class ClientWindow implements ActionListener {
	private JButton poll;
	private JLabel clientID;
	public JButton submit;
	private JRadioButton options[];
	private ButtonGroup optionGroup;
	private JLabel question;
	private JLabel timer;
	private JLabel score;
	private JLabel currScore;
	public int currentScore = 0;
	public TimerTask clock;
	private String answer;
	private Client client;
	Timer t = new Timer();
	public Boolean polled = false;

	private JFrame window;

	private static SecureRandom random = new SecureRandom();

	// write setters and getters as you need

	public ClientWindow(Client client) {
		JOptionPane.showMessageDialog(window, "Film & Entertainment Trivia");
		this.client = client;
		window = new JFrame("Trivia");
		question = new JLabel("Q1. This is a sample question"); // represents the question
		window.add(question);
		question.setBounds(10, 5, 750, 100);
		

		options = new JRadioButton[4];
		optionGroup = new ButtonGroup();
		for (int index = 0; index < options.length; index++) {
			options[index] = new JRadioButton("Option " + (index + 1)); // represents an option
			// if a radio button is clicked, the event would be thrown to this class to
			// handle
			options[index].addActionListener(this);
			options[index].setBounds(10, 110 + (index * 20), 350, 20);
			window.add(options[index]);
			optionGroup.add(options[index]);
		}

		timer = new JLabel("TIMER"); // represents the countdown shown on the window
		timer.setBounds(250, 250, 100, 20);
		clock = new TimerCode(15); // represents clocked task that should run after X seconds
		// Timer t = new Timer(); // event generator
		t.schedule(clock, 0, 1000); // clock is called every second
		window.add(timer);

		clientID = new JLabel("Client ID:"); // represents the score
		clientID.setBounds(280, 20, 100, 20);
		window.add(clientID);

		score = new JLabel("SCORE:"); // represents the score
		score.setBounds(50, 250, 50, 20);
		window.add(score);

		currScore = new JLabel(""); 
        currScore.setBounds(100, 250, 25, 20); 
        window.add(currScore);

		poll = new JButton("Poll"); // button that use clicks/ like a buzzer
		poll.setBounds(10, 300, 100, 20);
		poll.addActionListener(this); // calls actionPerformed of this class
		window.add(poll);

		submit = new JButton("Submit"); // button to submit their answer
		submit.setBounds(200, 300, 100, 20);
		submit.addActionListener(this); // calls actionPerformed of this class
		window.add(submit);

		window.setSize(400, 400);
		window.setBounds(50, 50, 800, 400);
		window.setLayout(null);
		window.setVisible(true);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setResizable(false);
	}

	public void updateTimer(int remainingTime) {
		timer.setText("TIMER: " + remainingTime + " seconds");
		if (remainingTime < 6)
			timer.setForeground(Color.RED);
		else
			timer.setForeground(Color.BLACK);
	}

	public void resetTimer(int duration) {
        clock.cancel(); // Cancel the previous timer task
        timer.setText("TIMER");
        clock = new TimerCode(duration); // Create a new TimerTask with the new duration
        t.schedule(clock, 0, 1000); // Schedule the new task
		// polled = true;
    }

	public void setTimer(int duration) {
        clock.cancel(); // Cancel the previous timer task
        timer.setText("TIMER");
        clock = new TimerCode(duration); // Create a new TimerTask with the new duration
        t.schedule(clock, 0, 1000); // Schedule the new task
    }

	// this method is called when you check/uncheck any radio button
	// this method is called when you press either of the buttons- submit/poll
	@Override
	public void actionPerformed(ActionEvent e) {
		if ("Poll".equals(e.getActionCommand())) {
			client.sendBuzz(); // Call the method when Poll button is clicked
			// resetTimer(10);
		} else if ("Submit".equals(e.getActionCommand())) {
			// Existing submit logic
			String selectedOption = getSelectedOptionIndex();
			submit.setEnabled(false);
			if (selectedOption != null && selectedOption.equals(answer)) {
				// updateScore(true);
				client.sendAnswerFeedback("Correct");
				JOptionPane.showMessageDialog(window, "Correct Answer!");
			} else {
				// updateScore(false);
				client.sendAnswerFeedback("Wrong");
				JOptionPane.showMessageDialog(window, "Wrong Answer!");
				clock.cancel();
			}
		}
		

	}

	public void updateClientID(String id) {
		clientID.setText(id);
    }

	public void updateScore(boolean correct) {
		if (correct) {
			currentScore += 10; 
		} else {
			currentScore -= 10; 
		}
		currScore.setText(String.valueOf(currentScore));
    }

	private String getSelectedOptionIndex() {
        for (int i = 0; i < options.length; i++) {
            if (options[i].isSelected()) {
                return String.valueOf(i + 1); 
            }
        }
        return null; 
    }

	// this class is responsible for running the timer on the window
	public class TimerCode extends TimerTask {
		private int duration; // write setters and getters as you need

		public TimerCode(int duration) {
			this.duration = duration;
		}

		@Override
		public void run() {
			if (duration <= 0) {
				if (polled)
				{
					// currentScore -= 20;
					client.sendAnswerFeedback("Time's up");
				}
				poll.setEnabled(false);
				timer.setText("Timer expired");
				currScore.setText(String.valueOf(currentScore));
				client.sendAnswerFeedback("Next");
				window.repaint();
				this.cancel(); // cancel the timed task
				return;
				// you can enable/disable your buttons for poll/submit here as needed
			}

			if (duration < 6)
				timer.setForeground(Color.red);
			else
				timer.setForeground(Color.black);

			timer.setText(duration + "");
			duration--;
			window.repaint();
		}
	}

	public void updateQuestion(String text) {
		question.setText(text);
		poll.setEnabled(true);
		submit.setEnabled(false);
		setTimer(15);
	}

	public void setOptions(String[] optionsText, String correctAnswer) {
		answer = correctAnswer;
		for (int i = 0; i < options.length && i < optionsText.length; i++) {
			options[i].setText(optionsText[i]);
			options[i].setVisible(true);
		}
	}

	public void disableOptions() {
		for (int i = 0; i < options.length; i++) {
			options[i].setEnabled(false);
		}
	}

	public void enableOptions() {
		for (int i = 0; i < options.length; i++) {
			options[i].setEnabled(true);
		}
	}
	
}