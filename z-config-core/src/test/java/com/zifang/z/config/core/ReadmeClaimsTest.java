package com.zifang.z.config.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * z-config README 定量主张自守卫 —— README 里写死的版本、reactor 模块名单、REST 端点前缀、
 * 能力清单点名的类、客户端配置键、Netty 实际 bind 端口、测试类清单，
 * 必须能从源码重算出来；README 改了就红，代码没动就不红。
 *
 * <p><b>为什么要有这一层</b>：本仓 README 把「当前版本 1.0.10」「参与 reactor 的模块 = …（6 个）」
 * 「REST 前缀有哪些」「能力清单点到哪个类」「客户端配置键有哪些」「Netty 实际 bind 12888」
 * 写成了硬数字。此前**没有任何尺子**去问代码「现在还是吗」——
 * 实测已经漂过：本仓根 POM 抬到 {@code 1.0.10}（2026-10-02 的发布提交），
 * 而 README 的「当前版本」「Maven Central」「结构树 revision」三处仍写 {@code 1.0.9}。</p>
 *
 * <p><b>口径四条</b>（都是踩过的坑）：</p>
 * <ol>
 *   <li><b>事实对象只装源码侧事实</b>（{@link Facts}）—— README 侧的数值一律在核对<b>当下</b>
 *       从被核对的那份 README 里现解析。两边都从同一份 README 取数，
 *       尺子就成了恒等式比较，README 改成 9.9.9 尺子照样绿；</li>
 *   <li><b>主张抠不到就红</b>：正则没匹配上不许静默跳过，那正是空跑；</li>
 *   <li><b>清单类比集合，不比「每个都被点名」</b>：把一个模块名换成另一个同样存在的模块名，
 *       「每个都被点名」照样成立 ⇒ 尺子必须比集合；</li>
 *   <li><b>端点与能力清单只做「README → 源码」单向</b>：源码可以比 README 多
 *       （新增端点、新增类），README 说的每一条都必须能在源码里找到 ——
 *       这正是口径第 5 条「能力可追溯」要的方向。</li>
 * </ol>
 *
 * <p>本仓既有测试是 JUnit 4（{@code org.junit.Test}），但根 POM 的 {@code <dependencies>}
 * 直供 {@code junit-jupiter}（{@code test} 作用域，所有子模块继承）⇒ 本守卫用 JUnit 5，
 * 与 {@code z-boot-web-starter} 那条链上的新测试同口径。</p>
 */
public class ReadmeClaimsTest {

    /** 仓根标志：artifactId 命中 + packaging=pom + 真带 <module>（子模块 pom 的 parent 里也有同名 artifactId）。 */
    private static final String ROOT_ARTIFACT_ID = "z-config";

    /** 源码侧路径一律相对仓根写死，不按目录名统计（多模块仓里同名目录不止一处）。 */
    private static final String WEB_SRC = "z-config-web/src/main/java";
    private static final String PROPERTIES_SRC =
            "z-config-spring-boot-starter/src/main/java/com/zifang/z/config/starter/properties/ZConfigProperties.java";
    private static final String CONSTANCE_SRC =
            "z-config-common/src/main/java/com/zifang/z/config/common/Constance.java";

    /** 配置键前缀（{@code @ConfigurationProperties(prefix = "z.config")}）。 */
    private static final String CONFIG_PREFIX = "z.config";

    /** 客户端配置节里，README 声明「键名实测于该类」的那一段。 */
    private static final String SECTION_CAPABILITY = "能力清单";
    private static final String SECTION_API = "API 一览";
    private static final String SECTION_CONFIG = "客户端配置";
    private static final String SECTION_TEST = "测试";

    // ==================================================================
    // 基础设施
    // ==================================================================

