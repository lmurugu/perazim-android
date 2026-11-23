"""Road to Jerusalem stages. Each stage: question, answer, three wrong options, KJV reference, explanation.
Fields are separated by '~' in the generated Java bank, so no field may contain '~'."""

EASY = [
    ("Each year Jesus's parents went up to which city for the Passover?", "Jerusalem", "Nazareth", "Bethlehem", "Capernaum", "Luke 2:41", "Every year his parents travelled to Jerusalem for the feast."),
    ("In which town was Jesus born?", "Bethlehem", "Nazareth", "Jericho", "Capernaum", "Matthew 2:1", "Jesus was born in Bethlehem of Judaea."),
    ("In which town did Joseph and Mary settle after returning from Egypt?", "Nazareth", "Bethlehem", "Cana", "Jericho", "Matthew 2:23", "He came and dwelt in a city called Nazareth."),
    ("A blind man sat begging near which city?", "Jericho", "Jerusalem", "Samaria", "Damascus", "Luke 18:35", "He sat by the road as Jesus came near Jericho."),
    ("Which chief tax collector climbed a sycamore tree to see Jesus?", "Zacchaeus", "Matthew", "Levi", "Nicodemus", "Luke 19:2", "Zacchaeus was the chief among the publicans."),
    ("Saul was journeying toward which city when a light shone from heaven?", "Damascus", "Antioch", "Samaria", "Gaza", "Acts 9:3", "As he journeyed, he came near Damascus."),
    ("Jesus needed to pass through which region on his way?", "Samaria", "Decapolis", "Idumaea", "Perea", "John 4:4", "And he must needs go through Samaria."),
    ("Joseph went up from Galilee out of which city?", "Nazareth", "Bethlehem", "Capernaum", "Jericho", "Luke 2:4", "Joseph went up from Galilee, out of the city of Nazareth."),
    ("After the feast, where did the boy Jesus stay behind?", "Jerusalem", "Nazareth", "Bethany", "Galilee", "Luke 2:43", "The child Jesus tarried behind in Jerusalem."),
    ("Jesus went up to Jerusalem at which feast of the Jews?", "Passover", "Pentecost", "Tabernacles", "Purim", "John 2:13", "The Jews' passover was at hand, and Jesus went up to Jerusalem."),
    ("Jesus told the disciples not to depart from where?", "Jerusalem", "Galilee", "Bethany", "Samaria", "Acts 1:4", "They were to wait in Jerusalem for the promise of the Father."),
    ("Near which village did Jesus send two disciples for a colt?", "Bethphage", "Bethlehem", "Emmaus", "Cana", "Mark 11:1", "They drew nigh unto Bethphage and Bethany, at the mount of Olives."),
]

