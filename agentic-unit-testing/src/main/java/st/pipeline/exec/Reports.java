package st.pipeline.exec;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Parses the JUnit XML report and the JaCoCo XML coverage report. */
public final class Reports {

    public record Failure(String test, String message) {}

    public record TestResults(int total, int passed, List<Failure> failures) {
        public static TestResults none() { return new TestResults(0, 0, List.of()); }
    }

    /** Per-line coverage: missed/covered instructions (mi/ci) and branches (mb/cb), as reported by JaCoCo. */
    public record Line(int nr, int mi, int ci, int mb, int cb) {}

    public record Coverage(int linesCovered, int linesTotal, int branchesCovered, int branchesTotal,
                           List<Integer> missedLines, List<Line> partialBranchLines) {
        public double linePct() { return pct(linesCovered, linesTotal); }
        public double branchPct() { return pct(branchesCovered, branchesTotal); }
        private static double pct(int c, int t) { return t == 0 ? 100.0 : 100.0 * c / t; }
    }

    private Reports() {}

    public static TestResults parseJUnit(Path reportsDir) throws Exception {
        Path xml = reportsDir.resolve("TEST-junit-jupiter.xml");
        if (!Files.exists(xml)) return TestResults.none();
        Document doc = parse(xml);
        NodeList cases = doc.getElementsByTagName("testcase");
        List<Failure> failures = new ArrayList<>();
        for (int i = 0; i < cases.getLength(); i++) {
            Element tc = (Element) cases.item(i);
            Element f = first(tc, "failure");
            if (f == null) f = first(tc, "error");
            if (f != null) {
                String msg = f.getAttribute("message");
                if (msg.isBlank()) msg = f.getAttribute("type");
                failures.add(new Failure(tc.getAttribute("name"), msg));
            }
        }
        return new TestResults(cases.getLength(), cases.getLength() - failures.size(), failures);
    }

    public static Coverage parseJacoco(Path xml) throws Exception {
        Element report = parse(xml).getDocumentElement();
        int lc = 0, lt = 0, bc = 0, bt = 0;
        // Report-level counters are direct children of <report>
        for (Element c : children(report, "counter")) {
            int missed = Integer.parseInt(c.getAttribute("missed"));
            int covered = Integer.parseInt(c.getAttribute("covered"));
            switch (c.getAttribute("type")) {
                case "LINE" -> { lc = covered; lt = missed + covered; }
                case "BRANCH" -> { bc = covered; bt = missed + covered; }
                default -> {}
            }
        }
        List<Integer> missedLines = new ArrayList<>();
        List<Line> partial = new ArrayList<>();
        NodeList lines = report.getElementsByTagName("line");
        for (int i = 0; i < lines.getLength(); i++) {
            Element e = (Element) lines.item(i);
            Line l = new Line(Integer.parseInt(e.getAttribute("nr")), Integer.parseInt(e.getAttribute("mi")),
                    Integer.parseInt(e.getAttribute("ci")), Integer.parseInt(e.getAttribute("mb")),
                    Integer.parseInt(e.getAttribute("cb")));
            if (l.ci() == 0 && l.mi() > 0) missedLines.add(l.nr());
            else if (l.mb() > 0) partial.add(l);
        }
        return new Coverage(lc, lt, bc, bt, missedLines, partial);
    }

    private static Document parse(Path xml) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        // JaCoCo reports declare a DTD; do not try to fetch it
        f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        return f.newDocumentBuilder().parse(xml.toFile());
    }

    private static Element first(Element parent, String tag) {
        NodeList n = parent.getElementsByTagName(tag);
        return n.getLength() > 0 ? (Element) n.item(0) : null;
    }

    private static List<Element> children(Element parent, String tag) {
        List<Element> out = new ArrayList<>();
        for (var n = parent.getFirstChild(); n != null; n = n.getNextSibling())
            if (n instanceof Element e && e.getTagName().equals(tag)) out.add(e);
        return out;
    }
}
