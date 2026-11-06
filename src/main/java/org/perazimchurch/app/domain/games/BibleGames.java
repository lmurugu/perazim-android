package org.perazimchurch.app.domain.games;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Offline learning games. Scores belong to these games, never member rewards. */
public final class BibleGames {
    public enum Mode { QUIZ, WHO_AM_I, VERSE_ORDER }

    public static final class Card {
        public final String prompt;
        public final String answer;
        public final String reference;
        public final String explanation;
        public final String hint;
        public final List<String> options;
        private final List<String> accepted;

        private Card(String prompt, String answer, String reference, String explanation,
                     String hint, String options, String aliases) {
            this.prompt = prompt;
            this.answer = answer;
            this.reference = reference;
            this.explanation = explanation;
            this.hint = hint;
            this.options = Collections.unmodifiableList(options.isEmpty()
                    ? new ArrayList<>() : Arrays.asList(options.split("\\|")));
            accepted = new ArrayList<>();
            accepted.add(normalize(answer));
            if (!aliases.isEmpty()) for (String alias : aliases.split("\\|")) accepted.add(normalize(alias));
        }

        public boolean accepts(String value) { return accepted.contains(normalize(value)); }

        public List<String> words() {
            return Collections.unmodifiableList(Arrays.asList(answer.split(" ")));
        }
    }

    /** One attempt per card; revisiting or tapping twice cannot award extra points. */
    public static final class Round {
        private final List<Card> cards;
        private int index;
        private int score;
        private boolean answered;
        private boolean correct;

        public Round(Mode mode, long seed) {
            List<Card> shuffled = new ArrayList<>(cards(mode));
            Collections.shuffle(shuffled, new Random(seed));
            cards = Collections.unmodifiableList(new ArrayList<>(shuffled.subList(0, Math.min(5, shuffled.size()))));
        }

        public Card current() {
            if (finished()) throw new IllegalStateException("Round finished");
            return cards.get(index);
        }
        public int position() { return index + 1; }
        public int size() { return cards.size(); }
        public int score() { return score; }
        public boolean answered() { return answered; }
        public boolean correct() { return correct; }
        public boolean finished() { return index >= cards.size(); }

        public boolean answer(String value) {
            if (finished() || answered) return false;
            correct = current().accepts(value);
            answered = true;
            if (correct) score++;
            return correct;
        }

        public boolean next() {
            if (!answered || finished()) return false;
            index++;
            answered = false;
            correct = false;
            return true;
        }
    }

    /** Word indices preserve duplicate words as separate tiles. */
    public static final class VersePuzzle {
        private final List<String> words;
        private final List<Integer> order = new ArrayList<>();
        private final List<Integer> selected = new ArrayList<>();

        public VersePuzzle(Card card, long seed) {
            words = card.words();
            for (int i = 0; i < words.size(); i++) order.add(i);
            Collections.shuffle(order, new Random(seed));
            if (order.size() > 1 && isOriginalOrder()) Collections.rotate(order, 1);
        }
        private boolean isOriginalOrder() {
            for (int i = 0; i < order.size(); i++) if (order.get(i) != i) return false;
            return true;
        }
        public List<Integer> tiles() { return Collections.unmodifiableList(order); }
        public String word(int id) { return words.get(id); }
        public boolean selected(int id) { return selected.contains(id); }
        public boolean choose(int id) {
            if (id < 0 || id >= words.size() || selected.contains(id)) return false;
            selected.add(id);
            return true;
        }
        public void undo() { if (!selected.isEmpty()) selected.remove(selected.size() - 1); }
        public void clear() { selected.clear(); }
        public boolean complete() { return selected.size() == words.size(); }
        public String sentence() {
            List<String> result = new ArrayList<>();
            for (int id : selected) result.add(words.get(id));
            return String.join(" ", result);
        }
    }

    private static Card c(String q, String a, String ref, String explanation, String hint, String options, String aliases) {
        return new Card(q, a, ref, explanation, hint, options, aliases);
    }
    private static Card v(String answer, String reference, String explanation, String hint) {
        return c("Put the words in order", answer, reference, explanation, hint, "", "");
    }