MEDIUM = [
    ("How far was Emmaus from Jerusalem?", "Threescore furlongs", "Fifteen furlongs", "A sabbath day's journey", "Two hundred furlongs", "Luke 24:13", "Emmaus was about sixty furlongs from Jerusalem."),
    ("How far was Bethany from Jerusalem?", "About fifteen furlongs", "About a sabbath day's journey", "About sixty furlongs", "About a hundred furlongs", "John 11:18", "Bethany was about fifteen furlongs off."),
    ("Mount Olivet is how far from Jerusalem?", "A sabbath day's journey", "Fifteen furlongs", "Threescore furlongs", "A day's walk on foot", "Acts 1:12", "It was a sabbath day's journey from the city."),
    ("Moses and Elias spoke of Jesus' decease to be accomplished where?", "Jerusalem", "Galilee", "Sinai", "Hebron", "Luke 9:31", "They spoke of his decease which he should accomplish at Jerusalem."),
    ("Jesus taught in the cities and villages, journeying toward which city?", "Jerusalem", "Capernaum", "Nazareth", "Bethany", "Luke 13:22", "He went through the cities teaching, journeying toward Jerusalem."),
    ("On his way to Jerusalem, Jesus passed through the midst of which two regions?", "Samaria and Galilee", "Judaea and Samaria", "Galilee and Perea", "Decapolis and Galilee", "Luke 17:11", "He passed between Samaria and Galilee."),
    ("Which road did the angel send Philip down?", "The road from Jerusalem to Gaza", "The road from Jericho to Jerusalem", "The road from Joppa to Lydda", "The road from Damascus to Antioch", "Acts 8:26", "The way that goeth down from Jerusalem unto Gaza."),
    ("As Jesus went out of which city, blind Bartimaeus called to him?", "Jericho", "Bethany", "Jerusalem", "Nazareth", "Mark 10:46", "As he went out of Jericho, blind Bartimaeus sat by the highway."),
    ("The man in the parable went down from Jerusalem to which city?", "Jericho", "Bethany", "Emmaus", "Samaria", "Luke 10:30", "A certain man went down from Jerusalem to Jericho."),
    ("After baptising the eunuch, Philip was found at which town?", "Azotus", "Gaza", "Joppa", "Lydda", "Acts 8:40", "Philip was found at Azotus."),
    ("Joseph turned aside from Judaea into which region?", "The parts of Galilee", "The region of Samaria", "The land of Moab", "The coast of Tyre", "Matthew 2:22", "He turned aside into the parts of Galilee."),
    ("Which city did Barnabas and Saul return from after fulfilling their ministry?", "Jerusalem", "Antioch", "Tarsus", "Cyprus", "Acts 12:25", "Barnabas and Saul returned from Jerusalem."),
]

HARD = [
    ("Mary and Joseph brought Jesus to Jerusalem to present him to whom?", "The Lord", "The priest", "The temple elders", "Simeon", "Luke 2:22", "They presented him to the Lord."),
    ("What did the crowd take to greet Jesus as he entered Jerusalem?", "Branches of palm trees", "Olive branches", "Garments only", "Lamps", "John 12:13", "They took branches of palm trees and went forth to meet him."),
    ("What did the crowd cry out to Jesus?", "Hosanna to the son of David", "Peace in the highest", "Save us now, King", "Glory to David's house", "Matthew 21:9", "They cried, Hosanna to the son of David."),
    ("Devout men from every nation were dwelling in which city at Pentecost?", "Jerusalem", "Antioch", "Ephesus", "Caesarea", "Acts 2:5", "There were dwelling at Jerusalem Jews, devout men, out of every nation."),
    ("The devil took Jesus to Jerusalem and set him where?", "On a pinnacle of the temple", "On a mountain", "On the wall of the city", "At the well", "Luke 4:9", "He set him on a pinnacle of the temple."),
    ("Repentance and remission of sins were to be preached beginning where?", "Jerusalem", "Galilee", "Bethany", "Samaria", "Luke 24:47", "Beginning at Jerusalem."),
    ("After three years, Paul went up to Jerusalem to see whom?", "Peter", "James", "John", "Barnabas", "Galatians 1:18", "He went up to Jerusalem to see Peter."),
    ("Paul said he went bound in the spirit to which city?", "Jerusalem", "Rome", "Corinth", "Ephesus", "Acts 20:22", "I go bound in the spirit unto Jerusalem."),
    ("Jesus cried over which city, saying it kills the prophets?", "Jerusalem", "Nineveh", "Samaria", "Bethlehem", "Luke 13:34", "O Jerusalem, Jerusalem, thou that killest the prophets."),
    ("Jesus said they were going up to where, to see the prophets fulfilled?", "Jerusalem", "Galilee", "Bethany", "Damascus", "Luke 18:31", "We go up to Jerusalem."),
    ("When Jesus was missing, Mary and Joseph turned back to where?", "Jerusalem", "Nazareth", "Bethlehem", "Capernaum", "Luke 2:45", "They turned back again to Jerusalem, seeking him."),
    ("Great crowds followed Jesus from Galilee, Decapolis, Jerusalem and Judaea, and from beyond which river?", "Jordan", "Nile", "Euphrates", "Kidron", "Matthew 4:25", "They came from beyond Jordan."),
]
