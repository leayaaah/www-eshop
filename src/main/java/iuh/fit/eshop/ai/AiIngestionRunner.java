package iuh.fit.eshop.ai;

import iuh.fit.eshop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
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
        vectorStore.delete("source == 'product'");

        List<Document> docs = products.findAllWithCategory().stream()
                .map(p -> new Document(
                        p.getName() + ". " + p.getDescription() + " Giá " + p.getPrice() + " đồng.",
                        Map.of("source", "product",
                                "productId", p.getId(),
                                "categoryId", p.getCategory().getId())))
                .toList();
        vectorStore.add(docs);
    }
}