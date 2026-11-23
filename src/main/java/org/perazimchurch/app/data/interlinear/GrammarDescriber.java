package org.perazimchurch.app.data.interlinear;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns the morphology codes of the bundled interlinears into plain English.
 *
 * <p>Greek codes follow Robinson's parsing scheme ({@code V-AAI-3S}, {@code N-GSF}). Hebrew codes follow
 * the Open Scriptures OSHB scheme ({@code HC/Td/Ncbsa}, {@code Vqp3ms}), where "/" separates prefixes
 * from the main word. Codes that are not recognised are returned unchanged rather than guessed.
 */
public final class GrammarDescriber {

    private GrammarDescriber() {
    }

    /** Plain-English parsing of a Robinson-style Greek code, or an empty string when there is none. */
    public static String describeGreek(String code) {
        if (code == null || code.isEmpty()) {
            return "";
        }
        String simple = GREEK_INDECLINABLE.get(code);
        if (simple != null) {
            return simple;
        }
        String[] parts = code.split("-");
        String pos = GREEK_POS.get(parts[0]);
        if (pos == null || parts.length < 2) {
            return code;
        }
        List<String> out = new ArrayList<>();
        out.add(pos);
        if (parts[0].equals("V")) {
            String tvm = parts[1];
            // Second-aorist and second-perfect forms carry a leading "2" (for example "2ADI" in egeneto).
            if (tvm.length() == 4 && tvm.charAt(0) == '2') {
                tvm = tvm.substring(1);
                addOnce(out, "second");
            }
            if (tvm.length() == 3) {
                addOnce(out, GREEK_TENSE.get(tvm.substring(0, 1)));
                addOnce(out, GREEK_VOICE.get(tvm.substring(1, 2)));
                addOnce(out, GREEK_MOOD.get(tvm.substring(2, 3)));
            }
            if (parts.length > 2) {
                String tail = parts[2];
                if (tail.length() == 2 && Character.isDigit(tail.charAt(0))) {
                    out.add(tail.charAt(0) + ordinal(tail.charAt(0)) + " person · "
                            + (tail.charAt(1) == 'S' ? "singular" : "plural"));
                } else {
                    addCaseGenderNumber(out, tail);
                }
            }
        } else if (parts.length > 1 && parts[1].length() == 3) {
            addCaseGenderNumber(out, parts[1]);
        }
        return join(out);
    }

    /** Plain-English parsing of an OSHB Hebrew code (prefix segments separated by "/"). */
    public static String describeHebrew(String code) {
        if (code == null || code.isEmpty()) {
            return "";
        }
        List<String> segments = new ArrayList<>();
        for (String segment : code.split("/")) {
            String described = describeHebrewSegment(segment);
            if (described == null) {
                return code;
            }
            segments.add(described);
        }
        return join(segments);
    }

    private static String describeHebrewSegment(String raw) {
        if (raw.isEmpty()) {
            return null;
        }
        String seg = raw.startsWith("H") ? raw.substring(1) : raw;
        if (seg.isEmpty()) {
            return null;
        }
        char tag = seg.charAt(0);
        String rest = seg.substring(1);
        switch (tag) {
            case 'T':
                return rest.isEmpty() ? "particle" : HEB_PARTICLE.getOrDefault(rest.charAt(0), "particle");
            case 'R':
                return "preposition";
            case 'C':
                return "conjunction";
            case 'D':
                return "adverb";
            case 'I':
                return "interjection";
            case 'N':
                return describeHebrewNoun(rest);
            case 'A':
                return describeHebrewAdjective(rest);
            case 'P':
                return describeHebrewPronoun(rest);
            case 'S':
                return "pronominal suffix" + suffixPerson(rest);
            case 'V':
                return describeHebrewVerb(rest);
            default:
                return null;
        }
    }

    private static String describeHebrewNoun(String rest) {
        if (rest.length() < 1) {
            return null;
        }
        String kind = rest.charAt(0) == 'p' ? "proper noun" : "noun";
        return kind + genderNumberState(rest.substring(1));
    }

    private static String describeHebrewAdjective(String rest) {
        return "adjective" + genderNumberState(rest.length() > 1 ? rest.substring(1) : "");
    }

    private static String describeHebrewPronoun(String rest) {
        StringBuilder text = new StringBuilder("pronoun");
        for (char c : rest.toCharArray()) {
            if (Character.isDigit(c)) {
                text.append(", ").append(c).append(ordinal(c)).append(" person");
            } else if (HEB_GENDER.containsKey(c)) {
                text.append(", ").append(HEB_GENDER.get(c));
            } else if (HEB_NUMBER.containsKey(c)) {
                text.append(", ").append(HEB_NUMBER.get(c));
            }
        }
        return text.toString();
    }

