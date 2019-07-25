
package src.main.java;

import java.io.*;

/** This class is used to test the UAQuery class from the command line. */

public class UAQueryTest {

    /** The main function used to process a query and return a list of results. It requires the input and output
    directories from the companion class, UAInvertedIndex.
    @param args Accepts the following arugment from the command line: <input tokenized files> <random access files> <w1> ... <wn>.
    */

    public static void main(String[] args) {

    if(args.length < 1) {
      String[] test = {"input2","output","cat","pictures","videos"};
      args = test;
    }/*************************************************************************/

    File inDir = new File(args[0]);
    File rafDir = new File(args[1]);

    String[] spl;
    int count = 0;

    for(int i = 2; i < args.length; i++) {
      spl = args[i].split("([\\s\\.&-])+");

      for(String s : spl) {
        count++;
      }
    }

    String[] query = new String[count];
    int j = 0;
    for(int i = 2; i < args.length; i++) {
        spl = args[i].split("([\\s\\.&-])+");

        for(String s : spl) {
          query[j] = s;
          j++;
        }
    }

    UAQuery q = new UAQuery(rafDir,"stats.raf");
    q.runQuery(inDir,rafDir,query);

  }

}
