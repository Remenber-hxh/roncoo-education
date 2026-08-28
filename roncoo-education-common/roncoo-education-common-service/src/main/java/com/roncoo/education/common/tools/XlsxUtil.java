package com.roncoo.education.common.tools;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 最小实现的 xlsx 读写，只用 JDK 自带能力（zip + StAX）。
 * <p>
 * 为什么不用 POI / EasyExcel：
 * 公共模块同时引了 easyexcel 3.2.1（要 poi-ooxml 4.1.2 -> poi 4.1.2）
 * 和 poi-scratchpad 5.2.5（要 poi 5.2.5），Maven 最终选了 poi 5.2.5，
 * easyexcel 用到的 org.apache.poi.util.POILogFactory 在 POI 5 已被移除，
 * 一调用就 NoClassDefFoundError。
 * 而本地 Maven 仓库里 poi 4.1.2 只有 pom 没有 jar、poi-ooxml 没有 5.x，
 * 离线环境既升不上去也降不下来。
 * xlsx 本身就是一个装着 XML 的 zip，读写它不需要第三方库，
 * 所以这里自己实现，顺带把依赖冲突这个雷绕开。
 * <p>
 * 只覆盖导入导出需要的部分：单工作表、纯文本单元格、不处理样式与公式。
 *
 * @author 二开
 */
public final class XlsxUtil {

    private XlsxUtil() {
    }

    private static final String NS_MAIN = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
    private static final String NS_REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
    private static final String NS_PKG_REL = "http://schemas.openxmlformats.org/package/2006/relationships";
    private static final String NS_CT = "http://schemas.openxmlformats.org/package/2006/content-types";

    // ================= 读 =================

    /**
     * 读第一个工作表，返回按行的单元格文本。
     * 数字单元格返回其原始文本（日期序列号如 45839 会原样返回，由调用方决定怎么解释）。
     */
    public static List<List<String>> read(InputStream in) throws IOException {
        Map<String, byte[]> parts = unzip(in);

        // 共享字符串表：t="s" 的单元格存的是这张表的下标
        List<String> shared = new ArrayList<>();
        byte[] ss = parts.get("xl/sharedStrings.xml");
        if (ss != null) {
            shared = readSharedStrings(ss);
        }

        // 取第一个工作表。不假定一定叫 sheet1.xml
        byte[] sheet = null;
        String firstName = null;
        for (Map.Entry<String, byte[]> e : parts.entrySet()) {
            String name = e.getKey();
            if (name.startsWith("xl/worksheets/") && name.endsWith(".xml")) {
                if (firstName == null || name.compareTo(firstName) < 0) {
                    firstName = name;
                    sheet = e.getValue();
                }
            }
        }
        if (sheet == null) {
            throw new IOException("文件里找不到工作表，可能不是有效的 xlsx");
        }
        return readSheet(sheet, shared);
    }

    private static List<String> readSharedStrings(byte[] xml) throws IOException {
        List<String> list = new ArrayList<>();
        try {
            XMLStreamReader r = newReader(xml);
            StringBuilder si = null;
            while (r.hasNext()) {
                int event = r.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    String ln = r.getLocalName();
                    if ("si".equals(ln)) {
                        si = new StringBuilder();
                    } else if ("t".equals(ln) && si != null) {
                        si.append(r.getElementText());
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT && "si".equals(r.getLocalName())) {
                    list.add(si == null ? "" : si.toString());
                    si = null;
                }
            }
            r.close();
        } catch (Exception e) {
            throw new IOException("解析 sharedStrings 失败: " + e.getMessage(), e);
        }
        return list;
    }