    private static String describeHebrewVerb(String rest) {
        if (rest.length() < 2) {
            return "verb";
        }
        String stem = HEB_STEM.getOrDefault(rest.charAt(0), "other stem");
        String form = HEB_CONJUGATION.getOrDefault(rest.charAt(1), "");
        StringBuilder text = new StringBuilder("verb · ").append(stem);
        if (!form.isEmpty()) {
            text.append(" · ").append(form);
        }
        String tail = rest.length() > 2 ? rest.substring(2) : "";
        for (char c : tail.toCharArray()) {
            if (Character.isDigit(c)) {
                text.append(" · ").append(c).append(ordinal(c)).append(" person");
            } else if (HEB_GENDER.containsKey(c)) {
                text.append(" · ").append(HEB_GENDER.get(c));
            } else if (HEB_NUMBER.containsKey(c)) {
                text.append(" · ").append(HEB_NUMBER.get(c));
            }
        }
        return text.toString();
    }

    private static String genderNumberState(String letters) {
        StringBuilder text = new StringBuilder();
        if (letters.length() > 0 && HEB_GENDER.containsKey(letters.charAt(0))) {
            text.append(" · ").append(HEB_GENDER.get(letters.charAt(0)));
        }
        if (letters.length() > 1 && HEB_NUMBER.containsKey(letters.charAt(1))) {
            text.append(" · ").append(HEB_NUMBER.get(letters.charAt(1)));
        }
        if (letters.length() > 2 && HEB_STATE.containsKey(letters.charAt(2))) {
            text.append(" · ").append(HEB_STATE.get(letters.charAt(2)));
        }
        return text.toString();
    }

    private static String suffixPerson(String rest) {
        if (rest.isEmpty()) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (char c : rest.toCharArray()) {
            if (Character.isDigit(c)) {
                text.append(" · ").append(c).append(ordinal(c)).append(" person");
            } else if (HEB_GENDER.containsKey(c)) {
                text.append(" · ").append(HEB_GENDER.get(c));
            } else if (HEB_NUMBER.containsKey(c)) {
                text.append(" · ").append(HEB_NUMBER.get(c));
            }
        }
        return text.toString();
    }

    private static void addCaseGenderNumber(List<String> out, String cgn) {
        if (cgn.length() < 3) {
            return;
        }
        addOnce(out, GREEK_CASE.get(cgn.substring(0, 1)));
        addOnce(out, GREEK_NUMBER.get(cgn.substring(1, 2)));
        addOnce(out, GREEK_GENDER.get(cgn.substring(2, 3)));
    }

    private static void addOnce(List<String> out, String value) {
        if (value != null && !value.isEmpty()) {
            out.add(value);
        }
    }

    private static String join(List<String> parts) {
        return String.join(" · ", parts);
    }

    private static String ordinal(char digit) {
        return digit == '1' ? "st" : digit == '2' ? "nd" : digit == '3' ? "rd" : "th";
    }

    private static final Map<String, String> GREEK_INDECLINABLE = new HashMap<>();
    private static final Map<String, String> GREEK_POS = new HashMap<>();
    private static final Map<String, String> GREEK_TENSE = new HashMap<>();
    private static final Map<String, String> GREEK_VOICE = new HashMap<>();
    private static final Map<String, String> GREEK_MOOD = new HashMap<>();
    private static final Map<String, String> GREEK_CASE = new HashMap<>();
    private static final Map<String, String> GREEK_NUMBER = new HashMap<>();
    private static final Map<String, String> GREEK_GENDER = new HashMap<>();
    private static final Map<Character, String> HEB_PARTICLE = new HashMap<>();
    private static final Map<Character, String> HEB_GENDER = new HashMap<>();
    private static final Map<Character, String> HEB_NUMBER = new HashMap<>();
    private static final Map<Character, String> HEB_STATE = new HashMap<>();
    private static final Map<Character, String> HEB_STEM = new HashMap<>();
    private static final Map<Character, String> HEB_CONJUGATION = new HashMap<>();

