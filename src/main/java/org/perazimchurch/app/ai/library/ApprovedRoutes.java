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
                "Jesus says that not everyone who calls Him Lord will enter the kingdom of heaven, but the one who does the will of His "
                + "Father in heaven (Matthew 7:21). Many will say 'Lord, Lord' and be turned away, because He never knew them (Matthew 7:22–23). "
                + "Calling Him Lord is not enough. Jesus asks: 'why call ye me, Lord, Lord, and do not the things which I say?' (Luke 6:46).\n\n"
                + "This does not mean that we earn salvation. Faith in Christ is what saves, and true faith is shown in obedience that comes "
                + "from it (Ephesians 2:8–10). We do not judge any person's standing before God. Test yourself and every profession by the "
                + "words of Christ, and talk with a trusted church leader or pastor if you are unsure.",
                "Matthew 7:21", "Luke 6:46"));

        r.add(new Route("pharaoh-heart",
                "pharaoh|pharoah",
                "The Bible teaches two truths side by side, and we hold both. Pharaoh hardened his own heart again and again (Exodus 8:15). "
                + "God also hardened Pharaoh's heart, and He told Pharaoh that He had raised him up to show His power and to make His name "
                + "known throughout the earth (Exodus 9:12, 16; Romans 9:17–18).\n\n"
                + "Scripture does not make God the author of sin. Pharaoh was responsible for refusing God's word, and God's purpose was "
                + "carried out through that refusal. We do not claim to explain every mystery. Paul answers the question with a call to "
                + "humility before God (Romans 9:20). Human responsibility and God's sovereignty are both taught in Scripture (Philippians 2:12–13).",
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

        r.add(new Route("hiv-disclosure",
                "\\bhiv\\b|tested positive",
                "I'm sorry, this is very hard news. Please see a doctor or clinic soon for care and treatment, and they can explain your "
                + "health and how to protect your wife. You do not have to decide alone. Please talk with your pastor or a trusted church "
                + "leader, and with a counsellor, about how and when to tell your wife. Scripture calls us to honesty and to care for one "
                + "another in love (Ephesians 4:15; Ephesians 5:25).",
                "Ephesians 4:15", "Ephesians 5:25"));

        r.add(new Route("depression-and-faith",
                "depress",
                "Scripture does not teach that depression is simply a lack of faith. Elijah felt deep despair, and God cared for him "
                + "(1 Kings 19:4–8). The psalmist wrote honestly about his sorrow, and he still called on God (Psalms 42:5). Please talk with a "
                + "doctor or clinic about how you are feeling, and with your pastor or a trusted church leader. You are not alone in this.",
                "1 Kings 19:4", "Psalms 42:5"));

        r.add(new Route("unanswered-prayer-and-faith",
                "hasn't answered|hasn't answered me|not answered|haven't answered|feel like a failure|feel like i am a failure|seems so strong",
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
