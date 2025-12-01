import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

/**
 * コマンドラインベースのオセロ対戦管理プログラム。
 */
public class OthelloJudgeConsole {
    private final GameEngine gameEngine = new GameEngine();
    private AIPlayerAdapter blackAI;
    private AIPlayerAdapter whiteAI;
    private Scanner scanner;

    public OthelloJudgeConsole() {
        this.scanner = new Scanner(System.in);
    }

    public static void main(String[] args) {
        System.out.println("--- OthelloJudgeConsole Start ---");
        OthelloJudgeConsole judge = new OthelloJudgeConsole();
        judge.setupAndRunGame();
    }
    
    /** ユーザーからAIのパスを入力させ、ゲームを開始する */
    private void setupAndRunGame() {
        // 1. AIパスの入力受付
        String blackPath = readPath("黒番(1) AIプログラムのファイルパスを入力してください (例: SampleAI.jar): ");
        String whitePath = readPath("白番(2) AIプログラムのファイルパスを入力してください (例: SampleAI.jar): ");
        
        System.out.println("\n--- 対戦設定 ---");
        System.out.println("黒番: " + blackPath);
        System.out.println("白番: " + whitePath);
        System.out.println("----------------\n");

        // 2. AIアダプターの初期化
        blackAI = new AIPlayerAdapter(blackPath, GameEngine.BLACK);
        whiteAI = new AIPlayerAdapter(whitePath, GameEngine.WHITE);

        // 3. ゲーム開始
        runGameLoop();
    }
    
    /** ファイルパスを読み込む */
    private String readPath(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    /** メインのゲームループ */
    private void runGameLoop() {
        gameEngine.resetBoard();
        
        // 盤面が埋まるか、連続パスが発生するまでループ
        while (true) {
            AIPlayerAdapter currentAI = (gameEngine.getCurrentPlayer() == GameEngine.BLACK) ? blackAI : whiteAI;
            int opponentColor = (gameEngine.getCurrentPlayer() == GameEngine.BLACK) ? GameEngine.WHITE : GameEngine.BLACK;
            
            System.out.println("------------------------------------");
            System.out.println("手番: " + currentAI.getPlayerName());
            System.out.println("------------------------------------");

            // 【手番AIへ要求を出す前に盤面を表示】
            System.out.println("現在の盤面:");
            System.out.println(gameEngine.displayBoard()); // GameEngineが持つ複数行表示メソッド
            
            // AIプログラムへ渡すプロトコル用の1行文字列を表示 (デバッグ用)
            System.out.println("--- AIへの入力 (プロトコル文字列) ---");
            String boardStringForAI = gameEngine.boardToString();
            System.out.println("COLOR " + currentAI.getPlayerColor());
            System.out.println("MOVE BOARD:" + boardStringForAI);
            System.out.println("------------------------------------");
            
            boolean hasLegalMove = gameEngine.hasLegalMove();
            String move = null;
            
            try {
                // 1. AIから着手を取得
                move = currentAI.getMove(gameEngine.boardToString());
                System.out.println(">>> " + currentAI.getPlayerName() + "が打った手: " + move);

            } catch (TimeoutException e) {
                // 2. タイムアウト判定
                System.err.println("[ERROR] タイムアウト発生: " + e.getMessage());
                endGame(opponentColor, currentAI.getPlayerName() + "が5秒以内に応答しなかったため、");
                return;
            } catch (IOException e) {
                // 3. 通信エラーなど
                System.err.println("[ERROR] AI実行エラー: " + e.getMessage());
                endGame(opponentColor, currentAI.getPlayerName() + "の実行中にエラーが発生したため、");
                return;
            }
            
            // 4. 着手処理
            if (move.equalsIgnoreCase("pass")) {
                if (handlePass(hasLegalMove, currentAI, opponentColor) == false) {
                    return;
                }
            } else {
                if (handleStonePlacement(move, hasLegalMove, currentAI, opponentColor) == false) {
                    return;
                }
            }

            // 5. 手番交代
            gameEngine.switchPlayer();
            
            // 6. 最終判定 (盤面が完全に埋まった場合など)
            if (gameEngine.countStones(GameEngine.EMPTY) == 0) {
                 endGame(0, "盤面が完全に埋まりました。"); // 0は引き分け判定用
                 return;
            }
        }
    }
    
    /** パス処理 */
    private boolean handlePass(boolean hasLegalMove, AIPlayerAdapter currentAI, int opponentColor) {
        if (hasLegalMove) {
            // 合法手があるのにパスは無効手
            System.err.println("[ERROR] 無効手: 合法手があるにも関わらずパスしました。");
            endGame(opponentColor, currentAI.getPlayerName() + "が無効手（不必要なパス）を打ったため、");
            return false;
        } else {
            System.out.println("（合法手がないためパスしました）");
            // パス後に相手にも合法手がないかチェック
            gameEngine.switchPlayer(); // 一時的に相手に手番を渡す
            if (!gameEngine.hasLegalMove()) {
                endGame(0, "両者とも打つ手がなくなり、ゲーム終了。");
                return false;
            }
            gameEngine.switchPlayer(); // 手番を元に戻す
            return true;
        }
    }
    
    /** 石の配置処理 */
    private boolean handleStonePlacement(String move, boolean hasLegalMove, AIPlayerAdapter currentAI, int opponentColor) {
        if (!gameEngine.applyMove(move)) {
            // 不正な座標、または合法手ではない
            System.err.println("[ERROR] 無効手: 座標 " + move + " は合法手ではありません。");
            endGame(opponentColor, currentAI.getPlayerName() + "が無効手（不正な位置への着手）を打ったため、");
            return false;
        } else if (!hasLegalMove) {
            // パスしなければならない局面で着手した場合も無効手
            System.err.println("[ERROR] 無効手: パスしなければならない局面で着手しました。");
            endGame(opponentColor, currentAI.getPlayerName() + "が無効手（本来パスすべき局面での着手）を打ったため、");
            return false;
        } else {
            // 合法な着手
            System.out.println("[SUCCESS] " + currentAI.getPlayerName() + "の着手 (" + move + ") を適用しました。");
            System.out.println(gameEngine.displayBoard()); // 更新後の盤面表示
            return true;
        }
    }

    /** ゲーム終了処理 */
    private void endGame(int winnerColor, String reason) {
        System.out.println("\n====================================");
        System.out.println("GAME OVER - " + reason);
        System.out.println(gameEngine.displayBoard());
        
        int blackCount = gameEngine.countStones(GameEngine.BLACK);
        int whiteCount = gameEngine.countStones(GameEngine.WHITE);
        
        System.out.println("最終結果: 黒(" + blackCount + ") vs 白(" + whiteCount + ")");
        
        String winner;
        if (winnerColor == GameEngine.BLACK) {
            winner = blackAI.getPlayerName();
        } else if (winnerColor == GameEngine.WHITE) {
            winner = whiteAI.getPlayerName();
        } else {
            // 0の場合、通常の石数判定か引き分け
            if (blackCount > whiteCount) {
                winner = blackAI.getPlayerName();
            } else if (whiteCount > blackCount) {
                winner = whiteAI.getPlayerName();
            } else {
                winner = "引き分け";
            }
        }
        
        System.out.println("勝者: " + winner);
        System.out.println("====================================\n");
    }
}