package chess;

import java.util.Collection;
import java.util.Objects;

/**
 * For a class that can manage a chess game, making moves on a board
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessGame {
    //This is the board that holds the pieces
    private ChessBoard board;

    //This is for whose turn it is to move
    private TeamColor teamTurn;

    public ChessGame() {
        this.board = new ChessBoard();
        this.board.resetBoard(); //This is for the start position
        this.teamTurn = TeamColor.WHITE; //This is for white to move

    }
    /**
     * @return Which team's turn it is
     */
    public TeamColor getTeamTurn() {
        return teamTurn;
    }

    /**
     * Set's which teams turn it is
     *
     * @param team the team whose turn it is
     */
    public void setTeamTurn(TeamColor team) {
        this.teamTurn = team;
    }

    /**
     * Enum identifying the 2 possible teams in a chess game
     */
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


    /**
     * Gets a valid moves for a piece at the given location
     *
     * @param startPosition the piece to get valid moves for
     * @return Set of valid moves for requested piece, or null if no piece at
     * startPosition
     */
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
            TeamColor opp = (piece.getTeamColor()==TeamColor.WHITE)? TeamColor.BLACK : TeamColor.WHITE;
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
            // Promotion: replace the pawn with the promoted type on the end square.
            b.addPiece(e, new ChessPiece(moving.getTeamColor(), move.getPromotionPiece()));
            b.removePiece(s);
        } else {
            // Normal move or capture: overwrite destination, clear start.
            b.addPiece(e, moving);
            b.removePiece(s);
        }
    }

    private ChessPosition findKing(ChessBoard b, TeamColor side) {
        for (int r=1; r<=8; r++) for (int c=1; c<=8; c++) {
            var p = new ChessPosition(r,c);
            var pc = b.getPiece(p);
            if (pc!=null && pc.getPieceType()==ChessPiece.PieceType.KING && pc.getTeamColor()==side) {
                return p;
            }
        }
        return null;
    }

    private boolean isSquareAttacked(ChessBoard b, ChessPosition square, TeamColor byTeam) {
        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                ChessPosition from = new ChessPosition(r, c);
                ChessPiece pc = b.getPiece(from);
                if (pc == null || pc.getTeamColor() != byTeam) continue;
                if (attacks(b, from, square, pc)) return true;
            }
        }
        return false;
    }

    private boolean attacks(ChessBoard b, ChessPosition from, ChessPosition to, ChessPiece pc) {
        int dr = to.getRow() - from.getRow();
        int dc = to.getColumn() - from.getColumn();
        int adr = Math.abs(dr), adc = Math.abs(dc);

        switch (pc.getPieceType()) {
            case PAWN: {
                int dir = (pc.getTeamColor() == TeamColor.WHITE) ? 1 : -1;
                // pawns attack diagonally one step forward
                return dr == dir && adc == 1;
            }
            case KNIGHT:
                return (adr == 2 && adc == 1) || (adr == 1 && adc == 2);

            case KING:
                return Math.max(adr, adc) == 1;

            case BISHOP:
                return adr == adc && adr > 0 && clearRay(b, from, dr, dc);

            case ROOK:
                return ((adr == 0 && adc > 0) || (adc == 0 && adr > 0)) && clearRay(b, from, dr, dc);

            case QUEEN:
                return ((adr == adc) || adr == 0 || adc == 0) && clearRay(b, from, dr, dc);
        }
        return false;
    }

    /** True if every square strictly between from and to along the line of motion is empty. */
    private boolean clearRay(ChessBoard b, ChessPosition from, int dr, int dc) {
        int stepR = Integer.compare(dr, 0);
        int stepC = Integer.compare(dc, 0);
        int r = from.getRow() + stepR, c = from.getColumn() + stepC;
        int endR = from.getRow() + dr,   endC = from.getColumn() + dc;
        while (r != endR || c != endC) {
            if (b.getPiece(new ChessPosition(r,c)) != null) return false;
            r += stepR; c += stepC;
        }
        return true;
    }

    private boolean hasAnyLegalMove(TeamColor team) {
        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                ChessPosition p = new ChessPosition(r, c);
                ChessPiece pc = board.getPiece(p);
                if (pc == null || pc.getTeamColor() != team) continue;
                var vm = validMoves(p);
                if (vm != null && !vm.isEmpty()) return true;
            }
        }
        return false;
    }
    

    /**
     * Makes a move in a chess game
     *
     * @param move chess move to perform
     * @throws InvalidMoveException if move is invalid
     */
    public void makeMove(ChessMove move) throws InvalidMoveException {
        ChessPosition start = move.getStartPosition();
        ChessPiece piece = board.getPiece(start);
        if (piece == null) throw new InvalidMoveException("No piece at start square.");
        if (piece.getTeamColor() != teamTurn) throw new InvalidMoveException("Wrong side to move.");

        var legal = validMoves(start);
        if (legal == null || !legal.contains(move)) throw new InvalidMoveException("Illegal move.");

        // Apply on the real board (captures/promotion handled inside)
        applyMove(board, move);

        // Switch the turn on success
        teamTurn = (teamTurn == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;
    }

    /**
     * Determines if the given team is in check
     *
     * @param teamColor which team to check for check
     * @return True if the specified team is in check
     */
    public boolean isInCheck(TeamColor teamColor) {
        ChessPosition king = findKing(board, teamColor);
        if (king == null) return false; // defensive
        TeamColor enemy = (teamColor == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;
        return isSquareAttacked(board, king, enemy);
    }

    /**
     * Determines if the given team is in checkmate
     *
     * @param teamColor which team to check for checkmate
     * @return True if the specified team is in checkmate
     */
    public boolean isInCheckmate(TeamColor teamColor) {
        return isInCheck(teamColor) && !hasAnyLegalMove(teamColor);
    }

    /**
     * Determines if the given team is in stalemate, which here is defined as having
     * no valid moves while not in check.
     *
     * @param teamColor which team to check for stalemate
     * @return True if the specified team is in stalemate, otherwise false
     */
    public boolean isInStalemate(TeamColor teamColor) {
        return !isInCheck(teamColor) && !hasAnyLegalMove(teamColor);
    }

    /**
     * Sets this game's chessboard with a given board
     *
     * @param board the new board to use
     */
    public void setBoard(ChessBoard board) {
        this.board = board;
    }

    /**
     * Gets the current chessboard
     *
     * @return the chessboard
     */
    public ChessBoard getBoard() {
        return board;
    }
}
