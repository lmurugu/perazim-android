package org.perazimchurch.app.ai.library;

import androidx.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * NO-GO ZONES: categories that still fail their gate after a full evaluation round. In a no-go zone the guide gives
 * one fixed, warm decline that names a trusted church leader, and the model never answers. A zone is lifted only by
 * a rerun that passes its gate. Safety and privacy are never listed here.
 *
 * Declared 2026-11-25 after the grounded-answer rubric review (ai-eval/runs/2026-11-25-grounded). Zones cover every
 * question in that review. Zones are matched after the safety, privacy, approved-doctrine
 * and verbatim-verse routes, so those answers are unaffected. Patterns are lowercase regular expressions.
 */
public final class NoGoZones {

    /** One declared zone: a stable id, the question shapes it covers, and the fixed decline. */
    public static final class Zone {
        public final String id;
        public final Pattern pattern;
        public final String decline;

        Zone(String id, String pattern, String decline) {
            this.id = id;
            this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            this.decline = decline;
        }
    }

    /** The one fixed decline used in every zone. It names a trusted church leader and never answers the question. */
    static final String DECLINE =
            "I'm sorry, I can't answer this one well yet, so I won't guess. Please talk it through with a trusted "
            + "church leader or your pastor, who can walk with you through it.";

    /** Other faiths and groups: no general description. Ask which specific teaching is meant (Levi, 2026-11-25). */
    static final String OTHER_GROUP_DECLINE =
            "I can't verify a general claim like that. I am designed specifically to assist with Christian faith and the Bible, so I am not authorised to compare religions in general. "
            + "Christians hold different views on some questions like this, and the guide does not judge any person's standing before God. "
            + "I can examine a specific teaching of a church or group against Scripture, so which teaching do you have in mind? "
            + "Please talk it through with a trusted church leader or your pastor, who can help you test it by the Bible.";

    /** No verdict on any individual: only God knows hearts and final destiny. Warm, with the way of faith for the asker. */
    /** True when the question asks for a verdict on an individual's eternal state. */
    public static boolean matchesDestinyZone(@Nullable String query) {
        if (query == null) return false;
        for (Zone z : ZONES) {
            if ("individual-destiny".equals(z.id)) return z.pattern.matcher(query).find();
        }
        return false;
    }

    public static final String DESTINY_DECLINE = "Only God knows people's hearts and their final destiny, so I will not pronounce on anyone's eternal state. "
            + "I am not authorised to pronounce on anyone's salvation. "
            + "If you are asking about your own salvation, come to Jesus in faith and trust Him as Lord (Romans 10:9–10). "
            + "Please talk this through with a trusted church leader or your pastor, who can walk with you through it.";

    /** Round 23: church authority and human decisions. The guide never decides these; an authorised human leader does. */
    static final String AUTHORITY_DECLINE =
            "This needs an authorised human leader, such as the Bishop or an elder, so I cannot make this decision for the church. "
            + "Please contact the church office and ask to speak with the Bishop or an elder. You can also talk it through with a trusted "
            + "church leader or your pastor, who can walk with you through it.";

    /** Round 23: tasks outside the guide's scope (documents, calculations, browsing, role-play as a church leader). */
    static final String SCOPE_DECLINE =
            "I am designed specifically to assist with matters relating to Perazim Mission Church (PMCI), Christian faith and religious "
            + "matters, so I am not authorised to advise on this matter. Please speak with a trusted church leader or your pastor, "
            + "or another appropriate person.";

    /** Round 23: live research, links and news. The guide cannot check these, so it says so and does not guess. */
    public static final String UNCERTAIN_DECLINE =
            "I cannot browse links, search the internet or read live news, and I do not have access to them. I cannot verify that source "
            + "or any current research, so I will not guess or summarise it. The approved sources in this app do not establish that answer. "
            + "Please check the source yourself, or ask a trusted church leader or your pastor.";

    /** Reasoning and argument: no approved route yet. Points to Scripture and does not guess (Levi's rules). */
    static final String REASONING_DECLINE =
            "I want to point you to what Scripture teaches. I can't weigh this "
            + "argument well yet, so I won't guess. Please talk it through with a trusted church leader or your pastor, who can "
            + "take you through the Bible's answer.";

    /** Denominations and groups ranked against each other: no verdict, and no single church has the whole truth. */
    static final String DENOMINATION_DECLINE =
            "No single church or denomination has a monopoly on the truth. Christians of different traditions hold some different "
            + "views, and I won't rank churches or groups. Each teaching can be checked against Scripture with a trusted church leader "
            + "or your pastor.";

    private static final Pattern NICENE = Pattern.compile("\\bnicene\\b", Pattern.CASE_INSENSITIVE);

