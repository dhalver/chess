package chess;

import java.util.Arrays;
import java.util.Objects;

/**
 * A chessboard that can hold and rearrange chess pieces.
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessBoard {
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ChessBoard that)) {
            return false;
        }
        return Objects.deepEquals(squares, that.squares);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(squares);
    }

    ChessPiece[][] squares = new ChessPiece[8][8];
    public ChessBoard() {
        
    }

    /**
     * Adds a chess piece to the chessboard
     *
     * @param position where to add the piece to
     * @param piece    the piece to add
     */
    public void addPiece(ChessPosition position, ChessPiece piece) {
        squares[position.getRow() - 1][position.getColumn() - 1] = piece;
    }

    /**
     * Gets a chess piece on the chessboard
     *
     * @param position The position to get the piece from
     * @return Either the piece at the position, or null if no piece is at that
     * position
     */
    public ChessPiece getPiece(ChessPosition position) {

        return squares[position.getRow() - 1][position.getColumn() - 1];
    }

    /**
     * Sets the board to the default starting board
     * (How the game of chess normally starts)
     */
    public void resetBoard() {
        // clear
        for (int r = 0; r < 8; r++) {
            Arrays.fill(squares[r], null);
        }

        // standard setup
        var W = ChessGame.TeamColor.WHITE;
        var B = ChessGame.TeamColor.BLACK;

        // pawns
        for (int c = 1; c <= 8; c++) {
            addPiece(new ChessPosition(2, c), new ChessPiece(W, ChessPiece.PieceType.PAWN));
            addPiece(new ChessPosition(7, c), new ChessPiece(B, ChessPiece.PieceType.PAWN));
        }

        // back ranks (left→right: R N B Q K B N R)
        ChessPiece.PieceType[] order = {
                ChessPiece.PieceType.ROOK,
                ChessPiece.PieceType.KNIGHT,
                ChessPiece.PieceType.BISHOP,
                ChessPiece.PieceType.QUEEN,
                ChessPiece.PieceType.KING,
                ChessPiece.PieceType.BISHOP,
                ChessPiece.PieceType.KNIGHT,
                ChessPiece.PieceType.ROOK
        };

        for (int c = 1; c <= 8; c++) {
            addPiece(new ChessPosition(1, c), new ChessPiece(W, order[c - 1]));
            addPiece(new ChessPosition(8, c), new ChessPiece(B, order[c - 1]));
        }
    }
}
