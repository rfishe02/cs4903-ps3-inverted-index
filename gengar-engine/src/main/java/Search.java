
package src.main.java;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

//@WebServlet("/search")
public class Search extends HttpServlet {

   public void init() throws ServletException {
   
   }

   public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

      response.setContentType("text/html");
      PrintWriter out = response.getWriter();
      out.println("Not supported.");
      out.close();
      
   }
   
   public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
   
      response.setContentType("text/html");
      PrintWriter out = response.getWriter();
      ServletContext con = getServletContext();
      
      try {

        String query = request.getParameter("search-box");
        String[] spl = query.split(" ");
   
        /*File inDir = new File( con.getResource("/home/srv-read/clean/output").toURI() );
        File rafDir = new File( con.getResource("/home/srv-read/ps3/output").toURI() );*/
        
        /*
        out.println(inDir.canRead() && inDir.exists());
        out.println(rafDir.canRead() && rafDir.exists());
        out.println(new File(rafDir.getPath()+"/dict.raf").canRead());
        */
        
        File inDir = new File( "/home/srv-read/clean/output" );
        File rafDir = new File( "/home/srv-read/ps3/output" );
        
        UAQuery q = new UAQuery(rafDir,"stats.raf");
        String[] res = q.runQuery(inDir,rafDir,spl);
        
        out.print("resources/input ");
        
        for(String s : res) {
            out.print(s+" ");
        }
      
      } catch( Exception ex ) {
        out.write("Exception occured.");
        System.exit(1);
      }
      
      out.close();

   }

   public void destroy() {
   
   }
}
