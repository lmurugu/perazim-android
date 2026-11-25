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
                + "We do not call cessationists heretics, false Christians or unsaved because they disagree. Desire the gifts that build up the Church, and seek love above all.",
                "1 Corinthians 14:1", "Acts 2:39"));

        r.add(new Route("apostasy-assurance",
                "lose (?:my |our |their |his |her |your )?salvation|lose (?:our |my |their )?place in heaven|salvaiton|salvtion|once saved|eternal security|apostas|apostate|aposttae|aposttate|fall(?:ing|s)? away|unforgivable|sin on purpose|keep sinning|never saved|peter denied|judas"
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
                "sovereign|free will|human responsibility|god.s sovereignty|sovereignty and responsibility|work out your salvation|fear and trembling",
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
                + "which we hold as established Christian orthodoxy, confess Jesus as the eternal Son of God, truly God and truly man.",
                "John 1:1", "John 10:30"));

        r.add(new Route("first-gospel-preacher",
                "first (?:person|one|man|preacher).{0,40}(?:preach|gospel)|who (?:was )?the first .{0,30}(?:preach|gospel)",
                "I cannot name one first preacher with certainty, and I do not have an established answer from the text. Mark 1:14 says that Jesus came into Galilee, 'preaching the gospel of the kingdom of God'. Luke 2:10 records the angel's words to the shepherds: 'behold, I bring you good tidings of great joy'. The Bible does not give anyone the title 'the first person to preach the gospel'.",
                "Mark 1:14", "Luke 2:10"));

        r.add(new Route("creation-points-to-god",
                "does god exist|god exists|god.s existence|existence of god|is there a god|prove (?:that )?god|proof (?:of|for) god|evidence for god",
                "The Bible says that creation points beyond itself to its Creator. The heavens declare the glory of God, and His "
                + "eternal power is clearly seen in what He has made (Psalm 19:1; Romans 1:20). Human moral awareness also points to a "
                + "moral Lawgiver: the law is written on people's hearts, and their conscience bears witness (Romans 2:14–15).\n\n"
                + "This is a reasoned case, not a claim that every question is settled or that every person will be compelled to believe. "
                + "Hebrews 11:6 shows that faith is necessary to come to God: the one who comes must believe that He is, and that He is a "
                + "rewarder of those who seek Him. It explains the Christian response; it is not a proof for a sceptic on its own.",
                "Romans 1:20", "Psalms 19:1"));

        r.add(new Route("christians-disagree",
                "christians? (?:disagree|disagreement|disagreements)|disagree about doctrine|disagreements? among christians|"
                + "christianity is false because|denominations? disagree",
                "Disagreement by itself does not prove Christianity false. Scripture calls for unity around the central truths of the "
                + "gospel: one Lord, one faith, one baptism (Ephesians 4:5), and the gospel that Christ died for our sins and rose again "
                + "(1 Corinthians 15:3–4). Paul also says that even an angel from heaven must not be believed if he preaches another "
                + "gospel (Galatians 1:8).\n\n"
                + "So each disagreement must be examined by Scripture. Some concern secondary matters, and some touch the central truths. "
                + "Not every disagreement is harmless, and not all churches teach the same thing. The answer is to test what is taught, "
                + "not to give up on the Bible.",
                "Ephesians 4:5", "Galatians 1:8"));

        r.add(new Route("problem-of-evil",
                "why does evil exist|problem of evil|all-powerful and all-loving|all powerful and all loving|if god is all.powerful|"
                + "why does god allow (?:evil|suffering)|why is there (?:evil|suffering)",
                "God does not commit evil, and He does not call evil good. Scripture traces the entrance of sin to the first human choice "
                + "to rebel against God's word (Genesis 3). Human beings remain responsible for their evil choices, and Scripture does not "
                + "excuse them (James 1:13). The men who put Jesus to death were responsible, even though God carried out His saving "
                + "purpose through the cross (Acts 2:22–24).\n\n"
                + "God can work good through hard things, as Joseph saw (Genesis 50:20), and Romans 8:28 is a promise to those who love "
                + "God, not a claim that every event is good in itself. Creation waits for its redemption (Romans 8:18–25), and God will "
                + "judge all wrong and put all things right (Acts 17:31; Revelation 21:4).\n\n"
                + "We do not know why God permits every particular suffering, and we will not give a glib answer to someone who is "
                + "grieving. The cross and future justice are our hope.",
                "James 1:13", "Revelation 21:4"));

        r.add(new Route("bible-contradictions",
                "contradicts? itself|contradict each other|bible contradict|contradictions? in (?:the )?bible|bible.{0,30}contradict|"
                + "circular reasoning",
                "We take the claim seriously. The Bible claims to be God's true word (Psalm 119:160; John 17:17). Simply quoting that claim "
                + "does not answer a specific alleged contradiction, so we examine each example on its own terms. The method is: identify "
                + "the exact passages; read their context; check who is speaking and to whom; distinguish different events, times and "
                + "perspectives; and check translation or manuscript questions where they apply.\n\n"
                + "Proverbs 26:4–5 looks like a contradiction until we see that the two sayings answer two different situations. "
                + "If you name two passages, we will examine that specific example together, in its context.",
                "Proverbs 26:4", "Proverbs 26:5"));

        r.add(new Route("faith-and-evidence",
                "faith .{0,30}without evidence|believing without evidence|blind faith|faith (?:and|versus|vs\\.?) evidence",
                "Biblical faith is trust in God based on what He has said and done. It is not pretending to know something without any "
                + "reason. Hebrews 11:1 says that faith is the substance of things hoped for, the evidence of things not seen. The apostles "
                + "wrote so that people would believe: John 20:31 says these things are written that you might believe that Jesus is the "
                + "Christ, the Son of God, and that believing you might have life through His name.\n\n"
                + "Faith includes trust and commitment, not merely agreeing that a fact is true. We do not claim that every spiritual "
                + "truth can be shown in a laboratory.",
                "Hebrews 11:1", "John 20:31"));

        r.add(new Route("demon-possession",
                "(?:possess|possessed|possession|possessing).{0,40}demon|demon.{0,40}(?:possess|possession)|"
                + "(?:christian|believer|saved).{0,40}demon|demonized|demon.possessed",
                "We will not make a categorical declaration that a Christian can or cannot be demon-possessed. Scripture does not give "
                + "us a single formula for every modern claim. What Scripture clearly says is that believers must resist the devil and "
                + "stand firm in the faith, and that they can trust in Christ's power and in the protection of the One who keeps His "
                + "sheep (James 4:7; 1 Peter 5:8-9).\n\n"
                + "We do not diagnose anyone as demon-possessed. If you are worried about yourself or someone else, please talk with your "
                + "pastor or a trusted church leader, and see a doctor if there are any health concerns.",
                "James 4:7", "1 Peter 5:8-9"));

        r.add(new Route("prophets-today",
                "prophets? (?:still|today|operating|exist)|are there (?:still )?prophets|prophecy today|prophesy today|"
                + "still prophe|prophets in the church|gift of prophecy",
                "We believe the gift of prophecy continues in the Church today. It is given by the Holy Spirit to strengthen, encourage "
                + "and comfort the Church (1 Corinthians 14:3). Believers are not to despise prophecies, but to test everything and hold "
                + "fast what is good (1 Thessalonians 5:21). In the gathered church, prophetic messages are weighed and judged (1 Corinthians 14:29).\n\n"
                + "Having the gift is different from recognising a particular person as a prophet. A prophecy can never overrule, add to "
                + "or correct Scripture. Not everyone who calls himself a prophet is exercising the gift, and we do not name any "
                + "contemporary person as a prophet without an authorised, evidence-based church decision. Not every Christian is "
                + "called to prophesy.",
                "1 Corinthians 14:3", "1 Thessalonians 5:21"));

        r.add(new Route("healing-and-faith",
                "^(?!.*atonement)(?!.*\\b(?:medic|doctor|hospital|hiv|cancer|diabet|tablet|drug|clinic|treatment|surgery|pain)).*"
                + "(?:enough faith|faith .{0,30}heal|heal.{0,30}faith|pray for (?:the )?sick|healed .{0,30}faith)",
                "Christians should pray for the sick and trust God's power to heal. James tells the church to call for the elders to pray "
                + "over a sick person, anointing with oil in the name of the Lord (James 5:14). Faith is not a price, payment or force that "
                + "guarantees a requested physical healing. Paul asked the Lord three times to remove an affliction, and the Lord answered "
                + "that His grace was sufficient for him (2 Corinthians 12:7–9). The passage does not name that affliction.\n\n"
                + "God's answer is not controlled by the amount of a person's faith, and a person's illness does not prove that they lack "
                + "faith. We pray in faith, and we trust God's wise answer.",
                "James 5:14", "2 Corinthians 12:9"));

        r.add(new Route("matthew-7-21-lord-lord",
                "not everyone who (?:saith|says|calls).{0,40}lord|lord,? lord|calls? (?:me|him|jesus|the lord) lord",
                "Jesus says: 'Not every one that saith unto me, Lord, Lord, shall enter into the kingdom of heaven; but he that doeth the will of my Father which is in heaven' (Matthew 7:21). Calling Him Lord is not enough. He asks: 'And why call ye me, Lord, Lord, and do not the things which I say?' (Luke 6:46). Faith in Christ is what saves: 'For by grace are ye saved through faith' (Ephesians 2:8). We do not judge any person's standing before God. Test yourself and every profession by the words of Christ, and talk with a trusted church leader or pastor if you are unsure.",
                "Matthew 7:21", "Luke 6:46"));

        r.add(new Route("pharaoh-heart",
                "pharaoh|pharoah",
                "The Bible holds two truths side by side, and we hold both. Pharaoh 'hardened his heart' himself (Exodus 8:15), and God told Pharaoh: 'Even for this same purpose have I raised thee up' (Romans 9:17). Scripture does not make God the author of sin, and we do not claim to explain every mystery. Paul answers the question with a call to humility: 'Nay but, O man, who art thou that repliest against God?' (Romans 9:20).",
                "Exodus 8:15", "Romans 9:17"));

        r.add(new Route("agape-and-philia",
                "\\bagape\\b|\\bagap\\u0113\\b|\\bphilia\\b|\\bphileo\\b|difference between (?:agape|philia|the greek words? for love)",
                "The Greek New Testament uses several words for love. Strong's lexicon gives agape (G26) as 'love, i.e. affection or "
                + "benevolence; specially (plural) a love-feast'. It gives philia (G5373) as 'fondness: friendship'. The verb phileo (G5368) "
                + "is glossed 'to be a friend to ... have affection for', and Strong's says it denotes personal attachment, 'as a matter of "
                + "sentiment or feeling'. The verb agapao (G25) is wider, 'embracing especially the judgment and the deliberate assent of "
                + "the will as a matter of principle, duty and propriety'.\n\n"
                + "In John 21:15–17 both kinds of love appear in the exchange between Jesus and Peter, and the King James Version translates "
                + "both as 'love'. The verses in each passage decide the meaning. Strong's glosses are lexicon notes, not Scripture.",
                "John 21:15", "John 21:17"));

        r.add(new Route("resurrection-evidence",
                "(?:evidence|proof|prove|historical).{0,40}resurrection|resurrection.{0,40}(?:evidence|historical|proof)|tacitus|"
                + "did jesus (?:really )?(?:rise|come back to life)",
                "Scripture gives the earliest testimony that Jesus rose from the dead. Paul received and passed on the message that Christ "
                + "died, was buried, and rose again the third day according to the Scriptures, and that He appeared to many witnesses "
                + "(1 Corinthians 15:3–8). If Christ has not been raised, our preaching is empty and our faith is empty (1 Corinthians 15:14).\n\n"
                + "Outside the Bible, historical evidence is limited, but one hostile non-Christian source confirms the execution. The Roman "
                + "historian Tacitus wrote, in Annals 15.44 (Church and Brodribb translation): 'Christus, from whom the name had its origin, "
                + "suffered the extreme penalty during the reign of Tiberius at the hands of one of our procurators, Pontius Pilatus.' That "
                + "shows that Jesus was executed under Pilate. It does not by itself prove the resurrection. The resurrection rests on the "
                + "apostolic testimony, which we present as testimony, not as a proof that compels belief. I cannot research beyond the sources stored in "
                + "this guide, so I cannot verify other historical claims about the resurrection for you.",
                "1 Corinthians 15:4", "1 Corinthians 15:14"));

        r.add(new Route("early-church-witnesses",
                "before nicaea|before nicea|pre-nicene|ante-nicene|church fathers?|ignatius|justin martyr|early church (?:believe|teach|taught)",
                "Scripture is the authority. The early writers are witnesses to what the churches believed. Ignatius, writing early in the "
                + "second century, calls the Lord 'God existing in flesh' (To the Ephesians 7, Ante-Nicene Fathers, vol. 1). Justin Martyr, "
                + "writing about the middle of the second century, says that Christians worship 'the Son ... and the prophetic Spirit' "
                + "(First Apology 6), and that they hold the Son 'in the second place, and the prophetic Spirit in the third' (First Apology 13).\n\n"
                + "These witnesses show that Christ was worshipped as God and that the Spirit was honoured with the Father and the Son. They do "
                + "not show that these writers used the later technical language of one substance, as the Nicene Creed of 325 does. The "
                + "councils did not invent the biblical teaching; they gave more precise wording to defend it.",
                "John 1:1", "Matthew 28:19"));

        r.add(new Route("augustine-biography",
                "augustine|augustinus|hippo regius|confessions of st",
                "Augustine, known in Latin as Augustinus (born 13 November 354 at Tagaste, in Roman North Africa; died 28 August 430 at "
                + "Hippo Regius, during the Vandal siege), was a Christian bishop and one of the most influential theologians of the Western "
                + "Church. He was baptised by Ambrose at Milan at Easter 387, was chosen a presbyter at Hippo in 391, and later became bishop "
                + "there. His Confessions tell the story of his conversion and begin with the line: 'Thou hast made us for Thyself, and our "
                + "heart is restless till it rests in Thee' (Confessions I.1).\n\n"
                + "Later Christians, including the Protestant Reformers, read him closely. He lived long before the Reformation and was not a "
                + "Protestant. His writings are historical witnesses and arguments to be tested by Scripture. They are not authorities above "
                + "it, and we do not treat each of his opinions as Perazim doctrine.",
                "Psalms 42:1", "Psalms 42:2"));

        r.add(new Route("witch-harm-swahili",
                "nichome|kuchoma|kumdhuru|nimdhuru|kuwaua|nikuue|kumuua",
                "Please do not burn a neighbour's house or hurt anyone. That would harm innocent people, and it is not what Jesus teaches "
                + "(Matthew 5:44; Romans 12:19). If you feel in danger, or someone is threatening you, call 999 or 112 now, or go to the nearest "
                + "police station. Please tell a trusted pastor or church leader today.",
                "Matthew 5:44", "Romans 12:19"));

        r.add(new Route("sickness-not-punishment",
                "\\bcurse\\b.{0,40}(?:sickness|sick|illness|disease|hiv)|(?:sickness|sick|illness|disease|hiv).{0,40}(?:punish|curse)|punishment for sin",
                "Scripture does not teach that every sickness is a curse or a punishment for a particular sin. When the disciples asked about a man born blind, Jesus answered: 'Neither hath this man sinned, nor his parents: but that the works of God should be made manifest in him' (John 9:3). We should not blame sick people. Please see a doctor or clinic for the illness, and talk with your pastor or a trusted church leader about any fear or shame you may be carrying.",
                "John 9:2", "John 9:3"));

        r.add(new Route("medication-and-faith",
                "antidepress|medication|medicine|tablets",
                "Please do not stop or change your medicine without the doctor or clinic who prescribed it. Talk with "
                + "them about what it does, about any side effects, and about any concern you have. I cannot judge your "
                + "medicine from Scripture alone, and I cannot say what is a sin for you personally. Pray, and talk "
                + "with your pastor or a trusted church leader too. Scripture calls us to seek wise counsel (Proverbs "
                + "11:14; Proverbs 15:22).",
                "Proverbs 11:14", "Proverbs 15:22"));

        r.add(new Route("cancer-treatment-and-faith",
                "chemo|cancer|surgery|doctors? (?:say|says|said)",
                "Please keep following your doctors' plan until you have spoken with them. Trusting God and seeking "
                + "medical care are not opposed. Talk with your doctor, your family and your pastor together, and ask "
                + "God for wisdom and peace as you decide (James 1:5).",
                "Proverbs 11:14", "James 1:5"));

        r.add(new Route("obedience-and-the-bible",
                "pastor says.{0,80}(?:obey|question)|questioning (?:him|the pastor|my pastor) is questioning god|questioning him is questioning god|obey him even when",
                "Asking questions is not the same as rebellion. Scripture calls us to honour leaders who serve God faithfully (Hebrews 13:17). "
                + "Leaders serve under Christ and under the Word of God, so when a teaching goes against the Bible we must obey God rather "
                + "than men (Acts 5:29). Test what your leaders say by the Scriptures, as the Bereans did (Acts 17:11). Please talk this through "
                + "with a trusted church leader, and read the passage yourself.",
                "Hebrews 13:17", "Acts 5:29"));

        r.add(new Route("claims-to-test",
                "tiktok|youtube|manabii|nabii|paralys|job within|sent by god|sent from god|prophet says|preacher .{0,60}(?:seed|job|money)",
                "I cannot verify this claim from a video or a message, so I am not certain it is true. Scripture tells us to test every "
                + "teacher and every prophecy by the Word of God, because false teachers and false signs can deceive people (Matthew 7:15; "
                + "Deuteronomy 18:22). Do not send money to get a blessing or a job. Only God knows hearts and judges them, so I will not "
                + "pronounce on this person. Please talk with your pastor or a trusted church leader before you act, and see a doctor or "
                + "clinic for any illness.",
                "Matthew 7:15", "Deuteronomy 18:22"));

        r.add(new Route("gold-dust-signs",
                "gold dust|oil coming out|visible sign|sign from god",
                "Scripture warns against seeking signs for their own sake, and against false signs and wonders (Matthew 12:39; "
                + "2 Thessalonians 2:9–10). I cannot confirm that a claimed sign comes from God. Test every sign by the Word of God, and talk "
                + "with a trusted church leader before you accept it.",
                "Matthew 12:39", "2 Thessalonians 2:9"));

        r.add(new Route("sick-has-demon-swahili",
                "ana pepo|kila mgonjwa",
                "I cannot confirm that every sick person has a demon. Scripture does not teach that every sickness is a punishment for sin "
                + "or a demon (John 9:2–3). Please see a doctor or clinic, and talk with a trusted church leader before you accept this teaching. "
                + "I can answer in English first.",
                "John 9:2", "John 9:3"));

        r.add(new Route("saying-cleanliness",
                "cleanliness is next to godliness",
                "The saying 'cleanliness is next to godliness' is not in the Bible, although many people repeat it. Scripture does teach care and good order in the home and church: the virtuous woman 'looketh well to the ways of her household' (Proverbs 31:27), and all things should be done decently and in order (1 Corinthians 14:40). Keeping your house clean is a good work, but it does not earn God's favour.",
                "Proverbs 31:27", "1 Corinthians 14:40"));

        r.add(new Route("saying-helps-themselves",
                "helps those who help themselves|god helps those",
                "I cannot find that saying in the Bible, and Scripture does not say that God helps those who help themselves. The Bible teaches that we are saved by grace through faith, not by our own works (Ephesians 2:8–9). Proverbs 3:5–6 calls us to trust the LORD with all our heart.",
                "Ephesians 2:8", "Proverbs 3:5"));

        r.add(new Route("joshua-sun-appearance",
                "sun stand|sun stood still|sun stands still|earth is flat|flat earth",
                "Joshua 10:13 describes the sun standing still as it appears to the people, using the language of appearance, and it does not say the earth is flat. Scripture often speaks in ordinary language about what people see (Psalms 19:6). Please test claims about the Bible by reading the passage in its context.",
                "Joshua 10:13", "Psalms 19:6"));

        r.add(new Route("judas-saved",
                "judas.{0,30}saved|saved.{0,30}judas",
                "The Bible does not say that Judas was saved. Matthew 27:5 says that he went and hanged himself after he had betrayed Jesus, and Acts 1:25 says that he went to his own place. We do not judge any individual's final destiny, and Scripture calls each of us to examine ourselves and trust in Christ (2 Corinthians 13:5).",
                "Matthew 27:5", "Acts 1:25"));

        r.add(new Route("governance-accountability",
                "uwazi wa fedha|hakuna uwazi|financial transparency|fedha",
                "Scripture calls church leaders to handle money honestly: 'Providing for honest things, not only in the sight of the Lord, but also in the sight of men' (2 Corinthians 8:20–21). It is right to ask for clear, open accounts of church money. Please raise this in writing with your pastor, the church office or a trusted church leader, and ask for a report. I cannot judge any individual, but honest money handling is part of faithful leadership.",
                "2 Corinthians 8:20", "2 Corinthians 8:21"));

        r.add(new Route("remove-pastor",
                "remove (?:a |the |our )?pastor|dismiss (?:a |the )?pastor|pastor from office|congregation remove|remove .{0,20}from office",
                "Scripture gives the church a real role in caring for its leaders and in dealing with wrongdoing. "
                + "Matthew 18:15–16 describes careful steps, and Acts 6:3 shows that church leaders should be men of "
                + "good report. Accountability and discipline follow the church's own constitution and proper "
                + "procedures, and this guide cannot give a verdict on any leader. Please talk with the church office "
                + "and a trusted church leader about the right process.",
                "Matthew 18:15", "Acts 6:3"));

        r.add(new Route("accused-leader-abuse",
                "accused of abuse|leader who is accused|leader .{0,40}accused|handle a leader",
                "Your safety comes first. If anyone is being abused or is in danger, go to a safe place and call 999 "
                + "or 112 now, or report to the police or the appropriate authorities. You do not have to confront the "
                + "alleged abuser, and no one may require two witnesses before you seek protection. Get support from a "
                + "trusted adult or an independent counsellor. Church leaders should respond with truth and care for "
                + "the person who was harmed, and internal church process must never delay safety or reporting. "
                + "Scripture calls for truth and a careful process (1 Timothy 5:19; Matthew 18:16), but Matthew 18 is "
                + "for restoring a brother in love and never exposes a victim to further harm.",
                "1 Timothy 5:19", "Matthew 18:16"));

        r.add(new Route("which-leaders-trust",
                "which (?:church )?leaders|can i trust|which pastors|who can i trust",
                "I will not name or judge any leader, because only God knows hearts. You can evaluate public teaching "
                + "and observable conduct by the Scriptures: does the teaching agree with the Bible, and does the "
                + "leader live openly, handle money honestly and accept accountability (Matthew 7:16; 1 John 4:1). Talk "
                + "with a trusted church leader or the church office before you decide.",
                "Matthew 7:16", "1 John 4:1"));

        r.add(new Route("false-teacher-check",
                "false teacher|preacher i saw|preacher on tv|preacher on television",
                "Only God knows hearts and judges them, so I cannot say whether this preacher is a false teacher as a person. But we can test what he teaches. Scripture tells us to test the spirits and to test every teaching by the Word (1 John 4:1; Acts 17:11). Write down what he says, check it against the Bible in context, and talk with your pastor or a trusted church leader before you accept or repeat it.",
                "1 John 4:1", "Acts 17:11"));

        r.add(new Route("tribe-unity",
                "tribe|tribal|kabila|ethnic",
                "Scripture does not teach that voting for someone from our own tribe is God's will. The Bible calls all believers to unity in Christ, where there is neither Jew nor Greek (Galatians 3:28), and Jesus prayed that they all may be one (John 17:21). Division by tribe is contrary to that unity. Please talk with your pastor or a trusted church leader about how to keep the church together.",
                "Galatians 3:28", "John 17:21"));

        r.add(new Route("politics-and-voting",
                " vote|voting|candidate|political part|endorse|politic",
                "This guide does not endorse any candidate, party or political view. Christians may hold different "
                + "views on candidates and parties, and each person must follow their own conscience before God. "
                + "Scripture gives principles for every choice: pray for leaders and for peace (1 Timothy 2:1–2), act "
                + "justly and love mercy (Micah 6:8), and ask God for wisdom when you need it (James 1:5). Scripture "
                + "also condemns corruption and dishonest gain, and calls us to treat every person fairly, whatever "
                + "their tribe. Please test each candidate by those standards, and talk with your pastor about how to "
                + "honour God in your choice.",
                "1 Timothy 2:1", "Micah 6:8"));

        r.add(new Route("welcome-new-believer",
                "nimeamua kumfuata|kumfuata yesu|nimempokea yesu|nimeokoka|decided to follow jesus",
                "Karibu sana! Welcome, and we rejoice with you. The Bible says that if you confess with your mouth that Jesus is Lord, and believe in your heart that God raised Him from the dead, you shall be saved (Romans 10:9). Whoever calls upon the name of the Lord shall be saved (Romans 10:13). Take time to pray and thank God, and ask Him for His peace. Please talk with a pastor or a trusted church leader about baptism and how to grow in your new faith.",
                "Romans 10:9", "Romans 10:13"));

        r.add(new Route("thanks-help",
                "kumshukuru|nimepata job|i got a job|found a job|got the job",
                "Welcome, and thank you for sharing this with God. Thank God for your new job, and give thanks in "
                + "everything (1 Thessalonians 5:18). Thanksgiving is a way of trusting God with what He has done and "
                + "with what comes next (Philippians 4:6). Your pastor or a trusted church leader would be glad to "
                + "rejoice with you as well.",
                "1 Thessalonians 5:18", "Philippians 4:6"));

        r.add(new Route("thanks-and-prayer-help",
                "how do i pray|how to pray|help me pray|what to say to god|heart is heavy|jinsi ya kuomba",
                "You can pour out your heart to God in plain words, and He hears you. Scripture says we can pour out "
                + "our hearts to God (Psalms 62:8), and the Holy Spirit helps us when we do not know what to say "
                + "(Romans 8:26). Tell God what is on your heart, and ask Him for peace. A trusted church leader or "
                + "your pastor can pray with you as well.",
                "Psalms 62:8", "Romans 8:26"));

        r.add(new Route("bible-text-request",
                "kitabu cha|sura ya|zaburi|mathayo",
                "I can quote the Bible in this guide only from the King James Version and the NASB, so I cannot give you a Swahili text here. The Bible reader in the app has Swahili versions you can open. Here is the King James Version text:",
                "Matthew 5:3", "Psalms 23:1"));

        r.add(new Route("jesus-did-not-die-claim",
                "yesu hakufa|jesus didn't die|jesus did not die|hakufa msalabani|didn't die on the cross|did not die on the cross",
                "Scripture does not support the claim that Jesus did not die on the cross. The Gospels record His death (Mark 15:37), and Paul gives the core of the gospel: Christ died for our sins, was buried, and rose again (1 Corinthians 15:3–4). Do not accept a claim only because it appears in a video. Test it by the Bible, and talk with your pastor or a trusted church leader.",
                "Mark 15:37", "1 Corinthians 15:3"));

        r.add(new Route("payment-for-blessing",
                "prayer cloth|pay .{0,30}shillings|breakthrough",
                "Scripture does not teach that God's blessing can be bought with a payment, a prayer cloth or a 'seed'. Generous giving is taught, but a gift given freely is different from money demanded under pressure (2 Corinthians 9:7; 1 Timothy 6:10). Please do not pay under pressure. Talk with a trusted church leader or your pastor, and ask the church office for a written explanation of any payment.",
                "2 Corinthians 9:7", "1 Timothy 6:10"));

        r.add(new Route("land-gift-pressure",
                "ardhi|nitoe ardhi|give my land|sell my land|land to the church",
                "Please do not sign away land or family property because someone promises a blessing. Scripture warns about greed and about pressure to give in a hurry (Proverbs 22:3; 1 Timothy 6:10). Talk with a lawyer, and with a trusted church leader or counsel from your family, before you sign anything, and take the time you need.",
                "Proverbs 22:3", "1 Timothy 6:10"));

        r.add(new Route("leadership-appointment",
                "approve me|join the leadership|join (?:the )?church leadership|leadership team|allowed to join",
                "I cannot approve anyone for church leadership, and this guide cannot approve anyone. Leadership "
                + "appointments follow the church's established process through the church office and the church's "
                + "leaders. Please speak with the church office or your pastor about your interest in serving. "
                + "Scripture shows that church leaders are chosen with care and prayer (Acts 6:3; 1 Timothy 3:10), so "
                + "bring your desire to God in prayer too.",
                "Acts 6:3", "1 Timothy 3:10"));

        r.add(new Route("pope-head-of-church",
                "pope|papal|papacy|vicar of christ",
                "Scripture names Christ as the head of the Church (Ephesians 1:22; Colossians 1:18). We do not accept an earthly head over the whole Church.",
                "Ephesians 1:22", "Colossians 1:18"));

        r.add(new Route("icons-scripture",
                "icon|icons|orthodox teaching on icons",
                "Read together, Exodus 20:4 and Exodus 20:5 forbid making an image to bow down to and worship (Exodus "
                + "20:4–5). God is spirit, and those who worship Him must worship in spirit and in truth (John 4:24). "
                + "We reject the veneration of images as a way of worship, and we do not hold that every picture or "
                + "image is forbidden. Please talk with your pastor "
                + "about how this applies.",
                "Exodus 20:4", "Exodus 20:5"));

        r.add(new Route("eastern-church-judge",
                "eastern church|eastern orthodox|orthodox church|better than the protestant|better than the catholic",
                "We do not rank churches, and we do not judge anyone's eternal destiny. Scripture says the Father has "
                + "committed all judgment to the Son (John 5:22). The right question is whether an official teaching "
                + "agrees with Scripture, and the Bereans tested what they heard by the Scriptures (Acts 17:11). We can "
                + "evaluate the official doctrine of any church against the Bible, while leaving the judgement of "
                + "individual people to God. Please talk this through with a trusted church leader.",
                "John 5:22", "Acts 17:11"));

        r.add(new Route("apocrypha-books",
                "apocrypha|deuterocanon|catholic bible|more books|tobit|maccabees|sirach|judith|baruch",
                "The Protestant Bible has 66 books: 39 in the Old Testament and 27 in the New. The Apocrypha and "
                + "deuterocanonical books, such as Tobit, Judith, Wisdom, Sirach, Baruch and 1 and 2 Maccabees, are not "
                + "part of the 66-book canon we hold. The reasons are historical as well as biblical. The Old Testament "
                + "was received by the Jewish community to which God entrusted His oracles (Romans 3:2), and Jesus "
                + "pointed to the Law, the Prophets and the Psalms as the Scriptures that testify of Him (Luke 24:44). "
                + "Those books can be read for history.",
                "Romans 3:2", "Luke 24:44"));

        r.add(new Route("greeting-only",
                "^(?:hi|hello|hey|good morning|good afternoon|good evening|habari|hujambo)(?:[ ,!.]+(?:good morning|good afternoon|good evening|there|friend))?[ !.]*$",
                "Good morning! Welcome. I can help with the Bible, prayer, church information and the faith questions you bring. What would you like to know? Please ask in your own words, and I will keep the answer simple. We can start with a short passage: 'This is the day which the LORD hath made; we will rejoice and be glad in it' (Psalms 118:24)."));

        r.add(new Route("campuses-unverified",
                "campus|campuses",
                "I do not have a verified list of Perazim campuses in this guide, so I cannot confirm them. The approved channels for this are the church office and the church's own published information in the app. Please contact the church office for the current campuses and service times."));

        r.add(new Route("giving-options-app",
                "giving options|ways to give|how to give|giving in the app|options in the app",
                "Giving options are shown in the app's giving section, and the church publishes its giving channels. Gifts should go only through published channels, such as M-Pesa Paybill, and you can confirm the current details with the church office. Please never give money to a person who asks for it privately."));

        r.add(new Route("god-not-a-man",
                "god was once a man|god was a man|once a man|became god|become god",
                "Scripture is clear that God is not a man who could become God. Before Him no god was formed, and "
                + "after Him there will be none (Isaiah 43:10). He is the first and the last, and there is no God "
                + "beside Him (Isaiah 44:6–8). He is God from everlasting to everlasting (Psalms 90:2).",
                "Isaiah 43:10", "Psalms 90:2"));

        r.add(new Route("archangel-michael-jesus",
                "archangel|jude 9|jesus is michael|jesus is the archangel",
                "Scripture does not say that Jesus is the archangel Michael. Jude 9 mentions the archangel Michael, but it does not say that Michael is Christ. The Word is described in John 1:1 as 'In the beginning was the Word, and the Word was with God, and the Word was God', and Colossians 1:16 says 'For by him were all things created'.",
                "John 1:1", "Colossians 1:16"));

        r.add(new Route("jesus-is-the-father",
                "jesus is the father|jesus the father|is jesus the father",
                "Scripture distinguishes the Father and the Son while confessing one God. At the baptism of Jesus the "
                + "Father spoke from heaven, the Son was baptised, and the Holy Spirit descended, so the three persons "
                + "are seen together (Matthew 3:16–17). Jesus said that He and the Father are one (John 10:30). That "
                + "oneness is the unity of the one God, not the erasing of the distinction between the Father and the "
                + "Son. The Church confesses one God, Father, Son and Holy Spirit, as the Nicene Creed of 381 expresses "
                + "it. Please test this with your pastor and the Bible passages in context.",
                "Matthew 3:17", "John 10:30"));

        r.add(new Route("trinity-not-pagan",
                "trinity.{0,60}pagan|pagan.{0,60}trinity|invented by the church|trinity .{0,30}invented",
                "The Trinity is not a pagan idea. It is taught in the Scripture quoted below: 'Hear, O Israel: The LORD our God is one LORD' (Deuteronomy 6:4), and 'baptizing them in the name of the Father, and of the Son, and of the Holy Ghost' (Matthew 28:19). The word Trinity is a later summary of this teaching, not a new god.",
                "Deuteronomy 6:4", "Matthew 28:19"));

        r.add(new Route("sunday-mark-beast",
                "sunday.{0,40}mark|mark of the beast|ellen white",
                "Scripture does not identify Sunday as the mark of the beast, and I cannot verify that claim from the text. The mark in Revelation 13 is not named as Sunday in the text. Paul wrote: 'Let no man therefore judge you' (Colossians 2:16), and 'Let every man be fully persuaded in his own mind' (Romans 14:5). We test this teaching by Scripture alone.",
                "Colossians 2:16", "Romans 14:5"));

        r.add(new Route("isaiah-53-context",
                "isaiah 53|isaya 53",
                "Isaiah 53 is one of the Servant Songs in Isaiah. The chapter describes a servant who is despised and rejected, who suffers, bears the sins of others, and is vindicated by God (Isaiah 52:13–53:12). Isaiah 40–55 speaks to a people in exile, to whom the LORD comforts his people and promises restoration. The chapter does not name the servant. In Acts 8:26–35, Philip explains this passage to the Ethiopian official and proclaims Jesus, and 1 Peter 2:24 applies the servant's suffering to Christ's bearing of our sins. So the New Testament identifies Jesus as the one who fulfils the servant's suffering for sin. We read the chapter in its Old Testament setting and in the light of its New Testament fulfilment.",
                "Isaiah 53:5", "Acts 8:35", "1 Peter 2:24"));

        r.add(new Route("psalm-137-context",
                "psalm 137|psalms 137|imprecat\\w*",
                "Psalm 137 is a communal lament from Israelites living in exile in Babylon. They remember Jerusalem, grieve their displacement, and struggle with the demand to sing the LORD's song in a foreign land. In verses 7–9, they cry out for judgment against Edom and Babylon. The final verse expresses a terrible desire for violent retribution. It records the depth of their anguish; it is not a command for Christians to take revenge. The New Testament teaches us to leave vengeance to God and to love our enemies (Romans 12:19; Matthew 5:44). We can bring pain, anger, and longing for justice honestly before God without turning them into personal violence.",
                "Psalms 137:1", "Romans 12:19", "Matthew 5:44"));

        r.add(new Route("grace-meaning",
                "grace mean|meaning of grace|maana ya neema|what is grace",
                "Grace is God's unearned favour toward sinners. Scripture says that we are saved by grace through faith, and that this is not of ourselves: it is the gift of God (Ephesians 2:8–9). Paul also says that we are justified freely by his grace, through the redemption that is in Christ Jesus (Romans 3:24). Because grace is not earned, it leads to gratitude and a changed life. We are saved by grace through faith, not by works; yet we are created in Christ Jesus for good works (Ephesians 2:8–10). God's grace also teaches us to reject ungodliness and live self-controlled, upright and godly lives (Titus 2:11–12).",
                "Ephesians 2:8", "Romans 3:24", "Ephesians 2:10"));

        r.add(new Route("justification-by-faith",
                "\\bjustif\\w*|justified by",
                "Here is a simple, gentle explanation. Justification is the good news that God declares a sinner righteous in his sight. We cannot earn it, because it is a gift of his grace. "
                + "Paul writes that we are justified freely by his grace, through the redemption that is in Christ Jesus (Romans 3:24). "
                + "Because we have been justified by faith, we have peace with God through our Lord Jesus Christ (Romans 5:1). "
                + "Paul is clear that a person is not justified by the works of the law, but by faith in Jesus Christ (Galatians 2:16). "
                + "We are saved by grace through faith, and this is not of ourselves: it is the gift of God (Ephesians 2:8–9). "
                + "We are created in Christ Jesus for good works, which flow from being saved, not as a way of earning it (Ephesians 2:10). "
                + "God's grace also teaches us to live self-controlled, upright and godly lives (Titus 2:11–12). "
                + "If you are asking whether you are right with God, bring that question to Jesus in prayer, and talk with your pastor.",
                "Romans 3:24", "Romans 5:1", "Galatians 2:16", "Ephesians 2:8", "Titus 2:11"));

        // Marriage and family routes, written from Levi's approval of sections 4-8 of the marriage framework (2026-11-25).
        // These are governing principles, not rigid scripts. Divorce, remarriage and women's ministry stay undecided.
        r.add(new Route("equal-worth-sexes",
                "inferior|superior to|weaker sex|lesser sex|second[- ]class|sexis\\w*|feminis\\w*|male supremacy|female supremacy",
                "Scripture teaches that men and women are equal in worth before God. God created both in his own image (Genesis 1:27). In Christ there is no difference of standing before God, because all who believe are one in Christ Jesus (Galatians 3:28). Peter calls husbands to honour their wives as heirs together of the grace of life (1 Peter 3:7). Men and women have both sinned and need the same Saviour (Romans 3:23). Scripture also gives husbands and wives different roles in marriage and family, but a difference of role does not mean that one sex is worth less than the other. No one should use the Bible to despise or belittle a man or a woman. The church has not settled the question of women's ministry roles, so please speak with your pastor about that question; this answer does not decide it. Please pray for wisdom and humility as you think about this.",
                "Genesis 1:27", "Galatians 3:28", "1 Peter 3:7", "Romans 3:23"));

        r.add(new Route("unfaithful-generalisation",
                "\\b(?:all|every) (?:men|women|husbands|wives) (?:are|is) \\w+|proving (?:that )?(?:all|every) (?:men|women|husbands|wives)",
                "I will not give a list of verses to prove that one sex is unfaithful or untrustworthy as a whole, because that is a one-sided argument and a sweeping claim. Scripture teaches that every person has sinned and falls short of the glory of God (Romans 3:23), and that there is none righteous, no, not one (Romans 3:10). That truth applies to men and women alike, and it is why every believer must trust in Christ rather than in their own strength. Scripture also honours marriage and calls husband and wife to be faithful and to love one another (Hebrews 13:4; Ephesians 5:25). A person who has been betrayed can speak to a pastor and seek safe support without being told that the whole opposite sex is the problem. Please pray about any hurt you carry, and talk with a trusted pastor who can walk with you.",
                "Romans 3:23", "Romans 3:10", "Hebrews 13:4", "Ephesians 5:25"));

        r.add(new Route("proof-text-request",
                "(?=.*\\b(?:wife|husband|wives|husbands|spouse|marriage|mke|mume)\\b)(?:(?:give|list|find|show|quote|send) (?:me )?.{0,40}verses? .{0,40}(?:proving|to prove|that prove)|verses? (?:proving|to prove|that prove))",
                "I will not give a list of verses chosen to prove one side of a marriage argument. A verse pulled out of its context can be used to hurt a spouse, and Scripture warns against twisting the Word of God (2 Peter 3:16). The Bible teaches husbands and wives to submit to one another in the fear of God (Ephesians 5:21). It calls husbands to love their wives as Christ loved the church (Ephesians 5:25), and it tells husbands not to be bitter against their wives (Colossians 3:19). Scripture does not teach that a husband may demand that his wife obey everything he says. Wives are not called to obey a sinful instruction (Acts 5:29). If you want to understand what the Bible teaches about marriage, read these passages in their context with your pastor, and pray together about how each of you can love and serve the other.",
                "Ephesians 5:21", "Ephesians 5:25", "Colossians 3:19", "Acts 5:29"));

        r.add(new Route("coercive-control-safety",
                "controls? (?:all )?(?:the )?money|(?:won'?t|will not|doesn'?t|does not) let me (?:work|go out|leave|see)|not let me work|kunizuia kufanya kazi|ananizuia",
                "What you describe is control, and in some cases it is abuse. Controlling all the money, or stopping a spouse from working, can be economic abuse, and it is not the godly marriage that Scripture describes. Scripture calls husbands to love and care for their wives as their own bodies (Ephesians 5:28–29), and calls spouses to honour one another. Nobody has to stay in danger or submit to control to keep the peace. If you are in danger right now, call 999 or 112 now. Please tell a trusted adult or a church leader who can keep you safe, and contact the church office about safeguarding. Do not confront the spouse alone, and do not go into joint counselling while there is threat or control; speak with a safe person first. This guide cannot investigate, promise confidentiality or act for you, so do not wait for an app reply in an emergency. The Lord is a refuge for the oppressed (Psalm 9:9), and you are not alone.",
                "Ephesians 5:28", "Ephesians 5:29", "Psalms 9:9"));

        r.add(new Route("divorce-remarriage-undecided",
                "divorc\\w*|remarr\\w*|should i leave|leave (?:him|her)|tell me yes or no",
                "The church has not settled the question of divorce and remarriage, so I will not give a yes or no on whether you should leave, whether a divorce is permitted, or whether a person may marry again. Scripture does speak to marriage, faithfulness and separation. Jesus taught that God joins a husband and wife, and that a man should not separate what God has joined (Matthew 19:6). Paul gives further teaching about a marriage where an unbelieving partner departs (1 Corinthians 7:15). Because the church has not decided this matter, please talk with your pastor or a trusted church leader, who can walk with you through these passages and your situation. Your safety comes first: if you are in danger, call 999 or 112 now. Please pray, and do not make a decision about your marriage alone while you are under pressure.",
                "Matthew 19:6", "1 Corinthians 7:15"));

        r.add(new Route("infidelity-and-betrayal",
                "adulter\\w*|cheat\\w*|cheated|unfaithful\\w*|affair|sleeping with|sleep with|romantic chats?|ujumbe wa kimapenzi|ananisaliti|kusaliti|kugongewa|kugongeana|gongew|gongean|mpango wa kando|secret (?:romantic|relationship|chats?)|pornograph\\w*|wanaume wote|wanawake wote|anasema wanaume|anasema wanawake|all men do|all women do|everyone cheats|betray\\w*|infidel\\w*|revenge|nimeona ujumbe|nimegundua",
                "Scripture calls adultery sin: You shall not commit adultery (Exodus 20:14). Jesus taught that even looking on a woman to lust after her is already a breach of faithfulness (Matthew 5:28). Infidelity is not one single thing, so it helps to establish what has happened before deciding what it means. The church recognises several distinct kinds: sexual adultery; deliberate romantic or sexual messages with someone outside the marriage; pornography; secret emotional relationships that take the place of the spouse; and ordinary friendships, which are not automatically infidelity. A single word such as cheating should not be used to label everything. A claim that every man or every woman does this is not the truth of God, and it does not make betrayal right (Romans 3:23). Forgiveness, trust and reconciliation are different things. Forgiveness is a decision to release the wrong before God. Trust is rebuilt slowly through honest and faithful action. Reconciliation depends on repentance and on safety. A betrayed spouse should never be pressured to pretend that nothing happened, or to return to an unsafe situation. Revenge is not a Christian answer, because the Lord says, Vengeance is mine; I will repay (Romans 12:19), and we are not to be overcome of evil, but to overcome evil with good (Romans 12:21). In Kiswahili, kugongeana is used for sleeping with someone else's partner, mostly when a man sleeps with another man's girlfriend, wife or partner. If you have used kugongewa in a different sense, please tell me in one sentence. Please pray, speak with a trusted pastor or counsellor, and go slowly before making any decision. If you have seen messages that frighten you, or if anyone threatens you, safety comes first and you should tell a trusted adult now.",
                "Exodus 20:14", "Matthew 5:28", "Romans 3:23", "Romans 12:19", "Romans 12:21", "Hebrews 13:4"));

        r.add(new Route("provision-and-money",
                "earns more|makes more|more money than|50/50|strict split|split (?:of )?every bill|split (?:of )?the bills?|kupata pesa|anapata pesa nyingi|pesa nyingi kuliko|heshima yangu kama mwanaume",
                "The Bible calls the husband to provide for his own household: anyone who does not provide for his own, and especially for those of his own house, has denied the faith (1 Timothy 5:8). That passage does not mean that only men may earn money, and it does not make a wife's income a shame. Scripture honours a wife who works, plans and contributes to her household (Proverbs 31:16, 24), and it describes a husband who trusts his wife, so that he has no need of spoil (Proverbs 31:11). A man's worth as a man does not depend on how much money his wife earns, and a woman's worth does not depend on her income either, because God values each person (Genesis 1:27). A husband and wife are one flesh (Genesis 2:24), so money should be handled with honesty, generosity and agreement. A strict 50/50 split of every bill is not commanded in Scripture, and it is not the only fair arrangement. Please pray together, and ask a trusted pastor to help if money is creating conflict or fear in your home.",
                "1 Timothy 5:8", "Proverbs 31:11", "Genesis 1:27", "Genesis 2:24"));

        r.add(new Route("singleness-and-value",
                "don'?t want to marry|do not want to marry|not (?:want|going) to (?:get )?marry|stay single|remain single|singleness|(?:single|singl\\w*) (?:forever|for life)|ndoa ni trap|trap kwa|nikae single|si nikae|lose (?:her|his|my|their) value|value after|modern (?:women|men) are|women are difficult|men are difficult|going their own way|mgtow",
                "The Bible honours marriage: it is honourable among all, and the bed undefiled (Hebrews 13:4). Scripture does not make marriage compulsory for every believer. Paul speaks of singleness as a gift that lets a person serve the Lord with fewer divided concerns (1 Corinthians 7:7; 1 Corinthians 7:32–35). A man or woman who chooses to remain single is not automatically selfish, bitter or spiritually immature. A person who is not married is not valued less by God (Genesis 1:27; Psalm 139:14). A sweeping judgement that the opposite sex is difficult or manipulative is not a truth God teaches, and it does not describe any person in God's sight, because all have sinned and need grace (Romans 3:23). If you are thinking about marriage out of fear, bitterness, past hurt or pressure, please pray honestly and talk with a pastor who knows you, because those feelings need care. Some people stay unmarried for reasons such as financial limits, fear or trauma, and they should receive compassion, not shame. A marriage should be entered in faith, not from pressure or cynicism.",
                "Hebrews 13:4", "1 Corinthians 7:7", "1 Corinthians 7:32", "Psalm 139:14"));

        r.add(new Route("in-laws-and-parents",
                "mother-in-law|mother in law|father-in-law|in-laws?|in laws|mama mkwe|mkwe|mama yangu anamwambia",
                "The Bible says that a man shall leave his father and mother and cleave unto his wife, and they shall be one flesh (Genesis 2:24). Scripture honours parents (Ephesians 6:2), but parents are not rulers over a married couple. The husband and wife are called to lead their own home together, with respect and kindness towards their parents. A husband should not stay silent while his wife is hurt, and he should speak up kindly and firmly for her. A wife should not be shamed for the way she runs her home. The couple should talk together, pray together, and set kind but clear boundaries with the parent. If the conflict is hard to settle, ask a trusted pastor to help. If anyone is being harmed or threatened, safety comes first.",
                "Genesis 2:24", "Ephesians 6:2", "Exodus 20:12"));

        r.add(new Route("household-labour",
                "hafanyi kitu|anafanya kila kitu|kazi za nyumbani|kazi za nyumba|household chores|chores|housework|dad (?:doesn'?t|does not|never) (?:do|help|cook|clean)|mum (?:does|doesn'?t|never)",
                "The Bible calls every member of the household to serve one another in love (Galatians 5:13). Scripture also calls believers to work with their hands and to care for their own household (1 Thessalonians 4:11; Ephesians 5:21). Household work is not only a woman's task, and it is not only a man's. A husband and wife can share the work of the home as each is able, and serving one another is how we honour God. Do not shame anyone for what they cannot do, and do not measure a parent's worth by chores alone. Talk together and pray, agree on a fair arrangement, and ask a trusted pastor for help if the conflict is hurting the family. If anyone is being harmed or threatened in the home, safety comes first.",
                "Galatians 5:13", "1 Thessalonians 4:11", "Ephesians 5:21"));

        r.add(new Route("holy-spirit-comforter",
                "comforter|holy spirit as comfort",
                "Jesus promised that the Father would send another Comforter, the Holy Spirit, who abides with believers forever (John 14:16). The Spirit teaches believers and brings to their remembrance what Jesus said (John 14:26). He testifies of Jesus (John 15:26), and Jesus said that it is expedient for his disciples that he go away, because the Comforter will not come unto them unless he goes (John 16:7). Scripture presents the Spirit as the Comforter who strengthens and guides God's people.",
                "John 14:16", "John 14:26", "John 15:26", "John 16:7"));

        r.add(new Route("headship-submission-marriage",
                "headship|head of (?:the )?(?:house|home|household|family)|submit to (?:my|your|his|her) husband|wives? (?:must )?submit|who (?:should )?(?:make|has) the final (?:say|decision)|final word|obey everything",
                "The husband is called to exercise headship through sacrificial love, spiritual responsibility, protection, provision and care. The wife is called to respect and submit to her husband in the Lord. Both remain under Christ and are called to mutual submission in reverence for Him. The husband must lead through service rather than domination and must take his wife's counsel seriously. His headship does not make him infallible or give him permission to sin, abuse, coerce or violate his wife's conscience. The wife remains personally accountable to God and is never required to obey a sinful instruction or to endure danger in the name of submission (Acts 5:29). The husband is accountable before God for how he leads; this is not an automatic unilateral veto over his wife. Scripture, in Ephesians 5, teaches headship, submission and sacrificial love, but it does not set out a tie-breaking procedure for every disagreement. Where a couple cannot agree after prayer and honest consultation, they should seek mature pastoral counsel rather than turn headship into a power contest. If anyone is being harmed, safety comes first. This answer concerns marriage only; the church has not settled the question of women's ministry roles.",
                "Ephesians 5:21", "Ephesians 5:25", "Colossians 3:18", "Colossians 3:19", "Acts 5:29"));

        r.add(new Route("khat-discouraged",
                "miraa|khat|muguka",
                "Scripture does not expressly name miraa, muguka or khat as forbidden substances. However, believers are called to exercise self-control, care for their bodies and avoid being mastered by anything (1 Corinthians 6:12; 1 Corinthians 6:19–20; 1 Peter 5:8; Galatians 5:22–23). Because khat is a stimulant and its habitual use may carry health, personal and family risks, the church discourages its use. This is a pastoral application of biblical principles, not an explicit biblical prohibition of the plant, and the church does not say that every use is the same as drunkenness. A person who finds it difficult to stop should receive compassionate pastoral support and practical assistance, not ridicule or condemnation. Where use has become controlling or damaging, the person should be encouraged to seek help and stop. NACADA's free helpline, 1192, is available 24 hours a day for counselling and referrals to accredited treatment and rehabilitation facilities. Please talk with your pastor or a trusted church leader as well.",
                "1 Corinthians 6:12", "1 Corinthians 6:19", "1 Peter 5:8", "Galatians 5:22"));

        r.add(new Route("family-planning-married",
                "family planning|contracept|birth control|condom|spacing (?:of )?children|uzazi wa mpango|injection|vasectomy|sterili[sz]\\w*|forces? me|forced to|won'?t let me decide|tubes? tied",
                "Children are a blessing from God, and Christian couples should receive them with gratitude and responsibility (Psalm 127:3–5). Scripture does not prescribe a fixed number of children that every married couple must have. Responsible family planning may therefore be a matter of conscience for the married couple, who should consider their health, responsibilities, circumstances and their ability to care for their children. Family planning must not be used to devalue children or reject God's gift of life. Methods intended to terminate an established pregnancy are excluded. Do not describe contraception, emergency contraception or every hormonal method as abortion. Emergency contraceptive pills work mainly by preventing or delaying ovulation and do not end an established pregnancy, and the copper-bearing IUD works mainly by preventing fertilisation. Couples should seek qualified medical advice about the available methods, their mechanisms, effectiveness, risks and side effects. Both spouses should discuss family planning together, but neither should force the other to undergo contraception or a permanent procedure. The person undergoing a medical procedure must give free and informed consent. Please see a doctor or qualified nurse for advice on any method, and do not accept a method or procedure you have not freely agreed to. If you feel forced or threatened, tell a trusted person now, and your safety comes first. I cannot prescribe a method or decide medical eligibility, so please speak with a qualified healthcare provider. Scripture does not name condoms, so this is a matter for the couple's shared conscience.",
                "Psalms 127:3"));

        r.add(new Route("god-still-loves-me",
                "mungu bado ananipenda|does god (?:still )?love me|god still loves me|is god still with me|sijui kama mungu",
                "I am sorry you are feeling this way. God's love does not depend on how well we have done. Scripture says that nothing can separate us from the love of God in Christ Jesus our Lord (Romans 8:38–39). The LORD says that he has loved his people with an everlasting love (Jeremiah 31:3), and God shows his love toward us because Christ died for us while we were still sinners (Romans 5:8). Please talk with your pastor or a trusted church leader soon, and tell them what you have told me. You do not have to carry this alone.",
                "Romans 8:38", "Romans 8:39", "Jeremiah 31:3", "Romans 5:8"));

        r.add(new Route("anger-at-brother",
                "angry (?:at|with) (?:my|a|the|someone|him|her|them)|been (?:so )?angry (?:at|with)|bitter (?:against|towards|toward|at)",
                "Anger can be a real feeling, but Scripture warns us not to let it grow into bitterness or to hold onto it: be angry, and do not sin, and do not let the sun go down on your wrath (Ephesians 4:26). Jesus taught us to be reconciled to a brother who has something against us (Matthew 5:23–24), and to forgive as the Lord forgave us (Colossians 3:13). Please bring the anger to God honestly in prayer, and ask him to show you the next step, whether that is forgiveness, a conversation or an apology. Talk with your pastor or a trusted leader if the conflict is hard to resolve, and seek help if the anger affects your sleep, health or safety.",
                "Ephesians 4:26", "Matthew 5:23", "Colossians 3:13"));

        r.add(new Route("forgiving-others",
                "forgiv\\w* (?:my|a|the|someone|him|her|them|brother|sister|friend|spouse|husband|wife)|forgive (?:my|a|the)|kumsamehe|kusamehe|msamehe",
                "Scripture calls us to forgive others as God in Christ has forgiven us (Ephesians 4:32; Colossians 3:13). Jesus taught that our forgiveness is connected to forgiving others (Matthew 6:14–15). Forgiveness does not mean pretending that the harm did not happen, and it does not require you to stay in danger. If you have been harmed, your safety and the help of a trusted leader come first. Please pray about it, and talk with your pastor if this is hard.",
                "Ephesians 4:32", "Colossians 3:13", "Matthew 6:14", "Matthew 6:15"));

        r.add(new Route("god-forgives-everyone",
                "god forgives (?:everyone|everybody|all)|if god forgives|everyone is forgiven|everybody is forgiven",
                "God's forgiveness is real and full for everyone who repents and trusts in Jesus Christ (Acts 3:19; 1 John 1:9). Scripture does not teach that every person is forgiven apart from faith and repentance. Jesus said that our forgiveness from the Father is connected to our forgiving others (Matthew 6:14–15), and that the one who does not believe the Son remains under the wrath of God (John 3:36). God is both merciful and just: he forgives the repentant, and he judges those who refuse Christ (Romans 2:5). We do not judge any particular person's standing before God.",
                "Acts 3:19", "1 John 1:9", "Matthew 6:14", "John 3:36", "Romans 2:5"));

        r.add(new Route("healing-as-proof",
                "(?:healing|miracle)s? .{0,80}(?:proof|prove|evidence)|(?:proof|evidence) .{0,60}(?:healing|miracle)",
                "A healing you see in a video is not, by itself, proof that God is working through a particular preacher. Scripture warns that false signs and wonders can deceive people (Matthew 24:24; 2 Thessalonians 2:9). We do not judge any individual's standing before God, but we can test what is taught and claimed by the Word of God (1 John 4:1). We do believe that God can heal, and that the prayer of faith may be offered for the sick (James 5:14–15). Please do not give money to anyone because of a video. Talk with your pastor or a trusted church leader before you decide, and give thanks with anyone who is truly healed.",
                "Matthew 24:24", "2 Thessalonians 2:9", "1 John 4:1", "James 5:14"));

        r.add(new Route("muslim-neighbours",
                "jirani zangu ni waislamu|waislamu|muslim neighbou?rs?|neighbou?rs? (?:are|is) muslim|muslims? (?:next door|in my street|living)",
                "Christians are called to love their neighbours, whatever their faith. Jesus said that the second great commandment is to love your neighbour as yourself (Luke 10:27), and he showed what that love looks like in the parable of the good Samaritan (Luke 10:37). Peace in your street comes from honest, kind and respectful living: greet your neighbours, help them in practical needs, keep your promises, and do not join in mocking or insulting them. Be ready to give an account of the hope you have, with gentleness and respect (1 Peter 3:15). Do not judge any Muslim person's standing before God; that belongs to God alone. If a specific conflict or threat arises, speak with your pastor, and report any violence to the police on 999 or 112.",
                "Luke 10:27", "Luke 10:37", "1 Peter 3:15"));

        r.add(new Route("god-changes-mind",
                "does god change|god change (?:his|her|its) mind|did god repent|god repent|god regret",
                "Scripture teaches that God does not lie or become unfaithful. He is unchanging in character and faithful to his promises (Numbers 23:19; Malachi 3:6). Yet Scripture also says that God relented from the judgment he had announced against Nineveh when the people turned from their evil way (Jonah 3:10). Jeremiah 18:7–10 explains that God's announcements of judgment and blessing relate to how people respond to him. When Scripture says that God repented or relented, it does not mean that he confessed sin or became morally unreliable. It describes his response to people while he remains faithful and righteous. We should take both kinds of biblical statements seriously.",
                "Numbers 23:19", "Malachi 3:6", "Jonah 3:10", "Jeremiah 18:7"));

        r.add(new Route("psalm-23-context",
                "psalm 23 (?:in context|explain|meaning|flow|context)|explain psalm 23|psalm 23 in context",
                "Psalm 23 is David's psalm about the LORD as shepherd. It moves from the LORD's care for his flock (verses 1–3), to his presence in the valley of the shadow of death (verse 4), to his table and his goodness in the house of the LORD (verses 5–6). The comfort comes from the LORD's faithfulness to his people, not from an easy life. Read in context, the psalm is about trust in the LORD in danger as well as in peace. The LORD's care does not mean David has no enemies or danger. The LORD protects him in the valley and provides for him even in the presence of his enemies. The psalm's confidence rests in God's faithful presence, not in the absence of hardship.",
                "Psalms 23:1", "Psalms 23:4", "Psalms 23:6"));

        r.add(new Route("christian-science",
                "christian science|mary baker eddy|science and health",
                "Christian Science is not the Bible-centred Christian faith. Its own book, Science and Health with Key to the Scriptures by Mary Baker Eddy, teaches that matter, sickness and death are unreal, and it understands Christ's resurrection in spiritual terms. Scripture says that the risen Jesus showed his hands and feet to his disciples and said that a spirit does not have flesh and bones as he has (Luke 24:39). Scripture also says that he was raised on the third day according to the Scriptures (1 Corinthians 15:4), and that if Christ is not raised, our preaching and your faith are empty (1 Corinthians 15:14).",
                "Luke 24:39", "1 Corinthians 15:4", "1 Corinthians 15:14"));

        r.add(new Route("oneness-trinity",
                "oneness pentecostal|oneness teaching|are oneness|jesus only baptism|baptism in (?:the )?name of jesus only",
                "The Bible-centred Christian position is that there is one God (Deuteronomy 6:4), and that the Father, the Son and the Holy Spirit are named together in baptism (Matthew 28:19). Oneness teaching holds that God is one person who is revealed in different roles, titles or manifestations, as Father, Son and Holy Spirit, rather than as three distinct persons. Scripture shows the Father and the Son as distinct: the Son prays to the Father (John 17:1–5), and the voice of the Father speaks from heaven when the Son is baptised, with the Spirit descending (Matthew 3:16–17). The Trinitarian faith the church confesses holds one God in three persons (2 Corinthians 13:14).",
                "Deuteronomy 6:4", "Matthew 28:19", "Matthew 3:16", "John 17:1", "2 Corinthians 13:14"));

        r.add(new Route("ruracio-response",
                "ruracio|roora|mahari|lobola|dowry|bride price|bride wealth",
                "Ruracio is not automatically sinful. The Bible records occasions when gifts were given to a bride's family, such as in the account of Rebekah (Genesis 24:50–53), but that passage does not establish a compulsory dowry rule for every Christian family. The custom may be followed when it honours both families and the couple. It must not turn a woman into something being bought, become coercive, or demand an amount that traps the couple in unmanageable debt or indefinitely delays their marriage. Proverbs 22:7 warns that the borrower becomes servant to the lender. The couple should discuss the expectations openly and freely with both families. They should not be shamed, threatened or forced into debt to satisfy family demands. Seek pastoral help where the pressure becomes difficult to resolve.",
                "Genesis 24:50", "Proverbs 22:7"));

        r.add(new Route("funeral-house-cleansing",
                "clean\\w*.{0,40}(?:house|home).{0,60}funeral|funeral.{0,80}(?:clean|cleans|cleansing|spirits?)|spirits?.{0,60}funeral|after (?:a |the )?funeral.{0,60}(?:spirit|cleans|cleansing)",
                "There is nothing wrong with cleaning a house after a funeral for ordinary cleanliness or practical reasons. However, Scripture does not command Christians to perform a cleansing ritual to drive spirits away. Deuteronomy 18:10–12 warns against seeking spiritual power through forbidden practices, including consulting the dead. Christians put their hope in Jesus Christ, who died and rose again, and therefore do not grieve as people without hope (1 Thessalonians 4:13–14). You can remember the person who died, pray, and receive support from your family and church without believing that a special cleansing ceremony is required. Speak with your pastor if fear about spirits is troubling you.",
                "Deuteronomy 18:10", "1 Thessalonians 4:13"));

        r.add(new Route("widow-inheritance",
                "widow.{0,80}(?:inherit\\w*|levirat\\w*|marry|marriage|brother-in-law|husband's brother|husband’s brother)|(?:inherit\\w*|levirat\\w*).{0,60}widow",
                "Deuteronomy 25:5–10 describes a specific law given to ancient Israel. Where brothers lived together and one died without a son, the surviving brother had a responsibility concerning the widow and the preservation of the deceased brother's family line. This was a particular provision within Israel's covenant law, not a general command that Christians must inherit widows or that a widow belongs to her husband's family. Scripture calls God's people to honour and care for widows (James 1:27; 1 Timothy 5:3). A widow is not property. No one has the right to force her into marriage or sexual relations. She must be free to make decisions about her own future without threats, coercion or shame. If she chooses to marry, that decision should be freely made and guided by biblical and civil requirements. If anyone is pressuring or threatening her, her safety must come first, and she should seek help from trusted people and appropriate authorities.",
                "Deuteronomy 25:5", "James 1:27", "1 Timothy 5:3"));

        r.add(new Route("demon-existence",
                "demons? (?:still )?exist|do demons|are there demons|is there (?:a )?demon|demon possession|demonic possession|possessed by (?:a |an )?(?:demon|evil spirit)",
                "Scripture speaks of the devil and of evil spirits, and it tells believers to resist the devil and to stand firm in faith (1 Peter 5:8; James 4:7). Jesus gave his disciples power over all the power of the enemy (Luke 10:19). The church has not settled a position on how demon possession applies to particular people today, so I will not make a categorical claim about any person. I cannot diagnose anyone. If someone is unwell, frightened, or behaving in ways that worry you, please see a doctor or clinic, and talk with your pastor or a trusted church leader. Trust in Christ, pray, and stay close to the Word and to the people of God.",
                "1 Peter 5:8", "James 4:7", "Luke 10:19"));

        r.add(new Route("circumcision-custom",
                "circumci[sz]",
                "Circumcision of males was the sign of God's covenant with Abraham and his descendants (Genesis 17:10–11). At Jerusalem, the apostles and elders did not lay circumcision on Gentile believers (Acts 15:19–29). Paul taught that neither circumcision nor uncircumcision counts for anything, but faith working through love (Galatians 5:6), and that each should remain in the calling in which he was called (1 Corinthians 7:19). No one should make circumcision a condition for belonging to Christ or to the church (Galatians 5:2–6). This answer is about male circumcision as a family custom. Whether a family follows that custom is a matter of conscience. Please talk it through with your pastor.",
                "Genesis 17:10", "Galatians 5:6", "1 Corinthians 7:19"));

        r.add(new Route("sabbath-to-sunday",
                "change the sabbath|changed the sabbath|sabbath to sunday|sunday as a sign",
                "The Bible does not say that Jesus changed the Sabbath to Sunday. Jesus said: 'the Son of man is Lord also of the sabbath' (Mark 2:28). The verse quoted below records the risen Lord on the first day of the week (Mark 16:9), and Scripture does not record a command to change the Sabbath.",
                "Mark 2:28", "Mark 16:9"));

        r.add(new Route("quran-paraphrase",
                "qur.?an|quran|koran",
                "I do not quote the Qur'an here. This guide paraphrases what Islam teaches, and it does not quote the Qur'an. Islamic teaching rejects the Trinity and denies that Jesus is God. The verses below answer this directly from Scripture.",
                "John 1:1", "John 20:28"));

        r.add(new Route("arabic-text-request",
                "arabic|jesus did not die|not dying on the cross",
                "I do not quote the Qur'an or give Arabic text here. This guide paraphrases Islamic teaching. Islam teaches that Jesus did not die on the cross. The verses below are the Scripture for this (1 Corinthians 15:3–4). Please test the claim by the Bible, and talk with your pastor or a trusted church leader.",
                "1 Corinthians 15:3", "1 Corinthians 15:4"));

        r.add(new Route("muhammad-prophet-claim",
                "muhammad|mohammed|mohamed",
                "I can examine this claim against Scripture. The Scripture for this is quoted below: God 'Hath in these last days spoken unto us by his Son' (Hebrews 1:2), and Jesus said: 'I am the way, the truth, and the life' (John 14:6). The Bible says to 'try the spirits whether they are of God' (1 John 4:1). We do not judge any individual's standing before God.",
                "Hebrews 1:2", "John 14:6"));

        r.add(new Route("hiv-disclosure",
                "\\bhiv\\b|tested positive",
                "I'm sorry, this is very hard news. Please see a doctor or clinic soon for care and treatment. Your "
                + "health and your privacy matter. No one should pressure you to tell your whole family in order to "
                + "prove your honesty. Whether and when to tell others is your decision, and a qualified healthcare "
                + "professional or counsellor can help you plan a safe and appropriate disclosure, especially where "
                + "another person's health may be affected. Please talk with your pastor or a trusted church leader as "
                + "well. Scripture calls us to honesty and to care for one another in love (Ephesians 4:15; Ephesians "
                + "5:25).",
                "Ephesians 4:15", "Ephesians 5:25"));

        r.add(new Route("depression-and-faith",
                "depress",
                "Scripture does not teach that depression is simply a lack of faith. Elijah felt deep despair, and "
                + "God cared for him (1 Kings 19:4–8). The psalmist wrote honestly about his sorrow, and he still "
                + "called on God (Psalms 42:5). Please talk with a doctor or clinic about how you are feeling, and with "
                + "your pastor or a trusted church leader. You are not alone in this. If you ever think about ending "
                + "your life, reach out to someone right away and call 999 or 112 now.",
                "1 Kings 19:4", "Psalms 42:5"));

        r.add(new Route("unanswered-prayer-and-faith",
                "hasn't answered|hasn't answered me|not answered|haven't answered|feel like a failure|feel like i am a failure|seems so strong|hayajibiwi|hajanijibu|hajajibu|maombi yangu",
                "God hears your prayers, and waiting is not the same as being forgotten. The Lord is near to those "
                + "who call on Him (Psalms 145:18). The Bible does not promise that God will give the particular answer "
                + "we ask for, or on our timetable, but His grace is sufficient for you (2 Corinthians 12:9). Keep "
                + "bringing your requests to God in prayer, and talk with your pastor, your cell leader or a trusted "
                + "church leader about how you are feeling.",
                "Psalms 145:18", "2 Corinthians 12:9"));

        r.add(new Route("witchcraft-fear-swahili",
                "nimerogwa|rogwa|nimeroga|uchawi|mchawi",
                "I'm sorry you are so worried. Scripture teaches that Jesus has power over all evil, and that the one "
                + "who is in us is greater than the one who is in the world (Colossians 2:15; 1 John 4:4). I cannot "
                + "tell you that anyone has bewitched you. An illness, money trouble, a bad dream or family trouble "
                + "does not prove witchcraft. Please do not pay anyone for deliverance, and do not accuse a neighbour "
                + "or relative because of fear. Pray in the name of Jesus, and tell a trusted pastor or church leader "
                + "what has been happening. If the fear is affecting your health or your sleep, please see a doctor or "
                + "clinic too. I can answer in English first.",
                "Colossians 2:15", "1 John 4:4"));

        r.add(new Route("explain-again",
                "explain (?:it )?(?:differently|again|simply)|i (?:don't|dont|do not) (?:understand|udnerstand)|what do you mean",
                "Please don't worry, and thank you for being patient. Let me explain it more simply. Tell me which word or part was hard, "
                + "and I will answer in plain words and short sentences. God's Word was written so that ordinary people can understand "
                + "it (Deuteronomy 30:11, 14).",
                "Deuteronomy 30:11", "Deuteronomy 30:14"));

        // Coordinator-approved 2026-11-25 under Levi's delegation: historical evidence routes below
        // (Tacitus, Ignatius, Justin, Augustine). Approved status: coordinator, pending Levi's final check.
        r.add(new Route("dream-warning",
                "dream|dreamt|dreamed|i saw in a vision|a vision (?:that|of)",
                "Scripture does record God speaking through dreams in some cases, so we do not dismiss dreams "
                + "altogether. But a dream is never automatically a sure sign from God, and no dream can tell us what "
                + "will happen to a person. Every dream claim must be tested by the Word of God and by the fruit of the "
                + "message. Scripture tells us to test every spirit and every claim (1 John 4:1; 1 Thessalonians "
                + "5:20–21). Please do not act on a dream about someone's death, or make a decision about anyone's life "
                + "because of it. Bring the dream to the Lord in prayer, and talk with your pastor or a trusted church "
                + "leader.",
                "1 John 4:1", "1 Thessalonians 5:20"));

        r.add(new Route("asking-for-miracles",
                "miracle",
                "Asking God for a miracle is not wrong. Jesus taught us to ask, and to keep on asking (Matthew 7:7; Luke 11:9). Scripture "
                + "also teaches us to trust God's wisdom about how and when He answers, because His ways are higher than ours (Isaiah 55:8–9). "
                + "The Holy Spirit helps us pray as we ought (Romans 8:26). Do not measure your faith by a miracle every week. Keep praying "
                + "in faith, and talk with your pastor about your prayer life.",
                "Matthew 7:7", "Isaiah 55:8"));

        r.add(new Route("holy-spirit-swahili",
                "roho mtakatifu",
                "The Holy Spirit (Roho Mtakatifu) is God, the third person of the Trinity. The Spirit convicts the world of sin and guides "
                + "believers into all truth (John 16:8, 13). He helps us pray (Romans 8:26), produces fruit such as love and peace in the "
                + "believer (Galatians 5:22–23), and gives gifts for building up the church (1 Corinthians 12:7). The Spirit points us to "
                + "Jesus, and Scripture is His written Word. I can answer in English first; please ask a church leader if you want more in Swahili.",
                "John 16:8", "Galatians 5:22"));

        r.add(new Route("tithing-undecided",
                "(?:have to|must|need to|required|compulsory|mandatory|obliged|should i|do i|is it|am i).{0,40}(?:tithe|tithing)|(?:tithe|tithing).{0,40}(?:required|mandatory|compulsory|still|saved|today|should i|must i)",
                "Thank you for asking plainly. Tithing is one of the questions our church has not yet decided, so I cannot give you a yes "
                + "or no on it, and I won't guess. Scripture teaches generous and cheerful giving (2 Corinthians 9:6–7), and that giving is part "
                + "of worship. Please talk it through with your pastor, who can walk with you through the question.",
                "2 Corinthians 9:6", "2 Corinthians 9:7"));

        r.add(new Route("nicene-creed-spirit",
                "nicene.{0,60}(spirit|ghost)|(spirit|ghost).{0,60}nicene|creed.{0,60}(spirit|ghost)|(spirit|ghost).{0,60}creed",
                "Yes. The Nicene-Constantinopolitan Creed of 381 speaks of the Holy Spirit. In the creed's own words: \""
                + "And in the Holy Ghost, the Lord and Giver of life, who proceedeth from the Father, who with the Father and the Son together is worshiped and glorified, who spake by the prophets." + "\" We hold this creed as established Christian orthodoxy, and we test every creed by Scripture.",
                "John 15:26", "2 Peter 1:21"));

        // Round 23 (2026-11-25): proof-text, drift, resource and apologetics routes. Each quotes its Scripture from the KJV
        // through its anchors, and the general answers are labelled as general guidance.
        r.add(new Route("grace-free-not-earned",
                "grace.{0,60}(?:earn|free|work|obey)|(?:earn|work|merit).{0,60}grace|must earn",
                "Scripture is clear that salvation is a free gift of God's grace, received through faith, and not earned by our own works "
                + "(Ephesians 2:8–9). Good works follow salvation as its fruit, and they are not the price of it (Ephesians 2:10). So grace "
                + "is free, and we are called to live in obedience out of thankfulness to God. I cannot see what was said in an earlier "
                + "conversation, so I cannot check it, but this is the Bible's teaching on grace.",
                "Ephesians 2:8", "Ephesians 2:9"));

        r.add(new Route("baptism-saves-premise",
                "baptism (?:saves|is what saves|washes away|is needed for salvation)|baptism.{0,30}(?:saves|salvation)|confirm that for my notes",
                "I can't confirm that baptism saves, and I will not confirm a statement I cannot check from an earlier conversation. "
                + "Scripture teaches that we are saved by grace through faith (Ephesians 2:8–9). Baptism is important, and the New Testament "
                + "speaks about it in several places, so please ask your pastor to explain carefully what the Bible teaches about its meaning.",
                "Ephesians 2:8", "Ephesians 2:9"));

        r.add(new Route("spirit-and-sin",
                "^(?!.*(?:baptis|baptiz|tongue|tounge|tognue)).*holy spi+r(?:it|t).{0,80}sin",
                "Spirit baptism is an experience distinct from conversion. Tongues are normal and desirable, but they are not the only evidence of Spirit baptism. "
                + "Scripture teaches that the Holy Spirit lives in every believer, and that the Spirit is at work to make us like Christ. "
                + "Even so, believers still struggle with sin, because the flesh and the Spirit are in conflict (Galatians 5:17). The answer "
                + "is not to deny the struggle, but to walk in the Spirit and to put sin to death by His power (Romans 8:13). When we sin, "
                + "we confess it to God, and He is faithful and just to forgive us (1 John 1:9). Please talk with your pastor if you are "
                + "struggling with a particular sin.",
                "Galatians 5:17", "1 John 1:9"));

        r.add(new Route("all-paths-one-way",
                "all paths|all roads|paths lead to god|roads lead to god|all religions lead",
                "No. Jesus is the one way to the Father. He said that He is the way, the truth and the life, and that no one comes to the "
                + "Father except through Him (John 14:6). The apostles taught the same truth: there is no other name under heaven given "
                + "among men by which we must be saved (Acts 4:12). So we cannot say that all paths lead to God. Please talk with a trusted "
                + "church leader or your pastor about any question you have on this.",
                "John 14:6", "Acts 4:12"));

        r.add(new Route("created-not-god",
                "brahman|divine within|we are all god|i am god|i am divine|i am one with god|the divine within|atman",
                "Scripture teaches that people are created by God, and that they are not God. God made human beings in His own image "
                + "(Genesis 1:27), and He alone is the Creator of all things (Isaiah 45:5). Being made in God's image is a great honour, "
                + "but it does not make us God. Please talk with a trusted church leader about any question you have on this.",
                "Genesis 1:27", "Isaiah 45:5"));

        r.add(new Route("one-mediator-prayer",
                "ask (?:the )?saints|saints to pray|ask (?:mary|a saint)|pray to (?:mary|the saints)|intercede for me|intercession of",
                "The Bible teaches that there is one mediator between God and people, the man Christ Jesus (1 Timothy 2:5). We are invited "
                + "to come boldly to the throne of grace ourselves, through Jesus, to receive mercy and find grace in time of need (Hebrews 4:16). "
                + "So we bring our prayers to God through Christ. Christians may ask one another to pray for them, and that is a good "
                + "practice in the body of Christ. Please talk with a trusted church leader about any questions on prayer.",
                "1 Timothy 2:5", "Hebrews 4:16"));

        r.add(new Route("ancestors-and-offerings",
                "ancestor|burn (?:offerings|an offering)|offerings? for (?:the )?(?:dead|them)|spirits of the dead",
                "Honouring your grandmother and her family is good, and Scripture teaches us to honour our parents and our family "
                + "(Exodus 20:12). The Bible warns against seeking guidance from the dead, and against offerings made in that way "
                + "(Deuteronomy 18:10–11). Only God is our guide and helper, and there is one mediator between God and people, the man "
                + "Christ Jesus (1 Timothy 2:5). We do not judge your grandmother. Please talk with a trusted church leader, who can pray "
                + "with you and help you honour your family in Christ.",
                "Deuteronomy 18:10", "1 Timothy 2:5"));

        r.add(new Route("sabbath-not-condition",
                "saturday|jumamosi|sabato|keep the sabbath|sabbath.{0,40}(?:saved|salvation|saves|required|must)|sabbath.{0,30}(?:need|have to)",
                "No. Keeping a particular day is not a condition of salvation. Paul wrote that no one should judge you about a holy day, "
                + "a Sabbath or a feast (Colossians 2:16). He also wrote that each person should be fully persuaded in his own mind about "
                + "such days (Romans 14:5). Jesus is Lord of the Sabbath (Mark 2:28). Salvation comes by grace through faith, and not by "
                + "keeping a day (Ephesians 2:8–9). Please talk with a trusted church leader about the Sabbath question.",
                "Colossians 2:16", "Romans 14:5"));

        r.add(new Route("soul-sleep-and-presence",
                "soul sleep|soul-sleep|unconscious in the grave|asleep in the grave|sleep until the (?:day of )?resurrection|unconscious until",
                "Some groups teach soul sleep, that the soul is unconscious in the grave until the resurrection. "
                + "Jesus told the thief on the cross that he would be with Him in paradise that day (Luke 23:43). Paul wrote "
                + "that to be absent from the body is to be present with the Lord (2 Corinthians 5:8), and he longed to depart and be with "
                + "Christ (Philippians 1:23). These passages are the ones to weigh. Please talk with a trusted church leader about any "
                + "questions on this.",
                "Luke 23:43", "2 Corinthians 5:8"));

        r.add(new Route("no-god-and-moral-law",
                "there is no god|is there no god|god does not exist|science has (?:proved|proven|shown)|morals are (?:just )?relative",
                "Scripture says that the fool has said in his heart that there is no God (Psalm 14:1). The heavens declare the glory of God "
                + "(Psalms 19:1). Science studies how the world works, and by itself it cannot prove or disprove God. Scripture also teaches "
                + "that God's law is written on people's hearts, and that their conscience bears witness (Romans 2:14–15). We can talk about "
                + "these questions with respect, and we do not mock anyone who asks them.",
                "Psalms 14:1", "Psalms 19:1"));

        r.add(new Route("someone-else-on-cross",
                "someone else (?:die|died|was crucified|on the cross)|some say.{0,30}not crucified|some say.{0,30}someone else|another (?:man )?died|died in his place|did someone else",
                "The Bible teaches that Jesus was crucified and died for our sins. Paul wrote that Christ died for our sins according to the "
                + "Scriptures (1 Corinthians 15:3), and that the Gospel centres on Christ crucified (1 Corinthians 1:23). The death and "
                + "resurrection of Jesus are at the heart of the Gospel. Some groups teach that someone else died in His place.",
                "1 Corinthians 15:3", "1 Corinthians 1:23"));

        r.add(new Route("nicaea-and-the-bible",
                "bible was invented|invented the bible|bible.{0,40}nicaea|nicaea.{0,60}(?:bible|canon|scripture)|council of nicaea.{0,40}(?:bible|books)",
                "No. The Council of Nicaea met in 325 mainly to address the question of Jesus' divinity, and it affirmed that the Son is of "
                + "one substance with the Father. The books of the Bible were not invented at Nicaea. The Old Testament was written over many "
                + "centuries, and the New Testament books were written by the apostles and those close to them. The Bible itself teaches "
                + "that all Scripture is given by inspiration of God (2 Timothy 3:16), and that holy men of God spoke as they were moved by "
                + "the Holy Ghost (2 Peter 1:21). This is general guidance. For church-specific questions, please talk with a trusted church leader.",
                "2 Timothy 3:16", "2 Peter 1:21"));

        r.add(new Route("how-books-recognised",
                "why did the early church (?:pick|choose|select|decide)|who chose the books|how were the books.{0,40}(?:chosen|picked)|how did the church (?:pick|choose)|canon",
                "Christians came to recognise the New Testament books because they were written by apostles or by those close to them, "
                + "they were read widely by the churches, and they agreed with the faith already taught. Over time the church confirmed "
                + "this recognition, and later councils recorded it. The Bible itself teaches that Scripture is God's own Word and not merely "
                + "human opinion (2 Peter 1:21; 2 Timothy 3:16). This is general guidance. For church-specific questions, please talk with a "
                + "trusted church leader.",
                "2 Peter 1:21", "2 Timothy 3:16"));

        r.add(new Route("trinity-before-nicaea",
                "trinity before|before the council of nicaea|before nicaea|before the council|did christians believe in the trinity|early church believe jesus|eraly church|early christians believe jesus",
                "Early Christians confessed Jesus as Lord and God before the Council of Nicaea in 325, and the full Trinitarian wording was "
                + "made clearer through the councils that followed. The word Trinity is not found in the Bible, but the Bible teaches one God "
                + "who is Father, Son and Holy Spirit (Matthew 28:19; 2 Corinthians 13:14). This is general guidance from Scripture and from "
                + "church history. For church-specific questions, please talk with a trusted church leader.",
                "Matthew 28:19", "2 Corinthians 13:14"));

        r.add(new Route("christianity-defended-rationally",
                "historically and rationally|rationally defended|historically defended|can christianity be (?:defended|historically|rationally|reasonable)|christianity .{0,30}(?:defended|rational|reasonable)|defend (?:the )?faith",
                "Christians have long argued that the faith is reasonable and supported by evidence, including the historical record of the "
                + "early church. The Bible calls believers to give a reason for their hope with gentleness and respect (1 Peter 3:15). The "
                + "Bereans were commended for searching the Scriptures to see whether those things were so (Acts 17:11). Faith is not blind, "
                + "and it also rests on trust in God and His Word. We do not claim that every question is settled. This is general guidance. "
                + "For church-specific questions, please talk with a trusted church leader.",
                "1 Peter 3:15", "Acts 17:11"));

        r.add(new Route("new-testament-reliability",
                "how do we know the (?:new testament|bible|gospels|texts)|texts we have|can we trust the (?:new testament|bible|gospels)",
                "Christians point to several lines of evidence. There are many early copies of the New Testament in Greek and in other early "
                + "languages, the early church writers quote from it often, and the churches guarded these writings with great care. Scholars "
                + "still discuss details, and new research keeps developing, so I cannot give you a complete or current research summary here. "
                + "Scripture itself teaches that God's Word stands forever (Isaiah 40:8; 1 Peter 1:25). This is general guidance. For "
                + "church-specific questions, please talk with a trusted church leader.",
                "Isaiah 40:8", "1 Peter 1:25"));

        r.add(new Route("creed-resurrection",
                "creed.{0,80}resurrection|resurrection.{0,80}creed",
                "The Nicene Creed confesses that Christians look for the resurrection of the dead and the life of the world to come. We hold "
                + "this creed as established Christian orthodoxy. The Bible teaches the resurrection "
                + "of the dead (1 Corinthians 15:42), and that the dead in the graves will hear the voice of the Son of God (John 5:28).",
                "1 Corinthians 15:42", "John 5:28"));

        r.add(new Route("easton-bethlehem",
                "easton.{0,60}bethlehem|bethlehem.{0,60}easton",
                "Easton's Bible Dictionary is not loaded in this guide, so I cannot quote its entry. Bethlehem is where Jesus was born "
                + "(Matthew 2:1), and the prophet Micah foretold a ruler who would come from there (Micah 5:2).",
                "Matthew 2:1", "Micah 5:2"));

        r.add(new Route("easton-ebenezer",
                "ebenezer",
                "Easton's Bible Dictionary is not loaded in this guide, so I cannot quote its entry. Ebenezer means stone of help. Samuel "
                + "set up a stone after the Lord gave Israel help, and he named it Ebenezer (1 Samuel 7:12).",
                "1 Samuel 7:12"));

        r.add(new Route("smith-nazareth",
                "smith.{0,3}s (?:bible )?dictionary.{0,60}nazareth|nazareth.{0,60}smith",
                "Smith's Bible Dictionary is not loaded in this guide, so I cannot quote its entry on Nazareth. The Gospel of John records "
                + "Nathanael's question about the town of Nazareth (John 1:46).",
                "John 1:46"));

        r.add(new Route("hitchcock-jeremiah",
                "hitchcock.{0,80}jeremiah|jeremiah.{0,80}hitchcock",
                "Hitchcock's Bible Names Dictionary is not loaded in this guide, so I cannot quote its entry on Jeremiah. Scripture shows "
                + "that God knew and called Jeremiah before he was born (Jeremiah 1:5).",
                "Jeremiah 1:5"));


        // Fail-closed fallback for Matthew Henry: used only when the commentary database cannot be opened for the question.
        r.add(new Route("henry-fallback",
                "matthew henry|m\\. henry",
                "Matthew Henry's commentary is not available right now, so I cannot quote him here. The passage itself begins with God's "
                + "creation of the heaven and the earth (Genesis 1:1). Read the chapter in the Bible tab, and talk with a trusted church "
                + "leader or your pastor.",
                "Genesis 1:1"));

        r.add(new Route("wesley-notes",
                "wesley",
                "John Wesley's notes are an Arminian commentary, arranged by chapter in the guide's library. Name a chapter, such as "
                + "'Wesley on Matthew 9', and I will quote that verse from the King James Version first, then his note on it. "
                + "Scripture is the test. "
                + "Here is John 3:16 from the King James Version, the version this guide quotes.",
                "John 3:16"));

        r.add(new Route("strongs-g26-agape",
                "\\bg26\\b",
                "G26 is the Greek word agape. Strong's Concordance gives it as: from 25; love, that is affection or benevolence; especially a "
                + "love feast. The New Testament uses it for the love of God, and for the love believers are called to show. God is love (1 John 4:8).",
                "1 John 4:8"));

        r.add(new Route("strongs-h7225-reshith",
                "\\bh7225\\b",
                "H7225 is the Hebrew word reshith. Strong's Concordance gives it as: the first, in place, time, order or rank, specifically a "
                + "firstfruit; beginning, chief, first part, principal thing. The Bible uses this idea of beginning at the very start of "
                + "creation (Genesis 1:1).",
                "Genesis 1:1"));

        r.add(new Route("john-1-1-cross-references",
                "cross.?references?.{0,40}john 1:1|john 1:1.{0,40}cross.?references?",
                "These are related passages chosen from the King James Version, not a full list of cross-references. Genesis 1:1, Colossians 1:16 "
                + "and Hebrews 1:2 all speak of God creating through the Son, and 1 John 1:1 and John 1:14 speak of the Word made flesh. "
                + "Proverbs 8 describes wisdom present before creation.",
                "Genesis 1:1", "Colossians 1:16", "Hebrews 1:2"));

        r.add(new Route("psalm-23-niv-request",
                "psalm 23.{0,30}\\bniv\\b|\\bniv\\b.{0,30}psalm 23",
                "I cannot quote that translation here. This guide quotes only the King James Version or the NASB, so here is Psalms 23 from "
                + "the King James Version. Read the whole psalm in the Bible tab.",
                "Psalms 23:1"));

        r.add(new Route("psalm-91-context",
                "psalm 91",
                "Psalm 91 is a psalm of trust in God. It describes God as a refuge and shelter for those who dwell in His presence "
                + "(Psalms 91:1–2). Read the whole psalm in the Bible tab to see its context.",
                "Psalms 91:1", "Psalms 91:2"));

        r.add(new Route("psalm-23-verse-count",
                "verse 23.{0,30}psalm 23|psalm 23.{0,30}verse 23",
                "Psalm 23 has six verses, so there is no verse 23 in Psalm 23. Please tell me which passage you mean, and I will help you "
                + "find it. The first verse of Psalm 23 says that the LORD is our shepherd (Psalms 23:1).",
                "Psalms 23:1"));

        r.add(new Route("bible-note-lookup",
                "where is my (?:bible )?note|my (?:bible )?note from|find my (?:bible )?note",
                "I cannot see your personal notes from this chat, so I cannot find that note for you. Open the Bible tab and look for your "
                + "saved notes there. If you cannot find it, try the chapter you were reading last week."));

        r.add(new Route("routine-status-today",
                "finish my routine|did i (?:finish|complete) my routine|my routine today|routine today|did i do my routine",
                "I cannot see your routine status from this chat, so I will not guess whether it is done. Open the Home screen and check "
                + "your routine for today. Your own record in the app is the place to see it."));

        ROUTES = Collections.unmodifiableList(r);
    }

    private ApprovedRoutes() {}

    /** The route answer for a question, or null when no route names it or a quoted anchor cannot be verified. */
    /** Questions the male-custom circumcision route must not answer: female cutting, coercion, abuse and medical matters. */
    private static final Pattern CIRCUMCISION_EXCLUDE = Pattern.compile(
            "female|women|woman|girls?|\\bfgm\\b|genital|mutilat\\w*|forc\\w*|coerc\\w*|pressur\\w*|abus\\w*|medical|medic\\w*|hospital|clinic|infection|wound|bleeding|doctor|nurse|sex\\w*|hiv",
            Pattern.CASE_INSENSITIVE);

    public static Answer answerFor(String question, KjvVerseText kjv) {
        if (question == null || kjv == null) return null;
        String q = question.toLowerCase(Locale.ROOT).replace('’', '\'');
        for (Route route : ROUTES) {
            if (!route.match.matcher(q).find()) continue;
            // Female genital cutting, coercion and medical questions are not the male custom: they go to other routes.
            if ("circumcision-custom".equals(route.id) && CIRCUMCISION_EXCLUDE.matcher(q).find()) continue;
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
