package org.perazimchurch.app.ai.guide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic drift detection: notices when a question uses a concept from another faith, a heterodox group, or
 * a teaching outside Scripture, so the guide can answer gently from Scripture. Pure Java (no Android types) so the
 * rules run in host tests. Every concept id here must exist in assets/ai/guide/comparative/comparative-kb.json.
 *
 * A hit only adds evidence and a playbook; it never blocks a question. Crisis and safety checks run first.
 */
public final class DriftDetector {

    /** One concept the question touched, and the phrase that matched it. */
    public static final class Hit {
        public final String conceptId;
        public final String matched;

        Hit(String conceptId, String matched) {
            this.conceptId = conceptId;
            this.matched = matched;
        }
    }

    private static final String[][] RULES = {
            {"karma", "\\bkarm(?:a|ic)\\b"},
            {"reincarnation", "\\b(?:reincarnat\\w*|rebirth|transmigrat\\w*|samsara|past lives?|come back as (?:another|a new)|another life (?:after|on) earth)\\b"},
            {"nirvana_moksha", "\\b(?:nirvana|moksha|liberation from (?:the )?(?:cycle|wheel) of)\\b"},
            {"purgatory", "\\bpurgatory|purgatorial\\b"},
            {"saint_prayer", "\\b(?:pray|prays|praying|prayer|prayers|ask|asking|intercede)\\s+(?:to|through)\\s+(?:the\\s+)?(?:saints?|mary|the virgin)\\b|\\bpatron saints?\\b|\\bintercession of (?:the )?saints?\\b|\\bhail mary\\b|\\bsaints? (?:pray|intercede) for us\\b"},
            {"mary_mediator", "\\bco-?redeemer\\b|\\bmediatrix\\b|\\bqueen of heaven\\b|\\bmary (?:is|as) (?:our )?(?:mediator|co-?redeemer|queen)\\b"},
            {"icons_veneration", "\\b(?:venerat\\w*|kiss\\w*|bow\\w* to|pray before|prayer before) (?:the )?(?:icons?|statues?|images?)\\b|\\b(?:rosary|prayer beads)\\b"},
            {"soul_sleep", "\\bsoul[- ]sleep\\b|\\bsleep of the soul\\b|\\b(?:dead|souls?) (?:are )?(?:asleep|sleeping) (?:until|till)\\b|\\bunconscious (?:until|till|in the grave)\\b"},
            {"conditional_immortality", "\\bconditional immortality\\b|\\b(?:soul|souls|human soul) (?:is|are) (?:not|isn'?t) (?:inherently |naturally |by nature )?immortal\\b|\\bannihilation(?:ism|ist)?\\b"},
            {"all_paths", "\\ball (?:paths|roads|religions|faiths|beliefs) (?:lead|go|point)\\b|\\bdifferent (?:paths|roads) to (?:the same )?god\\b|\\bone god by (?:many|different) names\\b|\\bevery religion (?:leads|is a path)\\b"},
            {"divine_self", "\\b(?:i|we|you|everyone|people|humans?) (?:am|are|is|can be|can become) (?:one with|part of|the same as|identical to) (?:god|the divine|brahman|the universe)\\b|\\batman\\b|\\bbrahman\\b|\\bdivine spark\\b|\\b(?:we|you|i) (?:are|am) god\\b|\\bbecome (?:one with|a) god\\b|\\bgod within (?:you|us|every)\\b"},
            {"eternal_progression", "\\beternal progression\\b|\\bbecome (?:like|as) gods?\\b|\\bas man (?:now )?is,? god (?:once )?was\\b|\\bexaltation (?:to|into) (?:godhood|godhead|the celestial)\\b|\\bcelestial (?:kingdom|glory)\\b"},
            {"works_salvation", "\\b(?:earn|merit|deserve|buy|work for|work my way into|work your way into)\\s+(?:my |your |our |their )?(?:salvation|heaven|eternal life|god'?s favou?r)\\b|\\bafter all we can do\\b|\\bgood works? (?:earn|get|buy|secure)\\b|\\bbalance the scales\\b|\\bweighed (?:in the )?balance\\b"},
            {"ancestors", "\\b(?:ancestors?|ancestral|mizimu)\\b.{0,40}\\b(?:pray|worship|venerat\\w*|appease|sacrific\\w*|offer\\w*|spirits?|protect\\w*|guide\\w*|intercede|call on|speak to)\\b|\\b(?:pray|worship|venerat\\w*|appease|sacrific\\w*|offer\\w*|call on|speak to)\\b.{0,40}\\b(?:ancestors?|ancestral spirits?|mizimu|the dead)\\b"},
            {"trinity_denial", "\\b(?:trinity|triune god)\\b.{0,40}\\b(?:false|wrong|pagan|invented|made up|unbiblical|not in the bible|false doctrine|a lie)\\b|\\b(?:oneness|jesus only|modalis[mt])\\b|\\bthree gods\\b|\\bjesus (?:is|was) (?:the )?father\\b|\\bone person and three\\b"},
            {"jesus_created", "\\bjesus (?:was|is) (?:a )?(?:created|first creation|created being)\\b|\\bjesus\\b.{0,25}\\bmichael\\b.{0,15}\\b(?:archangel|angel|created)\\b"},
            {"holy_spirit_force", "\\bholy spirit (?:is|was) (?:just |only |merely )?(?:a |an )?(?:force|energy|power|impersonal)\\b|\\bimpersonal (?:holy )?spirit\\b|\\bactive force\\b|\\bholy spirit (?:is|was) (?:not|isn'?t) (?:a )?(?:person|god)\\b"},
            {"baptism_for_dead", "\\bbaptis[mz]s? (?:for|on behalf of) (?:the )?dead\\b|\\bproxy baptism\\b"},
            {"saturday_sabbath", "\\b(?:saturday|seventh[- ]day) sabbath\\b|\\bsabbath (?:is|on|must be) (?:on )?saturday\\b|\\bmust keep (?:the )?(?:saturday|sabbath on saturday)\\b|\\binvestigative judgment\\b|\\bsunday (?:is|was) (?:the )?mark\\b"},
            {"new_age_energy", "\\blaw of attraction\\b|\\buniversal (?:energy|consciousness|mind)\\b|\\bcosmic (?:energy|consciousness)\\b|\\bchakras?\\b|\\bhigher self\\b|\\bastral (?:travel|projection)\\b|\\bmanifest (?:your|my) (?:reality|desires|destiny)\\b|\\bvibrations? of the universe\\b|\\bcrystal healing\\b|\\benergy (?:healing|field)\\b"},
            {"christian_science", "\\bchristian science\\b|\\bscience and health\\b|\\bmatter is (?:an )?illusion\\b|\\bsickness is (?:an )?(?:illusion|error)\\b|\\bmind over matter\\b|\\bmortal mind\\b"},
            {"secular_worldview", "\\bthere (?:is |exists )?no god\\b|\\bno god exists\\b|\\bscience (?:has )?(?:proved|proven|disproved|disproves) (?:god|there is no god)\\b|\\bmorals? (?:are|is) (?:just )?(?:relative|subjective|social constructs?)\\b|\\bno absolute truth\\b|\\breligion is (?:just|only) (?:a )?(?:myth|fairy tale|psychology|a crutch)\\b"},
            {"islam_no_crucifixion", "\\bjesus (?:was not|wasn'?t|did not die|didn'?t die) (?:on the cross|crucified|killed|die)\\b|\\bsomeone else (?:was crucified|was killed|died|was made to look like|took (?:his|jesus'?s?) place)\\b|\\b(?:did|didn'?t|was) (?:someone|somebody) else (?:die|dying|crucified)\\b|\\bjesus was (?:not|never) (?:crucified|killed)\\b"},
            {"islam_son_denial", "\\bjesus (?:is|was) (?:not|never) (?:the )?son of god\\b|\\bgod (?:has )?no (?:son|children)\\b|\\bjesus (?:was|is) (?:only )?(?:a )?(?:prophet|messenger) only\\b|\\bjesus (?:is )?(?:not|isn'?t) god\\b"},
            {"canon_books", "\\b(?:more|extra|additional) books\\b.{0,40}\\bbible\\b|\\bdeuterocanon\\w*|\\bapocrypha\\w*|\\btobit\\b|\\bjudith\\b|\\bmaccabees?\\b|\\bsirach\\b|\\becclesiasticus\\b|\\bbaruch\\b|\\bwisdom of solomon\\b|\\bbible canon\\b|\\bcatholic bible\\b|\\borthodox bible\\b"},
            {"church_identity_question", "\\b(?:catholic|catholics|roman catholic|eastern orthodox|orthodox church|orthodox christians?)\\b.{0,30}\\b(?:church|christian|christians|saved|wrong|right|true|false|heresy|cult|pope|going to heaven|heaven)\\b"},
            {"group_question", "\\b(?:mormons?|mormonism|latter[- ]day saints?|lds|jehovah'?s witness(?:es)?|seventh[- ]day adventists?|sda|oneness pentecostals?|christian scientists?|christadelphians?)\\b.{0,40}\\b(?:cult|cults|heresy|heretical|false|true|saved|real christians?|christian|wrong|right|believe|teach|taught)\\b"}
    };

