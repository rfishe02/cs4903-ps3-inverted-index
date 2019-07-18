
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
        File f = new File(con.getRealPath("/raf/dict.raf"));
        
        
        
        out.close();
      
      } catch( Exception ex ) {
        System.exit(1);
      }

   }

   public void destroy() {
   
   }
}
