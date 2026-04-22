package server.main.global.util.news;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NewsServiceImpl implements NewsService {

    private final NaverNewsClient naverNewsClient;

    @Override
    public List<NewsItemDto> getStoNews() {
        return naverNewsClient.fetchStoNews(10).stream()
                .map(NewsItemDto::from)
                .toList();
    }
}
