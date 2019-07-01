
import java.io.*;
import java.util.TreeMap;
import java.util.SortedMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Comparator;

public class UAInvertedIndex {

  static GlobalMap gh;
  static int termID;

  public static void main(String[] args) {

    if(args.length < 1) {
      args = new String[2];
      args[0] = "./input";
      args[1] = "./output";
    }

    File inDir = new File(args[0]);
    File outDir = new File(args[1]);

    gh = new GlobalMap(3000000); // initialize global hash table.
    termID = 0;

    SortedMap<String, Integer> ht;
    BufferedReader br;
    BufferedWriter bw;
    TermData t;
    String read;
    int totalFreq;

    int docID = 0;

    try {

      for(File d : inDir.listFiles()) {

        br = new BufferedReader(new FileReader(d));

        ht = new TreeMap<String,Integer>(new TermComparator()); // initialize a document hash table.
        totalFreq = 0; // set totalFreq to zero.

        while((read = br.readLine())!=null) {
          if(ht.containsKey(read)) {
            ht.put(read,ht.get(read)+1);
          } else {
            ht.put(read,1);
          }
          totalFreq++;
        }

        bw = new BufferedWriter(new FileWriter(outDir.getPath()+"/doc"+docID+".temp")); // Open new temporary file f.

        // for all term t in document hash table ht, do

        for(Map.Entry<String,Integer> entry : ht.entrySet()) {

          if( ( t = gh.get( entry.getKey() ) ) != null ) {

            t.setCount(t.getCount() + 1);
            gh.put(t);

          } else {

            t = new TermData(entry.getKey(),termID,1); // put(t, <termID, # documents = 1>)
            gh.put(t);
            termID = termID + 1;

          } // if a term hasn't been found in prior documents.

          bw.write(entry.getKey() +" "+ docID +" "+ entry.getValue() +"\n"); // f.write(t, documentID, termFrequency (or tf / totalFrequency));

        } // Sort all ht entries by term alphabetically.

        docID++;
        br.close();
        bw.close(); // close temp file f.
      }

    } catch(Exception ex) {
      ex.printStackTrace();
      System.exit(1);
    }

  }

  public void buildInvertedIndex() {



  }

  static class TermComparator implements Comparator<String> {
    public int compare(String s1, String s2) {
      return s1.compareToIgnoreCase(s2);
    }
  }

}
