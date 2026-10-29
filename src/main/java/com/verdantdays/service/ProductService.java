package com.verdantdays.service;

import com.verdantdays.model.Product;
import com.verdantdays.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    public List<Product> getAllActive() {
        return productRepository.findByActiveTrueOrderByDisplayNameAsc();
    }

    public Page<Product> getPage(String search, int page, int size) {
        var p = PageRequest.of(page, size);
        if (search != null && !search.trim().isEmpty()) {
            return productRepository.searchActive(search.trim(), p);
        }
        return productRepository.findByActiveTrueOrderByDisplayNameAsc(p);
    }

    public Product getById(Integer id) {
        return productRepository.findById(id).orElseThrow(() -> new RuntimeException("Product not found"));
    }

    public Product createOrUpdate(Product product) {
        return productRepository.save(product);
    }

    public void delete(Integer id) {
        Product p = getById(id);
        p.setActive(false);
        productRepository.save(p);
    }
}
