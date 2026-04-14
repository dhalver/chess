package chess;

import java.util.Collection;
import java.util.Objects;

public class ChessGame {

    private ChessBoard board;
    private TeamColor teamTurn;


    private boolean gameOver = false;

    public ChessGame() {
        this.board = new ChessBoard();
        this.board.resetBoard();
        this.teamTurn = TeamColor.WHITE;
    }

    public TeamColor getTeamTurn() {
        return teamTurn;
    }

    public void setTeamTurn(TeamColor team) {
        this.teamTurn = team;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    public enum TeamColor {
        WHITE,
        BLACK
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ChessGame chessGame)) {
            return false;
        }
        return Objects.equals(board, chessGame.board) && teamTurn == chessGame.teamTurn;
    }

    @Override
    public int hashCode() {
        return Objects.hash(board, teamTurn);
    }

    public Collection<ChessMove> validMoves(ChessPosition startPosition) {
        ChessPiece piece = board.getPiece(startPosition);
        if (piece == null) {
            return null;
        }

        var pseudo = piece.pieceMoves(board, startPosition);
        var legal = new java.util.ArrayList<ChessMove>();

        for (ChessMove mv : pseudo) {
            ChessBoard b2 = copyBoard(board);
            applyMove(b2, mv);
            ChessPosition myKing = findKing(b2, piece.getTeamColor());
            TeamColor opp = (piece.getTeamColor()==TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;

            if (!isSquareAttacked(b2, myKing, opp)) {
                legal.add(mv);
            }
        }

        return legal;
    }

    private ChessBoard copyBoard(ChessBoard original) {
        ChessBoard c = new ChessBoard();
        c.copyFrom(original);
        return c;
    }

    private void applyMove(ChessBoard b, ChessMove move) {
        ChessPosition s = move.getStartPosition();
        ChessPosition e = move.getEndPosition();
        ChessPiece moving = b.getPiece(s);

        if (move.getPromotionPiece() != null) {
            b.removePiece(s);
            b.addPiece(e, new ChessPiece(moving.getTeamColor(), move.getPromotionPiece()));
        } else {
            b.addPiece(e, moving);
            b.removePiece(s);
        }
    }

    private ChessPosition findKing(ChessBoard b, TeamColor side) {
        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                var p = new ChessPosition(r, c);
                var pc = b.getPiece(p);

                if (pc != null && pc.getPieceType() == ChessPiece.PieceType.KING && pc.getTeamColor() == side) {
                    return p;
                }
            }
        }
        return null;
    }

    private boolean isSquareAttacked(ChessBoard b, ChessPosition square, TeamColor byTeam) {
        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                ChessPosition from = new ChessPosition(r, c);
                ChessPiece pc = b.getPiece(from);

                if (pc == null || pc.getTeamColor() != byTeam) {
                    continue;
                }

                if (attacks(b, from, square, pc)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean attacks(ChessBoard b, ChessPosition from, ChessPosition to, ChessPiece pc) {
        int dr = to.getRow() - from.getRow();
        int dc = to.getColumn() - from.getColumn();
        int adr = Math.abs(dr), adc = Math.abs(dc);

        switch (pc.getPieceType()) {
            case PAWN -> {
                int dir = (pc.getTeamColor() == TeamColor.WHITE) ? 1 : -1;
                return dr == dir && adc == 1;
            }
            case KNIGHT -> {
                return (adr == 2 && adc == 1) || (adr == 1 && adc == 2);
            }
            case KING -> {
                return Math.max(adr, adc) == 1;
            }
            case BISHOP -> {
                return adr == adc && adr > 0 && clearRay(b, from, dr, dc);
            }
            case ROOK -> {
                return ((adr == 0 && adc > 0) || (adc == 0 && adr > 0)) && clearRay(b, from, dr, dc);
            }
            case QUEEN -> {
                return ((adr == adc) || adr == 0 || adc == 0) && clearRay(b, from, dr, dc);
            }
        }
        return false;
    }

    private boolean clearRay(ChessBoard b, ChessPosition from, int dr, int dc) {
        int stepR = Integer.compare(dr, 0);
        int stepC = Integer.compare(dc, 0);

        int steps = Math.max(Math.abs(dr), Math.abs(dc)) - 1;
        int r = from.getRow();
        int c = from.getColumn();

        for (int i = 0; i < steps; i++) {
            r += stepR;
            c += stepC;

            if (b.getPiece(new ChessPosition(r, c)) != null) {
                return false;
            }
        }

        return true;
    }

    private boolean hasAnyLegalMove(TeamColor team) {
        for (int idx = 0; idx < 64; idx++) {
            int r = (idx / 8) + 1;
            int c = (idx % 8) + 1;

            ChessPosition p = new ChessPosition(r, c);
            ChessPiece pc = board.getPiece(p);

            if (pc == null || pc.getTeamColor() != team) {
                continue;
            }

            var vm = validMoves(p);
            if (vm != null && !vm.isEmpty()) {
                return true;
            }
        }

        return false;
    }

    public void makeMove(ChessMove move) throws InvalidMoveException {
        ChessPosition start = move.getStartPosition();
        ChessPiece piece = board.getPiece(start);

        if (piece == null) {
            throw new InvalidMoveException("No piece at start square.");
        }

        if (piece.getTeamColor() != teamTurn) {
            throw new InvalidMoveException("Wrong side to move.");
        }

        var legal = validMoves(start);

        if (legal == null || !legal.contains(move)) {
            throw new InvalidMoveException("Illegal move.");
        }

        applyMove(board, move);

        teamTurn = (teamTurn == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;
    }

    public boolean isInCheck(TeamColor teamColor) {
        ChessPosition king = findKing(board, teamColor);

        if (king == null) {
            return false;
        }

        TeamColor enemy = (teamColor == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;
        return isSquareAttacked(board, king, enemy);
    }

    public boolean isInCheckmate(TeamColor teamColor) {
        return isInCheck(teamColor) && !hasAnyLegalMove(teamColor);
    }

    public boolean isInStalemate(TeamColor teamColor) {
        return !isInCheck(teamColor) && !hasAnyLegalMove(teamColor);
    }

    public void setBoard(ChessBoard board) {
        this.board = board;
    }

    public ChessBoard getBoard() {
        return board;
    }
}
