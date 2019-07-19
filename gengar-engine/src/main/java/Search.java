
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

      try {
      
        PrintWriter out = response.getWriter();
        ServletContext con = getServletContext();
        
        String query = request.getParameter("search-box");
        String[] spl = query.split(" ");
        
        out.println(query);
        
        File inDir = new File( con.getRealPath("input") );
        File rafDir = new File( con.getRealPath("raf") );
        
        out.println(inDir.getPath());
        out.println(rafDir.getPath());
        
        UAQuery q = new UAQuery(rafDir,"stats.raf");
        String[] res = q.runQuery(inDir,rafDir,spl);
        
        for(String s : res) {
            out.println(s);
        }
        
        out.close();
      
      } catch( Exception ex ) {
        System.exit(1);
      }

   }

   public void destroy() {
   
   }
}
