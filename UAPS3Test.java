
import java.io.*;

public class UAPS3Test {

  public static void main(String[] args) {

    String[] test = {"cat","videos","youtube"};

    UAQuery q = new UAQuery();
    q.runQuery(test);

  }

  public static void buildIndex(String[] args, boolean test) {

    if(test && args.length < 1) {
      args = new String[2];
      args[0] = "./input";
      args[1] = "./output";
    }/*************************************************************************/

    File inDir = new File(args[0]);
    File outDir = new File(args[1]);

    UAInvertedIndex i = new UAInvertedIndex();
    i.buildInvertedIndex(inDir,outDir);

  }

  public static void runQuery() {


  }

}
