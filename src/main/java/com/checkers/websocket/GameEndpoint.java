package com.checkers.websocket;

import com.checkers.hibernate.util.ReaderDB;
import com.checkers.hibernate.util.SaverDB;
import com.checkers.model.game.Game;
import com.checkers.model.messages.ChatMessage;
import com.checkers.model.messages.Message;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

@ServerEndpoint(
        value = "/game/{userId}",
        decoders = UserMoveDecoder.class,
        encoders = BoardStateEncoder.class)
public class GameEndpoint {
    private ReaderDB readerDB = new ReaderDB();
    private SaverDB saverDB = new SaverDB();
    private Session session;
    private static HashMap<Integer, GameEndpoint> gameEndpoints = new HashMap<>();
    private Timer timer = new Timer();

    @OnOpen
    public synchronized void onOpen(Session session, @PathParam("userId") Integer userId) {
        this.session = session;
        gameEndpoints.put(userId, this);

    }

    @OnMessage
    public void onMessage(Session session, Message message, @PathParam("userId") Integer user_Id) {
        Game game = null;
        int gameId = message.getGameId();
        int userId = user_Id;

        if ((game = readerDB.load(gameId)) == null) {
            game = new Game(gameId);
        }
        if (message.getMyColor() != null) {
            System.out.println("***********************\n" + message.getUserId() + "   " + message.getMyColor());
            game.setPlayerRole(message.getUserId(), message.getMyColor());
            game.setTime(message.getTimeControl(), message.getTimeControlBonus());
        }
        saverDB.save(game);
        game.readBoardState();

        System.out.println("\n" + message.toString());

        if (message.getType() != null)
            switch (message.getType()) {
                case "message":
                    broadcastChat(game, message);
                    break;
                case "piece-click":
                    if (message.isStart()) {
                        if (userId == game.getCurrentPlayerId() && game.getBlackUser_id() != 0 && game.getWhiteUser_id() != 0) {
                            message.setBoard(game.executeClick(message.getMessage(), userId));
                            send(user_Id, message);
                        }
                    }
                    break;
                case "move":
                    if (message.isStart()) {
                        if (userId == game.getCurrentPlayerId() && game.getBlackUser_id() != 0 && game.getWhiteUser_id() != 0) {
                            message.setBoard(game.executeMove(message.getMessage(), userId));
                            message.setCurrentPlayer(game.getCurrentPlayerId() == game.getWhiteUser_id() ? 0 : 1);
                            broadcastMove(game, message);
                        }
                    }
                    break;
            }

        if (game.checkIfEnd())
            message.winner(game.winner());

        saverDB.save(game);
    }

    private void broadcastChat(Game game, Message message) {
        try {
            ChatMessage chatMessage = new ChatMessage(message);
            System.out.println(chatMessage.toString());
            gameEndpoints.get(game.getWhiteUser_id()).session.getBasicRemote().sendObject(chatMessage);
            gameEndpoints.get(game.getBlackUser_id()).session.getBasicRemote().sendObject(chatMessage);
        } catch (IOException | EncodeException e) {
            e.printStackTrace();
        }
    }

    @OnClose
    public void onClose(Session session, @PathParam("userId") Integer user_Id) {
        gameEndpoints.remove(user_Id);
        try {
            this.session.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void send(Integer user_id, Message message) {
        try {
            System.out.println(message.toString());
            gameEndpoints.get(user_id).session.getBasicRemote().sendObject(message);
        } catch (IOException | EncodeException e) {
            e.printStackTrace();
        }
    }

    private void broadcastMove(Game game, Message message) {
        try {
            timer.cancel();
            timer = new Timer();
            timer.schedule(new TimePassed(game), game.currentPlayerTimeLeft());
            message.setBoard(game.boardStateToSend(game.getBlackUser_id()));
            System.out.println(message.toString());
            gameEndpoints.get(game.getBlackUser_id()).session.getBasicRemote().sendObject(message);
            message.setBoard(game.boardStateToSend(game.getWhiteUser_id()));
            System.out.println(message.toString());
            gameEndpoints.get(game.getWhiteUser_id()).session.getBasicRemote().sendObject(message);
        } catch (IOException | EncodeException e) {
            e.printStackTrace();
        }
    }

    private void broadcastWinner(Game game) {
        Message message = new Message(game.winner());
        try {
            gameEndpoints.get(game.getBlackUser_id()).session.getBasicRemote().sendObject(message);
            gameEndpoints.get(game.getWhiteUser_id()).session.getBasicRemote().sendObject(message);
        } catch (IOException | EncodeException e) {
            e.printStackTrace();
        }

    }

    class TimePassed extends TimerTask {
        private Game game;

        TimePassed(Game game) {
            this.game = game;
        }

        @Override
        public void run() {
            game.timeIsUpForCurrentPlayer();
            broadcastWinner(game);
        }
    }

}
