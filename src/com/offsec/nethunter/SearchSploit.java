package com.offsec.nethunter;

/* 漏洞信息实体类 */
public class SearchSploit {
    private long id;               // 编号
    private String description;    // 描述
    private String file;           // 文件名
    private String date;           // 发布日期
    private String author;         // 作者
    private String platform;       // 平台
    private String type;           // 类型
    private Integer port;          // 端口
    /* 表名及字段常量 */
    final static String TABLE = "SearchSploitTable";
    final static String ID = "ID";
    final static String FILE = "FILE";
    final static String DESCRIPTION = "DESCRIPTION";
    final static String DATE = "DATE";
    final static String AUTHOR = "AUTHOR";
    final static String PLATFORM = "PLATFORM";
    final static String TYPE = "TYPE";
    final static String PORT = "PORT";

    public SearchSploit() {

    }

    public SearchSploit(long id, String file, String description, String date, String author, String type, String platform, Integer port) {
        this.id = id;
        this.file = file;
        this.description = description;
        this.date = date;
        this.author = author;
        this.type = type;
        this.platform = platform;
        this.port = port;
    }

    public void setId(long id) {
        this.id = id;
    }
    public long getId() {
        return id;
    }
    public String getFile() {
        return file;
    }
    public void setFile(String file) {
        this.file = file;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getDate() {
        return date;
    }
    public void setDate(String date) {
        this.date = date;
    }
    String getAuthor() {
        return author;
    }
    void setAuthor(String author) {
        this.author = author;
    }
    String getPlatform() {
        return platform;
    }
    void setPlatform(String platform) {
        this.platform = platform;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public Integer getPort() { return port; }
    public void setPort(Integer port) {
        this.port = port;
    }
}