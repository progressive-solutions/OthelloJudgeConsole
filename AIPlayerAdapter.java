import java.io.*;
import java.util.concurrent.*;

/**
 * 外部のAIプログラムと標準入出力で通信するためのアダプター。
 * タイムアウト処理（5秒）を実装。
 */
public class AIPlayerAdapter {
    private final String aiPath;
    private final int playerColor;
    private final String playerName;
    
    // タイムアウト時間（5秒）
    private static final int TIMEOUT_SECONDS = 5;

    public AIPlayerAdapter(String aiPath, int playerColor) {
        this.aiPath = aiPath;
        this.playerColor = playerColor;
        this.playerName = (playerColor == GameEngine.BLACK ? "黒(1)" : "白(2)") + " - " + new File(aiPath).getName();
    }

    public String getAiPath() {
        return aiPath;
    }

    public String getPlayerName() {
        return playerName;
    }
    public int getPlayerColor() {
        return playerColor;
    }

    /**
     * AIプロセスを起動し、指定されたタイムアウト時間で着手を受け取る。
     * @param boardString 盤面情報文字列
     * @return AIの着手文字列 ("a1"～"h8"または"pass")
     * @throws TimeoutException 5秒以内に応答がなかった場合
     * @throws IOException 通信エラーが発生した場合
     */
    public String getMove(String boardString) throws TimeoutException, IOException {
        String move = null;
        Process aiProcess = null;
        
        // 1. 外部プロセス起動
        try {
            // JARファイルまたはクラスファイルを実行するためのコマンド
            ProcessBuilder builder;
            if (aiPath.endsWith(".jar")) {
                 builder = new ProcessBuilder("java", "-jar", aiPath);
            } else {
                 // 例: java SampleAI (aiPath = SampleAI)
                 builder = new ProcessBuilder("java", aiPath); 
            }
            builder.redirectErrorStream(true); // エラー出力を標準出力に統合
            aiProcess = builder.start();

            // 2. 標準入力/出力の準備
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(aiProcess.getOutputStream()), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(aiProcess.getInputStream()));

            // 3. AIへの命令送信
            writer.println("COLOR " + playerColor); // 色の通知
            writer.println("MOVE BOARD:" + boardString); // 思考開始指示と盤面送信
            
            // 4. タイムアウト付きで応答を待つ
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<String> future = executor.submit(reader::readLine);

            try {
                // 5秒間、AIからの応答を待つ
                move = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException e) {
                // スレッド中断または実行エラー
                throw new IOException("AI execution error or interrupted: " + e.getMessage());
            } catch (java.util.concurrent.TimeoutException e) {
                // タイムアウト発生
                throw new TimeoutException("AI did not respond within " + TIMEOUT_SECONDS + " seconds.");
            } finally {
                // 終了処理
                future.cancel(true);
                executor.shutdownNow();
            }

        } finally {
            // プロセスを終了させる
            if (aiProcess != null) {
                aiProcess.destroyForcibly();
            }
        }
        
        if (move == null || move.isEmpty()) {
            throw new IOException("AI returned empty move.");
        }
        
        return move.trim();
    }
}