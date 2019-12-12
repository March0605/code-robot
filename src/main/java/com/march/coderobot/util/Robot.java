package com.march.coderobot.util;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.WordUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;

@Component
public class Robot {

    @Autowired
    public TableMapper mapper;

    private static TableMapper tableMapper;

    @PostConstruct
    public void init(){
        tableMapper = mapper;
    }

    public static void output(String[] tableNames) {
        for(String tableName : tableNames){
            Map<String, String> tableInfo = tableMapper.get(tableName);
            List<Map<String, String>> columns = tableMapper.listColumns(tableName);
            generatorCode(tableInfo, columns);
        }
    }

    public static void generatorCode(Map<String, String> tableInfo, List<Map<String, String>> columns) {
        Properties prop = new Properties();
        prop.put("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        Velocity.init(prop);

        Configuration config = getConfig();

        List<Map<String, String>> columsList = new ArrayList<>();
        for (Map<String, String> column : columns) {
            Map<String,String> c = new HashMap<>(16);
            c.put("columnName",column.get("columnName"));
            c.put("dataType",column.get("dataType"));
            c.put("columnComment",column.get("columnComment"));
            c.put("extra",column.get("extra"));
            String attrName = toAttributeName(column.get("columnName"));
            c.put("attrName",attrName);
            c.put("attrname",StringUtils.uncapitalize(attrName));
            String attrType = config.getString(column.get("dataType"), "undefined");
            c.put("attrType",attrType);
            columsList.add(c);
        }

        Map<String, Object> map = new HashMap<>(16);
        map.put("tableName", tableInfo.get("tableName"));
        map.put("comments", tableInfo.get("tableComment"));
        String className = removeTablePrefix(tableInfo.get("tableName"), config.getList("tablePrefix"));
        map.put("className", className);
        map.put("classname", StringUtils.uncapitalize(className));
        map.put("columns", columsList);
        map.put("package", config.getString("package"));
        map.put("author", config.getString("author"));
        map.put("email", config.getString("email"));
        map.put("company", config.getString("company"));
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        map.put("datetime", df.format(new Date()));

        VelocityContext context = new VelocityContext(map);

        String path = createFiles(config.getString("package"));
        if(StringUtils.isNotEmpty(path)){
            List<String> templateNames = getTemplates();
            for (String name : templateNames) {
                StringWriter stringWriter = new StringWriter();
                Template template = Velocity.getTemplate(name, "UTF-8");
                template.merge(context, stringWriter);
                try {
                    OutputStream out = new FileOutputStream(path + getFileName(name,className));
                    IOUtils.write(stringWriter.toString(), out, "UTF-8");
                    IOUtils.closeQuietly(stringWriter);
                } catch (Exception e) {
                    System.out.println(map.get("tableName") + "生成失败：" + e.getMessage());
                }
            }
        }
    }

    /**
     * 功能描述:〈获取模板〉
     * @param  1
     * @return : java.util.List<java.lang.String>
     * @throws :
     */
    public static List<String> getTemplates() {
        List<String> templates = new ArrayList<String>();
        templates.add("templates/Service.java.vm");
        templates.add("templates/ServiceImpl.java.vm");
        templates.add("templates/Controller.java.vm");
        templates.add("templates/Entity.java.vm");
        return templates;
    }

    /**
     * 功能描述:〈获得Java属性名称〉
     * @param name 1
     * @return : java.lang.String
     * @throws :
     */
    public static String toAttributeName(String name) {
        return WordUtils.capitalizeFully(name, new char[]{'_'}).replace("_", "");
    }

    /**
     * 功能描述:〈去表的前缀〉
     * @param name 1
     * @param prefixs 2
     * @return : java.lang.String
     * @throws :
     */
    public static String removeTablePrefix(String name, List<Object> prefixs) {
        for(Object p : prefixs){
            if(name.indexOf(p.toString()) == 0 ){
                name = name.replace(p.toString(),"");
            }
        }
        return toAttributeName(name);
    }

    /**
     * 功能描述:〈获得配置信息〉
     * @param  1
     * @return : org.apache.commons.configuration.Configuration
     */
    public static Configuration getConfig() {
        try {
            return new PropertiesConfiguration("generator.properties");
        } catch (ConfigurationException e) {
            System.out.println("获取配置文件失败，" + e.getMessage());
        }
        return null;
    }

    /**
     * 功能描述:〈得到文件名〉
     * @param name 1
     * @param className 2
     * @return : java.lang.String
     */
    public static String getFileName(String name, String className) {
        if (name.contains("Service.java.vm")) {
            return File.separator + className + "Service.java";
        }
        if (name.contains("ServiceImpl.java.vm")) {
            return File.separator + className + "ServiceImpl.java";
        }
        if (name.contains("Controller.java.vm")) {
            return File.separator + className + "Controller.java";
        }
        if (name.contains("Entity.java.vm")) {
            return File.separator + className + ".java";
        }
        return null;
    }

    /**
     * 功能描述:〈得到输出目录〉
     * @param packageName 1
     * @return : java.lang.String
     */
    private static String createFiles(String packageName){
        File directory = new File(".");
        String path = null;
        try {
            path = directory.getCanonicalPath();
        } catch (IOException e) {
            System.out.println("获取当前目录失败");
            return null;
        }
        path += File.separator + "src" + File.separator + "main" + File.separator + "java" +
                File.separator + packageName.replace(".",File.separator) + File.separator + "out";
        File file = new File(path);
        System.out.println(path);
        if (!file.exists() && !file.isDirectory()) {
            file .mkdir();
        }
        return path;
    }

}
