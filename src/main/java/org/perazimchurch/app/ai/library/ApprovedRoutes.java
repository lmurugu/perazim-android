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
                "Romans 1:20", "Psalm 19:1"));

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
                "I cannot judge your medicine from Scripture alone, and I cannot say what is a sin for you personally. Please talk with your "
                + "doctor about your medicine and ask them to explain what it does. Do not stop or start a medicine without your doctor. Pray, "
                + "and talk with your pastor or a trusted church leader too. Scripture calls us to seek wise counsel (Proverbs 11:14; "
                + "Proverbs 15:22).",
                "Proverbs 11:14", "Proverbs 15:22"));

        r.add(new Route("cancer-treatment-and-faith",
                "chemo|cancer|surgery|doctors? (?:say|says|said)",
                "Please keep following your doctors' plan until you have spoken with them. Trusting God and seeking medical care are not "
                + "opposed; Jesus Himself said that the sick need a physician (Luke 5:31). Talk with your doctor, your family and your pastor "
                + "together, and pray for wisdom and peace as you decide.",
                "Luke 5:31", "Proverbs 11:14"));

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
                "I cannot find that saying in the Bible, and Scripture does not teach that God helps those who help themselves. The Bible teaches that we are saved by grace through faith, not by our own works (Ephesians 2:8–9). Proverbs 3:5–6 calls us to trust the LORD with all our heart.",
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
                "Scripture gives the church a real role in caring for its leaders and in dealing with wrongdoing. Matthew 18:15–16 teaches patient, careful steps with witnesses, and Acts 6:3 shows that church leaders should be men of good report. The church's own constitution and leaders' process decides how this is done, and this guide cannot give a verdict on any leader. Please talk with the church office and a trusted church leader.",
                "Matthew 18:15", "Acts 6:3"));

        r.add(new Route("accused-leader-abuse",
                "accused of abuse|leader who is accused|leader .{0,40}accused|handle a leader",
                "A church should take an accusation of abuse seriously. Scripture calls for truth, care for the person who was harmed, and a careful process with witnesses (1 Timothy 5:19; Matthew 18:16). The person harmed should have a trusted adult with them, and the matter should go to the police and the proper authorities.",
                "1 Timothy 5:19", "Matthew 18:16"));

        r.add(new Route("which-leaders-trust",
                "which (?:church )?leaders|can i trust|which pastors|who can i trust",
                "Only God knows hearts and judges them, so I will not name or judge any leader. Test every leader by the Scriptures and by their fruit (Matthew 7:16; 1 John 4:1). Look for leaders who live openly, handle money honestly and accept accountability, and talk with a trusted church leader or the church office before you decide.",
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
                "This guide does not tell anyone who to vote for. Christians may hold different views on candidates and parties, and each person must follow their own conscience before God. Scripture calls us to pray for leaders (1 Timothy 2:1–2), to seek justice and honesty (Micah 6:8), and to ask God for wisdom (James 1:5). Please test each candidate by those standards, and talk with your pastor about how to honour God in your choice.",
                "1 Timothy 2:1", "Micah 6:8"));

        r.add(new Route("welcome-new-believer",
                "nimeamua kumfuata|kumfuata yesu|nimempokea yesu|nimeokoka|decided to follow jesus",
                "Karibu sana! Welcome, and we rejoice with you. The Bible says that if you confess with your mouth that Jesus is Lord, and believe in your heart that God raised Him from the dead, you shall be saved (Romans 10:9). Whoever calls upon the name of the Lord shall be saved (Romans 10:13). Take time to pray and thank God, and ask Him for His peace. Please talk with a pastor or a trusted church leader about baptism and how to grow in your new faith.",
                "Romans 10:9", "Romans 10:13"));

        r.add(new Route("thanks-and-prayer-help",
                "kumshukuru|nimepata job|how do i pray|how to pray|help me pray|what to say to god|heart is heavy|jinsi ya kuomba",
                "Welcome, and thank you for bringing this to God. Thank God for your new job. Pray with thanksgiving, and tell God what is on your heart (Philippians 4:6). God's peace will guard your heart and mind (Philippians 4:7). Scripture says we can pour out our hearts to God (Psalms 62:8), and the Holy Spirit helps us when we do not know what to say (Romans 8:26). A trusted church leader or your pastor can pray with you as well.",
                "Philippians 4:6", "Psalms 62:8"));

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
                "I cannot approve anyone for church leadership. Leadership appointments are decided by the church's leaders through the church "
                + "office, and the Bishop's process is the right route. Please speak with the church office or your pastor about your interest in "
                + "serving. Scripture shows that church leaders are chosen with care and prayer (Acts 6:3), so bring your desire to God in prayer too.",
                "Acts 6:3", "1 Timothy 3:10"));

        r.add(new Route("pope-head-of-church",
                "pope|papal|papacy|vicar of christ",
                "Scripture names Christ as the head of the Church (Ephesians 1:22; Colossians 1:18). We do not accept an earthly head over the whole Church. We test any claim of such a role by Scripture alone, not by tradition or office.",
                "Ephesians 1:22", "Colossians 1:18"));

        r.add(new Route("icons-scripture",
                "icon|icons|orthodox teaching on icons",
                "Scripture forbids making and worshipping images for worship: 'Thou shalt not make unto thee any graven image' (Exodus 20:4), which is the second commandment. God is spirit, and they that worship Him must worship Him in spirit and in truth (John 4:24). We do not use images in worship, and we test any teaching on icons by Scripture alone.",
                "Exodus 20:4", "John 4:24"));

        r.add(new Route("eastern-church-judge",
                "eastern church|eastern orthodox|orthodox church|better than the protestant|better than the catholic",
                "We do not rank churches or judge which is better before God. Only Christ, the head of the Church, judges (Colossians 1:18). The right question is whether a teaching agrees with Scripture, and the Bereans tested what they heard by the Scriptures (Acts 17:11). Please talk this through with a trusted church leader.",
                "Colossians 1:18", "Acts 17:11"));

        r.add(new Route("apocrypha-books",
                "apocrypha|deuterocanon|catholic bible|more books|tobit|maccabees|sirach|judith|baruch",
                "The Protestant Bible has 66 books: 39 in the Old Testament and 27 in the New. The books called the Apocrypha or deuterocanonical books, including Tobit, Judith, Wisdom, Sirach, Baruch and 1 and 2 Maccabees, are not part of the 66-book canon we hold. Scripture closes with a warning not to add to God's words (Revelation 22:18), and Deuteronomy 4:2 says the same. Those books are historically interesting, but we test the canon by Scripture itself.",
                "Revelation 22:18", "Deuteronomy 4:2"));

        r.add(new Route("greeting-only",
                "^(?:hi|hello|hey|good morning|good afternoon|good evening|habari|hujambo)(?:[ ,!.]+(?:good morning|good afternoon|good evening|there|friend))?[ !.]*$",
                "Good morning! Welcome. I can help with the Bible, prayer, church information and the faith questions you bring. What would you like to know? We can start with a short passage: 'This is the day which the LORD hath made; we will rejoice and be glad in it' (Psalms 118:24).",
                "Psalms 118:24", "Psalms 118:24"));

        r.add(new Route("campuses-unverified",
                "campus|campuses",
                "I do not have a verified list of Perazim campuses in this guide, so I cannot confirm them. The approved channels for this are the church office and the church's own published information in the app. Please contact the church office for the current campuses and service times.",
                "Psalms 118:24", "Psalms 118:24"));

        r.add(new Route("giving-options-app",
                "giving options|ways to give|how to give|giving in the app|options in the app",
                "Giving options are shown in the app's giving section, and the church publishes its giving channels. Gifts should go only through published channels, such as M-Pesa Paybill, and you can confirm the current details with the church office. Please never give money to a person who asks for it privately.",
                "Psalms 118:24", "Psalms 118:24"));

        r.add(new Route("god-not-a-man",
                "god was once a man|god was a man|once a man|became god|become god",
                "Scripture says plainly: 'God is not a man, that he should lie' (Numbers 23:19). God is not a man who became God. We test every teaching about God by Scripture alone, and we do not accept a later teaching that adds to what God has revealed.",
                "Numbers 23:19", "Malachi 3:6"));

        r.add(new Route("archangel-michael-jesus",
                "archangel|jude 9|jesus is michael|jesus is the archangel",
                "Scripture does not say that Jesus is the archangel Michael. Jude 9 mentions the archangel Michael, but it does not say that Michael is Christ. The Word is described in John 1:1 as 'In the beginning was the Word, and the Word was with God, and the Word was God', and Colossians 1:16 says 'For by him were all things created'.",
                "John 1:1", "Colossians 1:16"));

        r.add(new Route("jesus-is-the-father",
                "jesus is the father|jesus the father|is jesus the father",
                "Scripture distinguishes the Father and the Son while confessing one God. Jesus said: 'my Father is greater than I' (John 14:28), and He also said: 'I and my Father are one' (John 10:30). The Church confesses one God, with the Father, the Son and the Holy Spirit, as the Nicene Creed of 381 expresses it. Please test this with your pastor and the Bible passages in context.",
                "John 14:28", "John 10:30"));

        r.add(new Route("trinity-not-pagan",
                "trinity.{0,60}pagan|pagan.{0,60}trinity|invented by the church|trinity .{0,30}invented",
                "The Trinity is not a pagan idea. It is taught in the Scripture quoted below: 'Hear, O Israel: The LORD our God is one LORD' (Deuteronomy 6:4), and 'baptizing them in the name of the Father, and of the Son, and of the Holy Ghost' (Matthew 28:19). The word Trinity is a later summary of this teaching, not a new god.",
                "Deuteronomy 6:4", "Matthew 28:19"));

        r.add(new Route("sunday-mark-beast",
                "sunday.{0,40}mark|mark of the beast|ellen white",
                "Scripture does not identify Sunday as the mark of the beast. The mark in Revelation 13 is not named as Sunday in the text. Paul wrote: 'Let no man therefore judge you' (Colossians 2:16), and 'Let every man be fully persuaded in his own mind' (Romans 14:5). We test this teaching by Scripture alone.",
                "Colossians 2:16", "Romans 14:5"));

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
                "I'm sorry, this is very hard news. Please see a doctor or clinic soon for care and treatment, and they can explain your "
                + "health and how to protect the people you love. You do not have to decide alone. Please talk with your pastor or a trusted church "
                + "leader, and with a counsellor, about how and when to tell your family. Scripture calls us to honesty and to care for one "
                + "another in love (Ephesians 4:15; Ephesians 5:25).",
                "Ephesians 4:15", "Ephesians 5:25"));

        r.add(new Route("depression-and-faith",
                "depress",
                "Scripture does not teach that depression is simply a lack of faith. Elijah felt deep despair, and God cared for him "
                + "(1 Kings 19:4–8). The psalmist wrote honestly about his sorrow, and he still called on God (Psalms 42:5). Please talk with a "
                + "doctor or clinic about how you are feeling, and with your pastor or a trusted church leader. You are not alone in this.",
                "1 Kings 19:4", "Psalms 42:5"));

        r.add(new Route("unanswered-prayer-and-faith",
                "hasn't answered|hasn't answered me|not answered|haven't answered|feel like a failure|feel like i am a failure|seems so strong|hayajibiwi|hajanijibu|hajajibu|maombi yangu",
                "God hears your prayers, and waiting is not the same as being forgotten. The Lord is near to those who call on Him "
                + "(Psalms 145:18). Your faith is not measured by how strong other people seem, because God's grace is sufficient for you "
                + "(2 Corinthians 12:9). Keep bringing your requests to God in prayer, and talk with your pastor, your cell leader or a "
                + "trusted church leader about how you are feeling.",
                "Psalms 145:18", "2 Corinthians 12:9"));

        r.add(new Route("witchcraft-fear-swahili",
                "nimerogwa|rogwa|nimeroga|uchawi|mchawi",
                "I'm sorry you are so worried. Scripture teaches that Jesus has power over all evil, and that the one who is in us is "
                + "greater than the one who is in the world (Colossians 2:15; 1 John 4:4). Pray in the name of Jesus, and tell a trusted "
                + "pastor or church leader what has been happening. If the fear is affecting your health or your sleep, please see a doctor "
                + "or clinic too. I can answer in English first.",
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
                "A dream is not a sure sign from God, and no dream can tell us what will happen to a person. Scripture tells us to test "
                + "every spirit and every claim against the Word of God (1 John 4:1; 1 Thessalonians 5:20–21). We do not fear dreams, and we "
                + "do not treat them as settled truth. If a dream troubles you, bring it to the Lord in prayer, ask the Holy Spirit for peace, "
                + "and talk with your pastor or a trusted church leader. Please do not act on a dream about someone's death, or make a "
                + "decision about anyone's life because of it.",
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
