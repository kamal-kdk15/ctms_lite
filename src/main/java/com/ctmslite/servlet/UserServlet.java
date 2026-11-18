package com.ctmslite.servlet;

import com.ctmslite.util.AuditLogger;
import com.ctmslite.util.DBConnection;
import com.ctmslite.util.DBUtil;
import com.ctmslite.util.WebUtil;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Admin account status view and unlock action. */
@WebServlet("/users")
public class UserServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        try(Connection c=DBConnection.getConnection()){
            req.setAttribute("users",DBUtil.query(c,
                    "SELECT user_id,username,role,failed_attempts,locked_until,"
                    +"(locked_until IS NOT NULL AND locked_until>NOW()) AS is_locked FROM users ORDER BY user_id"));
            req.getRequestDispatcher("/WEB-INF/views/users.jsp").forward(req,resp);
        }catch(SQLException e){throw new ServletException(e);}
    }
    @Override
    protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws IOException{
        Integer id=WebUtil.parseInt(req.getParameter("user_id")); int uid=WebUtil.userId(req);
        String reason=WebUtil.trim(req.getParameter("reason"));
        if(id==null||WebUtil.isBlank(reason)||reason.length()>255){
            WebUtil.flash(req,"danger","Select an account and enter an unlock reason.");
        }else{
            try(Connection c=DBConnection.getConnection()){
                int changed=DBUtil.update(c,"UPDATE users SET failed_attempts=0,locked_until=NULL WHERE user_id=?",id);
                if(changed>0){
                    AuditLogger.log(c,uid,"ACCOUNT_UNLOCKED","users","User ID "+id+" unlocked. Reason: "+reason);
                    WebUtil.flash(req,"success","Account unlocked.");
                }else WebUtil.flash(req,"warning","Account not found.");
            }catch(SQLException e){WebUtil.flash(req,"danger","Unable to unlock account.");}
        }
        WebUtil.redirect(req,resp,"/users");
    }
}