    private static List<List<String>> readSheet(byte[] xml, List<String> shared) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        try {
            XMLStreamReader r = newReader(xml);
            List<String> row = null;
            String cellType = null;
            int colIdx = -1;
            StringBuilder value = null;
            boolean inInlineStr = false;

            while (r.hasNext()) {
                int event = r.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    String ln = r.getLocalName();
                    if ("row".equals(ln)) {
                        row = new ArrayList<>();
                    } else if ("c".equals(ln) && row != null) {
                        cellType = r.getAttributeValue(null, "t");
                        colIdx = colIndex(r.getAttributeValue(null, "r"));
                        value = new StringBuilder();
                        inInlineStr = "inlineStr".equals(cellType);
                        // 自闭合的 <c r="B6"/> 在这里 START 后紧跟 END，
                        // 值保持为空即可，不能漏掉它的列位否则后面的列会整体错位
                    } else if ("v".equals(ln) && value != null && !inInlineStr) {
                        value.append(r.getElementText());
                    } else if ("t".equals(ln) && value != null && inInlineStr) {
                        value.append(r.getElementText());
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    String ln = r.getLocalName();
                    if ("c".equals(ln) && row != null && value != null) {
                        String text = value.toString();
                        if ("s".equals(cellType)) {
                            try {
                                int i = Integer.parseInt(text.trim());
                                text = i >= 0 && i < shared.size() ? shared.get(i) : "";
                            } catch (NumberFormatException ignored) {
                                text = "";
                            }
                        }
                        // 用列号补齐空缺，保证列位与表头对齐
                        if (colIdx >= 0) {
                            while (row.size() < colIdx) {
                                row.add("");
                            }
                            if (row.size() == colIdx) {
                                row.add(text);
                            } else {
                                row.set(colIdx, text);
                            }
                        } else {
                            row.add(text);
                        }
                        value = null;
                        cellType = null;
                        inInlineStr = false;
                    } else if ("row".equals(ln) && row != null) {
                        rows.add(row);
                        row = null;
                    }
                }
            }
            r.close();
        } catch (Exception e) {
            throw new IOException("解析工作表失败: " + e.getMessage(), e);
        }
        return rows;
    }

    /** A1 -> 0, B1 -> 1, AA1 -> 26 */
    private static int colIndex(String ref) {
        if (ref == null || ref.isEmpty()) {
            return -1;
        }
        int n = 0;
        for (int i = 0; i < ref.length(); i++) {
            char c = ref.charAt(i);
            if (c < 'A' || c > 'Z') {
                break;
            }
            n = n * 26 + (c - 'A' + 1);
        }
        return n - 1;
    }

    private static XMLStreamReader newReader(byte[] xml) throws Exception {
        XMLInputFactory f = XMLInputFactory.newInstance();
        // 关外部实体，避免上传的文件带 XXE
        f.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        f.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        return f.createXMLStreamReader(new ByteArrayInputStream(xml));
    }

    private static Map<String, byte[]> unzip(InputStream in) throws IOException {
        Map<String, byte[]> parts = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(in, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int len;
                while ((len = zis.read(buf)) > 0) {
                    bos.write(buf, 0, len);
                }
                parts.put(entry.getName(), bos.toByteArray());
            }
        }
        if (parts.isEmpty()) {
            throw new IOException("文件不是有效的 xlsx（解不出内容）");
        }
        return parts;
    }

    // ================= 写 =================

    /**
     * 写单工作表，全部按文本写入（inlineStr），不依赖共享字符串表。
     */
    public static void write(OutputStream out, String sheetName, List<List<String>> rows) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            put(zos, "[Content_Types].xml",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                            + "<Types xmlns=\"" + NS_CT + "\">"
                            + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                            + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                            + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                            + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                            + "</Types>");

            put(zos, "_rels/.rels",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                            + "<Relationships xmlns=\"" + NS_PKG_REL + "\">"
                            + "<Relationship Id=\"rId1\" Type=\"" + NS_REL + "/officeDocument\" Target=\"xl/workbook.xml\"/>"
                            + "</Relationships>");

            put(zos, "xl/workbook.xml",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                            + "<workbook xmlns=\"" + NS_MAIN + "\" xmlns:r=\"" + NS_REL + "\">"
                            + "<sheets><sheet name=\"" + esc(sheetName) + "\" sheetId=\"1\" r:id=\"rId1\"/></sheets>"
                            + "</workbook>");

            put(zos, "xl/_rels/workbook.xml.rels",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                            + "<Relationships xmlns=\"" + NS_PKG_REL + "\">"
                            + "<Relationship Id=\"rId1\" Type=\"" + NS_REL + "/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
                            + "</Relationships>");

            StringBuilder sb = new StringBuilder();
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
                    .append("<worksheet xmlns=\"").append(NS_MAIN).append("\"><sheetData>");
            for (int r = 0; r < rows.size(); r++) {
                List<String> row = rows.get(r);
                sb.append("<row r=\"").append(r + 1).append("\">");
                for (int c = 0; c < row.size(); c++) {
                    String v = row.get(c);
                    if (v == null || v.isEmpty()) {
                        continue;
                    }
                    sb.append("<c r=\"").append(colName(c)).append(r + 1).append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                            .append(esc(v)).append("</t></is></c>");
                }
                sb.append("</row>");
            }
            sb.append("</sheetData></worksheet>");
            put(zos, "xl/worksheets/sheet1.xml", sb.toString());
        }
    }

    private static void put(ZipOutputStream zos, String name, String content) throws IOException {
        zos.putNextEntry(new ZipEntry(name));
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    /** 0 -> A, 25 -> Z, 26 -> AA */
    private static String colName(int idx) {
        StringBuilder sb = new StringBuilder();
        int n = idx + 1;
        while (n > 0) {
            int r = (n - 1) % 26;
            sb.insert(0, (char) ('A' + r));
            n = (n - 1) / 26;
        }
        return sb.toString();
    }

    private static String esc(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&apos;");
                default -> {
                    // xlsx 不允许控制字符，遇到就丢掉，否则 Excel 会报文件损坏
                    if (c >= 0x20 || c == '\t' || c == '\n' || c == '\r') {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
