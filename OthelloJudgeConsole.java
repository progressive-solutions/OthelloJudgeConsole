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
    private PrintStream log = System.out;

    /**
     * ログ出力とコンソール出力のどちらも行う
     * @param text ログ内容
     */
    private void logAndConsole(String text) {
        log.println(text);
        System.out.println(text);
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
            System.out.println("🤖 オセロAIジャッジを開始します。");
            Scanner scanner = new Scanner(System.in);
            System.out.print("黒番AIプログラムのファイルパス (.jar) を入力してください: ");
            blackAIPath = scanner.nextLine().trim();
            System.out.print("白番AIプログラムのファイルパス (.jar) を入力してください: ");
            whiteAIPath = scanner.nextLine().trim();
            // Scannerはmainメソッド終了時に自動で閉じられることが期待されるが、明示的に閉じる
            // ただし、System.inを閉じると他の標準入力に影響が出るため、ここでは閉じない方が安全な場合もある。
        } else {
            // 引数の数が不正
            System.out.println("❌ 起動エラー: 引数の数が不正です。");
            System.out.println("使用法1: java -cp classes OthelloJudgeConsole [黒AIパス] [白AIパス]");
            System.out.println("使用法2: java -cp classes OthelloJudgeConsole (引数なし)");
            return;
        }



        // 3. ゲーム開始
        try {
            // 黒と白を入れ替えて戦う
            for (int j = 0; j < 2; j++) {
                if (j == 1) {
                    String temp = blackAIPath;
                    blackAIPath = whiteAIPath;
                    whiteAIPath = temp;
                }
                // ３回勝負
                for (int i = 1; i <= 3; i++) {
                    System.out.print("黒：" + blackAIPath + " 、 白：" + whiteAIPath + " ：" + i + "回戦:対戦中・・・");
                    OthelloJudgeConsole judge = new OthelloJudgeConsole(blackAIPath, whiteAIPath);
                    Result result = judge.runGameLoop();
                    if (result == null) {
                        return;
                    }
                    System.out.print("\r");
                    String winner = "引き分け";
                    if (result.countBlack > result.countWhite) {
                        winner = "勝者：黒：" + result.winnerAiPah;
                    } else if (result.countBlack < result.countWhite) {
                        winner = "勝者：白：" + result.winnerAiPah;
                    }
                    System.out.println("黒：" + blackAIPath + " 、 白：" + whiteAIPath + " ：" + i + "回戦:試合終了：黒(" + result.countBlack + "), 白(" + result.countWhite + "), " + winner );
                }
            }
            
        } catch (Exception e) {
            System.err.println("致命的なエラーが発生しました: " + e.getMessage());
            e.printStackTrace(); // スタックトレースをファイルに出力
        }
    }
    
    public OthelloJudgeConsole(String blackPath, String whitePath) {
        // AIアダプターの初期化
        blackAI = new AIPlayerAdapter(blackPath, GameEngine.BLACK);
        whiteAI = new AIPlayerAdapter(whitePath, GameEngine.WHITE);
    }


    /**
     * ログファイルを設定し、PrintStreamをセットアップする。
     * @param blackAIPath 黒番AIのパス
     * @param whiteAIPath 白番AIのパス
     * @return 設定が成功したかどうか
     */
    private boolean setupLogFile(String blackName, String whiteName) {
        try {
            // 日付フォーマット (例: 20251202_093000)
            String dateString = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

            // 最終ファイル名: 日付_黒番ファイル名_白番ファイル名.txt
            String logFileName = dateString + "_" + blackName + "_" + whiteName + ".log";

            // PrintStreamを設定 (UTF-8エンコーディング指定)
            // これ以降、log.println()でファイルに書き込まれる
            log = new PrintStream(new File(logFileName), "UTF-8");
            
            
            return true;
        } catch (FileNotFoundException e) {
            System.err.println("❌ ログファイルの作成に失敗しました: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("❌ ファイル名解析中に予期せぬエラーが発生しました: " + e.getMessage());
            return false;
        }
    }
    /** メインのゲームループ */
    private Result runGameLoop() {
        try {
            Result result = null;

            // ログファイルの設定とリダイレクト
            if (!setupLogFile(blackAI.getPlayerName(), whiteAI.getPlayerName())) {
                return null;
            }

            // ログストリームにヘッダーを出力 (ファイルへの書き込み開始)
            log.println("==================================================");
            log.println("========== Othello AI Judge Console ==============");
            log.println("==================================================");
            log.println("黒番 AI: " + blackAI.getAiPath());
            log.println("白番 AI: " + whiteAI.getAiPath());
            log.println("開始日時: " + new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()));
            log.println("--------------------------------------------------");
            
            gameEngine.resetBoard();
            
            // 盤面が埋まるか、連続パスが発生するまでループ
            while (true) {
                AIPlayerAdapter currentAI = (gameEngine.getCurrentPlayer() == GameEngine.BLACK) ? blackAI : whiteAI;
                int opponentColor = (gameEngine.getCurrentPlayer() == GameEngine.BLACK) ? GameEngine.WHITE : GameEngine.BLACK;
                
                log.println("------------------------------------");
                log.println("手番: " + currentAI.getPlayerName());
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
                    return endGame(opponentColor, currentAI.getPlayerName() + "が5秒以内に応答しなかったため、");
                } catch (IOException e) {
                    // 3. 通信エラーなど
                    log.println("[ERROR] AI実行エラー: " + e.getMessage());
                    return endGame(opponentColor, currentAI.getPlayerName() + "の実行中にエラーが発生したため、");
                }
                
                // 4. 着手処理
                if (move.equalsIgnoreCase("pass")) {
                    result = handlePass(hasLegalMove, currentAI, opponentColor);
                    if (result != null) {
                        return result;
                    }
                } else {
                    result = handleStonePlacement(move, hasLegalMove, currentAI, opponentColor);
                    if (result != null) {
                        return result;
                    }
                }

                // 5. 手番交代
                gameEngine.switchPlayer();
                
                // 6. 最終判定 (盤面が完全に埋まった場合など)
                if (gameEngine.countStones(GameEngine.EMPTY) == 0) {
                    return endGame(0, "盤面が完全に埋まりました。"); // 0は引き分け判定用
                }
            }
        } catch (Exception e) {
            System.err.println("致命的なエラーが発生しました: " + e.getMessage());
            log.println("致命的なエラーが発生しました: " + e.getMessage());
            e.printStackTrace(log); // スタックトレースをファイルに出力
            return null;
        } finally {
            if (log != null) {
                log.close(); // ログファイルを閉じる
                log = null;
            }
        }
    }
    
    /** パス処理 */
    private Result handlePass(boolean hasLegalMove, AIPlayerAdapter currentAI, int opponentColor) {
        if (hasLegalMove) {
            // 合法手があるのにパスは無効手
            log.println("[ERROR] 無効手: 合法手があるにも関わらずパスしました。");
            return endGame(opponentColor, currentAI.getPlayerName() + "が無効手（不必要なパス）を打ったため、");
        } else {
            log.println("（合法手がないためパスしました）");
            // パス後に相手にも合法手がないかチェック
            gameEngine.switchPlayer(); // 一時的に相手に手番を渡す
            if (!gameEngine.hasLegalMove()) {
                return endGame(0, "両者とも打つ手がなくなり、ゲーム終了。");
            }
            gameEngine.switchPlayer(); // 手番を元に戻す
            return null;
        }
    }
    
    /** 石の配置処理 */
    private Result handleStonePlacement(String move, boolean hasLegalMove, AIPlayerAdapter currentAI, int opponentColor) {
        if (!gameEngine.applyMove(move)) {
            // 不正な座標、または合法手ではない
            log.println("[ERROR] 無効手: 座標 " + move + " は合法手ではありません。");
            return endGame(opponentColor, currentAI.getPlayerName() + "が無効手（不正な位置への着手）を打ったため、");
        } else if (!hasLegalMove) {
            // パスしなければならない局面で着手した場合も無効手
            log.println("[ERROR] 無効手: パスしなければならない局面で着手しました。");
            return endGame(opponentColor, currentAI.getPlayerName() + "が無効手（本来パスすべき局面での着手）を打ったため、");
        } else {
            // 合法な着手
            log.println("[SUCCESS] " + currentAI.getPlayerName() + "の着手 (" + move + ") を適用しました。");
            log.println(gameEngine.displayBoard()); // 更新後の盤面表示
            log.println("MOVE BOARD:" +  gameEngine.boardToString());
            return null;
        }
    }

    /** ゲーム終了処理 */
    private Result endGame(int winnerColor, String reason) {
        log.println("\n====================================");
        log.println("GAME OVER - " + reason);
        log.println(gameEngine.displayBoard());
        
        Result result =new Result();

        result.countBlack = gameEngine.countStones(GameEngine.BLACK);
        result.countWhite = gameEngine.countStones(GameEngine.WHITE);
        
        log.println("最終結果: 黒(" + result.countBlack + ") vs 白(" + result.countWhite + ")");
        
        String winner;
        if (winnerColor == GameEngine.BLACK) {
            winner = blackAI.getPlayerName();
        } else if (winnerColor == GameEngine.WHITE) {
            winner = whiteAI.getPlayerName();
        } else {
            // 0の場合、通常の石数判定か引き分け
            if (result.countBlack > result.countWhite) {
                winner = blackAI.getPlayerName();
                result.winnerAiPah = blackAI.getAiPath();
            } else if (result.countWhite > result.countBlack) {
                winner = whiteAI.getPlayerName();
                result.winnerAiPah = whiteAI.getAiPath();
            } else {
                winner = "引き分け";
            }
        }
        
        log.println("勝者: " + winner);
        log.println("====================================\n");

        return result;
    }
}