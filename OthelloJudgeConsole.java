import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

/**
 * コマンドラインベースのオセロ対戦管理プログラム。
 */
public class OthelloJudgeConsole {
    private final GameEngine gameEngine = new GameEngine();
    private AIPlayerAdapter blackAI;
    private AIPlayerAdapter whiteAI;

    /** 全てのゲームログ出力に使うストリーム。初期値はlogだが、すぐにファイルにリダイレクトされる。*/ 
    private static PrintStream log = System.out;

    /**
     * ログ出力とコンソール出力のどちらも行う
     * @param text ログ内容
     */
    private static void logAndConsole(String text) {
        log.println(text);
        System.out.println(text);
    }

    /**
     * ログファイルを設定し、PrintStreamをセットアップする。
     * @param blackAIPath 黒番AIのパス
     * @param whiteAIPath 白番AIのパス
     * @return 設定が成功したかどうか
     */
    private static boolean setupLogFile(String blackAIPath, String whiteAIPath) {
        try {
            // ファイル名から拡張子とパスを削除し、ファイル名部分だけを取得
            // 例: /path/to/MyAi.jar -> MyAi
            String blackName = new File(blackAIPath).getName().replace(".jar", "");
            String whiteName = new File(whiteAIPath).getName().replace(".jar", "");

            // 日付フォーマット (例: 20251202_093000)
            String dateString = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

            // 最終ファイル名: 日付_黒番ファイル名_白番ファイル名.txt
            String logFileName = dateString + "_" + blackName + "_" + whiteName + ".txt";

            // PrintStreamを設定 (UTF-8エンコーディング指定)
            // これ以降、log.println()でファイルに書き込まれる
            log = new PrintStream(new File(logFileName), "UTF-8");
            
            // コンソールにはログファイル作成成功のメッセージのみを出力
            log.println("✅ ゲームログをファイルに出力します: " + logFileName);
            
            return true;
        } catch (FileNotFoundException e) {
            log.println("❌ ログファイルの作成に失敗しました: " + e.getMessage());
            return false;
        } catch (Exception e) {
            log.println("❌ ファイル名解析中に予期せぬエラーが発生しました: " + e.getMessage());
            return false;
        }
    }

    public static void main(String[] args) {
        String blackAIPath = null;
        String whiteAIPath = null;

        // 1. 引数の処理
        if (args.length == 2) {
            // 使用法1: java ... OthelloJudgeConsole [黒AIパス] [白AIパス]
            blackAIPath = args[0];
            whiteAIPath = args[1];
        } else if (args.length == 0) {
            // 使用法2: java ... OthelloJudgeConsole (標準入力でパス入力)
            log.println("🤖 オセロAIジャッジを開始します。");
            Scanner scanner = new Scanner(System.in);
            log.print("黒番AIプログラムのファイルパス (.jar) を入力してください: ");
            blackAIPath = scanner.nextLine().trim();
            log.print("白番AIプログラムのファイルパス (.jar) を入力してください: ");
            whiteAIPath = scanner.nextLine().trim();
            // Scannerはmainメソッド終了時に自動で閉じられることが期待されるが、明示的に閉じる
            // ただし、System.inを閉じると他の標準入力に影響が出るため、ここでは閉じない方が安全な場合もある。
        } else {
            // 引数の数が不正
            log.println("❌ 起動エラー: 引数の数が不正です。");
            log.println("使用法1: java -cp classes OthelloJudgeConsole [黒AIパス] [白AIパス]");
            log.println("使用法2: java -cp classes OthelloJudgeConsole (引数なし)");
            return;
        }

        // 2. ログファイルの設定とリダイレクト
        if (!setupLogFile(blackAIPath, whiteAIPath)) {
            return;
        }

        // ログストリームにヘッダーを出力 (ファイルへの書き込み開始)
        logAndConsole("==================================================");
        logAndConsole("========== Othello AI Judge Console ==============");
        logAndConsole("==================================================");
        logAndConsole("黒番 AI: " + blackAIPath);
        logAndConsole("白番 AI: " + whiteAIPath);
        logAndConsole("開始日時: " + new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()));
        logAndConsole("--------------------------------------------------");
        System.out.println("対戦中・・・");
        

