package fr.uge.clonewareDBClient;

/**
 * POJO representing Information.
 */
public class Information {
  private String filename;
  private long line_nb;
  private long hash;

  /**
   * Default constructor.
   */
  public Information() {
  }

  /**
   * Create information with filename, line_nb and hash.
   *
   * @param filename filename of the file
   * @param line_nb  first line number of instruction(s) in that file
   * @param hash     hash of that line
   */
  public Information(String filename, long line_nb, long hash) {
    this.filename = filename;
    this.line_nb = line_nb;
    this.hash = hash;
  }

  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  public long getLine_nb() {
    return line_nb;
  }

  public void setLine_nb(long line_nb) {
    this.line_nb = line_nb;
  }

  public long getHash() {
    return hash;
  }

  public void setHash(long hash) {
    this.hash = hash;
  }
}
