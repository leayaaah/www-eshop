package iuh.fit.eshop.ai;

import iuh.fit.eshop.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductSearchService {
    private final ProductVectorSearchService vectorSearch;

    public List<Product> semanticSearch(String query, int limit) {
        return vectorSearch.search(query, limit);
    }
}
