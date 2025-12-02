import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
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
        Scanner scanner = new Scanner(System.in);

        List<AiInfo> listAiInfo = new ArrayList<AiInfo>();

        // 1. 引数の処理
        if (args.length >= 2) {
            // 使用法1: java ... OthelloJudgeConsole [黒AIパス] [白AIパス]
            for (int i = 0; i< args.length; i++) {
                listAiInfo.add(new AiInfo(args[i]));
            }
        } else if (args.length == 0) {
            // 使用法2: java ... OthelloJudgeConsole (標準入力でパス入力)
            System.out.println("オセロAIジャッジを開始します。");
            System.out.println("AIプログラムのファイルパス (.jar) を入力していってください。未入力でEnterするとゲーム開始します。 ");
            System.out.print("パス or 未入力:");
            while (true) {
                String input = scanner.nextLine().trim();
                if (input.isEmpty()) {
                    break;
                }
                listAiInfo.add(new AiInfo(input));
            }
            if (listAiInfo.size() < 2 ) {
                System.out.println("[ERROR]AIプログラムは２つ以上指定してください。");
                scanner.close();
                return;
            }
        } else {
            // 引数の数が不正
            System.out.println("❌ 起動エラー: 引数の数が不正です。");
            System.out.println("使用法1: java -cp classes OthelloJudgeConsole [黒AIパス] [白AIパス]");
            System.out.println("使用法2: java -cp classes OthelloJudgeConsole (引数なし)");
            scanner.close();
            return;
        }



        // 3. ゲーム開始
        try {
            // 総当たりで戦う
            for (int i = 0; i < listAiInfo.size(); i++) {
                for (int j = i+1; j < listAiInfo.size(); j++) {

                    // 黒と白を入れ替えて戦う
                    for (int k = 0; k < 2; k++) {
                        int blackAi = k == 0 ? i : j;
                        int whiteAi = k == 0 ? j : i;

                        File fileBlack = listAiInfo.get(blackAi).file;
                        File fileWhite = listAiInfo.get(whiteAi).file;

                        // ３回勝負
                        for (int l = 1; l <= 3; l++) {
                            System.out.print("黒：" + fileBlack.getName() + " , 白：" + fileWhite.getName() + " ：" + l + "回戦:対戦中・・・");
                            OthelloJudgeConsole judge = new OthelloJudgeConsole(listAiInfo.get(blackAi).path, listAiInfo.get(whiteAi).path);
                            Result result = judge.runGameLoop();
                            if (result == null) {
                                scanner.close();
                                return;
                            }
                            System.out.print("\r");
                            String winner = "引き分け";
                            if (result.countBlack != result.countWhite) {
                                if (result.countBlack > result.countWhite) {
                                    winner = "勝者：黒：";
                                    listAiInfo.get(blackAi).winBlack++;
                                    listAiInfo.get(whiteAi).loseWhite++;
                                } else if (result.countBlack < result.countWhite) {
                                    winner = "勝者：白：";
                                    listAiInfo.get(blackAi).loseBlack++;
                                    listAiInfo.get(whiteAi).winWhite++;
                                }
                                if (result.winnerAiPah == null) {
                                    winner += result.winnerAiPah;
                                } else {
                                    File fileWinner = new File(result.winnerAiPah);
                                    winner += fileWinner.getName();
                                }
                            } else {
                                listAiInfo.get(blackAi).drawBlack++;
                                listAiInfo.get(whiteAi).drawWhiete++;
                            }
                            System.out.println("黒：" + fileBlack.getName() + " , 白：" + fileWhite.getName() + " ：" + l + "回戦:試合終了：黒(" + result.countBlack + "), 白(" + result.countWhite + "), " + winner + " : " + result.reason );

                            listAiInfo.get(blackAi).countStone += result.countBlack;
                            listAiInfo.get(whiteAi).countStone += result.countWhite;

                        }// l
                    }// k

                }// j
            }// i

            System.out.println("------------------------");
            // 並び替え
            listAiInfo.sort(new Comparator<AiInfo>() {
               @Override
               public int compare(AiInfo a1, AiInfo a2) {
                    int a1Win = (a1.winBlack+a2.winWhite);
                    int a2Win = a2.winBlack + a2.winWhite;
                    if (a1Win == a2Win) {
                        // 勝利数が同じ場合は負け数が少ない方が上
                        int a1Lose = (a1.loseBlack+a2.loseWhite);
                        int a2Lose = a2.loseBlack + a2.loseWhite;
                        return a1Lose - a2Lose;
                    }
                    return a2Win - a1Win;
               } 
            });
            for (int i = 0; i < listAiInfo.size(); i++) {
                AiInfo ai = listAiInfo.get(i);
                System.out.println(String.format("第%d位:勝ち:%02d(黒:%02d,白:%02d), 負け:%02d(黒:%02d,白:%02d), 引分:%02d(黒:%02d,白:%02d), 獲得石数:%03d : %s",
                    i+1,
                    ai.winBlack+ai.winWhite, ai.winBlack, ai.winWhite,
                    ai.loseBlack+ai.loseWhite, ai.loseBlack, ai.loseWhite,
                    ai.drawBlack+ai.drawWhiete, ai.drawBlack, ai.drawWhiete,
                    ai.countStone,
                    ai.file.getName()
                ));
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
                    if (move.length() > "[ERROR]".length() && move.substring(0, "[ERROR]".length()-1) == "[ERROR]" ) {
                        log.println("[ERROR] AIプログラム側でエラーが発生 " );
                        return endGame(opponentColor, currentAI.getPlayerName() + "でエラーが発生したため、");
                    }

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
        result.reason = reason;

        result.countBlack = gameEngine.countStones(GameEngine.BLACK);
        result.countWhite = gameEngine.countStones(GameEngine.WHITE);
        
        log.println("最終結果: 黒(" + result.countBlack + ") vs 白(" + result.countWhite + ")");
        
        String winner;
        if (winnerColor == GameEngine.BLACK) {
            winner = blackAI.getPlayerName();
            result.winnerAiPah = blackAI.getAiPath();
        } else if (winnerColor == GameEngine.WHITE) {
            winner = whiteAI.getPlayerName();
                result.winnerAiPah = whiteAI.getAiPath();
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