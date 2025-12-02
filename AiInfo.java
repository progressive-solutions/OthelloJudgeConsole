import java.io.File;

public class AiInfo {
  public String path;
  public File file;
  public int winBlack;
  public int winWhite;
  public int loseBlack;
  public int loseWhite;
  public int drawBlack;
  public int drawWhiete;
  public int countStone;

  public AiInfo(String path) {
    this.path = path;
    this.file = new File(path);
    this.winBlack = 0;
    this.winWhite = 0;
    this.loseBlack = 0;
    this.loseWhite = 0;
    this.drawBlack = 0;
    this.drawWhiete = 0;
    this.countStone = 0;
  }

}