    static {
        GREEK_INDECLINABLE.put("CONJ", "conjunction");
        GREEK_INDECLINABLE.put("PREP", "preposition");
        GREEK_INDECLINABLE.put("ADV", "adverb");
        GREEK_INDECLINABLE.put("PRT", "particle");
        GREEK_INDECLINABLE.put("INJ", "interjection");
        GREEK_INDECLINABLE.put("ADJ", "adjective");
        GREEK_INDECLINABLE.put("COND", "conditional particle");
        GREEK_INDECLINABLE.put("ARAM", "Aramaic word");
        GREEK_INDECLINABLE.put("HEB", "Hebrew word");
        GREEK_INDECLINABLE.put("NUI", "indeclinable number");
        GREEK_POS.put("N", "noun");
        GREEK_POS.put("V", "verb");
        GREEK_POS.put("A", "adjective");
        GREEK_POS.put("T", "article");
        GREEK_POS.put("P", "personal pronoun");
        GREEK_POS.put("R", "relative pronoun");
        GREEK_POS.put("D", "demonstrative pronoun");
        GREEK_POS.put("I", "interrogative pronoun");
        GREEK_POS.put("X", "indefinite pronoun");
        GREEK_POS.put("S", "possessive pronoun");
        GREEK_POS.put("F", "reflexive pronoun");
        GREEK_POS.put("K", "correlative pronoun");
        GREEK_POS.put("C", "reciprocal pronoun");
        GREEK_TENSE.put("P", "present");
        GREEK_TENSE.put("I", "imperfect");
        GREEK_TENSE.put("F", "future");
        GREEK_TENSE.put("A", "aorist");
        GREEK_TENSE.put("X", "aorist");
        GREEK_TENSE.put("R", "perfect");
        GREEK_TENSE.put("Y", "pluperfect");
        GREEK_TENSE.put("L", "pluperfect");
        GREEK_VOICE.put("A", "active");
        GREEK_VOICE.put("M", "middle");
        GREEK_VOICE.put("P", "passive");
        GREEK_VOICE.put("E", "middle or passive");
        GREEK_VOICE.put("D", "middle");
        GREEK_MOOD.put("I", "indicative");
        GREEK_MOOD.put("S", "subjunctive");
        GREEK_MOOD.put("O", "optative");
        GREEK_MOOD.put("M", "imperative");
        GREEK_MOOD.put("D", "imperative");
        GREEK_MOOD.put("N", "infinitive");
        GREEK_MOOD.put("P", "participle");
        GREEK_CASE.put("N", "nominative");
        GREEK_CASE.put("G", "genitive");
        GREEK_CASE.put("D", "dative");
        GREEK_CASE.put("A", "accusative");
        GREEK_CASE.put("V", "vocative");
        GREEK_NUMBER.put("S", "singular");
        GREEK_NUMBER.put("P", "plural");
        GREEK_GENDER.put("M", "masculine");
        GREEK_GENDER.put("F", "feminine");
        GREEK_GENDER.put("N", "neuter");
        HEB_PARTICLE.put('d', "definite article");
        HEB_PARTICLE.put('o', "direct object marker");
        HEB_PARTICLE.put('i', "interrogative particle");
        HEB_PARTICLE.put('n', "negative particle");
        HEB_PARTICLE.put('c', "comparative particle");
        HEB_PARTICLE.put('e', "exhortation particle");
        HEB_PARTICLE.put('a', "affirmation particle");
        HEB_GENDER.put('m', "masculine");
        HEB_GENDER.put('f', "feminine");
        HEB_GENDER.put('c', "common gender");
        HEB_GENDER.put('b', "masculine or feminine");
        HEB_NUMBER.put('s', "singular");
        HEB_NUMBER.put('p', "plural");
        HEB_NUMBER.put('d', "dual");
        HEB_STATE.put('a', "absolute state");
        HEB_STATE.put('c', "construct state");
        HEB_STATE.put('d', "determined state");
        HEB_STEM.put('q', "Qal");
        HEB_STEM.put('N', "Niphal");
        HEB_STEM.put('p', "Piel");
        HEB_STEM.put('P', "Pual");
        HEB_STEM.put('h', "Hiphil");
        HEB_STEM.put('H', "Hophal");
        HEB_STEM.put('t', "Hithpael");
        HEB_CONJUGATION.put('p', "perfect");
        HEB_CONJUGATION.put('q', "sequential perfect");
        HEB_CONJUGATION.put('i', "imperfect");
        HEB_CONJUGATION.put('w', "sequential imperfect");
        HEB_CONJUGATION.put('h', "cohortative");
        HEB_CONJUGATION.put('j', "jussive");
        HEB_CONJUGATION.put('v', "imperative");
        HEB_CONJUGATION.put('r', "active participle");
        HEB_CONJUGATION.put('s', "passive participle");
        HEB_CONJUGATION.put('a', "infinitive absolute");
        HEB_CONJUGATION.put('c', "infinitive construct");
    }
}
