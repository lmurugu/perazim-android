package org.perazimchurch.app.ai.library;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Approved teaching routes written by Levi's decisions of 2026-11-25. Each route answers only the question shapes it
 * names, quotes its anchor passages word for word from the bundled KJV, and labels Levi's developing views as
 * UNDER REVIEW. A route fails closed: if any anchor verse is missing from the bundled text, no answer is given and the
 * question moves on to the next route, which declines in the normal way.
 *
 * Routes run before the doctrine library and before the no-go zones. They never judge an individual's destiny.
 */
public final class ApprovedRoutes {

    /** An answer from a route: the route id (for the evaluation status) and the member-facing text. */
    public static final class Answer {
        public final String routeId;
        public final String text;

        Answer(String routeId, String text) {
            this.routeId = routeId;
            this.text = text;
        }
    }

    private static final class Route {
        final String id;
        final Pattern match;
        final String body;
        final String[] quotes;

        Route(String id, String pattern, String body, String... quotes) {
            this.id = id;
            this.match = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            this.body = body;
            this.quotes = quotes;
        }
    }

    private static final String NO_VERDICT = "We do not judge any person's eternal destiny.";

    private static final List<Route> ROUTES;

    static {
        List<Route> r = new ArrayList<>();

        r.add(new Route("israel-end-times",
                "israel|isreal|millenni|thousand years|antichrist|anti-christ|rapture|tribulation|second coming|end times"
                        + "|reign on earth|pre-?trib|premillenni|national israel|romans 11|replac(?:e|ed|ing) (?:the )?(?:church|israel)",
                "Here is our working position on Israel and the end times. It is under review. It is not a settled "
                + "Perazim doctrine, and we do not say every Christian must agree with it.\n\n"
                + "Jewish and Gentile believers are one New Covenant people in Christ (Ephesians 2:13–16). Jewish believers "
                + "are part of the Church. Gentile believers are grafted into the blessings of God's promises (Romans 11:17–18), "
                + "but they do not become ethnic Israelites. The Church has not replaced national Israel.\n\n"
                + "Our working position is pre-tribulation and premillennial, and it is under review. We understand that Christ "
                + "will come for His Church before the tribulation (1 Thessalonians 4:13–18). Revelation 3:10 was addressed to "
                + "the church in Philadelphia. Applying it to Christ's protection of His Church in the future tribulation is "
                + "our working interpretation, supported by the other passages. The phrase \"seven-year tribulation\" is not in "
                + "the text. Reading Daniel's final week (Daniel 9:24–27) as a future seven years is our working interpretation.\n\n"
                + "We understand that Christ will reign on earth for a thousand years (Revelation 20:1–6). Other passages help "
                + "us understand that reign: Zechariah 14:4 and 14:9, Isaiah 11:1–10, and Daniel 2:34–35 and 2:44–45, where the "
                + "stone becomes a great mountain and fills the earth. Connecting these with Christ's future physical reign on "
                + "earth is our interpretation.\n\n"
                + "We understand that national Israel will turn to Jesus near the end of the tribulation (Romans 11:25–27; "
                + "Zechariah 12:10). We connect this with Daniel 9:27, Daniel 12:1 and Matthew 24:15–22. This sequence is our "
                + "interpretation. We do not set dates, and we do not identify any present-day person or nation as fulfilling these passages.\n\n"
                + "We understand the antichrist to be a final individual who opposes Christ and deceives people (2 Thessalonians "
                + "2:3–4; Daniel 7:8, 25). Revelation 13:1–8 describes the beast's blasphemy and war against the saints, and "
                + "Revelation 13:11–14 describes its deception. We do not identify anyone today, and we do not guess.\n\n"
                + "The land was given and was possessed (Joshua 21:43), and God's covenant promises to Israel have not been "
                + "cancelled (Romans 11:29). This is a covenant position, not a political endorsement. It does not mean that every "
                + "action of any modern government is approved by God.\n\n"
                + "No one knows the day or the hour (Matthew 24:36). Stay close to Scripture, and talk with your pastor.",
                "Romans 11:25", "Revelation 3:10"));

        r.add(new Route("cessationism",
                "cessation|cessationist|cessationism|gifts (?:have |had |of the spirit )?(?:ceased|stopped|ended)|ceased with the apostles|sign gifts",
                "Some cessationists teach that certain sign gifts, such as prophecy, healing and tongues, ceased with the "
                + "apostolic age. Not every cessationist explains this in exactly the same way. This is a disagreement within "
                + "Protestant Christianity.\n\n"
                + "Our position is that the gifts of the Holy Spirit continue to operate in the Church today. The Spirit gives "
                + "gifts to each believer as He wills (1 Corinthians 12:7–11).\n\n"
                + "We weigh the view against these passages: Acts 2:38–39 (the promise is for us and our children) and Acts 2:17–18 "
                + "(the last days, when God pours out His Spirit); 1 Corinthians 14:1 (desire spiritual gifts, especially that you "
                + "may prophesy); Ephesians 4:11–13 (gifts given until the body is built up); and 1 Thessalonians 5:19–21 (do not "
                + "quench the Spirit, and test everything).\n\n"
                + "Cessationists often read 1 Corinthians 13:8–10 as saying the gifts end when \"that which is perfect\" comes. "
                + "Our reading, working interpretation and under review, is that this refers to the Lord's return and to full "
                + "knowledge. We do not claim that the passage explicitly names the time as Christ's return, and we do not claim "
                + "that this is the only possible reading.\n\n"
                + "We do not call cessationists heretics, false Christians or unsaved because they disagree. Test every word and "
                + "experience by Scripture, desire the gifts that build up the Church, and seek love above all.",
                "1 Corinthians 14:1", "Acts 2:39"));

        r.add(new Route("apostasy-assurance",
                "lose (?:my |our |their |his |her |your )?salvation|once saved|eternal security|apostas|apostate|fall(?:ing|s)? away"
                        + "|finally depart|depart from (?:christ|the faith)|backslid|assurance of salvation|can a (?:christian|believer) (?:lose|fall)"
                        + "|abandon (?:jesus|christ|god|him)|genuinely saved|unsaved|never saved|was i saved|am i (?:really )?saved|keep sinning"
                        + "|turn (?:away|back) from (?:christ|jesus|god)|reject (?:god|christ|jesus)|birthright",
                "We hold the assurance that believers have in Christ together with the seriousness of the warnings in Scripture. "
                + "The view below is our developing view, under review. It is not a settled Perazim doctrine.\n\n"
                + "Assurance: no outside power can snatch Christ's sheep out of His hand (John 10:27–29). Nothing in creation can "
                + "separate believers from the love of God in Christ (Romans 8:38–39). These promises give security to those who "
                + "trust Him. They do not deny that a person can turn away by their own deliberate choice.\n\n"
                + "Warnings: Scripture also warns that a person can turn back from Christ. Hebrews 6:4–6, Hebrews 10:26–29 and "
                + "2 Peter 2:20–22 are serious warnings against turning back from Christ after knowing the truth.\n\n"
                + "Our developing view, under review: a genuinely saved person can freely and finally abandon Christ. This is not "
                + "a settled formula that says \"once saved, always saved\", and we do not say that anyone who falls away was never "
                + "saved. " + NO_VERDICT + " If you are asking about your own salvation, come to Jesus in faith and trust Him as Lord "
                + "(Romans 10:9–10). Hold fast to Christ, keep close to the church, and talk with your pastor if you are struggling.",
                "John 10:28", "Hebrews 6:4"));

        r.add(new Route("unconditional-election",
                "unconditional election|predestin|elect of god|chosen before the foundation|already chosen|has chosen"
                        + "|chosen (?:those|who|ones|people|his people)|if god has chosen",
                "Our working interpretation, under review, is unconditional election. We understand that God chose His people in "
                + "Christ before the foundation of the world, and that His choosing does not depend on anything good in us "
                + "(Ephesians 1:4–5; Romans 9:11–16). This is not a fully settled Perazim doctrine.\n\n"
                + "God's choosing does not cancel our responsibility to preach the gospel to everyone. God uses the preaching of "
                + "the gospel as the means by which He calls His chosen people (Romans 10:13–15; Acts 13:48), and Christ calls all "
                + "to come (John 3:16). We trust God's choosing without guessing who is chosen. " + NO_VERDICT,
                "Ephesians 1:4", "Romans 10:14"));

        r.add(new Route("grace-and-obedience",
                "grace and (?:obedience|works)|obedience and grace|works and grace|faith and works|faith without works"
                        + "|saved by grace|salvation is by grace|by grace.{0,40}obey|grace.{0,40}\\bobey"
                        + "|work for (?:my |our )?salvation|earn (?:my |our )?salvation|after all we can do"
                        + "|why (?:should|must|do|did) (?:christians|we|believers|i) obey|obey god|grace (?:or|vs\\.?|versus) (?:works|obedience)",
                "Scripture says we are saved by grace, through faith, and not by our own works (Ephesians 2:8–9). We are created "
                + "in Christ for good works, which God prepared beforehand (Ephesians 2:10). Grace teaches us to deny ungodliness "
                + "and to live soberly and righteously (Titus 2:11–12). Faith without works is dead (James 2:17, 26).\n\n"
                + "So obedience is the fruit of grace, not the payment for it. Good works do not buy our salvation. They show that "
                + "God's grace is at work in us. Obey God out of gratitude, not to earn His love.",
                "Ephesians 2:8", "James 2:26"));

        r.add(new Route("sovereignty-and-responsibility",
                "sovereignty|free will|human responsibility|god.s sovereignty|sovereignty and responsibility|work out your salvation|fear and trembling",
                "Scripture teaches both God's sovereignty and human responsibility. God is sovereign over all things, and His "
                + "purposes stand (Isaiah 46:9–10; Proverbs 16:9). Every person is responsible before God for their own sin "
                + "(Ezekiel 18:20; Romans 2:6). Work out your salvation with fear and trembling, because God is working in you "
                + "(Philippians 2:12–13).\n\n"
                + "We do not try to resolve every question about how these fit together, and we do not force a full Calvinist or "
                + "Arminian system onto the answer. We do not use God's sovereignty to excuse sin. Take responsibility for your "
                + "choices before God, and trust Him with what you cannot understand.",
                "Ezekiel 18:20", "Philippians 2:12"));

        r.add(new Route("spiritual-claims-tested",
                "prophecy|prophesied|prophetic word|word from the lord|word from god|god told me|spirit told me|dream from god|vision from god",
                "Spiritual claims must be tested by Scripture. No dream, prophecy, vision or experience can overrule the Bible, "
                + "and no one's word about your future replaces what God has said in His Word. Test the spirits, and hold fast to "
                + "what is good (1 John 4:1; Deuteronomy 18:22; 1 Thessalonians 5:19–21).\n\n"
                + "We believe the gifts of the Holy Spirit continue to operate in the Church today, and the Spirit gives them as He "
                + "wills (1 Corinthians 12:7–11). Scripture also warns that the love of money is a root of evil (1 Timothy 6:10). "
                + "If someone gives you a word about money, your future or a big decision, weigh it by the Bible and talk with your "
                + "pastor before you act on it. " + NO_VERDICT,
                "1 John 4:1", "Deuteronomy 18:22"));

        r.add(new Route("christ-deity",
                "just a prophet|not god.s son|jesus is god|is jesus god|deity of christ|jesus (?:is|was) (?:only|just) a (?:prophet|man)",
                "Scripture teaches that Jesus is God the Son, not merely a prophet. The Word was God, and the Word was made flesh "
                + "(John 1:1, 14). Jesus said that He and the Father are one (John 10:30). The Nicene and Chalcedonian statements, "
                + "which we hold as established Christian orthodoxy, confess Jesus as the eternal Son of God, truly God and truly man.\n\n"
                + "We test every claim about Jesus against Scripture, and this is what the Bible teaches.",
                "John 1:1", "John 10:30"));

        r.add(new Route("first-gospel-preacher",
                "first (?:person|one|man|preacher).{0,40}(?:preach|gospel)|who (?:was )?the first .{0,30}(?:preach|gospel)",
                "Scripture records Jesus preaching the gospel of the kingdom of God as He began His public ministry (Mark 1:14–15). "
                + "Before that, an angel brought the shepherds good tidings of great joy (Luke 2:10). The Bible does not give anyone "
                + "the title \u201cthe first person to preach the gospel\u201d.",
                "Mark 1:14", "Luke 2:10"));

        r.add(new Route("nicene-creed-spirit",
                "nicene.{0,60}(spirit|ghost)|(spirit|ghost).{0,60}nicene|creed.{0,60}(spirit|ghost)|(spirit|ghost).{0,60}creed",
                "Yes. The Nicene-Constantinopolitan Creed of 381 speaks of the Holy Spirit. In the creed's own words: \""
                + "And in the Holy Ghost, the Lord and Giver of life, who proceedeth from the Father, who with the Father and the Son together is worshiped and glorified, who spake by the prophets." + "\" We hold this creed as established Christian orthodoxy, and we test every creed by Scripture.",
                "John 15:26", "2 Peter 1:21"));

        ROUTES = Collections.unmodifiableList(r);
    }

    private ApprovedRoutes() {}

    /** The route answer for a question, or null when no route names it or a quoted anchor cannot be verified. */
    public static Answer answerFor(String question, KjvVerseText kjv) {
        if (question == null || kjv == null) return null;
        String q = question.toLowerCase(Locale.ROOT).replace('’', '\'');
        for (Route route : ROUTES) {
            if (!route.match.matcher(q).find()) continue;
            StringBuilder quoted = new StringBuilder();
            for (String anchor : route.quotes) {
                String line = IdentifyRefute.verseLine(anchor, kjv);
                if (line == null) return null;
                quoted.append(line).append("\n");
            }
            return new Answer(route.id, route.body + "\n\nScripture:\n" + quoted.toString().trim());
        }
        return null;
    }
}
