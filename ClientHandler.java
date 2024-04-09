import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ClientHandler implements Runnable {
    private static final Set<ClientHandler> handlers = Collections.synchronizedSet(new HashSet<>());
    private static int currentQuestionIndex = 1;
    private final Socket clientSocket;
    private final UDPThread udpThread;
    private PrintWriter out;
    private String receivedID;
    private DataOutputStream dos;
	private DataInputStream dis;
    private BufferedReader in;
    private int clientId;
    private String correctAnswer;
    private static Map<String, Integer> clientScores = new HashMap<>(); // Track client scores


    public ClientHandler(Socket socket, int clientId, UDPThread udpThread) {
        this.clientSocket = socket;
        this.udpThread = udpThread;
        this.clientId = clientId;
        try {
            dos = new DataOutputStream(clientSocket.getOutputStream());
			dis = new DataInputStream(socket.getInputStream());
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);
        } catch (IOException e) {
            System.err.println("Error setting up streams: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            dos.writeUTF("ClientID: " + clientId);
            dos.flush();
            synchronized (handlers) {
                if (!handlers.contains(this)) {
                    handlers.add(this);
                }
                System.out.println("Next");

                sendCurrentQuestion();
            }

            String feedback;
            while ((feedback = dis.readUTF()) != null) {
                System.out.println("Feedback from client " + clientId + ": " + feedback);
                synchronized (handlers) {
                    if ("buzz".equals(feedback.trim())) {
                        receivedID = dis.readUTF();
                        System.out.println(receivedID);
                        handleBuzz();
                    } else if ("Correct".equals(feedback.trim())) {
                        dos.writeUTF("Correct");
                        udpThread.removeClients();
                        System.out.println("sent the notification");
                        incrementScore(receivedID); // Increment score for correct answer
                        handleNext();
                    } else if ("Wrong".equals(feedback.trim())) {
                        dos.writeUTF("Wrong");
                        udpThread.removeClients();
                        decrementScore(receivedID); // Decrement score for correct answer
                        handleNext();
                    } else if ("Next".equals(feedback.trim())) { //for when timer runs out
                        udpThread.removeClients();
                        handleNext();
                    } else if ("Time's up".equals(feedback.trim())) {
                        dos.writeUTF("Time's up");
                    } 
                }
            }
        } catch (IOException e) {
            System.err.println("Error in communication with client " + clientId + ": " + e.getMessage());
        } finally {
            try {
                handlers.remove(this);
                if (clientSocket != null)
                    clientSocket.close();
                if (out != null)
                    out.close();
                if (dos != null)
                    dos.close();
                if (in != null)
                    in.close();
            } catch (IOException e) {
                System.err.println("Error closing resources for client " + clientId + ": " + e.getMessage());
            }
        }
    }

    private void handleNext() throws IOException {
        synchronized (ClientHandler.class) {
            currentQuestionIndex++;
            if (currentQuestionIndex > 20) {  // If all 20 questions have been asked
                for (ClientHandler handler : handlers) {
                    handler.dos.writeUTF("End of game");
                    handler.printScoresAndWinner();  // Notify clients and print scores
                    // Optionally, close client connections or reset the game state here
                }
                return;  // Exit the method to stop the game
            }
            findAnswer();  // Find the correct answer for the next question
            for (ClientHandler handler : handlers) {
                handler.sendCurrentQuestion();  // Send the next question to all clients
            }
        }
    }

    private void findAnswer() {
        String questionFilePath = "Question" + currentQuestionIndex + ".txt";
        try (BufferedReader reader = new BufferedReader(new FileReader(questionFilePath))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("Correct: ")) {
					correctAnswer = line.replace("Correct: ", "");
				}
			}
		} catch (IOException e) {
			System.err.println("Error reading the question file: " + e.getMessage());
		}
    }

    private void sendCurrentQuestion() throws IOException {
        String questionFilePath = "Question" + currentQuestionIndex + ".txt";
        byte[] fileContent = Files.readAllBytes(Paths.get(questionFilePath));
        System.out.println(fileContent.length);
        dos.writeUTF("Next Question");
        dos.writeInt(fileContent.length);
        dos.write(fileContent);
        dos.flush();
    }

    private void handleBuzz() throws IOException {
        String firstClientId = udpThread.firstInLine();
        if (receivedID.equals(firstClientId)) {
            System.out.println("sending ack");
            dos.writeUTF("ack");
            dos.flush();
        } else {
            dos.writeUTF("nack");
            dos.flush();
        }
    }

    private void incrementScore(String clientID) {
        clientScores.put(clientID, clientScores.getOrDefault(clientID, 0) + 10);
    }

    private void decrementScore(String clientID) {
        clientScores.put(clientID, clientScores.getOrDefault(clientID, 0) - 10);
    }

    // Print scores and announce winner
    private void printScoresAndWinner() {
        System.out.println("Scores:");
        for (Map.Entry<String, Integer> entry : clientScores.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
        // Find the winner
        String winner = Collections.max(clientScores.entrySet(), Map.Entry.comparingByValue()).getKey();
        System.out.println("Winner: " + winner);
    }
}