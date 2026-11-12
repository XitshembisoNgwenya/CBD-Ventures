package za.ac.richfield.cbdventures.assistant;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The student assistant's reply engine — a faithful port of the rule-based guide
 * from the CBDVentures Assistant project, minus the optional external AI provider
 * (which needed a server-side API key). Everything here runs offline against the
 * curated directory, so the demo never depends on a third party.
 */
@Service
public class AssistantService {

    /** One chat turn. */
    public record Message(String role, String content) { }

    /** What the client renders: the reply plus the place cards to offer. */
    public record Reply(String reply, List<String> suggestedPlaceIds, String mode, String warning,
                        List<String> suggestedMenuItemIds) {
        public Reply(String reply, List<String> suggestedPlaceIds, String mode, String warning) {
            this(reply, suggestedPlaceIds, mode, warning, List.of());
        }
    }

    private record MenuMatch(AssistantPlace place, AssistantPlace.MenuItem item) { }

    private static final Map<String, Pattern> PATTERNS = new LinkedHashMap<>();
    static {
        PATTERNS.put("food", Pattern.compile(
                "\\b(food|eat|hungry|restaurant|takeaway|lunch|breakfast|coffee|burger|chicken|shawarma|magwinya|bakery|fast food|pizza)\\b", Pattern.CASE_INSENSITIVE));
        PATTERNS.put("groceries", Pattern.compile(
                "\\b(grocer|supermarket|shoprite|woolworths|spar|pick n pay|snack|bread|milk)\\b", Pattern.CASE_INSENSITIVE));
        PATTERNS.put("health", Pattern.compile(
                "\\b(pharmacy|medicine|doctor|clinic|health|clicks|chemist|optometrist)\\b", Pattern.CASE_INSENSITIVE));
        PATTERNS.put("printing", Pattern.compile(
                "\\b(print|photocopy|copy|stationery|assignment|binding|postnet|paper)\\b", Pattern.CASE_INSENSITIVE));
        PATTERNS.put("study", Pattern.compile(
                "\\b(study|studying|library|books|research|quiet|reading|wifi|wi-fi)\\b", Pattern.CASE_INSENSITIVE));
        PATTERNS.put("banking", Pattern.compile(
                "\\b(bank|atm|withdraw|cash|fnb|absa|capitec|nedbank)\\b", Pattern.CASE_INSENSITIVE));
        PATTERNS.put("grooming", Pattern.compile(
                "\\b(barber|haircut|salon|braid|nails|beauty|hairdresser)\\b", Pattern.CASE_INSENSITIVE));
        PATTERNS.put("transport", Pattern.compile(
                "\\b(bus|taxi|rank|transport|commute|train|ride|bus stop|gautrain)\\b", Pattern.CASE_INSENSITIVE));
        PATTERNS.put("fun", Pattern.compile(
                "\\b(fun|entertainment|movie|theatre|theater|show|date|lounge|music|hangout)\\b", Pattern.CASE_INSENSITIVE));
    }

