package chess;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

/**
 * Represents a single chess piece
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessPiece {

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
        ChessPiece piece = board.getPiece(myPosition);
        //Since imported only need to use List and ArrayList do not need to type java.util
        List<ChessMove> movements = new ArrayList<>();
        if (piece.getPieceType() == PieceType.BISHOP) {
            int[][] directions = {{-1,-1}, {-1,1}, {1,-1}, {1,1}};
            for (int[] d : directions) {
                int row = myPosition.getRow() + d[0];
                int col = myPosition.getColumn() + d[1];

                while (row >= 1 && row <= 8 && col >= 1 && col <= 8){
                    movements.add(new ChessMove(myPosition, new ChessPosition(row, col), null));
                    row += d[0];
                    col += d[1];
                }
            }
            //return List.of(new ChessMove(new ChessPosition(5,4), new ChessPosition(1,8), null));
        }
        if (piece.getPieceType() == PieceType.KNIGHT) {
            int[][] directions = {{2,-1}, {2,1}, {-2,-1}, {-2,1}, {1,-2}, {1,2}, {-1,-2}, {-1,2}};
            int row0 = myPosition.getRow();
            int col0 = myPosition.getColumn();
            for (int[] d : directions) {
                int row = row0 + d[0];
                int col = col0 + d[1];

                if (row >= 1 && row <= 8 && col >= 1 && col <= 8) {
                    movements.add(new ChessMove(myPosition, new ChessPosition(row, col), null));
                }
            }
        }
        if (piece.getPieceType() == PieceType.ROOK) {
            int[][] directions = {{1,0}, {-1,0}, {0,-1},{0,1}};
            for (int[] d : directions) {
                //have to add + d[0] and d[1] to not add the start square as a move in any direction
                int row = myPosition.getRow() + d[0];
                int col = myPosition.getColumn() + d[1];

                while (row >= 1 && row <= 8 && col >= 1 && col <= 8){
                    movements.add(new ChessMove(myPosition, new ChessPosition(row, col), null));
                    row += d[0];
                    col += d[1];
                }
            }
        }
        if (piece.getPieceType() == PieceType.PAWN) {
            int[][] directions = {{2,0}, {1,0}, {1,-1},{1,1}};
            int row0 = myPosition.getRow();
            int col0 = myPosition.getColumn();
            for (int[] d : directions) {
                int row = row0 + d[0];
                int col = col0 + d[1];

                if (row >= 1 && row <= 8 && col >= 1 && col <= 8) {
                    movements.add(new ChessMove(myPosition, new ChessPosition(row, col), null));
                }
            }
        }

        if (piece.getPieceType() == PieceType.QUEEN) {
            int[][] directions = {{1,0}, {0,1}, {-1,0}, {0,-1}, {1,1}, {1,-1}, {-1,-1}, {-1,1}};
            for (int[] d : directions) {
                //have to add + d[0] and d[1] to not add the start square as a move in any direction
                int row = myPosition.getRow() + d[0];
                int col = myPosition.getColumn() + d[1];

                while (row >= 1 && row <= 8 && col >= 1 && col <= 8){
                    movements.add(new ChessMove(myPosition, new ChessPosition(row, col), null));
                    row += d[0];
                    col += d[1];
                }
            }
        }

        return movements;
    }
}
