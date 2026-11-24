package org.perazimchurch.app.ai.guide;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The bundled, locked operating files: Agents.md, Skills.md and the playbooks they name. */
public final class GuideDocs {

    /** Reads a file by its path relative to the guide folder, for example "playbooks/crisis.md". */
    public interface Source {
        String read(String path) throws IOException;
    }

    /** One row of the Skills.md index. */
    public static final class SkillEntry {
        public final String id;
        public final List<String> triggers;
        public final String playbook;

        SkillEntry(String id, List<String> triggers, String playbook) {
            this.id = id;
            this.triggers = Collections.unmodifiableList(triggers);
            this.playbook = playbook;
        }
    }

    private static final String CORE_BEGIN = "<!-- CORE:BEGIN -->";
    private static final String CORE_END = "<!-- CORE:END -->";

    private final String version;
    private final String core;
    private final List<SkillEntry> index;
    private final Map<String, String> playbooks;

    private GuideDocs(String version, String core, List<SkillEntry> index, Map<String, String> playbooks) {
        this.version = version;
        this.core = core;
        this.index = Collections.unmodifiableList(index);
        this.playbooks = Collections.unmodifiableMap(playbooks);
    }

    public static GuideDocs load(Source source) throws IOException {
        String agents = source.read("Agents.md");
        String skills = source.read("Skills.md");
        List<SkillEntry> index = parseIndex(skills);
        Map<String, String> playbooks = new HashMap<>();
        for (SkillEntry entry : index) {
            if (!playbooks.containsKey(entry.playbook)) {
                playbooks.put(entry.playbook, source.read(entry.playbook));
            }
        }
        return new GuideDocs(parseVersion(agents), extractCore(agents), index, playbooks);
    }

    public String version() { return version; }

    /** The compact rule block that is always sent to the model. */
    public String core() { return core; }

    public List<SkillEntry> index() { return index; }

    /** Playbook body with its heading and markup lines removed. */
    public String playbook(String path) {
        String raw = playbooks.get(path);
        if (raw == null) return "";
        StringBuilder out = new StringBuilder();
        for (String line : raw.split("\n")) {
            String t = line.trim();
            if (t.isEmpty() || t.startsWith("#")) continue;
            if (t.startsWith("- ")) t = t.substring(2);
            if (out.length() > 0) out.append(' ');
            out.append(t);
        }
        return out.toString();
    }

    static String parseVersion(String agents) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("Version (\\d+\\.\\d+\\.\\d+)").matcher(agents);
        return m.find() ? m.group(1) : "unknown";
    }

    static String extractCore(String agents) {
        int begin = agents.indexOf(CORE_BEGIN);
        int end = agents.indexOf(CORE_END);
        if (begin < 0 || end < begin) throw new IllegalStateException("Agents.md is missing its CORE block");
        return agents.substring(begin + CORE_BEGIN.length(), end).trim();
    }

    /** Parses rows shaped like "| id | trigger, trigger | playbooks/x.md |". Header and separator rows are skipped. */
    static List<SkillEntry> parseIndex(String skills) {
        List<SkillEntry> out = new ArrayList<>();
        for (String line : skills.split("\n")) {
            String t = line.trim();
            if (!t.startsWith("|") || !t.endsWith("|")) continue;
            String[] cells = t.substring(1, t.length() - 1).split("\\|");
            if (cells.length != 3) continue;
            String id = cells[0].trim();
            String playbook = cells[2].trim();
            if (id.isEmpty() || id.equals("id") || id.startsWith("-") || !playbook.endsWith(".md")) continue;
            List<String> triggers = new ArrayList<>();
            for (String trigger : cells[1].split(",")) {
                String norm = GuideText.normalise(trigger);
                if (!norm.isEmpty() && !norm.startsWith("(")) triggers.add(norm);
            }
            out.add(new SkillEntry(id, triggers, playbook));
        }
        return out;
    }
}
