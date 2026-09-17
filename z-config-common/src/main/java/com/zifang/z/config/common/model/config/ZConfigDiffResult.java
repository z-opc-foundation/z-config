package com.zifang.z.config.common.model.config;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置版本 Diff 对比结果
 * 对齐 Nacos 控制台的配置版本对比功能
 */
public class ZConfigDiffResult {

    /** 第一个版本的内容 */
    private String content1;

    /** 第二个版本的内容 */
    private String content2;

    /** 第一个版本的 MD5 */
    private String md51;

    /** 第二个版本的 MD5 */
    private String md52;

    /** 是否完全相同 */
    private boolean identical;

    /** 差异行列表 */
    private List<DiffLine> diffLines = new ArrayList<>();

    /** 第一个版本的修改时间 */
    private String gmtModified1;

    /** 第二个版本的修改时间 */
    private String gmtModified2;

    /** 第一个版本的操作人 */
    private String srcUser1;

    /** 第二个版本的操作人 */
    private String srcUser2;

    public String getContent1() {
        return content1;
    }

    public void setContent1(String content1) {
        this.content1 = content1;
    }

    public String getContent2() {
        return content2;
    }

    public void setContent2(String content2) {
        this.content2 = content2;
    }

    public String getMd51() {
        return md51;
    }

    public void setMd51(String md51) {
        this.md51 = md51;
    }

    public String getMd52() {
        return md52;
    }

    public void setMd52(String md52) {
        this.md52 = md52;
    }

    public boolean isIdentical() {
        return identical;
    }

    public void setIdentical(boolean identical) {
        this.identical = identical;
    }

    public List<DiffLine> getDiffLines() {
        return diffLines;
    }

    public void setDiffLines(List<DiffLine> diffLines) {
        this.diffLines = diffLines;
    }

    public String getGmtModified1() {
        return gmtModified1;
    }

    public void setGmtModified1(String gmtModified1) {
        this.gmtModified1 = gmtModified1;
    }

    public String getGmtModified2() {
        return gmtModified2;
    }

    public void setGmtModified2(String gmtModified2) {
        this.gmtModified2 = gmtModified2;
    }

    public String getSrcUser1() {
        return srcUser1;
    }

    public void setSrcUser1(String srcUser1) {
        this.srcUser1 = srcUser1;
    }

    public String getSrcUser2() {
        return srcUser2;
    }

    public void setSrcUser2(String srcUser2) {
        this.srcUser2 = srcUser2;
    }

    /**
     * 单行差异
     */
    public static class DiffLine {
        /** 行号（从 1 开始） */
        private int lineNumber;
        /** 差异类型：ADD=新增, DELETE=删除, MODIFY=修改, EQUAL=相同 */
        private String type;
        /** 版本1 的内容 */
        private String line1;
        /** 版本2 的内容 */
        private String line2;

        public DiffLine() {}

        public DiffLine(int lineNumber, String type, String line1, String line2) {
            this.lineNumber = lineNumber;
            this.type = type;
            this.line1 = line1;
            this.line2 = line2;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public void setLineNumber(int lineNumber) {
            this.lineNumber = lineNumber;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getLine1() {
            return line1;
        }

        public void setLine1(String line1) {
            this.line1 = line1;
        }

        public String getLine2() {
            return line2;
        }

        public void setLine2(String line2) {
            this.line2 = line2;
        }
    }
}
