package chess;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Represents a single chess piece
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessPiece {
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ChessPiece that)) {
            return false;
        }
        return pieceColor == that.pieceColor && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pieceColor, type);
    }

    private final ChessGame.TeamColor pieceColor;
    private final PieceType type;

    public ChessPiece(ChessGame.TeamColor pieceColor, ChessPiece.PieceType type) {
        this.pieceColor = pieceColor;
        this.type = type;
    }

    /**
     * The various different chess piece options
     */
    public enum PieceType {
        KING,
        QUEEN,
        BISHOP,
        KNIGHT,
        ROOK,
        PAWN
    }

    /**
     * @return Which team this chess piece belongs to
     */
    public ChessGame.TeamColor getTeamColor() {

        return pieceColor;
    }

    /**
     * @return which type of chess piece this piece is
     */
    public PieceType getPieceType() {
        return type;
    }

    /**
     * Calculates all the positions a chess piece can move to
     * Does not take into account moves that are illegal due to leaving the king in
     * danger
     *
     * @return Collection of valid moves
     */
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {

        //This changed code makes it so the code stops when they hit another piece
        //Also makes it so the pieces cannot move onto any of their own pieces
        //Makes it so that I can capture enemy pieces when I land on them

        List<ChessMove> moves = new ArrayList<>();
        PieceType pt = getPieceType();

        switch (pt) {
            case ROOK -> {
                addRay(moves, board, myPosition, +1, 0);
                addRay(moves, board, myPosition, -1, 0);
                addRay(moves, board, myPosition, 0, +1);
                addRay(moves, board, myPosition, 0, -1);
            }
            case BISHOP -> {
                addRay(moves, board, myPosition, +1, +1);
                addRay(moves, board, myPosition, +1, -1);
                addRay(moves, board, myPosition, -1, +1);
                addRay(moves, board, myPosition, -1, -1);
            }
            case QUEEN -> {
                // rook rays
                addRay(moves, board, myPosition, +1, 0);
                addRay(moves, board, myPosition, -1, 0);
                addRay(moves, board, myPosition, 0, +1);
                addRay(moves, board, myPosition, 0, -1);
                // bishop rays
                addRay(moves, board, myPosition, +1, +1);
                addRay(moves, board, myPosition, +1, -1);
                addRay(moves, board, myPosition, -1, +1);
                addRay(moves, board, myPosition, -1, -1);
            }
            case KNIGHT -> {
                int[][] d = {{+2, +1}, {+2, -1}, {-2, +1}, {-2, -1}, {+1, +2}, {+1, -2}, {-1, +2}, {-1, -2}};
                for (int[] s : d) addStepIfAllowed(moves, board, myPosition, s[0], s[1]);
            }
            case KING -> {
                int[][] d = {{+1, 0}, {-1, 0}, {0, +1}, {0, -1}, {+1, +1}, {+1, -1}, {-1, +1}, {-1, -1}};
                for (int[] s : d) addStepIfAllowed(moves, board, myPosition, s[0], s[1]);
            }
            //This fixed code allows for the correct direction for pawns based on which color
            //White piece needs to be +1 black piece needs to be -1
            //Next part was to make sure that it only moved one space if empty
            //But also could move two spaces from start position if both places are empty
            //Pawns also need to be able to move diagonal one place but only if enemy piece is there
            //Last thing was to allow for pawns to be promoted if they make it to the other side of the board
            case PAWN -> addPawnMoves(moves, board, myPosition);
        }

        return moves;
    }


    private static boolean inBounds(int r, int c) {
        return r >= 1 && r <= 8 && c >= 1 && c <= 8;
    }

    private void addRay(List<ChessMove> moves, ChessBoard board, ChessPosition from, int dr, int dc) {
        int r = from.getRow() + dr, c = from.getColumn() + dc;
        while (inBounds(r, c)) {
            ChessPosition to = new ChessPosition(r, c);
            ChessPiece there = board.getPiece(to);
            if (there == null) {
                moves.add(new ChessMove(from, to, null));
            } else {
                if (there.getTeamColor() != this.pieceColor) {
                    moves.add(new ChessMove(from, to, null)); // capture
                }
                break; // stop either way when we hit something
            }
            r += dr;
            c += dc;
        }
    }

    private void addStepIfAllowed(List<ChessMove> moves, ChessBoard board, ChessPosition from, int dr, int dc) {
        int r = from.getRow() + dr, c = from.getColumn() + dc;
        if (!inBounds(r, c)) return;
        ChessPosition to = new ChessPosition(r, c);
        ChessPiece there = board.getPiece(to);
        if (there == null || there.getTeamColor() != this.pieceColor) {
            moves.add(new ChessMove(from, to, null));
        }
    }

    private void addPawnMoves(List<ChessMove> moves, ChessBoard board, ChessPosition from) {
        int dir = (this.pieceColor == ChessGame.TeamColor.WHITE) ? +1 : -1;
        int startRow = (this.pieceColor == ChessGame.TeamColor.WHITE) ? 2 : 7;
        int promoRow = (this.pieceColor == ChessGame.TeamColor.WHITE) ? 8 : 1;

        int r = from.getRow();
        int c = from.getColumn();

        // Allows for 1-step forward if empty
        int r1 = r + dir;
        if (inBounds(r1, c) && board.getPiece(new ChessPosition(r1, c)) == null) {
            addPawnAdvanceOrPromote(moves, from, r1, c, promoRow);
            // This part helps with 2-step forward from start if both empty
            int r2 = r + 2 * dir;
            if (r == startRow && inBounds(r2, c)
                    && board.getPiece(new ChessPosition(r2, c)) == null) {
                moves.add(new ChessMove(from, new ChessPosition(r2, c), null));
            }
        }

        // Allows for diagonal captures
        for (int dc : new int[]{-1, +1}) {
            int rc = r + dir, cc = c + dc;
            if (!inBounds(rc, cc)) continue;
            ChessPosition to = new ChessPosition(rc, cc);
            ChessPiece there = board.getPiece(to);
            if (there != null && there.getTeamColor() != this.pieceColor) {
                addPawnCaptureOrPromote(moves, from, rc, cc, promoRow);
            }
        }

        // (En passant not required for your passoff set.)
    }

    private void addPawnAdvanceOrPromote(List<ChessMove> moves, ChessPosition from, int r, int c, int promoRow) {
        ChessPosition to = new ChessPosition(r, c);
        if (r == promoRow) {
            // 4 promotion options
            moves.add(new ChessMove(from, to, PieceType.QUEEN));
            moves.add(new ChessMove(from, to, PieceType.ROOK));
            moves.add(new ChessMove(from, to, PieceType.BISHOP));
            moves.add(new ChessMove(from, to, PieceType.KNIGHT));
        } else {
            moves.add(new ChessMove(from, to, null));
        }
    }

    private void addPawnCaptureOrPromote(List<ChessMove> moves, ChessPosition from, int r, int c, int promoRow) {
        ChessPosition to = new ChessPosition(r, c);
        if (r == promoRow) {
            moves.add(new ChessMove(from, to, PieceType.QUEEN));
            moves.add(new ChessMove(from, to, PieceType.ROOK));
            moves.add(new ChessMove(from, to, PieceType.BISHOP));
            moves.add(new ChessMove(from, to, PieceType.KNIGHT));
        } else {
            moves.add(new ChessMove(from, to, null));
        }
    }
}