    public static List<Card> cards(Mode mode) {
        switch (mode) {
            case QUIZ: return QUIZ;
            case WHO_AM_I: return PEOPLE;
            case VERSE_ORDER: return VERSES;
            default: throw new IllegalArgumentException("Unknown game");
        }
    }

    private static final List<Card> QUIZ = Collections.unmodifiableList(Arrays.asList(
        c("Who built the ark?", "Noah", "Genesis 6:13–22", "Noah followed God's instructions to prepare the ark.", "Read Genesis 6.", "Moses|Noah|Abraham|David", ""),
        c("Which town was Jesus born in?", "Bethlehem", "Matthew 2:1", "Matthew places Jesus' birth in Bethlehem of Judea.", "The town is linked to David.", "Nazareth|Jerusalem|Bethlehem|Jericho", ""),
        c("Who interpreted Pharaoh's dreams?", "Joseph", "Genesis 41:14–36", "Joseph explained the dreams and advised preparation for famine.", "He had once been sold by his brothers.", "Daniel|Joseph|Samuel|Aaron", ""),
        c("How many people did Jesus choose as apostles?", "Twelve", "Luke 6:12–16", "After praying, Jesus chose twelve from among his disciples.", "There were also twelve tribes of Israel.", "Seven|Ten|Twelve|Forty", "12"),
        c("Who helped the wounded man in Jesus' parable?", "A Samaritan", "Luke 10:30–37", "The Samaritan showed mercy through practical care. Jesus calls his listeners to do likewise.", "The story challenges assumptions about neighbours.", "A priest|A Levite|A Samaritan|A soldier", "Samaritan"),
        c("What did Jesus use to feed the five thousand?", "Five loaves and two fish", "John 6:9–13", "The small meal was shared, and the remaining pieces filled twelve baskets.", "A boy offered his meal.", "Seven loaves|Five loaves and two fish|Manna|Bread and wine", ""),
        c("Who climbed a sycamore tree to see Jesus?", "Zacchaeus", "Luke 19:1–10", "Jesus noticed Zacchaeus and visited his home, leading to a changed life.", "He was a chief tax collector.", "Peter|Zacchaeus|Nicodemus|Thomas", "Zaccheus"),
        c("Which quality is included in the fruit of the Spirit?", "Patience", "Galatians 5:22–23", "Paul describes the Spirit's fruit through love, joy, peace, patience and other virtues.", "It helps us respond gently when waiting.", "Envy|Pride|Patience|Boasting", ""),
        c("Who asked God for wisdom to lead?", "Solomon", "1 Kings 3:5–12", "Solomon asked for an understanding heart to judge fairly.", "He was David's son.", "Saul|Solomon|Gideon|Ahab", ""),
        c("What did Jesus wash at the Last Supper?", "His disciples' feet", "John 13:3–17", "Jesus modelled humble service and asked his disciples to follow his example.", "A task usually done by a servant.", "The cups|His disciples' feet|The windows|The fishing nets", ""),
        c("Which book describes the early church after Jesus' ascension?", "Acts", "Acts 1:1–11; 2:42–47", "Acts follows the apostles, the Spirit's work and the growing community of believers.", "It follows the four Gospels.", "Proverbs|Acts|Ruth|Exodus", ""),
        c("Who said that faith without works is dead?", "James", "James 2:14–26", "James teaches that genuine faith is expressed in practical action.", "His letter speaks often about daily conduct.", "James|Jonah|Isaiah|Nehemiah", "")
    ));

