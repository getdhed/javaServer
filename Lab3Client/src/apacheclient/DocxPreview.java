package apacheclient;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Небольшой встроенный просмотрщик текста DOCX для лабораторной работы.
 * DOCX остаётся настоящим файлом Word, полученным через HTTP.
 * Показываются абзацы, заголовки, простое форматирование и таблицы.
 * Сложная вёрстка, картинки, колонтитулы и фигуры не поддерживаются.
 */
public final class DocxPreview {
    private static final String W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private static final int MAX_XML_SIZE = 8 * 1024 * 1024;

    private DocxPreview() { }

    public static String fromFile(File file) throws Exception {
        byte[] documentXml = null;
        try (ZipInputStream zip = new ZipInputStream(new FileInputStream(file))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    documentXml = readLimited(zip, MAX_XML_SIZE);
                    break;
                }
            }
        }
        if (documentXml == null) {
            throw new IOException("Файл не является корректным DOCX: нет word/document.xml");
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        Document xml = factory.newDocumentBuilder()
                .parse(new java.io.ByteArrayInputStream(documentXml));
        NodeList bodies = xml.getElementsByTagNameNS(W, "body");
        if (bodies.getLength() == 0) {
            throw new IOException("В DOCX отсутствует тело документа");
        }

        StringBuilder html = new StringBuilder(8192);
        html.append("<html><head><meta charset='UTF-8'></head>")
            .append("<body style='font-family:SansSerif; color:#22313c; margin:22px;'>");
        Element body = (Element) bodies.item(0);
        for (Node node = body.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (is(node, "p")) {
                appendParagraph(html, (Element) node);
            } else if (is(node, "tbl")) {
                appendTable(html, (Element) node);
            }
        }
        html.append("</body></html>");
        return html.toString();
    }

    private static byte[] readLimited(InputStream in, int limit) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int n;
        while ((n = in.read(buffer)) != -1) {
            total += n;
            if (total > limit) {
                throw new IOException("DOCX слишком большой для встроенного просмотра");
            }
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }

    private static void appendParagraph(StringBuilder html, Element p) {
        Element pPr = child(p, "pPr");
        Element pStyle = child(pPr, "pStyle");
        String style = attr(pStyle, "val");
        Element jc = child(pPr, "jc");
        String align = "center".equals(attr(jc, "val")) ? "center" : "left";

        String openTag = "<p>";
        String closeTag = "</p>";
        if ("Title".equalsIgnoreCase(style)) {
            openTag = "<h1 align='center' style='color:#214b3e;'>";
            closeTag = "</h1>";
        } else if (style.startsWith("Heading1") || style.startsWith("heading 1")) {
            openTag = "<h2 style='color:#214b3e;'>";
            closeTag = "</h2>";
        } else if (style.startsWith("Heading2")) {
            openTag = "<h3>";
            closeTag = "</h3>";
        } else if ("center".equals(align)) {
            openTag = "<p align='center'>";
        }
        html.append(openTag);
        appendInline(html, p);
        html.append(closeTag);
    }

    private static void appendTable(StringBuilder html, Element table) {
        html.append("<table width='100%' border='1' cellpadding='7' cellspacing='0' ")
            .append("style='border-color:#bcc9c1; margin-top:10px; margin-bottom:10px;'>");
        int row = 0;
        for (Node tr = table.getFirstChild(); tr != null; tr = tr.getNextSibling()) {
            if (!is(tr, "tr")) continue;
            html.append("<tr>");
            for (Node tc = tr.getFirstChild(); tc != null; tc = tc.getNextSibling()) {
                if (!is(tc, "tc")) continue;
                if (row == 0) html.append("<th bgcolor='#dceadc'>");
                else html.append("<td>");
                for (Node p = tc.getFirstChild(); p != null; p = p.getNextSibling()) {
                    if (is(p, "p")) appendParagraph(html, (Element) p);
                }
                html.append(row == 0 ? "</th>" : "</td>");
            }
            html.append("</tr>");
            row++;
        }
        html.append("</table>");
    }

    private static void appendInline(StringBuilder html, Node parent) {
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (is(n, "r")) {
                Element run = (Element) n;
                Element properties = child(run, "rPr");
                boolean bold = child(properties, "b") != null;
                boolean italic = child(properties, "i") != null;
                boolean underline = child(properties, "u") != null;
                if (bold) html.append("<b>");
                if (italic) html.append("<i>");
                if (underline) html.append("<u>");
                for (Node rn = n.getFirstChild(); rn != null; rn = rn.getNextSibling()) {
                    if (is(rn, "t") || is(rn, "instrText")) {
                        html.append(escape(rn.getTextContent()));
                    } else if (is(rn, "tab")) {
                        html.append("&nbsp;&nbsp;&nbsp;&nbsp;");
                    } else if (is(rn, "br")) {
                        html.append("<br>");
                    }
                }
                if (underline) html.append("</u>");
                if (italic) html.append("</i>");
                if (bold) html.append("</b>");
            } else if (is(n, "hyperlink") || is(n, "ins")) {
                appendInline(html, n);
            }
        }
    }

    private static Element child(Element parent, String localName) {
        if (parent == null) return null;
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (is(n, localName)) return (Element) n;
        }
        return null;
    }

    private static boolean is(Node n, String localName) {
        return n != null && n.getNodeType() == Node.ELEMENT_NODE
                && W.equals(n.getNamespaceURI()) && localName.equals(n.getLocalName());
    }

    private static String attr(Element e, String localName) {
        return e == null ? "" : e.getAttributeNS(W, localName);
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }
}
