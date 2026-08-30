package com.anhvu.vlxd.repository;

import com.anhvu.vlxd.entity.Category;
import com.anhvu.vlxd.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByActiveTrueOrderByCreatedAtDesc();

    @Query("""
            select p from Product p
            join p.category c
            where p.active = true
              and (
                lower(p.name) like lower(concat('%', :keyword, '%'))
                or lower(p.description) like lower(concat('%', :keyword, '%'))
                or lower(c.name) like lower(concat('%', :keyword, '%'))
              )
            order by p.createdAt desc
            """)
    List<Product> searchActiveProducts(@Param("keyword") String keyword);

    @Query("""
            select p from Product p
            join p.category c
            where p.active = true
              and lower(c.name) in :categoryNames
              and (
                :keyword is null
                or :keyword = ''
                or lower(p.name) like lower(concat('%', :keyword, '%'))
                or lower(p.description) like lower(concat('%', :keyword, '%'))
              )
            order by p.createdAt desc
            """)
    List<Product> searchActiveProductsByCategory(@Param("keyword") String keyword,
                                                 @Param("categoryNames") List<String> categoryNames);

    List<Product> findTop8ByCategoryAndActiveTrueAndIdNotOrderByStockQuantityDesc(Category category, Long id);
}
