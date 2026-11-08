package com.brennensubs.billvault.repository;

import com.brennensubs.billvault.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
  java.util.List<Category> findAllByOrderByNameAsc();
}
