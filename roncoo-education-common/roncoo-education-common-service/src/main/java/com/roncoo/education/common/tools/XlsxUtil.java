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

        byte[] sheet = firstSheet(parts);
        if (sheet == null) {
            throw new IOException("文件里找不到工作表，可能不是有效的 xlsx");
        }
        return readSheet(sheet, shared);
    }

    /**
     * 取工作簿里排在第一位的工作表。
     * <p>
     * 先按 workbook.xml 声明的顺序取，再经 rels 换成实际文件名——
     * 文件名的顺序不等于工作表的顺序：模板现在带了「填写说明」第二张表，
     * 使用者在 Excel 里把它拖到前面，文件名多半还是 sheet2.xml，
     * 只按文件名排序就会把说明当成数据读进来。
     * 解析不出来时退回按文件名取最小的那个。
     */
    private static byte[] firstSheet(Map<String, byte[]> parts) {
        byte[] wb = parts.get("xl/workbook.xml");
        byte[] rels = parts.get("xl/_rels/workbook.xml.rels");
        if (wb != null && rels != null) {
            try {
                String wbXml = new String(wb, StandardCharsets.UTF_8);
                java.util.regex.Matcher m = java.util.regex.Pattern
                        .compile("<sheet\\b[^>]*r:id=\"([^\"]+)\"").matcher(wbXml);
                if (m.find()) {
                    String rid = m.group(1);
                    String relXml = new String(rels, StandardCharsets.UTF_8);
                    java.util.regex.Matcher t = java.util.regex.Pattern
                            .compile("<Relationship\\b[^>]*Id=\"" + java.util.regex.Pattern.quote(rid)
                                    + "\"[^>]*Target=\"([^\"]+)\"").matcher(relXml);
                    if (t.find()) {
                        String target = t.group(1).replaceFirst("^/?xl/", "").replaceFirst("^/", "");
                        byte[] hit = parts.get("xl/" + target);
                        if (hit != null) {
                            return hit;
                        }
                    }
                }
            } catch (Exception ignored) {
                // 结构不标准就走下面的兜底
            }
        }
        byte[] sheet = null;
        String firstName = null;
        for (Map.Entry<String, byte[]> e : parts.entrySet()) {
            String name = e.getKey();
            if (name.startsWith("xl/worksheets/") && name.endsWith(".xml")
                    && (firstName == null || name.compareTo(firstName) < 0)) {
                firstName = name;
                sheet = e.getValue();
            }
        }
        return sheet;
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
                        // 按 r 属性把中间跳过的空行补回来。
                        // Excel 不会为空行写 <row>，行 1、2 之后直接跟着 r="5"，
                        // 不补的话调用方按下标算出来的行号会整体前移，
                        // 导入报错提示的行号就对不上文件里实际的行——
                        // 管理员照着行号去找，看到的是一行没问题的数据
                        int rowNum = parseRowNum(r.getAttributeValue(null, "r"));
                        if (rowNum > 0 && rowNum - 1 > rows.size() && rowNum - 1 - rows.size() <= MAX_ROW_GAP) {
                            while (rows.size() < rowNum - 1) {
                                rows.add(new ArrayList<>());
                            }
                        }
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

    /**
     * 解压后单个条目、以及整包的字节上限。
     * <p>
     * 两万行的题库导出也就几 MB，这个额度对正常使用绰绰有余；
     * 它挡的是解压炸弹——压缩比可以做到上千倍，
     * 不设限的话一个几 MB 的上传就能把服务的堆吃光。
     */
    private static final long MAX_ENTRY_BYTES = 64L * 1024 * 1024;
    private static final long MAX_TOTAL_BYTES = 128L * 1024 * 1024;

    /**
     * 单次最多补多少个空行。
     * <p>
     * 有人在第 100 万行敲过一个字符，整张表的 r 就会跳到那里；
     * 无上限地补会凭空造出百万个空 List 把内存吃光。
     * 超过上限就不补了——行号会不准，但这种文件本身已经不正常。
     */
    private static final int MAX_ROW_GAP = 10000;

    /** &lt;row r="5"&gt; -&gt; 5，取不到返回 -1 */
    private static int parseRowNum(String ref) {
        if (ref == null || ref.isEmpty()) {
            return -1;
        }
        try {
            return Integer.parseInt(ref.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
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
        long total = 0;
        try (ZipInputStream zis = new ZipInputStream(in, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int len;
                long entryTotal = 0;
                while ((len = zis.read(buf)) > 0) {
                    entryTotal += len;
                    total += len;
                    // 边解边判，不能等读完再看大小——压缩包解出来多大在读完之前是未知的。
                    // 上传限制是 2GB（为传视频设的），而这里是整包解进内存：
                    // 一个几 MB 的 zip 炸弹解出来就能有几十 GB，足以把服务撑挂
                    if (entryTotal > MAX_ENTRY_BYTES || total > MAX_TOTAL_BYTES) {
                        throw new IOException("文件解压后过大，疑似损坏或非正常的 xlsx。"
                                + "正常的题库/员工导入文件不会超过 " + (MAX_TOTAL_BYTES / 1024 / 1024) + "MB");
                    }
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
     * 列宽按内容自动估算，并冻结表头行。
     */
    public static void write(OutputStream out, String sheetName, List<List<String>> rows) throws IOException {
        write(out, sheetName, rows, null);
    }

    /**
     * 一个工作表。
     */
    public static final class Sheet {
        final String name;
        final List<List<String>> rows;
        final int[] widths;

        public Sheet(String name, List<List<String>> rows, int[] widths) {
            this.name = name;
            this.rows = rows;
            this.widths = widths;
        }
    }

    /**
     * 写多工作表。
     * <p>
     * 导入模板用得到：填写说明放在第二张表，不跟数据挤在同一张表里——
     * 说明文字写在数据区的任何一列都会被导入当成一行数据去校验，
     * 于是每条说明都变成一条「题干为空」的报错。
     */
    public static void write(OutputStream out, List<Sheet> sheets) throws IOException {
        if (sheets == null || sheets.isEmpty()) {
            throw new IOException("至少要有一个工作表");
        }
        try (ZipOutputStream zos = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            StringBuilder ct = new StringBuilder()
                    .append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
                    .append("<Types xmlns=\"").append(NS_CT).append("\">")
                    .append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>")
                    .append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
                    .append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>");
            StringBuilder wbSheets = new StringBuilder();
            StringBuilder wbRels = new StringBuilder()
                    .append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
                    .append("<Relationships xmlns=\"").append(NS_PKG_REL).append("\">");

            for (int i = 0; i < sheets.size(); i++) {
                int n = i + 1;
                ct.append("<Override PartName=\"/xl/worksheets/sheet").append(n)
                        .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
                wbSheets.append("<sheet name=\"").append(esc(sheets.get(i).name))
                        .append("\" sheetId=\"").append(n).append("\" r:id=\"rId").append(n).append("\"/>");
                wbRels.append("<Relationship Id=\"rId").append(n).append("\" Type=\"").append(NS_REL)
                        .append("/worksheet\" Target=\"worksheets/sheet").append(n).append(".xml\"/>");
            }
            ct.append("</Types>");
            wbRels.append("</Relationships>");

            put(zos, "[Content_Types].xml", ct.toString());
            put(zos, "_rels/.rels",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                            + "<Relationships xmlns=\"" + NS_PKG_REL + "\">"
                            + "<Relationship Id=\"rId1\" Type=\"" + NS_REL + "/officeDocument\" Target=\"xl/workbook.xml\"/>"
                            + "</Relationships>");
            put(zos, "xl/workbook.xml",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                            + "<workbook xmlns=\"" + NS_MAIN + "\" xmlns:r=\"" + NS_REL + "\">"
                            + "<sheets>" + wbSheets + "</sheets></workbook>");
            put(zos, "xl/_rels/workbook.xml.rels", wbRels.toString());

            for (int i = 0; i < sheets.size(); i++) {
                put(zos, "xl/worksheets/sheet" + (i + 1) + ".xml", sheetXml(sheets.get(i)));
            }
        }
    }

    /**
     * 写单工作表。
     *
     * @param widths 各列宽度（Excel 字符宽度）。传 null 则按内容自动估算。
     *               列数不足的部分按估算值补齐。
     */
    public static void write(OutputStream out, String sheetName, List<List<String>> rows, int[] widths) throws IOException {
        write(out, List.of(new Sheet(sheetName, rows, widths)));
    }

    /** 生成一张工作表的 XML */
    private static String sheetXml(Sheet sheet) {
        List<List<String>> rows = sheet.rows;
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
                .append("<worksheet xmlns=\"").append(NS_MAIN).append("\">");

        // 冻结首行。表头有十几列，往下翻两屏就不知道哪列是哪列了
        sb.append("<sheetViews><sheetView workbookViewId=\"0\">")
                .append("<pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/>")
                .append("</sheetView></sheetViews>");

        int[] w = resolveWidths(rows, sheet.widths);
        if (w.length > 0) {
            sb.append("<cols>");
            for (int i = 0; i < w.length; i++) {
                sb.append("<col min=\"").append(i + 1).append("\" max=\"").append(i + 1)
                        .append("\" width=\"").append(w[i]).append("\" customWidth=\"1\"/>");
            }
            sb.append("</cols>");
        }

        sb.append("<sheetData>");
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
        return sb.toString();
    }

    /** 列宽下限/上限。太窄看不清，太宽一屏放不下几列 */
    private static final int MIN_WIDTH = 8;
    private static final int MAX_WIDTH = 42;

    /**
     * 估算列宽。
     * <p>
     * 中日韩字符在 Excel 里约占两个字符宽，按 1 计算的话中文表头永远偏窄——
     * 模板里「必填，单选/多选/判断」这类说明就是这么被截断的。
     * 只看前若干行：正文可能有很长的题干，全表扫描会把列撑到上限。
     */
    private static int[] resolveWidths(List<List<String>> rows, int[] given) {
        int cols = 0;
        for (List<String> r : rows) {
            cols = Math.max(cols, r.size());
        }
        int[] w = new int[cols];
        int sample = Math.min(rows.size(), 30);
        for (int c = 0; c < cols; c++) {
            if (given != null && c < given.length && given[c] > 0) {
                w[c] = Math.min(MAX_WIDTH, Math.max(MIN_WIDTH, given[c]));
                continue;
            }
            int max = 0;
            for (int r = 0; r < sample; r++) {
                List<String> row = rows.get(r);
                if (c >= row.size() || row.get(c) == null) {
                    continue;
                }
                max = Math.max(max, displayWidth(row.get(c)));
            }
            // 留两格余量，否则内容会顶到边框
            w[c] = Math.min(MAX_WIDTH, Math.max(MIN_WIDTH, max + 2));
        }
        return w;
    }

    /** 全角字符按 2 个宽度算 */
    private static int displayWidth(String s) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            n += s.charAt(i) > 0x2E80 ? 2 : 1;
        }
        return n;
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