    /**
     * 定位仓根：从 {@code user.dir} 向上找<b>聚合根</b>的 pom.xml。
     *
     * <p>判据同时满足：<b>项目自身</b>的 artifactId 命中（用 XML 解析取顶层
     * {@code <artifactId>}，不是全文 contains —— 子模块 pom 的 {@code <parent>} 里
     * 就写着 {@code z-config}，全文匹配会把子模块误判成仓根）+
     * {@code <packaging>pom</packaging>} + 至少一个 {@code <module>} + 同目录有 README。</p>
     */
    private static Path repoRoot() {
        Path p = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        for (int i = 0; i < 10 && p != null; i++, p = p.getParent()) {
            Path pom = p.resolve("pom.xml");
            if (Files.isRegularFile(pom) && Files.isRegularFile(p.resolve("README.md"))) {
                String t = stripXmlComments(readText(pom));
                boolean isRoot = ownArtifactId(t).equals(ROOT_ARTIFACT_ID)
                        && t.contains("<packaging>pom</packaging>")
                        && Pattern.compile("<module>").matcher(t).find();
                if (isRoot) {
                    return p;
                }
            }
        }
        throw new IllegalStateException("未找到 z-config 仓根：从 " + Paths.get("").toAbsolutePath()
                + " 向上找不到同时满足 artifactId=" + ROOT_ARTIFACT_ID
                + " / packaging=pom / 带 <module> / 同目录有 README.md 的 pom.xml");
    }

