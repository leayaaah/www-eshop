package iuh.fit.eshop.ai;

import iuh.fit.eshop.entity.Product;
import iuh.fit.eshop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
public class ProjectionService {
    private final ProductVectorIndex vectors;
    private final ProductRepository products;
    private final AtomicReference<ProjectionResult> cached = new AtomicReference<>();

    public ProjectionResult project() {
        ProjectionResult current = cached.get();
        if (current != null) return current;

        synchronized (cached) {
            current = cached.get();
            if (current != null) return current;
            vectors.reload();
            current = calculate(products.findAllWithCategory(), vectors.snapshot());
            cached.set(current);
            return current;
        }
    }

    @EventListener
    public void onProductChanged(ProductChangedEvent ignored) {
        cached.set(null);
    }

    ProjectionResult calculate(List<Product> allProducts, Map<Long, float[]> sourceVectors) {
        List<Product> selected = allProducts.stream()
                .sorted(Comparator.comparing(Product::getId))
                .limit(50)
                .toList();
        List<Product> available = selected.stream()
                .filter(product -> sourceVectors.containsKey(product.getId()))
                .toList();
        List<Product> missing = selected.stream()
                .filter(product -> !sourceVectors.containsKey(product.getId()))
                .toList();

        if (available.isEmpty()) {
            return new ProjectionResult(List.of(), missing, 0d);
        }

        int rows = available.size();
        int dimensions = available.stream()
                .mapToInt(product -> sourceVectors.get(product.getId()).length)
                .min().orElse(0);
        if (dimensions == 0) return new ProjectionResult(List.of(), allProducts, 0d);

        double[][] data = new double[rows][dimensions];
        for (int row = 0; row < rows; row++) {
            float[] vector = sourceVectors.get(available.get(row).getId());
            double norm = 0d;
            for (int col = 0; col < dimensions; col++) norm += vector[col] * vector[col];
            norm = Math.sqrt(norm);
            for (int col = 0; col < dimensions; col++) {
                data[row][col] = norm == 0d ? 0d : vector[col] / norm;
            }
        }
        center(data);

        double[][] gram = gram(data);
        Eigenvector first = dominant(gram, null);
        Eigenvector second = dominant(gram, first.vector);
        double totalVariance = trace(gram);
        double explained = totalVariance == 0d ? 0d
                : clamp((first.value + second.value) / totalVariance);

        double[] x = coordinates(first.vector, first.value);
        double[] y = coordinates(second.vector, second.value);
        normalize(x);
        normalize(y);

        Map<Long, List<Neighbor>> neighbors = nearestNeighbors(available, sourceVectors);
        List<ProductMapPoint> points = new ArrayList<>();
        for (int i = 0; i < rows; i++) {
            Product product = available.get(i);
            points.add(new ProductMapPoint(product.getId(), product.getName(),
                    product.getCategory().getName(), product.getPrice(), x[i], y[i],
                    neighbors.getOrDefault(product.getId(), List.of())));
        }
        return new ProjectionResult(List.copyOf(points), missing, explained);
    }

    private void center(double[][] data) {
        for (int col = 0; col < data[0].length; col++) {
            double mean = 0d;
            for (double[] row : data) mean += row[col];
            mean /= data.length;
            for (double[] row : data) row[col] -= mean;
        }
    }

    private double[][] gram(double[][] data) {
        double[][] result = new double[data.length][data.length];
        for (int i = 0; i < data.length; i++) {
            for (int j = i; j < data.length; j++) {
                double value = 0d;
                for (int col = 0; col < data[i].length; col++) value += data[i][col] * data[j][col];
                result[i][j] = value;
                result[j][i] = value;
            }
        }
        return result;
    }

    private Eigenvector dominant(double[][] matrix, double[] excluded) {
        double[] vector = new double[matrix.length];
        vector[0] = 1d;
        for (int iteration = 0; iteration < 200; iteration++) {
            double[] next = new double[vector.length];
            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix.length; j++) next[i] += matrix[i][j] * vector[j];
            }
            if (excluded != null) {
                double projection = dot(next, excluded);
                for (int i = 0; i < next.length; i++) next[i] -= projection * excluded[i];
            }
            double norm = Math.sqrt(dot(next, next));
            if (norm < 1e-12) return new Eigenvector(new double[vector.length], 0d);
            for (int i = 0; i < next.length; i++) next[i] /= norm;
            if (distance(next, vector) < 1e-10) {
                vector = next;
                break;
            }
            vector = next;
        }
        double value = 0d;
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix.length; j++) value += vector[i] * matrix[i][j] * vector[j];
        }
        fixSign(vector);
        return new Eigenvector(vector, Math.max(0d, value));
    }

    private void fixSign(double[] vector) {
        int largest = 0;
        for (int i = 1; i < vector.length; i++) {
            if (Math.abs(vector[i]) > Math.abs(vector[largest])) largest = i;
        }
        if (vector.length > 0 && vector[largest] < 0d) {
            for (int i = 0; i < vector.length; i++) vector[i] = -vector[i];
        }
    }

    private double[] coordinates(double[] eigenvector, double eigenvalue) {
        double scale = Math.sqrt(Math.max(0d, eigenvalue));
        double[] result = eigenvector.clone();
        for (int i = 0; i < result.length; i++) result[i] *= scale;
        return result;
    }

    private void normalize(double[] values) {
        if (values.length == 0) return;
        double min = Arrays.stream(values).min().orElse(0d);
        double max = Arrays.stream(values).max().orElse(0d);
        double range = max - min;
        for (int i = 0; i < values.length; i++) values[i] = range == 0d ? .5d : (values[i] - min) / range;
    }

    private Map<Long, List<Neighbor>> nearestNeighbors(List<Product> available, Map<Long, float[]> sourceVectors) {
        Map<Long, List<Neighbor>> result = new HashMap<>();
        for (Product source : available) {
            List<Neighbor> nearest = available.stream()
                    .filter(candidate -> !candidate.getId().equals(source.getId()))
                    .map(candidate -> new Neighbor(candidate.getId(), candidate.getName(), vectors.cosine(
                            sourceVectors.get(source.getId()), sourceVectors.get(candidate.getId()))))
                    .sorted(Comparator.comparingDouble(Neighbor::similarity).reversed())
                    .limit(5)
                    .toList();
            result.put(source.getId(), nearest);
        }
        return result;
    }

    private double trace(double[][] matrix) {
        double total = 0d;
        for (int i = 0; i < matrix.length; i++) total += matrix[i][i];
        return total;
    }

    private double dot(double[] left, double[] right) {
        double result = 0d;
        for (int i = 0; i < left.length; i++) result += left[i] * right[i];
        return result;
    }

    private double distance(double[] left, double[] right) {
        double result = 0d;
        for (int i = 0; i < left.length; i++) result = Math.max(result, Math.abs(left[i] - right[i]));
        return result;
    }

    private double clamp(double value) {
        return Math.max(0d, Math.min(1d, value));
    }

    private record Eigenvector(double[] vector, double value) {}

    public record Neighbor(Long id, String name, double similarity) {}

    public record ProductMapPoint(Long id, String name, String category, BigDecimal price,
                                  double x, double y, List<Neighbor> neighbors) {}

    public record ProjectionResult(List<ProductMapPoint> points, List<Product> missing,
                                   double explainedVariance) {}
}
