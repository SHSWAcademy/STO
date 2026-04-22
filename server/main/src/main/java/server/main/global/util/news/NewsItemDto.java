package server.main.global.util.news;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NewsItemDto {

    private String title;
    private String link;
    private String description;
    private String pubDate;

    public static NewsItemDto from(NaverNewsResponseDto.Item item) {
        return new NewsItemDto(
                stripHtml(item.getTitle()),
                item.getOriginallink(),
                stripHtml(item.getDescription()),
                item.getPubDate()
        );
    }

    private static String stripHtml(String html) {
        return html == null ? "" : html.replaceAll("<[^>]*>", "").replace("&quot;", "\"").replace("&amp;", "&");
    }
}