    private static final List<String> CONCEPT_IDS;
    private static final Pattern[] PATTERNS;

    static {
        List<String> ids = new ArrayList<>();
        PATTERNS = new Pattern[RULES.length];
        for (int i = 0; i < RULES.length; i++) {
            ids.add(RULES[i][0]);
            PATTERNS[i] = Pattern.compile(RULES[i][1], Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        }
        CONCEPT_IDS = Collections.unmodifiableList(ids);
    }

    private DriftDetector() {}

    /** Every concept id this detector can report. The comparative knowledge base must contain each one. */
    public static List<String> conceptIds() {
        return CONCEPT_IDS;
    }

    /**
     * Returns the concepts the question touches, in rule order, each at most once. Empty when nothing drifts.
     * Curly apostrophes are folded to straight ones and whitespace is collapsed before matching.
     */
    public static List<Hit> detect(String question) {
        List<Hit> hits = new ArrayList<>();
        if (question == null || question.trim().isEmpty()) return hits;
        String text = question.replace('’', '\'').replace('‘', '\'').replaceAll("\\s+", " ").trim()
                .toLowerCase(Locale.ROOT);
        for (int i = 0; i < PATTERNS.length; i++) {
            Matcher m = PATTERNS[i].matcher(text);
            if (m.find()) hits.add(new Hit(RULES[i][0], m.group()));
        }
        return hits;
    }

    /** True when the question touches at least one drift concept. */
    public static boolean drifts(String question) {
        return !detect(question).isEmpty();
    }
}
