package egovframework.example.sample.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import egovframework.example.sample.entity.Item;

@Repository
public interface ItemRepository extends JpaRepository<Item, Integer> {

	@Query(value = "SELECT id, title, embedding FROM public.items ORDER BY 1 - (embedding <=> CAST(:vector AS vector)) DESC LIMIT 5", nativeQuery = true)
    List<Item> findItemsSimilarity(@Param("vector") String text);
}
