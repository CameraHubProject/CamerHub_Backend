import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Verifies legacy row digests; optional Admin setup is restricted to the disposable test DB. */
class VerifyUnifiedDatabase {
    static Properties settings(String file)throws Exception{
        var properties=new Properties();
        for(String line:Files.readAllLines(Path.of(file))){int split=line.indexOf('=');if(split>0&&!line.startsWith("#")){String value=line.substring(split+1);if(value.startsWith("\"")&&value.endsWith("\""))value=value.substring(1,value.length()-1);properties.setProperty(line.substring(0,split),value);}}
        return properties;
    }
    public static void main(String[]args)throws Exception{
        Class.forName("org.postgresql.Driver");var config=settings(args.length>0?args[0]:".local/unified/test.env");
        try(var db=PrepareUnifiedTestDatabase.connect(config,config.getProperty("DB_URL"))){
            if(args.length>1&&args[1].equals("grant-test-admin")){
                String name;try(var q=db.createStatement();var rows=q.executeQuery("SELECT current_database()")){rows.next();name=rows.getString(1);}
                if(!name.startsWith("camerahub_unified_verify_")||args.length!=3)throw new IllegalArgumentException("Admin fixture requires disposable test DB");
                try(var q=db.prepareStatement("INSERT INTO camerahub.user_roles(user_id,role_code) SELECT id,'ADMIN' FROM camerahub.users WHERE id=? AND email LIKE 'micro-%@test.invalid' ON CONFLICT DO NOTHING")){q.setObject(1,UUID.fromString(args[2]));if(q.executeUpdate()!=1)throw new IllegalStateException("Expected new test account");}
                System.out.println("Test-only Admin fixture ready");return;
            }
            db.setReadOnly(true);int verified=0;
            for(String line:Files.readAllLines(Path.of(".local/unified/source-snapshot.tsv"))){var parts=line.split("\t");String actual=PrepareUnifiedTestDatabase.digest(db,parts[0],Arrays.asList(parts[1].split(",")));if(!parts[2].equals(actual))throw new IllegalStateException("Legacy data changed: "+parts[0]);verified++;}
            System.out.println("Verified unchanged legacy data in "+verified+" tables");
            try(var q=db.createStatement();var rows=q.executeQuery("SELECT (SELECT count(*) FROM camerahub.users),(SELECT count(*) FROM camerahub.products),(SELECT count(*) FROM camerahub.orders),(SELECT count(*) FROM camerahub.product_media_data),(SELECT count(*) FROM camerahub.user_roles WHERE role_code='INSPECTOR')")){rows.next();System.out.printf("Users=%d Products=%d Orders=%d PersistentPhotos=%d InspectorRoles=%d%n",rows.getInt(1),rows.getInt(2),rows.getInt(3),rows.getInt(4),rows.getInt(5));}
        }catch(SQLException error){System.err.println("Verification failed; SQLSTATE="+error.getSQLState());System.exit(1);}
    }
}
