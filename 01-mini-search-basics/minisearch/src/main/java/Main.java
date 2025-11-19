import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.lang.Math.log;
import java.util.*;

class NewsItem {
    public String category;
    public String headline;
    public String authors;
    public String link;
    public String short_description;
    public String date;
}

class SearchResult {
    final int docId;
    final double score;
    final NewsItem doc;

    SearchResult(int docId, double score, NewsItem doc) {
        this.docId = docId;
        this.score = score;
        this.doc = doc;
    }
}

class Searching {
    public List<NewsItem> LoadJson() throws IOException {
        Path jsonPath = Path.of("data", "News_Category_Dataset.json");
        ObjectMapper objectMapper = new ObjectMapper();

        List<NewsItem> items = new ArrayList<>();

        try (var reader = Files.newBufferedReader(jsonPath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue; // skip blank lines

                NewsItem item = objectMapper.readValue(line, NewsItem.class);
                items.add(item);
            }
        }

        System.out.println("Loaded " + items.size() + " news items.");
        return items;
    }
}

class Posting {
    int docId;
    List<Integer> positions = new ArrayList<>();

    Posting(int docId) {
        this.docId = docId;
    }

    void addPosition(int pos) {
        positions.add(pos);
    }
}

class InvertedIndex {
    // term -> list of postings
    private final Map<String, List<Posting>> index = new HashMap<>();

    // docId -> length (number of tokens) in that doc
    private final Map<Integer, Integer> docLengths = new HashMap<>();

    // optional: store docs by id so you can print results later
    private final Map<Integer, NewsItem> documents = new HashMap<>();

    public void addDocument(int docId, NewsItem item, String text) {
        documents.put(docId, item);

        List<String> tokens = tokenize(text);
        docLengths.put(docId, tokens.size());

        // term -> posting for this doc (so we don't create duplicates)
        Map<String, Posting> postingsForDoc = new HashMap<>();

        for (int position = 0; position < tokens.size(); position++) {
            String term = tokens.get(position);

            Posting posting = postingsForDoc.get(term);
            if (posting == null) {
                posting = new Posting(docId);
                postingsForDoc.put(term, posting);
            }
            posting.addPosition(position);
        }

        // merge into global index
        for (Map.Entry<String, Posting> e : postingsForDoc.entrySet()) {
            String term = e.getKey();
            Posting posting = e.getValue();

            List<Posting> postingsList = index.computeIfAbsent(term, t -> new ArrayList<>());
            postingsList.add(posting);
        }
    }

    public List<Posting> getPostings(String term) {
        return index.getOrDefault(term, Collections.emptyList());
    }

    public int getDocLength(int docId) {
        return docLengths.getOrDefault(docId, 0);
    }

    public int getDocumentFrequency(String term) {
        List<Posting> postings = index.get(term);
        return postings == null ? 0 : postings.size();
    }

    public NewsItem getDocument(int docId) {
        return documents.get(docId);
    }

    // --- tokenizer ---
    private static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null) return tokens;

        // lowercase
        String lower = text.toLowerCase(Locale.ROOT);

        // split on non-letter/non-digit
        // this turns "omicron-targeted" -> ["omicron", "targeted"]
        String[] parts = lower.split("[^a-z0-9]+");

        for (String p : parts) {
            if (!p.isEmpty()) {
                tokens.add(p);
            }
        }
        return tokens;
    }

    public int getDocCount() {
        return documents.size();
    }

    // --- TF-IDF search ---
    public List<SearchResult> search(String query, int k) {
        List<String> queryTerms = tokenize(query);
        Map<Integer, Double> scores = new HashMap<>();

        int N = getDocCount();

        for (String term : queryTerms) {
            List<Posting> postings = index.get(term);
            if (postings == null) continue;

            int df = postings.size();
            if (df == 0) continue;

            // idf = log(N / df)
            double idf = log((N + 1.0) / (df + 1.0)); // +1 to be safe

            for (Posting p : postings) {
                int tf = p.positions.size();                // tf(term, doc)
                double tfWeight = 1.0 + log(tf);            // log-scaled TF
                double scoreAdd = tfWeight * idf;

                scores.merge(p.docId, scoreAdd, Double::sum);
            }
        }

        // Convert to SearchResult list
        List<SearchResult> results = new ArrayList<>();
        for (Map.Entry<Integer, Double> e : scores.entrySet()) {
            int docId = e.getKey();
            double score = e.getValue();
            NewsItem doc = documents.get(docId);
            results.add(new SearchResult(docId, score, doc));
        }

        // sort descending by score
        results.sort((a, b) -> Double.compare(b.score, a.score));

        // return top K
        if (results.size() > k) {
            return results.subList(0, k);
        }
        return results;
    }
}


public class Main {
    public static void main(String[] args) throws Exception {
        Searching srch = new Searching();
        List<NewsItem> items = srch.LoadJson();

        InvertedIndex index = new InvertedIndex();
        for (int docId = 0; docId < items.size(); docId++) {
            NewsItem item = items.get(docId);
            String text = item.headline + " " + item.short_description;
            index.addDocument(docId, item, text);
        }

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("\nEnter query (or blank to quit): ");
            String q = scanner.nextLine().trim();
            if (q.isEmpty()) break;

            List<SearchResult> results = index.search(q, 5); // top 5
            for (SearchResult r : results) {
                System.out.printf("score=%.4f | %s (%s)%n",
                        r.score,
                        r.doc.headline,
                        r.doc.link);
            }
        }
    }
}

