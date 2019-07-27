
/**
An object that stores term data in the global hash table.
*/

public class TermData {
  private String term;
  private int docCount;
  private int start;
  private float idf;

  public TermData(String term, int docCount) {
    this.term = term;
    this.docCount = docCount;
    this.start = -9;
  }

  public String toString() {
    return term +" "+ docCount+" "+start;
  }

  public void setT(String term) {
    this.term = term;
  }

  public String getT() {
    return this.term;
  }

  public void setCount(int docCount) {
    this.docCount = docCount;
  }

  public int getCount() {
    return this.docCount;
  }

  public void setStart(int start) {
    this.start = start;
  }

  public int getStart() {
    return start;
  }

  public void setIDF(float idf) {
    this.idf = idf;
  }

  public float getIDF() {
    return idf;
  }

}