        // 3. ゲーム開始
        try {
            OthelloJudgeConsole judge = new OthelloJudgeConsole(blackAIPath, whiteAIPath);
            judge.runGameLoop();
        } catch (Exception e) {
            log.println("致命的なエラーが発生しました: " + e.getMessage());
            e.printStackTrace(log); // スタックトレースをファイルに出力
        } finally {
            log.close(); // ログファイルを閉じる
        }
    }
    
    public OthelloJudgeConsole(String blackPath, String whitePath) {
        // AIアダプターの初期化
        blackAI = new AIPlayerAdapter(blackPath, GameEngine.BLACK);
        whiteAI = new AIPlayerAdapter(whitePath, GameEngine.WHITE);
    }

    /** メインのゲームループ */
    private void runGameLoop() {
        gameEngine.resetBoard();
        
        // 盤面が埋まるか、連続パスが発生するまでループ
        while (true) {
            AIPlayerAdapter currentAI = (gameEngine.getCurrentPlayer() == GameEngine.BLACK) ? blackAI : whiteAI;
            int opponentColor = (gameEngine.getCurrentPlayer() == GameEngine.BLACK) ? GameEngine.WHITE : GameEngine.BLACK;
            
            log.println("------------------------------------");
            log.println("手番: " + currentAI.getPlayerName());
            log.println("------------------------------------");

            // 【手番AIへ要求を出す前に盤面を表示】
            log.println("現在の盤面:");
            log.println(gameEngine.displayBoard()); // GameEngineが持つ複数行表示メソッド
            
            // AIプログラムへ渡すプロトコル用の1行文字列を表示 (デバッグ用)
            log.println("--- AIへの入力 (プロトコル文字列) ---");
            String boardStringForAI = gameEngine.boardToString();
            log.println("COLOR " + currentAI.getPlayerColor());
            log.println("MOVE BOARD:" + boardStringForAI);
            log.println("------------------------------------");
            
            boolean hasLegalMove = gameEngine.hasLegalMove();
            String move = null;
            
            try {
                // 1. AIから着手を取得
                move = currentAI.getMove(gameEngine.boardToString());
                log.println(">>> " + currentAI.getPlayerName() + "が打った手: " + move);

            } catch (TimeoutException e) {
                // 2. タイムアウト判定
                log.println("[ERROR] タイムアウト発生: " + e.getMessage());
                endGame(opponentColor, currentAI.getPlayerName() + "が5秒以内に応答しなかったため、");
                return;
            } catch (IOException e) {
                // 3. 通信エラーなど
                log.println("[ERROR] AI実行エラー: " + e.getMessage());
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
            log.println("[ERROR] 無効手: 合法手があるにも関わらずパスしました。");
            endGame(opponentColor, currentAI.getPlayerName() + "が無効手（不必要なパス）を打ったため、");
            return false;
        } else {
            log.println("（合法手がないためパスしました）");
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
            log.println("[ERROR] 無効手: 座標 " + move + " は合法手ではありません。");
            endGame(opponentColor, currentAI.getPlayerName() + "が無効手（不正な位置への着手）を打ったため、");
            return false;
        } else if (!hasLegalMove) {
            // パスしなければならない局面で着手した場合も無効手
            log.println("[ERROR] 無効手: パスしなければならない局面で着手しました。");
            endGame(opponentColor, currentAI.getPlayerName() + "が無効手（本来パスすべき局面での着手）を打ったため、");
            return false;
        } else {
            // 合法な着手
            log.println("[SUCCESS] " + currentAI.getPlayerName() + "の着手 (" + move + ") を適用しました。");
            log.println(gameEngine.displayBoard()); // 更新後の盤面表示
            return true;
        }
    }

    /** ゲーム終了処理 */
    private void endGame(int winnerColor, String reason) {
        logAndConsole("\n====================================");
        logAndConsole("GAME OVER - " + reason);
        logAndConsole(gameEngine.displayBoard());
        
        int blackCount = gameEngine.countStones(GameEngine.BLACK);
        int whiteCount = gameEngine.countStones(GameEngine.WHITE);
        
        logAndConsole("最終結果: 黒(" + blackCount + ") vs 白(" + whiteCount + ")");
        
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
        
        logAndConsole("勝者: " + winner);
        logAndConsole("====================================\n");
    }
}