package com.ctmslite.servlet;

import com.ctmslite.util.DBConnection;
import com.ctmslite.util.DBUtil;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

/** CSV and PDF exports for audit logs and trial summaries. */
@WebServlet("/export")
public class ExportServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        String type=req.getParameter("type");
        String format=req.getParameter("format");
        if(!"audit".equals(type)&&!"trials".equals(type)){resp.sendError(400,"Unknown export type");return;}
        try(Connection c=DBConnection.getConnection()){
            List<Map<String,Object>> rows;
            List<String> columns=new ArrayList<String>();
            if("audit".equals(type)){
                rows=DBUtil.query(c,"SELECT l.log_id,u.username,l.action_type,l.affected_table,l.description,l.`timestamp` log_time "
                        +"FROM audit_logs l JOIN users u ON u.user_id=l.user_id ORDER BY l.log_id DESC");
                String[] cols={"log_id","username","action_type","affected_table","description","log_time"};
                for(String col:cols)columns.add(col);
            }else{
                rows=DBUtil.query(c,"SELECT t.trial_id,t.trial_name,t.drug_name,t.phase,t.status,t.start_date,t.end_date,t.target_enrollment,"
                        +"(SELECT COUNT(*) FROM randomization_allocations ra WHERE ra.trial_id=t.trial_id) + "
                        +"(SELECT COUNT(*) FROM patients p WHERE p.enrolled_trial=t.trial_id AND p.randomization_no IS NULL) enrolled, "
                        +"(SELECT COUNT(*) FROM trial_sites ts WHERE ts.trial_id=t.trial_id) site_count, "
                        +"(SELECT COUNT(*) FROM adverse_events a WHERE a.trial_id=t.trial_id) ae_count, "
                        +"(SELECT COUNT(*) FROM adverse_events a WHERE a.trial_id=t.trial_id AND a.is_sae=1) sae_count "
                        +"FROM trials t ORDER BY t.trial_id");
                String[] cols={"trial_id","trial_name","drug_name","phase","status","start_date","end_date","target_enrollment","enrolled","site_count","ae_count","sae_count"};
                for(String col:cols)columns.add(col);
            }
            if("pdf".equalsIgnoreCase(format)) writePdf(resp,type,columns,rows);
            else writeCsv(resp,type,columns,rows);
        }catch(SQLException e){throw new ServletException(e);}
    }
    private void writeCsv(HttpServletResponse resp,String type,List<String> columns,List<Map<String,Object>> rows)throws IOException{
        resp.setContentType("text/csv;charset=UTF-8");
        resp.setHeader("Content-Disposition","attachment; filename=\"ctms-"+type+"-export.csv\"");
        java.io.PrintWriter out=resp.getWriter();
        for(int i=0;i<columns.size();i++){if(i>0)out.print(',');out.print(quote(columns.get(i)));}out.println();
        for(Map<String,Object> row:rows){
            for(int i=0;i<columns.size();i++){if(i>0)out.print(',');Object v=row.get(columns.get(i));out.print(quote(v==null?"":String.valueOf(v)));}
            out.println();
        }
        out.flush();
    }
    private String quote(String value){return "\""+value.replace("\"","\"\"")+"\"";}
    private void writePdf(HttpServletResponse resp,String type,List<String> columns,List<Map<String,Object>> rows)throws IOException{
        resp.setContentType("application/pdf");
        resp.setHeader("Content-Disposition","attachment; filename=\"ctms-"+type+"-export.pdf\"");
        Document doc=new Document();
        try{
            PdfWriter.getInstance(doc,resp.getOutputStream());doc.open();
            doc.add(new Paragraph("CTMS Lite - "+("audit".equals(type)?"Audit Trail":"Trial Summary")));
            doc.add(new Paragraph("Generated from the current application database"));
            doc.add(new Paragraph(" "));
            PdfPTable table=new PdfPTable(columns.size());table.setWidthPercentage(100);
            for(String col:columns)table.addCell(new Phrase(col.replace('_',' ').toUpperCase()));
            for(Map<String,Object> row:rows)for(String col:columns){
                Object value=row.get(col);table.addCell(new Phrase(value==null?"":String.valueOf(value)));
            }
            doc.add(table);
        }catch(DocumentException e){throw new IOException(e);}finally{doc.close();}
    }
}
