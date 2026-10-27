import java.nio.file.*;
import java.sql.*;
import java.security.MessageDigest;
import java.util.*;

/** Creates a disposable same-project Neon copy; the configured source is read-only. */
class PrepareUnifiedTestDatabase {
    static String quote(String name) { return "\"" + name.replace("\"", "\"\"") + "\""; }
    static Connection connect(Properties config, String url) throws Exception {
        var properties=new Properties(); properties.setProperty("user",config.getProperty("DB_USERNAME")); properties.setProperty("password",config.getProperty("DB_PASSWORD"));
        properties.setProperty("connectTimeout","15");properties.setProperty("socketTimeout","180");
        return DriverManager.getConnection(url,properties);
    }
    static String url(String original, String database) {
        int query=original.indexOf('?');String base=query<0?original:original.substring(0,query);
        return base.substring(0,base.lastIndexOf('/')+1)+database+(query<0?"":original.substring(query));
    }
    static void execute(Connection db,String sql)throws Exception {try(var statement=db.createStatement()){statement.execute(sql);}}
    static List<String> tables(Connection db)throws Exception {
        var names=new ArrayList<String>();
        try(var q=db.createStatement();var rows=q.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema='camerahub' AND table_type='BASE TABLE' AND table_name NOT LIKE '%schema_history%' ORDER BY table_name")){while(rows.next())names.add(rows.getString(1));}
        return names;
    }
    static List<String> columns(Connection db,String table,boolean generated)throws Exception {
        var names=new ArrayList<String>();
        try(var q=db.prepareStatement("SELECT column_name FROM information_schema.columns WHERE table_schema='camerahub' AND table_name=? AND (? OR is_generated='NEVER') ORDER BY ordinal_position")){
            q.setString(1,table);q.setBoolean(2,generated);try(var rows=q.executeQuery()){while(rows.next())names.add(rows.getString(1));}
        }
        return names;
    }
    static String digest(Connection db,String table,List<String> columns)throws Exception {
        var lines=new ArrayList<String>();String fields=String.join(",",columns.stream().map(PrepareUnifiedTestDatabase::quote).toList());
        try(var q=db.createStatement();var rows=q.executeQuery("SELECT row_to_json(snapshot)::text FROM (SELECT "+fields+" FROM camerahub."+quote(table)+") snapshot")){while(rows.next())lines.add(rows.getString(1));}
        Collections.sort(lines);var hash=MessageDigest.getInstance("SHA-256");for(String line:lines)hash.update(line.getBytes(java.nio.charset.StandardCharsets.UTF_8));return HexFormat.of().formatHex(hash.digest());
    }
    public static void main(String[] args)throws Exception {
        Class.forName("org.postgresql.Driver");var config=InspectUnifiedDatabase.config();String original=config.getProperty("DB_URL");
        String name="camerahub_unified_verify_"+UUID.randomUUID().toString().replace("-","").substring(0,12);
        Path directory=Path.of(".local/unified");Files.createDirectories(directory);
        try(var admin=connect(config,original)){execute(admin,"CREATE DATABASE "+quote(name));}
        Files.writeString(directory.resolve("test-databases.txt"),name+System.lineSeparator(),StandardOpenOption.CREATE,StandardOpenOption.APPEND);
        var testConfig=new ArrayList<String>();
        testConfig.add("DB_URL="+url(original,name));testConfig.add("DB_USERNAME="+config.getProperty("DB_USERNAME"));testConfig.add("DB_PASSWORD="+config.getProperty("DB_PASSWORD"));
        testConfig.addAll(List.of("PORT=18080","AI_ENABLED=false","GOOGLE_LOGIN_ENABLED=false","SESSION_COOKIE_SECURE=false","FRONTEND_URL=http://127.0.0.1:5173"));
        Files.write(directory.resolve("test.env"),testConfig);
        try(var source=connect(config,original);var target=connect(config,url(original,name))){
            source.setReadOnly(true);source.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);source.setAutoCommit(false);
            try(var q=target.createStatement();var rows=q.executeQuery("SELECT current_database()")){rows.next();if(!rows.getString(1).equals(name))throw new IllegalStateException("Wrong disposable target");}
            execute(target,Files.readString(Path.of("src/main/resources/db/unified/V1__legacy_baseline.sql")));
            var originals=tables(source);var available=tables(target);
            if(!available.containsAll(originals)){var missing=new ArrayList<>(originals);missing.removeAll(available);throw new IllegalStateException("Baseline missing legacy tables: "+missing);}
            target.setAutoCommit(false);
            execute(target,"TRUNCATE "+String.join(",",available.stream().map(t->"camerahub."+quote(t)).toList())+" CASCADE");
            var foreignKeys=new ArrayList<String[]>();
            try(var q=target.createStatement();var rows=q.executeQuery("SELECT c.relname,k.conname,k.condeferrable FROM pg_constraint k JOIN pg_class c ON c.oid=k.conrelid JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='camerahub' AND k.contype='f'")){while(rows.next())if(!rows.getBoolean(3))foreignKeys.add(new String[]{rows.getString(1),rows.getString(2)});}
            for(var fk:foreignKeys)execute(target,"ALTER TABLE camerahub."+quote(fk[0])+" ALTER CONSTRAINT "+quote(fk[1])+" DEFERRABLE INITIALLY DEFERRED");
            execute(target,"SET CONSTRAINTS ALL DEFERRED");
            var snapshots=new ArrayList<String>();var backup=new ArrayList<String>();
            for(String table:originals){
                var fields=columns(source,table,false);if(!columns(target,table,false).containsAll(fields))throw new IllegalStateException("Legacy column missing: "+table);
                String names=String.join(",",fields.stream().map(PrepareUnifiedTestDatabase::quote).toList());String placeholders=String.join(",",Collections.nCopies(fields.size(),"?"));int count=0;
                try(var select=source.createStatement();var rows=select.executeQuery("SELECT "+names+" FROM camerahub."+quote(table));var insert=target.prepareStatement("INSERT INTO camerahub."+quote(table)+" ("+names+") VALUES ("+placeholders+")")){
                    while(rows.next()){for(int i=0;i<fields.size();i++)insert.setObject(i+1,rows.getObject(i+1));insert.addBatch();count++;if(count%100==0)insert.executeBatch();}insert.executeBatch();
                }
                try(var select=source.createStatement();var rows=select.executeQuery("SELECT row_to_json(snapshot)::text FROM (SELECT "+names+" FROM camerahub."+quote(table)+") snapshot")){while(rows.next())backup.add(table+"\t"+rows.getString(1));}
                var compared=columns(source,table,true);compared.removeAll(Set.of("checkout_batch_id","image_sha256","image_dhash","image_signature"));
                String sourceHash=digest(source,table,compared);if(!sourceHash.equals(digest(target,table,compared)))throw new IllegalStateException("Snapshot mismatch: "+table);
                snapshots.add(table+"\t"+String.join(",",compared)+"\t"+sourceHash);
                System.out.println("COPIED "+table+": "+count+" rows; verified");
            }
            target.commit();
            for(var fk:foreignKeys)execute(target,"ALTER TABLE camerahub."+quote(fk[0])+" ALTER CONSTRAINT "+quote(fk[1])+" NOT DEFERRABLE");
            target.commit();source.rollback();
            Files.write(directory.resolve("source-snapshot.tsv"),snapshots);
            Files.write(directory.resolve("legacy-backup-"+System.currentTimeMillis()+".jsonl"),backup);
            System.out.println("Verified disposable Neon copy ready: "+name+". Original tables were read-only.");
        }catch(SQLException error){System.err.println("Copy failed; SQLSTATE="+error.getSQLState()+"; test database="+name);System.exit(1);}
    }
}
