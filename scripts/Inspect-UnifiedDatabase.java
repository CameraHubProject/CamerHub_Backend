import java.nio.file.*;
import java.sql.*;
import java.util.*;

class InspectUnifiedDatabase {
    static Properties config() throws Exception {
        var result = new Properties();
        for (String line : Files.readAllLines(Path.of(".env"))) {
            String text=line.strip();
            if(text.isEmpty()||text.startsWith("#")||!text.contains("="))continue;
            int separator=text.indexOf('=');String value=text.substring(separator+1);
            if(value.length()>1&&((value.startsWith("\"")&&value.endsWith("\""))||(value.startsWith("'")&&value.endsWith("'"))))value=value.substring(1,value.length()-1);
            result.setProperty(text.substring(0,separator),value);
        }
        return result;
    }
    public static void main(String[] args)throws Exception {
        Class.forName("org.postgresql.Driver");
        var settings=config();var properties=new Properties();
        properties.setProperty("user",settings.getProperty("DB_USERNAME"));properties.setProperty("password",settings.getProperty("DB_PASSWORD"));
        properties.setProperty("connectTimeout","15");properties.setProperty("socketTimeout","60");
        Files.createDirectories(Path.of(".local/unified"));
        try(var db=DriverManager.getConnection(settings.getProperty("DB_URL"),properties)){
            db.setReadOnly(true);
            var rowsOut=new ArrayList<String>();
            try(var statement=db.createStatement();var rows=statement.executeQuery("SELECT table_name,column_name,udt_name,is_nullable,is_generated,COALESCE(column_default,'') FROM information_schema.columns WHERE table_schema='camerahub' ORDER BY table_name,ordinal_position")){
                while(rows.next()){var cells=new ArrayList<String>();for(int i=1;i<=6;i++)cells.add(rows.getString(i).replace("\n"," ").replace("\t"," "));rowsOut.add(String.join("\t",cells));}
            }
            Files.write(Path.of(".local/unified/neon-schema.tsv"),rowsOut);
            var constraints=new ArrayList<String>();
            try(var statement=db.createStatement();var rows=statement.executeQuery("SELECT c.relname,k.conname,pg_get_constraintdef(k.oid) FROM pg_constraint k JOIN pg_class c ON c.oid=k.conrelid JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='camerahub' ORDER BY c.relname,k.conname")){
                while(rows.next())constraints.add(rows.getString(1)+"\t"+rows.getString(2)+"\t"+rows.getString(3).replace("\n"," "));
            }
            Files.write(Path.of(".local/unified/neon-constraints.tsv"),constraints);
            try(var statement=db.createStatement();var rows=statement.executeQuery("SELECT current_database(),rolcreatedb FROM pg_roles WHERE rolname=current_user")){rows.next();System.out.println("Schema metadata exported; database="+rows.getString(1)+", canCreateTestDatabase="+rows.getBoolean(2));}
        }catch(SQLException e){System.err.println("Schema inspection failed; SQLSTATE="+e.getSQLState());System.exit(1);}
    }
}
