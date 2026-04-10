package websocket;

import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;

import java.net.URI;

@ClientEndpoint
public class WebSocketCommunicator {

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
        System.out.println("Received: " + message);
    }

    public void send(String message) {
        try {
            session.getBasicRemote().sendText(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send WebSocket message", e);
        }
    }
}