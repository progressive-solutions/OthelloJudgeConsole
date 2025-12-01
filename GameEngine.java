import java.util.ArrayList;
import java.util.List;

/**
 * オセロのゲームロジックを管理するクラス。
 * 盤面サイズは8x8で固定。
 */
public class GameEngine {
    // 盤面の状態を表す定数
    public static final int EMPTY = 0;
    public static final int BLACK = 1; // 黒石 (先手)
    public static final int WHITE = 2; // 白石 (後手)
    public static final int SIZE = 8;
    
    private int[][] board;
    private int currentPlayer;
    
    public GameEngine() {
        this.board = new int[SIZE][SIZE];
        resetBoard();
    }
    
    public void resetBoard() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                board[i][j] = EMPTY;
            }
        }
        board[3][3] = WHITE;
        board[3][4] = BLACK;
        board[4][3] = BLACK;
        board[4][4] = WHITE;
        currentPlayer = BLACK;
    }
    
    public int getCurrentPlayer() {
        return currentPlayer;
    }
    
    public void switchPlayer() {
        currentPlayer = (currentPlayer == BLACK) ? WHITE : BLACK;
    }
    
    /**
     * AIからの着手文字列（例: "c5"）を座標に変換し、石を打つ。
     * @param moveStr 着手文字列 ("a1"～"h8"または"pass")
     * @return 成功した場合true、無効手の場合false
     */
    public boolean applyMove(String moveStr) {
        if (moveStr.equalsIgnoreCase("pass")) {
            return true; // パスは常に合法
        }
        
        try {
            int col = moveStr.toLowerCase().charAt(0) - 'a'; // 'a'～'h' -> 0～7
            int row = Character.getNumericValue(moveStr.charAt(1)) - 1; // '1'～'8' -> 0～7
            
            if (col < 0 || col >= SIZE || row < 0 || row >= SIZE) {
                return false; // 範囲外
            }

            return placeStone(row, col);
        } catch (Exception e) {
            return false; // 不正な文字列形式
        }
    }
    
    // 以下、placeStone, isLegalMove, hasLegalMove は前バージョンと同じロジックを使用
    // ... (前回のGameEngineのコードをコピーしてください) ...
    // --- placeStone, isLegalMove, hasLegalMove メソッドの内容 ---
    public boolean placeStone(int row, int col) {
        if (!isLegalMove(row, col)) {
            return false;
        }
        
        board[row][col] = currentPlayer;
        int opponent = (currentPlayer == BLACK) ? WHITE : BLACK;
        int[] dr = {-1, -1, -1, 0, 0, 1, 1, 1}; 
        int[] dc = {-1, 0, 1, -1, 1, -1, 0, 1};
        
        int flippedCount = 0;
        for (int i = 0; i < 8; i++) {
            int r = row + dr[i];
            int c = col + dc[i];
            List<int[]> lineToFlip = new ArrayList<>();
            
            while (r >= 0 && r < SIZE && c >= 0 && c < SIZE && board[r][c] == opponent) {
                lineToFlip.add(new int[]{r, c});
                r += dr[i];
                c += dc[i];
            }
            
            if (r >= 0 && r < SIZE && c >= 0 && c < SIZE && board[r][c] == currentPlayer) {
                for (int[] pos : lineToFlip) {
                    board[pos[0]][pos[1]] = currentPlayer;
                    flippedCount++;
                }
            }
        }
        return true;
    }

    private boolean isLegalMove(int row, int col) {
        if (row < 0 || row >= SIZE || col < 0 || col >= SIZE || board[row][col] != EMPTY) {
            return false;
        }

        int opponent = (currentPlayer == BLACK) ? WHITE : BLACK;
        int[] dr = {-1, -1, -1, 0, 0, 1, 1, 1};
        int[] dc = {-1, 0, 1, -1, 1, -1, 0, 1};

        for (int i = 0; i < 8; i++) {
            int r = row + dr[i];
            int c = col + dc[i];
            boolean foundOpponent = false;

            while (r >= 0 && r < SIZE && c >= 0 && c < SIZE && board[r][c] == opponent) {
                foundOpponent = true;
                r += dr[i];
                c += dc[i];
            }

            if (foundOpponent && r >= 0 && r < SIZE && c >= 0 && c < SIZE && board[r][c] == currentPlayer) {
                return true; 
            }
        }
        return false;
    }

    public boolean hasLegalMove() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (isLegalMove(i, j)) {
                    return true;
                }
            }
        }
        return false;
    }
    // --- GameEngineのコード終了 ---

    /** 盤面をプロトコル用の文字列形式に変換 */
    public String boardToString() {
        StringBuilder sb = new StringBuilder();
        // BOARD: のプレフィックスはJudge側で付与するため、ここでは石のデータのみ
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                sb.append(board[i][j]);
            }
        }
        return sb.toString();
    }

    /** コンソール表示用の盤面文字列を生成 */
    public String displayBoard() {
        StringBuilder sb = new StringBuilder();
        String NL = System.lineSeparator();
        sb.append("  a b c d e f g h").append(NL);
        for (int i = 0; i < SIZE; i++) {
            sb.append(i + 1).append(" ");
            for (int j = 0; j < SIZE; j++) {
                char stone;
                switch (board[i][j]) {
                    case BLACK:
                        stone = '●';
                        break;
                    case WHITE:
                        stone = '○';
                        break;
                    default:
                        stone = '-';
                        break;
                }
                sb.append(stone).append(" ");
            }
            sb.append(NL);
        }
        return sb.toString();
    }
    
    /** 最終的な石数をカウント */
    public int countStones(int color) {
        int count = 0;
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (board[i][j] == color) {
                    count++;
                }
            }
        }
        return count;
    }
}