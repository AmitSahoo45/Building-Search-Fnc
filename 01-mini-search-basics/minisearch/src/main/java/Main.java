import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

class NewsItem {
    public String category;
    public String headline;
    public String authors;
    public String link;
    public String short_description;
    public LocalDateTime date;
}

class Searching {
    public List<NewsItem> LoadJson() throws IOException {
        Path jsonPath = Path.of("data", "News_Category_Dataset.json");
        String json = Files.readString(jsonPath, StandardCharsets.UTF_8);

        ObjectMapper objectMapper = new ObjectMapper();

        return objectMapper.readValue(json, new TypeReference<List<NewsItem>>() {
        });
    }
}

public class Main {
    public static void Main(String[] args) throws IOException {
        Searching srch = new Searching();
        List<NewsItem> items = srch.LoadJson();

    }
}