    private static final List<Card> PEOPLE = Collections.unmodifiableList(Arrays.asList(
        c("I led Israel out of Egypt and stood before Pharaoh. Who am I?", "Moses", "Exodus 3–14", "God called Moses to lead the Israelites out of slavery.", "My brother was Aaron.", "", "Musa"),
        c("I stayed faithful in a lions' den. Who am I?", "Daniel", "Daniel 6", "Daniel continued to pray even when a decree made it dangerous.", "I served in Babylon.", "", ""),
        c("I left my homeland with my mother-in-law Naomi. Who am I?", "Ruth", "Ruth 1:16–17", "Ruth's loyalty brought her to Bethlehem with Naomi.", "A Bible book bears my name.", "", ""),
        c("I was a shepherd who faced Goliath. Who am I?", "David", "1 Samuel 17", "David trusted God when facing the Philistine champion.", "I later became king of Israel.", "", "Daudi"),
        c("I was swallowed by a great fish after fleeing my calling. Who am I?", "Jonah", "Jonah 1–2", "Jonah prayed from inside the fish and was sent again to Nineveh.", "My destination was Nineveh.", "", "Yona"),
        c("I risked approaching the king to plead for my people. Who am I?", "Esther", "Esther 4–7", "Esther acted courageously after asking her community to fast.", "Mordecai was my relative.", "", "Esta"),
        c("I denied knowing Jesus three times, but later helped lead the church. Who am I?", "Peter", "Luke 22:54–62; John 21:15–19", "Peter's story includes failure, forgiveness and renewed service.", "My other name was Simon.", "", "Simon Peter|Simon|Petro"),
        c("I was the mother of Jesus. Who am I?", "Mary", "Luke 1:26–38", "Mary responded in faith to the angel's announcement.", "I lived in Nazareth.", "", "Mariam|Maria"),
        c("I helped rebuild Jerusalem's walls. Who am I?", "Nehemiah", "Nehemiah 2–6", "Nehemiah organised the rebuilding while facing opposition.", "I had served as the king's cupbearer.", "", "Nehemia"),
        c("I travelled widely to share the gospel after once persecuting Christians. Who am I?", "Paul", "Acts 9:1–22", "Paul's encounter with Jesus changed the direction of his life.", "I was also called Saul.", "", "Saul|Paulo"),
        c("I heard God calling my name as a child in the sanctuary. Who am I?", "Samuel", "1 Samuel 3", "With Eli's help, Samuel learned to recognise and answer God's call.", "My mother was Hannah.", "", "Samweli"),
        c("I met Jesus at a well and told my town about him. Who am I?", "The Samaritan woman", "John 4:4–30", "John does not give her name; her testimony led others to meet Jesus.", "The conversation happened in Samaria.", "", "Samaritan woman|Woman at the well|The woman at the well|Samaritan")
    ));

    // Short public-domain KJV passages; these games do not require downloaded translations.
    private static final List<Card> VERSES = Collections.unmodifiableList(Arrays.asList(
        v("The LORD is my shepherd; I shall not want.", "Psalm 23:1 · KJV", "The psalm describes trust in God's care and guidance.", "Begin with 'The LORD'."),
        v("We love him, because he first loved us.", "1 John 4:19 · KJV", "Our love is a response to God's love.", "Begin with 'We love'."),
        v("Let every thing that hath breath praise the LORD.", "Psalm 150:6 · KJV", "The psalm ends by inviting all living things to praise God.", "Begin with 'Let every'."),
        v("Rejoice in the Lord alway: and again I say, Rejoice.", "Philippians 4:4 · KJV", "Paul encourages joy in the Lord even in difficult circumstances.", "The first and last word are the same."),
        v("Be not overcome of evil, but overcome evil with good.", "Romans 12:21 · KJV", "Paul calls believers to answer evil with good.", "Begin with 'Be not'."),
        v("For we walk by faith, not by sight.", "2 Corinthians 5:7 · KJV", "Paul describes trust in God beyond what can currently be seen.", "Begin with 'For we'."),
        v("A soft answer turneth away wrath: but grievous words stir up anger.", "Proverbs 15:1 · KJV", "Gentle speech can calm conflict; harsh speech can inflame it.", "Begin with 'A soft'."),
        v("Thy word is a lamp unto my feet, and a light unto my path.", "Psalm 119:105 · KJV", "The psalm describes God's word as guidance for daily life.", "Begin with 'Thy word'.")
    ));

    public static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N} ]", " ").trim().replaceAll("\\s+", " ");
    }

    private BibleGames() { }
}
