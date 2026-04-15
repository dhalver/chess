package websocket;

import com.google.gson.Gson;
import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.CloseReason;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;
import ui.Main;
import websocket.commands.UserGameCommand;
import websocket.messages.ServerMessage;

import java.net.URI;

@ClientEndpoint
public class WebSocketCommunicator {

    private final Gson gson = new Gson();
    private Session session;

    public WebSocketCommunicator(String url) {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, new URI(url));
        } catch (Exception e) {
            throw new RuntimeException("Failed to connect to WebSocket server", e);
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        this.session.setMaxIdleTimeout(300000);
        System.out.println("WebSocket connected.");
    }

    @OnMessage
    public void onMessage(String message) {
        ServerMessage serverMessage = gson.fromJson(message, ServerMessage.class);

        switch (serverMessage.getServerMessageType()) {
            case LOAD_GAME -> Main.handleLoadGame(message);
            case NOTIFICATION -> Main.handleNotification(message);
            case ERROR -> Main.handleError(message);
        }
    }

    public void sendCommand(UserGameCommand command) {
        try {
            if (session == null) {
                throw new RuntimeException("WebSocket session is null");
            }

            if (!session.isOpen()) {
                throw new RuntimeException("WebSocket session is closed");
            }

            String json = gson.toJson(command);
            System.out.println("Sending: " + json);
            session.getBasicRemote().sendText(json);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to send WebSocket command: " + e.getMessage(), e);
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        System.out.println("WebSocket closed: " + reason);
        Main.handleSocketClosed();
    }

    @OnError
    public void onError(Session session, Throwable error) {
        System.out.println("WebSocket error: " + error.getMessage());
        error.printStackTrace();
    }
}