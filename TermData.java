
public class TermData {

  private String term;
  private int termID;
  private int docCount;

  public TermData(String term, int termID, int docCount) {
    this.term = term;
    this.termID = termID;
    this.docCount = docCount;
  }

  public String toString() {
    return term +" "+ termID +" "+ docCount;
  }

  public void setT(String term) {
    this.term = term;
  }

  public String getT() {
    return this.term;
  }

  public void setID(int termID) {
    this.termID = termID;
  }

  public int getID() {
    return this.termID;
  }

  public void setCount(int docCount) {
    this.docCount = docCount;
  }

  public int getCount() {
    return this.docCount;
  }

}
