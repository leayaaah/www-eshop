package iuh.fit.eshop.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.eshop.entity.Product;
import iuh.fit.eshop.repository.ProductRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class ProductVectorIndex {
    private final JdbcTemplate jdbc;
    private final ProductRepository products;
    private final ObjectProvider<EmbeddingModel> embeddings;
    private final ObjectMapper objectMapper;
    private final Path cacheFile;
    private final Map<Long, float[]> vectors = new ConcurrentHashMap<>();

    public ProductVectorIndex(
            JdbcTemplate jdbc,
            ProductRepository products,
            ObjectProvider<EmbeddingModel> embeddings,
            ObjectMapper objectMapper,
            @Value("${app.reco.vector-cache-file:${java.io.tmpdir}/eshop-product-vectors.bin}") String cacheFile) {
        this.jdbc = jdbc;
        this.products = products;
        this.embeddings = embeddings;
        this.objectMapper = objectMapper;
        this.cacheFile = Path.of(cacheFile);
    }

    @PostConstruct
    public void load() {
        try {
            loadFromMariaDb();
        } catch (RuntimeException ex) {
            log.warn("Unable to load product vectors from MariaDB: {}", ex.getMessage());
        }
        if (vectors.isEmpty()) {
            loadFromCache();
        }
        if (vectors.isEmpty()) {
            embedAndCache();
        }
        log.info("Recommendation vector index loaded: {} products, source={}",
                vectors.size(), vectors.isEmpty() ? "unavailable" : "memory");
    }

    public synchronized void reload() {
        vectors.clear();
        load();
    }

    public boolean available() {
        return !vectors.isEmpty();
    }

    public float[] vector(Long productId) {
        return vectors.get(productId);
    }

    public Map<Long, float[]> snapshot() {
        Map<Long, float[]> copy = new LinkedHashMap<>();
        vectors.forEach((id, vector) -> copy.put(id, vector.clone()));
        return Collections.unmodifiableMap(copy);
    }

    public double cosine(float[] left, float[] right) {
        if (left == null || right == null || left.length != right.length) return 0d;
        double dot = 0d;
        double leftNorm = 0d;
        double rightNorm = 0d;
        for (int i = 0; i < left.length; i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }
        return leftNorm == 0d || rightNorm == 0d
                ? 0d : dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    public List<Product> similar(Product product, int limit, List<Product> candidates) {
        float[] source = vector(product.getId());
        if (source == null) return List.of();
        return candidates.stream()
                .filter(candidate -> !Objects.equals(candidate.getId(), product.getId()))
                .filter(candidate -> candidate.getQuantity() != null && candidate.getQuantity() > 0)
                .sorted(Comparator.comparingDouble((Product candidate) ->
                        cosine(source, vector(candidate.getId()))).reversed())
                .limit(Math.max(1, Math.min(limit, 20)))
                .toList();
    }

    private void loadFromMariaDb() {
        jdbc.query("select metadata, embedding from vector_store " +
                        "where metadata like '%\"source\":\"product\"%'",
                resultSet -> {
            try {
                Map<?, ?> metadata = objectMapper.readValue(
                        resultSet.getString("metadata"), Map.class);
                Object productId = metadata.get("productId");
                float[] vector = toVector(resultSet.getObject("embedding"));
                if (productId instanceof Number number && vector != null) {
                    vectors.put(number.longValue(), vector);
                }
            } catch (Exception ex) {
                log.debug("Skipping malformed product vector row: {}", ex.getMessage());
            }
        });
    }

    private void loadFromCache() {
        if (!Files.isRegularFile(cacheFile)) return;
        try (DataInputStream input = new DataInputStream(Files.newInputStream(cacheFile))) {
            while (true) {
                long productId = input.readLong();
                int length = input.readInt();
                float[] vector = new float[length];
                for (int i = 0; i < length; i++) vector[i] = input.readFloat();
                vectors.put(productId, vector);
            }
        } catch (EOFException ignored) {
            log.info("Loaded cached product vectors from {}", cacheFile);
        } catch (Exception ex) {
            log.warn("Unable to load cached product vectors: {}", ex.getMessage());
            vectors.clear();
        }
    }

    private void embedAndCache() {
        EmbeddingModel model = embeddings.getIfAvailable();
        if (model == null) return;
        List<Product> source = products.findAllWithCategory().stream().limit(50).toList();
        if (source.isEmpty()) return;
        try {
            List<float[]> embedded = model.embed(source.stream().map(this::content).toList());
            for (int i = 0; i < Math.min(source.size(), embedded.size()); i++) {
                if (embedded.get(i) != null) vectors.put(source.get(i).getId(), embedded.get(i));
            }
            if (!vectors.isEmpty()) {
                Path parent = cacheFile.getParent();
                if (parent != null) Files.createDirectories(parent);
                try (DataOutputStream output = new DataOutputStream(Files.newOutputStream(cacheFile))) {
                    for (var entry : vectors.entrySet()) {
                        output.writeLong(entry.getKey());
                        output.writeInt(entry.getValue().length);
                        for (float value : entry.getValue()) output.writeFloat(value);
                    }
                }
                log.info("Cached {} product vectors to {}", vectors.size(), cacheFile);
            }
        } catch (RuntimeException | java.io.IOException ex) {
            log.warn("Unable to create product vector cache: {}", ex.getMessage());
        }
    }

    private String content(Product product) {
        return product.getName() + ". " +
                (product.getDescription() == null ? "" : product.getDescription()) +
                ". Danh mục " + product.getCategory().getName();
    }

    private float[] toVector(Object value) {
        if (value instanceof float[] floats) return floats;
        if (value instanceof double[] doubles) {
            float[] result = new float[doubles.length];
            for (int i = 0; i < doubles.length; i++) result[i] = (float) doubles[i];
            return result;
        }
        if (value instanceof byte[] bytes && bytes.length % 4 == 0) {
            float[] result = new float[bytes.length / 4];
            ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < result.length; i++) result[i] = buffer.getFloat();
            return result;
        }
        if (value instanceof java.sql.Blob blob) {
            try {
                return toVector(blob.getBytes(1, (int) blob.length()));
            } catch (java.sql.SQLException ignored) {
                return null;
            }
        }
        if (value instanceof String text) {
            String normalized = text.replace("[", "").replace("]", "").trim();
            if (normalized.isBlank()) return null;
            String[] parts = normalized.split(",");
            float[] result = new float[parts.length];
            for (int i = 0; i < parts.length; i++) result[i] = Float.parseFloat(parts[i].trim());
            return result;
        }
        return null;
    }
}
