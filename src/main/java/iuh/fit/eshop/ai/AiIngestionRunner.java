package iuh.fit.eshop.ai;

import iuh.fit.eshop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

@Component
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
@RequiredArgsConstructor
public class AiIngestionRunner implements ApplicationRunner {
    private final ProductRepository products;
    private final ProductEmbeddingService embeddings;
    @Value("${app.ai.ingest-on-startup:false}")
    private boolean enabled;

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) return;
        products.findAllWithCategory().forEach(embeddings::upsert);
    }
}
