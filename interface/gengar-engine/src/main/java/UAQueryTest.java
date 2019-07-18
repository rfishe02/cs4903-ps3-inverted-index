
public class UAQueryTest {

    /**
    The main function used to process a query and return a list of results. It requires the input and output
    directories from the companion class, UAInvertedIndex.
    @param inDir An input directory of temporary files.
    @param rafDar
    @param query A query as an array of words.
    @return A list of the top k results for the given query.
    */

    public static void main(String[] args) {
    
    if(args.length < 1) {
      String[] test = {"input2","output","cat","pictures","videos"};
      args = test;
    }/*************************************************************************/

    File inDir = new File(args[0]);
    File rafDir = new File(args[1]);

    try {
      
      UAQuery q = new UAQuery(rafDir,"stats.raf");
      q.runQuery(inDir,rafDir,args);

    } catch(IOException ex) {
      ex.printStackTrace();
      System.exit(1);
    }

  }

}
