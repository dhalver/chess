package websocket;

import com.google.gson.Gson;
import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;

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
        System.out.println("WebSocket connected.");
    }

    @OnMessage
    public void onMessage(String message) {
        ServerMessage serverMessage = gson.fromJson(message, ServerMessage.class);

        switch (serverMessage.getServerMessageType()) {
            case LOAD_GAME -> System.out.println("Game loaded!");
            case NOTIFICATION -> System.out.println("Notification received");
            case ERROR -> System.out.println("Error received");
        }
    }

    public void sendCommand(UserGameCommand command) {
        try {
            String json = gson.toJson(command);
            session.getBasicRemote().sendText(json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send WebSocket command", e);
        }
    }

}