    private static final Pattern GREETING = Pattern.compile(
            "^\\s*(hi|hello|hey|howzit|good morning|good afternoon|good evening|how are you)[!? .]*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern THANKS = Pattern.compile(
            "^\\s*(thanks|thank you|thank u|shot|sharp|cool|okay|ok)[!? .]*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern HELP = Pattern.compile(
            "^\\s*(help|options|features|what can you do|what can this bot do|how does this chatbot work)[!? .]*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FOLLOW_UP = Pattern.compile(
            "\\b(this place|that place|the place|there|it|its|their|the library|that library|this library|tell me more|more about|interesting|special|unique|features|facilities|what can i do|what does it offer|what makes|why visit|what about|is it|can i|does it|how about|what is good|what is nice|first one|second one|third one|option [123]|number [123]|[123](?:st|nd|rd) one)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern INTERESTING = Pattern.compile(
            "\\b(interesting|special|unique|features|facilities|what can i do|what does it offer|what makes|why visit|tell me more|more about|what is good|what is nice)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern NAVIGATION = Pattern.compile(
            "\\b(direction|directions|route|walk|walking|navigate|navigation|where is|how do i get|how can i get|how to get|how do i reach|how can i reach|take me to|get to|go to)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PRICING = Pattern.compile(
            "\\b(price|prices|cost|costs|costing|menu|special|meal deal|discount|afford|affordable|budget|cheap|cheapest|spend|buy|under)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern MONEY = Pattern.compile("\\bR\\s?(\\d{1,5})\\b|\\b(\\d{1,5})\\s?(?:rand|zar)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SERVICE_QUESTION = Pattern.compile(
            "\\b(wifi|wi-fi|internet|computers?|laptop|photocopy|printing|print|books|borrow|quiet|study space|seats?|desks?)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern QUESTION_FORM = Pattern.compile(
            "\\b(does|can|is|are|has|have|there|it|this|that)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRODUCTS = Pattern.compile(
            "\\b(what do they sell|what does it sell|what can i get there|what food|what kind of food|do they sell)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern HOURS = Pattern.compile(
            "\\b(open|opening|close|closing|hours|weekend|saturday|sunday)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern HOURS_FORM = Pattern.compile(
            "\\b(is|does|it|there|what|when|the)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern WEATHER = Pattern.compile("\\b(weather|temperature|rain|forecast)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern NEARBY = Pattern.compile(
            "\\b(near|nearby|closest|places|recommend|find|looking for|suggest|around campus)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ORDINAL = Pattern.compile(
            "\\b(?:first|second|third|1st|2nd|3rd|option 1|option 2|option 3|number 1|number 2|number 3)(?: one| place| library)?\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PLANET = Pattern.compile(
            "\\b(?:on|in|at|near|around|from|the planet|planet)\\s+(?:the\\s+)?(mars|venus|jupiter|saturn|uranus|neptune|pluto|mercury|moon)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern OTHER_CITY = Pattern.compile(
            "\\b(?:in|at|near|around|from|to)\\s+(?:the\\s+)?(johannesburg|joburg|durban|cape town|bloemfontein|polokwane|centurion|hatfield|menlyn|sandton|soweto|london|new york|paris|tokyo)\\b",
            Pattern.CASE_INSENSITIVE);

    private final AssistantDirectory directory;

    public AssistantService(AssistantDirectory directory) {
        this.directory = directory;
    }

    /* ------------------------------------------------------------------ API */

    public Reply respond(String question, List<Message> history, String viewerName) {
        String safe = question == null ? "" : question.replaceAll("\\s+", " ").trim();
        if (safe.isEmpty()) return new Reply("Ask me about the listed food, coffee, banking, beauty and entertainment places, or directions from campus.", List.of(), "local", null);

        Optional<Reply> outOfArea = outOfAreaRequest(safe);
        if (outOfArea.isPresent()) return outOfArea.get();

        List<AssistantPlace> nearby = directory.nearby();
        List<AssistantPlace> pending = directory.pending();
        List<AssistantPlace> all = directory.everything();

        AssistantPlace explicit = matchPlace(safe, all).orElse(null);
        AssistantPlace context = selectedFromHistory(safe, history, nearby, pending);
        boolean followUp = FOLLOW_UP.matcher(safe).find();
        AssistantPlace hit = explicit != null ? explicit : (followUp ? context : null);

        Matcher moneyMatcher = MONEY.matcher(safe);
        String money = moneyMatcher.find()
                ? (moneyMatcher.group(1) != null ? moneyMatcher.group(1) : moneyMatcher.group(2))
                : null;
        boolean pricing = PRICING.matcher(safe).find() || money != null;
        boolean navigation = NAVIGATION.matcher(safe).find();
        boolean interesting = INTERESTING.matcher(safe).find();
        boolean serviceQuestion = SERVICE_QUESTION.matcher(safe).find() && QUESTION_FORM.matcher(safe).find();
        boolean products = PRODUCTS.matcher(safe).find();
        boolean hoursQuestion = HOURS.matcher(safe).find() && HOURS_FORM.matcher(safe).find();
        String category = detectCategory(safe);

        if (GREETING.matcher(safe).matches()) {
            String who = viewerName == null || viewerName.isBlank()
                    ? ""
                    : " " + viewerName.trim().split("\\s+")[0] + "!";
            return new Reply("Hey" + who + " 👋 What are you looking for today — food menus and budget picks, groceries, pharmacies, printing, banks, beauty, entertainment, study spaces, or directions from Richfield?", List.of(), "local", null);
        }
        if (THANKS.matcher(safe).matches()) {
            return new Reply("You’re welcome! 😊 Anything else you want to find around campus?", List.of(), "local", null);
        }
        if (HELP.matcher(safe).matches()) {
            return new Reply("I can help with nearby food menus and budget picks, groceries, pharmacies, printing, banks, beauty, entertainment and study spaces. "
                    + "Ask about a place or get walking directions from Richfield.",
                    List.of(), "local", null);
        }

        if (hit != null) {
            if (products && "food".equals(hit.category())) {
                return menuReply(money, hit, safe);
            }
            if (hoursQuestion) {
                return new Reply("I don’t have today’s opening hours for " + hit.name() + ". It’s listed at "
                        + hit.address() + "; check with the venue before making a special trip. "
                        + "I can still show you directions from Richfield.", List.of(hit.id()), "local", null);
            }
            if ((interesting || serviceQuestion) && money == null && !PRICING.matcher(safe).find()) {
                return new Reply(interestingAnswer(hit), List.of(hit.id()), "local", null);
            }
            if (pricing) {
                return menuReply(money, hit, safe);
            }
            return new Reply(placeAnswer(hit, navigation), List.of(hit.id()), "local", null);
        }

        if (followUp && PRICING.matcher(safe).find() == false
                && Pattern.compile("\\b(this place|that place|there|it|interesting|special|tell me more|more about|what about|the library)\\b",
                        Pattern.CASE_INSENSITIVE).matcher(safe).find()) {
            List<String> studyIds = new ArrayList<>();
            nearby.stream().filter(p -> "study".equals(p.category())).limit(2).forEach(p -> studyIds.add(p.id()));
            pending.stream().filter(p -> "study".equals(p.category())).limit(2).forEach(p -> studyIds.add(p.id()));
            return new Reply("Which listed place would you like to know more about? Tap a place card or tell me its name.",
                    studyIds.stream().limit(2).toList(), "local", null);
        }

        if (pricing) return menuReply(money, null, safe);


        if (WEATHER.matcher(safe).find()) {
            return new Reply("I can help you find places around campus, but I’m not connected to a live weather forecast yet, "
                    + "so I don’t want to guess the weather in Pretoria. Check a weather service for the latest conditions.",
                    List.of(), "local", null);
        }

        if ("food".equals(category)) return menuReply(null, null, safe);

        if (category != null || navigation || NEARBY.matcher(safe).find()) {
            return new Reply(nearbyList(category, nearby, pending), listIds(category, nearby, pending), "local", null);
        }

        return new Reply("I’m best at helping with nearby food menus and budget picks, groceries, pharmacies, printing, banks, beauty, entertainment and study spaces around Richfield Pretoria. "
                + "Tell me what you’re looking for, or ask for walking directions from campus.",
                List.of(), "local", null);
    }

    /* --------------------------------------------------------------- pieces */

    /** "on campus" reads better than "0 m" for the campus library. */
    private static String distancePhrase(AssistantPlace place) {
        return place.distanceMeters() < 25
                ? "on campus"
                : "about " + place.distanceMeters() + " m from campus";
    }

    /**
     * Bullet suffix for the "nearby" list. Never repeats what the address
     * already says (the campus library's address already ends in "on campus").
     */
    private static String distanceSuffix(AssistantPlace place) {
        if (!place.withinRadius()) return "";
        String phrase = distancePhrase(place);
        if (place.address().toLowerCase().contains(phrase)) return "";
        return " (" + phrase + ")";
    }

    private String nearbyList(String category, List<AssistantPlace> nearby, List<AssistantPlace> pending) {
        List<AssistantPlace> matched = filter(category, nearby, 8);
        List<String> lines = matched.stream()
                .map(place -> "• " + place.name() + " — " + place.address() + distanceSuffix(place))
                .toList();
        String label = category == null ? "places" : AssistantDirectory.labelFor(category).toLowerCase();
        if (lines.isEmpty()) {
            String emptyMessage = "study".equals(category)
                    ? "I don’t have a nearby study place to show you just yet. "
                    : "I don’t have a " + label + " recommendation to show you just yet. ";
            return emptyMessage + "Try another category or ask me about a specific place.";
        }
        String introduction = "study".equals(category)
                ? "Here are nearby places to study from Richfield:"
                : "Here are verified places within 1 km of Richfield:";
        return introduction + System.lineSeparator() + System.lineSeparator() + String.join(System.lineSeparator(), lines)
                + System.lineSeparator() + System.lineSeparator()
                + "Tap a place card for its photo and walking directions from campus.";
    }

    private List<String> listIds(String category, List<AssistantPlace> nearby, List<AssistantPlace> pending) {
        return filter(category, nearby, 8).stream().map(AssistantPlace::id).toList();
    }

    private List<AssistantPlace> filter(String category, List<AssistantPlace> source, int limit) {
        return source.stream()
                .filter(place -> category == null || place.category().equals(category))
                .limit(limit)
                .toList();
    }

    private String detectCategory(String question) {
        for (Map.Entry<String, Pattern> entry : PATTERNS.entrySet()) {
            if (entry.getValue().matcher(question).find()) return entry.getKey();
        }
        return null;
    }

    private Optional<Reply> outOfAreaRequest(String question) {
        String words = AssistantDirectory.normaliseWords(question);
        Matcher planet = PLANET.matcher(words);
        if (planet.find()) {
            String name = planet.group(1).equals("moon")
                    ? "the Moon"
                    : planet.group(1).substring(0, 1).toUpperCase() + planet.group(1).substring(1);
            return Optional.of(new Reply("There aren't any public restaurants or shops on " + name
                    + " that I can recommend. My directory covers places around Richfield Pretoria, so I can help you "
                    + "find real nearby options instead.", List.of(), "local", null));
        }
        Matcher city = OTHER_CITY.matcher(words);
        if (city.find()) {
            String location = titleCase(city.group(1));
            return Optional.of(new Reply("I cover places within the Richfield Pretoria campus area, so I can't reliably "
                    + "recommend businesses in " + location + " from this directory. If you want places near Richfield, "
                    + "I can help with those.", List.of(), "local", null));
        }
        return Optional.empty();
    }

    private static String titleCase(String value) {
        String[] parts = value.split(" ");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.toString();
    }

    /** Exact alias containment first, then a bounded edit-distance fallback (typo tolerance). */
    Optional<AssistantPlace> matchPlace(String question, List<AssistantPlace> all) {
        String padded = " " + AssistantDirectory.normaliseWords(question) + " ";
        AssistantPlace best = null;
        int score = 0;
        for (AssistantPlace place : all) {
            for (String phrase : phrases(place)) {
                String alias = AssistantDirectory.normaliseWords(phrase);
                if (alias.length() < 4 || !padded.contains(" " + alias + " ")) continue;
                if (alias.length() > score) {
                    best = place;
                    score = alias.length();
                }
            }
        }
        if (best != null) return Optional.of(best);

        String[] words = AssistantDirectory.normaliseWords(question).split(" ");
        AssistantPlace candidate = null;
        double lowest = Double.MAX_VALUE;
        boolean tied = false;
        for (AssistantPlace place : all) {
            for (String phrase : phrases(place)) {
                String[] aliasWords = AssistantDirectory.normaliseWords(phrase).split(" ");
                String target = String.join("", aliasWords);
                if (target.length() < 7 || words.length < aliasWords.length) continue;
                int threshold = aliasWords.length == 1 ? 1 : target.length() >= 11 ? 3 : 2;
                for (int i = 0; i + aliasWords.length <= words.length; i++) {
                    String fragment = String.join("", java.util.Arrays.copyOfRange(words, i, i + aliasWords.length));
                    int distance = editDistance(fragment, target, threshold);
                    if (distance == 0 || distance > threshold) continue;
                    double adjusted = (double) distance / target.length();
                    if (adjusted < lowest - 0.001) {
                        candidate = place;
                        lowest = adjusted;
                        tied = false;
                    } else if (Math.abs(adjusted - lowest) < 0.001 && candidate != null && !candidate.id().equals(place.id())) {
                        tied = true;
                    }
                }
            }
        }
        return tied ? Optional.empty() : Optional.ofNullable(candidate);
    }

    private List<String> phrases(AssistantPlace place) {
        List<String> out = new ArrayList<>();
        out.add(place.name());
        out.addAll(place.aliases());
        return out;
    }

    private AssistantPlace selectedFromHistory(String question, List<Message> history,
                                               List<AssistantPlace> nearby, List<AssistantPlace> pending) {
        List<AssistantPlace> all = new ArrayList<>(nearby);
        all.addAll(pending);

        Matcher ordinal = ORDINAL.matcher(AssistantDirectory.normaliseWords(question));
        if (ordinal.find()) {
            String text = ordinal.group();
            int rank = (text.contains("first") || text.contains("1")) ? 0
                    : (text.contains("second") || text.contains("2")) ? 1 : 2;
            String lastCategory = null;
            for (int i = history.size() - 1; i >= 0; i--) {
                Message message = history.get(i);
                if (!"user".equals(message.role())) continue;
                String detected = detectCategory(message.content());
                if (detected != null && lastCategory == null) {
                    lastCategory = detected;
                    break;
                }
            }
            if (lastCategory != null) {
                List<AssistantPlace> pool = new ArrayList<>(filter(lastCategory, nearby, 5));
                pool.addAll(filter(lastCategory, pending, 5));
                return rank < pool.size() ? pool.get(rank) : null;
            }
        }

        for (int i = history.size() - 1; i >= 0; i--) {
            Message message = history.get(i);
            if (!"user".equals(message.role())) continue;
            AssistantPlace found = matchPlace(message.content(), all).orElse(null);
            if (found != null) return found;
            if (Pattern.compile("\\b(where can i|places to|nearby|recommend|find|show)\\b", Pattern.CASE_INSENSITIVE)
                    .matcher(message.content()).find() && detectCategory(message.content()) != null) {
                break;
            }
        }
        return null;
    }

    private String placeIntro(AssistantPlace place) {
        return switch (place.id()) {
            case "student:magwinya-zone" -> "Magwinya Zone is a takeaway spot for magwinya, the classic South African "
                    + "fried-dough snack. It could be a good option when you want a quick bite between classes.";
            case "student:honchos" -> "Honchos is a chicken and fast-food takeaway. It’s one of the options students "
                    + "can consider when looking for a quick meal in Pretoria Central.";
            case "student:asian-delight-shawarma" -> "Asian Delight Shawarma is a takeaway for shawarma and other food. "
                    + "You can also see its storefront photo in the guide.";
            case "student:top-class-fast-foods" -> "Top Class Fast Foods is a takeaway spot on Pretorius Street. "
                    + "Its storefront photo is included in the guide.";
            case "student:quick-bake" -> "Quick Bake is a bakery and confectionery spot. Its storefront sign also mentions coffee.";
            case "student:thuba-rush-kj" -> "Thuba Rush KJ is a takeaway on Pretorius Street. "
                    + "We have a photo of its storefront so you can recognise it.";
            case "student:four-four-two" -> "Four Four Two is a sports and music lounge in Pretoria Central. "
                    + "Check entry requirements and current events with the venue.";
            case "student:xpresso" -> "Xpresso is a coffee shop in Pretoria Central. Its map pin is available, "
                    + "but an exact street address was not supplied.";
            case "student:family-chips" -> "Family Chips is a takeaway food spot on Pretorius Street. "
                    + "The current menu and prices have not been supplied.";
            case "student:phola-shisa-nyama" -> "Phola Shisa Nyama is a shisa nyama spot at 219 Pretorius Street. "
                    + "The current menu and hours have not been supplied.";
            case "local:richfield-library" -> "The Richfield Pretoria campus library is conveniently on campus, "
                    + "so it is an option to consider for studying between lectures.";
            case "student:sammy-marks-library" -> "Es’kia Mphahlele Community Library is a public library near Sammy Marks "
                    + "with reading and study areas. It also has a documented American Corner.";
            case "student:absa-building" -> "ABSA Building is listed in the student guide as a banking-related location "
                    + "in Pretoria Central. Bank and ATM services at that address still need confirmation.";
            case "student:state-theatre" -> "The South African State Theatre is a performing-arts venue at 320 Pretorius Street. "
                    + "Phone: 012 392 4000. Check the current programme and ticket details on statetheatre.co.za.";
            case "student:container-4-et-beauty-salon" -> "Container 4 E&T is a beauty salon at 344 Francis Baard Street, Pretoria Central. "
                    + "Current services and opening hours were not supplied.";
            default -> switch (place.category()) {
                case "food" -> place.name() + " is one of the food options in the student directory.";
                case "study" -> place.name() + " is a study option in the student directory.";
                case "banking" -> place.name() + " is a banking-related location in the student directory.";
                case "transport" -> place.name() + " is a transport option in the student directory.";
                case "fun" -> place.name() + " is an entertainment option in the student directory.";
                case "health" -> place.name() + " is a health-related location in the student directory.";
                case "printing" -> place.name() + " is a printing or stationery option in the student directory.";
                case "grooming" -> place.name() + " is a grooming or salon option in the student directory.";
                case "groceries" -> place.name() + " is a grocery option in the student directory.";
                default -> place.name() + " is listed in the student directory.";
            };
        };
    }

    private Reply menuReply(String amountText, AssistantPlace place, String question) {
        Integer budget = null;
        if (amountText != null) {
            try { budget = Integer.parseInt(amountText); } catch (NumberFormatException ignored) { }
        }
        if (place != null && !"food".equals(place.category())) {
            return new Reply("I don’t have confirmed current charges for " + place.name()
                    + ". Please check with the venue directly; I can still show its location and directions.",
                    List.of(place.id()), "local", null);
        }
        if (place != null && !place.withinRadius()) {
            return new Reply("I can’t include " + place.name()
                    + " in the nearby menu finder because its map pin is not confirmed inside the 1 km campus radius.",
                    List.of(place.id()), "local", null);
        }

        final Integer selectedBudget = budget;
        String requestedCategory = menuCategory(question);
        List<AssistantPlace> candidates = place == null
                ? directory.nearby().stream().filter(p -> "food".equals(p.category())).toList()
                : List.of(place);
        boolean asksForDrinks = containsWord(AssistantDirectory.normaliseWords(question), "drink")
                || containsWord(AssistantDirectory.normaliseWords(question), "drinks")
                || containsWord(AssistantDirectory.normaliseWords(question), "beverage");
        List<MenuMatch> matches = candidates.stream()
                .flatMap(candidate -> candidate.menuItems().stream()
                        .filter(item -> item.price() != null)
                        .filter(item -> selectedBudget == null || item.price() <= selectedBudget + 0.0001)
                        .filter(item -> matchesMenuCategory(item, requestedCategory))
                        .filter(item -> asksForDrinks || !"drink".equals(item.category()))
                        .map(item -> new MenuMatch(candidate, item)))
                .sorted(Comparator.comparingDouble((MenuMatch match) -> match.item().price())
                        .thenComparingInt(match -> match.place().distanceMeters() == null
                                ? Integer.MAX_VALUE : match.place().distanceMeters()))
                .limit(10)
                .toList();

        if (matches.isEmpty()) {
            String message;
            if (place != null && place.menuItems().isEmpty()) {
                message = "I don’t have a current published menu for " + place.name()
                        + ", so I won’t guess its prices. Its location and walking directions are still available.";
            } else if (budget != null) {
                message = "I couldn’t find a verified menu item listed at or below R" + budget
                        + " in the nearby menus I checked. Try a higher budget or browse the menu cards in Explore.";
            } else if (requestedCategory != null) {
                message = "I couldn’t find a verified " + requestedCategory
                        + " item in the published nearby menus yet. Try another food type or open Explore.";
            } else {
                message = "I don’t have a published current menu for that place yet, so I won’t make up prices.";
            }
            List<String> placeIds = place == null ? List.of() : List.of(place.id());
            return new Reply(message, placeIds, "local", null);
        }

        String heading = budget != null
                ? "With R" + budget + ", these published menu items fit:"
                : place != null
                    ? "Here are menu items currently listed for " + place.name() + ":"
                    : requestedCategory != null
                        ? "Here are nearby " + requestedCategory + " picks:"
                        : "Here are some menu items from places within 1 km:";
        List<String> lines = matches.stream()
                .map(match -> "• " + match.item().name() + " — " + menuPrice(match.item().price())
                        + " at " + match.place().name())
                .toList();
        List<String> placeIds = matches.stream().map(match -> match.place().id()).distinct().toList();
        List<String> menuIds = matches.stream().map(match -> match.item().id()).toList();
        String note = "Prices come from the linked published menus and may differ at the counter. Tap a photo to see the place, full menu source and walking directions.";
        return new Reply(heading + System.lineSeparator() + System.lineSeparator()
                + String.join(System.lineSeparator(), lines) + System.lineSeparator() + System.lineSeparator() + note,
                placeIds, "local", null, menuIds);
    }

    private static String menuCategory(String question) {
        String words = AssistantDirectory.normaliseWords(question);
        if (containsWord(words, "pizza") || containsWord(words, "pizzas")) return "pizza";
        if (containsWord(words, "shawarma") || containsWord(words, "schwarma")) return "shawarma";
        if (containsWord(words, "wrap") || containsWord(words, "wraps")) return "wrap";
        if (containsWord(words, "burger") || containsWord(words, "burgers")) return "burger";
        if (containsWord(words, "chicken") || containsWord(words, "wings") || containsWord(words, "winglets")
                || containsWord(words, "paella")) return "chicken";
        if (containsWord(words, "scone") || containsWord(words, "scones") || containsWord(words, "pie")
                || containsWord(words, "pies") || containsWord(words, "bakery") || containsWord(words, "muffin")
                || containsWord(words, "muffins") || containsWord(words, "coffee")) return "bakery";
        if (containsWord(words, "drink") || containsWord(words, "drinks")
                || containsWord(words, "beverage") || containsWord(words, "beverages")) return "drink";
        if (containsWord(words, "sphatlho") || containsWord(words, "snack") || containsWord(words, "snacks")) return "snack";
        return null;
    }

    private static boolean matchesMenuCategory(AssistantPlace.MenuItem item, String requestedCategory) {
        if (requestedCategory == null) return true;
        if (requestedCategory.equals(item.category())) return true;
        String searchable = AssistantDirectory.normaliseWords(
                item.name() + " " + item.category() + " " + item.description());
        return searchable.contains(requestedCategory);
    }

    private static boolean containsWord(String words, String term) {
        return (" " + words + " ").contains(" " + term + " ");
    }

    private static String menuPrice(Double price) {
        return String.format(Locale.ROOT, "R%.2f", price);
    }

    private String interestingAnswer(AssistantPlace place) {
        List<String> facts = place.highlights();
        if (!facts.isEmpty()) {
            String details = facts.size() > 1
                    ? String.join("\n", facts.stream().map(fact -> "• " + fact).toList())
                    : facts.get(0);
            String access = "study".equals(place.category())
                    ? " Some services and current access may vary, so check with the library before visiting." : "";
            return place.name() + " has a few things worth knowing about:\n\n" + details + "\n\n"
                    + "These are the details currently listed in the student guide." + access;
        }
        List<String> lines = new ArrayList<>();
        lines.add(placeIntro(place));
        lines.add(addressDetail(place));
        if ("food".equals(place.category())) {
            lines.add(place.menuItems().isEmpty()
                    ? "The shop’s current menu isn’t published in this guide yet, so I won’t guess its prices."
                    : "Tap a menu card to compare this shop’s published item names and prices.");
        }
        if ("fun".equals(place.category())) {
            lines.add("I can show you directions, but I don’t have a confirmed current events schedule.");
        }
        lines.add("Want directions from Richfield or suggestions for a student budget?");
        return String.join("\n\n", lines);
    }

    private String addressDetail(AssistantPlace place) {
        return "📍 The guide lists it at " + place.address() + ".";
    }

    private String placeAnswer(AssistantPlace place, boolean navigation) {
        List<String> lines = new ArrayList<>();
        lines.add(placeIntro(place));
        lines.add(addressDetail(place));
        if (navigation) {
            lines.add("Tap Map on the place card to view the destination, or use the walking-directions link to start "
                    + "from Richfield, 291 Helen Joseph Street.");
            if (!place.verificationNote().isBlank()) lines.add(place.verificationNote());
        } else {
            lines.add("You can tap its card for available details and directions from Richfield.");
        }
        if (place.withinRadius()) {
            lines.add(place.distanceMeters() < 25
                    ? "It is on the campus itself, so there is barely any walk once you are at the Richfield gate."
                    : "The map puts it about " + place.distanceMeters() + " metres from campus in a straight line. "
                      + "The walking route may be longer.");
        } else {
            lines.add("I don’t have a reliable distance from campus yet, so I won’t guess the metres. "
                    + "The map link can help you check the walking route.");
        }
        return String.join("\n\n", lines);
    }

    /** Levenshtein distance with early exit. */
    static int editDistance(String a, String b, int limit) {
        if (Math.abs(a.length() - b.length()) > limit) return limit + 1;
        int[] previous = new int[b.length() + 1];
        for (int i = 0; i <= b.length(); i++) previous[i] = i;
        for (int i = 1; i <= a.length(); i++) {
            int[] current = new int[b.length() + 1];
            current[0] = i;
            int smallest = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                int next = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
                current[j] = next;
                if (next < smallest) smallest = next;
            }
            if (smallest > limit) return limit + 1;
            previous = current;
        }
        return previous[b.length()];
    }
}

