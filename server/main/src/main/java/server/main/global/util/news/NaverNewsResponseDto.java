package server.main.global.util.news;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class NaverNewsResponseDto {

    private int total;
    private int display;
    private List<Item> items;

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        private String title;
        private String originallink;
        private String link;
        private String description;
        private String pubDate;
    }
}