    private static final List<Zone> ZONES = Collections.unmodifiableList(Arrays.asList(
            new Zone("individual-destiny",
                    "(in hell|going to hell|go to hell|in heaven|saved|lost soul|right now).*(yes or no|is he|is she|is my|is it)"
                    + "|am i (really )?saved|was (he|she|they|my \\w+|\\w+) (ever )?saved|never believed|going to heaven|go to heaven|see my heart"
                    + "|(my|our) (father|mother|brother|sister|son|daughter|husband|wife|grandmother|grandfather|cousin|friend)"
                    + ".*(hell|heaven|saved|damned|lost)",
                    DESTINY_DECLINE),
            new Zone("denominational-verdict",
                    "only (one )?(denomination|church|group|religion)|only one .*(right|true)|(one|only) (true|right) (church|denomination)"
                    + "|which (denomination|church|group) is (right|true)|are all churches"
                    + "|(arminian|calvinis|presbyterian|baptist|methodist|pentecostal)s?.{0,30}(wrong|heretic|false|damned)",
                    DENOMINATION_DECLINE),
            new Zone("uncertain-live-research",
                    "browse this link|browse the (?:web|internet)|search the internet|latest news|recent (?:academic )?research"
                    + "|academic research|find recent|https?://",
                    UNCERTAIN_DECLINE),
            new Zone("out-of-scope-task",
                    "what god wants me to do tomorrow|exactly what god wants|pretend to be|pertend to be|act as (?:our |the )?(?:senior )?pastor"
                    + "|perform (?:some |the )?calculations|do (?:the |some )?calculations|bible study document|study document|word file"
                    + "|email it to me|summari[sz]e a (?:theological )?book|supplied by the user|changed its doctrine",
                    SCOPE_DECLINE),
            new Zone("human-decision-authority",
                    "above correction|above (?:being )?corrected|change my name|membership list|excommunicat|discipline the"
                    + "|bishop is right|elder who was removed|should this member|(?:question|questioning|doubt|challenge).{0,15}tradition",
                    AUTHORITY_DECLINE),
            new Zone("salvation-works-apostasy",
                    "lose (my |our )?salvation|once saved|eternal security|fall(ing|s)? away|falil|apostat|abandon (jesus|god)|sin on purpose|hebrews|baptism saves|saves us"
                    + "|depart from the faith|finally depart|earn (it|my salvation|salvation)|salvation by grace"
                    + "|grace.*obey|obey god|chosen (those|who|ones)|already chosen|predestin|too late|reject god"
                    + "|birthright|denied jesus|genuinely saved|saved christian|work for my salvation|work out your salvation",
                    DECLINE),
            new Zone("spirit-gifts-miracles",
                    "holy spirit|holy spiirt|spirit (in|baptism|baptised|baptized|told)|tongues|spiritual gift"
                    + "|prophe|deliverance|demon|dream|holy water|miracle|healing|healed|paralys|tiktok|youtube preacher"
                    + "|anoint|speak in",
                    DECLINE),
            new Zone("other-faiths-and-groups",
                    "jehovah|witness|latter.day|mormon|adventist|sabbath|saturday|sunday|seventh|ellen white|mark of the beast"
                    + "|catholic|purgatory|\\bmary\\b|miriam|apocrypha|orthodox|icon|karma|reincarnat|rebirth|next life"
                    + "|come back as|ancestor|shrine|kiapo|traditional (rite|dance|oath)|islam|allah|muslim|qur|hindu|buddh|jain"
                    + "|soul sleep|soul|denomination|church of christ|cult|archangel|michael|pluralism|same thing"
                    + "|all religions|going to hell|is she going|is he going|hell right now"
                    + "|all paths|all roads|law of attraction|universal energy|higher self|new age|brahman|saints|ancestors|soul sleep"
                    + "|saturday|sabbath|one way to god",
                    OTHER_GROUP_DECLINE),
            new Zone("history-authorship-research",
                    "resurrection|historical|research|academic|papers|earliest|early (church|christian)|prove|proof"
                    + "|who wrote|did paul write|write hebrews|authorship|canon|nicaea|trinity before|before the council|eraly|was god"
                    + "|crucif|on the cross|died on|not die|did not die|was not crucified",
                    DECLINE),
            new Zone("verse-recall-and-misquote",
                    "quote (the )?verse|read me the verse|verse (that|says|about|where)|the verse|exact (wording|words)"
                    + "|misquote|didn.t jesus say|did jesus say|said .|says .|the bible says|chapter and verse"
                    + "|arabic|in genesis|where in the bible|which book|bible verse|verse for|god helps|helps those"
                    + "|money is the source|paul said|paul did",
                    DECLINE),
            new Zone("scripture-interpretation",
                    "pharaoh|harden|isaiah|original hearers|what did paul mean|mean in|meant by|interpret|explain the passage",
                    DECLINE),
            new Zone("reasoning-and-argument",
                    "argument|evaluate|someone claims|a person says|says christianity is false|definition of faith"
                    + "|without evidence|reasoning|you said|you told me|yesterday you|earlier that",
                    REASONING_DECLINE),
            new Zone("resource-lookup",
                    "what does .* say about|according to|commentary|dictionary|matthew henry|hitchcock|smith.s"
                    + "|creed",
                    DECLINE),
            new Zone("betting-and-gambling",
                    "betting|gambl|\\bbet\\b|casino|sports betting",
                    "I'm glad you asked, and I can see this matters to you. I can't give a reliable answer on this one, "
                    + "so I won't guess. Please talk it through with a trusted church leader or your pastor, who knows you "
                    + "and can help you weigh it with Scripture."),
            new Zone("mental-health",
                    "antidepress|depress|anxiety|mental health|mental illness|psychiatr|therapy|stigma",
                    "I'm sorry you're carrying this, and it takes courage to ask. I can't answer the medical or spiritual "
                    + "side reliably, so I won't guess. Please speak with a doctor or nurse about your medicine, and with a "
                    + "trusted church leader or your pastor, who can pray with you. If your thoughts ever turn to ending "
                    + "your life, reach out to someone right away."),
            new Zone("leaving-the-faith",
                    "leave the faith|leave christianity|leave the church|leave my (faith|church)|stop believing"
                    + "|leaving the faith|doubt my faith|question my faith|lost my passion|passion for christianity",
                    "Thank you for trusting me with something this honest. I can't answer this one well, so I won't guess "
                    + "about your faith. Please talk with a trusted church leader or your pastor soon. You don't have to work "
                    + "it out alone, and your questions are welcome there. If your thoughts ever turn to ending your life, "
                    + "reach out to someone right away."),
            new Zone("grief-and-loss",
                    "grief|grieving|passed away|funeral|death of|someone died|my (mother|father|husband|wife|child|son"
                    + "|daughter|brother|sister|friend) (died|passed)|since my (mother|father|husband|wife|child|son"
                    + "|daughter|brother|sister|friend)",
                    "I'm so sorry about your loss. I can't answer this one well, so I won't guess, but you don't have to "
                    + "carry it alone. Please talk with a trusted church leader or your pastor, who can sit with you and pray. "
                    + "If the grief becomes too heavy, reach out to someone you trust straight away."),
            new Zone("pastoral-personal",
                    "\\bi feel|\\bi am\\b|\\bi.m\\b|\\bi keep|\\bi can.t|\\bi.ve been|\\bi have been|\\bmy (brother|mother|father"
                    + "|church|cell|pastor|grandmother|husband|wife|job|money|family|employer|uncles|cousin)"
                    + "|alone|lonely|failing|failure|hopeless|angry|afraid|scared|worried|anxious|grief|jealous|guilt"
                    + "|struggl|depress|stigma|antidepress|\\bhiv\\b|cancer|no food|lost my|forgive|sad|shame|ashamed"
                    + "|drinking|weekends|betting|gambl|condom",
                    DECLINE),
            new Zone("giving-money-pressure",
                    "seed|shilling|tithe|tithing|offering|prosperity|wealth|rich|blessing|blessed|bless me|prayer cloth"
                    + "|\\bpay\\b|paid|money|loan|\\bbet|sell my|bank account|ministry|send money|breakthrough|\\bjob\\b"
                    + "|giving|\\bgive (?:me )?(?:money|cash|shillings|a seed|an offering|tithe|to the church)\\b",
                    DECLINE),
            new Zone("scope-and-generation",
                    "write me|write a|\\bcv\\b|resume|poem|joke|funny|recipe|sermon|essay|song|story|politic|vote"
                    + "|election|party|moon|campus|official statement|statement on|how do i pray|translate|summar"
                    + "|land|wedding|football|sport",
                    DECLINE),
            new Zone("trick-premise-and-claims",
                    "flat|sun stands|stand still|judas|saved after|was he an apostate|is jesus the father|jesus the father"
                    + "|just a prophet|god.s son|science has|no god|relative|sowing|reaping|sunday.*mark|mark of"
                    + "|archangel|jesus is god",
                    DECLINE)
    ));

    private NoGoZones() {}

    /** The zone that covers this question, or null when the question is not in a no-go zone. */
    public static Zone match(String question) {
        if (question == null) return null;
        String normalized = question.toLowerCase(Locale.ROOT);
        for (Zone z : ZONES) {
            // The approved Nicene Creed is answered verbatim from its approved text, so it is not zoned.
            if (z.id.equals("resource-lookup") && NICENE.matcher(normalized).find()) continue;
            if (z.pattern.matcher(normalized).find()) return z;
        }
        return null;
    }

    /** Every declared zone, for the report and for tests. */
    public static List<Zone> all() {
        return ZONES;
    }
}
