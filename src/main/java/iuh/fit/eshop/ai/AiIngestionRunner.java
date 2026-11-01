package iuh.fit.eshop.ai;

import iuh.fit.eshop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
@RequiredArgsConstructor
public class AiIngestionRunner implements ApplicationRunner {
    private final VectorStore vectorStore;
    private final ProductRepository products;

    @Value("${app.ai.ingest-on-startup:false}")
    private boolean enabled;

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) return;
        ingestProducts();
    }

    private void ingestProducts() {
        List<Document> docs = products.findAllWithCategory().stream()
                .map(p -> new Document(
                        p.getName() + ". " + p.getDescription() + " Giá " + p.getPrice() + " đồng.",
                        Map.of("source", "product",
                                "productId", p.getId(),
                                "categoryId", p.getCategory().getId())))
                .toList();
        for (Document doc : docs) {
            Object id = doc.getMetadata().get("productId");
            if (id == null) continue;
            var existing = vectorStore.similaritySearch(
                    org.springframework.ai.vectorstore.SearchRequest.builder()
                            .query(doc.getText()).topK(1)
                            .filterExpression("source == 'product' && productId == " + id).build());
            if (existing.isEmpty()) vectorStore.add(List.of(doc));
        }
    }
}