    /** 取 pom 里<b>项目自身</b>的 artifactId（顶层元素，不是 parent 里的那个）。 */
    private static String ownArtifactId(String pomText) {
        Document doc = parseXml(pomText);
        NodeList kids = doc.getDocumentElement().getChildNodes();
        for (int i = 0; i < kids.getLength(); i++) {
            Node n = kids.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && "artifactId".equals(n.getNodeName())) {
                return n.getTextContent().trim();
            }
        }
        return "";
    }

    private static Document parseXml(String xml) {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(false);
            return f.newDocumentBuilder().parse(
                    new java.io.ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("XML 解析失败", e);
        }
    }

    private static String readText(Path p) {
        try {
            return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("读文件失败：" + p, e);
        }
    }

    /** 剥掉 XML 注释（根 pom 里注释掉的 {@code <module>} 会骗过正则）。 */
    private static String stripXmlComments(String s) {
        return s.replaceAll("(?s)<!--.*?-->", "");
    }

    /**
     * 剥掉 Java 注释（块注释 + 行注释），字符串字面量原样保留。
     *
     * <p>数 {@code @Test} 方法时必须先剥注释：Javadoc 里出现过 {@code @Test} 字样
     * 就会被当成真测试方法。</p>
     */
    private static String stripComments(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        int i = 0;
        int n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (c == '"' || c == '\'') {
                char quote = c;
                sb.append(c);
                i++;
                while (i < n) {
                    char d = s.charAt(i);
                    sb.append(d);
                    i++;
                    if (d == '\\' && i < n) {
                        sb.append(s.charAt(i));
                        i++;
                        continue;
                    }
                    if (d == quote) {
                        break;
                    }
                }
                continue;
            }
            if (c == '/' && i + 1 < n && s.charAt(i + 1) == '/') {
                while (i < n && s.charAt(i) != '\n') {
                    i++;
                }
                continue;
            }
            if (c == '/' && i + 1 < n && s.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < n && !(s.charAt(i) == '*' && s.charAt(i + 1) == '/')) {
                    i++;
                }
                i = Math.min(n, i + 2);
                continue;
            }
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    /** 某目录下的全部 .java 文件（相对仓根的路径定位，不按目录名统计）。 */
    private static List<Path> javaFiles(Path dir) {
        List<Path> out = new ArrayList<>();
        if (!Files.isDirectory(dir)) {
            return out;
        }
        try (Stream<Path> s = Files.walk(dir)) {
            for (Path p : (Iterable<Path>) s.filter(x -> x.toString().endsWith(".java"))::iterator) {
                out.add(p);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out;
    }

    // ==================================================================
    // README 侧解析
    // ==================================================================

    /**
     * 去掉行首 #、emoji 与变体选择符，剩下的就是标题文字。
     *
     * <p>emoji 用码点判断（不吃编译编码的亏）：U+1F300–U+1FAFF、U+2600–U+27BF、
     * 变体选择符 U+FE0F、零宽连接符 U+200D。</p>
     */
    private static String headingText(String raw) {
        String t = raw.trim();
        while (t.startsWith("#")) {
            t = t.substring(1);
        }
        t = t.trim();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < t.length(); i++) {
            int cp = t.codePointAt(i);
            if (cp == 0xFE0F || cp == 0x200D
                    || (cp >= 0x1F300 && cp <= 0x1FAFF)
                    || (cp >= 0x2600 && cp <= 0x27BF)) {
                if (cp > 0xFFFF) {
                    i++;
                }
                continue;
            }
            sb.appendCodePoint(cp);
        }
        return sb.toString().trim();
    }

    /**
     * 取某一节的内容（不含标题行本身）。
     *
     * <p>按<b>标题文字前缀</b>匹配 —— 用全等会漏掉带后缀的标题
     * （本仓的「⚙️ 客户端配置（`z.config.*`）」「⚠️ 实测坑（接入前先看）」）。
     * 用前缀而不是 contains，是因为前缀能躲开「测试」撞「编译与测试」那类标记 ——
     * 撞的是<b>后缀</b>，「编译与测试」并不以「测试」开头。</p>
     *
     * <p>小节边界只在「同级或更高级」标题处断开。</p>
     */
    private static String section(String readme, String headingText) {
        String[] lines = readme.split("\n", -1);
        int start = -1;
        int level = 0;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (!line.startsWith("#")) {
                continue;
            }
            int lv = 0;
            while (lv < line.length() && line.charAt(lv) == '#') {
                lv++;
            }
            if (headingText(headingText(line)).startsWith(headingText) && level == 0) {
                start = i + 1;
                level = lv;
                break;
            }
        }
        if (start < 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < lines.length; i++) {
            String line = lines[i];
            if (line.startsWith("#")) {
                int lv = 0;
                while (lv < line.length() && line.charAt(lv) == '#') {
                    lv++;
                }
                if (lv <= level) {
                    break;
                }
            }
            sb.append(line).append('\n');
        }
        return sb.toString();
    }

    /** 取一张 Markdown 表格的各行单元格（不含表头与分隔行）。 */
    private static List<List<String>> tableRows(String sectionText) {
        List<List<String>> rows = new ArrayList<>();
        boolean headerSkipped = false;
        for (String line : sectionText.split("\n")) {
            String t = line.trim();
            if (!t.startsWith("|")) {
                continue;
            }
            if (t.replaceAll("[\\s|:-]", "").isEmpty()) {
                continue;
            }
            List<String> cells = new ArrayList<>();
            for (String c : t.replaceAll("^\\|", "").replaceAll("\\|$", "").split("\\|")) {
                cells.add(c.trim());
            }
            if (!headerSkipped) {
                headerSkipped = true;
                continue;
            }
            rows.add(cells);
        }
        return rows;
    }

    /** 去掉 Markdown 强调与行内代码的定界符，留下纯文字。 */
    private static String plain(String s) {
        return s.replace("`", "").replace("*", "").trim();
    }

    /** 抠出反引号包住的全部片段。 */
    private static List<String> codeSpans(String s) {
        List<String> out = new ArrayList<>();
        Matcher m = Pattern.compile("`([^`\n]+)`").matcher(s);
        while (m.find()) {
            out.add(m.group(1).trim());
        }
        return out;
    }

    // ==================================================================
    // 事实采集（只装源码侧事实）
    // ==================================================================

    /** 事实对象：<b>只装源码侧</b>的事实。README 侧的数字一律现解析。 */
    private static final class Facts {
        String revision = "";
        /** 根 POM {@code <modules>}（保序）。 */
        List<String> modules = new ArrayList<>();
        /** 全部模块 {@code src/main} 的类级 {@code @RequestMapping} 前缀。 */
        Set<String> apiPrefixes = new TreeSet<>();
        /** 本仓自己的类 + 源码里明确 import 过的类（简单名）。 */
        Set<String> knownClasses = new TreeSet<>();
        /** 客户端配置键（{@code z.config.xxx}）。 */
        Set<String> configKeys = new TreeSet<>();
        /** Netty 服务端实际 bind 的端口。 */
        int serveBindPort = -1;
        /** 全部模块 {@code src/test} 的测试类简单名。 */
        Set<String> testClasses = new TreeSet<>();
    }

    private static Facts facts() {
        Path root = repoRoot();
        Facts f = new Facts();

        // 根 pom：<revision> 与 <modules>（保序）
        String rootPom = stripXmlComments(readText(root.resolve("pom.xml")));
        Document doc = parseXml(rootPom);
        for (Node n : childElements(doc.getDocumentElement())) {
            if ("properties".equals(n.getNodeName())) {
                for (Node p : childElements(n)) {
                    if ("revision".equals(p.getNodeName())) {
                        f.revision = p.getTextContent().trim();
                    }
                }
            } else if ("modules".equals(n.getNodeName())) {
                for (Node m : childElements(n)) {
                    if ("module".equals(m.getNodeName())) {
                        f.modules.add(m.getTextContent().trim());
                    }
                }
            }
        }

        // 端点前缀：只取**类级** @RequestMapping —— 判据是「注解出现在该文件第一个 class 声明之前」。
        // 方法级映射与 Javadoc 里提到的注解都不算数（后者还要先剥注释）。
        for (Path p : javaFiles(root.resolve(WEB_SRC))) {
            String s = stripComments(readText(p));
            int cls = s.indexOf("class ");
            String head = cls > 0 ? s.substring(0, cls) : s;
            Matcher m = Pattern.compile("@RequestMapping\\s*\\(\\s*\"([^\"]+)\"").matcher(head);
            while (m.find()) {
                f.apiPrefixes.add(m.group(1));
            }
        }

        // 本仓类名 + import 过的类名（框架类如 IdleStateHandler 走 import 这条路）
        for (String module : f.modules) {
            for (String scope : new String[] {"src/main/java", "src/test/java"}) {
                for (Path p : javaFiles(root.resolve(module).resolve(scope))) {
                    String simple = p.getFileName().toString();
                    f.knownClasses.add(simple.substring(0, simple.length() - ".java".length()));
                    String s = stripComments(readText(p));
                    Matcher im = Pattern.compile("^\\s*import\\s+(?:static\\s+)?([\\w.]+);",
                            Pattern.MULTILINE).matcher(s);
                    while (im.find()) {
                        String fq = im.group(1);
                        f.knownClasses.add(fq.substring(fq.lastIndexOf('.') + 1));
                    }
                }
            }
        }
        // src/test 的测试类单独记一份（测试类清单那条判据要用）
        for (String module : f.modules) {
            for (Path p : javaFiles(root.resolve(module).resolve("src/test/java"))) {
                String simple = p.getFileName().toString();
                f.testClasses.add(simple.substring(0, simple.length() - ".java".length()));
            }
        }

        // 客户端配置键：ZConfigProperties 的字段名 → kebab（Spring relaxed binding）
        String props = stripComments(readText(root.resolve(PROPERTIES_SRC)));
        Matcher fm = Pattern.compile("^\\s*private\\s+(?:boolean|int|long|String|Integer|Long)\\s+(\\w+)\\s*[;=]",
                Pattern.MULTILINE).matcher(props);
        while (fm.find()) {
            f.configKeys.add(CONFIG_PREFIX + "." + toKebab(fm.group(1)));
        }

        // Netty 实际 bind 的端口
        String constance = stripComments(readText(root.resolve(CONSTANCE_SRC)));
        Matcher pm = Pattern.compile("serveBindPort\\s*=\\s*(\\d+)").matcher(constance);
        if (pm.find()) {
            f.serveBindPort = Integer.parseInt(pm.group(1));
        }

        return f;
    }

    /** 某元素下的全部直接子元素（跳过文本节点与注释）。 */
    private static List<Node> childElements(Node parent) {
        List<Node> out = new ArrayList<>();
        NodeList kids = parent.getChildNodes();
        for (int i = 0; i < kids.getLength(); i++) {
            Node n = kids.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                out.add(n);
            }
        }
        return out;
    }

    /** 驼峰转 kebab（首字母大写不转横线）。 */
    private static String toKebab(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                sb.append('-').append(Character.toLowerCase(c));
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }

    // ==================================================================
    // 核对
    // ==================================================================

    private static List<String> verify(String readme, Facts f) {
        List<String> problems = new ArrayList<>();

        // ---- 当前版本：README 基本信息表的「当前版本」行必须等于根 POM 的 <revision> ----
        String basic = section(readme, "基本信息");
        Matcher vm = Pattern.compile("\\*\\*当前版本\\*\\*\\s*\\|\\s*`([0-9]+\\.[0-9]+\\.[0-9]+)`").matcher(basic);
        if (!vm.find()) {
            problems.add("【版本】基本信息表里找不到「当前版本」那一行");
        } else if (!vm.group(1).equals(f.revision)) {
            problems.add("【版本】README 写 " + vm.group(1) + "，根 POM <revision> 是 " + f.revision);
        }

        // ---- reactor 模块名单：README 那句话里的名单 == 根 POM <modules>，且「不在 modules 里」的那几个确实不在 ----
        String structure = section(readme, "项目结构");
        // ⚠ DOTALL 是必须的：README 把模块名单排版成了**两行**，
        // 跨行不匹配 ⇒ 一行都抠不到（尺子自身的坑，不是 README 的）。
        Matcher rm = Pattern.compile("参与 reactor 的模块\\s*=\\s*(.+?)（(\\d+)\\s*个", Pattern.DOTALL)
                .matcher(structure);
        if (!rm.find()) {
            problems.add("【模块清单】项目结构一节里找不到「参与 reactor 的模块 = …（N 个）」这一句");
        } else {
            Set<String> said = new TreeSet<>();
            for (String sp : codeSpans(rm.group(1))) {
                if (sp.startsWith("z-config")) {
                    said.add(sp);
                }
            }
            Set<String> real = new TreeSet<>(f.modules);
            if (!said.equals(real)) {
                problems.add("【模块清单】README 说参与 reactor 的模块是 " + said
                        + "，根 POM <modules> 是 " + real);
            }
            int saidCount = Integer.parseInt(rm.group(2));
            if (saidCount != real.size()) {
                problems.add("【模块清单】README 说 " + saidCount + " 个模块，根 POM <modules> 实有 "
                        + real.size() + " 个");
            }
            // 「… 个目录**不在** `<modules>` 里」——按**行**取那一句，再抠它的反引号片段
            // （`<modules>` 本身除外）。不按固定个数抠：目录数会变，写死个数就成哑巴。
            Set<String> notIn = new TreeSet<>();
            boolean foundNotIn = false;
            for (String line : structure.split("\n")) {
                if (!line.contains("**不在**") || !line.contains("`<modules>` 里")) {
                    continue;
                }
                foundNotIn = true;
                for (String sp : codeSpans(line)) {
                    if (!sp.equals("<modules>")) {
                        notIn.add(sp);
                    }
                }
            }
            if (!foundNotIn) {
                problems.add("【模块清单】项目结构一节里找不到「… 个目录**不在** `<modules>` 里」那一句");
            }
            for (String name : notIn) {
                if (real.contains(name)) {
                    problems.add("【模块清单】README 说 " + name + " 不在 <modules> 里，但它就在根 POM <modules> 里");
                }
            }
        }

        // ---- REST 端点前缀：README API 一览表第一列点名的每个前缀都必须在源码里存在（单向）----
        String api = section(readme, SECTION_API);
        List<List<String>> apiRows = tableRows(api);
        if (apiRows.isEmpty()) {
            problems.add("【端点】API 一览一节里一张表都没抠到");
        }
        Set<String> saidPrefixes = new TreeSet<>();
        for (List<String> row : apiRows) {
            if (row.isEmpty()) {
                continue;
            }
            for (String sp : codeSpans(row.get(0))) {
                if (sp.startsWith("/")) {
                    saidPrefixes.add(sp);
                }
            }
        }
        if (saidPrefixes.isEmpty()) {
            problems.add("【端点】API 一览表第一列里一个路径前缀都没抠到");
        }
        for (String p : saidPrefixes) {
            if (!f.apiPrefixes.contains(p)) {
                problems.add("【端点】README 点名的前缀 " + p + " 在源码的类级 @RequestMapping 里找不到");
            }
        }

        // ---- 能力可追溯：README 能力清节点名的每个类都必须能在源码里找到（单向）----
        String cap = section(readme, SECTION_CAPABILITY);
        Set<String> saidClasses = new TreeSet<>();
        for (String sp : codeSpans(cap)) {
            String last = sp.substring(sp.lastIndexOf('/') + 1);
            if (last.matches("[A-Z][A-Za-z0-9]*")) {
                saidClasses.add(last);
            }
        }
        if (saidClasses.isEmpty()) {
            problems.add("【能力可追溯】能力清单一节里一个类名都没抠到");
        }
        for (String c : saidClasses) {
            if (!f.knownClasses.contains(c)) {
                problems.add("【能力可追溯】README 点名的类 " + c + " 在源码里找不到"
                        + "（本仓类与源码 import 过的类都算）");
            }
        }

        // ---- 客户端配置键：源码的每个键都必须在 README 那一节里被提到（kebab 或 camel 都算）----
        // ⚠ 判「被提到」**不能用 `contains`**：README 写 `connect-timeout-ms`，
        // 把它改成 `connect-timeout-msX` 后 contains 照样命中 ⇒ 尺子永远绿。
        // ⇒ 必须带**词边界**：键后面不能再跟字母/数字/连字符。
        String cfg = section(readme, SECTION_CONFIG);
        for (String key : f.configKeys) {
            String tail = key.substring(CONFIG_PREFIX.length() + 1);
            String camel = toCamel(tail);
            if (!mentions(cfg, tail, true) && !mentions(cfg, camel, false)) {
                problems.add("【配置键】源码的 " + key + " 在 README「客户端配置」一节里没被提到");
            }
        }

        // ---- Netty 实际 bind 的端口 ----
        String pitfalls = section(readme, "实测坑");
        Matcher bm = Pattern.compile("真正 bind 的是\\s*`([0-9]+)`").matcher(pitfalls);
        if (!bm.find()) {
            problems.add("【端口】实测坑一节里找不到「真正 bind 的是 `N`」这一处");
        } else if (Integer.parseInt(bm.group(1)) != f.serveBindPort) {
            problems.add("【端口】README 写 Netty 实际 bind " + bm.group(1)
                    + "，Constance.serveBindPort 是 " + f.serveBindPort);
        }

        // ---- 测试类清单：README 点名的每个测试类都必须在盘上存在（不钉用例个数）----
        String test = section(readme, SECTION_TEST);
        Set<String> saidTests = new TreeSet<>();
        for (String sp : codeSpans(test)) {
            String last = sp.substring(sp.lastIndexOf('/') + 1);
            if (last.matches("[A-Z][A-Za-z0-9]*")) {
                saidTests.add(last);
            }
        }
        if (saidTests.isEmpty()) {
            problems.add("【测试类清单】测试一节里一个测试类都没抠到");
        }
        for (String c : saidTests) {
            if (!f.testClasses.contains(c) && !f.knownClasses.contains(c)) {
                problems.add("【测试类清单】README 点名的 " + c + " 在盘上找不到源码");
            }
        }

        return problems;
    }

    /** 驼峰转 camelCase 原样（首字母大写的字段名）。 */
    private static String toCamel(String kebab) {
        StringBuilder sb = new StringBuilder();
        boolean up = false;
        for (int i = 0; i < kebab.length(); i++) {
            char c = kebab.charAt(i);
            if (c == '-') {
                up = true;
                continue;
            }
            sb.append(up ? Character.toUpperCase(c) : c);
            up = false;
        }
        return sb.toString();
    }

    /**
     * 判断一个词是否在文本里被<b>当成整个词</b>提到。
     *
     * <p>不能用 {@code contains} —— 「contains 命中」不等于「这个词被提到」：
     * 把 {@code connect-timeout-ms} 改成 {@code connect-timeout-msX}，contains 照样命中。
     * ⇒ 后面不能再跟字母/数字（kebab 形态还要挡连字符）。</p>
     */
    private static boolean mentions(String text, String token, boolean kebab) {
        String tail = kebab ? "(?![A-Za-z0-9\\-])" : "(?![A-Za-z0-9])";
        return Pattern.compile(Pattern.quote(token) + tail).matcher(text).find();
    }

    /**
     * 核对入口：不一致就抛 AssertionError（JUnit 5 的 fail 抛的是 AssertionError 子类）。
     *
     * <p>牙口用例直接用这个入口，才能验到「尺子真的会抛」而不只是「尺子算出了差异」。</p>
     */
    private static void assertConsistent(String readme, Facts f) {
        List<String> problems = verify(readme, f);
        if (!problems.isEmpty()) {
            StringBuilder sb = new StringBuilder("z-config README 与源码不一致，共 " + problems.size() + " 处：");
            for (String p : problems) {
                sb.append("\n  - ").append(p);
            }
            fail(sb.toString());
        }
    }

    // ==================================================================
    // 用例
    // ==================================================================

    @Test
    @DisplayName("用例 A：README 的每条定量主张都能从源码重算出来")
    void readmeClaimsMatchSource() {
        assertConsistent(readme(), facts());
    }

    @Test
    @DisplayName("用例 C：真实 README 不得被判红")
    void healthyReadmeIsNotAccused() {
        List<String> problems = verify(readme(), facts());
        assertTrue(problems.isEmpty(), "真实 README 被判红：\n" + String.join("\n", problems));
    }

    @Test
    @DisplayName("牙口：版本号改错必红，且报错指向版本那条主张")
    void mutatedVersionMustFail() {
        String readme = readme();
        String mutated = readme.replace("| **当前版本** | `1.0.10`", "| **当前版本** | `9.9.9`");
        assertFalse(mutated.equals(readme), "合成变异没生效：README 里没找到版本声明那一处");
        AssertionError err = assertThrows(AssertionError.class, () -> assertConsistent(mutated, facts()));
        assertTrue(err.getMessage().contains("9.9.9"), "报错没指出被改坏的版本号：\n" + err.getMessage());
        assertTrue(err.getMessage().contains("【版本】"), "报错没指向版本这条主张：\n" + err.getMessage());
    }

    @Test
    @DisplayName("牙口：模块名单改错必红，且报错指向模块那条主张")
    void mutatedModuleMustFail() {
        String readme = readme();
        String mutated = readme.replace("`z-config-common`", "`z-config-commonX`");
        assertFalse(mutated.equals(readme), "合成变异没生效：README 里没找到模块名单那一处");
        AssertionError err = assertThrows(AssertionError.class, () -> assertConsistent(mutated, facts()));
        assertTrue(err.getMessage().contains("z-config-commonX"), "报错没指出被改坏的模块名：\n" + err.getMessage());
        assertTrue(err.getMessage().contains("【模块清单】"), "报错没指向模块清单这条主张：\n" + err.getMessage());
    }

    @Test
    @DisplayName("牙口：端点前缀改错必红，且报错指向端点那条主张")
    void mutatedEndpointMustFail() {
        String readme = readme();
        String mutated = readme.replace("| `/api/cluster`", "| `/api/clusterX`");
        assertFalse(mutated.equals(readme), "合成变异没生效：README 里没找到端点前缀那一处");
        AssertionError err = assertThrows(AssertionError.class, () -> assertConsistent(mutated, facts()));
        assertTrue(err.getMessage().contains("/api/clusterX"), "报错没指出被改坏的前缀：\n" + err.getMessage());
        assertTrue(err.getMessage().contains("【端点】"), "报错没指向端点这条主张：\n" + err.getMessage());
    }

    @Test
    @DisplayName("守卫生效确认：仓根定位、事实采集与 README 抓取都真的工作")
    void guardItselfWorks() {
        Path root = repoRoot();
        assertTrue(Files.isRegularFile(root.resolve("pom.xml")), "仓根定位不对：" + root);
        assertTrue(Files.isRegularFile(root.resolve("README.md")), "仓根定位不对：" + root);
        Facts f = facts();
        assertNotNull(f, "facts 应能采到");
        assertFalse(f.revision.isEmpty(), "revision 没采到");
        assertFalse(f.modules.isEmpty(), "模块清单没采到");
        assertFalse(f.apiPrefixes.isEmpty(), "端点前缀没采到");
        assertFalse(f.knownClasses.isEmpty(), "类名集合没采到");
        assertFalse(f.configKeys.isEmpty(), "配置键没采到");
        assertTrue(f.serveBindPort > 0, "Netty bind 端口没采到");
        assertFalse(f.testClasses.isEmpty(), "测试类清单没采到");
        assertTrue(readme().contains("z-config"), "读到的 README 不含仓名，仓根定位可能错了");
        // 模块清单里不该有重复（重复会让集合比较失去意义）
        assertEquals(f.modules.size(), new TreeSet<>(f.modules).size(), "模块清单不应有重复");
    }

    private static String readme() {
        return readText(repoRoot().resolve("README.md"));
    }
}
